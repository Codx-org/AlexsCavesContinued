# 1.1.0

**Minecraft 26.3 is now supported**, on Fabric and NeoForge. Forge has no 26.3 build, so there is no
Forge jar for it — the same as 1.21.2.

Everything else is unchanged from 1.0.11: if you are not on 26.3, this release behaves exactly like
the last one and there is no reason to rush the update.

## New

- **Minecraft 26.3 support** — Fabric and NeoForge. That brings the mod to 60 builds across
  21 Minecraft versions.

## Fixed (26.3 only)

- **Cave biomes could be found but never actually generated.** `/locate` and Cave Maps would point
  you at a Forlorn Hollows or a Magnetic Caves that simply was not there when you dug down. 26.3
  changed how the game asks for biomes and the mod was only answering half the question.
- **Six crashes and rendering failures** introduced by 26.3's new render system — a crash on the
  first frame inside a cave biome, one when a cave item icon first drew, a disconnect while riding
  or being near moving entities, and several places where the wrong colours or nothing at all was
  drawn.

## Note

On 26.3 this mod needs **CodxLib 1.6.1 or newer**. On every other version 1.3.6 is still fine.
