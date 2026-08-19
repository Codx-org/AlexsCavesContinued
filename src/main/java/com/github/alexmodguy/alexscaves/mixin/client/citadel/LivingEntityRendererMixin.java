package com.github.alexmodguy.alexscaves.mixin.client.citadel;

import com.github.alexmodguy.alexscaves.citadel.CitadelConstants;
import com.github.alexmodguy.alexscaves.citadel.client.event.EventLivingRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.renderer.entity.LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Shadow
    protected net.minecraft.client.model.EntityModel model;

    // 1.21.2 rebuilt this renderer around render states. setupRotations became
    // (S, PoseStack, float bodyRot, float scale) — no entity, no partial tick — so both come back
    // off the state through ACStateAccess, which mixin.renderstate.EntityRendererMixin fills in
    // during extraction.
    //
    // Only this first of the four injections survives the move. The two around EntityModel#setupAnim
    // and the one at the end of render() post events that nothing in this tree listens for, and
    // EventLivingRenderer is relocated into this mod, so nothing outside it can listen either.
    // render() lost the entity, the yaw and the partial tick that those three events carry, so
    // reinstating them would mean inventing values for an event with no reader. They stop at 1.21.1.
    //? if >=1.21.2 {
    /*@Inject(
            method = {"Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;setupRotations(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V"},
            remap = CitadelConstants.REMAPREFS,
            at = @At(value = "RETURN")
    )
    protected void citadel_setupRotations(net.minecraft.client.renderer.entity.state.LivingEntityRenderState renderState, PoseStack poseStack, float bodyYRot, float scale, CallbackInfo ci) {
        if (com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess.entity(renderState) instanceof LivingEntity livingEntity) {
            EventLivingRenderer.SetupRotations event = new EventLivingRenderer.SetupRotations(livingEntity, model, poseStack, bodyYRot,
                    com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess.partialTick(renderState));
            EventLivingRenderer.SetupRotations.post(event);
        }
    }
    *///?} else {
    @Inject(
            method = {"Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V"},
            remap = CitadelConstants.REMAPREFS,
            at = @At(value = "RETURN")
    )
    protected void citadel_setupRotations(LivingEntity livingEntity, PoseStack poseStack, float ageInTicks, float bodyYRot, float partialTick, CallbackInfo ci) {
        EventLivingRenderer.SetupRotations event = new EventLivingRenderer.SetupRotations(livingEntity, model, poseStack, bodyYRot, partialTick);
        EventLivingRenderer.SetupRotations.post(event);

    }

    @Inject(
            method = {"Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
            remap = CitadelConstants.REMAPREFS,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",
                    shift = At.Shift.BEFORE
            )
    )
    protected void citadel_render_setupAnim_before(LivingEntity livingEntity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        EventLivingRenderer.PreSetupAnimations event = new EventLivingRenderer.PreSetupAnimations(livingEntity, model, poseStack, yaw, partialTicks, bufferSource, packedLight);
        EventLivingRenderer.PreSetupAnimations.post(event);

    }

    @Inject(
            method = {"Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
            remap = CitadelConstants.REMAPREFS,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",
                    shift = At.Shift.AFTER
            )
    )
    protected void citadel_render_setupAnim_after(LivingEntity livingEntity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        EventLivingRenderer.PostSetupAnimations event = new EventLivingRenderer.PostSetupAnimations(livingEntity, model, poseStack, yaw, partialTicks, bufferSource, packedLight);
        EventLivingRenderer.PostSetupAnimations.post(event);
    }

    @Inject(
            method = {"Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
            remap = CitadelConstants.REMAPREFS,
            at = @At(value = "RETURN")
    )
    protected void citadel_render_renderToBuffer(LivingEntity livingEntity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        EventLivingRenderer.PostRenderModel event = new EventLivingRenderer.PostRenderModel(livingEntity, model, poseStack, yaw, partialTicks, bufferSource, packedLight);
        EventLivingRenderer.PostRenderModel.post(event);
    }
    //?}
}
