package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.particle.ACParticleRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
//? if <1.21.5
import net.minecraft.world.item.ArmorItem;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;

// 1.21.5 deleted net.minecraft.world.item.ArmorItem — an armour piece is a plain Item whose
// Properties carry humanoidArmor(material, ArmorType). See ACArmorMaterial#properties.
// Below 1.21 the worn-armour tick still arrives through the loader's own onArmorTick hook, so the
// item has no inventory-tick work of its own and does not declare the interface — implementing it
// there would mean a no-op call on every stack, every tick.
//? if >=1.21.5 {
/*public class HazmatArmorItem extends net.minecraft.world.item.Item implements CustomArmorPostRender, ACClientExtensionItem, ACTickingItem {
*///?} elif >=1.21 {
/*public class HazmatArmorItem extends ArmorItem implements CustomArmorPostRender, ACClientExtensionItem, ACTickingItem {
*///?} else {
public class HazmatArmorItem extends ArmorItem implements CustomArmorPostRender, ACClientExtensionItem, ACArmorTickItem {
//?}

    public HazmatArmorItem(ACArmorMaterial armorMaterial, ArmorItem.Type slot) {
        super(armorMaterial.vanilla(), slot, armorMaterial.properties(new Properties(), slot));
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions) AlexsCaves.PROXY.getArmorProperties());
    }

    // See DarknessArmorItem: 1.21 has no onArmorTick, so the worn-armour tick arrives through
    // vanilla's per-slot inventory tick and has to be filtered down to the armour slots again.
    //? if >=1.21 {
    /*@Override
    public void acInventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, boolean selected) {
        if (entity instanceof Player player && com.github.alexmodguy.alexscaves.server.misc.ACCompat.isWornArmor(stack, player)) {
            acArmorTick(stack, level, player);
        }
    }
    *///?} else {
    @SuppressWarnings("removal")
    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        acArmorTick(stack, level, player);
    }
    //?}

    private void acArmorTick(ItemStack stack, Level level, Player player) {
        if (stack.is(ACItemRegistry.HAZMAT_MASK.get()) && Math.cos(player.tickCount * 0.05F) >= 0.9F) {
            Vec3 eyes = player.getEyePosition();
            if (level.getRandom().nextBoolean()) {
                Vec3 leftOffset = new Vec3(0.25F, -0.3F, 0.25F).xRot((float) Math.toRadians(-player.getXRot())).yRot((float) Math.toRadians(-player.getYHeadRot()));
                level.addParticle(ACParticleRegistry.HAZMAT_BREATHE.get(), eyes.x + leftOffset.x, eyes.y + leftOffset.y, eyes.z + leftOffset.z, (level.getRandom().nextFloat() - 0.5F) * 0.1F, (level.getRandom().nextFloat() - 0.5F) * 0.1F, (level.getRandom().nextFloat() - 0.5F) * 0.1F);
            }
            if (level.getRandom().nextBoolean()) {
                Vec3 rightOffset = new Vec3(-0.25F, -0.3F, 0.25F).xRot((float) Math.toRadians(-player.getXRot())).yRot((float) Math.toRadians(-player.getYHeadRot()));
                level.addParticle(ACParticleRegistry.HAZMAT_BREATHE.get(), eyes.x + rightOffset.x, eyes.y + rightOffset.y, eyes.z + rightOffset.z, (level.getRandom().nextFloat() - 0.5F) * 0.1F, (level.getRandom().nextFloat() - 0.5F) * 0.1F, (level.getRandom().nextFloat() - 0.5F) * 0.1F);
            }
        }
    }

    @Nullable
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        if (slot == EquipmentSlot.LEGS) {
            return AlexsCaves.MODID + ":textures/armor/hazmat_suit_1.png";
        } else {
            return AlexsCaves.MODID + ":textures/armor/hazmat_suit_0.png";
        }
    }

    public static int getWornAmount(LivingEntity entity) {
        int i = 0;
        if (entity.getItemBySlot(EquipmentSlot.HEAD).is(ACItemRegistry.HAZMAT_MASK.get())) {
            i++;
        }
        if (entity.getItemBySlot(EquipmentSlot.CHEST).is(ACItemRegistry.HAZMAT_CHESTPLATE.get())) {
            i++;
        }
        if (entity.getItemBySlot(EquipmentSlot.LEGS).is(ACItemRegistry.HAZMAT_LEGGINGS.get())) {
            i++;
        }
        if (entity.getItemBySlot(EquipmentSlot.FEET).is(ACItemRegistry.HAZMAT_BOOTS.get())) {
            i++;
        }
        return i;
    }
}
