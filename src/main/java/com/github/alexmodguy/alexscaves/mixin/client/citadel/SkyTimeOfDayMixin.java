package com.github.alexmodguy.alexscaves.mixin.client.citadel;

import com.github.alexmodguy.alexscaves.citadel.Citadel;
import com.github.alexmodguy.alexscaves.citadel.CitadelConstants;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

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
 *
 * <p>⚠⚠ Like {@link OutlineColorMixin} this is a {@code @WrapOperation} and was a
 * {@code @Redirect} once, for the same reason and with the same evidence. <b>Original Citadel
 * redirects this exact instruction too</b> — {@code citadel.mixins.json:client.LevelRendererMixin}'s
 * {@code citadel_getTimeOfDay}, which is where this code came from — so a player who installs
 * Citadel for some other mod (Rats, Ice and Fire, …) alongside this one used to get the
 * {@code "@Redirect conflict. Skipping …"} / {@code Critical injection failure … (0/1) succeeded}
 * crash at {@code Initializing game}, exactly as Alex's Mobs Continued did on {@code getTeamColor}.
 * Those two instructions are the whole {@code @Redirect} overlap: Citadel 2.6.3 has only three
 * {@code @Redirect}s in the mod, and the third ({@code SmithingMenuMixin}'s {@code getRecipesFor})
 * is not one this tree vendors. ⚠ That is <b>not</b> the whole overlap, though —
 * {@code @ModifyConstant} is a {@code RedirectInjector} subclass and collides identically, which is
 * why {@code ServerLevelMixin}, {@code ClientLevelMixin}, {@code MinecraftServerMixin} and
 * {@code SplashRendererMixin} are all {@code @ModifyExpressionValue} now. The Citadel repro in fact
 * crashed at {@code ServerLevel#tickTime}'s {@code 1L}, before any render mixin ran. When adding a
 * vendored-Citadel injector, assume every exclusive injector in the overlap conflicts.
 *
 * <p>The body deliberately does <i>not</i> call {@code original} — the lerped time of day is a
 * total replacement, and Citadel's own redirect, which is what {@code original} would be, computes
 * the identical value.
 */
//? if >=1.21.9 {
/*@Mixin(net.minecraft.client.renderer.SkyRenderer.class)
*///?} else {
@Mixin(LevelRenderer.class)
//?}
public class SkyTimeOfDayMixin {

    @WrapOperation(
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
    private float citadel_getTimeOfDay(ClientLevel instance, float partialTicks, Operation<Float> original) {
        //default implementation does not lerp the time of day
        float lerpBy = Citadel.PROXY.isGamePaused() ? 0F : partialTicks;
        float lerpedDayTime = (instance.dimensionType().fixedTime().orElse(instance.dayTime()) + lerpBy) / 24000.0F;
        double d0 = Mth.frac((double) lerpedDayTime - 0.25D);
        double d1 = 0.5D - Math.cos(d0 * Math.PI) / 2.0D;
        return (float) (d0 * 2.0D + d1) / 3.0F;
    }
}
