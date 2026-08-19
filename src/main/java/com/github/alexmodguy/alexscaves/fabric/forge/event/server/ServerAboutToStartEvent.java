package com.github.alexmodguy.alexscaves.fabric.forge.event.server;

import net.minecraft.server.MinecraftServer;

/**
 * Fabric stand-in for "the server exists and its registries are loaded, but it has not started".
 *
 * <p>The timing is the whole point and it is narrower than it looks: the handler walks the loaded
 * biome registry to build this mod's biome-rarity table and the cave-biome map, so it must run
 * <b>after</b> the datapack registries are frozen and <b>before</b> the first chunk is generated.
 * Fabric's own {@code ServerLifecycleEvents.SERVER_STARTING} is that moment; a later hook would
 * have spawn-area generation already asking questions the table cannot answer yet.
 */
public class ServerAboutToStartEvent extends ServerLifecycleEvent {

    public ServerAboutToStartEvent(MinecraftServer server) {
        super(server);
    }
}
