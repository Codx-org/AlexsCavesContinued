package com.github.alexmodguy.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.item.PrimordialArmorItem;
import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    @Shadow
    public abstract void eat(int nutrition, float saturation);

    // Forge's three-argument FoodData#eat, which was the only one that knew who was eating, is gone
    // from 1.20.5 on — food became a data component and the eater-aware overload went with it. From
    // there the same hook lives on Player#eat instead; see PlayerMixin#ac_eat.
    // It is also a loader PATCH rather than vanilla, so it does not exist on Fabric at ANY version:
    // below 1.20.5 that node takes the Player#eat seam early (PlayerMixin's `fabric && <1.20.5` arm),
    // and this class is simply an empty mixin there. A selector naming a method the jar does not have
    // is not a compile error — it surfaces as a `Cannot remap` warning at remapJar and a hard failure
    // at boot under defaultRequire: 1, which is how this was found.
    //? if !fabric && <1.20.5 {
    @Inject(
            method = {"Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V"},
            cancellable = true,
            remap = false, //FORGE METHOD
            at = @At(value = "HEAD")
    )
    public void ac_eat(Item item, ItemStack stack, LivingEntity entity, CallbackInfo ci) {
        if (entity != null && stack.is(ACTagRegistry.RAW_MEATS)) {
            int extraShanksFromArmor = PrimordialArmorItem.getExtraSaturationFromArmor(entity);
            if (extraShanksFromArmor != 0) {
                ci.cancel();
                if (ACCompat.isEdible(stack)) {
                    FoodProperties foodproperties = ACCompat.food(stack, entity);
                    this.eat(ACCompat.nutrition(foodproperties) + extraShanksFromArmor, ACCompat.saturationModifier(foodproperties) + (extraShanksFromArmor * 0.125F));
                }
            }
        }
    }
    //?}

    // The other half of what that Forge overload does, and the half every item gets rather than only
    // the raw meats: it asks the STACK for its food, not the item. Vanilla's two-argument overload —
    // the one Fabric actually runs below 1.20.5 — reads Item#getFoodProperties(), which cannot vary
    // per stack, so ACFoodPropertiesItem would silently mean nothing and the biome treat would feed
    // the wrong number of shanks (or, having no item-level food at all, NPE on getNutrition). This
    // redirect is the loaders' patch expressed at their own call site; ACCompat.food is the single
    // place the dispatch lives, and its Fabric arm falls through to the vanilla lookup for every item
    // that does not implement the interface.
    // The eater is genuinely unknown here — that is the entire reason the loaders added an overload
    // that carries one — so null is passed, which the interface declares as legal and the mod's one
    // implementor ignores.
    // ⚠ @ModifyExpressionValue rather than @Redirect, and for the reason written up in
    // mixin.fabric.LivingEntityFoodMixin: fabric-item-api-v1 redirects this same instruction, a
    // @Redirect is exclusive, and the mod that loses the tie dies on its own require check. Returning
    // `original` for anything that is not an ACFoodPropertiesItem is what keeps Fabric API's
    // stack-aware food working for every other mod.
    //? if fabric && <1.20.5 {
    /*@com.llamalad7.mixinextras.injector.ModifyExpressionValue(
            method = {"Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;)V"},
            remap = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;getFoodProperties()Lnet/minecraft/world/food/FoodProperties;")
    )
    private FoodProperties ac_foodProperties(FoodProperties original, Item item, ItemStack stack) {
        return stack.getItem() instanceof com.github.alexmodguy.alexscaves.server.item.ACFoodPropertiesItem
                ? ACCompat.food(stack, null) : original;
    }
    *///?}

}
