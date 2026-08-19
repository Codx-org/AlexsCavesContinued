package com.github.alexmodguy.alexscaves.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The modern half of {@code ACClientCompat#runAsFancy}.
 *
 * <p>That helper used to flip the graphics option to FANCY around one draw. 1.21.11 deleted the
 * option and replaced it with a boolean whose setter rewrites the player's graphics preset to CUSTOM
 * — a change that survives into {@code options.txt} — so the flip cannot be a setter call any more.
 * What the six affected renderers were really asking is a single query, so from 1.21.11 the answer is
 * forced at the query instead of at the option behind it.
 *
 * <p>The query itself then moved house at 26.2: {@code Minecraft#useShaderTransparency()} is gone, and
 * the same test — not panoramic, and improved transparency enabled — is an instance method on
 * {@code GameRenderState}, which reads it off the camera and options render states it already holds.
 * Its caller set is unchanged from 26.1 ({@code WeatherEffectRenderer}, {@code LevelRenderer} and
 * {@code ItemFeatureRenderer}), so this is a pure move, and the handler follows it and stops being
 * static along the way — Mixin matches a handler's static-ness against its target's.
 *
 * <p>This lives in its own file with the {@code @Mixin} target gated, rather than as another arm in
 * {@code MinecraftMixin}, because the injection moved to a different class rather than to a different
 * member — the same shape as {@code OutlineColorMixin} and {@code SkyTimeOfDayMixin}. Below 1.21.11
 * the class contributes nothing at all; {@code runAsFancy} really does flip the option there.
 *
 * <p>Priority -100, as it was on {@code MinecraftMixin}, so anything else that wants the last word on
 * this query still gets it.
 */
//? if >=26.2 {
/*@Mixin(value = net.minecraft.client.renderer.state.GameRenderState.class, priority = -100)
*///?} else {
@Mixin(value = net.minecraft.client.Minecraft.class, priority = -100)
//?}
public class ShaderTransparencyMixin {

    //? if >=26.2 {
    /*@Inject(method = "Lnet/minecraft/client/renderer/state/GameRenderState;useShaderTransparency()Z",
            at = @At("HEAD"),
            cancellable = true)
    private void ac_useShaderTransparency(CallbackInfoReturnable<Boolean> cir) {
        if (com.github.alexmodguy.alexscaves.client.ACClientCompat.isForcingFancy()) {
            cir.setReturnValue(false);
        }
    }
    *///?} elif >=1.21.11 {
    /*@Inject(method = "Lnet/minecraft/client/Minecraft;useShaderTransparency()Z",
            at = @At("HEAD"),
            cancellable = true)
    private static void ac_useShaderTransparency(CallbackInfoReturnable<Boolean> cir) {
        if (com.github.alexmodguy.alexscaves.client.ACClientCompat.isForcingFancy()) {
            cir.setReturnValue(false);
        }
    }
    *///?}
}
