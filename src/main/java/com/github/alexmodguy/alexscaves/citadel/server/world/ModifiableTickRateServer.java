package com.github.alexmodguy.alexscaves.citadel.server.world;

/**
 * Implemented on {@code MinecraftServer} by {@code mixin.citadel.MinecraftServerMixin}; lets the
 * tick-rate system stretch or compress the server's ms-per-tick.
 */
public interface ModifiableTickRateServer {

    void acSetGlobalTickLengthMs(long msPerTick);

    long acGetMasterMs();

    default void acResetGlobalTickLengthMs() {
        acSetGlobalTickLengthMs(-1);
    }
}
