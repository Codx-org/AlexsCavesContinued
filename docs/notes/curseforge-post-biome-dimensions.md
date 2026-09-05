# CurseForge post: putting the cave biomes in other dimensions

Copy/paste, not a notes file. Post it as a project comment or a page section on both stores.
Written in the same plain voice as `curseforge-replies-1.0.1.md`.

---

**Putting the cave biomes in other dimensions**

Somebody asked whether the "dimensions" line in the biome config actually does anything. It does,
and it's the one setting in there that most people never touch, so here's how it works.

Open `config/alexscaves_biome_generation/`. There's one file per cave biome, six of them, and each
one ends with something like this:

```json
"dimensions": [
  "minecraft:overworld"
]
```

That's a plain list of dimension IDs. Add more and the biome becomes eligible in each of them:

```json
"dimensions": [
  "minecraft:overworld",
  "twilightforest:twilight_forest",
  "undergarden:undergarden"
]
```

The IDs have to be exact. No wildcards, no tags, no "everything except". Whatever `/execute in`
tab-completes to is the right spelling. Run `/acc reload` afterwards and `/acc biomes` to see what
loaded. Only chunks generated from that point on are affected, so go somewhere new to test.

One thing not to do: don't delete the "dimensions" line. If it's missing the mod treats the file as
an old-format one and quietly overwrites it with the defaults, and the only sign is a line in the
log you probably won't be reading.

**Two things that will make it look broken**

First, the dimension has to use a multi noise biome source. That's the vanilla overworld style of
worldgen where biomes are picked from temperature and humidity and so on. Most datapack dimensions
and anything built on TerraBlender qualifies. Dimensions with a single fixed biome, or ones where
the mod wrote its own biome placement in code, don't, and nothing I can do in a config file changes
that. If you're not sure, look at `generator.biome_source.type` in the dimension's JSON.

Second, and this is the one that gets everybody: **delete the "continentalness" and "depth" lines
when you point a biome at a non-overworld dimension.** Those numbers describe the overworld's
terrain noise. In the Nether, the End, and basically every dimension built from those presets,
Minecraft pins continentalness, erosion, depth and weirdness to a flat zero. So a file asking for
continentalness between 0.6 and 1.0 can never match, and the biome silently never generates. It
looks exactly like the dimensions line being ignored, and it isn't.

Every one of those range lines is optional. A file this short is completely valid:

```json
{
  "disabled_completely": false,
  "distance_from_spawn": 400,
  "alexscaves_rarity_offset": 0,
  "dimensions": ["minecraft:overworld", "undergarden:undergarden"]
}
```

Bear in mind that drops the overworld restrictions too, so the biome gets commoner at home as well.
If you want to keep the overworld tuned the way it ships, see the offset trick below.

**What "alexscaves_rarity_offset" actually is**

It's not a rarity number, it's a slot. The world is divided into a scattered grid of cells, each
cell rolls a number from 0 to 5, and a biome only generates in cells matching its offset. That's
why there are exactly six offsets for six biomes.

Two biomes sharing an offset normally means one of them wins and the other never appears. But if
their dimension lists don't overlap, only one of them can pass anyway, so sharing is safe and each
dimension gets a full share of the cells instead of a sixth. That's the trick for a modpack: give
your modded dimension its own set of two or three biomes at their own offsets, and leave the
overworld files alone.

Worth knowing: the cell grid comes from the world seed, and every dimension in a world shares that
seed. So a biome allowed in three dimensions lands on the same X/Z spots in all three. It's not
rolled fresh per dimension.

Also, "distance_from_spawn" is measured from x=0 z=0 of that dimension, not from your bed.

**One limitation, and one bug**

Right now the cave biomes will generate in another dimension, with the right blocks and the right
mobs, but Alex's Caves structures won't place there. Things like the gingerbread town, the forlorn
bridge and the abyssal ruins are still overworld only. That's a limitation on my end rather than
something you can configure around, and I'll fix it in the next patch.

And a bug inherited from the original mod: the "humidity" and "temperature" ranges are compared
against each other's values. If you write either of them by hand, swap them. Only the abyssal chasm
ships a temperature line by default, so this affects almost nobody, but it'll bite you if you start
tuning. I'm leaving it alone for now because fixing it would shift generation in worlds people are
already playing.

If you get a setup working, post it. I'd like to see what people point these at.
