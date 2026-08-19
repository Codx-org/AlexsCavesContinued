package com.github.alexmodguy.alexscaves.server.block;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

public class CandyCaneBlock extends RotatedPillarBlock {

    public CandyCaneBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F, 3.0F).sound(ACSoundTypes.HARD_CANDY));
    }

    public BlockState getToolModifiedState(BlockState state, UseOnContext context, ToolAction toolAction, boolean simulate) {
        ItemStack itemStack = context.getItemInHand();
        if (!com.github.alexmodguy.alexscaves.server.misc.ACCompat.canPerform(itemStack, toolAction))
            return null;
        if (ToolActions.AXE_STRIP == toolAction && (this == ACBlockRegistry.CANDY_CANE_BLOCK.get() || this == ACBlockRegistry.CHISELED_CANDY_CANE_BLOCK.get())) {
            return ACBlockRegistry.STRIPPED_CANDY_CANE_BLOCK.get().defaultBlockState().setValue(AXIS, state.getValue(AXIS));
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
