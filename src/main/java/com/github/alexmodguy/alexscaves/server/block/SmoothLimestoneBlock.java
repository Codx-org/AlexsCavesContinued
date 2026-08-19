package com.github.alexmodguy.alexscaves.server.block;

import com.github.alexmodguy.alexscaves.server.misc.ACAdvancementTriggerRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import com.google.common.collect.Lists;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import java.util.function.Supplier;

import java.util.List;

public class SmoothLimestoneBlock extends Block {

    private static final List<Supplier<Block>> RANDOM_CAVE_PAINTINGS = Util.make(Lists.newArrayList(), (list) -> {
        list.add(ACBlockRegistry.CAVE_PAINTING_AMBERSOL);
        list.add(ACBlockRegistry.CAVE_PAINTING_DARK);
        list.add(ACBlockRegistry.CAVE_PAINTING_FOOTPRINT);
        list.add(ACBlockRegistry.CAVE_PAINTING_FOOTPRINTS);
        list.add(ACBlockRegistry.CAVE_PAINTING_TREE_STARS);
        list.add(ACBlockRegistry.CAVE_PAINTING_PEWEN);
        list.add(ACBlockRegistry.CAVE_PAINTING_TRILOCARIS);
        list.add(ACBlockRegistry.CAVE_PAINTING_GROTTOCERATOPS);
        list.add(ACBlockRegistry.CAVE_PAINTING_GROTTOCERATOPS_FRIEND);
        list.add(ACBlockRegistry.CAVE_PAINTING_DINO_NUGGETS);
        list.add(ACBlockRegistry.CAVE_PAINTING_VALLUMRAPTOR_CHEST);
        list.add(ACBlockRegistry.CAVE_PAINTING_VALLUMRAPTOR_FRIEND);
        list.add(ACBlockRegistry.CAVE_PAINTING_RELICHEIRUS);
        list.add(ACBlockRegistry.CAVE_PAINTING_RELICHEIRUS_SLASH);
        list.add(ACBlockRegistry.CAVE_PAINTING_ENDERMAN);
        list.add(ACBlockRegistry.CAVE_PAINTING_PORTAL);
        list.add(ACBlockRegistry.CAVE_PAINTING_SUBTERRANODON);
        list.add(ACBlockRegistry.CAVE_PAINTING_SUBTERRANODON_RIDE);
        list.add(ACBlockRegistry.CAVE_PAINTING_TREMORSAURUS);
        list.add(ACBlockRegistry.CAVE_PAINTING_TREMORSAURUS_FRIEND);
    });

    public SmoothLimestoneBlock(Properties properties) {
        super(properties);
    }

    // 1.20.5 split BlockBehaviour#use into useItemOn and useWithoutItem; this block's rule is
    // item-driven, so it hangs off useItemOn there. The body below is shared — only the entry point
    // and the "we did nothing" return differ. See ACCompat#itemResult.
    //? if >=1.21.2 {
    /*protected net.minecraft.world.InteractionResult useItemOn(ItemStack usedStack, BlockState state, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        return ACCompat.itemResult(acUse(state, level, blockPos, player, interactionHand, blockHitResult));
    }
    *///?} elif >=1.20.5 {
    /*protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack usedStack, BlockState state, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        return ACCompat.itemResult(acUse(state, level, blockPos, player, interactionHand, blockHitResult));
    }
    *///?} else {
    public InteractionResult use(BlockState state, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        InteractionResult acResult = acUse(state, level, blockPos, player, interactionHand, blockHitResult);
        return acResult == InteractionResult.PASS ? super.use(state, level, blockPos, player, interactionHand, blockHitResult) : acResult;
    }
    //?}

    private InteractionResult acUse(BlockState state, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        ItemStack itemstack = player.getItemInHand(interactionHand);
        if (itemstack.is(Items.CHARCOAL) && level.getBlockState(blockPos).is(ACTagRegistry.TURNS_INTO_CAVE_PAINTINGS)) {
            if (!player.isCreative()) {
                itemstack.shrink(1);
            }
            if (!level.isClientSide()) {
                boolean isMystery = false;
                if(level.getRandom().nextFloat() < 0.3F && attemptPlaceMysteryCavePainting(level, blockPos, blockHitResult.getDirection(), true)){
                    isMystery = attemptPlaceMysteryCavePainting(level, blockPos, blockHitResult.getDirection(), false);
                }
                if(!isMystery){
                    BlockState cavePainting = Util.getRandom(RANDOM_CAVE_PAINTINGS, player.getRandom()).get().defaultBlockState();
                    level.setBlockAndUpdate(blockPos, cavePainting.setValue(CavePaintingBlock.FACING, blockHitResult.getDirection()));
                }
                if(player instanceof ServerPlayer serverPlayer){
                    if(isMystery){
                        ACAdvancementTriggerRegistry.MYSTERY_CAVE_PAINTING.triggerForEntity(serverPlayer);
                    }else{
                        ACAdvancementTriggerRegistry.CAVE_PAINTING.triggerForEntity(serverPlayer);
                    }
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, blockPos, itemstack);
                }
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, blockPos);
            }
            return InteractionResult.SUCCESS;

        }

        return InteractionResult.PASS;
    }

    private boolean attemptPlaceMysteryCavePainting(Level level, BlockPos pos, Direction facing, boolean checkOnly){
        for(int i = -1; i <= 1; i++){
            for(int j = -1; j <= 1; j++){
                BlockPos paintingPos;
                if(facing == Direction.DOWN){
                    paintingPos = pos.relative(Direction.SOUTH, i).relative(Direction.WEST, j);
                }else if(facing == Direction.UP){
                    paintingPos = pos.relative(Direction.NORTH, i).relative(Direction.WEST, j);
                }else{
                    paintingPos = pos.above(i).relative(facing.getClockWise(), j);
                }
                if(!level.getBlockState(paintingPos).is(ACBlockRegistry.SMOOTH_LIMESTONE.get())){
                    return false;
                }else if(!checkOnly){
                    BlockState cavePainting = getMysteryCavePainting(i, j).defaultBlockState();
                    level.setBlockAndUpdate(paintingPos, cavePainting.setValue(CavePaintingBlock.FACING, facing));
                }
            }
        }
        return true;
    }

    private Block getMysteryCavePainting(int i, int j) {
        if(i == -1 && j == -1){
            return ACBlockRegistry.CAVE_PAINTING_MYSTERY_9.get();
        }else if(i == -1 && j == 0){
            return ACBlockRegistry.CAVE_PAINTING_MYSTERY_8.get();
        }else if(i == -1 && j == 1){
            return ACBlockRegistry.CAVE_PAINTING_MYSTERY_7.get();
        }else if(i == 0 && j == -1){
            return ACBlockRegistry.CAVE_PAINTING_MYSTERY_6.get();
        }else if(i == 0 && j == 0){
            return ACBlockRegistry.CAVE_PAINTING_MYSTERY_5.get();
        }else if(i == 0 && j == 1){
            return ACBlockRegistry.CAVE_PAINTING_MYSTERY_4.get();
        }else if(i == 1 && j == -1){
            return ACBlockRegistry.CAVE_PAINTING_MYSTERY_3.get();
        }else if(i == 1 && j == 0){
            return ACBlockRegistry.CAVE_PAINTING_MYSTERY_2.get();
        }else if(i == 1 && j == 1){
            return ACBlockRegistry.CAVE_PAINTING_MYSTERY_1.get();
        }
        return ACBlockRegistry.CAVE_PAINTING_DARK.get();
    }

}
