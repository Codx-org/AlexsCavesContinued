package com.github.alexmodguy.alexscaves.mixin;

/**
 * Two unrelated repairs to vanilla's lake feature, which is why this mixin carries two handlers.
 *
 * <p><b>ac_place</b> keeps vanilla lakes out of the Abyssal Chasm.
 *
 * <p><b>ac_unfuzzedLakeBiome</b> fixes a crash that is vanilla's, not ours. A water lake decides
 * where to freeze by asking {@code getBiome} for every column of its 16x16 top layer.
 * {@code getBiome} fuzzes the position through {@code BiomeManager} before reading it, which can
 * move it one noise cell further out — and a lake near the edge of its chunk already reaches into
 * the next one, so the fuzz lands two chunks away. The FEATURES step only holds the chunks directly
 * around the one being decorated, and from 1.21 the region throws "Requested chunk unavailable
 * during world generation" instead of answering. The Primordial Caves' own water lake
 * (primordial_caves_lake) hits it, as does any water lake another mod or datapack adds, and the
 * crash names no one. The unfuzzed noise biome of the same column always sits inside the region,
 * and for a freeze check it is indistinguishable.
 *
 * <p>26.3 rebuilt worldgen features as a registry of {@code MapCodec}s: {@code Feature} is now an
 * interface, every feature is a record implementing it, and the single {@code FeaturePlaceContext}
 * parameter was unpacked back into {@code place(WorldGenLevel, ChunkGenerator, RandomSource,
 * BlockPos)}. A mixin names its target by descriptor, so that rename cannot be a replacement rule —
 * the two arms below carry the two descriptors, verified with {@code javap -s} against the 26.3 jar
 * rather than composed by hand. The {@code @At} target of the second handler is untouched by the
 * rewrite: 26.3's {@code LakeFeature.place} still calls {@code WorldGenLevel#getBiome(BlockPos)},
 * confirmed in that jar's bytecode, so the wrap still has something to wrap.
 */
//? if >=26.3 {
/*import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import com.github.alexmodguy.alexscaves.server.level.feature.FeaturePositionValidator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LakeFeature.class)
public class LakeFeatureMixin {

    @Inject(
            method = {"Lnet/minecraft/world/level/levelgen/feature/LakeFeature;place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"},
            remap = true,
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private void ac_place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin, CallbackInfoReturnable<Boolean> cir) {
        if (FeaturePositionValidator.isBiome(level, origin, ACBiomeRegistry.ABYSSAL_CHASM)) {
            cir.cancel();
        }
    }

    // See the class javadoc for why the fuzzed lookup cannot be used here.
    @WrapOperation(
            method = {"Lnet/minecraft/world/level/levelgen/feature/LakeFeature;place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"},
            remap = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;")
    )
    private Holder<Biome> ac_unfuzzedLakeBiome(WorldGenLevel level, BlockPos pos, Operation<Holder<Biome>> original) {
        return level.getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()));
    }
}
*///?} else {
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import com.github.alexmodguy.alexscaves.server.level.feature.FeaturePositionValidator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LakeFeature.class)
public class LakeFeatureMixin {

    @Inject(
            method = {"Lnet/minecraft/world/level/levelgen/feature/LakeFeature;place(Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z"},
            remap = true,
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private void ac_place(FeaturePlaceContext context, CallbackInfoReturnable<Boolean> cir) {
        if (FeaturePositionValidator.isBiome(context, ACBiomeRegistry.ABYSSAL_CHASM)) {
            cir.cancel();
        }
    }

    // See the class javadoc for why the fuzzed lookup cannot be used here.
    @WrapOperation(
            method = {"Lnet/minecraft/world/level/levelgen/feature/LakeFeature;place(Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z"},
            remap = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;")
    )
    private Holder<Biome> ac_unfuzzedLakeBiome(WorldGenLevel level, BlockPos pos, Operation<Holder<Biome>> original) {
        return level.getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()));
    }
}
//?}
