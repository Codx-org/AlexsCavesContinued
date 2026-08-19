package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.client.render.item.ACItemDisplayContexts;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Publishes the display context of the item being submitted, which 26 stopped passing to a special
 * renderer.
 *
 * <p>See {@link ACItemDisplayContexts} for why this mod needs it when vanilla does not. This is the
 * right anchor because it is the only one: {@code ItemStackRenderState} keeps the context in a field
 * that its own {@code submit} reads, the per-layer submit that calls
 * {@code SpecialModelRenderer#submit} is private to the class, and nothing else calls it.
 *
 * <p>26 and up only — {@code ModPlatformPlugin} excludes this file from the compile below it and
 * prunes the entry back out of the mixin config, since every earlier version still carries the
 * context as a parameter and has nothing for this to do.
 */
@Mixin(ItemStackRenderState.class)
public class ItemStackRenderStateMixin {

    @Shadow
    ItemDisplayContext displayContext;

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V", at = @At("HEAD"))
    private void ac_publishDisplayContext(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay, int outlineColor, CallbackInfo ci) {
        ACItemDisplayContexts.set(this.displayContext);
    }

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V", at = @At("RETURN"))
    private void ac_clearDisplayContext(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay, int outlineColor, CallbackInfo ci) {
        ACItemDisplayContexts.set(ItemDisplayContext.NONE);
    }
}
