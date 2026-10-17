package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.event.ACDamageEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric's producer for {@code LivingAttackEvent} and {@code LivingDamageEvent} on the {@code Player}
 * half of the damage pipeline — the half that carries the rainbounce boots and the extinction spear.
 * {@link ACDamageEvents} carries the whole argument for why {@code Player} needs its own site for
 * each; the short form is that {@code Player#actuallyHurt} never calls super, and {@code Player#hurt}
 * returns early several times before it does.
 *
 * <p>⚠️ Same 1.21.2 split as its {@code LivingEntity} counterpart, and it must be kept in step with
 * it: a version bump that moves one of the four descriptors moves all four.
 */
@Mixin(Player.class)
public class PlayerDamageEventsMixin {

    //? if >=1.21.2 {
    /*@Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void ac_playerAttacked(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (ACDamageEvents.postAttack((LivingEntity) (Object) this, source, amount, true)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("HEAD"), cancellable = true)
    private void ac_playerDamage(ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
        if (ACDamageEvents.postDamage((LivingEntity) (Object) this, source, amount)) {
            ci.cancel();
        }
    }
    *///?} else {
    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void ac_playerAttacked(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (ACDamageEvents.postAttack((LivingEntity) (Object) this, source, amount, true)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("HEAD"), cancellable = true)
    private void ac_playerDamage(DamageSource source, float amount, CallbackInfo ci) {
        if (ACDamageEvents.postDamage((LivingEntity) (Object) this, source, amount)) {
            ci.cancel();
        }
    }
    //?}
}
