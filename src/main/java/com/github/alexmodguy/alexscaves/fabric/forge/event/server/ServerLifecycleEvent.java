package com.github.alexmodguy.alexscaves.fabric.forge.event.server;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;
import net.minecraft.server.MinecraftServer;

/**
 * Fabric stand-in for the base of the server lifecycle events — everything the mod listens to here
 * wants nothing but the server itself.
 */
public class ServerLifecycleEvent extends Event {

    private final MinecraftServer server;

    public ServerLifecycleEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer getServer() {
        return server;
    }
}
