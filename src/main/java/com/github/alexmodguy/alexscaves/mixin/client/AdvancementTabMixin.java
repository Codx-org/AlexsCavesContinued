package com.github.alexmodguy.alexscaves.mixin.client;


import com.github.alexmodguy.alexscaves.client.ACClientPlatform;
import com.github.alexmodguy.alexscaves.client.gui.ACAdvancementTabs;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import com.github.alexmodguy.alexscaves.client.ACClientCompat;

@Mixin(AdvancementTab.class)
public class AdvancementTabMixin {

    @Shadow
    private boolean centered;

    @Shadow
    private double scrollX;

    @Shadow
    private double scrollY;

    @Shadow
    private int maxX;

    @Shadow
    private int minX;

    @Shadow
    private int maxY;

    @Shadow
    private int minY;

    @Shadow
    @Final
    private DisplayInfo display;

    @Shadow
    @Final
    private AdvancementWidget root;

    // Only ever iterated by value, so what the map is keyed on does not matter here — which is
    // just as well, because 1.20.2 rekeyed it from Advancement to AdvancementHolder.
    @Shadow
    @Final
    //? if >=1.20.2
    /*private Map<net.minecraft.advancements.AdvancementHolder, AdvancementWidget> widgets;*/
    //? if <1.20.2
    private Map<net.minecraft.advancements.Advancement, AdvancementWidget> widgets;


    @Inject(
            method = {"Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;drawContents(Lnet/minecraft/client/gui/GuiGraphics;II)V"},
            remap = true,
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private void ac_drawContents(GuiGraphics guiGraphics, int topX, int topY, CallbackInfo ci) {
        if (ACAdvancementTabs.isAlexsCavesWidget(ACClientPlatform.advancementId(root))) {
            ci.cancel();
            guiGraphics.enableScissor(topX, topY, topX + 234, topY + 113);
            ACClientCompat.pushPose(guiGraphics);
            ACClientCompat.translate(guiGraphics, (float) topX, (float) topY);
            if (!this.centered) {
                this.scrollX = (double) (117 - (this.maxX + this.minX) / 2);
                this.scrollY = (double) (56 - (this.maxY + this.minY) / 2);
                this.centered = true;
            }
            int width = this.maxX - this.minX;
            int height = this.maxY - this.minY;
            int i = Mth.floor(this.scrollX);
            int j = Mth.floor(this.scrollY);
            ACAdvancementTabs.setDimensions(width, height);
            ACAdvancementTabs.renderTabBackground(guiGraphics, topX, topY, this.display, this.scrollX, this.scrollY);
            this.root.drawConnectivity(guiGraphics, i, j, true);
            this.root.drawConnectivity(guiGraphics, i, j, false);
            this.root.draw(guiGraphics, i, j);
            ACClientCompat.popPose(guiGraphics);
            guiGraphics.disableScissor();
        }
    }

    // 26.2 moved the hover search out of the tooltip pass. The tab is ticked with the mouse position
    // (tick(int, int)) and remembers what it found in a new `hovered` field; extractTooltips no longer
    // receives the mouse at all — it takes the two window offsets only — and just draws whatever the
    // tick left behind. So the scan below has nowhere to run and nothing to run on, and the arm reads
    // the field instead.
    //
    // Vanilla never clears `hovered`; it lets the accompanying `fade` fall to zero so a stale widget
    // draws at alpha 0. Reading it unguarded matches this mixin's own behaviour exactly, because the
    // old code likewise only ever *set* a hover type and never cleared one — a mouse that leaves a
    // widget re-sets the value it had rather than resetting it, on both sides of the split.
    //? if >=26.2 {
    /*@Shadow
    private AdvancementWidget hovered;

    @Inject(
            method = {"Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;drawTooltips(Lnet/minecraft/client/gui/GuiGraphics;II)V"},
            remap = true,
            at = @At(value = "HEAD")
    )
    private void ac_drawTooltips(GuiGraphics guiGraphics, int topX, int topY, CallbackInfo ci) {
        if (ACAdvancementTabs.isAlexsCavesWidget(ACClientPlatform.advancementId(root))) {
            if (this.hovered != null && ACAdvancementTabs.Type.isTreeNodeUnlocked(this.hovered)) {
                ACAdvancementTabs.setHoverType(ACAdvancementTabs.Type.forAdvancement(this.hovered));
            }
        }
    }
    *///?} else {
    @Inject(
            method = {"Lnet/minecraft/client/gui/screens/advancements/AdvancementTab;drawTooltips(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"},
            remap = true,
            at = @At(value = "HEAD")
    )
    private void ac_drawTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int topX, int topY, CallbackInfo ci) {
        if (ACAdvancementTabs.isAlexsCavesWidget(ACClientPlatform.advancementId(root))) {
            int i = Mth.floor(this.scrollX);
            int j = Mth.floor(this.scrollY);
            ACAdvancementTabs.Type hoverType = null;
            if (mouseX > 0 && mouseX < 234 && mouseY > 0 && mouseY < 113) {
                for (AdvancementWidget advancementwidget : this.widgets.values()) {
                    if (advancementwidget.isMouseOver(i, j, mouseX, mouseY)) {
                        if (ACAdvancementTabs.Type.isTreeNodeUnlocked(advancementwidget)) {
                            hoverType = ACAdvancementTabs.Type.forAdvancement(advancementwidget);
                        }
                    }
                }
            }
            if (hoverType != null) {
                ACAdvancementTabs.setHoverType(hoverType);
            }
        }
    }
    //?}
}
