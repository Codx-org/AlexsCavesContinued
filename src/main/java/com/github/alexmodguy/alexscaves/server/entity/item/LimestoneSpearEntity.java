package com.github.alexmodguy.alexscaves.server.entity.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.item.ACItemRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class LimestoneSpearEntity extends AbstractArrow {
    private boolean dealtDamage;

    public LimestoneSpearEntity(EntityType entityType, Level level) {
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
        /*super(entityType, level, new ItemStack(ACItemRegistry.LIMESTONE_SPEAR.get()));*/
        //? if <1.20.3
        super(entityType, level);
    }

    public LimestoneSpearEntity(Level level, LivingEntity shooter, ItemStack itemStack) {
        //? if >=1.21
        /*super(ACEntityRegistry.LIMESTONE_SPEAR.get(), shooter, level, new ItemStack(ACItemRegistry.LIMESTONE_SPEAR.get()), null);*/
        //? if >=1.20.3 && <1.21
        /*super(ACEntityRegistry.LIMESTONE_SPEAR.get(), shooter, level, new ItemStack(ACItemRegistry.LIMESTONE_SPEAR.get()));*/
        //? if <1.20.3
        super(ACEntityRegistry.LIMESTONE_SPEAR.get(), shooter, level);
    }

    public LimestoneSpearEntity(Level level, double x, double y, double z) {
        //? if >=1.21
        /*super(ACEntityRegistry.LIMESTONE_SPEAR.get(), x, y, z, level, new ItemStack(ACItemRegistry.LIMESTONE_SPEAR.get()), null);*/
        //? if >=1.20.3 && <1.21
        /*super(ACEntityRegistry.LIMESTONE_SPEAR.get(), x, y, z, level, new ItemStack(ACItemRegistry.LIMESTONE_SPEAR.get()));*/
        //? if <1.20.3
        super(ACEntityRegistry.LIMESTONE_SPEAR.get(), x, y, z, level);
    }


    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return ACPlatform.getEntitySpawningPacket(this);
    }

    protected ItemStack getPickupItem() {
        return new ItemStack(ACItemRegistry.LIMESTONE_SPEAR.get());
    }


    @Nullable
    protected EntityHitResult findHitEntity(Vec3 vec3, Vec3 vec31) {
        return this.dealtDamage ? null : super.findHitEntity(vec3, vec31);
    }

    protected void onHitEntity(EntityHitResult hitResult) {
        Entity entity = hitResult.getEntity();
        float f = 4.0F;
        if (entity instanceof LivingEntity livingentity) {
            f += ACCompat.damageBonus(this.getPickupItem(), livingentity);
        }

        Entity entity1 = this.getOwner();
        DamageSource damagesource = this.damageSources().trident(this, (Entity) (entity1 == null ? this : entity1));
        this.dealtDamage = true;
        SoundEvent soundevent = ACSoundRegistry.LIMESTONE_SPEAR_HIT.get();
        if (ACCompat.hurt(entity, damagesource, f)) {
            if (entity.getType() == EntityType.ENDERMAN) {
                return;
            }

            if (entity instanceof LivingEntity) {
                LivingEntity livingentity1 = (LivingEntity) entity;
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

    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return ACSoundRegistry.LIMESTONE_SPEAR_HIT.get();
    }

    public boolean shouldRender(double x, double y, double z) {
        return true;
    }

}
