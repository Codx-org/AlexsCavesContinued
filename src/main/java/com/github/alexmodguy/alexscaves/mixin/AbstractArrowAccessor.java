package com.github.alexmodguy.alexscaves.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * A public handle on {@code AbstractArrow#setPierceLevel}.
 *
 * <p>The setter was public up to 1.20.6, and the burrowing arrow simply called it. 1.21 made it
 * private — vanilla now derives the pierce level from the piercing enchantment on the weapon the
 * arrow was fired from, which is not how this mod's arrow works: it pierces one extra target because
 * of what it is, not because of what shot it. An invoker restores the call without an access
 * transformer, so the same line compiles on every node and no loader-specific widening file has to
 * learn about it.
 */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {

    @Invoker("setPierceLevel")
    void ac$setPierceLevel(byte pierceLevel);

    /**
     * A public handle on the {@code baseDamage} field.
     *
     * <p>{@code getBaseDamage()} existed up to 1.21.4 and was deleted in 1.21.5 — the setter and
     * {@code setBaseDamageFromMob} survive, but nothing reads the value back, and neither loader
     * patches a getter in. The field itself is unchanged (a {@code private double}) on every node,
     * so one ungated accessor answers the one call site (the dreadbow's perfect shot, which doubles
     * whatever the ammo already had) identically everywhere.
     */
    @Accessor("baseDamage")
    double ac$getBaseDamage();
}
