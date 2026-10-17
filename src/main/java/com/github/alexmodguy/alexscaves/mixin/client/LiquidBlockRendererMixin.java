package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
//? if <26
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 26 renamed the class LiquidBlockRenderer -> FluidRenderer (same package, and the target method
// is untouched). A replacements.string rule cannot do it: the token is a prefix of this mixin's
// own public class name, which Stonecutter may not rewrite because it keeps the file name.
//? if >=26 {
/*@Mixin(net.minecraft.client.renderer.block.FluidRenderer.class)
*///?} else {
@Mixin(LiquidBlockRenderer.class)
//?}
public class LiquidBlockRendererMixin {

    // 1.21.2 dropped the level and position from the signature — the method never read either, it
    // only ever asked the neighbour's BlockState, which is still the last argument. 26 kept that
    // signature and only moved the owner, so the >=26 arm differs from the one below it by one word.
    //? if >=26 {
    /*@Inject(
            method = {"Lnet/minecraft/client/renderer/block/FluidRenderer;isFaceOccludedByNeighbor(Lnet/minecraft/core/Direction;FLnet/minecraft/world/level/block/state/BlockState;)Z"},
            remap = true,
            cancellable = true,
            at = @At(value = "TAIL")
    )
    private static void isFaceOccludedByNeighbor(Direction direction, float f, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.is(ACBlockRegistry.DEPTH_GLASS.get())) {
            cir.setReturnValue(true);
        }
    }
    *///?} elif >=1.21.2 {
    /*@Inject(
            method = {"Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;isFaceOccludedByNeighbor(Lnet/minecraft/core/Direction;FLnet/minecraft/world/level/block/state/BlockState;)Z"},
            remap = true,
            cancellable = true,
            at = @At(value = "TAIL")
    )
    private static void isFaceOccludedByNeighbor(Direction direction, float f, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.is(ACBlockRegistry.DEPTH_GLASS.get())) {
            cir.setReturnValue(true);
        }
    }
    *///?} else {
    @Inject(
            method = {"Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;isFaceOccludedByNeighbor(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;FLnet/minecraft/world/level/block/state/BlockState;)Z"},
            remap = true,
            cancellable = true,
            at = @At(value = "TAIL")
    )
    private static void isFaceOccludedByNeighbor(BlockGetter blockGetter, BlockPos pos, Direction direction, float f, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.is(ACBlockRegistry.DEPTH_GLASS.get())) {
            cir.setReturnValue(true);
        }
    }
    //?}
}
