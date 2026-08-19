package com.github.alexmodguy.alexscaves.client.render.item.tooltip;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;

import com.github.alexmodguy.alexscaves.server.item.tooltip.SackOfSatingTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ClientSackOfSatingTooltip implements ClientTooltipComponent {

    private static final ResourceLocation GUI_ICONS_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/icons.png");
    private final SackOfSatingTooltip tooltipComponent;

    public ClientSackOfSatingTooltip(SackOfSatingTooltip tooltipComponent) {
        this.tooltipComponent = tooltipComponent;
    }

    // 1.21.2 reshaped both of ClientTooltipComponent's measuring/drawing hooks: getHeight gained the
    // Font (a component may lay itself out against it) and renderImage gained the tooltip's final
    // width and height. Neither number is used here — the sack's row is a fixed 11px of shank icons
    // drawn from x — but renderImage carries no @Override upstream, so a stale signature would
    // silently stop drawing rather than fail to compile. Both headers therefore move together, with
    // the body left in one place below.
    //? if >=1.21.2 {
    /*@Override
    public int getHeight(Font font) {
        return tooltipComponent.getHungerValue() == 0 ? 0 : 11;
    }

    @Override
    public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics guiGraphics) {
        this.drawShanks(font, x, y, guiGraphics);
    }
    *///?} else {
    @Override
    public int getHeight() {
        return tooltipComponent.getHungerValue() == 0 ? 0 : 11;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        this.drawShanks(font, x, y, guiGraphics);
    }
    //?}

    @Override
    public int getWidth(Font font) {
        return isTruncated() ? font.width(getHungerValueMultiplierText()) + 9 : Mth.ceil(tooltipComponent.getHungerValue() / 2.0D) * 9;
    }

    private void drawShanks(Font font, int x, int y, GuiGraphics guiGraphics) {
        int hungerValue = tooltipComponent.getHungerValue();
        int shanks = (int) Math.ceil(hungerValue / 2.0D);
        if (isTruncated()) {
            ACClientCompat.blit(guiGraphics, GUI_ICONS_LOCATION, x, y, 16, 27, 9, 9);
            ACClientCompat.blit(guiGraphics, GUI_ICONS_LOCATION, x, y, 52, 27, 9, 9);
            // 0XFFA8A8A8, not upstream's 0XA8A8A8: the alpha byte is part of the colour a glyph is
            // tinted with, and vanilla stopped filling in a missing one somewhere before 1.21.5 — an
            // alpha of 0 draws nothing at all there, and from 1.21.6 drawString returns immediately on
            // it. On the versions that did fill it in this is the very value they produced.
            //? if >=1.21.6 {
            /*// The GUI is collected into render states before anything is drawn, so there is no
            // MultiBufferSource to borrow mid-frame; drawString is the submit path, and a plain
            // shadowed string in the GUI's own transform is all this ever was.
            guiGraphics.drawString(font, getHungerValueMultiplierText(), x + 10, y + 1, 0XFFA8A8A8, true);
            *///?} else {
            ACClientCompat.drawSpecial(guiGraphics, bufferSource -> font.drawInBatch(getHungerValueMultiplierText(), (float)x + 10, (float)y + 1, 0XFFA8A8A8, true, guiGraphics.pose().last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, 15728880));
            //?}
        } else {
            for (int i = 0; i < shanks; i++) {
                boolean halfShank = i == 0 && hungerValue % 2 == 1;
                //background outline
                ACClientCompat.blit(guiGraphics, GUI_ICONS_LOCATION, x + i * 9, y, 16, 27, 9, 9);
                ACClientCompat.blit(guiGraphics, GUI_ICONS_LOCATION, x + i * 9, y, halfShank ? 61 : 52, 27, 9, 9);
            }
        }
    }

    private boolean isTruncated() {
        return tooltipComponent.getHungerValue() >= 30;
    }

    private String getHungerValueMultiplierText(){
        int hungerValue = tooltipComponent.getHungerValue();
        double d = (hungerValue / 2.0D);
        String drawText = "x";
        if(d % 1.0D == 0.0D){
            drawText += (int)d;
        }else{
            drawText += d;
        }
        return drawText;
    }
}