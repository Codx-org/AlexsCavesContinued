package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living.MobEffectEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric's producer for the three status-effect lifecycle events — {@link MobEffectEvent.Added},
 * {@link MobEffectEvent.Remove} and {@link MobEffectEvent.Expired}. Between them they drive the
 * enter/exit sounds of Darkness Incarnate and Sugar Rush, Darkness Incarnate's flight toggle, and
 * Sugar Rush's time dilation, so all three have to fire and have to be told apart correctly: an
 * expiry that is mistaken for a removal leaves a player in slow motion for good.
 *
 * <h2>Why the two removal events share one anchor</h2>
 *
 * Forge posts {@code Remove} from {@code removeEffect} and {@code removeAllEffects} and
 * {@code Expired} from {@code tickEffects}, i.e. from three places. Rather than chase all three
 * across the range, this hooks the <b>one method all three funnel through</b> —
 * {@code onEffectRemoved}/{@code onEffectsRemoved} — and uses {@link #ac_inTickEffects} to say which
 * caller it is under. That was checked rather than assumed: disassembling every Fabric node in the
 * matrix shows that method has exactly three callers on every one of them, and they are exactly
 * {@code tickEffects}, {@code removeAllEffects} and {@code removeEffect}. It also sidesteps the
 * 1.20.5 {@code MobEffect} → {@code Holder} split entirely, because the funnel hands over a whole
 * {@link MobEffectInstance} on every version while {@code removeEffect}'s own parameter is the thing
 * that changed type.
 *
 * <p><b>Two arms, because the funnel was renamed and re-shaped at 1.21.2</b>:
 * {@code onEffectRemoved(MobEffectInstance)V} on 1.20.1→1.21.1 (8 nodes) became
 * {@code onEffectsRemoved(Collection)V} on 1.21.2→26.2 (14 nodes), which batches the whole set a
 * tick expired. The collection form posts one event per instance, which is what the single form did
 * per call anyway.
 *
 * <p>⚠️ <b>One deliberate divergence from Forge.</b> Forge posts {@code Remove} at the HEAD of
 * {@code removeEffect}, so it fires even when the entity does not have the effect and the removal is
 * a no-op; this posts only on a confirmed removal. That is strictly the better shape and it is
 * invisible to both listeners here — each plays an <i>exit</i> sound, which should not play for an
 * effect that was never on. If a listener is ever added that needs the attempt rather than the
 * outcome, it needs its own anchor, not a change to this one.
 *
 * <p>⚠️ The flag is a plain field rather than a thread-local because {@code tickEffects} is not
 * re-entrant and every caller of it is on the thread that owns the entity. It is cleared in a
 * {@code RETURN} inject, which Mixin applies at <i>every</i> return of the method, so an early exit
 * cannot leave it set.
 */
@Mixin(LivingEntity.class)
public class LivingEntityEffectEventsMixin {

    @Unique
    private boolean ac_inTickEffects;

    /**
     * {@code Added} is posted on a confirmed application, reading the <b>incoming</b> instance.
     *
     * <p>Forge reads the map's instance instead, and the difference is not observable: on a fresh
     * add the two are the same object, and on a merge vanilla's {@code MobEffectInstance#update}
     * copies the incoming amplifier and duration into the stored one before anything can look, so
     * the only two things the listeners read — the effect type and {@code getDuration()} — agree
     * either way. Reading the parameter avoids a {@code getEffect} lookup, which is the one call in
     * this file that would have needed a {@code Holder} gate.
     */
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("RETURN"))
    private void ac_effectAdded(MobEffectInstance effectInstance, net.minecraft.world.entity.Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            MinecraftForge.EVENT_BUS.post(new MobEffectEvent.Added((LivingEntity) (Object) this, effectInstance, null, source));
        }
    }

    @Inject(method = "tickEffects()V", at = @At("HEAD"))
    private void ac_tickEffectsStart(CallbackInfo ci) {
        this.ac_inTickEffects = true;
    }

    @Inject(method = "tickEffects()V", at = @At("RETURN"))
    private void ac_tickEffectsEnd(CallbackInfo ci) {
        this.ac_inTickEffects = false;
    }

    //? if >=1.21.2 {
    /*@Inject(method = "onEffectsRemoved(Ljava/util/Collection;)V", at = @At("HEAD"))
    private void ac_effectsRemoved(java.util.Collection<MobEffectInstance> removed, CallbackInfo ci) {
        for (MobEffectInstance instance : removed) {
            ac_postRemoval(instance);
        }
    }
    *///?} else {
    @Inject(method = "onEffectRemoved(Lnet/minecraft/world/effect/MobEffectInstance;)V", at = @At("HEAD"))
    private void ac_effectRemoved(MobEffectInstance removed, CallbackInfo ci) {
        ac_postRemoval(removed);
    }
    //?}

    @Unique
    private void ac_postRemoval(MobEffectInstance instance) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (this.ac_inTickEffects) {
            MinecraftForge.EVENT_BUS.post(new MobEffectEvent.Expired(self, instance));
        } else {
            MinecraftForge.EVENT_BUS.post(new MobEffectEvent.Remove(self, instance));
        }
    }
}
