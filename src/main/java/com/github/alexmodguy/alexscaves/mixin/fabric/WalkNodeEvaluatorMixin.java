package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.server.block.ACAdjacentPathTypeBlock;
import com.github.alexmodguy.alexscaves.server.block.ACPathTypeBlock;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric's stand-in for the two path-type hooks Forge and NeoForge patch into
 * {@code WalkNodeEvaluator} — {@link ACPathTypeBlock} and {@link ACAdjacentPathTypeBlock}.
 *
 * <p>The loaders' patch is <b>body-only</b>: every signature on this class is identical to
 * vanilla's, and a {@code javap -p} diff of the patched jar against the unpatched one is empty.
 * Only {@code javap -c} shows it, and both insertion points were read out of that disassembly
 * rather than guessed, so the two injections below sit exactly where the loaders' own calls do.
 *
 * <ul>
 *   <li>{@code getBlockPathTypeRaw} — the loaders make {@code state.getBlockPathType(level, pos,
 *       null)} the method's <em>first</em> statement and return it when it is non-null, ahead of
 *       the whole vanilla {@code instanceof}/tag cascade. So this is a HEAD inject, and the
 *       {@code Mob} really is passed as {@code null}: the vanilla call site has no mob to give.
 *       1.20.5 renamed it {@code getPathTypeFromState} and changed nothing else about it — same
 *       descriptor, same patch, same first statement — so only the selector is gated and the
 *       handler is shared.</li>
 *   <li>{@code checkNeighbourBlocks} — the loaders call {@code state.getAdjacentBlockPathType(
 *       level, pos, null, originalType)} <em>inside</em> the 3×3×3 neighbour loop, immediately
 *       after the {@code BlockState} is fetched and stored, returning early when it is non-null.
 *       {@code INVOKE_ASSIGN} on that fetch is that instruction offset precisely (the store lands
 *       at 80 and the loader's hook at 82), which is why the anchor is the fetch rather than the
 *       {@code Blocks.CACTUS} test that follows it: injecting after the cascade had begun would
 *       let vanilla's answer win for a block that has an opinion, and reimplementing the loop at
 *       HEAD would give mod blocks priority over vanilla's checks for <em>earlier</em> neighbours
 *       — neither is what the loaders do.</li>
 * </ul>
 *
 * <p>The neighbour state arrives as a {@code @Local} rather than being looked up again: this
 * method runs 26 times per pathfinding node, and a second {@code getBlockState} per neighbour to
 * serve a hook that almost never answers is real cost. The merged Mojmap jars carry no
 * {@code LocalVariableTable}, so MixinExtras infers the slot from the {@code StackMapTable} — the
 * implicit form is unambiguous because it is the only {@code BlockState} local in scope.
 *
 * <p><b>1.20.5 rebuilt {@code checkNeighbourBlocks} around a {@code PathfindingContext}</b>, and
 * that is why the second injection needs a whole arm rather than a renamed selector. It takes the
 * context and three absolute {@code int}s now instead of a {@code BlockGetter} and a mutable
 * position, and the loop body no longer fetches a {@code BlockState} at all — it asks the context
 * for a {@code PathType} directly, so there is no state local left to borrow. Forge's patch adapts
 * the same way: after the {@code PathfindingContext#getPathTypeFromState(III)} store it builds a
 * {@code BlockPos} from the loop offsets, reads the state off {@code context.level()} and only then
 * asks the block, still ahead of the cascade (1.20.6 offsets 55–103). The arm below is that,
 * instruction for instruction, with the anchor on the same store.
 *
 * <p>⚠️ The three {@code @Local} ints are the loop counters, and they are addressed by
 * <b>ordinal</b> because there is nothing else to address them by. Ordinals count every {@code int}
 * in scope at the injection point in slot order, parameters included — {@code x}, {@code y},
 * {@code z} are 0–2 and the counters are 3–5. Re-derive them from the disassembly if the loop ever
 * gains or loses an {@code int}; nothing else would notice, and a wrong ordinal here reads a
 * neighbour that is not the one the type came from.
 *
 * <p><b>There is deliberately no dispatcher for {@link
 * com.github.alexmodguy.alexscaves.server.block.ACBurningBlock} here</b>, even though it is the
 * third block of this family: {@code Block#isBurning} has no caller on any loader in the range —
 * see that interface's javadoc for the census. Adding one would give Fabric behaviour Forge and
 * NeoForge do not have, which is the opposite of a port.
 */
@Mixin(WalkNodeEvaluator.class)
public class WalkNodeEvaluatorMixin {

    //? if <1.20.5 {
    @Inject(method = "getBlockPathTypeRaw", at = @At("HEAD"), cancellable = true)
    //?} else {
    /*@Inject(method = "getPathTypeFromState", at = @At("HEAD"), cancellable = true)
    *///?}
    private static void ac_getBlockPathTypeRaw(BlockGetter level, BlockPos pos, CallbackInfoReturnable<BlockPathTypes> cir) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ACPathTypeBlock typed) {
            BlockPathTypes type = typed.getBlockPathType(state, level, pos, null);
            if (type != null) {
                cir.setReturnValue(type);
            }
        }
    }

    //? if <1.20.5 {
    @Inject(
            method = "checkNeighbourBlocks",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/level/BlockGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
            ),
            cancellable = true
    )
    private static void ac_checkNeighbourBlocks(BlockGetter level, BlockPos.MutableBlockPos pos, BlockPathTypes originalType, CallbackInfoReturnable<BlockPathTypes> cir, @Local BlockState state) {
        if (state.getBlock() instanceof ACAdjacentPathTypeBlock typed) {
            BlockPathTypes type = typed.getAdjacentBlockPathType(state, level, pos, null, originalType);
            if (type != null) {
                cir.setReturnValue(type);
            }
        }
    }
    //?} else {
    /*@Inject(
            method = "checkNeighbourBlocks",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/level/pathfinder/PathfindingContext;getPathTypeFromState(III)Lnet/minecraft/world/level/pathfinder/BlockPathTypes;"
            ),
            cancellable = true
    )
    private static void ac_checkNeighbourBlocks(net.minecraft.world.level.pathfinder.PathfindingContext context, int x, int y, int z, BlockPathTypes originalType, CallbackInfoReturnable<BlockPathTypes> cir,
                                                @Local(ordinal = 3) int offsetX, @Local(ordinal = 4) int offsetY, @Local(ordinal = 5) int offsetZ) {
        BlockPos pos = new BlockPos(x + offsetX, y + offsetY, z + offsetZ);
        BlockState state = context.level().getBlockState(pos);
        if (state.getBlock() instanceof ACAdjacentPathTypeBlock typed) {
            BlockPathTypes type = typed.getAdjacentBlockPathType(state, context.level(), pos, null, originalType);
            if (type != null) {
                cir.setReturnValue(type);
            }
        }
    }
    *///?}
}
