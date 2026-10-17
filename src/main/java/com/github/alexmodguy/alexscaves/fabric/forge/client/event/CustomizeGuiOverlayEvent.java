package com.github.alexmodguy.alexscaves.fabric.forge.client.event;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Cancelable;
import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;

/**
 * Fabric stand-in for the loader's "adjust one HUD element as it is drawn" family.
 *
 * <p>Only {@link BossEventProgress} is modelled, because it is the only member of the family this
 * mod uses — and unlike its neighbour {@link RenderGuiOverlayEvent} it is live on every node, not
 * just below 1.20.5: this mod replaces the boss bars of its own bosses with custom art, on all three
 * loaders and all 58 versions.
 *
 * <p>Cancelling suppresses vanilla's bar for that one boss while leaving the rest of the list alone,
 * which is why the increment has to be writable too — a listener that draws taller art has to tell
 * the caller how far down the next bar starts. Both are read and written here.
 */
public class CustomizeGuiOverlayEvent extends Event {

    private final GuiGraphics guiGraphics;
    private final float partialTick;

    public CustomizeGuiOverlayEvent(GuiGraphics guiGraphics, float partialTick) {
        this.guiGraphics = guiGraphics;
        this.partialTick = partialTick;
    }

    public GuiGraphics getGuiGraphics() {
        return guiGraphics;
    }

    public float getPartialTick() {
        return partialTick;
    }

    /**
     * One boss bar, at the position the caller has reached down the list.
     *
     * <p>The dispatcher fires this per bar from a mixin on vanilla's boss-overlay draw, with
     * {@code increment} set to the row height vanilla would have used.
     */
    @Cancelable
    public static class BossEventProgress extends CustomizeGuiOverlayEvent {

        private final LerpingBossEvent bossEvent;
        private final int x;
        private final int y;
        private int increment;

        public BossEventProgress(GuiGraphics guiGraphics, float partialTick, LerpingBossEvent bossEvent,
                                 int x, int y, int increment) {
            super(guiGraphics, partialTick);
            this.bossEvent = bossEvent;
            this.x = x;
            this.y = y;
            this.increment = increment;
        }

        public LerpingBossEvent getBossEvent() {
            return bossEvent;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        /** How far down the next bar starts. Written by a listener that drew something taller. */
        public int getIncrement() {
            return increment;
        }

        public void setIncrement(int increment) {
            this.increment = increment;
        }
    }
}
