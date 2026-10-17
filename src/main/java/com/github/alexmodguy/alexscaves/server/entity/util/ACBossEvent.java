package com.github.alexmodguy.alexscaves.server.entity.util;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.message.UpdateBossBarMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

public class ACBossEvent extends ServerBossEvent {

    private final int renderType;

    // 26 deleted the convenience constructor that made up the bar's id, leaving only the four-argument
    // form. The id is what the client keys a boss bar on and nothing persists it, so a fresh random
    // one is exactly what the old constructor produced.
    public ACBossEvent(Component component, int renderType) {
        //? if >=26 {
        /*super(java.util.UUID.randomUUID(), component, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        *///?} else {
        super(component, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
        //?}
        this.renderType = renderType;
    }

    public int getRenderType() {
        return renderType;
    }

    public void addPlayer(ServerPlayer serverPlayer) {
        AlexsCaves.sendNonLocal(new UpdateBossBarMessage(this.getId(), renderType), serverPlayer);
        super.addPlayer(serverPlayer);
    }

    public void removePlayer(ServerPlayer serverPlayer) {
        AlexsCaves.sendNonLocal(new UpdateBossBarMessage(this.getId(), -1), serverPlayer);
        super.removePlayer(serverPlayer);
    }
}
