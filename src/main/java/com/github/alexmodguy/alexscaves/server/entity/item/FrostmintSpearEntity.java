package com.github.alexmodguy.alexscaves.server.entity.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.util.FrostmintExplosion;
import com.github.alexmodguy.alexscaves.server.entity.util.FrostmintFreezableAccessor;
import com.github.alexmodguy.alexscaves.server.entity.util.TephraExplosion;
import com.github.alexmodguy.alexscaves.server.item.ACItemRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;

public class FrostmintSpearEntity extends AbstractArrow {
    private boolean dealtDamage;
    private boolean exploded;
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(FrostmintSpearEntity.class, EntityDataSerializers.INT);

    public FrostmintSpearEntity(EntityType entityType, Level level) {
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
        /*super(entityType, level, new ItemStack(ACItemRegistry.FROSTMINT_SPEAR.get()));*/
        //? if <1.20.3
        super(entityType, level);
    }

    public FrostmintSpearEntity(Level level, LivingEntity shooter, ItemStack itemStack) {
        //? if >=1.21
        /*super(ACEntityRegistry.FROSTMINT_SPEAR.get(), shooter, level, new ItemStack(ACItemRegistry.FROSTMINT_SPEAR.get()), null);*/
        //? if >=1.20.3 && <1.21
        /*super(ACEntityRegistry.FROSTMINT_SPEAR.get(), shooter, level, new ItemStack(ACItemRegistry.FROSTMINT_SPEAR.get()));*/
        //? if <1.20.3
        super(ACEntityRegistry.FROSTMINT_SPEAR.get(), shooter, level);
    }

    public FrostmintSpearEntity(Level level, double x, double y, double z) {
        //? if >=1.21
        /*super(ACEntityRegistry.FROSTMINT_SPEAR.get(), x, y, z, level, new ItemStack(ACItemRegistry.FROSTMINT_SPEAR.get()), null);*/
        //? if >=1.20.3 && <1.21
        /*super(ACEntityRegistry.FROSTMINT_SPEAR.get(), x, y, z, level, new ItemStack(ACItemRegistry.FROSTMINT_SPEAR.get()));*/
        //? if <1.20.3
        super(ACEntityRegistry.FROSTMINT_SPEAR.get(), x, y, z, level);
    }


    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return ACPlatform.getEntitySpawningPacket(this);
    }

    protected ItemStack getPickupItem() {
        return new ItemStack(ACItemRegistry.FROSTMINT_SPEAR.get());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_ID, -1);
    }

    @Nullable
    protected EntityHitResult findHitEntity(Vec3 vec3, Vec3 vec31) {
        return this.dealtDamage ? null : super.findHitEntity(vec3, vec31);
    }

    @Override
    public void tick(){
        super.tick();
        Entity owner = this.getOwner();
        if(!level().isClientSide() && owner != null){
            this.entityData.set(OWNER_ID, owner.getId());
        }
    }

    @Nullable
    public Entity getOwner() {
        if(level().isClientSide() && this.entityData.get(OWNER_ID) != -1){
            return this.level().getEntity(this.entityData.get(OWNER_ID));
        }else{
            return super.getOwner();
        }
    }

    protected void onHitEntity(EntityHitResult hitResult) {
        Entity entity = hitResult.getEntity();
        float f = 6.0F;
        if (entity instanceof LivingEntity livingentity) {
            f += ACCompat.damageBonus(this.getPickupItem(), livingentity);
        }

        Entity entity1 = this.getOwner();
        DamageSource damagesource = this.damageSources().trident(this, (Entity) (entity1 == null ? this : entity1));
        this.dealtDamage = true;
        SoundEvent soundevent = ACSoundRegistry.FROSTMINT_SPEAR_HIT.get();
        if (ACCompat.hurt(entity, damagesource, f)) {
            if (entity.getType() == EntityType.ENDERMAN) {
                return;
            }

            if (entity instanceof LivingEntity livingentity1) {
                if (entity1 instanceof LivingEntity) {
                    ACCompat.postAttackEffects(livingentity1, (LivingEntity) entity1, damagesource);
                }

                this.doPostHurtEffects(livingentity1);
            }
        }
        this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
        float f1 = 1.0F;
        this.playSound(soundevent, f1, 1.0F);
    }

    protected void onHit(HitResult hitResult) {
        if(!exploded && tickCount > 1){
            exploded = true;
            explode();
        }
        super.onHit(hitResult);
    }

    private void explode() {
        FrostmintExplosion explosion = new FrostmintExplosion(level(), this.getOwner(), this.getX(), this.getY(0.5), this.getZ(), 2.0F, Explosion.BlockInteraction.KEEP, true);
        explosion.explode();
        explosion.finalizeExplosion(true);
    }

    protected void doPostHurtEffects(LivingEntity living) {
        living.setTicksFrozen(living.getTicksRequiredToFreeze() + 200);
        ((FrostmintFreezableAccessor)living).setFrostmintFreezing(true);
    }

    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return ACSoundRegistry.FROSTMINT_SPEAR_HIT.get();
    }

    public boolean shouldRender(double x, double y, double z) {
        return true;
    }

}
