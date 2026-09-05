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
stops resolving. Declared runtime floor is **`1.3.6` on all three loaders** — `[1.3.6,)` in both
`mods.toml`s, `>=1.3.6` in `fabric.mod.json` (verified in the shipped `1.0.5` jars). Anything below
`1.3.4` is unusable on Fabric anyway: 1.3.3's Fabric jars demand fabricloader `0.19.3` and refuse to
start. ⚠️ **This floor is the only thing enforcing the dependency for CurseForge users** — CF
relations carry no version, and the codx relation still has to be added to the CF project by hand.

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
6. **`accessTransformers` must be ABSENT from the generated `mods.toml`, not empty.** Forge's
   `ModFile` falls back to `META-INF/accesstransformer.cfg` — exactly where loom puts it — only when
   the key is missing; a present-but-empty `accessTransformers = [ ]` means *this mod has zero access
   transformers*, so the file is never read and every AT-widened member stays inaccessible
   (`IllegalAccessError` at registration on Forge, NeoForge unaffected). Shipped that way through
   `1.0.6`; fixed in `1.0.7` by dropping the field from `ForgeManifest` entirely.

## Deep notes — read the one you need, not all of them

**This file is re-injected into every context window, so it deliberately holds only what changes
what you do by default.** Everything expensive-but-situational lives in `docs/notes/`. Read the row
that matches the work; do not read them all, and do not re-derive what one of them already records.

| Read this | When |
|---|---|
| [`docs/notes/gotchas-api.md`](docs/notes/gotchas-api.md) | Before any MC bump, any new `//?` gate or `replacements.string` rule, and before touching `DataPackMigration`. Every per-version/per-loader API and data-pack trap this tree has hit, with its real boundary |
| [`docs/notes/gotchas-runtime.md`](docs/notes/gotchas-runtime.md) | Before running or trusting a dev server, dev client, RCON battery or sweep. Bugs that survived a green build, plus the rig rules that make a verdict mean something |
| [`docs/notes/version-walk.md`](docs/notes/version-walk.md) | Per-wave post-mortems (26.2 … 1.21.3), release-build gotchas, licensing. History — read the wave matching a version you are touching |
| [`docs/notes/citadel.md`](docs/notes/citadel.md) | Before touching anything under `citadel/` — the 93 vendored classes and 13 mixins, **and the rules for coexisting with the real Citadel mod** |
| [`docs/notes/fabric.md`](docs/notes/fabric.md) | Before touching anything under `fabric/` — the 115-file port, its 69 `!fab-*` rules and 26 dispatcher mixins |
| [`docs/notes/1.0.1-triage.md`](docs/notes/1.0.1-triage.md) | **Live working state** for the current bug-report cycle. Check it first; update its status column as work lands |

## The dozen rules that are always on

Each is one line here and written up in full in the archive named beside it. These are the ones that
silently change the meaning of an ordinary edit or an ordinary test result.

- **Author source in the OLDEST spelling and rewrite upwards.** A `replacements { }` block guarded by
  a Kotlin `if (eval(...))` gets **no reverse pass**, so a helper written in the modern spelling
  compiles on the nodes the rule covers and fails on every node below. → *gotchas-api*
- **`replacements.string` matches plain substrings, boundary-checked on NEITHER edge, and rules do
  NOT chain** — every rule sees the original text. Two bands changing one span means a
  version-dependent Kotlin `val`, not a second rule. → *gotchas-api*
- **Stonecutter does not nest gates, and a bare one-line `//?` uncomments exactly ONE line.** Use the
  braced form for anything longer; put prose *above* the `//? if`, never inside an arm. → *gotchas-api*
- **The ACTIVE node (`1.20.1-forge`) has no live generated tree** — it compiles from `src/`, so never
  diagnose a gate from its `build/generated/`. Check an inactive node. → *gotchas-api*
- **Check mixin targets by DESCRIPTOR, not by name**, and run `scripts/verify_mixins.py` before
  booting anything. A count going *up* after a checker change means something had been unverified;
  a count going *down* needs the same scrutiny — **and do the arithmetic on the delta**, since the
  script reads a `processResources` output for the mixin *config*: run that task on all 58 nodes
  first, or a newly-registered mixin is silently checked on none of them. → *gotchas-runtime*
- **`runServer`'s exit code is backwards** (0 = crashed, 124 = booted). The only verdict is the log:
  `Done (Ns)` present and `Failed to parse` / `Couldn't parse` / `Unknown registry` / `Couldn't load
  advancement` / `Couldn't load tag` / `Missing tag` all absent. → *gotchas-runtime*
- **A green build proves the shapes exist, not that anything calls them.** Grep for the *producer*
  (`new <Event>(`, the `post`, the call site in the patched jar), not just the consumer. → *gotchas-runtime*
- **A vanilla tag can SHRINK, and a predicate built from `optionalFieldOf` fails OPEN.** Both parse
  clean, log nothing, and quietly match everything or nothing. → *gotchas-runtime*
- **Put `@Override` on anything that overrides.** A supertype signature moving is otherwise a silent
  behaviour bug; `scripts/override_audit.py` checks the tree. → *gotchas-runtime*
- **A fluid or blockstate probe that can run during worldgen must never go through `Level`.**
  `Level#getFluidState` -> `getChunkAt` -> `ServerChunkCache#getChunk` is a blocking main-thread round
  trip from a generation worker, so it deadlocks the server or throws `Requested chunk unavailable
  during world generation`. Use `ChunkSource#getChunkNow`, which never forces a load and returns null
  off-thread. -> *gotchas-api*
- **Never key a cache on a whole `BlockState`.** A `Map<BlockState, VoxelShape>` built by enumerating
  *your* properties has no key for the states a third-party mod's extra property creates, `get`
  returns `null`, and vanilla's `BlockStateBase$Cache` dereferences it — a hard startup NPE on Fabric,
  inside your own registration, naming neither the block nor the mod at fault. Key on the properties
  that affect the shape. → *gotchas-api*
- **Fix content bugs in `src/main/resources`, not in a `DataPackMigration` pass** — a source fix is
  correct on all 58 nodes and repairs released versions retroactively; a migration pass can only fix
  the band it is aimed at. The exception is when the *correct* spelling genuinely differs per band.
- **This shell is zsh and applies history modifiers inside expansions**: write `":${node}:runServer"`,
  never `:$node:runServer` (which becomes `:1.20unServer`). Multi-node Gradle work must be **one**
  invocation over an array.
- **Vendoring relocates classes; it does NOT relocate what a mixin merges into a vanilla class.** Every
  interface method, handler name, merged field and `SavedData` string id under `citadel/` must be unique to
  this mod, or installing the real Citadel is a hard `IncompatibleClassChangeError` at class load. Only a
  boot with that jar in `run/mods/` proves it. → *citadel.md*
- **AC dev servers are pinned off 25565** (`versions/<node>/run/server.properties`, gitignored) so the
  neighbouring repos' servers cannot be mistaken for a regression here — **25599 on 55 nodes, 25597 on
  `1.20.1-forge` / `1.21.11-neoforge` / `26.2-fabric`** (rcon 25596, password `acctest`) so two AC
  nodes can run at once. Read the file rather than assuming; a crashed server keeps `session.lock`,
  and the pid comes from `ss -lntp | grep 2559`.

## Standing scripts

Run these rather than re-deriving what they check. All take no arguments unless noted.

| Script | Checks |
|---|---|
| `verify_mixins.py` | Every injection point on every node against the loader-patched jar. **16507 across 58 nodes**. Run `processResources` on all 58 first when the mixin config changed |
| `aw_check.py` | Access-widener entries. ⚠️ Takes **MC versions**, not node names |
| `event_audit.py` | Every `@SubscribeEvent` event has a producer under `fabric/` |
| `synced_data_audit.py` | `defineId(X.class)` names the declaring class; declared == defined |
| `ai_attribute_audit.py` | Every attribute a mob's vanilla AI reads is one its supplier declares |
| `override_audit.py` | Methods shadowing a vanilla supertype member without `@Override` |
| `posestack_audit.py` | A locally-constructed `PoseStack` never pops its base pose |
| `submit_flush_audit.py` | Every `new ACSubmitBuffers(...)` is `flush()`ed — an unflushed recorder draws nothing, silently (26.2) |
| `text_alpha_audit.py` | Every text-draw colour literal has a non-empty alpha byte — vanilla stopped filling one in at 1.21.6, and a 0 alpha draws nothing |
| `model_audit.py` | Face `uv`s in range, texture slots resolve from the bake roots and are atlas-resident (`--fix`) |
| `lang_audit.py` | Every registered block/item/entity/effect/tab has the translation key vanilla actually asks for. A block-backed item only gets the `block.` key from 1.21.2 if it opts in |
| `sound_audit.py` | `SoundEvent` ↔ `sounds.json` ↔ subtitle ↔ `en_us`, in all four directions |
| `tagshrink.py` / `tagdiff.py` | Vanilla tag **membership** per MC version — not merely whether the tag exists |
| `verify_at.py` | Access-transformer entries |
| `mcjavap.py` / `vprobe.sh` / `probe_worldgen.py` / `rcon.py` | Per-node bytecode probes and the in-world RCON rig |
| `mcdrive.py` / `glow_shots.sh` / `block_shots.sh` / `block_occlusion_test.sh` | The dev-client input rig: matched-frame A/B capture of an entity, a block, and a depth-occlusion check. Rules in `gotchas-runtime.md` |
| `gen_icon.py` / `gen_enchantments.py` | Generated assets — re-run rather than editing the outputs |
| `pin_drift.py` | Loader pins in `stonecutter.properties.toml` vs the latest published build. Run before a release — a NeoForge point release keeps moving after MC freezes, and 26.1.2.87→.97 changed API |
| `modrinth_upload.py` / `curseforge_upload.py` | Release uploads. `--only <node>` first, always |

## 1.0.9 shipped (2026-09-05)

**Five player reports in `reports/report1…5`** — and two of them turned out to be the *same* 26.2
defect seen from two directions. Four fixes, one report not reproduced, two sub-items still open for
want of a screenshot and a third-party mod. Full triage in
[`docs/notes/1.0.1-triage.md`](docs/notes/1.0.1-triage.md) ("Eleventh wave").

| Report | Root cause | Blast radius |
|---|---|---|
| Hitting the "boss dino" crashes the client (26.2 NeoForge), and interacting with a scaled Tremorzilla crashes too | Seven multipart owners build their `PartEntity`s in their own constructor. `Entity.<init>` takes `level.getNextEntityId()`, which `ServerLevel` overrides with a real counter and the base `Level` answers **0** — so a part built client-side never had an id. Harmless until **26.2**, where `Entity.getId()` grew a `throw new IllegalStateException("Tried to access entity ID before ID assignment")` (13-instruction body vs 5 on `1.20.1`→`26.1.2`, read with `javap -c` across 14 versions). Any interact or attack landing on a part hit it. The seven now override `setId` and hand parts `parentId + i + 1` via `ACMultipartOwner#acAssignPartIds` — **vanilla's own mechanism**, which `EnderDragon` uses through 1.21.11 and moved into `recreateFromPacket` at 26.2 (our override still fires: `recreateFromPacket` calls `setId` last) | crashes on **26.2**; the hook is client-only in practice, so safe on all 58 |
| Underwater flashing, then the screen fills with flat biome fog (1.21.5 Fabric) | `ClientEvents`' fog band-aid re-reads `RenderSystem`'s fog distance as its default so a foreign mod's value is not clobbered — gated `>=1.21.6`. The real boundary is **1.21.2**, where `setupFog` stopped writing `RenderSystem` and started *returning* an immutable `FogParameters` the caller writes later, so at event time the getter holds the **previous frame's already-multiplied** value. It compounds per frame until the fog is on the camera. Gate moved to `>=1.21.2` | `1.21.2` → `1.21.5`, all loaders |
| Crash placing a pewen boat (`mclo.gs/94zKrOc`) | Two defects stacked. (1) `AlexsCavesBoatRenderer<T extends Boat & AlexsCavesBoat>` — 1.21.2 moved chest boats onto `ChestBoat → AbstractChestBoat → AbstractBoat`, making them siblings of `Boat`, so the erased checkcast could not pass; bound gated onto `AbstractBoat`, and a new `!mc2111-pkg-abstractboat` rule follows the 1.21.11 package move. (2) `AlexsCavesBoat` declared `getRowingTime(int,float)` and relied on the **vanilla superclass** to supply the body — which only works where the runtime is Mojmap. On Fabric the vanilla method is intermediary-named, nothing implemented the interface method, and rendering either boat threw `AbstractMethodError`. Renamed `acGetRowingTime`, delegated on both entities | >=1.21.2 for (1); all Fabric nodes for (2) |
| AC gear offers no enchantments at an enchanting table, but naturally-generated books carry the cave enchantments (Fabric) | From 1.21.2 `ItemStack#isEnchantable` is purely `has(DataComponents.ENCHANTABLE)` and `EnchantmentMenu` asks it. Forge and NeoForge each stamp that component from `ACEnchantableItem#getEnchantmentValue`; **Fabric had no such arm**, so all thirteen enchantable AC items were unenchantable. Books were unaffected because `EnchantmentHelper#getAvailableEnchantmentResults` special-cases `Items.BOOK` — the exact asymmetry the reporter described, and what identified the defect. New `//? if fabric && >=1.21.2` arm on `DefaultItemComponentEvents.MODIFY` | 14 Fabric nodes (>=1.21.2) |

**The two reusable traps**, both worth carrying anywhere:

- ***A mod interface may never expect a VANILLA supertype to satisfy it.*** Declare a mod-unique
  name and delegate. The vanilla-named version compiles and runs on Forge/NeoForge — where the
  runtime is Mojmap — and throws `AbstractMethodError` on Fabric, where the vanilla method is
  intermediary-named. Nothing in a build or in `verify_mixins.py` can see it.
- ***A generic bound on a renderer is a checkcast.*** `EntityRenderer<T extends Vanilla & Mine>`
  erases `T` to the vanilla half, so a version that re-parents one of your entities away from that
  class turns registration into a `ClassCastException` — and the registration site being raw is
  exactly why it still compiles.

**Report #4 — the giant-sweetberry "x-ray" — was NOT reproduced**, over four rigs on
`26.2-neoforge`: a 9×9×4 mass at spawn (void: unlit cavern), the same at `y=200` lit (the "grey
panels" resolved by PIL to `(110,107,90)`, a darkened beige, not stone `(122,122,122)`), an open-air
A/B wall on a red backdrop, and finally two identical hollow 9×9×5 masses — ice cream vs a
`white_concrete` control, both berry-capped, on a **red concrete floor**, viewed from inside each at
pitch ±45 — all solid, control matching. The red floor is the lesson from rig 2: inside a beige room
a neutral grey is ambiguous between "hole showing stone" and "shading", so give the rig a ground
plane that cannot be mistaken for one. Static analysis agrees — every occlusion path in
`IceCreamBlock` and the berry **under**-occludes (the UP face occlusion shape of a TYPE 0/1 ice
cream block is *empty*; `isSolidRender` is false for every state), and under-occlusion draws extra
faces, it cannot open a hole. One unrelated correctness fix shipped anyway: `GiantSweetberryBlock`
never called `.noOcclusion()` despite a berry-on-a-stem model, so it took part in face occlusion and
light blocking it has no business in. It is **not** claimed as the fix.

**Still open**, both from report #2 and both needing something this tree does not have: "some of the
sides of the diving helmet have a texture problem" — a PIL pass over `diving_suit_0.png` against the
model's box UVs found nothing off-sheet, nothing overlapping, and only two fully-transparent faces,
both inward-facing and never visible; it needs a screenshot. And the Skin Layers 3D headwear
overlap, which needs that mod.

**Honest verification gaps.** Only the multipart fix was driven in-world. On `26.2-neoforge`, an
in-mod self-test reported `owner=LuxtructosaurusEntity ownerId=413 parts=7` → all seven
`getId=414…420 interact=Pass[] attack=ok`; with the ids forced back to zero all three calls **threw**
`IllegalStateException`; restored, green again — then the same for `TremorzillaEntity ownerId=489
parts=5`. The fog, boat and enchantment fixes are **compile-verified and artifact-verified but not
runtime-verified**; each is a small local change whose pre-fix behaviour is visible in the source,
and the boat one is named by the reporter's own crash log.

Verified: release build `BUILD SUCCESSFUL`, 727 actionable tasks, **58 release jars** (22 fabric /
18 forge / 18 neoforge), 0 `-SNAPSHOT`, every filename `alexscaves-1.0.9-*`; `verify_mixins.py`
**16589 injection points across 58 nodes, all resolving** — unchanged from 1.0.7/1.0.8, as expected
since no mixin moved. Shipped bytecode spot-checked with `javap`: `void setId(int)` present on all
seven owners in the `26.2-fabric` jar, `acAssignPartIds` on the interface with its array rewritten to
`net.neoforged.neoforge.entity.PartEntity` on NeoForge, the renderer bound erased to
`…vehicle.boat.AbstractBoat` on 26.2, `acGetRowingTime` on `AlexsCavesBoat`,
`modifyFabricDefaultComponents` in the Fabric `AlexsCaves`, `noOcclusion` in
`GiantSweetberryBlock`, `version = "1.0.9"` with **no** `accessTransformers` key in the Forge
`mods.toml` (and `META-INF/accesstransformer.cfg` present, 7173 bytes), and
`depends.codxlib >=1.3.6`. Loader pins unchanged from 1.0.1; `pin_drift.py` reports **18** behind
(was 17 at 1.0.8), none API-relevant — including `neoforge 26.2.0.66 → 26.2.0.76`, i.e. report #1's
reporter is one point release ahead of our pin and that is not what crashed them.

Both stores carry all 58 files. **Modrinth** `cO2CvXug`: 58 versions (`uploaded=57 skipped=1
failed=0` — the pilot node is the skip), then **every one re-read with a fresh
`GET /v2/version/{id}`** — one file, one loader, one MC version, `# 1.0.9` changelog, `6oyMM4yX`
required and project-level on all 58, `P7dR8mSH` on all 22 fabric and on **none** of the other 36;
**0 problems**. Featured nine moved to `1.0.9+{fabric,forge,neoforge}-{1.21.11,26.1.2,26.2}` and the
1.0.8 nine turned off — 18 slots, and the apply-and-reconcile loop converged on pass 2 with **0
mismatched**. **CurseForge** `1645389`: 58 files, `uploaded=57 skipped=1 failed=0 unsupported=0`,
ledgered under `1.0.9/*` in `scripts/.cf_uploaded.json` (22/18/18) — no transient 500s this run.

⚠️ Still owed by hand, and now **six** releases old: the **CurseForge `codxlib` relation** (CF
relations carry no version and the upload API has no field for them, so it is web-UI only — without
it CF users get no dependency prompt at all) and the **LGPL source offer** for
`Codx-org/AlexsCavesContinued`.

**Paste-ready replies for all five reporters** are in `reports/REPLIES-1.0.9.md`.

⚠️ **A 58-node build needs the heap from `gradle.properties`, and the command-line override
REPLACES it.** The first release pass here passed `-Dorg.gradle.jvmargs="-Xmx3G
-Djava.io.tmpdir=…"` — the `-Xmx3G` copied from a workspace note written for a different mod — and
died with **8 failures, every one `Java heap space`**, in KSP and Stonecutter's `PrepareAction`, on
the 26.1.x nodes. Nothing about the message says "your `-Xmx` is wrong"; it reads like a per-node
Stonecutter fault. This tree's `gradle.properties` says `-Xmx8G`: repeat *that* value, not the one in
the note.

## 1.0.8 shipped (2026-08-30)

**Five player reports in `reports/report1…5`** — three real defects, one feature request implemented,
one report whose three parts split three ways — plus **a fourth defect nobody reported**, found while
verifying the feature request. Full triage in
[`docs/notes/1.0.1-triage.md`](docs/notes/1.0.1-triage.md) ("Tenth wave").

| Report | Root cause | Blast radius |
|---|---|---|
| Cave maps say the biome is not nearby while `/locate biome` finds it | `MultiNoiseBiomeSourceMixin` learned its seed and dimension **only from `ChunkStatusMixin`**, i.e. only while terrain was actively generating. A world already generated leaves the seed at `0`; more than one dimension loaded leaves whichever generated last. The map asks `ACBiomeRarity` directly and gets the right layout, the biome source answered from a stale one, and the two disagreed — `/locate biome` kept working because it goes through the biome source's own index and is therefore self-consistent. The source now resolves its owning level **once** by walking `getAllLevels()` for the one whose generator holds *this* biome source, and freezes both fields | all nodes |
| Nuclear Furnace will not place on 26.2 | Multiblock assembly used a default-flag `setBlockAndUpdate`, so vanilla re-ran `canSurvive` on every neighbour — including the components not yet converted. A half-converted neighbour does not survive, so each piece popped as the next went up. Now assembles with `UPDATE_NEIGHBORS \| UPDATE_CLIENTS \| UPDATE_KNOWN_SHAPE` | all nodes |
| Subterranadon and Grottoceratops shake their heads violently while idle | `DinosaurEntity` set `yHeadRot` every tick but never moved `yBodyRot` toward it. Vanilla's damping lives in `Mob#tickHeadTurn`, which their custom tick path never reaches, so the head clamped at `getMaxHeadYRot()`, snapped back, and clamped again. New `followHeadWithBody()` reproduces vanilla's damping | all nodes |
| *(unreported)* Six of seven post-process shader chains silently dead on Fabric `<1.21.2` | Legacy `PostPass`/`EffectInstance` builds its program id by **concatenating** `"shaders/program/" + name + ".json"`, and the one-argument `ResourceLocation` constructor splits on `:` — so `alexscaves:blur` yields namespace `shaders/program/alexscaves` and takes the whole chain down. Forge and NeoForge patch that constructor; **Fabric does not**, and upstream AC was Forge-only. New `DataPackMigration.unnamespacePostProgramsBelow1212` flattens the assets into `assets/minecraft/shaders/program/alexscaves_*` | **8 Fabric nodes** (`1.20.1` → `1.21.1`) |

**New in this release**, both from report #5: the Hologram Projector now renders **any** entity —
`NotorRenderer` had a `LivingEntityRenderer` branch and a `FerrouslimeRenderer` branch and nothing
else, so most modded mobs and vanilla's own Ender Dragon simply did not draw; the new arm lets the
entity's own renderer draw through `ACHologramBuffers`, a `MultiBufferSource` wrapper that swaps
every requested render type for the hologram type. And it gained a **seven-step size cycle**
(`0.25 … 3.0`), crouch-use with an empty hand, persisted to NBT, with `getRenderBoundingBox()`
inflated so a 3× hologram does not cull at its own edge. Cave Tablet, Cave Codex, Cave Map and Biome
Treat are now `Rarity.UNCOMMON` — the last on **both** gated `BiomeTreatItem` arms, since missing the
second would have shipped the colour on 6 nodes and not the other 52.

Two reports closed without a code change: the pewen-branch "black spaces" on 1.20.1 was **not
reproduced** (a PIL pass over every mod blockstate → model → parent chain found **0** blocks
referencing an alpha-bearing texture with no `render_type` on the chain, and a real pewen tree renders
correctly on `1.20.1-fabric`), and the unloaded-chunk fluid deadlock was the ninth wave's #3/#10/#12,
already fixed in `1.0.7`.

⚠️ **Two traps recorded for the next hologram or pewen test.** `/setblock`-ing a bare
`alexscaves:pewen_branch` looks like it renders nothing — it does not, the branch needs a supporting
pewen log and **self-removes the tick after placement**; test with the feature, not the block. And
`displayEntity` is cached on the projector block entity and `onDataPacket` never invalidates it
(upstream behaviour, left alone), so `/data merge block` will not change what the client draws —
replace the block.

**The general trap**, worth carrying to any multiloader mod with post-process shaders: *a loader that
patches a vanilla constructor hides an asset-layout bug from every mod built against it.* Anything
upstream got away with because Forge patched `ResourceLocation` has to be re-checked on Fabric, and
the symptom is a `WARN` in a boot log nobody reads plus an effect that is simply absent. The boundary
was established empirically, not assumed — probe boots gave `1.21.1-fabric` **3** failures,
`1.21.4-fabric` **0**, `1.21.11-fabric` **0**, `26.2-fabric` **0**.

**Honest verification gaps.** #5 and the shader fix were driven in-world on `1.20.1-fabric` (crouch-use
advanced the scale `1.0 → 1.5` and persisted; an **Ender Dragon** drew as a hologram through the new
generic branch; post-fix boot logged **zero** `Failed to load shader`). The biome-source, furnace and
dinosaur fixes are **compile-verified and artifact-verified but not runtime-verified** — each needs a
rig this tree does not have (a pre-generated multi-dimension world with a cave map, a 26.2 client with
the furnace recipe, an idle dinosaur observed over time). Each is a small local change whose pre-fix
behaviour is visible in the source.

Verified: 58-node `classes` `BUILD SUCCESSFUL in 14m 29s`, 0 errors; release build
`BUILD SUCCESSFUL in 17m 57s`, 727 actionable tasks, **58 release jars** (22 fabric / 18 forge /
18 neoforge), 0 `-SNAPSHOT`; `verify_mixins.py` **16589 injection points across 58 nodes, all
resolving** — unchanged from 1.0.7, as expected since no mixin moved. Shipped assets and bytecode
spot-checked: the shader flattening is present on exactly the **8** Fabric `<1.21.2` nodes (13
`assets/minecraft/shaders/program/alexscaves_*` each, **0** leftover namespaced) and on none of the
other 50 — `1.20.1-forge` still ships them namespaced and `1.21.2-fabric` ships the modern
`post_effect/` layout, so the migration's boundary is artifact-proven in both directions;
`bipush 19` (`UPDATE_NEIGHBORS | UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE`) at both furnace `setBlock`
sites; `ac_resolveOwningLevel`, `followHeadWithBody`, `ACHologramBuffers`, `HOLOGRAM_SCALES` and the
four `Rarity.UNCOMMON` items all present; `version = "1.0.8"` with **no** `accessTransformers` key in
the Forge `mods.toml` and `META-INF/accesstransformer.cfg` present. Loader pins unchanged from 1.0.1;
`pin_drift.py` reports **17** behind (was 16 at 1.0.7), none API-relevant.

Both stores carry all 58 files. **Modrinth** `cO2CvXug`: 58 versions (`uploaded=57 skipped=1
failed=0` — the pilot node is the skip), then **every one re-read with a fresh
`GET /v2/version/{id}`** — one file, one loader, one MC version, `# 1.0.8` changelog, `6oyMM4yX`
required and project-level on all 58, `P7dR8mSH` on all 22 fabric and on **none** of the other 36;
**0 problems**. Featured nine moved to `1.0.8+{fabric,forge,neoforge}-{1.21.11,26.1.2,26.2}` and the
1.0.7 nine turned off — 18 slots, and the apply-and-reconcile loop converged on pass 2 with **0
mismatched** (keep the loop regardless; a clean pass is not evidence 1.0.6's silent drop is gone).
**CurseForge** `1645389`: 58 files ledgered under `1.0.8/*` in `scripts/.cf_uploaded.json`
(22/18/18). ⚠️ The batch reported `uploaded=54 skipped=1 failed=3` — **all three 1.21.10 nodes**
returned the documented transient bare `HTTP 500` back to back, which looks like a per-MC-version
taxonomy problem and is not one: each landed on a plain `--only` re-run with no payload change.

⚠️ Still owed by hand, and now **five** releases old: the **CurseForge `codxlib` relation** (CF
relations carry no version and the upload API has no field for them, so it is web-UI only — without
it CF users get no dependency prompt at all) and the **LGPL source offer** for
`Codx-org/AlexsCavesContinued`.

## 1.0.7 shipped (2026-08-28)

**Twelve player reports in `reports/report1…12`** — five real defects, and four of the remaining
seven were already fixed in `1.0.6`. Full triage in
[`docs/notes/1.0.1-triage.md`](docs/notes/1.0.1-triage.md) ("Ninth wave").

| Report | Root cause | Blast radius |
|---|---|---|
| Bootstrap crash `Block id not set` (26.1.2 NeoForge + Sinytra Connector + Farmer's Delight Refabricated) | From 1.21.2 a `BlockBehaviour.Properties` carries the block's id. The `ACRegistryIds.constructing` stamp ran on the **builder calls**, so a `Properties` passed straight to a constructor kept a null id — and `ACBlockRegistry` held **22 shared `static final … _PROPERTIES` constants** built in `<clinit>`, outside every constructing window, used by ~162 registrations. Both mixins now stamp at `<init>` RETURN and all 22 constants are per-use factory methods | >=1.21.2, all loaders |
| AC armour renders **nothing** on NeoForge 1.21.1 | NeoForge 21.1 adds a 12-arg `renderArmorPiece(…HumanoidModel,FFFFFF)` that `render` actually calls; the 6-arg vanilla one is only a bridge. The `else` arm resolved, applied and never ran, so the cancel never happened and vanilla drew from the material's layer list — which `ACArmorMaterial#vanilla` deliberately leaves empty. New `neoforge && >=1.21.1 && <1.21.2` arm | `1.21.1-neoforge` only |
| Worldgen stalls, freezes, and `Requested chunk unavailable during world generation` | `ACFluids.modFluidHeight`/`isEyeInModFluid` called `Level#getFluidState`, which reaches `ServerChunkCache#getChunk` — off-thread that posts to the main-thread executor and `join()`s. Vanilla Ocean Ruins place a zombie during generation → `Entity.save` → `isInWater` → our check → deadlock or the "unavailable" crash. Both probes now use `ChunkSource#getChunkNow` (never forces a load, null off-thread) with a per-column cache | all nodes |
| Crash on Forge 1.20.1 with only ACC + codxlib | `accessTransformers = [ ]` present-but-empty in the generated `mods.toml` means *this mod has zero ATs*, so Forge never reads `META-INF/accesstransformer.cfg` and every AT-widened member stays inaccessible. The key is now **absent** | 18 Forge nodes |
| TerraBlender `26.2.0.0.2` surface rules silently unused | `SurfaceRuleManager$RuleBuilder extends Function<HolderGetter<Biome>, RuleSource>` replaced the bare `RuleSource` parameter. `ModCompatBridge` now tries the legacy signature and falls back to a `java.lang.reflect.Proxy` `RuleBuilder` | 26.2, all loaders |

Also hardened: using an Occult Gem on a Beholder with **no player** holding it (a deployer) — report
#9's unreproduced crash — no longer NPEs.

**The general trap** (#3/#10/#12), now worth carrying anywhere: *a fluid/blockstate probe that runs
during worldgen must never go through `Level`.* `Level#getFluidState` → `getChunkAt` →
`ServerChunkCache#getChunk` is a blocking main-thread round trip from a worker; `getChunkNow` is the
non-forcing read. One reporter's spark profile had **19% of worldgen time** in that call.

**Proven by reproduction.** Same-seed A/B on `1.21.11-fabric`: post-fix **4624 chunks /
41,658,550 bytes** with RCON responsive and zero errors; pre-fix wedged at **2,278,754 bytes** with
RCON dead, and `jstack` named the chain `OceanRuinPieces$OceanRuinPiece.handleDataMarker →
addFreshEntityWithPassengers → ProtoChunk.addEntity → Entity.save → Zombie.addAdditionalSaveData →
Entity.isInWater → ac_isInWater → ACFluids.modFluidHeight → Level.getFluidState →
ServerChunkCache.getChunk → join()` on Worker-Main-14 while the Server thread sat in
`MainThreadExecutor.managedBlock`. Two corrections came out of it: the trigger is a **vanilla**
structure, not an AC one (all 91 AC templates re-parsed, none contains an entity), and the defect
**hangs as readily as it crashes**. #2/#5 were confirmed in a `1.21.1-neoforge` dev client — five
armour stands wearing diving, hazmat, primordial, gingerbread and darkness sets, all drawing.
#1 was confirmed on the reporter's own stack (`:26.1.2-neoforge:runServer` with Connector + FD
Refabricated, `Done (0.236s)`); #6 with `terrablender-26.2.0.0.2.jar` on `26.2-fabric`
(`Done (1.557s)`, bridge logs the hand-off, zero "could not be reached" warnings). #8 is
artifact-verified only — a dev run cannot test it, since loom applies ATs itself.

Verified: release build `BUILD SUCCESSFUL in 19m 22s`, 727 actionable tasks, **58 release jars**
(22 fabric / 18 forge / 18 neoforge), 0 `-SNAPSHOT`; `verify_mixins.py` **16589 injection points
across 58 nodes, all resolving** — **+82 over 1.0.6's 16507**, exactly the two new
`Properties.<init>` injects × the 41 nodes their `>=1.21.2` gate covers (14 fabric / 13 forge /
14 neoforge). Shipped bytecode spot-checked with `javap`: the 12-arg `ac_renderArmorPiece` in the
`1.21.1-neoforge` jar, two `getChunkNow` sites in `ACFluids`, `version = "1.0.7"` and **no
`accessTransformers` key** in the Forge `mods.toml` (with `META-INF/accesstransformer.cfg` present).
Loader pins unchanged from 1.0.1; `pin_drift.py` reports 16 behind, none API-relevant.

Both stores carry all 58 files. **Modrinth** `cO2CvXug`: 58 versions (`uploaded=57 skipped=1
failed=0` — the pilot node is the skip), then **every one re-read with a fresh
`GET /v2/version/{id}`** — one file, one loader, one MC version, `# 1.0.7` changelog, `6oyMM4yX`
required and project-level on all 58, `P7dR8mSH` on all 22 fabric and on **none** of the other 36;
**0 problems**. Featured nine moved to `1.0.7+{fabric,forge,neoforge}-{1.21.11,26.1.2,26.2}` and the
1.0.6 nine turned off — 18 slots patched in one pass and the reconcile re-read reported **0
mismatched**, so 1.0.6's silent-drop behaviour did not recur (keep the apply-and-reconcile loop
regardless; a single clean pass is not evidence the drop is gone). **CurseForge** `1645389`: 58
files, `uploaded=57 skipped=1 failed=0 unsupported=0`, ledgered under `1.0.7/*` in
`scripts/.cf_uploaded.json` (22/18/18) — no transient 500s this run.

⚠️ Still owed by hand, and now three releases old: the **CurseForge `codxlib` relation** (CF
relations carry no version and the upload API has no field for them, so it is web-UI only — without
it CF users get no dependency prompt at all) and the **LGPL source offer** for
`Codx-org/AlexsCavesContinued`.

## 1.0.6 shipped (2026-08-26)

**A startup crash in a 743-mod pack that named no mod but ours** — `mclo.gs/t1PKU1o`, MC 26.2 /
Fabric. The reporter had bisected it to "Alex's Caves + Alex's Mobs + both Farmer's Delight compat
mods", and none of those were ever involved: that combination boots clean here (`Done (1.657s)`,
48 mods; +ferritecore `Done (1.707s)`, 49). Full triage in
[`docs/notes/1.0.1-triage.md`](docs/notes/1.0.1-triage.md) ("Eighth wave"); the rule it produced is
now one of the always-on rules above.

| Report | Root cause | Blast radius |
|---|---|---|
| Game dies during startup in a large pack, stack ends inside our own `DeferredRegister$Entry.resolve` | `AbyssmarineWallBlock` held two `Map<BlockState, VoxelShape>` caches (9720 keys each) **keyed on whole-`BlockState` identity**. `BlockState` equality *is* identity, and the maps were built by enumerating *this mod's* properties, so the moment any third-party mod widens that block's state-definition product every lookup misses. `getCollisionShape` returns `null`, and vanilla's `BlockStateBase$Cache.<init>` calls `.isEmpty()` on it — a hard NPE naming neither the block nor the mod at fault | all 58 nodes, whenever another mod adds a property to that block |
| A charged Tremorzilla's dorsal plates never brighten | Upstream pulsed `sin(ageInTicks*0.2)*0.15 + 0.5` **regardless of charge** — being powered only swapped in a texture with more lit pixels, it never raised the alpha, and the glow is additive so alpha *is* brightness. `1.0.4`'s `TYPE_CACHE` removed an accidental double draw, which had been the only thing making the plates read as bright; the players' "they don't light up any more" was that fix seen from the other side. Powered now pulses `0.70..1.00` — the same peak the double draw reached, without the flicker | all nodes, since 1.0.4 |

The wall fix is two 162-entry `VoxelShape[]` arrays indexed on the five properties that actually
change the outline (`UP` × four `WallSide`s); the four altar/water properties the block also carries
are deliberately not part of the key. `createBlockStateDefinition`, `tick`, `updateShape`,
`getStateForPlacement`, `applyWallShape` and `registerDefaultState` are byte-unchanged.

**Proven by reproduction, not inference.** All 80 shape-getter overrides in the tree were audited —
only `AbyssmarineWallBlock`'s two can return `null`. Adding one stand-in `BooleanProperty` to the
*pre-fix* block reproduces the reporter's stack frame-for-frame; the fixed block boots
`Done (1.518s)` with that property still present, plus lithium `0.25.3` and moreculling `1.8.1`. The
mod among the 743 that adds the property was never identified (31 candidate jars scanned) and does
not need to be.

**Why it is a *startup* crash, and only on Fabric.** The defect is loader-neutral — the timing is
not. `fabric-registry-sync-v0`'s `initShapeCache` mixes into `Blocks.<clinit>` to add a
`RegistryEntryAddedCallback` that runs `BlockStateBase::initCache` over **every possible state at
registration time**, so the NPE lands inside our own registry flush before any other mod appears on
the stack. On Forge/NeoForge the same null would wait until something asked for that state's shape.

**The general trap** — now an always-on rule above: *never key a cache on a whole `BlockState`.* A
`Map<BlockState, …>` built from your own property enumeration has no key for the states a third-party
mod creates, and the failure surfaces as an NPE inside vanilla with your mod at the top of the stack.
Key on the properties that affect the value.

A second report arrived in the same window and is **not a bug** — `Mod alexscaves requires codxlib
1.3.6 or above / Currently, codxlib is not installed`, MC 1.20.1 / Forge 47.4.23, `Suspected Mods:
NONE`. The manifest floor did exactly its job. ⚠️ The systemic half is still owed by hand: the
**CurseForge project has no `codxlib` relation**, so CF users get no dependency prompt at all.

Verified: release build `BUILD SUCCESSFUL in 23m 39s`, 727 actionable tasks, **58 release jars**
(22 fabric / 18 forge / 18 neoforge), 0 `-SNAPSHOT`; `verify_mixins.py` **16507 injection points
across 58 nodes, all resolving** — unchanged from 1.0.3–1.0.5, as expected since no mixin moved. The
`26.2-fabric` jar spot-checked with `javap`: `VoxelShape[] shapeByIndex` / `collisionShapeByIndex`,
both `shapeIndex` overloads, `version: 1.0.6`, `depends.codxlib >=1.3.6`, `depends.fabric-api
>=0.155.2+26.2`.

Both stores carry all 58 files. **Modrinth** `cO2CvXug`: 58 versions (`uploaded=57 skipped=1
failed=0` — the pilot node counts as the skip), then **every one re-read with a fresh
`GET /v2/version/{id}`** — one file, one loader, one MC version, changelog present, `6oyMM4yX`
required and project-level on all 58, `P7dR8mSH` on all 22 fabric and on **none** of the other 36;
**0 problems**. **CurseForge** `1645389`: 58 files, `failed=0 unsupported=0`, ledgered under
`1.0.6/*` in `scripts/.cf_uploaded.json` (22/18/18) — no transient 500s this run. Loader pins
unchanged from 1.0.1; `pin_drift.py` reports 16 behind, none API-relevant.

⚠️ **A rapid batch of `PATCH /v2/version/{id}` `{"featured": …}` calls SILENTLY DROPS most of
them.** All 18 PATCHes here returned success; a re-read ~2 minutes later — with fresh single-version
GETs, not the cached project listing — showed **10 of 18 still holding the old value**, and they were
still wrong on a second read after that, so this is a genuine drop rather than the documented
read-back lag. A second pass over only the wrong ones took all 10 and the third read converged. Treat
the featured flag as **apply-and-reconcile**: loop `read → patch the mismatches → wait → read` until
a pass reports zero, instead of trusting the PATCH responses or diagnosing an apparently-failed call.

## 1.0.5 shipped (2026-08-25)

**One report, one crash, and it made the mod unplayable beside Citadel** — `mclo.gs/gBhSDVR`, MC
1.21.1 / NeoForge, Citadel 2.7.1 + ACC 1.0.4. Full triage in
[`docs/notes/1.0.1-triage.md`](docs/notes/1.0.1-triage.md) ("Sixth wave"); the rule it produced is in
[`docs/notes/citadel.md`](docs/notes/citadel.md).

| Report | Root cause | Blast radius |
|---|---|---|
| Game dies at startup with the real Citadel installed | Both Citadel's `MinecraftServerMixin` and our vendored copy make `MinecraftServer` implement their own `ModifiableTickRateServer`, and both declare `default void resetGlobalTickLengthMs()`. `MinecraftServer` inherits two same-signature defaults and cannot load — `IncompatibleClassChangeError: Conflicting default methods`, before any mod code runs | all 58 nodes, whenever both are installed |
| *(masked by the above)* `ClassCastException` every server tick | `CitadelServerData` kept upstream's `SavedData` id `"citadel_world_data"` verbatim, and `DimensionDataStorage` is keyed by a plain string — whichever mod asked second got the other's instance. Now `alexscaves_citadel_world_data` | all nodes |

The fix is a **rename pass**, no injection point or gate moved: interface methods `ac`-prefixed
(`acSetGlobalTickLengthMs`, `acGetMasterMs`, `acResetGlobalTickLengthMs`, `acGetCitadelEntityData`,
`acSetCitadelEntityData`, `acOnSaveData`), `citadel_x` → `acc_citadel_x` on every handler in
`mixin/citadel/` and `mixin/client/citadel/` plus the two in AC's own `EntityMixin` /
`FallingBlockEntityMixin`, merged fields renamed (`accModifiedMsPerTick`, `accMasterMs`,
`accMasterTick`, `ACC_CITADEL_DATA_INSTALLED`, `accSplashTextColor`), and the two vendored tick
loggers renamed `alexscaves-citadel-*`.

**The general trap** — now an always-on rule above: *vendoring relocates classes; it does NOT relocate
what a mixin merges into a vanilla class.* Four channels collide, and only the first one is loud:
interface `default`s (hard `IncompatibleClassChangeError`), merged methods with identical
name+descriptor (Mixin silently drops the loser by priority), merged fields without a unique prefix,
and shared **string** ids. None of them can be caught by a build, by `verify_mixins.py`, or by any
single-mod dev run — **only a boot with the other mod's jar present.** ⚠️ **AlexsMobsContinued vendors
Citadel the same way** and still merges `get`/`setCitadelEntityData` into `LivingEntity` with
upstream's descriptors; channels 2–4 apply there, though it cannot produce this crash.

Verified: 58-node release build `BUILD SUCCESSFUL in 24m 4s`, 727 tasks, **58 release jars**
(22 fabric / 18 forge / 18 neoforge), 0 `-SNAPSHOT`; `verify_mixins.py` **16507 injection points
across 58 nodes, all resolving** — unchanged from 1.0.4, as a rename must leave it; the shipped
bytecode spot-checked with `javap` for the renamed interface members and the new saved-data string.
**Booted `1.20.1-forge` with real Citadel 2.6.3 *and* Rats 8.1.3** — dev client to the title screen,
then `--quickPlaySingleplayer` into a world (`Dev joined the game`, spawn prepared in 2011 ms) and
left ticking: **zero** `IncompatibleClassChangeError` / `ClassCastException` / `AbstractMethodError` /
`NoSuchFieldError` / mixin-conflict lines. Earlier the same fix was booted on `1.21.1-neoforge` with
Citadel 2.7.1, server and client.

⚠️ **A published classic-Forge jar (<1.20.6) dropped into `versions/<node>/run/mods/` dies at
bootstrap**: those jars are SRG-named and the arch-loom dev client runs Mojmap. Refmaps *are* remapped
at runtime, but a mixin's own bytecode is not, so Citadel's static `EntityDataAccessor` merged into
`LivingEntity.<clinit>` throws `NoSuchFieldError: f_135042_`. Consume such a jar as a **mod
dependency** instead — `build.forgeg.gradle.kts` now registers any jar in `testmods/<node>/` as
`modLocalRuntime` and logs `[testmods] <node>: …` at startup (directory gitignored; empty by default).
Modern NeoForge is Mojmap, which is why `run/mods/` works there.

Both stores carry all 58 files. **Modrinth** `cO2CvXug`: 58 versions (`uploaded=57 skipped=1
failed=0`), then **every one re-read with a fresh `GET /v2/version/{id}`** — one file, one loader, one
MC version, changelog present, `6oyMM4yX` (+ `P7dR8mSH` on all 22 fabric, and on no others) required
and project-level; **0 problems**. Featured nine moved to
`1.0.5+{fabric,forge,neoforge}-{1.21.11,26.1.2,26.2}` and the 1.0.4 nine unfeatured, each confirmed
per version. **CurseForge** `1645389`: 58 files, ledgered under `1.0.5/*` in
`scripts/.cf_uploaded.json` (22/18/18). ⚠️ Three nodes (`1.21.2-fabric`, `1.21.2-neoforge`,
`1.21.3-fabric`) failed the batch with the documented transient bare **HTTP 500**; a plain re-run
(the ledger skips what landed) took all three. Loader pins unchanged from 1.0.1 — `pin_drift.py`
reports the same 15 behind, none API-relevant.

## 1.0.4 shipped (2026-08-25)

A fifth wave: seven items from one reporter on **1.21.11 fabric**, plus a crash log that is not ours.
Three of the seven were already fixed in `1.0.3`, which is how we know the reporter is on an older
build. Full triage in [`docs/notes/1.0.1-triage.md`](docs/notes/1.0.1-triage.md) ("Fifth wave").

| Report | Root cause | Blast radius |
|---|---|---|
| `mclo.gs/DSWm6h9` crash | **not ours** — `NoSuchFieldError: EnumRuneType … DUSK` out of `animusnv`/`neovitae`; `alexscaves` is absent from the stack | — |
| Primal Cave chest boat closes the game | see the triage doc | >=1.21.2, all loaders |
| Ancient leaves drop blocks, not saplings, by hand | `DataPackMigration.migrateMatchToolEnchantments`, with 79 other block tables | the bands the migration covers |
| Hullbreaker drops nothing | **not reproduced**; two real defects hardened — leftover loot flushed before removal, and a null `getLastDamageSource()` (the source self-nulls at 40 ticks while `ANIMATION_DIE` runs 50) falls back to `damageSources().generic()` | all nodes |
| Tremorzilla glow "borderline epileptic" **and** the submarine cockpit | `ACRenderTypes` built a **fresh `RenderType` on every call** in 26 factories. `RenderType` declares neither `equals` nor `hashCode` on any version in this range, so `MultiBufferSource$BufferSource#startedBuilders`/`#fixedBuffers` — and from 26.2 `RenderTypeFeatureRenderer$Group#lastRenderType` — compare by **identity**: every lookup missed, the shared batch was rebuilt each frame, and the additive pass was intermittently drawn twice. All 26 now go through one `TYPE_CACHE`; each `getXxx` is a thin wrapper over a private `buildXxx` so no `//?` arm moved | all nodes; worst on 26.2 |

The glow measurement, `/tick freeze` so every frame should be identical — `26.2-fabric`: 2 alternating
states, 819 px differing by up to 161 → **12/12 byte-identical**. `1.21.11-fabric` (the reporter's own
version), A/B/A inside one client boot: **2 of 30** frames spiking peak green 58 → **213**, then
**0 of 30**, then 2 of 30 again; frame-to-frame energy swing 1.151× → 1.007×.

Verified: 58-node `compileJava` `GRADLE_EXIT=0`, 0 errors; `verify_mixins.py` **16507 injection points
across 58 nodes, all resolving** — unchanged from 1.0.3, since no mixin moved. `ACRenderTypes`' `//?`
gate count is **100 before and after** the 26-factory conversion, i.e. every gated arm is byte-intact.
Re-checked in-world on `1.21.11-fabric` with `/tick freeze`: 12/12 frames byte-identical with the
submarine, the nucleeper lights and the radiation glow all in frame.

**The general trap:** a factory that builds a `RenderType` per call is a latent double-draw and a
per-frame churn of an identity-keyed vanilla map, on *every* MC version. It only becomes visible when
the type's blend is additive. Vanilla ships every one of its own types as a singleton or behind
`Util.memoize`.

Release build `MOD_IS_RELEASE=true ./gradlew build`: `BUILD SUCCESSFUL in 22m 20s`, 727 actionable
tasks, **58 release jars** (22 fabric / 18 forge / 18 neoforge), 0 `-SNAPSHOT`.

Both stores carry all 58 files. **Modrinth** `cO2CvXug`: 58 versions, then **every one re-read with a
fresh `GET /v2/version/{id}`** — one file, one loader, one MC version, changelog present, and
`6oyMM4yX` (+ `P7dR8mSH` on all 22 fabric) required and project-level on every one; zero problems.
⚠️ One node (`1.0.4+forge-1.20.4`) failed the batch with a Cloudflare **HTTP 524**; it had *not*
landed server-side (checked before retrying, so no duplicate) and a plain `--only` re-run took it.
Treat a 524 like CurseForge's transient 500 — verify, then retry. Featured nine moved to
`1.0.4+{fabric,forge,neoforge}-{1.21.11,26.1.2,26.2}` and the 1.0.3 nine unfeatured, each confirmed
per version. **CurseForge** `1645389`: 58 files, `failed=0 unsupported=0`, ledgered under `1.0.4/*`
in `scripts/.cf_uploaded.json` (22/18/18). Loader pins unchanged from 1.0.1 (15 behind, no API
relevance).

Still owed by hand: a human pass on the raygun beam and the cave map / book widget (the input rig
cannot drive a GUI, so those two call sites were fixed but not visually re-tested).

## 1.0.3 shipped (2026-08-24)

An eleven-report player wave against shipped `1.0.2`, all fixed in `src/` so every fix is correct on
all 58 nodes -- plus two screen bugs nobody reported, found while chasing the eleventh. Full triage with the per-report root causes is in
[`docs/notes/1.0.1-triage.md`](docs/notes/1.0.1-triage.md) ("Third wave"); the five reusable API
traps it produced are in [`docs/notes/gotchas-api.md`](docs/notes/gotchas-api.md).

| Report | Root cause | Blast radius |
|---|---|---|
| Cave Compendium shading | `EMISSIVE` gates only the lightmap — only `NO_CARDINAL_LIGHTING` is unlit. Now owned: `ACInternalShaders.ENTITY_UNLIT_TRANSLUCENT` + a three-armed `ACRenderTypes.getUnlitTranslucent`, 10 call sites | 22 Fabric + `26.2-forge` |
| Submarine unsteerable | `ServerPlayer#setPlayerInput` deleted at 1.21.2; `ACCompat.riderXxa/riderZza` read `getLastClientInput()` instead | >=1.21.2, all loaders |
| Can't surface / no drowning in soda | mod fluids never counted as water for eye-in-fluid and fluid height; two gated `@Inject`s in `EntityMixin` + `ACFluids.modFluidHeight/isEyeInModFluid` | Fabric + NeoForge >=26.1 |
| Cave map black | `TextureManager.register` stopped loading at 1.21.4; gated onto `registerAndLoad` | >=1.21.4, all loaders |
| Neodymium magnetism dead | Fabric never maps blockstates onto a registered `PoiType` — new `mixin/fabric/PoiTypesInvoker` called from `DeferredRegister#resolve` | 22 Fabric (all 9 POI types) |
| Quarry demolished its own frame | `AABB.encapsulatingFullBlocks` adds 1 to each maximum; `ACPlatform.encapsulating` now uses the six-double constructor, ungated | >=1.20.3 |
| Armour has no animations | vanilla's un-named pose copy inside `renderArmorPiece` was cancelled along with the method; three arms restore it before the animation pass | all nodes |
| Mine guardian chain untextured | vanilla renamed `textures/block/chain.png` at 1.21.9; one shared gated constant on `BoundroidWinchRenderer` | >=1.21.9 |
| Candy hook rope black | upstream fed the light lookup two **relative** offsets, sampling light near x=0,z=0 — wrong since 1.20.1 | all nodes |
| Extinction spear crash | not reproduced; two latent defects hardened (null `getPickupItem()`, client-side `explode()`) | all nodes |
| Mobs punching themselves airborne | new `mobs_can_target_themselves` server option, **default false**, in the chest menu and `/acc config` | all nodes |
| Cave Compendium "shows nothing" | upstream's own `renderBackground` call, kept from 1.20.1, is a **second** pass of the same `0xC0101010`->`0xD0101010` gradient on every node from 1.20.2 (vanilla's `Screen#render` -- and from 1.21.6 the final `renderWithTooltip` -- already draws it). Alpha 0.82 twice leaves the frame at 3.4% instead of 18%; the book behind it reads as absent. Measured A/B on `26.2-fabric`: hotbar brightest 60/255 with one pass, 24/255 with two, reporter's screenshot 26/255 | >=1.20.2, all loaders |
| Nuclear furnace screen crashed on open *(unreported)* | `AbstractContainerScreen#renderBackground` calls `renderBg` from 1.20.2, so the `renderBg` override calling `renderBackground` was unbounded mutual recursion -- a hard `StackOverflowError` | 1.20.2 -> 1.21.11, all loaders |
| Spelunkery table double-darkened *(unreported)* | same shape as the compendium, plus its own `renderBg` re-blit: the panel was drawn three times and the frame dimmed twice | >=1.20.2, all loaders |

Release build `MOD_IS_RELEASE=true ./gradlew build`: `BUILD SUCCESSFUL in 18m 25s`, 727 actionable
tasks, `GRADLE_EXIT=0`, **58 release jars** (22 fabric / 18 forge / 18 neoforge), 0 `-SNAPSHOT`.
`verify_mixins.py` **16507 injection points across 58 nodes, all resolving** (was 16433 through 1.0.2 — +74, all accounted for: two gated `EntityMixin` `@Inject`s over the 26 nodes their gate covers, plus `fabric.PoiTypesInvoker` on all 22 Fabric nodes).

Both stores carry all 58 files. **Modrinth** `cO2CvXug`: 58 versions, `failed=0`, then **every one
re-read with a fresh `GET /v2/version/{id}`** — one file, one loader, one MC version, changelog
present, and `6oyMM4yX` (+ `P7dR8mSH` on all 22 fabric versions) required and project-level on
every one; zero problems. Featured nine moved to `1.0.3+{fabric,forge,neoforge}-{1.21.11,26.1.2,26.2}`,
each confirmed per version. **CurseForge** `1645389`: 58 files, `failed=0 unsupported=0`, ledgered
under `1.0.3/*` in `scripts/.cf_uploaded.json` (22/18/18). Loader pins unchanged from 1.0.1.


## 1.0.2 shipped (2026-08-23)

Three fixes: the toucanlib crash (Citadel's synched-data `@Redirect` on `SynchedEntityData$Builder
.build()` became a stackable `@ModifyArg` on the `defineSynchedData(builder)` call — a redirect owns
its instruction, so any second mod redirecting it crashed both), the 26.2 raygun crash
(`sortOnUpload` on the two TRIANGLES render types; sorting has only ever applied to QUADS and 26.2
throws instead of ignoring it), and Better Combat support (six `parent`-only files under
`data/alexscaves/weapon_attributes/`; BC's fallback only auto-assigns items whose *static*
components declare ATTACK_DAMAGE, which AC's dynamic-attribute weapons never do — `primitive_club`
excluded, it drives its own swing).

58 jars from `MOD_IS_RELEASE=true ./gradlew build`, `verify_mixins.py` unchanged at 16433. Modrinth
`cO2CvXug` 58 versions, each re-read with a fresh `GET /v2/version/{id}` — and this is the first
release where the **fabric-api dependency is present on all 22 fabric versions**. CurseForge
`1645389` 58 files, ledgered under `1.0.2/*`. Featured nine moved to
`1.0.2+{fabric,forge,neoforge}-{1.21.11,26.1.2,26.2}`, confirmed per version. Loader pins were left
exactly where 1.0.1 shipped (14 Forge point releases behind, no API relevance).

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
