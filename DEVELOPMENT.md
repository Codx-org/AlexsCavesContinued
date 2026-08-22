# DEVELOPMENT.md — Alex's Caves Continued

Multiversion continuation of **Alex's Caves** (upstream `2.0.2`, Forge-only, MC 1.20.1) onto
the same Stonecutter harness as the sibling **AlexsMobsContinued** repo. Everything about the
build harness that is not restated here is documented in
`../AlexsMobsContinued/docs/notes/build-harness.md`, which this tree's `build-logic/` was
copied from verbatim.

- Upstream: <https://github.com/AlexModGuy/AlexsCaves> (LGPL-3.0)
- Mod id `alexscaves`, java root `com.github.alexmodguy.alexscaves`, group
  `com.github.alexmodguy`, version `1.0.0`.
- The pre-Stonecutter Forge buildscript is preserved under `docs/upstream-build/`.

## Node map

The target is the **full 58-node matrix — the same set codxlib ships**, so every node of this
mod has a companion library build. AlexsMobsContinued stopped at 49; this tree goes further,
and the extra 9 are: `26.1` and `26.1.1` on all three loaders (AMC folded those into one
26.1.2 node with a widened range) plus **Fabric** on `1.20.2`, `1.20.3` and `1.20.5`.

58 is not 20 MC versions × 3 loaders. It is **18 Forge + 18 NeoForge + 22 Fabric**, because
the loaders differ in what upstream ever published:

| Absent node | Why |
|---|---|
| `1.20.2`, `1.20.3`, `1.20.5` on Forge **and** NeoForge | No usable upstream build. Forge's 1.20.3 userdev resolves but its `bootstrap-dev:2.0.0` is gone from the NeoForge maven; 1.20.2's Forge 48.0.0 was short-lived; neither loader has 1.20.5. **Fabric reaches all three** |
| `1.20.1-neoforge` | 1.20.1 NeoForge is the legacy Forge-fork toolchain, not modern moddev |
| `1.21.2-forge` | Forge published no 1.21.2 build |

Since `26.1`/`26.1.1`/`26.1.2` are now three real nodes, each declares an **exact** MC range.
Do not re-add the old `deps.minecraft-range = "[26.1, 26.1.3)"` widening — with three jars all
claiming 26.1, the store hands players an arbitrary one.

`settings.gradle.kts` holds the full map commented out; nodes are uncommented wave by wave.
`stonecutter.properties.toml` carries the complete pin table for all 58.

## Milestones

- **M0 — repo scaffold. DONE.** Stonecutter 0.9.2 + `build-logic` convention plugin +
  arch-loom/MDG/loom buildscripts in place; identity, manifests, mixin config and pack.mcmeta
  templated.
- **M0b — `1.20.1-forge` baseline builds. DONE.** `:1.20.1-forge:build` green.
- **M0c — baseline boots. DONE.** Dev server reaches `Done (2.2s)`; dev client reaches the
  title screen with all 53 upstream mixins applied and no injection failures.
- **M1 — vendor Citadel. DONE.** 93 classes + 13 mixins relocated into
  `com.github.alexmodguy.alexscaves.citadel`; the external dependency is gone from every
  buildscript and manifest. `:1.20.1-forge:build` green, dev server `Done (1.4s)` with zero
  advancement errors, dev client clean. Full write-up: **`docs/notes/citadel.md`** — read it
  before touching anything under `citadel/`.
- **M1b — wire codxlib. DONE.** Required dependency on all five buildscripts + all three
  generated manifests; update checker registered; the four loader-specific platform calls
  replaced. See **codxlib** below.
- **M2 — the Forge/NeoForge version walk. DONE.** All **36** Forge/NeoForge nodes, `1.20.1` → `26.2`,
  compile green with `scripts/verify_mixins.py` resolving 8818 injection points. Wave-by-wave
  post-mortems below, newest first.
- **M3 — the Fabric milestone. DONE.** All **22** Fabric nodes, `1.20.1` → `26.2`, so the matrix is
  **58/58**. Fabric is the only loader here that reaches all 22 MC versions. The port supplies Forge's
  *shapes* under the mod's own namespace rather than rewriting the mod to Fabric idioms, so ~700
  registration lines and ~60 hook call sites are byte-identical on all three loaders: a 115-file
  `fabric/**` package excluded from every non-Fabric compile, stand-in types under `fabric/forge/**`,
  **69** `!fab-*` replacement rules and 26 dispatcher mixins under `mixin/fabric/**`. Full write-up:
  **`docs/notes/fabric.md`** — read it before touching anything under `fabric/`.
- Then: publishing.

## codxlib

Alex's Caves Continued is a codxlib consumer, wired to the standard recipe (see the workspace
notes). Build-time pin `deps.codxlib` in `stonecutter.properties.toml`; each node
resolves `codx:codxlib:<ver>-<loader>+<mc>` from **mavenLocal**, so
`cd ../codxlib && python3 scripts/install_maven_local.py` after any codxlib change or this tree
stops resolving. Declared runtime floor is `[1.3,)` on Forge/NeoForge and `>=1.3.4` on Fabric
(1.3.3's Fabric jars demand fabricloader `0.19.3` and refuse to start).

What it is actually used for — deliberately a small surface, because Alex's Caves is a content
mod, not a HUD/config mod:

| Use | Where |
|---|---|
| Update checker + `/codxlib versions` + debug report | `UpdateChecker.register(modInfo())` in the `AlexsCaves` constructor |
| `CodxLib.isModLoaded` | `ACLoadedMods`, `CommonEvents#playerLoggedIn`, `citadel/compat/ModCompatBridge` |
| `CodxLib.configDir()` | `BiomeGenerationConfig#getConfigDirectory` |

`AlexsCaves.MODRINTH_SLUG` is **hardcoded** (`alexs-caves-continued`) — never read it from
`mod-metadata.properties`; every codx mod ships one under that name and `getResourceAsStream`
returns an arbitrary mod's copy.

The two `ForgeConfigSpec` configs (`alexscaves-general.toml`, `alexscaves-client.toml`) are
**not** migrated to `JsonConfig`. They are ~200 upstream options read all over the codebase, and
the Fabric milestone is where a loader-neutral config actually has to be solved — doing it now
would be a large diff with no payoff on Forge/NeoForge.

## What differs from Alex's Mobs (read this before assuming the AMC recipe applies)

1. **Alex's Caves has 66 mixins** (53 of its own — 28 common + 25 client — plus the 13 vendored
   from Citadel, in `src/main/resources/alexscaves.mixins.json`). Alex's Mobs had **zero**. Every
   MC bump must descriptor-check all 66 — a renamed-but-resignatured target is a hard crash with
   `defaultRequire: 1`. Port AMC's `scripts/verify_mixins.py` before the first version wave.
2. **Citadel is vendored, not depended on** — 93 relocated classes under
   `com.github.alexmodguy.alexscaves.citadel` (the surface AC used was roughly twice Alex's Mobs':
   45 distinct types vs 25). There is no `deps.citadel` anywhere. See `docs/notes/citadel.md`.
3. **`expand()` cannot be used on this mod's mixin config.** Gradle's `expand()` runs the file
   through Groovy's SimpleTemplateEngine, which treats every `$` as interpolation, and a nested
   mixin is addressed `Outer$Inner` — this mod has
   `client.SpriteResourceLoaderMixin$PalettedPermutationsAccessor`, so `expand()` dies with
   *"Missing property (PalettedPermutationsAccessor)"*. There is no escape that is also legal
   JSON. `ModPlatformPlugin.configureProcessResources` uses a plain `filter { }` string replace
   of `${java}` instead. Do not "simplify" it back to `expand`.
4. **No `refmap` key in the mixin config.** Upstream declared one; arch-loom remaps mixin
   annotations in place at `remapJar`, so a declared refmap is a dangling reference.
5. **`logoFile` comes from `mod.fabric.icon`** (`assets/alexscaves/icon.png`) — one property feeds
   both manifests. That is the *continuation's* icon, generated by `scripts/gen_icon.py`, which
   also writes the two store-facing sizes at the repo root; re-run it rather than editing any of
   the three PNGs. Upstream's `assets/alexscaves/textures/misc/mod_logo.png` still ships untouched
   (it is the script's input), it is simply no longer what the manifests point at.
6. **`accessTransformers` is deliberately empty in the generated `mods.toml`** — Forge auto-loads
   `META-INF/accesstransformer.cfg`, which is exactly where loom puts it. Naming it as well makes
   Forge read the file twice.

## Gotchas already hit

- ⚠️⚠️ **MC 26.1 added `ChunkGenerator#validate()`, and on a SINGLEPLAYER world it makes this mod's
  biomes crash chunk decoration.** The whole body is `this.featuresPerStep.get(); return;` — it does
  nothing but *force* the per-step feature index that `FeatureSorter` builds from the generator's
  current biome set — and the **client** calls it before the integrated server exists:
  `WorldOpenFlows#openWorldLoadLevelStem` loops over every `LevelStem` of the freshly-loaded
  `WorldStem` and validates its generator (offset 87) before `Minecraft#doWorldLoad`, and
  `WorldCreationContext#validate` does the same on world creation. This mod adds its six biomes from
  `ServerAboutToStartEvent`, so the index memoises **without** them and is never rebuilt; decoration
  then asks `stepFeatureData.indexMapping()` for an AC placed feature, gets the identity map's `-1`
  default (there is no guard) and dies in `applyBiomeDecoration` with `IndexOutOfBoundsException:
  Index -1 out of bounds for length N`, killing the chunk worker the instant a player reaches an AC
  biome — *"I look for a biome, TP to it and the game freezes"*. Fixed 2026-08-21 by
  `mixin/ChunkGeneratorValidateMixin`, a HEAD cancel of `validate()` gated `>=26.1`, which lets the
  index memoise lazily inside the first `applyBiomeDecoration` — on a chunk worker, long after the
  event. Three things to carry forward. **(1) The blast radius is a version boundary, not a loader
  one**: `validate()` is absent on every cached jar 1.20.1 → 1.21.11 and byte-identical on 26.1,
  26.1.1, 26.1.2 and 26.2, so it is 12 nodes. **(2) Forge is NOT shielded by its own patch.** Forge
  retypes the field to `ClearableLazy` and adds `public void refreshFeaturesPerStep()`, which reads
  like a repair path — but `grep -rl refreshFeaturesPerStep` over the patched jar matches only
  `ChunkGenerator.class` itself, i.e. **nothing ever calls it**, and Forge's `validate()` forces the
  lazy exactly as vanilla's does. *A loader shipping the API to fix a problem is not the same as the
  loader fixing it.* **(3) No dedicated server ever calls `validate()`** — neither `MinecraftServer`
  nor `ServerLevel` does — which is precisely why the whole RCON in-world battery and all 58 green
  `runServer` boots said nothing about it. The only thing given up by cancelling is vanilla's early
  feature-order-cycle diagnosis at the load screen; the same throw still arrives at the first
  decorated chunk. Verified by re-opening the very world that crashed twice, with
  `--quickPlaySingleplayer` so the client loads straight to the player's position inside the biome:
  chunks generate, no crash report.
- ⚠️⚠️ **1.20.5 turned `LocationPredicate`'s `structure`/`biome` and the advancement
  `BlockPredicate`'s `tag` into holder sets, and because every field of both records is an
  `optionalFieldOf` the old keys were DROPPED IN SILENCE — leaving an empty predicate that matches
  everywhere.** `minecraft:location` polls the player, so on all 51 nodes ≥1.20.5 the four
  structure advancements (`root`, `discover_abyssal_ruins`, `gingerbread_town`, `licowitch_tower`)
  and the six `discover_*` biome ones granted **the instant a world was entered** — and `root`
  granting pops the whole tab, which is exactly what the user reported. `walk_on_rock_candy` is the
  same bug on the block half (`stepping_on.block.tag`): it granted on any block at all. Read the key
  sets out of the Mojmap bytecode, node by node, because the boundary is **not** guessable —
  1.20.2, 1.20.3 and 1.20.4 all still spell them singular, so this is 1.20.5 exactly:

  | class | ≤1.20.4 | ≥1.20.5 |
  |---|---|---|
  | `LocationPredicate` | `biome`, `structure` | `biomes`, `structures` |
  | `advancements…BlockPredicate` | `blocks`, `tag` | `blocks` only |

  Fixed 2026-08-21 by `DataPackMigration.migrateAdvancementPredicatesTo1205`, gated `>=1.20.5`,
  expected count **11**. A bare id string is a legal single-element holder set, so only the *key*
  moves; `#tag` is how a holder set spells a tag. It has to be a migration pass rather than a source
  fix — the standing preference in this file — because the correct spelling genuinely differs per
  band, which is the one case that preference does not cover.
  Two things generalise past this instance. **(1) A predicate built entirely from `optionalFieldOf`
  fails OPEN.** There is no log line, no failed load and no gate that can see it: the advancement
  parses, loads and fires, it just fires on nothing in particular. The same shape already cost
  AlexsMobsContinued its "Gone Bananas" advancement (report #31). **(2) The pass has to walk the
  document, not `conditions`' top-level keys** — `player` is an *array of loot conditions* in every
  affected file, so the host to identify is a `"condition": "minecraft:entity_properties"` object,
  which is why this reuses `rewriteEntityPredicateHosts`' shape rather than
  `migrateItemPredicateFields`'. Grep `src/main/resources` for `"structure"`, `"biome"` and `"tag"`
  before assuming the blast radius: every non-advancement hit here was a false alarm
  (`loot_modifiers` = the mod's *own* `alexscaves:cave_tablet` codec field, `structure_set` =
  vanilla's own unchanged entry field, `set_nbt`'s tag and the recipe ingredient tags = handled by
  existing passes), and `location_check` appears nowhere in the tree.
- ⚠️ **1.21.2 stopped deriving a BlockItem's name from its block, and every one of this mod's
  ~360 block items was called `item.alexscaves.<path>` because of it.** `BlockItem` used to
  override `getDescriptionId()` to return `getBlock().getDescriptionId()`; from 1.21.2 the name
  comes off `Item.Properties` like any other item's and a BlockItem must ask for the block prefix
  with `Item.Properties#useBlockDescriptionPrefix()`. `ACBlockRegistry` built all 11 of its
  BlockItem variants from a bare `new Item.Properties()`, so on every node 1.21.2 → 26.2 the
  tooltips were raw lang keys while the shipped keys are all `block.alexscaves.*`. **Models and
  icons come from a different `Properties` field and were fine**, which is precisely why a boot
  test could not see it — you have to hover an item. Fixed 2026-08-21 with a
  `blockItemProperties()` helper gated `>=1.21.2`, verified by compiling `1.21.2-fabric` (the
  boundary node) and `1.21.5-fabric` alongside the active tree. Read it out of javap:
  `net.minecraft.world.item.BlockItem` on 26.2 declares **nothing but a constructor**.

- ⚠️ **`Entity#hashCode` goes through `Entity#getId`, and `getId` THROWS on an entity that was
  never added to a level.** So a `PartEntity` — built in its parent's constructor, never
  registered, never given an id — cannot be a `HashMap`/`HashSet` key, and the failure is
  `IllegalStateException: Tried to access entity ID before ID assignment` thrown from
  **rendering**, not from the collection. `citadel/client/render/LightningRender` kept its bolt
  owners in an `Object2ObjectOpenHashMap` and `MagnetronRenderer` passes a `MagnetronPartEntity`
  as the owner, so the client died the first frame a Magnetron was on screen
  (`LightningRender.update` → `Map.computeIfAbsent` → `Entity.hashCode`). Fixed 2026-08-21 with
  an `IdentityHashMap`, which is what "bolt owner" meant anyway and is behaviour-identical on
  older versions where `Entity` did not override `hashCode` at all. `QuarrySmasherRenderer`'s
  four `update(1..4, …)` owners became interned `Object` constants at the same time rather than
  leaning on the `Integer` cache to make identity work. The mod has **five** `PartEntity`
  subclasses (magnetron, tremorzilla, hullbreaker, sauropod, `ACMultipartEntity`) — the other
  entity-keyed collections here are all typed `LivingEntity`, which no part entity is, so they
  are safe. **Anything typed `Entity` or `Object` that hashes is not.**

- ⚠️ **…and the same 26.2 throw has a second, unrelated source: a DISPLAY entity, read directly
  rather than through a hash.** `getId()` is harmless on every node 1.20.1 → 26.1.2 (javap'd: it
  simply returns the field); **26.2 alone** made it throw while the id is still 0, and 26.2's
  `ItemModelResolver#updateForLiving` reads it *unconditionally* — the id plus
  `ItemDisplayContext.ordinal()` is the seed that picks an item-model variant — so
  `LivingEntityRenderer#extractRenderState` on any entity that was never added to a level dies.
  This mod builds exactly that on purpose in three places: the amber monolith's encased mob, the
  hologram projector's projection, and the cave book's `EntityWidget` / nocked arrow. The amber
  one is the loud one, because `AmberMonolithBlockRenderer.renderEntityInAmber` wraps its body in
  `catch (Throwable) → ReportedException("Rendering entity in world")` — an instant hard crash the
  first frame a monolith is on screen, which is what the user hit on `26.2-fabric`. Fixed
  2026-08-21 with `ACCompat.markDisplayEntity(T)`, a `>=26.2`-gated `setId()` of a **negative,
  decrementing** id: every real, level-assigned id is positive, so a negative one can collide with
  nothing, and since the id is only a model-variant seed any non-zero value is behaviour-neutral.
  All three sites funnel through that helper. Two lessons. **(1) Read `getId`'s bytecode, not its
  reputation** — the throw is one version wide and the neighbouring nodes give a false all-clear.
  **(2) Grep for the *direct* read as well as for the hashing one**: the `PartEntity` bullet above
  fixed every `Entity`-keyed collection in the tree and this bug was still live, because nothing
  here hashed a display entity — vanilla just read its id.

- ⚠️ **A duplicate `add(output, …)` in a creative tab is a client crash, and no boot test finds it.**
  `ACCreativeTabRegistry` listed `GALENA_BRICKS` twice in the Magnetic Caves tab (once before
  `GALENA_WALL`, once in its right place). Vanilla's `CreativeModeTab$ItemDisplayBuilder.accept`
  throws `IllegalStateException: Accidentally adding the same item stack twice
  [item.alexscaves.galena_bricks]`, and `CreativeModeTabs.buildAllTabContents` runs the first time
  a player **opens the creative inventory** — so the client boots to the title screen, loads a
  world, and dies on the E key. The 56-node dev-client shakedown could never have caught it because
  it only ever checked that the title screen appeared. The duplicate predates the fork and every
  node from **1.19.3 up** shipped it. Fixed 2026-08-21 by deleting the stray line; a Python scan of
  all 7 tabs confirmed it was the only one. **Scan the tab lists for repeated ids after any edit** —
  and open creative once in any client test.

- ⚠️ **Never assign an IMMUTABLE collection to an access-widened vanilla field that other mods also
  extend.** `ACBlockEntityRegistry.expandVanillaDefinitions` added the pewen/thornwood signs by
  writing `ImmutableSet.Builder.build()` into `BlockEntityType.validBlocks` (widened `accessible` +
  `mutable` in `alexscaves.accesswidener`). Farmer's Delight adds its canvas signs to the same
  vanilla `SIGN` type through Fabric API's `BlockEntityType#addValidBlock`, which writes straight
  into that field — so it hit `ImmutableCollection.add` → `UnsupportedOperationException`, its
  entrypoint threw, and the game died at `Minecraft.<init>` with a crash report naming only FD.
  **Whoever initialises second loses**, and Fabric orders mod initialisers by discovery, so it
  reproduced on some launches and not others — the same two jars passed a full probe run at 23:11
  on 19 Aug and crashed at 23:10, the only difference being the ACC jar's filename. Fixed
  2026-08-21 by rebuilding both sets as a `LinkedHashSet`: same contents, same iteration order,
  still open for whoever comes third. Found while testing **AlexsCavesContinuedDelight**, which
  requires both mods and so would have shipped the coin toss to every player.
  ✅ All 58 nodes were rebuilt with these fixes on 2026-08-21 (`MOD_IS_RELEASE=true`), so the shipped
  `1.0.0` jars carry them everywhere.
- **`"Loaded 7 recipes"` on a 1.20.1 dev server is NORMAL, not a broken data pack.** AMC's
  known-good 1.20.1-forge node prints the same line in every one of its archived logs. 1.20.1's
  initial `WorldLoader` pass loads a reduced pack set; the advancement count on the same line
  pair (1417) is the one that proves the vanilla + mod data actually resolved. Don't chase it.
- **A type-use `@NotNull` on an array component does not compile here.**
  `GummyColorLootFunction.deserialize` had `LootItemCondition @NotNull [] conditionsIn`; the
  `org.jetbrains.annotations` version on this classpath has no `TYPE_USE` target, so it must be
  written as a declaration annotation (`@NotNull LootItemCondition[]`).
- **The upstream `src/main/resources/META-INF/mods.toml` had to go** (moved to
  `docs/upstream-build/`). `mod-platform` generates its own into
  `build/generated/modManifest/` and adds that as a resource dir — two files at one path.
- **Dropping a bundled dependency means grepping `resources/` for its namespace, not just the Java
  imports.** 30 of AC's advancement JSONs used `citadel:icon_item`/`citadel:effect_item` as their
  display icon — invisible to every Java grep, and after Citadel was vendored the dev server logged
  112 `Couldn't load advancement` errors cascading from *"Expected item to be an item, was unknown
  string 'citadel:icon_item'"*. Both items now exist as `alexscaves:` items. Details in
  `docs/notes/citadel.md`.
- **An import-closure script misses same-package references.** The Citadel closure missed
  `ChunkCache` and `IPassabilityNavigator` because `AbstractPathJob` uses them unqualified from the
  same package. After computing a closure, diff each vendored package against the upstream one.
- **`scripts/verify_mixins.py` is the version-walk tool — run it before booting anything.** It javaps
  every injection point on every uncommented node out of the *generated* Stonecutter tree (so it sees
  post-gate sources) and against the **loader-patched** jar first, vanilla only as a fallback — Forge
  and NeoForge patch vanilla classes in place, so reading plain Mojmap invents missing members
  (`FoodData.eat(Item, ItemStack, LivingEntity)` and `MapDecoration.render(int)` both exist only in
  the patched jar). Three rules it encodes that each cost a boot:
  - **`@Shadow`/`@Accessor`/`@Invoker` resolve against the target class ALONE**, no hierarchy —
    Mixin throws *"was not located in the target class"* for an inherited member. `@At(target=…)` is
    the opposite and must be checked through the supers, since the bytecode names the static type at
    the call site. The minecart damage accessors moving onto the new `VehicleEntity` in 1.20.2 is the
    canonical case: it compiles (Java inherits them), it just cannot be shadowed.
  - **A `@Shadow` method or `@Invoker` is matched by name AND descriptor**, so its declared parameter
    list is part of the assertion. `MapDecoration.Type`'s constructor kept its name and gained a
    serialised name + an `isExplorationMapElement` flag in 1.20.2; only the descriptor says so.
  - Stonecutter disables a **single-line** `//?` branch with a `//` prefix, not `/* … */`, so a
    comment stripper that only eats block comments reads inactive members as live.
  - **Run `processResources` on a new node before believing the checker.** It reads the mixin config
    from `build/resources/main/` and falls back to `build/generated/stonecutter/main/resources/` —
    the **pre-pruning** copy, which still lists every class `DataPackMigration.pruneMixinEntries`
    takes out. A node that has only been `compileJava`'d therefore reports phantom misses for
    mixins that are not in its build at all: the first 1.21.10 run showed 4 problems each
    (`MapDecorationTypeMixin`, `MapRendererMapInstanceMixin`, `LevelRendererSkyMixin`) and looked
    exactly like a 1.21.10 API break. **The tell is the count going UP** — 261 where 1.21.9 has
    249/246 — since a stale config can only add entries. After `processResources` both nodes report
    249/246, i.e. not one target moved.
  - ⚠️ **The ACTIVE node has no live generated tree, so never diagnose a gate from it.**
    `stonecutter.gradle.kts` sets `stonecutter active "1.20.1-forge"`, and the active node compiles
    from `src/` directly — its `versions/1.20.1-forge/build/generated/stonecutter/` is whatever was
    last written there and is never refreshed (`:1.20.1-forge:compileJava` comes back `UP-TO-DATE`
    and touches nothing). A newly-added gate is therefore *absent* from that tree while being
    perfectly correct in `src/`, which reads exactly like a rule that failed to register. Check the
    gate on any **inactive** node instead.
  - ⚠️ **A bare `grep` of a generated tree cannot tell an active arm from a disabled one** — an arm
    that is gated off survives as commented text (`/*if (…) {` … `*///?}`), so the line you are
    looking for matches on the nodes where it is *switched off*. Always read it with context
    (`grep -B2` / `sed -n`) and look for the `/*` before it.
- **1.20.4-forge has no underground-cabin map marker.** `MapDecoration.render(int)` is a loader
  patch, not vanilla; Forge dropped it when `MapDecoration` became a record in 1.20.2 while NeoForge
  kept it, so `MapDecorationMixin#ac_render` is gated `<1.20.2 || (neoforge && <1.20.5)` and the
  marker falls back to the vanilla icon on that one node. There is no vanilla-portable substitute —
  `MapRenderer$MapInstance.draw` short-circuits `renderOnFrame()` outside a frame and a `getImage()`
  redirect cannot skip the vanilla draw. 1.20.5 replaces the whole `Type` enum with a
  `MapDecorationType` registry, which is where this gets a native implementation.
- **NeoForge's EventBus 7 rejects a listener whose SUPERTYPE declares `@SubscribeEvent`** —
  *"Only the listener object can have @SubscribeEvent methods"*, thrown from `EventBus.register`.
  It also rejects registering an object with **no** `@SubscribeEvent` methods at all. Forge's older
  bus tolerates both, so this is a NeoForge-only failure — and the supertype one is **client-only**,
  because on a dedicated server the proxy is a plain `CitadelProxy` with no subclass in play, so a
  green `runServer` says nothing about it. Handlers therefore live in standalone listener classes
  (`CitadelEvents` / `CitadelClientEvents`) that the proxy merely *registers* via
  `registerEventHandlers()`; the proxy hierarchy itself carries none. Same reason the dead
  `MinecraftForge.EVENT_BUS.register(this)` came out of the `AlexsCaves` constructor.
- **The criteria list became a real frozen registry in 1.20.2.** `CriteriaTriggers.register` from
  common setup throws *"Registry is already frozen"* (`minecraft:trigger_type`).
  `ACAdvancementTriggerRegistry` keeps its 23 public constants — so every call site is untouched —
  and gates *how* they are registered: a `<1.20.2` loop through `CriteriaTriggers.register`, or a
  `DeferredRegister<CriterionTrigger<?>>` over `Registries.TRIGGER_TYPE` that hands the registry
  event the very same instances.
- **A NeoForge node has to re-point every Forge-namespaced id that is not a tag**, and an unknown
  one is **fatal, not skipped**: `underground_cabin.json`'s `forge:and`/`forge:not` HolderSet types
  killed the whole `RegistryDataLoader` pass with *"Unknown registry key in
  ResourceKey[minecraft:root / neoforge:holder_set_type]"* and the server never started.
  `DataPackMigration.migrateNeoForge` now rewrites a whitelist — `loot_table_id`, `and`, `or`, `not`
  — through the boundary-aware `forgeNamespace` regex. It stays a whitelist because everything else
  spelled `forge:` in this tree (41 of the 44 distinct ids) is a convention tag, and those stay in
  `forge:` until 1.20.5 moves the whole namespace to `c:`.
- **A dev server writes `eula=false` on first run and stops.** Each node has its own
  `versions/<node>/run/`, so every new node needs its `run/eula.txt` flipped once before
  `runServer` gets anywhere. Also: `runServer` binds 25565, so a stray server from an abandoned run
  makes the next node die with *"FAILED TO BIND TO PORT"* → `IllegalStateException: Failed to
  initialize server` — check `ss -lntp | grep 25565` before reading that as a port regression.
- **Don't `pkill -f` a pattern that appears in your own command line.** `pkill -f "…runClient"`
  matches the shell running it and kills the launcher before Gradle ever starts — the symptom is a
  task that "fails" with no log file at all.
- **`runServer`'s exit code is backwards — never read it as pass/fail.** The Gradle task exits **0
  when the server crashes** (FML catches the throwable, logs it and returns cleanly) and **124 when
  the boot succeeded**, because a healthy server never returns and `timeout` kills it. The only
  reliable verdict is the log: `Done ([0-9.]+s)` present, and `Failed to parse` / `Couldn't parse` /
  `Unknown registry` / `Couldn't load advancement` absent. Every node in the walk is checked that way.
- **FML's `enumExtensions` key must live inside `[[mods]]`, not at the top of the manifest.**
  `LoadingModList` reads it off the *mod's* config section
  (`IModInfo.getConfig().getConfigElement("enumExtensions")`), so a bare top-level key parses as
  valid TOML and is then silently never seen — the two `ACMobCategories` constants just don't exist.
  `Loader.ForgeLike.generateManifest` splices the line in after `[[mods]]` for that reason.
- **Deferred registration only defers what it is handed lazily.** `ACBlockRegistry` used to pass
  `ACFoods.X` *by value* into `registerBlockAndItemEdible`, which runs `ACFoods.<clinit>` while the
  static fields of `ACBlockRegistry` initialise — i.e. inside `new AlexsCaves()`. Harmless until
  1.20.5, where `FoodProperties.Builder#effect` takes the `MobEffectInstance` eagerly (the
  `!mc205-food-effect` rule drops the lambda), so building `ACFoods` calls
  `ACEffectRegistry.RAGE.get()` and the mod dies with *"Trying to access unbound value:
  alexscaves:rage"*. The helper now takes a `Supplier<FoodProperties>` and all 75 call sites pass
  `() -> ACFoods.X`, moving the work to item-registration time — safe because `BuiltInRegistries`
  declares `MOB_EFFECT` before `BLOCK` and `ITEM`.
- **1.20.5 flipped the JSON shape of every int provider** (39 of this mod's placed features: 38
  `count` placements + one `weighted_list` entry), with errors like *"Not a number:
  {"type":"minecraft:uniform","value":{…}}"*. It is a DFU detail, not a format rename: a
  `KeyDispatchCodec` inlines the dispatched codec's fields only when the element codec is a
  `MapCodecCodec`, and `.validate(…)`/`.comapFlatMap(…)` on a `Codec` erases MapCodec-ness. Up to
  1.20.4 `UniformInt.CODEC` was `RecordCodecBuilder.create(…).validate(…)` → fields nested under
  `value`; 1.20.5 rebuilt it as `RecordCodecBuilder.mapCodec(…).validate(…)` → fields inline.
  `DataPackMigration.flattenIntProvidersTo1205` unwraps them, gated `>=1.20.5`.
- **1.20.5 also made `optionalFieldOf` strict, which turns latent upstream bugs into boot failures.**
  Both of this mod's structure sets carry an `exclusion_zone.other_set` naming a *tag*
  (`#alexscaves:licowitch_tower_generates_far_from`), but that field is
  `RegistryFileCodec.create(…, allowInline = false)` — one plain id, never a tag, unchanged since
  1.20.1. Before 1.20.5 the decode error was swallowed by the lenient optional field, so the zone
  has **never** applied on any version; from 1.20.5 it is a fatal *"Inline definitions not allowed
  here"*. `dropUnreadableExclusionZones` therefore removes it **unconditionally** — that is a no-op
  behaviourally, and keeping the field on old nodes would only preserve a silent error.
- **Data-pack ids are prefix-optional, so migration matching must normalise.** AC writes
  `"function": "set_nbt"`, not `"minecraft:set_nbt"`; `migrateLootFunction`'s exact-string dispatch
  skipped all 14 affected tables and 1.20.5+ died with *"Unknown registry key … loot_function_type:
  minecraft:set_nbt"*. Every dispatch key in `DataPackMigration` now goes through `idOf`.
- **1.20.3 renamed the block `minecraft:grass` to `minecraft:short_grass`** (`grass_block` and
  `grass_path` are untouched, which is why `renameShortGrassTo1203` replaces the quoted whole token).
- **A mod-owned interface that mirrors a loader patch must be gated to EVERY band of that patch, and
  the replacement rules which rewrite the implementations cannot reach the interface.** The Fabric
  milestone's `ACUpdatePacketReceiver` declares `BlockEntity#onDataPacket` so the nine block entities'
  `@Override`s are legal on a loader that has no such patch. Declaring it unconditionally in the
  1.20.1 shape compiled on exactly one band and broke **every Forge/NeoForge node ≥1.20.5** with
  *"X is not abstract and does not override abstract method onDataPacket(Connection,
  ClientboundBlockEntityDataPacket)"* × 9 classes. The patch has **four** signatures, read out of each
  loader's own universal jar: `<1.20.5` `(Connection, ClientboundBlockEntityDataPacket)`;
  `>=1.20.5 && <1.21.6` the same plus a trailing `HolderLookup.Provider`; and from 1.21.6 the packet
  becomes a `ValueInput` — where **the two loaders disagree**, NeoForge taking `(Connection,
  ValueInput)` (the lookup is read off the input) and Forge keeping its provider for
  `(Connection, ValueInput, Provider)`. Fabric ≥1.21.6 is a fifth case that is *not* a stopgap: both
  `!mc216-be-datapacket-*` rules are loader-scoped, so nothing rewrites the implementations there and
  the original two-argument form stays correct. The trap that made this silent for a whole session:
  the three `replacements.string` rules are anchored on `public void onDataPacket(… ) {` — a `public`
  modifier and a trailing brace — so they rewrite the nine **declarations** and slide straight past an
  interface method, which has neither. **Whenever a rule rewrites a signature, ask what else declares
  that signature.**
- **`@Redirect` is EXCLUSIVE, and on Fabric that makes it a crash rather than a lost feature.** Two
  mods redirecting one instruction with equal priority is not a merge: Mixin picks the earlier
  applicator, logs *"@Redirect conflict. Skipping fabric-item-api-v1.mixins.json:LivingEntityMixin …
  already redirected by alexscaves.mixins.json:fabric.LivingEntityFoodMixin"*, and then **the loser's
  own `require` throws** — `InjectionError: Critical injection failure: Redirector
  getStackAwareFoodComponent … (0/1) succeeded. Scanned 0 target(s)`, out of `Bootstrap.bootStrap`,
  before anything else runs. Winning the tie is therefore not a fix; whoever loses still kills the
  game. AC's three redirects of `Item#getFoodProperties()` (`mixin.fabric.LivingEntityFoodMixin` ×2
  and `FoodDataMixin`'s `fabric && <1.20.5` arm) collide with fabric-item-api-v1's stack-aware food on
  exactly that instruction, so all three are **`@ModifyExpressionValue`** now — MixinExtras composes,
  and it ships inside Fabric Loader (0.19.3 carries 0.5.4), so it needs no dependency. ⚠️ **The
  handler must return `original` for anything that is not the mod's own item**, or the mod silently
  eats every other mod's stack-aware food; the `instanceof ACFoodPropertiesItem` guard is what makes
  the two coexist rather than one merely winning. Note 1.20.1/1.20.2/1.20.3-fabric booted green on the
  old `@Redirect` only because their pinned fabric-api predates the hook — **a green node whose green
  depends on another mod's build is not evidence**, so the fix covers the whole `<1.20.5` band. Inside
  a Stonecutter arm the annotation has to be spelled
  `@com.llamalad7.mixinextras.injector.ModifyExpressionValue`; `verify_mixins.py` handles the
  qualified prefix.
- **Fabric builds a block's shape cache DURING registration from 1.21.2, so `getShape` runs while the
  mod's own `DeferredRegister` is still flushing.** `fabric-registry-sync-v0` mixes into
  `MappedRegistry#register` and runs `Blocks.initShapeCache`'s lambda on the block just registered →
  `BlockStateBase$Cache.<init>` → `getCollisionShape` → the mod's `getShape(state,
  EmptyBlockGetter.INSTANCE, ORIGIN, empty())`. Vanilla builds that cache once from `Blocks`' own
  class initialiser, long after every mod block exists, which is why Forge and NeoForge never see it.
  `NuclearFurnaceComponentBlock#getShape` asks the level what its neighbour is, and the bare
  `ACBlockRegistry.NUCLEAR_FURNACE.get()` threw *"Used nuclear_furnace before its registry was
  flushed"* out of `AlexsCaves.<init>` — **all ten Fabric nodes ≥1.21.2, none of the other 48**. The
  fix is an `isAir()` short-circuit before the comparison: every state `EmptyBlockGetter` answers with
  is air, so the supplier is untouched during the cache build and nothing changes at runtime (a
  nuclear furnace is never air). General form: **on Fabric, a block's `getShape`/`getCollisionShape`
  may run before any other block is registered — it must not dereference a registry supplier**.
- **fabric-api ≥ the 1.21.5 build makes `EntityDataSerializers.registerSerializer` a hard error, and
  the class it points you at is RENAMED halfway up the range — so this is a THREE-arm gate.**
  fabric-object-builder-api-v1 mixes a refusal into vanilla's method — *"Tried to register tracked
  data handler … using TrackedDataHandlerRegistry.register. This is not allowed as it can lead to
  desynchronization issues"* — thrown out of `onInitialize`, i.e. a boot failure, because vanilla's
  incremental network ids depend on load order and Fabric wants an id-keyed registration. That is why
  this tree's `fabric/registries/DeferredRegister` sink is a `BiConsumer<ResourceLocation, T>`: the
  unregistered flush has to pass the id it already knows. Enumerating the nested object-builder jar of
  all 22 pinned bundles gives the exact boundaries: **no class and no refusal** below 1.21.5 (1.21.4's
  `0.119.4`, object-builder `18.0.14`); **`FabricTrackedDataRegistry`** — a Yarn-era name kept even in
  the Mojmap-facing API — from 1.21.5's `0.128.2` (`21.1.2`) through 1.21.11's `0.141.6` (`21.1.40`);
  **`FabricEntityDataRegistry`** from 26.1's `0.145.1` (`23.0.13`). Both spell `register(Identifier,
  EntityDataSerializer<?>)`, so only the owner moves. ⚠⚠ **This gate said `>=26.1` for an entire
  milestone** — the *rename* was found at 26.1 and mistaken for the *arrival* — and every Fabric node
  from 1.21.5 up died at `AlexsCavesFabric.onInitialize` the first time one was booted, seven nodes in
  a row. Neither boundary is inferable from a vanilla change sitting nearby (the `Identifier` rename at
  1.21.11 is between the two and means nothing): **it tracks the fabric-api BUILD**. `unzip -l` the
  bundle and javap the module — for every version in the range, not just the one you are on.
- **On Fabric, `FMLCommonSetupEvent` runs from `onInitialize`, which is inside the
  components-not-bound window from 26.1.** An `IBrewingRecipe` holds finished `ItemStack`s, and from
  26.1 `ItemStack.<init>` reads `Holder$Reference#components()` — unbound until
  `ReloadableServerResources#updateComponentsAndStaticRegistryTags` runs at the first datapack reload
  — so `ACEffectRegistry#setup` died at `PotionContents.createItemStack` on all four Fabric 26.x
  nodes. Forge and NeoForge escape it *by accident of the gate*: from 1.20.5 their brewing comes from
  a loader event that fires after the reload, so their arm of `setup` is empty. `BrewingRecipeRegistry
  .deferRecipes(Runnable)` now holds the filler and the first `getRecipes()` runs it, which is
  necessarily after a world exists. **This is the third instance of the same family** (`ACFoods` at
  1.20.5, `LicowitchEntity`'s static stack and `RecipeCaveMap` at 26.2) and the rule generalises:
  **anything that builds an `ItemStack` must be reachable only from a lambda, and on Fabric "mod
  init" is much earlier than the loaders' equivalent phase.**
- **⚠️ `Couldn't load tag` was NOT in the boot-log verdict regex, and it hid a real bug on eight
  nodes.** A tag that references a missing tag fails **whole** — every entry in it is lost, and so is
  every tag that referenced *it* — while the server still reaches `Done`, so the four markers this
  file lists (`Failed to parse`, `Couldn't parse`, `Unknown registry`, `Couldn't load advancement`)
  all stay absent. Add `Couldn't load tag` and `Missing tag` to any harness. What it was hiding:
  **fabric-api's convention-tags module ships a different subset on every build**, so
  `alexscaves:ferromagnetic_items` failed for `c:ingots/iron` on 1.20.1-fabric, for `c:nuggets/iron`
  on 1.20.5/1.20.6/1.21-fabric and for `c:ores/iron` on 1.21.2-fabric — three different missing tags
  across four fabric-api pins, each taking the whole tag and
  `alexscaves:galena_gauntlet_crystallization_items` with it, i.e. **magnets did nothing to iron on
  those nodes**. `c:gravels` does not exist on *any* Fabric build (the metal-swarf recipe, fatal from
  1.21.2 and silently empty below it). The fix is the shape the `alexscaves:concrete` tag already
  established, applied twice: **name the vanilla members outright and make every loader-supplied
  convention reference `{"id": …, "required": false}`**, so the loaders that define them still bring
  other mods' iron along. Do not audit this by reading fabric-api's source — enumerate
  `data/c/tags/**` out of the **pinned** `fabric-convention-tags-v2` jar for the node in question.
- **⚠️ Every `c:`/`forge:` convention tag this mod's recipes reference was a silent hole on Fabric, and
  the audit that finds them has to union BOTH convention-tags modules.** A fabric-api bundle nests
  `fabric-convention-tags-v1` **and** `-v2` from 1.20.5 up, and **only v1** below it — and v1's naming
  is a flat plural (`c:iron_ingots`, `c:black_dyes`, `c:wooden_barrels`, `c:diamonds`,
  `c:glass_blocks`), nothing like the v2 path form (`c:ingots/iron`) the upstream recipes were written
  against. Filtering an audit to either module alone gives a wrong answer *in either direction*: v2-only
  said 1.21.2-fabric was missing one tag when it was missing none, v1-only said 27 were missing on
  nodes that are fine. Union them. What the union found: **27 distinct tags** missing on the four
  oldest Fabric nodes, 3 on 1.20.5/1.20.6, 2 on 1.21 — taking the tag-backed part of the crafting tree
  with them. ⚠️ **This bullet used to say "90 of this mod's 90 recipes … the entire crafting tree
  gone", and both halves of that are wrong.** Recounted 2026-08-19: **467** recipe JSONs ship (301
  shaped, 71 stonecutting, 41 shapeless, 52 cooking, 1 trim, 1 `alexscaves:cave_map`), **107** carry a
  tag ingredient, **104** name a mod-owned tag, and **34** tag files fold a convention tag in as
  `"required": false`. 90 was neither the total nor the affected count; don't quote it, and don't read
  "the whole tree" into a break that reaches at most 104 of 467.
  **And it is invisible below 1.21.2**, where a missing ingredient tag is a
  silently EMPTY ingredient rather than a fatal one — no log line, a green `Done`, and an uncraftable
  recipe. The fix is the shape `alexscaves:concrete` and `alexscaves:gravel` already established,
  applied 27 more times: **the mod owns the tag**, naming the vanilla member outright and folding in
  both convention spellings as `{"id": …, "required": false}` so other mods' equivalents still count
  wherever a loader defines them. One file each, no gates, correct on all 58 nodes, and it repairs the
  released-version behaviour retroactively. Prefer this over a `DataPackMigration` rename rule whenever
  the mod is the only consumer — a rename can only ever fix the band you aim it at.
- **`runServer` binds a port, and the neighbouring repos' dev servers are on 25565.** An
  AlexsMobsContinued dev server left running is indistinguishable from a regression here — the
  symptom is *"FAILED TO BIND TO PORT!"*. Every AC node's `versions/<node>/run/server.properties` is
  pinned to **25599** (and `online-mode=false`) so the two trees can't collide.
- **This shell applies zsh history modifiers inside variable expansions.** `./gradlew :$n:runServer`
  with `n=1.20.4-forge` expands to `:1.20unServer` — `$n:r` is "remove extension" — and Gradle
  reports the unrelated *"task '1.20unServer' not found"*. Always write `":${n}:runServer"`.
- **Forge's bundled Mixin refuses `@Inject` into a constructor anywhere but RETURN/TAIL; NeoForge's
  allows it.** Citadel's `EntityMixin#citadel_registerData` used to be `@Inject` + `@Local` at the
  `SynchedEntityData$Builder.build()` call, because 1.20.5 made the data map immutable once built and
  TAIL is too late to `define` anything. That booted fine on 1.21-neoforge and killed **every Forge
  node from 1.20.6 up** with *"@At("INVOKE") selector Found @Inject targetting a constructor"*. A
  `@Redirect` of the same `build()` call carries no such restriction once the node is past the
  delegate `super()` (verified with `javap -c`: the `build()` invoke is at offset 384, the delegate at
  offset 3), so the `>=1.20.5` arm is a `@Redirect` now and works on all three loaders. **A green
  NeoForge boot says nothing about Forge for constructor injection.**
- **`Player#eat`'s `FoodData` call is a LOADER PATCH that diverges between Forge and NeoForge on
  1.20.5–1.20.6.** NeoForge restored an eater-aware `FoodData#eat(ItemStack, LivingEntity)` and calls
  that; Forge (and vanilla, so every Fabric node) calls `FoodData#eat(ItemStack)`. A `@Redirect`
  matches on descriptor, so one spelling gives *"Redirector ac_eat … failed injection check, (0/1)
  succeeded"* on the other loader. `PlayerMixin` therefore has **one arm per loader** for that window
  — `//? if neoforge && >=1.20.5 && <1.21` and `//? if !neoforge && …`. 1.21 reunifies them on
  `FoodData#eat(FoodProperties)`. When a mixin target is a loader patch, javap **both** jars.
- **`verify_mixins.py` used to miss both of the bugs above, for two separate reasons; both are fixed.**
  (1) It only asserted that an `@At` target member *exists on the owner class*, never that the call
  appears in the enclosing method — and `FoodData` declares both `eat` overloads on both loaders, so
  1763/1763 stayed green through the crash. It now checks call-site presence: an `@At` injection
  carries the enclosing `method =` selectors (`Injection.sites`) and the target must show up in one of
  their disassemblies, matched against javap's constant-pool comments (`// Method owner/Cls.name:desc`,
  short-form `// Method name:desc` when the owner is the class being read, `."<init>"` for a ctor).
  (2) **Every NeoForge node was being checked against pure vanilla.** MDG stages four jars in
  `build/moddev/artifacts/` and `sorted(glob("neoforge-*.jar"))[0]` picked
  `neoforge-<ver>-client-extra-aka-minecraft-resources.jar` — `-` sorts before `.` — which holds no
  classes, so every lookup fell through to the vanilla fallback and no NeoForge patch was ever
  verified. It now names `neoforge-<ver>.jar` from the pin table exactly.
  (3) Its annotation regex was anchored at `@Inject`/`@Redirect`/…, so the **fully-qualified**
  spellings a gated-out Stonecutter arm is obliged to use — `@org.spongepowered.asm.mixin.injection.
  Redirect`, `@com.llamalad7.mixinextras.injector.ModifyExpressionValue` — matched nothing and were
  skipped in silence. `EntityMixin`'s whole `>=1.20.5` arm was invisible to the checker for that
  reason. An optional `[\w.]+\.` prefix fixes it; **the count going *up* after a checker change is
  the signal that something had been unverified**, so watch the per-node numbers, not just OK/FAIL.
  (4) **…and the identical bug survived in `MEMBER_ANNO` for seventeen more waves**, because that fix
  was applied to the *injection* regex only. `@(Shadow|Accessor|Invoker)` is anchored at the `@`, so
  every member annotation inside a Stonecutter arm — where an import is impossible and the
  fully-qualified `@org.spongepowered.asm.mixin.Shadow` is compulsory — was invisible. Found when a
  new arm's predicted `+6` came back as `+5`. The same optional prefix fixes it and the total goes
  **9116 → 9243**: **127** shadows, accessors and invokers across all 37 nodes that had never been
  checked, every one of which resolves. Lesson beyond the one-line fix: **when a parser bug is found
  in one regex, grep the file for every other regex of the same shape** — the "fully-qualified inside
  an arm" trap applies to *anything* the checker matches by annotation name.
- **NeoForge 1.21 rewrote `Level#tickTime`'s day-time step as `getDayTime() + advanceDaytime()`**
  (their day-length gamerule), leaving Citadel's `@ModifyConstant(longValue = 1L, expect = 2)` on
  `ServerLevel`/`ClientLevel#tickTime` with only the game-time constant to hit. Mixin merely *warns*
  when `expect` is missed — `require` is 1 — so nothing crashed and the day-time half of a CELESTIAL
  tick-rate modifier just silently stopped working on NeoForge ≥1.21. Both mixins now gate `expect`
  to 1 on `neoforge && >=1.21` and add a `@ModifyExpressionValue` on `advanceDaytime()` that runs the
  result through the same `getDayTimeIncrement`. **An `expect` mismatch is a behaviour bug that boots
  green** — the checker is the only thing that catches it.
- **The four "plain" armour sets were invisible from 1.20.5.** `ACArmorMaterial.vanilla()` emits an
  empty `layers()` list, so vanilla's `HumanoidArmorLayer` draws nothing, and primordial/hazmat/
  diving/gingerbread were not `CustomArmorPostRender`, so `HumanoidArmorLayerMixin` did not cancel
  and redraw them either — below 1.20.5 they had been riding Forge's `IForgeItem#getArmorTexture`
  hook, which is gone. All four implement `CustomArmorPostRender` now (they already declared the
  exact method) and `ACArmorRenderProperties.renderCustomArmor` grew an `else` branch that draws with
  `RenderType.armorCutoutNoCull(texture)` + `ACClientCompat.armorFoilBuffer` — vanilla-equivalent,
  since none of the four is dyeable or trimmable. That makes all six sets take one code path on every
  version, so the old nodes exercise it too rather than keeping a second, version-only path alive.
- **Some Forge API facts track the BUILD, not the MC version — a `//?` predicate cannot describe them,
  so check the jar.** Two hit on the same node. (1) `Tags.Biomes.IS_CONIFEROUS` is gone in Forge
  **52.x** (1.21.1) — renamed `IS_CONIFEROUS_TREE`, the same sweep NeoForge did in 20.5 — while
  51.0.33 (1.21) still had it. The `!tag-coniferous` replacement rule therefore fires on
  `neoforge >=1.20.5 || forge >=1.21.1`. Its two neighbours did **not** move: `Tags.Biomes.IS_WATER`
  and `Tags.Items.SHEARS` survive on Forge as deprecated aliases assigned from `IS_AQUATIC` /
  `TOOLS_SHEAR` in the same `<clinit>`, i.e. the identical `TagKey` — read that out of
  `forge-universal.jar` with `javap -c`, don't infer it from the deprecation. (2) 50.2.9 ships
  `AddGuiOverlayLayersEvent` + `ForgeLayeredDraw`, 51.0.33 ships **neither**, 52.1.15 has them back —
  which is why the HUD-layer arm is gated `forge && >=1.20.5 && !=1.21`.
- **`modEventBus.addListener(this::clientOnlyHandler)` is a dedicated-server crash on Forge 52.x.**
  The single-arg overload resolves the event type by loading the parameter class, and from 52.x
  `AddGuiOverlayLayersEvent` drags `net.minecraft.client.gui.LayeredDraw` in with it; RuntimeDistCleaner
  refuses that on a server and CONSTRUCT dies with *"Attempted to load class … for invalid dist
  DEDICATED_SERVER"* (`LoadingFailedException`, no mixin involved). 50.2.9 resolved the same event
  without reaching `LayeredDraw`, so **1.20.6-forge booted green and 1.21.1-forge did not** on
  identical source. Guard the registration with
  `if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient())` — the method reference is safe
  merely *existing*, since its invokedynamic links only when the branch runs. A client-typed parameter
  on a method of a common class is likewise fine; it is the `addListener` call that resolves it.
- **1.21.2 requires every `Item`/`Block` `Properties` to carry its registry id before construction,
  which `DeferredRegister` cannot supply.** The constructors read `effectiveDescriptionId()` /
  `effectiveModel()` eagerly and throw *"Item id not set"*. Rather than touch ~700 upstream call
  sites, `server/misc/ACRegistryIds` holds the pending `ResourceKey` in a ThreadLocal,
  `server/misc/ACDeferredRegister` sets it around each supplier call, and the `>=1.21.2`-gated
  `mixin/ItemPropertiesMixin` / `mixin/BlockPropertiesMixin` stamp it at HEAD of those two getters.
- **NeoForge 21.2.1-beta freezes the registries with mod-created tags still unbound.** 1.21.2 turned
  "what repairs this armour" into a data component that `ArmorMaterial` fills from an item **tag**,
  via `Item.Properties#repairable(TagKey)` — and that call *creates* the tag in `BuiltInRegistries.ITEM`
  as an unbound `HolderSet.Named` the moment an armour item is constructed. Vanilla escapes it because
  `BuiltInRegistries.freeze()` binds every bootstrapped tag to empty first; NeoForge's
  `GameData.unfreezeData()` → `unfreeze(true)` clears `allTags`, defeating the Neo early-return in
  `MappedRegistry.freeze()`, so `GameData.freezeData()` dies with *"Unbound tags in registry
  ResourceKey[minecraft:root / minecraft:item]: [alexscaves:repairs_*_armor]"* **before any data pack
  is read**. Fix: `ACItemRegistry.bindModCreatedItemTags` calls the (public) `bindAllTagsToEmpty()` on
  the ITEM registry from a **LOWEST-priority `RegisterEvent`** listener — the last hook before the
  freeze, since `postRegisterEvents()` and `freezeData()` run in one `runInitTask` with no event
  between them. It is a no-op for already-bound tags and the data pack rebinds them all on world load.
  21.2.1-beta is the last 1.21.2 NeoForge build, so bumping the pin is not an option.
- **1.21.2 flattened a biome's `carvers`** from a `GenerationStep.Carving` map to one
  `HolderSet<ConfiguredWorldCarver<?>>` (`ConfiguredWorldCarver.LIST_CODEC`). The field is
  `promotePartial`, so a stale map form drops all carvers **and** fails the biome, taking the whole
  `RegistryDataLoader` pass down (*"Carver: Failed to parse either. First: Input does not contain a
  key [type]"* × 6 → `ReportedException: Registry Loading`). `DataPackMigration.flattenBiomeCarversTo1212`
  rewrites it, idempotent because it only touches the `JsonObject` form.
- **`DataPackMigration.renamedTags` matches the EXACT path, so a renamed convention tag's sub-tags
  need their own entries.** `glass` → `glass_blocks` did not carry `glass/colorless`, and `concrete` →
  `concretes` was missing entirely, so three recipes (`cinder_brick`, `hologram_projector`,
  `siren_light`) failed with *"Missing tag: 'c:concrete'"* / *"'c:glass/colorless'"*. Below 1.21.2 the
  same two ids resolved to **silently empty** ingredients, i.e. those three recipes had been quietly
  uncraftable on 1.20.6/1.21/1.21.1-neoforge as well — the fix repairs them retroactively. Authoritative
  names come from NeoForge's `Tags.java` (`c:concretes`, `c:glass_blocks/colorless`); diff every
  `forge:` id in `src/main/resources` against it rather than guessing.
- **…and a convention tag the mod asks for may not exist on the loader it is *named* after.** The
  same `cinder_brick` recipe then failed on `1.21.3-forge` — *"Missing tag: 'forge:concrete' in
  'minecraft:item'"* — because **no Forge build has ever shipped a concrete tag in the `forge:`
  namespace**: unzipping every cached universal jar from 47.4.21 (1.20.1) to 54.1.17 (1.21.4) finds
  zero, and the name Forge settled on is `c:concretes`, only from 52.x (1.21.1). So the recipe was
  uncraftable on **every Forge node since upstream**, silently until 1.21.2 made ingredient tags
  strict. A rename rule could only have fixed 1.21.1-and-up; instead the mod now **owns the tag** —
  `data/alexscaves/tags/items/concrete.json` names the sixteen vanilla concretes and folds in
  `{"id": "#c:concretes", "required": false}` so modded concretes still count wherever the loader
  does define it. One file, no gates, correct on all 58 nodes, and it repairs the old versions too.
  Its `__comment` deliberately never spells the old id, because the Forge-26 convention pass rewrites
  that string in prose as readily as in a value. Prefer this shape over a migration rule whenever the
  mod is the only consumer of the tag.
- **NeoForge 21.8 rejects an `EntityDataAccessor` FIELD merged into a vanilla entity class, not the
  `defineId` call.** `CommonHooks.verifyEntityDataAccessorRegistration` scans the holder class for
  accessor-typed fields annotated `@MixinMerged` and throws *"attempt to add synced data to a foreign
  entity"* from `Bootstrap.bootStrap`. Keep the `defineId` in the mixin (so ids and definition order
  are unchanged) and store the accessor in a plain holder class — `CitadelSyncedData` /
  `ACSyncedData` here. Fatal only in dev, and the verdict is cached per caller class, so the first
  `defineId` in a `<clinit>` decides for all of them. Full write-up in the 1.21.8 wave section below.
- **`maven.neoforged.net` answers a path it does not host with HTTP 200 and an EMPTY BODY**, not a
  404 — and Gradle treats unparseable metadata as a hard failure rather than falling through to the
  next repository. Harmless for a fixed version (the POM 404s properly); fatal for a **dynamic** one,
  which must list versions before it can pick. `1.20.4-neoforge` and `1.20.6-neoforge` pull
  `net.minecraftforge:unsafe:0.2.0`, whose POM asks for `org.apache.logging.log4j:{log4j-api,
  log4j-core}:2.11.+`, so `createMinecraftArtifacts` dies with *"Failed to list versions … Premature
  end of file"* on `maven-metadata.xml` — the two nodes that broke the 1.21.9 wave-closing build.
  `build.neoforge.gradle.kts` keeps the whole log4j group off that maven with a repository content
  filter (applied by URL over `repositories.withType<MavenArtifactRepository>()`, because MDG adds
  its own instance of the repository), sending the lookup to Maven Central, which answers. Neither
  artifact has ever lived on the NeoForged maven, so the filter costs nothing.
- **An unqualified version predicate in a MIDDLE arm silently claims every loader that reaches that
  version.** Stonecutter evaluates arms in order, so `//? if neoforge && >=1.21 { … } elif >=26 { … }
  else { … }` sends *Fabric* ≥26 into the middle arm, not into the `else` it was written to keep. That
  is how `AlexsCaves#PROXY` and `Citadel#PROXY` broke every Fabric node from 26 up on
  `net.minecraftforge.fml.loading.FMLEnvironment` — a class this tree stubs only for the `else` arm's
  spelling. **When a wave adds an arm because one loader deleted an API, the arm must name that
  loader** (`forge && >=26`); a version-only predicate in the last arm-but-one is a loader wildcard.
- **`Level#random` went `public` → `protected` at MC 26.** A mixin reads a shadowed field through the
  target's own class, so `this.level.random` is a compile error from 26 while `this.level.getRandom()`
  is public across the whole 1.20.1→26.x range and returns the same instance. Prefer the getter to an
  access-widener/AT entry wherever one exists — it needs no gate on any of the 58 nodes.
- **The `c:` convention sweep is a TAG sweep, and `forge:`-namespaced ids that are not tags must be
  exempted from it — on Forge 26 exactly as they already were on NeoForge.** `migrateConventionTags`
  rewrote every `forge:<path>` it found, including the *holder-set type* ids `forge:and` / `forge:not`
  inside `underground_cabin.json`'s biome set. Forge 65.1.0 still registers those three types under
  `forge:` (read out of `ForgeMod`'s `<clinit>` constant pool in the universal jar) — the tags moved,
  the holder-set types did not — so the rewrite produced an id nothing has ever registered, and an
  unknown holder-set type is **fatal**: it takes the whole `RegistryDataLoader` pass down and the
  server never starts, the identical failure the NeoForge migration note above describes. The old
  `neoForgeIds` whitelist is renamed **`nonTagForgeIds`** and is now read by *both* passes for
  opposite reasons — `migrateNeoForge` re-points exactly these, `migrateConventionTags` leaves exactly
  these alone. Found on `1.20.1-fabric` and it would have killed **all five Forge nodes ≥26**, none of
  which had ever been booted. Lesson: a namespace-wide rename pass needs to know what *kind* of thing
  each id names, and the cheapest way to find out is the loader's own bytecode.
- **Fabric has no composite `HolderSet` at all, and there is nothing to add one to.** Vanilla's
  `HolderSetCodec` reads a `"#tag"` string or a list of ids, full stop; the `and`/`or`/`not`/`any`
  composition this mod's cabin structure is authored with is a **loader patch** on Forge and NeoForge
  (a `holder_set_type` registry), not an extension point a mod can register into. So the file is
  fatally unreadable there — *"Not a string: {"type":"c:and", …}"* → `Unbound values in registry
  minecraft:worldgen/structure` → `Failed to load registries`. `DataPackMigration
  .flattenCompositeHolderSets` (Fabric only) rewrites each composite down to its single **positive**
  member, erroring loudly rather than guessing if one has zero or two. Here that is exact, not an
  approximation: the composite means "vanilla stronghold biomes **except** this mod's six cave
  biomes", the mod ships no override of `#minecraft:stronghold_biased_to`, and its six biomes are not
  in it — so the intersection *is* the positive tag, and only a third-party datapack that added a cave
  biome to the vanilla tag could tell the difference.
- **`META-INF/enumextensions.json` matches the target enum constructor BY DESCRIPTOR, and 26.2 widened
  `MobCategory`'s.** A `debugAbbreviation` String was inserted after the serialized name, so
  `(Ljava/lang/String;IZZI)V` — correct on `26.1.2` and `1.21.11`, javap'd on both — is rejected on
  26.2 with *"Invalid, non-existant or disallowed constructor … for field
  'ALEXSCAVES_CAVE_CREATURE'"*, and the cascade is unhelpfully far from the cause:
  `NoClassDefFoundError: Could not initialize class …EntityTypes` → `FatalStartupException: Couldn't
  find Minecraft server thread`. The file is a **plain resource, not a preprocessed source**, so no
  `//?` gate and no `replacements.string` rule can reach it — it takes a `processResources` pass
  (`DataPackMigration.retargetEnumExtensionsTo1262`, NeoForge ≥26.2, since `Loader.NeoForge` is the
  only loader that emits the key). The Java side needed nothing: `MobCategoryInvoker` already had its
  `>=26.2` arm. **Whenever a vanilla constructor a manifest names by descriptor changes, grep
  `resources/` — the compiler is checking a different copy of that signature than the loader is.**
- **From 26.2 an `ItemStack` cannot be built in a class initialiser that runs before the registries
  freeze.** `ItemStack.<init>` reads `Holder$Reference#components` eagerly now, which throws
  `NullPointerException: Components not bound yet` until the item registry's component maps are bound.
  `LicowitchEntity` held its splash-potion stack in a `static final`, and the class is loaded from
  `ACEntityRegistry#initializeAttributes` — i.e. while `EntityAttributeCreationEvent` fires, long
  before the freeze — so the mod died at `ExceptionInInitializerError`. The fix is a lazy accessor
  (`hungerPotion()`), **ungated**, since the stack it builds is identical on every node and only the
  licowitch's own goal reads it, on the server thread. Same family as the `ACFoods`/`FoodProperties`
  eager-effect trap at 1.20.5: **deferred registration only defers what it is handed lazily**, and 26.2
  moved one more thing from "cheap at clinit" to "needs a bound registry". Grep for `static final
  ItemStack` before every future wave.
- **…and "before the freeze" is not the whole window — on 26.2 an item's components are still unbound
  while the DATAPACK is being read, so a recipe cannot build an `ItemStack` either.** The binding is
  `DataComponentInitializers.PendingComponents#apply()` → `Holder.Reference#bindComponents`, and
  `ReloadableServerResources` calls it from `updateComponentsAndStaticRegistryTags()` — which runs
  **after** `SimpleReloadInstance` has finished every reload listener. `RecipeManager.prepare` is one of
  those listeners, so a recipe constructor invoked from a `KeyDispatchCodec.decode` sees exactly the
  same unbound holders a class initialiser does: *"Failed to load datapacks, can't proceed with server
  load … NullPointerException: Components not bound yet"* at `ItemStack.<init>` ← `RecipeCaveMap.<init>`,
  cascading to `FatalStartupException: Couldn't find Minecraft server thread`. **On both loaders** —
  this one is vanilla, not a loader patch. The fix is that 26's own result type wants an item, not a
  stack: `new ItemStackTemplate(item)` keeps only `item.builtInRegistryHolder()` and an empty
  `DataComponentPatch`, where `ItemStackTemplate.fromNonEmptyStack(new ItemStack(item))` round-trips
  through the very components that are not there yet. General form, and the reason this is a *second*
  bullet rather than an edit to the one above: **"is the registry frozen?" and "are components bound?"
  are two different questions with two different answers**, and the second one stays `no` until the
  first datapack reload is over. Anything decoded from a codec — recipes, loot modifiers, entries in a
  datapack registry — is inside that window.
- **Forge REFUSES an `@OnlyIn` annotation anywhere in mod code from build 62.0.9 (the first 26.1
  build), and that is a Forge-BUILD change rather than an MC one.** `RuntimeDistCleaner.processClassWithFlags` throws
  `UnsupportedOperationException: Method X in mod class Y is annotated with @OnlyIn, this is no longer
  supported as it slowed down startup times`. The identical throws are already present in Forge
  **61.1.0** (1.21.11), but behind `LazyInit.CAN_EXPLODE = !FMLEnvironment.production &&
  "21.6".equals(FMLLoader.versionInfo().mcVersion())` — a guard that can never fire — so nothing below
  26.1 ever noticed. ⚠️ This bullet said "65.1.0" and gated the rule at `>=26.2` until the shakedown
  booted 26.1 for the first time: all three 26.1.x Forge nodes throw the identical 13 times, while
  1.21.11-forge (61.1.0) boots clean. **A Forge-build fact found on one node dates the change at that
  node and nowhere else** — the boundary is only known once the node below it has actually booted. **Two different failure modes, and only one of them shows up on a server.** A
  *class*-level `@OnlyIn` throws on either dist and so kills a dev client too; a *method* or *field*
  one throws only on the dist it is not for, so `runServer` is the only place the 9 server-loaded
  ones surfaced — the boot aborted mid-`RegisterEvent` and cascaded into a wall of
  `Registry Object not present: alexscaves:pewen_door` / `NoClassDefFoundError` lines that name
  everything except the cause. This tree carries **71** of them and every body is dist-neutral
  (`ACPlatform.encapsulating`, `getRangeBB`, `new ItemStack`, `level().addParticle`), so the fix is one
  `forge && >=26.1` `replacements.string` rule — `!mc261-onlyin-forge` — that rewrites the annotation
  to a `/* client-only */` comment. Neutralising it is behaviour-neutral: the annotation only ever told
  the dist cleaner to strip the member, and a member that is never called on a server costs nothing.
  Fabric and NeoForge are untouched, and the two `@OnlyIn`/`Dist` imports are deliberately left
  dangling (an unused import is legal Java; a rule that also removed them would have to match two more
  spellings for no gain).
- **`Item#components()` stops being answerable at 26.1, and it takes `ModifyDefaultComponentsEvent`
  with it.** Up to 26 an `Item` kept its default components in a plain field; from **26.1** the getter
  delegates to `builtInRegistryHolder().components()`, which throws `NullPointerException: Components
  not bound yet` until `DataComponentInitializers` binds them — and NeoForge fires
  `ModifyDefaultComponentsEvent` from inside that very build, so `AlexsCaves#modifyDefaultComponents`
  reading the tier's existing `REPAIRABLE` off `item.components()` is a hard boot failure on **all four
  NeoForge 26.x nodes**, not just the newest. The builder the modifier is handed has already been
  through vanilla's own initializers (`DataComponentInitializers#createInitializerForRegistry` runs
  them, *then* calls `DataComponentModifiers#apply` on the same builder), so it answers exactly the
  same question — and **26.1 is also where `DataComponentMap.Builder#get(DataComponentType)` arrives**,
  i.e. the replacement read ships in the version that broke the old one. The whole REPAIRABLE branch
  therefore moves inside the `event.modify` lambda and the read goes through a two-arm helper,
  `acExistingRepairable`, gated `neoforge && >=26.1` / `neoforge && >=1.21.2`. ⚠️ **Its builder
  parameter type is per-arm**, because 26.1 also swapped `modify`'s `Consumer<DataComponentPatch
  .Builder>` for a `Consumer<DataComponentMap.Builder>` — javap the event, not just the getter, since
  the two changes are invisible to each other. (26.1.2 additionally *deprecates* that `Consumer`
  overload in favour of an `Initializer` functional interface; the `Consumer` one still exists on 26.2
  and is what this uses, so no third arm is needed yet.)

- **26.1 deleted `Feature.RANDOM_PATCH`, and its replacement is PLACEMENT rather than another
  feature.** ⚠️ **26.1, not 26.2** — this bullet said 26.2 until the runtime shakedown booted the
  26.1.x nodes for the first time and all three NeoForge ones died on it. The pass was *written*
  during the 26.2 wave, which is a statement about when it was noticed, not about when vanilla
  moved; it and the lake pass below are two unrelated changes that shared one wave and are gated
  separately now (`>=26.1` and `>=26.2`). General form: **a compile-only wave dates a break at the
  node you happened to be on.** Gone with it: `FLOWER`, `NO_BONEMEAL_FLOWER`, `DRIPSTONE_CLUSTER`, `POINTED_DRIPSTONE`,
  `FOREST_ROCK`, `ICE_SPIKE`. An unknown feature type is fatal, not skipped — the whole
  `RegistryDataLoader` pass dies with *"Unknown registry key in ResourceKey[minecraft:root /
  minecraft:worldgen/feature]: minecraft:random_patch"* and the server never starts. Vanilla's own
  `VegetationFeatures`/`VegetationPlacements` show the shape: the patch's inner `simple_block`
  **becomes** the configured feature, and its three fields move onto the **placed** feature that
  referenced it — `tries` → `minecraft:count`, `xz_spread`/`y_spread` → `minecraft:random_offset`
  over `TrapezoidInt.triangle(range)` (`{"type":"minecraft:trapezoid","min":-range,"max":range,
  "plateau":0}`, whose `sample` is `nextInt(max+1) - nextInt(max+1)` — byte-for-byte what
  `RandomPatchFeature` computed inline), and the patch's own inner `placement` list appended last.
  `DataPackMigration.unrollRandomPatchesTo1262` does both halves, keyed on the configured feature's
  id; expect **8** files (4 configured + 4 placed). ⚠️ **The new modifiers go at the END of the
  existing list, after `biome`, and that ordering is the behaviour.** The old shape biome-tested the
  patch *origin* once and only then scattered; appending earlier would biome-test every scattered
  position instead, which is a different feature. Same reasoning for a mod whose patches are
  referenced from more than one placed feature: the unroll is per-reference, so each gets its own copy.
- **…and at 26.2 `minecraft:lake` grew three required block predicates**
  (`can_place_feature`, `can_replace_with_air_or_fluid`, `can_replace_with_barrier`) that had been
  hard-coded inside `LakeFeature#place` since 1.17 — *"No key can_replace_with_barrier in
  MapLike[…]"*, fatal the same way. The values that reproduce the deleted logic exactly are vanilla
  26.2's own `MiscOverworldFeatures.LAKE_LAVA`: `{"type":"minecraft:true"}`, then
  `not(matching_block_tag minecraft:features_cannot_replace)` and
  `not(matching_block_tag minecraft:lava_pool_stone_cannot_replace)`.
  `DataPackMigration.fillLakePredicatesTo1262` fills only keys that are absent, so a file that
  already spells one keeps it; expect **2**. General lesson for the whole `>=26.2` band: **a
  worldgen codec that gained a required field and a feature type that was deleted look identical
  from the log** — both are one `Caused by` under `Registry Loading` — so read every entry in the
  error report, not the first.

### Gotchas the 26.x Fabric client shakedown found (2026-08-20)

Two user-reported symptoms on two adjacent nodes — *"on 26.2 fabric I get a black menu screen and in
26.1.2 the game crashed"* — and neither is a 26.x port bug in the sense the version walk was looking
for. One is a resource the mod ships being **right below a version and fatal at it**; the other had
been latent on **every Fabric node ≥1.20.5 since the Fabric milestone** and needed a living entity to
touch water before anything noticed.

- **⚠️ 1.21.9 deleted every post-pass VERTEX shader but `rotscale`, and a chain naming a deleted one
  cannot compile.** The fullscreen quad is generated from `gl_VertexID` now, so `minecraft:post/blit`
  and `minecraft:post/sobel` are gone as vertex stages and a pass that names one logs *"Couldn't
  compile program for pipeline"* and is **nulled** — `ShaderManager#getPostChain` returns null and the
  effect silently does nothing (below 26.2 that is a lost effect; at 26.2 it blanks the frame, see
  below). `DataPackMigration.migratePostShadersTo1219` deletes this mod's three equivalent vertex
  stages and rewrites the five fragment shaders onto the new inputs; expect **15** assets. It refuses
  to drop a vertex stage that is not byte-equivalent to the screen quad (`Position.xy * OutSize`,
  `texCoord = Position.xy;`, `oneTexel`) rather than silently losing a stage that did something else.
- **⚠️⚠️ 26.2 split a post pipeline's binds into TWO bind groups, which makes the chain-level
  `"Globals"` uniform declaration a duplicate — and that is the black main menu.** From 26.2
  `PostChain.createPass` builds on `RenderPipelines.POST_PROCESSING_SNIPPET`, which now includes
  `GLOBALS_SNIPPET`, so `Globals` is already bound in **group 0**; every input's `<name>Sampler`,
  `SamplerInfo` and *every key of `Pass.uniforms()`* go into a second `BindGroupLayout`, and
  `BindGroupLayout.ensureCompatible(List<BindGroupLayout>)` walks all groups with **one**
  `HashSet<String>` → *"Duplicate bind name 'Globals' in bind group layout 1"*. The throw is swallowed
  the same way a compile failure is, so the chain is null, the mod's screen pass draws nothing, and the
  **whole frame** is black with no crash and no obvious log line. ⚠️ **The same line is load-bearing
  below 26.2**: `PostChain` there contains *zero* references to `BindGroupLayout` and
  `POST_PROCESSING_SNIPPET` declares no `Globals` (1.21.6 declares only `Projection`; 1.21.9 and 26.1.2
  declare nothing), so declaring it is what makes `GameTime` resolve at all — which is why
  `blockPostChainUniformsTo1216` emits it and why the fix is a **separate `>=26.2` pass**
  (`dropPostChainGlobalsTo1262`, expect **1** — `hologram`, the one fragment shader that reads `Time`)
  rather than an edit to the older one. General form, and the reason a green build says nothing here:
  **a JSON asset can be simultaneously required on one node and fatal on the next, and the loader
  reports neither as an error.** The javap tell is one grep: `PostChain` naming `BindGroupLayout`, and
  `RenderPipelines.POST_PROCESSING_SNIPPET`'s uniform list.
- **⚠️⚠️ On Fabric, `Bootstrap.validate()` builds every vanilla `AttributeSupplier` BEFORE mod init, so
  an attribute registered from `onInitialize` is wrapped as a `Holder.direct` and can never be looked
  up again.** `AttributeSupplier` is a `Map<Holder<Attribute>, AttributeInstance>` and
  `MappedRegistry.wrapAsHolder(T)` returns `byValue.get(value)` — a bound `Holder$Reference` — or, when
  the value is not registered yet, `Holder.direct(value)`, a record. The two can never be equal. In the
  26.1.2 dev log `Bootstrap.validate()` → `DefaultAttributes.<clinit>` → `createLivingAttributes` is
  stamped **four seconds before** `Fabric common init`, so `mixin/fabric/LivingEntityAttributesMixin`'s
  `@Inject` at `createLivingAttributes` RETURN keyed the map with a Direct; at tick time the attribute
  *was* registered, `wrapAsHolder` returned the Reference, and `getAttributeInstance`'s bare
  `instances.get(holder)` missed — `IllegalArgumentException: Can't find attribute
  alexscaves:swim_speed`, out of `LivingEntity.travelInWater` ← `travel` ← `aiStep` ← `Bat.tick`, i.e.
  **the first time any living entity entered a fluid**. ⚠️ **The message is a red herring**: it is built
  from `holder.getRegisteredName()`, whose default implementation prints `[unregistered]` only for the
  *lookup* holder — so a message that names the attribute proves the lookup side is bound and says
  nothing about the key side. Fixed by moving the two `Registry.register` calls into
  `ACFabricAttributes`' **static initialiser**, so the mixin's first touch of `SWIM_SPEED` registers it
  at that instant whenever the class is first loaded; `register()` stays as an empty method that forces
  `<clinit>` at the old point for a production run, where `Bootstrap.validate()` never runs. General
  form: **on Fabric, "mod init" is later than vanilla's bootstrap, so anything a vanilla static
  initialiser can reach must register from its own `<clinit>`, not from `onInitialize`.** This is the
  fourth member of the family this file records (after `ACFoods` at 1.20.5, the brewing `ItemStack` at
  26.1 and `RecipeCaveMap` at 26.2) and the only one that is an *identity* bug rather than a
  not-yet-bound one. Verified at runtime on `26.1.2-fabric`: a zombie and a bat live in a water column
  for six seconds with zero exceptions, and `/attribute @e[type=zombie] alexscaves:swim_speed get`
  answers `1.0` — that command performs exactly the holder-keyed lookup that used to miss.
- **Kotlin block comments NEST, so a literal `/*` inside a KDoc silently breaks the file.** Writing a
  Stonecutter arm marker or a shader snippet into a `DataPackMigration` doc comment opens a second
  comment that the closing `*/` only half-closes, and the failure is an "Unclosed comment" cascade
  pointing at whatever declaration comes next — never at the doc comment. Never put an opening
  comment marker inside another comment; describe it in prose instead.
### Gotchas the content-warning pass found (same 26.x client shakedown, 2026-08-20)

The two fixes above made the 26.x Fabric client *boot*; the log it then produced was still carrying
34 content warnings that every green build and every `Done` server had passed. None is a port bug —
all four families are upstream content defects that a newer MC merely started reporting — and three
of the four are **invisible below a specific version**, which is why 58 green nodes said nothing.
Two standing scripts came out of it: **`scripts/model_audit.py`** and **`scripts/sound_audit.py`**.

- **⚠️⚠️ An out-of-bounds face `uv` was a silent wrong-texture bug on every version this mod has ever
  shipped, and is a FATAL bake failure from 26.1.** A face `uv` is model space scaled by
  `texture_size` (default `[16,16]`); a rect that leaves that box does **not** clamp — it samples
  whatever neighbouring sprite happens to sit beside this one in the stitched atlas, so the block has
  been rendering foreign pixels since 1.20.1 with no log line anywhere. From 26.1
  `FaceBakery.computeMaterialTransparency` → `SpriteContents.computeTransparency` →
  `NativeImage.computeTransparency` hard-throws *"Cannot compute translucency out of bounds: [16, 6,
  20, 10] in 16x16 image"*, and the throw fails the **whole model bake**, cascading into `Missing
  model for variant` for every blockstate that named it — i.e. a missing block, not a slightly wrong
  one. **51 faces across 13 models** were out of range here (`uranium_rod` and `abyssal_altar` being
  the two that actually threw). The repair is per axis and mechanical — if the rect overflows,
  translate it so its lower edge sits on the sprite origin; clamp only if it is *wider* than the
  sprite, which is only ever true of a degenerate face — and a pure translation preserves the
  authored orientation, rotation and extent, so the fixes are pixel-exact rather than a guess.
  `scripts/model_audit.py --fix` does it.
- **Only a face with AREA reaches the transparency check, so the degenerate ones are latent rather
  than fatal — and that same short-circuit is why a `#missing` texture can sit in a shipped model for
  years without ever showing magenta.** MC skips a zero-extent quad *before* the texture lookup as
  well as before the transparency check. That is the whole reason `curly_fern_top` / `fern_thatch`
  carry `#missing` faces and never warn, while `heart_of_iron` / `quarry` — whose `#missing` faces are
  real geometry, merely fully occluded by their siblings — do. **Do not read "nobody has ever seen
  it" as "it is not a bug"**; a later version that stops short-circuiting turns the whole set fatal at
  once, which is exactly what 26.1 did to the UVs.
- **`Missing texture references in model …` is a 1.21.4-and-up warning family** — 0 on the 30 nodes
  below it, 20 on 26.x — in two shapes. (1) `#missing` is a **Blockbench** placeholder for a face the
  modeller left untextured; it is never present in the model's `textures` map, so it can only ever
  resolve to the magenta sprite. 48 faces across four models here; retexturing them to the sibling
  face's slot keeps geometry and quad count byte-identical, so it is provably a no-op on the
  degenerate ones and strictly an improvement on the occluded ones. (2) An **ISTER item model**
  (`builtin/entity`, no `textures` block at all) has never resolved `particle` — and note that is
  *this port's* doing rather than upstream's: `DataPackMigration.stripDeadParent` strips the
  `builtin/entity` parent from 1.21.4, which is what leaves the slot open. Cosmetic only
  (`SpecialModelWrapper` bakes the base model solely for its display transforms, so the slot feeds
  the break/use particle icon and nothing else), but it is 16 of the 20 lines.
- **⚠️ A texture slot is resolved against the STITCHED ATLAS, so a PNG existing on disk is not
  the same question as the texture existing.** The `minecraft:blocks` atlas is sourced from the
  `block/` and `item/` directories only — across every namespace, which is why a mod that ships no
  `atlases/blocks.json` of its own still gets its own `block/`+`item/` sprites stitched, and why a
  texture under `entity/` (loaded standalone by an entity renderer) is **not** in it. Filling the 16
  ISTER `particle` slots above, three of them — `raygun`, `shot_gum`, `galena_gauntlet` — got the
  obvious file, the entity texture the item's own renderer already uses, and that traded one warning
  family for another: `Missing textures in model alexscaves:item/raygun:` where the baseline log had
  **zero**. Each now names the item's dominant crafting material instead (`item/polymer_plate`,
  `item/gumball_pile`, `block/packed_galena`) — all three lack an `item/<id>` sprite of their own,
  being entity-rendered. Two lessons, and the second is the larger one: a `texture_exists`-style
  check keyed on `os.path.isfile` passes this bug silently, so `model_audit.py`'s check 2 asserts
  **atlas residency** as well as existence; and **a fix verified only against the warnings it was
  aimed at is not verified** — count every marker in the after-log against the baseline, including
  the ones you were not expecting to move.
- **⚠️ Auditing a model's texture slots in ISOLATION invents dozens of false misses — resolve from
  the BAKE ROOTS.** A model that is only ever used as a `parent` is a *template* and deliberately
  leaves slots open (`block/anemone_base` and friends expect a child to fill `#base`/`#tentacles`);
  MC resolves slots in the context of the **leaf**, and so must any checker. `model_audit.py` walks
  only the models a blockstate or an item model actually names. Two smaller traps in the same code:
  an unqualified texture id (`block/iron_block`) means **`minecraft:`**, not the mod namespace, and
  `elements` are inherited from the nearest ancestor that declares any, not from the leaf alone.
- **A model whose `parent` names something that was never registered still gets loaded, and warns.**
  `models/item/thornwood_leaves.json` was an upstream orphan — no block, no blockstate, no block
  model, no texture, nothing in `ACBlockRegistry` (the thornwood tree has *branches*,
  `ThornwoodBranchBlock`) — but `DataPackMigration.writeItemModelDefinitions` derives its 575
  definitions **from the model tree**, so the orphan got an item-model definition, MC loaded it, and
  the client logged `Missing block model: alexscaves:block/thornwood_leaves` forever. Deleting the
  one file is the whole fix. Same family as the `cave_painting_friendship` / `cave_painting_hunt`
  orphan blockstates already recorded above — **grep the model tree against the registry, not the
  other way round**, since an orphan is by definition referenced by nothing.
- **⚠️⚠️ Sweep the sound tree in BOTH directions, because a key-name typo reads as "missing content"
  from one end and as nothing at all from the other.** A registered `SoundEvent` with no
  `sounds.json` key logs `Missing sound for event: …` once and is then silent for the session; a
  `sounds.json` key no event registers is never reported at all. A typo produces one of each, and the
  **pairing is the diagnosis**: here the counts stayed equal at 481/481 while
  `abyssal_chasm_ambience_mood` and `luxtructosaurus_breath` were "missing" and `abyssal_chasm_mood`
  and `luxtructosaurus_breathe` were dead — i.e. the audio had shipped correctly all along and only
  the spelling was wrong, so both "missing sounds" were one renamed key each. The abyssal chasm had
  therefore been playing **no mood sound at all** since upstream (its biome JSON references the
  correct id; the other five biomes all spell it the long way). Two more defects fell out of the same
  sweep: `luxtructosaurus_snort` is registered and played on the nostril-particle animation but **no
  snort audio exists anywhere in the tree**, and `purple_soda_swim` named five files where three ever
  shipped (a copy-paste from `acid_swim_*`, which really does have five). ⚠️ **A third direction is
  worth checking too** — a `subtitle` key with no `en_us` translation renders the raw key on screen
  and **nothing logs it**; two were referenced here, one of them a singular/plural mismatch against a
  key that was already translated into 12 languages. `scripts/sound_audit.py` checks all four.
- **Fix content bugs in `src/main/resources`, not in a `DataPackMigration` pass** — the standing
  preference in this file, and every fix in this pass honours it. All four families are wrong on all
  58 nodes and merely *reported* on some, so a source fix is correct everywhere and repairs the
  released versions retroactively, where a migration pass could only ever fix the band it is aimed at.

### Gotchas the in-world test battery found (first world ever generated, 2026-08-19)

Every verdict before this section was boot-level: a dev server reaching `Done`, a dev client reaching
the title screen. **`TESTPLAN-1.0.0.md`'s first execution generated the first world this mod has ever
made, and it found two bugs that no boot can see** — one of which had silently disabled the entire
mod on 22 nodes and the other of which crashes the server on 40 of them. Both are *runtime* facts
about content, and neither `verify_mixins.py` nor a green build has any opinion about them.

- **⚠️⚠️ NOTHING on Fabric ever POSTS a Forge game-bus event, so ~20 of `CommonEvents`' handlers had
  never run on any of the 22 Fabric nodes — and the loudest consequence is that the mod's six cave
  biomes DO NOT EXIST.** The Fabric port supplies Forge's *shapes* (`fabric/forge/**` stand-in types,
  `fabric/forge/common/MinecraftForge` with its `EVENT_BUS`), and the 25 `mixin/fabric/**` classes
  reproduce Forge's **loader patches** — but nothing anywhere constructs a lifecycle, living or player
  event: `grep` for `new ServerAboutToStartEvent(`, `new LivingDeathEvent`, `new TickEvent.`, `new
  PlayerEvent.` finds **zero** sites in the whole tree, and `MinecraftForge.EVENT_BUS.post` appears
  only for Citadel's own events and client render events. `fabric/forge/common/MinecraftForge`'s
  javadoc promises a `fabric/event/**` dispatch layer; that package contains exactly one file
  (`ACEventBus.java`). The bus exists, the handlers are registered on it, and it is never fired.
  **The biome symptom is the one that matters**: `CommonEvents#onServerAboutToStart` is what calls
  `ACBiomeRarity.init()` and hands `BiomeSourceAccessor#setResourceKeyMap` the biome table that
  `MultiNoiseBiomeSourceMixin#ac_getNoiseBiomeCoords` consumes, so with no post the injection chain
  is dead and every world generates with vanilla caves only — **silently, with no log line, on a
  server that boots green**. Fixed in `AlexsCavesFabric` by posting `ServerAboutToStartEvent` from
  Fabric's `SERVER_STARTING` (same guarantee: registries frozen, no level loaded) and
  `ServerStoppingEvent` from `SERVER_STOPPING` (**not** `STOPPED` — the handler clears tick-rate
  modifiers off a tracker it looks up from the server, so the server must still be usable). Proven at
  runtime: after the fix `1.21.11-fabric` locates all six cave biomes and all fourteen structures at
  **coordinates byte-identical to `1.20.1-forge` on the same seed**.
  ✅ **The remaining 18 handlers were closed on 2026-08-20** — the whole dispatch layer now exists, in
  four stages: (1) the two lifecycle events above; (2) `fabric/event/ACGameEvents` + `ACDamageEvents`
  and ~20 `mixin/fabric/**` dispatchers for the server/player/living surface, including
  **`serverTick`, the one with teeth, which drives `ACWorldWorkerManager` and so cave-map resolution**;
  (3) `fabric/event/ACClientGameEvents` + `mixin/fabric/client/**` for camera angles, hand render, fog
  colour and setup, FOV, block screen effect, living-renderer pre/post, HUD overlays and boss-bar
  progress; (4) `fabric/event/ACFabricVillagerTrades` + `VillagerTradesTableMixin` /
  `WandererTradesTableMixin` for the two underground-cabin-map trades (MC <26 only — from 26 the whole
  code-side trade API is gone and trades are datapack entries on every loader). Three events are
  deliberately answered *without* a producer and must not get one: `RenderLevelStageEvent` is
  superseded by `client/ACLevelRenderStage`, and `EntityAttributeCreationEvent` /
  `SpawnPlacementRegisterEvent` are answered by `fabric/entity/ACFabricEntityRegistration`. Full
  write-up, stage table and the trade-table design rationale: **`docs/notes/fabric.md`**, whose "Known
  gaps" list was wrong by omission and has been rewritten.
  ⚠️ **The standing check that would have caught this is now `scripts/event_audit.py`** — a set
  difference of every `*Event` named by an `@SubscribeEvent` handler against every `new *Event(` under
  `fabric/` + `mixin/fabric/`, exit 1 on any gap. **Run it after any wave that adds a handler**, and
  before any release. Two things make it precise rather than noisy, and both are worth knowing before
  editing it. (1) A raw scan sees *every* loader's spelling at once, because the arms are all present
  in the file as commented-out text — `TickEvent.ServerTickEvent.Post`, `ServerTickEvent.Pre`,
  `EntityTickEvent.Pre` and `RenderGuiLayerEvent` are Forge/NeoForge bands that Fabric never compiles.
  What scopes them out is **the existence of a stand-in class under `fabric/forge/`**: a handler cannot
  consume an event whose class does not exist on that loader, so an event with no stand-in is out of
  scope and one whose *outer* is a stand-in is checked under the outer name (Fabric's
  `TickEvent.ServerTickEvent` carries a `phase` field, not `Pre`/`Post` subclasses). (2) A nested event
  is spelled `Pre` / `Added` / `RightClickItem` — the *file* contains "Event", the nested declaration
  does not, so a declaration regex keyed on the name silently drops them and the audit invents eleven
  misses. Three events are exempt with a written reason (`RenderLevelStageEvent`,
  `RenderGuiOverlayEvent.Pre`/`.Post`) and the script says so on every run, plus warns when an
  exemption no longer has a consumer. **Sensitivity-checked** by pointing `PRODUCER_DIRS` at
  `fabric/forge` alone: it then names exactly the handlers that really were dead, `TickEvent
  .ServerTickEvent` and both trade events among them. A checker that cannot be made to fail on demand
  has not been verified.
  General form, and the reason this was invisible for a whole milestone: **a compile-green,
  boot-green loader port proves the shapes exist, not that anything calls them.** When a port
  supplies another loader's API surface, enumerate the *producers* as carefully as the consumers —
  `grep` for `new <Event>(` and for the bus's `post`, not just for the handler annotations.

- **⚠️ 1.21.2 moved `TemptGoal`'s range onto a new `TEMPT_RANGE` attribute that only `Animal`'s
  supplier carries, so eleven of this mod's mobs crash the server the instant one ticks — on ALL
  THREE LOADERS, on every node from 1.21.2 up.** Up to 1.21.1 `TemptGoal` used a hard-coded
  `TargetingConditions.range(10.0)`; from 1.21.2 `canUse` opens with
  `mob.getAttributeValue(Attributes.TEMPT_RANGE)`, unconditionally. Vanilla adds the attribute in
  **`Animal.createAnimalAttributes()` alone** — `Mob`, `Monster` and `LivingEntity` do not — and
  `AttributeSupplier#getAttributeInstance` throws `IllegalArgumentException: Can't find attribute
  minecraft:tempt_range` for anything the supplier never declared. All eleven AC mobs that add a
  vanilla `TemptGoal` (relicheirus, gingerbread man, subterranodon, tremorsaurus, grottoceratops,
  gummy bear, atlatitan, candicorn, vallumraptor, raycat, tremorzilla) build from
  `Monster.createMonsterAttributes()`, so each is `ReportedException: Ticking entity` one tick after
  it spawns. **Neither loader patches the lookup to be lenient** — javap'd on Forge, NeoForge and
  Fabric — so this is 40 of the 58 nodes, not a Fabric bug; it only surfaced on Fabric because that
  is where the first mob was ever summoned. Fixed with one `>=1.21.2`-gated helper,
  `ACCompat.temptable(builder)`, wrapped round all eleven suppliers. `10.0` is not a guess: it is
  both vanilla's own default for the attribute and the constant 1.20.1's `TemptGoal` baked in, so
  behaviour is unchanged on all 58 nodes.
  ⚠️ **This is a standing check now — `scripts/ai_attribute_audit.py`** — because it generalises
  past this one attribute and a new instance would be equally invisible. Per node it reads the
  mob→supplier map out of `ACEntityRegistry#initializeAttributes`, resolves what each mob actually
  declares (the vanilla base supplier followed transitively through the bytecode, plus the mod's own
  `.add(Attributes.X)` calls and the `ACCompat.temptable` helper), then javaps every vanilla
  `ai.goal`/`ai.behavior` class the mob constructs and set-differences the `Attributes.X` each reads.
  **43 mobs, 23 vanilla AI classes, all 58 nodes green**; the two attributes vanilla AI reads here are
  `FOLLOW_RANGE` (`TargetGoal`, every node) and `TEMPT_RANGE` (`TemptGoal`, ≥1.21.2 only) — that 1→2
  step at 1.21.2 is visible in the per-node output and is what the counters are printed for. Three
  things it does that a naive version would not, each of which made it silently vacuous first:
  **(1)** ten of the eleven affected mobs reach `TemptGoal` through a **wildcard** import, so an
  explicit-import map alone resolves nothing and the check passes on 10 of the 11 cases of the very
  bug it exists for (the same hole `verify_mixins.py` had with `@Mixin` targets); wildcards are
  probed against the node's own jars. **(2)** A goal of the mod's own inherits its superclass's
  reads, so an unresolved simple name is followed up the *mod* hierarchy to its first vanilla
  ancestor. **(3)** A vanilla goal's reads are unioned with its **vanilla supers** —
  `NearestAttackableTargetGoal` reads nothing itself; `FOLLOW_RANGE` is `TargetGoal`'s.
  Sensitivity-checked by deleting the `temptable` helper's contribution in-process: it then names
  exactly the eleven mobs and exits 1.

- **⚠️ One upstream `defineId` naming the WRONG class killed two mobs on 51 of the 58 nodes, and it is
  invisible below 1.20.5.** `LicowitchEntity`'s `TELEPORTING_TO_POS` was declared
  `SynchedEntityData.defineId(TremorzillaEntity.class, …)` — a copy-paste in upstream 2.0.2. An
  accessor's id is allocated out of the **id tree of the class handed to `defineId`**, so the
  licowitch's accessor took slot 35 in the *tremorzilla's* tree, and from 1.20.5
  `SynchedEntityData.Builder` sizes its slot array from `ClassTreeIdRegistry.getCount(entity.getClass())`
  — which walks up from the entity's own class and stops at the first entry it finds. Both ends
  therefore break, and they break with two different messages that do not obviously belong to one
  cause: summoning a **licowitch** throws `IllegalArgumentException: Data value id is too big with 35!
  (Max is 26)` from `define`, and summoning a **tremorzilla** throws `IllegalStateException: Entity
  class …TremorzillaEntity has not defined synched data value 35` from `build()`, for the hole the
  stolen slot left in its own tree. Vanilla catches both (`Exception loading entity:` + *"Unable to
  summon entity"*), so **the server survives and logs a warning** — no crash, no failed boot, just two
  mobs that can never exist. `Builder` arrives at 1.20.5 (javap'd: absent on 1.20.4, present on
  1.20.6), and below it ids were a plain unbounded map, so the bug was genuinely harmless on the seven
  nodes ≤1.20.4 — **which is exactly why the `1.20.1-forge` battery reported 43/43 summons and looked
  like proof.** Fixed by naming `LicowitchEntity.class`; ungated, and behaviour-neutral everywhere
  because the mod is required on both sides so client and server number the tree identically.
  That audit is **`scripts/synced_data_audit.py`** now, not a promise to write one — run it before
  every release; it reports 71 declaring classes and 274 accessors and exits 1 on a gap. It asserts
  two things, and the second is the same bug approached from the other side: every
  `defineId(X.class, …)` must name the declaring class, **and** the set an entity declares must equal
  the set it `define`s, since a declared-but-never-defined accessor leaves the identical hole in the
  id tree. It compares accessors as a **set of names** rather than as a count, which is what makes it
  gate-proof: a Stonecutter-gated entity carries both the `this.entityData.define(` and the
  `builder.define(` spelling in one file, so any count double-counts. The six `mixin/**` sites that
  hand a *vanilla* class are correct by design and are excluded by path. **Sensitivity-checked** by
  restoring the upstream bug in a scratch copy — it names all six of the licowitch's accessors.

- **The rig itself has five rules, each of which cost a wasted run.** (1) **`level-type` must be
  `minecraft:normal`** — the default flat/void world has no caves, so the whole battery is a false
  negative. (2) **A dedicated server has no player, so only console-issuable commands count**:
  `locate biome`/`locate structure`/`place structure`/`setblock`/`summon`/`loot spawn`. And vanilla's
  `LocateCommand` calls `sendSuccess(…, false)`, so **a locate result appears only in the RCON reply
  and never in the server log** — RCON is not a convenience here, it is the only channel. (3)
  **`/place structure` needs the target chunk already LOADED and `forceload add` does not take effect
  within the same tick**, so spreading targets out and forceloading each in turn answers "That
  position is not loaded" *and* wedges the server generating chunks back-to-back. Place everything in
  the spawn chunks and let the structures overwrite each other — the question is whether the template
  pool resolves, not what the result looks like. Note `/place structure` also honours the structure's
  own generation predicates, so a biome-gated structure legitimately answering *"Failed to place
  structure"* at a vanilla-biome spawn is **not** a failure.
  (4) ⚠️ **`say` executes but sends NO RCON REPLY PACKET on `1.21.11-forge`, which hangs the client
  forever** — and it is the perfect trap for a sentinel command, which is exactly what it was being
  used for (`execute if entity … run say mobs-present`, to prove the summoned mobs were still alive).
  The server is *fine*: a thread dump shows the `Server thread` idle in `waitForTasks` and the `RCON
  Client` thread already looped back to `read`, i.e. it believes it answered; the log even carries
  the `[Rcon] mobs-present` line. Only the client is stuck, blocked on a read that will never return,
  with no crash, no timeout and no log line — the jsonl simply stops growing mid-file. **Bisect with a
  fresh RCON connection rather than re-running**: `list` and `time query daytime` reply normally on
  the same hung server and `say` does not, which localises it in one command. `1.21.11-fabric` and
  `1.20.1-forge` both reply to `say` normally, so it is neither a mod bug nor a general RCON one —
  and a second run reproduced it at the identical command, so it is deterministic. Use any command
  with a real `sendSuccess` as a sentinel (`time query daytime` works). The 61 remaining commands were
  then finished over a **new RCON connection to the still-running server**, which is much cheaper than
  a re-boot; the jsonl is opened line-buffered and appended to, so nothing was lost.
  (5) From 1.21.x a dedicated server pauses itself after `pause-when-empty-seconds` (default 60) with
  no players. That is *not* what caused the hang above — the second run had it disabled and hung at
  the same command — but `run_node.sh` sets it to `0` anyway so a paused world can never be mistaken
  for a stalled battery again.

- **Every section needs a vanilla control, or a rig failure is indistinguishable from a mod failure.**
  Three vanilla biomes and three vanilla structures are located first in each run; that discipline is
  the only reason "0/6 AC cave biomes, 6/6 vanilla" could be read as a mod bug on sight rather than
  as a broken world seed. Related: pin the seed (`20250819` here) so the Fabric and Forge runs are
  directly comparable — matching *coordinates* is a far stronger signal than matching counts.


### Gotchas the `/acc` + interactive client/server pass found (2026-08-20)

The first pass that ever ran a dev **client and a dev server together** — a persistent RCON server
with a dev client joined to it over quick-play — rather than booting each alone. Two of the five
below are things a solo boot cannot see at all, and one of them corrects a claim this file made.

- **⚠️⚠️ NeoForge does not merely *log* the `@OnlyIn` finding — from build `21.7.25-beta` (MC 1.21.7)
  it raises a BLOCKING MODAL, and this file asserted the opposite.** The Forge half of that gotcha
  (`RuntimeDistCleaner` throwing from 62.0.9) is recorded above and was fixed with the
  `!mc261-onlyin-forge` rule; the closing sentence of that note read *"NeoForge 26.2 is untouched too
  — it only logs the same finding through `OnlyInWarningsHandler` and boots past it"*, which is true
  of a **server** and false of a **client**. `net.neoforged.neoforge.common.OnlyInWarningsHandler`
  stops the client on *"Warning while loading mods / 1 warning has occurred during loading"*, names
  this mod, and waits for a click on **Proceed to main menu** — every launch, for every player, before
  the title screen. It is a NeoForge-**BUILD** boundary exactly like Forge's: probing all 18 cached
  universal jars gives `21.6.20-beta` → absent, `21.7.25-beta` → present, so **nine** nodes are
  affected (1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2). Fixed with a
  second replacement group, `!mc2117-onlyin-neoforge`, gated `neoforge && >=1.21.7` and rewriting the
  same 71 dist-neutral `@OnlyIn(Dist.CLIENT)` bodies to `/* client-only */`. It is a **separate
  group** from the Forge one rather than a widened condition: a node is either Forge or NeoForge so
  the two are never registered together, and keeping them apart makes the two boundaries
  independently editable. ⚠️ **The general lesson is the one this file keeps re-learning**: a
  loader's *server* behaviour is not evidence about its *client* behaviour, and a warning that a
  dedicated server prints to a log may be a modal on the other dist. Grep a loader's universal jar
  for the handler class rather than inferring the boundary from the neighbouring node.
- **⚠️ `--args=` means opposite things to loom and to MDG, and getting it wrong looks like a mod
  crash.** On a **loom** node (every Fabric and every Forge node here) `runClient` carries an *empty*
  program-args list — the real arguments live in
  `.gradle/loom-cache/projects/<node>/launch.cfg`'s `clientArgs` section (only `--assetIndex` and
  `--assetsDir`) — so `--args=…` is purely **additive** and appending `--quickPlayMultiplayer
  localhost:<port>` just works. On an **MDG** node (every NeoForge node) the whole invocation,
  *including the main class*, is read from `versions/<node>/build/moddev/clientRunProgramArgs.txt`,
  and `--args=` **replaces that entire list** — so passing only the quick-play flags makes
  `net.neoforged.devlaunch.Main` treat `--quickPlayMultiplayer` as the main class and die with
  *"Could not find main class or main method"*, `GRADLE_EXIT=1`, with no Minecraft log at all. The
  fix is to re-read that file and prepend it: `A=$(grep -v '^#' …/clientRunProgramArgs.txt | grep -v
  '^$' | tr '\n' ' ')` then `"--args=${A}--quickPlayMultiplayer localhost:25597"`.
- **A dev client with a fresh run directory stops on the accessibility onboarding screen, and
  quick-play does NOT skip it.** *"Welcome to Minecraft! Would you like to enable the Narrator…"*
  blocks before the title screen and therefore before the auto-join, so the server sits at 0 players
  while the client log looks perfectly healthy — `Sound engine started` present, zero bad markers, and
  simply no `Connecting to` line. It is `onboardAccessibility:true` in `versions/<node>/run/options.txt`,
  which the client itself writes on that very screen; flip it to `false` and relaunch. Same family as
  `eula.txt` for a dev server: **a per-node run directory needs seeding once, and the symptom of not
  seeding it is silence rather than an error.**
- **⚠️ GUI automation of the dev client is impossible on this box, and the workaround is better than
  the thing it replaces.** The client is an XWayland surface: neither XTEST (`python-xlib`) nor
  `ydotool`/uinput delivers a click or a keystroke to it, and `xdotool` is not installed. What works
  instead is to **drive everything from the server**: run a persistent dev server with RCON enabled,
  join it from the dev client with `--quickPlayMultiplayer`, and then trigger every code path with
  `execute as <player> run <cmd>` plus `op`/`deop` over RCON, reading the result off a **window-only**
  screenshot (`xwd -id <winid>` → `magick`; never a full-desktop capture). That exercises the real
  client-server pair — packets, menu sync, permission revocation — which no amount of clicking a
  single-player world would.
- **`/execute as <player> run …` changes the executing ENTITY but NOT the `CommandSourceStack`'s
  output target.** Anything the command emits with `source.sendSuccess` goes back to the **RCON
  caller**, never to that player's chat, so an `execute as` probe proves the command ran and proves
  nothing about what the player saw. Only code that resolves `source.getPlayer()` and sends to it
  directly — `CodxNotify.toPlayer(...)`, which is what `/acc version`'s async update-check callback
  uses — is verifiable client-side this way. Budget a human step for anything whose output is
  ordinary command feedback.

### Gotchas the runtime shakedown found (client boots, 2026-08-18)

- **⚠️ A verdict file written by two versions of the same sweep script is not one table, and reading
  it as one invents bugs that were never there.** Round 1 of the client sweep ran *before* the
  `grep -vF SignedJWT` filter was added to `clients.sh`, so 14 perfectly healthy nodes are recorded in
  `/tmp/acc-boot/CLIENTS` as `DIRTY badlines=1` or `2` — every one of them the benign Realms
  offline-auth line. Round 2 ran with the filter and recorded the same class of node as `PASS`. The
  two rounds sit in the same file under a header line, which makes the mixture invisible. **Never
  reconcile a sweep from its verdict column; re-derive every verdict from the LOGS under the current
  rules**, which for all 58 client logs gives **56 PASS + 2 no-boot** (both the Wayland/GLFW
  environment failure below — since closed, so the real figure is **58 PASS**) and **zero** dirty
  nodes. Corollary for writing
  the harness: a sweep script that changes its verdict rule should start a *new* results file rather
  than append to the old one.

These are the bugs that **only a dev client** surfaces. Every one of them booted a green `runServer`
on every node, which is exactly why the "test them all at the end" plan had to include clients.

- **⚠️ NeoForge 21.7 gave `PayloadRegistrar#playBidirectional` a SECOND handler, and turned the old
  three-argument form into a convenience that passes `null` for the clientbound one.** Same name,
  same arity, opposite meaning — the argument order is `(type, codec, serverboundHandler,
  clientboundHandler)`, proven by disassembling `playToClient`, which passes `null, handler` with
  flow `CLIENTBOUND`. So one unchanged source line registers **both** directions below 1.21.7 and
  **only the serverbound one** from it. It compiles on every node and no server ever notices, because
  the check that catches it runs on the client dist: `IllegalStateException: Some clientbound payloads
  are missing client-side handlers: [alexscaves:main_channel]`, thrown at load. It killed **all nine**
  NeoForge nodes ≥1.21.7 and nothing below. Fixed by hoisting the handler into a local in `ACNetwork`
  so the `!mc217-bidirectional-nf` rule can name the call's tail (`ACPayload.CODEC, handler)`) as one
  token and append the second handler. Boundary read out of every cached `neoforge-*-universal.jar`
  from `20.4.251` to `26.2.0.37-beta`: one overload up to `21.6.20-beta`, two from `21.7.25-beta`.
  General form, and the third instance of it in this file (after NeoForge's `forceChunk` boolean at
  1.21.5 and `entityInside`'s trailing flag at 1.21.10): **a loader API that keeps its signature while
  changing what an argument MEANS is invisible to the compiler and to `verify_mixins.py`.**
- **`ClientProxy#clientInit` is NOT mod construction on Forge/NeoForge, and from 26.1 that costs every
  dynamic item its model.** It is `FMLClientSetupEvent#enqueueWork`, and on 26.1 those enqueued
  tasks are pumped on the render thread **while Minecraft's first resource reload is already running
  on the workers** — so `ClientItemInfoLoader` parsed the item model definitions before
  `ACItemModelShims.register()` had put the mod's ids into the vanilla `LateBoundIdMapper`s. The
  symptom is 35 × `Couldn't parse item model 'alexscaves:<id>' … Unknown element id: alexscaves:tint |
  alexscaves:item_renderer | alexscaves:legacy`, i.e. every tinted item, every special-rendered item
  and every range-dispatched item silently falls back to the missing-model cube. **Fabric never saw
  it** — there `clientInit` really is mod-init and does precede the reload — which is why the log line
  `registered item model definition types` sits *before* `Reloading ResourceManager` on a Fabric node
  and *after* it on a Forge/NeoForge 26.1 one. That log-line ordering is the whole diagnosis; keep it.
  Fixed by moving the call to the top of `ClientProxy#commonInit`, which really does run at CONSTRUCT
  (the log line moves onto `modloading-worker-N`), joining the three listeners already registered
  there for the same "CONSTRUCT is the only point guaranteed to precede it" reason. ⚠️ **The
  boundary below 26.1 is unproven** — 26.1/26.1.1/26.1.2 are simply the first Forge/NeoForge nodes
  whose client was ever booted, and no pre-fix client log exists for 1.21.4–1.21.11, so this may have
  been broken on every Forge/NeoForge node since the 1.21.4 wave. The fix is ungated and correct on
  all of them either way; do not record a boundary that was never observed.
- **⚠️ `Sound engine started` is NOT a safe "the client booted" marker — it fires before NeoForge's
  clientbound-payload check.** `1.21.11-neoforge` reached it, was recorded **PASS**, and then died on
  the payload bug above. A client sweep's verdict needs a crash test as well as a good marker:
  `---- Minecraft Crash Report`, `Exception message:`, `FAILURE: Build failed`. And **`Failed to parse
  into SignedJWT` is benign offline-Realms noise** that matches the standard BAD regex on *every*
  client node, so it has to be filtered or every node reads DIRTY. `/tmp/acc-boot/reclass.sh` encodes
  both.
- **The Wayland/GLFW dev-client block is GLFW's PLATFORM SELECTION, and it is fixable from the
  environment — so all 58 dev clients boot.** ⚠️ This bullet read *"verify those two statically or
  on X11"* until the recipe below was found; that was a workaround written down as a limit. Forge
  builds **62.0.9 (26.1)** and **63.0.2 (26.1.1)** die on a Wayland session with
  `IllegalStateException: GLFW error before init: [0x1000C]Wayland: The platform does not provide
  the window position`, `Suspected Mods: NONE`, while **64.0.12 (26.1.2)** and **65.1.0 (26.2)**
  reach the title screen on the same box — so it does track the Forge *build*, and it is neither
  monotonic nor contiguous. The cause is that **GLFW 3.4 selects the Wayland platform whenever it
  can reach a compositor and never falls back to X11 once it has committed**, which is why the two
  obvious fixes both fail, and fail *differently*: unsetting `WAYLAND_DISPLAY` changes nothing
  (libwayland's `wl_display_connect(NULL)` still finds `$XDG_RUNTIME_DIR/wayland-0`, so the same
  crash comes back), and pointing it at a socket that does not exist turns the crash into
  `IllegalStateException: Unable to initialize GLFW` out of Forge's early display — GLFW has already
  chosen Wayland and simply stops. What works is making Wayland **undiscoverable**: a private
  `XDG_RUNTIME_DIR` that contains no `wayland-0`, with `pulse`, `pipewire-0` and `bus` symlinked
  into it so audio and dbus still work and `Sound engine started` stays a usable marker —
  `env -u WAYLAND_DISPLAY XDG_RUNTIME_DIR=<private dir> DISPLAY=:0 XDG_SESSION_TYPE=x11 ./gradlew
  --no-daemon ":<node>:runClient"`. **`--no-daemon` is load-bearing**: a running daemon hands the
  client its own environment, which is why the first attempt at this appeared to change nothing.
  Both nodes then reach `Sound engine started` with zero bad markers (2026-08-20). Not a mod bug,
  and no longer an untested node.
- **`scripts/verify_mixins.py` skipped a `@Mixin` target that arrived by WILDCARD import, silently.**
  `resolve_imports` only ever built a map from explicit `import a.b.C;` lines, so under `import
  net.minecraft.world.entity.*;` a `@Mixin(Entity.class)` resolved to the bare name `Entity`, and
  `check()` returns `[]` for any owner with no dot in it — a green pass that verified nothing. That had
  left **13 injection points on `EntityMixin`, the largest mixin in the tree, unchecked on every node
  for the whole walk**. Fixed three ways at once: `wildcard_packages()` + a `probe` callback that tries
  `<pkg>.<Spec>` (and the nested `$` form) against the node's own jars; and — the part that matters
  more than the fix — **an unresolved owner is now a loud failure** (`unresolved @Mixin target 'X' — no
  import places it`) rather than a skip. The same commit made a `@Shadow`/`@Accessor` **field's
  declared type** part of the assertion, closing the gap this file had listed as open since the
  1.21.11 wave. Per-node counts are unchanged by all three, which is the proof they add assertions
  rather than points. Lesson: **a checker that silently skips what it cannot resolve reports the
  absence of evidence as evidence of absence** — make every unresolved thing fatal.
- **⚠️ Forge does not bundle MixinExtras below build 60.1.11 (MC 1.21.10), and the two annotation
  families fail DIFFERENTLY — only one of them is loud.** Fabric Loader has bundled it since 0.15 and
  NeoForge always has, so `@Local` / `@ModifyExpressionValue` / `@WrapOperation` "just work" on 40 of
  the 58 nodes; on Forge they are compile-time only unless the mod ships the runtime itself.
  Authoritative check is the userdev `config.json` of each cached Forge jar — every build from
  **47.4.21 through 59.0.5 lists none**, and 60.1.11 / 61.1.0 / 62+ list
  `io.github.llamalad7:mixinextras-forge:0.5.3`. That is a Forge-**BUILD** boundary, not an MC one.
  - `@Local` on an `@Inject` handler is a **hard crash at mixin apply**: Mixin sees a handler
    parameter it cannot account for — *"Invalid descriptor on …client.GameRendererMixin->@Inject::
    ac_renderLevel(…CallbackInfo;Lcom/mojang/blaze3d/vertex/PoseStack;)V! Expected
    (…CallbackInfo)V"*, `Suspected Mods: NONE`. Killed the dev clients on `1.21.8-forge` and
    `1.21.9-forge`.
  - `@ModifyExpressionValue` is **SILENT**: an unknown annotation is not an injector, so the handler
    is simply never applied — no log line, no failure count. `ItemStackAttributeModifiersMixin` is in
    the **common** list, so on Forge 1.21.3–1.21.9 it had been quietly doing nothing **on servers as
    well**, which is exactly why the all-green 58-node server sweep did not catch this.
  Fix in `build.forgeg.gradle.kts` (the buildscript for every Forge node 1.20.1→1.21.11; 26.x uses
  `build.forgenr.gradle.kts` and needs nothing): keep the `compileOnly` + `annotationProcessor` on
  `mixinextras-common`, and below Forge 60 add `include(…)` **and** plain `implementation(…)` of
  `io.github.llamalad7:mixinextras-forge:0.4.1`. ⚠️ **`mixinextras-forge` is a WRAPPER** — its jar
  holds only `MixinExtrasMod.class`, a config plugin and `META-INF/jars/MixinExtras-0.4.1.jar`, so it
  has to be *loaded as a mod*, not merely be on the classpath. It is: FML's `ClasspathLocator` finds
  it in dev exactly as `JarInJarDependencyLocator` finds Forge 60+'s own copy, and the log then says
  *"Initializing MixinExtras via com.llamalad7.mixinextras.service.MixinExtrasServiceImpl"*. Do not
  reach for `modRuntimeOnly` (a pointless remap of a plain library) or for shading `mixinextras-common`.
  **How to find every affected node**: `grep -rl --binary-files=text llamalad7` over each node's
  compiled classes, then `javap -p -v` to tell `sugar/Local` from `injector/ModifyExpressionValue`.
  ⚠️ **`grep` skips binary files by default here** — a plain `grep -rl` over `.class` files reports
  "none" on all 18 Forge nodes and reads exactly like a clean bill of health.

- **`ClientBootstrap.bootstrap()` is called at a DIFFERENT MOMENT on NeoForge than on every other
  loader, and that is what makes a CONSTRUCT-time reflective read of a vanilla client holder class
  safe on 46 nodes and wrong on 12.** Vanilla calls it from `net.minecraft.client.main.Main.main()`
  **before any mod loads** — so Forge and Fabric inherit that — while NeoForge calls it from
  `Minecraft.<init>`, **after** `ClientModLoader.finish(...)`. `ACItemModelShims.register()` reads the
  three `private static final` `LateBoundIdMapper`s by reflection, and `Field.get(null)` on
  `SpecialModelRenderers`' forces that class's `<clinit>` → `BedSpecialRenderer$Unbaked.<init>` →
  `Sheets.<clinit>`. On NeoForge that lands inside mod construction, before
  `CommonModLoader.areRegistriesLoaded()`, and NeoForge logs **`Sheets loaded too early`** — 8 of the
  12 NeoForge nodes ≥1.21.4 printed it (the other four never got that far in the same sweep, for
  unrelated stale-build reasons). ⚠️ **The guard only `LOGGER.error`s, it does not throw**, so the
  client still reaches the title screen and the sweep still says PASS — but `Sheets.SIGN_MATERIALS`
  is built in that `<clinit>` and `getSignMaterial` is a bare `Map.get` with **no fallback**, so the
  mod's pewen and thornwood signs would resolve to a null `Material`. Fix: `ACItemModelShims` grows a
  `neoforge && >=1.21.4` arm, `registerNeoForge(IEventBus)`, that registers through the three mod-bus
  events NeoForge fires for exactly this — `RegisterSpecialModelRendererEvent`,
  `RegisterColorHandlersEvent$ItemTintSources`, `RegisterRangeSelectItemModelPropertyEvent`, all
  present and signature-stable 1.21.4 → 26.2 (only the `ResourceLocation`→`Identifier` rename moves,
  and passing values rather than naming types sidesteps it) — and the reflective `register()` call in
  `ClientProxy#commonInit` is gated `!neoforge && >=1.21.4`. The two special-renderer codecs go
  through **raw** `MapCodec` locals: 26.1 made `SpecialModelRenderer.Unbaked` generic, so the event's
  bound is `? extends Unbaked<?>` there and `? extends Unbaked` below it, and this mod's own
  `Unbaked` implements the raw type — a raw codec satisfies both bounds, a parameterised one does
  not. General form: **a reflective read of a vanilla holder class is a class *load*, and when that
  load happens is a loader fact, not a Minecraft one.**
- **A dev client's log always contains `Failed to parse into SignedJWT`, and it will poison any
  verdict regex built around `Failed to parse`.** It is `RealmsClient` failing to read an offline
  dev account's token — noise on all 58 nodes, on every loader. `/tmp/acc-boot/clients.sh` filters it
  with `grep -E "$BAD" | grep -vF SignedJWT`; before that filter existed, 25 perfectly healthy nodes
  were reported DIRTY with `badlines=1` or `2`. The four *server*-side markers this file lists are
  unaffected — this one only shows up client-side.
- **⚠️ The Wayland/Forge dev-client block is TWO builds, not "26.x Forge" — and this file
  asserted the wrong set twice.** The workspace notes record a 26.2 Forge dev client dying at
  `GLX._initGlfw` with *"[0x1000C]Wayland: The platform does not provide the window position"*, and
  both bullets here repeated **65.1.0 (26.2)** as blocked. The full client sweep disproves it:
  `26.2-forge` reaches `Sound engine started` with **zero** GLFW errors in its log. What is actually
  blocked on Wayland is **62.0.9 (26.1)** and **63.0.2 (26.1.1)**; **64.0.12 (26.1.2)** and
  **65.1.0 (26.2)** both boot. So it is neither "all 26.x Forge" nor monotonic nor even contiguous.
  **A build-tracking environment failure has to be probed per build**, exactly like the Forge API
  facts elsewhere in this file; inheriting one node's verdict is how the wrong set got written down.
  Both blocked builds boot fine once GLFW is kept off Wayland — see the environment recipe in the
  shakedown gotchas above. ⚠️ The crash report Forge then tries to write fails on its own (`Can't
  getDevice() before it was initialized`, `ModList.indexedMods is null`), which buries the real
  `Suspected Mods: NONE` line — read the GLFW throw, not the report generator's secondary failure.


## Where the version walk stands (2026-08-19)

**The in-world battery has run, and it is the only check so far that has ever generated a world.**
`TESTPLAN-1.0.0.md`'s first two full-pass nodes — `1.20.1-forge` and `1.21.11-fabric` — plus
`1.21.11-forge`, added because it is the cheapest Forge node *above* both the `tempt_range` and
`defineId` bug boundaries — were driven
over RCON on a pinned seed (`20250819`, `level-type minecraft:normal`, ~865 commands each: locate
biome/structure, place structure, setblock every block, summon every mob, and every loot table).
It found **three bugs that every boot-level check had passed**, all now fixed and re-verified: the
Forge game bus is never posted on Fabric (so the six cave biomes did not exist there), `TemptGoal`'s
1.21.2 `TEMPT_RANGE` attribute crashed eleven mobs on all three loaders, and one upstream `defineId`
naming the wrong class bricked the licowitch and the tremorzilla on every node ≥1.20.5. All three are
written up under *"Gotchas the in-world test battery found"* above. Post-fix, `1.21.11-fabric` reports
**43/43 summons, 43/43 loot-entity tables, 16/16 loot chests, 352/354 setblocks and a server log with
zero exceptions**, and `1.21.11-forge` reports the identical figures — the two 1.21.11 nodes are
section-for-section equal to each other and to `1.20.1-forge`, on the same seed, down to which five
`place structure` calls are refused. That last node is what actually proves the `tempt_range` and
`defineId` fixes: `1.20.1-forge` sits *below* both bug boundaries, so its green was never evidence. The two setblock misses are `cave_painting_friendship` /
`cave_painting_hunt`, orphan upstream blockstate JSONs with no registry entry — upstream's, not this
port's. ⚠️ The remaining test-plan scope is real: two of the four full-pass nodes and the six-node
smoke pass have not been run and **nothing client-side or interactive has been exercised at all**.
The 18 dead `CommonEvents` handlers this paragraph used to list as open scope were closed on
2026-08-20; see the game-bus bullet above.

**The runtime shakedown is COMPLETE: all 58 dev CLIENTS boot clean.** Re-deriving every verdict from
the 58 logs in `/tmp/acc-boot/client/` under one rule set (GOOD = `Sound engine started|OpenAL
initialized`; BAD = the ten fatal markers minus `SignedJWT`; plus `Minecraft Crash Report|FAILURE:
Build failed|GLFW error before init`) gives **56 PASS, 0 DIRTY, 2 NOBOOT** — `26.1-forge` and
`26.1.1-forge`, both `GLFW error before init: [0x1000C]Wayland`, `Suspected Mods: NONE` — and those
last two were an **environment** failure, not the mod: re-run on 2026-08-20 with GLFW kept off
Wayland (the private-`XDG_RUNTIME_DIR` recipe in the gotchas above) each reaches `Sound engine
started` with zero bad markers, making it **58 PASS**. The two client-only fixes are confirmed at runtime across
their whole affected band, not just the node they were written against: `Sheets loaded too early` is
absent from **all 12** NeoForge nodes ≥1.21.4 with `registered item model definition types` landing on
`modloading-worker-0` *before* `Reloading ResourceManager` on every one, and `1.21.8-forge` /
`1.21.9-forge` now load `mixinextras-forge-0.4.1.jar` via `ClasspathLocator` + the nested
`MixinExtras-0.4.1.jar` via `JarInJarDependencyLocator`, with zero injection errors.


**Ship-readiness re-verification, 2026-08-19, on `MOD_IS_RELEASE=true` and version `1.0.0`.** The
closing build was re-run as a *release* build so it doubles as the artifact build rather than costing
a second pass, and all four checks are green against those artifacts rather than against snapshots:
`BUILD SUCCESSFUL`, `GRADLE_EXIT=0`, 727 actionable tasks, **zero** task failures across all 58;
`verify_mixins.py` **16433 injection points, all targets resolve**; `aw_check.py` **problems=0** on
all 22 MC versions (83 entries at 1.20.1 falling to 70 at 26.2); `convaudit.py` **missing=0** on all
22 Fabric versions. `versions/*/build/libs/` holds **58** mod jars, every one
`alexscaves-1.0.0-<loader>+<mc>.jar`, with **zero** `-SNAPSHOT` — plus 58 sources and 58 javadoc jars
that any uploader must filter out (see the release-build gotchas above).

**Green: all 58 nodes — the whole planned matrix.** The wave-closing all-node `--continue` build is
**confirmed** — `BUILD SUCCESSFUL in 26m 48s`, 727 actionable tasks (550 executed, 177 up-to-date),
zero task failures across all 58
(`TASKS=(${(f)"$(ls versions/ | sed 's|^|:|; s|$|:build|')"})` then `./gradlew "${TASKS[@]}" --continue`
— zsh does not word-split, so this has to be an array, and it has to be **one** invocation).
`python3 scripts/verify_mixins.py` with no arguments resolves **16433 injection points across all 58
nodes**, and `python3 scripts/aw_check.py` with no arguments reports **problems=0** on every one of
the 22 MC versions (83 entries at 1.20.1–1.20.4 falling to 70 at 26.2, as gated arms drop out).
⚠️ **`aw_check.py` takes MC VERSIONS, not node names** — handed `26.1-fabric` it prints "no cached
vanilla Mojmap jars … skipping", which reads exactly like a missing build.

Everything below this paragraph predates the Fabric milestone and is about the 36 Forge/NeoForge
nodes; the Fabric numbers and per-node sign-off live in `docs/notes/fabric.md`. Of the 36, `1.21.11-forge`, `1.21.11-neoforge`,
`1.21.10-forge`, `1.21.10-neoforge`, `1.21.9-forge`,
`1.21.9-neoforge`, `1.21.8-forge`, `1.21.8-neoforge`, `1.21.7-forge`, `1.21.7-neoforge`, `1.21.6-forge`,
`1.21.6-neoforge`, `1.21.5-forge`, `1.21.5-neoforge`, `1.21.4-forge`, `1.21.4-neoforge`, `1.21.3-forge`,
`1.21.2-neoforge` and `1.20.1-forge` boot their dev servers to `Done` with none of the four fatal log
markers. Note that a `stonecutter.gradle.kts` edit only invalidates the nodes whose *effective* rule
set changed — that is Stonecutter's own verdict, computed against the edited script, not a
stale-green assumption (the 1.21.10 wave made an existing rule's target version-dependent, a no-op
below 1.21.10, so 24 nodes stayed legitimately UP-TO-DATE). A **`build-logic`** edit invalidates
*everything*, which is what the 1.21.11 wave did, so its closing build was a genuine 28-node
re-verification rather than a formality — and it earned its keep: it caught a one-line regression in
`ACClientCompat` that had compiled on all 23 nodes ≥1.21 and broke all five below (see the wave notes).
⚠️ **Every absolute count in this section predates the `MEMBER_ANNO` fix** (gotcha (4) above), which
added 127 previously-invisible `@Shadow`/`@Accessor`/`@Invoker` points spread over all 37 nodes. The
current headline is **9243 across 37 nodes** (36 Forge/NeoForge + `1.20.1-fabric`'s 317); the
per-wave arithmetic below is still correct *relative to itself*, since the fix shifted every node.

`scripts/verify_mixins.py` → **8818 injection points across the 36 nodes, all resolving**
(`26.2` reports **245** on Forge and **240** on NeoForge; `26.1`/`26.1.1`/`26.1.2` report 246/240 and
`1.21.11` 244/241). Those last two pairs are each **one lower than this file recorded before the 26.2
wave**, and deliberately so: deleting the vendored-Citadel `LevelRendererMixin`'s `@Shadow minecraft`
took exactly 1 off **every one of the 34 older nodes**, which is what makes the total checkable —
`8367 − 34 = 8333`, plus 26.2's `245 + 240 = 485`, is **8818** exactly, i.e. not one other injection
point moved anywhere in the tree. A *rising* count
after a checker or source change is the
signal that something previously unverified is now covered, and a *falling* one wants the same
scrutiny — watch the per-node numbers, not just OK/FAIL. The 1.21.7 wave's +16 was the rising case:
hoisting the prose out of `SplashRendererMixin`'s middle arm made two injections legible to the checker
on eight nodes where the mangled arm had hidden them. The 1.21.9 wave's −1 on every `>=1.20.5` node was
the falling case, and equally intended: the map mixins that have been dead since 1.20.5 are pruned now.

**Recipe/advancement counts are per MC version, not a constant to check against.** 1.20.1→1.21.3 all
print **1804 recipes / 1594 advancements**; both 1.21.4 nodes print **1837 / 1627**, both 1.21.5
nodes **1840 / 1630**, all six 1.21.6/1.21.7/1.21.8 nodes **1874 / 1666**, the four 1.21.9/1.21.10
nodes **1928 / 1720** and both 1.21.11 nodes **1937 / 1730**, because vanilla ships more of both.
Compare a node against its own MC version, never
against the last wave's numbers — but *do* compare it against its own sibling loader, which is how the
1.21.9 chain rename was caught: a node that silently drops four data files still reaches `Done`, and
the only tell is three recipes missing from the count.

Log lines that are **not** problems: NeoForge's dev-only `TagConventionLogWarning` "Legacy Tags
detected" (this tree's generated resources contain zero `forge:` ids — it is not us); `Detected
minecraft:frog … registered with CREATURE … added under ALEXSCAVES_CAVE_CREATURE` (an upstream design
choice); and on a dedicated server the wall of `RuntimeDistCleaner` "invalid dist" ERRORs with the
matching `@Mixin target … was not found` warnings, which is just the client mixins being skipped.
**Never `head -N` a boot-log error grep** — that noise is long enough to hide a real `Couldn't parse`
line, which it did for two full boot cycles.

**A crashed dev server keeps holding `versions/<node>/run/world/session.lock` and port 25599**, so the
next `runServer` dies with `DirectoryLock$LockException: … already locked (possibly by other Minecraft
instance?)` before any mod code runs — a verdict about nothing. `pkill -f` is unreliable here (its
pattern matches the shell running it); take the pid from `ss -lntp | grep 25599` and `kill -9` it.

**Next up: the runtime shakedown, then publishing.** The version walk itself is **complete** — 58 of
58 nodes, 1.20.1 through 26.2, all three loaders. Two predictions this section used to carry were
resolved by the Fabric milestone and are recorded here so they are not re-derived: the missing
`EntityFluidInteraction` below 26 became `EntityMixin`'s `fabric && <26` `ac_pushInModFluids` arm,
which retires at 26 when vanilla grows the same hook; and `SurfaceRules$Context`'s constructor needs
**no** access-widener line — the three `>=26.2` `@Invoker`s resolve on Fabric exactly as on the other
two loaders.

The 26.1 and 26.2 waves are finished; the 1.21.5 wave's five `>=1.21.5`
`DataPackMigration` passes have all really run and report their expected counts on every node from
1.21.5 up: **43** spawn eggs, **1** advancement background, **7** post chains, **6** biomes,
**1** trim recipe — and the 1.21.11 biome-attributes pass reports **6** on all eight nodes ≥1.21.11.

### What the 26.2 wave cost (immediate mode is gone, and a sealed `Holder` reads the mod's old bugs back)

Two nodes — `26.2-forge` (Forge **65.1.0**) and `26.2-neoforge` (NeoForge **26.2.0.35-beta**) — and the
last wave of the Forge/NeoForge walk. It is a **larger** wave than 26.1 by rule count (**71** `!mc262*`
replacement rules against 26.1's 38) and by the number of subsystems that were deleted rather than
renamed, but it produced no new *kind* of trap: everything below is an instance of a lesson this file
already records. `verify_mixins.py` reports **245 / 240** and both nodes build clean.

**Immediate-mode rendering was deleted outright, and that is the wave's centre of gravity.**
`Minecraft#renderBuffers()` is gone, `MultiBufferSource` and `VertexMultiConsumer` no longer exist, and
`RenderType#draw(MeshData)` went with them — a render type hands out a `PreparedRenderType` and the
caller draws GPU buffers itself. This mod draws by hand in ~40 places, so the two interfaces are
**vendored** (`client/render/compat/MultiBufferSource.java` and friends), on the same precedent as the
Citadel classes: a type the mod needs on every node, supplied by vanilla below 26.2 and by the mod
above it. What cannot be vendored is the *destination*: a draw now has to reach the frame's
`SubmitNodeCollector`, which nothing hands to a mod. `client/render/compat/ACRenderContext` makes it
**ambient** — pushed for the length of `LevelRenderer#submitFeatures` and popped after. Two consequences
worth stating out loud, because neither is visible at a call site:

- **The four render stages collapse onto one moment in the frame.** AFTER_SKY, the block layers and
  AFTER_ENTITIES were four distinct anchors as recently as 1.21.5; from 1.21.6 they were two; on 26.2
  they are all one `submitFeatures` injection, since that is the only window in which a collector
  exists. `ChunkSectionsToRenderMixin` is therefore excluded on 26.2 (its `renderGroup` anchor has
  nothing left to contribute) and `LevelRenderStageMixin` drops from two injections to one.
- **A draw made with no collector pushed is silently discarded.** No exception, no log line. Any new
  hand-rolled draw on 26.2 must run inside the ambient window or it renders nothing.

**26.2 SEALED `Holder`** (`javap -v` shows flags `0x0601` and a `PermittedSubclasses` attribute on both
loaders), and that is the good kind of break: it made javac reject two **latent upstream bugs** that had
compiled silently since 1.20.5. `ACCompat` compared a `Holder<MobEffect>` against a `MobEffect` by
reference — always false, so seven magnetizing/irradiated immunities have never worked — and downcast a
`Holder` to a concrete effect class, which threw `ClassCastException` inside
`DarknessIncarnateEffect#getIntensity`. Routing both through the existing `vanillaEffect` unwrap fixes
them **on every node from 1.20.5 up**, not just on 26.2. Worth remembering as a category: *a language
restriction added in a new version is a free audit of every place the old version let you be sloppy.*

**The nine `verify_mixins.py` problems, and what each one turned out to be.** All nine were real API
moves; none was a checker artefact.

| target | 26.2 finding | fix |
|---|---|---|
| `Entity#move` → `Block#updateEntityAfterFallOn` | Hook deleted (from `SlimeBlock`/`BedBlock` too) and folded back into a private `Entity#restituteMovementAfterCollisions(BlockState,ZZ,Vec3)` called from the same place | three-way flat `at = {…}` arm chain + a widened-guard correction (below) |
| `MinecraftServer#<init>` | gained a trailing `NotificationManager` | `>=26.2` arm restating the 11-arg list |
| `AdvancementTab#extractTooltips` | dropped to `(GuiGraphicsExtractor,II)V`; the hover scan moved into `tick(int,int)` behind a new `hovered` field | `>=26.2` arm: `@Shadow hovered` + HEAD of the 3-arg form, no scan |
| citadel `LevelRendererMixin` `@Shadow minecraft` | field gone | shadow **deleted outright** on every node; both uses go through `Minecraft.getInstance()` |
| citadel `LevelRendererMixin` `initOutline()V` | gone with the class's reload-listener role | carved into `PostEffectInitMixin` (gated `@Mixin`, `ShaderManager#apply` TAIL from 26.2) |
| citadel `LevelRendererMixin` `renderLevel(…)` | renamed `render(…)`, lost the `ChunkSectionsToRender` param | new `>=26.2` descriptor arm |
| `RenderType#draw(MeshData)` | deleted; the colour modulator moved into `prepare()` → `writeDynamicTransforms` | `>=26.2` `@Redirect` swapping `DynamicUniforms.writeTransform` overloads (below) |
| `Minecraft#useShaderTransparency()Z` | moved to `GameRenderState` as an **instance** method; caller set unchanged | new `ShaderTransparencyMixin` (gated `@Mixin` **and** gated handler static-ness) |
| `LevelRenderer#cullTerrain(Camera,Frustum,Z)` | gone; `private void repositionCamera(CameraRenderState)` is `render()`'s first act, called unconditionally | `>=26.2` arm at HEAD of `repositionCamera` |

Four of those repay a closer look.

**⚠️ `restituteMovementAfterCollisions` has a NeoForge-only extra parameter *and* a widened guard, and
the second is the dangerous half.** NeoForge patches a leading `BlockPos` onto the method and `move()`
calls **that** overload, leaving the vanilla-shaped four-argument one behind as a delegate nothing
invokes — so the `@At` target is per loader, since a call-site match is by descriptor, and injecting the
vanilla spelling on NeoForge would succeed and never run (the exact failure mode the 26.1 wave's
`ServerClockManager` note describes). The guard is the part a checker cannot see: through 26.1 the call
was made under `canSimulateMovement() && vec3.y != vec32.y` — "this step asked for a y it did not get",
i.e. a landing — and 26.2's is `canSimulateMovement() && ((flag && verticalCollision) ||
horizontalCollision)`, which **also fires on a purely horizontal collision**. The rainbounce boots would
have bounced off walls. `verticalCollision` is the same statement about the same step the old guard
made, so the missing half moves into the handler as a `>=26.2`-gated early return. **When an anchor's
enclosing condition changes, re-derive the guard — the injection resolving says nothing about it.**

**A deleted method whose behaviour survives as a *default argument* is a choice of overload, not a
constant to modify.** `RenderType#draw` used to build a `new Vector4f(1,1,1,1)` modulator that
`CompositeRenderTypeMixin` overwrote with a `@ModifyExpressionValue`. On 26.2 the modulator is written
by `DynamicUniforms#writeTransform(Matrix4f, Matrix4f)`, the two-argument overload that fills in
private `WHITE`/`NO_OFFSET` constants before delegating — there is no `new Vector4f` left to modify. The
hook becomes a `@Redirect` that calls the four-argument form instead when a tint is pending. Both
overloads build the same record, so the untinted path is byte-for-byte vanilla.

**`ShaderTransparencyMixin` is the third use of the gated-`@Mixin` shape, and the first that also gates
handler static-ness.** `Minecraft#useShaderTransparency()` was `private static` on 1.21.11–26.1.x and is
an instance method on `GameRenderState` from 26.2; Mixin matches a handler's static-ness against its
target's, so the arm chain carries `private static` in the middle arm and an instance method in the top
one, with **no** arm at all below 1.21.11 (where `ACClientCompat#runAsFancy` still flips the real
option). Prefer this over `vanishedMixins`: a gated target needs no `build-logic` change.

**Deleting a `@Shadow` is the cheapest of the nine fixes and the one that moves every node's count.**
The vendored Citadel `LevelRendererMixin` shadowed `minecraft` purely to reach
`getMainRenderTarget()`; 26.2 deleted the field, and `Minecraft.getInstance()` answers the same question
on all 36 nodes, so the shadow came out **unconditionally** rather than being gated. That is the −1
on every older node, and the arithmetic above is what proves it is the *only* thing that moved.

**Per-file deltas, 26.1.2 → 26.2** (Forge 246 → 245, NeoForge 240 → 240):

| file | Forge Δ | NeoForge Δ | why |
|---|---|---|---|
| `AdvancementTabMixin` | +1 | +1 | the new `@Shadow hovered` |
| `DecoratedPotPatternsMixin` | +1 | +1 | the new `itemToPatternMappings` TAIL inject |
| `ChunkSectionsToRenderMixin` | −1 | −1 | excluded — every stage collapses onto one moment |
| `LevelRenderStageMixin` | −1 | −1 | same collapse, on the surviving mixin |
| `GuiRendererMixin` | −1 | 0 | the buffer source leaving `GuiRenderer` (Forge-only arm) |

**The renames and small moves, for grep value.** `Gui` → `Hud`. `Tuple` and `FlyingAnimal` deleted.
`Sheets#addWoodType` deleted. `RenderPass.draw`'s arguments inverted in order (same types — so it
compiles either way, which makes it exactly the shape of trap the NeoForge `forceChunk` boolean was in
1.21.5; read the patched source, do not trust the signature). `TextureTarget` wants an explicit
`GpuFormat.RGBA8_UNORM`. `Optional<Vector4fc>` replaced `OptionalInt` in the colour APIs.
`GuiRenderer` and `PictureInPictureRenderer` lost their buffer source. `BlockEntityType`'s and
`EntityType`'s `public static final` constants moved into `BlockEntityTypes`/`EntityTypes` holder
classes — the rename a `replacements.string` rule **cannot** express (old name is a prefix of the new
one, and the type keeps its spelling), so those go through `BuiltInRegistries.*.getOptional(Ids.vanilla(…))`
exactly as this file predicted before the wave. The sixteen dyed concretes and the weathering-copper
constants collapsed into `ColorCollection`. `RenderPipelines`' snippets changed shape.
`DecoratedPotPatterns`' mutable `ITEM_TO_POT_TEXTURE` map became
`itemToPatternMappings(BiConsumer)` — hence the new `@Inject` at TAIL, in a mixin that is listed
unconditionally and is simply empty below 26.2, the same harmless shape as `SurfaceRulesContextAccessor`.
`StructureProcessor` became an interface carrying its own `MapCodec`.

**`SurfaceRules$Context` moved three members at once, and an access transformer is deliberately not
used for them.** `getBiome()` is new, `updateY` lost its x/z, and the constructor takes a
`Set<Holder<Biome>>` where it took a biome `Registry` — all three reached through `@Invoker`s in a
`>=26.2` arm. An AT entry would be tempting (it needs no gate) and is wrong here: **an AT line that
matches nothing is a silent no-op on Forge but a hard error on NeoForge**, so one shared Mojmap file
naming a member that exists on only some nodes cannot work. ⚠️ **This paragraph used to predict that
Fabric would need the access-widener equivalent for the constructor; it does not** — all three
`@Invoker`s resolve there too, and `SurfaceRulesContextAccessor` contributes the same `+3` on
`26.2-fabric` that it does on `26.2-forge`.

**Still not done on any 26.x node: `runServer` and `runClient`.** Every verdict in this wave and the
last is `compileJava` + `verify_mixins.py`, per the standing "test them all at the end" plan.

### What the 26.1 wave cost (six nodes at once, and three subsystems that were replaced rather than renamed)

Six nodes in one wave — `26.1`, `26.1.1`, `26.1.2` × Forge/NeoForge — pinned Forge **62.0.9** /
**63.0.2** / **64.0.12** and NeoForge **26.1.0.19-beta** / **26.1.1.15-beta** / **26.1.2.87**.
`verify_mixins.py` reports **247** on each Forge node and **241** on each NeoForge node against
1.21.11's 245 / 242, every delta accounted for per file below. Half the wave is a rename sweep of the
same shape as 1.21.11 (the GUI's `render*`/`draw*` chain became `extract*`, ~15 package moves,
`getLightColor` → `getLightCoords`, `DimensionDataStorage` → `SavedDataStorage`, the `PathType`
constants) and needs nothing said about it beyond the rules in `stonecutter.gradle.kts`. The other half
is three subsystems that were **replaced**, where the old call has no successor to rename it to.

**⚠️ CORRECTION to what this file predicted before the wave: 26.1, 26.1.1 and 26.1.2 are NOT
API-identical.** That was written as a known fact and it is wrong — the *loaders* move inside the
range even where vanilla does not, and every divergence found is NeoForge's:

| gate | what moved |
|---|---|
| `neoforge && >=26.1.1` | `ServerClockManager$ClockInstance#tick()` → `tick(boolean)` (below) |
| `neoforge && >=26.1.2` | `IForgeRailBlock#getRailMaxSpeed` deleted (`MagneticLevitationRailBlock`) |
| `neoforge && >=26.1.2` | `IGlobalLootModifier` gained an abstract `int priority()` (`DEFAULT_PRIORITY` = 1000) — `CaveTabletLootModifier`, `CabinMapLootModifier`, `ACCompat` |

Each still declares an **exact** MC range, as planned. Treat "these point releases are the same" as a
hypothesis to disprove per node, never a reason to skip one.

**⚠️ From MC 26, Forge ships official (Mojmap) names rather than remapping — so a synthetic lambda is
javac's own name again on both loaders.** `lambda$addMainPass$0` and `lambda$addSkyPass$0`, where
1.21.9–1.21.11 Forge spelled them `method_62214` / `method_62215`. That makes the `method_622…` arms a
**closed window**, which is why they are the only ones in those chains carrying an upper bound. It also
means 26 can name the lambda **literally** instead of matching it with a regex, and that is worth doing
on its own account: **`verify_mixins.py` can assert that a named selector exists and can assert nothing
whatsoever about a regex one**, so every `method = "/lambda\$…/"` arm is a hole in the checker. The
Forge-side −1 in the table below was exactly that hole opening, and it is closed again by spelling the
name out. Prefer a literal selector wherever the bytecode makes one possible.

**`Camera#setup` is deleted, and its replacement is two methods, only one of which is the right
anchor.** `update(DeltaTracker)` took over the outer half (fov, hud fov, cull frustum, perspective —
none of which `setup` ever did); the private **`alignWithEntity(float partialTicks)`** does the
placing and orienting that `CameraMixin` cares about. Injecting at `update`'s TAIL would compile and
be **one frame stale in the culling**: javap puts the `alignWithEntity` call at offset 90 and
`prepareCullFrustum(Matrix4fc, Matrix4f, position)` at 130, so a position written after `update`
returns is not the one the frustum was built from. The three arguments `setup` used to hand in are all
still readable — `entity` and `detached` as fields (two new `>=26` `@Shadow`s, the `+2` in the table),
`mirrored` off `options.getCameraType()`, which is where `setup`'s caller read it from anyway.

**Day time left `ServerLevel#tickTime` on BOTH loaders, into a `WorldClock`.** Until 26 it was a second
counter stepped beside the game time, and Citadel's CELESTIAL tick-rate modifier scaled the `1L` it
stepped by (`ServerLevelMixin`'s `@ModifyConstant`, plus a NeoForge-only `advanceDaytime`
`@ModifyExpressionValue` since 1.21). From 26 a level's sky reads a clock from the world's
`ServerClockManager`, whose per-clock instance holds a `float rate` and a `partialTick` accumulator and
adds whole ticks out of it. So the modifier belongs on the **rate**, which is both simpler and strictly
better behaved than what it replaces: a rate below 1 slows the sky smoothly where `getDayTimeIncrement`
could only drop every n-th tick. New `citadel/ServerClockInstanceMixin` + `client/citadel/
ClientClockManagerMixin`; the two scale independently and only the base rate travels over the wire, so
they cannot compound. `ServerLevelMixin`'s `@ModifyConstant` drops to `expect = 1` on 26 (the `1L` is
loaded once now) and the `advanceDaytime` arm is gated off — that is the NeoForge-only `−2 / −2` below.

- **⚠️⚠️ …and NeoForge's per-clock patch tracks the NeoForge BUILD, not the MC version.** Vanilla (so
  Forge, and Fabric later) tests the ADVANCE_TIME game rule once in `ServerClockManager#tick` and gives
  the instance a bare `tick()`; NeoForge moved that test per-clock so it can honour its own
  `ignores_advance_time_rule` tag, and its instance takes `tick(boolean)` with the no-arg one left
  behind as a deprecated delegate **that nothing calls** — injecting into the vanilla spelling on
  NeoForge succeeds and never runs. But **26.1.0.19-beta ships only the vanilla shape**;
  26.1.1.15-beta is the first build with the overload. Written as `neoforge && >=26.1.1` only because
  the pin table fixes one build per node; **on a pin bump, javap the class rather than trusting the
  predicate.** `verify_mixins.py` caught this and nothing else would have — it is a silent no-op at
  runtime, not a crash.

**`LightTexture` was split, and the two halves went to different places.** The instance half (the
texture, its lifecycle, the per-frame upload) kept the class and was renamed **`Lightmap`**; the static
bit-packing helpers moved out to **`net.minecraft.util.LightCoordsUtil`**, which is not even a client
class any more; and everything that used to be computed inline in `updateLightTexture` is now a
`LightmapRenderState` of public fields filled by **`LightmapRenderStateExtractor`**. A bare-token rule
cannot express a one-token split into two destinations — and could not be written at all here, since
the token also spells `LightTextureMixin`'s own class name (Stonecutter can never rewrite that, the
filename being fixed) and the `updateLightTexture` field. Both static call sites are therefore fully
qualified in source and matched whole. `LightTextureMixin` itself is excluded from the source set and
pruned from the mixin config on 26 (`−11`), replaced by `LightmapMixin` + `LightmapRenderStateExtractorMixin`;
`ACLightmapAdditions` holds what all three share. Two consequences: **`ac_lightmap.fsh` is dead from
26**, and the `UseBrightLightmap` End special-case goes with it.

**Villager trades became datapack registry entries.** There is no `VillagerTrades.ItemListing` to
implement and no loader event to add one from, so the two underground-cabin-map trades ship as
`data/alexscaves/villager_trade/*.json` pulled into vanilla's `cartographer/level_2` and
`wandering_trader/common` trade tags. `ACVillagerTradeEvents` and `VillagerUndergroundCabinMapTrade`
are excluded from the source set on 26; below it, `DataPackMigration.dropVillagerTradeData` removes the
four files (two trades + the two vanilla trade tags naming them) so the old nodes ship no dead weight.
This is the **exclude-both-ways** shape, and it is the right one whenever a feature exists on every
node but through two mechanisms that share nothing.

**A `replacements.string` guard rule has to START EARLIER than the rule it is protecting.** `GuiGraphics`
→ `GuiGraphicsExtractor` is a bare-token rule (one rule then also covers the 13 slash-form descriptors
inside mixin selectors), but **both** loaders still spell the overlay-event accessor `getGuiGraphics()`
on 26.1 — each declares `public GuiGraphicsExtractor getGuiGraphics()` — so the bare rule would rewrite
the *method* name at its 12 call sites and nothing would resolve. The fix is a rule declared **first**
that widens the span to include `get`: `replace("getGuiGraphics(", "getGuiGraphics (")`. Since the
earlier-*starting* rule consumes the span, the bare one never sees those offsets, and the inserted space
is legal Java. Same trick scopes `renderLabels` (`CaveMapRenderer` has an unrelated one) and
`root.draw(` (kept off `root.drawConnectivity(`). This is CleanHUD's collision in mirror image — there
it arrived through the reverse pass, which a Kotlin-`if`-guarded group like these never gets.

**Per-file deltas, 1.21.11 → 26.1.x** (Forge +2 → 247, NeoForge −1 → 241):

| file | Forge Δ | NeoForge Δ | why |
|---|---|---|---|
| `LightTextureMixin` | −11 | −11 | excluded + pruned from 26 |
| `LightmapMixin` | +1 | +1 | `getBrightness`'s new owner |
| `LightmapRenderStateExtractorMixin` | +1 | +1 | the render-state edit that replaces the custom shader |
| `ItemStackRenderStateMixin` | +3 | +3 | new — republishes the `ItemDisplayContext` 26 dropped from `SpecialModelRenderer#submit` |
| `CameraMixin` | +2 | +2 | the two `@Shadow`s `alignWithEntity` needs |
| `EntityMixin` | +2 | +2 | the `EntityFluidInteraction` `@ModifyArg` |
| `ServerClockInstanceMixin` | +2 | +2 | new — the world clock |
| `ClientClockManagerMixin` | +2 | +2 | new — its client half |
| `ServerLevelMixin` | 0 | −2 | the `advanceDaytime` arm the clock replaces |
| `ClientLevelMixin` | 0 | −2 | same |
| `client/citadel/LevelRendererMixin` | 0 | +1 | the regex selector became a literal (Forge lost `method_62214` and gained `lambda$addMainPass$0`; NeoForge only gained) |

**Still not done on any 26.x node: `runServer` and `runClient`.** Every verdict in this wave is
`compileJava` + `verify_mixins.py`, per the standing "test them all at the end" plan.

### What the 1.21.11 wave cost (a rename wave, and the exact limits of Stonecutter's two tools)

The largest wave of the walk by files touched and the smallest by ideas. `ResourceLocation` →
`Identifier`, ~37 package moves, `ResourceKey#location` → `#identifier`, `Entity#hasImpulse` →
`#needsSync`, `Camera`'s getters de-`get`-ed, `GameRules` reshaped from a bag of nested types into
`GameRule<T>`, and `RenderType` split into `RenderType` + `RenderTypes` in a package of its own.
Pins: `1.21.11-forge` = Forge **61.1.0** (deliberately, not a later 61.1.x — later builds patch
`ByteBufCodecs` inconsistently with loom's merged jar and the dev server dies at vanilla startup),
`1.21.11-neoforge` = NeoForge **21.11.44**. 34 source files carry a `>=1.21.11` gate and the
`>=1.21.11` `replacements` block is by far the longest in `stonecutter.gradle.kts`. Both dev servers
boot to `Done` with **1937 recipes / 1730 advancements** — a new per-MC-version pair, so do not
compare it against 1.21.10's 1928 / 1720.

Because almost everything was a rename, the wave's real cost was learning where each of Stonecutter's
two tools stops working, and what to reach for instead.

**Stonecutter does not nest gates, and there is a preference order for what to do about it.** A
`//? if >=1.21.11` line placed inside an existing `//? if >=1.21.5 {` arm leaves *both* arms
commented out and the enclosing method silently loses its signature. In rough order of cost:

1. **Hoist the divergent expression into a helper** that lives in its own top-level gate chain, and
   call it from inside the arm. `ACClientCompat#setLineWidth` / `#setColorPacked` / `#renderLineBox`
   are all this shape.
2. **Declare the method unconditionally, without `@Override`**, and route the difference through
   that helper. 1.21.11 promoted `VertexConsumer#setColor(int)` and added `#setLineWidth(float)` as
   *abstract* methods; the three hand-written consumers in this tree each live inside an arm of
   their own, so they answer both on every node — an extra public method is harmless below 1.21.11,
   and omitting `@Override` is what makes that legal.
3. **A `replacements.string` rule**, when the difference is one token *inside* an arm. That is why
   `!mc2111-compositestate-type` and `!mc2111-drawcollector-hitbox` are rules rather than gates.
4. **Duplicate the whole method into every arm** — last resort, and the only option when an arm
   chain would otherwise orphan a shared body (below).

**Three more arm mechanics, each of which cost a compile:**

- **An arm may be deliberately empty**, which is how a method or an injection is *deleted* on one
  band without touching the other. `SplashRendererMixin`'s `@ModifyConstant` and `ClientLevelMixin`'s
  `ac_getSkyColor_timeOfDay` both end in an empty `>=1.21.11` arm.
- **A gate can ride inside an annotation.** `SkyRendererMixin`'s two `@ModifyVariable`s gate only
  `method` + `ordinal` while `at` / `argsOnly` / `remap` stay shared — much smaller than duplicating
  the annotation, and it keeps the two halves impossible to drift apart.
- **…but an arm chain of annotation-only arms cannot then host a complete method**, because the
  shared body sits after the chain and belongs to whichever arm is active. Where the *body* also
  differs, either hoist it into a gated helper or duplicate it whole into every arm
  (`SkyRendererMixin#ac_skyDiscColor` took the second route).
- **Prose still cannot live inside an arm** — the rule from the 1.21.6 wave, unchanged and hit
  again. Commentary goes above the `//? if`.

**`replacements.string` is plain substring matching, boundary-checked on NEITHER edge.** The older
note in this file claimed "right edge only"; that is wrong, and this wave disproved it twice — a
rule on `ResourceLocation` fires inside `ModelResourceLocation` *and* inside this tree's own
`ACResourceLocations` (since renamed `ACIdFactories` for exactly that reason), and
`RenderType.entityCutout` fires inside `entityCutoutNoCull`. The `entityCutout` case is harmless
only because the prefix rule leaves the tail alone and `RenderTypes.entityCutout` + `NoCull`
reassembles into what the longer rule would have produced anyway; **that is not a general licence** —
a pair whose replacements disagree on the shared prefix must be spelled so neither can start at the
same offset. Related: **two rules whose matches overlap do not both apply**; the earlier-*starting*
one consumes the span.

**A package-move rule cannot reach a type that arrived by wildcard import.** `import
net.minecraft.client.renderer.rendertype.*;` is what `!mc2111-rendertype-import` produces, and it is
deliberate: one rule then serves every class in the new package. Safe here only because none of the
ten affected files' other wildcard imports declares any of that package's six types — check that
before copying the trick.

**Rules do not chain, so a second band's change to a span an existing rule already owns goes INTO
that rule as a version-dependent Kotlin `val`.** Six of them now exist (`keyAccessor`, `glintOwner`,
`lineBoxOwner`, `linesOwner`, `lineBoxPose`, `fluidCutoutLayer`) — this is the same conclusion the
1.21.10 wave reached, reached five more times.

**A `replacements { }` block guarded by a Kotlin `if (eval(current.version, …))` has no reverse
pass.** Bidirectionality applies to a rule whose *condition* is false, not to a block that was never
registered — and this tree writes every group the second way. So **source is authored in the OLDEST
spelling and rewritten upwards**, always. A new `ACClientCompat` helper written in the modern
`VertexConsumer` DSL compiled on all 23 nodes ≥1.21 and broke all five below it, on one line. That is
what the wave-end all-node build is for; nothing else would have caught it.

**⚠️ An access-transformer entry is NOT preprocessed, and one that matches nothing is a silent
no-op** — which is what makes it safe to leave both eras' entries side by side in one file, and also
what makes a wrong one so quiet. **A package move makes a class NEW to MCPConfig, so every one of its
members is assigned a fresh SRG id too.** `AbstractArrow` moved to
`net.minecraft.world.entity.projectile.arrow`, and re-spelling the class while keeping `m_36799_`
matches nothing; `startFalling()V` is **`m_439507_`** on 1.21.11. The failure surfaces as a bogus
*"does not override"* compile error on the **mod's own** class, which sends you looking in entirely
the wrong place. Lookup route: `~/.gradle/caches/fabric-loom/<mc>/srg/<mcp-ver>/srg.tsrg` for the
obf↔SRG pair, cross-referenced with `~/.gradle/caches/fabric-loom/<mc>/forge/mojmap.tsrg2` for the
Mojmap name.

**⚠️ `scripts/verify_mixins.py` asserts a shadowed FIELD's existence but never its type.**
`parse_members` marks a `@Shadow` field (and an `@Accessor`) `name_only_field=True` and never turns
the declaration's Java type into a descriptor, so the `field_desc` comparison two branches down is
dead for them — a shadowed method's parameters *are* mirrored, a shadowed field's type is not.
1.21.11 retyped `SplashRenderer`'s `splash` from `String` to `Component` and the checker stayed
green; javac caught it, which it will not always (a retyped field that still compiles fails at
class-load exactly like a missing one). **CLOSED during the runtime shakedown** — the field's declared type is part of the assertion now; see the shakedown gotchas above.

**An option-backed API can be replaced by one whose setter carries a persisted side effect, so
translate the EFFECT, not the mechanism.** 1.21.11 deleted the FANCY graphics option, and its
replacement's setter rewrites the graphics preset to CUSTOM — a change that survives into
`options.txt`. `ACClientCompat#runAsFancy` therefore stops touching the option at all and instead
answers the one question the callers were really asking, through a static `@Inject` at HEAD of
`Minecraft#useShaderTransparency()Z` (which is **static** on 1.21.11).

**When a deleted API's consumers all funnel through one new accessor, mixin the funnel.**
`ClientLevel#getSkyColor(Vec3, F)` and `#getSkyDarken(F)` are both gone; everything that read either
value now goes through `Camera#attributeProbe().getValue(EnvironmentAttribute, float)` — the sky disc
via `SkyRenderer#extractRenderState`, the fog via `AtmosphericFogEnvironment`, the lightmap via this
mod's own `LightTextureMixin`. So **one** injection in `client/EnvironmentAttributeProbeMixin`
replaces two, on exactly the same set of callers.

**A lambda can change static-ness across a version, and Mixin matches handler static-ness against the
target's.** Forge's sky-pass lambda `method_62215` is `private **static**` on 1.21.11 where it was an
instance method before, so its handler had to become static too — and a static handler cannot use a
captured `this`, it reaches state through `Minecraft.getInstance()`. Read the modifier out of each
loader's own bytecode alongside the name index; neither is an API and nothing warns.

**⚠️ The biome `effects` → `attributes` move is the SILENT kind of break.** 1.21.11 emptied
`BiomeSpecialEffects` down to five colour fields and moved fog, sky, water fog, music, ambient loop /
mood / additions and ambient particles into a top-level `attributes` map of pathed
`EnvironmentAttribute` ids (`minecraft:visual/sky_color`, `minecraft:audio/ambient_sounds`, …). An
unmigrated biome **parses clean, boots clean and simply renders with vanilla's sky, fog and ambience
and plays no music** — no log line at all. `DataPackMigration.migrateBiomeAttributesTo12111` does the
move at `processResources`; expect `6` on every node ≥1.21.11. It accepts both the bare-`Music` and
the 1.21.4 weighted-list spellings, and `wrapBiomeMusicTo1214` is gated off from 1.21.11, so the two
passes are order-independent either way. Everything about the target shape was read out of the
bytecode — `Biome`'s codec string constants, `EnvironmentAttributeMap$Entry.createCodec`'s
`Codec.either` shape (so the plain form is a bare value), each sub-record's key names, and each
attribute's positional flag by scanning `EnvironmentAttributes.<clinit>` for `notPositional()`.

**Accounting for a falling injection count is not optional, and a per-file diff is how.**
`verify_mixins.py` reports **245** on `1.21.11-forge` and **242** on `1.21.11-neoforge` against
1.21.10's 249 / 246 — a drop of 4, which gets the same scrutiny as a rise. Import the checker and
`collections.Counter(i.source for i in vm.node_injections(node)[0])` on both nodes, then justify
every delta by name; six files moved and they net to −4:

| file | Δ | why |
|---|---|---|
| `ClientLevelMixin` | −2 | `getSkyColor(Vec3,F)` and `getSkyDarken(F)` deleted |
| `EnvironmentAttributeProbeMixin` | +1 | the single injection that replaces both |
| `SkyTimeOfDayMixin` (citadel) | −2 | excluded + pruned; see below |
| `BiomeAmbientSoundsHandlerMixin` | −1 | the handler's `BiomeManager` field is gone, so one `@Shadow` goes with it |
| `MinecraftMixin` | +1 | the new `useShaderTransparency` inject |
| `SplashRendererMixin` | −1 | the `@ModifyConstant` on the splash colour, deleted by an empty arm |

**A vanished feature is worth a paragraph of justification, not a prune and a shrug.** 1.21.11
deleted `Level#getTimeOfDay(float)` along with `DimensionType#fixedTime()` and
`ClientLevel#dayTime()`, so Citadel's smoothed sky rotation has no call left to redirect — that is the
`SkyTimeOfDayMixin` −2, and the file is excluded from the source set and pruned from the config from
1.21.11 (the exclude-plus-`pruneMixinEntries` convention, since the *target's member* is what
vanished). Nothing is lost: the celestial angles are `EnvironmentAttributes` now, read through
`EnvironmentAttributeProbe#getValue(attr, partialTick)` over time-based layers, which interpolates
by partial tick natively — which is precisely what Citadel's lerp added.
`EnvironmentAttributeProbeMixin` is the mirror image, excluded on every node *below* 1.21.11. Both
exclusions carry that
reasoning in `ModPlatformPlugin` beside the code, because a pruned mixin is otherwise indistinguishable
from a silently dropped feature.

### What the 1.21.10 wave cost (one boolean, and a replacement rule that had to grow a variable)

The cheapest wave of the walk after 1.21.7/1.21.8, and for the same reason: **1.21.10 changed exactly
one thing this mod touches.** Pins: `1.21.10-forge` = Forge **60.1.11**, `1.21.10-neoforge` =
NeoForge **21.10.64**. `verify_mixins.py` reports **249 / 246** on the two nodes — byte-for-byte the
1.21.9 numbers, so not one of the 66 mixins' targets moved — both dev servers boot to `Done` with the
same **1928 recipes / 1720 advancements** 1.21.9 prints, and neither loader needed a source
change of its own (Forge 59→60 and NeoForge 21.9→21.10 touch nothing AC uses: no
`onDestroyedByPlayer`, and the transfer API, `Capabilities.Item.ENTITY_AUTOMATION` and
`CustomizeGuiOverlayEvent$BossEventProgress` are all intact in 21.10.64). Forge's `LevelRenderer`
synthetics `method_62214`/`method_62215` are unchanged from 1.21.9, so the render-stage anchors held.

**`BlockBehaviour#entityInside` gained a trailing `boolean`.** `Entity#checkInsideBlocks` computes it
as `flag || aabb.intersects(pos)`: **true** when the entity's bounding box really intersects this
block, **false** when it merely swept through the block during the movement step. None of this mod's
nine `entityInside` blocks wants the distinction — acid, both magmas, purple soda, unrefined waste,
the nuclear-furnace component, both guanos and muck all act immediately, on any pass — so the
parameter is added and ignored, exactly like the `InsideBlockEffectApplier` 1.21.5 added.

**A `replacements.string` rule can take a version-dependent target, and here it had to.** The
1.21.10 half is a `val acEntityInsideTail` computed with `eval(current.version, ">=1.21.10")` and
interpolated into the two existing `!mc2105-entityinside-*` rules inside the `>=1.21.5` group — *not*
a second rule in a `>=1.21.10` group, which is what was tried first and silently did nothing:

- **Replacement groups do NOT chain.** Every rule matches against the **original** file text, so a
  rule keyed on what an earlier rule produced can never fire. This is a fifth semantic to add to the
  four already listed under the `replacements.string` gotcha in the workspace notes.
- The failure is quiet by construction. Only `PurpleSodaBlock` carries an `@Override` on
  `entityInside`, so it was the single compile error; the other eight would have compiled into
  methods that override nothing and are never called — a live block that silently stops working.
  **Put an `@Override` on a signature a rename rule rewrites, or nothing tells you.**

### What the 1.21.9 wave cost (the render rewrite, and both loaders moving house)

The biggest wave since 1.21.6, and for the same reason: **1.21.9 finished the job 1.21.6 started.**
1.21.6 made the GUI record render states and rasterise later; 1.21.9 does that to the *level* — a
frame now extracts every entity, sky and map into a render state, then replays the states into a
`SubmitNodeCollector`. Half a dozen mixin targets move as a consequence. On top of that **both**
loaders shipped unrelated API breaks in the same version, and vanilla renamed an item this mod's
recipes name. Pins: `1.21.9-forge` = Forge **59.0.5**, `1.21.9-neoforge` = NeoForge **21.9.16-beta**.

**The deferred-submit rewrite, target by target** (all descriptors javap'd on *both* loaders — every
one below is identical on the two, so these arms are loader-agnostic):

- **`LevelRenderer#renderEntities` is gone**, split into `extractVisibleEntities(…)`, which fills the
  `LevelRenderState`, and a private `submitEntities(PoseStack, LevelRenderState, SubmitNodeCollector)`,
  which queues the draws. The main pass calls the *second*, so both the AFTER_ENTITIES stage
  (`LevelRenderStageMixin`) and Citadel's pre-entity hook follow it there.
- **Forge's name for the main-pass lambda changed shape.** It was `lambda$addMainPass$N` through
  1.21.8 — which is why the `/lambda\$addMainPass\$/` regex selector worked on every loader — and is
  **`method_62214`** from 1.21.9, so Forge needs the name spelled out while NeoForge still keeps
  javac's (`lambda$addMainPass$1`). Same story one pass over: the sky lambda is Forge
  **`method_62215`** and NeoForge **`lambda$addSkyPass$8`** (was `$13` — the rewrite deleted lambdas
  ahead of it and everything renumbered). **Read the index out of each loader's own bytecode every
  time**; a lambda name is not an API and nothing warns when it shifts.
- **`renderLevel` gained a third `Matrix4f`** — the model-view the deferred submit replays each node
  against, alongside the frustum and projection matrices. Same on both loaders.
- **`Entity#getTeamColor()` left `LevelRenderer`.** The outline colour is read while *extracting* an
  entity's render state now, so the only call site in the client is
  `EntityRenderer#extractRenderState(Entity, EntityRenderState, float)`.
- **`ClientLevel#getTimeOfDay(F)F` left it too**, into `SkyRenderer#extractRenderState(ClientLevel, F,
  Vec3, SkyRenderState)` — one call, as on 1.21.2+.
- `HumanoidArmorLayer#setPartVisibility` **deleted with no successor** (an `ArmorModelSet` bakes one
  model per slot, so there is nothing left to hide); `ItemFrameRenderer#render` →
  `submit(ItemFrameRenderState, PoseStack, SubmitNodeCollector, CameraRenderState)` with the light
  moved onto `state.lightCoords`; `SkyRenderer` dropped the `MultiBufferSource$BufferSource` from
  `renderSunriseAndSunset` / `renderSunMoonAndStars`; `LevelRenderer#setupRender(Camera, Frustum, ZZ)`
  became private `cullTerrain(Camera, Frustum, Z)`; `MapRenderer#render` is now
  `(MapRenderState, PoseStack, SubmitNodeCollector, boolean, int)`.

**A `@Mixin` annotation can itself be Stonecutter-gated, and that is the cheap fix when an injection's
call site moves to a different CLASS.** Two of the moves above are exactly that, and both were carved
out of `client/citadel/LevelRendererMixin` into files of their own — `OutlineColorMixin` (target
`LevelRenderer` below 1.21.9, `EntityRenderer` from it) and `SkyTimeOfDayMixin` (`LevelRenderer` →
`SkyRenderer`) — because a class can only name one target and the redirect bodies themselves never
changed. Prefer this over the exclude-from-source-set + `pruneMixinEntries` convention: that one is
for a mixin whose target class *vanishes*, and it needs a `ModPlatformPlugin` change; a gated target
needs nothing, since the class compiles on every node.

⚠️ `OutlineColorMixin` must **never** `import net.minecraft.client.renderer.entity.EntityRenderer` —
the `!mc2102-render-import-entity` replacement rewrites precisely that import to this mod's render
shim on every ≥1.21.2 node, which would silently retarget the `@Mixin` at a class whose
`extractRenderState` takes an `ACRenderState`. It compiles clean and dies at mixin-apply. The target
is fully qualified for that reason, and the descriptor strings are slash-separated so the rule cannot
reach them either.

**Forge 59 moved two events from the mod bus to the game bus**, and it is a hard load failure, not a
silent no-op: `EntityAttributeCreationEvent` and `SpawnPlacementRegisterEvent` are plain
`MutableEvent`s now and no longer implement `IModBusEvent`, so `@Mod.EventBusSubscriber(bus = MOD)` on
`ACEntityRegistry` killed the boot with *"BusGroup "modBusForalexscaves" requires all events on it to
inherit from interface …IModBusEvent but class …EntityAttributeCreationEvent doesn't"*, thrown from
"Failed to register automatic subscribers". The arm is `bus = Bus.FORGE` from 1.21.9. **The javap
tell**: on eventbus 7 a *game*-bus event carries a `public static final EventBus<X> BUS` field; a
*mod*-bus event has only `getBus(BusGroup)` and implements `IModBusEvent`, directly or through a
supertype (`RegisterEvent` directly, `FMLCommonSetupEvent` via `ParallelDispatchEvent`). Note the
merged loom jar does **not** contain FML's own classes — `Mod$EventBusSubscriber` lives in
`javafmllanguage-<mc>-<forge>.jar` in the Gradle module cache.

**NeoForge 21.9 deleted `WorldWorkerManager` and replaced the item-handler capability.** Both are
vendored rather than gated, on the Citadel precedent, because both are needed on Fabric too:
`server/level/map/ACWorldWorkerManager` is a fifty-line copy of Forge's tick-budgeted worker queue
(one user, `CaveBiomeMapWorldWorker`), and `server/misc/ACItemAccess` is the two-method slice of an
entity inventory the gingerbread man's stealing goal needs. The capability change is **not** a rename:
the transfer API's `ResourceHandler<ItemResource>` models a slot as a resource plus an amount, and a
removal has to run in a transaction, so there is no `IItemHandler`-shaped adapter — only the two
operations the caller actually performs survive the translation. NeoForge also renamed
`FMLEnvironment.dist` → `getDist()`, split `EntityRenderersEvent$AddLayers`' skin accessors
(`getSkins()` now returns `PlayerModelType`s and the per-skin lookup moved to `getModelTypes()`), and
gave `RenderLivingEvent` a third shape.

**Vanilla renamed the chain.** The copper age gave chains a metal in their name, so
`minecraft:chain` is `minecraft:iron_chain` (and `copper_chain` is new) — a plain rename of both the
block and the item, confirmed against `Blocks`/`Items`. Six of this mod's data files name it: the two
hanging-sign recipes, `quarry_smasher`, the boundroid loot table and both ferromagnetic tags. The Java
side is a `replacements.string` rule; the data side is `DataPackMigration.renameIronChainTo1219`,
whole-token so `minecraft:chain_command_block` cannot match. Nothing gains the copper chain — it is
not iron, so it is not ferromagnetic. **This is what a boot catches and nothing else does**: the four
JSON failures were `Couldn't parse` lines in an otherwise-green log that still reached `Done`, and the
only numeric tell was `Loaded 1925 recipes` where the fixed node prints **1928**.

**Two housekeeping findings from the same wave.** (1) The three map mixins
(`MapDecorationMixin`, `MapDecorationTypeMixin`, `client.MapRendererMapInstanceMixin`) and their one
consumer (`client/render/VanillaMapDecorationRenderer`) had been **dead code since 1.20.5** — the
`MapDecoration.Type` enum became the `MapDecorationType` registry, which is the extension point the
first two existed to fake, and the third only ever fed the renderer. All four are excluded from the
source set and pruned from the mixin config from 1.20.5 now. `MapRendererMapInstanceMixin` was the
one still contributing an injection, so every `>=1.20.5` node's count drops by exactly 1
(1.20.6-forge 253→252) while `1.20.1-forge` stays at 259. That arithmetic *is* the verification. (2) **Two `replacements.string` rules whose
matches overlap do not both apply** — the second sees text the first already consumed. Where two
independent axes change the same span, one of them has to become a `//?` gate, the same conclusion the
"Ambiguous replacement" rule forces when two rules share a *target*.

### What the 1.21.8 wave cost (foreign synced data)

Vanilla 1.21.8 changed **nothing** this mod touches — both nodes (`1.21.8-forge` = Forge **58.1.19**,
`1.21.8-neoforge` = NeoForge **21.8.54**) compiled on the first attempt with zero source changes, and
`verify_mixins.py` reports the same **248 / 246** as 1.21.6 and 1.21.7. The whole cost of the wave was
one **loader** check that NeoForge 21.8 added.

**NeoForge 21.8 refuses to let a mixin add synced data to a vanilla entity class.** Its patched
`SynchedEntityData#defineId` calls `CommonHooks.verifyEntityDataAccessorRegistration(
STACK_WALKER.getCallerClass(), holderClass)`, which throws *"Identified an attempt to add synced data
to a foreign entity … Entity class: net.minecraft.world.entity.Entity, Mixins into entity class:
…mixin.EntityMixin"* — from `Bootstrap.bootStrap`, before anything else runs. Three things about it
matter:

- **It is a FIELD SCAN, not a caller check.** It rejects a caller that is not the holder class *and* —
  even when the caller *is* the holder, which is exactly what a mixin-merged `<clinit>` looks like —
  any holder that declares an `EntityDataAccessor`-typed field carrying Mixin's `@MixinMerged`
  annotation. So keeping the `defineId` call in the mixin is fine; keeping the **field** there is not.
- **The verdict is cached per caller class** (`EDA_CHECKED_CLASSES.add`), so the very first `defineId`
  in the class initialiser decides it for the whole class — you cannot fix half the accessors.
- **It is fatal only under `SharedConstants.IS_RUNNING_IN_IDE`**, a `LOGGER.warn` in production. That
  makes it easy to wave off, but a dev server that cannot boot costs the version walk its only
  runtime check, so it is fixed rather than tolerated.

The fix keeps one code path on all 58 planned nodes: the `defineId` calls **stay in the mixins**, so
each still runs inside its target's own class initialiser and takes that class's slots in the id pool
— definition order, ids and wire format are byte-for-byte identical on every version — but the
accessors are **stored in holder classes**: `citadel/server/entity/CitadelSyncedData` (the four
`Entity` magnet fields + the `LivingEntity` `CompoundTag` bag) and
`server/entity/util/ACSyncedData` (`FallingBlockEntity`'s magnet grace timer, kept separate to
preserve the Citadel vendoring boundary). Each `install…` method returns a `boolean` so the mixin can
hold the call in a `private static final boolean` field initialiser — a field that is *not* an
`EntityDataAccessor` and so is invisible to the scan.

NeoForge's suggested replacement, **syncable data attachments, was deliberately declined**: it exists
on one loader only, and the id-pool concern behind the warning is unchanged by either shape because
the mod is required on both sides of the connection. Both holder classes' javadoc says so, so this
does not get re-litigated.

Grep for `SynchedEntityData.defineId` under `mixin/` before every future wave — three sites existed
here (`EntityMixin`, `citadel/LivingEntityMixin`, `FallingBlockEntityMixin`) and each one is a separate
boot failure, discovered one per run.

### What the 1.21.7 wave cost (one qualified name)

The cheapest wave of the walk, and the shape to expect from a vanilla point release: **1.21.7 changed
nothing this mod touches.** `1.21.7-forge` (Forge **57.0.0**) compiled on the first attempt with zero
source changes; `verify_mixins.py` reports **248 / 246** injection points on the two nodes — the same
numbers as 1.21.6, so not one of the 66 mixins' targets moved; and both dev servers boot to `Done` with
the same 1874 recipes / 1666 advancements 1.21.6 prints. No new `DataPackMigration` pass, no new gate.

The single compile error was a **NeoForge** API split, not a Minecraft one:

- **NeoForge 21.7 cut `PacketDistributor` in two.** It keeps the seven clientbound methods and **lost
  `sendToServer`**, which now lives alone on `net.neoforged.neoforge.client.network.ClientPacketDistributor`.
  Despite the package it carries **no `@OnlyIn`** (checked with `javap -v`), so naming it from
  `ACNetwork` — a common class — is safe: the constant-pool entry resolves lazily and the only caller
  is `AlexsCaves.sendMSGToServer`, which by definition runs client-side.
- The fix is the `!mc217-sendtoserver-nf` **`replacements.string` rule**, not a `//?` gate, because the
  difference is one qualified name *inside* the existing `neoforge && >=1.20.5` arm and **Stonecutter
  cannot nest** a second condition in it. The source string carries `.sendToServer`, so it cannot touch
  the `sendToPlayer` line two below, which keeps the old owner on every version; the reverse direction
  is a no-op because the post-image spelling appears nowhere in `src/`.

### What the 1.21.6 wave cost (the GUI became a render graph, and Forge moved to EventBus 7)

This is the largest wave so far. Two independent rewrites land on the same MC version: vanilla stopped
drawing the GUI immediately, and **Forge 56.0.0 is the first Forge build on EventBus 7** — the
generation NeoForge moved to years earlier, except Forge did not copy NeoForge's API. Pins:
`1.21.6-forge` = Forge **56.0.0**, `1.21.6-neoforge` = NeoForge **21.6.20-beta**.

- **A screen can no longer draw a 3D model where it stands.** The GUI is recorded as render states
  first and rasterised afterwards, and the only door left open for model geometry is a
  `PictureInPictureRenderer`, which renders into its own colour + depth texture and blits that back.
  So the cave book's whole draw is deferred: `CaveBookRenderState` (screen-sized box, deliberately —
  the book's anchor drifts with the page-flip animation and its scale grows over the opening one, so
  any fixed margin would be a guess that clips) is submitted by the screen and `CaveBookPipRenderer`
  calls back into it when the pass runs. **The PiP depth convention is inverted** relative to every
  screen this mod ever drew: its projection is `setOrtho(…, zNear = -1000, zFar = 1000)` and
  `prepare()` has already applied `scale(f, f, -f)`, so a larger model z is *further away*, where the
  book's own chain and the page widgets (an item lifted off the page, an entity in front of it) are
  written the other way round. One more `scale(1, 1, -1)` in `renderToTexture` restores exactly the
  handedness and winding they had, rather than reversing the layering inside every page.
- **Registering that renderer has no shared path.** NeoForge fires
  `RegisterPictureInPictureRenderersEvent`; **Forge 56.0.0 does not patch `GuiRenderer` at all** — its
  renderer map is built in the constructor from an immutable list, and Forge's Mixin refuses an
  `@Inject` into a constructor outside RETURN/TAIL (the same restriction that bit Citadel's
  `EntityMixin` on 1.20.6) — so that node hands the renderer back from the *map lookup* instead, in
  `mixin.client.GuiRendererMixin`.
- **`Lighting` became an instance held by the `GameRenderer`**, and its four static setups folded into
  one `setupFor(Lighting.Entry)`; `Lighting$Entry.ITEMS_3D` is what replaced `setupFor3DItems()`. The
  call still sits in the same place in `render(DeltaTracker, boolean)`, which is what
  `GameRendererMixin#ac_render` anchors on.
- **`GuiGraphics#pose()` is a `Matrix3x2fStack`** — 2D, no `last()`, no third axis — and a GUI quad is
  addressed in whole pixels, so a fractional lerp has to move into the blit's own coordinates
  (`SpelunkeryTableScreen`). `setTooltipForNextFrame` replaces `renderTooltip` and is deferred.
  `RenderSystem.setShaderColor` is gone, so a tinted GUI blit goes through `GuiGraphics#blit`'s tinted
  overload (`ColorBlitHelper`). The splash text moves with it: `SplashRenderer#render`'s trailing
  argument went `int` → `float`, the tilt is `Matrix3x2fStack.rotate(F)` rather than a quaternion
  `mulPose`, and the yellow is `sipush -256` (0xFFFFFF00) handed to `ARGB.color(float, int)` instead of
  `ldc 16776960` OR'd with an alpha byte — `ARGB.color` masks to 24 bits, so the event's RGB means the
  same thing on both sides and only the constant to match changes.
- **Chunk layers are drawn as groups now.** `LevelRenderer#renderSectionLayer` is gone;
  `ChunkSectionsToRender#renderGroup(ChunkSectionLayerGroup)` draws OPAQUE (`SOLID` +
  `CUTOUT_MIPPED` + `CUTOUT`), TRANSLUCENT or TRIPWIRE inside a single open `RenderPass`, so there is
  no point *between* two opaque layers at which anything else can draw. **`AFTER_CUTOUT_MIPPED_BLOCKS`
  therefore has no anchor above 1.21.6** — free here, because nothing in this mod or the vendored
  Citadel asks for it. NeoForge made exactly the same collapse: its `RenderLevelStageEvent` became an
  abstract base with concrete subclasses and lost the per-cutout ones, which is why **≥1.21.6 drives
  the stages from `mixin.client.LevelRenderStageMixin` on every loader**, not just on Forge.
  The sky-pass lambda that mixin targets is spelled per loader (`method_62215` on the loom-mapped
  loaders, `lambda$addSkyPass$13` with two extra captured args on NeoForge) — read the index out of
  each loader's own bytecode.
- **`LevelRenderer#renderLevel` lost the `GameRenderer`** and gained a `GpuBufferSlice` of fog uniforms,
  a `Vector4f` sky colour and the world-preview flag; `GameRenderer#renderItemInHand(Camera, float,
  Matrix4f)` became `(float, boolean, Matrix4f)`. `renderLevel(DeltaTracker)` itself is unchanged and
  still builds the local `PoseStack` that `@Local` recovers.
- **The fog left the CPU entirely.** `FogRenderer` moved to `.renderer.fog`, `FogMode` is gone, and the
  fog is a std140 block written into a ring buffer — so it cannot be read or overridden per draw. The
  three `ACClientCompat` shader-fog shims throw on ≥1.21.6 and the callers read
  `ViewportEvent.RenderFog` instead.
- **Forge EventBus 7** is the second half of the wave. Three packages moved and two types were renamed
  — `eventbus.api.SubscribeEvent` → `eventbus.api.listener.SubscribeEvent`, `EventPriority` →
  `eventbus.api.listener.Priority` (byte constants, same names) — which are `replacements.string` rules
  registered only on Forge nodes ≥1.21.6, so the NeoForge `!nf-eventbus` rule rewriting the same prefix
  can never collide with them. What cannot be a string swap is gated in source: the **per-event static
  `BUS`** replacing `MinecraftForge.EVENT_BUS.post`, **`BusGroup`** replacing the mod-bus object, and
  **cancellation moving from `event.setCanceled(true)` to a boolean return**. There is no shared `Event`
  supertype and no bus-wide `post`, so each of Citadel's seven event classes names its own `BUS`.
- **…and EventBus 7 refuses to scan a class that contributes exactly one listener.** *"Only a single
  listener found in class …, you should directly call addListener() on the EventBus of ServerTickEvent
  instead"* — thrown out of `MinecraftForge.EVENT_BUS.register` and **fatal at mod load**
  (`LoadingFailedException`, no mixin involved). `CitadelEvents` is exactly that class, and
  `CitadelClientEvents` becomes one from 1.21.6 because its render-stage listener is gated out. Both
  now carry a `forge && >=1.21.6` arm with a static `register()` that calls
  `TickEvent.ServerTickEvent.BUS.addListener(Priority.LOWEST, …)` / `TickEvent.ClientTickEvent.BUS
  .addListener(…)` and no `@SubscribeEvent` at all; the two proxies' `registerEventHandlers()` gate
  between that and the old `EVENT_BUS.register(new …())`. `TickEvent` is an `InheritableEvent`, so a
  listener on `ServerTickEvent`'s own bus still sees the `Pre`/`Post` subclasses and `phase` still
  tells them apart. **This is a boot-time crash that no amount of mixin verification predicts** — the
  30- and 32-listener classes (`ClientEvents`, `CommonEvents`) are unaffected, so it only shows up on
  the small ones.
- **Forge's HUD gap reopens permanently.** `AddGuiOverlayLayersEvent` is gone again from 56.0.0 (it was
  absent on 51.0.33 too, back on 52.1.15), so `forge && >=1.21.6` ships without those three overlay
  cancellations — the gate is now `forge && >=1.20.5 && !=1.21 && <1.21.6`.
- **1.21.6 merged the experience bar, the jump meter and the new locator bar into one "contextual info
  bar"**, drawn by whichever `ContextualBarRenderer` the state selects. `EXPERIENCE_BAR` and
  `JUMP_METER` are gone; the two ids that replace them are the bar and its background.
  `EXPERIENCE_LEVEL` (the number above it) stays its own layer on both eras and is left alone, since it
  is not what the riding meter overlaps.
- **`ValueInput`/`ValueOutput` replaced the `CompoundTag` on every save/load signature** — ~150
  mechanical rewrites driven by `replacements.string`. **A mixin's `method =` descriptor string is
  invisible to those rules**, which is exactly why `citadel/LivingEntityMixin` was left behind by the
  sweep and had to be gated by hand. Remember that on every future rename wave: string rules rewrite
  *code*, and a mixin selector is a string literal describing bytecode.
- **`ItemStack#forEachModifier(EquipmentSlotGroup, BiConsumer)` became `(EquipmentSlotGroup,
  org.apache.commons.lang3.function.TriConsumer)`** — the third argument is the new
  `ItemAttributeModifiers$Display`. The `EquipmentSlot` gameplay overload is unchanged. **Both loaders
  moved**, but only Forge reported it, because `ItemStackAttributeModifiersMixin`'s body is gated
  `forge && >=1.21.2`. Stonecutter cannot nest a version gate inside a loader gate, so that arm is
  repeated whole as `//? if forge && >=1.21.6 { … *///?} elif forge && >=1.21.2 { … *///?}`.
- **`SoundEngine#tickNonPaused()` was renamed `tickInGameSound()`** — same empty descriptor, same
  position in `tick(boolean)`; the other half is `tickMusicWhenPaused()`. Only the target string moves,
  so the handler is shared between the arms.
- **Two `verify_mixins.py` gaps were closed this wave, and both were false *reds*, not false greens.**
  (1) `@At("NEW")` names a constructor and spells it as the *type being constructed* —
  `(FFFF)Lorg/joml/Vector4f;`, or a bare `Lorg/joml/Vector4f;` when the arguments are left open —
  neither of which is a method reference, so four injections were reported as "unparsed @At target".
  The checker now resolves both shapes to the owner's `<init>`. (2) `@ModifyConstant` constants were
  matched as **plain substrings**, but javap column-aligns an operand (`sipush        -256`), so the
  `bipush`/`sipush` forms had *never* matched on any node — only `iconst_N` and the `// int N` ldc
  comment ever worked. `constant_forms` returns whitespace-tolerant regexes now and the count site uses
  `re.findall`.
- **Stonecutter arms must be pure code.** A plain `//` comment line placed *between* an arm's
  `//?} elif …  {` marker and its `/*` produces a file that compiles on the node where the arm is
  active and dies with `illegal character: '#'` / `unclosed character literal` where it is not — the
  comment text gets read as code. Put all commentary **above** the `//? if`, never inside an arm.
  **Both directions bite.** The *other* failure mode of the same shape hit `SplashRendererMixin` during
  the 1.21.7 sweep: on the node where that arm is **active**, Stonecutter strips the `// ` prefix off
  those prose lines and the sentence compiles as source — seven syntax errors (`illegal start of type`,
  `';' expected`) with the comment text quoted back as code. So an arm with commentary inside it is
  broken on *some* node no matter which way the gate falls.
- **A `stonecutter.gradle.kts` edit re-generates EVERY node's tree, so "green" on an untouched node can
  be stale.** Adding one `replacements.string` rule for 1.21.7 invalidated all 20 `stonecutterGenerate`
  tasks and the next full build surfaced **8 failures on older nodes** that had been passing for waves —
  they had simply never been re-generated since the source that broke them was written. Two real bugs
  came out of it (the `SplashRendererMixin` prose above; `ACRenderTypes.acTexture` gated `>=1.21.5` when
  the `TriState` blur ctor actually arrives at **1.21.2** on both loaders). Treat the wave-end all-node
  `--continue` build as mandatory, not a formality, and never trust a node's last-known-green if the
  build script has moved since.
- **`instanceof` with a provably-true pattern is a Java-17 error and a Java-21 non-error, so it splits
  the node set by toolchain, not by loader.** `IEventBus#post` returns `T`, so
  `post(event) instanceof ICancellableEvent c && c.isCanceled()` on an event class that *declares*
  `implements ICancellableEvent` is unconditional — javac rejects it with *"expression type X is a
  subtype of pattern type ICancellableEvent"* under `--release 17` (1.20.4) and accepts it from 21
  (1.20.6 up). `AnimationEvent$Start.post` and `EventChangeEntityTickRate.post` both call `.isCanceled()`
  on the returned event directly now. `ACPlatform.postCancelable` keeps the pattern legitimately — its
  parameter is the base `Event`, so the test is genuinely conditional there.

### What the 1.21.5 wave cost (forced chunks, feature order, trim patterns)

- **Forge 55.x deleted its forced-chunk API outright.** Every `ForgeChunkManager` method now throws
  `UnsupportedOperationException("Mod used ForgeChnkManager when they should use vanilla's TicketType
  system…")` — Forge's own typo — so `registerForcedChunkCallback` is gated `forge && <1.21.5` and the
  `forge && >=1.21.5` arm of `ACPlatform` forces chunks through vanilla tickets itself. Two details
  make that more than a one-line swap. (1) **A vanilla ticket has no owner and no ref-count**:
  `TicketStorage.addTicket` dedupes by `(type, level)`, so two owners forcing one chunk share a ticket
  and the first release would unforce it for both. `ACPlatform` therefore counts owners in a
  `WeakHashMap` keyed on the `ServerLevel` (weak so a singleplayer world reload starts empty rather
  than believing chunks are still forced) and only touches the ticket on the first add and the last
  remove. (2) **The ticking/loaded distinction is the ticket's LEVEL, not a flag**:
  `addTicketWithRadius` puts the chunk at `ChunkLevel.byStatus(FULL) - radius`, so radius 2 is level
  31 = `ENTITY_TICKING` — exactly where vanilla's own forced chunks sit — and radius 0 leaves it at 33,
  loaded but not ticking. Because the two spellings must be distinguishable to that dedupe, they are
  two registered `TicketType`s (`forced_ticking` = `LOADING_AND_SIMULATION`, `forced_loading` =
  `LOADING`, both `NO_TIMEOUT`, neither persisting) over the ordinary vanilla `Registries.TICKET_TYPE`.
- **NeoForge 21.5 kept `TicketController` but repurposed `forceChunk`'s trailing boolean** from
  `ticking` to **`forceNaturalSpawning`** — same arity, same types, opposite meaning, so it compiles
  silently and would have turned every chunk these four call sites force into a mob-spawning one. The
  `!forge && >=1.21.5` arm passes `false`. A loader API that keeps its signature while changing what an
  argument *means* is invisible to both the compiler and `verify_mixins.py`; read the patched source.
- **1.21.5 swapped `patch_pumpkin` and `patch_sugar_cane`** in vanilla's own biomes, and that is fatal
  for a mod that kept the old order. A biome declares no absolute feature order — vanilla topologically
  sorts the adjacent pairs every loaded biome contributes, so one disagreement anywhere makes
  `FeatureSorter` throw *"Feature order cycle found, involved sources: [minecraft:windswept_savanna,
  alexscaves:candy_cavity]"* on the **first chunk generated**, i.e. during spawn-area prep, long after
  the data pack has parsed cleanly. `DataPackMigration.orderBiomeFeaturesTo1215` swaps the pair in all
  six cave biomes. Rebuilding that graph over vanilla 1.21.5 + this mod's biomes shows it is the only
  cycle — but the check is worth re-running on every MC bump, because the conflict lives between two
  files rather than inside the mod's own.
- **A `smithing_trim` recipe must now name its pattern.** 1.21.5 added a required
  `TrimPattern.CODEC.fieldOf("pattern")`, and `TrimPattern` became `record TrimPattern(ResourceLocation
  assetId, Component description, boolean decal)` — `template_item` is gone from its codec, though it
  survives in the file as a silently-ignored extra key, which is what
  `DataPackMigration.addTrimPatternsTo1215` reads back to fill the recipe's new field. Without it,
  *"Couldn't parse data file 'alexscaves:armortrim/polarity' … 'No key pattern in MapLike[…]'"*.

### What the 1.21.4 wave cost (item model definitions)

**An item's model became indirect.** `assets/<ns>/items/<id>.json` is what binds an item to an
`ItemModel.Unbaked`; `models/item/<id>.json` on its own renders the missing-model cube, and the failure
is logged per item rather than thrown, so every one of this mod's items would have gone blank silently.
`DataPackMigration.writeItemModelDefinitions` derives all **575** of them from the existing model tree
at `processResources` time, in four shapes:

- plain `minecraft:model`;
- `minecraft:special` for the 23 models that parented to `builtin/entity` — `icon_item`/`effect_item`
  → `alexscaves:icon`, the other 21 → `alexscaves:item_renderer`;
- `tints` for the five dynamically-coloured items (`alexscaves:tint`, sources `biome`/`pearl`/
  `jelly_bean`/`biome_treat`, with `minecraft:constant` `-1` padding the untinted layers) and for the
  43 spawn eggs, whose two colours are read straight out of `ACItemRegistry`'s `spawnEgg(…)` calls —
  they are spelled nowhere else;
- `minecraft:range_dispatch` over `alexscaves:legacy` for the 11 models that carried `overrides`,
  which is the nine predicate names `ClientProxy` used to hand `ItemProperties.register`.

The replacements for what 1.21.4 deleted are registered **by reflection into the vanilla
`LateBoundIdMapper`s** (`ACItemModelShims.register()`): ISTERs → two `minecraft:special` renderers,
`RegisterColorHandlersEvent.Item` → a tint source, `ItemProperties` → a range-select property. Both
special renderers' `Unbaked.MAP_CODEC` is a `MapCodec.unit`, so their definitions need no extra fields;
the tint and range-dispatch codecs **inline** theirs next to `"type"` / `"property"` (they are
`dispatch`/`dispatchMap`), which is what vanilla's own `bow.json` and `allay_spawn_egg.json` show.

Four things that each cost a round:

- **`SpecialModelWrapper` bakes `base` only for its display transforms.** So a now-dead
  `builtin/entity` model must keep its authored `display` block — strip *only* the `parent`. AMC's
  version of this pass rewrites such a file to `{}`, which here would flatten every spear, `cave_map`,
  `ortholance` and both icon items to default transforms. `stripDeadParent` is also the one
  **non-idempotent** pass in `DataPackMigration` (a stripped model no longer looks dead); that is safe
  only because `processResources` re-copies the models from source whenever it re-executes.
- **Three mixin targets moved, and `verify_mixins.py` caught all three** — the first boot died on the
  first of them with `InvalidInjectionException: Invalid descriptor … Expected ()V`:
  `PotionContents#getColor(Iterable)` split into an instance `getColor()` and the static
  **`getColorOptional(Iterable)`**, which is the direct successor (so the `>=1.21.4` arm targets that
  and returns an `OptionalInt`, keeping a custom potion colour winning as before);
  `LevelRenderer#renderLevel` dropped its `LightTexture` argument; and `SkyRenderer`'s
  `renderSunriseAndSunset`/`renderSunMoonAndStars` take a `MultiBufferSource$BufferSource` where
  1.21.2/1.21.3 passed a `Tesselator` (nothing the mixin reads moves — but a `method` selector is
  matched by descriptor).
- **A biome's `music` became a weighted list**: `SimpleWeightedRandomList.wrappedCodecAllowingEmpty(
  Music.CODEC)`, i.e. `[{"data": <Music>, "weight": n}]`. `Music` itself is unchanged, so
  `wrapBiomeMusicTo1214` is purely the wrapper — but it is fatal the same way the carvers were: all six
  biomes failed with *"Not a json array: {"max_delay":…}"* and took the whole `RegistryDataLoader` pass
  down with them.
- One **upstream copy-paste bug is reproduced verbatim, on purpose**: `extinction_spear.json`'s
  override points at `alexscaves:item/limestone_spear_throwing`, not its own throwing model. All three
  `*_spear_throwing` models have byte-identical `display` blocks, so it has never been visible;
  `dispatchOverrides` rewrites what it finds rather than silently changing behaviour, and carries a ⚠️
  saying so.

### What the 1.21.3 wave cost (the first Forge node ≥1.21.2)

Every `neoforge && >=1.21.2` gate written during the 1.21.2 wave had to be audited for a Forge
counterpart, because 1.21.2 has no Forge build — so 1.21.3-forge is where all of them first ran.
Four were real:

- **Forge patched `HumanoidArmorLayer#renderArmorPiece` to carry a trailing `HumanoidRenderState`**;
  vanilla and NeoForge did not. Same name, different descriptor, so the mixin missed. The draw body
  now lives in one `@Unique` helper in its own flat `>=1.21.2` block and each loader gets only its
  `@Inject` shim — and the Forge arm needs no `ac_renderState` capture, since the state arrives as a
  parameter.
- **Forge has no `ModifyDefaultComponentsEvent`**, and 53.1.11 deleted `getEnchantmentValue` /
  `isValidRepairItem` from `IForgeItem` — so `ACEnchantableItem` / `ACRepairableItem` answered nobody
  and enchantability and extra repair materials were **silently** lost. Forge's equivalent is
  `GatherComponentsEvent.Item` on the **game** bus (NeoForge's walks the registry on the **mod** bus),
  fired lazily per item from `Item#components()`. Hence `AlexsCaves#gatherItemComponents`. **Trap:**
  read `event.getOriginalComponentMap()`, never `item.components()` — the latter is the very method
  firing the event, and its cache is filled only after the event returns, so reading it recurses
  forever. Firing lazily also means item registration is long finished, so `builtInRegistryHolder()`
  on a repair material is safe.
- **Forge did *not* delete the item-side client-extension hook** the way NeoForge did in 1.21.2; it
  moved it off `IForgeItem` onto a patched `Item#initializeClient(Consumer<IClientItemExtensions>)`
  (called from `Item#initClient()` in the constructor). So the nineteen overrides still work on Forge
  and `ClientProxy#registerClientExtensions` correctly stays NeoForge-only — a doc fix, no code.
- **`forge:concrete` has never existed on any Forge build** — see the convention-tag gotchas above.

Two API gaps that are **permanent on Forge**, not version blips (both absent from 53.1.11/53.1.12,
54.1.18 and 61.1.0 — checked by listing `net/minecraftforge/client/event/` in each sources jar):

- **`RegisterShadersEvent` is gone.** Cheap: from 1.21.2 `ShaderManager.getProgram` compiles on demand
  and caches, so registration was only an eager-preload convenience. The listener is dropped on
  `forge && >=1.21.3`; the `ShaderProgram` constants in `ACInternalShaders` still resolve.
- **`RenderLevelStageEvent` is gone**, and Fabric never had it. Replaced by the loader-neutral
  `client/ACLevelRenderStage` — six stages under the mod's own names, supplied from the loader event
  where one exists and from `mixin.client.LevelRenderStageMixin` where it does not. That mixin is
  also the path the 22 Fabric nodes will take, which is why it is not shaped like a Forge event.
  Anchors: all four block layers come from one plain `renderSectionLayer`, just before
  `RenderType#clearRenderState`; AFTER_ENTITIES and AFTER_SKY are inside the frame-graph lambdas
  (`addMainPass` / `addSkyPass`) and have to be targeted as `lambda$…` synthetics, whose index must be
  read out of **each loader's own** bytecode — NeoForge added an `addSkyPass` overload and the
  numbering differs.

Plus one latent upstream bug fixed in passing: **`getSpawnEggFor` passed `null` to
`SpawnEggItem#getType`, which NPEs on every node ≥1.20.5** (the overload took a `@Nullable
CompoundTag` on 1.20.1 and an `ItemStack` from 1.20.5, dereferenced immediately). `ItemStack.EMPTY` is
the right argument — a real empty `PatchedDataComponentMap`, so it returns the default type. And
**NeoForge 21.3 deleted `DeferredSpawnEggItem`**, which is fine there: `BuiltInRegistries` declares
`ENTITY_TYPE` before `ITEM`, so a deferred item supplier can call `new SpawnEggItem(type.get(), …)`.

**Not yet done on any node: `runClient`.** Every verdict so far is `runServer` plus
`verify_mixins.py`, so nothing client-side — the armour layer, the render-stage mixin, the shaders —
has been exercised at runtime.

### Release-build gotchas (found closing the walk, 2026-08-19)

- **⚠️ A long Gradle build launched through the tool's own background mechanism dies when that
  background task is killed** — the kill takes the whole process group, Gradle daemon included. The
  58-node closing build was cut down at 49/58 with zero failures, which is indistinguishable in the
  log from a build that is merely still running: the tell is that no `gradle` process for this tree
  appears in `ps` while the log's last line is a live task. Launch anything that takes tens of minutes
  **detached** — `setsid zsh <script> < /dev/null > /dev/null 2>&1 &` from a foreground call, with the
  script appending its own `GRADLE_EXIT` — and poll the log rather than holding the process. Gradle's
  incrementality makes the restart cheap (the 49 finished nodes came back UP-TO-DATE and the resume
  took 3m22s), so the cost of getting this wrong is recoverable, but only if you notice.
- **`ls versions/*/build/libs/*.jar | grep -v sources | wc -l` is NO LONGER the release pre-flight
  count.** This tree emits **three** jars per node — the mod jar, `-sources` and `-javadoc` — so that
  documented check returns **116**, not 58, and an uploader that regex-matches the directory would
  happily create store entries for javadoc artifacts. Filter both: `grep -v -e sources -e javadoc`.
  Expect `58`, every one named `alexscaves-1.0.0-<loader>+<mc>.jar` with no `-SNAPSHOT`.
- **A `git push` is where you find out the repository moved.** GitHub answers a transferred repo with
  `remote: This repository moved` and then completes the push through the redirect, so nothing fails
  and it is easy to miss. The manifests' `sources_url`/`issues_url` had been baked from the old owner
  and needed re-pointing at `Codx-org` — a redirect is not a canonical URL, and it breaks the moment
  anyone creates a new repo at the old path. Grep the tree for the old owner after any transfer;
  here it is exactly `stonecutter.properties.toml` lines 26 and 28, and changing them invalidates all
  58 nodes, so it wants to ride along with a build you were going to run anyway.

### Licensing (settled 2026-08-19)

- **The mod is LGPL-3.0, and so is upstream** — `AlexModGuy/AlexsCaves` and `AlexModGuy/Citadel` both
  declare `license="GNU LESSER GENERAL PUBLIC LICENSE"` in their `mods.toml`, but **neither GitHub repo
  ships a licence file at all** (checked via `/contents/` and `/license`). The claim exists only in the
  manifest. This continuation now carries the text properly, which also settles the vendored-Citadel
  question: incorporation is *same-licence*, not compatible-licence. `docs/notes/citadel.md` said
  GPL-3.0 for a long time and was wrong.
- **LGPLv3 is two files, not one.** It is the GPLv3 text plus additional permissions, so the FSF layout
  is `COPYING` (GPLv3) + `COPYING.LESSER` (LGPLv3), both at the root. ✅ **GitHub's licensee detects
  that pair correctly** — `gh api repos/Codx-org/AlexsCavesContinued --jq .license` reports
  `LGPL-3.0`, i.e. it picks the LESSER one rather than reading the pair as GPL. Verified after push;
  no filename workaround is needed.
- **GPLv3 §4 makes the licence travel with the binary**, so the same two files also live at
  `src/main/resources/META-INF/`. Before that, **not one of the 58 jars contained any licence text**.
  No build-logic change was needed: `ModPlatformPlugin.configureProcessResources` scopes its `filter {}`
  to `filesMatching("*.mixins.json")`, so plain resources pass through untouched. Re-verify after any
  release build with `unzip -l <jar> | grep META-INF/COPYING` — expect two entries in all 58.
- ⚠️ **The repo is still PRIVATE** (`"private": true`). Shipping LGPL binaries to Modrinth/CurseForge
  obliges offering the corresponding source to recipients, so it has to go public — or a source offer
  has to exist — **before** the first upload, not after.

## 1.0.0 shipped (2026-08-21)

Both stores carry all **58** files, built from one `MOD_IS_RELEASE=true` pass
(`BUILD SUCCESSFUL in 24m 52s`, `GRADLE_EXIT=0`, 727 tasks, 0 failures, 0 `-SNAPSHOT`) and gated by
`verify_mixins.py` at **16433 injection points across 58 nodes, all resolving**.

- **Modrinth** `cO2CvXug` / `alexs-caves-continued`: 58 versions, each re-read individually with
  `GET /v2/version/{id}` — one file, correct loader, correct MC, and the project-level required
  dependency on codxlib `6oyMM4yX` on every one. Nine featured
  (`1.0.0+{fabric,forge,neoforge}-{1.21.11,26.1.2,26.2}`), confirmed per version, never off the
  cached project listing. Submitted for review (`status: processing`, `requested_status: approved`).
- **CurseForge** `1645389`: 58 files, each with a returned file id, ledgered in
  `scripts/.cf_uploaded.json` (22 fabric / 18 forge / 18 neoforge). ⚠️ `api.cfwidget.com` reported
  **0 files** for the whole upload window — expected, it is a caching proxy in front of CF's own
  approval scan. Do not read it as a failure.
- ⚠️ **`client_side`/`server_side` cannot be set on a Modrinth project with ZERO versions.** The
  same `PATCH {"client_side":"required","server_side":"required"}` returned 204 and left both
  `unknown` before the first upload, and took immediately after. Patch sides *after* the versions land.
- ⚠️ **Still owed by hand: the CurseForge CodxLib relation.** CF relations carry no version and the
  upload API has no field for them, so `{slug: codxlib, type: requiredDependency}` has to be added in
  the web UI. The runtime floor (`>=1.3.6`) is enforced by each jar's own manifest regardless.
- ⚠️ **`Codx-org/AlexsCavesContinued` was still private at upload time.** LGPL-3.0 obliges offering
  corresponding source to whoever receives the binaries; make the repo public (or post a written
  source offer on both project pages).

## Standing workspace rules that bite here

- **`rm` is blocked by the sandbox** — `mv` unwanted files to `/tmp/acc-trash/`.
- Multi-node Gradle work must be **one** invocation (`./gradlew :a:build :b:build … --continue`);
  back-to-back separate calls collide on the single-use daemon and Stonecutter's active-version
  state.
- Build release jars with `MOD_IS_RELEASE=true`, else everything is `-SNAPSHOT`.
