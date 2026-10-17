package com.github.alexmodguy.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import com.github.alexmodguy.alexscaves.citadel.server.entity.IModifiesTime;
import com.github.alexmodguy.alexscaves.citadel.server.tick.modifier.LocalEntityTickRateModifier;
import com.github.alexmodguy.alexscaves.citadel.server.tick.modifier.TickRateModifier;
import com.github.alexmodguy.alexscaves.server.item.PrimordialArmorItem;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements IModifiesTime {

    @Shadow public abstract float getSpeed();

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }


    @Inject(
            method = {"Lnet/minecraft/world/entity/player/Player;getSpeed()F"},
            remap = true,
            cancellable = true,
            at = @At(value = "RETURN")
    )
    public void ac_getSpeed(CallbackInfoReturnable<Float> cir) {
        if (AlexsCaves.COMMON_CONFIG.sugarRushSlowsTime.get() && this.hasEffect(ACCompat.effect(ACEffectRegistry.SUGAR_RUSH.get())) && AlexsCaves.PROXY.isTickRateModificationActive(this.level())) {
            cir.setReturnValue(cir.getReturnValue() * 3.0F);
        }
    }

    @Inject(
            method = {"Lnet/minecraft/world/entity/player/Player;getFlyingSpeed()F"},
            remap = true,
            cancellable = true,
            at = @At(value = "RETURN")
    )
    public void ac_getFlyingSpeed(CallbackInfoReturnable<Float> cir) {
        if (AlexsCaves.COMMON_CONFIG.sugarRushSlowsTime.get() && this.hasEffect(ACCompat.effect(ACEffectRegistry.SUGAR_RUSH.get())) && AlexsCaves.PROXY.isTickRateModificationActive(this.level())) {
            cir.setReturnValue(this.getSpeed() * 0.5F);
        }
    }

    // The primordial-armour saturation bonus used to hang off Forge's eater-aware
    // FoodData#eat(Item, ItemStack, LivingEntity) — see FoodDataMixin, which still owns it below
    // 1.20.5. That overload went away with the food component, so from 1.20.5 on the same rule is
    // applied by redirecting the one FoodData#eat(ItemStack) call inside Player#eat, which is the
    // only place left that knows both the eater and the stack.
    // 1.21 moved the FoodProperties lookup out of Player#eat — the caller resolves the component
    // and hands it in, so both the enclosing method's descriptor and the redirected call's move.
    // The redirect handler appends the target's own arguments to get the stack back, which is the
    // only thing it needs beyond the properties it is already given.
    // 1.21.2 removes Player#eat outright — eating is a Consumable now and the FoodData call lives in
    // FoodProperties#onConsume, a different class, so from there this hangs off FoodPropertiesMixin.
    //? if >=1.21 && <1.21.2 {
    /*@Redirect(
            method = {"Lnet/minecraft/world/entity/player/Player;eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;"},
            remap = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V")
    )
    private void ac_eat(FoodData foodData, FoodProperties foodProperties, Level acLevel, ItemStack stack, FoodProperties acFoodProperties) {
        int extraShanksFromArmor = stack.is(ACTagRegistry.RAW_MEATS) ? PrimordialArmorItem.getExtraSaturationFromArmor(this) : 0;
        if (extraShanksFromArmor != 0 && foodProperties != null) {
            foodData.eat(ACCompat.nutrition(foodProperties) + extraShanksFromArmor, ACCompat.saturationModifier(foodProperties) + (extraShanksFromArmor * 0.125F));
        } else {
            foodData.eat(foodProperties);
        }
    }
    *///?}

    // Between 1.20.5 and 1.21 the redirected call is a LOADER PATCH, and the two loaders patched it
    // differently: NeoForge restored an eater-aware FoodData#eat(ItemStack, LivingEntity) and made
    // Player#eat call that, while Forge left the vanilla FoodData#eat(ItemStack) in place. Vanilla
    // (so every Fabric node) matches Forge. A `@Redirect` is matched on the target descriptor, so the
    // Forge spelling simply finds nothing on NeoForge: "Redirector ac_eat ... failed injection check,
    // (0/1) succeeded" and the game dies. Hence one arm per loader rather than one per version.
    //? if neoforge && >=1.20.5 && <1.21 {
    /*@Redirect(
            method = {"Lnet/minecraft/world/entity/player/Player;eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"},
            remap = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V")
    )
    private void ac_eat(FoodData foodData, ItemStack stack, LivingEntity eater) {
        int extraShanksFromArmor = stack.is(ACTagRegistry.RAW_MEATS) ? PrimordialArmorItem.getExtraSaturationFromArmor(this) : 0;
        FoodProperties foodProperties = extraShanksFromArmor == 0 ? null : ACCompat.food(stack, this);
        if (foodProperties != null) {
            foodData.eat(ACCompat.nutrition(foodProperties) + extraShanksFromArmor, ACCompat.saturationModifier(foodProperties) + (extraShanksFromArmor * 0.125F));
        } else {
            foodData.eat(stack, eater);
        }
    }
    *///?}

    //? if !neoforge && >=1.20.5 && <1.21 {
    /*@Redirect(
            method = {"Lnet/minecraft/world/entity/player/Player;eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"},
            remap = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/item/ItemStack;)V")
    )
    private void ac_eat(FoodData foodData, ItemStack stack) {
        int extraShanksFromArmor = stack.is(ACTagRegistry.RAW_MEATS) ? PrimordialArmorItem.getExtraSaturationFromArmor(this) : 0;
        FoodProperties foodProperties = extraShanksFromArmor == 0 ? null : ACCompat.food(stack, this);
        if (foodProperties != null) {
            foodData.eat(ACCompat.nutrition(foodProperties) + extraShanksFromArmor, ACCompat.saturationModifier(foodProperties) + (extraShanksFromArmor * 0.125F));
        } else {
            foodData.eat(stack);
        }
    }
    *///?}

    // ...and below 1.20.5 the same split runs the other way. FoodDataMixin owns this hook there by
    // injecting into Forge's eater-aware FoodData#eat(Item, ItemStack, LivingEntity) — which is a
    // loader PATCH, so on Fabric it does not exist and that injection is gated off. Vanilla's own
    // overload is the two-argument FoodData#eat(Item, ItemStack), and the one place that calls it is
    // Player#eat, which is where the eater is `this` — i.e. exactly the arrangement 1.20.5 later
    // forced on every loader, arrived at four versions early. The bodies are identical apart from
    // the extra Item the older call carries and hands straight back.
    //? if fabric && <1.20.5 {
    /*@Redirect(
            method = {"Lnet/minecraft/world/entity/player/Player;eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"},
            remap = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;)V")
    )
    private void ac_eat(FoodData foodData, net.minecraft.world.item.Item item, ItemStack stack) {
        int extraShanksFromArmor = stack.is(ACTagRegistry.RAW_MEATS) ? PrimordialArmorItem.getExtraSaturationFromArmor(this) : 0;
        FoodProperties foodProperties = extraShanksFromArmor == 0 ? null : ACCompat.food(stack, this);
        if (foodProperties != null) {
            foodData.eat(ACCompat.nutrition(foodProperties) + extraShanksFromArmor, ACCompat.saturationModifier(foodProperties) + (extraShanksFromArmor * 0.125F));
        } else {
            foodData.eat(item, stack);
        }
    }
    *///?}

    @Override
    public boolean isTimeModificationValid(TickRateModifier tickRateModifier){
        return !(tickRateModifier instanceof LocalEntityTickRateModifier) || this.hasEffect(ACCompat.effect(ACEffectRegistry.SUGAR_RUSH.get()));
    }
}
