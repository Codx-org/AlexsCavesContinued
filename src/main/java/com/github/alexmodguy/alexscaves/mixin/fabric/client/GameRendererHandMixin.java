package com.github.alexmodguy.alexscaves.mixin.fabric.client;

import com.github.alexmodguy.alexscaves.fabric.forge.client.event.RenderHandEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric's producer for {@code RenderHandEvent}, whose one listener hides the player's own arm while
 * they are looking out of something else's eyes.
 *
 * <p>HEAD of {@code GameRenderer#renderItemInHand} with {@code cancellable = true} is the exact
 * equivalent of Forge's hook, which fires from inside {@code ItemInHandRenderer#renderHandsWithItems}
 * and skips the render when cancelled — one method further in, but with no observable difference,
 * since everything between the two is setup for the render being skipped.
 *
 * <p>The descriptor bands are copied from the {@code @At} targets in the loader-neutral
 * {@link com.github.alexmodguy.alexscaves.mixin.client.GameRendererMixin}, where they are already
 * verified on all 58 nodes: the pose stack left the signature at 1.20.5 and the camera followed it
 * at 1.21.6. There is deliberately no fourth arm for 26, because the tree's own
 * {@code !mc261-renderiteminhand} replacement rule rewrites that descriptor wherever it is spelled —
 * including here.
 *
 * <p>⚠️ The handler mirrors <em>none</em> of the target's arguments, and that is what keeps one
 * handler legal against all four shapes. An {@code @Inject} handler must mirror the target's whole
 * argument list or omit it entirely; mirroring it costs a Java arm per band, and the arm for 26 is
 * exactly the one that was missing when this file first shipped — it compiled on every node and
 * would have failed at mixin-apply on the four Fabric 26.x ones. The event carries no fields (see
 * its javadoc), so nothing here ever wanted a parameter.
 */
@Mixin(GameRenderer.class)
public class GameRendererHandMixin {

    @Inject(
            //? if >=1.21.6 {
            /*method = {"Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(FZLorg/joml/Matrix4f;)V"},
            *///?} elif >=1.20.5 {
            /*method = {"Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/Camera;FLorg/joml/Matrix4f;)V"},
            *///?} else {
            method = {"Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Camera;F)V"},
            //?}
            remap = true, cancellable = true, at = @At(value = "HEAD")
    )
    public void ac_fabricRenderHand(CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new RenderHandEvent())) {
            ci.cancel();
        }
    }
}
