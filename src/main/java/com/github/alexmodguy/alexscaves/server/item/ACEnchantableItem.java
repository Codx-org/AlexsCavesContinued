package com.github.alexmodguy.alexscaves.server.item;

import net.minecraft.world.item.ItemStack;

/**
 * An item this mod makes enchantable by hand rather than by inheriting a tier.
 *
 * <p>Both methods were {@code Item}'s own until 1.21.2, which replaced them with the
 * {@code minecraft:enchantable} data component — a plain int, applied to the item's default
 * components instead of answered per call. Redeclaring them here keeps every implementation and its
 * {@code @Override} identical on all versions (the same arrangement as
 * {@link ACClientExtensionItem}); from 1.21.2 the value is read once by
 * {@code AlexsCaves#modifyDefaultComponents} (NeoForge) or {@code AlexsCaves#gatherItemComponents}
 * (Forge) and {@code isEnchantable} stops being consulted, since
 * the component has no per-stack half. Nothing is lost: every implementation answers
 * "a stack of one", which is what a component-enchantable item already means.
 */
public interface ACEnchantableItem {

    int getEnchantmentValue();

    default boolean isEnchantable(ItemStack stack) {
        return stack.getCount() == 1;
    }
}
