#!/usr/bin/env python3
"""Same diff as tagshrink.py, but scoped to the vanilla tags THIS MOD references.

tagshrink.py sweeps all of vanilla and is noisy on a big MC bump; this one answers
the question that actually matters before a release -- "did anything the mod leans
on get smaller?" -- and so is the one to run on every wave.

References are collected two ways, and the second is a deliberate approximation:

  1. ``"#minecraft:<path>"`` strings anywhere under ``src/main/resources``.
  2. ``BlockTags.NAME`` / ``ItemTags.NAME`` / ``BiomeTags.NAME`` / ``EntityTypeTags.NAME``
     / ``FluidTags.NAME`` / ``DamageTypeTags.NAME`` in ``src/main/java``, lowercased.
     Vanilla's constant names match their tag paths for very nearly all of these; a
     handful do not (``BlockTags.LOGS_THAT_BURN`` is fine, but e.g. an aliased
     constant would not be), so a name that resolves to no tag in EITHER version is
     reported as UNKNOWN rather than silently dropped.

Usage
-----
    python3 scripts/tagdiff.py <old-mc> <new-mc>

Exit code 1 if any referenced tag shrank or vanished.
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tagshrink import load  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HOLDERS = "BlockTags|ItemTags|BiomeTags|EntityTypeTags|FluidTags|DamageTypeTags|StructureTags"


def referenced():
    refs = set()
    res = os.path.join(ROOT, "src/main/resources")
    for dp, _, fns in os.walk(res):
        for fn in fns:
            if not fn.endswith(".json"):
                continue
            text = open(os.path.join(dp, fn), encoding="utf-8", errors="replace").read()
            refs.update(re.findall(r'"#minecraft:([a-z0-9_/]+)"', text))
    java = os.path.join(ROOT, "src/main/java")
    for dp, _, fns in os.walk(java):
        for fn in fns:
            if not fn.endswith(".java"):
                continue
            text = open(os.path.join(dp, fn), encoding="utf-8", errors="replace").read()
            for name in re.findall(r'\b(?:' + HOLDERS + r')\.([A-Z][A-Z0-9_]+)\b', text):
                refs.add(name.lower())
    return refs


def expand(tags, key, seen=None):
    """Transitively resolve a tag to its concrete members.

    Comparing the literal ``values`` list gives FALSE ALARMS: 26.2 replaced
    ``#minecraft:dirt`` inside ``#minecraft:overworld_carver_replaceables`` with
    ``#minecraft:substrate_overworld``, which reads as a shrink and is in fact a
    net gain (52 -> 55 concrete blocks).  Only the expanded set is meaningful.
    """
    seen = set() if seen is None else seen
    if key in seen:
        return set()
    seen.add(key)
    out = set()
    for m in tags.get(key, set()):
        if not m:
            continue
        if m.startswith("#"):
            out |= expand(tags, (key[0], m.split(":", 1)[1]), seen)
        else:
            out.add(m)
    return out


def main():
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    old_v, new_v = sys.argv[1], sys.argv[2]
    old, new = load(old_v), load(new_v)
    refs = referenced()

    problems = 0
    for path in sorted(refs):
        keys = sorted({k for k in set(old) | set(new) if k[1] == path})
        if not keys:
            print(f"UNKNOWN {path}  (no vanilla tag with that path in either version)")
            continue
        for key in keys:
            a, b = expand(old, key), expand(new, key)
            lost, gained = a - b, b - a
            label = f"{key[0]}/{key[1]}"
            if key not in new:
                print(f"GONE    {label}  (had {len(a)} members)")
                problems += 1
            elif lost:
                print(f"SHRANK  {label}  {len(a)} -> {len(b)}  -{sorted(lost)}"
                      + (f"  +{sorted(gained)}" if gained else ""))
                problems += 1
            elif gained:
                print(f"grew    {label}  +{sorted(gained)}")
    print(f"\n{len(refs)} referenced tag path(s); {old_v} -> {new_v}: {problems} problem(s)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
