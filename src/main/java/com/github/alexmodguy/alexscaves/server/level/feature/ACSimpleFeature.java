package com.github.alexmodguy.alexscaves.server.level.feature;

/**
 * Base class for the mod's config-free features — the ~38 that were {@code Feature<NoneFeatureConfiguration>}.
 *
 * <p>26.3 rewrote the feature layer: {@code Feature} became a non-generic interface whose registry
 * entry is a {@code MapCodec} rather than an instance, {@code FeaturePlaceContext} was deleted, and
 * {@code place} now takes its four arguments directly. Rather than gate all fifty feature classes,
 * the whole difference is absorbed here and in {@code ACFeature}: each subclass implements one
 * abstract {@code acPlace} that both arms forward to, and is otherwise identical on all 60 nodes.
 *
 * <p>{@code acPlace} cannot be called {@code place}: below 26.3 {@code Feature} already declares a
 * five-argument {@code place(FC, WorldGenLevel, ChunkGenerator, RandomSource, BlockPos)}, and an
 * override of the wrong one would resolve silently.
 *
 * <p>The constructor takes {@code Object} so that every subclass constructor stays one ungated line.
 * What it receives differs by band and neither arm can name the other's type: below 26.3 the registry
 * hands each feature its {@code Codec}, while at 26.3 the {@code MapCodec} constructs one instance
 * per decoded feature and the argument is unused.
 *
 * <p>No {@code ChunkGenerator} parameter is carried because no feature in this mod reads
 * {@code context.chunkGenerator()}.
 */
//? if >=26.3 {
/*public abstract class ACSimpleFeature implements net.minecraft.world.level.levelgen.feature.Feature {

    public ACSimpleFeature(Object unused) {
    }

    @Override
    public com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.levelgen.feature.Feature> codec() {
        return ACFeatureRegistry.codecFor(this.getClass());
    }

    @Override
    public boolean place(net.minecraft.world.level.WorldGenLevel acLevel, net.minecraft.world.level.chunk.ChunkGenerator acChunkGenerator, net.minecraft.util.RandomSource acRandom, net.minecraft.core.BlockPos acOrigin) {
        return this.acPlace(acLevel, acRandom, acOrigin);
    }

    public abstract boolean acPlace(net.minecraft.world.level.WorldGenLevel acLevel, net.minecraft.util.RandomSource acRandom, net.minecraft.core.BlockPos acOrigin);
}
*///?} else {
public abstract class ACSimpleFeature extends net.minecraft.world.level.levelgen.feature.Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> {

    @SuppressWarnings("unchecked")
    public ACSimpleFeature(Object codec) {
        super((com.mojang.serialization.Codec<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>) codec);
    }

    @Override
    public boolean place(net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration> context) {
        return this.acPlace(context.level(), context.random(), context.origin());
    }

    public abstract boolean acPlace(net.minecraft.world.level.WorldGenLevel acLevel, net.minecraft.util.RandomSource acRandom, net.minecraft.core.BlockPos acOrigin);
}
//?}
