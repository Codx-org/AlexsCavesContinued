package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The picture-in-picture half of {@link GuiItemAtlasTargetMixin} — the second of the two places
 * 26.2 set {@code RenderSystem.outputColorTextureOverride} that this mod can end up drawing
 * inside. The window is deliberately the one between the texture being (re)created and the
 * contents being submitted: after {@code renderToTexture} returns, vanilla opens its own pass on
 * the same texture, and a mod pass nested inside that one would be refused anyway.
 *
 * <p>Pushing at HEAD would be wrong for two separate reasons, not one: {@code prepare} returns
 * early down the blit path, so the pop would never run, and the two views are only valid once
 * {@code prepareTexturesAndProjection} has sized them for this state.
 */
@Mixin(PictureInPictureRenderer.class)
public class PictureInPictureTargetMixin {

    @Shadow
    private GpuTextureView textureView;

    @Shadow
    private GpuTextureView depthTextureView;

    @Inject(
            method = {"Lnet/minecraft/client/gui/render/pip/PictureInPictureRenderer;prepare(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;I)V"},
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/pip/PictureInPictureRenderer;prepareTexturesAndProjection(ZII)V",
                    shift = At.Shift.AFTER
            )
    )
    private void ac_pushPipTarget(PictureInPictureRenderState state, GuiRenderState guiState,
                                  FeatureRenderDispatcher dispatcher, int scale, CallbackInfo ci) {
        ACClientCompat.pushImmediateTarget(this.textureView, this.depthTextureView);
    }

    @Inject(
            method = {"Lnet/minecraft/client/gui/render/pip/PictureInPictureRenderer;prepare(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;I)V"},
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/pip/PictureInPictureRenderer;renderToTexture(Lnet/minecraft/client/renderer/state/gui/pip/PictureInPictureRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void ac_popPipTarget(PictureInPictureRenderState state, GuiRenderState guiState,
                                 FeatureRenderDispatcher dispatcher, int scale, CallbackInfo ci) {
        ACClientCompat.popImmediateTarget();
    }
}
