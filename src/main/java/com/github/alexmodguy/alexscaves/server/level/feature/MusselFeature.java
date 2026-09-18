package com.github.alexmodguy.alexscaves.server.level.feature;

import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.github.alexmodguy.alexscaves.server.block.MusselBlock;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

public class MusselFeature extends ACSimpleFeature {

    public MusselFeature(Object codec) {
        super(codec);
    }

    @Override
    public boolean acPlace(WorldGenLevel acLevel, RandomSource acRandom, BlockPos acOrigin) {
        BlockPos pos = acOrigin;
        WorldGenLevel level = acLevel;
        RandomSource randomSource = acRandom;
        if (tryPlaceMussel(level, pos, randomSource)) {
            for (int i = 0; i < randomSource.nextInt(3); i++) {
                tryPlaceMussel(level, pos.offset(randomSource.nextInt(4) - 2, randomSource.nextInt(4) - 2, randomSource.nextInt(4) - 2), randomSource);
            }
            return true;
        }
        return false;
    }

    private boolean tryPlaceMussel(WorldGenLevel level, BlockPos pos, RandomSource randomSource) {
        if (level.getBlockState(pos).is(Blocks.WATER)) {
            List<Direction> possiblities = new ArrayList<>();
            for (Direction possible : Direction.values()) {
                BlockPos check = pos.relative(possible);
                if (isSameChunk(pos, check) && level.getBlockState(check).isFaceSturdy(level, check, possible.getOpposite())) {
                    possiblities.add(possible.getOpposite());
                }
            }
            Direction direction = selectDirection(possiblities, randomSource);
            if (direction != null) {
                level.setBlock(pos, ACBlockRegistry.MUSSEL.get().defaultBlockState().setValue(MusselBlock.FACING, direction).setValue(MusselBlock.WATERLOGGED, true).setValue(MusselBlock.MUSSELS, 1 + randomSource.nextInt(4)), 3);
                return true;
            }
        }
        return false;
    }

    private boolean isSameChunk(BlockPos pos, BlockPos check) {
        return SectionPos.blockToSectionCoord(pos.getX()) == SectionPos.blockToSectionCoord(check.getX()) && SectionPos.blockToSectionCoord(pos.getZ()) == SectionPos.blockToSectionCoord(check.getZ());
    }

    private static Direction selectDirection(List<Direction> directionList, RandomSource randomSource) {
        if (directionList.size() <= 0) {
            return null;
        } else if (directionList.size() <= 1) {
            return directionList.get(0);
        } else {
            return directionList.get(randomSource.nextInt(directionList.size() - 1));
        }
    }
}
