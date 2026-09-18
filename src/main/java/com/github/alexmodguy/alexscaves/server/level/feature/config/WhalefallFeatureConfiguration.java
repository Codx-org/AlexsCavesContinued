package com.github.alexmodguy.alexscaves.server.level.feature.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class WhalefallFeatureConfiguration implements ACFeatureConfiguration {

    public static final MapCodec<WhalefallFeatureConfiguration> MAP_CODEC = RecordCodecBuilder.mapCodec((configurationInstance) -> {
        return configurationInstance.group(ResourceLocation.CODEC.listOf().fieldOf("head_structures").forGetter((p_159830_) -> {
                    return p_159830_.headStructures;
                }), ResourceLocation.CODEC.listOf().fieldOf("body_structures").forGetter((p_159830_) -> {
                    return p_159830_.bodyStructures;
                }), ResourceLocation.CODEC.listOf().fieldOf("tail_structures").forGetter((p_159830_) -> {
                    return p_159830_.tailStructures;
                })
        ).apply(configurationInstance, WhalefallFeatureConfiguration::new);
    });
    public static final Codec<WhalefallFeatureConfiguration> CODEC = MAP_CODEC.codec();
    public final List<ResourceLocation> headStructures;
    public final List<ResourceLocation> bodyStructures;
    public final List<ResourceLocation> tailStructures;

    public WhalefallFeatureConfiguration(List<ResourceLocation> headStructures, List<ResourceLocation> bodyStructures, List<ResourceLocation> tailStructures) {
        if (headStructures.isEmpty() || bodyStructures.isEmpty() || tailStructures.isEmpty()) {
            throw new IllegalArgumentException("structure lists need at least one entry");
        } else {
            this.headStructures = headStructures;
            this.bodyStructures = bodyStructures;
            this.tailStructures = tailStructures;
        }
    }
}