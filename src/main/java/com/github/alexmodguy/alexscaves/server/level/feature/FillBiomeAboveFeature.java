package com.github.alexmodguy.alexscaves.server.level.feature;

import net.minecraft.util.RandomSource;
import com.github.alexmodguy.alexscaves.server.level.feature.config.FillBiomeAboveConfiguration;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

public class FillBiomeAboveFeature extends ACFeature<FillBiomeAboveConfiguration> {

    public FillBiomeAboveFeature(Object codec) {
        super(codec);
    }

    @Override
    public boolean acPlace(FillBiomeAboveConfiguration acConfig, WorldGenLevel acLevel, RandomSource acRandom, BlockPos acOrigin) {
        WorldGenLevel level = acLevel;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int startY = level.getSeaLevel() + acConfig.yAboveSeaLevel;
        pos.set(acOrigin.getX(), startY, acOrigin.getZ());
        ChunkAccess chunkAccess = level.getChunk(pos);
        if (chunkAccess != null) {
            int lastSectionIndex = -1;
            while (pos.getY() < level.getMaxBuildHeight()) {
                pos.move(0, 8, 0);
                if (pos.getY() >> 4 != lastSectionIndex) {
                    lastSectionIndex = pos.getY() >> 4;
                    int sectionIndex = chunkAccess.getSectionIndex(pos.getY());
                    if (sectionIndex >= 0 && sectionIndex < chunkAccess.getSections().length) {
                        LevelChunkSection section = chunkAccess.getSection(sectionIndex);
                        PalettedContainer<Holder<Biome>> container = section.getBiomes().recreate();
                        for (int biomeX = 0; biomeX < 4; ++biomeX) {
                            for (int biomeY = 0; biomeY < 4; ++biomeY) {
                                for (int biomeZ = 0; biomeZ < 4; ++biomeZ) {
                                    container.getAndSetUnchecked(biomeX, biomeY, biomeZ, acConfig.newBiome);
                                }
                            }
                        }
                        section.biomes = container;
                    }
                }
            }
        }

        return true;
    }

}
