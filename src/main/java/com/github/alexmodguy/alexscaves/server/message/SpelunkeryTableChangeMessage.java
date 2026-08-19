package com.github.alexmodguy.alexscaves.server.message;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.inventory.SpelunkeryTableMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class SpelunkeryTableChangeMessage {

    public boolean pass;

    public SpelunkeryTableChangeMessage(boolean pass) {
        this.pass = pass;
    }


    public SpelunkeryTableChangeMessage() {
    }

    public static SpelunkeryTableChangeMessage read(FriendlyByteBuf buf) {
        return new SpelunkeryTableChangeMessage(buf.readBoolean());
    }

    public static void write(SpelunkeryTableChangeMessage message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.pass);
    }

    public static void handle(SpelunkeryTableChangeMessage message, ACNetworkContext context) {
        context.setPacketHandled(true);
        Player player = context.getSender();
        if (context.isClientSide()) {
            player = AlexsCaves.PROXY.getClientSidePlayer();
        }
        if (player != null) {
            if (player.containerMenu instanceof SpelunkeryTableMenu tableMenu) {
                tableMenu.onMessageFromScreen(player, message.pass);
            }
        }
    }
}