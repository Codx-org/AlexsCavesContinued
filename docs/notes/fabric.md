# The Fabric milestone — 22 nodes, `1.20.1` → `26.2`

Companion to the root `DEVELOPMENT.md`. Read that first for the harness; this file is about the one
loader that had to be *built* rather than walked.

**Status: complete.** All 22 Fabric nodes compile, and the whole 58-node matrix closes green —
`BUILD SUCCESSFUL in 26m 48s`, 727 actionable tasks, zero task failures; `scripts/verify_mixins.py`
resolves **15307** injection points across all 58; `scripts/aw_check.py` reports **0 problems** on
every one of the 22 MC versions.

Fabric is the only loader here that reaches all 22 MC versions — Forge and NeoForge stop at 18
apiece for the reasons in the root node map — so it is also the widest single-loader walk in the
tree.

## The shape of the port

Alex's Caves is a Forge mod through and through: ~700 deferred registrations, ~60 Forge game-bus
hooks, eight events **it publishes itself**, `ForgeConfigSpec`, `FluidType`, `ToolAction`,
`IClientItemExtensions`, `IGlobalLootModifier`. None of that exists on Fabric. The port does **not**
rewrite the mod to Fabric idioms; it supplies Forge's shapes under the mod's own namespace, so that
every consumer line stays byte-identical on all three loaders. Four mechanisms, in order of how much
they carry:

1. **A Fabric-only source package** — `com.github.alexmodguy.alexscaves.fabric.**`, **115 files**,
   excluded from the compile on every non-Fabric node by `ModPlatformPlugin.configureJava`. Because
   nothing outside Fabric ever compiles it, **no file in it needs a loader gate and every file in it
   may name `net.fabricmc.**` freely** — which is why the version gates inside it are all plain
   `//? if >=X`.
2. **Stand-in types under `fabric/forge/**`** that reproduce exactly the slice of the Forge API the
   mod uses — `DeferredRegister`, `MinecraftForge`/`Event`/`SubscribeEvent`, ~30 event classes,
   `ForgeConfigSpec`, `FluidType`, `Tags`, `ToolActions`, the client extension interfaces. Each
   carries a javadoc saying **why it is the slice it is**; read the class before widening it.
3. **69 `!fab-*` `replacements.string` rules** that re-point the mod's `net.minecraftforge.*` /
   `net.neoforged.*` spellings at those stand-ins. This is the whole reason the ~700 registration
   lines and the 20-odd `post(...)` call sites never gained a Fabric arm.
4. **26 dispatcher mixins under `mixin/fabric/**`** for the hooks Fabric API has no callback for —
   the ones that make the vendored bus actually fire.

Two hubs are worth knowing by name: **`fabric/ModBus.java`** stands in for Forge's *mod* bus (so
`AlexsCaves`'s ~28 `X.DEF_REG.register(modEventBus)` lines are untouched, and registration **order**
is preserved, which is load-bearing here), and **`fabric/event/ACEventBus.java`** stands in for the
*game* bus. The second is a real dispatcher rather than a direct-call table on purpose: the mod
**publishes** eight event classes of its own for other mods to steer it with, and a direct-call
shim answers the inbound half while silently dropping the outbound one.

## What the 26.x Fabric wave cost

The four 26.x nodes (`26.1`, `26.1.1`, `26.1.2`, `26.2`) are where fabric-api itself moved, on top of
everything vanilla moved. `26.1` took four rounds; `26.1.1`, `26.1.2` and `26.2` then each compiled
**first try**, and `26.1.x` all report an identical 281 injection points, so nothing in vanilla moved
inside that band on this loader.

**fabric-api's own 26 sweep is seven pure renames plus one shape change.** Read out of
`fabric-api-0.145.1+26.1.jar`'s nested modules with `javap`, not guessed:

| before | after |
|---|---|
| `client.keybinding.v1.KeyBindingHelper#registerKeyBinding` | `client.keymapping.v1.KeyMappingHelper#registerKeyMapping` |
| `client.particle.v1.ParticleFactoryRegistry` | `ParticleProviderRegistry` (same `getInstance()`, same two `register` overloads) |
| `client.particle.v1.FabricSpriteProvider` | `FabricSpriteSet` (still `extends SpriteSet`) |
| `client.rendering.v1.ColorProviderRegistry.BLOCK::register` | `BlockColorRegistry::register` — **block only**, and static |
| `client.rendering.v1.EntityModelLayerRegistry` | `ModelLayerRegistry` |
| `client.rendering.v1.TooltipComponentCallback` | `ClientTooltipComponentCallback` |
| `api.loot.v2` | `api.loot.v3` (`ALL_LOADED` keeps its `(ResourceManager, Registry<LootTable>)` shape) |
| `itemgroup.v1.FabricItemGroup#builder` | `creativetab.v1.FabricCreativeModeTab#builder` — the whole `fabric-item-group-api-v1` module is gone |
| `networking.v1.PayloadTypeRegistry#playC2S/playS2C` | `#serverboundPlay/#clientboundPlay` |
| `blockrenderlayer`/`rendering` `BlockRenderLayerMap` (fluids) | **deleted** → `client.render.fluid.v1.FluidRenderingRegistry.register(source, flowing, FluidModel.Unbaked)` |

Nine of those are one-line rules in the `fabric && >=26` group. The last is a *shape* change — a
model object where there was a render-layer token — so it is a `fabric && >=26` arm in `ClientProxy`
instead. ⚠️ **That arm must sit ABOVE the `else`**: Stonecutter evaluates arms in order and the
`else` is what every Fabric node below 26 takes.

**⚠️ A fabric-api rename can be a rename only by luck.** `ModelLayerRegistry.registerModelLayer`'s
second parameter changed type as well (`Supplier<LayerDefinition>` → `TexturedLayerDefinitionProvider`,
whose method is `LayerDefinition createLayerDefinition()`). The call site survives the one-token rule
untouched *only* because it passes a method reference — `definition::get` — which binds to whichever
functional interface is expected. Had it passed a typed variable, the rename rule would have compiled
into a type error two versions later. Check the parameter types of a "rename", not just the name.

**⚠️ An `elif >=26` arm is not a Forge arm, and on a five-arm chain that is easy to miss.**
`AlexsCaves#PROXY` and `Citadel#PROXY` gate their `DistExecutor` → `FMLEnvironment.dist` swap as
`neoforge && >=1.21` / `>=26` / else. The middle arm was written during the 26.1 *Forge* wave and its
predicate says nothing about the loader, so every Fabric node from 26 up took it and reached for
`net.minecraftforge.fml.loading.FMLEnvironment` — a class Fabric has no stand-in for. What Fabric
does have is the stand-in for the class the **else** arm names, which `!fab-fml-distexecutor`
re-points; scoping the middle arm to `forge && >=26` is the whole fix. **General form: when a wave
adds an arm for one loader, spell the loader — an unqualified version predicate silently claims every
loader that ever reaches that version.**

**MC 26 narrowed `Level#random` to `protected`.** A mixin reads a shadowed field through the
target's own class, so `this.level.random` in `mixin/fabric/ServerPlayerGameModeMixin` stopped
compiling. `Level#getRandom()` has been public across the whole 1.20.1→26.x range and returns the
same instance, so the getter needs no gate — prefer it to an access-widener line whenever one exists.

**Two predictions this file used to carry did *not* come true, and both are now retired.**
`SurfaceRules$Context`'s 26.2 constructor was expected to need an access-widener entry where the
other loaders use an `@Invoker`; it does not — all three `>=26.2` `@Invoker`s resolve on Fabric
exactly as they do on Forge (`SurfaceRulesContextAccessor` is `+3` on both). And `26.2-fabric`
compiled with no source change at all, because the 26.2 wave's vendored render-compat layer
(`client/render/compat/**`, `ACRenderContext`) is loader-neutral by construction.

## Sign-off arithmetic

Every Fabric node's per-file injection delta was diffed against **the same file's delta on the
sibling Forge node across the same MC step**. That cross-check is what turns a number into a proof: a
delta that matches Forge's is a vanilla move, and a delta that does not has to be explained.

Only one divergence survived the 26.x band, and it is the intended one:

| step | file | Fabric | Forge | why |
|---|---|---|---|---|
| 1.21.11 → 26.1 | `EntityMixin.java` | **+1** | +2 | Both gain `ac_trackModFluids` (26 gave vanilla an `EntityFluidInteraction`, so both loaders now use the same `@ModifyArg`). Fabric additionally **loses** `ac_pushInModFluids`, the `fabric && <26` stopgap that existed only because this loader had no fluid-interaction hook at all. Forge never had it, so Forge nets +2. |

Everything else matches file for file, including 26.2's `AdvancementTabMixin +1`,
`DecoratedPotPatternsMixin +1`, `ChunkSectionsToRenderMixin −1`, `LevelRenderStageMixin −2`,
`GuiRendererMixin −1` and `SurfaceRulesContextAccessor +3`.

Headline counts: `1.20.1-fabric` 317 → `1.21.11-fabric` 280 → `26.1.x-fabric` 281 → `26.2-fabric`
282. Access-widener entries fall 83 → 75 → 70 over the same range as gated arms drop out.

## Known gaps — Fabric ships without these

Each is a deliberate hole with a reason, not an oversight. Written down so they are not rediscovered
as bugs:

- **Villager trades are not dispatched.** Below 26 the two underground-cabin-map trades come from
  `ACVillagerTradeEvents`' Forge/NeoForge listeners and nothing on Fabric ever constructed the two
  stub events; from 26 the trades are datapack entries and work on every loader. The two stubs are
  therefore in the `>=26` source-set exclusion beside the class that named them.
- **No `RenderLivingEvent` dispatcher mixin exists** — the event class is present so consumers
  compile, but nothing posts it on this loader.
- The deferred `1.20.1` seam items: HUD-overlay stubs, multipart level plumbing, tool-action
  dispatch, `AcidFluidType#move`, fluid-interaction dispatch below 26.
- ~20 `//? if fabric` gates are still unbounded above; revisit them the next time a version band
  makes one of them wrong.

## Still not done anywhere in the tree

`runClient` has never been run on any of the 58 nodes, and `runServer` has never been run on a 26.x
node. Every verdict in this milestone is `compileJava` + `verify_mixins.py` + `aw_check.py`, per the
standing "test them all at the end" plan.
