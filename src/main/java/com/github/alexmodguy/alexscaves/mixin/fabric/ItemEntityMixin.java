package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.server.item.ACDestroyedItem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fabric's dispatcher for {@link ACDestroyedItem} — the item that wants to know <i>what</i> destroyed
 * its dropped entity.
 *
 * <p>Vanilla's {@code ItemEntity#hurt} ends its lethal branch with
 * {@code this.getItem().onDestroyed(this)}; Forge patches that one call site to
 * {@code onDestroyed(this, source)} and routes it through {@code IForgeItem}'s two-argument default,
 * whose own default body is vanilla's one-argument method. Read out of the 1.20.1 merged jars, the
 * two disassemblies are identical up to that instruction — offset 112 in vanilla, 113 on Forge — and
 * differ in nothing else, so a {@code @Redirect} of exactly that invoke is the whole patch.
 *
 * <p>The redirect reproduces the loaders' fallback rather than replacing it: an item that does not
 * implement the interface still gets vanilla's one-argument call, which is what
 * {@code IForgeItem#onDestroyed(ItemEntity, DamageSource)} delegates to. The one implementor,
 * {@code RadioactiveOnDestroyedBlockItem}, opens its override with
 * {@code super.onDestroyed(itemEntity, damageSource)} — rewritten to the vanilla arity here by the
 * {@code !fab-item-ondestroyed-super} replacement rule — so the vanilla behaviour runs exactly once
 * on every loader.
 *
 * <p>The {@link DamageSource} is the enclosing method's own first argument, appended to the handler
 * the way {@code @Redirect} allows; {@code amount} comes with it because a redirect handler that
 * captures any of the target's parameters must capture all of them.
 *
 * <p>⚠️ {@code Entity#hurt} splits into {@code hurtServer}/{@code hurtClient} at 1.21.2, and the
 * lethal branch — the only one that can destroy the stack — lands wholly in {@code hurtServer
 * (ServerLevel, DamageSource, float)}; javap on the 1.21.2 jar finds the sole {@code onDestroyed}
 * invoke there, at offset 87, and none at all in {@code hurtClient}. So it is the {@code method}
 * selector that moves, not the {@code @At} target, which is on {@code ItemStack} and never changed.
 * A {@code @Redirect} handler that captures any of its target's parameters must capture all of them,
 * so the handler's tail moves with the selector: the leading {@code ServerLevel} is new and the
 * {@code DamageSource} the redirect actually wants is the second argument from there up.
 */
@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    @Redirect(
            //? if >=1.21.2 {
            /*method = "hurtServer",
            *///?} else {
            method = "hurt",
            //?}
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;onDestroyed(Lnet/minecraft/world/entity/item/ItemEntity;)V"
            )
    )
    //? if >=1.21.2 {
    /*private void ac_onDestroyed(ItemStack stack, ItemEntity itemEntity, net.minecraft.server.level.ServerLevel level, DamageSource damageSource, float amount) {
    *///?} else {
    private void ac_onDestroyed(ItemStack stack, ItemEntity itemEntity, DamageSource damageSource, float amount) {
    //?}
        if (stack.getItem() instanceof ACDestroyedItem typed) {
            typed.onDestroyed(itemEntity, damageSource);
        } else {
            stack.onDestroyed(itemEntity);
        }
    }
}
