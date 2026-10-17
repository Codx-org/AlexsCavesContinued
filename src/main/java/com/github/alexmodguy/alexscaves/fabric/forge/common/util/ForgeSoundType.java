package com.github.alexmodguy.alexscaves.fabric.forge.common.util;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;

import java.util.function.Supplier;

/**
 * Fabric stand-in for the lazy {@code SoundType} — vanilla's takes five {@code SoundEvent}s by
 * value, this one takes five suppliers.
 *
 * <p><b>Why the mod needs the lazy form at all.</b> {@code ACSoundTypes} declares 24 of these as
 * {@code static final} fields, and 22 of them name this mod's own sounds through
 * {@code ACSoundRegistry}'s deferred handles. Those handles are unbound while the class
 * initialiser runs, so passing {@code X.get()} eagerly would throw "Trying to access unbound
 * value" — the same trap {@code ACBlockRegistry} hit with {@code ACFoods} on 1.20.5, recorded in
 * DEVELOPMENT.md. Deferring each getter to first use is what makes a constant-shaped declaration legal.
 *
 * <p>Vanilla's five sound getters are plain instance methods on every version this mod targets, so
 * overriding them is enough and the {@code null}s handed to {@code super} are never read.
 */
public class ForgeSoundType extends SoundType {

    private final Supplier<SoundEvent> breakSound;
    private final Supplier<SoundEvent> stepSound;
    private final Supplier<SoundEvent> placeSound;
    private final Supplier<SoundEvent> hitSound;
    private final Supplier<SoundEvent> fallSound;

    public ForgeSoundType(float volume, float pitch,
                          Supplier<SoundEvent> breakSound,
                          Supplier<SoundEvent> stepSound,
                          Supplier<SoundEvent> placeSound,
                          Supplier<SoundEvent> hitSound,
                          Supplier<SoundEvent> fallSound) {
        super(volume, pitch, null, null, null, null, null);
        this.breakSound = breakSound;
        this.stepSound = stepSound;
        this.placeSound = placeSound;
        this.hitSound = hitSound;
        this.fallSound = fallSound;
    }

    @Override
    public SoundEvent getBreakSound() {
        return breakSound.get();
    }

    @Override
    public SoundEvent getStepSound() {
        return stepSound.get();
    }

    @Override
    public SoundEvent getPlaceSound() {
        return placeSound.get();
    }

    @Override
    public SoundEvent getHitSound() {
        return hitSound.get();
    }

    @Override
    public SoundEvent getFallSound() {
        return fallSound.get();
    }
}
