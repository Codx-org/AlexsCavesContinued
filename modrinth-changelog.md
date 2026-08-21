# 1.0.0

First release. This is AlexModGuy's Alex's Caves, ported forward and out sideways: every Minecraft
version from 1.20.1 to 26.2, on Fabric, NeoForge and Forge. 58 files in all.

The content is untouched. Same six biomes, same 43 mobs, same 14 structures, same 354 blocks and 575
items, same recipes and advancements. Nothing rebalanced, nothing added, nothing cut. If you played
the original, this is it.

## What changed around it

Citadel is bundled now. The original needed it as a separate download; that code lives inside this
jar instead, so it's one less thing to keep in step.

Fabric and NeoForge work, which the original never supported.

CodxLib is required: https://modrinth.com/mod/codxlib. Grab the file for your exact Minecraft
version and loader and drop it in alongside this one.

Install it on the client and the server both. It's a content mod, so both sides need it.

## Bugs fixed

These were all in the original. Some of them only started showing up on newer Minecraft versions,
which is how I found them.

The licowitch and the tremorzilla couldn't be summoned at all on 1.20.5 and up. One wrong class name
in the original source had the two mobs fighting over the same slot of synced data, and newer
Minecraft versions turn that into a hard error instead of quietly coping. Both spawn fine now.

Eleven mobs crashed the server the first tick after they spawned, on 1.21.2 and up. That's the
version where Minecraft moved the "notices a player holding food" range onto an entity attribute,
and those eleven never declared it.

A pile of recipes were silently uncraftable. They pointed at shared convention tags that the loader
in question had never actually defined, and a missing ingredient tag doesn't error on older
versions, it just empties the ingredient and the recipe stops matching. Concrete and iron nuggets
were the worst of it. The mod owns those tags itself now, so the recipes work everywhere, and other
mods' equivalent items still count toward them.

Magnets did nothing to iron on a few Fabric builds. Same cause.

The primordial, hazmat, diving and gingerbread armour sets rendered as nothing at all from 1.20.5
onward. They were relying on a Forge hook that stopped existing.

A structure exclusion zone that had never once applied, and a spear model pointing at the wrong
file. Fixed where fixing it was clearly right, left alone and written down where it wasn't.

## Testing

All 58 files boot, client and server, on the Minecraft version and loader they're built for. Three
of them got a full in-world pass over RCON on a fixed seed: every biome located, every structure
placed, every block set, every mob summoned, every loot table rolled. Those three agree with each
other down to the coordinates.

Alex's Caves is AlexModGuy's work and this project claims none of it. LGPL-3.0, same as the
original.
