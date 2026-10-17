package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.message.ArmorKeyMessage;
import com.github.alexmodguy.alexscaves.server.message.UpdateItemTagMessage;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import com.github.alexmodguy.alexscaves.server.potion.DarknessIncarnateEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
//? if <1.21.5
import net.minecraft.world.item.ArmorItem;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;

// 1.21.5 deleted net.minecraft.world.item.ArmorItem — an armour piece is a plain Item whose
// Properties carry humanoidArmor(material, ArmorType). See ACArmorMaterial#properties.
//? if >=1.21.5 {
/*public class DarknessArmorItem extends net.minecraft.world.item.Item implements CustomArmorPostRender, KeybindUsingArmor, UpdatesStackTags, ACClientExtensionItem, ACTickingItem {
*///?} else {
public class DarknessArmorItem extends ArmorItem implements CustomArmorPostRender, KeybindUsingArmor, UpdatesStackTags, ACClientExtensionItem, ACTickingItem, ACArmorTickItem {
//?}

    public DarknessArmorItem(ACArmorMaterial armorMaterial, ArmorItem.Type slot) {
        super(armorMaterial.vanilla(), slot, armorMaterial.properties(new Properties().rarity(ACItemRegistry.RARITY_DEMONIC), slot));
    }

    private static boolean canChargeUp(LivingEntity entity, boolean creative) {
        return (!DarknessIncarnateEffect.isInLight(entity, 11) || creative && entity instanceof Player player && player.isCreative()) && entity.getItemBySlot(EquipmentSlot.HEAD).is(ACItemRegistry.HOOD_OF_DARKNESS.get()) && !entity.hasEffect(ACCompat.effect(ACEffectRegistry.DARKNESS_INCARNATE.get()));
    }

    public static boolean canChargeUp(ItemStack itemStack) {
        CompoundTag tag = ACCompat.getTag(itemStack);
        return tag == null || ACCompat.getBoolean(tag, "CanCharge");
    }

    public static boolean hasMeter(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ACItemRegistry.CLOAK_OF_DARKNESS.get()) && player.getItemBySlot(EquipmentSlot.HEAD).is(ACItemRegistry.HOOD_OF_DARKNESS.get()) && !player.hasEffect(ACCompat.effect(ACEffectRegistry.DARKNESS_INCARNATE.get()));
    }

    public static float getMeterProgress(ItemStack cloak) {
        CompoundTag tag = ACCompat.getTag(cloak);
        if (tag == null) {
            return 0.0F;
        } else {
            return ACCompat.getInt(tag, "CloakCharge") / (float) AlexsCaves.COMMON_CONFIG.darknessCloakChargeTime.get();
        }
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions) AlexsCaves.PROXY.getArmorProperties());
    }

    // Darkness armour equips silently. Upstream said so by overriding Equipable#getEquipSound to
    // SoundEvents.EMPTY, which shadowed whatever DARKNESS_ARMOR_MATERIAL declared; 1.21.2 deleted
    // Equipable outright (equip sound is a field of the equippable component, built from the
    // material), so there is nothing left to override. DARKNESS_ARMOR_MATERIAL now names EMPTY
    // itself, which is the same silence on every version and reaches the component for free —
    // the material is used by these two items and nothing else.

    // Forge's onArmorTick hook is gone in 1.21 and NeoForge never replaced it either; the per-tick
    // call that reaches a worn piece from then on is vanilla's Item#inventoryTick, which 1.21's
    // Inventory#tick runs over the armour compartment as well as the hotbar. Guarding on "is this
    // the stack in an armour slot" restores the old contract — the body below only ever ran for
    // armour that was actually on.
    // This item already overrides inventoryTick, so the 1.21 bridge lives in there rather than in
    // a second copy of it.
    //? if <1.21 {
    @SuppressWarnings("removal")
    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        acArmorTick(stack, level, player);
    }
    //?}

    private void acArmorTick(ItemStack stack, Level level, Player player) {
        if (stack.is(ACItemRegistry.CLOAK_OF_DARKNESS.get())) {
            if (!level.isClientSide()) {
                CompoundTag tag = ACCompat.getOrCreateTag(stack);
                int charge = ACCompat.getInt(tag, "CloakCharge");
                boolean flag = false;
                if (charge < AlexsCaves.COMMON_CONFIG.darknessCloakChargeTime.get() && canChargeUp(stack)) {
                    charge += 1;
                    tag.putInt("CloakCharge", charge);
                    flag = true;
                }
                ACCompat.setTag(stack, tag);
                if (flag) {
                    AlexsCaves.sendNonLocal(new UpdateItemTagMessage(player.getId(), stack), (ServerPlayer) player);
                }
            }
        }
    }

    public void acInventoryTick(ItemStack stack, Level level, Entity entity, boolean held) {
        //? if >=1.21
        /*if (entity instanceof Player acWearer && ACCompat.isWornArmor(stack, acWearer)) { acArmorTick(stack, level, acWearer); }*/
        if (stack.is(ACItemRegistry.CLOAK_OF_DARKNESS.get()) && entity instanceof LivingEntity living) {
            if (living.getItemBySlot(EquipmentSlot.CHEST) == stack) {
                CompoundTag tag = ACCompat.getOrCreateTag(stack);
                if (!level.isClientSide()) {
                    long lastLightTimestamp = ACCompat.getLong(tag, "LastLightTimestamp");
                    long lastEquipMessageTime = ACCompat.getLong(tag, "LastEquipMessageTime");
                    if (lastLightTimestamp <= 0 || level.getGameTime() - lastLightTimestamp > 10) {
                        tag.putLong("LastLightTimestamp", level.getGameTime());
                        tag.putBoolean("CanCharge", canChargeUp(living, true));
                    }
                    if (lastEquipMessageTime <= 0 && entity instanceof Player player && !level.isClientSide()) {
                        tag.putLong("LastEquipMessageTime", level.getGameTime());
                        ACCompat.displayClientMessage(player, Component.translatable("item.alexscaves.cloak_of_darkness.equip"), true);
                    }
                    ACCompat.setTag(stack, tag);
                } else if (AlexsCaves.PROXY.getClientSidePlayer() == entity && getMeterProgress(stack) >= 1.0F && AlexsCaves.PROXY.isKeyDown(2)) {
                    AlexsCaves.sendMSGToServer(new ArmorKeyMessage(EquipmentSlot.CHEST.ordinal(), living.getId(), 2));
                    onKeyPacket(living, stack, 2);
                }
            }
        }
    }

    public void onKeyPacket(Entity wearer, ItemStack itemStack, int key) {
        if (wearer instanceof LivingEntity living && canChargeUp(living, false)) {
            CompoundTag writeBackTag = ACCompat.getOrCreateTag(itemStack);
            writeBackTag.putInt("CloakCharge", 0);
            ACCompat.setTag(itemStack, writeBackTag);
            living.addEffect(new MobEffectInstance(ACCompat.effect(ACEffectRegistry.DARKNESS_INCARNATE.get()), AlexsCaves.COMMON_CONFIG.darknessCloakFlightTime.get(), 0, false, false, false));
        } else if (wearer instanceof Player player && !wearer.level().isClientSide()) {
            ACCompat.displayClientMessage(player, Component.translatable("item.alexscaves.cloak_of_darkness.requires_darkness"), true);
        }
    }


    @Nullable
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return AlexsCaves.MODID + ":textures/armor/darkness_armor.png";
    }
}
