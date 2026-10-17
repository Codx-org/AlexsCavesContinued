package com.github.alexmodguy.alexscaves.server.level.feature;

import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.github.alexmodguy.alexscaves.server.block.CycadBlock;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

public class CycadFeature extends ACSimpleFeature {

    public CycadFeature(Object codec) {
        super(codec);
    }

    @Override
    public boolean acPlace(WorldGenLevel acLevel, RandomSource acRandom, BlockPos acOrigin) {
        RandomSource randomsource = acRandom;
        WorldGenLevel level = acLevel;
        BlockPos treeBottom = acOrigin;
        if (!level.getBlockState(treeBottom.below()).is(ACTagRegistry.DIRT_LIKE)) {
            return false;
        }
        int height = 1 + (int) Math.ceil(randomsource.nextFloat() * 2.5F);
        for (int i = 0; i <= height; i++) {
            BlockPos trunk = treeBottom.above(i);
            if (canReplace(level.getBlockState(trunk))) {
                level.setBlock(trunk, ACBlockRegistry.CYCAD.get().defaultBlockState().setValue(CycadBlock.TOP, i == height), 2);
            }
        }
        return true;
    }

    private static boolean canReplace(BlockState state) {
        return (state.isAir() || state.canBeReplaced()) && !state.is(ACTagRegistry.UNMOVEABLE) && state.getFluidState().isEmpty();
    }
}
