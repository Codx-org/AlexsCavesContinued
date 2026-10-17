package com.github.alexmodguy.alexscaves.fabric.forge.common;

import com.github.alexmodguy.alexscaves.fabric.event.ACEventBus;

/**
 * Fabric stand-in for the holder of the game event bus, reduced to the one member the shared source
 * names: {@code EVENT_BUS}.
 *
 * <p>It is reached from both sides here. The two proxies and the mod class register their handler
 * objects on it, and roughly twenty call sites post to it — Citadel's seven published events,
 * {@code AnimationEvent}, and the two places that repost a render event by hand after cancelling
 * the first half. See {@link ACEventBus} for why those posts get a real dispatch rather than being
 * dropped on this loader.
 *
 * <p>The bus is filled from the other direction by {@code fabric/event/**}, which turns Fabric API
 * callbacks and this mod's own mixins into the loader events the handlers expect.
 */
public final class MinecraftForge {

    public static final ACEventBus EVENT_BUS = new ACEventBus();

    private MinecraftForge() {
    }
}
