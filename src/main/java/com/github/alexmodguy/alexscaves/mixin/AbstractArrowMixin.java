package com.github.alexmodguy.alexscaves.mixin;


import com.github.alexmodguy.alexscaves.server.entity.living.DeepOneBaseEntity;
import com.github.alexmodguy.alexscaves.server.entity.util.MagnetUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin extends Projectile {

    protected AbstractArrowMixin(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(
            method = {"Lnet/minecraft/world/entity/projectile/AbstractArrow;canHitEntity(Lnet/minecraft/world/entity/Entity;)Z"},
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private void ac_canHitEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (getOwner() instanceof DeepOneBaseEntity && entity instanceof DeepOneBaseEntity) {
            cir.setReturnValue(false);
        }
    }

    // 1.20.3 gave every AbstractArrow constructor a trailing ItemStack (the stack the arrow drops on
    // pickup, stored in the new pickupItemStack field), so the shooter constructor's descriptor — and
    // therefore the handler's argument list — changes with it.
    //? if <1.20.3 {
    @Inject(
            method = {"Lnet/minecraft/world/entity/projectile/AbstractArrow;<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;)V"},
            at = @At(value = "TAIL")
    )
    private void ac_playerConstructor(EntityType arrowEntityType, LivingEntity shooter, Level level, CallbackInfo ci) {
        ac_offsetForMagnetism(shooter);
    }
    //?}

    //? if >=1.20.3 && <1.21 {
    /*@Inject(
            method = {"Lnet/minecraft/world/entity/projectile/AbstractArrow;<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)V"},
            at = @At(value = "TAIL")
    )
    private void ac_playerConstructor(EntityType arrowEntityType, LivingEntity shooter, Level level, net.minecraft.world.item.ItemStack pickupItem, CallbackInfo ci) {
        ac_offsetForMagnetism(shooter);
    }
    *///?}

    // …and 1.21 gave it a second one — the weapon the arrow was fired from, which is where the
    // projectile enchantments are now read from. It is nullable, so this arm cannot simply reuse
    // the one above with a wider descriptor.
    //? if >=1.21 {
    /*@Inject(
            method = {"Lnet/minecraft/world/entity/projectile/AbstractArrow;<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V"},
            at = @At(value = "TAIL")
    )
    private void ac_playerConstructor(EntityType arrowEntityType, LivingEntity shooter, Level level, net.minecraft.world.item.ItemStack pickupItem, net.minecraft.world.item.ItemStack firedFromWeapon, CallbackInfo ci) {
        ac_offsetForMagnetism(shooter);
    }
    *///?}

    private void ac_offsetForMagnetism(LivingEntity shooter) {
        if (MagnetUtil.getEntityMagneticDirection(shooter) != Direction.DOWN) {
            this.setPos(shooter.getEyePosition().add(0, -0.1, 0));
        }
    }
}
