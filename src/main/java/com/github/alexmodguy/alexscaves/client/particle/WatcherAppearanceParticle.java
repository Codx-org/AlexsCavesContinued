package com.github.alexmodguy.alexscaves.client.particle;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.github.alexmodguy.alexscaves.client.model.WatcherModel;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.ForgeRenderTypes;

public class WatcherAppearanceParticle extends ACCustomParticle {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "textures/entity/watcher_appearance.png");
    private final WatcherModel model = new WatcherModel();

    private WatcherAppearanceParticle(ClientLevel lvl, double x, double y, double z) {
        super(lvl, x, y, z);
        this.setSize(12, 12);
        this.gravity = 0.0F;
        this.lifetime = 30;
    }

    public void render(VertexConsumer vertexConsumer, Camera camera, float partialTick) {
        // Pulling the far plane in to 40 for the duration of this one draw stops the watcher being
        // washed out by a long view distance. It is not portable past 1.21.5: the fog lives in a
        // GPU uniform block a frame at a time from 1.21.6, with no public way to override it for a
        // single draw and no way to read the old value back to restore it. The model sits about two
        // blocks from the camera, where linear fog contributes a couple of percent either way, so
        // the node simply draws it under the scene's own fog. See ACClientCompat's fog section.
        //? if <1.21.6 {
        float fogBefore = ACClientCompat.getShaderFogEnd();
        ACClientCompat.setShaderFogEnd(40);
        //?}
        float age = this.age + partialTick;
        float f = (age - 5) / (float) (this.lifetime - 5);
        float initalFlip = Math.min(f, 0.1F) / 0.1F;
        float scale = 1;
        PoseStack posestack = new PoseStack();
        posestack.mulPose(camera.rotation());
        posestack.translate(0.0D, 0F, -1.2F);
        posestack.mulPose(Axis.XP.rotationDegrees(0F));
        posestack.scale(-scale, -scale, scale);
        posestack.translate(0.0D, 0.5F, 2 + (1F - initalFlip));
        MultiBufferSource multibuffersource$buffersource = ACParticleBuffers.source();
        VertexConsumer vertexconsumer = multibuffersource$buffersource.getBuffer(ForgeRenderTypes.getUnlitTranslucent(TEXTURE));
        this.model.positionForParticle(partialTick, age);
        this.model.renderToBuffer(posestack, vertexconsumer, 240, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, Mth.clamp(1 - f * f, 0F, 1F));
        ACParticleBuffers.endBatch(multibuffersource$buffersource);
        //? if <1.21.6 {
        ACClientCompat.setShaderFogEnd(fogBefore);
        //?}
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new WatcherAppearanceParticle(worldIn, x, y, z);
        }
    }
}
