package com.github.alexmodguy.alexscaves.server.level.feature.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class GalenaHexagonFeatureConfiguration implements ACFeatureConfiguration {
    public static final MapCodec<GalenaHexagonFeatureConfiguration> MAP_CODEC = RecordCodecBuilder.mapCodec((config) -> {
        return config.group(BlockStateProvider.CODEC.fieldOf("hexagon_made_of").forGetter((otherConfig) -> {
            return otherConfig.hexBlock;
        }), Codec.BOOL.fieldOf("ceiling").forGetter((otherConfig) -> {
            return otherConfig.ceiling;
        })).apply(config, GalenaHexagonFeatureConfiguration::new);
    });
    public static final Codec<GalenaHexagonFeatureConfiguration> CODEC = MAP_CODEC.codec();
    public final BlockStateProvider hexBlock;
    public final boolean ceiling;

    public GalenaHexagonFeatureConfiguration(BlockStateProvider hexBlock, boolean ceiling) {
        this.hexBlock = hexBlock;
        this.ceiling = ceiling;
    }
}