package com.github.alexmodguy.alexscaves.server.entity.item;

import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.living.BrainiacEntity;
import com.github.alexmodguy.alexscaves.server.misc.ACDamageTypes;
import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ThrownWasteDrumEntity extends Entity {

    // 1.21.2 made Entity#hurt final and moved the overridable half to an abstract
    // hurtServer(ServerLevel, DamageSource, float), so every direct Entity subclass now has to
    // declare one. This class never overrode hurt, so what it wants back is the behaviour
    // 1.21.1's Entity#hurt gave it for free — which is, verbatim, vanilla's own Projectile#hurtServer.
    //? if >=1.21.2 {
    /*@Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel acServerLevel, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!this.isInvulnerableToBase(source)) {
            this.markHurt();
        }
        return false;
    }
    *///?}

    private static final EntityDataAccessor<Integer> ON_GROUND_FOR = SynchedEntityData.defineId(ThrownWasteDrumEntity.class, EntityDataSerializers.INT);
    public static final int MAX_TIME = 20;

    private BlockPos removeWasteAt = null;

    public ThrownWasteDrumEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }


    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return ACPlatform.getEntitySpawningPacket(this);
    }

    public void tick() {
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.08D, 0.0D));
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        if (this.onGround()) {
            this.setOnGroundFor(this.getOnGroundFor() + 1);
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.7D, -0.7, 0.7D));
        }
        if (this.getDeltaMovement().length() > 0.03F) {
            AABB killBox = this.getBoundingBox();
            boolean b = false;
            for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, killBox)) {
                if (!(entity instanceof BrainiacEntity)) {
                    b = true;
                    entity.hurt(ACDamageTypes.causeAcidDamage(level().registryAccess()), 2);
                }
            }
            if (b) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.2D, 1, 0.2D));
            }
        }
        if (this.getOnGroundFor() >= MAX_TIME && !level().isClientSide()) {
            if (this.getOnGroundFor() == MAX_TIME) {
                BlockPos landed = this.blockPosition();
                while (landed.getY() < level().getMaxBuildHeight() && (!level().getBlockState(landed).isAir() || !level().getBlockState(landed).getFluidState().isEmpty() && !level().getBlockState(landed).getFluidState().is(ACTagRegistry.ACID))) {
                    landed = landed.above();
                }
                removeWasteAt = landed;
                if (level().getBlockState(removeWasteAt).isAir()) {
                    BlockState fluid = ACBlockRegistry.ACID.get().defaultBlockState();
                    level().setBlockAndUpdate(removeWasteAt, fluid);
                }
            }
            if (this.getOnGroundFor() >= MAX_TIME + 15 && removeWasteAt != null) {
                this.remove(RemovalReason.DISCARDED);
                if (level().getFluidState(removeWasteAt).is(ACTagRegistry.ACID)) {
                    level().setBlockAndUpdate(removeWasteAt, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ON_GROUND_FOR, 0);
    }

    public int getOnGroundFor() {
        return this.entityData.get(ON_GROUND_FOR);
    }

    public void setOnGroundFor(int time) {
        this.entityData.set(ON_GROUND_FOR, time);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {

    }
}
