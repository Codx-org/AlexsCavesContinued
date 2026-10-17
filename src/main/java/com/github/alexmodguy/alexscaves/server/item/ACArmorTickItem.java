package com.github.alexmodguy.alexscaves.server.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A piece of armour that does work every tick while it is worn.
 *
 * <p>{@code Item#onArmorTick} is a <b>loader patch</b> ({@code IForgeItem}, NeoForge's
 * {@code IItemExtension}) with an empty default, and it exists only below 1.21 — from there the
 * per-tick call that reaches a worn piece is vanilla's own {@code Item#inventoryTick}, which
 * {@code Inventory#tick} runs over the armour compartment too, and both implementors switch to
 * {@code ACTickingItem} with a slot guard. That is why the declaration is gated to the same band as
 * the two overrides it legalises, and why the interface is an empty marker above it.
 *
 * <p>Unconditional as an {@code implements} clause, and declared with the loaders' exact name and
 * signature, for the reasons spelled out on {@code ACUpdatePacketReceiver} — the gate is on the
 * <i>method</i>, never on the clause, so no implementor's class declaration had to grow an arm.
 */
public interface ACArmorTickItem {

    //? if <1.21 {
    void onArmorTick(ItemStack stack, Level level, Player player);
    //?}
}
