package com.github.alexmodguy.alexscaves.fabric.forge.client.event;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.function.Function;

/**
 * Fabric stand-in for the "how is this server-side tooltip component drawn" registration phase.
 *
 * <p>One call site: the sack of sating, whose tooltip lists what it has eaten as item icons rather
 * than as text. The mapping is data the client has to be told, since the component that travels in
 * the stack carries no drawing code of its own.
 *
 * <p>The sink is an interface with a generic method for the same reason the particle one is — the
 * type variable ties the component class to the factory that consumes it, and a lambda cannot
 * declare one.
 */
public class RegisterClientTooltipComponentFactoriesEvent extends Event {

    public interface Sink {
        <T extends TooltipComponent> void accept(Class<T> type, Function<? super T, ? extends ClientTooltipComponent> factory);
    }

    private final Sink sink;

    public RegisterClientTooltipComponentFactoriesEvent(Sink sink) {
        this.sink = sink;
    }

    public <T extends TooltipComponent> void register(Class<T> type, Function<? super T, ? extends ClientTooltipComponent> factory) {
        sink.accept(type, factory);
    }
}
