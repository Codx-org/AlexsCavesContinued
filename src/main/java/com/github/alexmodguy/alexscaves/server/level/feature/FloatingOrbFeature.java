package com.github.alexmodguy.alexscaves.server.level.feature;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.github.alexmodguy.alexscaves.server.level.feature.config.FloatingOrbFeatureConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class FloatingOrbFeature extends ACFeature<FloatingOrbFeatureConfig> {

    public FloatingOrbFeature(Object codec) {
        super(codec);
    }

    @Override
    public boolean acPlace(FloatingOrbFeatureConfig acConfig, WorldGenLevel acLevel, RandomSource acRandom, BlockPos acOrigin) {
        BlockPos pos = acOrigin;
        WorldGenLevel level = acLevel;
        RandomSource randomSource = acRandom;
        if (!canReplace(level.getBlockState(pos))) {
            return false;
        }
        int minRadius = acConfig.minRadius;
        int radAdd = Math.max(1, acConfig.maxRadius - acConfig.minRadius);
        int radius = minRadius + randomSource.nextInt(radAdd);
        drawOrb(level, pos, randomSource, acConfig.orbBlock, radius + randomSource.nextInt(2) - 1, radius + randomSource.nextInt(2) - 1, radius + randomSource.nextInt(2) - 1);
        return true;
    }

    private static boolean canReplace(BlockState state) {
        return state.isAir() || state.canBeReplaced();
    }

    private static void drawOrb(WorldGenLevel level, BlockPos center, RandomSource random, BlockStateProvider blockState, int radiusX, int radiusY, int radiusZ) {
        double equalRadius = (radiusX + radiusY + radiusZ) / 3.0D;
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -radiusY; y <= radiusY; y++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    BlockPos fill = center.offset(x, y, z);
                    if (fill.distToLowCornerSqr(center.getX(), center.getY(), center.getZ()) <= equalRadius * equalRadius + random.nextFloat() * 2) {
                        if (canReplace(level.getBlockState(fill))) {
                            level.setBlock(fill, ACCompat.providerState(level, blockState, random, fill), 3);
                        }
                    }
                }
            }
        }
    }
}
