package com.github.alexmodguy.alexscaves.server.level.feature;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.github.alexmodguy.alexscaves.server.level.feature.config.AbyssalFloraFeatureConfiguration;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

public class AbyssalFloraFeature extends ACFeature<AbyssalFloraFeatureConfiguration> {

    public AbyssalFloraFeature(Object codec) {
        super(codec);
    }

    @Override
    public boolean acPlace(AbyssalFloraFeatureConfiguration acConfig, WorldGenLevel acLevel, RandomSource acRandom, BlockPos acOrigin) {
        RandomSource randomsource = acRandom;
        WorldGenLevel level = acLevel;
        BlockPos.MutableBlockPos trenchBottom = new BlockPos.MutableBlockPos();
        trenchBottom.set(acOrigin);
        while (!level.getBlockState(trenchBottom).getFluidState().isEmpty() && trenchBottom.getY() > level.getMinBuildHeight()) {
            trenchBottom.move(0, -1, 0);
        }
        if (acOrigin.getY() - trenchBottom.getY() < 15) {
            return false;
        }
        BlockPos above = trenchBottom.above();
        if (canReplace(level.getBlockState(above))) {
            level.setBlock(above, ACCompat.providerState(level, acConfig.floraBlock, randomsource, above), 2);
        }
        return true;
    }

    private static boolean canReplace(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER) && state.liquid();
    }
}
