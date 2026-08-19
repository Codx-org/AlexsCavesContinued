package com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api;

/**
 * Fabric stand-in for bus 6's listener priorities.
 *
 * <p>Declared highest-first, which is both Forge's declaration order and its dispatch order, so
 * {@code ACEventBus} can sort on {@link #ordinal()} ascending and get Forge's semantics for free.
 * That is not academic here: three handlers name a priority and mean it — two tooltip/HUD hooks run
 * {@code LOWEST} so their lines land after every other mod's, and one runs {@code HIGHEST} so it
 * gets first refusal.
 */
public enum EventPriority {
    HIGHEST,
    HIGH,
    NORMAL,
    LOW,
    LOWEST,
}
