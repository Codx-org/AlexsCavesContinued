package com.github.alexmodguy.alexscaves.fabric.forge.client.event;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/**
 * Fabric stand-in for the screen-render family, of which only one member is needed.
 *
 * <p>This is the mod's one <b>published</b> client event: the cave book overrides
 * {@code renderBackground} and so skips the loader's own post, which upstream compensated for by
 * firing it by hand so third-party background hooks keep working. Nothing in this tree listens to
 * it.
 *
 * <p>That makes it the honest case for a stand-in that reaches no Fabric API at all. The loader
 * fires this to a bus other mods share; the mod's own bus on this loader is its own, so posting here
 * reaches this mod's listeners — none — and stops. Fabric has no cross-mod screen-background
 * callback to forward it to, so a dispatcher hook would be inventing an audience. Keeping the call
 * legal, and keeping the courtesy visible for the day one exists, is the whole job.
 */
public class ScreenEvent extends Event {

    private final Screen screen;

    public ScreenEvent(Screen screen) {
        this.screen = screen;
    }

    public Screen getScreen() {
        return screen;
    }

    /** Fired after a screen has drawn its background and before its widgets. */
    public static class BackgroundRendered extends ScreenEvent {

        private final GuiGraphics guiGraphics;

        public BackgroundRendered(Screen screen, GuiGraphics guiGraphics) {
            super(screen);
            this.guiGraphics = guiGraphics;
        }

        public GuiGraphics getGuiGraphics() {
            return guiGraphics;
        }
    }
}
