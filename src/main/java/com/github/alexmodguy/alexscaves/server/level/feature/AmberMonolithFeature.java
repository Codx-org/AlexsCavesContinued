package com.github.alexmodguy.alexscaves.server.level.feature;

import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

public class AmberMonolithFeature extends ACSimpleFeature {

    public AmberMonolithFeature(Object codec) {
        super(codec);
    }

    @Override
    public boolean acPlace(WorldGenLevel acLevel, RandomSource acRandom, BlockPos acOrigin) {
        RandomSource randomsource = acRandom;
        WorldGenLevel level = acLevel;
        BlockPos below = acOrigin;
        if (!level.getBlockState(below.below()).isSolid()) {
            return false;
        }
        BlockPos.MutableBlockPos pillar = new BlockPos.MutableBlockPos();
        pillar.set(below);
        for (int i = 0; i < 4 + randomsource.nextInt(2); i++) {
            level.setBlock(pillar, ACBlockRegistry.LIMESTONE_PILLAR.get().defaultBlockState(), 3);
            pillar.move(0, 1, 0);
        }
        level.setBlock(pillar, ACBlockRegistry.AMBER_MONOLITH.get().defaultBlockState(), 3);
        if (randomsource.nextBoolean()) {
            pillar.move(0, 1, 0);
            level.setBlock(pillar, ACBlockRegistry.LIMESTONE_SLAB.get().defaultBlockState(), 3);
        }
        BlockPos pillarTop = pillar.immutable();
        for (int i = 0; i < 4 + randomsource.nextInt(6); i++) {
            BlockPos offset = pillarTop.offset(randomsource.nextInt(6) - 3, 1, randomsource.nextInt(6) - 3);
            while (level.isEmptyBlock(offset) && offset.getY() > level.getMinBuildHeight()) {
                offset = offset.below();
            }
            if (level.getBlockState(offset).isFaceSturdy(level, offset, Direction.UP) && level.isEmptyBlock(offset.above())) {
                BlockState randomState;
                float f = randomsource.nextFloat();
                if (f < 0.3F) {
                    randomState = ACBlockRegistry.LIMESTONE_SLAB.get().defaultBlockState();
                } else if (f < 0.6F) {
                    randomState = ACBlockRegistry.LIMESTONE_PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
                } else if (f < 0.9F) {
                    randomState = ACBlockRegistry.LIMESTONE_PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
                } else {
                    randomState = ACBlockRegistry.AMBER.get().defaultBlockState();
                }
                level.setBlock(offset.above(), randomState, 3);

            }
        }
        return true;
    }
}
