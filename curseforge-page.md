# CurseForge project setup — Alex's Caves Continued

CurseForge has **no project-creation API** (the upload API only creates *files* on an
existing project), so the project itself has to be made once in the web UI. This file is the
paste-ready content for that form, plus the settings that actually matter.

Create it at **https://legacy.curseforge.com/project/create?game=minecraft** (the legacy UI
exposes every field; the new Studio UI hides some behind later steps).

## Form fields

| Field | Value |
|---|---|
| Game | Minecraft |
| **Project type** | **Mods** — *not* Addons |
| Name | `Alex's Caves Continued` |
| Summary | see below |
| Categories | **World Gen**, **Mobs**, **Adventure and RPG** (add *Structures* if offered) |
| License | LGPL-3.0 |
| Source / Issues | `https://github.com/Codx-org/AlexsCavesContinued` / `.../issues` |
| Logo | `icon.png` in the repo root |

> **Project type is the one setting to get right**, and it can't be changed cheaply
> afterwards. An **Addons** project accepts game-version IDs only from CurseForge's separate
> "Addons" taxonomy, which is incomplete (no `26.2`) and invisible to the Mods listing that
> launchers and modpack tooling search. Uploads fail with
> `errorCode 1009 — Invalid game version ID: <id> belongs to an invalid dependency.`
> CodxLib was created as Addons first and had to be deleted and remade — don't repeat it.

After creating it, copy the numeric **Project ID** from the project page sidebar into
`scripts/.cf_project_id` (gitignored) — the upload script needs the number, not the slug.

## Relations

Add **CodxLib** as a **Required Dependency**.

> ⚠️ CurseForge relations carry **no version**. A relation is just "requires CodxLib" — the
> minimum version is enforced by the loader reading each jar's own manifest, not by the
> store. Don't go looking for a version field; there isn't one.

## Environment tags

This is a content mod, so it is required on **both** sides — tag **Client** *and* **Server**
(ids `9638` and `9639`; every upload must send both or it fails `errorCode 1021`).

## Summary (short description)

Both stores cap this; the text below is 227 characters, inside Modrinth's 256 limit.

```
A continuation of Alex's Caves. Six vast underground biomes — magnetic, primordial, toxic, abyssal, forlorn and candy — with their own mobs, blocks and gear. Now on Fabric, NeoForge and Forge, for Minecraft 1.20.1 through 26.2.
```

## Description

Paste the body below. It is the same text as the Modrinth page
(`modrinth-description.md`) — **keep the two in sync when either changes.**

---

**Alex's Caves Continued** is a continuation of [Alex's Caves](https://modrinth.com/mod/alexs-caves) by AlexModGuy — the same six enormous, hand-authored cave biomes, the same 43 mobs and 350-odd blocks, brought forward to **Minecraft 1.20.1 through 26.2** and to **Fabric and NeoForge** alongside Forge.

Nothing about the mod's content has been changed. It is the original mod, ported — same biomes, same mobs, same recipes, same progression, same cave book.

## What's down there

The Overworld's underground is split into six regions, each generated as one continuous space rather than as a scattering of ore veins. You will not stumble into one by accident at Y=11; you have to go looking, and each is large enough to get lost in.

### ⛏ Magnetic Caves
Sheer iron-bearing rock under a haze of floating metal. Ferrouslimes ooze between the pillars, magnetrons hover and pull, and mine guardians defend the ruins. Magnetism is a real mechanic here — you'll be repelling and attracting blocks, mobs, and yourself, and the **galena gauntlet** turns that into a weapon.

### 🦕 Primordial Caves
A lost world of ferns and mossy stone where the dinosaurs never stopped. Tremorsaurs, grottoceratops, atlatitans and subterranodons range across it, amber preserves what used to live there — and something considerably larger than any of them is still down there.

### ☢ Toxic Caves
An irradiated wasteland of glowing waste and rusted concrete. Radiation is a status effect you have to actually manage, which is what the **hazmat suit** is for. Gammaroaches, nucleepers, brainiacs and raycats infest it, the **nuclear furnace** smelts on uranium, and if things go badly wrong the **tremorzilla** is what comes out of the dark.

### 🌊 Abyssal Chasm
A flooded trench that goes far deeper than the ocean above it, lit by lanternfish and the ruins of a civilisation that is still occupied. The **deep ones** — and their knights and mages — do not want visitors. Bring a **diving suit**, and expect the pressure and the dark to be the real enemies.

### 🕯 Forlorn Hollows
Gothic spires, thornwood, and bridges over a canyon you cannot see the bottom of. Watchers, vespers, gloomoths and the forsaken haunt it; the **licowitch** keeps a tower here. This is where the mod's darkness mechanics live, and where the **spelunkery table** becomes essential.

### 🍬 Candy Cavity
Nougat and hardened sugar, gum worms, sweetish fish, caramel cubes and gummy bears. Gingerbread men have built an entire town down here and they are not friendly. It is exactly as strange as it sounds, and it is a genuine biome with a full progression, not a joke.

## Beyond the biomes

- **The Cave Book** — an in-game guide that fills itself in as you discover things, so you're never reading a wiki in another window.
- **The Spelunkery Table** — decipher the words carved into the walls to unlock what the deeper caves are hiding.
- **Cave maps** — trade for them, then follow them; each points at one biome or structure.
- **14 structures** — abyssal ruins, gingerbread town, the licowitch tower, dino bowls, volcanoes, underground cabins and more.
- **Full gear progression per biome** — primordial, hazmat, diving, gingerbread and more, each with its own armour set, tools and abilities.
- **467 recipes, 146 advancements, 575 items, 354 blocks** — the whole of the original mod.

## What "Continued" means

Alex's Caves shipped for Forge on Minecraft 1.20.1 only. This project brings the identical mod to **58 builds** — every Minecraft version from 1.20.1 to 26.2, on all three loaders that shipped for it.

- **Content is untouched.** No rebalancing, no additions, no removals. If you played the original, this is that.
- **Citadel is built in.** The original required Citadel as a separate download; that code is bundled here, so there is one less dependency to keep in sync.
- **Every version is a real port**, not a manifest bump — the mod's 66 mixins are verified against each Minecraft version's actual bytecode, and each build is checked on its own.

## Requirements

| | |
|---|---|
| **Required dependency** | **[CodxLib](https://modrinth.com/mod/codxlib)** — install the file matching your Minecraft version and loader |
| **Citadel** | **Not needed.** It's bundled. |
| **Sides** | Client **and** server — this is a content mod, so both need it |

Install the Alex's Caves Continued file **and** the CodxLib file for your exact Minecraft version and loader, and drop both in `mods`.

## Supported versions

Pick the file matching your Minecraft version **and** loader.

| Minecraft | Fabric | NeoForge | Forge |
|---|:---:|:---:|:---:|
| 1.20.1 | ✅ | — | ✅ |
| 1.20.2 | ✅ | — | — |
| 1.20.3 | ✅ | — | — |
| 1.20.4 | ✅ | ✅ | ✅ |
| 1.20.5 | ✅ | — | — |
| 1.20.6 | ✅ | ✅ | ✅ |
| 1.21 | ✅ | ✅ | ✅ |
| 1.21.1 | ✅ | ✅ | ✅ |
| 1.21.2 | ✅ | ✅ | — |
| 1.21.3 | ✅ | ✅ | ✅ |
| 1.21.4 | ✅ | ✅ | ✅ |
| 1.21.5 | ✅ | ✅ | ✅ |
| 1.21.6 | ✅ | ✅ | ✅ |
| 1.21.7 | ✅ | ✅ | ✅ |
| 1.21.8 | ✅ | ✅ | ✅ |
| 1.21.9 | ✅ | ✅ | ✅ |
| 1.21.10 | ✅ | ✅ | ✅ |
| 1.21.11 | ✅ | ✅ | ✅ |
| 26.1 | ✅ | ✅ | ✅ |
| 26.1.1 | ✅ | ✅ | ✅ |
| 26.1.2 | ✅ | ✅ | ✅ |
| 26.2 | ✅ | ✅ | ✅ |

A dash means that loader never released a build for that Minecraft version, so there is nothing to install there — it isn't a limitation of this mod.

## Credits

**Alex's Caves is AlexModGuy's work** — the biomes, the mobs, the art, the music, the design, all of it. This project exists to keep that work playable on current Minecraft and on every loader, and claims none of it.

- Original mod: [AlexModGuy/AlexsCaves](https://github.com/AlexModGuy/AlexsCaves)
- Citadel (bundled): [AlexModGuy/Citadel](https://github.com/AlexModGuy/Citadel)

## Links

- **Source:** https://github.com/Codx-org/AlexsCavesContinued
- **Issues / bug reports:** https://github.com/Codx-org/AlexsCavesContinued/issues

Licensed under **LGPL-3.0**, the same licence as the original.
