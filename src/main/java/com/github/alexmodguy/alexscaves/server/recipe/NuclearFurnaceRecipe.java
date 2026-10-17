package com.github.alexmodguy.alexscaves.server.recipe;

import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class NuclearFurnaceRecipe extends AbstractCookingRecipe {
    // 1.20.2 moved the recipe id out into RecipeHolder, so AbstractCookingRecipe$Factory (which
    // SimpleCookingSerializer calls) no longer passes one. 1.21.2 went further and dropped the
    // RecipeType argument as well — the type is now an abstract method every cooking recipe answers
    // for itself.
    // 26 bundled the arguments instead: the notification flag became Recipe$CommonInfo, the group and
    // book category became AbstractCookingRecipe$CookingBookInfo, and the result became an
    // ItemStackTemplate. The parameter list still mirrors the factory the serializer calls, one for one.
    //? if >=26 {
    /*public NuclearFurnaceRecipe(net.minecraft.world.item.crafting.Recipe.CommonInfo commonInfo,
            AbstractCookingRecipe.CookingBookInfo bookInfo, Ingredient ingredient,
            net.minecraft.world.item.ItemStackTemplate result, float experience, int cookingTime) {
        super(commonInfo, bookInfo, ingredient, result, experience, cookingTime);
    }
    *///?} elif >=1.21.2 {
    /*public NuclearFurnaceRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int cookingTime) {
        super(group, category, ingredient, result, experience, cookingTime);
    }
    *///?} elif >=1.20.2 {
    /*public NuclearFurnaceRecipe(String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int cookingTime) {
        super(ACRecipeRegistry.NUCLEAR_FURNACE_TYPE.get(), group, category, ingredient, result, experience, cookingTime);
    }
    *///?} else {
    public NuclearFurnaceRecipe(ResourceLocation name, String group, CookingBookCategory category, Ingredient ingredient, ItemStack result, float experience, int cookingTime) {
        super(ACRecipeRegistry.NUCLEAR_FURNACE_TYPE.get(), name, group, category, ingredient, result, experience, cookingTime);
    }
    //?}

    // The recipe book's icon for this recipe family. Up to 1.21.1 that was the generic
    // Recipe#getToastSymbol; 1.21.2 replaced it with a cooking-specific furnaceIcon, and made the
    // recipe answer its own type and book category rather than carrying them as constructor state.
    //? if >=1.21.2 {
    /*protected net.minecraft.world.item.Item furnaceIcon() {
        return ACBlockRegistry.NUCLEAR_FURNACE.get().asItem();
    }

    public net.minecraft.world.item.crafting.RecipeType<? extends AbstractCookingRecipe> getType() {
        return ACRecipeRegistry.NUCLEAR_FURNACE_TYPE.get();
    }

    public net.minecraft.world.item.crafting.RecipeBookCategory recipeBookCategory() {
        return net.minecraft.world.item.crafting.RecipeBookCategories.FURNACE_MISC;
    }

    public RecipeSerializer<? extends AbstractCookingRecipe> getSerializer() {
        return (RecipeSerializer<? extends AbstractCookingRecipe>) (RecipeSerializer<?>) ACRecipeRegistry.NUCLEAR_FURNACE.get();
    }
    *///?} else {
    public ItemStack getToastSymbol() {
        return new ItemStack(ACBlockRegistry.NUCLEAR_FURNACE.get());
    }

    public RecipeSerializer<?> getSerializer() {
        return ACRecipeRegistry.NUCLEAR_FURNACE.get();
    }
    //?}
}
