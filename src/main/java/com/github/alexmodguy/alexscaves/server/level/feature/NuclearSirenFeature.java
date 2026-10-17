package com.github.alexmodguy.alexscaves.server.level.feature;

import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;

public class NuclearSirenFeature extends ACSimpleFeature {

    public NuclearSirenFeature(Object codec) {
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
        int concrete = 0;
        while(concrete < 1 || !level.getFluidState(pillar).isEmpty()){
            concrete++;
            level.setBlock(pillar, ACBlockRegistry.CINDER_BLOCK.get().defaultBlockState(), 3);
            pillar.move(0, 1, 0);
        }
        level.setBlock(pillar, ACBlockRegistry.CINDER_BLOCK_WALL.get().defaultBlockState(), 3);
        pillar.move(0, 1, 0);
        for (int i = 0; i < 1 + randomsource.nextInt(2); i++) {
            level.setBlock(pillar, Blocks.DARK_OAK_FENCE.defaultBlockState(), 3);
            pillar.move(0, 1, 0);
        }
        level.setBlock(pillar, ACBlockRegistry.NUCLEAR_SIREN.get().defaultBlockState(), 3);
        return true;
    }
}
