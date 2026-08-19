package com.github.alexmodguy.alexscaves.fabric.forge.event.entity;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;
import net.minecraft.world.entity.Entity;

/**
 * Fabric stand-in for the root of every entity-scoped loader event this mod listens to, and the
 * reason this hierarchy is a hierarchy rather than a set of unrelated classes.
 *
 * <p>{@code getEntity()} is expected to come back already narrowed at the call site:
 * this class gives {@link Entity}, {@link com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living.LivingEvent}
 * gives a {@code LivingEntity} and
 * {@link com.github.alexmodguy.alexscaves.fabric.forge.event.entity.player.PlayerEvent} gives a
 * {@code Player}. The other two loaders do that with covariant overrides, so these stubs do too —
 * flattening it to one {@code Entity}-typed getter would force a cast at every one of the ~90 call
 * sites in {@code CommonEvents} and {@code ClientEvents} and put this loader on a different source
 * text from the other two.
 *
 * <p>The hierarchy is also load-bearing at dispatch time, not just at compile time:
 * {@link com.github.alexmodguy.alexscaves.fabric.event.ACEventBus} walks an event's superclasses, so
 * a handler declared on this class would see every subclass below it — the same property the mod
 * relies on where it subscribes to a base event.
 */
public class EntityEvent extends Event {

    private final Entity entity;

    public EntityEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}
