package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.client.render.ACLightmapAdditions;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The 26.1 half of {@code LightTextureMixin#ac_getBrightness}.
 *
 * <p>26.1 split {@code LightTexture} in two: the texture and its per-frame upload kept the class and
 * were renamed {@link Lightmap}, while the extraction of everything the shader needs moved to
 * {@code LightmapRenderStateExtractor} (see {@code LightmapRenderStateExtractorMixin}, which carries
 * the other half of this mod's lightmap work). {@code getBrightness} went with the first of those and
 * is otherwise untouched, so this is the old injection with a new owner.
 *
 * <p>This file is compiled and named in the mixin config only on 26 and up — see
 * {@code ModPlatformPlugin.configureJava} and its {@code vanishedMixins} list.
 */
@Mixin(value = Lightmap.class, priority = -100)
public class LightmapMixin {

    @Inject(
            method = {"Lnet/minecraft/client/renderer/Lightmap;getBrightness(Lnet/minecraft/world/level/dimension/DimensionType;I)F"},
            remap = true,
            cancellable = true,
            at = @At(value = "TAIL")
    )
    private static void ac_getBrightness(DimensionType dimensionType, int lightTextureIndex, CallbackInfoReturnable<Float> cir) {
        float bonus = ACLightmapAdditions.ambientBonus();
        if (bonus != 0.0F) {
            cir.setReturnValue(Math.max(0.0F, cir.getReturnValue() + bonus));
        }
    }
}
