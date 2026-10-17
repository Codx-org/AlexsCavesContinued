package com.github.alexmodguy.alexscaves.fabric.forge.fml.event.lifecycle;

/**
 * Fabric stand-in for "every mod has finished loading".
 *
 * <p>This is the phase the mod-compat bridge and the fluid post-init hang off, and the phase whose
 * timing looks load-bearing because two of its three tasks are named "after all mods loaded". On
 * this loader it is not, and {@code AlexsCavesFabric} posts it inline at the end of
 * {@code onInitialize} rather than deferring it: both of those tasks only ask
 * {@code CodxLib.isModLoaded}, and Fabric's mod list is fully populated before <i>any</i>
 * entrypoint runs, so there is no half-populated window to wait out. The third — the fluid
 * interaction table — names only this mod's own fluids and blocks.
 */
public class FMLLoadCompleteEvent extends ParallelDispatchEvent {
}
