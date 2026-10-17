package com.github.alexmodguy.alexscaves.fabric.forge.fml.event.lifecycle;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;

/**
 * Fabric stand-in for the base of Forge's mod-loading lifecycle events — the ones whose defining
 * feature is that mod loading runs in PARALLEL, so anything touching shared game state has to be
 * handed back to the loading thread.
 *
 * <p><b>{@link #enqueueWork} runs the work immediately here, and that is correct rather than a
 * simplification.</b> Fabric loads mods one at a time on one thread: an entrypoint's
 * {@code onInitialize} is called serially, so there is no concurrency for the hand-back to protect
 * against and no other thread to hand back to. Running the runnable in place preserves the one
 * ordering guarantee the callers actually depend on — that everything enqueued from a given phase
 * has happened before the next phase begins — which on Forge comes from the loader draining the
 * work queue between phases.
 *
 * <p>Forge's version answers a {@code CompletableFuture<Void>}; this one answers nothing, because
 * no call site in this tree reads the result.
 */
public class ParallelDispatchEvent extends Event {

    public void enqueueWork(Runnable work) {
        work.run();
    }
}
