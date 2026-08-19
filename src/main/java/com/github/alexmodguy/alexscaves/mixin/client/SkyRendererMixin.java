package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.github.alexmodguy.alexscaves.client.ClientProxy;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The 1.21.2-and-up form of Alex's Caves' cave-biome sky.
 *
 * <p>Up to 1.21.1 this was {@link LevelRendererSkyMixin}: a whole copy of {@code renderSky} with
 * three values changed. 1.21.2 moved every draw in that method into {@code SkyRenderer}, whose
 * entry points take exactly those three values as arguments — so the copy is gone and what is left
 * is the three changes themselves:
 *
 * <ul>
 * <li>the sky disc's colour goes through {@code ClientProxy#processSkyColor};
 * <li>the sunrise/sunset colour's alpha is scaled down by the override amount;
 * <li>the sun, moon and stars dim as though it were raining that hard.
 * </ul>
 *
 * <p>The old copy also re-checked {@code doesMobEffectBlockSky}, the fluid fog type and the sky
 * type before drawing anything — all three are still vanilla's own guards around the call into
 * {@code SkyRenderer}, so nothing here has to repeat them.
 *
 * <p>1.21.4 hands both entry points a {@code MultiBufferSource.BufferSource} where 1.21.2/1.21.3
 * passed a {@code Tesselator}. Nothing this mixin reads moves — the tinted arguments keep their
 * positions and their per-type ordinals — but a {@code method} selector is matched by descriptor,
 * so each of the three has an arm per shape.
 *
 * <p>1.21.6 does it again to {@code renderSunMoonAndStars}, which drops its trailing
 * {@code FogParameters} — fog is a uniform block the render pass binds now rather than something
 * threaded through the sky. Nothing this mixin reads moves there either, but the descriptor does.
 *
 * <p>And 1.21.9 a third time, to both: the {@code MultiBufferSource.BufferSource} is gone, the sky
 * drawing its own geometry rather than batching into a shared source. That is once again purely a
 * descriptor change here — the three tinted arguments keep their positions, and because the
 * parameter that vanished was neither an {@code int} nor a {@code float}, all three {@code ordinal}s
 * are unchanged too.
 */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {

    // renderSkyDisc's three floats exist only to be handed to the colour modulator, so the tint goes
    // on where they are consumed rather than on the parameters — one injection instead of three, and
    // no ordering question between them.
    //
    // Where that is moved in 1.21.6, which deleted RenderSystem#setShaderColor: the modulator is a
    // member of the per-draw DynamicTransforms block now, and the method builds it from a
    // `new Vector4f(r, g, b, 1.0F)` — the only one in the method, and the direct successor of the
    // setShaderColor call this used to redirect. Below 1.21.6, ordinal 0 is the colour itself; the
    // second call in the method is the reset to white.
    // And moved once more in 1.21.11, which passes the disc's colour in as one packed ARGB int —
    // the sky is an EnvironmentAttributes value now, extracted into SkyRenderState. The modulator is
    // built by ARGB#vector4fFromARGB32 rather than by a constructor, so the anchor is that call and
    // the body is unchanged. (Repeated per arm rather than hoisted: an arm chain that carries only
    // annotations cannot also carry the one arm below whose handler has a different shape.)
    //? if >=1.21.11 {
    /*@com.llamalad7.mixinextras.injector.ModifyExpressionValue(
            method = "Lnet/minecraft/client/renderer/SkyRenderer;renderSkyDisc(I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;vector4fFromARGB32(I)Lorg/joml/Vector4f;"),
            remap = true
    )
    private org.joml.Vector4f ac_skyDiscColor(org.joml.Vector4f color) {
        if (AlexsCaves.CLIENT_CONFIG.biomeSkyOverrides.get()) {
            Vec3 vec3 = ClientProxy.processSkyColor(new Vec3(color.x, color.y, color.z), ACClientCompat.frameTime());
            color.set((float) vec3.x, (float) vec3.y, (float) vec3.z, color.w);
        }
        return color;
    }
    *///?} elif >=1.21.6 {
    /*@com.llamalad7.mixinextras.injector.ModifyExpressionValue(
            method = "Lnet/minecraft/client/renderer/SkyRenderer;renderSkyDisc(FFF)V",
            at = @At(value = "NEW", target = "(FFFF)Lorg/joml/Vector4f;"),
            remap = true
    )
    private org.joml.Vector4f ac_skyDiscColor(org.joml.Vector4f color) {
        if (AlexsCaves.CLIENT_CONFIG.biomeSkyOverrides.get()) {
            Vec3 vec3 = ClientProxy.processSkyColor(new Vec3(color.x, color.y, color.z), ACClientCompat.frameTime());
            color.set((float) vec3.x, (float) vec3.y, (float) vec3.z, color.w);
        }
        return color;
    }
    *///?} else {
    @Redirect(
            method = "Lnet/minecraft/client/renderer/SkyRenderer;renderSkyDisc(FFF)V",
            at = @At(value = "INVOKE", ordinal = 0, target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V"),
            remap = true
    )
    private void ac_skyDiscColor(float red, float green, float blue, float alpha) {
        if (AlexsCaves.CLIENT_CONFIG.biomeSkyOverrides.get()) {
            Vec3 vec3 = ClientProxy.processSkyColor(new Vec3(red, green, blue), ACClientCompat.frameTime());
            red = (float) vec3.x;
            green = (float) vec3.y;
            blue = (float) vec3.z;
        }
        RenderSystem.setShaderColor(red, green, blue, alpha);
    }
    //?}

    // The sunrise colour is one packed ARGB int now rather than a float[4], so fading it out inside
    // a cave biome is a scale of its alpha channel — the `afloat[3] * (1F - override)` the old copy
    // did, to the nearest 8-bit step.
    @ModifyVariable(
            //? if >=1.21.9 {
            /*method = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunriseAndSunset(Lcom/mojang/blaze3d/vertex/PoseStack;FI)V",
            *///?} elif >=1.21.4 {
            /*method = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunriseAndSunset(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;FI)V",
            *///?} else {
            method = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunriseAndSunset(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/Tesselator;FI)V",
            //?}
            at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = true
    )
    private int ac_sunriseColor(int color) {
        float override = ClientProxy.acSkyOverrideAmount;
        if (override <= 0.0F || !AlexsCaves.CLIENT_CONFIG.biomeSkyOverrides.get()) {
            return color;
        }
        // Packed by hand rather than through ARGB's own packer: that call would read as the vertex
        // builder's colour setter did before 1.21, and the !mc21-vc-color rule rewrites that token
        // wherever it appears — replacements do not know what a receiver is.
        return (int) (ARGB.alpha(color) * (1.0F - override)) << 24 | color & 0x00FFFFFF;
    }

    // renderSunMoonAndStars' fifth argument is vanilla's `1 - rainLevel`, which is what dims the sun
    // and the moon. Alex's Caves treats the cave-biome override as a rain level of its own and takes
    // whichever is heavier, so the brightness is whichever is lower.
    // 1.21.11 unpacked the celestial angles: the single time-of-day float became one angle each for
    // the sun, the moon and the stars, and the moon phase became a MoonPhase enum where it was an
    // int. Neither of the two arguments read here moved position, but three floats now precede them
    // instead of one — so the ordinals go 1 -> 3 and 2 -> 4. The ordinal rides in the arm with the
    // descriptor for that reason.
    @ModifyVariable(
            //? if >=1.21.11 {
            /*method = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunMoonAndStars(Lcom/mojang/blaze3d/vertex/PoseStack;FFFLnet/minecraft/world/level/MoonPhase;FF)V", ordinal = 3,
            *///?} elif >=1.21.9 {
            /*method = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunMoonAndStars(Lcom/mojang/blaze3d/vertex/PoseStack;FIFF)V", ordinal = 1,
            *///?} elif >=1.21.6 {
            /*method = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunMoonAndStars(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;FIFF)V", ordinal = 1,
            *///?} elif >=1.21.4 {
            /*method = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunMoonAndStars(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;FIFFLnet/minecraft/client/renderer/FogParameters;)V", ordinal = 1,
            *///?} else {
            method = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunMoonAndStars(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/Tesselator;FIFFLnet/minecraft/client/renderer/FogParameters;)V", ordinal = 1,
            //?}
            at = @At("HEAD"), argsOnly = true, remap = true
    )
    private float ac_rainBrightness(float rainBrightness) {
        return Math.min(rainBrightness, 1.0F - ac_skyOverride());
    }

    // ...and the sixth is `starBrightness * rainBrightness`, so it takes the same factor. Derived
    // from the level rather than from the fifth argument, which another @ModifyVariable may or may
    // not have reached first — this is the same getRainLevel call vanilla made a line earlier.
    @ModifyVariable(
            //? if >=1.21.11 {
            /*method = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunMoonAndStars(Lcom/mojang/blaze3d/vertex/PoseStack;FFFLnet/minecraft/world/level/MoonPhase;FF)V", ordinal = 4,
            *///?} elif >=1.21.9 {
            /*method = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunMoonAndStars(Lcom/mojang/blaze3d/vertex/PoseStack;FIFF)V", ordinal = 2,
            *///?} elif >=1.21.6 {
            /*method = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunMoonAndStars(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;FIFF)V", ordinal = 2,
            *///?} elif >=1.21.4 {
            /*method = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunMoonAndStars(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;FIFFLnet/minecraft/client/renderer/FogParameters;)V", ordinal = 2,
            *///?} else {
            method = "Lnet/minecraft/client/renderer/SkyRenderer;renderSunMoonAndStars(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/Tesselator;FIFFLnet/minecraft/client/renderer/FogParameters;)V", ordinal = 2,
            //?}
            at = @At("HEAD"), argsOnly = true, remap = true
    )
    private float ac_starBrightness(float starBrightness) {
        float override = ac_skyOverride();
        if (override <= 0.0F || Minecraft.getInstance().level == null) {
            return starBrightness;
        }
        float rainBrightness = 1.0F - Minecraft.getInstance().level.getRainLevel(ACClientCompat.frameTime());
        return rainBrightness <= 0.0F ? 0.0F : starBrightness * Math.min(rainBrightness, 1.0F - override) / rainBrightness;
    }

    @org.spongepowered.asm.mixin.Unique
    private static float ac_skyOverride() {
        return AlexsCaves.CLIENT_CONFIG.biomeSkyOverrides.get() ? ClientProxy.acSkyOverrideAmount : 0.0F;
    }
}
