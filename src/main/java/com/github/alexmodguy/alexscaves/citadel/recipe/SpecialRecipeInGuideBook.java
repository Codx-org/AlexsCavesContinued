package com.github.alexmodguy.alexscaves.citadel.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/**
 * A recipe that wants to show something other than its own contents in the guide book.
 *
 * <p>Upstream Citadel spelled the ingredient list as {@code NonNullList<Ingredient>}. From 1.21.2
 * an {@code Ingredient} can no longer be asked for its stacks, and a shaped recipe's list became
 * {@code List<Optional<Ingredient>>}, so this is stated in the terms the book actually renders in:
 * one entry per slot, holding the stacks that slot cycles through.
 */
public interface SpecialRecipeInGuideBook {
    java.util.List<ItemStack[]> getDisplayIngredients();

    ItemStack getDisplayResultFor(NonNullList<ItemStack> stacks);
}
