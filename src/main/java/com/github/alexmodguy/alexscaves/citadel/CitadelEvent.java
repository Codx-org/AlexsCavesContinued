package com.github.alexmodguy.alexscaves.citadel;

//? if !forge || <1.21.6
import net.minecraftforge.eventbus.api.Event;

/**
 * Base class for the Citadel events that a listener can steer, carrying the tri-state result those
 * events are read through.
 *
 * <p>Upstream leaned on EventBus's own {@code @Event.HasResult} plus {@code Event.Result}. NeoForge's
 * EventBus 8 — which arrives with 1.20.5 — deleted both: an event that wants a decision from its
 * listeners now declares the field itself. Since these events are Citadel's own, they do exactly
 * that, and every call site then reads the same on every loader and every version.
 */
// Forge 56 (1.21.6) is the first Forge build on EventBus 7, which deleted the single Event base
// class: a mutable, extendable event now extends MutableEvent instead, and each concrete event type
// carries its own bus rather than being posted to a bus-wide post(). This class is abstract and is
// never posted, so it only needs the new supertype; its concrete subclasses each grow a BUS and a
// static post() below.
//? if forge && >=1.21.6
/*public abstract class CitadelEvent extends net.minecraftforge.eventbus.api.event.MutableEvent {*/
//? if !forge || <1.21.6
public abstract class CitadelEvent extends Event {

    /** Mirrors the old {@code Event.Result}: a listener overrode the behaviour, vetoed it, or said nothing. */
    public enum Result {
        DEFAULT,
        ALLOW,
        DENY
    }

    private Result citadelResult = Result.DEFAULT;

    public Result getCitadelResult() {
        return citadelResult;
    }

    public void setCitadelResult(Result citadelResult) {
        this.citadelResult = citadelResult;
    }
}
