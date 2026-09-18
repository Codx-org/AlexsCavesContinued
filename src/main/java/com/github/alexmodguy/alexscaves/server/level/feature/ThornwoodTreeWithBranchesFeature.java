package com.github.alexmodguy.alexscaves.server.level.feature;

import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;

public class ThornwoodTreeWithBranchesFeature extends ThornwoodTreeFeature {

    public ThornwoodTreeWithBranchesFeature(Object codec) {
        super(codec);
    }

    @Override
    public boolean acPlace(WorldGenLevel acLevel, RandomSource acRandom, BlockPos acOrigin) {
        BlockPos pos = acOrigin;
        if (super.acPlace(acLevel, acRandom, acOrigin)) {
            RandomSource randomSource = acRandom;
            WorldGenLevel level = acLevel;
            BlockPos.MutableBlockPos branchPos = new BlockPos.MutableBlockPos();

            for (int i = 0; i < 5 + randomSource.nextInt(10); i++) {
                branchPos.set(pos);
                branchPos.move(randomSource.nextInt(12) - 6, randomSource.nextInt(12), randomSource.nextInt(12) - 6);
                while (level.isEmptyBlock(branchPos) && branchPos.getY() > level.getMinBuildHeight()) {
                    branchPos.move(0, -1, 0);
                }
                if (level.getBlockState(branchPos).isCollisionShapeFullBlock(level, branchPos)) {
                    branchPos.move(0, 1, 0);
                    if (level.getBlockState(branchPos).canBeReplaced()) {
                        level.setBlock(branchPos, ACBlockRegistry.THORNWOOD_BRANCH.get().defaultBlockState(), 3);
                    }
                }
            }
            return true;
        }
        return false;
    }
}
