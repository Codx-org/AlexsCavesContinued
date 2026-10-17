package com.github.alexmodguy.alexscaves.client.render.compat;

/**
 * Pre-1.21.2 {@code net.minecraft.world.entity.PowerableMob}.
 *
 * <p>1.21.2 deleted the interface: the only thing that read it was {@code EnergySwirlLayer}, and
 * that now takes the flag off a render state instead. Two of this mod's entities implement it and
 * the sibling {@link EnergySwirlLayer} shim still bounds on it, so it is re-declared here and the
 * import is swapped by the same Stonecutter rules that point the renderers at this package.
 *
 * <p>It lives here rather than under {@code server.entity} because this whole package is the one
 * that is compiled only on 1.21.2+; nothing below that version can see it, and nothing below that
 * version needs to.
 */
public interface PowerableMob {

	boolean isPowered();
}
