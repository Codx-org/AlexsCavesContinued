package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.entity.ACLightningDamage;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Supplies {@link ACLightningDamage} on {@code LightningBolt} — the field half of Fabric's stand-in
 * for the loaders' {@code setDamage}/{@code getDamage} patch. The reading half is
 * {@link EntityThunderHitMixin}.
 *
 * <p>Read out of the 1.20.1 merged jars: Forge adds {@code private float damage} to
 * {@code LightningBolt}, seeds it with {@code 5.0F} in the constructor (offset 14, right after
 * {@code hitEntities}) and hangs a plain getter and setter off it — no save data, no synced data, no
 * network. This mixin is that, verbatim, including the seed, so a bolt nobody has touched hurts for
 * exactly what vanilla's constant says.
 *
 * <p>Reached only through {@code ACCompat#setLightningDamage}; the tesla bulb is the one caller.
 */
@Mixin(LightningBolt.class)
public class LightningBoltDamageMixin implements ACLightningDamage {

    @Unique
    private float ac_lightningDamage = 5.0F;

    @Override
    public void ac_setLightningDamage(float damage) {
        this.ac_lightningDamage = damage;
    }

    @Override
    public float ac_getLightningDamage() {
        return this.ac_lightningDamage;
    }
}
