package com.github.alexmodguy.alexscaves.server.misc;

import net.minecraft.resources.ResourceKey;

import java.util.function.Supplier;

/**
 * The registry id of the block or item that is being constructed right now.
 *
 * <p><b>Why this exists.</b> 1.21.2 moved a block's loot table and a block's/item's description id
 * out of a lazy {@code BuiltInRegistries} lookup and into the {@code Properties} object, which must
 * therefore carry the id <i>before</i> the constructor runs — {@code BlockBehaviour.Properties}
 * and {@code Item.Properties} both gained {@code setId(ResourceKey)}, and both throw
 * {@code NullPointerException: Block id not set} / {@code Item id not set} without it.
 *
 * <p>Threading that key through by hand is not workable here. Alex's Caves has ~700 block and item
 * registrations, most of them built from a {@code Supplier} that takes no arguments and constructs
 * its own {@code Properties} deep inside a block class; and the ~30 shared {@code Properties}
 * constants in {@code ACBlockRegistry} are each reused by a dozen different blocks, so no one
 * {@code setId} call on them could be right.
 *
 * <p>So the id is passed out of band instead: {@link ACDeferredRegister} wraps every supplier in
 * {@link #constructing}, and a {@code >=1.21.2} mixin on each {@code Properties} class reads
 * {@link #pending()} and stamps it in. Deferred registration runs the suppliers one at a time, so
 * the value is unambiguous for exactly as long as one object is being built. Below 1.21.2 nothing
 * reads it and the push is a few hundred thread-local writes at startup.
 */
public final class ACRegistryIds {

    private static final ThreadLocal<ResourceKey<?>> PENDING = new ThreadLocal<>();

    private ACRegistryIds() {
    }

    /** Runs {@code supplier} with {@code id} visible to {@link #pending()}. */
    public static <T> T constructing(ResourceKey<?> id, Supplier<? extends T> supplier) {
        ResourceKey<?> previous = PENDING.get();
        PENDING.set(id);
        try {
            return supplier.get();
        } finally {
            if (previous == null) {
                PENDING.remove();
            } else {
                PENDING.set(previous);
            }
        }
    }

    /** The id of the object under construction on this thread, or null outside {@link #constructing}. */
    public static ResourceKey<?> pending() {
        return PENDING.get();
    }
}
