package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.server.entity.item.CandyCaneHookEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.GumWormSegmentEntity;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
//? if <1.21.2
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.ToolActions;

import javax.annotation.Nullable;
import java.util.UUID;

public class CandyCaneHookItem extends Item implements ACEnchantableItem, ACTickingItem {

    public CandyCaneHookItem() {
        super(new Item.Properties().durability(200));
    }

    // 1.21.2 merged the result-plus-stack pair back into a plain InteractionResult.
    //? if >=1.21.2
    /*public net.minecraft.world.InteractionResult use(Level level, Player player, InteractionHand hand) {*/
    //? if <1.21.2
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        ItemStack itemStackOpposite = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if(!level.isClientSide()){
            if (canLaunchHook(player, itemstack, level, true, hand) && (hand == InteractionHand.MAIN_HAND || !itemStackOpposite.is(this) || isHookLaunchedInWorld(level, itemStackOpposite))) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ACSoundRegistry.CANDY_CANE_HOOK_LAUNCH.get(), SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
                CandyCaneHookEntity hookEntity = new CandyCaneHookEntity(player, level, itemstack, hand == InteractionHand.OFF_HAND);
                hookEntity.setOwner(player);
                hookEntity.setReeling(false);
                if (!level.isClientSide()) {
                    level.addFreshEntity(hookEntity);
                }
                setLastLaunchedHookUUID(itemstack, hookEntity.getUUID());
                setReelingIn(itemstack, false);

                player.awardStat(Stats.ITEM_USED.get(this));
                player.gameEvent(GameEvent.ITEM_INTERACT_START);
                player.swing(hand);
                return ACCompat.useConsume(itemstack);
            } else if(!(player.getRootVehicle() instanceof GumWormSegmentEntity) && !(itemStackOpposite.is(this) && !isActive(itemStackOpposite))){
                if (isActive(itemstack)) {
                    if(itemStackOpposite.is(this) && isActive(itemStackOpposite) && !isReelingIn(itemStackOpposite)){
                        setReelingIn(itemStackOpposite, true);
                        if (!level.isClientSide()) {
                            ACCompat.hurtAndBreak(itemStackOpposite, 1, player, hand);
                        }
                    }
                    if (!isReelingIn(itemstack)) {
                        setReelingIn(itemstack, true);
                        if (!level.isClientSide()) {
                            ACCompat.hurtAndBreak(itemstack, 1, player, hand);
                        }
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), ACSoundRegistry.CANDY_CANE_HOOK_REEL.get(), SoundSource.NEUTRAL, 1.0F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
                        player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
                        return ACCompat.useSidedSuccess(itemstack, level.isClientSide());
                    }
                }
            }
        }
        return ACCompat.usePass(itemstack);
    }

    private boolean isHookLaunchedInWorld(Level level, ItemStack stack) {
        if(isActive(stack)){
            CompoundTag compoundTag = ACCompat.getOrCreateTag(stack);
            if (level instanceof ServerLevel serverLevel && compoundTag.contains("LastLaunchedHookUUID") && compoundTag.contains("LastLaunchedHookUUID")) {
                Entity entity = serverLevel.getEntity(ACCompat.getUUID(compoundTag, "LastLaunchedHookUUID"));
                if (entity instanceof CandyCaneHookEntity candyCaneHook) {
                    return candyCaneHook.isAlive() && candyCaneHook.tickCount > 0;
                }
            }
        }
        return false;
    }

    public void acInventoryTick(ItemStack stack, Level level, Entity entity, boolean held) {

        if (entity instanceof Player player && !level.isClientSide() && !(stack == player.getItemBySlot(EquipmentSlot.MAINHAND) || stack == player.getItemBySlot(EquipmentSlot.OFFHAND)) && isActive(stack)) {
            if (!isReelingIn(stack)) {
                setReelingIn(stack, true);
            }
            if (canLaunchHook(player, stack, level, false, InteractionHand.MAIN_HAND)) {
                setLastLaunchedHookUUID(stack, null);
            }
        }
    }

    public static boolean isActive(ItemStack itemStack) {
        CompoundTag compoundtag = ACCompat.getTag(itemStack);
        return compoundtag != null && compoundtag.contains("LastLaunchedHookUUID");
    }

    @Nullable
    public static UUID getLaunchedHookUUID(ItemStack itemStack) {
        CompoundTag compoundtag = ACCompat.getTag(itemStack);
        if (compoundtag != null && compoundtag.contains("LastLaunchedHookUUID")) {
            return ACCompat.getUUID(compoundtag, "LastLaunchedHookUUID");
        } else {
            return null;
        }
    }

    public static void setLastLaunchedHookUUID(ItemStack itemStack, @Nullable UUID uuid) {
        CompoundTag compoundtag = ACCompat.getOrCreateTag(itemStack);
        if(uuid == null){
            compoundtag.remove("LastLaunchedHookUUID");
        }else{
            ACCompat.putUUID(compoundtag, "LastLaunchedHookUUID", uuid);
        }
        ACCompat.setTag(itemStack, compoundtag);
    }


    public static boolean isReelingIn(ItemStack itemStack) {
        CompoundTag compoundtag = ACCompat.getTag(itemStack);
        return isActive(itemStack) && compoundtag != null && ACCompat.getBoolean(compoundtag, "Reeling");
    }

    public static void setReelingIn(ItemStack itemStack, boolean reeling) {
        CompoundTag compoundtag = ACCompat.getOrCreateTag(itemStack);
        compoundtag.putBoolean("Reeling", reeling);
        ACCompat.setTag(itemStack, compoundtag);
    }

    public static boolean canLaunchHook(Player player, ItemStack itemStack, Level level, boolean checkHands, InteractionHand hand) {
        CompoundTag compoundtag = ACCompat.getOrCreateTag(itemStack);
        if (level instanceof ServerLevel serverLevel && compoundtag.contains("LastLaunchedHookUUID") && compoundtag.contains("LastLaunchedHookUUID")) {
            Entity entity = serverLevel.getEntity(ACCompat.getUUID(compoundtag, "LastLaunchedHookUUID"));
            if (entity instanceof CandyCaneHookEntity candyCaneHook) {
                return !(candyCaneHook.isAlive() && candyCaneHook.getOwner() != null && candyCaneHook.getOwner().is(player) && (!checkHands || hand == candyCaneHook.getHandLaunchedFrom()));
            }
            return true;
        } else {
            return true;
        }
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.getCount() == 1;
    }

    // This is the one seam that hands a tool action back TO the loader rather than asking it a
    // question, and Fabric has nothing on the other side of it: no patched Item method to override,
    // and so no supertype answer to fall through to. The whole override goes, and with it the only
    // use of the cast action anywhere in this tree — which is why the stand-in's dispatch answers
    // false for it rather than pretending to know.
    //? if !fabric {
    @Override
    public boolean canPerformAction(ItemStack stack, net.minecraftforge.common.ToolAction toolAction) {
        return toolAction == ToolActions.FISHING_ROD_CAST || super.canPerformAction(stack, toolAction);
    }
    //?}
}
