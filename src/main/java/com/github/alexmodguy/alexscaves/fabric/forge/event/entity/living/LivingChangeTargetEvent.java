package com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Cancelable;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fabric stand-in for "this mob is about to acquire a new target", cancellable to refuse the change.
 *
 * <p>Two behaviours use it: a hidden Vallumraptor keeps its ambush rather than being pulled onto a
 * new target, and an entity under a possession or fear effect is stopped from retargeting at all.
 *
 * <p>The loader event also carries a target-type discriminator (the reason for the change: combat,
 * behaviour goal, …). Nothing in this tree reads it, so it is not modelled — a field no handler
 * touches would have to be invented by the dispatcher out of information Fabric does not give it.
 */
@Cancelable
public class LivingChangeTargetEvent extends LivingEvent {

    private LivingEntity newTarget;

    public LivingChangeTargetEvent(LivingEntity entity, LivingEntity newTarget) {
        super(entity);
        this.newTarget = newTarget;
    }

    public LivingEntity getNewTarget() {
        return newTarget;
    }

    public void setNewTarget(LivingEntity newTarget) {
        this.newTarget = newTarget;
    }
}
