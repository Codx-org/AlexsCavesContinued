package com.github.alexmodguy.alexscaves.mixin.citadel;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.citadel.CitadelConstants;
import com.github.alexmodguy.alexscaves.citadel.server.entity.CitadelSyncedData;
import com.github.alexmodguy.alexscaves.citadel.server.entity.ICitadelDataEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements ICitadelDataEntity {

    // Defined here so the defineId call runs in LivingEntity's own class initialiser, but stored on
    // CitadelSyncedData rather than in a field merged into LivingEntity — NeoForge 21.8 scans the
    // holder class for @MixinMerged EntityDataAccessor fields and throws in dev when it finds any.
    // See CitadelSyncedData's class notes. Hence the boolean field: it holds the call without being
    // an accessor itself.
    private static final boolean CITADEL_DATA_INSTALLED = CitadelSyncedData.installCitadelData(
            SynchedEntityData.defineId(LivingEntity.class, com.github.alexmodguy.alexscaves.server.misc.ACDataSerializers.COMPOUND_TAG));

    protected LivingEntityMixin(EntityType<? extends Entity> entityType, Level world) {
        super(entityType, world);
    }

    // 1.20.5 gave defineSynchedData a SynchedEntityData.Builder parameter and made the built
    // container immutable, so both the target descriptor and the body change. LivingEntity's
    // override is concrete on both eras, so a plain TAIL inject still works — unlike Entity's,
    // which is abstract (see EntityMixin).
    //? if >=1.20.5 {
    /*@Inject(at = @At("TAIL"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/world/entity/LivingEntity;defineSynchedData(Lnet/minecraft/network/syncher/SynchedEntityData$Builder;)V")
    private void citadel_registerData(net.minecraft.network.syncher.SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(CitadelSyncedData.CITADEL_DATA, new CompoundTag());
    }
    *///?} else {
    @Inject(at = @At("TAIL"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/world/entity/LivingEntity;defineSynchedData()V")
    private void citadel_registerData(CallbackInfo ci) {
        entityData.define(CitadelSyncedData.CITADEL_DATA, new CompoundTag());
    }
    //?}

    // 1.21.6 replaced the CompoundTag on both save signatures with ValueOutput/ValueInput. The
    // ~150 overrides in the mod itself are rewritten by the !mc216-entity-{save,read}sig rules in
    // stonecutter.gradle.kts, but those anchor on a `void …(CompoundTag <name>) {` declaration and
    // a mixin's target is a descriptor string in an annotation, so this one is gated by hand. The
    // bodies are the same either way — ACCompat.tagOf hands back the CompoundTag underneath, which
    // is the same zero-copy bridge those rules generate.
    //? if >=1.21.6 {
    /*@Inject(at = @At("TAIL"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/world/entity/LivingEntity;addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V")
    private void citadel_writeAdditional(net.minecraft.world.level.storage.ValueOutput output, CallbackInfo ci) {
        CompoundTag citadelDat = getCitadelEntityData();
        if (citadelDat != null) {
            ACCompat.tagOf(output).put("CitadelData", citadelDat);
        }
    }

    @Inject(at = @At("TAIL"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/world/entity/LivingEntity;readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V")
    private void citadel_readAdditional(net.minecraft.world.level.storage.ValueInput input, CallbackInfo ci) {
        CompoundTag compoundNBT = ACCompat.tagOf(input);
        if (compoundNBT.contains("CitadelData")) {
            setCitadelEntityData(ACCompat.getCompound(compoundNBT, "CitadelData"));
        }
    }
    *///?} else {
    @Inject(at = @At("TAIL"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/world/entity/LivingEntity;addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V")
    private void citadel_writeAdditional(CompoundTag compoundNBT, CallbackInfo ci) {
        CompoundTag citadelDat = getCitadelEntityData();
        if (citadelDat != null) {
            compoundNBT.put("CitadelData", citadelDat);
        }
    }

    @Inject(at = @At("TAIL"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/world/entity/LivingEntity;readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V")
    private void citadel_readAdditional(CompoundTag compoundNBT, CallbackInfo ci) {
        if (compoundNBT.contains("CitadelData")) {
            setCitadelEntityData(ACCompat.getCompound(compoundNBT, "CitadelData"));
        }
    }
    //?}

    public CompoundTag getCitadelEntityData() {
        return entityData.get(CitadelSyncedData.CITADEL_DATA);
    }

    public void setCitadelEntityData(CompoundTag nbt) {
        entityData.set(CitadelSyncedData.CITADEL_DATA, nbt);
    }
}
