package com.github.alexmodguy.alexscaves.server.level.feature;

import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.github.alexmodguy.alexscaves.server.block.AmbersolBlock;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

public class AmbersolFeature extends ACSimpleFeature {

    public AmbersolFeature(Object config) {
        super(config);
    }

    public boolean acPlace(WorldGenLevel acLevel, RandomSource acRandom, BlockPos acOrigin) {
        BlockPos blockpos = acOrigin;
        WorldGenLevel worldgenlevel = acLevel;
        RandomSource randomsource = acRandom;

        for (blockpos = acOrigin; blockpos.getY() >= worldgenlevel.getMaxBuildHeight() - 3; blockpos = blockpos.above()) {
            if (!worldgenlevel.isEmptyBlock(blockpos.above())) {
                break;
            }
        }
        BlockPos copy = blockpos;
        if (blockpos.getY() >= worldgenlevel.getMaxBuildHeight() - 3) {
            return false;
        } else {
            for (int i = 0; i < 3; i++) {
                drawOrb(worldgenlevel, blockpos.offset(randomsource.nextInt(4) - 2, randomsource.nextInt(4) - 2, randomsource.nextInt(4) - 2), randomsource, ACBlockRegistry.AMBER.get().defaultBlockState(), 2 + randomsource.nextInt(2), 2 + randomsource.nextInt(2), 2 + randomsource.nextInt(2));
            }
            drawOrb(worldgenlevel, blockpos, randomsource, ACBlockRegistry.AMBER.get().defaultBlockState(), 2, 2, 2);
            worldgenlevel.setBlock(blockpos, ACBlockRegistry.AMBERSOL.get().defaultBlockState(), 3);
            AmbersolBlock.fillWithLights(blockpos, worldgenlevel);
            return true;
        }
    }

    private static boolean canReplace(BlockState state) {
        return state.isAir() || state.canBeReplaced();
    }

    private static void drawOrb(WorldGenLevel level, BlockPos center, RandomSource random, BlockState blockState, int radiusX, int radiusY, int radiusZ) {
        double equalRadius = (radiusX + radiusY + radiusZ) / 3.0D;
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int y = -radiusY; y <= radiusY; y++) {
                for (int z = -radiusZ; z <= radiusZ; z++) {
                    BlockPos fill = center.offset(x, y, z);
                    if (fill.distToLowCornerSqr(center.getX(), center.getY(), center.getZ()) <= equalRadius * equalRadius - random.nextFloat() * 4) {
                        if (canReplace(level.getBlockState(fill))) {
                            level.setBlock(fill, blockState, 2);
                        }
                    }
                }
            }
        }
    }
}