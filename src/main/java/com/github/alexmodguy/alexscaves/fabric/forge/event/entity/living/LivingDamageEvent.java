package com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Cancelable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fabric stand-in for the <b>post</b>-mitigation damage hook, carrying the amount actually about to
 * be dealt and cancellable to drop it.
 *
 * <p>Four situations cancel here — a passenger on a flying mount taking wall/fall damage, a target
 * possessed by a watcher, a player parrying with an extinction spear, and rainbounce boots eating a
 * fall. They are stated once in {@code CommonEvents.acCancelDamage} and the listener around it is
 * gated, because NeoForge 1.21 replaced this one cancellable event with a {@code Pre}/{@code Post}
 * pair whose {@code Pre} is not cancellable. Only that arm names a nested class; on this loader the
 * plain cancellable shape is always the one taken, so no nested types are stubbed.
 *
 * <p>{@link #setAmount} is present for shape fidelity; this mod only ever cancels.
 */
@Cancelable
public class LivingDamageEvent extends LivingEvent {

    private final DamageSource source;
    private float amount;

    public LivingDamageEvent(LivingEntity entity, DamageSource source, float amount) {
        super(entity);
        this.source = source;
        this.amount = amount;
    }

    public DamageSource getSource() {
        return source;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }
}
