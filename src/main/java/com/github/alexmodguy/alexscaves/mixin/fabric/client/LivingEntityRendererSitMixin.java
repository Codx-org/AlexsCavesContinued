package com.github.alexmodguy.alexscaves.mixin.fabric.client;

import com.github.alexmodguy.alexscaves.server.entity.util.ACRiderSitEntity;
import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fabric's dispatcher for {@link ACRiderSitEntity} on the one place it is actually observed:
 * {@code LivingEntityRenderer#render}.
 *
 * <p>{@code Entity#shouldRiderSit()} is a loader patch ({@code IForgeEntity}, NeoForge's
 * {@code IEntityExtension}) whose entire reason for existing is this method. Vanilla asks
 * {@code entity.isPassenger()} three times in {@code render} and treats every passenger as seated;
 * the loaders compute the answer once, into a local, and substitute it at all three sites:
 *
 * <ul>
 *   <li>{@code this.model.riding = …} — the seated pose (bent legs);</li>
 *   <li>the {@code getVehicle() instanceof LivingEntity} branch — a seated rider inherits its
 *       mount's body rotation instead of turning under its own;</li>
 *   <li>the walk-animation guard — a seated rider's limbs do not swing.</li>
 * </ul>
 *
 * <p>So one {@code @Redirect} with no {@code ordinal} reproduces the patch exactly: it replaces
 * every matching call in the method, and the replacement is Forge's own expression verbatim —
 * {@code isPassenger() && getVehicle() != null && getVehicle().shouldRiderSit()} — routed through
 * {@link ACCompat#shouldRiderSit(net.minecraft.world.entity.Entity)}, which is the single place that
 * dispatch lives. Read out of the 1.20.1 merged jars: vanilla offsets 23 / 76 / 338 against Forge's
 * {@code istore 7} at 75 and its three {@code iload 7}s at 81 / 132 / 392.
 *
 * <p>This is not cosmetic for this mod. Both implementors — {@code SubterranodonEntity} and
 * {@code GumWormSegmentEntity} — answer {@code false}, and the subterranodon is a
 * {@code LivingEntity}, so without this all three effects fire: a rider sits, is locked to the
 * mount's facing and stops animating.
 *
 * <p>⚠️ Gated {@code <1.21.2}, which is where {@code render} takes a {@code LivingEntityRenderState}
 * instead of the entity and this selector stops resolving. The passenger question moves into
 * {@code extractRenderState} there and the loaders' patch moves with it, so the arm above that band
 * has to be re-derived from the bytecode rather than guessed — and note that {@code defaultRequire:
 * 1} would let a band with fewer than three matching call sites pass silently.
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererSitMixin {

    //? if <1.21.2 {
    @Redirect(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isPassenger()Z")
    )
    private boolean ac_shouldRiderSit(LivingEntity entity) {
        return entity.isPassenger() && entity.getVehicle() != null && ACCompat.shouldRiderSit(entity.getVehicle());
    }
    //?}
}
