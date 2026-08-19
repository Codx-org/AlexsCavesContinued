package com.github.alexmodguy.alexscaves.citadel.server.message;

import com.github.alexmodguy.alexscaves.server.message.ACNetworkContext;
import com.github.alexmodguy.alexscaves.citadel.Citadel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class SyncClientTickRateMessage {
    private final CompoundTag compound;

    public SyncClientTickRateMessage(CompoundTag compound) {
        this.compound = compound;
    }

    public static void write(SyncClientTickRateMessage message, FriendlyByteBuf packetBuffer) {
        PacketBufferUtils.writeTag(packetBuffer, message.compound);
    }

    public static SyncClientTickRateMessage read(FriendlyByteBuf packetBuffer) {
        return new SyncClientTickRateMessage(PacketBufferUtils.readTag(packetBuffer));
    }

    public static class Handler {

        public static void handle(final SyncClientTickRateMessage message, ACNetworkContext context) {
            context.setPacketHandled(true);
            context.enqueueWork(() -> {
                if (context.isClientSide()) {
                    Citadel.PROXY.handleClientTickRatePacket(message.compound);

                }
            });
        }
    }
}