package com.github.alexmodguy.alexscaves.fabric.forge.common.brewing;

import net.minecraft.world.item.ItemStack;

/**
 * Fabric stand-in for the loaders' brewing-recipe interface — the shape
 * {@code ACEffectRegistry#registerBrewing} hands its eleven recipes out as, and therefore the one
 * type every era and loader agrees on (see that method's javadoc for why the list is written once
 * and the sink differs).
 *
 * <p>Three methods, exactly the loaders': is this the bottle, is this the ingredient, and what comes
 * out. Vanilla has no equivalent — its own brewing is a fixed table of item-to-item mixes with no
 * way to ask a recipe a question — which is why the interface has to exist here at all rather than
 * being renamed onto something Minecraft already ships.
 *
 * <p>Nothing on Fabric consumes one yet. {@link BrewingRecipeRegistry} collects them; the mixin that
 * feeds vanilla's brewing stand from that collection is part of the event-dispatch batch.
 */
public interface IBrewingRecipe {

    boolean isInput(ItemStack input);

    boolean isIngredient(ItemStack ingredient);

    ItemStack getOutput(ItemStack input, ItemStack ingredient);
}
