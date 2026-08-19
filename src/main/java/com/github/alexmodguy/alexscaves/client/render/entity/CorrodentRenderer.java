package com.github.alexmodguy.alexscaves.client.render.entity;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.model.CorrodentModel;
import com.github.alexmodguy.alexscaves.server.entity.living.CorrodentEntity;
import com.mojang.blaze3d.vertex.PoseStack;
//? if <26
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class CorrodentRenderer extends MobRenderer<CorrodentEntity, CorrodentModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "textures/entity/corrodent.png");
    private static final ResourceLocation TEXTURE_EYES = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "textures/entity/corrodent_eyes.png");
    private static final Map<BlockPos, Integer> allDugBlocksOnScreen = new HashMap<>();

    public CorrodentRenderer(EntityRendererProvider.Context renderManagerIn) {
        super(renderManagerIn, new CorrodentModel(), 0.5F);
        this.addLayer(new LayerGlow());
    }

    public ResourceLocation getTextureLocation(CorrodentEntity entity) {
        return TEXTURE;
    }

    public void render(CorrodentEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferIn, int packedLightIn) {
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        double x = Mth.lerp(partialTicks, entityIn.xOld, entityIn.getX());
        double y = Mth.lerp(partialTicks, entityIn.yOld, entityIn.getY());
        double z = Mth.lerp(partialTicks, entityIn.zOld, entityIn.getZ());
        float digAmount = entityIn.getDigAmount(partialTicks);
        if (digAmount > 0) {
            double digEffectDistance = 3;
            // 26 has no immediate-mode block draw left, so the crumbling decals cannot be batched up
            // and drawn from a render stage any more — submitBreakingBlockModel needs the frame's
            // SubmitNodeCollector, and by the time a chunk layer is being rasterised the submission
            // phase is over. The one place a collector is in reach is right here: from 1.21.9 the
            // MultiBufferSource an entity render body is handed is this mod's ACSubmitBuffers, which
            // carries it. So each corrodent submits its own decals as it is drawn. The only thing
            // lost is the cross-corrodent dedupe the map did — two corrodents chewing the same block
            // submit it twice, which is overdraw of identical geometry, not a visual difference.
            //? if >=26 {
            /*net.minecraft.client.renderer.SubmitNodeCollector acCollector =
                    com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers.collectorOf(bufferIn);
            *///?}
            for (BlockPos mutableBlockPos : BlockPos.betweenClosed((int) Math.floor(x - digEffectDistance), (int) Math.floor(y - digEffectDistance), (int) Math.floor(z - digEffectDistance), (int) Math.floor(x + digEffectDistance), (int) Math.floor(y + digEffectDistance), (int) Math.floor(z + digEffectDistance))) {
                int amount = (int) (entityIn.getCorrosionAmount(mutableBlockPos) * digAmount);
                if (amount >= 0) {
                    //? if >=26 {
                    /*submitCrumbling(acCollector, poseStack, mutableBlockPos.immutable(), amount - 1, x, y, z);
                    *///?} else {
                    allDugBlocksOnScreen.put(mutableBlockPos.immutable(), Math.max(allDugBlocksOnScreen.getOrDefault(mutableBlockPos, -1), amount));
                    //?}
                }
            }
        }
    }

    /**
     * Submits one crumbling decal, the way {@code LevelRenderer#submitBlockDestroyAnimation} does for
     * vanilla's own block breaking: translate to the block relative to whatever the pose is currently
     * anchored on — the entity here, the camera there — and hand the state's model, its position seed
     * and the 0-9 stage to the collector.
     */
    //? if >=26 {
    /*private static void submitCrumbling(net.minecraft.client.renderer.SubmitNodeCollector collector, PoseStack poseStack, BlockPos pos, int progress, double anchorX, double anchorY, double anchorZ) {
        if (collector == null || progress < 0 || progress >= 10 || Minecraft.getInstance().level == null) {
            return;
        }
        net.minecraft.world.level.block.state.BlockState state = Minecraft.getInstance().level.getBlockState(pos);
        if (state.getRenderShape() != net.minecraft.world.level.block.RenderShape.MODEL) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(pos.getX() - anchorX, pos.getY() - anchorY, pos.getZ() - anchorZ);
        submitBreakingModel(collector, poseStack, state, pos, progress);
        poseStack.popPose();
    }
    *///?}

    /**
     * The submit call itself, which moved between 26 and 26.2 — hoisted out of {@link #submitCrumbling}
     * because Stonecutter cannot nest a second condition inside the arm that method already lives in.
     *
     * <p>26 handed the collector a whole {@code BlockStateModel} plus the position seed and let it pick
     * the parts. 26.2 takes the baked {@code BlockStateModelPart} list instead, so the caller does what
     * {@code LevelRenderer#submitBlockDestroyAnimation} does: seed a thread-local random with the
     * block's position seed, collect the parts through it and copy the list, since the collector keeps
     * the reference and vanilla reuses its scratch buffer. It also applies the block's own render
     * offset (the sway on grass and flowers), which 26's overload did internally.
     */
    //? if >=26.2 {
    /*private static void submitBreakingModel(net.minecraft.client.renderer.SubmitNodeCollector collector, PoseStack poseStack, net.minecraft.world.level.block.state.BlockState state, BlockPos pos, int progress) {
        poseStack.translate(state.getOffset(pos));
        java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> parts = new java.util.ArrayList<>();
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.createThreadLocalInstance();
        random.setSeed(state.getSeed(pos));
        Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state).collectParts(random, parts);
        collector.submitBreakingBlockModel(poseStack, java.util.List.copyOf(parts), progress);
    }
    *///?} elif >=26 {
    /*private static void submitBreakingModel(net.minecraft.client.renderer.SubmitNodeCollector collector, PoseStack poseStack, net.minecraft.world.level.block.state.BlockState state, BlockPos pos, int progress) {
        collector.submitBreakingBlockModel(poseStack,
                Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state),
                state.getSeed(pos), progress);
    }
    *///?}

    public static void renderEntireBatch(LevelRenderer levelRenderer, PoseStack poseStack, int renderTick, Camera camera, float partialTick) {
        if (!allDugBlocksOnScreen.isEmpty()) {
            poseStack.pushPose();
            Vec3 cameraPos = camera.getPosition();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            MultiBufferSource.BufferSource multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().crumblingBufferSource();
            for (Map.Entry<BlockPos, Integer> posAndInt : allDugBlocksOnScreen.entrySet()) {
                int progress = posAndInt.getValue() - 1;
                if (progress >= 0 && progress < 10) {
                    poseStack.pushPose();
                    BlockPos pos = posAndInt.getKey();
                    poseStack.translate((double) pos.getX(), (double) pos.getY(), (double) pos.getZ());
                    // NeoForge deleted its whole client/model/data package in 21.5 (vanilla's own
                    // renderBreakingTexture is the 5-arg form there); Forge 55.1.11 still ships
                    // ModelData and keeps its 6-arg overload alongside the vanilla one. 26 deleted
                    // the whole shape — SheetedDecalTextureGenerator and renderBreakingTexture both —
                    // and nothing ever enters the map there, so the >=26 arm is empty by construction
                    // rather than by choice; see render(...) for where the decals go instead.
                    // Fabric shares the vanilla arm on every version below 26: ModelData is a loader
                    // type, Level#getModelDataManager a loader patch, and the 6-arg overload a loader
                    // addition, so there is nothing there to be given the data even if this mod filled
                    // one — which it never does (see the ModelData stand-in).
                    //? if >=26 {
                    /*
                    *///?} elif (neoforge && >=1.21.5) || fabric {
                    /*PoseStack.Pose posestack$pose1 = poseStack.last();
                    VertexConsumer vertexconsumer1 = new SheetedDecalTextureGenerator(multibuffersource$buffersource.getBuffer(ModelBakery.DESTROY_TYPES.get(progress)), posestack$pose1.pose(), posestack$pose1.normal(), 1.0F);
                    Minecraft.getInstance().getBlockRenderer().renderBreakingTexture(Minecraft.getInstance().level.getBlockState(pos), pos, Minecraft.getInstance().level, poseStack, vertexconsumer1);
                    *///?} else {
                    PoseStack.Pose posestack$pose1 = poseStack.last();
                    VertexConsumer vertexconsumer1 = new SheetedDecalTextureGenerator(multibuffersource$buffersource.getBuffer(ModelBakery.DESTROY_TYPES.get(progress)), posestack$pose1.pose(), posestack$pose1.normal(), 1.0F);
                    net.minecraftforge.client.model.data.ModelData modelData = Minecraft.getInstance().level.getModelDataManager().getAt(pos);
                    Minecraft.getInstance().getBlockRenderer().renderBreakingTexture(Minecraft.getInstance().level.getBlockState(pos), pos, Minecraft.getInstance().level, poseStack, vertexconsumer1, modelData == null ? net.minecraftforge.client.model.data.ModelData.EMPTY : modelData);
                    //?}
                    poseStack.popPose();
                }
            }
            poseStack.popPose();
        }
        allDugBlocksOnScreen.clear();

    }

    class LayerGlow extends RenderLayer<CorrodentEntity, CorrodentModel> {

        public LayerGlow() {
            super(CorrodentRenderer.this);
        }

        public void render(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, CorrodentEntity entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            VertexConsumer ivertexbuilder = bufferIn.getBuffer(RenderType.eyes(TEXTURE_EYES));
            float alpha = 1.0F;
            this.getParentModel().renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, LivingEntityRenderer.getOverlayCoords(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, alpha);
        }
    }
}


