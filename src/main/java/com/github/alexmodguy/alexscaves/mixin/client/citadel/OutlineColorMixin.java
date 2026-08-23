package com.github.alexmodguy.alexscaves.mixin.client.citadel;

import com.github.alexmodguy.alexscaves.citadel.CitadelConstants;
import com.github.alexmodguy.alexscaves.citadel.CitadelEvent;
import com.github.alexmodguy.alexscaves.citadel.client.event.EventGetOutlineColor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Citadel's {@code EventGetOutlineColor} hook: it lets a listener override the glow colour vanilla
 * takes from {@code Entity#getTeamColor()}.
 *
 * <p>This lived in the sibling {@link LevelRendererMixin} until 1.21.9, and the injection itself has
 * never changed — but 1.21.9 moved the call it wraps <i>out of {@code LevelRenderer}</i>. The
 * deferred-submit rewrite reads an entity's outline colour while extracting its render state rather
 * than while drawing it, so the only call left in the client is in
 * {@code EntityRenderer#extractRenderState}. A {@code @Mixin} target cannot be varied inside a class,
 * which is why this is its own file: the target is the gate.
 *
 * <p>⚠⚠ This is a {@code @WrapOperation}, deliberately, and it was a {@code @Redirect} once. Every
 * mod that vendors Citadel ships this same hook on this same instruction — <b>AlexsMobsContinued
 * does</b>, as {@code alexsmobs.mixins.json:client.LevelRendererMixin} — and {@code @Redirect} is
 * EXCLUSIVE: Mixin keeps whichever applicator ran first, logs
 * {@code "@Redirect conflict. Skipping …"} for the other, and then the loser's own
 * {@code "defaultRequire": 1} throws
 * {@code InjectionError: Critical injection failure: Redirector citadel_getTeamColor … (0/1)
 * succeeded}. That fails the whole {@code EntityRenderer} transformation, so <i>both</i> mods' client
 * entrypoints die at {@code Initializing game} — installing the two together was an unconditional
 * crash on every version and every loader. Winning the tie would not have helped; whoever loses still
 * kills the game. MixinExtras composes instead: it is a late injector (applied after every vanilla
 * redirect) and it wraps whatever the instruction has become, so the other mod's redirect runs as
 * {@code original.call(entity)} and its colour is the base this event then gets to override — which
 * is what "override the colour vanilla would have used" meant anyway. Same family, same fix, as
 * {@code mixin.fabric.LivingEntityFoodMixin} and fabric-item-api's food redirect. MixinExtras needs
 * no dependency of its own: Fabric Loader and NeoForge bundle it, and {@code build.forgeg.gradle.kts}
 * ships {@code mixinextras-forge} on the Forge builds below 60.1.11 that do not.
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

    @WrapOperation(
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
    private int citadel_getTeamColor(Entity entity, Operation<Integer> original) {
        int color = original.call(entity);
        EventGetOutlineColor event = new EventGetOutlineColor(entity, color);
        EventGetOutlineColor.post(event);
        if (event.getCitadelResult() == CitadelEvent.Result.ALLOW) {
            color = event.getColor();
        }
        return color;
    }
}
