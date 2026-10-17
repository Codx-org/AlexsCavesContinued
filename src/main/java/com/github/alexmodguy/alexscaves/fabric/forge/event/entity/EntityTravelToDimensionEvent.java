package com.github.alexmodguy.alexscaves.fabric.forge.event.entity;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Cancelable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Fabric stand-in for "this entity is about to change dimension".
 *
 * <p>One handler, and it is a cleanup rather than a veto: a player under Sugar Rush leaves slow
 * motion, because the tick-rate dilation is a property of the level they are leaving. Missing it
 * would strand a server at a modified tick rate after a portal, which is why this event is worth
 * dispatching even though nothing cancels it.
 */
@Cancelable
public class EntityTravelToDimensionEvent extends EntityEvent {

    private final ResourceKey<Level> dimension;

    public EntityTravelToDimensionEvent(Entity entity, ResourceKey<Level> dimension) {
        super(entity);
        this.dimension = dimension;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }
}
