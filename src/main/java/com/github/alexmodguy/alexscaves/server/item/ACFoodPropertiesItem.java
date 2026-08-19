package com.github.alexmodguy.alexscaves.server.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * An item whose nutrition depends on the individual stack rather than on the item.
 *
 * <p>Vanilla below 1.20.5 hangs one immutable {@code FoodProperties} off the {@code Item}, so a
 * biome treat that has already been used cannot be worth less than a fresh one. The loaders' patch
 * ({@code IForgeItem}, NeoForge's {@code IItemExtension}) adds a stack-aware
 * {@code getFoodProperties(ItemStack, LivingEntity)} and asks that instead, which is the only reason
 * the distinction exists at all before 1.20.5.
 *
 * <p>From 1.20.5 the stack carries its own {@code FOOD} data component and vanilla answers the
 * question itself, so there is nothing left for an item to override — hence the gate, which matches
 * {@code BiomeTreatItem}'s own. The {@code implements} clause stays unconditional and the interface
 * is simply empty above the band, for the reasons spelled out on {@code ACUpdatePacketReceiver}.
 *
 * <p>On Fabric the dispatch lives in {@code ACCompat#food(ItemStack, LivingEntity)}, and the three
 * vanilla call sites the loaders patch to use their stack-aware form are redirected onto it:
 * {@code FoodData#eat(Item, ItemStack)} (nutrition — {@code FoodDataMixin}) and
 * {@code LivingEntity#shouldTriggerItemUseEffects} / {@code #addEatEffect} (fast-food pacing and eat
 * effects — {@code mixin.fabric.LivingEntityFoodMixin}).
 */
public interface ACFoodPropertiesItem {

    //? if <1.20.5 {
    FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity);
    //?}
}
