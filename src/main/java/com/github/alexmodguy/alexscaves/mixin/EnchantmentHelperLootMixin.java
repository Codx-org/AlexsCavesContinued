package com.github.alexmodguy.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.enchantment.ACLootEnchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

// Drops this mod's enchantments from the pool enchant_with_levels picks from, on the rolls
// `enchantment_loot_chance` says to. Inert outside such a roll (see ACLootEnchantments), so the
// enchanting table and every other caller see vanilla's list untouched. Descriptor bands from javap:
// 1.20.5 added a leading FeatureFlagSet, 1.21 replaced the treasure flag with a Stream of holders.
@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperLootMixin {

    @Inject(
            //? if >=1.21
            /*method = {"Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getAvailableEnchantmentResults(ILnet/minecraft/world/item/ItemStack;Ljava/util/stream/Stream;)Ljava/util/List;"},*/
            //? if >=1.20.5 && <1.21
            /*method = {"Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getAvailableEnchantmentResults(Lnet/minecraft/world/flag/FeatureFlagSet;ILnet/minecraft/world/item/ItemStack;Z)Ljava/util/List;"},*/
            //? if <1.20.5
            method = {"Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getAvailableEnchantmentResults(ILnet/minecraft/world/item/ItemStack;Z)Ljava/util/List;"},
            remap = true,
            at = @At(value = "RETURN"),
            cancellable = true)
    private static void ac_rarerLootEnchantments(CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        List<EnchantmentInstance> filtered = ACLootEnchantments.levelsCandidates(cir.getReturnValue());
        if (filtered != null) {
            cir.setReturnValue(filtered);
        }
    }
}
