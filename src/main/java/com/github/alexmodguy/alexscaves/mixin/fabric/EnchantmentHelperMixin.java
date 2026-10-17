package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.server.enchantment.ACWeaponEnchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes the enchanting table ask an enchantment whether it accepts an item, rather than asking the
 * {@code EnchantmentCategory} it was built with — which is precisely the line Forge patches, and
 * which this loader needs for the same reason Forge added it.
 *
 * <p>Below 1.20.5 an enchantment's applicability is an {@code EnchantmentCategory}, an <em>enum</em>
 * that Forge lets a mod extend and Fabric does not. So Alex's Caves' fifty-one enchantments are
 * built here with vanilla's {@code VANISHABLE} as a placeholder and answer
 * {@link ACWeaponEnchantment#canEnchant(ItemStack)} out of a predicate instead — see
 * {@code ACEnchantmentRegistry} for the whole argument. Every consumer of applicability goes through
 * {@code canEnchant} and is therefore already correct; {@code getAvailableEnchantmentResults} is the
 * one exception, reading {@code enchantment.category.canEnchant(item)} straight off the field. Left
 * alone it would offer all fifty-one on anything vanishable, which is nearly every tool and weapon
 * in the game.
 *
 * <p>The redirect is on {@code isDiscoverable()} rather than on the category call one line further
 * down, because that invoke's receiver <em>is</em> the enchantment being tested — so the handler
 * needs neither a captured local nor the enclosing loop's state, only the {@link ItemStack} the
 * target method already takes. The two calls are the same {@code &&} chain and short-circuiting
 * either one excludes the enchantment identically. The book case is carried over verbatim: vanilla
 * ORs the category test with "the stack is a book", so a book still offers everything discoverable.
 *
 * <p>From 1.20.5 applicability is an item tag, the mod uses real tags, and none of this exists —
 * neither {@code Enchantment#isDiscoverable} nor {@code ACWeaponEnchantment#canEnchant}. The mixin
 * is then simply empty, which costs nothing and keeps the entry out of the loader-specific pruning
 * that {@code mixin.fabric} already does per loader.
 */
@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    //? if <1.20.5 {
    @Redirect(
            method = "getAvailableEnchantmentResults(ILnet/minecraft/world/item/ItemStack;Z)Ljava/util/List;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;isDiscoverable()Z"))
    private static boolean ac_isDiscoverable(Enchantment enchantment, int level, ItemStack stack, boolean allowTreasure) {
        if (!enchantment.isDiscoverable()) {
            return false;
        }
        if (enchantment instanceof ACWeaponEnchantment weapon) {
            return weapon.canEnchant(stack) || stack.is(Items.BOOK);
        }
        return true;
    }
    //?}
}
