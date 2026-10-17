package com.github.alexmodguy.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.item.PrimordialArmorItem;
import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The primordial-armour saturation bonus, from 1.21.2 on.
 *
 * <p>Its history is one hook chasing another: Forge's eater-aware {@code FoodData#eat(Item,
 * ItemStack, LivingEntity)} up to 1.20.4 (still owned by {@code FoodDataMixin} there), then the
 * {@code FoodData#eat} call inside {@code Player#eat} up to 1.21.1 (see {@code PlayerMixin}).
 * 1.21.2 turned eating into a {@code Consumable} data component and deleted {@code Player#eat}; the
 * one place that still knows the eater, the stack and the food values at once is
 * {@code FoodProperties#onConsume}, which is a different class — hence a mixin of its own rather
 * than another arm in {@code PlayerMixin}.
 *
 * <p>This class does not exist below 1.21.2: {@code ModPlatformPlugin} drops it from the source set
 * and {@code DataPackMigration} strips it from the mixin config.
 */
@Mixin(FoodProperties.class)
public abstract class FoodPropertiesMixin {

    @Redirect(
            method = {"Lnet/minecraft/world/food/FoodProperties;onConsume(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/component/Consumable;)V"},
            remap = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V")
    )
    private void ac_eat(FoodData foodData, FoodProperties foodProperties, Level acLevel, LivingEntity eater, ItemStack stack, net.minecraft.world.item.component.Consumable consumable) {
        int extraShanksFromArmor = stack.is(ACTagRegistry.RAW_MEATS) ? PrimordialArmorItem.getExtraSaturationFromArmor(eater) : 0;
        if (extraShanksFromArmor != 0 && foodProperties != null) {
            foodData.eat(ACCompat.nutrition(foodProperties) + extraShanksFromArmor, ACCompat.saturationModifier(foodProperties) + (extraShanksFromArmor * 0.125F));
        } else {
            foodData.eat(foodProperties);
        }
    }
}
