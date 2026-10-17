package com.github.alexmodguy.alexscaves.fabric.forge.event.server;

import net.minecraft.server.MinecraftServer;

/**
 * Fabric stand-in for "the server is shutting down but is still usable".
 *
 * <p>The handler drops the sugar-rush tick-rate modifiers and empties this mod's cave-map worker
 * queue. Both are process-lifetime state on a singleplayer client, which is exactly why this has to
 * fire on an integrated server too: leaving a tick-rate modifier behind would carry a slowed clock
 * into the next world the player opens.
 */
public class ServerStoppingEvent extends ServerLifecycleEvent {

    public ServerStoppingEvent(MinecraftServer server) {
        super(server);
    }
}
