package com.github.alexmodguy.alexscaves.mixin;

/**
 * Keeps vanilla's multiface growth (glow lichen and friends) out of the Abyssal Chasm.
 *
 * <p>26.3 rebuilt worldgen features as a registry of {@code MapCodec}s: {@code Feature} is now an
 * interface, every feature is a record implementing it, and the single {@code FeaturePlaceContext}
 * parameter was unpacked back into {@code place(WorldGenLevel, ChunkGenerator, RandomSource,
 * BlockPos)}. A mixin names its target by descriptor, so that rename cannot be a replacement rule —
 * the two arms below carry the two descriptors, verified with {@code javap -s} against the 26.3 jar
 * rather than composed by hand.
 *
 * <p>The handler body is the same test either way; only how it reaches the level and the origin
 * changes, which is why {@code FeaturePositionValidator.isBiome} grew a
 * {@code (WorldGenLevel, BlockPos, ...)} overload that is valid on all 60 nodes.
 */
//? if >=26.3 {
/*import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import com.github.alexmodguy.alexscaves.server.level.feature.FeaturePositionValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.MultifaceGrowthFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultifaceGrowthFeature.class)
public class MultifaceGrowthFeatureMixin {

    @Inject(
            method = {"Lnet/minecraft/world/level/levelgen/feature/MultifaceGrowthFeature;place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"},
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private void ac_place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin, CallbackInfoReturnable<Boolean> cir) {
        if (FeaturePositionValidator.isBiome(level, origin, ACBiomeRegistry.ABYSSAL_CHASM)) {
            cir.cancel();
        }
    }
}
*///?} else {
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import com.github.alexmodguy.alexscaves.server.level.feature.FeaturePositionValidator;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.MultifaceGrowthFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultifaceGrowthFeature.class)
public class MultifaceGrowthFeatureMixin {

    @Inject(
            method = {"Lnet/minecraft/world/level/levelgen/feature/MultifaceGrowthFeature;place(Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z"},
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private void ac_place(FeaturePlaceContext context, CallbackInfoReturnable<Boolean> cir) {
        if (FeaturePositionValidator.isBiome(context, ACBiomeRegistry.ABYSSAL_CHASM)) {
            cir.cancel();
        }
    }
}
//?}
