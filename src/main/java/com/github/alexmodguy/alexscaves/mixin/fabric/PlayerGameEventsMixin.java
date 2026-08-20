package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import com.github.alexmodguy.alexscaves.fabric.forge.event.TickEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric's producer for the two {@code Player} game-bus events this mod listens to:
 * {@link TickEvent.PlayerTickEvent} and {@link PlayerInteractEvent.EntityInteract}.
 *
 * <p><b>The tick is fired ONCE, in {@code Phase.END}.</b> Forge's single {@code PlayerTickEvent}
 * carries the player in a public field and is posted in both phases, so upstream's handler ran twice
 * a tick; {@code CommonEvents#playerTick} only destroys the restricted biome-locator exploit items,
 * which is idempotent, so {@code END} alone is enough and is half the work. That reading is recorded
 * beside the handler as well — if a listener is ever added that genuinely needs {@code START}, add the
 * HEAD inject here rather than moving this one.
 *
 * <p><b>{@code interactOn} is the anchor for {@code EntityInteract}, and it needs two arms</b> —
 * 26.1 appended a {@code Vec3} hit position. Forge's patch sits immediately after the spectator
 * check,
 *
 * <pre>
 * if (this.isSpectator()) { ... return InteractionResult.PASS; }
 * PlayerInteractEvent.EntityInteract event = ForgeHooks.onInteractEntity(this, target, hand);
 * if (event.isCanceled()) return event.getCancellationResult();
 * </pre>
 *
 * which a HEAD inject reproduces by asking {@code isSpectator()} itself rather than by chasing the
 * branch — cheaper than an {@code @At("INVOKE")} anchor and immune to the branch being reshaped.
 * The cancellation result is passed through rather than assumed, because a listener that cancels with
 * {@code SUCCESS} means "I handled this" (which is what the holocoder binding does) and one that
 * cancels with the default {@code PASS} means "nothing happened here".
 *
 * <p>⚠️ Both descriptors were read off every Fabric node in the matrix: {@code tick()V} is identical
 * on all 22, and {@code interactOn} is the two-argument form on 1.20.1→1.21.11 and the
 * three-argument one on 26.1→26.2. Re-derive rather than inherit on a new node.
 */
@Mixin(Player.class)
public class PlayerGameEventsMixin {

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void ac_playerTick(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, (Player) (Object) this));
    }

    //? if >=26.1 {
    /*@Inject(
            method = "interactOn(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ac_entityInteract(Entity target, InteractionHand hand, net.minecraft.world.phys.Vec3 hitPos, CallbackInfoReturnable<InteractionResult> cir) {
        ac_postEntityInteract(target, hand, cir);
    }
    *///?} else {
    @Inject(
            method = "interactOn(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ac_entityInteract(Entity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ac_postEntityInteract(target, hand, cir);
    }
    //?}

    @org.spongepowered.asm.mixin.Unique
    private void ac_postEntityInteract(Entity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Player self = (Player) (Object) this;
        if (self.isSpectator()) {
            return;
        }
        PlayerInteractEvent.EntityInteract event = new PlayerInteractEvent.EntityInteract(self, hand, target);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            cir.setReturnValue(event.getCancellationResult());
        }
    }
}
