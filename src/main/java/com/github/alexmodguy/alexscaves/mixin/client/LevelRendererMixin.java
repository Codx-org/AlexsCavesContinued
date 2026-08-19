package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.server.block.EnergizedGalenaBlock;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(value = LevelRenderer.class, priority = 800)
public abstract class LevelRendererMixin {

    private int aclastCameraChunkX;
    private int aclastCameraChunkY;
    private int aclastCameraChunkZ;
    @Shadow
    @Nullable
    private ViewArea viewArea;

    // 1.21.9 renamed setupRender to cullTerrain and dropped one of its two booleans. It is private
    // now, which changes nothing for an @Inject — only the name and the descriptor move. The handler
    // reads none of its arguments, so the work sits in a @Unique helper and each arm is nothing but
    // the annotation and the argument list it has to mirror.
    //
    // 26.2 splits cullTerrain up: the culling itself moved into the chunk-render preparation, and the
    // one piece this mixin cares about — telling the ViewArea where the camera is — became a private
    // repositionCamera(CameraRenderState) that render() calls unconditionally as its very first act
    // (profiler section "repositionCamera", before the model-view stack is even pushed). So HEAD of
    // that is the same moment in the frame as HEAD of cullTerrain was.
    //? if >=26.2 {
    /*@Inject(method = "Lnet/minecraft/client/renderer/LevelRenderer;repositionCamera(Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At(
                    value = "HEAD"
            ),
            allow = 1)
    private void ac_setupRender(net.minecraft.client.renderer.state.CameraRenderState cameraState, CallbackInfo ci) {
        ac_repositionViewArea();
    }
    *///?} elif >=1.21.9 {
    /*@Inject(method = "Lnet/minecraft/client/renderer/LevelRenderer;cullTerrain(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Z)V",
            at = @At(
                    value = "HEAD"
            ),
            allow = 1)
    private void ac_setupRender(Camera camera, Frustum frustum, boolean isSpectator, CallbackInfo ci) {
        ac_repositionViewArea();
    }
    *///?} else {
    @Inject(method = "Lnet/minecraft/client/renderer/LevelRenderer;setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
            at = @At(
                    value = "HEAD"
            ),
            allow = 1)
    private void ac_setupRender(Camera camera, Frustum frustum, boolean b1, boolean b2, CallbackInfo ci) {
        ac_repositionViewArea();
    }
    //?}

    @org.spongepowered.asm.mixin.Unique
    private void ac_repositionViewArea() {
        if (Minecraft.getInstance().getCameraEntity() != null && Minecraft.getInstance().getCameraEntity() != Minecraft.getInstance().player) { // fixes chunks being too far to load when not the player
            double d0 = Minecraft.getInstance().getCameraEntity().getX();
            double d1 = Minecraft.getInstance().getCameraEntity().getY();
            double d2 = Minecraft.getInstance().getCameraEntity().getZ();
            int i = SectionPos.posToSectionCoord(d0);
            int j = SectionPos.posToSectionCoord(d1);
            int k = SectionPos.posToSectionCoord(d2);
            if (this.aclastCameraChunkX != i || this.aclastCameraChunkY != j || this.aclastCameraChunkZ != k) {
                this.aclastCameraChunkX = i;
                this.aclastCameraChunkY = j;
                this.aclastCameraChunkZ = k;
                // 1.21.2 replaced the two world-space doubles with the section the camera sits in —
                // ViewArea does the posToSectionCoord itself now.
                //? if >=1.21.2 {
                /*viewArea.repositionCamera(SectionPos.of(i, j, k));
                *///?} else {
                viewArea.repositionCamera(d0, d2);
                //?}
            }
        }
    }
}
