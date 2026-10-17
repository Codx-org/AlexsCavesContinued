package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;


import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.ClientProxy;
import com.github.alexmodguy.alexscaves.client.render.ACInternalShaders;
import com.github.alexmodguy.alexscaves.client.render.ACLightmapAdditions;
import com.github.alexmodguy.alexscaves.server.entity.util.PossessesCamera;
import com.github.alexmodguy.alexscaves.server.misc.ACLoadedMods;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import com.github.alexmodguy.alexscaves.server.potion.DeepsightEffect;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LightTexture.class, priority = -100)
public abstract class LightTextureMixin {

    // 1.21.2 moved the lightmap onto the GPU. The CPU-side NativeImage the loop below wrote into,
    // the DynamicTexture it was uploaded through and the two helpers that shaped the colour are all
    // gone; what is left is a 16x16 render target that a fragment shader fills in one draw. So the
    // two eras shadow disjoint sets of members — see ac_updateLightTexture.
    // ...and 1.21.5 went one step further: the 16x16 RenderTarget became a bare GpuTexture, drawn
    // into by a RenderPass rather than by binding a framebuffer.
    //? if >=1.21.5 {
    /*@Shadow
    @Final
    private com.mojang.blaze3d.textures.GpuTexture texture;
    *///?} elif >=1.21.2 {
    /*@Shadow
    @Final
    private com.mojang.blaze3d.pipeline.TextureTarget target;
    *///?} else {
    @Shadow
    @Final
    private NativeImage lightPixels;
    @Shadow
    @Final
    private DynamicTexture lightTexture;

    @Shadow
    private static void clampColor(Vector3f p_254122_) {
    }

    @Shadow
    protected abstract float notGamma(float p_109893_);
    //?}

    // 1.21.6 puts a GpuTextureView in front of the texture — createRenderPass takes the view now —
    // and folds every scalar uniform into one std140 block. Vanilla's own LightmapInfo buffer is
    // sized for its nine members, so this mixin cannot borrow it: AC's block adds ACAmbientLight and
    // ACLightColor, and a short buffer is a driver-level truncation rather than a compile error.
    // Hence a second MappableRingBuffer, created on first use and closed with the LightTexture.
    // Layout note: Std140Builder#putVec3 advances 16 bytes, so both vec3s must come LAST or the
    // scalars after them would silently land in the padding. 11 members => 80 bytes.
    //? if >=1.21.6 {
    /*@Shadow
    @Final
    private com.mojang.blaze3d.textures.GpuTextureView textureView;

    @org.spongepowered.asm.mixin.Unique
    private static final int AC_LIGHTMAP_UBO_SIZE = new com.mojang.blaze3d.buffers.Std140SizeCalculator()
            .putFloat().putFloat().putFloat().putInt().putFloat()
            .putFloat().putFloat().putFloat().putFloat()
            .putVec3().putVec3().get();

    @org.spongepowered.asm.mixin.Unique
    private net.minecraft.client.renderer.MappableRingBuffer ac_lightmapUbo;

    @Inject(
            method = {"Lnet/minecraft/client/renderer/LightTexture;close()V"},
            remap = true,
            at = @At(value = "TAIL")
    )
    private void ac_closeLightmapUbo(CallbackInfo ci) {
        if (this.ac_lightmapUbo != null) {
            this.ac_lightmapUbo.close();
            this.ac_lightmapUbo = null;
        }
    }
    *///?}

    @Shadow
    private boolean updateLightTexture;
    @Shadow
    @Final
    private Minecraft minecraft;

    // 1.21.5 deleted getDarknessGamma and inlined its one-line body at the call site; a @Shadow of a
    // method that no longer exists is a hard failure, so it goes with it.
    //? if <1.21.5 {
    @Shadow
    protected abstract float getDarknessGamma(float p_234320_);
    //?}

    @Shadow
    protected abstract float calculateDarknessScale(LivingEntity p_234313_, float p_234314_, float p_234315_);

    @Shadow
    public static float getBrightness(DimensionType p_234317_, int p_234318_) {
        return 0;
    }

    @Shadow
    private float blockLightRedFlicker;

    @Shadow
    @Final
    private GameRenderer renderer;

    @Inject(
            method = {"Lnet/minecraft/client/renderer/LightTexture;getBrightness(Lnet/minecraft/world/level/dimension/DimensionType;I)F"},
            remap = true,
            cancellable = true,
            at = @At(value = "TAIL")
    )
    private static void ac_getBrightness(DimensionType dimensionType, int lightTextureIndex, CallbackInfoReturnable<Float> cir) {
        float bonus = ac_ambientLightBonus();
        if (bonus != 0.0F) {
            cir.setReturnValue(Math.max(0.0F, cir.getReturnValue() + bonus));
        }
    }

    /**
     * How much this mod adds to a raw light level. Split out of {@code ac_getBrightness} because from
     * 1.21.2 the lightmap no longer calls {@code getBrightness} at all: the same bonus has to be
     * handed to the fragment shader instead, and the two have to agree.
     *
     * <p>The body itself lives in {@link ACLightmapAdditions}, because from 26.1 there is a third
     * caller in a different class again — see that class's javadoc.
     */
    private static float ac_ambientLightBonus() {
        return ACLightmapAdditions.ambientBonus();
    }

    // The three values vanilla's updateLightTexture reads off the level, hoisted into helpers because
    // 1.21.11 deleted every one of the calls that produced them and Stonecutter cannot nest a gate
    // inside the >=1.21.5 arm that holds the body. Hoisting is preferred over a replacements.string
    // rule here: the <1.21.5 arm below contains the very same three lines, and a text rule matching
    // one would rewrite the other too.
    //
    // 1.21.11 moved all three into the environment-attribute system:
    //   * the sky factor is EnvironmentAttributes.SKY_LIGHT_FACTOR, whose ClientLevel time-based layer
    //     is verbatim the old "flash ? 1 : darken * 0.95 + 0.05" (read out of ClientLevel's bytecode),
    //     so the attribute IS the post-ramp f1 and getSkyDarken/getSkyFlashTime are not missed;
    //   * the sky light tint is EnvironmentAttributes.SKY_LIGHT_COLOR, which replaces the hardcoded
    //     (f, f, 1) lerped 35% toward white;
    //   * forceBrightLightmap is gone with DimensionSpecialEffects and has no successor — the end
    //     dimension's flat lightmap is an attribute layer now, so nothing is left for a caller to ask.
    //? if >=1.21.11 {
    /*private float ac_skyFactor(ClientLevel level, float partialTicks) {
        return this.minecraft.gameRenderer.getMainCamera().attributeProbe()
                .getValue(net.minecraft.world.attribute.EnvironmentAttributes.SKY_LIGHT_FACTOR, partialTicks);
    }

    private Vector3f ac_skyLightColor(ClientLevel level, float partialTicks) {
        return net.minecraft.util.ARGB.vector3fFromRGB24(this.minecraft.gameRenderer.getMainCamera().attributeProbe()
                .getValue(net.minecraft.world.attribute.EnvironmentAttributes.SKY_LIGHT_COLOR, partialTicks));
    }

    private boolean ac_forceBrightLightmap(ClientLevel level) {
        return false;
    }
    *///?} elif >=1.21.5 {
    /*private float ac_skyFactor(ClientLevel level, float partialTicks) {
        float f = level.getSkyDarken(1.0F);
        return level.getSkyFlashTime() > 0 ? 1.0F : f * 0.95F + 0.05F;
    }

    private Vector3f ac_skyLightColor(ClientLevel level, float partialTicks) {
        float f = level.getSkyDarken(1.0F);
        return (new Vector3f(f, f, 1.0F)).lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
    }

    private boolean ac_forceBrightLightmap(ClientLevel level) {
        return level.effects().forceBrightLightmap();
    }
    *///?}

    // From 1.21.2 the lightmap is a 16x16 render target filled by one draw of core/lightmap, so
    // there is no CPU loop left to re-run. The two insertions move into a copy of that shader
    // (assets/alexscaves/shaders/core/ac_lightmap.fsh) and this is vanilla's own body verbatim,
    // pointed at it, with the biome bonus and the biome tint handed over as two extra uniforms.
    // 1.21.5 keeps that one draw and changes only how it is issued: no CompiledShaderProgram and no
    // framebuffer binding, just a RenderPass naming the 16x16 texture it fills, with the uniforms set
    // on the pass. The uniform names, the values and ac_lightmap.fsh itself are untouched.
    //? if >=1.21.5 {
    /*@Inject(
            method = {"Lnet/minecraft/client/renderer/LightTexture;updateLightTexture(F)V"},
            remap = true,
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private void ac_updateLightTexture(float partialTicks, CallbackInfo ci) {
        boolean tint = ACLightmapAdditions.tintEnabled();
        float bonus = ac_ambientLightBonus();
        if (!tint && bonus == 0.0F) {
            return;
        }
        ci.cancel();
        if (!this.updateLightTexture) {
            return;
        }
        this.updateLightTexture = false;
        net.minecraft.util.profiling.ProfilerFiller profiler = net.minecraft.util.profiling.Profiler.get();
        profiler.push("lightTex");
        ClientLevel clientlevel = this.minecraft.level;
        if (clientlevel != null) {
            float f1 = ac_skyFactor(clientlevel, partialTicks);
            float f2 = this.minecraft.options.darknessEffectScale().get().floatValue();
            float f3 = this.minecraft.player.getEffectBlendFactor(MobEffects.DARKNESS, partialTicks) * f2;
            float f4 = this.calculateDarknessScale(this.minecraft.player, f3, partialTicks) * f2;
            float f6 = this.minecraft.player.getWaterVision();
            float f5;
            if (this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION)) {
                f5 = GameRenderer.getNightVisionScale(this.minecraft.player, partialTicks);
            } else if (f6 > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER)) {
                f5 = f6;
            } else {
                f5 = 0.0F;
            }

            Vector3f vector3f = ac_skyLightColor(clientlevel, partialTicks);
            float f7 = this.blockLightRedFlicker + 1.5F;
            float f8 = clientlevel.dimensionType().ambientLight();
            boolean flag = ac_forceBrightLightmap(clientlevel);
            float f9 = this.minecraft.options.gamma().get().floatValue();
            Vec3 lightColor = new Vec3(1.0D, 1.0D, 1.0D);
            if (tint) {
                //INSERTION BY AC: the same gamma lift the CPU loop applied, on the one uniform that
                //carries it now. The shader skips the tint itself on a bright lightmap, as the loop did.
                float biomeAmbientLight = ClientProxy.lastBiomeAmbientLightAmountPrev + (ClientProxy.lastBiomeAmbientLightAmount - ClientProxy.lastBiomeAmbientLightAmountPrev) * ACClientCompat.frameTime();
                if (biomeAmbientLight > 0.0F) {
                    f9 = Mth.clamp(f9 + biomeAmbientLight, 0.0F, 1.0F);
                }
                lightColor = ACLightmapAdditions.tintColor();
            }
    *///?}
    // The draw itself is the only part that moved in 1.21.6: the eleven per-uniform calls become one
    // mapped std140 buffer written in the block's declaration order (see ac_lightmap.fsh), the pass
    // names the GpuTextureView rather than the GpuTexture, bindDefaultUniforms supplies the vanilla
    // blocks, and drawIndexed gained its baseVertex/instanceCount pair. Sibling gate: the enclosing
    // >=1.21.5 block closes above and reopens below, so everything around it stays shared.
    //
    // 1.21.9 then dropped the quad entirely — core/screenquad builds a full-screen triangle out of
    // gl_VertexID over DefaultVertexFormat.EMPTY — so the sequential index buffer, the vertex buffer
    // and drawIndexed all collapse into draw(0, 3), exactly as vanilla's own LightTexture now does.
    //? if >=1.21.9 {
    /*            com.mojang.blaze3d.systems.CommandEncoder encoder = com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder();
            if (this.ac_lightmapUbo == null) {
                this.ac_lightmapUbo = new net.minecraft.client.renderer.MappableRingBuffer(
                        () -> "AC Lightmap UBO",
                        com.mojang.blaze3d.buffers.GpuBuffer.USAGE_UNIFORM | com.mojang.blaze3d.buffers.GpuBuffer.USAGE_MAP_WRITE,
                        AC_LIGHTMAP_UBO_SIZE);
            }

            try (com.mojang.blaze3d.buffers.GpuBuffer.MappedView view = encoder.mapBuffer(this.ac_lightmapUbo.currentBuffer(), false, true)) {
                com.mojang.blaze3d.buffers.Std140Builder.intoBuffer(view.data())
                        .putFloat(f8)
                        .putFloat(f1)
                        .putFloat(f7)
                        .putInt(flag ? 1 : 0)
                        .putFloat(f5)
                        .putFloat(f4)
                        .putFloat(this.renderer.getDarkenWorldAmount(partialTicks))
                        .putFloat(Math.max(0.0F, f9 - f3))
                        .putFloat(bonus)
                        .putVec3(vector3f)
                        .putVec3((float) lightColor.x, (float) lightColor.y, (float) lightColor.z);
            }

            try (com.mojang.blaze3d.systems.RenderPass renderpass = encoder.createRenderPass(() -> "AC update light", this.textureView, java.util.OptionalInt.empty())) {
                renderpass.setPipeline(ACInternalShaders.LIGHTMAP);
                com.mojang.blaze3d.systems.RenderSystem.bindDefaultUniforms(renderpass);
                renderpass.setUniform("LightmapInfo", this.ac_lightmapUbo.currentBuffer());
                renderpass.draw(0, 3);
            }

            this.ac_lightmapUbo.rotate();
    *///?} elif >=1.21.6 {
    /*            com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer indices = com.mojang.blaze3d.systems.RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            com.mojang.blaze3d.buffers.GpuBuffer indexBuffer = indices.getBuffer(6);
            com.mojang.blaze3d.systems.CommandEncoder encoder = com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder();
            if (this.ac_lightmapUbo == null) {
                this.ac_lightmapUbo = new net.minecraft.client.renderer.MappableRingBuffer(
                        () -> "AC Lightmap UBO",
                        com.mojang.blaze3d.buffers.GpuBuffer.USAGE_UNIFORM | com.mojang.blaze3d.buffers.GpuBuffer.USAGE_MAP_WRITE,
                        AC_LIGHTMAP_UBO_SIZE);
            }

            try (com.mojang.blaze3d.buffers.GpuBuffer.MappedView view = encoder.mapBuffer(this.ac_lightmapUbo.currentBuffer(), false, true)) {
                com.mojang.blaze3d.buffers.Std140Builder.intoBuffer(view.data())
                        .putFloat(f8)
                        .putFloat(f1)
                        .putFloat(f7)
                        .putInt(flag ? 1 : 0)
                        .putFloat(f5)
                        .putFloat(f4)
                        .putFloat(this.renderer.getDarkenWorldAmount(partialTicks))
                        .putFloat(Math.max(0.0F, f9 - f3))
                        .putFloat(bonus)
                        .putVec3(vector3f)
                        .putVec3((float) lightColor.x, (float) lightColor.y, (float) lightColor.z);
            }

            try (com.mojang.blaze3d.systems.RenderPass renderpass = encoder.createRenderPass(() -> "AC update light", this.textureView, java.util.OptionalInt.empty())) {
                renderpass.setPipeline(ACInternalShaders.LIGHTMAP);
                com.mojang.blaze3d.systems.RenderSystem.bindDefaultUniforms(renderpass);
                renderpass.setUniform("LightmapInfo", this.ac_lightmapUbo.currentBuffer());
                renderpass.setVertexBuffer(0, com.mojang.blaze3d.systems.RenderSystem.getQuadVertexBuffer());
                renderpass.setIndexBuffer(indexBuffer, indices.type());
                renderpass.drawIndexed(0, 0, 6, 1);
            }

            this.ac_lightmapUbo.rotate();
    *///?} elif >=1.21.5 {
    /*            com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer indices = com.mojang.blaze3d.systems.RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            com.mojang.blaze3d.buffers.GpuBuffer indexBuffer = indices.getBuffer(6);

            try (com.mojang.blaze3d.systems.RenderPass renderpass = com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder().createRenderPass(this.texture, java.util.OptionalInt.empty())) {
                renderpass.setPipeline(ACInternalShaders.LIGHTMAP);
                renderpass.setUniform("AmbientLightFactor", f8);
                renderpass.setUniform("SkyFactor", f1);
                renderpass.setUniform("BlockFactor", f7);
                renderpass.setUniform("UseBrightLightmap", flag ? 1 : 0);
                renderpass.setUniform("SkyLightColor", vector3f.x, vector3f.y, vector3f.z);
                renderpass.setUniform("NightVisionFactor", f5);
                renderpass.setUniform("DarknessScale", f4);
                renderpass.setUniform("DarkenWorldFactor", this.renderer.getDarkenWorldAmount(partialTicks));
                renderpass.setUniform("BrightnessFactor", Math.max(0.0F, f9 - f3));
                renderpass.setUniform("ACAmbientLight", bonus);
                renderpass.setUniform("ACLightColor", (float) lightColor.x, (float) lightColor.y, (float) lightColor.z);
                renderpass.setVertexBuffer(0, com.mojang.blaze3d.systems.RenderSystem.getQuadVertexBuffer());
                renderpass.setIndexBuffer(indexBuffer, indices.type());
                renderpass.drawIndexed(0, 6);
            }
    *///?}
    //? if >=1.21.5 {
    /*
            profiler.pop();
        }
    }
    *///?} elif >=1.21.2 {
    /*@Inject(
            method = {"Lnet/minecraft/client/renderer/LightTexture;updateLightTexture(F)V"},
            remap = true,
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private void ac_updateLightTexture(float partialTicks, CallbackInfo ci) {
        boolean tint = ACLightmapAdditions.tintEnabled();
        float bonus = ac_ambientLightBonus();
        if (!tint && bonus == 0.0F) {
            return;
        }
        ci.cancel();
        if (!this.updateLightTexture) {
            return;
        }
        this.updateLightTexture = false;
        net.minecraft.util.profiling.ProfilerFiller profiler = net.minecraft.util.profiling.Profiler.get();
        profiler.push("lightTex");
        ClientLevel clientlevel = this.minecraft.level;
        if (clientlevel != null) {
            float f = clientlevel.getSkyDarken(1.0F);
            float f1;
            if (clientlevel.getSkyFlashTime() > 0) {
                f1 = 1.0F;
            } else {
                f1 = f * 0.95F + 0.05F;
            }

            float f2 = this.minecraft.options.darknessEffectScale().get().floatValue();
            float f3 = this.getDarknessGamma(partialTicks) * f2;
            float f4 = this.calculateDarknessScale(this.minecraft.player, f3, partialTicks) * f2;
            float f6 = this.minecraft.player.getWaterVision();
            float f5;
            if (this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION)) {
                f5 = GameRenderer.getNightVisionScale(this.minecraft.player, partialTicks);
            } else if (f6 > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER)) {
                f5 = f6;
            } else {
                f5 = 0.0F;
            }

            Vector3f vector3f = (new Vector3f(f, f, 1.0F)).lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
            float f7 = this.blockLightRedFlicker + 1.5F;
            float f8 = clientlevel.dimensionType().ambientLight();
            boolean flag = clientlevel.effects().forceBrightLightmap();
            float f9 = this.minecraft.options.gamma().get().floatValue();
            Vec3 lightColor = new Vec3(1.0D, 1.0D, 1.0D);
            if (tint) {
                //INSERTION BY AC: the same gamma lift the CPU loop applied, on the one uniform that
                //carries it now. The shader skips the tint itself on a bright lightmap, as the loop did.
                float biomeAmbientLight = ClientProxy.lastBiomeAmbientLightAmountPrev + (ClientProxy.lastBiomeAmbientLightAmount - ClientProxy.lastBiomeAmbientLightAmountPrev) * ACClientCompat.frameTime();
                if (biomeAmbientLight > 0.0F) {
                    f9 = Mth.clamp(f9 + biomeAmbientLight, 0.0F, 1.0F);
                }
                lightColor = ACLightmapAdditions.tintColor();
            }

            net.minecraft.client.renderer.CompiledShaderProgram program = java.util.Objects.requireNonNull(
                    com.mojang.blaze3d.systems.RenderSystem.setShader(ACInternalShaders.LIGHTMAP), "Alex's Caves lightmap shader not loaded");
            program.safeGetUniform("AmbientLightFactor").set(f8);
            program.safeGetUniform("SkyFactor").set(f1);
            program.safeGetUniform("BlockFactor").set(f7);
            program.safeGetUniform("UseBrightLightmap").set(flag ? 1 : 0);
            program.safeGetUniform("SkyLightColor").set(vector3f);
            program.safeGetUniform("NightVisionFactor").set(f5);
            program.safeGetUniform("DarknessScale").set(f4);
            program.safeGetUniform("DarkenWorldFactor").set(this.renderer.getDarkenWorldAmount(partialTicks));
            program.safeGetUniform("BrightnessFactor").set(Math.max(0.0F, f9 - f3));
            program.safeGetUniform("ACAmbientLight").set(bonus);
            program.safeGetUniform("ACLightColor").set((float) lightColor.x, (float) lightColor.y, (float) lightColor.z);
            this.target.bindWrite(true);
            com.mojang.blaze3d.vertex.BufferBuilder bufferbuilder = com.mojang.blaze3d.systems.RenderSystem.renderThreadTesselator()
                    .begin(VertexFormat.Mode.QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.BLIT_SCREEN);
            bufferbuilder.addVertex(0.0F, 0.0F, 0.0F);
            bufferbuilder.addVertex(1.0F, 0.0F, 0.0F);
            bufferbuilder.addVertex(1.0F, 1.0F, 0.0F);
            bufferbuilder.addVertex(0.0F, 1.0F, 0.0F);
            com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
            this.target.unbindWrite();
            profiler.pop();
        }
    }
    *///?} else {
    //redirect causes startup crash with optifine, so this is the workaround :/
    @Inject(
            method = {"Lnet/minecraft/client/renderer/LightTexture;updateLightTexture(F)V"},
            remap = true,
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private void ac_updateLightTexture(float partialTicks, CallbackInfo ci) {
        if (ACLightmapAdditions.tintEnabled()) {
            ci.cancel();
            if (this.updateLightTexture) {
                this.updateLightTexture = false;
                this.minecraft.getProfiler().push("lightTex");
                ClientLevel clientlevel = this.minecraft.level;
                if (clientlevel != null) {
                    float f = clientlevel.getSkyDarken(1.0F);
                    float f1;
                    if (clientlevel.getSkyFlashTime() > 0) {
                        f1 = 1.0F;
                    } else {
                        f1 = f * 0.95F + 0.05F;
                    }

                    float f2 = this.minecraft.options.darknessEffectScale().get().floatValue();
                    float f3 = this.getDarknessGamma(partialTicks) * f2;
                    float f4 = this.calculateDarknessScale(this.minecraft.player, f3, partialTicks) * f2;
                    float f6 = this.minecraft.player.getWaterVision();
                    float f5;
                    if (this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION)) {
                        f5 = GameRenderer.getNightVisionScale(this.minecraft.player, partialTicks);
                    } else if (f6 > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER)) {
                        f5 = f6;
                    } else {
                        f5 = 0.0F;
                    }

                    Vector3f vector3f = (new Vector3f(f, f, 1.0F)).lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
                    float f7 = this.blockLightRedFlicker + 1.5F;
                    Vector3f vector3f1 = new Vector3f();

                    for (int i = 0; i < 16; ++i) {
                        for (int j = 0; j < 16; ++j) {
                            float f8 = getBrightness(clientlevel.dimensionType(), i) * f1;
                            float f9 = getBrightness(clientlevel.dimensionType(), j) * f7;
                            float f10 = f9 * ((f9 * 0.6F + 0.4F) * 0.6F + 0.4F);
                            float f11 = f9 * (f9 * f9 * 0.6F + 0.4F);
                            vector3f1.set(f9, f10, f11);
                            boolean flag = clientlevel.effects().forceBrightLightmap();
                            if (flag) {
                                vector3f1.lerp(new Vector3f(0.99F, 1.12F, 1.0F), 0.25F);
                                clampColor(vector3f1);
                            } else {
                                Vector3f vector3f2 = (new Vector3f((Vector3fc) vector3f)).mul(f8);
                                vector3f1.add(vector3f2);
                                vector3f1.lerp(new Vector3f(0.75F, 0.75F, 0.75F), 0.04F);
                                if (this.renderer.getDarkenWorldAmount(partialTicks) > 0.0F) {
                                    float f12 = this.renderer.getDarkenWorldAmount(partialTicks);
                                    Vector3f vector3f3 = (new Vector3f((Vector3fc) vector3f1)).mul(0.7F, 0.6F, 0.6F);
                                    vector3f1.lerp(vector3f3, f12);
                                }
                            }

                            // This body is a copy of vanilla's updateLightTexture, so it keeps the
                            // loader's own hook where vanilla calls it. It is an
                            // IForgeDimensionSpecialEffects addition with no vanilla counterpart —
                            // vanilla calls nothing here — so on Fabric the line simply goes away.
                            //? if !fabric
                            clientlevel.effects().adjustLightmapColors(clientlevel, partialTicks, f, f7, f8, j, i, vector3f1);
                            //INSERTION BY AC...
                            this.applyACLightingColors(clientlevel, vector3f1);

                            if (f5 > 0.0F) {
                                float f13 = Math.max(vector3f1.x(), Math.max(vector3f1.y(), vector3f1.z()));
                                if (f13 < 1.0F) {
                                    float f15 = 1.0F / f13;
                                    Vector3f vector3f5 = (new Vector3f((Vector3fc) vector3f1)).mul(f15);
                                    vector3f1.lerp(vector3f5, f5);
                                }
                            }

                            if (!flag) {
                                if (f4 > 0.0F) {
                                    vector3f1.add(-f4, -f4, -f4);
                                }

                                clampColor(vector3f1);
                            }

                            float f14 = this.minecraft.options.gamma().get().floatValue();
                            //INSERTION BY AC
                            float biomeAmbientLight = ClientProxy.lastBiomeAmbientLightAmountPrev + (ClientProxy.lastBiomeAmbientLightAmount - ClientProxy.lastBiomeAmbientLightAmountPrev) * ACClientCompat.frameTime();
                            if(biomeAmbientLight > 0.0F){
                                f14 = Mth.clamp(f14 + biomeAmbientLight, 0.0F, 1.0F);
                            }

                            Vector3f vector3f4 = new Vector3f(this.notGamma(vector3f1.x), this.notGamma(vector3f1.y), this.notGamma(vector3f1.z));
                            vector3f1.lerp(vector3f4, Math.max(0.0F, f14 - f3));
                            vector3f1.lerp(new Vector3f(0.75F, 0.75F, 0.75F), 0.04F);
                            clampColor(vector3f1);
                            vector3f1.mul(255.0F);
                            int j1 = 255;
                            int k = (int) vector3f1.x();
                            int l = (int) vector3f1.y();
                            int i1 = (int) vector3f1.z();
                            ACClientCompat.setPixelABGR(this.lightPixels, j, i, -16777216 | i1 << 16 | l << 8 | k);
                        }
                    }

                    this.lightTexture.upload();
                    this.minecraft.getProfiler().pop();
                }
            }
        }
    }
    //?}

    // Only the pre-1.21.2 CPU loop calls this — from 1.21.2 the same tint is a shader uniform — so it
    // is gated rather than carried dead, which also keeps DimensionSpecialEffects out of every node
    // from 1.21.11 up, where the class no longer exists.
    //? if <1.21.2 {
    private void applyACLightingColors(ClientLevel clientLevel, Vector3f vector3f) {
        if (!clientLevel.effects().forceBrightLightmap()) {
            Vec3 in = new Vec3(vector3f);
            Vec3 to = ACLightmapAdditions.tintColor();
            vector3f.set(to.x * in.x, to.y * in.y, to.z * in.z);
        }
    }
    //?}


}
