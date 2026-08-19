package com.github.alexmodguy.alexscaves.fabric.forge.client.event;

import com.github.alexmodguy.alexscaves.fabric.forge.client.gui.overlay.NamedGuiOverlay;
import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Cancelable;
import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Fabric stand-in for the per-HUD-element render pair.
 *
 * <p>Received only, and only on nodes below 1.20.5 — above that the mod reaches the HUD through two
 * other paths, one per loader, and both of those arms are gated out on this one. The {@code Pre}
 * handler cancels the four elements that must not show while possessing another creature; the
 * {@code Post} handler hangs the riding meter off the crosshair and the irradiated hearts off the
 * health row, which is why it carries the graphics rather than just the id.
 *
 * <p>The dispatcher fires one {@code Pre}/{@code Post} pair per element from a {@code Gui} mixin,
 * naming each through {@link com.github.alexmodguy.alexscaves.fabric.forge.client.gui.overlay.VanillaGuiOverlay}
 * — see that enum for why only five of vanilla's elements are addressable.
 */
public class RenderGuiOverlayEvent extends Event {

    private final NamedGuiOverlay overlay;
    private final GuiGraphics guiGraphics;
    private final float partialTick;

    public RenderGuiOverlayEvent(NamedGuiOverlay overlay, GuiGraphics guiGraphics, float partialTick) {
        this.overlay = overlay;
        this.guiGraphics = guiGraphics;
        this.partialTick = partialTick;
    }

    public NamedGuiOverlay getOverlay() {
        return overlay;
    }

    public GuiGraphics getGuiGraphics() {
        return guiGraphics;
    }

    public float getPartialTick() {
        return partialTick;
    }

    /** Cancelling this suppresses the element; the dispatcher must skip the draw. */
    @Cancelable
    public static class Pre extends RenderGuiOverlayEvent {

        public Pre(NamedGuiOverlay overlay, GuiGraphics guiGraphics, float partialTick) {
            super(overlay, guiGraphics, partialTick);
        }
    }

    /** Fired after the element is drawn, and only when {@link Pre} was not cancelled. */
    public static class Post extends RenderGuiOverlayEvent {

        public Post(NamedGuiOverlay overlay, GuiGraphics guiGraphics, float partialTick) {
            super(overlay, guiGraphics, partialTick);
        }
    }
}
