package com.github.alexmodguy.alexscaves.fabric.forge.event.entity.player;

import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living.LivingEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Fabric stand-in for the player branch of the event tree, narrowing {@link #getEntity()} to
 * {@link Player}.
 */
public class PlayerEvent extends LivingEvent {

    public PlayerEvent(Player player) {
        super(player);
    }

    @Override
    public Player getEntity() {
        return (Player) super.getEntity();
    }

    /**
     * A player has finished joining the server.
     *
     * <p>One handler, and it is the reason this event has to fire on a dedicated server as well as
     * in singleplayer: it syncs the mod's config-driven state to the joining client and runs the
     * mod-compat notice. The dispatcher fires it from Fabric's own join callback, which is the
     * equivalent moment — after the player entity exists and the connection can carry packets.
     */
    public static class PlayerLoggedInEvent extends PlayerEvent {

        public PlayerLoggedInEvent(ServerPlayer player) {
            super(player);
        }
    }
}
