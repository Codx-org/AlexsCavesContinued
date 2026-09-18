package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells {@code ACClientCompat#drawImmediate} that the target under it is a slot of the GUI item
 * atlas rather than the main render target, for as long as this method runs.
 *
 * <p>Needed only from 26.3, and only because 26.3 deleted the two statics that used to say so.
 * Through 26.2 {@code RenderSystem.outputColorTextureOverride} / {@code outputDepthTextureOverride}
 * were set here by vanilla itself and read by {@code PreparedRenderType} when it opened a pass;
 * 26.3 hands the pass down as a parameter instead, and a mod that opens its own has nothing left
 * to ask. {@code drawToSlot} also sets a scissor in ATLAS coordinates, so guessing the main target
 * is not a mis-draw but a hard {@code IllegalArgumentException} out of {@code enableScissor} —
 * every AC item with a special renderer crashes the client the first frame it is shown in a slot.
 *
 * @see com.github.alexmodguy.alexscaves.mixin.client.PictureInPictureTargetMixin
 */
@Mixin(GuiItemAtlas.class)
public class GuiItemAtlasTargetMixin {

    @Shadow
    @Final
    private GpuTextureView textureView;

    @Shadow
    @Final
    private GpuTextureView depthTextureView;

    @Inject(
            method = {"Lnet/minecraft/client/gui/render/GuiItemAtlas;drawToSlot(IIZLnet/minecraft/client/renderer/item/ItemStackRenderState;)V"},
            remap = true,
            at = @At(value = "HEAD")
    )
    private void ac_pushAtlasTarget(int slotX, int slotY, boolean clear, ItemStackRenderState state, CallbackInfo ci) {
        ACClientCompat.pushImmediateTarget(this.textureView, this.depthTextureView);
    }

    @Inject(
            method = {"Lnet/minecraft/client/gui/render/GuiItemAtlas;drawToSlot(IIZLnet/minecraft/client/renderer/item/ItemStackRenderState;)V"},
            remap = true,
            at = @At(value = "RETURN")
    )
    private void ac_popAtlasTarget(int slotX, int slotY, boolean clear, ItemStackRenderState state, CallbackInfo ci) {
        ACClientCompat.popImmediateTarget();
    }
}
