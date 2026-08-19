package com.github.alexmodguy.alexscaves.server.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * The armour sets that draw themselves rather than letting {@code HumanoidArmorLayer} draw them —
 * see {@code HumanoidArmorLayerMixin}, which cancels the vanilla layer for these and calls
 * {@code ACArmorRenderProperties.renderCustomArmor} instead.
 */
public interface CustomArmorPostRender {

    /**
     * The armour texture this set draws with.
     *
     * <p>Up to 1.20.4 this doubled as Forge's {@code IForgeItem#getArmorTexture} override, which is
     * where the six armour classes' identical method bodies came from. 1.20.5 replaced that hook
     * with one keyed on an {@code ArmorMaterial.Layer} — a shape these sets cannot express, since
     * their textures do not live at vanilla's {@code textures/models/armor/…_layer_N.png} path.
     * Declaring it here instead means the layer mixin reads the texture straight off the item, the
     * same way on every version and every loader, and never goes through a loader hook at all.
     */
    String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type);
}
