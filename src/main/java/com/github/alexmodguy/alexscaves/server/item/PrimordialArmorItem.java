package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
//? if <1.21.5
import net.minecraft.world.item.ArmorItem;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import com.github.alexmodguy.alexscaves.server.item.ACClientExtensionItem;

// 1.21.5 deleted net.minecraft.world.item.ArmorItem — an armour piece is a plain Item whose
// Properties carry humanoidArmor(material, ArmorType). See ACArmorMaterial#properties.
//? if >=1.21.5 {
/*public class PrimordialArmorItem extends net.minecraft.world.item.Item implements CustomArmorPostRender, ACClientExtensionItem {
*///?} else {
public class PrimordialArmorItem extends ArmorItem implements CustomArmorPostRender, ACClientExtensionItem {
//?}

    public PrimordialArmorItem(ACArmorMaterial armorMaterial, ArmorItem.Type slot) {
        super(armorMaterial.vanilla(), slot, armorMaterial.properties(new Properties(), slot));
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions) AlexsCaves.PROXY.getArmorProperties());
    }

    @Nullable
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        if (slot == EquipmentSlot.LEGS) {
            return AlexsCaves.MODID + ":textures/armor/primordial_armor_1.png";
        } else {
            return AlexsCaves.MODID + ":textures/armor/primordial_armor_0.png";
        }
    }

    public static int getExtraSaturationFromArmor(LivingEntity entity) {
        int i = 0;
        if (entity.getItemBySlot(EquipmentSlot.HEAD).is(ACItemRegistry.PRIMORDIAL_HELMET.get())) {
            i++;
        }
        if (entity.getItemBySlot(EquipmentSlot.CHEST).is(ACItemRegistry.PRIMORDIAL_TUNIC.get())) {
            i++;
        }
        if (entity.getItemBySlot(EquipmentSlot.LEGS).is(ACItemRegistry.PRIMORDIAL_PANTS.get())) {
            i++;
        }
        return i;
    }
}
