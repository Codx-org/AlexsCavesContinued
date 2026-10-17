package com.github.alexmodguy.alexscaves.server.message;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class UpdateBossEruptionStatus  {

    private int entityId;
    private boolean erupting;

    public UpdateBossEruptionStatus(int entityId, boolean erupting) {
        this.entityId = entityId;
        this.erupting = erupting;
    }


    public static UpdateBossEruptionStatus read(FriendlyByteBuf buf) {
        return new UpdateBossEruptionStatus(buf.readInt(), buf.readBoolean());
    }

    public static void write(UpdateBossEruptionStatus message, FriendlyByteBuf buf) {
        buf.writeInt(message.entityId);
        buf.writeBoolean(message.erupting);
    }

    public static void handle(UpdateBossEruptionStatus message, ACNetworkContext context) {
        context.setPacketHandled(true);
        Player playerSided = context.getSender();
        if (context.isClientSide()) {
            playerSided = AlexsCaves.PROXY.getClientSidePlayer();
        }
        if(playerSided != null){
            AlexsCaves.PROXY.setPrimordialBossActive(playerSided.level(), message.entityId, message.erupting);
        }
    }

}
