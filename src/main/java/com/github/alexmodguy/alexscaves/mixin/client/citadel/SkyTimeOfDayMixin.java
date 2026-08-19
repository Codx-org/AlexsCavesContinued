package com.github.alexmodguy.alexscaves.mixin.client.citadel;

import com.github.alexmodguy.alexscaves.citadel.Citadel;
import com.github.alexmodguy.alexscaves.citadel.CitadelConstants;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Citadel's smoothed sky rotation: vanilla's own {@code ClientLevel#getTimeOfDay} steps once per
 * tick, so this substitutes a partial-tick-lerped one for the sky's use only.
 *
 * <p>Like {@link OutlineColorMixin} this lived in {@link LevelRendererMixin} until 1.21.9, and like
 * it the redirect body has never changed while its call site kept moving:
 *
 * <ul>
 * <li>up to 1.20.4, {@code LevelRenderer#renderSky} with a leading {@code PoseStack};</li>
 * <li>1.20.5 replaces that stack with the frustum {@code Matrix4f} — descriptor only;</li>
 * <li>1.21.2 deletes {@code renderSky} and draws the sky from {@code addSkyPass}' lambda, of which
 *     only one of the two former {@code getTimeOfDay} calls survived (hence the {@code expect});</li>
 * <li>1.21.9 hoists it out of the draw entirely, into {@code SkyRenderer#extractRenderState} — a
 *     different class, which is why this is a file of its own: the {@code @Mixin} target is the gate.</li>
 * </ul>
 *
 * <p>The lambda has no name of its own, so the loaders' mappings invent one differently: NeoForge's
 * keeps javac's {@code lambda$addSkyPass$N}, while both loom-mapped loaders — Forge and Fabric —
 * number it {@code method_NNNNN}. Hence the {@code !neoforge} arm rather than a Forge-only one; the
 * Fabric 1.21.2 jar was javap'd for it and answers {@code method_62215}, the same as Forge. The
 * number is per MC version, so every node in the 1.21.2–1.21.8 window needs its own checked — which
 * is exactly what {@code scripts/verify_mixins.py} reports, and why it is run before anything is
 * booted.
 */
//? if >=1.21.9 {
/*@Mixin(net.minecraft.client.renderer.SkyRenderer.class)
*///?} else {
@Mixin(LevelRenderer.class)
//?}
public class SkyTimeOfDayMixin {

    @Redirect(
            //? if >=1.21.9 {
            /*method = "Lnet/minecraft/client/renderer/SkyRenderer;extractRenderState(Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/state/SkyRenderState;)V",
            *///?} elif !neoforge && >=1.21.2 {
            /*method = "method_62215",
            *///?} elif >=1.21.2 {
            /*method = "/lambda\\$addSkyPass\\$/",
            *///?} elif >=1.20.5 {
            /*method = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
            *///?} else {
            method = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
            //?}
            remap = CitadelConstants.REMAPREFS,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F"),
            //? if >=1.21.2 {
            /*expect = 1
            *///?} else {
            expect = 2
            //?}
    )
    private float citadel_getTimeOfDay(ClientLevel instance, float partialTicks) {
        //default implementation does not lerp the time of day
        float lerpBy = Citadel.PROXY.isGamePaused() ? 0F : partialTicks;
        float lerpedDayTime = (instance.dimensionType().fixedTime().orElse(instance.dayTime()) + lerpBy) / 24000.0F;
        double d0 = Mth.frac((double) lerpedDayTime - 0.25D);
        double d1 = 0.5D - Math.cos(d0 * Math.PI) / 2.0D;
        return (float) (d0 * 2.0D + d1) / 3.0F;
    }
}
