package com.github.alexmodguy.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.level.biome.MultiNoiseBiomeSourceAccessor;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Remembers which world last asked for terrain, so the mod's biome source can answer
 * {@code getLastSampledSeed}/{@code Dimension} — it is consulted from places that have no level.
 *
 * <p>{@code ChunkStatus#generate} has had two shapes. Up to 1.20.4 it took the level, the generator
 * and the light engine as loose arguments and returned an {@code Either} carrying a
 * {@code ChunkHolder.ChunkLoadingFailure}. 1.20.5 collected the first three into the
 * {@code WorldGenContext} record, replaced the {@code Function} callback with {@code ToFullChunk},
 * and dropped the {@code Either} for a plain {@code ChunkAccess} — every argument and the return
 * type changed, so the injector exists twice and only the two lines that read the level are shared.
 *
 * <p>1.21 then took generation off {@code ChunkStatus} entirely: the pipeline is a list of
 * {@code ChunkStep}s, each holding a {@code ChunkStatusTask}, and the one place every step passes
 * through is {@code ChunkStep#apply}. So from 1.21 the mixin targets a different class — the target is
 * gated on the annotation, because {@code alexscaves.mixins.json} is JSON and cannot be gated at all.
 * {@code WorldGenContext} still carries the level and the generator, so the shared body is unchanged.
 *
 * <p>The {@code method=} spec deliberately carries no owner prefix. Upstream wrote one, and it named
 * {@code net/minecraft/world/level/chunk/ChunkStatus} — a path the class left in 1.20.2 when it moved
 * into the {@code chunk.status} package, which would make the spec match nothing from 1.20.2 onward.
 */
//? if >=1.21 {
/*@Mixin(net.minecraft.world.level.chunk.status.ChunkStep.class)
*///?} else {
@Mixin(ChunkStatus.class)
//?}
public class ChunkStatusMixin {

    //? if >=1.21 {
    /*@Inject(at = @At("HEAD"),
            method = "apply(Lnet/minecraft/world/level/chunk/status/WorldGenContext;Lnet/minecraft/util/StaticCache2D;Lnet/minecraft/world/level/chunk/ChunkAccess;)Ljava/util/concurrent/CompletableFuture;")
    private void ac_fillFromNoise(net.minecraft.world.level.chunk.status.WorldGenContext worldGenContext, net.minecraft.util.StaticCache2D<net.minecraft.server.level.GenerationChunkHolder> cache, ChunkAccess chunk, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        ac_rememberSampledLevel(worldGenContext.generator(), worldGenContext.level());
    }
    *///?} elif >=1.20.5 {
    /*@Inject(at = @At("HEAD"),
            method = "generate(Lnet/minecraft/world/level/chunk/status/WorldGenContext;Ljava/util/concurrent/Executor;Lnet/minecraft/world/level/chunk/status/ToFullChunk;Ljava/util/List;)Ljava/util/concurrent/CompletableFuture;")
    private void ac_fillFromNoise(net.minecraft.world.level.chunk.status.WorldGenContext worldGenContext, Executor executor, net.minecraft.world.level.chunk.status.ToFullChunk toFullChunk, List<ChunkAccess> chunks, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        ac_rememberSampledLevel(worldGenContext.generator(), worldGenContext.level());
    }
    *///?} else {
    @Inject(at = @At("HEAD"),
            method = "generate(Ljava/util/concurrent/Executor;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;Lnet/minecraft/server/level/ThreadedLevelLightEngine;Ljava/util/function/Function;Ljava/util/List;)Ljava/util/concurrent/CompletableFuture;")
    private void ac_fillFromNoise(Executor p_283276_, ServerLevel serverLevel, ChunkGenerator chunkGenerator, StructureTemplateManager p_281305_, ThreadedLevelLightEngine p_282570_, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> p_283114_, List<ChunkAccess> p_282723_, CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> cir) {
        ac_rememberSampledLevel(chunkGenerator, serverLevel);
    }
    //?}

    @Unique
    private static void ac_rememberSampledLevel(ChunkGenerator chunkGenerator, ServerLevel serverLevel) {
        if (chunkGenerator.getBiomeSource() instanceof MultiNoiseBiomeSourceAccessor multiNoiseBiomeSourceAccessor) {
            multiNoiseBiomeSourceAccessor.setLastSampledSeed(serverLevel.getSeed());
            multiNoiseBiomeSourceAccessor.setLastSampledDimension(serverLevel.dimension());
        }
    }
}
