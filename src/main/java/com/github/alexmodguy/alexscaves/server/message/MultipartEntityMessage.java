package com.github.alexmodguy.alexscaves.server.message;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class MultipartEntityMessage {

    public int parentId;
    public int playerId;
    public int type;
    public double damage;

    public MultipartEntityMessage(int parentId, int playerId, int type, double damage) {
        this.parentId = parentId;
        this.playerId = playerId;
        this.type = type;
        this.damage = damage;
    }


    public MultipartEntityMessage() {
    }

    public static MultipartEntityMessage read(FriendlyByteBuf buf) {
        return new MultipartEntityMessage(buf.readInt(), buf.readInt(), buf.readInt(), buf.readDouble());
    }

    public static void write(MultipartEntityMessage message, FriendlyByteBuf buf) {
        buf.writeInt(message.parentId);
        buf.writeInt(message.playerId);
        buf.writeInt(message.type);
        buf.writeDouble(message.damage);
    }

    public static void handle(MultipartEntityMessage message, ACNetworkContext context) {
        context.enqueueWork(() -> {
            Player playerSided = context.getSender();
            if (context.isClientSide()) {
                playerSided = AlexsCaves.PROXY.getClientSidePlayer();
            }
            Entity parent = playerSided.level().getEntity(message.parentId);
            Entity interacter = playerSided.level().getEntity(message.playerId);
            if (interacter != null && parent != null && com.github.alexmodguy.alexscaves.server.misc.ACCompat.isMultipartEntity(parent) && interacter.distanceTo(parent) < 16) {
                if (message.type == 0) {
                    if (interacter instanceof Player player) {
                        // 26 folded interactAt into interact, so it takes the hit location. This
                        // packet carries only the two entity ids — the part the player clicked is
                        // resolved server-side — so there is nothing to forward but the origin.
                        //? if >=26 {
                        /*parent.interact(player, player.getUsedItemHand(), net.minecraft.world.phys.Vec3.ZERO);
                        *///?} else {
                        parent.interact(player, player.getUsedItemHand());
                        //?}
                    }
                } else if (message.type == 1) {
                    parent.hurt(parent.damageSources().generic(), (float) message.damage);
                }
            }
        });
        context.setPacketHandled(true);
    }
}