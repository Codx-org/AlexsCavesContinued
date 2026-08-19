package com.github.alexmodguy.alexscaves.server.misc;

import net.minecraft.world.item.ItemStack;

/**
 * The slice of an entity's automation inventory the gingerbread man's stealing goal needs — read a
 * slot, take one item out of it — behind a spelling that does not change with the loader.
 *
 * <p>It exists because NeoForge 21.9 replaced {@code IItemHandler} with the transfer API
 * ({@code ResourceHandler<ItemResource>}), which is not a renamed interface but a different model:
 * a slot holds a resource plus an amount rather than an {@link ItemStack}, and a removal has to run
 * inside a transaction. Nothing the goal does needs either concept, so the two are reconciled here
 * instead of at the call sites — and this is the shape the Fabric nodes will need as well, since
 * they have no item-handler capability at all.
 *
 * @see ACPlatform#entityItemHandler
 */
public interface ACItemAccess {

    int size();

    /**
     * The stack in a slot, for inspection only. Never modify what this returns — on the transfer API
     * there is no live stack behind a slot to modify, so a write would be silently lost.
     */
    ItemStack stackInSlot(int slot);

    /**
     * Removes a single item from a slot and returns a copy of what was removed, or
     * {@link ItemStack#EMPTY} if nothing could be taken.
     */
    ItemStack takeOne(int slot);
}
