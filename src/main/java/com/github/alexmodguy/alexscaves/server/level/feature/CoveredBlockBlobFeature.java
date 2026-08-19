package com.github.alexmodguy.alexscaves.server.level.feature;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.github.alexmodguy.alexscaves.server.level.feature.config.CoveredBlockBlobConfiguration;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class CoveredBlockBlobFeature extends Feature<CoveredBlockBlobConfiguration> {

    public CoveredBlockBlobFeature(Codec<CoveredBlockBlobConfiguration> config) {
        super(config);
    }

    public boolean place(FeaturePlaceContext<CoveredBlockBlobConfiguration> context) {
        BlockPos blockpos = context.origin();
        WorldGenLevel worldgenlevel = context.level();
        RandomSource randomsource = context.random();

        CoveredBlockBlobConfiguration blockstateconfiguration;
        for (blockstateconfiguration = context.config(); blockpos.getY() > worldgenlevel.getMinBuildHeight() + 3; blockpos = blockpos.below()) {
            if (!worldgenlevel.isEmptyBlock(blockpos.below())) {
                BlockState blockstate = worldgenlevel.getBlockState(blockpos.below());
                // 26 deleted Feature#isDirt and Feature#isStone — vanilla's own BlockBlobFeature asks
                // its configuration for a BlockPredicate now. Giving this mod's covered-blob
                // configuration such a field would change the feature's data format on all 58 nodes
                // for no gain, so the two predicates are spelled out at exactly the meaning the
                // deleted helpers carried. isStone was a Forge patch reading Tags.Blocks.STONES where
                // vanilla read BlockTags.BASE_STONE_OVERWORLD; both name the same six overworld stones
                // (stone, granite, diorite, andesite, tuff, deepslate), and the vanilla tag needs no
                // loader — which is also what the Fabric nodes will want. Inline rather than an
                // ACCompat shim because Feature#isStone is protected: only a subclass can call it.
                //? if >=26 {
                /*if (blockstate.is(net.minecraft.tags.BlockTags.DIRT) || blockstate.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)) {
                *///?} else {
                if (isDirt(blockstate) || isStone(blockstate)) {
                //?}
                    break;
                }
            }
        }

        if (blockpos.getY() <= worldgenlevel.getMinBuildHeight() + 3) {
            return false;
        } else {
            for (int l = 0; l < 3; ++l) {
                int i = randomsource.nextInt(2);
                int j = randomsource.nextInt(2);
                int k = randomsource.nextInt(2);
                float f = (float) (i + j + k) * 0.333F + 0.5F;
                double radius = (double) (f * f);
                for (BlockPos blockpos1 : BlockPos.betweenClosed(blockpos.offset(-i, -j, -k), blockpos.offset(i, j, k))) {
                    if (blockpos1.distSqr(blockpos) <= radius) {
                        worldgenlevel.setBlock(blockpos1, ACCompat.providerState(worldgenlevel, blockstateconfiguration.block, randomsource, blockpos1), 3);
                        BlockPos blockpos2 = blockpos1.above();
                        if (blockpos2.distSqr(blockpos) > radius && worldgenlevel.getBlockState(blockpos2).isAir()) {
                            worldgenlevel.setBlock(blockpos2, ACCompat.providerState(worldgenlevel, blockstateconfiguration.coverBlock, randomsource, blockpos2), 3);
                        }
                    }
                }

                blockpos = blockpos.offset(-1 + randomsource.nextInt(2), -randomsource.nextInt(2), -1 + randomsource.nextInt(2));
            }

            return true;
        }
    }
}