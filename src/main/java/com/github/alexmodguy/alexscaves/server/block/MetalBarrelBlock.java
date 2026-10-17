package com.github.alexmodguy.alexscaves.server.block;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.server.block.blockentity.MetalBarrelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class MetalBarrelBlock extends BarrelBlock {

    public MetalBarrelBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F).sound(ACSoundTypes.SCRAP_METAL));
    }

    // 1.20.5 split BlockBehaviour#use into useItemOn and useWithoutItem. Vanilla calls useItemOn
    // for every hand and every stack -- the empty one included -- and only falls through to
    // useWithoutItem when it answers "did nothing", so hanging the whole rule off useItemOn keeps
    // this block reachable with a full hotbar exactly as it was below 1.20.5. The body is shared;
    // only the entry point and the "we did nothing" return differ. See ACCompat#itemResult.
    //? if >=1.21.2 {
    /*protected net.minecraft.world.InteractionResult useItemOn(net.minecraft.world.item.ItemStack usedStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult result) {
        return com.github.alexmodguy.alexscaves.server.misc.ACCompat.itemResult(acUse(blockState, level, blockPos, player, hand, result));
    }
    *///?} elif >=1.20.5 {
    /*protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack usedStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult result) {
        return com.github.alexmodguy.alexscaves.server.misc.ACCompat.itemResult(acUse(blockState, level, blockPos, player, hand, result));
    }
    *///?} else {
    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult result) {
        InteractionResult acResult = acUse(blockState, level, blockPos, player, hand, result);
        return acResult == InteractionResult.PASS ? super.use(blockState, level, blockPos, player, hand, result) : acResult;
    }
    //?}

    private InteractionResult acUse(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult result) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        } else {
            BlockEntity blockentity = level.getBlockEntity(blockPos);
            if (blockentity instanceof MetalBarrelBlockEntity) {
                player.openMenu((MetalBarrelBlockEntity) blockentity);
                player.awardStat(Stats.OPEN_BARREL);
                ACCompat.angerNearbyPiglins(player, true);
            }
            return InteractionResult.CONSUME;
        }
    }

    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource randomSource) {
        BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity instanceof MetalBarrelBlockEntity) {
            ((MetalBarrelBlockEntity) blockentity).recheckOpen();
        }
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState blockState) {
        return new MetalBarrelBlockEntity(pos, blockState);
    }

    // See GingerbarrelBlock: the custom name is copied by BlockItem from 1.20.5 on.
    //? if <1.20.5 {
    public void setPlacedBy(Level level, BlockPos pos, BlockState blockState, @Nullable LivingEntity entity, ItemStack stack) {
        if (ACCompat.hasCustomHoverName(stack)) {
            BlockEntity blockentity = level.getBlockEntity(pos);
            if (blockentity instanceof MetalBarrelBlockEntity) {
                ((MetalBarrelBlockEntity) blockentity).setCustomName(stack.getHoverName());
            }
        }
    }
    //?}

    // 1.21.5 replaced onRemove with affectNeighborsAfterRemoval, which is handed a ServerLevel,
    // no successor state, and -- critically -- runs AFTER the block entity has been discarded.
    // Dropping the contents is BlockEntity#preRemoveSideEffects now, whose default already does
    // exactly this for any Container (MetalBarrelBlockEntity is a RandomizableContainerBlockEntity), and
    // vanilla removes the block entity itself. Only the comparator update is left to do here.
    // Ungated, this method quietly stopped being called on all 33 nodes from 1.21.5 up.
    //? if >=1.21.5 {
    /*@Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos blockPos, boolean force) {
        level.updateNeighbourForOutputSignal(blockPos, this);
    }
    *///?} else {
    @Override
    public void onRemove(BlockState state, Level level, BlockPos blockPos, BlockState newState, boolean force) {
        if (state.hasBlockEntity() && (!(newState.getBlock() instanceof MetalBarrelBlock) || !newState.hasBlockEntity())) {
            BlockEntity blockentity = level.getBlockEntity(blockPos);
            if (blockentity instanceof Container) {
                Containers.dropContents(level, blockPos, (Container)blockentity);
                level.updateNeighbourForOutputSignal(blockPos, this);
            }

            level.removeBlockEntity(blockPos);
        }
    }
    //?}

}
