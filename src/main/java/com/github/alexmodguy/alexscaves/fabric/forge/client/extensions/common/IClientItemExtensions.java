package com.github.alexmodguy.alexscaves.fabric.forge.client.extensions.common;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric stand-in for the per-item client-extension object.
 *
 * <p>On the other two loaders an item hands one of these back from {@code initializeClient} and the
 * loader asks it questions the vanilla {@code Item} cannot answer. This tree asks exactly two, so
 * that is all this interface declares — a stand-in that mirrored the whole Forge surface would be
 * inventing members nothing calls and would drag half the client into a file the dedicated server
 * also loads.
 *
 * <p>Both are {@code default}, and deliberately: the three implementors
 * ({@code ACItemRenderProperties}, {@code ACArmorRenderProperties} and Citadel's) each override only
 * the one they care about, and every {@code initializeClient} body in the tree casts to this type,
 * so an abstract member here would be a compile error in the other two.
 *
 * <p>Only one of the two is dispatched, and that is on purpose. {@code fabric.client.
 * ACFabricItemRenderers} walks the item registry, asks each {@code ACClientExtensionItem} for its
 * extension object and forwards {@link #getCustomRenderer()} to Fabric API's builtin-renderer
 * registry. Nothing forwards {@link #getHumanoidArmorModel} — all six of this mod's armour sets are
 * {@code CustomArmorPostRender}, so {@code mixin.client.HumanoidArmorLayerMixin} cancels vanilla's
 * draw and reaches {@code ACArmorRenderProperties} directly on every loader; the method is declared
 * here only so the three implementors still compile against the shape they were written for.
 *
 * <p>The armour hook's arms mirror {@code ACArmorRenderProperties}' own, minus its
 * {@code forge && >=1.21.2} arm — Forge patched a trailing render state onto its version of the
 * signature, and no file under this package is ever compiled on that loader. Every version-scoped
 * vanilla type is fully qualified rather than imported, since each exists on only part of the range.
 */
public interface IClientItemExtensions {

    // Deleted along with the ISTER mechanism in 1.21.4; from there an item's special renderer is a
    // registered ItemModel instead, so the method has no successor to keep.
    //? if <1.21.4 {
    default net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return null;
    }
    //?}

    //? if >=1.21.2 {
    /*default net.minecraft.client.model.Model getHumanoidArmorModel(ItemStack itemStack, net.minecraft.world.item.equipment.EquipmentModel.LayerType layerType, net.minecraft.client.model.Model _default) {
        return _default;
    }
    *///?} else {
    default net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlot armorSlot, net.minecraft.client.model.HumanoidModel<?> _default) {
        return _default;
    }
    //?}
}
