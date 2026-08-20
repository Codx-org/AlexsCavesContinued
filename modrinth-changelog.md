# 1.0.0 — the first release

The first release of **Alex's Caves Continued**: AlexModGuy's Alex's Caves, unchanged in content,
brought forward from Minecraft 1.20.1/Forge to **58 builds** — every Minecraft version from
**1.20.1 to 26.2**, on **Fabric, NeoForge and Forge**.

If you played the original, this is that mod. No rebalancing, no additions, no removals.

## What's in it

- **All six cave biomes**, all 43 mobs, all 14 structures, 354 blocks, 575 items, 467 recipes and
  146 advancements — the whole of the original.
- **Citadel is bundled.** The original needed it as a separate download; that code now ships inside
  the mod, so there is one less dependency to keep in step.
- **Fabric and NeoForge support**, which the original never had.
- **[CodxLib](https://modrinth.com/mod/codxlib) is required** — install the file matching your exact
  Minecraft version and loader, alongside this one.

## Fixed along the way

Porting turned up a handful of bugs that were present in the original and have been fixed here:

- **The licowitch and the tremorzilla could not be summoned** on Minecraft 1.20.5 and above. A
  one-word mix-up in the original's code made the two mobs fight over the same slot of synced data,
  and newer Minecraft versions turn that into a hard error. Both work now.
- **Eleven mobs crashed the server** the moment one of them noticed a player holding food, on
  Minecraft 1.21.2 and above — the version where Minecraft moved that behaviour onto a property
  those mobs did not declare.
- **Several recipes were quietly uncraftable.** A number of recipes referenced shared "convention"
  item tags that the loader in question had never actually defined, which silently emptied the
  ingredient — concrete and iron-nugget recipes among them. Every one of those tags is now owned by
  the mod, so the recipes work on all 58 builds, and other mods' equivalent items still count.
- **Magnets ignored iron** on several Fabric builds, for the same reason.
- **The four plain armour sets were invisible** from Minecraft 1.20.5 onward.
- A **map exclusion zone** and a **spear model reference** that had never worked in the original
  have been corrected or, where correcting them would change behaviour, left alone and documented.

## Notes

- **Install on both the client and the server.** This is a content mod; both sides need it.
- **Every one of the 58 builds has been launch-tested**, client and server, on the Minecraft version
  and loader it is built for.

Alex's Caves is AlexModGuy's work. This project exists to keep it playable, and claims none of it.
Licensed under LGPL-3.0, the same licence as the original.
