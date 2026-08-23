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

## Deep notes — read the one you need, not all of them

**This file is re-injected into every context window, so it deliberately holds only what changes
what you do by default.** Everything expensive-but-situational lives in `docs/notes/`. Read the row
that matches the work; do not read them all, and do not re-derive what one of them already records.

| Read this | When |
|---|---|
| [`docs/notes/gotchas-api.md`](docs/notes/gotchas-api.md) | Before any MC bump, any new `//?` gate or `replacements.string` rule, and before touching `DataPackMigration`. Every per-version/per-loader API and data-pack trap this tree has hit, with its real boundary |
| [`docs/notes/gotchas-runtime.md`](docs/notes/gotchas-runtime.md) | Before running or trusting a dev server, dev client, RCON battery or sweep. Bugs that survived a green build, plus the rig rules that make a verdict mean something |
| [`docs/notes/version-walk.md`](docs/notes/version-walk.md) | Per-wave post-mortems (26.2 … 1.21.3), release-build gotchas, licensing. History — read the wave matching a version you are touching |
| [`docs/notes/citadel.md`](docs/notes/citadel.md) | Before touching anything under `citadel/` — the 93 vendored classes and 13 mixins |
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
  a count going *down* needs the same scrutiny. → *gotchas-runtime*
- **`runServer`'s exit code is backwards** (0 = crashed, 124 = booted). The only verdict is the log:
  `Done (Ns)` present and `Failed to parse` / `Couldn't parse` / `Unknown registry` / `Couldn't load
  advancement` / `Couldn't load tag` / `Missing tag` all absent. → *gotchas-runtime*
- **A green build proves the shapes exist, not that anything calls them.** Grep for the *producer*
  (`new <Event>(`, the `post`, the call site in the patched jar), not just the consumer. → *gotchas-runtime*
- **A vanilla tag can SHRINK, and a predicate built from `optionalFieldOf` fails OPEN.** Both parse
  clean, log nothing, and quietly match everything or nothing. → *gotchas-runtime*
- **Put `@Override` on anything that overrides.** A supertype signature moving is otherwise a silent
  behaviour bug; `scripts/override_audit.py` checks the tree. → *gotchas-runtime*
- **Fix content bugs in `src/main/resources`, not in a `DataPackMigration` pass** — a source fix is
  correct on all 58 nodes and repairs released versions retroactively; a migration pass can only fix
  the band it is aimed at. The exception is when the *correct* spelling genuinely differs per band.
- **This shell is zsh and applies history modifiers inside expansions**: write `":${node}:runServer"`,
  never `:$node:runServer` (which becomes `:1.20unServer`). Multi-node Gradle work must be **one**
  invocation over an array.
- **AC dev servers are pinned off 25565** (`versions/<node>/run/server.properties`, gitignored) so the
  neighbouring repos' servers cannot be mistaken for a regression here — **25599 on 55 nodes, 25597 on
  `1.20.1-forge` / `1.21.11-neoforge` / `26.2-fabric`** (rcon 25596, password `acctest`) so two AC
  nodes can run at once. Read the file rather than assuming; a crashed server keeps `session.lock`,
  and the pid comes from `ss -lntp | grep 2559`.

## Standing scripts

Run these rather than re-deriving what they check. All take no arguments unless noted.

| Script | Checks |
|---|---|
| `verify_mixins.py` | Every injection point on every node against the loader-patched jar. **16433 across 58 nodes** |
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
