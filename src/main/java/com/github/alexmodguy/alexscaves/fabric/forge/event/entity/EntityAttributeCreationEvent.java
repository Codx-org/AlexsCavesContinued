package com.github.alexmodguy.alexscaves.fabric.forge.event.entity;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

/**
 * Fabric stand-in for "hand over the attribute suppliers for your entity types" — the 45 calls in
 * {@code ACEntityRegistry.initializeAttributes}.
 *
 * <p>This is a <b>mod-bus</b> event on the other two loaders, and the Fabric dispatcher does not
 * post it through {@code ACEventBus} at all: there is no mod bus here, so the initializer is called
 * directly with a freshly constructed event during mod init. That keeps one code path — the same 45
 * lines run on all three loaders — while the delivery differs.
 *
 * <p>{@link #put} forwards into Fabric's own default-attribute registry, which is a static map that
 * must be filled before any entity of that type is constructed. An entity type with no supplier
 * fails at spawn time with a null attribute map, not at registration, so the ordering is load-bearing:
 * the dispatcher runs this after the entity types themselves are registered and before the world
 * loads.
 */
public class EntityAttributeCreationEvent extends Event {

    private final java.util.function.BiConsumer<EntityType<? extends LivingEntity>, AttributeSupplier> sink;

    public EntityAttributeCreationEvent(java.util.function.BiConsumer<EntityType<? extends LivingEntity>, AttributeSupplier> sink) {
        this.sink = sink;
    }

    public void put(EntityType<? extends LivingEntity> type, AttributeSupplier map) {
        sink.accept(type, map);
    }
}
