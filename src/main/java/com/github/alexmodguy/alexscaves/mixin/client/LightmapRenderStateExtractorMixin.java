package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.client.render.ACLightmapAdditions;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This mod's two lightmap insertions on 26.1, as edits to the render state rather than to a shader.
 *
 * <p>Up to 1.21.1 the lightmap was a CPU pixel loop that {@code LightTextureMixin} re-ran with its
 * own additions; from 1.21.2 it was one GPU draw and the additions became two extra uniforms on a
 * copy of vanilla's fragment shader ({@code assets/alexscaves/shaders/core/ac_lightmap.fsh}). 26.1
 * splits the draw off from the data: {@link LightmapRenderStateExtractor} fills a plain
 * {@link LightmapRenderState} of public fields, and {@code Lightmap#render} uploads it into one
 * std140 block. Every input this mod wanted to bend is a field on that state, so on 26 there is no
 * custom shader, no pipeline and no uniform buffer of our own — just a TAIL injection that adjusts
 * four numbers. {@code ac_lightmap.fsh} is consequently dead from 26 on.
 *
 * <p><b>The bonus.</b> The old shader added {@code ACAmbientLight} inside {@code get_brightness}, so
 * it reached the fragment as a constant {@code bonus * BlockFactor} of block-tinted light plus
 * {@code bonus * SkyFactor} of sky-coloured light, on every texel including the black corner. 26.1's
 * shader has a channel for exactly that constant — {@code AmbientColor}, which is where vanilla now
 * puts the dimension's own ambient light — so the bonus is folded in there with the same two
 * weights, and clamped at zero per component the way {@code get_brightness} clamped its sum (the
 * bonus goes negative while a primordial boss is active).
 *
 * <p><b>The tint.</b> The old shader multiplied the finished colour by {@code ACLightColor} before
 * night vision scaled it up, which preserved the hue. 26.1 composes the colour as
 * {@code max(AmbientColor, NightVisionColor * f) + SkyLightColor * sky + blockTintMix * block}, so
 * the equivalent is to multiply all four colour inputs — night vision included, without which the
 * tint would wash out under the effect rather than survive it as it used to.
 *
 * <p>One deliberate behaviour change: the old shader skipped the tint on a bright lightmap
 * ({@code UseBrightLightmap}, i.e. the End). That flag is gone — the End's flat lightmap is an
 * environment-attribute layer now, with nothing left for a caller to ask — so the tint applies
 * there too. It is white outside a coloured cave biome, which no End biome is.
 *
 * <p>This file is compiled and named in the mixin config only on 26 and up — see
 * {@code ModPlatformPlugin.configureJava} and its {@code vanishedMixins} list.
 */
@Mixin(value = LightmapRenderStateExtractor.class, priority = -100)
public class LightmapRenderStateExtractorMixin {

    @Inject(
            method = {"Lnet/minecraft/client/renderer/LightmapRenderStateExtractor;extract(Lnet/minecraft/client/renderer/state/LightmapRenderState;F)V"},
            remap = true,
            at = @At(value = "TAIL")
    )
    private void ac_extract(LightmapRenderState state, float partialTicks, CallbackInfo ci) {
        float bonus = ACLightmapAdditions.ambientBonus();
        boolean tint = ACLightmapAdditions.tintEnabled();
        if (bonus == 0.0F && !tint) {
            return;
        }
        // TAIL is the right anchor rather than RETURN: extract's two other exits are the "nothing was
        // extracted" ones (no update pending, or no level/player yet) and leave the state untouched.
        if (bonus != 0.0F) {
            state.ambientColor = new Vector3f(
                    Math.max(0.0F, state.ambientColor.x() + bonus * (state.blockLightTint.x() * state.blockFactor + state.skyLightColor.x() * state.skyFactor)),
                    Math.max(0.0F, state.ambientColor.y() + bonus * (state.blockLightTint.y() * state.blockFactor + state.skyLightColor.y() * state.skyFactor)),
                    Math.max(0.0F, state.ambientColor.z() + bonus * (state.blockLightTint.z() * state.blockFactor + state.skyLightColor.z() * state.skyFactor))
            );
        }
        if (tint) {
            Vec3 color = ACLightmapAdditions.tintColor();
            state.ambientColor = ac_tint(state.ambientColor, color);
            state.skyLightColor = ac_tint(state.skyLightColor, color);
            state.blockLightTint = ac_tint(state.blockLightTint, color);
            state.nightVisionColor = ac_tint(state.nightVisionColor, color);
        }
    }

    @Unique
    private static Vector3f ac_tint(Vector3fc source, Vec3 color) {
        return new Vector3f((float) (source.x() * color.x), (float) (source.y() * color.y), (float) (source.z() * color.z));
    }
}
