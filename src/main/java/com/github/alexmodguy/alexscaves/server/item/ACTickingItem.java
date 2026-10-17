package com.github.alexmodguy.alexscaves.server.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * An item that wants a tick while it merely sits in an inventory, on both logical sides.
 *
 * <p>Up to 1.21.4 that was {@code Item#inventoryTick(ItemStack, Level, Entity, int, boolean)},
 * called from {@code ItemStack#inventoryTick} on the client and the server alike. 1.21.5 narrowed
 * the hook to {@code Item#inventoryTick(ItemStack, ServerLevel, Entity, EquipmentSlot)}, which
 * vanilla forwards to only when the level is a {@code ServerLevel} — and neither loader put a
 * client-side equivalent in its place. Six of this mod's seventeen ticking items do their work on
 * the client (the raygun and dreadbow charge animations, the shotgum crank, the gauntlet, the
 * resistor shield and the darkness armour), so the narrowed hook would have silently stopped them.
 *
 * <p>The dispatch therefore does not go through {@code Item} at all: {@code ItemStackTickMixin}
 * calls this interface from the head of {@code ItemStack#inventoryTick}, which is the one call site
 * vanilla has ever had for the feature and is still reached on both sides on every version.
 *
 * <p>The inventory slot index that the old signature carried is gone from the parameter list — no
 * implementor ever read it, 1.21.5 does not pass it down, and inventing one from the
 * {@code EquipmentSlot} would be a different number wearing the same name.
 */
public interface ACTickingItem {

    void acInventoryTick(ItemStack stack, Level level, Entity entity, boolean selected);
}
