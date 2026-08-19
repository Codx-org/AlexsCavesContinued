package com.github.alexmodguy.alexscaves.server.entity.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.server.enchantment.ACEnchantmentRegistry;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.item.ACItemRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACDamageTypes;
import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class DesolateDaggerEntity extends Entity {

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

    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(DesolateDaggerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> STAB = SynchedEntityData.defineId(DesolateDaggerEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> PLAYER_ID = SynchedEntityData.defineId(DesolateDaggerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> ITEMSTACK = SynchedEntityData.defineId(DesolateDaggerEntity.class, EntityDataSerializers.ITEM_STACK);

    protected final RandomSource orbitRandom = RandomSource.create();
    private float orbitOffset = 0;
    private float prevStab = 0;
    public int orbitFor = 20;
    public ItemStack daggerRenderStack = new ItemStack(ACItemRegistry.DESOLATE_DAGGER.get());
    private int lSteps;
    private double lx;
    private double ly;
    private double lz;
    private double lyr;
    private double lxr;
    private double lxd;
    private double lyd;
    private double lzd;

    private boolean playedSummonNoise = false;

    public DesolateDaggerEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        orbitFor = 20 + level.getRandom().nextInt(10);
    }


    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return ACPlatform.getEntitySpawningPacket(this);
    }

    @Override
    public void tick() {
        super.tick();
        prevStab = this.getStab();
        Entity entity = getTargetEntity();
        if (level().isClientSide()) {
            level().addParticle(DustParticleOptions.REDSTONE, (double) this.getRandomX(0.75F), (double) this.getRandomY(), (double) this.getRandomZ(0.75F), 0.0D, 0.0D, 0.0D);
        }
        if (!playedSummonNoise) {
            this.playSound(ACSoundRegistry.DESOLATE_DAGGER_SUMMON.get());
            playedSummonNoise = true;
        }
        if (entity != null) {
            this.noPhysics = true;
            float invStab = 1F - getStab();
            Vec3 orbitAround = entity.position().add(0, entity.getBbHeight() * 0.25F, 0);
            orbitRandom.setSeed(this.getId());
            if (orbitOffset == 0) {
                orbitOffset = orbitRandom.nextInt(360);
            }
            Vec3 orbitAdd = new Vec3(0, (orbitRandom.nextFloat() + entity.getBbHeight()) * invStab, (orbitRandom.nextFloat() + entity.getBbWidth()) * invStab).yRot((float) Math.toRadians((orbitOffset)));
            this.setDeltaMovement(orbitAround.add(orbitAdd).subtract(this.position()));
            if (!level().isClientSide()) {
                if (orbitFor > 0 && entity.isAlive()) {
                    orbitFor--;
                } else {
                    this.setStab(Math.min(this.getStab() + 0.2F, 1F));
                }
                if (this.getStab() >= 1F) {
                    Entity player = getPlayer();
                    Entity damageFrom = player == null ? this : player;
                    float damage = 2 + ACCompat.enchantLevel(this.getItemStack(), ACEnchantmentRegistry.IMPENDING_STAB) * 2F;
                    if (ACCompat.hurt(entity, ACDamageTypes.causeDesolateDaggerDamage(this.level().registryAccess(), damageFrom), damage)) {
                        this.playSound(ACSoundRegistry.DESOLATE_DAGGER_HIT.get());
                        int healBy = ACCompat.enchantLevel(this.getItemStack(), ACEnchantmentRegistry.SATED_BLADE);
                        if(healBy > 0 && damageFrom instanceof Player healPlayer && healPlayer.getFoodData().getSaturationLevel() < 5F){
                            healPlayer.getFoodData().setSaturation(healPlayer.getFoodData().getSaturationLevel() + healBy * 0.1F);
                        }
                    }
                    this.discard();
                }
            }
            double d1 = entity.getZ() - this.getZ();
            double d3 = entity.getEyeY() - this.getEyeY();
            double d2 = entity.getX() - this.getX();
            float f = Mth.sqrt((float) (d2 * d2 + d1 * d1));
            this.setYRot(-((float) Mth.atan2(d2, d1)) * (180F / (float) Math.PI));
            this.setXRot(-(float) (Mth.atan2(d3, f) * (double) (180F / (float) Math.PI)));
        } else if (tickCount > 3) {
            this.noPhysics = false;
            this.discard();
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9F));

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
        }
    }

    public ItemStack getItemStack() {
        return this.entityData.get(ITEMSTACK);
    }

    public void setItemStack(ItemStack item) {
        this.entityData.set(ITEMSTACK, item);
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

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(PLAYER_ID, -1);
        this.entityData.define(STAB, 0F);
        this.entityData.define(ITEMSTACK, new ItemStack(Items.IRON_SWORD));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {

    }

    private int getTargetId() {
        return this.entityData.get(TARGET_ID);
    }

    public void setTargetId(int id) {
        this.entityData.set(TARGET_ID, id);
    }

    private int getPlayerId() {
        return this.entityData.get(PLAYER_ID);
    }

    public void setPlayerId(int id) {
        this.entityData.set(PLAYER_ID, id);
    }

    public float getStab() {
        return this.entityData.get(STAB);
    }

    public float getStab(float partialTicks) {
        return prevStab + (getStab() - prevStab) * partialTicks;
    }

    public void setStab(float stab) {
        this.entityData.set(STAB, stab);
    }

    private Entity getTargetEntity() {
        int id = getTargetId();
        return id == -1 ? null : level().getEntity(id);
    }

    private Entity getPlayer() {
        int id = getPlayerId();
        return id == -1 ? null : level().getEntity(id);
    }
}
