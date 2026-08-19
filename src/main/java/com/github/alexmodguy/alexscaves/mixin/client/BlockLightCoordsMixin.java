package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.server.block.EnergizedGalenaBlock;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Energized galena lights itself: the block's own emission is folded into the block-light half of
// the packed light coords, so it glows even where the level's light engine says it is dark.
//
// This lives in a file of its own rather than in LevelRendererMixin because 26.2 moved the static
// it injects into — both getLightCoords overloads and the BrightnessGetter interface with them —
// out of LevelRenderer and into net.minecraft.util.LightCoordsUtil. A mixin class names exactly one
// target, so the target itself is Stonecutter-gated here. That is the OutlineColorMixin carve-out
// from the 1.21.9 wave, and it is cheaper than the exclude-from-source-set + pruneMixinEntries
// convention: both classes exist on every node, so nothing has to be pruned from the config.
//? if >=26.2 {
/*@Mixin(net.minecraft.util.LightCoordsUtil.class)
*///?} else {
@Mixin(LevelRenderer.class)
//?}
public abstract class BlockLightCoordsMixin {

    // 1.21.5 threaded a brightness lookup through this static overload, so it gained a leading
    // parameter. Nothing the body reads moved, so the arms are the same handler with a
    // different argument list in front of it and the work stays in one place below.
    //
    // 26 renamed the method getLightCoords — the same rename Particle#getLightColor got, which is
    // why the `!mc261-getlightcoords` replacement rule covers both — and swapped the getter it takes
    // from BlockAndTintGetter to the narrower BlockAndLightGetter. Two independent changes to one
    // descriptor, so this arm is spelled out natively rather than left to the rule: the rule would
    // fix the name and leave the parameter wrong, which fails at mixin-apply rather than at compile.
    //
    // 26.2 changes only the owner, so its arm is the 26 one with LightCoordsUtil in front of it.
    //? if >=26.2 {
    /*@Inject(method = "Lnet/minecraft/util/LightCoordsUtil;getLightCoords(Lnet/minecraft/util/LightCoordsUtil$BrightnessGetter;Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At("HEAD"),
            cancellable = true)
    private static void ac_getLightCoords(net.minecraft.util.LightCoordsUtil.BrightnessGetter brightness, net.minecraft.world.level.BlockAndLightGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        ac_energizedGalenaLight(level, state, pos, cir);
    }
    *///?} elif >=26 {
    /*@Inject(method = "Lnet/minecraft/client/renderer/LevelRenderer;getLightCoords(Lnet/minecraft/client/renderer/LevelRenderer$BrightnessGetter;Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At("HEAD"),
            cancellable = true)
    private static void ac_getLightCoords(net.minecraft.client.renderer.LevelRenderer.BrightnessGetter brightness, net.minecraft.world.level.BlockAndLightGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        ac_energizedGalenaLight(level, state, pos, cir);
    }
    *///?} elif >=1.21.5 {
    /*@Inject(method = "Lnet/minecraft/client/renderer/LevelRenderer;getLightColor(Lnet/minecraft/client/renderer/LevelRenderer$BrightnessGetter;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At("HEAD"),
            cancellable = true)
    private static void ac_getLightColor(net.minecraft.client.renderer.LevelRenderer.BrightnessGetter brightness, BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        ac_energizedGalenaLight(level, state, pos, cir);
    }
    *///?} else {
    @Inject(method = "Lnet/minecraft/client/renderer/LevelRenderer;getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At("HEAD"),
            cancellable = true)
    private static void ac_getLightColor(BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        ac_energizedGalenaLight(level, state, pos, cir);
    }
    //?}

    @org.spongepowered.asm.mixin.Unique
    //? if >=26 {
    /*private static void ac_energizedGalenaLight(net.minecraft.world.level.BlockAndLightGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
    *///?} else {
    private static void ac_energizedGalenaLight(BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
    //?}
        if (state.getBlock() instanceof EnergizedGalenaBlock) {
            int i = level.getBrightness(LightLayer.SKY, pos);
            int j = level.getBrightness(LightLayer.BLOCK, pos);
            //? if fabric
            /*int k = state.getLightEmission() - 1;*/
            //? if !fabric
            int k = state.getLightEmission(level, pos) - 1;
            if (j < k) {
                j = k;
            }
            cir.setReturnValue(i << 20 | (j) << 4);
        }
    }
}
