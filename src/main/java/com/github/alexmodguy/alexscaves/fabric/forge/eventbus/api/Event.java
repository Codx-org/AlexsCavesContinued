package com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api;

/**
 * Fabric stand-in for the event base class of Forge's bus 6.
 *
 * <p><b>Two populations of events sit on top of this.</b> The ones the mod <i>receives</i> — a
 * loader's tick, damage, tooltip and render hooks, which {@code CommonEvents} and
 * {@code ClientEvents} subscribe to and which {@code fabric/event/**} constructs from Fabric API
 * callbacks — and the ones the mod <i>publishes</i>: Citadel's seven event classes plus
 * {@code AnimationEvent}, which exist so downstream mods can steer this one. Both extend this, on
 * every loader, which is why the stub reproduces the base rather than the individual events'
 * shapes.
 *
 * <p><b>Why the whole hierarchy is stubbed rather than the handlers rewritten.</b>
 * {@code CommonEvents} and {@code ClientEvents} are ~2,700 lines of game logic across ~60 hooks,
 * and all of it is loader-neutral once the event object has been unpacked. Reproducing that logic
 * on Fabric would put every future fix on two axes at once; reproducing the accessors it reads is a
 * data holder the compiler checks against the same call sites the other two loaders use. Same
 * relocated-compat-namespace pattern as {@code fabric/registries/DeferredRegister} and the vendored
 * Citadel classes.
 *
 * <p><b>{@link Result} is kept even though both other loaders have dropped it.</b> NeoForge's
 * EventBus 8 deleted it at 1.20.5 and Forge's 7 at 1.21.6; this tree's source is authored in the
 * oldest spelling and rewritten upwards, so the tri-state is what the shared code still reads, and
 * {@code ACPlatform} and {@code ACClientCompat} translate it per loader at their own gates.
 */
public class Event {

    /**
     * The tri-state a listener answers a "should this happen?" event with: force it, veto it, or
     * abstain and let the game decide.
     */
    public enum Result {
        DENY,
        DEFAULT,
        ALLOW,
    }

    private final boolean cancelable = getClass().isAnnotationPresent(Cancelable.class);
    private boolean canceled;
    private Result result = Result.DEFAULT;

    /**
     * Whether this event's type is marked {@link Cancelable}. Read once per instance because the
     * annotation cannot change, and inherited rather than declared-only would be wrong: Forge marks
     * each concrete event, and a cancellable subclass of a non-cancellable event is normal.
     */
    public boolean isCancelable() {
        return cancelable;
    }

    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Rejects a cancel on an event that is not {@link Cancelable}, exactly as Forge's bus does —
     * this is the one behaviour that annotation buys, and dropping it would let a handler silently
     * believe it had vetoed something.
     */
    public void setCanceled(boolean canceled) {
        if (!cancelable && canceled) {
            throw new UnsupportedOperationException("Attempted to cancel " + getClass().getName()
                    + ", which is not @Cancelable");
        }
        this.canceled = canceled;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }
}
