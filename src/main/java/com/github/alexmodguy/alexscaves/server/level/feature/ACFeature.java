package com.github.alexmodguy.alexscaves.server.level.feature;

import com.github.alexmodguy.alexscaves.server.level.feature.config.ACFeatureConfiguration;

/**
 * Base class for the mod's ten configured features — those that were {@code Feature<SomeConfiguration>}.
 *
 * <p>The config-free sibling {@code ACSimpleFeature} documents why this layer exists and why the
 * constructor takes {@code Object}; the one difference here is where the config comes from. Below
 * 26.3 it arrives per placement, inside the {@code FeaturePlaceContext}. At 26.3 a feature *is* its
 * config — the registry stores a {@code MapCodec} and decoding produces one instance per configured
 * feature — so it is constructor state, and {@code place} reads the field.
 *
 * <p>That is also why the 26.3 constructor argument is the config while the pre-26.3 one is a
 * {@code Codec}: {@code ACFeatureRegistry} passes the right thing on each arm, and the subclasses
 * never mention either type.
 */
//? if >=26.3 {
/*public abstract class ACFeature<FC extends ACFeatureConfiguration> implements net.minecraft.world.level.levelgen.feature.Feature {

    private final FC acConfig;

    @SuppressWarnings("unchecked")
    public ACFeature(Object config) {
        this.acConfig = (FC) config;
    }

    public FC acConfig() {
        return this.acConfig;
    }

    @Override
    public com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.levelgen.feature.Feature> codec() {
        return ACFeatureRegistry.codecFor(this.getClass());
    }

    @Override
    public boolean place(net.minecraft.world.level.WorldGenLevel acLevel, net.minecraft.world.level.chunk.ChunkGenerator acChunkGenerator, net.minecraft.util.RandomSource acRandom, net.minecraft.core.BlockPos acOrigin) {
        return this.acPlace(this.acConfig, acLevel, acRandom, acOrigin);
    }

    public abstract boolean acPlace(FC acConfig, net.minecraft.world.level.WorldGenLevel acLevel, net.minecraft.util.RandomSource acRandom, net.minecraft.core.BlockPos acOrigin);
}
*///?} else {
public abstract class ACFeature<FC extends ACFeatureConfiguration> extends net.minecraft.world.level.levelgen.feature.Feature<FC> {

    @SuppressWarnings("unchecked")
    public ACFeature(Object codec) {
        super((com.mojang.serialization.Codec<FC>) codec);
    }

    @Override
    public boolean place(net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<FC> context) {
        return this.acPlace(context.config(), context.level(), context.random(), context.origin());
    }

    public abstract boolean acPlace(FC acConfig, net.minecraft.world.level.WorldGenLevel acLevel, net.minecraft.util.RandomSource acRandom, net.minecraft.core.BlockPos acOrigin);
}
//?}
