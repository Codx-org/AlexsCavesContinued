package com.github.alexmodguy.alexscaves.server.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class DirectionalFacingBlock extends DirectionalBlock {

    // 1.20.3 made Block#codec() abstract for datapack-defined blocks; Alex's Caves' blocks
    // are never described by value, so they all share one placeholder. See ACPlatform.
    //? if >=1.20.3 {
    /*@Override
    public com.mojang.serialization.MapCodec<? extends DirectionalFacingBlock> codec() {
        return com.github.alexmodguy.alexscaves.server.misc.ACPlatform.unsupportedBlockCodec();
    }
    *///?}

    private final boolean facesPlayer;

    protected DirectionalFacingBlock(Properties properties) {
        this(properties, false);
    }

    protected DirectionalFacingBlock(Properties properties, boolean facesPlayer) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
        this.facesPlayer = facesPlayer;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> stateBuilder) {
        stateBuilder.add(FACING);
    }

    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, facesPlayer ? context.getNearestLookingDirection().getOpposite() : context.getClickedFace());
    }
}
