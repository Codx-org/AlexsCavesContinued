package com.github.alexmodguy.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.server.entity.util.MagnetUtil;
import com.github.alexmodguy.alexscaves.server.entity.util.MagneticEntityAccessor;
import com.github.alexmodguy.alexscaves.server.item.ACItemRegistry;
import com.github.alexmodguy.alexscaves.server.item.RainbounceBootsItem;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import com.github.alexmodguy.alexscaves.citadel.CitadelConstants;
import com.github.alexmodguy.alexscaves.citadel.server.entity.CitadelSyncedData;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin implements MagneticEntityAccessor {

    @Shadow
    @Final
    protected SynchedEntityData entityData;

    @Shadow
    protected abstract void playStepSound(BlockPos p_20135_, BlockState p_20136_);

    @Shadow
    private Level level;
    @Shadow
    private Vec3 position;

    @Shadow
    private EntityDimensions dimensions;

    @Shadow
    public abstract void tick();

    @Shadow
    public abstract void refreshDimensions();

    // Only the >=1.20.5 arm of ac_getEyeHeight needs this — that version's getEyeHeight no longer
    // receives the dimensions — but the method has existed unchanged since well before 1.20.1, so
    // the shadow is left ungated rather than duplicated.
    @Shadow
    public abstract EntityDimensions getDimensions(Pose pose);

    @Shadow
    protected boolean wasTouchingWater;

    @Shadow
    public abstract AABB getBoundingBox();

    @Shadow
    public abstract Level level();

    @Shadow
    public abstract boolean onGround();

    @Shadow
    public abstract double getY();

    // The four magnet accessors are DEFINED here — so the defineId calls run inside Entity's own
    // class initialiser and take Entity's slots in the id pool, exactly as before — but they are
    // STORED on CitadelSyncedData rather than in fields merged into Entity. NeoForge 21.8 scans the
    // holder class for @MixinMerged EntityDataAccessor fields and throws in dev when it finds any;
    // see CitadelSyncedData's class notes for the whole story. Hence the boolean field: it holds the
    // call without being an accessor itself.
    private static final boolean AC_MAGNET_DATA_INSTALLED = CitadelSyncedData.installMagnet(
            SynchedEntityData.defineId(Entity.class, EntityDataSerializers.FLOAT),
            SynchedEntityData.defineId(Entity.class, EntityDataSerializers.FLOAT),
            SynchedEntityData.defineId(Entity.class, EntityDataSerializers.FLOAT),
            SynchedEntityData.defineId(Entity.class, EntityDataSerializers.DIRECTION));
    private float attachChangeProgress = 0F;
    private float prevAttachChangeProgress = 0F;
    private Direction prevAttachDir = Direction.DOWN;
    private int jumpFlipCooldown = 0;

    private BlockPos lastStepPos;
    private Vec3 lastBouncePos;

    // Citadel registers this entity data at the TAIL of Entity's constructor because
    // Entity#defineSynchedData is abstract and cannot be injected into. 1.20.5 kept it abstract
    // but made SynchedEntityData immutable once built, so the TAIL of the constructor is too
    // late — there is no `define` to call any more. The last point at which the four accessors
    // can still be added is the `builder.build()` call the constructor ends with (offset 384 in
    // the 1.20.6 bytecode, long after the delegate super() call), so that is what this replaces.
    //
    // It is a @Redirect and not an @Inject-with-@Local, which is what this used to be: Mixin only
    // allows @Inject into a constructor at RETURN/TAIL unless the bundled Mixin is new enough to
    // support arbitrary constructor injection. NeoForge's is; Forge's is not, and every Forge node
    // from 1.20.6 up died at startup with "@At("INVOKE") selector Found @Inject targetting a
    // constructor". @Redirect carries no such restriction once the node is past the delegate call,
    // so this shape works on all three loaders. Fully-qualified so the gated-out branch needs no
    // import.
    //? if >=1.20.5 {
    /*@org.spongepowered.asm.mixin.injection.Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/network/syncher/SynchedEntityData$Builder;build()Lnet/minecraft/network/syncher/SynchedEntityData;"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/world/entity/Entity;<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V")
    private net.minecraft.network.syncher.SynchedEntityData citadel_registerData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(CitadelSyncedData.MAGNET_DELTA_X, 0F);
        builder.define(CitadelSyncedData.MAGNET_DELTA_Y, 0F);
        builder.define(CitadelSyncedData.MAGNET_DELTA_Z, 0F);
        builder.define(CitadelSyncedData.MAGNET_ATTACHMENT_DIRECTION, Direction.DOWN);
        return builder.build();
    }
    *///?} else {
    @Inject(at = @At("TAIL"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/world/entity/Entity;<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V")
    private void citadel_registerData(CallbackInfo ci) {
        entityData.define(CitadelSyncedData.MAGNET_DELTA_X, 0F);
        entityData.define(CitadelSyncedData.MAGNET_DELTA_Y, 0F);
        entityData.define(CitadelSyncedData.MAGNET_DELTA_Z, 0F);
        entityData.define(CitadelSyncedData.MAGNET_ATTACHMENT_DIRECTION, Direction.DOWN);
    }
    //?}


    @Inject(
            method = {"Lnet/minecraft/world/entity/Entity;tick()V"},
            remap = true,
            at = @At(value = "TAIL")
    )
    public void ac_tick(CallbackInfo ci) {
        Entity thisEntity = (Entity) (Object) this;
        prevAttachChangeProgress = attachChangeProgress;
        if (this.prevAttachDir != this.getMagneticAttachmentFace()) {
            if (attachChangeProgress < 1.0F) {
                attachChangeProgress += 0.1F;
            } else if (attachChangeProgress >= 1.0F) {
                this.prevAttachDir = this.getMagneticAttachmentFace();
            }
        } else {
            this.attachChangeProgress = 1.0F;
        }

        if (MagnetUtil.isPulledByMagnets(thisEntity)) {
            MagnetUtil.tickMagnetism(thisEntity);
            if (this.jumpFlipCooldown > 0) {
                this.jumpFlipCooldown--;
            }
        } else {
            if (this.getMagneticAttachmentFace() != Direction.DOWN) {
                this.setMagneticAttachmentFace(Direction.DOWN);
                this.refreshDimensions();
            }
        }
    }

    @Inject(
            method = {"Lnet/minecraft/world/entity/Entity;onSyncedDataUpdated(Lnet/minecraft/network/syncher/EntityDataAccessor;)V"},
            remap = true,
            at = @At(value = "TAIL")
    )
    public void ac_onSyncedDataUpdated(EntityDataAccessor<?> entityDataAccessor, CallbackInfo ci) {
        if (CitadelSyncedData.MAGNET_ATTACHMENT_DIRECTION.equals(entityDataAccessor)) {
            this.prevAttachChangeProgress = 0.0F;
            this.attachChangeProgress = 0.0F;
        }
    }

    @Inject(
            method = {"Lnet/minecraft/world/entity/Entity;getEyePosition()Lnet/minecraft/world/phys/Vec3;"},
            remap = true,
            cancellable = true,
            at = @At(value = "HEAD")
    )
    public void ac_getEyePosition(CallbackInfoReturnable<Vec3> cir) {
        if (getMagneticAttachmentFace() != Direction.DOWN) {
            cir.setReturnValue(MagnetUtil.getEyePositionForAttachment((Entity) (Object) this, getMagneticAttachmentFace(), 1.0F));
        }
    }

    @Inject(
            method = {"Lnet/minecraft/world/entity/Entity;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;"},
            remap = true,
            cancellable = true,
            at = @At(value = "HEAD")
    )
    public void ac_getEyePosition_lerp(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (getMagneticAttachmentFace() != Direction.DOWN && getMagneticAttachmentFace() != Direction.UP) {
            cir.setReturnValue(MagnetUtil.getEyePositionForAttachment((Entity) (Object) this, getMagneticAttachmentFace(), partialTick));
        }
    }

    @Inject(
            method = {"Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"},
            remap = true,
            cancellable = true,
            at = @At(value = "HEAD")
    )
    //must override entire method for compatibility with Radium mod
    public void ac_collide(Vec3 deltaIn, CallbackInfoReturnable<Vec3> cir) {

        AABB aabb = this.getBoundingBox();
        Entity thisEntity = (Entity) (Object) this;
        //AC CODE START
        List<VoxelShape> list;
        //fix infinity voxel collection crash for ItemEntity
        if (this.getY() > this.level().getMinBuildHeight() - 200) {
            list = this.level().getEntityCollisions(thisEntity, aabb.expandTowards(deltaIn));
            List<VoxelShape> list2 = MagnetUtil.getMovingBlockCollisions(thisEntity, aabb);
            list = ImmutableList.<VoxelShape>builder().addAll(list).addAll(list2).build();
        } else {
            list = List.of();
        }
        //AC CODE END
        Vec3 vec3 = deltaIn.lengthSqr() == 0.0D ? deltaIn : Entity.collideBoundingBox(thisEntity, deltaIn, aabb, this.level(), list);
        boolean flag = deltaIn.x != vec3.x;
        boolean flag1 = deltaIn.y != vec3.y;
        boolean flag2 = deltaIn.z != vec3.z;
        boolean flag3 = this.onGround() || flag1 && deltaIn.y < 0.0D;
        float stepHeight = thisEntity.getStepHeight();
        if (stepHeight > 0.0F && flag3 && (flag || flag2)) {
            Vec3 vec31 = Entity.collideBoundingBox(thisEntity, new Vec3(deltaIn.x, stepHeight, deltaIn.z), aabb, this.level, list);
            Vec3 vec32 = Entity.collideBoundingBox(thisEntity, new Vec3(0.0D, stepHeight, 0.0D), aabb.expandTowards(deltaIn.x, 0.0D, deltaIn.z), this.level, list);
            if (vec32.y < (double) stepHeight) {
                Vec3 vec33 = Entity.collideBoundingBox(thisEntity, new Vec3(deltaIn.x, 0.0D, deltaIn.z), aabb.move(vec32), this.level(), list).add(vec32);
                if (vec33.horizontalDistanceSqr() > vec31.horizontalDistanceSqr()) {
                    vec31 = vec33;
                }
            }

            if (vec31.horizontalDistanceSqr() > vec3.horizontalDistanceSqr()) {
                cir.setReturnValue(vec31.add(Entity.collideBoundingBox(thisEntity, new Vec3(0.0D, -vec31.y + deltaIn.y, 0.0D), aabb.move(vec31), this.level(), list)));
                return;
            }
        }

        cir.setReturnValue(vec3);
    }

    @Inject(
            method = {"Lnet/minecraft/world/entity/Entity;turn(DD)V"},
            remap = true,
            cancellable = true,
            at = @At(value = "HEAD")
    )
    public void ac_turn(double yBy, double xBy, CallbackInfo ci) {
        if (getMagneticAttachmentFace() != Direction.DOWN) {
            ci.cancel();
            MagnetUtil.turnEntityOnMagnet((Entity) (Object) this, xBy, yBy, getMagneticAttachmentFace());
        }
    }

    @Inject(
            method = {"Lnet/minecraft/world/entity/Entity;makeBoundingBox()Lnet/minecraft/world/phys/AABB;"},
            remap = true,
            cancellable = true,
            at = @At(value = "HEAD")
    )
    public void ac_makeBoundingBox(CallbackInfoReturnable<AABB> cir) {
        if (this.entityData.isDirty() && getMagneticAttachmentFace() != Direction.DOWN) {
            cir.setReturnValue(MagnetUtil.rotateBoundingBox(dimensions, getMagneticAttachmentFace(), position));
        }
    }

    @Inject(
            method = {"Lnet/minecraft/world/entity/Entity;isInWater()Z"},
            remap = true,
            cancellable = true,
            at = @At(value = "HEAD")
    )
    public void ac_isInWater(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LivingEntity living && living.getActiveEffectsMap() != null && living.hasEffect(ACCompat.effect(ACEffectRegistry.BUBBLED.get())) && (living.canBreatheUnderwater() || ACCompat.isAquatic(living)) && !living.getType().builtInRegistryHolder().is(ACTagRegistry.RESISTS_BUBBLED)) {
            cir.setReturnValue(true);
        }
    }

    // The rainbounce boots' bounce hangs off "this move ended by landing on something", and until 26.2
    // that moment was a call out to the block: Entity#move asked the state's Block to update the entity
    // after a fall onto it, guarded by "the y this step asked for is not the y it got". (The hook's own
    // name is deliberately not spelled here — a replacement rule rewrites it, in prose as readily as in
    // a target string.)
    //
    // 26.2 deleted that hook (from SlimeBlock and BedBlock too — see SundropBlock) and folded the work
    // back into the entity, as a private restituteMovementAfterCollisions(BlockState, boolean, boolean,
    // Vec3) called from the same place in move(). Two things follow. (1) NeoForge patches an extra
    // leading BlockPos onto it and calls THAT overload from move(), leaving the vanilla-shaped one
    // behind as a delegate nothing invokes — so the anchor is per loader, since @At matches the call
    // site's descriptor. (2) The new guard is `canSimulateMovement() && ((flag && verticalCollision) ||
    // horizontalCollision)`, i.e. it also fires on a purely horizontal collision, which the old one
    // never did; verticalCollision is the same statement about the same step that "asked-for y is not
    // the y it got" was, so the missing half of the guard moves into the handler.
    @Inject(
            method = {"Lnet/minecraft/world/entity/Entity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"},
            remap = true,
            cancellable = true,
            //? if neoforge && >=26.2 {
            /*at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;restituteMovementAfterCollisions(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;ZZLnet/minecraft/world/phys/Vec3;)V",
                    shift = At.Shift.AFTER
            )}
            *///?} elif >=26.2 {
            /*at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;restituteMovementAfterCollisions(Lnet/minecraft/world/level/block/state/BlockState;ZZLnet/minecraft/world/phys/Vec3;)V",
                    shift = At.Shift.AFTER
            )}
            *///?} else {
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;updateEntityAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V",
                    shift = At.Shift.AFTER
            )}
            //?}
    )
    public void ac_move(MoverType moverType, Vec3 vec3, CallbackInfo ci) {
        //? if >=26.2 {
        /*if (!((Entity) (Object) this).verticalCollision) {
            return;
        }
        *///?}
        if ((Object) this instanceof LivingEntity living && living.getItemBySlot(EquipmentSlot.FEET).is(ACItemRegistry.RAINBOUNCE_BOOTS.get())) {
            RainbounceBootsItem.onEntityLand(living, vec3);
        }
    }

    // Citadel guards every magnet-data access, because on 1.20.1 the accessors are registered from
    // a constructor TAIL inject that a subclass could conceivably outrun. 1.20.5 deleted
    // SynchedEntityData#hasItem outright — the container is now a fixed-size array built in one
    // shot — and the question no longer arises: registration happens inside Entity's constructor,
    // before the container exists, so if an entity exists at all its data is there.
    private boolean ac_hasMagnetData(EntityDataAccessor<?> accessor) {
        //? if >=1.20.5 {
        /*return true;
        *///?} else {
        return entityData.hasItem(accessor);
        //?}
    }

    @Override
    public float getMagneticDeltaX() {
        return ac_hasMagnetData(CitadelSyncedData.MAGNET_DELTA_X) ? entityData.get(CitadelSyncedData.MAGNET_DELTA_X) : 0.0F;
    }

    @Override
    public float getMagneticDeltaY() {
        return ac_hasMagnetData(CitadelSyncedData.MAGNET_DELTA_Y) ? entityData.get(CitadelSyncedData.MAGNET_DELTA_Y) : 0.0F;
    }

    @Override
    public float getMagneticDeltaZ() {
        return ac_hasMagnetData(CitadelSyncedData.MAGNET_DELTA_Z) ? entityData.get(CitadelSyncedData.MAGNET_DELTA_Z) : 0.0F;
    }

    // Upstream did this from Forge's EntityEvent.Size, which Forge deleted in 1.20.2. This is the
    // method that event existed to adjust, so the magnet cases are reimplemented here on every
    // version and loader instead. A magnetised entity is stuck to some face of a block: on the
    // ceiling its eyes are the same distance from the (now upper) feet, and on a wall it is lying
    // sideways, so there is no vertical eye offset at all.
    //
    // 1.20.5 moved the eye height onto EntityDimensions itself, so the dimensions stopped being a
    // parameter: getEyeHeight(Pose) now just reads it off getDimensions(pose). The newer arm asks
    // for those dimensions itself so the body below is shared. Injecting into the Pose overload
    // rather than the no-arg one covers both, because the no-arg one delegates to it.
    @Inject(
            //? if >=1.20.5 {
            /*method = {"Lnet/minecraft/world/entity/Entity;getEyeHeight(Lnet/minecraft/world/entity/Pose;)F"},
            *///?} else {
            method = {"Lnet/minecraft/world/entity/Entity;getEyeHeight(Lnet/minecraft/world/entity/Pose;Lnet/minecraft/world/entity/EntityDimensions;)F"},
            //?}
            remap = true,
            at = @At(value = "RETURN"),
            cancellable = true
    )
    //? if >=1.20.5 {
    /*public void ac_getEyeHeight(Pose pose, CallbackInfoReturnable<Float> cir) {
        EntityDimensions dimensions = this.getDimensions(pose);
    *///?} else {
    public void ac_getEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
    //?}
        Direction dir = this.getMagneticAttachmentFace();
        if (dir == Direction.UP) {
            cir.setReturnValue(ACCompat.height(dimensions) - cir.getReturnValueF());
        } else if (dir.getAxis() != Direction.Axis.Y) {
            cir.setReturnValue(0.0F);
        }
    }

    @Override
    public Direction getMagneticAttachmentFace() {
        return ac_hasMagnetData(CitadelSyncedData.MAGNET_ATTACHMENT_DIRECTION) ? entityData.get(CitadelSyncedData.MAGNET_ATTACHMENT_DIRECTION) : Direction.DOWN;
    }

    @Override
    public Direction getPrevMagneticAttachmentFace() {
        return prevAttachDir;
    }

    @Override
    public float getAttachmentProgress(float partialTicks) {
        return prevAttachChangeProgress + (attachChangeProgress - prevAttachChangeProgress) * partialTicks;
    }

    @Override
    public void setMagneticDeltaX(float f) {
        if (ac_hasMagnetData(CitadelSyncedData.MAGNET_DELTA_X)) {
            entityData.set(CitadelSyncedData.MAGNET_DELTA_X, f);
        }
    }

    @Override
    public void setMagneticDeltaY(float f) {
        if (ac_hasMagnetData(CitadelSyncedData.MAGNET_DELTA_Y)) {
            entityData.set(CitadelSyncedData.MAGNET_DELTA_Y, f);
        }
    }

    @Override
    public void setMagneticDeltaZ(float f) {
        if (ac_hasMagnetData(CitadelSyncedData.MAGNET_DELTA_Z)) {
            entityData.set(CitadelSyncedData.MAGNET_DELTA_Z, f);
        }
    }

    @Override
    public void setMagneticAttachmentFace(Direction dir) {
        if (ac_hasMagnetData(CitadelSyncedData.MAGNET_ATTACHMENT_DIRECTION)) {
            entityData.set(CitadelSyncedData.MAGNET_ATTACHMENT_DIRECTION, dir);
        }
    }

    @Override
    public void postMagnetJump() {
        this.jumpFlipCooldown = 20;
    }

    @Override
    public boolean canChangeDirection() {
        return jumpFlipCooldown <= 0 && getAttachmentProgress(1.0F) == 1.0F;
    }

    @Override
    public void stepOnMagnetBlock(BlockPos pos) {
        if (lastStepPos == null || lastStepPos.distSqr(pos) > 2) {
            this.lastStepPos = pos;
            this.playStepSound(pos, level.getBlockState(pos));
        }
    }

    // 26 pulled an entity's fluid state into a vanilla EntityFluidInteraction, which tracks height,
    // eye-immersion and flow per TagKey<Fluid> — for exactly the tags it is handed at construction.
    // Entity's field initialiser hands it Set.of(WATER, LAVA), so a modded fluid is invisible to
    // getFluidHeight/isEyeInFluid, and NeoForge 26.1 removed the FluidType-shaped queries that used
    // to answer for one. Widening that one argument restores every question ACFluids asks, on every
    // loader and with no per-entity state of our own; the trackers cost two more entries in a
    // Reference2ObjectArrayMap and one identity compare per distinct fluid in the entity's box.
    //
    // A @ModifyArg rather than an @Inject because the call site is a field initialiser, i.e. inside
    // <init>, and Forge's Mixin refuses an @Inject into a constructor anywhere but RETURN/TAIL —
    // where the object has already been built with the narrow set. Other injector types carry no
    // such restriction (see citadel/EntityMixin's SynchedEntityData$Builder redirect).
    //
    // ⚠️ INVOKE on the constructor, NOT @At("NEW"). "NEW" resolves to the `new` instruction itself,
    // which is a TypeInsnNode carrying no arguments, so @ModifyArg dies at apply time with
    // "targetting a non-method insn" — a hard boot failure that compiles clean and that
    // verify_mixins.py cannot see, since the type the NEW form names does exist. The arguments live
    // on the invokespecial two instructions later (javap: `63: invokespecial … ."<init>":(Ljava/util/Set;)V`),
    // which is what an argument-modifying injector has to anchor on.
    //? if >=26 {
    /*@org.spongepowered.asm.mixin.injection.ModifyArg(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityFluidInteraction;<init>(Ljava/util/Set;)V"))
    private java.util.Set<net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid>> ac_trackModFluids(
            java.util.Set<net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid>> tracked) {
        java.util.Set<net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid>> widened =
                new java.util.LinkedHashSet<>(tracked);
        widened.add(ACTagRegistry.ACID);
        widened.add(ACTagRegistry.PURPLE_SODA);
        return widened;
    }
    *///?}

    // Below 26 there is no EntityFluidInteraction to widen, but the map it replaced is the same idea
    // and just as open: Entity#fluidHeight is keyed by TagKey<Fluid> and filled by public
    // updateFluidHeightAndDoFluidPushing(TagKey, double) calls, so adding this mod's two fluids to it
    // is two more calls rather than a mechanism. Forge and NeoForge never take this arm — they have
    // the FluidType-shaped queries ACFluids uses below 26 — so it is Fabric's 18 sub-26 nodes only.
    //
    // updateInWaterStateAndDoFluidPushing() is the single driver: it clears the map at HEAD, then does
    // water and lava. TAIL is therefore the only correct anchor — HEAD would have our entries cleared
    // out from under us — and the method's name and descriptor are byte-identical on 1.20.1, 1.20.5,
    // 1.21, 1.21.5, 1.21.9 and 1.21.11 (javap'd against the vanilla jar, not the loader-patched one),
    // so one arm covers the whole band. The return value is deliberately not touched: it feeds
    // baseTick's water/lava bookkeeping, which this mod's fluids have never claimed to be.
    //
    // 0.014D is the push scale. That is Forge's FluidType#motionScale default, which neither
    // AcidFluidType nor PurpleSodaFluidType overrides, so it is exactly what the other two loaders
    // apply below 26. Eye immersion needs no arm at all — updateFluidOnEyes records every tag of the
    // fluid state at eye level, so isEyeInFluid answers for a mod tag for free.
    //? if fabric && <26 {
    /*@Inject(method = "updateInWaterStateAndDoFluidPushing()Z", at = @At("TAIL"))
    private void ac_pushInModFluids(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        self.updateFluidHeightAndDoFluidPushing(ACTagRegistry.ACID, 0.014D);
        self.updateFluidHeightAndDoFluidPushing(ACTagRegistry.PURPLE_SODA, 0.014D);
    }
    *///?}
}
