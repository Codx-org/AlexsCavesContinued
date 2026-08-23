# Runtime & test-rig gotchas — Alex's Caves Continued

**Read this before running anything — a dev server, a dev client, an RCON battery, a client
shakedown — and before believing a green result from one.** Every section here is a bug that a
compiling, booting, mixin-verified tree still had, plus the rig rules that make a test verdict
mean something. The recurring lesson: *a green build proves the shapes exist, not that anything
calls them.*

Sections, newest first: **driving the dev client without a mouse** (2026-08-22) · the first player
bug report (2026-08-22) · the 26.x Fabric client shakedown (2026-08-20) · the content-warning pass
(2026-08-20) · the in-world test battery (2026-08-19) · the `/acc` interactive client+server pass
(2026-08-20) · the runtime shakedown (2026-08-18).

Split out of `DEVELOPMENT.md` on 2026-08-22 so the always-loaded file stays small — see that file
for the map of which archive holds what. Nothing here has been edited in the move.

### Driving the dev client without a mouse (the input rig, 2026-08-22)

**Built to reproduce report #6, and it worked** — the smithing-table crash was reproduced and then
re-tested from a shell, with no human at the keyboard. Read this before trying to automate a client
again; three of the four steps below are counter-intuitive and each cost a debugging round. Scripts
live in the session scratchpad, not in the repo (`mcdrive.py`, `drive.sh`, `guiclick.py`) — except the
two that earned their keep, `scripts/glow_shots.sh` and `scripts/glow_ring.py` (see #12).

**The box**: Wayland compositor, Xwayland for the game. GLFW reports `GFLW Platform: x11`.

1. **Find and screenshot the window with python-Xlib + `xwd`.** Walk the tree for a window whose
   `WM_NAME` contains `Minecraft` and whose width is > 200 (the splash window is smaller), then
   `xwd -id <hex id> -out … && magick … out.png`. ⚠️ `xwd` needs `DISPLAY=:0` **in its own
   environment** — a subprocess launched from a tool-run shell inherits a stripped env and fails
   with *"Authorization required… unable to open display ':0'"*. Pass
   `env=dict(os.environ, DISPLAY=':0')`.
2. **X input focus does not follow `set_input_focus`.** Send a `_NET_ACTIVE_WINDOW` ClientMessage
   to the root window instead. (In practice the compositor then *keeps* the client focused, so the
   rig's per-keystroke re-activation step is not only unnecessary but harmful — it swallowed the
   keys that followed it. `act(){ :; }`.)
3. ⚠️ **XTEST keyboard injection is silently dead here.** Not "unreliable" — dead. Proved with a
   self-made Xlib probe window: focus it, inject `a` from a second connection, and it receives
   `ConfigureNotify`/`MapNotify`/`Expose` and **no `KeyPress` at all**. XTEST *pointer* warps do
   change X pointer state, which is exactly what makes this look like a game problem rather than an
   injection problem. **Use `ydotool`** (uinput, below X entirely): `ydotoold` running, user in the
   `input` group, keys by **Linux scancode** — `ydotool key 20:1 20:0` is `t`, `1` is Escape,
   `28` is Enter, `47` is `v`. `ydotool type -- "<text>"` for chat.
   ⚠️ **`scripts/mcdrive.py` still spoke XTEST for `key`/`chat` until 2026-08-23** — i.e. the repo's
   own rig contradicted this bullet, and every `key`/`chat` it printed (`keys: ['F11']`, `sent: …`)
   was a lie. It now shells out to `ydotool` (`YKEYS` maps X keysym names → Linux scancodes), so use
   the script and not raw `ydotool`. The tell that you are in this failure mode: **`_NET_ACTIVE_WINDOW`
   is the game, `get_input_focus()` is the game, the pointer is over the game, the panorama is still
   animating, and nothing at all responds** — including `F11`, which is the cheapest probe there is
   (the window resizes to the full root, `xwininfo` shows it, no screenshot needed).
4. ⚠️ **ydotool's mouse does not reach Minecraft at all** — neither the grabbed in-world pointer
   (right-click never opened a block, left-click never broke one in creative) **nor a GUI screen**
   (moving to an inventory slot produces no hover highlight, so the click that follows lands
   nowhere). Retested during the #6 work with absolute positioning computed from the window origin;
   still nothing. The likely cause is the multi-monitor virtual desktop — this window sits at
   `+3413+841` and `ydotool mousemove -a` scales to a single screen's axis range — but it was not
   worth chasing. **Consequences, and the workaround:**
   - **Rebind the mouse actions to keyboard keys** in `versions/<node>/run/options.txt` and restart
     the client: `key_key.attack:key.keyboard.b`, `key_key.use:key.keyboard.v`,
     `key_key.pickItem:key.keyboard.n`. That covers everything done *in the world*.
   - **Anything that needs a click inside a GUI screen is still not automatable.** So "the screen
     opens" is testable and "the button inside it works" is not — e.g. the spelunkery table's word
     buttons (report #9) could only be verified by reading the source. Do not record a widget fix as
     runtime-verified on the strength of the screen appearing.
5. **Aim with `/tp`, not with mouse-look.** `/tp @s <x> <y> <z> <yaw> <pitch>` puts the crosshair
   exactly where you want it; a block at the player's feet needs about `pitch 22`. Bring an entity
   to the camera with `/execute at @s run tp @e[type=<id>,limit=1] ~ ~ ~3` — the bare
   `/tp @e[...] ~ ~ ~3` is relative to *the entity*, which moves it nowhere useful.
6. **Boot straight into a world** with `--args=--quickPlaySingleplayer <levelname>`; there is no
   keyboard route through world creation — but there *is* one through world **selection**, which is
   enough to recover a client that has already booted to the title screen without paying for a
   restart: `Tab`,`Return` (Singleplayer) → `Tab` (focuses the world list on its first row),
   `Up`/`Down` to pick, `Return` to play. Screenshot between steps; `Tab` lands on the search box
   first, and the row below the one you want is one keypress from `Delete`. Only some nodes have a `run/saves/actest` — check with
   `ls versions/*/run/saves` before planning an A/B, and note the world is not portable between MC
   versions.
7. **`pkill -f` on the client returns a non-zero exit that aborts a compound command** (144 was
   seen), and the Fabric client ignores plain `SIGTERM` — use `pkill -9 -f "fabric.dli.config"` as
   its own call, then re-check with `pgrep`.
8. The client log is **not** subject to the dev-server verdict rule. `Failed to parse into
   SignedJWT` and the `authlib`/Yggdrasil stack that follows it are the offline dev account failing
   to authenticate against Realms, on every run, always. Filter them out before reading the log for
   real errors.

9. **Capture with the game's own F1+F2, never with `xwd`.** `xwd` grabs the X window including
   whatever the compositor composited over it, and on this box it also races the swap. F1 (scancode
   `59`) hides the GUI, F2 (`60`) writes a real framebuffer PNG to `versions/<node>/run/screenshots/`,
   F1 again restores it. The chat log echoes `Saved screenshot as <name>.png`, which is also the
   confirmation that the keystroke landed. **`pauseOnLostFocus:false`** must be set in
   `run/options.txt` or the client pauses the moment the rig's focus message lands and every
   subsequent key goes to the pause menu.
10. ⚠️ **A rig world needs `allowCommands = 1` in its `level.dat`, and the failure mode names the
    wrong cause.** Without it every command comes back `Unknown or incomplete command, see below for
    error<newline>/gamerule doDaylight…<--[HERE]` — and because vanilla **strips the leading `/`**
    when it echoes the offending input, this reads exactly like the rig failed to type the slash.
    It did not; cheats are off. `/help` settles it in one keystroke: a no-cheats world offers only
    `/w /random /seed /teammsg /tm /trigger` (plus this suite's `/codxlib` and `/acc`). There is no
    command to turn cheats on from inside such a world, so patch the file — it is a single gzipped
    NBT byte:

    ```python
    import gzip
    d = gzip.open('run/saves/actest/level.dat','rb').read()
    i = d.find(b'allowCommands')            # NBT byte tag: name, then one value byte
    d = d[:i+13] + b'\x01' + d[i+14:]
    open('run/saves/actest/level.dat','wb').write(gzip.compress(d))
    ```
11. **`pkill -f <node>` matches the tool-run shell's own command line and kills it**, which surfaces
    as exit 144 aborting the whole compound command — the same symptom as a crashed client. Split
    the pattern so it cannot match itself: `P='1.21.5-fab''ric'; pkill -f "$P"`.
12. ⚠️ **Never compare an animated shader from single frames.** `rendertype_irradiated.fsh` swings
    its green channel over a ~3.8 s `sin(GameTime*2000)` cycle, so eight consecutive frames of the
    *same* raycat on the *same* node measured a ring green-excess of 5.32 → 9.49 — a 1.8× spread
    with nothing changed. A between-node A/B against that is noise, and it cost a whole bisect
    (report #5). Average **≥8 frames**, and hold resolution and crop fixed: MC's window size is not
    the same on every node's fresh run directory, and the metric is scale-sensitive.
13. **`for f in $(ls …)` breaks on this tree's paths** — the checkout lives under `Minecraft Mods`,
    with a space. Use `ls -t … | head -N | tac | while IFS= read -r f`, and quote every expansion.
14. **When the reporter's screenshot does not render, re-introduce the bug on a scratch build.**
    A fix reasoned out from bytecode only ever earns "we found something that would explain this".
    Un-doing it deliberately, rebuilding the one node, and re-photographing the reported location with
    the same rig turns that into a matched before/after — and it is cheap, because it is usually one
    line. Done for report #2/#12: flipping `ACPipelineState.NEARER_OR_EQUAL` back to
    `LESS_THAN_OR_EQUAL` put a field of ambersol starbursts through the primordial terrain and drew a
    full pinwheel over a sealed stone box; restoring it removed both. **Copy the file aside first and
    `diff -q` it back afterwards** — a stray experiment left in a 58-node tree compiles everywhere and
    is invisible in a build log.
15. **A location can be unable to disqualify a fix.** The same experiment showed *no* difference in
    abyssal chasm, for the dull reason that no mod render type was in frame there. Before reading a
    null result as "not that bug", check that the thing under test is actually being drawn — a
    controlled scene (`scripts/block_occlusion_test.sh`) discriminates where a landscape shot cannot.

16. **F1 hides the chat, so it hides your evidence.** The screenshot recipe for a *visual* check is
    F1 (clean frame) then F2, and reaching for the same two keys after `/data get` or `/say`
    photographs a frame with the answer stripped out of it. Command output is read by pressing **F2
    alone**, HUD up. There is no other channel: a dev *client*'s integrated server does not log chat,
    and RCON is a server-only rig.

17. **Run an item test in the game mode the fixed branch belongs to.** `RemoteDetonatorItem.useOn`
    forks on `!player.getAbilities().instabuild && itemstack.getCount() == 1`; the detached-
    `custom_data` write-back that was actually broken lives on the *survival* side, and creative
    takes a different branch that builds a fresh stack and looks fine either way. Creative is the
    convenient way to hand yourself the item — it is not the mode to press the button in. Read the
    branch before choosing the mode.

18. **`scripts/mcdrive.py keyhold <key> <seconds>`** holds one key down — charging a bow, holding
    a walk. `key` presses and releases in 30 ms, which cannot draw a bow, and the mouse `hold`
    command is buttons-only. In-world actions go through the rebound keys (`b` attack, `v` use,
    `n` pick) because synthesized mouse buttons do not reach MC's GUI handling.

### Gotchas the first player bug report found (2026-08-22)

`1.0.0` shipped, a player played it on **26.2 Fabric**, and reported three things in one message:
*"I can see something render through the blocks… all the jellyfish died… soda does not seem to be a
liquid… can't find any raptors or other animals."* That is **three independent bugs**, none of which
any of the 58 green builds, the 58 green dev-client boots, the 16433-point mixin verification or the
three-node in-world RCON battery had any opinion about — and two of them are **vanilla changing
something underneath the mod without an error of any kind**. Every one is written up below with the
version boundary it actually has, because none of the three has the boundary you would guess.

- **⚠️⚠️ 26.2 SWITCHED MINECRAFT TO A REVERSED-Z DEPTH BUFFER, and a mod pipeline that keeps naming
  `LESS_THAN_OR_EQUAL` then draws THROUGH TERRAIN — at any distance, with no log line.** Near is
  `1.0` and far is `0.0` from 26.2, and vanilla flipped every one of its own depth tests to match. The
  census is unambiguous and takes one grep of each version's `RenderPipelines`: **26.1.2 states
  `LESS_THAN_OR_EQUAL` 15 times and `EQUAL` twice; 26.2 states `GREATER_THAN_OR_EQUAL` 14 times and
  `EQUAL` twice**, and `DepthStencilState.DEFAULT` moved with them — `(LESS_THAN_OR_EQUAL, true)` on
  26.1.x, `(GREATER_THAN_OR_EQUAL, true)` on 26.2. Corroborate it from the other end:
  `LevelRenderer.lambda$addAlwaysOnTopPass$0` clears the depth texture to **`0.0`** on 26.2, which is
  only a "far" clear under reversed-Z. **Nothing about the API changed**, so a mod pipeline naming the
  old comparison still compiles, still has a depth state, still has a depth attachment — and passes
  for every fragment *farther away* than what is already in the buffer, i.e. it is an x-ray. Every
  render type in this mod is built through `ACPipelineState.depth()`, so this was the **whole mod**
  x-raying on exactly the three 26.2 nodes and nowhere else. Fixed with a `NEARER_OR_EQUAL` constant
  in `ACPipelineState` — `GREATER_THAN_OR_EQUAL` from 26.2, `LESS_THAN_OR_EQUAL` on 26.x below it —
  and nothing else: the projection is vanilla's (it arrives through the `MATRICES_PROJECTION` bind
  group), so the depth *values* this mod writes were already reversed; only the *comparison* was left
  behind. `depthEqual` and `noDepth` need no arm at all — `EQUAL` and `ALWAYS_PASS` mean the same
  thing whichever way the axis points. **General form, and the reason no build and no boot caught it:
  a change to the SIGN CONVENTION of an existing API is invisible to the compiler, to
  `verify_mixins.py`, and to a log-marker sweep.** The only checkable artefact is vanilla's own
  bytecode — count what vanilla's pipelines state, and if the mod's spelling is in the minority, the
  mod is wrong.

- **⚠️⚠️ A VANILLA TAG CAN SHRINK, and `#minecraft:dirt` lost 6 of its 9 members at 26.1 — silently,
  and it stopped every land dinosaur in the mod from spawning.** Read straight out of
  `data/minecraft/tags/block/dirt.json` in each cached `minecraft-extracted_server.jar`: 1.20.1 →
  1.21.3 is **9** members, 1.21.4 → 1.21.11 is **10** (`pale_moss_block` joins), and
  **26.1 / 26.1.1 / 26.1.2 / 26.2 is THREE** — `dirt`, `coarse_dirt`, `rooted_dirt`. `grass_block`,
  `podzol`, `mycelium`, `moss_block`, `mud`, `muddy_mangrove_roots` and `pale_moss_block` are all
  gone; `item/dirt` moved identically. So the boundary is exactly **26.1, i.e. 12 of 58 nodes**, and
  the blast radius is much wider than the reported symptom because this mod named `BlockTags.DIRT` in
  **11 places**: `dinosaurs_spawnable_on` (so nothing could spawn on a grass floor — the player's "no
  raptors"), `AnimalLayEggGoal` (so no dinosaur could lay an egg), `VolcanoStructurePiece`,
  `CoveredBlockBlobFeature`'s `>=26` arm, and **every cave tree feature** — cycad, licoroot, ancient,
  giant ancient, pewen and thornwood all stopped generating. ⚠️ **The obvious substitution does not
  work**: the replacement vanilla offers is `#minecraft:grass_blocks`, which **does not exist on
  1.21.11 or below**, so it cannot be named tree-wide. The fix is the shape this file already prefers
  — **the mod owns the tag**: `alexscaves:dirt_like` folds in `#minecraft:dirt` *plus* the seven
  dropped members by name (`pale_moss_block` as `{"id": …, "required": false}`, since it does not
  exist below 1.21.4), `dinosaurs_spawnable_on` becomes `["#alexscaves:dirt_like",
  "#minecraft:sand"]`, and all 11 Java sites move to `ACTagRegistry.DIRT_LIKE`. One file, no gates,
  identical membership on all 58 nodes. **The standing check that comes out of this is
  `scripts/tagshrink.py` / `scripts/tagdiff.py`: on every MC bump, diff the MEMBERSHIP of every
  vanilla tag the mod references against the previous version — not merely whether the tag still
  exists.** A tag that shrinks parses clean, loads clean, logs nothing, and quietly makes a feature
  match nothing; the four fatal markers this file lists (`Failed to parse`, `Couldn't parse`,
  `Unknown registry`, `Couldn't load advancement`) and even the two added later (`Couldn't load tag`,
  `Missing tag`) are all absent, because nothing failed. Verified at runtime on `26.2-fabric` with a
  fresh world on seed `20250819`: 6/6 `grass_block` floors in the primordial caves now match
  `#alexscaves:dinosaurs_spawnable_on` (0/6 before), and a census over 117 freshly generated
  forceloaded chunks finds **vallumraptor 8, grottoceratops 4, tremorsaurus 1, relicheirus 1,
  subterranodon 13** where every ground dinosaur was 0. (`atlatitan 0` is correct — it is gated by
  `checkPrehistoricPostBossSpawnRules`.) ⚠️ AC's cave-creature spawning runs from `NaturalSpawnerMixin`
  at TAIL of `spawnMobsForChunkGeneration`, so **a census is only meaningful over freshly generated
  chunks** — re-reading an already-explored region proves nothing.

- **⚠️ 1.21.5 gave `WaterAnimal#handleAirSupply` a `ServerLevel` parameter, so five of this mod's fish
  silently stopped overriding it and drowned in their own fluid.** `handleAirSupply(int)` →
  `handleAirSupply(ServerLevel, int)`, and the five subclasses that reset their air supply
  (`SweetishFishEntity`, `RadgillEntity`, `TrilocarisEntity`, `LanternfishEntity`, `SeaPigEntity`)
  declared the old shape **without `@Override`**, so from 1.21.5 they are five new unrelated methods
  nothing calls and vanilla's drowning ran unopposed — **33 of 58 nodes**, on all three loaders. That
  is the player's dead jellyfish. Fixed with the `!mc2105-handleairsupply` replacement rule plus an
  `@Override` on all five. **The lesson is the `@Override`, not the rule**: an overriding method with
  no `@Override` annotation is a silent behaviour bug the moment the supertype's signature moves, and
  this file already records the same trap from the other direction (the 1.21.10 `entityInside`
  boolean, where `PurpleSodaBlock`'s lone `@Override` was the only thing that reported the break).
  `scripts/override_audit.py` now checks the whole tree: every method whose name matches a vanilla
  supertype member is flagged when it carries no `@Override`. Verified at runtime — in a purple-soda
  tank `sweetish_fish` holds `Air=400` and `trilocaris` `Air=300` after two minutes, each the exact
  `setAirSupply` constant in its own class. (`radgill`, `lanternfish` and `sea_pig` still die in soda:
  that is by design, their classes only reset air in water.)

- **⚠️⚠️ NEOFORGE STOPPED HONOURING `FluidType#move` AT 26.1 WHILE STILL SHIPPING THE WHOLE `FluidType`
  API — so "does the extension point still exist?" is a worthless probe.** This mod's fluids are
  water-like on Forge for one reason: `FluidType#move` defaults to **false**, `PurpleSodaFluidType`
  does not override it, and Forge's patched `LivingEntity` therefore falls *through* the fluid-type
  branch into vanilla's own water travel. `AcidFluidType` **does** override `move`, and its body is a
  verbatim copy of vanilla 1.20.1's water branch plus one swim-speed multiply — same `0.8F` drag,
  `0.02F` speed, `0.54600006F` depth-strider lerp, `0.96F` dolphin's grace, `0.08D` gravity, `0.3F`
  ledge hop. Both therefore *mean* "treat me as water". From **26.1** NeoForge deleted the call site:
  disassembling `LivingEntity#shouldTravelInFluid` out of
  `versions/<node>/build/moddev/artifacts/minecraft-patched-*.jar` shows the **vanilla** shape on
  NeoForge 26.1 / 26.1.2 / 26.2 — `isInWater()`, `isInLava()`, `isAffectedByFluids()`,
  `canStandOnFluid(FluidState)`, **no `isInFluidType`** — while every cached build below it has
  `isInFluidType` with 2–3 call sites, and **Forge still has it on 61.1.0, 62.0.9 and 65.1.0, so all
  18 Forge nodes are unaffected**. Fabric never had any of it: this tree's `fabric/forge/fluids/
  FluidType` says so in its own javadoc (*"nothing calls any of this yet… the physics are compiled and
  dormant"*), and a grep for `.move(` / `canSwimIn` / `motionScale` under `fabric/**` returns zero.
  So on **all 22 Fabric nodes and the 4 NeoForge 26.x ones** an entity in purple soda or acid was in
  air as far as travel, buoyancy and swimming were concerned. Four things generalise:
  - **Probe the CALL SITE in the patched Minecraft class, never the API type.** `unzip -l` on every
    cached `neoforge-*-universal.jar` from `20.4.251` to `26.2.0.37-beta` finds the three
    `neoforge/fluids/FluidType` entries on **every** build, and `javap -p FluidType.class` shows
    `move`/`motionScale`/`canSwim`/`isAir` unchanged through 26.2. The API is intact; nothing calls it.
  - **`javap -p` cannot see an inherited default interface method.** `isInFluidType` comes from
    `IForgeEntity`, so a method listing of `LivingEntity` reports nothing either way — grep the
    `javap -p -c` **disassembly** instead. And `isInFluidType` is the discriminator that works;
    grepping for `FluidType.move` gives 0 hits on every build, including the ones that do call it.
  - **Only 4 NeoForge nodes have MDG artifacts staged.** For the other 14, the fallback that carries
    every build is `~/.gradle/caches/neoformruntime/intermediate_results/compiledWithNeoForge_*_output.jar`
    (38 distinct entries here). Related: loom's `minecraftMaven` **sources** jars are 22-byte stubs —
    real decompiled sources exist only as MDG's `minecraft-patched-*-sources.jar`.
  - **The fix is `isInWater()`, because that is the one seam that does not move.** `travel`'s shape
    changes three times across the range (monolithic below 1.21.2; `travelInAir`/`travelInFluid`/
    `travelRidden`/`travelFallFlying` from 1.21.2; `travelInFluid` split again into
    `travelInWater`/`travelInLava` at 1.21.11), whereas `Entity#isInWater()Z` is unchanged on all 22
    MC versions. `EntityMixin#ac_isInWater` therefore returns `true` when the entity is in acid or
    purple soda, gated `fabric || (neoforge && >=26.1)`. ⚠️ **Do not "fix" this at
    `shouldTravelInFluid` instead** — the 26.2 dispatch is a *two*-stage gate, and `travelInFluid`
    branches on `isInWater()` with **`travelInLava` as the else**, so opening only the first stage
    gives the entity *lava* physics. Two divergences are accepted deliberately and are written into
    the mixin: no drowning in soda (the fish handle their own air) and no underwater overlay.
    Verified at runtime on `26.2-fabric`: a standing zombie's `Motion` is `-0.005` in soda and in
    water but `-0.0784000015258789` in air, and a drop from y=115 gives **byte-identical** positions
    in soda and water at every sample (114.59884706160592, 114.09998666151675, 113.07499989129813,
    111.07499977350237) while the air control had already landed.

- **Rig notes from this pass.** `NoAI:1b` **freezes an entity completely** — all three probe zombies
  sat at exactly y=115.0, including the air control, which makes a physics test vacuous while looking
  like a clean negative result; summon with AI enabled and drop the entity from a height instead. The
  soda block id is **`alexscaves:purple_soda`**, not `purple_soda_block`. And 26.2 removed the daytime
  timeline, so `time query daytime` — this file's recommended RCON sentinel — **fails there**; use a
  bare `execute if block <pos> <block-or-#tag>` with no `run` clause, which answers on every version
  and doubles as a tag-membership probe.

- **⚠️⚠️ `PoseStack.popPose()` gained a "you cannot pop the base pose" guard at 1.21.5, which turns
  one line of dead upstream code into a hard render crash on 33 of the 58 nodes.** The constructor
  seeds the stack with one base pose (`poses.add(new PoseStack$Pose()); lastIndex = 0`). Up to
  1.21.4 `popPose()` was a bare `Deque#removeLast()`, so popping that base pose silently emptied a
  one-element deque and nothing ever complained; from 1.21.5 it opens `if (lastIndex == 0) throw new
  NoSuchElementException();`. Census over the per-node bytecode: **`Deque.removeLast` on 1.20.4,
  1.20.6, 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4; THROWS-GUARD on 1.21.5 → 26.2**, so the boundary is
  1.21.5 exactly and this is **not** a 26.x bug even though 26.2 is where a player hit it.
  `TeletorModel#translateToHead` builds a fresh `PoseStack`, applies nine rotations, reads
  `last().pose()` into a `Vector4f` and *then* pops — so on `26.2-fabric` a Teletor coming on screen
  is an instant `ReportedException: Rendering entity in world` ← `NoSuchElementException` at
  `PoseStack.popPose`, with the whole client gone. **The pop is dead code**: the value it would
  restore is never read, `offset` is already computed, and the stack is a local that dies at the
  `return`. Deleting the line is therefore behaviour-identical on all 58 nodes and needs **no
  Stonecutter gate** — much the better fix than gating a call that never did anything.
  Three things generalise.
  **(1)** The standing check is **`scripts/posestack_audit.py`**: for every `PoseStack x = new
  PoseStack()` in the tree it walks the enclosing block with a running depth and fails on any
  `x.popPose()` reached at depth 0. It reports **32 locally-constructed stacks, 0 underflows** now,
  and was sensitivity-checked by restoring the bug in a scratch copy — it then names exactly that
  line and exits 1. It deliberately says nothing about a stack received as a *parameter*, whose depth
  belongs to its caller.
  **(2)** The crash names the *vanilla* method and the *renderer*, never the model class that is
  actually wrong — `PoseStack.popPose` ← `TeletorModel.translateToHead` ← `TeletorRenderer.render` —
  and the mod's own frame is one line in the middle of a wall of dispatch frames. Read the whole
  trace, not the top three frames.
  **(3)** ⚠️ **Reading vanilla bytecode per version: loom's `minecraft-client.jar` is OBFUSCATED
  below MC 26**, so `javap com.mojang.blaze3d.vertex.PoseStack` against it returns nothing at all —
  which reads exactly like "the class does not exist on this version" and would have dated this
  change wrongly. `forge/<ver>/minecraft-merged-patched.jar` does not carry blaze3d either. What
  works across the whole range is **MDG's per-node artifacts**,
  `versions/<node>-neoforge/build/moddev/artifacts/` — `neoforge-<ver>.jar` for ≤1.21.11 and
  `minecraft-patched-<ver>.jar` for 26.x (filter out `-sources`, `-merged`, `-client-extra`). From
  MC 26 the loom base jar is readable again, because Mojang names ship official.


- **⚠️⚠️ ON 26.2, A DRAW CALL THAT REACHES THE WRONG COLLECTOR — OR NONE — IS A SILENT NO-OP.** The
  Cave Compendium opened as an empty screen, and it took two separate fixes to fill it, neither of
  which logged anything at any point. **(1)** `CaveBookPipRenderer.renderToTexture`'s `>=26.2` arm
  built an `ACSubmitBuffers` around the picture-in-picture pass's `SubmitNodeCollector` and never
  called `flush()`. An `ACSubmitBuffers` *records*; the recording only becomes geometry when
  `flush()` hands it to the collector, so the book rasterised into an empty texture. **(2)** With
  the model drawing, the pages were still blank: `PageRenderer` fetched
  `Minecraft.getInstance().renderBuffers().bufferSource()` rather than using the buffer source it
  was being drawn into. Below 26.2 those are the same object. On 26.2 that call resolves to
  `ACRenderContext.bufferSource()`, whose collector is pushed only inside
  `LevelRenderer#submitFeatures` and is therefore null during a GUI pass — and
  `ACClientCompat.drawInBatch`'s 26.2 arm returns early on a null collector. Every glyph and widget
  was discarded.
  **So of any legacy render body on 26.2 ask two questions the log will never answer: *who flushes
  this?* and *whose collector am I on?*** The first has a standing detector,
  `scripts/submit_flush_audit.py` (19 construction sites, 0 unflushed). The second does not: fetching
  a buffer source from the game instead of taking the one you were handed is the smell, and a GUI
  that draws a model is where it bites. Both were found only by temporary `LOGGER.info` probes at
  each link of the chain — state constructor, mixin lookup, `renderToTexture` — which is the general
  technique for a render path that fails without an exception.

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


