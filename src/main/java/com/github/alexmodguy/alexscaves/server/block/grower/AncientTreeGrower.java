package com.github.alexmodguy.alexscaves.server.block.grower;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/**
 * The ancient tree grows a giant variant when nine ancient saplings stand in a 3×3 square — which
 * is why this one cannot be a vanilla grower on any version: vanilla only ever recognised 2×2
 * mega-saplings, and since 1.20.3 the grower type is a final record with no room for the check.
 */
public class AncientTreeGrower implements ACTreeGrower {

    public static final ResourceKey<ConfiguredFeature<?, ?>> ANCIENT_TREE = ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "ancient_tree"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> GIANT_ANCIENT_TREE = ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "giant_ancient_tree"));

    @Override
    public ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource randomSource) {
        return ANCIENT_TREE;
    }

    @Override
    public boolean growTree(ServerLevel serverLevel, ChunkGenerator chunkGenerator, BlockPos blockPos, BlockState state, RandomSource randomSource) {
        // The sapling that was bonemealed can be any of the nine, so try every 3×3 square it could
        // be a part of before falling back to a single tree.
        for (int i = 0; i >= -2; --i) {
            for (int j = 0; j >= -2; --j) {
                if (isThreeByThreeSapling(state, serverLevel, blockPos, i, j)) {
                    return this.placeMega(serverLevel, chunkGenerator, blockPos, state, randomSource, i, j);
                }
            }
        }

        return ACTreeGrower.super.growTree(serverLevel, chunkGenerator, blockPos, state, randomSource);
    }

    public boolean placeMega(ServerLevel serverLevel, ChunkGenerator chunkGenerator, BlockPos blockPos, BlockState blockState, RandomSource randomSource, int x, int z) {
        Holder<ConfiguredFeature<?, ?>> holder = ACPlatform.configuredFeature(serverLevel, GIANT_ANCIENT_TREE);
        holder = ACPlatform.onSaplingGrow(serverLevel, randomSource, blockPos, holder);
        if (holder == null) {
            return false;
        }
        ConfiguredFeature<?, ?> configuredfeature = holder.value();
        BlockState blockstate = Blocks.AIR.defaultBlockState();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                serverLevel.setBlock(blockPos.offset(x + i, 0, z + j), blockstate, 4);
            }
        }
        if (configuredfeature.place(serverLevel, chunkGenerator, randomSource, blockPos.offset(x + 1, 0, z + 1))) {
            return true;
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                serverLevel.setBlock(blockPos.offset(x + i, 0, z + j), blockstate, 4);
            }
        }
        return false;
    }

    public static boolean isThreeByThreeSapling(BlockState state, BlockGetter level, BlockPos pos, int x, int z) {
        Block block = state.getBlock();
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                mutableBlockPos.set(pos.getX() + x + i, pos.getY(), pos.getZ() + z + j);
                if (!level.getBlockState(mutableBlockPos).is(block)) {
                    return false;
                }
            }
        }
        return true;
    }
}
