package com.github.alexmodguy.alexscaves.server.potion;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import net.minecraftforge.common.brewing.BrewingRecipe;

import javax.annotation.Nonnull;

/**
 * A brewing recipe whose input bottle is matched on its whole contents, not just its item — every one
 * of this mod's eleven recipes brews one potion into another, so "a potion" is never a precise enough
 * input.
 *
 * <p>The template is held as an {@link ItemStack} rather than as an {@code Ingredient} on purpose. An
 * {@code Ingredient} stopped being able to carry one from 1.21.2 (it is a plain item set there, so the
 * loaders' component-aware subclasses still report only the bare item from {@code items()}), and from
 * Forge 62 there is no component-aware subclass left at all — its {@code NBTIngredient} matches
 * {@code CUSTOM_DATA} alone, which every potion shares. Comparing against the stack directly needs no
 * loader ingredient and no version gate, and is what the old strict-ingredient spelling meant anyway.
 */
class ProperBrewingRecipe extends BrewingRecipe {

    private final ItemStack template;

    public ProperBrewingRecipe(ItemStack template, Ingredient ingredient, ItemStack output) {
        super(Ingredient.of(template.getItem()), ingredient, output);
        this.template = template;
    }

    @Override
    public boolean isInput(@Nonnull ItemStack stack) {
        return stack != null && ACCompat.sameItemSameData(this.template, stack);
    }

}
