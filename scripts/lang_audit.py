#!/usr/bin/env python3
"""Every registered thing that shows a name to the player has an en_us key.

The creative tab is where a missing translation is visible: an entry with no key renders
as the raw key. This walks the DeferredRegister declarations in the AC*Registry classes,
derives the translation key vanilla will actually ask for, and diffs that against
assets/alexscaves/lang/en_us.json.

The key rules are not one rule, which is why a plain "id has a key" grep gives four kinds
of false positive:

  * A BlockItem takes its name from the BLOCK, so item.alexscaves.pewen_door is never
    asked for -- block.alexscaves.pewen_door is. Either spelling counts.
  * Potions are named through the item that holds them, as
    item.minecraft.potion.effect.<id>, once for each of the four potion items. They are
    NOT effect.alexscaves.<id>, and POTION_DEF_REG is a *substring* of DEF_REG, so a
    careless regex attributes all 22 of them to the effect registry.
  * Spawn eggs are registered as "spawn_egg_" + name from a helper, so the literal id
    never appears in the source; the 43 call sites do.
  * A block with no BlockItem (a light block, a flower pot variant) has no way to show a
    name at all. Reported separately as FYI rather than as a miss.

Registrations inside a commented-out `//?` arm count -- a key missing there is missing on
the band that arm is for -- so ids come from the raw file text, gates and all, deduped.

  --fyi  also list the nameless-block and locale-coverage sections
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = ROOT / "src/main/java/com/github/alexmodguy/alexscaves"
RES = ROOT / "src/main/resources"
LANG = RES / "assets/alexscaves/lang"
NS = "alexscaves"

# POTION_DEF_REG contains DEF_REG, so the register call has to be anchored on its left.
DEF_REG = r'(?<![A-Z0-9_])DEF_REG\.register\(\s*"([a-z0-9_/]+)"'
POTION_REG = r'POTION_DEF_REG\.register\(\s*"([a-z0-9_/]+)"'

POTION_ITEMS = ("potion", "splash_potion", "lingering_potion", "tipped_arrow")


def read(rel):
    return (SRC / rel).read_text(encoding="utf-8")


def ids(rel, pattern=DEF_REG):
    return sorted(set(re.findall(pattern, read(rel))))


def block_item_ids():
    """Block ids that a BlockItem is registered for -- those are named by the block."""
    text = read("server/block/ACBlockRegistry.java") + read("server/item/ACItemRegistry.java")
    return set(re.findall(r'(?<![A-Z0-9_])DEF_REG\.register\(\s*"([a-z0-9_/]+)"', text))


def main():
    fyi = "--fyi" in sys.argv
    en = json.loads((LANG / "en_us.json").read_text(encoding="utf-8"))

    item_ids = set(ids("server/item/ACItemRegistry.java"))
    # "spawn_egg_" + entityName never appears whole in the source; the call sites carry it.
    item_ids.discard("spawn_egg_")
    item_ids |= {
        "spawn_egg_" + n for n in re.findall(r'spawnEgg\("([a-z0-9_]+)"', read("server/item/ACItemRegistry.java"))
    }
    block_ids = set(ids("server/block/ACBlockRegistry.java"))

    checks = []  # (label, id, [acceptable keys])
    for i in sorted(item_ids):
        # A BlockItem is named by its block; accept either spelling.
        checks.append(("item", i, [f"item.{NS}.{i}", f"block.{NS}.{i}"]))
    for b in sorted(block_ids):
        checks.append(("block", b, [f"block.{NS}.{b}"]))
    for e in ids("server/entity/ACEntityRegistry.java"):
        checks.append(("entity", e, [f"entity.{NS}.{e}"]))
    for e in ids("server/potion/ACEffectRegistry.java"):
        checks.append(("effect", e, [f"effect.{NS}.{e}"]))
    for p in ids("server/potion/ACEffectRegistry.java", POTION_REG):
        for holder in POTION_ITEMS:
            checks.append(("potion", p, [f"item.minecraft.{holder}.effect.{p}"]))
    for t in ids("server/misc/ACCreativeTabRegistry.java"):
        checks.append(("tab", t, [f"itemGroup.{NS}.{t}"]))
    for d in sorted(p.stem for p in (RES / "data" / NS / "enchantment").glob("*.json")):
        checks.append(("enchantment", d, [f"enchantment.{NS}.{d}"]))

    # Blocks with no item can never show a name; not a miss, but worth listing once.
    itemless = sorted(b for b in block_ids if b not in item_ids)

    missing = [(label, rid, keys) for label, rid, keys in checks if not any(k in en for k in keys)]

    for label, rid, keys in missing:
        nameless = " [block has no item]" if label == "block" and rid in itemless else ""
        print(f"MISSING  {keys[0]:<55} ({label}){nameless}")

    print()
    print(f"{len(checks)} translation keys checked, {len(missing)} missing")

    if fyi:
        print(f"\n{len(itemless)} blocks have no BlockItem (cannot show a name anywhere):")
        print("  " + ", ".join(itemless))
        print()
        wanted = set(en)
        for f in sorted(LANG.glob("*.json")):
            if f.name == "en_us.json":
                continue
            other = json.loads(f.read_text(encoding="utf-8"))
            print(f"  {f.name:<12} {len(set(other) & wanted):>5}/{len(wanted)} of en_us")

    return 1 if missing else 0


if __name__ == "__main__":
    sys.exit(main())
