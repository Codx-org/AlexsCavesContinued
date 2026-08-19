package com.github.alexmodguy.alexscaves.server.block;

import com.github.alexmodguy.alexscaves.server.block.grower.ACTreeGrower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Every Alex's Caves sapling. It keeps vanilla's {@code SaplingBlock} for the shape, the
 * {@code STAGE} property and the bonemeal plumbing, but grows through the mod's own
 * {@link ACTreeGrower} rather than the vanilla grower handed to {@code super} — see
 * {@code ACTreeGrower} for why the vanilla one is unusable here.
 *
 * <p>The grower passed up to {@code super} is therefore a placeholder that is never consulted:
 * {@link #advanceTree} is overridden and never reaches {@code SaplingBlock}'s own copy. Its type is
 * the one thing that must be version-gated, because 1.20.3 replaced {@code AbstractTreeGrower} with
 * the final {@code TreeGrower}.
 */
public class CaveSaplingBlock extends SaplingBlock {

    private final ACTreeGrower caveTreeGrower;
    private final boolean growsNaturally;

    public CaveSaplingBlock(ACTreeGrower grower, Properties properties, boolean growsNaturally) {
        //? if >=1.20.3 {
        /*super(net.minecraft.world.level.block.grower.TreeGrower.OAK, properties);
        *///?} else {
        super(new net.minecraft.world.level.block.grower.OakTreeGrower(), properties);
        //?}
        this.caveTreeGrower = grower;
        this.growsNaturally = growsNaturally;
    }

    @Override
    public void advanceTree(ServerLevel serverLevel, BlockPos blockPos, BlockState blockState, RandomSource randomSource) {
        // Vanilla's own sequence: the first growth step only advances STAGE; the second grows.
        if (blockState.getValue(STAGE) == 0) {
            serverLevel.setBlock(blockPos, blockState.cycle(STAGE), 4);
        } else {
            caveTreeGrower.growTree(serverLevel, serverLevel.getChunkSource().getGenerator(), blockPos, blockState, randomSource);
        }
    }

    @Override
    public void randomTick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
        if (growsNaturally) {
            super.randomTick(blockState, serverLevel, blockPos, randomSource);
        }
    }

    @Override
    protected boolean mayPlaceOn(BlockState blockState, BlockGetter getter, BlockPos pos) {
        return blockState.isFaceSturdy(getter, pos, Direction.UP, SupportType.FULL);
    }
}
