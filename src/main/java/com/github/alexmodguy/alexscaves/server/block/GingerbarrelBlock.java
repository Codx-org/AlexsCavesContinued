package com.github.alexmodguy.alexscaves.server.block;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.server.block.blockentity.GingerbarrelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class GingerbarrelBlock extends BarrelBlock {

    public static final VoxelShape SHAPE_X = Block.box(3, 0, 4, 13, 8, 12);
    public static final VoxelShape SHAPE_Y_UP = Block.box(4, 0, 4, 12, 10, 12);
    public static final VoxelShape SHAPE_Y_DOWN = Block.box(4, 6, 4, 12, 16, 12);
    public static final VoxelShape SHAPE_Z = Block.box(4, 0, 3, 12, 8, 13);
    public GingerbarrelBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_BROWN).strength(1.5F).sound(ACSoundTypes.DENSE_CANDY).noOcclusion());
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
            if (blockentity instanceof GingerbarrelBlockEntity) {
                player.openMenu((GingerbarrelBlockEntity) blockentity);
                player.awardStat(Stats.OPEN_BARREL);
                ACCompat.angerNearbyPiglins(player, true);
            }
            return InteractionResult.CONSUME;
        }
    }

    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource randomSource) {
        BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity instanceof GingerbarrelBlockEntity) {
            ((GingerbarrelBlockEntity) blockentity).recheckOpen();
        }
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState blockState) {
        return new GingerbarrelBlockEntity(pos, blockState);
    }

    // From 1.20.5 the custom name rides along as an item component and BlockItem copies it onto the
    // block entity by itself — BaseContainerBlockEntity picks it up in applyImplicitComponents — so
    // there is nothing left for this override to do, and no setCustomName to do it with.
    //? if <1.20.5 {
    public void setPlacedBy(Level level, BlockPos pos, BlockState blockState, @Nullable LivingEntity entity, ItemStack stack) {
        if (ACCompat.hasCustomHoverName(stack)) {
            BlockEntity blockentity = level.getBlockEntity(pos);
            if (blockentity instanceof GingerbarrelBlockEntity) {
                ((GingerbarrelBlockEntity) blockentity).setCustomName(stack.getHoverName());
            }
        }
    }
    //?}

    // 1.21.5 replaced onRemove with affectNeighborsAfterRemoval, which is handed a ServerLevel,
    // no successor state, and -- critically -- runs AFTER the block entity has been discarded.
    // Dropping the contents is BlockEntity#preRemoveSideEffects now, whose default already does
    // exactly this for any Container (GingerbarrelBlockEntity is a RandomizableContainerBlockEntity), and
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
        if (state.hasBlockEntity() && (!(newState.getBlock() instanceof GingerbarrelBlock) || !newState.hasBlockEntity())) {
            BlockEntity blockentity = level.getBlockEntity(blockPos);
            if (blockentity instanceof Container) {
                Containers.dropContents(level, blockPos, (Container)blockentity);
                level.updateNeighbourForOutputSignal(blockPos, this);
            }

            level.removeBlockEntity(blockPos);
        }
    }
    //?}

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        switch (state.getValue(FACING).getAxis()){
            case X:
                return SHAPE_X;
            case Y:
                return state.getValue(FACING) == Direction.UP ? SHAPE_Y_UP : SHAPE_Y_DOWN;
            case Z:
                return SHAPE_Z;
            default:
                return SHAPE_Y_UP;
        }
    }
}
