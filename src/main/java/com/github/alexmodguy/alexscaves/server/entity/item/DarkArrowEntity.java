package com.github.alexmodguy.alexscaves.server.entity.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACDamageTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

public class DarkArrowEntity extends AbstractArrow {

    private float fadeOut = 0;
    private float prevFadeOut = 0;
    private boolean startFading = false;
    private float arrowR = 0;
    private float prevArrowR = 0;
    private static final EntityDataAccessor<Float> SHADOW_ARROW_DAMAGE = SynchedEntityData.defineId(DarkArrowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> PERFECT_SHOT = SynchedEntityData.defineId(DarkArrowEntity.class, EntityDataSerializers.BOOLEAN);

    public DarkArrowEntity(EntityType entityType, Level level) {
        // 1.20.3 gave AbstractArrow the stack it drops when picked up, stored and saved on the
        // entity. This mod's arrows answer that with their own getPickupItem() override on every
        // version, so the stack passed here only has to match it.
        //
        // 1.21 then took it off the two-argument form again and appended a nullable weapon stack to
        // the other two, the item the shot was fired from, which vanilla reads for the piercing and
        // punch enchantments. These arrows are not fired from anything the game should enchant off,
        // so they pass null and keep deciding their own pierce level.
        //? if >=1.21
        /*super(entityType, level);*/
        //? if >=1.20.3 && <1.21
        /*super(entityType, level, ItemStack.EMPTY);*/
        //? if <1.20.3
        super(entityType, level);
    }

    public DarkArrowEntity(Level level, LivingEntity shooter) {
        //? if >=1.21
        /*super(ACEntityRegistry.DARK_ARROW.get(), shooter, level, ItemStack.EMPTY, null);*/
        //? if >=1.20.3 && <1.21
        /*super(ACEntityRegistry.DARK_ARROW.get(), shooter, level, ItemStack.EMPTY);*/
        //? if <1.20.3
        super(ACEntityRegistry.DARK_ARROW.get(), shooter, level);
    }

    public DarkArrowEntity(Level level, double x, double y, double z) {
        //? if >=1.21
        /*super(ACEntityRegistry.DARK_ARROW.get(), x, y, z, level, ItemStack.EMPTY, null);*/
        //? if >=1.20.3 && <1.21
        /*super(ACEntityRegistry.DARK_ARROW.get(), x, y, z, level, ItemStack.EMPTY);*/
        //? if <1.20.3
        super(ACEntityRegistry.DARK_ARROW.get(), x, y, z, level);
    }


    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SHADOW_ARROW_DAMAGE, 0.0F);
        this.entityData.define(PERFECT_SHOT, false);
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void startFalling() {
        // 1.21.2 hid AbstractArrow's inGround field behind isInGround()/setInGround(). The reads
        // are rewritten by the !mc2102-arrow-inground rule; a write cannot be, so it is gated.
        //? if >=1.21.2 {
        /*this.setInGround(false);
        *///?} else {
        this.inGround = false;
        //?}
    }

    @Override
    public void tick() {
        super.tick();
        this.prevArrowR = this.arrowR;
        this.prevFadeOut = this.fadeOut;
        if (this.inGround) {
            this.startFading = true;
        }
        if (this.startFading) {
            this.noPhysics = true;
            this.setDeltaMovement(this.getDeltaMovement().scale(0.7F));
            if (this.fadeOut++ > 5F) {
                this.discard();
            }
        }
        if(this.isPerfectShot() && this.arrowR < 1.0F){
            this.arrowR = Math.min(arrowR + 0.15F, 1.0F);
        }
    }

    protected float getWaterInertia() {
        return 0.9F;
    }

    protected void onHitEntity(EntityHitResult entityHitResult) {
        Entity entity = entityHitResult.getEntity();
        Entity owner = this.getOwner();
        float damage = this.getShadowArrowDamage();
        if(this.isPerfectShot()){
            damage *= 2;
        }
        DamageSource damageSource = ACDamageTypes.causeDarkArrowDamage(entity.level().registryAccess(), owner);
        if ((owner == null || !entity.is(owner) && !entity.isAlliedTo(owner) && !owner.isAlliedTo(entity)) && !this.startFading) {
            if (ACCompat.hurt(entity, damageSource, damage)) {
                this.startFading = true;
            }
        }
    }

    public float getShadowArrowDamage() {
        return this.entityData.get(SHADOW_ARROW_DAMAGE);
    }

    public void setShadowArrowDamage(float f) {
        this.entityData.set(SHADOW_ARROW_DAMAGE, f);
    }

    public void setPerfectShot(boolean b) {
        this.entityData.set(PERFECT_SHOT, b);
    }

    public boolean isPerfectShot() {
       return this.entityData.get(PERFECT_SHOT);
    }

    public float getFadeOut(float partialTicks) {
        return prevFadeOut + (fadeOut - prevFadeOut) * partialTicks;
    }
    public float getArrowRed(float partialTicks) {
        return prevArrowR + (arrowR - prevArrowR) * partialTicks;
    }
}