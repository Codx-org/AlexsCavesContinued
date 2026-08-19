package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;
import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.misc.ACAdvancementTriggerRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if <1.21.2
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RemoteDetonatorItem extends Item implements ACTickingItem {

    public RemoteDetonatorItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static boolean isActive(ItemStack itemStack) {
        CompoundTag compoundtag = ACCompat.getTag(itemStack);
        return compoundtag != null && (compoundtag.contains("BombDimension") || compoundtag.contains("BombPos"));
    }

    private static Optional<ResourceKey<Level>> getBombDimension(CompoundTag tag) {
        return Level.RESOURCE_KEY_CODEC.parse(NbtOps.INSTANCE, tag.get("BombDimension")).result();
    }

    @Nullable
    public static GlobalPos getBombPosition(CompoundTag tag) {
        boolean flag = tag.contains("BombPos");
        boolean flag1 = tag.contains("BombDimension");
        if (flag && flag1) {
            Optional<ResourceKey<Level>> optional = getBombDimension(tag);
            if (optional.isPresent()) {
                BlockPos blockpos = ACCompat.getBlockPos(tag, "BombPos");
                return GlobalPos.of(optional.get(), blockpos);
            }
        }

        return null;
    }

    // 1.21.2 merged the result-plus-stack pair back into a plain InteractionResult.
    //? if >=1.21.2
    /*public net.minecraft.world.InteractionResult use(Level level, Player player, InteractionHand hand) {*/
    //? if <1.21.2
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (isActive(itemstack)) {
            CompoundTag tag = ACCompat.getOrCreateTag(itemstack);
            GlobalPos globalPos = getBombPosition(tag);
            if(globalPos != null && globalPos.dimension() != null && !level.isClientSide() && level instanceof ServerLevel serverLevel){
                ServerLevel dimensionLevel = serverLevel.getServer().getLevel(globalPos.dimension());
                if(dimensionLevel != null){
                    loadChunksAround(dimensionLevel, player.getUUID(), globalPos.pos(), true);
                    BlockState blockState = dimensionLevel.getBlockState(globalPos.pos());
                    if(blockState.is(ACTagRegistry.REMOTE_DETONATOR_ACTIVATES)){
                        com.github.alexmodguy.alexscaves.server.misc.ACCompat.onCaughtFire(blockState, dimensionLevel, globalPos.pos(), Direction.UP, player);
                        if(player.distanceToSqr(Vec3.atCenterOf(globalPos.pos())) > 1000){
                            ACAdvancementTriggerRegistry.REMOTE_DETONATION.triggerForEntity(player);
                        }
                        tag.remove("BombDimension");
                        tag.remove("BombPos");
                        ACCompat.setTag(itemstack, tag);
                        level.setBlockAndUpdate(globalPos.pos(), Blocks.AIR.defaultBlockState());
                    }
                }
            }
            return ACCompat.useSuccess(itemstack);
        }
        return ACCompat.usePass(itemstack);
    }


    private static void loadChunksAround(ServerLevel serverLevel, UUID ticket, BlockPos center, boolean load){
        ChunkPos chunkPos = ACCompat.chunkPos(center);
        for(int i = -1; i <= 1; i++){
            for(int j = -1; j <= 1; j++){
                ACPlatform.forceChunk(serverLevel, ticket, ACCompat.chunkX(chunkPos) + i, ACCompat.chunkZ(chunkPos) + j, load, true);
            }
        }
    }


    public void acInventoryTick(ItemStack itemStack, Level level, Entity entity, boolean b) {
        if (!level.isClientSide()) {
            if (isActive(itemStack)) {
                CompoundTag compoundtag = ACCompat.getOrCreateTag(itemStack);
                if (compoundtag.contains("BombTracked") && !ACCompat.getBoolean(compoundtag, "BombTracked")) {
                    return;
                }
                Optional<ResourceKey<Level>> optional = getBombDimension(compoundtag);
                if (optional.isPresent() && optional.get() == level.dimension() && compoundtag.contains("BombPos")) {
                    BlockPos blockpos = ACCompat.getBlockPos(compoundtag, "BombPos");
                    boolean flag = false;
                    if((entity.tickCount + entity.getId()) % 20 == 0){
                        if(level.isLoaded(blockpos) && !level.getBlockState(blockpos).is(ACTagRegistry.REMOTE_DETONATOR_ACTIVATES)){
                            flag = true;
                        }
                    }
                    if (!level.isInWorldBounds(blockpos) || flag) {
                        compoundtag.remove("BombPos");
                        compoundtag.remove("BombDimension");
                        ACCompat.setTag(itemStack, compoundtag);
                    }
                }
            }
        }
    }

    public InteractionResult useOn(UseOnContext context) {
        BlockPos blockpos = context.getClickedPos();
        Level level = context.getLevel();
        if (!level.getBlockState(blockpos).is(ACTagRegistry.REMOTE_DETONATOR_ACTIVATES)) {
            return super.useOn(context);
        } else {
            level.playSound((Player)null, blockpos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
            Player player = context.getPlayer();
            ItemStack itemstack = context.getItemInHand();
            boolean flag = !player.getAbilities().instabuild && itemstack.getCount() == 1;
            if (flag) {
                this.addBombTags(level.dimension(), blockpos, ACCompat.getOrCreateTag(itemstack));
            } else {
                ItemStack itemstack1 = new ItemStack(ACItemRegistry.REMOTE_DETONATOR.get(), 1);
                CompoundTag compoundtag = ACCompat.hasTag(itemstack) ? ACCompat.getTag(itemstack).copy() : new CompoundTag();
                ACCompat.setTag(itemstack1, compoundtag);
                itemstack.shrink(1);
                this.addBombTags(level.dimension(), blockpos, compoundtag);
                if (!player.getInventory().add(itemstack1)) {
                    player.drop(itemstack1, false);
                }
            }

            return ACCompat.sidedSuccess(level.isClientSide());
        }
    }

    private void addBombTags(ResourceKey<Level> levelResourceKey, BlockPos blockPos, CompoundTag tag) {
        ACCompat.putBlockPos(tag, "BombPos", blockPos);
        Level.RESOURCE_KEY_CODEC.encodeStart(NbtOps.INSTANCE, levelResourceKey).result().ifPresent((p_40731_) -> {
            tag.put("BombDimension", p_40731_);
        });
        tag.putBoolean("BombTracked", true);
    }


    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        if (ACCompat.getTag(stack) != null && ACCompat.getTag(stack).contains("BombPos")) {
            Optional<ResourceKey<Level>> optional = getBombDimension(ACCompat.getTag(stack));
            BlockPos blockpos = ACCompat.getBlockPos(ACCompat.getTag(stack), "BombPos");
            if (optional.isPresent() && blockpos != null) {
                Component untranslated = Component.translatable("item.alexscaves.remote_detonator.desc", blockpos.getX(), blockpos.getY(), blockpos.getZ()).withStyle(ChatFormatting.GRAY);
                tooltip.add(untranslated);
            }
        }
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
    }
}
