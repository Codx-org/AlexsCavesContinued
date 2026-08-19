package com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living;

import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fabric stand-in for the living-entity branch of the event tree — narrows {@link #getEntity()} to
 * {@link LivingEntity}, which nearly every handler below it relies on.
 */
public class LivingEvent extends EntityEvent {

    public LivingEvent(LivingEntity entity) {
        super(entity);
    }

    @Override
    public LivingEntity getEntity() {
        return (LivingEntity) super.getEntity();
    }

    /**
     * Per-entity tick, and this mod's busiest hook by far: it drives the magnetism pull on every
     * ferrous entity, the possession leash, the Vallumraptor hide-and-ambush retarget, the sugar-rush
     * time dilation and a dozen armour behaviours. Both {@code CommonEvents} and {@code ClientEvents}
     * subscribe to it, so it must fire on both sides.
     *
     * <p>Which means the Fabric dispatcher fires it from a mixin on {@code LivingEntity#tick} rather
     * than from a server tick callback: it has to see client-side entities and non-player living
     * entities alike.
     */
    public static class LivingTickEvent extends LivingEvent {

        public LivingTickEvent(LivingEntity entity) {
            super(entity);
        }
    }
}
