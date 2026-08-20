#!/usr/bin/env python3
"""Check every entity's synched-data accessors against the class that owns them.

Why this exists
---------------
An ``EntityDataAccessor``'s id is allocated out of the id tree of the class handed
to ``defineId`` — not the class the field is declared in.  Upstream 2.0.2 had one
copy-paste where ``LicowitchEntity``'s accessor named ``TremorzillaEntity.class``,
so the licowitch's accessor took a slot in the *tremorzilla's* tree.  From 1.20.5
``SynchedEntityData.Builder`` sizes its slot array from the entity's own tree, so
**both** entities broke, with two unrelated-looking messages:

    licowitch    IllegalArgumentException: Data value id is too big with 35! (Max is 26)
    tremorzilla  IllegalStateException: Entity class ...TremorzillaEntity has not
                 defined synched data value 35

and vanilla catches both — the server logs a warning, reaches ``Done``, and the two
mobs simply can never exist.  Below 1.20.5 ids were an unbounded map, so the bug was
genuinely harmless there, which is why a green ``1.20.1-forge`` battery looked like
proof.

Two assertions, both spelling- and Stonecutter-gate-independent:

1. every ``defineId(X.class, ...)`` in an entity class must name **that** class;
2. the set of accessors a class *declares* must equal the set it *defines* in
   ``defineSynchedData`` — a declared-but-never-defined accessor is the same hole
   in the id tree, arrived at from the other side.

Run it before every release.  Exit code is 1 if anything is off.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src/main/java/com/github/alexmodguy/alexscaves"

# Mixin classes legitimately hand a *vanilla* class: the accessor belongs to
# Entity / LivingEntity / FallingBlockEntity by design, and keeping the defineId
# in the mixin is what preserves definition order and wire format.  (NeoForge
# 21.8 forbids the accessor *field* living there, which is why the fields sit in
# CitadelSyncedData / ACSyncedData — see DEVELOPMENT.md.)
SKIP_DIRS = ("mixin/",)

DECLARE = re.compile(
    r"\b(\w+)\s*=\s*(?:SynchedEntityData\.)?defineId\(\s*([\w.]+)\.class", re.S)
# `this.entityData.define(` below 1.20.5, `builder.define(` from it — a gated file
# carries both spellings, so accessors are compared as a SET of names rather than
# counted, which is also a stronger assertion than a count.
DEFINE = re.compile(r"\b(?:entityData|builder)\.define\(\s*(?:[\w.]+\.)?(\w+)")


def main() -> int:
    files = [f for f in sorted(SRC.rglob("*.java"))
             if not any(d in str(f.relative_to(SRC)) for d in SKIP_DIRS)]
    wrong_owner, unpaired = [], []
    classes = accessors = 0

    for f in files:
        text = f.read_text()
        declared = {m.group(1): m.group(2).split(".")[-1] for m in DECLARE.finditer(text)}
        if not declared:
            continue
        classes += 1
        accessors += len(declared)
        rel = f.relative_to(SRC)
        for name, owner in declared.items():
            if owner != f.stem:
                wrong_owner.append((rel, name, owner, f.stem))
        defined = {m.group(1) for m in DEFINE.finditer(text)} & set(declared)
        missing = set(declared) - defined
        if missing:
            unpaired.append((rel, sorted(missing)))

    print(f"scanned     {len(files)} files, {classes} declaring classes, {accessors} accessors")

    if wrong_owner:
        print(f"\nWRONG OWNER ({len(wrong_owner)}) — the accessor takes a slot in another class's "
              f"id tree, breaking BOTH entities from 1.20.5:")
        for rel, name, owner, stem in wrong_owner:
            print(f"  {rel}\n      {name} names {owner}.class, should be {stem}.class")
    if unpaired:
        print(f"\nDECLARED BUT NEVER DEFINED ({len(unpaired)}) — leaves a hole in the id tree, so "
              f"build() throws 'has not defined synched data value N':")
        for rel, names in unpaired:
            print(f"  {rel}: {', '.join(names)}")
    if wrong_owner or unpaired:
        return 1
    print("\nOK — every accessor names its own class and is defined by it.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
