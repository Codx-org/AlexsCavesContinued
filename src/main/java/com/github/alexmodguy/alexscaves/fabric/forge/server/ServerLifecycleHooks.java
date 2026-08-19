package com.github.alexmodguy.alexscaves.fabric.forge.server;

import com.github.alexmodguy.alexscaves.fabric.AlexsCavesFabric;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;

/**
 * Fabric stand-in for Forge's "give me the running server" accessor.
 *
 * <p>Four call sites want it: broadcasting a packet to every player, the two beholder messages
 * resolving a dimension server-side, and the vendored Citadel pathfinding thread factory looking
 * for the server work queue. All four are on the server side of a call that has already happened,
 * so all four tolerate the null the field carries before a world is open — and Forge's own version
 * is equally nullable, which is why none of them needed extra guarding when this arrived.
 *
 * <p>The state itself lives on {@link AlexsCavesFabric}, filled from Fabric API's server lifecycle
 * events; this class is only the name the shared source knows it by.
 */
public final class ServerLifecycleHooks {

    private ServerLifecycleHooks() {
    }

    @Nullable
    public static MinecraftServer getCurrentServer() {
        return AlexsCavesFabric.getServer();
    }
}
