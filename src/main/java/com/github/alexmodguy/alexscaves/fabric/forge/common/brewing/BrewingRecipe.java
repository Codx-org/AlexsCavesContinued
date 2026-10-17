package com.github.alexmodguy.alexscaves.fabric.forge.common.brewing;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Fabric stand-in for the loaders' ingredient-pair implementation of {@link IBrewingRecipe} — a
 * bottle ingredient, a reagent ingredient, and the stack that comes out. Reproduced rather than
 * simplified away because {@code ProperBrewingRecipe} extends it and overrides only
 * {@link #isInput}, so the other two methods have to behave the way the loaders' do.
 *
 * <p>{@link Ingredient#test} is the one vanilla call this needs and it is spelled the same from
 * 1.20.1 to 26, so the class needs no gate. That is not an accident of this class: the loaders'
 * version stores {@code Ingredient}s too, and the reason this mod's own subclass exists at all is
 * that an {@code Ingredient} cannot carry a potion's components on the newer versions — see
 * {@code ProperBrewingRecipe}.
 *
 * <p>The getters are kept even though nothing in this tree reads them, because the collection in
 * {@link BrewingRecipeRegistry} is going to be walked by a mixin that has to turn each recipe back
 * into something vanilla's brewing stand understands, and the bottle ingredient is the only way to
 * ask which slot a recipe belongs in.
 */
public class BrewingRecipe implements IBrewingRecipe {

    private final Ingredient input;
    private final Ingredient ingredient;
    private final ItemStack output;

    public BrewingRecipe(Ingredient input, Ingredient ingredient, ItemStack output) {
        this.input = input;
        this.ingredient = ingredient;
        this.output = output;
    }

    @Override
    public boolean isInput(ItemStack stack) {
        return this.input.test(stack);
    }

    @Override
    public boolean isIngredient(ItemStack stack) {
        return this.ingredient.test(stack);
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        return isInput(input) && isIngredient(ingredient) ? this.output.copy() : ItemStack.EMPTY;
    }

    public Ingredient getInput() {
        return this.input;
    }

    public Ingredient getIngredient() {
        return this.ingredient;
    }

    public ItemStack getOutput() {
        return this.output;
    }
}
