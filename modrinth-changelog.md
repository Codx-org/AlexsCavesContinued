# 1.0.9

Five reports came in after `1.0.8`. Four of them are fixed here.

## Fixed

- **Hitting a Luxtructosaurus crashed the game on 26.2.** Same crash for Tremorzilla, Hullbreaker,
  Corrodent, Magnetron, Quarry Smasher and the Gossamer Worm, and it's also the Tremorzilla crash
  people hit after resizing one with `/attribute`. These mobs are built out of several hitboxes, and
  on your client those extra hitboxes never got an ID. Every version up to 26.1 quietly ignored
  that; 26.2 throws instead.
- **Going underwater on 1.21.2 and later filled the screen with flat biome fog** and made blocks and
  particles flicker. Worst in the Toxic Caves, Forlorn Hollows and Abyssal Chasm. There was already
  a workaround in the mod for this, but it only kicked in from 1.21.6 up. It starts two versions
  earlier now.
- **Placing a pewen or thornwood boat crashed the game.** Two separate mistakes on top of each
  other: the boat renderer was still written against the old 1.20 boat class, and one of our own
  methods was relying on Minecraft to fill it in, which doesn't work on Fabric.
- **Alex's Caves gear showed no enchantments at an enchanting table on Fabric.** The enchantments
  existed and turned up in generated books, but the table never offered them for the mod's own
  tools and armour.

## Couldn't reproduce

- **The giant sweetberry x-ray in the candy cavity.** We built it four different ways on 26.2 and
  never got it to happen. We did find one thing that was wrong and fixed it: sweetberries were
  counting as a full solid block for lighting and face culling, which they very much are not. If
  you still see it after this update, a screenshot and your mod list would really help.
- **The diving helmet side textures, and the custom helmets not lining up with 3D Skin Layers.** We
  went over the helmet model and its texture sheet and everything lines up here, so we're stuck
  without a screenshot.
