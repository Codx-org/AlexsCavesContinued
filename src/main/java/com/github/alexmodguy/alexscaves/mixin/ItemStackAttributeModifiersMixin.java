package com.github.alexmodguy.alexscaves.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Lets {@link com.github.alexmodguy.alexscaves.server.item.ACDynamicAttributeItem} state an item's
 * attribute modifiers on nodes whose loader offers no hook for it.
 *
 * <p>Below 1.20.5 vanilla itself asks the item ({@code Item#getAttributeModifiers(EquipmentSlot,
 * ItemStack)}), so every loader is covered there. From 1.20.5 the modifiers are a data component
 * and only the two Forge-likes patched a stack-aware override back in — Forge per slot, NeoForge
 * per item. Forge then deleted its version in 1.21.2 along with the rest of {@code IForgeItem}'s
 * item hooks and put nothing in its place: there is no {@code ItemAttributeModifierEvent} in the jar
 * and {@code Item} declares no attribute method, so the component set at construction is the only
 * answer vanilla will ever give. That is not enough for two of the seven implementors, whose
 * modifiers depend on the stack rather than the item — the swiftwood-enchanted club and the
 * durability-scaled gingerbread armour.
 *
 * <p><b>Fabric never had such a patch at all</b>, so it needs this from 1.20.5 rather than from
 * 1.21.2 — the same hole, opened two MC versions earlier. NeoForge is the only loader that still
 * answers unaided, which is why the arms below are spelled {@code !neoforge} at the top rather than
 * {@code forge}.
 *
 * <p>So the component read itself is intercepted. Both {@code ItemStack#forEachModifier} overloads
 * open with the same {@code getOrDefault(ATTRIBUTE_MODIFIERS, EMPTY)}, and everything that cares
 * goes through one of the two: {@code LivingEntity} applies equipment attributes via the
 * {@code EquipmentSlot} overload, and {@code ItemStack#addAttributeTooltips} builds the tooltip via
 * the {@code EquipmentSlotGroup} one. Nothing else in the game reads the component for gameplay
 * (the only other readers are the advancement predicate, the loot function that writes it, and
 * {@code Mob}'s equipment comparison), so replacing its value in these two places covers both the
 * numbers and what the player is told about them.
 *
 * <p>The value that was there stays the fallback, which is exactly right: it is whatever
 * {@code Item.Properties#attributes} set, i.e. the answer the item would have given unaided.
 */
@Mixin(ItemStack.class)
public class ItemStackAttributeModifiersMixin {

    // Three bands, and the boundaries are not the same on the two loaders that need this.
    //
    // 1.21.6 gave the tooltip overload a third argument — the ItemAttributeModifiers.Display that
    // says how a modifier is worded — so its consumer is a TriConsumer now. The slot overload, which
    // is the gameplay one, is unchanged. And the tooltip overload does not exist at all on 1.20.5 /
    // 1.20.6 (javap'd: forEachModifier(EquipmentSlotGroup, ...) arrives in 1.21), which only Fabric
    // reaches, since Forge still had its own hook until 1.21.2. A selector naming a method the class
    // does not declare is a hard injection failure, so that band gets an arm of its own listing the
    // EquipmentSlot overload alone.
    //
    // Only the selectors move across all three; Stonecutter cannot nest a gate inside the loader
    // gate, so each arm is repeated whole.
    //? if !neoforge && >=1.21.6 {
    /*@com.llamalad7.mixinextras.injector.ModifyExpressionValue(
            method = {
                    "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
                    "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V"
            },
            at = @org.spongepowered.asm.mixin.injection.At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getOrDefault(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object ac_dynamicAttributeModifiers(Object original) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.getItem() instanceof com.github.alexmodguy.alexscaves.server.item.ACDynamicAttributeItem dynamic) {
            return com.github.alexmodguy.alexscaves.server.misc.ACCompat.itemAttributes(
                    dynamic::acModifiers,
                    self,
                    () -> (net.minecraft.world.item.component.ItemAttributeModifiers) original);
        }
        return original;
    }
    *///?} elif (forge && >=1.21.2) || (fabric && >=1.21) {
    /*@com.llamalad7.mixinextras.injector.ModifyExpressionValue(
            method = {
                    "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
                    "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V"
            },
            at = @org.spongepowered.asm.mixin.injection.At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getOrDefault(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object ac_dynamicAttributeModifiers(Object original) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.getItem() instanceof com.github.alexmodguy.alexscaves.server.item.ACDynamicAttributeItem dynamic) {
            return com.github.alexmodguy.alexscaves.server.misc.ACCompat.itemAttributes(
                    dynamic::acModifiers,
                    self,
                    () -> (net.minecraft.world.item.component.ItemAttributeModifiers) original);
        }
        return original;
    }
    *///?} elif fabric && >=1.20.5 {
    /*@com.llamalad7.mixinextras.injector.ModifyExpressionValue(
            method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
            at = @org.spongepowered.asm.mixin.injection.At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getOrDefault(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object ac_dynamicAttributeModifiers(Object original) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.getItem() instanceof com.github.alexmodguy.alexscaves.server.item.ACDynamicAttributeItem dynamic) {
            return com.github.alexmodguy.alexscaves.server.misc.ACCompat.itemAttributes(
                    dynamic::acModifiers,
                    self,
                    () -> (net.minecraft.world.item.component.ItemAttributeModifiers) original);
        }
        return original;
    }
    *///?}
}
