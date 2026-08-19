#!/usr/bin/env python3
"""Generate the 1.21+ data-pack enchantment files from ACEnchantmentRegistry's pre-1.21 arm.

From MC 1.21 an Enchantment is a final record loaded out of `data/<ns>/enchantment/<name>.json`
instead of being constructed in code, so the numbers upstream passed to `new ACWeaponEnchantment(...)`
have to exist as JSON. Rather than transcribe 51 rows by hand this parses them straight out of the
`//? if <1.21` arm of ACEnchantmentRegistry -- that arm stays in the tree for the older nodes, so it
remains the single source of truth and this script can be re-run after any balance change.

Emits:
  data/alexscaves/enchantment/<name>.json                  (51 files)
  data/minecraft/tags/enchantment/{in_enchanting_table,non_treasure,tradeable,on_random_loot}.json

Both directories are ignored by every MC version below 1.21 (no such data-pack registry, and the
tag manager only walks the directories of registries it knows), so they ship on every node unguarded.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
REGISTRY = ROOT / "src/main/java/com/github/alexmodguy/alexscaves/server/enchantment/ACEnchantmentRegistry.java"
DATA = ROOT / "src/main/resources/data"

# ACWeaponEnchantment.Grade -- weight and anvil cost per grade.
GRADES = {"COMMON": (10, 1), "UNCOMMON": (5, 2), "RARE": (2, 4), "VERY_RARE": (1, 8)}

# ACEnchantmentRegistry.areCompatible, one entry per pair. 1.21 checks exclusivity in both
# directions (`Enchantment.areCompatible` tests each side's exclusive_set), so each pair is
# listed once, on whichever side upstream's if-chain named first.
EXCLUSIVE = {
    "x_ray": ["alexscaves:gamma_ray"],
    "second_wave": ["alexscaves:tsunami"],
    "taxing_bellow": ["minecraft:unbreaking", "minecraft:mending"],
    "bouncing_bolt": ["alexscaves:triple_splash"],
    "detonating_death": ["alexscaves:astral_transferring"],
    "impending_stab": ["alexscaves:double_stab"],
    "relentless_darkness": [
        "alexscaves:precise_volley",
        "alexscaves:dark_nock",
        "alexscaves:twilight_perfection",
    ],
    "targeted_ricochet": ["alexscaves:triple_split"],
}

# isDiscoverable() was unconditionally true, and isTradeable()/isAllowedOnBooks() followed the
# `enchantments_in_loot` config. A tag cannot read a config, so all four memberships are static and
# EnchantRandomlyFunctionMixin keeps enforcing the toggle on the loot path (see its comment).
TAGS = ["in_enchanting_table", "non_treasure", "tradeable", "on_random_loot"]

CALL = re.compile(
    r'DEF_REG\.register\("(?P<name>\w+)",\s*\(\)\s*->\s*new ACWeaponEnchantment\('
    r'\s*"(?P=name)"\s*,\s*Grade\.(?P<grade>\w+)\s*,\s*(?P<category>\w+)\s*,'
    r'\s*(?P<levels>\d+)\s*,\s*(?P<min_xp>\d+)\s*,\s*(?P<slots>[^)]*)\)'
)


def parse() -> list[dict]:
    out = []
    for m in CALL.finditer(REGISTRY.read_text()):
        slots = [s.strip().removeprefix("EquipmentSlot.").lower() for s in m["slots"].split(",")]
        out.append(
            {
                "name": m["name"],
                "grade": m["grade"],
                "category": m["category"].lower(),
                "levels": int(m["levels"]),
                "min_xp": int(m["min_xp"]),
                "slots": slots,
            }
        )
    return out


def enchantment_json(e: dict) -> dict:
    weight, anvil_cost = GRADES[e["grade"]]
    doc = {
        "description": {"translate": f"enchantment.alexscaves.{e['name']}"},
        "supported_items": f"#alexscaves:enchantable/{e['category']}",
        "weight": weight,
        "max_level": e["levels"],
        # The two curves reproduce ACWeaponEnchantment's overrides exactly:
        #   getMinCost(i) = 1 + (i - 1) * minXP
        #   getMaxCost(i) = (1 + i * 10) + 30 = 41 + (i - 1) * 10
        "min_cost": {"base": 1, "per_level_above_first": e["min_xp"]},
        "max_cost": {"base": 41, "per_level_above_first": 10},
        "anvil_cost": anvil_cost,
        "slots": e["slots"],
    }
    if e["name"] in EXCLUSIVE:
        doc["exclusive_set"] = EXCLUSIVE[e["name"]]
    return doc


def write(path: Path, doc) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(doc, indent=2) + "\n")


def main() -> int:
    entries = parse()
    if len(entries) != 51:
        print(f"expected 51 enchantments, parsed {len(entries)}", file=sys.stderr)
        return 1

    names = [e["name"] for e in entries]
    unknown = {t for v in EXCLUSIVE.values() for t in v if t.startswith("alexscaves:")}
    unknown -= {f"alexscaves:{n}" for n in names}
    if unknown:
        print(f"exclusive_set references unknown enchantments: {sorted(unknown)}", file=sys.stderr)
        return 1

    for e in entries:
        write(DATA / "alexscaves/enchantment" / f"{e['name']}.json", enchantment_json(e))
    for tag in TAGS:
        write(
            DATA / "minecraft/tags/enchantment" / f"{tag}.json",
            {"values": [f"alexscaves:{n}" for n in names]},
        )

    print(f"wrote {len(entries)} enchantments and {len(TAGS)} tags")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
