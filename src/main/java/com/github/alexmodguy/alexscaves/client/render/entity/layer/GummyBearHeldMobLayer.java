package com.github.alexmodguy.alexscaves.client.render.entity.layer;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.model.GummyBearModel;
import com.github.alexmodguy.alexscaves.client.render.entity.GummyBearRenderer;
import com.github.alexmodguy.alexscaves.server.entity.living.SubterranodonEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.GummyBearEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;

public class GummyBearHeldMobLayer extends RenderLayer<GummyBearEntity, GummyBearModel> {

    public GummyBearHeldMobLayer(GummyBearRenderer render) {
        super(render);
    }

    public void render(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn, GummyBearEntity bear, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        Entity heldMob = bear.getHeldMob();
        if (heldMob != null) {
            float bodyYaw = heldMob.yRotO + (heldMob.getYRot() - heldMob.yRotO) * partialTicks;
            AlexsCaves.PROXY.releaseRenderingEntity(heldMob.getUUID());
            matrixStackIn.pushPose();
            getParentModel().translateToHand(HumanoidArm.RIGHT, matrixStackIn);
            matrixStackIn.translate(0.1F * bear.getScale(), 0.7F * bear.getScale(), -0.3F * bear.getScale());
            matrixStackIn.mulPose(Axis.XN.rotationDegrees(180F));
            matrixStackIn.mulPose(Axis.YN.rotationDegrees(-90F));
            matrixStackIn.mulPose(Axis.XN.rotationDegrees(-10F));
            if (!AlexsCaves.PROXY.isFirstPersonPlayer(heldMob)) {
                renderEntity(heldMob, 0, 0, 0, 0, partialTicks, matrixStackIn, bufferIn, packedLightIn);
            }
            matrixStackIn.popPose();
            AlexsCaves.PROXY.blockRenderingEntity(heldMob.getUUID());
        }
    }

    public <E extends Entity> void renderEntity(E entityIn, double x, double y, double z, float yaw, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn, int packedLight) {
        // The nested-render body (dispatcher lookup + vanilla's crash-report wrapping) moved to
        // ACClientCompat.renderEntity: from 1.21.2 the dispatcher's renderer lookup is
        // parameterised on a render state, so no call site can name its return type any more.
        ACClientCompat.renderEntity(entityIn, yaw, partialTicks, matrixStack, bufferIn, packedLight);
    }
}
