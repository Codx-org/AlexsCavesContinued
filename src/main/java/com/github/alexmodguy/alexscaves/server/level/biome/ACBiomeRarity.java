package com.github.alexmodguy.alexscaves.server.level.biome;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationConfig;
import com.github.alexmodguy.alexscaves.server.misc.VoronoiGenerator;
import com.google.common.collect.ImmutableList;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
//? if <26.3
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

public class ACBiomeRarity {
    private static long lastTestedSeed = 0;
    private static final List<Integer> BIOME_OCTAVES = ImmutableList.of(0);
    // 26.3 deleted PerlinSimplexNoise. With the single octave {0} this mod uses, that class was
    // never doing fBm at all: its constructor built exactly one SimplexNoise, skipped the j>0
    // branch that would consume extra RNG, and both frequency factors came out 1.0 — so
    // getValue(x, z, false) was literally simplex.getValue(x, z). The 26.3 replacement is the bare
    // SimplexNoise with the "no offsets" flag, whose constructor delegates to
    // GradientNoise(rng, flag ? 0.0 : 256.0): it consumes the same three nextDouble() calls and
    // runs the identical nextInt(256 - i) permutation loop, and the 16-row gradient table is
    // byte-identical to 26.2's, so the permutation table and the sampled field match for a given
    // seed. The only difference is the return type, float instead of double.
    //? if >=26.3 {
    /*private static final net.minecraft.world.level.levelgen.synth.SimplexNoise NOISE_X = new net.minecraft.world.level.levelgen.synth.SimplexNoise(new XoroshiroRandomSource(1234L), true);
    private static final net.minecraft.world.level.levelgen.synth.SimplexNoise NOISE_Z = new net.minecraft.world.level.levelgen.synth.SimplexNoise(new XoroshiroRandomSource(4321L), true);
    *///?} else {
    private static final PerlinSimplexNoise NOISE_X = new PerlinSimplexNoise(new XoroshiroRandomSource(1234L), BIOME_OCTAVES);
    private static final PerlinSimplexNoise NOISE_Z = new PerlinSimplexNoise(new XoroshiroRandomSource(4321L), BIOME_OCTAVES);
    //?}
    private static final VoronoiGenerator VORONOI_GENERATOR = new VoronoiGenerator(42L);

    private static double biomeSize;
    private static double seperationDistance;

    /**
     * Called after config reset
     */
    public static void init() {
        VORONOI_GENERATOR.setOffsetAmount(AlexsCaves.COMMON_CONFIG.caveBiomeSpacingRandomness.get());
        biomeSize = AlexsCaves.COMMON_CONFIG.caveBiomeMeanWidth.get() * 0.25D;
        seperationDistance = biomeSize + AlexsCaves.COMMON_CONFIG.caveBiomeMeanSeparation.get() * 0.25D;
    }

    /**
     * Does the heavy lifting of finding if x and z quads should contain a rare biome.
     *
     * @param worldSeed seed of the world for Voroni generation
     * @param x         xQuad being tested
     * @param z         zQuad being tested
     * @return the voroni info if a rare biome is present
     */
    @Nullable
    public static VoronoiGenerator.VoronoiInfo getRareBiomeInfoForQuad(long worldSeed, int x, int z) {
        //start of code to initialize noise for world
        VORONOI_GENERATOR.setSeed(worldSeed);
        double sampleX = x / seperationDistance;
        double sampleZ = z / seperationDistance;
        //? if >=26.3 {
        /*double positionOffsetX = AlexsCaves.COMMON_CONFIG.caveBiomeWidthRandomness.get() * NOISE_X.get(sampleX, sampleZ);
        double positionOffsetZ = AlexsCaves.COMMON_CONFIG.caveBiomeWidthRandomness.get() * NOISE_Z.get(sampleX, sampleZ);
        *///?} else {
        double positionOffsetX = AlexsCaves.COMMON_CONFIG.caveBiomeWidthRandomness.get() * NOISE_X.getValue(sampleX, sampleZ, false);
        double positionOffsetZ = AlexsCaves.COMMON_CONFIG.caveBiomeWidthRandomness.get() * NOISE_Z.getValue(sampleX, sampleZ, false);
        //?}
        VoronoiGenerator.VoronoiInfo info = VORONOI_GENERATOR.get2(sampleX + positionOffsetX, sampleZ + positionOffsetZ);
        if (info.distance() < (biomeSize / seperationDistance)) {
            return info;
        } else {
            return null;
        }
    }

    /**
     * gets the center of the biome pertaining to the voronoiInfo. Result is in quad coordinates.
     *
     * @param voronoiInfo the info of the sampled biome
     * @return a coordinate
     */
    @Nullable
    public static Vec3 getRareBiomeCenter(VoronoiGenerator.VoronoiInfo voronoiInfo) {
        return voronoiInfo.cellPos().scale(seperationDistance);
    }

    /**
     * gets the 'rarityOffset' for voroniInfo, which is essentially an internal biome ID.
     *
     * @param voronoiInfo the info of the sampled biome
     * @return the rarityOffset of the info
     */
    @Nullable
    public static int getRareBiomeOffsetId(VoronoiGenerator.VoronoiInfo voronoiInfo) {
        return (int) (((voronoiInfo.hash() + 1D) * 0.5D) * (double) BiomeGenerationConfig.getBiomeCount());
    }

    public static boolean isQuartInRareBiome(long worldSeed, int x, int z) {
        return ACBiomeRarity.getRareBiomeInfoForQuad(worldSeed, x, z) != null;
    }
}
