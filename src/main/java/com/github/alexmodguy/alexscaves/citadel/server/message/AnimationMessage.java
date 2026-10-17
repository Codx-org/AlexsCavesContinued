package com.github.alexmodguy.alexscaves.citadel.server.message;

import com.github.alexmodguy.alexscaves.server.message.ACNetworkContext;
import com.github.alexmodguy.alexscaves.citadel.Citadel;
import net.minecraft.network.FriendlyByteBuf;

public class AnimationMessage {

    private final int entityID;
    private final int index;

    public AnimationMessage(int entityID, int index) {
        this.entityID = entityID;
        this.index = index;
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(AnimationMessage message, ACNetworkContext context) {
            Citadel.PROXY.handleAnimationPacket(message.entityID, message.index);
            context.setPacketHandled(true);
        }
    }

    public static AnimationMessage read(FriendlyByteBuf buf) {
        return new AnimationMessage(buf.readInt(), buf.readInt());
    }

    public static void write(AnimationMessage message, FriendlyByteBuf buf) {
        buf.writeInt(message.entityID);
        buf.writeInt(message.index);
    }
}
