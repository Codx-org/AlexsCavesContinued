package com.github.alexmodguy.alexscaves.server.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

class FallingBlockWithColor extends FallingBlock {

    // 1.20.3 made Block#codec() abstract for datapack-defined blocks; Alex's Caves' blocks
    // are never described by value, so they all share one placeholder. See ACPlatform.
    //? if >=1.20.3 {
    /*@Override
    public com.mojang.serialization.MapCodec<? extends FallingBlockWithColor> codec() {
        return com.github.alexmodguy.alexscaves.server.misc.ACPlatform.unsupportedBlockCodec();
    }
    *///?}

    private int dustColor;

    public FallingBlockWithColor(BlockBehaviour.Properties properties, int dustColor) {
        super(properties);
        this.dustColor = dustColor;
    }

    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return dustColor;
    }

}