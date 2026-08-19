package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.particle.ACParticleRegistry;
import com.github.alexmodguy.alexscaves.server.enchantment.ACEnchantmentRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
//? if <1.21.2
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import com.github.alexmodguy.alexscaves.server.item.ACClientExtensionItem;

public class ResistorShieldItem extends ShieldItem implements ACClientExtensionItem, ACEnchantableItem, ACRepairableItem, ACTickingItem {

    public ResistorShieldItem() {
        super(new Item.Properties().stacksTo(1).durability(1000).rarity(Rarity.UNCOMMON));
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions) AlexsCaves.PROXY.getISTERProperties());
    }

    // 1.21.2 merged the result-plus-stack pair back into a plain InteractionResult.
    //? if >=1.21.2
    /*public net.minecraft.world.InteractionResult use(Level level, Player player, InteractionHand interactionHand) {*/
    //? if <1.21.2
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
        ItemStack itemstack = player.getItemInHand(interactionHand);
        player.startUsingItem(interactionHand);
        if (player.isShiftKeyDown()) {
            setPolarity(itemstack, !isScarlet(itemstack));
        }
        player.playSound(ACSoundRegistry.RESITOR_SHIELD_SPIN.get());
        return ACCompat.useConsume(itemstack);
    }

    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.alexscaves.resistor_shield.desc").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.getCount() == 1;
    }

    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int timeUsing) {
        super.onUseTick(level, living, stack, timeUsing);
        int i = getUseDuration(stack) - timeUsing;
        boolean scarlet = isScarlet(stack);
        boolean firstHit = i >= 10 && i <= 12;
        int slamEnchantAmount = ACCompat.enchantLevel(stack, ACEnchantmentRegistry.HEAVY_SLAM);
        float range = 5F;
        if (level.isClientSide()) {
            setUseTime(stack, i);
            if(i == 10){
                living.playSound(ACSoundRegistry.RESITOR_SHIELD_SLAM.get());
            }
            if (i >= 10 && i % 5 == 0) {
                AlexsCaves.PROXY.playWorldSound(living, (byte) (scarlet ? 9 : 10));
                Vec3 particlesFrom = living.position().add(0, 0.2, 0);
                float particleMax = 5 + living.getRandom().nextInt(5);
                for (int particles = 0; particles < particleMax; particles++) {
                    Vec3 vec3 = new Vec3((living.getRandom().nextFloat() - 0.5) * 0.3F, (living.getRandom().nextFloat() - 0.5) * 0.3F, range * 0.5F + range * 0.5F * living.getRandom().nextFloat()).yRot((float) ((particles / particleMax) * Math.PI * 2)).add(particlesFrom);
                    if (scarlet) {
                        level.addParticle(ACParticleRegistry.SCARLET_SHIELD_LIGHTNING.get(), vec3.x, vec3.y, vec3.z, particlesFrom.x, particlesFrom.y, particlesFrom.z);
                    } else {
                        level.addParticle(ACParticleRegistry.AZURE_SHIELD_LIGHTNING.get(), particlesFrom.x, particlesFrom.y, particlesFrom.z, vec3.x, vec3.y, vec3.z);
                    }
                }
            }
        }
        if (i >= 10 && i % 5 == 0) {
            AABB bashBox = living.getBoundingBox().inflate(5, 1, 5);
            for (LivingEntity entity : living.level().getEntitiesOfClass(LivingEntity.class, bashBox)) {
                if (!living.isAlliedTo(entity) && !entity.equals(living) && entity.distanceTo(living) <= range) {
                    entity.hurt(living.damageSources().mobAttack(living), firstHit ? 6 + (slamEnchantAmount * 3) : 2);
                    if (scarlet) {
                        ACCompat.knockback(entity, firstHit ? 0.5D : 0.2D, entity.getX() - living.getX(), entity.getZ() - living.getZ());
                    } else {
                        ACCompat.knockback(entity, firstHit ? 0.5D : 0.2D, living.getX() - living.getX(), living.getZ() - entity.getZ());
                    }
                }
            }
        }
        if (i == 10 && !level.isClientSide()) {
            ACCompat.hurtAndBreakUsedHand(stack, 1, living);
        }
    }

    @Override
    public Item[] acExtraRepairMaterials() {
        return new Item[]{ACItemRegistry.SCARLET_NEODYMIUM_INGOT.get(), ACItemRegistry.AZURE_NEODYMIUM_INGOT.get()};
    }

    // 1.21.2 deleted Item#isValidRepairItem for the minecraft:repairable component; see
    // ACRepairableItem, which every version answers through.
    //? if <1.21.2 {
    public boolean isValidRepairItem(ItemStack item, ItemStack repairItem) {
        return ACRepairableItem.isExtraRepairMaterial(this, repairItem) || super.isValidRepairItem(item, repairItem);
    }
    //?}

    public void releaseUsing(ItemStack stack, Level level, LivingEntity player, int useTimeLeft) {
        super.releaseUsing(stack, level, player, useTimeLeft);
        AlexsCaves.PROXY.clearSoundCacheFor(player);
        //? if >=1.21.2
        /*return false;*/
    }

    public void acInventoryTick(ItemStack stack, Level level, Entity entity, boolean held) {
        if (getUseTime(stack) != 0 && entity instanceof LivingEntity living && !living.getUseItem().equals(stack)) {
            setUseTime(stack, 0);
            CompoundTag writeBackTag = ACCompat.getOrCreateTag(stack);
            writeBackTag.putInt("PrevUseTime", 0);
            ACCompat.setTag(stack, writeBackTag);
        }
        if (level.isClientSide()) {
            boolean scarlet = isScarlet(stack);
            int switchTime = getSwitchTime(stack);
            CompoundTag tag = ACCompat.getOrCreateTag(stack);
            if (ACCompat.getInt(tag, "PrevSwitchTime") != ACCompat.getInt(tag, "SwitchTime")) {
                tag.putInt("PrevSwitchTime", getSwitchTime(stack));
            }
            ACCompat.setTag(stack, tag);
            if (scarlet && switchTime < 5.0F) {
                setSwitchTime(stack, switchTime + 1);
            }
            if (!scarlet && switchTime > 0.0F) {
                setSwitchTime(stack, switchTime - 1);
            }
        }
    }

    public static void setUseTime(ItemStack stack, int useTime) {
        CompoundTag tag = ACCompat.getOrCreateTag(stack);
        tag.putInt("PrevUseTime", getUseTime(stack));
        tag.putInt("UseTime", useTime);
        ACCompat.setTag(stack, tag);
    }

    public static void setSwitchTime(ItemStack stack, int useTime) {
        CompoundTag tag = ACCompat.getOrCreateTag(stack);
        tag.putInt("PrevSwitchTime", getSwitchTime(stack));
        tag.putInt("SwitchTime", useTime);
        ACCompat.setTag(stack, tag);
    }


    public static void setPolarity(ItemStack stack, boolean scarlet) {
        CompoundTag tag = ACCompat.getOrCreateTag(stack);
        tag.putBoolean("Polarity", scarlet);
        ACCompat.setTag(stack, tag);
    }

    public static int getUseTime(ItemStack stack) {
        CompoundTag compoundtag = ACCompat.getTag(stack);
        return compoundtag != null ? ACCompat.getInt(compoundtag, "UseTime") : 0;
    }

    public static float getLerpedUseTime(ItemStack stack, float f) {
        CompoundTag compoundtag = ACCompat.getTag(stack);
        float prev = compoundtag != null ? (float) ACCompat.getInt(compoundtag, "PrevUseTime") : 0F;
        float current = compoundtag != null ? (float) ACCompat.getInt(compoundtag, "UseTime") : 0F;
        return prev + f * (current - prev);
    }

    public static int getSwitchTime(ItemStack stack) {
        CompoundTag compoundtag = ACCompat.getTag(stack);
        return compoundtag != null ? ACCompat.getInt(compoundtag, "SwitchTime") : 0;
    }

    public static float getLerpedSwitchTime(ItemStack stack, float f) {
        CompoundTag compoundtag = ACCompat.getTag(stack);
        float prev = compoundtag != null ? (float) ACCompat.getInt(compoundtag, "PrevSwitchTime") : 0F;
        float current = compoundtag != null ? (float) ACCompat.getInt(compoundtag, "SwitchTime") : 0F;
        return prev + f * (current - prev);
    }

    public static boolean isScarlet(ItemStack stack) {
        CompoundTag compoundtag = ACCompat.getTag(stack);
        return compoundtag != null ? ACCompat.getBoolean(compoundtag, "Polarity") : false;
    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return !oldStack.is(ACItemRegistry.RESISTOR_SHIELD.get()) || !newStack.is(ACItemRegistry.RESISTOR_SHIELD.get());
    }
}
