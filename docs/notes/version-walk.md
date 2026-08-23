# The version walk — per-wave post-mortems, release-build notes, licensing

**History, not standing instruction.** The walk is finished (58/58 nodes, `1.20.1` → `26.2`, all
three loaders) and `1.0.0` has shipped from it, so nothing here needs reading to do ordinary work.
Read the wave that matches a version you are touching, or the release-build section before cutting
a release. The traps that generalise past their own wave were promoted into
[`gotchas-api.md`](gotchas-api.md) and [`gotchas-runtime.md`](gotchas-runtime.md) — those two are
the ones to read by default.

Contents: where the walk stands · the 26.2, 26.1, 1.21.11, 1.21.10, 1.21.9, 1.21.8, 1.21.7, 1.21.6,
1.21.5, 1.21.4 and 1.21.3 waves · release-build gotchas · licensing.

Split out of `DEVELOPMENT.md` on 2026-08-22 so the always-loaded file stays small — see that file
for the map of which archive holds what. Nothing here has been edited in the move.

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

