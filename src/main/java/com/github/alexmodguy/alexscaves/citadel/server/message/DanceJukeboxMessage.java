package com.github.alexmodguy.alexscaves.citadel.server.message;

import com.github.alexmodguy.alexscaves.server.message.ACNetworkContext;
import com.github.alexmodguy.alexscaves.citadel.Citadel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class DanceJukeboxMessage {

    public int entityID;
    public boolean dance;
    public BlockPos jukeBox;

    public DanceJukeboxMessage(int entityID, boolean dance, BlockPos jukeBox) {
        this.entityID = entityID;
        this.dance = dance;
        this.jukeBox = jukeBox;
    }

    public DanceJukeboxMessage() {
    }

    public static DanceJukeboxMessage read(FriendlyByteBuf buf) {
        return new DanceJukeboxMessage(buf.readInt(), buf.readBoolean(), buf.readBlockPos());
    }

    public static void write(DanceJukeboxMessage message, FriendlyByteBuf buf) {
        buf.writeInt(message.entityID);
        buf.writeBoolean(message.dance);
        buf.writeBlockPos(message.jukeBox);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(DanceJukeboxMessage message, ACNetworkContext context) {
            context.setPacketHandled(true);
            context.enqueueWork(() -> {
                Player player = context.getSender();
                if (context.isClientSide()) {
                    player = Citadel.PROXY.getClientSidePlayer();
                }

                if (player != null) {
                    Citadel.PROXY.handleJukeboxPacket(player.level(), message.entityID, message.jukeBox, message.dance);

                }
            });
        }
    }
}