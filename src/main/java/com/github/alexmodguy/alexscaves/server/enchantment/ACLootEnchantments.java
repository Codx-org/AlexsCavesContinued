package com.github.alexmodguy.alexscaves.server.enchantment;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.List;
import java.util.function.Supplier;

// The `enchantment_loot_chance` server option: how often this mod's enchantments are allowed to be
// among the candidates when a loot table rolls a random enchantment. The decision is made once per
// roll, so at 0.5 half of all rolls see the full pool and half see it with this mod's enchantments
// removed — the relative odds between vanilla enchantments never change. It applies to both loot
// functions that pick enchantments: enchant_randomly (EnchantRandomlyFunctionMixin) and
// enchant_with_levels (EnchantWithLevelsFunctionMixin + EnchantmentHelperLootMixin). It stacks on
// top of `enchantments_in_loot`, which keeps blocking exactly what it blocked before.
public final class ACLootEnchantments {

    // Set only while EnchantWithLevelsFunction is inside EnchantmentHelper#enchantItem, so the filter
    // on getAvailableEnchantmentResults never touches an enchanting table. null = not in a loot roll.
    private static final ThreadLocal<Boolean> LEVELS_ROLL_ALLOWS_MOD = new ThreadLocal<>();

    private ACLootEnchantments() {
    }

    public static boolean allowOnThisRoll(RandomSource random) {
        double chance = AlexsCaves.COMMON_CONFIG.enchantmentLootChance.get();
        return chance >= 1.0D || (chance > 0.0D && random.nextDouble() < chance);
    }

    public static boolean isModEnchantment(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey().filter(key -> key.location().getNamespace().equals(AlexsCaves.MODID)).isPresent();
    }

    public static boolean isModEnchantment(EnchantmentInstance instance) {
        //? if >=1.21.5
        /*return isModEnchantment(instance.enchantment());*/
        //? if >=1.21 && <1.21.5
        /*return isModEnchantment(instance.enchantment);*/
        //? if <1.21
        return instance.enchantment instanceof ACWeaponEnchantment;
    }

    // enchant_randomly from 1.21: the candidate list it is about to pick from. If filtering would
    // leave nothing (an item only this mod's enchantments fit), the list is left alone — the item
    // keeps a fitting enchantment rather than coming out of the chest unenchanted.
    public static List<Holder<Enchantment>> randomlyCandidates(List<Holder<Enchantment>> candidates, RandomSource random) {
        if (AlexsCaves.COMMON_CONFIG.enchantmentsInLoot.get() && allowOnThisRoll(random)) {
            return candidates;
        }
        List<Holder<Enchantment>> filtered = candidates.stream().filter(holder -> !isModEnchantment(holder)).toList();
        return filtered.isEmpty() ? candidates : filtered;
    }

    public static ItemStack duringLevelsRoll(RandomSource random, Supplier<ItemStack> roll) {
        Boolean previous = LEVELS_ROLL_ALLOWS_MOD.get();
        LEVELS_ROLL_ALLOWS_MOD.set(allowOnThisRoll(random));
        try {
            return roll.get();
        } finally {
            if (previous == null) {
                LEVELS_ROLL_ALLOWS_MOD.remove();
            } else {
                LEVELS_ROLL_ALLOWS_MOD.set(previous);
            }
        }
    }

    // Returns the list to hand back from getAvailableEnchantmentResults, or null to leave it as is.
    // selectEnchantment removes entries from what it gets, so the replacement must stay mutable.
    public static List<EnchantmentInstance> levelsCandidates(List<EnchantmentInstance> available) {
        Boolean allowed = LEVELS_ROLL_ALLOWS_MOD.get();
        if (allowed == null || allowed || available.stream().noneMatch(ACLootEnchantments::isModEnchantment)) {
            return null;
        }
        List<EnchantmentInstance> filtered = new java.util.ArrayList<>(available);
        filtered.removeIf(ACLootEnchantments::isModEnchantment);
        return filtered.isEmpty() ? null : filtered;
    }
}
