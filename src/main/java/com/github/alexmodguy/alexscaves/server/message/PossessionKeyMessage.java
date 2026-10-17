package com.github.alexmodguy.alexscaves.server.message;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.entity.util.PossessesCamera;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class PossessionKeyMessage {

    public int watcher;
    public int playerId;
    public int type;

    public PossessionKeyMessage(int watcher, int playerId, int type) {
        this.watcher = watcher;
        this.playerId = playerId;
        this.type = type;
    }


    public PossessionKeyMessage() {
    }

    public static PossessionKeyMessage read(FriendlyByteBuf buf) {
        return new PossessionKeyMessage(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void write(PossessionKeyMessage message, FriendlyByteBuf buf) {
        buf.writeInt(message.watcher);
        buf.writeInt(message.playerId);
        buf.writeInt(message.type);
    }

    public static void handle(PossessionKeyMessage message, ACNetworkContext context) {
        context.enqueueWork(() -> {
            Player playerSided = context.getSender();
            if (context.isClientSide()) {
                playerSided = AlexsCaves.PROXY.getClientSidePlayer();
            }
            Entity watcher = playerSided.level().getEntity(message.watcher);
            Entity keyPresser = playerSided.level().getEntity(message.playerId);
            if (watcher instanceof PossessesCamera watcherEntity && keyPresser instanceof Player) {
                watcherEntity.onPossessionKeyPacket(keyPresser, message.type);
            }
        });
        context.setPacketHandled(true);
    }
}