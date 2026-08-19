package com.github.alexmodguy.alexscaves.server.entity.util;

import net.minecraft.network.syncher.EntityDataAccessor;

/**
 * Holds every {@link EntityDataAccessor} Alex's Caves itself adds to a vanilla entity class — at
 * present just the magnet grace timer on {@code FallingBlockEntity}, which keeps a block the player
 * has just pulled loose from being re-attracted for a few ticks.
 *
 * <p>Same shape and same reason as Citadel's {@code CitadelSyncedData}: the {@code defineId} call
 * stays in the mixin, so it runs inside the target's own class initialiser and takes that class's
 * slot in the id pool, but the accessor is stored here instead of in a field merged into the vanilla
 * class. NeoForge 21.8's {@code CommonHooks.verifyEntityDataAccessorRegistration} scans the holder
 * class for {@code EntityDataAccessor} fields carrying Mixin's {@code @MixinMerged} annotation and
 * throws in dev when it finds any; moving the field satisfies that while leaving ids and wire format
 * identical on every node.
 *
 * <p>Assigned exactly once, from {@code FallingBlockEntity.<clinit>}.
 */
public final class ACSyncedData {

    public static EntityDataAccessor<Integer> FALL_BLOCK_TIME;

    private ACSyncedData() {
    }

    /**
     * Publishes the accessor. Returns a {@code boolean} so the mixin can hold the call in a field
     * initialiser without that field being an {@code EntityDataAccessor} — see the class notes.
     */
    public static boolean installFallBlockTime(EntityDataAccessor<Integer> fallBlockTime) {
        FALL_BLOCK_TIME = fallBlockTime;
        return true;
    }
}
