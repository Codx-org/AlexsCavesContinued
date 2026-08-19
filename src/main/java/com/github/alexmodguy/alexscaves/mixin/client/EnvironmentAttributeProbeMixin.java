package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.ClientProxy;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The 1.21.11 home of the cave-biome sky override, which up to 1.21.10 lived in two injections on
 * {@code ClientLevel} — {@code getSkyColor(Vec3, float)} and {@code getSkyDarken(float)}. Both
 * methods are gone: the sky is described by {@code EnvironmentAttributes} now, sampled through the
 * camera's probe.
 *
 * <p>One injection replaces both, and it is not a coincidence that it can. Every client consumer of
 * either value goes through {@code Camera#attributeProbe().getValue(attr, partialTick)} — the sky
 * disc via {@code SkyRenderer#extractRenderState}, the fog via {@code AtmosphericFogEnvironment},
 * the lightmap via {@code LightTextureMixin#ac_skyFactor} — so this is the same single choke point
 * the two {@code ClientLevel} methods used to be, for the same set of callers.
 *
 * <p>{@code SKY_LIGHT_FACTOR} is the post-ramp value, i.e. vanilla's own
 * {@code flash ? 1 : darken * 0.95 + 0.05}, so the old {@code max(skyDarken, amount)} is applied
 * here as {@code max(factor, amount * 0.95 + 0.05)} — the same number the old code produced once
 * vanilla had ramped it.
 */
@Mixin(EnvironmentAttributeProbe.class)
public class EnvironmentAttributeProbeMixin {

    @Inject(
            method = {"Lnet/minecraft/world/attribute/EnvironmentAttributeProbe;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;"},
            remap = true,
            cancellable = true,
            at = @At(value = "RETURN")
    )
    private void ac_getValue(EnvironmentAttribute<?> attribute, float partialTick, CallbackInfoReturnable<Object> cir) {
        if (!AlexsCaves.CLIENT_CONFIG.biomeSkyOverrides.get() || ClientProxy.acSkyOverrideAmount <= 0.0F) {
            return;
        }
        if (attribute == EnvironmentAttributes.SKY_COLOR) {
            int packed = (Integer) cir.getReturnValue();
            Vec3 prevVec3 = new Vec3(
                    ARGB.red(packed) / 255.0D,
                    ARGB.green(packed) / 255.0D,
                    ARGB.blue(packed) / 255.0D);
            Vec3 sampledVec3 = ClientProxy.processSkyColor(ClientProxy.acSkyOverrideColor, partialTick);
            Vec3 mixed = prevVec3.add(sampledVec3.subtract(prevVec3).scale(ClientProxy.acSkyOverrideAmount));
            cir.setReturnValue(ARGB.colorFromFloat(
                    ARGB.alpha(packed) / 255.0F,
                    (float) mixed.x, (float) mixed.y, (float) mixed.z));
        } else if (attribute == EnvironmentAttributes.SKY_LIGHT_FACTOR) {
            cir.setReturnValue(Math.max((Float) cir.getReturnValue(), ClientProxy.acSkyOverrideAmount * 0.95F + 0.05F));
        }
    }
}
