package com.github.alexmodguy.alexscaves.server.entity.util;

/**
 * A vehicle that wants its riders drawn standing rather than sitting.
 *
 * <p>{@code Entity#shouldRiderSit()} is a <b>loader patch</b> ({@code IForgeEntity}, NeoForge's
 * {@code IEntityExtension}), not vanilla — vanilla has no way for a vehicle to say this at all, and
 * simply sits every passenger. Two of this mod's mounts want the opposite: the gum worm's segments
 * carry a standing rider along the worm's back, and the subterranodon is gripped rather than sat on.
 *
 * <p>The interface is <b>unconditional</b>, and declares the method with the loader's exact name and
 * signature on purpose. On Forge and NeoForge the inherited patch already satisfies it, so
 * implementing it costs those loaders nothing and the two {@code @Override}s keep meaning what they
 * always did; on Fabric the very same {@code @Override}s are satisfied by this declaration instead.
 * That is why neither entity needed a gate — the same trick {@code ACUpdatePacketReceiver} and
 * {@code ACTickingItem} use.
 *
 * <p>Reading it goes through {@code ACCompat#shouldRiderSit(Entity)}, which asks the loader on
 * Forge/NeoForge and this interface on Fabric. The fallback for a vehicle that does not implement it
 * is {@code true}, which is the loaders' own interface default — read out of {@code IForgeEntity} in
 * the 1.20.1 universal jar, where the whole body is {@code iconst_1; ireturn}.
 */
public interface ACRiderSitEntity {

    boolean shouldRiderSit();
}
