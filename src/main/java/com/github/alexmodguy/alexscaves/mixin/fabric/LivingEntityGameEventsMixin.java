package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living.LivingDeathEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living.LivingEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living.LivingHealEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric's producer for the three {@code LivingEntity} game-bus events this mod listens to:
 * {@link LivingEvent.LivingTickEvent}, {@link LivingDeathEvent} and {@link LivingHealEvent}.
 *
 * <p><b>Why this class has to exist at all.</b> The Fabric port supplies Forge's event <em>shapes</em>
 * — the stand-in classes under {@code fabric/forge/event/**} and the bus they travel on
 * ({@code fabric/event/ACEventBus}) — so that {@code CommonEvents} compiles and registers unchanged on
 * all three loaders. What it did not originally supply was anything that <em>constructs</em> one, and a
 * bus with listeners and no producer is silent in a way no compile and no boot can see. This mixin and
 * its siblings under {@code mixin/fabric/} are the missing half.
 *
 * <p><b>Descriptor stability.</b> All three targets are byte-identical on every one of the 22 Fabric
 * nodes — {@code tick()V}, {@code die(Lnet/minecraft/world/damagesource/DamageSource;)V} and
 * {@code heal(F)V} — so none of them needs a Stonecutter gate. ⚠️ That is a fact about 1.20.1→26.2 as
 * they stand today; re-derive it from each new node's own bytecode rather than inheriting this
 * sentence.
 *
 * <p><b>Where Forge posts each one</b>, read out of the 1.20.1 universal jar:
 * <ul>
 *   <li>{@code LivingTickEvent} — at the top of {@code tick()}, ahead of vanilla's body, and
 *       <em>not</em> cancellable in this tree's stand-in (Forge's is; nothing here cancels it, and
 *       {@code Event#setCanceled} throws on a non-{@code @Cancelable} event, so keeping it
 *       non-cancellable is the honest shape rather than a missing feature).</li>
 *   <li>{@code LivingDeathEvent} — {@code if (ForgeHooks.onLivingDeath(this, cause)) return;} as
 *       {@code die}'s first statement. A HEAD-cancellable inject is the whole patch.</li>
 *   <li>{@code LivingHealEvent} — {@code heal} is a <em>modify</em> hook on Forge: the amount goes
 *       through the event and a cancel returns early. See the note on {@code ac_livingHeal}.</li>
 * </ul>
 *
 * <p>⚠️ {@code LivingTickEvent} is posted for every living entity on both sides, exactly as on Forge —
 * {@code CommonEvents#livingTick} deliberately runs client-side too (it drives the bubbled and
 * darkness-incarnate removals and the diving-helmet water breathing). Do not "optimise" it to the
 * server.
 */
@Mixin(LivingEntity.class)
public class LivingEntityGameEventsMixin {

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void ac_livingTick(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingTickEvent((LivingEntity) (Object) this));
    }

    @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"), cancellable = true)
    private void ac_livingDeath(DamageSource cause, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new LivingDeathEvent((LivingEntity) (Object) this, cause))) {
            ci.cancel();
        }
    }

    /**
     * A cancel is expressed as a <b>zero heal</b> rather than as a {@code ci.cancel()}, which is exactly
     * equivalent here and is what lets one injector carry both halves of Forge's hook (the veto
     * <i>and</i> {@code setAmount}). {@code heal(F)V} disassembles byte-for-byte identically on
     * 1.20.1 and 26.2 as
     *
     * <pre>
     * float f = this.getHealth();
     * if (f &gt; 0.0F) this.setHealth(f + healAmount);
     * </pre>
     *
     * so an amount of zero reaches {@code setHealth(f)}, and {@code setHealth} writes through
     * {@code SynchedEntityData#set}, which discards a write of the value already stored. No health
     * change, no sync packet, no side effect — i.e. indistinguishable from the early return.
     *
     * <p>Using {@code @ModifyVariable} rather than a second cancellable {@code @Inject} also avoids
     * two injectors racing at the same HEAD offset, where the application order between an
     * {@code @Inject} and a {@code @ModifyVariable} is not something to rely on.
     */
    @ModifyVariable(method = "heal(F)V", at = @At("HEAD"), argsOnly = true)
    private float ac_livingHeal(float healAmount) {
        LivingHealEvent event = new LivingHealEvent((LivingEntity) (Object) this, healAmount);
        return MinecraftForge.EVENT_BUS.post(event) ? 0.0F : event.getAmount();
    }
}
