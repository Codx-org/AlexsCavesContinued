#!/usr/bin/env python3
"""Audit every shipped block/item model: face UVs, and texture-slot resolution.

TWO INDEPENDENT CHECKS, both of which caught a real shipped bug.

== 1. Out-of-bounds face UVs ==


A face `uv` is expressed in model space scaled by the model's `texture_size`
(default [16, 16]) and is then mapped onto the sprite.  A rect that leaves that
box does not clamp -- it samples whatever *neighbouring sprite* happens to sit
beside this one in the stitched atlas, so it has silently rendered foreign
pixels on every MC version this mod has ever shipped on.

From MC 26.1 it is no longer silent.  `FaceBakery.computeMaterialTransparency`
asks `SpriteContents.computeTransparency`, which asks `NativeImage
.computeTransparency`, and that now hard-throws

    IllegalArgumentException: Cannot compute translucency out of bounds:
                              [16, 6, 20, 10] in 16x16 image

The throw fails the *whole model bake*, which cascades into `Missing model for
variant` for every blockstate that referenced it -- i.e. a black/missing block,
not a slightly wrong texture.  Only faces whose quad has area reach the
transparency check, so the degenerate (zero-extent) ones are latent rather than
fatal; they are still wrong and are repaired the same way.

`--fix` rewrites the offending arrays in place with the minimal correction:
per axis, if the rect overflows, translate it so its lower edge sits on the
sprite origin; if it is *wider* than the sprite (only ever true of degenerate
faces) clamp instead.  A pure translation preserves the authored orientation,
rotation and extent, which is why the four real repairs below are pixel-exact
rather than a guess -- the sprites' opaque content sits at the origin.

== 2. Unresolved texture references ==

A face may name a slot (`#0`, `#missing`) that the model's own `textures` map
-- or a mod-owned ancestor's -- never defines, and a model may name a texture
id with no PNG behind it.  Both render the magenta missing-texture sprite, and
from **MC 1.21.4** both are reported as

    Missing texture references in model alexscaves:block/quarry:
        #missing

so this is invisible on the 30 nodes below 1.21.4 and noisy on the 28 above.
Blockbench emits `#missing` for a face the modeller left untextured; here every
one of them was either a zero-area quad on a degenerate element or an interior
face fully enclosed by its siblings, which is why nobody ever saw magenta.

The third shape is an ISTER item model -- `parent: builtin/entity` with no
`textures` block at all -- whose `particle` slot has therefore never resolved.
`SpecialModelWrapper` bakes the base model only for its display transforms, so
that one is cosmetic (the break/use particle icon) rather than visible geometry.

Exits 1 on any finding.  Run before every release.
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODELS = os.path.join(ROOT, "src/main/resources/assets/alexscaves/models")

# which two element axes a face's quad spans; a zero extent on either makes the
# quad degenerate (zero area), so it rasterises nothing whatever its uv says.
FACE_AXES = {
    "north": (0, 1), "south": (0, 1),
    "east": (2, 1), "west": (2, 1),
    "up": (0, 2), "down": (0, 2),
}

UV_RE = re.compile(r'("uv"\s*:\s*)\[([^\]]*)\]')


def faces_in_document_order(model):
    """Yield (element, face_name, uv) in the order the arrays appear in the file.

    `elements` is a list and `faces` is a JSON object, and json.loads preserves
    both orderings, so the Nth yield here is the Nth `"uv"` array in the text.
    """
    for elem in model.get("elements", []):
        for name, face in elem.get("faces", {}).items():
            if "uv" in face:
                yield elem, name, face["uv"]


def repair(uv, size):
    """Minimal in-bounds correction: translate per axis, clamp only if too wide."""
    out = list(uv)
    for axis, limit in ((0, size[0]), (1, size[1])):
        a, b = out[axis], out[axis + 2]
        lo, hi = min(a, b), max(a, b)
        if lo >= 0 and hi <= limit:
            continue
        if hi - lo > limit:                      # cannot fit -- clamp both ends
            out[axis] = max(0, min(limit, a))
            out[axis + 2] = max(0, min(limit, b))
        else:                                    # slide the rect back in
            shift = -lo if lo < 0 or hi > limit else 0
            out[axis] = a + shift
            out[axis + 2] = b + shift
    return out


def audit(fix=False):
    bad = 0
    unparsable = []
    for dirpath, _, names in os.walk(MODELS):
        for name in sorted(names):
            if not name.endswith(".json"):
                continue
            path = os.path.join(dirpath, name)
            text = open(path, encoding="utf-8").read()
            try:
                model = json.loads(text)
            except ValueError as exc:
                unparsable.append((os.path.relpath(path, ROOT), exc))
                continue
            size = model.get("texture_size", [16, 16])
            faces = list(faces_in_document_order(model))
            hits = UV_RE.findall(text)
            if len(hits) != len(faces):
                print("!! %s: %d uv arrays in text but %d faces parsed -- skipped"
                      % (os.path.relpath(path, ROOT), len(hits), len(faces)))
                bad += 1
                continue
            fixes = {}
            for idx, (elem, fname, uv) in enumerate(faces):
                if len(uv) != 4:
                    continue
                new = repair(uv, size)
                if new == list(uv):
                    continue
                bad += 1
                ax = FACE_AXES.get(fname)
                kind = "real"
                if ax and "from" in elem and "to" in elem:
                    d = [abs(elem["to"][i] - elem["from"][i]) for i in range(3)]
                    if d[ax[0]] == 0 or d[ax[1]] == 0:
                        kind = "degenerate"
                print("%-58s elem%-2d %-5s %-22s -> %-20s (%s)"
                      % (os.path.relpath(path, MODELS), model["elements"].index(elem),
                         fname, uv, new, kind))
                fixes[idx] = new
            if fixes and fix:
                counter = {"n": -1}

                def sub(m):
                    counter["n"] += 1
                    if counter["n"] not in fixes:
                        return m.group(0)
                    return m.group(1) + "[" + ", ".join(
                        str(v) for v in fixes[counter["n"]]) + "]"

                open(path, "w", encoding="utf-8").write(UV_RE.sub(sub, text))

    for path, exc in unparsable:
        print("!! unparsable: %s (%s)" % (path, exc))
    print("\nout-of-bounds faces: %d   unparsable models: %d"
          % (bad, len(unparsable)))
    return 0 if bad == 0 and not unparsable else 1


# ---------------------------------------------------------------- check 2 ---
ASSETS = os.path.join(ROOT, "src/main/resources/assets/alexscaves")
BLOCKSTATES = os.path.join(ASSETS, "blockstates")
NS = "alexscaves"


def model_id(path):
    rel = os.path.relpath(path, MODELS)[:-len(".json")]
    return "%s:%s" % (NS, rel.replace(os.sep, "/"))


def qualify(ref):
    return ref if ":" in ref else "minecraft:" + ref


def load_all():
    out = {}
    for dirpath, _, names in os.walk(MODELS):
        for name in sorted(names):
            if name.endswith(".json"):
                path = os.path.join(dirpath, name)
                try:
                    out[model_id(path)] = json.loads(
                        open(path, encoding="utf-8").read())
                except ValueError:
                    pass                      # check 1 already reports these
    return out


def bake_roots(models):
    """Every model MC actually bakes: named by a blockstate, or an item model.

    A model that is only ever a `parent` is a TEMPLATE -- `block/anemone_base`
    and friends leave `#base`/`#tentacles` deliberately open for a child to
    fill, so resolving slots on one in isolation invents dozens of misses.
    MC resolves in the context of the leaf, and so does this.
    """
    roots = set()
    for name in sorted(os.listdir(BLOCKSTATES)):
        if not name.endswith(".json"):
            continue
        text = open(os.path.join(BLOCKSTATES, name), encoding="utf-8").read()
        roots.update(qualify(m) for m in re.findall(r'"model"\s*:\s*"([^"]+)"', text))
    for mid, model in models.items():
        if mid.startswith("%s:item/" % NS):
            roots.add(mid)
        for ov in model.get("overrides", []):
            if "model" in ov:
                roots.add(qualify(ov["model"]))
    return {r for r in roots if r in models}


def ancestry(models, mid):
    """The model and each mod-owned ancestor, nearest first."""
    chain, seen = [], set()
    while mid in models and mid not in seen:
        seen.add(mid)
        chain.append(mid)
        parent = models[mid].get("parent")
        mid = qualify(parent) if parent else None
    return chain


def resolve_slot(models, chain, slot):
    """Follow `#slot` -> ... -> texture id through the leaf's own ancestry.

    Every vanilla template (`block/block`, `block/cube_all`, `builtin/entity`,
    `item/generated`, ...) *references* slots and defines none, so a chain that
    runs off into `minecraft:` unresolved is a genuine miss, not a blind spot.
    """
    for _ in range(16):                       # alias depth guard
        for mid in chain:
            value = models[mid].get("textures", {}).get(slot)
            if value is None:
                continue
            if not value.startswith("#"):
                return value
            slot = value[1:]                  # aliased onto another slot
            break
        else:
            return None
    return None


def texture_exists(tex):
    ns, sep, path = tex.rpartition(":")
    if sep and ns != NS:
        return True                           # vanilla; not ours to verify
    if not sep:
        return True                           # unqualified == minecraft:
    return os.path.isfile(os.path.join(ASSETS, "textures", path + ".png"))


def atlas_resident(tex):
    """A model texture slot is resolved against the stitched `minecraft:blocks`
    atlas, whose only sources are the `block/` and `item/` directories (across
    every namespace -- the mod ships no `atlases/blocks.json` of its own).  A
    texture that exists on disk but lives anywhere else -- `entity/`, which is
    loaded standalone by an entity renderer -- is therefore NOT in the atlas,
    and naming one warns `Missing textures in model <id>:` even though the PNG
    is right there.  That is the failure mode that made this check necessary:
    three ISTER items were given a `particle` slot pointing at their entity
    texture, which is the obvious file and the wrong atlas.
    """
    ns, sep, path = tex.rpartition(":")
    if not sep or ns != NS:
        return True                           # vanilla; not ours to verify
    return path.split("/")[0] in ("block", "item")


def audit_textures():
    models = load_all()
    bad = 0
    for mid in sorted(bake_roots(models)):
        chain = ancestry(models, mid)
        # elements are inherited from the nearest ancestor that declares any
        elements = next((models[a]["elements"] for a in chain
                         if "elements" in models[a]), [])
        for elem in elements:
            for fname, face in elem.get("faces", {}).items():
                ref = face.get("texture", "")
                if not ref.startswith("#"):
                    continue
                tex = resolve_slot(models, chain, ref[1:])
                if tex is None:
                    print("%-46s %-5s %s unresolved" % (mid, fname, ref))
                    bad += 1
                elif not texture_exists(tex):
                    print("%-46s %-5s %s missing png" % (mid, fname, tex))
                    bad += 1
                elif not atlas_resident(tex):
                    print("%-46s %-5s %s not in the block atlas"
                          % (mid, fname, tex))
                    bad += 1
        # a bakeable model with no particle icon warns from 1.21.4; the ISTER
        # item models (`builtin/entity`, no `textures` at all) are the family
        particle = resolve_slot(models, chain, "particle")
        if particle is not None and not texture_exists(particle):
            print("%-46s particle %s missing png" % (mid, particle))
            bad += 1
        elif particle is not None and not atlas_resident(particle):
            print("%-46s particle %s not in the block atlas" % (mid, particle))
            bad += 1
        if particle is None and not any(
                a.startswith("minecraft:") for a in chain[-1:] if False):
            if "minecraft:builtin/entity" == qualify(
                    models[mid].get("parent", "")):
                print("%-46s builtin/entity with no particle texture" % mid)
                bad += 1
    for mid in sorted(models):                # (b) ids named outright
        for slot, value in models[mid].get("textures", {}).items():
            if not value.startswith("#") and not texture_exists(value):
                print("%-46s slot %-9s %s missing png" % (mid, slot, value))
                bad += 1
    print("\nunresolved texture references: %d" % bad)
    return 0 if bad == 0 else 1


# ---------------------------------------------------------------- check 3 ---
def audit_parents():
    """Every `parent` in the mod's namespace must name a model that exists.

    An item model left behind for a block that was never registered still gets
    an item-model definition generated for it (`writeItemModelDefinitions`
    derives them from this tree), so MC loads it and warns `Missing block
    model` for the parent it cannot find -- a dangling file nothing else in the
    tree references.  Vanilla parents are taken on trust; they are not ours.
    """
    models = load_all()
    bad = 0
    for mid in sorted(models):
        parent = models[mid].get("parent")
        if not parent:
            continue
        parent = qualify(parent)
        if parent.startswith(NS + ":") and parent not in models:
            print("%-46s missing parent %s" % (mid, parent))
            bad += 1
    print("\nunresolvable model parents: %d" % bad)
    return 0 if bad == 0 else 1


if __name__ == "__main__":
    sys.exit(audit(fix="--fix" in sys.argv) | audit_textures()
             | audit_parents())
