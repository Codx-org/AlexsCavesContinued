package com.github.alexmodguy.alexscaves.mixin.client.citadel;

import com.github.alexmodguy.alexscaves.citadel.CitadelConstants;
import com.github.alexmodguy.alexscaves.citadel.CitadelEvent;
import com.github.alexmodguy.alexscaves.citadel.client.event.EventGetOutlineColor;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Citadel's {@code EventGetOutlineColor} hook: it lets a listener override the glow colour vanilla
 * takes from {@code Entity#getTeamColor()}.
 *
 * <p>This lived in the sibling {@link LevelRendererMixin} until 1.21.9, and the redirect itself has
 * never changed — but 1.21.9 moved the call it redirects <i>out of {@code LevelRenderer}</i>. The
 * deferred-submit rewrite reads an entity's outline colour while extracting its render state rather
 * than while drawing it, so the only call left in the client is in
 * {@code EntityRenderer#extractRenderState}. A {@code @Mixin} target cannot be varied inside a class,
 * which is why this is its own file: the target is the gate.
 *
 * <p>⚠️ This file must NEVER {@code import net.minecraft.client.renderer.entity.EntityRenderer} —
 * the {@code !mc2102-render-import-entity} replacement rewrites exactly that statement to this mod's
 * own render-compat shim on every &gt;=1.21.2 node, which would silently retarget {@code @Mixin} at a
 * class whose {@code extractRenderState} takes an {@code ACRenderState}. It compiles clean either way
 * and crashes at mixin-apply. Hence the fully-qualified target below, and the descriptor strings,
 * which are slash-separated and so cannot match the rule at all.
 */
//? if >=1.21.9 {
/*@Mixin(net.minecraft.client.renderer.entity.EntityRenderer.class)
*///?} else {
@Mixin(LevelRenderer.class)
//?}
public class OutlineColorMixin {

    @Redirect(
            //? if >=1.21.9 {
            /*method = "Lnet/minecraft/client/renderer/entity/EntityRenderer;extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
            *///?} elif >=1.21.2 {
            /*method = "Lnet/minecraft/client/renderer/LevelRenderer;renderEntities(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/Camera;Lnet/minecraft/client/DeltaTracker;Ljava/util/List;)V",
            *///?} elif >=1.21 {
            /*method = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            *///?} elif >=1.20.5 {
            /*method = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            *///?} else {
            method = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V",
            //?}
            remap = CitadelConstants.REMAPREFS,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I")
    )
    private int citadel_getTeamColor(Entity entity) {
        EventGetOutlineColor event = new EventGetOutlineColor(entity, entity.getTeamColor());
        EventGetOutlineColor.post(event);
        int color = entity.getTeamColor();
        if (event.getCitadelResult() == CitadelEvent.Result.ALLOW) {
            color = event.getColor();
        }
        return color;
    }
}
