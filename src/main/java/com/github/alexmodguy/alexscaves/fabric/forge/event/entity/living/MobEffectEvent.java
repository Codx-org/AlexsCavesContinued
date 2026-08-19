package com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fabric stand-in for the status-effect lifecycle events. Three of the four shapes are used here —
 * {@link Added}, {@link Remove} and {@link Expired} — and they drive the enter/exit sounds of
 * Darkness Incarnate and Sugar Rush plus the sugar-rush time dilation, so all three must fire.
 *
 * <p>{@link Remove} is the odd one and it is deliberate, not an oversight: on this loader (as on
 * Forge) it answers {@link #getEffect()} with a bare {@link MobEffect}, because a removal is
 * addressed by effect type and the instance may already be gone. {@code ACCompat.vanillaEffect} has
 * an identity overload for exactly that, so the call site is the same text on all three loaders.
 * {@link Added} and {@link Expired} both hand over the whole {@link MobEffectInstance}, which the
 * handlers need for its duration.
 *
 * <p>The base class carries the instance and is not itself posted; the dispatcher always constructs
 * one of the three subclasses, since the bus resolves a handler by the exact class it declares plus
 * its supertypes.
 *
 * <p>{@link Remove}'s two constructors both cross the 1.20.5 {@code Holder} boundary — one asks
 * vanilla for an instance <i>by</i> effect, the other reads an instance's effect back — so both go
 * through {@code ACCompat}'s shims. Storing the bare {@link MobEffect} rather than a holder is what
 * keeps the accessor's own signature the same on every node, which is what the handlers were
 * written against.
 */
public class MobEffectEvent extends LivingEvent {

    private final MobEffectInstance effectInstance;

    public MobEffectEvent(LivingEntity entity, MobEffectInstance effectInstance) {
        super(entity);
        this.effectInstance = effectInstance;
    }

    public MobEffectInstance getEffectInstance() {
        return effectInstance;
    }

    /** An effect has just been applied — fired after the instance is in the entity's effect map. */
    public static class Added extends MobEffectEvent {

        private final MobEffectInstance oldEffectInstance;
        private final net.minecraft.world.entity.Entity source;

        public Added(LivingEntity entity, MobEffectInstance effectInstance) {
            this(entity, effectInstance, null, null);
        }

        public Added(LivingEntity entity, MobEffectInstance effectInstance, MobEffectInstance oldEffectInstance, net.minecraft.world.entity.Entity source) {
            super(entity, effectInstance);
            this.oldEffectInstance = oldEffectInstance;
            this.source = source;
        }

        /** The instance being replaced, or null when the effect is new to this entity. */
        public MobEffectInstance getOldEffectInstance() {
            return oldEffectInstance;
        }

        /** Whoever applied the effect, where that is known. Null for most sources. */
        public net.minecraft.world.entity.Entity getEffectSource() {
            return source;
        }
    }

    /**
     * An effect is being taken off the entity before it ran out — a milk bucket, a cure, or code
     * calling {@code removeEffect}. See the class javadoc for why this one is typed by effect.
     */
    public static class Remove extends MobEffectEvent {

        private final MobEffect effect;

        public Remove(LivingEntity entity, MobEffect effect) {
            super(entity, entity.getEffect(ACCompat.effect(effect)));
            this.effect = effect;
        }

        public Remove(LivingEntity entity, MobEffectInstance effectInstance) {
            super(entity, effectInstance);
            this.effect = ACCompat.vanillaEffect(effectInstance.getEffect());
        }

        public MobEffect getEffect() {
            return effect;
        }
    }

    /** An effect ran its duration out. */
    public static class Expired extends MobEffectEvent {

        public Expired(LivingEntity entity, MobEffectInstance effectInstance) {
            super(entity, effectInstance);
        }
    }
}
