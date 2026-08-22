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

CodxLib is required, 1.3.6 or newer: https://modrinth.com/mod/codxlib. Grab the file for your exact
Minecraft version and loader and drop it in alongside this one. Without it the game won't start.

Install both mods on the client and the server. It's a content mod, so both sides need it.

There's a `/acc` command now, for server owners. It opens a chest menu over the general config, so
the options can be changed in game instead of by editing a toml and restarting.

## Bugs fixed

Most of these were in the original. Some only started showing up on newer Minecraft versions, which
is how I found them.

Every advancement fired the moment you loaded a world, on 1.20.5 and up. The mod's advancements ask
"are you standing in this biome" and "are you inside this structure", and 1.20.5 renamed the fields
that carry the answer. Minecraft doesn't complain about a field it doesn't recognise, it just drops
it — which left the question empty, and an empty question is true everywhere. Since one of the ones
firing was the root advancement, the whole tab popped at once.

The licowitch and the tremorzilla couldn't be summoned at all on 1.20.5 and up. One wrong class name
in the original source had the two mobs fighting over the same slot of synced data, and newer
Minecraft versions turn that into a hard error instead of quietly coping. Both spawn fine now.

Eleven mobs crashed the server the first tick after they spawned, on 1.21.2 and up. That's the
version where Minecraft moved the "notices a player holding food" range onto an entity attribute,
and those eleven never declared it.

Every block's item was named `item.alexscaves.something` instead of its real name, on 1.21.2 and up.
Icons and models were fine, so it only showed if you hovered over one.

Opening the creative inventory crashed the client. Galena bricks were listed twice in the Magnetic
Caves tab and Minecraft refuses to build a tab with a duplicate in it. That one has been in the mod
since 1.19.3.

The Magnetron crashed the client the first frame you could see one, because of how the lightning
bolts kept track of who they belonged to.

Finding one of the mod's cave biomes and teleporting into it froze the game, on 26.1 and up, in
singleplayer only. Minecraft added a check that runs before the world has finished opening and
freezes the list of things each biome is allowed to generate; the mod adds its biomes a moment
later, so they weren't on the list, and the first chunk that tried to decorate one died with the
world already loaded. Servers were never affected, which is why it took this long to find.

On 26.2, an Amber Monolith crashed the client the first frame it came into view, and the hologram
projector would have done the same. Both show a mob that isn't really in the world, and 26.2 started
refusing to answer a question about mobs like that.

Installing this next to Farmer's Delight crashed the game at startup, sometimes — it depended on
which of the two loaded first. Both add signs to the same list and this mod was sealing it shut.

A pile of recipes were silently uncraftable. They pointed at shared convention tags that the loader
in question had never actually defined, and a missing ingredient tag doesn't error on older
versions, it just empties the ingredient and the recipe stops matching. Concrete and iron nuggets
were the worst of it. The mod owns those tags itself now, so the recipes work everywhere, and other
mods' equivalent items still count toward them.

Magnets did nothing to iron on a few Fabric builds. Same cause.

The primordial, hazmat, diving and gingerbread armour sets rendered as nothing at all from 1.20.5
onward. They were relying on a Forge hook that stopped existing.

On NeoForge 1.21.7 and up, the game stopped on a warning screen naming this mod every single launch,
and you had to click past it to reach the menu. Gone.

On Fabric 1.21.5 and up, the game crashed the first time anything walked into water.

51 faces across 13 block models were reaching outside their own texture, which means they've been
drawing whatever pixels happened to sit next to it on the sheet since 1.20.1. On 26.1 that stopped
being a cosmetic problem and started failing the model outright. Another 48 faces had no texture
assigned at all. Both fixed, geometry untouched.

Two sound effects were spelled one way where they were registered and another way in the sound
list, so they never played. The abyssal chasm has an ambient track now, which it always shipped and
never used.

A structure exclusion zone that had never once applied, and a spear model pointing at the wrong
file. Fixed where fixing it was clearly right, left alone and written down where it wasn't.

## Testing

All 58 files boot, client and server, on the Minecraft version and loader they're built for. Three
of them got a full in-world pass over RCON on a fixed seed: every biome located, every structure
placed, every block set, every mob summoned, every loot table rolled. Those three agree with each
other down to the coordinates.

Alex's Caves is AlexModGuy's work and this project claims none of it. LGPL-3.0, same as the
original.
