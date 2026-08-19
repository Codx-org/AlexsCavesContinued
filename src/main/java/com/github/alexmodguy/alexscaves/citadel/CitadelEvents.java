package com.github.alexmodguy.alexscaves.citadel;

import com.github.alexmodguy.alexscaves.citadel.server.tick.ServerTickRateTracker;
import com.github.alexmodguy.alexscaves.citadel.server.world.CitadelServerData;
import com.github.alexmodguy.alexscaves.citadel.server.world.ModifiableTickRateServer;
import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;
import net.minecraft.server.MinecraftServer;
// Fabric keeps this on every version. The gate is about NeoForge, which folded the tick events
// into per-target ones at 1.20.5; Fabric's TickEvent is this tree's own vendored stub, fired by
// its own bus, so there is nothing there to fold and every listener below takes the else arm.
//? if forge || fabric || <1.20.5
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Citadel's common game-bus listener — upstream kept this handler on the proxy itself.
 *
 * <p>It cannot live there any more: NeoForge's EventBus 7 rejects registering an object whose
 * <em>supertype</em> declares an {@code @SubscribeEvent} method ("Only the listener object can have
 * @SubscribeEvent methods"), and {@link CitadelClientProxy} extends {@link CitadelProxy}. On a
 * dedicated server the proxy is a plain {@code CitadelProxy} and the check passes, so this only
 * ever surfaced on a NeoForge <em>client</em>. Splitting the handlers out into listener classes
 * with no inheritance between them satisfies the rule on both loaders.
 *
 * @see CitadelClientEvents
 */
public final class CitadelEvents {

    // Forge's eventbus 7 (56.0.0, i.e. 1.21.6) refuses to scan a class that contributes exactly one
    // listener — "Only a single listener found in class …, you should directly call addListener()
    // on the EventBus of ServerTickEvent instead", thrown out of MinecraftForge.EVENT_BUS.register
    // and fatal at mod-load. This class is exactly that class, so on those nodes it registers
    // itself against the per-event bus and carries no @SubscribeEvent at all. TickEvent is an
    // InheritableEvent, so a listener on ServerTickEvent's own bus still sees the Pre and Post
    // subclasses and `phase` still tells them apart.
    //
    // NeoForge's 1.20.5 tick rework, the middle arm, split TickEvent into one event class per tick
    // target, each with its own Pre and Post subclass, and dropped the `phase` field the single
    // event used to carry. START is Pre, so that listener is the last one written differently; the
    // work itself is shared below.
    // Forge 59.x (1.21.9) finished the same rework NeoForge did in 1.20.5: TickEvent is a sealed
    // interface whose per-target members are records with their own Pre and Post, so `phase` is
    // gone here too and each half has its own bus. START is Pre. The getters became record
    // accessors along with it, hence server() for getServer().
    //? if forge && >=1.21.9 {
    /*public static void register() {
        TickEvent.ServerTickEvent.Pre.BUS.addListener(EventPriority.LOWEST, event -> tickServer(event.server()));
    }
    *///?} elif forge && >=1.21.6 {
    /*public static void register() {
        TickEvent.ServerTickEvent.BUS.addListener(EventPriority.LOWEST, event -> {
            if (event.phase == TickEvent.Phase.START) {
                tickServer(event.getServer());
            }
        });
    }
    *///?} elif neoforge && >=1.20.5 {
    /*@SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Pre event) {
        tickServer(event.getServer());
    }
    *///?} else {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            tickServer(event.getServer());
        }
    }
    //?}

    private static void tickServer(MinecraftServer server) {
        if (server.isRunning()) {
            CitadelServerData citadelServerData = CitadelServerData.get(server);
            ServerTickRateTracker tickRateTracker = citadelServerData.getOrCreateTickRateTracker();
            if (server instanceof ModifiableTickRateServer modifiableServer) {
                long l = tickRateTracker.getServerTickLengthMs();
                if (l == ACPlatform.MS_PER_TICK) {
                    modifiableServer.resetGlobalTickLengthMs();
                } else {
                    modifiableServer.setGlobalTickLengthMs(l);
                }
                if (!server.isShutdown()) {
                    tickRateTracker.masterTick();
                }
            }
        }
    }
}
