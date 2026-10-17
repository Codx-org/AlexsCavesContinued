# 1.1.2

A bug-fix release, mostly for Minecraft 26.3, plus a new Tremorzilla option.

## Fixed

- **Cave maps crashed the game on Minecraft 26.3**, and the world kept crashing on every load while the map was still in your hand. Affected worlds load normally again after updating.
- **The Holocoder crashed when scanning a mob** on Fabric. It now binds the mob without crashing on every loader.
- **Irradiated z-fought with the player's skin on 26.3** instead of glowing. It now draws as a translucent green shell around you.
- **Toxic Caves, Magnetic Caves and the other cave biomes generated their stone only in tiny patches** on 26.2 and 26.3 when TerraBlender was installed. This also restores the cave floors dinosaurs spawn on. Only newly generated chunks are affected.
- **The Resistor Shield didn't block damage** on 1.21.5 and newer, so its enchantments never triggered either.
- **Players riding Alex's Caves mounts faced a fixed direction** on 1.21.2 and newer, instead of following the mount.
- **The Dreadbow with Relentless Darkness kept firing after its arrows ran out.** It now stops, except in creative.
- **The Limestone and Extinction Spears showed a jab instead of their charge pose** on 1.21.11 and newer.
- **Alex's Caves armour (gingerbread, diving, hazmat, primordial, darkness) rendered as a broken copy of the wearer's body** on Fabric 1.20.1–1.21.1, worst on armour stands.
- **The camera sat at the wrong distance from the block while walking on the side or underside of magnetic blocks.** It now sits where it did in the original Alex's Caves.
- **Cave biomes could appear in the End** when a mod such as Nullscape gives the End an Overworld-style biome layout.
- **Other mods' biomes could generate without vegetation or structures**, because Alex's Caves added its cave biomes to the world's biome list before other mods had finished adding theirs.

## Added

- **`tremorzilla_beam_breaks_blocks`** (server config, and page 2 of `/acc menu`): turn it off and the Tremorzilla's beam still hurts what it hits but leaves the terrain alone, without disabling `mobGriefing` for every other mob.
