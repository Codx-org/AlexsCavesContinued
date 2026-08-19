package com.github.alexmodguy.alexscaves.server.level.map;

import java.util.ArrayList;
import java.util.List;

/**
 * A tick-budgeted queue of long-running server tasks — this mod's own copy of Forge's
 * {@code net.minecraftforge.common.WorldWorkerManager}, which NeoForge deleted in 21.9 and Fabric
 * has never had. Only {@link CaveBiomeMapWorldWorker} uses it, and only ever one worker at a time.
 *
 * <p>The behaviour is deliberately identical to the loader implementation it replaces: at the start
 * of a server tick {@link #tick(boolean) tick(true)} stamps the clock, and at the end
 * {@code tick(false)} runs workers round-robin until the tick's 50&nbsp;ms are spent — with a 10&nbsp;ms
 * floor, so a server that is already behind still makes progress rather than starving the queue
 * forever. A worker that returns {@code false} from {@link IWorker#doWork()} yields to the next one
 * for the rest of the tick; a worker that stops reporting {@link IWorker#hasWork()} is dropped.
 *
 * <p>Vendored for the same reason Citadel was: the alternative is a per-loader, per-version gate
 * around a class that comes and goes, for fifty lines that have no loader-specific content at all.
 *
 * <p>Original code &copy; Forge Development LLC and contributors, LGPL-2.1-only.
 */
public final class ACWorldWorkerManager {

    private static final List<IWorker> WORKERS = new ArrayList<>();
    private static long startTime = -1;
    private static int index = 0;

    private ACWorldWorkerManager() {
    }

    public static void tick(boolean start) {
        if (start) {
            startTime = System.currentTimeMillis();
            return;
        }

        index = 0;
        IWorker task = getNext();
        if (task == null) {
            return;
        }

        long time = 50 - (System.currentTimeMillis() - startTime);
        if (time < 10) {
            time = 10;
        }
        time += System.currentTimeMillis();

        while (System.currentTimeMillis() < time && task != null) {
            boolean again = task.doWork();

            if (!task.hasWork()) {
                remove(task);
                task = getNext();
            } else if (!again) {
                task = getNext();
            }
        }
    }

    public static synchronized void addWorker(IWorker worker) {
        WORKERS.add(worker);
    }

    private static synchronized IWorker getNext() {
        return WORKERS.size() > index ? WORKERS.get(index++) : null;
    }

    private static synchronized void remove(IWorker worker) {
        WORKERS.remove(worker);
        index--;
    }

    /** Dropped on server shutdown, so a worker cannot survive into the next world on an integrated server. */
    public static synchronized void clear() {
        WORKERS.clear();
    }

    public interface IWorker {

        boolean hasWork();

        /**
         * Perform a task. Returning true asks the manager to call this worker again this tick if
         * there is time left; returning false yields to the next worker until the next tick.
         */
        boolean doWork();
    }
}
