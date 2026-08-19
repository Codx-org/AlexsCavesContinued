package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.server.entity.util.DeepOneReaction;
import com.github.alexmodguy.alexscaves.server.level.storage.ACWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.Locale;

public class GazingPearlItem extends Item implements ACTickingItem {

    public GazingPearlItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    public static int getPearlColor(ItemStack stack) {
        CompoundTag tag = ACCompat.getTag(stack);
        if(tag != null && ACCompat.getBoolean(tag, "HasReputation")){
            float shine = (float) (Math.sin(System.currentTimeMillis() / 4000F) + 1F) * 0.5F;
            int reputation = ACCompat.getInt(tag, "Reputation");
            int color = 100 - reputation;
            int rainbow = Color.HSBtoRGB(color / 200F, shine * 0.3F + 0.7F,  1F);
            return rainbow;
        }else{
            float hue = (System.currentTimeMillis() % 10000) / 10000f;
            float shine = (float) (Math.sin(System.currentTimeMillis() / 4000F) + 1F) * 0.5F;
            int rainbow = Color.HSBtoRGB(hue, shine * 0.3F + 0.7F, 1f);
            return rainbow;
        }
    }


    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        CompoundTag tag = ACCompat.getTag(stack);
        if(tag != null && ACCompat.getBoolean(tag, "HasReputation")){
            int reputation = ACCompat.getInt(tag, "Reputation");
            DeepOneReaction reaction = DeepOneReaction.fromReputation(reputation);
            String key = "item.alexscaves.gazing_pearl.desc_" + reaction.name().toLowerCase(Locale.ROOT);
            tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        }
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
    }

    public void acInventoryTick(ItemStack stack, Level level, Entity entity, boolean held) {
        if(!level.isClientSide()){
            CompoundTag tag = ACCompat.getOrCreateTag(stack);
            long lastReputationTimestamp = ACCompat.getLong(tag, "LastReputationTimestamp");
            if(lastReputationTimestamp <= 0 || level.getGameTime() - lastReputationTimestamp > 100){
                ACWorldData acWorldData = ACWorldData.get(level);
                if(acWorldData != null){
                    tag.putLong("LastReputationTimestamp", level.getGameTime());
                    tag.putBoolean("HasReputation", true);
                    tag.putInt("Reputation", acWorldData.getDeepOneReputation(entity.getUUID()));
                    ACCompat.setTag(stack, tag);
                }
            }
        }
    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return !oldStack.is(ACItemRegistry.GAZING_PEARL.get()) || !newStack.is(ACItemRegistry.GAZING_PEARL.get());
    }
}
