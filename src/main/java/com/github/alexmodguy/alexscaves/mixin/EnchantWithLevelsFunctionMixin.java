package com.github.alexmodguy.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.enchantment.ACLootEnchantments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Marks the EnchantmentHelper#enchantItem call made by the enchant_with_levels loot function, so
// EnchantmentHelperLootMixin can apply `enchantment_loot_chance` to that roll and to nothing else.
// The call's descriptor changed twice (javap, all 22 MC versions): 1.20.5 added a leading
// FeatureFlagSet, and 1.21 dropped the treasure flag for a RegistryAccess and the optional
// HolderSet of allowed enchantments.
@Mixin(EnchantWithLevelsFunction.class)
public class EnchantWithLevelsFunctionMixin {

    //? if >=1.21 {
    /*@WrapOperation(
            method = {"Lnet/minecraft/world/level/storage/loot/functions/EnchantWithLevelsFunction;run(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/storage/loot/LootContext;)Lnet/minecraft/world/item/ItemStack;"},
            remap = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;enchantItem(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/core/RegistryAccess;Ljava/util/Optional;)Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack ac_enchantWithLevels(RandomSource random, ItemStack stack, int levels, net.minecraft.core.RegistryAccess registryAccess, java.util.Optional<?> options, Operation<ItemStack> original) {
        return ACLootEnchantments.duringLevelsRoll(random, () -> original.call(random, stack, levels, registryAccess, options));
    }
    *///?} elif >=1.20.5 {
    /*@WrapOperation(
            method = {"Lnet/minecraft/world/level/storage/loot/functions/EnchantWithLevelsFunction;run(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/storage/loot/LootContext;)Lnet/minecraft/world/item/ItemStack;"},
            remap = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;enchantItem(Lnet/minecraft/world/flag/FeatureFlagSet;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/item/ItemStack;IZ)Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack ac_enchantWithLevels(net.minecraft.world.flag.FeatureFlagSet features, RandomSource random, ItemStack stack, int levels, boolean treasure, Operation<ItemStack> original) {
        return ACLootEnchantments.duringLevelsRoll(random, () -> original.call(features, random, stack, levels, treasure));
    }
    *///?} else {
    @WrapOperation(
            method = {"Lnet/minecraft/world/level/storage/loot/functions/EnchantWithLevelsFunction;run(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/storage/loot/LootContext;)Lnet/minecraft/world/item/ItemStack;"},
            remap = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;enchantItem(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/item/ItemStack;IZ)Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack ac_enchantWithLevels(RandomSource random, ItemStack stack, int levels, boolean treasure, Operation<ItemStack> original) {
        return ACLootEnchantments.duringLevelsRoll(random, () -> original.call(random, stack, levels, treasure));
    }
    //?}
}
