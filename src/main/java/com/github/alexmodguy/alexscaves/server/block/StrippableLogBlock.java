package com.github.alexmodguy.alexscaves.server.block;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

public class StrippableLogBlock extends RotatedPillarBlock {

    public StrippableLogBlock(Properties properties) {
        super(properties);
    }

    public BlockState getToolModifiedState(BlockState state, UseOnContext context, ToolAction toolAction, boolean simulate) {
        ItemStack itemStack = context.getItemInHand();
        if (!com.github.alexmodguy.alexscaves.server.misc.ACCompat.canPerform(itemStack, toolAction))
            return null;

        if (ToolActions.AXE_STRIP == toolAction) {
            if(this == ACBlockRegistry.PEWEN_LOG.get()){
                return ACBlockRegistry.STRIPPED_PEWEN_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
            }
            if(this == ACBlockRegistry.PEWEN_WOOD.get()){
                return ACBlockRegistry.STRIPPED_PEWEN_WOOD.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
            }
            if(this == ACBlockRegistry.THORNWOOD_LOG.get()){
                return ACBlockRegistry.STRIPPED_THORNWOOD_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
            }
            if(this == ACBlockRegistry.THORNWOOD_WOOD.get()){
                return ACBlockRegistry.STRIPPED_THORNWOOD_WOOD.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
            }
        }
        // Fabric patches no such hook onto Block, so there is no supertype answer to defer to.
        // null is what the other two loaders' own default returns anyway: "this tool changes
        // nothing about this block".
        //? if fabric {
        /*return null;
        *///?} else {
        return super.getToolModifiedState(state, context, toolAction, simulate);
        //?}
    }
}

