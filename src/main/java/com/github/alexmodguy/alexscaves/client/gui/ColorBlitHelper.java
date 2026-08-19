package com.github.alexmodguy.alexscaves.client.gui;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class ColorBlitHelper {

    public static void blitWithColor(GuiGraphics guiGraphics, ResourceLocation p_283377_, int p_281970_, int p_282111_, int p_283134_, int p_282778_, int p_281478_, int p_281821_, float r, float g, float b, float a) {
        blitWithColor(guiGraphics, p_283377_, p_281970_, p_282111_, 0, (float) p_283134_, (float) p_282778_, p_281478_, p_281821_, 256, 256, r, g, b, a);
    }

    public static void blitWithColor(GuiGraphics guiGraphics, ResourceLocation p_283573_, int p_283574_, int p_283670_, int p_283545_, float p_283029_, float p_283061_, int p_282845_, int p_282558_, int p_282832_, int p_281851_, float r, float g, float b, float a) {
        blitWithColor(guiGraphics, p_283573_, p_283574_, p_283574_ + p_282845_, p_283670_, p_283670_ + p_282558_, p_283545_, p_282845_, p_282558_, p_283029_, p_283061_, p_282832_, p_281851_, r, g, b, a);
    }

    public static void blitWithColor(GuiGraphics guiGraphics, ResourceLocation p_282034_, int p_283671_, int p_282377_, int p_282058_, int p_281939_, float p_282285_, float p_283199_, int p_282186_, int p_282322_, int p_282481_, int p_281887_, float r, float g, float b, float a) {
        blitWithColor(guiGraphics, p_282034_, p_283671_, p_283671_ + p_282058_, p_282377_, p_282377_ + p_281939_, 0, p_282186_, p_282322_, p_282285_, p_283199_, p_282481_, p_281887_, r, g, b, a);
    }

    public static void blitWithColor(GuiGraphics guiGraphics, ResourceLocation p_283272_, int p_283605_, int p_281879_, float p_282809_, float p_282942_, int p_281922_, int p_282385_, int p_282596_, int p_281699_, float r, float g, float b, float a) {
        blitWithColor(guiGraphics, p_283272_, p_283605_, p_281879_, p_281922_, p_282385_, p_282809_, p_282942_, p_281922_, p_282385_, p_282596_, p_281699_, r, g, b, a);
    }

    /**
     * Every overload above funnels through here: a quad from {@code startX,startY} to
     * {@code endX,endY}, showing the {@code regionWidth}×{@code regionHeight} patch of the texture at
     * {@code u,v}, tinted.
     *
     * <p>From 1.21.6 that is exactly what {@code GuiGraphics#blit}'s tinted overload does — same
     * arguments, same {@code position_tex_color} shader, same translucent blend as the immediate draw
     * below — so the hand-rolled quad gives way to it rather than to a custom {@code
     * GuiElementRenderState}. The GUI is batched behind render states by then, and a mod-built state
     * would have to reach a submit method that only NeoForge exposes.
     *
     * <p>The z argument goes with it. A GUI draw no longer carries one; depth comes from submission
     * order, and every caller here passed {@code 0} anyway.
     */
    private static void blitWithColor(GuiGraphics guiGraphics, ResourceLocation texture, int startX, int endX, int startY, int endY, int zLevel, int regionWidth, int regionHeight, float u, float v, int texWidth, int texHeight, float r, float g, float b, float a) {
        //? if >=1.21.6 {
        /*guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture,
                startX, startY, u, v, endX - startX, endY - startY, regionWidth, regionHeight,
                texWidth, texHeight, net.minecraft.util.ARGB.colorFromFloat(a, r, g, b));
        *///?} else {
        blitWithColor(guiGraphics, texture, startX, endX, startY, endY, zLevel, (u + 0.0F) / (float) texWidth, (u + (float) regionWidth) / (float) texWidth, (v + 0.0F) / (float) texHeight, (v + (float) regionHeight) / (float) texHeight, r, g, b, a);
        //?}
    }

    // The hand-rolled quad itself, and the one thing about it that could not survive: a GUI pose is a
    // Matrix3x2fStack from 1.21.6, with no last() and no third axis, and there is no immediate draw
    // left to feed anyway. Unreachable above 1.21.6 — the funnel goes to GuiGraphics#blit there — so
    // the method goes away rather than being kept alive against a stack that cannot describe it.
    //? if >=1.21.6 {
    /*// gone — see above.
    *///?} else {
    private static void blitWithColor(GuiGraphics guiGraphics, ResourceLocation texture, int startX, int endX, int startY, int endY, int zLevel, float u0, float u1, float v0, float v1, float r, float g, float b, float a) {
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        BufferBuilder bufferbuilder = ACClientCompat.beginImmediate(ACClientCompat.ImmediateDraw.POSITION_TEX_COLOR_QUADS, texture);
        bufferbuilder.vertex(matrix4f, (float) startX, (float) startY, (float) zLevel).color(r, g, b, a).uv(u0, v0).endVertex();
        bufferbuilder.vertex(matrix4f, (float) startX, (float) endY, (float) zLevel).color(r, g, b, a).uv(u0, v1).endVertex();
        bufferbuilder.vertex(matrix4f, (float) endX, (float) endY, (float) zLevel).color(r, g, b, a).uv(u1, v1).endVertex();
        bufferbuilder.vertex(matrix4f, (float) endX, (float) startY, (float) zLevel).color(r, g, b, a).uv(u1, v0).endVertex();
        ACClientCompat.drawImmediate(ACClientCompat.ImmediateDraw.POSITION_TEX_COLOR_QUADS, bufferbuilder, texture);
    }
    //?}
}
