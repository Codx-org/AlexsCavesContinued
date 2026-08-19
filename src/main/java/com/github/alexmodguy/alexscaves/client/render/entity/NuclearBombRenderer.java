package com.github.alexmodguy.alexscaves.client.render.entity;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.github.alexmodguy.alexscaves.server.entity.item.NuclearBombEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class NuclearBombRenderer extends EntityRenderer<NuclearBombEntity> {

    public NuclearBombRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    public void render(NuclearBombEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource source, int lightIn) {
        super.render(entity, entityYaw, partialTicks, poseStack, source, lightIn);
        float progress = (entity.getTime() + partialTicks) / NuclearBombEntity.MAX_TIME;
        float expandScale = 1F + (float) Math.sin(progress * progress * Math.PI) * 0.5F;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (Math.cos((double) entity.tickCount * 3.25D) * 1.2F * progress * Math.PI)));
        poseStack.scale(1F + progress * 0.03F, 1, 1F + progress * 0.03F);
        poseStack.pushPose();
        poseStack.scale(expandScale, expandScale - progress * 0.3F, expandScale);
        poseStack.translate(-0.5D, 0.0D, -0.5D);
        BlockState state = ACBlockRegistry.NUCLEAR_BOMB.get().defaultBlockState();
        ACClientCompat.renderTintedBlock(state, poseStack, source, 1.0F - progress * 0.5F, 1.0F + progress, 1.0F - progress, 240, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        poseStack.popPose();
    }

    public ResourceLocation getTextureLocation(NuclearBombEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

