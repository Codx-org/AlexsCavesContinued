package com.github.alexmodguy.alexscaves.fabric.event;

import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import com.github.alexmodguy.alexscaves.fabric.forge.event.TickEvent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * The client half of {@link ACGameEvents}. Registered from {@code AlexsCavesFabricClient}, so it is
 * never loaded on a dedicated server.
 *
 * <p>Both phases are fired: Citadel's client tick hook wants START and one of this mod's own client
 * handlers wants END, and neither is reachable if only one edge is posted.
 */
public final class ACClientGameEvents {

    private ACClientGameEvents() {
    }

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(client ->
                MinecraftForge.EVENT_BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.START)));
        ClientTickEvents.END_CLIENT_TICK.register(client ->
                MinecraftForge.EVENT_BUS.post(new TickEvent.ClientTickEvent(TickEvent.Phase.END)));
    }
}
