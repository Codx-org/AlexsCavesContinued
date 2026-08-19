package com.github.alexmodguy.alexscaves.fabric.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;

/**
 * Fabric stand-in for the loader's multipart base class.
 *
 * <p>The original is three members — a parent, its accessor, and a spawn packet that refuses to
 * exist — so vendoring it is cheaper than any abstraction over it. Checked against the 1.20.1 jar
 * rather than remembered: {@code javap} lists the field, {@code getParent()} and
 * {@code getAddEntityPacket()} and nothing else, so a subclass overriding {@code is} or
 * {@code shouldBeSaved} (as {@code ACMultipartEntity} does) overrides vanilla, not this.
 *
 * <p>Five classes extend it here — {@code ACMultipartEntity} and the four parts that skip it
 * ({@code MagnetronPartEntity}, {@code HullbreakerPartEntity}, {@code TremorzillaPartEntity},
 * {@code SauropodPartEntity}) — and a Fabric-only {@code replacements} rule
 * ({@code !fab-partentity}) re-points the type name, so those files stay byte-identical across all
 * three loaders.
 *
 * <p>No gate on the spawn packet: its header is spelled exactly as the 38 other overrides in this
 * tree are, so 1.21's {@code ServerEntity} parameter is threaded in by the existing
 * {@code !mc21-addentitypacket-decl} rule.
 *
 * <p><b>⚠️ The CLASS is the easy half; the LEVEL PLUMBING is not vendored here.</b> The loaders do
 * not merely supply this base class — they patch vanilla so parts are visible to the world:
 * {@code Level#getEntities}/{@code getEntityCollisions}/{@code getNearestEntity} fold in a part
 * collection, {@code ServerLevel#getEntity(int)} falls back to a part index, and both levels'
 * tracking callbacks fill those from {@code Entity#getParts()}. Vanilla has the identical mechanism
 * hard-typed to {@code EnderDragonPart}, so on this loader it is closed to us and the equivalent
 * has to be built with mixins. Until it is, the seven multipart mobs render and tick but their
 * segments are not pickable, attackable or collidable — each parent's own hitbox still is.
 *
 * <p>{@code isMultipartEntity()}/{@code getParts()} are patches on vanilla {@code Entity}, so they
 * have no home here either; see {@link com.github.alexmodguy.alexscaves.server.entity.util.ACMultipartOwner}
 * for the half of the contract that solves.
 */
public abstract class PartEntity<T extends Entity> extends Entity {

    private final T parent;

    public PartEntity(T parent) {
        super(parent.getType(), parent.level());
        this.parent = parent;
    }

    public T getParent() {
        return parent;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        throw new UnsupportedOperationException();
    }
}
