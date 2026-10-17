package com.github.alexmodguy.alexscaves.server.item;

import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

/**
 * An item whose attribute modifiers depend on more than its {@code Properties} — either on the stack
 * (this mod has two such: the swiftwood-enchanted club and the durability-scaled gingerbread armour)
 * or simply on being stated in code rather than at construction.
 *
 * <p>The seven implementors all had a single {@code acModifiers} method already, called from
 * whichever version-specific hook the node's loader offers. Naming it in an interface adds the one
 * thing a hook cannot give: a way to ask an arbitrary {@link ItemStack} the same question. Forge
 * needs that from 1.21.2, where it deleted {@code IForgeItem#getAttributeModifiers} outright — the
 * whole per-item attribute API is gone there, with no event in its place, so
 * {@code mixin.ItemStackAttributeModifiersMixin} intercepts the component read inside
 * {@code ItemStack#forEachModifier} and dispatches through here instead.
 *
 * <p>The interface is declared on every node, not just the ones that need it, so that all seven
 * items read the same on all of them.
 */
public interface ACDynamicAttributeItem {

    /**
     * The modifiers this item contributes in {@code slot}, or {@code null} to defer entirely to
     * whatever the item would otherwise have said.
     *
     * <p>An empty multimap is <em>not</em> the same answer as {@code null}: it means "I speak for
     * this slot and contribute nothing", which is how the armour items suppress the material's own
     * modifiers in the slots they do not fill.
     */
    Multimap<Attribute, AttributeModifier> acModifiers(EquipmentSlot slot, ItemStack stack);
}
