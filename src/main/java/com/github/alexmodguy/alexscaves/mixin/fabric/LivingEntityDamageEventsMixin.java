package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.event.ACDamageEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric's producer for {@code LivingAttackEvent} and {@code LivingDamageEvent} on the
 * {@code LivingEntity} half of the damage pipeline. {@code PlayerDamageEventsMixin} is the other
 * half, and {@link ACDamageEvents} explains why there has to be one.
 *
 * <p>Both anchors are HEAD, which is where Forge splices {@code onLivingAttack}: read out of the
 * 1.20.1 and 1.21.11 universal jars, the {@code invokestatic} is at bytecode offset 3 of
 * {@code hurt}/{@code hurtServer}, i.e. ahead of vanilla's own invulnerability test.
 *
 * <p>⚠️ Two arms, split at 1.21.2, where the damage path became server-side and gained a leading
 * {@code ServerLevel}: {@code hurt(DamageSource,float)} became
 * {@code hurtServer(ServerLevel,DamageSource,float)} and {@code actuallyHurt} grew the same
 * parameter. Verified against all 22 Fabric nodes — 8 below, 14 at and above.
 */
@Mixin(LivingEntity.class)
public class LivingEntityDamageEventsMixin {

    //? if >=1.21.2 {
    /*@Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void ac_livingAttack(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (ACDamageEvents.postAttack((LivingEntity) (Object) this, source, amount, false)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("HEAD"), cancellable = true)
    private void ac_livingDamage(ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
        if (ACDamageEvents.postDamage((LivingEntity) (Object) this, source, amount)) {
            ci.cancel();
        }
    }
    *///?} else {
    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void ac_livingAttack(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (ACDamageEvents.postAttack((LivingEntity) (Object) this, source, amount, false)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("HEAD"), cancellable = true)
    private void ac_livingDamage(DamageSource source, float amount, CallbackInfo ci) {
        if (ACDamageEvents.postDamage((LivingEntity) (Object) this, source, amount)) {
            ci.cancel();
        }
    }
    //?}
}
