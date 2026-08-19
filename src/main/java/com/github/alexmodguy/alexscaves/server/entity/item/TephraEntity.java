package com.github.alexmodguy.alexscaves.server.entity.item;

import com.github.alexmodguy.alexscaves.server.entity.ACEntityDataRegistry;

import com.github.alexmodguy.alexscaves.client.particle.ACParticleRegistry;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.util.TephraExplosion;
import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;
import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

public class TephraEntity extends Projectile {

    private static final EntityDataAccessor<Optional<UUID>> ARC_TOWARDS_ENTITY_UUID = SynchedEntityData.defineId(TephraEntity.class, ACEntityDataRegistry.OPTIONAL_UUID.get());
    private static final EntityDataAccessor<Float> MAX_SCALE = SynchedEntityData.defineId(TephraEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(TephraEntity.class, EntityDataSerializers.FLOAT);
    private int lSteps;
    private double lx;
    private double ly;
    private double lz;
    private double lyr;
    private double lxr;
    private double lxd;
    private double lyd;
    private double lzd;
    private float prevScale;
    private boolean playedSpawnSound = false;

    private int dieIn = -1;

    private int clipFor = 5;

    public TephraEntity(EntityType entityType, Level level) {
        super(entityType, level);
    }


    public TephraEntity(Level level, LivingEntity shooter) {
        this(ACEntityRegistry.TEPHRA.get(), level);
        float f = shooter instanceof Player ? 0.3F : 0.1F;
        this.setPos(shooter.getX(), shooter.getEyeY() - f, shooter.getZ());
        this.setOwner(shooter);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return ACPlatform.getEntitySpawningPacket(this);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ARC_TOWARDS_ENTITY_UUID, Optional.empty());
        this.entityData.define(MAX_SCALE, 1.0F);
        this.entityData.define(SCALE, 0.1F);
    }

    public void tick() {
        super.tick();
        prevScale = this.getScale();
        if(!playedSpawnSound){
            playedSpawnSound = true;
            this.playSound(ACSoundRegistry.TEPHRA_WHISTLE.get(), 8.0F, Mth.clamp(2.0F - this.getMaxScale() * 0.5F, 0.5F, 2.0F));
        }
        if (!level().isClientSide()) {
            Entity arcTowards = getArcingTowards();
            if (arcTowards != null && tickCount > 3 && dieIn == -1 && this.distanceTo(arcTowards) > 1.5F && tickCount < 20) {
                Vec3 arcVec = arcTowards.position().add(0, 0.3 * arcTowards.getBbHeight(), 0).subtract(this.position()).normalize();
                this.setDeltaMovement(this.getDeltaMovement().add(arcVec.scale(0.1F)));
            }
            this.setScale(Mth.approach(this.getScale(), this.getMaxScale(), 0.1F));
        } else {
            for (int j = 0; j < 1 + random.nextInt(2); ++j) {
                Vec3 delta = new Vec3(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F).scale(0.025F);
                this.level().addAlwaysVisibleParticle(ACParticleRegistry.TEPHRA.get(), true, this.getRandomX(this.getScale()) + this.getDeltaMovement().x, this.getRandomY() + this.getDeltaMovement().y, this.getRandomZ(this.getScale()) + this.getDeltaMovement().z, delta.x, delta.y, delta.z);
            }
        }
        Vec3 vec3 = this.getDeltaMovement();
        double d0 = this.getX() + vec3.x;
        double d1 = this.getY() + vec3.y;
        double d2 = this.getZ() + vec3.z;
        this.updateRotation();
        if (this.level().getBlockStates(this.getBoundingBox()).noneMatch(BlockBehaviour.BlockStateBase::isAir) && !ACCompat.isInWaterOrBubble(this)) {
            this.discard();
        } else {
            this.setDeltaMovement(vec3.scale((double) 0.9F));
            if (!this.isNoGravity()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, (double) -0.1F, 0.0D));
            }
            if (this.level().isClientSide()) {
                if (this.lSteps > 0) {
                    double d5 = this.getX() + (this.lx - this.getX()) / (double) this.lSteps;
                    double d6 = this.getY() + (this.ly - this.getY()) / (double) this.lSteps;
                    double d7 = this.getZ() + (this.lz - this.getZ()) / (double) this.lSteps;
                    this.setYRot(Mth.wrapDegrees((float) this.lyr));
                    this.setXRot(this.getXRot() + (float) (this.lxr - (double) this.getXRot()) / (float) this.lSteps);
                    --this.lSteps;
                    this.setPos(d5, d6, d7);
                } else {
                    this.reapplyPosition();
                }
            } else {
                this.setPos(d0, d1, d2);
            }
        }

        HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitresult.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hitresult)) {
            this.onHit(hitresult);
        }
        if (dieIn > 0) {
            dieIn--;
            if (dieIn == 0) {
                this.discard();
            }
        }
        if(clipFor > 0){
            clipFor--;
            this.noPhysics = true;
        }else{
            this.noPhysics = false;
        }
    }

    // 1.20.2 dropped lerpTo's trailing boolean (it selected "teleport" behaviour that vanilla
    // no longer distinguishes); none of these overrides read it. 1.21.5 deleted lerpTo outright —
    // the client hands the target to the InterpolationHandler returned by getInterpolation(), so the
    // same capture happens in a handler subclass and this entity's own tick lerps exactly as before.
    //? if >=1.21.5 {
    /*private net.minecraft.world.entity.InterpolationHandler acInterpolation;

    @Override
    public net.minecraft.world.entity.InterpolationHandler getInterpolation() {
        if (this.acInterpolation == null) {
            this.acInterpolation = new net.minecraft.world.entity.InterpolationHandler(this) {
                @Override
                public void interpolateTo(net.minecraft.world.phys.Vec3 pos, float yr, float xr) {
                    lx = pos.x();
                    ly = pos.y();
                    lz = pos.z();
                    lyr = yr;
                    lxr = xr;
                    lSteps = net.minecraft.world.entity.InterpolationHandler.DEFAULT_INTERPOLATION_STEPS;
                    setDeltaMovement(lxd, lyd, lzd);
                }
            };
        }
        return this.acInterpolation;
    }
    *///?} elif >=1.20.2 {
    /*@Override
    public void lerpTo(double x, double y, double z, float yr, float xr, int steps) {
        this.lx = x;
        this.ly = y;
        this.lz = z;
        this.lyr = yr;
        this.lxr = xr;
        this.lSteps = steps;
        this.setDeltaMovement(this.lxd, this.lyd, this.lzd);
    }
    *///?} else {
    @Override
    public void lerpTo(double x, double y, double z, float yr, float xr, int steps, boolean b) {
        this.lx = x;
        this.ly = y;
        this.lz = z;
        this.lyr = yr;
        this.lxr = xr;
        this.lSteps = steps;
        this.setDeltaMovement(this.lxd, this.lyd, this.lzd);
    }
    //?}

    @Override
    public void lerpMotion(double lerpX, double lerpY, double lerpZ) {
        this.lxd = lerpX;
        this.lyd = lerpY;
        this.lzd = lerpZ;
        this.setDeltaMovement(this.lxd, this.lyd, this.lzd);
    }


    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (!this.level().isClientSide() && !ownedBy(hitResult.getEntity()) && !this.noPhysics) {
            explode();
        }
    }

    private void explode() {
        Explosion.BlockInteraction blockinteraction = com.github.alexmodguy.alexscaves.server.misc.ACPlatform.canEntityGrief(level(), this) ? ACCompat.gameRule(level(), GameRules.RULE_MOB_EXPLOSION_DROP_DECAY, true) ? Explosion.BlockInteraction.DESTROY_WITH_DECAY : Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP;
        TephraExplosion explosion = new TephraExplosion(level(), this, this.getX(), this.getY(0.5), this.getZ(), 1.0F + getMaxScale(), blockinteraction);
        explosion.explode();
        explosion.finalizeExplosion(true);
        this.discard();
    }

    public float getMaxScale() {
        return this.entityData.get(MAX_SCALE);
    }

    public void setMaxScale(float scale) {
        this.entityData.set(MAX_SCALE, scale);
    }

    public float getScale() {
        return this.entityData.get(SCALE);
    }

    public void setScale(float scale) {
        this.entityData.set(SCALE, scale);
    }

    public float getLerpedScale(float partialTicks) {
        return prevScale + (getScale() - prevScale) * partialTicks;
    }

    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (!this.level().isClientSide() && !this.noPhysics) {
            explode();
        }
    }

    public Entity getArcingTowards() {
        UUID id = this.entityData.get(ARC_TOWARDS_ENTITY_UUID).orElse(null);
        return id == null ? null : ((ServerLevel) level()).getEntity(id);
    }

    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setScale(ACCompat.getFloat(tag, "Scale"));
        this.setMaxScale(ACCompat.getFloat(tag, "MaxScale"));

    }

    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putFloat("Scale", this.getScale());
        compoundTag.putFloat("MaxScale", this.getMaxScale());
    }

    public void setArcingTowards(@javax.annotation.Nullable UUID arcingTowards) {
        this.entityData.set(ARC_TOWARDS_ENTITY_UUID, Optional.ofNullable(arcingTowards));
    }
}

