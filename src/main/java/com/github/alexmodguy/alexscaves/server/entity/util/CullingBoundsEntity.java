package com.github.alexmodguy.alexscaves.server.entity.util;

import net.minecraft.world.phys.AABB;

/**
 * An entity that wants a say in how it is frustum-culled.
 *
 * <p>1.21.2 moved that decision off the entity and onto the renderer: {@code
 * Entity#getBoundingBoxForCulling()} and the public {@code Entity#noCulling} field are both gone,
 * replaced by {@code EntityRenderer#getBoundingBoxForCulling(T)} and {@code
 * EntityRenderer#affectedByCulling(T)}. Seventeen entities in this mod answered the first and one
 * the second, always because the drawn model reaches well outside the collision box — a Tremorzilla
 * tail, a Gum Worm segment, a Magnetron's formed shell.
 *
 * <p>Redeclaring both here is the usual marker-interface trick: below 1.21.2 the vanilla methods
 * the implementations already override satisfy this interface unchanged, and from 1.21.2 up it is
 * the only declaration, so every implementation and every {@code @Override} stays spelled exactly
 * as it was. The shims in {@code client.render.compat} read it back on the renderer side, and
 * {@code ACClientCompat#cullingBox} reads it at the handful of sites that ask a part entity
 * directly.
 */
public interface CullingBoundsEntity {

    /**
     * The box the renderer should test against the frustum. Vanilla's default was the entity's own
     * bounding box, which is what every implementation here starts from and inflates.
     */
    AABB getBoundingBoxForCulling();

    /**
     * The {@code noCulling} field's replacement; {@code false} means "never cull me". Only the Gum
     * Worm segment answers {@code false}, and only when the Entity Culling mod is installed.
     */
    default boolean isAffectedByCulling() {
        return true;
    }
}
