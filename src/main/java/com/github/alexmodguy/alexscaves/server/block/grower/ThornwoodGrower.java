package com.github.alexmodguy.alexscaves.server.block.grower;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class ThornwoodGrower implements ACTreeGrower {

    public static final ResourceKey<ConfiguredFeature<?, ?>> THORNWOOD_TREE = ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "thornwood_tree"));

    @Override
    public ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource randomSource) {
        return THORNWOOD_TREE;
    }
}
