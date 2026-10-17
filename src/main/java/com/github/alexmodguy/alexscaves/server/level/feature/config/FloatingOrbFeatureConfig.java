package com.github.alexmodguy.alexscaves.server.level.feature.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class FloatingOrbFeatureConfig implements ACFeatureConfiguration {
    public static final MapCodec<FloatingOrbFeatureConfig> MAP_CODEC = RecordCodecBuilder.mapCodec((config) -> {
        return config.group(BlockStateProvider.CODEC.fieldOf("orb_made_of").forGetter((otherConfig) -> {
            return otherConfig.orbBlock;
        }), Codec.INT.fieldOf("min_radius").forGetter((otherConfig) -> {
            return otherConfig.minRadius;
        }), Codec.INT.fieldOf("max_radius").forGetter((otherConfig) -> {
            return otherConfig.maxRadius;
        })).apply(config, FloatingOrbFeatureConfig::new);
    });
    public static final Codec<FloatingOrbFeatureConfig> CODEC = MAP_CODEC.codec();
    public final BlockStateProvider orbBlock;
    public final int minRadius;
    public final int maxRadius;

    public FloatingOrbFeatureConfig(BlockStateProvider orbBlock, int minRadius, int maxRadius) {
        this.orbBlock = orbBlock;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
    }
}