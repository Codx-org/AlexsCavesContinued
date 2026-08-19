package com.github.alexmodguy.alexscaves.server.item;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * An item that wants to know what killed its dropped entity, not merely that it died.
 *
 * <p>Vanilla's own hook is {@code Item#onDestroyed(ItemEntity)} — the item entity alone, no cause.
 * The loaders widen it: {@code IForgeItem} (NeoForge's {@code IItemExtension}) adds
 * {@code onDestroyed(ItemEntity, DamageSource)} and calls that instead, which is what
 * {@code RadioactiveOnDestroyedBlockItem} overrides so a creative player deleting a stack of nuclear
 * bomb components does not level the room.
 *
 * <p>Unconditional, and declared with the loaders' exact name and signature, for the reasons spelled
 * out on {@code ACUpdatePacketReceiver}. On Fabric the vanilla one-argument method is what actually
 * fires, so the {@code super.onDestroyed(itemEntity, damageSource)} call inside the override is
 * rewritten to the vanilla arity by the {@code !fab-item-ondestroyed-super} replacement rule, and the
 * dispatcher hands the damage source in from the mixin.
 *
 * <p>That dispatcher is {@code mixin.fabric.ItemEntityMixin}, which redirects the single
 * {@code ItemStack#onDestroyed(ItemEntity)} call in {@code ItemEntity#hurt} — the exact call site the
 * loaders patch — and falls through to it unchanged for every item that does not implement this
 * interface, which is what the loaders' own default does.
 */
public interface ACDestroyedItem {

    void onDestroyed(ItemEntity itemEntity, DamageSource damageSource);
}
