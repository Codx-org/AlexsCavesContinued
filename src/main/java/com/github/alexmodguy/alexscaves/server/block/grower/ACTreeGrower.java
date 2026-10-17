package com.github.alexmodguy.alexscaves.server.block.grower;

import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.jetbrains.annotations.Nullable;

/**
 * Alex's Caves' own sapling-growing contract, replacing vanilla's grower type.
 *
 * <p>Upstream's four growers extended {@code AbstractTreeGrower}. 1.20.3 deleted that class: growing
 * became data-driven through a <b>final</b> {@code TreeGrower} record that can only be configured
 * with feature keys, so a subclass is no longer possible — and {@code AncientTreeGrower} needs
 * behaviour the record cannot express anyway (vanilla's mega-sapling detection is hard-coded to
 * 2×2; the ancient tree is 3×3).
 *
 * <p>So the mod stops using vanilla's grower entirely. {@link
 * com.github.alexmodguy.alexscaves.server.block.CaveSaplingBlock} overrides {@code advanceTree} and
 * calls one of these instead, which keeps every version on identical growth logic and takes the
 * whole family out of the blast radius of future churn in that corner of vanilla.
 */
public interface ACTreeGrower {

    /** The feature this sapling grows into, or null to not grow at all. */
    @Nullable
    ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource randomSource);

    /**
     * Replaces the sapling with its tree feature. Mirrors vanilla's own sequence: clear the sapling
     * to air first so the feature can occupy that block, and put it back if the feature declines to
     * generate (usually because there is no headroom).
     */
    default boolean growTree(ServerLevel serverLevel, ChunkGenerator chunkGenerator, BlockPos blockPos, BlockState state, RandomSource randomSource) {
        Holder<ConfiguredFeature<?, ?>> holder = ACPlatform.configuredFeature(serverLevel, getConfiguredFeature(randomSource));
        holder = ACPlatform.onSaplingGrow(serverLevel, randomSource, blockPos, holder);
        if (holder == null) {
            return false;
        }
        serverLevel.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 4);
        if (holder.value().place(serverLevel, chunkGenerator, randomSource, blockPos)) {
            return true;
        }
        serverLevel.setBlock(blockPos, state, 4);
        return false;
    }
}
