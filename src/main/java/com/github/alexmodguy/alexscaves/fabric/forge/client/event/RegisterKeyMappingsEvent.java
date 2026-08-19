package com.github.alexmodguy.alexscaves.fabric.forge.client.event;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;
import net.minecraft.client.KeyMapping;

import java.util.function.Consumer;

/**
 * Fabric stand-in for the keybind registration phase.
 *
 * <p>One call site and one key: the special-ability bind the tamed mounts use. The loader adds the
 * mapping to the options' key list so it shows in the controls screen and is saved with the rest;
 * Fabric's equivalent does the same through its own helper, which is what the dispatcher hands in
 * here.
 */
public class RegisterKeyMappingsEvent extends Event {

    private final Consumer<KeyMapping> sink;

    public RegisterKeyMappingsEvent(Consumer<KeyMapping> sink) {
        this.sink = sink;
    }

    public void register(KeyMapping key) {
        sink.accept(key);
    }
}
