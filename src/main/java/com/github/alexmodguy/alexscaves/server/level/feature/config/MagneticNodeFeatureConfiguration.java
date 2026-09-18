package com.github.alexmodguy.alexscaves.server.level.feature.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class MagneticNodeFeatureConfiguration implements ACFeatureConfiguration {
    public static final MapCodec<MagneticNodeFeatureConfiguration> MAP_CODEC = RecordCodecBuilder.mapCodec((config) -> {
        return config.group(BlockStateProvider.CODEC.fieldOf("pillar").forGetter((otherConfig) -> {
            return otherConfig.pillarBlock;
        }), BlockStateProvider.CODEC.fieldOf("node").forGetter((otherConfig) -> {
            return otherConfig.nodeBlock;
        })).apply(config, MagneticNodeFeatureConfiguration::new);
    });
    public static final Codec<MagneticNodeFeatureConfiguration> CODEC = MAP_CODEC.codec();
    public final BlockStateProvider pillarBlock;
    public final BlockStateProvider nodeBlock;

    public MagneticNodeFeatureConfiguration(BlockStateProvider pillarBlock, BlockStateProvider nodeBlock) {
        this.pillarBlock = pillarBlock;
        this.nodeBlock = nodeBlock;
    }
}