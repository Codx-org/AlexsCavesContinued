# Vendored Citadel

Upstream Alex's Caves `2.0.2` has a **hard dependency on Citadel** (`citadel`, Alex the 666's
shared library). Citadel only ever shipped for MC 1.20.1 (Forge + NeoForge) and 1.21.1 (NeoForge),
and never for Fabric — so a 49-node Forge/NeoForge/Fabric matrix cannot depend on it. Alex's Caves
Continued therefore **bundles the subset it uses**, package-relocated from
`com.github.alexthe666.citadel` to **`com.github.alexmodguy.alexscaves.citadel`**.

Relocation is mandatory, not cosmetic: a player may well have the real Citadel installed for
another mod, and two copies of one fully-qualified class name in a single classloader is a hard
failure. Licence-wise this is clean — Citadel is LGPL-3.0 and Alex's Caves is GPL-3.0, so
incorporation is allowed.

There is deliberately **no `deps.citadel`** in `stonecutter.properties.toml` and no
`required("citadel")` in any buildscript. Nothing resolves the real mod on any node.

## What is vendored

93 classes under `src/main/java/com/github/alexmodguy/alexscaves/citadel/`, derived by taking the
45 Citadel types Alex's Caves names directly and closing over their references. Broadly:

| Area | Notes |
|---|---|
| `animation/` | `Animation`, `AnimationHandler`, `IAnimatedEntity`, leg solvers |
| `client/model/` | `AdvancedEntityModel`/`AdvancedModelBox`/`ModelAnimator` and the basic model pair |
| `client/render/` | `LightningRender` + `LightningBoltData`; the pathfinding debug renderer |
| `client/shader/` | `PostEffectRegistry` — AC registers three post effects through it |
| `client/rewards/` | `CitadelCapes` (dev/contributor capes) |
| `client/tick/`, `server/tick/` | the whole tick-rate-modifier system |
| `server/entity/pathfinding/raycoms/` | the raycoms A* navigator AC's big mobs use |
| `server/generation/`, `server/world/` | `SurfaceRulesManager`, `ExpandedBiomes`/`ExpandedBiomeSource`, `CitadelSurfaceRuleWrapper` |
| `server/message/` | six packets, re-registered on AC's own channel |
| `item/` | `BlockItemWithSupplier`, plus the two display items (below) |

## What is *not* vendored, and why

Citadel's own content is gone: the guide-book item and GUI, the lectern block, its config, the
patreon/space-station renderers, the capes selection screen, the Tabula model loader, the rainbow
aura shader, the April Fools Tetris, the web helper, `CitadelRecipes`.

Four Citadel mixins were skipped after grepping AC for their events:

| Skipped | Because AC never uses |
|---|---|
| `ChunkGeneratorMixin` | `EventMergeStructureSpawns` |
| `SmithingMenuMixin` | `CitadelRecipes` |
| `ItemBlockRenderTypesMixin` | `EventGetFluidRenderType` |
| the `EventGetStarBrightness` inject inside `ClientLevelMixin` | that event (the rest of the mixin is kept) |

## The seam: `citadel/Citadel.java`

Upstream this was Citadel's `@Mod` class. Here it is a plain holder that AC calls into, so the
vendored code keeps working while everything it touches routes through **AC's** mod id, network
channel and event bus:

- `Citadel.registerModBus(modEventBus)` — called from the `AlexsCaves` constructor. Registers the
  `CitadelSurfaceRuleWrapper` codec (an unregistered `RuleSource` codec makes the world save fail),
  registers the two display items, and registers `PROXY` on the Forge event bus.
- `Citadel.registerMessages(NETWORK_WRAPPER, packetsRegistered)` — called from `commonSetup` after
  AC's own 16 packets; returns the next free discriminator.
- `Citadel.loadComplete(event)` — called from AC's `loadComplete`; runs the TerraBlender hand-off.
- `Citadel.LOGGER`, `Citadel.PROXY`, and the three `sendMSG*` helpers delegate to `AlexsCaves`.

`CitadelConstants` had to be kept as a real class: **AC's own mixins** (`mixin/EntityMixin`,
`mixin/FallingBlockEntityMixin`) reference `CitadelConstants.REMAPREFS` in their annotations.

### Behavioural deviations from upstream Citadel

Three, each deliberate:

1. **`CitadelCapes.getCurrentCape`** falls back to `getFirstApplicable(player)` when the entity has
   no `CitadelCapeType` tag. Upstream returned null and relied on its cape-selection screen to write
   that tag; that screen is not vendored, so without the fallback AC's dev/contributor capes would
   never render.
2. **`LecternBooks`** lost its `init()` and Citadel's own book. `ACItemRegistry` already puts the
   cave book into `LecternBooks.BOOKS` during setup, so the map just starts empty.
3. **TerraBlender compat is reflective** (`citadel/compat/ModCompatBridge`). Upstream had a compile
   dependency; here `SurfaceRuleManager.addToDefaultSurfaceRulesAtStage` /
   `addSurfaceRules` are invoked via `Class.forName`, and a `ReflectiveOperationException` degrades
   to a logged warning. That keeps TerraBlender off the compile classpath of all 49 nodes.

## The two display items (easy to miss)

AC's advancement JSONs use `citadel:icon_item` (28 of them) and `citadel:effect_item` (2) as their
display icons — a **data-side** dependency that no Java grep finds. Dropping Citadel without
handling it produced 112 `Couldn't load advancement` errors on the dev server, cascading from
`Expected item to be an item, was unknown string 'citadel:icon_item'`.

Both are now registered under **`alexscaves:`** (`citadel/item/CitadelDisplayItems`) and all 30
advancement JSONs were rewritten to match. They render through
`citadel/client/CitadelItemstackRenderer`, a trimmed copy of Citadel's BEWLR — Citadel's third
display item, `fancy_item`, is unused by AC and was not vendored, and an `icon_item` with no
`IconLocation` tag now draws nothing rather than dragging Citadel's default PNG along.

**Lesson: when dropping a bundled dependency, grep the `resources/` tree for its namespace too, not
just the Java imports.**

## Mixins

Citadel's own mixins were relocated into subpackages — `mixin/citadel/` and
`mixin/client/citadel/` — because AC already owns classes of the same name (`LivingEntityMixin`,
`ClientLevelMixin`, `LevelRendererMixin`, `LivingEntityRendererMixin`, `SoundEngineMixin`). All 13
are listed in `alexscaves.mixins.json`, bringing that config to **66** injection targets (53 AC + 13
Citadel) under `defaultRequire: 1`.

| `mixin/citadel/` | `mixin/client/citadel/` |
|---|---|
| `LevelMixin` | `AbstractClientPlayerMixin` |
| `LivingEntityMixin` | `ClientLevelMixin` |
| `MinecraftServerMixin` | `HumanoidModelMixin` |
| `NoiseGeneratorSettingsMixin` | `LevelRendererMixin` |
| `PrimaryLevelDataMixin` | `LivingEntityRendererMixin` |
| `ServerLevelMixin` | `SoundEngineMixin` |
| | `SplashRendererMixin` |

`MinecraftServerMixin` was repointed from Citadel's `ServerProxy` to `CitadelProxy`.

Citadel's 20 access-transformer lines were merged into
`src/main/resources/META-INF/accesstransformer.cfg` under a marked section; `SurfaceRules$Context`
and `SurfaceRules$SurfaceRule` were already granted by AC and are not repeated.

## Verification status

`:1.20.1-forge:build` green; dev server reaches `Done (1.4s)` with zero advancement errors and no
mixin injection failures. The only remaining `ERROR` lines are `RuntimeDistCleaner` complaints about
client-only mixin targets on a dedicated server — the same category the pre-Citadel baseline
printed, now three longer (`SplashRenderer`, `HumanoidModel`, `AbstractClientPlayer`).

## Gotchas hit while vendoring

- **The import-closure script misses same-package references.** `AbstractPathJob` used
  `ChunkCache` and `IPassabilityNavigator` without imports (same package), so both were absent from
  the computed closure and only surfaced at `compileJava`. After running a closure, diff each
  vendored package against the upstream package and eyeball the leftovers.
- `SmallExplosionParticle` had an unused `repack.jcodec.scale.ColorUtil` import that survived the
  package rewrite and then failed to resolve. Deleted.
