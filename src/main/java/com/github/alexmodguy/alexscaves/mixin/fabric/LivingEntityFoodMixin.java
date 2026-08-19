package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.server.item.ACFoodPropertiesItem;
import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fabric's dispatcher for {@link ACFoodPropertiesItem} on the two eating hooks that live in
 * {@code LivingEntity} — the third is in {@code FoodData} and is owned by {@code FoodDataMixin}.
 *
 * <p>Below 1.20.5 an item's food is a property of the <em>item</em>, so vanilla asks
 * {@code Item#getFoodProperties()} and nothing can vary it per stack. The loaders' patch
 * ({@code IForgeItem}, NeoForge's {@code IItemExtension}) adds a stack-aware
 * {@code ItemStack#getFoodProperties(LivingEntity)} and swaps it in at every vanilla call site; read
 * out of the 1.20.1 merged jars there are exactly three, and this class covers the two in
 * {@code LivingEntity}:
 *
 * <ul>
 *   <li>{@code shouldTriggerItemUseEffects()} — offset 12 vanilla, 10 on Forge, where the stack is
 *       {@code this.useItem} and the answer feeds {@code FoodProperties#isFastFood};</li>
 *   <li>{@code addEatEffect(ItemStack, Level, LivingEntity)} — offset 16 vanilla and on Forge, where
 *       the stack is the first parameter and the answer feeds {@code FoodProperties#getEffects}.</li>
 * </ul>
 *
 * <p>In both the eater the loaders hand in is {@code this} ({@code aload_0}), not
 * {@code addEatEffect}'s trailing {@code LivingEntity} — worth stating because the two are usually
 * the same object and a wrong guess would never show.
 *
 * <p>Neither hook changes anything for the mod's only implementor today: both of the biome treat's
 * profiles are non-fast and carry no effects, and only the nutrition differs (which is the
 * {@code FoodData} site). They are reproduced anyway because the interface is a general extension
 * point, and a stand-in that is complete only for the current implementor is a trap for the next one.
 *
 * <p>Gated to the interface's own band: from 1.20.5 food is a stack data component, vanilla asks the
 * stack itself, and {@code Item#getFoodProperties()} does not exist to redirect.
 *
 * <p>⚠ These are {@code @ModifyExpressionValue}, deliberately, and they were {@code @Redirect} once.
 * Fabric API's own {@code fabric-item-api-v1} redirects the very same two instructions — its
 * {@code getStackAwareFoodComponent} is the same idea reached through {@code FabricItem} — and a
 * {@code @Redirect} is <em>exclusive</em>: two of them at one call site with equal priority means Mixin
 * skips the second and the loser's own {@code require} then throws
 * {@code InjectionError: … (0/1) succeeded. Scanned 0 target(s)}, killing the server inside
 * {@code Bootstrap.bootStrap}. Winning the race is not a fix — whoever loses still crashes.
 * {@code @ModifyExpressionValue} composes instead: it sees whatever value the call produced, Fabric
 * API's redirect included, so both mods keep working. Which is also why each handler returns
 * {@code original} unless the item is actually an {@link ACFoodPropertiesItem} — overriding
 * unconditionally would discard Fabric API's stack-aware answer for every other mod's food.
 */
@Mixin(LivingEntity.class)
public class LivingEntityFoodMixin {

    //? if <1.20.5 {
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(
            method = "shouldTriggerItemUseEffects",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;getFoodProperties()Lnet/minecraft/world/food/FoodProperties;")
    )
    private FoodProperties ac_useItemFood(FoodProperties original) {
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack stack = self.getUseItem();
        return stack.getItem() instanceof ACFoodPropertiesItem ? ACCompat.food(stack, self) : original;
    }

    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(
            method = "addEatEffect(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;getFoodProperties()Lnet/minecraft/world/food/FoodProperties;")
    )
    private FoodProperties ac_eatEffectFood(FoodProperties original, ItemStack stack, Level level, LivingEntity entity) {
        return stack.getItem() instanceof ACFoodPropertiesItem ? ACCompat.food(stack, (LivingEntity) (Object) this) : original;
    }
    //?}
}
