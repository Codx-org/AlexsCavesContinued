package com.github.alexmodguy.alexscaves.fabric.forge.client.gui.overlay;

import com.github.alexmodguy.alexscaves.server.misc.ACIdFactories;
import net.minecraft.resources.ResourceLocation;

/**
 * Fabric stand-in for the loader's constants naming vanilla's own HUD elements.
 *
 * <p>Only the five this tree compares against are declared: the crosshair, the experience bar, the
 * jump bar and the held-item name (all hidden while possessing another creature, and the experience
 * bar again for the config option that hides it outright), plus the health row, which the irradiated
 * hearts are drawn over. The loader has around twenty; the missing fifteen are elements nothing here
 * touches, and each one would oblige the dispatcher to find the moment in the frame it corresponds
 * to.
 *
 * <p>The ids match the loader's, so a third-party listener comparing against its own copy of these
 * names still lines up.
 */
public enum VanillaGuiOverlay implements NamedGuiOverlay {

    CROSSHAIR("crosshair"),
    JUMP_BAR("jump_bar"),
    EXPERIENCE_BAR("experience_bar"),
    ITEM_NAME("item_name"),
    PLAYER_HEALTH("player_health");

    private final ResourceLocation id;

    VanillaGuiOverlay(String path) {
        this.id = ACIdFactories.vanilla(path);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }
}
