# Alex's Caves Continued — 1.0.0 test plan

Release is gated on this passing. Budget about three hours for the four full passes and one more
for the six smoke passes.

**What this exists to catch.** Every verdict on this build so far is boot-level: 58 dev servers
reached `Done`, 56 dev clients reached the title screen, `verify_mixins.py` resolved 15307 injection
points, and three data audits came back clean. **Not one world has ever been generated.** For a mod
that is six cave biomes, 14 structures, 43 mobs, 575 items and 467 recipes, "the title screen
drew" says nothing about whether the caves appear or the items have textures. Everything below
is the part no headless probe can see.

---

## Already green — do not re-test

| Check | Result |
|---|---|
| 58-node `MOD_IS_RELEASE=true` build | `BUILD SUCCESSFUL`, exit 0, 727 tasks, 0 failures |
| `scripts/verify_mixins.py` | 15307 injection points, 58 nodes, all targets resolve |
| `scripts/aw_check.py` | `problems=0` on all 22 MC versions |
| `convaudit.py` (convention tags) | `missing=0` on all 22 Fabric versions |
| Dev servers | all 58 reach `Done`, none of the ten fatal log markers |
| Dev clients | 56 PASS, 0 DIRTY, 2 no-boot (see below) |

`26.1-forge` and `26.1.1-forge` cannot start a client on this box — Forge builds 62.0.9 and 63.0.2
throw `GLFW error before init: [0x1000C]Wayland`, `Suspected Mods: NONE`, before any mod loads.
Neighbouring builds (64.0.12, 65.1.0) boot fine. **Not a mod bug**; they need an X11 session or
stay statically verified.

---

## Rigs

Every node runs from the repo:

```bash
./gradlew ":1.21.1-neoforge:runClient"        # brace the node name — zsh eats :1.21unClient
```

CodxLib must be in the local Maven repo first (`cd ../codxlib && python3 scripts/install_maven_local.py`)
or nothing resolves.

⚠️ If you want to test **alongside other released mods**, `runClient` is the wrong rig on Forge and
NeoForge — release jars are SRG-mapped and the loom dev runtime is Mojmap, so their mixins die at
load. Use a real launcher instance with the jar from `versions/<node>/build/libs/` plus the matching
CodxLib jar. Alex's Caves Continued alone is fine in `runClient` on every loader.

---

## Full pass — four nodes

Chosen because each is the only node exercising a band of code the others cannot reach.

| Node | Why this one |
|---|---|
| `1.20.1-forge` | The baseline. Upstream's own target, the oldest code paths, the pre-1.20.5 food/armour/int-provider shapes, the only band where the underground-cabin map marker uses `MapDecoration.render` |
| `1.21.1-neoforge` | The mainstream version most players are on; JEI exists here; mid-range component/data paths |
| `1.21.11-fabric` | The biome `effects`→`attributes` migration (silent if wrong), the `Identifier` rename wave, and Fabric's own food/registry fixes |
| `26.2-fabric` | The most-rewritten rendering in the range — immediate mode is gone, every draw goes through the ambient `ACRenderContext`, and a draw made outside that window is silently discarded |

### 1. World generation — the thing nothing has checked

Create a **new creative superflat-off world**, then for each of the six biomes:

```
/locate biome alexscaves:magnetic_caves
/tp <coords>
```

Six biomes: `magnetic_caves`, `primordial_caves`, `toxic_caves`, `abyssal_chasm`,
`forlorn_hollows`, `candy_cavity`.

- [ ] All six locate within a few thousand blocks
- [ ] Each looks like itself — correct stone, correct ambience, correct fog and sky tint
- [ ] Vegetation and clutter are **present and scattered**, not absent and not clumped at one point

> ⚠️ The clumping check is the point on `26.2-fabric`. `minecraft:random_patch` was deleted at 26.1
> and unrolled by hand into a configured feature plus `count` + `random_offset` placement modifiers,
> appended **after** `biome`. If the ordering were wrong the patches would biome-test every scattered
> position instead of the origin — the tell is vegetation that generates in tight clusters or not at
> all. Same node: the two `minecraft:lake` features gained three required block predicates.

- [ ] Ambient music plays in each biome, and the ambient loop/mood sounds fire
      *(1.21.11+ only — this is the `attributes` migration; wrong and it silently falls back to
      vanilla sky, fog and silence with no log line)*

Then structures — at minimum one from each biome:

```
/locate structure alexscaves:underground_cabin
```

- [ ] `underground_cabin` generates *(its biome set is a composite `HolderSet`; on Fabric it was
      flattened to the positive member, so verify it still lands in sensible biomes)*
- [ ] `volcano`, `gingerbread_town`, `abyssal_ruins`, `licowitch_tower`, `dino_bowl` generate
- [ ] Nothing generates floating, sheared, or half-buried in a way that looks broken

### 2. Items — 575 model definitions, none ever looked at

Open the creative tabs and scroll every one.

- [ ] **No missing-model purple/black cubes anywhere.** This is the single highest-value check in
      the plan: from 1.21.4 an item's model is bound by `assets/alexscaves/items/<id>.json`, and all
      575 of those files are *generated* at `processResources` from the model tree. A wrong one
      renders the missing-model cube and logs nothing on most versions
- [ ] The five dynamically-tinted items show their colour (not grey): the tint sources are
      `biome`, `pearl`, `jelly_bean`, `biome_treat`
- [ ] All 43 spawn eggs show **two** distinct colours, not one flat default
- [ ] The 21 special-rendered items draw their 3D model in hand and in the inventory — the three
      spears, `cave_map`, the ortholances, both icon items
- [ ] Held-item models change state where they should: drawing a bow-like item, a thrown spear

### 3. The three custom screens

- [ ] **Cave book** opens, pages turn, the entity and item widgets on each page draw in front of
      the page rather than behind it, and the crafting-recipe widgets show their items
- [ ] **Spelunkery table** opens and the word buttons respond
- [ ] **Nuclear furnace** opens, accepts fuel, and smelts

> ⚠️ The cave book is the deep end on `1.21.6+`. From 1.21.6 a screen cannot draw a 3D model where
> it stands — the whole book is deferred into a `PictureInPictureRenderer` whose depth convention is
> **inverted** relative to every other screen in the mod, corrected by one extra `scale(1, 1, -1)`.
> If that is wrong the symptom is subtle: page contents layered in the wrong order, an item behind
> the page it sits on, or an entity clipped by the book. Look at it properly, on `1.21.11-fabric`
> and `26.2-fabric` especially.

### 4. Crafting — the 27 convention tags

The mod ships **467 recipes** — 301 shaped, 71 stonecutting, 41 shapeless, 52 cooking, one trim and
the `alexscaves:cave_map` type — and **104** of them rest on the **34 mod-owned item tags** that fold
a `c:`/`forge:` convention tag in as an optional member. Before that rewrite those 104 were
unresolvable on the four oldest Fabric nodes, and three were silently uncraftable on several
Forge/NeoForge nodes. Those are the ones to actually craft.

- [ ] Open the recipe book / JEI and confirm the mod's recipes list
- [ ] Craft at least: `cinder_brick`, `hologram_projector`, `siren_light` *(the three that were
      silently broken)*, plus one recipe using each of iron, glass, concrete and gravel
- [ ] The smithing-trim recipe (`armortrim/polarity`) works *(1.21.5+ gained a required `pattern`
      field, filled by migration)*

### 5. Mobs

Spawn eggs are the fast route; natural spawning is the real check.

- [ ] Spawn one of each of the 43 mobs in a flat world. **Every one has a texture, an animation, and
      no missing-model cube.** Note any that T-pose or render inside-out
- [ ] Animations play — this is the vendored Citadel animation system, 93 relocated classes
- [ ] In each biome, mobs spawn naturally under the custom `ALEXSCAVES_CAVE_CREATURE` mob category
      *(the category is an FML enum extension matched by constructor descriptor — 26.2 widened that
      constructor, so this is the check that the retarget worked)*
- [ ] Hostile mobs attack, pathfind and take damage

### 6. Armour, food and effects

- [ ] All six armour sets render on the player when worn. **Four of them — primordial, hazmat,
      diving, gingerbread — were invisible from 1.20.5** until they were routed through
      `CustomArmorPostRender`; check those four specifically on every node ≥1.20.5
- [ ] Eating the mod's foods applies their effects *(on Fabric the food hook is a
      `@ModifyExpressionValue` that must return `original` for other mods' items — if another food
      mod is installed, verify its food still works too)*
- [ ] Potions brew and apply *(brewing is deferred to first `getRecipes()` on Fabric 26.x)*
- [ ] The mod's status effects show the right icon in the inventory

### 7. Magnets and the ferromagnetic tag

- [ ] A magnet attracts iron items, iron blocks and the mod's own metal items
- [ ] It does **not** attract non-ferrous things
- [ ] The galena gauntlet crystallises what it should

> This is the tag that failed *whole* on four Fabric builds — one missing member takes the entire
> tag and everything referencing it, while the server still reaches `Done`. If magnets do nothing to
> iron, that is this bug, not a mechanics bug.

### 8. Rendering and the world-level effects

- [ ] The cave-biome lightmap/darkness effect looks right underground
      *(from 26 the custom `ac_lightmap.fsh` shader is dead and the effect is a render-state edit —
      compare 26.2 against 1.21.1 side by side)*
- [ ] Fog behaves in each biome
- [ ] The tremorzilla beam, nuclear explosion and other particle-heavy effects draw
- [ ] Boats and boat-chests render and are rideable
- [ ] The cave map item renders its custom decoration
      *(**known**: no marker on `1.20.4-forge` — a loader patch Forge dropped, documented and
      accepted)*

### 9. Persistence

- [ ] Save, quit to title, reload the world. Mod blocks, block entities and mob NBT survive
- [ ] The nine block entities that implement `ACUpdatePacketReceiver` still sync after reload
      *(their `onDataPacket` signature has four different shapes across the range)*
- [ ] `/codxlib versions` lists the mod with the right version

---

## Smoke pass — six more nodes

Boot, load a world, visit one biome, scroll the creative tabs, craft one thing, spawn three mobs.
Each of these is the *only* node covering a specific fix:

| Node | The one thing it proves |
|---|---|
| `1.20.1-fabric` | fabric-api convention tags v1 only — the oldest tag shape |
| `1.20.6-neoforge` | The 1.20.5 band: eager food effects, flattened int providers, dropped exclusion zones, the four repaired armour sets |
| `1.21.4-neoforge` | The 575 item model definitions **and** the `Sheets loaded too early` fix — check the pewen and thornwood signs render |
| `1.21.6-forge` | The PiP cave book **and** Forge EventBus 7 |
| `1.21.8-forge` | `mixinextras-forge-0.4.1` bundling — without it `@ModifyExpressionValue` silently does nothing. Check the tooltip attribute modifiers on a mod weapon |
| `26.1.2-forge` | The only bootable 26.1.x Forge client on this box |

---

## Recording results

Log each node as PASS / FAIL with the step number. For any failure, capture the full log — the
repo's `DEVELOPMENT.md` documents the failure signature of nearly every trap in this range, and a log
line is usually enough to identify which one it is.

**Do not read a green boot as a pass.** Several of the bugs found in this port reached `Done` and
the title screen while being completely broken: the biome attributes, the ferromagnetic tag, the
silently-uncraftable recipes and the unapplied `@ModifyExpressionValue` handlers all boot clean.

---

## Results — server-side automated battery (2026-08-19)

Two of the four full-pass nodes are done — `1.20.1-forge` and `1.21.11-fabric` — plus
`1.21.11-forge`, added off-plan because it is the cheapest node that sits *above* both the
`tempt_range` (1.21.2) and `defineId` (1.20.5) bug boundaries on Forge. Driven over RCON on a
**fresh normal world**, seed pinned
to `20250819` so the two runs are coordinate-comparable. ~866 commands each: three vanilla-biome and
three vanilla-structure controls, then locate/place every AC biome and structure, `setblock` every
block, `summon` every mob, and `loot spawn` every chest and entity table. Harness in `/tmp/acc-test/`
(`run_node.sh`, `battery.py`, `analyse.py`, `rcon.py`) — worth committing if it is wanted again.

| Section | `1.20.1-forge` | `1.21.11-fabric` | `1.21.11-forge` |
|---|---|---|---|
| vanilla controls (biome + structure) | 3/3 + 3/3 | 3/3 + 3/3 | 3/3 + 3/3 |
| `locate biome` (AC) | 5/6 | 5/6 | 5/6 |
| `locate structure` (AC) | 13/14 | 13/14 | 13/14 |
| `place structure` | 9/14 | 9/14 | 9/14 |
| `setblock` (every block) | 352/354 | 352/354 | 352/354 |
| `summon` (every mob) | 43/43 | 43/43 | 43/43 |
| `loot spawn` chest tables | 16/16 | 16/16 | 16/16 |
| `loot spawn` entity tables | 43/43 | 43/43 | 43/43 |
| exceptions / `ERROR` in server log | none | none | none |

**Every non-100% cell is expected and identical on all three nodes**, which is the point of running
them on one seed:

- `toxic_caves` is simply not within `locate`'s search radius on this seed — the other five AC
  biomes resolve, and the three vanilla controls do too, so the biome source works.
- `ocean_trench` is ocean-only; a land spawn cannot locate it and `place` answers *"That position is
  not loaded"*.
- The four `place structure` misses (`gingerbread_town`, `licowitch_tower`, `soda_bottle`, `volcano`)
  are biome-gated and correctly refuse to generate in a vanilla biome — `/place` honours a
  structure's own predicates.
- The two `setblock` misses are `cave_painting_friendship` / `cave_painting_hunt`: orphan **upstream**
  blockstate JSONs with no registry entry behind them. Not this port's doing, and cosmetic.

**What the battery caught that nothing else could** — three bugs, all fixed, all written up in
`DEVELOPMENT.md` under *"Gotchas the in-world test battery found"*: nothing on Fabric ever posted a Forge
**game**-bus event (so the six cave biomes did not exist on any of the 22 Fabric nodes), 1.21.2's
`TEMPT_RANGE` attribute crashed eleven mobs on all three loaders, and one upstream `defineId` naming
the wrong class bricked the licowitch and the tremorzilla on every node ≥1.20.5. The `1.21.11-forge`
row above is the proof of the last two on Forge *above* both of their version boundaries — the
`1.20.1-forge` row sits below both and its green was never evidence for them.

### Still not covered by this

- `1.21.1-neoforge` and `26.2-fabric` (the other two full-pass nodes) and the six-node smoke pass.
- **Everything client-side and everything interactive.** No GUI, no rendering, no armour, no mob
  behaviour, no crafting, no cave book, no cave map. A summon that succeeds proves the entity
  constructs and survives its first ticks — nothing about what it then does.
- **18 of the 20 `CommonEvents` handlers are still dead on all 22 Fabric nodes** (only the two
  server-lifecycle events are posted). `serverTick` is among them, so cave maps never resolve there.
