package com.github.alexmodguy.alexscaves.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;

// 26.2 gave SurfaceRules$Context a protected getBiome() that memoises the biome holder for the
// current position; ACSurfaceRuleConditionRegistry's own biome condition needs it. The member does
// not exist below 26.2, so the invoker lives in an arm and this is an empty (harmless) mixin
// interface there — an @Invoker naming a missing method is a hard apply failure, and the file is
// listed in alexscaves.mixins.json on every node.
//
// An access transformer entry is the other route and is deliberately not taken: this file's twin,
// META-INF/accesstransformer_mojmap.cfg, is shared by every Mojmap node, and an entry naming an
// existing class but a missing member is a hard error on NeoForge — so a 26.2-only widening cannot
// sit there. The same reasoning already governs mixin.client.CameraAccessor.
@Mixin(SurfaceRules.Context.class)
public interface SurfaceRulesContextAccessor {

    //? if >=26.2 {
    /*@org.spongepowered.asm.mixin.gen.Invoker("getBiome")
    Holder<Biome> ac_callGetBiome();

    // updateY lost its x and z on 26.2 and the constructor took a Set<Holder<Biome>> in place of
    // the biome Registry. Both are protected, and both were reachable before only because the
    // pre-26.2 signatures are named in the access transformers — which cannot gain the new shapes,
    // since a widening for a member that does not exist is fatal on the other Mojmap nodes. A
    // constructor invoker is a static method with a throwaway body that Mixin overwrites.
    @org.spongepowered.asm.mixin.gen.Invoker("updateY")
    void ac_callUpdateY(int stoneDepthAbove, int stoneDepthBelow, int waterHeight, int y);

    @org.spongepowered.asm.mixin.gen.Invoker("<init>")
    static SurfaceRules.Context ac_newContext(net.minecraft.world.level.levelgen.SurfaceSystem system, net.minecraft.world.level.levelgen.RandomState randomState, net.minecraft.world.level.chunk.ChunkAccess chunk, net.minecraft.world.level.levelgen.NoiseChunk noiseChunk, java.util.function.Function<net.minecraft.core.BlockPos, Holder<Biome>> biomeGetter, net.minecraft.world.level.levelgen.WorldGenerationContext context, java.util.Set<Holder<Biome>> possibleBiomes) {
        throw new AssertionError();
    }
    *///?}
}
