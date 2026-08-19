package com.github.alexmodguy.alexscaves.fabric;

import com.github.alexmodguy.alexscaves.fabric.event.ACEventBus;
import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;

import java.util.function.Consumer;

/**
 * Stands in for Forge's MOD event bus, which Fabric has no equivalent of.
 *
 * <p><b>Why it is a type at all.</b> {@link com.github.alexmodguy.alexscaves.AlexsCaves}'s
 * constructor ends in ~28 lines of the shape {@code X.DEF_REG.register(modEventBus)} — the mod's
 * whole registration order, written down once. On Forge and NeoForge that argument is the mod bus
 * and the call merely schedules the fill; on Fabric there is no bus, and the vendored
 * {@link com.github.alexmodguy.alexscaves.fabric.registries.DeferredRegister} fills on the spot.
 * Taking a token anyway is what keeps all 28 lines <b>byte-identical on all three loaders</b>: only
 * the one line that declares {@code modEventBus} is gated, not the block that uses it.
 *
 * <p>That matters more here than the saved diff suggests. The flush order is load-bearing on this
 * loader alone — item suppliers dereference block handles, the creative tab walks the item registry
 * — and a second copy of it, in a Fabric-only entrypoint, would be a copy free to drift out of sync
 * with the real one every time a registry is added.
 *
 * <p><b>Why it also dispatches.</b> Registration is only half of what the mod bus carries.
 * {@code ClientProxy} hangs eight client-registration callbacks off it — particles, keybinds, block
 * and item colours, tooltip components, entity layers, model baking, shaders — none of which
 * happens at any Fabric registry, and all of which the Fabric client entrypoint has to fire itself.
 * So the token owns a real {@link ACEventBus} and the call sites keep the shape they already have.
 *
 * <p><b>The listener's event type is named explicitly, never inferred.</b> Forge derives it from
 * the lambda's own signature, which has no portable equivalent — the same reason
 * {@link ACEventBus} takes a class literal — so on this loader every {@code addListener} call site
 * sits in a {@code fabric} arm that passes one. Failing that at compile time is the point: a bus
 * that guessed would fail at dispatch, in a callback that simply never runs.
 *
 * <p>A single {@link #INSTANCE} mirrors {@code MinecraftForge.EVENT_BUS} on the game side: FML
 * gives each mod its own mod bus, and this tree is one mod.
 */
public final class ModBus {

    public static final ModBus INSTANCE = new ModBus();

    private final ACEventBus bus = new ACEventBus();

    private ModBus() {
    }

    public <T extends Event> void addListener(Class<T> eventType, Consumer<T> listener) {
        bus.addListener(eventType, listener);
    }

    /** Fires a mod-lifecycle or client-registration event. Answers whether a listener cancelled it. */
    public boolean post(Event event) {
        return bus.post(event);
    }
}
