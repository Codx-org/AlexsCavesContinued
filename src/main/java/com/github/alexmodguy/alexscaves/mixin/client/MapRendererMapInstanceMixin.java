package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.client.event.ClientEvents;
import com.mojang.blaze3d.vertex.PoseStack;
//? if <1.21.2 {
import net.minecraft.client.gui.MapRenderer;
//?}
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands {@link ClientEvents} the pose stack, buffer source and light the vanilla map is being drawn
 * with, so this mod's own map overlays can draw into the same batch.
 *
 * <p>1.21.2 moved map rendering off the per-map {@code MapRenderer.MapInstance} object in
 * {@code net.minecraft.client.gui} and onto a stateless {@code net.minecraft.client.renderer
 * .MapRenderer} that takes an extracted {@code MapRenderState}. The three values this captures are
 * the same three arguments in the same order, one position later, so only the target moves.
 */
//? if >=1.21.2 {
/*@Mixin(net.minecraft.client.renderer.MapRenderer.class)
*///?} else {
@Mixin(MapRenderer.MapInstance.class)
//?}
public class MapRendererMapInstanceMixin {

    //? if >=1.21.2 {
    /*@Inject(
            method = {"Lnet/minecraft/client/renderer/MapRenderer;render(Lnet/minecraft/client/renderer/state/MapRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ZI)V"},
            remap = true,
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private void ac_render(net.minecraft.client.renderer.state.MapRenderState mapRenderState, PoseStack poseStack, MultiBufferSource multiBufferSource, boolean inFrame, int packedLighting, CallbackInfo ci) {
        ClientEvents.lastVanillaMapPoseStack = poseStack;
        ClientEvents.lastVanillaMapRenderBuffer = multiBufferSource;
        ClientEvents.lastVanillaMapRenderPackedLight = packedLighting;
    }
    *///?} else {
    @Inject(
            method = {"Lnet/minecraft/client/gui/MapRenderer$MapInstance;draw(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ZI)V"},
            remap = true,
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private void ac_render(PoseStack poseStack, MultiBufferSource multiBufferSource, boolean inFrame, int packedLighting, CallbackInfo ci) {
        ClientEvents.lastVanillaMapPoseStack = poseStack;
        ClientEvents.lastVanillaMapRenderBuffer = multiBufferSource;
        ClientEvents.lastVanillaMapRenderPackedLight = packedLighting;
    }
    //?}
}
