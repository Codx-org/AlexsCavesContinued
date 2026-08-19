package com.github.alexmodguy.alexscaves.fabric.forge.client.event;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

/**
 * Fabric stand-in for the particle-factory registration phase.
 *
 * <p>The two methods are the two kinds of factory this mod ships: seventeen types in all, six of
 * them sprite-set-driven and eleven drawing their own geometry.
 *
 * <p><b>Both take a sink rather than a registry.</b> Fabric registers a particle factory through its
 * own API rather than through anything vanilla exposes, and the two shapes go to two different calls
 * there, so the stub carries the split and lets the dispatcher supply the destination. The sinks are
 * interfaces with a <i>generic method</i>, not generic interfaces, because the type variable has to
 * tie a type to its provider at each call site the way the loader's does — which means a dispatcher
 * implements them with an anonymous class rather than a lambda. That is the price of keeping the
 * seventeen call sites unchanged.
 */
public class RegisterParticleProvidersEvent extends Event {

    /** Sink for a factory that draws its own geometry. */
    public interface SpecialSink {
        <T extends ParticleOptions> void accept(ParticleType<T> type, ParticleProvider<T> provider);
    }

    /** Sink for a factory that is handed the atlas sprites of its own texture set. */
    public interface SpriteSetSink {
        <T extends ParticleOptions> void accept(ParticleType<T> type, ParticleEngine.SpriteParticleRegistration<T> registration);
    }

    private final SpecialSink specialSink;
    private final SpriteSetSink spriteSetSink;

    public RegisterParticleProvidersEvent(SpecialSink specialSink, SpriteSetSink spriteSetSink) {
        this.specialSink = specialSink;
        this.spriteSetSink = spriteSetSink;
    }

    public <T extends ParticleOptions> void registerSpecial(ParticleType<T> type, ParticleProvider<T> provider) {
        specialSink.accept(type, provider);
    }

    public <T extends ParticleOptions> void registerSpriteSet(ParticleType<T> type, ParticleEngine.SpriteParticleRegistration<T> registration) {
        spriteSetSink.accept(type, registration);
    }
}
