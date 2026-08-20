package com.github.alexmodguy.alexscaves.mixin.fabric.client;

import com.github.alexmodguy.alexscaves.fabric.forge.client.event.RenderBlockScreenEffectEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric producer for {@link RenderBlockScreenEffectEvent} — the full-screen water overlay. A
 * submarine keeps the water out, so this mod cancels it for a player riding one.
 *
 * <p>Only the WATER overlay is produced. The event has three types, but nothing in this mod listens
 * for FIRE or BLOCK, and a producer for an overlay no consumer asks about would be dead weight on
 * every frame; add the other two here if a consumer ever appears.
 *
 * <p>The target is private static on every node and no node declares a second overload of the name,
 * so a name-only selector is enough inside each arm. Three bands: it gained a
 * {@code MultiBufferSource} at 1.21.4 and was renamed {@code submitWater} with a
 * {@code SubmitNodeCollector} at 26.2. A mixin handler's static-ness must match its target's, hence
 * the static handlers, and cancelling at HEAD is what skips the draw.
 */
@Mixin(net.minecraft.client.renderer.ScreenEffectRenderer.class)
public class ScreenEffectWaterMixin {

    @org.spongepowered.asm.mixin.Unique
    private static boolean ac_fabricCancelWater() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        return MinecraftForge.EVENT_BUS.post(new RenderBlockScreenEffectEvent(player, RenderBlockScreenEffectEvent.OverlayType.WATER));
    }

    //? if >=26.2 {
    /*@Inject(method = "submitWater", remap = true, cancellable = true, at = @At(value = "HEAD"))
    private static void ac_fabricRenderWater(Minecraft minecraft, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, CallbackInfo ci) {
        if (ac_fabricCancelWater()) {
            ci.cancel();
        }
    }
    *///?} elif >=1.21.4 {
    /*@Inject(method = "renderWater", remap = true, cancellable = true, at = @At(value = "HEAD"))
    private static void ac_fabricRenderWater(Minecraft minecraft, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource bufferSource, CallbackInfo ci) {
        if (ac_fabricCancelWater()) {
            ci.cancel();
        }
    }
    *///?} else {
    @Inject(method = "renderWater", remap = true, cancellable = true, at = @At(value = "HEAD"))
    private static void ac_fabricRenderWater(Minecraft minecraft, com.mojang.blaze3d.vertex.PoseStack poseStack, CallbackInfo ci) {
        if (ac_fabricCancelWater()) {
            ci.cancel();
        }
    }
    //?}
}
