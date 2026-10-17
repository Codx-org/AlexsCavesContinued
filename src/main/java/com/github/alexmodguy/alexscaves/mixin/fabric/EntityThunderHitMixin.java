package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.entity.ACLightningDamage;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * The reading half of Fabric's stand-in for the loaders' lightning-damage patch;
 * {@link LightningBoltDamageMixin} is the field half.
 *
 * <p>Vanilla's {@code Entity#thunderHit} hurts for a hardcoded {@code 5.0F}. Forge replaces exactly
 * that constant with {@code bolt.getDamage()} and changes nothing else in the method — read out of
 * the 1.20.1 merged jars, where vanilla loads the constant at offset 31 and Forge has
 * {@code aload_2; invokevirtual LightningBolt.getDamage()} in its place. So the faithful
 * reproduction is a {@code @ModifyConstant} on the same constant, reading the bolt through the
 * enclosing method's own parameter.
 *
 * <p>The tesla bulb is why this matters: it builds a visual-only bolt purely as a carrier for a
 * damage value of {@code 1}, so on Fabric the value has to live somewhere and the vanilla constant
 * has to read it.
 *
 * <p>Deliberately ungated. The selector and the constant are unchanged at both ends of the range —
 * 26.2 still loads {@code 5.0F} in {@code thunderHit}, it merely feeds
 * {@code hurtServer(ServerLevel, DamageSource, float)} rather than {@code hurt(DamageSource, float)},
 * which this injection never names. ⚠️ Do not add a second float constant to the selector without an
 * {@code ordinal}: from the band where {@code setSecondsOnFire(8)} became
 * {@code igniteForSeconds(8.0F)} the method carries a second float, and only the value {@code 5.0F}
 * distinguishes them.
 *
 * <p>{@code @Local(argsOnly = true)} is safe here for the usual reason — the merged Mojmap jars carry
 * no LocalVariableTable, so a local is inferred from StackMapTable frames, but a distinctly-typed
 * reference <em>parameter</em> is read straight off the descriptor.
 */
@Mixin(Entity.class)
public class EntityThunderHitMixin {

    @ModifyConstant(
            method = "thunderHit(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LightningBolt;)V",
            constant = @Constant(floatValue = 5.0F)
    )
    private float ac_thunderHitDamage(float original, @Local(argsOnly = true) LightningBolt bolt) {
        return ((ACLightningDamage) bolt).ac_getLightningDamage();
    }
}
