#!/usr/bin/env python3
"""Find mod methods that LOOK like overrides of a vanilla method but override nothing.

The trap this exists for (DEVELOPMENT.md, "the ServerLevel sweep"): vanilla renames a
method's *parameters* while keeping its name, a mod method written without `@Override`
keeps compiling on every node, silently stops running from the version the signature
moved, and nothing — not javac, not `verify_mixins.py`, not a green boot — says so.
`WaterAnimal#handleAirSupply` gained a leading `ServerLevel` at 1.21.5 and drowned every
Alex's Caves water mob on 33 of the 58 nodes for exactly that reason.

The check is by PARAMETER LIST, not by full descriptor, so a covariant return (which javac
implements as a synthetic bridge) is not a false positive:

    for each mod method m that is not static/private/synthetic and not a constructor,
    walk the mod class's supers and interfaces up to their first vanilla ancestors;
    if any vanilla ancestor declares m's NAME but none of its overloads has m's
    PARAMETER LIST, m overrides nothing and is reported.

Reads the compiled dev classes (`versions/<node>/build/classes/java/main`, Mojmap, before
remap) against the cached Mojmap merged jar for that node's MC version, so it sees
post-gate, post-rename source exactly as `verify_mixins.py` does.

    python3 scripts/override_audit.py                     # every node with both halves cached
    python3 scripts/override_audit.py 1.21.5-fabric 26.2-fabric
    python3 scripts/override_audit.py --verbose

Exit 1 if anything is reported.
"""
import argparse
import glob
import mcjavap
import os
import struct
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.expanduser("~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft")

ACC_STATIC = 0x0008
ACC_PRIVATE = 0x0002
ACC_BRIDGE = 0x0040
ACC_SYNTHETIC = 0x1000

# Mod methods that share a name with a vanilla one but are deliberately a different method.
# Each entry is "<simple class>#<name>(<params>)" and must carry a reason.
ALLOW = {
    # Mod-internal, and only *named* like a vanilla method. LicowitchEntity#canTeleport() is the
    # witch's own "may I blink right now" question, asked from LicowitchUseCrucibleGoal,
    # LicowitchAttackGoal and within the class; Entity#canTeleport(Level, Level) is the
    # cross-dimension permission check and is not what any of those callers mean.
    "LicowitchEntity#canTeleport()",
    # Covariant-by-hand rather than by bridge: the 1.21/1.21.1 arm of BasicEntityModel declares the
    # packed-int renderToBuffer(PoseStack, VertexConsumer, int, int, int) vanilla calls and unpacks
    # it into this eight-float hook, so the wide shape below is the *target* of an override that
    # did take, one gate up. Checked in the source, not inferred.
    "BasicEntityModel#renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)",
}

# Packages whose whole point is to declare a vanilla method in a shape vanilla no longer
# uses: `client/render/compat/**` re-supplies the pre-1.21.2 renderer/model surface under
# the mod's own namespace so ~700 call sites stay byte-identical on all three loaders.
# Reporting those is reporting the design (see docs/notes/fabric.md).
ALLOW_PREFIX = (
    "com/github/alexmodguy/alexscaves/client/render/compat/",
)


# ---------------------------------------------------------------- class file

def parse_class(data):
    """-> (name, super, [interfaces], {method_name: {param_list: access}}) or None."""
    if data[:4] != b"\xca\xfe\xba\xbe":
        return None
    cp_count = struct.unpack_from(">H", data, 8)[0]
    pos = 10
    cp = [None] * cp_count
    i = 1
    while i < cp_count:
        tag = data[pos]
        pos += 1
        if tag == 1:
            n = struct.unpack_from(">H", data, pos)[0]
            cp[i] = data[pos + 2:pos + 2 + n].decode("utf-8", "replace")
            pos += 2 + n
        elif tag in (7, 8, 16, 19, 20):
            cp[i] = struct.unpack_from(">H", data, pos)[0]
            pos += 2
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            pos += 4
        elif tag in (5, 6):
            pos += 8
            i += 1
        elif tag == 15:
            pos += 3
        else:
            raise ValueError(f"bad constant pool tag {tag}")
        i += 1

    def cls(idx):
        return cp[cp[idx]] if idx else None

    pos += 2                                              # access_flags
    this_name = cls(struct.unpack_from(">H", data, pos)[0]); pos += 2
    super_name = cls(struct.unpack_from(">H", data, pos)[0]); pos += 2
    n_ifaces = struct.unpack_from(">H", data, pos)[0]; pos += 2
    ifaces = [cls(struct.unpack_from(">H", data, pos + 2 * k)[0]) for k in range(n_ifaces)]
    pos += 2 * n_ifaces

    def skip_members():
        nonlocal pos
        out = []
        count = struct.unpack_from(">H", data, pos)[0]; pos += 2
        for _ in range(count):
            acc, name_i, desc_i, n_attr = struct.unpack_from(">HHHH", data, pos)
            pos += 8
            for _ in range(n_attr):
                alen = struct.unpack_from(">I", data, pos + 2)[0]
                pos += 6 + alen
            out.append((acc, cp[name_i], cp[desc_i]))
        return out

    skip_members()                                        # fields
    methods = {}
    for acc, name, desc in skip_members():
        methods.setdefault(name, {})[desc[:desc.index(")") + 1]] = acc
    return this_name, super_name, ifaces, methods


def index_jar(path):
    idx = {}
    with zipfile.ZipFile(path) as z:
        for n in z.namelist():
            if not n.endswith(".class"):
                continue
            try:
                parsed = parse_class(z.read(n))
            except Exception:
                continue
            if parsed:
                idx[parsed[0]] = parsed
    return idx


def index_dir(path):
    idx = {}
    for f in glob.glob(os.path.join(path, "**", "*.class"), recursive=True):
        with open(f, "rb") as fh:
            try:
                parsed = parse_class(fh.read())
            except Exception:
                continue
        if parsed:
            idx[parsed[0]] = parsed
    return idx


# ---------------------------------------------------------------- vanilla jar

def vanilla_jar(mc):
    """The loom-cached vanilla jar for `mc`, or None.

    Delegates to mcjavap.jar_for so there is ONE place that knows loom's two cache layouts.
    A Fabric-only MC version (1.20.3, 1.20.5, 1.21.2 here) has no
    `<loader>-<mc>-<build>-minecraft-merged/` directory at all -- its jar sits under the shared
    `minecraft-merged/<mc>-loom…/` one. Getting that wrong silently SKIPS those nodes, and a
    skipped node's band gets guessed from its neighbours; that is how two boundaries in this tree
    ended up one MC version too high. jar_for exits on a miss, so translate that back to None.
    """
    try:
        return mcjavap.jar_for(mc)
    except SystemExit:
        return None


# ---------------------------------------------------------------- the check

def vanilla_overloads(name, start, mod, van):
    """Every parameter list vanilla ancestors of `start` declare under `name`.

    Walks supers and interfaces; a mod class in the chain is walked through, a vanilla one
    is read and its own supers followed. Returns (params set, [owner names]).
    """
    seen, queue, found, owners, modside = set(), [start], set(), [], set()
    while queue:
        cur = queue.pop(0)
        if cur is None or cur in seen:
            continue
        seen.add(cur)
        entry = mod.get(cur) or van.get(cur)
        if entry is None:
            continue
        _, sup, ifaces, methods = entry
        if cur != start and cur in mod and name in methods:
            modside |= {p for p, a in methods[name].items()
                        if not (a & (ACC_STATIC | ACC_PRIVATE))}
        if cur in van and name in methods:
            for params, acc in methods[name].items():
                if not (acc & (ACC_STATIC | ACC_PRIVATE)):
                    found.add(params)
            if cur not in owners:
                owners.append(cur)
        queue.append(sup)
        queue.extend(ifaces)
    return found, owners, modside


def audit(node, verbose=False):
    mc = node.rsplit("-", 1)[0]
    jar = vanilla_jar(mc)
    classes = os.path.join(ROOT, "versions", node, "build", "classes", "java", "main")
    if jar is None or not os.path.isdir(classes):
        return None
    van = index_jar(jar)
    mod = index_dir(classes)
    problems = []
    for cname, (_, sup, ifaces, methods) in sorted(mod.items()):
        for mname, forms in sorted(methods.items()):
            if mname in ("<init>", "<clinit>"):
                continue
            # A generic or covariant override is compiled as the narrow method plus a
            # synthetic bridge carrying the erased/wide descriptor. The narrow one matches
            # no vanilla parameter list and is not a bug — the bridge beside it is proof
            # the override took. Collect those first so they can be excused.
            bridged = {p for p, a in forms.items() if a & (ACC_BRIDGE | ACC_SYNTHETIC)}
            for params, acc in sorted(forms.items()):
                if acc & (ACC_STATIC | ACC_PRIVATE | ACC_BRIDGE | ACC_SYNTHETIC):
                    continue
                over, owners, modside = vanilla_overloads(mname, cname, mod, van)
                # A method that overrides a MOD ancestor's method of the same shape is not
                # a miss even when vanilla spells it differently: this tree interposes its
                # own shims between several renderers/entities and their vanilla bases
                # (the `!mc2102-render-import-entity` family), and those are the point.
                if not over or params in over or params in modside or (bridged & (over | modside)):
                    continue
                key = f"{cname.rsplit('/', 1)[-1]}#{mname}{params}"
                if key in ALLOW or cname.startswith(ALLOW_PREFIX):
                    continue
                problems.append((cname, mname, params, sorted(over), owners))
    return len(mod), problems


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("nodes", nargs="*")
    ap.add_argument("--verbose", action="store_true")
    args = ap.parse_args()

    nodes = args.nodes or sorted(
        os.path.basename(p) for p in glob.glob(os.path.join(ROOT, "versions", "*"))
        if os.path.isdir(os.path.join(p, "build", "classes", "java", "main"))
    )
    total, skipped = 0, []
    for node in nodes:
        res = audit(node, args.verbose)
        if res is None:
            skipped.append(node)
            continue
        n_classes, problems = res
        total += len(problems)
        status = "OK" if not problems else f"{len(problems)} PROBLEM(S)"
        print(f"{node:20} {n_classes:5} classes   {status}")
        for cname, mname, params, over, owners in problems:
            print(f"    {cname}")
            print(f"      declares  {mname}{params}")
            print(f"      vanilla   {', '.join(mname + p for p in over)}"
                  f"   [{', '.join(o.rsplit('/', 1)[-1] for o in owners)}]")
    if skipped:
        print(f"\nskipped (no cached vanilla jar or no compiled classes): {' '.join(skipped)}")
    print(f"\n{total} problem(s)")
    return 1 if total else 0


if __name__ == "__main__":
    sys.exit(main())
