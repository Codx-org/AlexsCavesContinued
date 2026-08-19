package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.item.NuclearExplosionEntity;
import com.github.alexmodguy.alexscaves.server.item.tooltip.SackOfSatingTooltip;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.Optional;

public class SackOfSatingItem extends Item implements ACTickingItem {

    public SackOfSatingItem() {
        super(new Item.Properties().stacksTo(1).rarity(ACItemRegistry.RARITY_SWEET));
    }

    public static int getHunger(ItemStack itemStack) {
        CompoundTag compoundtag = ACCompat.getTag(itemStack);
        return compoundtag != null ? ACCompat.getInt(compoundtag, "HungerValue") : 0;
    }

    public static boolean isChewing(ItemStack itemStack, long gameTimeIn) {
        CompoundTag compoundtag = ACCompat.getTag(itemStack);
        return compoundtag != null && compoundtag.contains("ChewTimestamp") && gameTimeIn - ACCompat.getLong(compoundtag, "ChewTimestamp") < 30;
    }

    public static boolean isExploding(ItemStack itemStack) {
        CompoundTag compoundtag = ACCompat.getTag(itemStack);
        return compoundtag != null && ACCompat.getBoolean(compoundtag, "Exploding");
    }

    public static long getFeedTimestamp(ItemStack itemStack) {
        CompoundTag compoundtag = ACCompat.getTag(itemStack);
        return compoundtag != null && compoundtag.contains("FeedTimestamp") ?  ACCompat.getLong(compoundtag, "FeedTimestamp") : -1;
    }

    public static void setHunger(ItemStack stack, int hunger) {
        CompoundTag tag = ACCompat.getOrCreateTag(stack);
        tag.putInt("HungerValue", hunger);
        ACCompat.setTag(stack, tag);
    }

    public static void setChewTimestamp(ItemStack stack, long timestamp) {
        CompoundTag tag = ACCompat.getOrCreateTag(stack);
        tag.putLong("ChewTimestamp", timestamp);
        ACCompat.setTag(stack, tag);
    }

    public static void setFeedTimestamp(ItemStack stack, long timestamp) {
        CompoundTag tag = ACCompat.getOrCreateTag(stack);
        tag.putLong("FeedTimestamp", timestamp);
        ACCompat.setTag(stack, tag);
    }

    public static void setExploding(ItemStack stack, boolean exploding) {
        CompoundTag tag = ACCompat.getOrCreateTag(stack);
        tag.putBoolean("Exploding", exploding);
        ACCompat.setTag(stack, tag);
    }


    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.of(new SackOfSatingTooltip(stack));
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack sackStack, ItemStack foodStack, Slot slot, ClickAction clickAction, Player player, SlotAccess slotAccess) {
        if (clickAction != ClickAction.SECONDARY || ACCompat.getTag(sackStack) == null || foodStack.is(ACTagRegistry.RESTRICTED_FROM_SACK_OF_SATING)) {
            return false;
        } else {
            if (!foodStack.isEmpty() && ACCompat.isEdible(foodStack)) {
                if(foodStack.is(ACTagRegistry.EXPLODES_SACK_OF_SATING)){
                    setExploding(sackStack, true);
                }
                int wholeHunger = calculateWholeStackHungerValue(foodStack, player);
                setHunger(sackStack, getHunger(sackStack) + wholeHunger);
                if(ACCompat.returnsBowl(foodStack)){
                    ItemStack bowlStack = new ItemStack(Items.BOWL, foodStack.getCount());
                    if(!player.addItem(bowlStack)){
                        player.drop(bowlStack, false);
                    }
                }
                if(foodStack.getItem() instanceof HoneyBottleItem || foodStack.getItem() instanceof DrinkableBottledItem){
                    ItemStack bowlStack = new ItemStack(Items.GLASS_BOTTLE, foodStack.getCount());
                    if(!player.addItem(bowlStack)){
                        player.drop(bowlStack, false);
                    }
                }
                foodStack.setCount(0);
                setChewTimestamp(sackStack, player.level().getGameTime());
                return true;
            }
            return false;
        }
    }

    public void acInventoryTick(ItemStack stack, Level level, Entity entity, boolean held) {
        if(ACCompat.getTag(stack) == null){
            ACCompat.setTag(stack, new CompoundTag());
        }
        int hungerValue = getHunger(stack);
        long timestamp = getFeedTimestamp(stack);
        if(!level.isClientSide() && hungerValue > 0 && entity instanceof Player player && !player.getAbilities().invulnerable && player.tickCount % 100 == 0 && player.canEat(false) && (timestamp == -1 || player.level().getGameTime() - timestamp > 40)){
            player.getFoodData().eat(1, 0.05F);
            setHunger(stack, hungerValue - 1);
            setFeedTimestamp(stack, player.level().getGameTime());
            level.gameEvent(player, GameEvent.EAT, player.blockPosition());
            level.playSound((Player)null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        }
        if(isChewing(stack, level.getGameTime()) && entity.tickCount % 6 == 0){
            level.playSound((Player)null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.5F, level.getRandom().nextFloat() * 0.3F + 1.3F);
        }
        if(isExploding(stack)){
            if(!level.isClientSide()){
                NuclearExplosionEntity explosion = ACCompat.createEntity(ACEntityRegistry.NUCLEAR_EXPLOSION.get(), level);
                explosion.setPos(entity.position().add(0, 4, 0));
                explosion.setSize(0.5F);
                explosion.setIntentionalGameDesign(true);
                level.addFreshEntity(explosion);
                setExploding(stack, false);
                stack.shrink(1);
            }
        }
    }

     public static int calculateWholeStackHungerValue(ItemStack foodStack, LivingEntity eater){
        FoodProperties foodProperties = ACCompat.food(foodStack, eater);
        if(foodProperties != null && !foodStack.is(ACTagRegistry.RESTRICTED_FROM_SACK_OF_SATING)){
            return ACCompat.nutrition(foodProperties) * foodStack.getCount();
        }
        return 0;
    }

}
