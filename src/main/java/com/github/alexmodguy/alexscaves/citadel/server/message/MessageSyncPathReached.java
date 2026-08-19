package com.github.alexmodguy.alexscaves.citadel.server.message;

import com.github.alexmodguy.alexscaves.server.message.ACNetworkContext;
import com.github.alexmodguy.alexscaves.citadel.client.render.pathfinding.PathfindingDebugRenderer;
import com.github.alexmodguy.alexscaves.citadel.server.entity.pathfinding.raycoms.MNode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashSet;
import java.util.Set;

/**
 * Message to sync the reached positions over to the client for rendering.
 */
public class MessageSyncPathReached {
    /**
     * Set of reached positions.
     */
    public Set<BlockPos> reached = new HashSet<>();

    /**
     * Create the message to send a set of positions over to the client side.
     */
    public MessageSyncPathReached(final Set<BlockPos> reached) {
        super();
        this.reached = reached;
    }

    public void write(final FriendlyByteBuf buf) {
        buf.writeInt(reached.size());
        for (final BlockPos node : reached) {
            buf.writeBlockPos(node);
        }

    }

    public static MessageSyncPathReached read(final FriendlyByteBuf buf) {
        int size = buf.readInt();
        Set<BlockPos> reached = new HashSet<>();
        for (int i = 0; i < size; i++) {
            reached.add(buf.readBlockPos());
        }
        return new MessageSyncPathReached(reached);
    }

    public static class Handler {
        public Handler() {
        }

        public static boolean handle(MessageSyncPathReached message, ACNetworkContext context) {
            context.enqueueWork(() -> {
                context.setPacketHandled(true);

                if (context.isClientSide()) {
                    for (final MNode node : PathfindingDebugRenderer.lastDebugNodesPath) {
                        if (message.reached.contains(node.pos)) {
                            node.setReachedByWorker(true);
                        }
                    }
                }

            });
            return true;
        }
    }
}