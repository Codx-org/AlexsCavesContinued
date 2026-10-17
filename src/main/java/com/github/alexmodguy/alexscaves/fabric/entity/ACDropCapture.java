package com.github.alexmodguy.alexscaves.fabric.entity;

import net.minecraft.world.entity.item.ItemEntity;

import java.util.Collection;

/**
 * Fabric stand-in for Forge's {@code Entity#captureDrops(Collection)}.
 *
 * <p>Both loaders patch {@code Entity} with a nullable {@code captureDrops} list: while it is
 * non-null, {@code spawnAtLocation} adds the {@code ItemEntity} to the list instead of to the
 * level, and the setter returns whatever list was installed before. That is the only way to get
 * hold of a mob's own death drops as objects, which is what {@code GumWormEntity} needs — a gum
 * worm dies underground and re-drops everything at the surface.
 *
 * <p>Vanilla has no equivalent, so on Fabric the field and the {@code spawnAtLocation} diversion
 * are supplied by {@code mixin.fabric.EntityDropCaptureMixin}, which implements this interface on
 * {@code Entity}. Reached only through {@code ACCompat#captureDrops}; never cast to directly.
 */
public interface ACDropCapture {

    /**
     * Installs {@code value} as the capture list (or {@code null} to stop capturing) and returns
     * the list that was installed before — exactly Forge's contract.
     */
    Collection<ItemEntity> ac_captureDrops(Collection<ItemEntity> value);
}
