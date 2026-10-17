package com.github.alexmodguy.alexscaves.server.level.feature.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class AbyssalFloraFeatureConfiguration implements ACFeatureConfiguration {

    public static final MapCodec<AbyssalFloraFeatureConfiguration> MAP_CODEC = RecordCodecBuilder.mapCodec((config) -> {
        return config.group(BlockStateProvider.CODEC.fieldOf("flora").forGetter((otherConfig) -> {
            return otherConfig.floraBlock;
        })).apply(config, AbyssalFloraFeatureConfiguration::new);
    });
    public static final Codec<AbyssalFloraFeatureConfiguration> CODEC = MAP_CODEC.codec();
    public final BlockStateProvider floraBlock;

    public AbyssalFloraFeatureConfiguration(BlockStateProvider floraBlock) {
        this.floraBlock = floraBlock;
    }
}