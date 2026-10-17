package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class HolocoderItem extends Item {
    public HolocoderItem(Item.Properties properties) {
        super(properties);
    }

    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        if (ACCompat.getTag(stack) != null) {
            Tag entity = ACCompat.getTag(stack).get("BoundEntityTag");
            if (entity instanceof CompoundTag) {
                Optional<EntityType<?>> optional = ACCompat.entityTypeFrom((CompoundTag) entity);
                if (optional.isPresent()) {
                    Component untranslated = optional.get().getDescription().copy().withStyle(ChatFormatting.GRAY);
                    tooltip.add(untranslated);
                }
            }
        }
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
    }

    public static UUID getBoundEntityUUID(ItemStack stack) {
        if (ACCompat.getTag(stack) != null && ACCompat.getTag(stack).contains("BoundEntityUUID")) {
            return ACCompat.getUUID(ACCompat.getTag(stack), "BoundEntityUUID");
        } else {
            return null;
        }
    }

    public static boolean isBound(ItemStack stack) {
        return getBoundEntityUUID(stack) != null;
    }
}
