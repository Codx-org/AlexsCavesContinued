package com.github.alexmodguy.alexscaves.mixin;

/**
 * Reaches the non-public members of the surface-rule context that the Conversion Crucible needs in
 * order to ask a rule source what block a biome would put at a given column.
 *
 * <p>An access transformer entry is the other route and is deliberately not taken, though NOT for
 * the reason this comment used to give. It claimed an entry naming an existing class but a missing
 * member is a hard error on NeoForge; that was measured false on 2026-09-17 — the shipped
 * 26.2-neoforge accesstransformer.cfg carries 8 entries that resolve to nothing on that node,
 * Boat#DATA_ID_TYPE and this very class's updateY(IIIIII)V among them, and it has booted every
 * release since 1.0.0. The real reason to keep it here is that the shape differs per version in
 * three ways at once (target class, constructor parameters, which members are already public), and
 * a whole-file gate keeps all of that in one place beside the call site, where an AT — a flat union
 * over all 60 nodes with no gating mechanism of any kind — cannot express it. The same reasoning
 * governs mixin.client.CameraAccessor. The file is
 * listed in alexscaves.mixins.json on every node, so the pre-26.2 arm is an empty (harmless) mixin
 * interface rather than an absent one; an {@code @Invoker} naming a missing method is a hard apply
 * failure, which is why each arm declares only what its own version actually has.
 *
 * <p>26.2 gave the context a {@code getBiome()} that memoises the biome holder for the current
 * position, dropped x and z from {@code updateY}, and took a {@code Set<Holder<Biome>>} in place of
 * the biome {@code Registry}. 26.3 then renamed the class to {@code MaterialRuleContext} and moved
 * it to {@code levelgen.material}, so the {@code @Mixin} target itself differs per arm and this is a
 * whole-file gate rather than a gated member. Two further 26.3 changes shape that arm:
 * {@code getBiome()} became <b>public</b>, so no invoker for it is needed (and declaring one for an
 * already-public method is pointless), and the constructor swapped its {@code ChunkAccess} +
 * {@code NoiseChunk} pair for the {@code DensityVolume} + {@code DensitySamplerSet} that
 * {@code NoiseChunk} now exposes through {@code volume()} and {@code cachingSamplers()}.
 *
 * <p>{@code updateXZ} is only needed as an invoker on 26.3: it is reachable directly below that, and
 * there it grew two surface-gradient ints — see the call site in {@code ConversionCrucibleBlockEntity}
 * for why they are passed as zero.
 */
//? if >=26.3 {
/*import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MaterialRuleContext.class)
public interface SurfaceRulesContextAccessor {

    @org.spongepowered.asm.mixin.gen.Invoker("updateXZ")
    void ac_callUpdateXZ(int x, int z, int surfaceGradientX, int surfaceGradientZ);

    @org.spongepowered.asm.mixin.gen.Invoker("updateY")
    void ac_callUpdateY(int stoneDepthAbove, int stoneDepthBelow, int waterHeight, int y);

    @org.spongepowered.asm.mixin.gen.Invoker("<init>")
    static MaterialRuleContext ac_newContext(net.minecraft.world.level.levelgen.material.MaterialSystem system, net.minecraft.world.level.levelgen.RandomState randomState, net.minecraft.world.level.levelgen.densityfunction.DensityVolume volume, net.minecraft.world.level.levelgen.densityfunction.DensitySamplerSet samplers, java.util.function.Function<net.minecraft.core.BlockPos, net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> biomeGetter, net.minecraft.world.level.levelgen.WorldGenerationContext context, java.util.Set<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> possibleBiomes) {
        throw new AssertionError();
    }
}
*///?} elif >=26.2 {
/*import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SurfaceRules.Context.class)
public interface SurfaceRulesContextAccessor {

    @org.spongepowered.asm.mixin.gen.Invoker("getBiome")
    Holder<Biome> ac_callGetBiome();

    @org.spongepowered.asm.mixin.gen.Invoker("updateY")
    void ac_callUpdateY(int stoneDepthAbove, int stoneDepthBelow, int waterHeight, int y);

    @org.spongepowered.asm.mixin.gen.Invoker("<init>")
    static SurfaceRules.Context ac_newContext(net.minecraft.world.level.levelgen.SurfaceSystem system, net.minecraft.world.level.levelgen.RandomState randomState, net.minecraft.world.level.chunk.ChunkAccess chunk, net.minecraft.world.level.levelgen.NoiseChunk noiseChunk, java.util.function.Function<net.minecraft.core.BlockPos, Holder<Biome>> biomeGetter, net.minecraft.world.level.levelgen.WorldGenerationContext context, java.util.Set<Holder<Biome>> possibleBiomes) {
        throw new AssertionError();
    }
}
*///?} else {
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SurfaceRules.Context.class)
public interface SurfaceRulesContextAccessor {
}
//?}
