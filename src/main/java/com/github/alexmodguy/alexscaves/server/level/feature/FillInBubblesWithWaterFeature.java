package com.github.alexmodguy.alexscaves.server.level.feature;

import net.minecraft.util.RandomSource;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;

class FillInBubblesWithWaterFeature extends ACSimpleFeature {
    public FillInBubblesWithWaterFeature(Object p_66836_) {
        super(p_66836_);
    }

    public boolean acPlace(WorldGenLevel acLevel, RandomSource acRandom, BlockPos acOrigin) {
        WorldGenLevel worldgenlevel = acLevel;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 16; ++i) {
            for (int j = 0; j < 16; ++j) {
                int k = acOrigin.getX() + i;
                int l = acOrigin.getZ() + j;
                int i1 = worldgenlevel.getSeaLevel() - 2;
                pos.set(k, i1, l);

            }
        }
        return true;
    }
}