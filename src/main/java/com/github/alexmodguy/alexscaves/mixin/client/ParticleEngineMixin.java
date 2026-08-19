package com.github.alexmodguy.alexscaves.mixin.client;

import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Gives Alex's Caves' thirteen self-drawing particles a home in the 1.21.9 particle engine.
 *
 * <p>1.21.9 turned {@code ParticleRenderType} from "what blend state do I draw under" into "which
 * extractor collects me", and the engine keeps one {@code ParticleGroup} per type. Two things stop a
 * mod's own type from working out of the box, and this mixin fixes exactly those two:
 *
 * <ul>
 *   <li>{@code createParticleGroup} falls through to a {@code QuadParticleGroup} for any type it does
 *       not recognise, which would try to extract these as sprite quads;</li>
 *   <li>{@code extract} walks the private {@code RENDER_ORDER} list — the three vanilla types — so a
 *       mod group would be ticked and never drawn.</li>
 * </ul>
 *
 * <p>Both hooks key off the {@link com.github.alexmodguy.alexscaves.client.particle.ACParticleBuffers#GROUP_TYPE}
 * singleton. That has to be the same instance every time: both loaders order two unregistered types
 * in the engine's {@code TreeMap} by {@code System.identityHashCode}, so a second, equal
 * {@code ParticleRenderType} would silently land in a different bucket.
 *
 * <p>Below 1.21.9 the whole body is gated out and this is an empty mixin, because there was nothing
 * to fix: those particles declared {@code ParticleRenderType.CUSTOM} and vanilla rendered them
 * itself.
 */
@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    //? if >=1.21.9 {
    /*@org.spongepowered.asm.mixin.Shadow
    @org.spongepowered.asm.mixin.Final
    private java.util.Map<net.minecraft.client.particle.ParticleRenderType, net.minecraft.client.particle.ParticleGroup<?>> particles;

    @org.spongepowered.asm.mixin.injection.Inject(method = "createParticleGroup", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private void ac_createParticleGroup(net.minecraft.client.particle.ParticleRenderType type, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.client.particle.ParticleGroup<?>> cir) {
        if (type == com.github.alexmodguy.alexscaves.client.particle.ACParticleBuffers.GROUP_TYPE) {
            cir.setReturnValue(new com.github.alexmodguy.alexscaves.client.particle.ACParticleBuffers.CustomGroup((ParticleEngine) (Object) this));
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "extract", at = @org.spongepowered.asm.mixin.injection.At("TAIL"))
    private void ac_extract(net.minecraft.client.renderer.state.ParticlesRenderState renderState, net.minecraft.client.renderer.culling.Frustum frustum, net.minecraft.client.Camera camera, float partialTick, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        net.minecraft.client.particle.ParticleGroup<?> group =
                this.particles.get(com.github.alexmodguy.alexscaves.client.particle.ACParticleBuffers.GROUP_TYPE);
        if (group != null && !group.isEmpty()) {
            renderState.add(group.extractRenderState(frustum, camera, partialTick));
        }
    }
    *///?}
}
