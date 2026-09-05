package com.github.alexmodguy.alexscaves.server.entity.util;

/**
 * The parent half of the multipart contract, spelled so that it costs nothing on any loader.
 *
 * <p>Forge and NeoForge do not just supply {@code PartEntity} — they <b>patch vanilla
 * {@code Entity}</b> with {@code isMultipartEntity()} and {@code getParts()}. Fabric's jar is
 * unpatched, so the seven parents here ({@code SauropodBaseEntity}, {@code TremorzillaEntity},
 * {@code HullbreakerEntity}, {@code MagnetronEntity}, {@code GossamerWormEntity},
 * {@code CorrodentEntity} and {@code QuarrySmasherEntity}) have nothing to override and their
 * {@code @Override}s stop compiling.
 *
 * <p>A mixin cannot fix that: mixin-merged members are invisible to javac. So the two methods are
 * declared here with <b>exactly the signatures the platform patch uses</b> and the seven parents
 * implement this interface <b>unconditionally, on every loader</b>. On Forge/NeoForge their existing
 * declarations satisfy both this interface and the vanilla patch at once, so {@code @Override} stays
 * valid everywhere and not one of the seven files needs a Stonecutter gate.
 *
 * <p>The array type is written fully qualified as {@code net.minecraftforge.entity.PartEntity} — the
 * NeoForge namespace rewrite and the Fabric {@code !fab-partentity} redirect each re-point it, the
 * same way the seven parents already spell their own return type.
 *
 * <p>A call site holding a bare {@code Entity} cannot use this and goes through
 * {@code ACCompat.isMultipartEntity(Entity)} instead, which also keeps the vanilla ender dragon the
 * platform patch covered.
 */
public interface ACMultipartOwner {

    boolean isMultipartEntity();

    net.minecraftforge.entity.PartEntity<?>[] getParts();

    /**
     * Hands every part the entity id the server knows it by. Call it from {@code setId}.
     *
     * <p>26.2 stopped handing an entity its id at construction: {@code Entity}'s constructor asks
     * the level for one, and a client level answers {@code 0} — the real id arrives with the
     * add-entity packet, which is why {@code recreateFromPacket} ends in a {@code setId}. A part
     * entity is never in any level's entity list, so on the client nothing ever gave it one, and
     * {@code Entity#getId} throws <i>"Tried to access entity ID before ID assignment"</i> the
     * moment that id is needed. It is needed on the two commonest paths there are:
     * {@code MultiPlayerGameMode#interact} and {@code #attack} both put the picked entity's id
     * straight into the packet they send — so from 26.2 up, one right-click or one hit aimed at a
     * tail, a neck or a head crashed the client outright.
     *
     * <p>The vanilla ender dragon has done exactly this since 1.17, and the arithmetic is its own:
     * every parent in this mod builds its whole part array inside its constructor, so the server
     * allocated their ids from its counter immediately after its own, in array order.
     * {@code parentId + i + 1} is therefore the id the <i>server</i> knows part {@code i} by, which
     * is what makes the interact and attack packets resolve to the right part rather than merely
     * stop throwing.
     *
     * <p>Below 26.2 an entity was given an id at construction, so this only overwrites a
     * client-side id that no index was keyed on — the client keeps its parts in a plain list. It
     * needs no version gate, and on those versions it is an improvement rather than a fix: the
     * ids it writes are the ones the server can actually resolve.
     */
    default void acAssignPartIds(int parentId) {
        net.minecraftforge.entity.PartEntity<?>[] parts = this.getParts();
        if (parts == null) {
            return;
        }
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] != null) {
                parts[i].setId(parentId + i + 1);
            }
        }
    }
}
