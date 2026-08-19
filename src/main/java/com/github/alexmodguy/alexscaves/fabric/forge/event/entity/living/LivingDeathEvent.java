package com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Cancelable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fabric stand-in for the "this entity is about to die" hook. One handler: a magma cube killed by a
 * primordial frog drops a carmine froglight.
 *
 * <p>Carries {@link Cancelable} because the loader event is, even though this mod only reads it —
 * an unannotated stand-in would make {@code setCanceled(true)} throw for anyone who later wires a
 * handler that does refuse a death.
 */
@Cancelable
public class LivingDeathEvent extends LivingEvent {

    private final DamageSource source;

    public LivingDeathEvent(LivingEntity entity, DamageSource source) {
        super(entity);
        this.source = source;
    }

    public DamageSource getSource() {
        return source;
    }
}
