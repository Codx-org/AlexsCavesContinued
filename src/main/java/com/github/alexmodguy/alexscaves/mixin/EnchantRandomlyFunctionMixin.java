package com.github.alexmodguy.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.enchantment.ACWeaponEnchantment;
import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
// 1.21.2 deleted EnchantedBookItem — an enchanted book is a plain Item with components now, and the
// one static this used moved to EnchantmentHelper#createBook. See the !mc2102-enchantedbook rule.
//? if <1.21.2
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(EnchantRandomlyFunction.class)
public class EnchantRandomlyFunctionMixin {

    // 1.21 passes the enchantment as a Holder, and the mod's own enchantments are no longer instances
    // of a class we can test with instanceof — the registry key's namespace is the identity that
    // survived. It also has no static registry left to draw a replacement from (enchantments are a
    // data-pack registry, and nothing reachable from this call site carries a RegistryAccess), so the
    // newer arm suppresses the enchantment rather than swapping in a random vanilla one. That is the
    // same answer to the question the config asks — keep AC's enchantments out of loot — and with the
    // toggle on, the JSON's #minecraft:on_random_loot membership puts them back the ordinary way.
    // 26 threads the whole LootContext through instead of the bare RandomSource (the function can now
    // stamp an ADDITIONAL_TRADE_COST component, which needs the context's parameters) and, with the
    // last static helper gone, `enchantItem` became an INSTANCE method — so this arm's handler is not
    // static either. Mixin rejects a static/non-static mismatch, which is the only reason the two arms
    // below cannot share one body.
    //? if >=26 {
    /*@Inject(
            method = {"Lnet/minecraft/world/level/storage/loot/functions/EnchantRandomlyFunction;enchantItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Holder;Lnet/minecraft/world/level/storage/loot/LootContext;)Lnet/minecraft/world/item/ItemStack;"},
            remap = true,
            at = @At(value = "HEAD"),
            cancellable = true)
    private void ac_enchantItem(ItemStack stack, net.minecraft.core.Holder<Enchantment> enchantment, net.minecraft.world.level.storage.loot.LootContext context, CallbackInfoReturnable<ItemStack> cir) {
        if (!AlexsCaves.COMMON_CONFIG.enchantmentsInLoot.get() && enchantment.unwrapKey().filter(key -> key.location().getNamespace().equals(AlexsCaves.MODID)).isPresent()) {
            cir.setReturnValue(stack);
        }
    }
    *///?} elif >=1.21 {
    /*@Inject(
            method = {"Lnet/minecraft/world/level/storage/loot/functions/EnchantRandomlyFunction;enchantItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Holder;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/item/ItemStack;"},
            remap = true,
            at = @At(value = "HEAD"),
            cancellable = true)
    private static void ac_enchantItem(ItemStack stack, net.minecraft.core.Holder<Enchantment> enchantment, RandomSource randomSource, CallbackInfoReturnable<ItemStack> cir) {
        if (!AlexsCaves.COMMON_CONFIG.enchantmentsInLoot.get() && enchantment.unwrapKey().filter(key -> key.location().getNamespace().equals(AlexsCaves.MODID)).isPresent()) {
            cir.setReturnValue(stack);
        }
    }
    *///?} else {
    @Inject(
            method = {"Lnet/minecraft/world/level/storage/loot/functions/EnchantRandomlyFunction;enchantItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/item/ItemStack;"},
            remap = true,
            at = @At(value = "HEAD"),
            cancellable = true)
    private static void ac_enchantItem(ItemStack stack, Enchantment enchantment, RandomSource randomSource, CallbackInfoReturnable<ItemStack> cir) {
        if(enchantment instanceof ACWeaponEnchantment && !AlexsCaves.COMMON_CONFIG.enchantmentsInLoot.get()){
            Enchantment enchantment1 = enchantment;
            boolean flag = stack.is(Items.BOOK);
            List<Enchantment> list = BuiltInRegistries.ENCHANTMENT.stream().filter(Enchantment::isDiscoverable).filter((enchantment2) -> {
                return flag || enchantment2.canEnchant(stack);
            }).collect(Collectors.toList());
            int tries = 0;
            while(enchantment1 instanceof ACWeaponEnchantment && tries < 100){
                enchantment1 = Util.getRandom(list, randomSource);
                tries++;
            }
            cir.setReturnValue(enchantItemNormally(stack, enchantment1, randomSource));
        }
    }

    private static ItemStack enchantItemNormally(ItemStack itemStack, Enchantment enchantment, RandomSource randomSource) {
        int i = Mth.nextInt(randomSource, enchantment.getMinLevel(), enchantment.getMaxLevel());
        if (itemStack.is(Items.BOOK)) {
            itemStack = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantedBookItem.addEnchantment(itemStack, new EnchantmentInstance(enchantment, i));
        } else {
            itemStack.enchant(enchantment, i);
        }

        return itemStack;
    }
    //?}
}
