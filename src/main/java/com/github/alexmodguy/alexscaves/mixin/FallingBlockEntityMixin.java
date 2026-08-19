package com.github.alexmodguy.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.entity.util.ACSyncedData;
import com.github.alexmodguy.alexscaves.server.entity.util.FallingBlockEntityAccessor;
import com.github.alexmodguy.alexscaves.server.entity.util.MagnetUtil;
import com.github.alexmodguy.alexscaves.citadel.CitadelConstants;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin extends Entity implements FallingBlockEntityAccessor {

    @Shadow
    public int time;
    @Shadow private BlockState blockState;
    // Defined here so the defineId call runs in FallingBlockEntity's own class initialiser, but
    // stored on ACSyncedData rather than in a field merged into FallingBlockEntity — NeoForge 21.8
    // scans the holder class for @MixinMerged EntityDataAccessor fields and throws in dev when it
    // finds any. See ACSyncedData's class notes. Hence the boolean field: it holds the call without
    // being an accessor itself.
    private static final boolean FALL_BLOCK_TIME_INSTALLED = ACSyncedData.installFallBlockTime(
            SynchedEntityData.defineId(FallingBlockEntity.class, EntityDataSerializers.INT));

    public FallingBlockEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    // See LivingEntityMixin: 1.20.5 added the SynchedEntityData.Builder parameter.
    //? if >=1.20.5 {
    /*@Inject(at = @At("TAIL"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/world/entity/item/FallingBlockEntity;defineSynchedData(Lnet/minecraft/network/syncher/SynchedEntityData$Builder;)V")
    private void citadel_registerData(net.minecraft.network.syncher.SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(ACSyncedData.FALL_BLOCK_TIME, 0);
    }
    *///?} else {
    @Inject(at = @At("TAIL"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/world/entity/item/FallingBlockEntity;defineSynchedData()V")
    private void citadel_registerData(CallbackInfo ci) {
        entityData.define(ACSyncedData.FALL_BLOCK_TIME, 0);
    }
    //?}

    @Inject(
            method = {"Lnet/minecraft/world/entity/item/FallingBlockEntity;tick()V"},
            remap = true,
            at = @At(value = "TAIL")
    )
    public void ac_tick(CallbackInfo ci) {
        if (!this.isNoGravity() && hasFallBlocking()) {
            time = 10;
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.04D, 0.0D));
        }
        int fallBlockTime = entityData.get(ACSyncedData.FALL_BLOCK_TIME);
        if (fallBlockTime > 0) {
            entityData.set(ACSyncedData.FALL_BLOCK_TIME, fallBlockTime - 1);
        }
        if (MagnetUtil.isPulledByMagnets(this)) {
            MagnetUtil.tickMagnetism(this);
            if (MagnetUtil.getEntityMagneticDelta(this) != Vec3.ZERO) {
                this.setFallBlockingTime();
            }
        }
    }

    public boolean hasFallBlocking() {
        return entityData.get(ACSyncedData.FALL_BLOCK_TIME) > 0;
    }

    public void setFallBlockingTime() {
        entityData.set(ACSyncedData.FALL_BLOCK_TIME, 10);
    }

    public void setBlockState(BlockState blockStateIn) {
        this.blockState = blockStateIn;
    }
}
