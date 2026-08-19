package com.github.alexmodguy.alexscaves.server.entity.ai;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.SwimNodeEvaluator;

public class NotLavaSwimNodeEvaluator extends SwimNodeEvaluator {

    public NotLavaSwimNodeEvaluator(boolean breach) {
        super(breach);
    }

    // See AllFluidsNodeEvaluator: 1.20.5 renamed the per-cell hook and moved the level into the
    // PathfindingContext.
    //? if >=1.20.5 {
    /*@Override
    public BlockPathTypes getPathTypeOfMob(net.minecraft.world.level.pathfinder.PathfindingContext context, int x, int y, int z, Mob p_77476_) {
        BlockGetter getter = context.level();
    *///?} else {
    @Override
    public BlockPathTypes getBlockPathType(BlockGetter getter, int x, int y, int z, Mob p_77476_) {
    //?}
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int i = x; i < x + entityWidth; ++i) {
            for (int j = y; j < y + entityHeight; ++j) {
                for (int k = z; k < z + entityDepth; ++k) {
                    FluidState fluidstate = getter.getFluidState(blockpos$mutableblockpos.set(i, j, k));
                    BlockState blockstate = getter.getBlockState(blockpos$mutableblockpos.set(i, j, k));
                    if (fluidstate.isEmpty() && ACCompat.isPathfindable(blockstate, getter, blockpos$mutableblockpos.below(), PathComputationType.WATER) && blockstate.isAir()) {
                        return BlockPathTypes.BREACH;
                    }

                    //works in water, soda and acid, not lava
                    if (fluidstate.is(FluidTags.LAVA)) {
                        return BlockPathTypes.BLOCKED;
                    }
                }
            }
        }

        BlockState blockstate1 = getter.getBlockState(blockpos$mutableblockpos);
        return ACCompat.isPathfindable(blockstate1, getter, blockpos$mutableblockpos, PathComputationType.WATER) ? BlockPathTypes.WATER : BlockPathTypes.BLOCKED;
    }
}
