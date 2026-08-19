package com.github.alexmodguy.alexscaves.server.message;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.entity.util.KeybindUsingMount;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class MountedEntityKeyMessage {

    public int mountId;
    public int playerId;
    public int type;

    public MountedEntityKeyMessage(int mountId, int playerId, int type) {
        this.mountId = mountId;
        this.playerId = playerId;
        this.type = type;
    }


    public MountedEntityKeyMessage() {
    }

    public static MountedEntityKeyMessage read(FriendlyByteBuf buf) {
        return new MountedEntityKeyMessage(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void write(MountedEntityKeyMessage message, FriendlyByteBuf buf) {
        buf.writeInt(message.mountId);
        buf.writeInt(message.playerId);
        buf.writeInt(message.type);
    }

    public static void handle(MountedEntityKeyMessage message, ACNetworkContext context) {
        context.enqueueWork(() -> {
            Player playerSided = context.getSender();
            if (context.isClientSide()) {
                playerSided = AlexsCaves.PROXY.getClientSidePlayer();
            }
            Entity parent = playerSided.level().getEntity(message.mountId);
            Entity keyPresser = playerSided.level().getEntity(message.playerId);
            if (keyPresser != null && parent instanceof KeybindUsingMount mount && keyPresser instanceof Player && keyPresser.isPassengerOfSameVehicle(parent)) {
                mount.onKeyPacket(keyPresser, message.type);
            }
        });
        context.setPacketHandled(true);
    }
}