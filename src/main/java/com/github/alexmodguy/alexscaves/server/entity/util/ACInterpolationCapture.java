package com.github.alexmodguy.alexscaves.server.entity.util;

import net.minecraft.world.phys.Vec3;

/**
 * The capture half of this mod's "suppress vanilla interpolation" idiom.
 *
 * <p>Sixteen entities here drive their own position lerp from private {@code lx/ly/lz/lyr/lxr/
 * lSteps} fields and want the client's interpolation target written into those fields rather than
 * applied by vanilla. Through 1.21.11 that was expressed by overriding a vanilla method and simply
 * not calling {@code super}; from 26.3 it has to be an object handed to vanilla instead, and the
 * fields it writes are {@code private} to each entity — so the entity passes a lambda that closes
 * over its own fields and {@code ACLerpInterpolationHandler} invokes it.
 *
 * <p>Every type in this signature exists unchanged on all 60 nodes, which is the whole reason this
 * interface is separate from and ungated relative to {@code ACLerpInterpolationHandler}: only the
 * handler mentions 26.3-only types, so only the handler needs a version gate.
 */
@FunctionalInterface
public interface ACInterpolationCapture {

    /**
     * Record the interpolation target. Called on the client, once per position packet.
     *
     * @param pos  the end position of the requested path
     * @param yRot target yaw, degrees
     * @param xRot target pitch, degrees
     */
    void captureInterpolationTarget(Vec3 pos, float yRot, float xRot);
}
