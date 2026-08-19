package com.github.alexmodguy.alexscaves.server.recipe;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
//? if <1.21.2 {
import net.minecraft.world.item.crafting.SimpleCookingSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
//?}
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;

public class ACRecipeRegistry {
    public static final DeferredRegister<RecipeType<?>> TYPE_DEF_REG = DeferredRegister.create(Registries.RECIPE_TYPE, AlexsCaves.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> DEF_REG = DeferredRegister.create(Registries.RECIPE_SERIALIZER, AlexsCaves.MODID);

    public static final Supplier<RecipeType<NuclearFurnaceRecipe>> NUCLEAR_FURNACE_TYPE = TYPE_DEF_REG.register("nuclear_furnace", () -> new RecipeType<>() {
    });

    // 1.21.2 deleted both of the little serializer wrappers upstream used here. The replacements sit
    // on the recipe classes themselves — CustomRecipe.Serializer takes the same single-argument
    // factory, and AbstractCookingRecipe.Serializer the same factory-plus-default-cooking-time pair.
    // 26 deleted those two as well, and made RecipeSerializer itself a plain record of a MapCodec and
    // a StreamCodec — so a serializer is now assembled rather than subclassed. The cooking half has a
    // direct pair of factories on AbstractCookingRecipe (same factory, same default cooking time); the
    // cave map's pair lives on the recipe, because it encodes only the book info and that field is
    // protected on NormalCraftingRecipe, i.e. reachable from the subclass alone.
    //? if >=26 {
    /*public static final Supplier<RecipeSerializer<?>> CAVE_MAP = DEF_REG.register("cave_map", RecipeCaveMap::serializer);
    public static final Supplier<RecipeSerializer<?>> NUCLEAR_FURNACE = DEF_REG.register("nuclear_furnace", () -> new RecipeSerializer<>(
            net.minecraft.world.item.crafting.AbstractCookingRecipe.cookingMapCodec(NuclearFurnaceRecipe::new, 100),
            net.minecraft.world.item.crafting.AbstractCookingRecipe.cookingStreamCodec(NuclearFurnaceRecipe::new)));
    *///?} elif >=1.21.2 {
    /*public static final Supplier<RecipeSerializer<?>> CAVE_MAP = DEF_REG.register("cave_map", () -> new net.minecraft.world.item.crafting.CustomRecipe.Serializer<>(RecipeCaveMap::new));
    public static final Supplier<RecipeSerializer<?>> NUCLEAR_FURNACE = DEF_REG.register("nuclear_furnace", () -> new net.minecraft.world.item.crafting.AbstractCookingRecipe.Serializer<>(NuclearFurnaceRecipe::new, 100));
    *///?} else {
    public static final Supplier<RecipeSerializer<?>> CAVE_MAP = DEF_REG.register("cave_map", () -> new SimpleCraftingRecipeSerializer<>(RecipeCaveMap::new));
    public static final Supplier<RecipeSerializer<?>> NUCLEAR_FURNACE = DEF_REG.register("nuclear_furnace", () -> new SimpleCookingSerializer<>(NuclearFurnaceRecipe::new, 100));
    //?}
}
