package com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fabric stand-in for bus 6's handler marker, and it does the same job here that it does on Forge:
 * {@code ACEventBus.register} scans a handler object for methods carrying it and keys each one on
 * its single parameter type. So the ~60 annotated handlers in {@code CommonEvents},
 * {@code ClientEvents}, {@code CitadelEvents} and {@code CitadelClientEvents} need no change and no
 * per-loader registration list — the same property that makes {@code EVENT_BUS.register(new
 * CommonEvents())} byte-identical on all three loaders.
 *
 * <p>{@link RetentionPolicy#RUNTIME} is therefore mandatory, not a convenience.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {

    EventPriority priority() default EventPriority.NORMAL;

    boolean receiveCanceled() default false;
}
