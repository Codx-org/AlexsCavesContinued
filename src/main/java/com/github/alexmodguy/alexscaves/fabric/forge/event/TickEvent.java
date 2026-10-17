package com.github.alexmodguy.alexscaves.fabric.forge.event;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

/**
 * Fabric stand-in for {@code net.minecraftforge.event.TickEvent}.
 *
 * <p>Only the pre-split shape is reproduced — one event per target carrying a <b>public
 * {@code phase} field</b> rather than {@code Pre}/{@code Post} subclasses. NeoForge split these in
 * 1.20.5 and Forge in 1.21.9, but both of those arms are loader-gated in {@code CommonEvents},
 * {@code CitadelEvents} and {@code CitadelClientEvents}, so Fabric lands on the phase-tagged shape
 * on every MC version. The field spelling matters: the shared source reads {@code event.phase}, not
 * a getter.
 *
 * <p>Which phases the dispatcher has to fire differs per event, and getting it wrong is silent:
 * {@link ServerTickEvent} and {@link ClientTickEvent} are both read for their phase and need
 * <b>both</b> ({@code ACWorldWorkerManager} budgets work across the pair, and the two Citadel tick
 * hooks want START while one client handler wants END), while {@link PlayerTickEvent} is never
 * asked and is fired <b>once</b> per tick — matching the {@code Post}-only listener the other two
 * loaders settled on, rather than upstream's accidental twice-a-tick.
 */
public class TickEvent extends Event {

    /** Whether this is the tick's leading or trailing edge. */
    public enum Phase {
        START,
        END,
    }

    public final Phase phase;

    public TickEvent(Phase phase) {
        this.phase = phase;
    }

    /** One per server tick, in both phases. */
    public static class ServerTickEvent extends TickEvent {

        private final MinecraftServer server;

        public ServerTickEvent(Phase phase, MinecraftServer server) {
            super(phase);
            this.server = server;
        }

        public MinecraftServer getServer() {
            return server;
        }
    }

    /** One per client tick, in both phases. */
    public static class ClientTickEvent extends TickEvent {

        public ClientTickEvent(Phase phase) {
            super(phase);
        }
    }

    /**
     * One per player per tick. The player arrives in a public field, which is Forge's spelling and
     * what the shared source reads; see the class note for why only one phase is fired.
     */
    public static class PlayerTickEvent extends TickEvent {

        public final Player player;

        public PlayerTickEvent(Phase phase, Player player) {
            super(phase);
            this.player = player;
        }
    }
}
