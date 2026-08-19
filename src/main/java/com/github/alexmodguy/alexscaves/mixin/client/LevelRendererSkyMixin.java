package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.ClientProxy;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * Alex's Caves' replacement for {@code LevelRenderer#renderSky} — it tints the sky, fades the
 * sunrise out and hides the sun, moon and stars inside a cave biome, by taking the whole method
 * over and re-emitting vanilla's body with three values changed.
 *
 * <p>This lives apart from {@link LevelRendererMixin} only because it has to disappear whole on
 * 1.21.2, which moved every one of these draws into {@code SkyRenderer} and left {@code renderSky}
 * with nothing to inject into. The body below already carries {@code /* *}{@code /}-commented
 * Stonecutter arms from the 1.20.5 wave, and those cannot nest inside another one — so the file is
 * dropped from the compile and from the mixin config instead (see {@code ModPlatformPlugin}). The
 * same three changes are made on 1.21.2 and up by {@link SkyRendererMixin}, which needs no copy of
 * vanilla at all.
 */
@Mixin(value = LevelRenderer.class, priority = 800)
public abstract class LevelRendererSkyMixin {

    @Shadow
    private ClientLevel level;
    @Shadow
    private int ticks;

    @Shadow @Final private Minecraft minecraft;

    @Shadow protected abstract void renderEndSky(PoseStack p_109781_);

    @Shadow protected abstract boolean doesMobEffectBlockSky(Camera p_234311_);

    @Shadow @Nullable private VertexBuffer skyBuffer;

    @Shadow @Final private static ResourceLocation SUN_LOCATION;

    @Shadow @Final private static ResourceLocation MOON_LOCATION;

    @Shadow @Nullable private VertexBuffer starBuffer;

    @Shadow @Nullable private VertexBuffer darkBuffer;

    // have to completely override this method for compatibility reasons
    //
    // 1.20.5 stopped handing the level renderer a PoseStack: the frustum arrives as a bare Matrix4f
    // and vanilla builds its own stack from it. Only the header differs — the body below is one
    // shared copy of vanilla's sky, so the newer arm rebuilds the stack the old parameter was.
    //? if >=1.20.5 {
    /*@Inject(method = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
            at = @At(
                    value = "HEAD"
            ),
            cancellable = true)
    private void ac_renderSky(Matrix4f frustumMatrix, Matrix4f matrix4f2, float partialTick, Camera camera, boolean foggy, Runnable runnable, CallbackInfo ci) {
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);
    *///?} else {
    @Inject(method = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
            at = @At(
                    value = "HEAD"
            ),
            cancellable = true)
    private void ac_renderSky(PoseStack poseStack, Matrix4f matrix4f2, float partialTick, Camera camera, boolean foggy, Runnable runnable, CallbackInfo ci) {
    //?}
        //AC CODE START
        float override = ClientProxy.acSkyOverrideAmount;
        float primordialBoss = AlexsCaves.PROXY.getPrimordialBossActiveAmount(partialTick);
        if(!AlexsCaves.CLIENT_CONFIG.biomeSkyOverrides.get() || override <= 0.0F && primordialBoss <= 0.0F){
           return;
        }
        ci.cancel();
        // AC CODE END
        // Forge answered 1.20.5's loss of the PoseStack by dropping the argument from its dimension
        // effects hook; NeoForge kept the slot and passes the frustum matrix through it instead. The
        // !mc205-rendersky-effects replacement produces the Forge shape, so NeoForge needs its own
        // spelling — and the frustum matrix is a parameter only on the newer mixin arm above.
        //
        // The whole call is an IForgeDimensionSpecialEffects addition — it is what lets another mod's
        // dimension effects take the sky over entirely — and vanilla has nothing at this point in
        // renderSky to stand in for it, so the Fabric arm is empty. (Fabric API does offer a sky
        // takeover, but as its own HEAD injection into this same method rather than as a call from
        // inside it, so the two would race here rather than compose; that is a runtime ordering
        // question for whenever a Fabric node is actually booted, not something to fake in this copy.)
        //? if neoforge && >=1.20.5 {
        /*if (level.effects().renderSky(level, ticks, partialTick, frustumMatrix, camera, matrix4f2, foggy, runnable))
            return;
        *///?} elif fabric {
        /*
        *///?} else {
        if (level.effects().renderSky(level, ticks, partialTick, poseStack, camera, matrix4f2, foggy, runnable))
            return;
        //?}
        runnable.run();
        if (!foggy) {
            FogType fogtype = camera.getFluidInCamera();
            if (fogtype != FogType.POWDER_SNOW && fogtype != FogType.LAVA && !this.doesMobEffectBlockSky(camera)) {
                if (this.minecraft.level.effects().skyType() == DimensionSpecialEffects.SkyType.END) {
                    this.renderEndSky(poseStack);
                } else if (this.minecraft.level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL) {
                    Vec3 vec3 = this.level.getSkyColor(this.minecraft.gameRenderer.getMainCamera().getPosition(), partialTick);
                    //AC CODE START
                    vec3 = ClientProxy.processSkyColor(vec3, partialTick);
                    // AC CODE END
                    float f = (float)vec3.x;
                    float f1 = (float)vec3.y;
                    float f2 = (float)vec3.z;
                    FogRenderer.levelFogColor();
                    RenderSystem.depthMask(false);
                    RenderSystem.setShaderColor(f, f1, f2, 1.0F);
                    ShaderInstance shaderinstance = RenderSystem.getShader();
                    this.skyBuffer.bind();
                    this.skyBuffer.drawWithShader(poseStack.last().pose(), matrix4f2, shaderinstance);
                    VertexBuffer.unbind();
                    RenderSystem.enableBlend();
                    float[] afloat = this.level.effects().getSunriseColor(this.level.getTimeOfDay(partialTick), partialTick);

                    // AC CODE START
                    //remove sunrises inside cave biomes.
                    if (afloat != null && afloat.length >= 4 && AlexsCaves.CLIENT_CONFIG.biomeSkyOverrides.get()) {
                        afloat[3] = afloat[3] * (1F - override);
                    }
                    // AC CODE END

                    if (afloat != null) {
                        RenderSystem.setShader(GameRenderer::getPositionColorShader);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        poseStack.pushPose();
                        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                        float f3 = Mth.sin(this.level.getSunAngle(partialTick)) < 0.0F ? 180.0F : 0.0F;
                        poseStack.mulPose(Axis.ZP.rotationDegrees(f3));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                        float f4 = afloat[0];
                        float f5 = afloat[1];
                        float f6 = afloat[2];
                        Matrix4f matrix4f = poseStack.last().pose();
                        BufferBuilder bufferbuilder = ACClientCompat.beginTesselator(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
                        bufferbuilder.vertex(matrix4f, 0.0F, 100.0F, 0.0F).color(f4, f5, f6, afloat[3]).endVertex();
                        int i = 16;

                        for(int j = 0; j <= 16; ++j) {
                            float f7 = (float)j * ((float)Math.PI * 2F) / 16.0F;
                            float f8 = Mth.sin(f7);
                            float f9 = Mth.cos(f7);
                            bufferbuilder.vertex(matrix4f, f8 * 120.0F, f9 * 120.0F, -f9 * 40.0F * afloat[3]).color(afloat[0], afloat[1], afloat[2], 0.0F).endVertex();
                        }

                        ACClientCompat.drawTesselator(bufferbuilder);
                        poseStack.popPose();
                    }

                    RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                    poseStack.pushPose();
                    // AC CODE START
                    //use the "rain level" to hide the sun and moon in the cave biomes.
                    float rainLevel = this.level.getRainLevel(partialTick);

                    rainLevel = Math.max(override, rainLevel);

                    float f11 = 1.0F - rainLevel;
                    // AC CODE END
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, f11);
                    poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
                    poseStack.mulPose(Axis.XP.rotationDegrees(this.level.getTimeOfDay(partialTick) * 360.0F));
                    Matrix4f matrix4f1 = poseStack.last().pose();
                    float f12 = 30.0F;
                    RenderSystem.setShader(GameRenderer::getPositionTexShader);
                    RenderSystem.setShaderTexture(0, SUN_LOCATION);
                    BufferBuilder bufferbuilder = ACClientCompat.beginTesselator(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                    bufferbuilder.vertex(matrix4f1, -f12, 100.0F, -f12).uv(0.0F, 0.0F).endVertex();
                    bufferbuilder.vertex(matrix4f1, f12, 100.0F, -f12).uv(1.0F, 0.0F).endVertex();
                    bufferbuilder.vertex(matrix4f1, f12, 100.0F, f12).uv(1.0F, 1.0F).endVertex();
                    bufferbuilder.vertex(matrix4f1, -f12, 100.0F, f12).uv(0.0F, 1.0F).endVertex();
                    ACClientCompat.drawTesselator(bufferbuilder);
                    f12 = 20.0F;
                    RenderSystem.setShaderTexture(0, MOON_LOCATION);
                    int k = this.level.getMoonPhase();
                    int l = k % 4;
                    int i1 = k / 4 % 2;
                    float f13 = (float)(l + 0) / 4.0F;
                    float f14 = (float)(i1 + 0) / 2.0F;
                    float f15 = (float)(l + 1) / 4.0F;
                    float f16 = (float)(i1 + 1) / 2.0F;
                    bufferbuilder = ACClientCompat.beginTesselator(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                    bufferbuilder.vertex(matrix4f1, -f12, -100.0F, f12).uv(f15, f16).endVertex();
                    bufferbuilder.vertex(matrix4f1, f12, -100.0F, f12).uv(f13, f16).endVertex();
                    bufferbuilder.vertex(matrix4f1, f12, -100.0F, -f12).uv(f13, f14).endVertex();
                    bufferbuilder.vertex(matrix4f1, -f12, -100.0F, -f12).uv(f15, f14).endVertex();
                    ACClientCompat.drawTesselator(bufferbuilder);
                    float f10 = this.level.getStarBrightness(partialTick) * f11;
                    if (f10 > 0.0F) {
                        RenderSystem.setShaderColor(f10, f10, f10, f10);
                        FogRenderer.setupNoFog();
                        this.starBuffer.bind();
                        this.starBuffer.drawWithShader(poseStack.last().pose(), matrix4f2, GameRenderer.getPositionShader());
                        VertexBuffer.unbind();
                        runnable.run();
                    }

                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    RenderSystem.disableBlend();
                    RenderSystem.defaultBlendFunc();
                    poseStack.popPose();
                    RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
                    double horizonHeight = this.level.getLevelData().getHorizonHeight(this.level);
                    double d0 = this.minecraft.player.getEyePosition(partialTick).y - horizonHeight;
                    if (d0 < 0.0D) {
                        poseStack.pushPose();
                        poseStack.translate(0.0F, 12.0F, 0.0F);
                        this.darkBuffer.bind();
                        this.darkBuffer.drawWithShader(poseStack.last().pose(), matrix4f2, shaderinstance);
                        VertexBuffer.unbind();
                        poseStack.popPose();
                    }

                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    RenderSystem.depthMask(true);
                }
            }
        }
    }
}
