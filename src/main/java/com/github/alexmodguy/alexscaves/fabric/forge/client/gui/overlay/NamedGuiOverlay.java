package com.github.alexmodguy.alexscaves.fabric.forge.client.gui.overlay;

import net.minecraft.resources.ResourceLocation;

/**
 * Fabric stand-in for the loader's handle on one HUD element.
 *
 * <p>An interface here rather than the loader's record, because the only thing this tree ever asks
 * of one is its id — {@code ClientEvents} compares the id it is given against the five constants in
 * {@link VanillaGuiOverlay} and cancels or draws accordingly. That makes {@link VanillaGuiOverlay}
 * able to be the enum it reads as, and leaves room for the dispatcher to name an element this mod
 * has no constant for without adding one.
 */
public interface NamedGuiOverlay {

    ResourceLocation id();
}
