package com.github.alexmodguy.alexscaves.client.render.entity.layer;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.model.AtlatitanModel;
import com.github.alexmodguy.alexscaves.client.render.entity.AtlatitanRenderer;
import com.github.alexmodguy.alexscaves.server.entity.living.AtlatitanEntity;
import com.github.alexmodguy.alexscaves.server.misc.ACMath;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class AtlatitanRiderLayer extends RenderLayer<AtlatitanEntity, AtlatitanModel> {

    public AtlatitanRiderLayer(AtlatitanRenderer render) {
        super(render);
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferIn, int packedLightIn, AtlatitanEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        float bodyYaw = entity.yBodyRotO + (entity.yBodyRot - entity.yBodyRotO) * partialTicks;
        if (entity.isVehicle()) {
            float animationIntensity = 0.0F;
            if(entity.getAnimation() == AtlatitanEntity.ANIMATION_STOMP){
                animationIntensity = ACMath.cullAnimationTick(entity.getAnimationTick(), 1F, AtlatitanEntity.ANIMATION_STOMP, partialTicks, 0, 30);
            }
            Vec3 offset = new Vec3(0, -5.75F, -0.5F - 0.7F * animationIntensity);
            Vec3 ridePos = getParentModel().getRiderPosition(offset);
            for (Entity passenger : entity.getPassengers()) {
                if (passenger == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
                    continue;
                }
                AlexsCaves.PROXY.releaseRenderingEntity(passenger.getUUID());
                poseStack.pushPose();
                poseStack.translate(ridePos.x, ridePos.y - 1.65F + passenger.getBbHeight(), ridePos.z);
                poseStack.mulPose(Axis.XN.rotationDegrees(180F));
                poseStack.mulPose(Axis.YN.rotationDegrees(360 - bodyYaw));
                renderPassenger(passenger, 0, 0, 0, 0, partialTicks, poseStack, bufferIn, packedLightIn);
                poseStack.popPose();
                AlexsCaves.PROXY.blockRenderingEntity(passenger.getUUID());
            }

        }
    }

    public static <E extends Entity> void renderPassenger(E entityIn, double x, double y, double z, float yaw, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn, int packedLight) {
        // The nested-render body (dispatcher lookup + vanilla's crash-report wrapping) moved to
        // ACClientCompat.renderEntity: from 1.21.2 the dispatcher's renderer lookup is
        // parameterised on a render state, so no call site can name its return type any more.
        ACClientCompat.renderEntity(entityIn, yaw, partialTicks, matrixStack, bufferIn, packedLight);
    }

}
