package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;


import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.ClientProxy;
import com.github.alexmodguy.alexscaves.client.render.entity.SubmarineRenderer;
import com.github.alexmodguy.alexscaves.client.render.entity.layer.ACPotionEffectLayer;
import com.github.alexmodguy.alexscaves.server.entity.item.SubmarineEntity;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    private float darkenWorldAmount;

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Inject(
            method = {"Lnet/minecraft/client/renderer/GameRenderer;tick()V"},
            remap = true,
            at = @At(value = "TAIL")
    )
    public void ac_tick(CallbackInfo ci) {
        if (((ClientProxy) AlexsCaves.PROXY).renderNukeSkyDarkFor > 0 && darkenWorldAmount < 1.0F) {
            darkenWorldAmount = Math.min(darkenWorldAmount + 0.3F, 1.0F);
        }
    }

    // 1.21 folded the frame's partial tick and nano time into a DeltaTracker. The float this
    // handler wants is the pause-aware residual the two old arguments already carried, which is
    // what getGameTimeDeltaPartialTick(true) returns — see ACClientCompat#frameTime.
    @Inject(
            //? if >=1.21 {
            /*method = {"Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V"},
            *///?} else {
            method = {"Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V"},
            //?}
            remap = true,
            at = @At(
                    value = "INVOKE",
                    // 1.21.6 made Lighting an instance held by the GameRenderer and folded its four
                    // static setups into one setupFor(Lighting.Entry). The call still sits in the
                    // same place — the last thing done to the world before the GUI is drawn — and
                    // ITEMS_3D is the entry that replaced setupFor3DItems.
                    //? if >=1.21.6 {
                    /*target = "Lcom/mojang/blaze3d/platform/Lighting;setupFor(Lcom/mojang/blaze3d/platform/Lighting$Entry;)V",
                    *///?} else {
                    target = "Lcom/mojang/blaze3d/platform/Lighting;setupFor3DItems()V",
                    //?}
                    shift = At.Shift.AFTER
            )
    )
    //? if >=1.21 {
    /*public void ac_render(net.minecraft.client.DeltaTracker deltaTracker, boolean idk, CallbackInfo ci) {
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
    *///?} else {
    public void ac_render(float partialTick, long nanos, boolean idk, CallbackInfo ci) {
    //?}
        ((ClientProxy) AlexsCaves.PROXY).preScreenRender(partialTick);
    }


    // Two shape changes stack up on this one. 1.20.5 stopped passing renderLevel a PoseStack —
    // it builds its own from the bobbing transforms, which is the very object the old parameter
    // used to be, so @Local recovers it — and moved renderItemInHand onto a projection matrix.
    // 1.21 then replaced the partial tick and nano time with a DeltaTracker. 1.21.6 dropped the
    // Camera from renderItemInHand — it reads the game renderer's own — and put the is-spectator
    // flag in its place; renderLevel itself is unchanged, and still builds the same local PoseStack
    // the @Local recovers.
    @Inject(
            //? if >=1.21 {
            /*method = {"Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V"},
            *///?} elif >=1.20.5 {
            /*method = {"Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJ)V"},
            *///?} else {
            method = {"Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V"},
            //?}
            remap = true,
            at = @At(
                    value = "INVOKE",
                    //? if >=1.21.6 {
                    /*target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(FZLorg/joml/Matrix4f;)V",
                    *///?} elif >=1.20.5 {
                    /*target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/Camera;FLorg/joml/Matrix4f;)V",
                    *///?} else {
                    target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Camera;F)V",
                    //?}
                    shift = At.Shift.BEFORE
            )
    )
    //? if >=1.21 {
    /*public void ac_renderLevel(net.minecraft.client.DeltaTracker deltaTracker, CallbackInfo ci, @com.llamalad7.mixinextras.sugar.Local PoseStack poseStack) {
        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);
    *///?} elif >=1.20.5 {
    /*public void ac_renderLevel(float partialTicks, long time, CallbackInfo ci, @com.llamalad7.mixinextras.sugar.Local PoseStack poseStack) {
    *///?} else {
    public void ac_renderLevel(float partialTicks, long time, PoseStack poseStack, CallbackInfo ci) {
    //?}
        Entity player = Minecraft.getInstance().getCameraEntity();
        if (player != null && player.isPassenger() && player.getVehicle() instanceof SubmarineEntity submarine && SubmarineRenderer.isFirstPersonFloodlightsMode(submarine)) {
            Vec3 offset = submarine.getPosition(partialTicks).subtract(player.getEyePosition(partialTicks));
            poseStack.pushPose();
            poseStack.translate(offset.x, offset.y, offset.z);
            SubmarineRenderer.renderSubFirstPerson(submarine, partialTicks, poseStack, renderBuffers.bufferSource());
            poseStack.popPose();
        }
    }

    // Same three shapes as ac_renderLevel above; only the shift differs.
    @Inject(
            //? if >=1.21 {
            /*method = {"Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V"},
            *///?} elif >=1.20.5 {
            /*method = {"Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJ)V"},
            *///?} else {
            method = {"Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V"},
            //?}
            remap = true,
            at = @At(
                    value = "INVOKE",
                    //? if >=1.21.6 {
                    /*target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(FZLorg/joml/Matrix4f;)V",
                    *///?} elif >=1.20.5 {
                    /*target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/Camera;FLorg/joml/Matrix4f;)V",
                    *///?} else {
                    target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Camera;F)V",
                    //?}
                    shift = At.Shift.AFTER
            )
    )
    //? if >=1.21 {
    /*public void ac_renderLevelAfterHand(net.minecraft.client.DeltaTracker deltaTracker, CallbackInfo ci, @com.llamalad7.mixinextras.sugar.Local PoseStack poseStack) {
    *///?} elif >=1.20.5 {
    /*public void ac_renderLevelAfterHand(float partialTicks, long time, CallbackInfo ci, @com.llamalad7.mixinextras.sugar.Local PoseStack poseStack) {
    *///?} else {
    public void ac_renderLevelAfterHand(float partialTicks, long time, PoseStack poseStack, CallbackInfo ci) {
    //?}
        if (Minecraft.getInstance().getCameraEntity() instanceof LivingEntity living && living.hasEffect(ACCompat.effect(ACEffectRegistry.BUBBLED.get())) && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            MultiBufferSource.BufferSource multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
            ACPotionEffectLayer.renderBubbledFirstPerson(poseStack);
            multibuffersource$buffersource.endBatch();
        }
    }
}
