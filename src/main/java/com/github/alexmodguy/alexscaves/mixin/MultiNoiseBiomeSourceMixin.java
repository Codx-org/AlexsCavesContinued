package com.github.alexmodguy.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationConfig;
import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationNoiseCondition;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRarity;
import com.github.alexmodguy.alexscaves.server.level.biome.BiomeSourceAccessor;
import com.github.alexmodguy.alexscaves.server.level.biome.MultiNoiseBiomeSourceAccessor;
import com.github.alexmodguy.alexscaves.server.misc.VoronoiGenerator;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Places this mod's cave biomes into any {@code MultiNoiseBiomeSource}, by asking
 * {@link ACBiomeRarity} whether the sampled quart falls inside one of its Voronoi cells.
 *
 * <p>That question needs the world seed and the dimension, and {@code getNoiseBiome} is handed
 * neither — so the source has to remember which level it belongs to. It used to learn that
 * <em>only</em> from {@code ChunkStatusMixin}, i.e. only while terrain was being generated, which
 * left two holes: on a world whose chunks all exist already (a re-joined singleplayer world, a
 * pre-generated server) nothing had generated this session, so the seed was still {@code 0} and the
 * dimension {@code null}; and with more than one dimension loaded the fields held whichever level
 * generated last, so an overworld query could be answered against the nether's key. Both make the
 * source place its biomes on a different layout than {@link ACBiomeRarity} does for anyone asking
 * the rarity question directly — which is what a cave map does, and why the map reported the biome
 * as absent while {@code /locate biome} still returned a position.
 *
 * <p>So the source now resolves its own level once, the first time it is asked: the running server
 * is walked for the level whose chunk generator holds <em>this</em> biome source. That is
 * authoritative and cannot go stale, and {@code ChunkStatusMixin} remains as the fast path that
 * fills the fields in before the server is reachable (and for any biome source no level owns).
 */
@Mixin(value = MultiNoiseBiomeSource.class, priority = -69420)
public class MultiNoiseBiomeSourceMixin implements MultiNoiseBiomeSourceAccessor {

    private long lastSampledWorldSeed;

    private ResourceKey<Level> lastSampledDimension;

    /** Set once the owning level has been found; from then on the two fields above are frozen. */
    @Unique
    private boolean ac_resolvedOwningLevel;

    /**
     * The placement question itself, lifted out of the injection handler so both arms below can ask
     * it. Returns the biome to substitute, or null to leave the source's own answer alone.
     *
     * <p>The loop deliberately does not stop at the first match: the original handler called
     * {@code setReturnValue} once per matching condition and the last one won, so this keeps
     * assigning rather than returning early.
     */
    @Unique
    private Holder<Biome> ac_rareBiomeAt(int x, int y, int z, Climate.Sampler sampler) {
        ac_resolveOwningLevel();
        VoronoiGenerator.VoronoiInfo voronoiInfo = ACBiomeRarity.getRareBiomeInfoForQuad(lastSampledWorldSeed, x, z);
        if (voronoiInfo == null) {
            return null;
        }
        Holder<Biome> found = null;
        float unquantizedDepth = Climate.unquantizeCoord(sampler.sample(x, y, z).depth());
        int foundRarityOffset = ACBiomeRarity.getRareBiomeOffsetId(voronoiInfo);
        for (Map.Entry<ResourceKey<Biome>, BiomeGenerationNoiseCondition> condition : BiomeGenerationConfig.BIOMES.entrySet()) {
            if (foundRarityOffset == condition.getValue().getRarityOffset() && condition.getValue().test(x, y, z, unquantizedDepth, sampler, lastSampledDimension, voronoiInfo)) {
                found = ((BiomeSourceAccessor)this).getResourceKeyMap().get(condition.getKey());
            }
        }
        return found;
    }

    // 26.3 pulled the lookup off the source: there is no getNoiseBiome(int,int,int,Sampler) to inject
    // into any more. The source hands out a BiomeResolver instead, whose own lookup takes no sampler
    // at all — so above 26.3 the injection moves to the factory and wraps the resolver it returns,
    // capturing the sampler the placement question still needs. Same question, same answer, asked one
    // level further out.
    //
    // There are TWO factories, and wrapping only the obvious one places no biomes at all. Chunk fill
    // goes ChunkGenerator#doCreateBiomes -> BiomeSource#createResolverForChunk -> fillBiomesFromNoise,
    // and MultiNoiseBiomeSource OVERRIDES createResolverForChunk without routing through
    // createResolver: it bulk-samples the six climate density functions over the chunk volume into
    // DensityBuffers and hands back a resolver that reads those buffers, so it never asks the source
    // for a position at all. createResolver is what locate, the carvers and ServerLevel's uncached
    // lookup use. Wrapping one and not the other is what made locate report a biome that generation
    // had never placed. Read out of 26.3 bytecode, not from the class names.
    //
    // The wrap deliberately keeps asking the passed Climate.Sampler rather than the buffers:
    // BiomeGenerationNoiseCondition#test samples the Voronoi CELL CENTRE, which is far outside the
    // chunk being filled and so outside the volume those buffers cover.
    //? if >=26.3 {
    /*@Inject(at = @At("RETURN"),
            method = "Lnet/minecraft/world/level/biome/MultiNoiseBiomeSource;createResolver(Lnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/world/level/biome/BiomeResolver;",
            cancellable = true
    )
    private void ac_wrapNoiseBiomeResolver(Climate.Sampler sampler, CallbackInfoReturnable<net.minecraft.world.level.biome.BiomeResolver> cir) {
        cir.setReturnValue(ac_wrapResolver(cir.getReturnValue(), sampler));
    }

    @Inject(at = @At("RETURN"),
            method = "Lnet/minecraft/world/level/biome/MultiNoiseBiomeSource;createResolverForChunk(Lnet/minecraft/world/level/biome/Climate$Sampler;IIIIII)Lnet/minecraft/world/level/biome/BiomeResolver;",
            cancellable = true
    )
    private void ac_wrapChunkBiomeResolver(Climate.Sampler sampler, int quartX, int quartY, int quartZ, int sizeX, int sizeY, int sizeZ, CallbackInfoReturnable<net.minecraft.world.level.biome.BiomeResolver> cir) {
        cir.setReturnValue(ac_wrapResolver(cir.getReturnValue(), sampler));
    }

    @Unique
    private net.minecraft.world.level.biome.BiomeResolver ac_wrapResolver(net.minecraft.world.level.biome.BiomeResolver inner, Climate.Sampler sampler) {
        if (inner == null) {
            return inner;
        }
        return (x, y, z) -> {
            Holder<Biome> rare = ac_rareBiomeAt(x, y, z, sampler);
            return rare != null ? rare : inner.getNoiseBiome(x, y, z);
        };
    }
    *///?} else {
    @Inject(at = @At("HEAD"),
            method = "Lnet/minecraft/world/level/biome/MultiNoiseBiomeSource;getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            cancellable = true
    )
    private void ac_getNoiseBiomeCoords(int x, int y, int z, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
        Holder<Biome> rare = ac_rareBiomeAt(x, y, z, sampler);
        if (rare != null) {
            cir.setReturnValue(rare);
        }
    }
    //?}

    /**
     * Finds the level this biome source belongs to, once. Reading a level's generator is a plain
     * field read on every version in the range, and worldgen workers call this, so a level list
     * caught mid-change is possible: the flag is only set on success, so a failed attempt simply
     * tries again on the next sample.
     */
    @Unique
    private void ac_resolveOwningLevel() {
        if (ac_resolvedOwningLevel) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        try {
            for (ServerLevel serverLevel : server.getAllLevels()) {
                if (serverLevel.getChunkSource().getGenerator().getBiomeSource() == (Object) this) {
                    lastSampledWorldSeed = serverLevel.getSeed();
                    lastSampledDimension = serverLevel.dimension();
                    ac_resolvedOwningLevel = true;
                    return;
                }
            }
        } catch (Exception exception) {
            // A level was added while the list was being walked; the next sample will retry.
        }
    }

    @Override
    public void setLastSampledSeed(long seed) {
        if (!ac_resolvedOwningLevel) {
            lastSampledWorldSeed = seed;
        }
    }

    @Override
    public void setLastSampledDimension(ResourceKey<Level> dimension) {
        if (!ac_resolvedOwningLevel) {
            lastSampledDimension = dimension;
        }
    }

    @Override
    public long getLastSampledSeed() {
        return lastSampledWorldSeed;
    }

    @Override
    public ResourceKey<Level> getLastSampledDimension() {
        return lastSampledDimension;
    }
}
