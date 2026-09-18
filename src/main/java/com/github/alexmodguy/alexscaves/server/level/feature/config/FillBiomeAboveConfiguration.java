package com.github.alexmodguy.alexscaves.server.level.feature.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public class FillBiomeAboveConfiguration implements ACFeatureConfiguration {
    public static final MapCodec<FillBiomeAboveConfiguration> MAP_CODEC = RecordCodecBuilder.mapCodec((config) -> {
        return config.group(Biome.CODEC.fieldOf("replacing").forGetter((otherConfig) -> {
            return otherConfig.replacing;
        }), Biome.CODEC.fieldOf("new_biome").forGetter((otherConfig) -> {
            return otherConfig.newBiome;
        }), Codec.INT.fieldOf("y_above_sea_level").forGetter((otherConfig) -> {
            return otherConfig.yAboveSeaLevel;
        })).apply(config, FillBiomeAboveConfiguration::new);
    });
    public static final Codec<FillBiomeAboveConfiguration> CODEC = MAP_CODEC.codec();
    public final Holder<Biome> replacing;
    public final Holder<Biome> newBiome;
    public final int yAboveSeaLevel;

    public FillBiomeAboveConfiguration(Holder<Biome> replacing, Holder<Biome> newBiome, int yAboveSeaLevel) {
        this.replacing = replacing;
        this.newBiome = newBiome;
        this.yAboveSeaLevel = yAboveSeaLevel;
    }
}