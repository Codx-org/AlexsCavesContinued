package com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Cancelable;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fabric stand-in for the "this entity is about to regain health" hook, fired before the heal is
 * applied and cancellable to refuse it. That is exactly what this mod does with it — a target under
 * an effect that forbids regeneration heals for nothing.
 *
 * <p>{@link #setAmount} is here because the loader event has it and the shape must match; nothing in
 * this tree rescales a heal, it only ever refuses one. The Fabric dispatcher reads the amount back
 * so a future rescale would work rather than being silently dropped.
 */
@Cancelable
public class LivingHealEvent extends LivingEvent {

    private float amount;

    public LivingHealEvent(LivingEntity entity, float amount) {
        super(entity);
        this.amount = amount;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }
}
