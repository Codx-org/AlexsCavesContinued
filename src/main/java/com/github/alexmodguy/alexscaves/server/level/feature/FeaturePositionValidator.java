package com.github.alexmodguy.alexscaves.server.level.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

public class FeaturePositionValidator {

    public static boolean isBiome(WorldGenLevel level, BlockPos origin, ResourceKey<Biome> biomeResourceKey) {
        int j = level.getHeight(Heightmap.Types.OCEAN_FLOOR, origin.getX(), origin.getZ());
        return level.getBiome(origin.atY(Math.min(level.getMinBuildHeight(), j - 30))).is(biomeResourceKey);
    }

    // The five vanilla-feature mixins that call this took a FeaturePlaceContext until 26.3 deleted the
    // type. The overload above is the real implementation and is valid on all 60 nodes; this one only
    // unpacks the context, and is gated away rather than rewritten because there is nothing on 26.3
    // for it to take. FeaturePlaceContext is spelled out in full so the file needs no gated import.
    //? if <26.3 {
    public static boolean isBiome(net.minecraft.world.level.levelgen.feature.FeaturePlaceContext context, ResourceKey<Biome> biomeResourceKey) {
        return isBiome(context.level(), context.origin(), biomeResourceKey);
    }
    //?}
}
