package com.github.alexmodguy.alexscaves.citadel.server.entity;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;

/**
 * Holds every {@link EntityDataAccessor} Citadel adds to a vanilla entity class: the four magnet
 * attachment fields on {@code Entity} (a delta vector plus the face the entity is stuck to) and the
 * {@code CompoundTag} bag on {@code LivingEntity} that backs {@code ICitadelDataEntity}.
 *
 * <p>The accessors are <em>defined</em> from the mixins, so each {@code defineId} call still runs
 * inside its target's class initialiser and takes that class's slots in the id pool — they are
 * merely <em>stored</em> here rather than in fields merged into the vanilla class.
 *
 * <p>That split is what NeoForge 21.8 requires. Its patched {@code SynchedEntityData#defineId} calls
 * {@code CommonHooks.verifyEntityDataAccessorRegistration}, which rejects a definition whose caller
 * is not the holder class <em>and</em> — even when it is — any holder that declares an
 * {@code EntityDataAccessor} field carrying Mixin's {@code @MixinMerged} annotation. Since the check
 * is a field scan, moving the fields off the vanilla class satisfies it while leaving the definition
 * order, the ids and the wire format byte-for-byte identical on every node. It is fatal only under
 * {@code SharedConstants.IS_RUNNING_IN_IDE} (a warning in production), but a dev server that cannot
 * boot would cost the version walk its only runtime check, so this is fixed rather than tolerated.
 *
 * <p>NeoForge's suggested replacement — syncable data attachments — is deliberately not taken: it
 * exists on one loader only, and the id-pool concern behind the warning is unchanged by either
 * shape, because the mod is required on both sides of the connection.
 *
 * <p>Each field is assigned exactly once, from its target's {@code <clinit>}, before any entity of
 * that class can exist.
 */
public final class CitadelSyncedData {

    public static EntityDataAccessor<Float> MAGNET_DELTA_X;
    public static EntityDataAccessor<Float> MAGNET_DELTA_Y;
    public static EntityDataAccessor<Float> MAGNET_DELTA_Z;
    public static EntityDataAccessor<Direction> MAGNET_ATTACHMENT_DIRECTION;

    public static EntityDataAccessor<CompoundTag> CITADEL_DATA;

    private CitadelSyncedData() {
    }

    /**
     * Publishes the {@code Entity} magnet accessors. Returns a {@code boolean} so the mixin can hold
     * the call in a field initialiser without that field being an {@code EntityDataAccessor} — see
     * the class notes.
     */
    public static boolean installMagnet(EntityDataAccessor<Float> deltaX,
                                        EntityDataAccessor<Float> deltaY,
                                        EntityDataAccessor<Float> deltaZ,
                                        EntityDataAccessor<Direction> attachmentDirection) {
        MAGNET_DELTA_X = deltaX;
        MAGNET_DELTA_Y = deltaY;
        MAGNET_DELTA_Z = deltaZ;
        MAGNET_ATTACHMENT_DIRECTION = attachmentDirection;
        return true;
    }

    /** Publishes the {@code LivingEntity} data bag. Same shape and reason as {@link #installMagnet}. */
    public static boolean installCitadelData(EntityDataAccessor<CompoundTag> citadelData) {
        CITADEL_DATA = citadelData;
        return true;
    }
}
