# API & data-pack gotchas — Alex's Caves Continued

**Read this before any MC version bump, any new `//?` gate, any `replacements.string` rule, and
before touching `DataPackMigration`.** Every entry is a trap that has already cost this tree at
least one debugging round; each records the exact version/loader boundary it has, because almost
none of them has the boundary you would guess.

Split out of `DEVELOPMENT.md` on 2026-08-22 so the always-loaded file stays small — see that file
for the map of which archive holds what. Nothing here has been edited in the move.


- ⚠️⚠️ **Vanilla deleted `Font#adjustColor` at 1.21.6, so every `0xRRGGBB` text colour in the mod
  became alpha 0 — invisible text on 22 of the 58 nodes, with nothing logged.** Up to and including
  **1.21.5**, both `Font#drawInternal` overloads *and* `drawInBatch8xOutline` (applied to the colour
  **and** the outline colour, never to `backgroundColor`) opened with
  `private static int adjustColor(int c) { return (c & 0xFC000000) == 0 ? ARGB.opaque(c) : c; }`, so
  the alpha-less literals mods have always written rendered fully opaque. From **1.21.6** that method
  does not exist: the colour reaches the glyph quads as written, a missing alpha byte means alpha 0,
  and the text is submitted, batched and drawn completely transparent. Nothing throws and nothing is
  logged. Confirmed by `javap` on the loader-patched jar of **all 22 MC versions in the matrix** —
  fixup present `1.20.1`…`1.21.5`, absent `1.21.6`…`26.2`; `GuiGraphics` has no fixup of its own on
  26.2 either. This blanked the Cave Compendium and the spelunkery table (reports #9/#9b), and on
  26.2 it was *invisible as a cause* because two unrelated 26.2 render bugs were dropping the same
  glyphs first. The fix is one helper — `ACColors.opaque(argb)`, doing exactly what vanilla used to —
  and because it is a **no-op below 1.21.6** the call sites need **no `//?` gate**: one ungated wrap
  is correct on all 58 nodes. It is applied inside `ACClientCompat`'s three text wrappers, so any
  site that already routes through them is fixed at once; only raw `GuiGraphics.drawString` sites
  need touching. `scripts/text_alpha_audit.py` is the standing detector (parses the colour argument
  of every text-draw call, skips wrapper-routed ones, fails on an empty alpha byte). **The tell for
  this class of bug is text that is *missing* rather than *wrong*, at a version boundary with
  nothing to do with the GUI code that stopped working** — and it is not specific to this mod: any
  mod carrying pre-1.21.6 colour literals has it.
- ⚠️ **`FormattedCharSequence.forward(String, Style)` is `StringDecomposer.iterate`, NOT
  `iterateFormatted`.** It walks the string verbatim, so legacy `§` codes come out as literal
  glyphs instead of styling the line. Vanilla's own `Font` `String` path is `bidirectionalShaping`
  then `StringDecomposer.iterateFormatted`. Cost a round in `ACClientCompat`'s `>=26.2`
  `drawInBatch(Font, String, …)` arm, which has to build the sequence by hand because 26.2's
  deferred text path takes a sink rather than a string.
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


## Added 2026-08-23 (the second bug-report wave)

- ⚠️⚠️ **NeoForge reintroduced `FluidType` into `EntityFluidInteraction` between builds
  `26.1.2.87` and `26.1.2.97` — same MC version, both live in players' hands.** On `.97`
  `trackerByFluid` is keyed by `FluidType`, the constructor's `Set<TagKey<Fluid>>` loop is
  commented out, and every `TagKey`-shaped method routes through `getFluidTypeByTag`, which
  **hard-throws `IllegalArgumentException: Cannot look up tracker by tag for non-vanilla fluid`**
  for anything that is not water or lava. So `Entity#isEyeInFluid(TagKey)` is a guaranteed crash
  for a modded fluid tag, and `EntityMixin`'s `@ModifyArg` widening of the tracked set silently
  stopped mattering. **This tracks the loader BUILD, not the MC version** — Stonecutter gates and
  `replacements.string` rules are both keyed on the MC version, so *neither can express it*; one
  jar has to be correct on both builds. The general fix for this whole class of problem is to
  **stop asking the loader**: `ACFluids` is now ungated and self-computes fluid height and
  eye-immersion from `Level#getFluidState`, which is byte-identical vanilla API on all 58 nodes.
  Crashed every `1.0.0` player on `.97` at world join, via `ClientEvents#fogColor`.
  ⚠️ Related non-trap: **`FluidState.is(TagKey)` looks deleted at 26 but is not** — the `is`
  overloads moved onto `net.minecraft.core.TypedInstance<Fluid>`, which `FluidState` implements,
  so the call still resolves ungated everywhere. Don't gate it.
- ⚠️ **`CollisionContext.of(Entity)` gained an `Objects.requireNonNull` at 1.21.2.** javap'd across
  the matrix: absent `1.20.1`…`1.21.1`, present `1.21.2`…`26.2`. Upstream passed `(Entity) null`
  into `new ClipContext(...)` for owner-less particle raycasts, which was legal for years and
  becomes an NPE from 1.21.2 up **with no compile-time tell** — the parameter type never changed.
  Symptom: the client dies while a magnet/tesla lightning particle is alive, i.e. "walking into a
  magnetic cave freezes the game". Use `CollisionContext.empty()` from 1.21.2; `ACCustomParticle
  #ownerlessClip` is the gated helper both particles now call. **Any vanilla method taking an
  entity is worth re-checking for a null guard on a bump** — the signature is not evidence.
- ⚠️ **`BlockItem#getDescriptionId()` was deleted at 1.21.2; the block-name prefix became an
  opt-in `Item.Properties#useBlockDescriptionPrefix()`.** javap'd: the override is present on
  `1.20.1`/`1.21.1` and absent `1.21.2`→`26.2`, and `useBlockDescriptionPrefix` appears at 1.21.2
  alongside `setId`/`overrideDescription`. A block-backed item that does not ask for the prefix
  falls back to `item.<ns>.<id>`, a key no language file has, and **renders as the raw key in the
  creative tab with nothing logged** — the block, its model and its recipe are all still fine.
  Seven items here (pewen door/sign/hanging sign, bioluminescent torch, thornwood door/sign/hanging
  sign) are registered in `ACItemRegistry` rather than through
  `ACBlockRegistry#registerBlockAndItem`, so they had it on every node from 1.21.2 up.
  `ACItemRegistry#blockItemProperties()` is the gated helper; `scripts/lang_audit.py` is the
  standing detector.
- ⚠️ **`ClientAsset` builds a texture path as `textures/<path>.png` — there is NO `entity/`
  segment.** Read out of the `StringConcatFactory` recipe constant with `javap -v`; byte-identical
  on 1.21.5, 1.21.6, 1.21.9, 26.1, 26.1.2 and 26.2, and the `asset_id` JSON field name is unchanged
  across them. At 26 the type became the inner record `ClientAsset$ResourceTexture` (`ClientAsset`
  itself is now an interface, which is why a naive javap shows only `id()`), but the recipe did not
  change. This bit the primordial frog: from **1.21.5** frog variants are a datapack registry
  (`data/<ns>/frog_variant/<id>.json`, `Registries.FROG_VARIANT`) and the `asset_id` must be
  `<ns>:entity/<file>`; below 1.21.5 they are `DeferredRegister`-registered and take the **full**
  path `textures/entity/<file>.png`, so the two spellings genuinely differ and only the >=1.21.5
  half was wrong.

## Added 2026-08-24 (the third bug-report wave)

- ⚠️⚠️ **`TextureManager#register` stopped loading the texture at 1.21.4.** Through 1.21.3 it
  called the texture's own `load()`; from 1.21.4 `javap -c` shows nothing but `byPath.put`,
  `safeClose` and the tickable-set add, and the loading call is the **new**
  `registerAndLoad(ResourceLocation, ReloadableTexture)` (absent on 1.21.2/1.21.3, present 1.21.4
  → 26.2). Anything that registers a `SimpleTexture` and then reads pixels back out of its
  `nativeImage` gets `null` from 1.21.4 up, with nothing thrown and nothing logged. Blanked this
  mod's Cave Map to solid black on every node from 1.21.4 while the biome labels — which read the
  biome array, not the textures — kept drawing, which is what made it look like a *map* bug rather
  than a *texture* bug. A `DynamicTexture` is unaffected and must keep using plain `register`: it
  is not reloadable, so there is nothing to load.
- ⚠️ **`EMISSIVE` is not "unlit". Only the `NO_CARDINAL_LIGHTING` shader define removes the diffuse
  term.** `EMISSIVE` gates the *lightmap* and leaves `minecraft_mix_light(...)` in place, so an
  entity-translucent-emissive type is a fullbright-by-lightmap type that is still shaded by its
  face normals. Verified in `core/entity.vsh` on 1.21.5, 1.21.6, 1.21.8, 1.21.9, 1.21.10, 1.21.11,
  26.1, 26.1.2 and 26.2. Below 1.21.5, `rendertype_entity_translucent_emissive.vsh` calls
  `minecraft_mix_light` too. The tell is a *hard bright/dark step between adjacent faces* of a
  model that is supposed to be flat-lit — not a brightness that is merely wrong.
  ⚠️ The loaders diverged here at 26.2 and **not in the same direction**: Forge 65.1.0 rewrote its
  unlit factories onto plain `RenderPipelines.ENTITY_TRANSLUCENT` (so its unlit type is no longer
  unlit), while NeoForge 26.2.0.66 still names `neoforge:pipeline/entity_unlit_translucent`, still
  declares the define and still binds the lightmap. Probe each loader; the other one is not
  evidence. This mod now owns the type (`ACInternalShaders.ENTITY_UNLIT_TRANSLUCENT` +
  `ACRenderTypes#getUnlitTranslucent`) and only delegates on the 35 nodes whose loader is correct.
- ⚠️ **`AABB.encapsulatingFullBlocks` is NOT a drop-in for `new AABB(BlockPos, BlockPos)`** (which
  1.20.3 deleted). The old constructor passed the two positions through as plain coordinates and
  let the six-double constructor sort them; the replacement takes min/max and **adds 1 to each
  maximum** so both blocks are enclosed whole. Every box translated that way is one block larger on
  its +X/+Y/+Z face — invisible in render bounds and entity searches, and a real behaviour bug
  anywhere the box drives a scan. Here it made the quarry's mining box reach its own corner torches
  and demolish its frame. **The six-double constructor is unchanged across the whole matrix and
  sorts its own corners, so the faithful translation needs no gate at all.** General form: a
  deletion whose replacement has a *different name* is worth reading for different *semantics*,
  not just a different spelling.
- ⚠️ **Registering a `PoiType` does not make its blockstates points of interest on Fabric.**
  `PoiTypes` keeps a private static `TYPE_BY_STATE` filled only by its own `bootstrap`, and
  `PoiTypes.forState` — the one question `PoiSection` asks — reads that map and nothing else.
  Forge fills it from `GameData$PointOfInterestTypeCallbacks` and NeoForge rebuilds it in
  `PoiTypeExtender.extendPoiTypes`; **Fabric does neither**, so a type that reached the registry is
  a POI no chunk ever records — no error, no log line, every lookup empty. All nine of this mod's
  POI types were dead on all 22 Fabric nodes (magnetism, moth balls, sundrops, the nuclear siren
  and furnace, the abyssal altar, the conversion crucible, the gingerbarrel). `registerBlockStates`
  is private but its descriptor is identical on 1.20.1, 1.20.5, 1.21.2, 1.21.5, 1.21.9, 1.21.11,
  26.1 and 26.2, so `mixin/fabric/PoiTypesInvoker` needs no gate. Existing worlds heal themselves —
  `checkConsistencyWithBlocks` runs from chunk deserialization.
- ⚠️ **Cancelling `HumanoidArmorLayer#renderArmorPiece` also cancels the pose copy nobody names.**
  Vanilla copies the wearer's pose onto the armour model *between* choosing the model and hiding
  parts, so a mixin that substitutes its own model and cancels the method leaves that armour in its
  **bind pose** — and any animation pass reading limb rotations off it reads zeros. The copy is
  spelled `copyPropertiesTo` below 1.21.9 and `setupAnim(state)` from 1.21.9 (the `ArmorModelSet`
  rewrite), and it has to run **before** the animation pass, not after model selection.
- ⚠️⚠️ **`renderBackground` inside a screen's own `render` is upstream 1.20.1 idiom and is wrong on
  every node above it — and inside `renderBg` it is a crash.** Two independent changes, both proven
  with `javap -c` rather than remembered:
  - From **1.20.2** vanilla's `Screen#render` opens by calling `renderBackground` itself, and from
    **1.21.6** the `public final renderWithTooltip` does it *ahead of* `render` (on **26** `render`
    is renamed `extractRenderState`). A screen that also calls it draws the dim gradient **twice**.
    That is not subtle: `0xC0101010` → `0xD0101010` is α ≈ 0.8157 at the screen bottom, so one pass
    leaves the frame at 18.4% and two at 3.4%. Players report it as *"it shows nothing"*, because
    what is behind the gradient is still there and simply unreadable — there is no error, no log
    line, and the screen's own widgets look fine.
  - From **1.20.2** `AbstractContainerScreen#renderBackground` **calls `renderBg`**. So a `renderBg`
    override that calls `renderBackground` is unbounded mutual recursion: a hard `StackOverflowError`
    the moment the screen opens, on 1.20.2 → 1.21.11, every loader.

  Both survive a version walk unnoticed when the **active node is 1.20.1**, where the old spelling is
  correct and the two methods are unrelated. Upstream leaned on `fillGradient`'s `z` of `-1000` to
  slide the gradient behind content already on screen; that `z` is gone from 1.21.6 anyway, where GUI
  layering is bounds-based (`GuiRenderState.findAppropriateNode`) rather than depth-based. **Grep
  every ported screen for a `renderBackground`/`renderBg` call of its own before trusting it** — in
  this tree three of them had one, and only one of the three was ever reported.

## Added 2026-08-25 (the seventh bug-report wave)

- **An additive pass that was accidentally drawn twice was shipping at 2× alpha — so removing the
  duplicate is a visible brightness regression, and players will report it as "the effect stopped
  working".** `1.0.4` gave `ACRenderTypes` a `TYPE_CACHE` because a fresh `RenderType` per call
  misses every identity-keyed vanilla map and so gets drawn more than once; that fixed the
  tremorzilla's "borderline epileptic" flashing. The next report was that the same dorsal plates
  "don't light up to begin with, even when fully charged" — from the *same* change, seen from the
  other side. With `BlendFunction(SRC_ALPHA, ONE, …)` alpha **is** brightness and one pass caps at
  1.0, so a duplicated pass at alpha 0.5 and a single pass at alpha 1.0 are the same peak; upstream's
  `LayerGlow` pulses `sin(age * 0.2) * 0.15 + 0.5`, i.e. it had never asked for more than half.
  When you fix a draws-twice bug, **check the intended single-pass value still looks like the effect
  it is meant to be**, and if not raise it deliberately where it is one readable number. Measured
  both directions on one node with only the client restarted between runs — the memoize itself is
  brightness-neutral (per-call 297,646–405,676 vs memoized 290,794–408,198 green energy, peak 255
  both), and the deliberate lift to 0.70–1.00 is 1.6–2.0×. Full numbers in
  [`1.0.1-triage.md`](1.0.1-triage.md) ("Seventh wave").
- **Being "powered"/"charged" is often only a *texture* swap, not a brightness change.** The
  tremorzilla's powered glow sheet lights 6,191 more pixels than the idle one but is drawn at the
  identical alpha, so "why doesn't charging it look like anything" is a fair question about upstream
  rather than about the port. Check the alpha before concluding a state flag is not reaching the
  client.

## Added 2026-08-26 (the eighth bug-report wave)

- **A `Map<BlockState, VoxelShape>` shape cache is a hard crash the moment ANY other mod adds a
  blockstate property to that block.** `BlockState` equality is identity, so a map built by
  enumerating *your* properties off `defaultBlockState()` has no key for the states that appear once
  a foreign property widens the state definition's cartesian product; `map.get(state)` returns
  `null`, and vanilla dereferences it without a guard —
  `BlockBehaviour$BlockStateBase$Cache.<init>` assigns `this.collisionShape = block
  .getCollisionShape(...)` and calls `.isEmpty()` on the next line. **Key the cache on the properties
  that actually change the outline** (read them by name), never on the state object. Hit in
  `AbyssmarineWallBlock`, which enumerated all eight of its properties into two 9720-entry maps;
  now two 162-entry arrays indexed on `UP` + the four `WallSide`s.
- **…and on Fabric that crash lands during YOUR registration call, in a stack that names neither the
  block nor the mod that caused it.** `fabric-registry-sync-v0`'s `initShapeCache` mixes into
  `Blocks.<clinit>` to add a `RegistryEntryAddedCallback` on `BuiltInRegistries.BLOCK` that runs
  `BlockStateBase::initCache` over **every possible state** of each block *at registration time*. So
  the NPE surfaces inside `DeferredRegister$Entry.resolve` in the mod constructor — a **startup**
  crash, not a place-a-block one — and the only mod named in the whole trace is ours. The defect is
  loader-neutral (Forge/NeoForge just defer it to first use), the *timing* is Fabric's.
- **A reporter's "it only crashes when I add mod X" bisection is worthless when the crash is inside
  your own registration.** Removing your mod removes the crash trivially, and every other
  combination they tried still contained the mod actually at fault. Reproduce the named combination
  yourself before spending any time on it — here `alexsmobs` + `accdelight` + `amcdelight` +
  `farmersdelight` + `ferritecore` booted clean, and the real trigger was never identified among the
  743 installed mods (nor did it need to be).

## Added 2026-08-27 (the ninth bug-report wave)

- **`accessTransformers` must be ABSENT from a generated `mods.toml`, never present-and-empty.**
  Forge's `ModFile` falls back to `META-INF/accesstransformer.cfg` (where loom puts it) **only when
  the key is missing**. `accessTransformers = [ ]` is a positive statement that the mod has none, so
  the file is never read and every AT-widened member stays inaccessible — an `IllegalAccessError` at
  registration, on Forge only (NeoForge resolves the file either way), and worst on the oldest Forge
  builds. Shipped that way from the port through `1.0.6`; the field is now gone from
  `LoaderMetadata.kt`'s `ForgeManifest`. General form: **for a manifest key with a
  "fall back if unset" rule, emitting the empty value is not the same as omitting it.**
- **A `//?` gate keyed on the MC version is not enough when a LOADER adds an overload and moves the
  call to it.** `HumanoidArmorLayer` on **NeoForge 1.21.1** has both vanilla's 6-argument
  `renderArmorPiece` and a NeoForge-added **12-argument** one
  `(PoseStack, MultiBufferSource, LivingEntity, EquipmentSlot, int, HumanoidModel, FFFFFF)V`;
  `render` calls the 12-arg one and the 6-arg one is a bridge *into* it. AC's `@Inject` targeted the
  6-arg method, so it resolved, passed `verify_mixins.py`, applied — and never ran, i.e. armour drew
  nothing on exactly that loader+version. **`verify_mixins.py` proves a target exists, not that it is
  the one on the call path**; when a hook is silently inert, `javap` the *caller* and check which
  overload it invokes. Fixed with a `neoforge && >=1.21.1 && <1.21.2` arm on the 12-arg descriptor.
- **A `BlockBehaviour.Properties` shared between blocks cannot carry a per-block id (>=1.21.2).**
  From 1.21.2 the id lives on the `Properties`, so a `static final X_PROPERTIES` constant built in
  `<clinit>` — outside every registration window — is a null id for every block but (at best) the
  first. Symptom is `NullPointerException: Block id not set` from whichever mod reads
  `effectiveDrops()` / `effectiveDescriptionId()` earliest; here Forgified Fabric API's
  `FabricBlock$FabricProperties.blockIdOrThrow`, reached through Sinytra Connector + Farmer's Delight
  Refabricated, which asks *before* any vanilla or NeoForge path does. Two halves to the fix: stamp
  the pending id in `Properties.<init>` (`@Inject(method = "<init>", at = @At("RETURN"))`) rather
  than only on builder calls, and make every shared `_PROPERTIES` constant a **per-use factory
  method** so each block builds its own inside `ACRegistryIds.constructing`.
