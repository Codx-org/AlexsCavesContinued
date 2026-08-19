package com.github.alexmodguy.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import com.github.alexmodguy.alexscaves.server.potion.IrradiatedEffect;
import net.minecraft.world.effect.MobEffectInstance;
//? if <1.20.5
import net.minecraft.world.item.alchemy.PotionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

/**
 * Paints a strongly irradiated potion cyan instead of letting it average out to the muddy colour a
 * mix of its effects would give.
 *
 * <p>The method being intercepted is the same computation throughout, but it moved twice. Up to
 * 1.20.4 it was {@code PotionUtils#getColor(Collection)}; 1.20.5 moved the whole family onto the
 * {@code PotionContents} component record as {@code getColor(Iterable)}; and 1.21.4 split that in
 * two — an <em>instance</em> {@code getColor()} that consults the custom-colour component first,
 * and the static {@code getColorOptional(Iterable)} that still averages the effects. The latter is
 * the direct successor, so that is what the {@code >=1.21.4} arm targets: overriding it keeps a
 * potion's explicit custom colour winning, exactly as before. Both the {@code @Mixin} target and
 * the injector's descriptor therefore exist three times — which is also why this class is no longer
 * named after {@code PotionUtils}.
 */
//? if >=1.20.5 {
/*@Mixin(net.minecraft.world.item.alchemy.PotionContents.class)
*///?} else {
@Mixin(PotionUtils.class)
//?}
public class PotionColorMixin {

    //? if >=1.21.4 {
    /*@Inject(
            method = "getColorOptional(Ljava/lang/Iterable;)Ljava/util/OptionalInt;",
            remap = true,
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private static void ac_getColor(Iterable<MobEffectInstance> collection, CallbackInfoReturnable<java.util.OptionalInt> cir) {
        if (ac_irradiated(collection)) {
            cir.setReturnValue(java.util.OptionalInt.of(0X00FFFF));
        }
    }
    *///?} elif >=1.20.5 {
    /*@Inject(
            method = "getColor(Ljava/lang/Iterable;)I",
            remap = true,
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private static void ac_getColor(Iterable<MobEffectInstance> collection, CallbackInfoReturnable<Integer> cir) {
        if (ac_irradiated(collection)) {
            cir.setReturnValue(0X00FFFF);
        }
    }
    *///?} else {
    @Inject(
            method = "getColor(Ljava/util/Collection;)I",
            remap = true,
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private static void ac_getColor(Collection<MobEffectInstance> collection, CallbackInfoReturnable<Integer> cir) {
        if (ac_irradiated(collection)) {
            cir.setReturnValue(0X00FFFF);
        }
    }
    //?}

    @Unique
    private static boolean ac_irradiated(Iterable<MobEffectInstance> effects) {
        for (MobEffectInstance instance : effects) {
            if (ACCompat.rawEffect(instance) == ACEffectRegistry.IRRADIATED.get() && instance.getAmplifier() >= IrradiatedEffect.BLUE_LEVEL) {
                return true;
            }
        }
        return false;
    }
}
