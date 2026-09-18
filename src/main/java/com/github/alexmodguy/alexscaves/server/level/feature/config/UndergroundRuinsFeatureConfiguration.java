package com.github.alexmodguy.alexscaves.server.level.feature.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class UndergroundRuinsFeatureConfiguration implements ACFeatureConfiguration {

    public static final MapCodec<UndergroundRuinsFeatureConfiguration> MAP_CODEC = RecordCodecBuilder.mapCodec((configurationInstance) -> {
        return configurationInstance.group(ResourceLocation.CODEC.listOf().fieldOf("structures").forGetter((p_159830_) -> {
                    return p_159830_.structures;
                }),
                ResourceLocation.CODEC.fieldOf("chest_loot").forGetter((otherConfig) -> {
                    return otherConfig.chestLoot;
                }), Codec.INT.fieldOf("sink_by").forGetter((otherConfig) -> {
                    return otherConfig.sinkBy;
                })).apply(configurationInstance, UndergroundRuinsFeatureConfiguration::new);
    });
    public static final Codec<UndergroundRuinsFeatureConfiguration> CODEC = MAP_CODEC.codec();
    public final List<ResourceLocation> structures;
    public final ResourceLocation chestLoot;
    public final int sinkBy;

    public UndergroundRuinsFeatureConfiguration(List<ResourceLocation> structures, ResourceLocation chestLoot, int sinkBy) {
        if (structures.isEmpty()) {
            throw new IllegalArgumentException("structure lists need at least one entry");
        } else {
            this.structures = structures;
        }
        this.sinkBy = sinkBy;
        this.chestLoot = chestLoot;
    }
}