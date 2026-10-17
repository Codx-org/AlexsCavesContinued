package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.github.alexmodguy.alexscaves.server.enchantment.ACEnchantmentRegistry;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.item.MagneticWeaponEntity;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
//? if <1.21.2
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class GalenaGauntletItem extends Item implements ACClientExtensionItem, ACEnchantableItem, ACRepairableItem, ACTickingItem {
    public GalenaGauntletItem() {
        super(new Item.Properties().stacksTo(1).durability(400).rarity(Rarity.UNCOMMON));
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
        ItemStack otherHand = interactionHand == InteractionHand.MAIN_HAND ? player.getItemInHand(InteractionHand.OFF_HAND) : player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean crystallization = ACCompat.enchantLevel(itemstack, ACEnchantmentRegistry.CRYSTALLIZATION) > 0;
        if (otherHand.is(crystallization ? ACTagRegistry.GALENA_GAUNTLET_CRYSTALLIZATION_ITEMS : ACTagRegistry.MAGNETIC_ITEMS)) {
            if (!player.isCreative()) {
                ACCompat.hurtAndBreakUsedHand(itemstack, 1, player);
            }
            player.startUsingItem(interactionHand);
            return ACCompat.useConsume(itemstack);
        } else {
            return ACCompat.useFail(itemstack);
        }
    }

    @Override
    public Item[] acExtraRepairMaterials() {
        return new Item[]{ACBlockRegistry.PACKED_GALENA.get().asItem()};
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
        if(player instanceof Player realPlayer){
            ACCompat.addCooldown(realPlayer, stack, 5);

        }
        AlexsCaves.PROXY.clearSoundCacheFor(player);
        player.playSound(ACSoundRegistry.GALENA_GAUNTLET_STOP.get());
        //? if >=1.21.2
        /*return false;*/
    }

    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    public int getUseDuration(ItemStack stack) {
        return 72000;
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
        InteractionHand otherHand = InteractionHand.MAIN_HAND;
        if (living.getItemInHand(InteractionHand.OFF_HAND) == stack) {
            otherHand = InteractionHand.MAIN_HAND;
        }
        if (living.getItemInHand(InteractionHand.MAIN_HAND) == stack) {
            otherHand = InteractionHand.OFF_HAND;
        }
        AlexsCaves.PROXY.playWorldSound(living, (byte) 11);
        ItemStack otherStack = living.getItemInHand(otherHand);
        boolean otherMagneticWeaponsInUse = false;
        boolean crystallization = ACCompat.enchantLevel(stack, ACEnchantmentRegistry.CRYSTALLIZATION) > 0;
        if (otherStack.is(crystallization ? ACTagRegistry.GALENA_GAUNTLET_CRYSTALLIZATION_ITEMS : ACTagRegistry.MAGNETIC_ITEMS)) {
            for(MagneticWeaponEntity magneticWeapon : level.getEntitiesOfClass(MagneticWeaponEntity.class, living.getBoundingBox().inflate(64, 64, 64))){
                Entity controller = magneticWeapon.getController();
                if(controller != null && controller.is(living)){
                    otherMagneticWeaponsInUse = true;
                    break;
                }
            }
            if(!otherMagneticWeaponsInUse) {
                ItemStack copy = otherStack.copy();
                otherStack.setCount(0);
                MagneticWeaponEntity magneticWeapon = ACCompat.createEntity(ACEntityRegistry.MAGNETIC_WEAPON.get(), level);
                magneticWeapon.setItemStack(copy);
                magneticWeapon.setPos(living.position().add(0, 1, 0));
                magneticWeapon.setControllerUUID(living.getUUID());
                level.addFreshEntity(magneticWeapon);
            }
        } else if (!otherStack.isEmpty()) {
            living.stopUsingItem();
        }
    }

    public void acInventoryTick(ItemStack stack, Level level, Entity entity, boolean held) {
        boolean using = entity instanceof LivingEntity living && living.getUseItem().equals(stack);
        if (level.isClientSide()) {
            int useTime = getUseTime(stack);
            CompoundTag tag = ACCompat.getOrCreateTag(stack);
            if (ACCompat.getInt(tag, "PrevUseTime") != ACCompat.getInt(tag, "UseTime")) {
                tag.putInt("PrevUseTime", getUseTime(stack));
            }
            ACCompat.setTag(stack, tag);
            if (using && useTime < 5.0F) {
                setUseTime(stack, useTime + 1);
            }
            if (!using && useTime > 0.0F) {
                setUseTime(stack, useTime - 1);
            }
        }
    }

    public static void setUseTime(ItemStack stack, int useTime) {
        CompoundTag tag = ACCompat.getOrCreateTag(stack);
        tag.putInt("PrevUseTime", getUseTime(stack));
        tag.putInt("UseTime", useTime);
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

    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.OFFHAND;
    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return !oldStack.is(ACItemRegistry.GALENA_GAUNTLET.get()) || !newStack.is(ACItemRegistry.GALENA_GAUNTLET.get());
    }
}
