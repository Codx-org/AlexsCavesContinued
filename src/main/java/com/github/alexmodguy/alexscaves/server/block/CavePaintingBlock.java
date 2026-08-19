package com.github.alexmodguy.alexscaves.server.block;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;

public class CavePaintingBlock extends DirectionalBlock {

    // 1.20.3 made Block#codec() abstract for datapack-defined blocks; Alex's Caves' blocks
    // are never described by value, so they all share one placeholder. See ACPlatform.
    //? if >=1.20.3 {
    /*@Override
    public com.mojang.serialization.MapCodec<? extends CavePaintingBlock> codec() {
        return com.github.alexmodguy.alexscaves.server.misc.ACPlatform.unsupportedBlockCodec();
    }
    *///?}

    private static String id = Util.makeDescriptionId("block", ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "cave_painting"));

    public CavePaintingBlock() {
        super(paintingProperties());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    // All thirty painting variants are one class registered thirty times, and they deliberately
    // share a single translation key — the block name is "Cave Painting" whichever picture it is.
    // Up to 1.21.1 that was an override of getDescriptionId(); 1.21.2 made that method final over a
    // field the Properties compute, and gave the builder overrideDescription() to say the same
    // thing. Same key either way, so nothing user-visible moves.
    private static BlockBehaviour.Properties paintingProperties() {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_YELLOW).requiresCorrectToolForDrops().strength(1.2F, 4.5F).sound(SoundType.DRIPSTONE_BLOCK);
        //? if >=1.21.2
        /*properties = properties.overrideDescription(id);*/
        return properties;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> blockStateBuilder) {
        blockStateBuilder.add(FACING);
    }

    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    //? if <1.21.2 {
    public String getDescriptionId() {
        return id;
    }
    //?}

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }
}