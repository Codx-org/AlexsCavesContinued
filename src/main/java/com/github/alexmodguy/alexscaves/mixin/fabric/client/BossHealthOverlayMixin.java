package com.github.alexmodguy.alexscaves.mixin.fabric.client;

import com.github.alexmodguy.alexscaves.fabric.forge.client.event.CustomizeGuiOverlayEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Fabric's producer for {@code CustomizeGuiOverlayEvent.BossEventProgress}, which is how this mod
 * replaces the vanilla boss bar with its own art for the bosses that ask for it.
 *
 * <p>Forge patches the event into {@code BossHealthOverlay}'s per-bar loop; this reproduces the same
 * three effects from the outside, in the order the loop performs them:
 *
 * <ol>
 *   <li>the bar draw is wrapped, which is where the event is posted (it is the first thing the loop
 *       does with the x/y the event has to carry) and where a cancel skips the vanilla art;</li>
 *   <li>the boss name draw is wrapped by the same verdict, because Forge's cancel skips the whole
 *       per-bar render and this mod draws its own outlined name;</li>
 *   <li>the vertical advance is taken from {@code event.getIncrement()} rather than the hard-coded
 *       {@code 10 + font.lineHeight}, which is what lets a listener claim a taller bar — this one
 *       adds 7.</li>
 * </ol>
 *
 * <p>The loop is offset-for-offset the same shape on every node from 1.20.1 to 26.2; only three
 * names move, and all three move together at 26.1, where the GUI draw chain became
 * {@code extract*}. The graphics-context type named inside every descriptor below is renamed by the
 * tree's own {@code !mc261-guigraphics} rule — which reaches slash-form descriptor strings exactly
 * as it reaches Java types, and which is why that type is deliberately never spelled in this
 * comment — so only the method names are gated here. The one extra band is the name draw, which
 * stopped returning an {@code int} at 1.21.6; below that its result is discarded with a
 * {@code pop}, which is precisely the case {@code @WrapWithCondition} is allowed to skip.
 *
 * <p>⚠️ The {@code 10} the third injection modifies is the loop's own {@code j += 10 +
 * font.lineHeight}. {@code Font#lineHeight} is a compile-time constant, so the sum leaves no call
 * to intercept and no second constant is safe to key on — but there is exactly one {@code bipush
 * 10} in the method on all eight bands (checked in the bytecode), and subtracting the same
 * {@code lineHeight} vanilla is about to add back makes the total exactly the increment. Do not
 * hard-code the 9.
 *
 * <p>The event's {@code partialTick} is deliberately {@code 0}: the loop is not handed one, the
 * only listener in the tree never reads it, and inventing a per-band way to fetch it would buy
 * nothing.
 */
@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {

    @Unique
    private boolean ac_fabricBossCanceled;

    @Unique
    private int ac_fabricBossIncrement;

    @com.llamalad7.mixinextras.injector.WrapWithCondition(
            //? if >=26.1 {
            /*method = {"Lnet/minecraft/client/gui/components/BossHealthOverlay;extractRenderState(Lnet/minecraft/client/gui/GuiGraphics;)V"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;extractBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;)V")
            *///?} else {
            method = {"Lnet/minecraft/client/gui/components/BossHealthOverlay;render(Lnet/minecraft/client/gui/GuiGraphics;)V"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;drawBar(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/BossEvent;)V")
            //?}
    )
    private boolean ac_fabricBossBar(BossHealthOverlay overlay, GuiGraphics guiGraphics, int x, int y, BossEvent bossEvent) {
        Font font = Minecraft.getInstance().font;
        CustomizeGuiOverlayEvent.BossEventProgress event = new CustomizeGuiOverlayEvent.BossEventProgress(
                guiGraphics, 0.0F, (LerpingBossEvent) bossEvent, x, y, 10 + font.lineHeight);
        ac_fabricBossCanceled = MinecraftForge.EVENT_BUS.post(event);
        ac_fabricBossIncrement = event.getIncrement();
        return !ac_fabricBossCanceled;
    }

    @com.llamalad7.mixinextras.injector.WrapWithCondition(
            //? if >=26.1 {
            /*method = {"Lnet/minecraft/client/gui/components/BossHealthOverlay;extractRenderState(Lnet/minecraft/client/gui/GuiGraphics;)V"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V")
            *///?} elif >=1.21.6 {
            /*method = {"Lnet/minecraft/client/gui/components/BossHealthOverlay;render(Lnet/minecraft/client/gui/GuiGraphics;)V"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V")
            *///?} else {
            method = {"Lnet/minecraft/client/gui/components/BossHealthOverlay;render(Lnet/minecraft/client/gui/GuiGraphics;)V"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I")
            //?}
    )
    private boolean ac_fabricBossName(GuiGraphics guiGraphics, Font font, Component name, int x, int y, int color) {
        return !ac_fabricBossCanceled;
    }

    @ModifyConstant(
            //? if >=26.1 {
            /*method = {"Lnet/minecraft/client/gui/components/BossHealthOverlay;extractRenderState(Lnet/minecraft/client/gui/GuiGraphics;)V"},
            *///?} else {
            method = {"Lnet/minecraft/client/gui/components/BossHealthOverlay;render(Lnet/minecraft/client/gui/GuiGraphics;)V"},
            //?}
            constant = @Constant(intValue = 10)
    )
    private int ac_fabricBossAdvance(int original) {
        return ac_fabricBossIncrement - Minecraft.getInstance().font.lineHeight;
    }
}
