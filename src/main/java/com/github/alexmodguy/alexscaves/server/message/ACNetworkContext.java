package com.github.alexmodguy.alexscaves.server.message;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * What a packet handler is told about the packet it is handling — the mod's replacement for Forge's
 * {@code NetworkEvent.Context}, which 1.20.2 deleted (it came back as
 * {@code CustomPayloadEvent.Context} with the same members) and which no other loader ever had.
 *
 * <p>The four members are exactly the ones Alex's Caves' and Citadel's twenty-two handlers use, so
 * every backend in {@link ACNetwork} can supply them.
 */
public interface ACNetworkContext {

    /**
     * Runs the given work on the receiving side's main thread. Handlers are invoked on the network
     * thread, so anything touching world state has to go through here.
     */
    void enqueueWork(Runnable runnable);

    /** The player who sent this packet, or null when it was sent by the server. */
    @Nullable
    ServerPlayer getSender();

    /** True when this packet is being received on the client. */
    boolean isClientSide();

    /**
     * Tells the loader the packet was consumed. Forge logs a warning for packets that are never
     * marked handled; loaders without the concept ignore it.
     */
    void setPacketHandled(boolean handled);
}
