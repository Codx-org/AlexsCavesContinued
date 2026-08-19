package com.github.alexmodguy.alexscaves.fabric.entity;

/**
 * Fabric stand-in for Forge's {@code LightningBolt#setDamage(float)}.
 *
 * <p>Vanilla's {@code Entity#thunderHit} deals a hardcoded {@code 5.0F}; both Forge and NeoForge
 * add a {@code damage} field to {@code LightningBolt} and hurt for that instead. The tesla bulb
 * builds a visual-only bolt purely as a carrier for a damage value of {@code 1}, so on Fabric the
 * value has to live somewhere and the vanilla constant has to read it.
 *
 * <p>Both halves are supplied by {@code mixin.fabric.LightningBoltDamageMixin} (the field, on
 * {@code LightningBolt}) and {@code mixin.fabric.EntityThunderHitMixin} (a {@code @ModifyConstant}
 * on the {@code 5.0F} in {@code Entity#thunderHit}). Going through the real {@code thunderHit}
 * rather than re-implementing it is deliberate: every vanilla override of it — creeper charging,
 * villager-to-witch, pig-to-piglin, turtle eggs — keeps working unchanged.
 *
 * <p>Reached only through {@code ACCompat#setLightningDamage}; never cast to directly.
 */
public interface ACLightningDamage {

    void ac_setLightningDamage(float damage);

    float ac_getLightningDamage();
}
