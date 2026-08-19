package com.github.alexmodguy.alexscaves.server.potion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * The base class for all nine of Alex's Caves' effects, holding the two places where
 * {@link MobEffect}'s shape changed under this mod's version range.
 *
 * <p><b>Per-tick gate.</b> 1.20.2 renamed {@code isDurationEffectTick} to
 * {@code shouldApplyEffectTickThisTick}. That rename is silent — an override of the old name still
 * compiles, it just never gets called again, so every effect that only acts on some ticks would
 * quietly start acting on all of them. The gate lives here once and calls {@link #shouldTick}.
 *
 * <p><b>Start and end.</b> Upstream ran its per-entity setup and teardown from
 * {@code addAttributeModifiers} / {@code removeAttributeModifiers}, which were handed the
 * {@link LivingEntity}. 1.20.2 dropped that parameter — the two methods now see only the
 * {@code AttributeMap}, and vanilla has an entity-aware {@code onEffectStarted} but nothing for
 * removal. Rather than gate four effects against a hook that only half exists,
 * {@link #onEffectStart} and {@link #onEffectEnd} are driven from
 * {@code LivingEntityMixin}'s injections into {@code LivingEntity#onEffectAdded},
 * {@code #onEffectUpdated} and {@code #onEffectRemoved} — three methods that are identical on every
 * version this mod spans, and the exact call sites vanilla used to reach the attribute methods from.
 */
public abstract class ACMobEffect extends MobEffect {

    protected ACMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    /** Whether {@link #tick} should run this tick. */
    public abstract boolean shouldTick(int duration, int amplifier);

    /**
     * The per-tick body, as {@code applyEffectTick} used to be spelled.
     *
     * <p>1.20.5 gave {@code applyEffectTick} a boolean return — false means "drop the effect now",
     * which none of this mod's effects ever want — and 1.21.2 prepended the {@code ServerLevel} the
     * effect is ticking on. Neither is anything the nine subclasses need, so they implement this
     * instead and the bridge below owns all three signatures.
     */
    public void tick(LivingEntity entity, int amplifier) {
    }

    /** Vanilla's own per-tick body, for the subclass that still wants it. */
    protected void superTick(LivingEntity entity, int amplifier) {
        //? if >=1.21.2 {
        /*if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            super.applyEffectTick(serverLevel, entity, amplifier);
        }
        *///?} else {
        super.applyEffectTick(entity, amplifier);
        //?}
    }

    //? if >=1.21.2 {
    /*public boolean applyEffectTick(net.minecraft.server.level.ServerLevel level, LivingEntity entity, int amplifier) {
        this.tick(entity, amplifier);
        return true;
    }
    *///?} elif >=1.20.5 {
    /*public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        this.tick(entity, amplifier);
        return true;
    }
    *///?} else {
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        this.tick(entity, amplifier);
    }
    //?}

    /** This effect has just been applied to {@code entity}, or re-applied at a new amplifier. */
    public void onEffectStart(LivingEntity entity, int amplifier) {
    }

    /** This effect has just left {@code entity}. */
    public void onEffectEnd(LivingEntity entity) {
    }

    //? if >=1.20.2
    /*public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {*/
    //? if <1.20.2
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return this.shouldTick(duration, amplifier);
    }
}
