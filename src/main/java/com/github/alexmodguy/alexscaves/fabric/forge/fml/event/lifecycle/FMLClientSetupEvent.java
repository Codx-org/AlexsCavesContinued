package com.github.alexmodguy.alexscaves.fabric.forge.fml.event.lifecycle;

/**
 * Fabric stand-in for the client half of {@link FMLCommonSetupEvent} — everything that may touch
 * {@code Minecraft}. Fired explicitly from {@code AlexsCavesFabricClient}, so it exists only in a
 * client run, exactly as the loader's own dist split arranges on Forge.
 */
public class FMLClientSetupEvent extends ParallelDispatchEvent {
}
