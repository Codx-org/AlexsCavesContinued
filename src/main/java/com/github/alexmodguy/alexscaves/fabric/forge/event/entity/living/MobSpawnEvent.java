package com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living;

import net.minecraft.world.entity.Mob;

/**
 * Fabric stand-in for the mob-spawn branch of the event tree, narrowing {@link #getEntity()} to
 * {@link Mob}.
 *
 * <p>Only {@link FinalizeSpawn} is used, and only for the three vanilla mobs this mod retrofits:
 * a creeper learns to avoid raycats, a drowned spawning in the Abyssal Chasm gets diving gear, and
 * a fox learns to hunt gingerbread men. All three edit goal selectors, so the event has to arrive
 * <b>before</b> the mob starts ticking — the dispatcher fires it out of {@code Mob#finalizeSpawn},
 * which is the same moment the other two loaders use.
 */
public class MobSpawnEvent extends LivingEvent {

    public MobSpawnEvent(Mob entity) {
        super(entity);
    }

    @Override
    public Mob getEntity() {
        return (Mob) super.getEntity();
    }

    /**
     * A mob has been placed and is about to have its spawn-time randomisation applied.
     *
     * <p>The loader event exposes the level, the position, the spawn type and the spawn data as
     * well; none of this mod's three handlers reads any of them, so they are not modelled. Adding
     * one later means adding it here <i>and</i> at the dispatcher's call site, which has all of them
     * in hand.
     */
    public static class FinalizeSpawn extends MobSpawnEvent {

        public FinalizeSpawn(Mob entity) {
            super(entity);
        }
    }
}
