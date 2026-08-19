package com.github.alexmodguy.alexscaves.fabric.forge.fml.event.lifecycle;

/**
 * Fabric stand-in for "registration is over, set up the things that read it".
 *
 * <p>The handler wires the pathfinding proxy and the network channel, then enqueues the surface
 * rules, capes, and the effect/block/item/advancement-trigger setup passes. Fired explicitly from
 * {@code AlexsCavesFabric} at the point in the sequence the mod bus would have fired it.
 */
public class FMLCommonSetupEvent extends ParallelDispatchEvent {
}
