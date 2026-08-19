package com.github.alexmodguy.alexscaves.fabric.forge.event.entity.player;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Cancelable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Fabric stand-in for "this player is about to left-click that entity", cancellable to refuse the
 * attack. One handler: you cannot punch the dinosaur you are riding alongside somebody else.
 *
 * <p>Cancelling has to happen before the attack is processed <i>and</i> before the arm swings, so
 * the dispatcher fires it at the top of the player's attack path rather than from a damage hook —
 * a {@code LivingAttackEvent} cancellation would stop the damage but still play the swing.
 */
@Cancelable
public class AttackEntityEvent extends PlayerEvent {

    private final Entity target;

    public AttackEntityEvent(Player player, Entity target) {
        super(player);
        this.target = target;
    }

    public Entity getTarget() {
        return target;
    }
}
