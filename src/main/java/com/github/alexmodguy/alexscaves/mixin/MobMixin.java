package com.github.alexmodguy.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.entity.util.EntityDropChanceAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntity implements EntityDropChanceAccessor {

    // 1.21.5 folded the two chance arrays into an immutable DropChances record and deleted the
    // per-slot getter. Its replacement is public, so it needs no @Shadow at all — and a @Shadow of
    // a member that no longer exists resolves fine at compile time and then fails at mixin apply
    // with "was not located in the target class", which is why this is gated and not left alone.
    //? if <1.21.5
    @Shadow protected abstract float getEquipmentDropChance(EquipmentSlot p_21520_);

    @Shadow public abstract void setDropChance(EquipmentSlot p_21410_, float p_21411_);

    @Shadow private boolean canPickUpLoot;

    // 1.21 handed dropCustomDeathLoot the level it is dropping into and took away the looting
    // multiplier. The bridge below keeps the older shape so EntityDropChanceAccessor and its one
    // caller do not have to change; the level is simply the one the mob is standing in, and the
    // caller is a server-side event, so the cast always holds.
    //? if >=1.21 {
    /*@Shadow protected abstract void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel serverLevel, DamageSource damageSource, boolean hitByPlayer);
    *///?} else {
    @Shadow protected abstract void dropCustomDeathLoot(DamageSource p_21385_, int p_21386_, boolean p_21387_);
    //?}

    public MobMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    public float ac_getEquipmentDropChance(EquipmentSlot equipmentSlot){
        //? if >=1.21.5 {
        /*return ((Mob) (Object) this).getDropChances().byEquipment(equipmentSlot);
        *///?} else {
        return this.getEquipmentDropChance(equipmentSlot);
        //?}
    }

    public void ac_setDropChance(EquipmentSlot equipmentSlot, float chance){
        this.setDropChance(equipmentSlot, chance);
    }

    public void ac_dropCustomDeathLoot(DamageSource damageSource, int i1, boolean idk){
        //? if >=1.21 {
        /*this.dropCustomDeathLoot((net.minecraft.server.level.ServerLevel) this.level(), damageSource, idk);
        *///?} else {
        this.dropCustomDeathLoot(damageSource, i1, idk);
        //?}
    }
}
