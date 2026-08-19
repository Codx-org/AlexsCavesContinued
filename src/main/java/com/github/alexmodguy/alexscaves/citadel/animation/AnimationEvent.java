package com.github.alexmodguy.alexscaves.citadel.animation;

import net.minecraft.world.entity.Entity;
//? if (forge && <1.21.6) || fabric
import net.minecraftforge.eventbus.api.Cancelable;
//? if !forge || <1.21.6
import net.minecraftforge.eventbus.api.Event;

// Forge 56 (1.21.6) is on EventBus 7, which has no shared Event base class and no bus-wide post.
// See CitadelEvent for the wider note; the two posted subclasses each carry a bus below.
//? if forge && >=1.21.6
/*public class AnimationEvent<T extends Entity & IAnimatedEntity> extends net.minecraftforge.eventbus.api.event.MutableEvent {*/
//? if !forge || <1.21.6
public class AnimationEvent<T extends Entity & IAnimatedEntity> extends Event {
    protected Animation animation;
    private final T entity;

    AnimationEvent(T entity, Animation animation) {
        this.entity = entity;
        this.animation = animation;
    }

    public T getEntity() {
        return this.entity;
    }

    public Animation getAnimation() {
        return this.animation;
    }

    // Bus 6 declares cancellability with an annotation, bus 7 with an interface. See
    // EventChangeEntityTickRate for the same split.
    //? if (forge && <1.21.6) || fabric
    @Cancelable
    //? if neoforge
    /*public static class Start<T extends Entity & IAnimatedEntity> extends AnimationEvent<T> implements net.neoforged.bus.api.ICancellableEvent {*/
    //? if forge && >=1.21.6
    /*public static class Start<T extends Entity & IAnimatedEntity> extends AnimationEvent<T> implements net.minecraftforge.eventbus.api.event.characteristic.Cancellable {*/
    //? if (forge && <1.21.6) || fabric
    public static class Start<T extends Entity & IAnimatedEntity> extends AnimationEvent<T> {
        public Start(T entity, Animation animation) {
            super(entity, animation);
        }

        public void setAnimation(Animation animation) {
            this.animation = animation;
        }

        // The bus is keyed on the raw class — a generic event type has one bus for every
        // instantiation, exactly as Forge's own generic RenderLivingEvent.Pre does.
        //? if forge && >=1.21.6
        /*@SuppressWarnings("rawtypes") public static final net.minecraftforge.eventbus.api.bus.CancellableEventBus<Start> BUS = net.minecraftforge.eventbus.api.bus.CancellableEventBus.create(Start.class);*/

        /**
         * Posts this event and answers whether a listener cancelled it.
         *
         * <p>The NeoForge arm reads {@code isCanceled()} straight off the returned event rather than
         * through an {@code instanceof} pattern: the bus hands the event back, so the
         * pattern would be provably true, which javac rejects under {@code --release 17} (1.20.4) and
         * only tolerates from 21. The class already implements {@code ICancellableEvent} here.
         */
        public static boolean post(Start<?> event) {
            //? if forge && >=1.21.6
            /*return BUS.post(event);*/
            //? if (forge && <1.21.6) || fabric
            return net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
            //? if neoforge
            /*return net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event).isCanceled();*/
        }
    }

    public static class Tick<T extends Entity & IAnimatedEntity> extends AnimationEvent<T> {
        protected int tick;

        public Tick(T entity, Animation animation, int tick) {
            super(entity, animation);
            this.tick = tick;
        }

        public int getTick() {
            return this.tick;
        }

        //? if forge && >=1.21.6
        /*@SuppressWarnings("rawtypes") public static final net.minecraftforge.eventbus.api.bus.EventBus<Tick> BUS = net.minecraftforge.eventbus.api.bus.EventBus.create(Tick.class);*/

        /** Posts this event; EventBus 7 has no bus-wide post, so the call site asks the event. */
        public static void post(Tick<?> event) {
            //? if forge && >=1.21.6
            /*BUS.post(event);*/
            //? if !forge || <1.21.6
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        }
    }
}