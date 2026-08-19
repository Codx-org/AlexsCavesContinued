package com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fabric stand-in for bus 6's "this event may be vetoed" marker.
 *
 * <p>Two of the mod's own published events carry it — {@code AnimationEvent.Start} and
 * {@code EventChangeEntityTickRate} — and the stubbed loader events that a handler cancels carry it
 * too. {@link Event} reads it in its constructor and {@link Event#setCanceled} enforces it, so the
 * annotation is load-bearing here rather than decorative: without it a handler could believe it had
 * cancelled something that was never cancellable.
 *
 * <p>{@link RetentionPolicy#RUNTIME} for that reason. Both other loaders express the same idea with
 * an interface on newer bus generations ({@code ICancellableEvent} / {@code Cancellable}), which is
 * why every site carrying this is already inside a version-and-loader gate.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Cancelable {
}
