package com.github.alexmodguy.alexscaves.citadel.server.entity.pathfinding.raycoms;
/*
    All of this code is used with permission from Raycoms, one of the developers of the minecolonies project.
 */

import com.github.alexmodguy.alexscaves.citadel.Citadel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.BlockableEventLoop;
//? if <1.21.5 && !fabric
import net.minecraftforge.common.util.LogicalSidedProvider;
//? if <1.21.5 && !fabric
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;


/**
 * Static class the handles all the Pathfinding.
 */
public final class Pathfinding {
    private static final BlockingQueue<Runnable> jobQueue = new LinkedBlockingDeque<>();
    private static ThreadPoolExecutor executor;

    private Pathfinding() {
        //Hides default constructor.
    }

    public static boolean isDebug() {
        return PathfindingConstants.isDebugMode;
    }

    /**
     * Creates a new thread pool for pathfinding jobs
     *
     * @return the threadpool executor.
     */
    public static ThreadPoolExecutor getExecutor() {
        if (executor == null) {
            executor = new ThreadPoolExecutor(1, PathfindingConstants.pathfindingThreads, 10, TimeUnit.SECONDS, jobQueue, new CitadelThreadFactory());
        }
        return executor;
    }

    /**
     * Ice and Fire specific thread factory.
     */
    public static class CitadelThreadFactory implements ThreadFactory {
        /**
         * Ongoing thread IDs.
         */
        public static int id;

        @Override
        public Thread newThread(final @NotNull Runnable runnable) throws RuntimeException {
            // 1.21.5's loaders deleted LogicalSidedProvider. What it is asked for here is always the
            // server work queue, i.e. the running MinecraftServer, and ServerLifecycleHooks answers
            // that directly. It is null when no server exists yet — in which case the calling
            // thread's own loader is the right answer, which is what the isSameThread() branch
            // below produced anyway.
            //
            // Fabric takes the same arm on every version, because it never had the sided provider
            // either — one stand-in (ServerLifecycleHooks, which this tree needs for three other
            // call sites regardless) rather than the two this line would otherwise ask for.
            //? if >=1.21.5 || fabric {
            /*BlockableEventLoop<?> workqueue = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            *///?} else {
            BlockableEventLoop<?> workqueue = LogicalSidedProvider.WORKQUEUE.get(LogicalSide.SERVER);
            //?}
            ClassLoader classLoader;
            if (workqueue == null || workqueue.isSameThread()) {
                classLoader = Thread.currentThread().getContextClassLoader();
            } else if (workqueue instanceof MinecraftServer server) {
                classLoader = server.getRunningThread().getContextClassLoader();
            } else {
                classLoader = CompletableFuture.supplyAsync(() -> Thread.currentThread().getContextClassLoader(), workqueue).orTimeout(10, TimeUnit.SECONDS).exceptionally((ex) -> {
                    throw new RuntimeException(String.format("Couldn't join threads within timeout range. Tried joining '%s' on '%s'", Thread.currentThread().getName(), workqueue.name()));
                }).join();
            }
            final Thread thread = new Thread(runnable, "Citadel Pathfinding Worker #" + (id++));
            thread.setDaemon(true);
            thread.setPriority(Thread.MAX_PRIORITY);
            if (thread.getContextClassLoader() != classLoader) {
                Citadel.LOGGER.info("Corrected CCL of new Citadel Pathfinding Thread, was: " + thread.getContextClassLoader().toString());
                thread.setContextClassLoader(classLoader);
            }
            thread.setUncaughtExceptionHandler((thread1, throwable) -> Citadel.LOGGER.error("Citadel Pathfinding Thread errored! ", throwable));
            return thread;
        }
    }
}
