package com.github.alexmodguy.alexscaves.client.render.blockentity;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.github.alexmodguy.alexscaves.client.model.SauropodBaseModel;
import com.github.alexmodguy.alexscaves.server.block.blockentity.AmberMonolithBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.ForgeRenderTypes;

public class AmberMonolithBlockRenderer<T extends AmberMonolithBlockEntity> implements BlockEntityRenderer<T> {


    protected final RandomSource random = RandomSource.create();

    public AmberMonolithBlockRenderer(BlockEntityRendererProvider.Context rendererDispatcherIn) {
    }

    @Override
    // 1.21.5 added the camera position to BlockEntityRenderer#render; none of these
    // renderers need it (they all work in the block-local pose).
    //? if >=1.21.5 {
    /*public void render(T amber, float partialTicks, PoseStack poseStack, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn, net.minecraft.world.phys.Vec3 acCameraPos) {
    *///?} else {
    public void render(T amber, float partialTicks, PoseStack poseStack, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
    //?}
        Entity currentEntity = amber.getDisplayEntity(Minecraft.getInstance().level);
        float age = amber.tickCount + partialTicks;
        float spin = amber.getRotation(partialTicks);
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.65F, 0.5F);
        if (currentEntity != null) {
            float f = 0.45F;
            float f1 = Math.max(currentEntity.getBbWidth(), currentEntity.getBbHeight());
            if ((double) f1 > 1.0D) {
                f /= f1 * 1.5F;
            }
            poseStack.translate(0, f * 1.5F - 1.25F + (float) (Math.cos(age * 0.05) * 0.05F), 0);
            poseStack.scale(f, f, f);
            poseStack.mulPose(Axis.YP.rotationDegrees(spin));
            renderEntityInAmber(currentEntity, 0, 0, 0, 0, partialTicks, poseStack, bufferIn, 1.0F);
        }
        poseStack.popPose();
    }

    public static <E extends Entity> void renderEntityInAmber(E entityIn, double x, double y, double z, float yaw, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn, float transparency) {

        Object render = null;
        try {
            // The renderer itself is never held: from 1.21.2 the dispatcher's lookup is
            // parameterised on a render state, so its type is unnameable here. Everything this
            // method wanted off it — model, texture, posing — goes through ACClientCompat.
            render = ACClientCompat.rendererModel(entityIn);
            float animSpeed = 0;
            float animSpeedOld = 0;
            float animPos = 0;
            float xRot = entityIn.getXRot();
            float xRotOld = entityIn.xRotO;
            float yRot = entityIn.getYRot();
            float yRotOld = entityIn.yRotO;
            float yBodyRot = 0;
            float yBodyRotOld = 0;
            float headRot = 0;
            float headRotOld = 0;
            if (entityIn instanceof LivingEntity living) {
                headRot = living.yHeadRot;
                headRotOld = living.yHeadRotO;
                yBodyRot = living.yBodyRot;
                yBodyRotOld = living.yBodyRotO;
                living.yHeadRot = 0;
                living.yHeadRotO = 0;
                living.yBodyRot = 0;
                living.yBodyRotO = 0;
                entityIn.setXRot(0);
                entityIn.xRotO = 0;
                entityIn.setYRot(0);
                entityIn.yRotO = 0;
                // net.minecraft's EntityModel spelled out, never imported: on 1.21.2+ that
                // import is rewritten to the compat shim, and a mob in amber may just as
                // well be a vanilla one whose model is not.
                if (render instanceof net.minecraft.client.model.EntityModel) {
                    net.minecraft.client.model.EntityModel model = (net.minecraft.client.model.EntityModel) render;
                    VertexConsumer ivertexbuilder = bufferIn.getBuffer(ForgeRenderTypes.getUnlitTranslucent(ACClientCompat.rendererTexture(entityIn, partialTicks)));
                    matrixStack.pushPose();
                    boolean shouldSit = entityIn.isPassenger() && (entityIn.getVehicle() != null && com.github.alexmodguy.alexscaves.server.misc.ACCompat.shouldRiderSit(entityIn.getVehicle()));
                    boolean prevCrouching = false;
                    // 1.21.2 moved the crouch flag onto the render state, and the model the
                    // amber poses is never the one vanilla extracted, so there is nothing to
                    // suppress: a humanoid in amber is already drawn from a neutral state.
                    //? if <1.21.2 {
                    if (model instanceof HumanoidModel<?> humanoidModel) {
                        prevCrouching = humanoidModel.crouching;
                        humanoidModel.crouching = false;
                    }
                    //?}
                    if(model instanceof SauropodBaseModel sauropodBaseModel){
                        sauropodBaseModel.straighten = true;
                    }
                    ACClientCompat.setupAnim(model, living, living.isBaby(), shouldSit, living.getAttackAnim(partialTicks),
                            0.0F, 0.0F, -0.1F, 0.0F, 0.0F, partialTicks);

                    if(model instanceof SauropodBaseModel sauropodBaseModel){
                        sauropodBaseModel.straighten = false;
                    }
                    matrixStack.scale(living.getScale(), -living.getScale(), living.getScale());
                    ACClientCompat.renderToBuffer(model, matrixStack, ivertexbuilder, 240, OverlayTexture.NO_OVERLAY, 0.3F, 0.16F, 0.2F, transparency);
                    matrixStack.popPose();
                    //? if <1.21.2 {
                    if (model instanceof HumanoidModel<?> humanoidModel) {
                        humanoidModel.crouching = prevCrouching;
                    }
                    //?}
                }
                entityIn.setXRot(xRot);
                entityIn.xRotO = xRotOld;
                entityIn.setYRot(yRot);
                entityIn.yRotO = yRotOld;
                living.yHeadRot = headRot;
                living.yHeadRotO = headRotOld;
                living.yBodyRot = yBodyRot;
                living.yBodyRotO = yBodyRotOld;
            }
            ACClientCompat.bindMainRenderTargetForWrite(false);
        } catch (Throwable throwable3) {
            CrashReport crashreport = CrashReport.forThrowable(throwable3, "Rendering entity in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Entity being rendered");
            entityIn.fillCrashReportCategory(crashreportcategory);
            CrashReportCategory crashreportcategory1 = crashreport.addCategory("Renderer details");
            crashreportcategory1.setDetail("Assigned model", render);
            crashreportcategory1.setDetail("Rotation", Float.valueOf(yaw));
            crashreportcategory1.setDetail("Delta", Float.valueOf(partialTicks));
            throw new ReportedException(crashreport);
        }
    }
}