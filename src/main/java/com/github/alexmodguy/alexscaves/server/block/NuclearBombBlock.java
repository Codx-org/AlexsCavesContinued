package com.github.alexmodguy.alexscaves.server.block;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.item.NuclearBombEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class NuclearBombBlock extends Block implements ACIgnitableBlock {
    public NuclearBombBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(8, 1001).sound(ACSoundTypes.NUCLEAR_BOMB));
    }

    // Both loaders' block extension made this return "did the block actually catch" in 1.21.5, so
    // TNT-like blocks can refuse. The four call sites here ignore the answer either way.
    //? if >=1.21.5
    /*public boolean onCaughtFire(BlockState state, Level level, BlockPos blockPos, @Nullable net.minecraft.core.Direction face, @Nullable LivingEntity igniter) {*/
    //? if <1.21.5
    public void onCaughtFire(BlockState state, Level level, BlockPos blockPos, @Nullable net.minecraft.core.Direction face, @Nullable LivingEntity igniter) {
        if (!level.isClientSide()) {
            NuclearBombEntity bomb = ACCompat.createEntity(ACEntityRegistry.NUCLEAR_BOMB.get(), level);
            bomb.setPos((double) blockPos.getX() + 0.5D, (double) blockPos.getY(), (double) blockPos.getZ() + 0.5D);
            level.addFreshEntity(bomb);
            level.playSound((Player) null, bomb.getX(), bomb.getY(), bomb.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(igniter, GameEvent.PRIME_FUSE, blockPos);
        }
        //? if >=1.21.5
        /*return true;*/
    }

    public void onPlace(BlockState state, Level level, BlockPos blockPos, BlockState blockState, boolean b) {
        if (!blockState.is(state.getBlock())) {
            if (level.hasNeighborSignal(blockPos)) {
                onCaughtFire(state, level, blockPos, null, null);
                level.removeBlock(blockPos, false);
            }

        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos blockPos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.hasNeighborSignal(blockPos)) {
            onCaughtFire(state, level, blockPos, null, null);
            level.removeBlock(blockPos, false);
        }
    }

    public void onProjectileHit(Level level, BlockState state, BlockHitResult blockHitResult, Projectile projectile) {
        if (!level.isClientSide()) {
            BlockPos blockpos = blockHitResult.getBlockPos();
            Entity entity = projectile.getOwner();
            if (projectile.isOnFire() && ACCompat.mayInteract(projectile, level, blockpos)) {
                onCaughtFire(state, level, blockpos, null, entity instanceof LivingEntity ? (LivingEntity) entity : null);
                level.removeBlock(blockpos, false);
            }
        }

    }

    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    // 1.20.5 split BlockBehaviour#use into useItemOn and useWithoutItem; this block's rule is
    // item-driven, so it hangs off useItemOn there. The body below is shared — only the entry point
    // and the "we did nothing" return differ. See ACCompat#itemResult.
    //? if >=1.21.2 {
    /*protected net.minecraft.world.InteractionResult useItemOn(ItemStack usedStack, BlockState state, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult result) {
        return ACCompat.itemResult(acUse(state, level, blockPos, player, hand, result));
    }
    *///?} elif >=1.20.5 {
    /*protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack usedStack, BlockState state, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult result) {
        return ACCompat.itemResult(acUse(state, level, blockPos, player, hand, result));
    }
    *///?} else {
    public InteractionResult use(BlockState state, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult result) {
        InteractionResult acResult = acUse(state, level, blockPos, player, hand, result);
        return acResult == InteractionResult.PASS ? super.use(state, level, blockPos, player, hand, result) : acResult;
    }
    //?}

    private InteractionResult acUse(BlockState state, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult result) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (!itemstack.is(Items.FLINT_AND_STEEL) && !itemstack.is(Items.FIRE_CHARGE)) {
            return InteractionResult.PASS;
        } else {
            onCaughtFire(state, level, blockPos, result.getDirection(), player);
            level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 11);
            Item item = itemstack.getItem();
            if (!player.isCreative()) {
                if (itemstack.is(Items.FLINT_AND_STEEL)) {
                    ACCompat.hurtAndBreak(itemstack, 1, player, hand);
                } else {
                    itemstack.shrink(1);
                }
            }

            player.awardStat(Stats.ITEM_USED.get(item));
            return ACCompat.sidedSuccess(level.isClientSide());
        }
    }

    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

}
