package com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Cancelable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fabric stand-in for the <b>pre</b>-mitigation damage hook — fired before armour, effects and
 * enchantments are applied, and cancellable to refuse the hit outright.
 *
 * <p>Two behaviours ride on it: a raised resistor shield turns an incoming arrow back on its shooter,
 * and a stunned attacker lands nothing at all.
 *
 * <p>Distinct from {@link LivingDamageEvent}, which fires after mitigation with the final amount.
 * The mod subscribes to both and they are not interchangeable.
 */
@Cancelable
public class LivingAttackEvent extends LivingEvent {

    private final DamageSource source;
    private final float amount;

    public LivingAttackEvent(LivingEntity entity, DamageSource source, float amount) {
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
}
