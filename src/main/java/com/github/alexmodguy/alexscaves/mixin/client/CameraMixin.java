package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.server.entity.util.MagnetUtil;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {

    // move() and getMaxZoom() are reached through CameraAccessor rather than shadowed: 1.21 retyped
    // both to float, and the access transformer that used to widen them could not follow.
    @Shadow
    protected abstract void setPosition(Vec3 vec3);

    @Shadow
    protected abstract void setRotation(float p_90573_, float p_90574_);

    @Shadow
    private float yRot;

    @Shadow
    private float xRot;

    @Shadow
    private boolean initialized;

    // The three values 26 stopped passing in — see the note on the injection below. All three are
    // fields the whole way back, so only the gate keeps them off the older nodes, where they would
    // be shadows nothing reads.
    //? if >=26 {
    /*@Shadow
    private Entity entity;

    @Shadow
    private boolean detached;
    *///?}

    // 1.21.11 narrowed setup's first parameter from BlockGetter to Level — the camera reads the
    // level's environment attributes now, which a bare BlockGetter cannot answer. Nothing here
    // touches that argument, so only the descriptor and the signature move.
    //
    // 26 deletes setup outright. Camera#update(DeltaTracker) took over its outer half — the fov,
    // the frustum and the projection, none of which existed on setup — and the half this injection
    // cares about, placing and orienting the camera on its entity, is the private
    // alignWithEntity(float) that update calls first. Injecting there rather than at update's TAIL
    // matters: update goes on to build the cull frustum from the position, so a position written
    // after it would be one frame stale in the culling. The three arguments that vanished are all
    // still readable — the entity and the detached flag as fields, mirrored off the camera type,
    // which is where setup's caller read it from anyway.
    //? if >=26 {
    /*@Inject(
            method = {"Lnet/minecraft/client/Camera;alignWithEntity(F)V"},
            remap = true,
            at = @At(value = "TAIL")
    )
    public void ac_onSyncedDataUpdated(float partialTicks, CallbackInfo ci) {
        Entity entity = this.entity;
        boolean detatched = this.detached;
        boolean mirrored = Minecraft.getInstance().options.getCameraType().isMirrored();
    *///?} elif >=1.21.11 {
    /*@Inject(
            method = {"Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V"},
            remap = true,
            at = @At(value = "TAIL")
    )
    public void ac_onSyncedDataUpdated(net.minecraft.world.level.Level level, Entity entity, boolean detatched, boolean mirrored, float partialTicks, CallbackInfo ci) {
    *///?} else {
    @Inject(
            method = {"Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V"},
            remap = true,
            at = @At(value = "TAIL")
    )
    public void ac_onSyncedDataUpdated(BlockGetter level, Entity entity, boolean detatched, boolean mirrored, float partialTicks, CallbackInfo ci) {
    //?}
        Direction dir = MagnetUtil.getEntityMagneticDirection(entity);
        if (dir != Direction.DOWN && dir != Direction.UP) {
            this.setPosition(MagnetUtil.getEyePositionForAttachment(entity, dir, partialTicks));
            if (detatched) {
                if (mirrored) {
                    this.setRotation(this.yRot + 180.0F, -this.xRot);
                }
                Camera self = (Camera) (Object) this;
                ACClientCompat.cameraMove(self, -ACClientCompat.cameraMaxZoom(self, 4.0D), 0.0D, 0.0D);
            }
        }
    }

    @Inject(
            method = {"Lnet/minecraft/client/Camera;getFluidInCamera()Lnet/minecraft/world/level/material/FogType;"},
            remap = true,
            cancellable = true,
            at = @At(value = "HEAD")
    )
    public void ac_getFluidInCamera(CallbackInfoReturnable<FogType> cir) {
        if (initialized && Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasEffect(ACCompat.effect(ACEffectRegistry.BUBBLED.get())) && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            cir.setReturnValue(FogType.WATER);
        }
    }
}
