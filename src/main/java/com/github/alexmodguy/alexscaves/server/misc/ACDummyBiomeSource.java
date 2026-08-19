package com.github.alexmodguy.alexscaves.server.misc;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

public class ACDummyBiomeSource extends BiomeSource {


    // A BiomeSource describes itself with a MapCodec from 1.20.5 on, so that a dimension's JSON can
    // inline its fields rather than nesting them. This source is never serialised — see below.
    //? if >=1.20.5 {
    /*@Override
    protected com.mojang.serialization.MapCodec<? extends BiomeSource> codec() {
        return null;
    }
    *///?} else {
    @Override
    protected Codec<? extends BiomeSource> codec() {
        return null;
    }
    //?}

    // Biomes are a datapack registry, so there is no loader-neutral static handle to enumerate them
    // (upstream reached for Forge's ForgeRegistries.BIOMES, which has no NeoForge or Fabric twin).
    // Nothing here needs the list: this source exists only to satisfy NoiseBasedChunkGenerator's
    // constructor in ConversionCrucibleBlockEntity, which then samples noise — codec() and
    // getNoiseBiome() are both already `return null`.
    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.empty();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int p_204238_, int p_204239_, int p_204240_, Climate.Sampler p_204241_) {
        return null;
    }
}
