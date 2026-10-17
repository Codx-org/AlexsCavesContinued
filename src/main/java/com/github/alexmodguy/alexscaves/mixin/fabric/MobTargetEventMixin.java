package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living.LivingChangeTargetEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric's producer for {@link LivingChangeTargetEvent} — the veto a mod holds over a mob acquiring a
 * target. Its one listener here is {@code CommonEvents#livingFindTarget}, which is what makes a
 * hiding vallumraptor untargetable.
 *
 * <p>{@code Mob#setTarget(LivingEntity)V} is byte-identical on all 22 Fabric nodes as a descriptor,
 * and its body is a bare field assignment on every one of them (26.2 routes the value through a new
 * {@code asValidTarget} first, which does not move the anchor). Forge's patch is
 *
 * <pre>
 * LivingChangeTargetEvent event = ForgeHooks.onLivingChangeTarget(this, target, MOB_TARGET);
 * if (!event.isCanceled()) this.target = event.getNewTarget();
 * </pre>
 *
 * so a HEAD-cancellable inject reproduces the veto exactly.
 *
 * <p>⚠️ {@code setNewTarget} is <b>not</b> honoured on Fabric — only the cancel is. Nothing in this
 * mod calls it (the one listener answers by cancelling and then calling {@code setTarget(null)}
 * itself), and the obvious one-injector trick that would carry both — a {@code @ModifyVariable} at
 * HEAD returning the current target to express a cancel — stops being an exact no-op at 26.2, where
 * the assignment runs through {@code asValidTarget} and could quietly drop a target that has since
 * gone stale. If a future listener ever needs the replacement, add a {@code @ModifyVariable} beside
 * this and re-check that interaction; do not assume it is free.
 *
 * <p>Note the listener re-enters this method: it calls {@code mob.setTarget(null)} from inside the
 * handler. That terminates because the re-fired event carries {@code newTarget == null}, which fails
 * the handler's {@code instanceof VallumraptorEntity} test on the first line.
 */
@Mixin(Mob.class)
public class MobTargetEventMixin {

    @Inject(method = "setTarget(Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), cancellable = true)
    private void ac_livingChangeTarget(LivingEntity target, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new LivingChangeTargetEvent((Mob) (Object) this, target))) {
            ci.cancel();
        }
    }
}
