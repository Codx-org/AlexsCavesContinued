package com.github.alexmodguy.alexscaves.client.render.entity;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.github.alexmodguy.alexscaves.server.entity.item.DesolateDaggerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
//? if <1.21.4
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if <1.21.4
import net.minecraftforge.client.model.data.ModelData;

import javax.annotation.Nullable;
import java.util.List;

public class DesolateDaggerRenderer extends EntityRenderer<DesolateDaggerEntity> {

    public DesolateDaggerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    public void render(DesolateDaggerEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource source, int lightIn) {
        super.render(entity, entityYaw, partialTicks, poseStack, source, lightIn);
        float ageInTicks = partialTicks + entity.tickCount;
        double stab = Math.max(entity.getStab(partialTicks), Math.sin(ageInTicks * 0.1F) * 0.2F);
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.5D, 0.0D);
        poseStack.mulPose(Axis.YN.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) + 90.0F));
        poseStack.mulPose(Axis.ZN.rotationDegrees((float) (Mth.lerp(partialTicks, entity.xRotO, entity.getXRot()) + 5F * Math.sin(ageInTicks * 0.2F))));
        poseStack.mulPose(Axis.ZN.rotationDegrees(45));
        float f = 1.0F;
        float f1 = 0;
        float f2 = 0;
        float startAlpha = ageInTicks < 3 ? 0 : (ageInTicks - 3) / 6F;
        float alpha = (float) Math.min(0.6F + stab, Math.min(1F, startAlpha));
        int redOverlay = OverlayTexture.pack(OverlayTexture.u(0), OverlayTexture.v(true));
        poseStack.translate(stab, stab + Math.cos(ageInTicks * 0.1F) * 0.2F, 0);
        // The item is drawn red and semi-transparent rather than as itself, which used to mean walking
        // its baked model and sending every quad to one buffer with a flat colour. From 1.21.4 the
        // model is unreachable and the render state draws itself, so the colour rides in on a vertex
        // consumer instead — and the state applies the -0.5 recentre this method used to do by hand.
        //? if >=1.21.4 {
        /*com.github.alexmodguy.alexscaves.client.render.item.ACItemRenderCompat.renderTinted(entity.daggerRenderStack, entity.level(), net.minecraft.world.item.ItemDisplayContext.NONE, poseStack, source.getBuffer(Sheets.translucentItemSheet()), f, f1, f2, alpha, 240, redOverlay);
        *///?} else {
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        BakedModel bakedmodel = Minecraft.getInstance().getItemRenderer().getModel(entity.daggerRenderStack, entity.level(), null, 0);
        for (net.minecraft.client.renderer.RenderType rt : ACClientCompat.itemRenderTypes(bakedmodel, entity.daggerRenderStack)) {
            renderModel(poseStack.last(), source.getBuffer(Sheets.translucentItemSheet()), alpha, null, bakedmodel, f, f1, f2, 240, redOverlay, ModelData.EMPTY, rt);
        }
        //?}
        poseStack.popPose();
    }

    //? if <1.21.4 {
    public static void renderModel(PoseStack.Pose p_111068_, VertexConsumer p_111069_, float alpha, @Nullable BlockState p_111070_, BakedModel p_111071_, float p_111072_, float p_111073_, float p_111074_, int p_111075_, int p_111076_, ModelData modelData, net.minecraft.client.renderer.RenderType renderType) {
        RandomSource randomsource = RandomSource.create();
        long i = 42L;

        for (Direction direction : Direction.values()) {
            randomsource.setSeed(42L);
            renderQuadList(p_111068_, p_111069_, p_111072_, p_111073_, p_111074_, alpha, ACClientCompat.modelQuads(p_111071_, p_111070_, direction, randomsource, modelData, renderType), p_111075_, p_111076_);
        }

        randomsource.setSeed(42L);
        renderQuadList(p_111068_, p_111069_, p_111072_, p_111073_, p_111074_, alpha, ACClientCompat.modelQuads(p_111071_, p_111070_, (Direction) null, randomsource, modelData, renderType), p_111075_, p_111076_);
    }

    private static void renderQuadList(PoseStack.Pose p_111059_, VertexConsumer p_111060_, float p_111061_, float p_111062_, float p_111063_, float alpha, List<BakedQuad> p_111064_, int p_111065_, int p_111066_) {
        for (BakedQuad bakedquad : p_111064_) {
            float f;
            float f1;
            float f2;
            f = Mth.clamp(p_111061_, 0.0F, 1.0F);
            f1 = Mth.clamp(p_111062_, 0.0F, 1.0F);
            f2 = Mth.clamp(p_111063_, 0.0F, 1.0F);
            com.github.alexmodguy.alexscaves.client.ACClientCompat.putBulkData(p_111060_, p_111059_, bakedquad, new float[]{1.0F, 1.0F, 1.0F, 1.0F}, f, f1, f2, alpha, new int[]{p_111065_, p_111065_, p_111065_, p_111065_}, p_111066_, false);
        }

    }
    //?}

    public ResourceLocation getTextureLocation(DesolateDaggerEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

