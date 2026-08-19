package com.github.alexmodguy.alexscaves.mixin.client.citadel;

import com.github.alexmodguy.alexscaves.citadel.CitadelEvent;
import com.github.alexmodguy.alexscaves.citadel.CitadelConstants;
import com.github.alexmodguy.alexscaves.citadel.client.event.EventPosePlayerHand;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin extends Model {

    // 1.21.2 gave Model a root ModelPart of its own. A mixin's constructor never runs — it exists
    // only so javac accepts the `extends Model` that lets the handlers see the model's parts.
    //? if >=1.21.2 {
    /*public HumanoidModelMixin(net.minecraft.client.model.geom.ModelPart root, Function<ResourceLocation, RenderType> p_103110_) {
        super(root, p_103110_);
    }
    *///?} else {
    public HumanoidModelMixin(Function<ResourceLocation, RenderType> p_103110_) {
        super(p_103110_);
    }
    //?}

    // 1.21.2 rewrote the arm posers around the render state: they take (HumanoidRenderState, ArmPose)
    // instead of the LivingEntity, and the entity is no longer reachable from the model at all. The
    // event this fires is entity-shaped — Alex's Caves reads the rider's vehicle, its pose and its
    // per-player use progress out of it — so the >=1.21.2 arm recovers the entity from the duck that
    // mixin.renderstate.EntityRendererMixin stamps onto every state it extracts.
    // 1.21.11 then dropped the ArmPose again — the pose is read off the render state inside the
    // poser now — so the descriptor and the handler both lose their trailing argument. Only the two
    // @Injects differ between the two render-state bands; citadel_poseArm below is shared.
    //? if >=1.21.11 {
    /*@Inject(at = @At("HEAD"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/client/model/HumanoidModel;poseRightArm(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", cancellable = true)
    private void citadel_poseRightArm(net.minecraft.client.renderer.entity.state.HumanoidRenderState state, CallbackInfo ci) {
        this.citadel_poseArm(com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess.entity(state), false, ci);
    }

    @Inject(at = @At("HEAD"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/client/model/HumanoidModel;poseLeftArm(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", cancellable = true)
    private void citadel_poseLeftArm(net.minecraft.client.renderer.entity.state.HumanoidRenderState state, CallbackInfo ci) {
        this.citadel_poseArm(com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess.entity(state), true, ci);
    }
    *///?} elif >=1.21.2 {
    /*@Inject(at = @At("HEAD"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/client/model/HumanoidModel;poseRightArm(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;Lnet/minecraft/client/model/HumanoidModel$ArmPose;)V", cancellable = true)
    private void citadel_poseRightArm(net.minecraft.client.renderer.entity.state.HumanoidRenderState state, HumanoidModel.ArmPose armPose, CallbackInfo ci) {
        this.citadel_poseArm(com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess.entity(state), false, ci);
    }

    @Inject(at = @At("HEAD"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/client/model/HumanoidModel;poseLeftArm(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;Lnet/minecraft/client/model/HumanoidModel$ArmPose;)V", cancellable = true)
    private void citadel_poseLeftArm(net.minecraft.client.renderer.entity.state.HumanoidRenderState state, HumanoidModel.ArmPose armPose, CallbackInfo ci) {
        this.citadel_poseArm(com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess.entity(state), true, ci);
    }
    *///?}

    //? if >=1.21.2 {
    /*@org.spongepowered.asm.mixin.Unique
    private void citadel_poseArm(net.minecraft.world.entity.Entity entity, boolean left, CallbackInfo ci) {
        if (entity instanceof LivingEntity living) {
            EventPosePlayerHand event = new EventPosePlayerHand(living, (HumanoidModel) ((Model) this), left);
            EventPosePlayerHand.post(event);
            if (event.getCitadelResult() == CitadelEvent.Result.ALLOW) {
                ci.cancel();
            }
        }
    }
    *///?} else {

    @Inject(at = @At("HEAD"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/client/model/HumanoidModel;poseRightArm(Lnet/minecraft/world/entity/LivingEntity;)V", cancellable = true)
    private void citadel_poseRightArm(LivingEntity entity, CallbackInfo ci) {
        EventPosePlayerHand event = new EventPosePlayerHand(entity, (HumanoidModel) ((Model) this), false);
        EventPosePlayerHand.post(event);
        if (event.getCitadelResult() == CitadelEvent.Result.ALLOW) {
            ci.cancel();
        }
    }


    @Inject(at = @At("HEAD"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/client/model/HumanoidModel;poseLeftArm(Lnet/minecraft/world/entity/LivingEntity;)V", cancellable = true)
    private void citadel_poseLeftArm(LivingEntity entity, CallbackInfo ci) {
        EventPosePlayerHand event = new EventPosePlayerHand(entity, (HumanoidModel) ((Model) this), true);
        EventPosePlayerHand.post(event);
        if (event.getCitadelResult() == CitadelEvent.Result.ALLOW) {
            ci.cancel();
        }
    }
    //?}

}
