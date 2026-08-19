package com.github.alexmodguy.alexscaves.server.misc;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * A {@link DeferredRegister} that remembers each entry's {@link ResourceKey} while its supplier
 * runs — see {@link ACRegistryIds} for what reads it and why.
 *
 * <p>It is a wrapper rather than a subclass so that the ~700 existing {@code DEF_REG.register(name,
 * () -> new X())} call sites need no edit at all: the method signature mirrors the one it delegates
 * to, including the {@code <I extends T>} inference that lets a {@code Supplier<CaveBookItem>} be
 * assigned to a {@code Supplier<Item>} field. Only the two registries whose entries need an id use
 * it; every other {@code DeferredRegister} in the mod is untouched.
 *
 * <p>{@link #raw()} exposes the delegate for the one call each registry makes that is not an entry
 * registration — handing the register to the mod event bus.
 */
public final class ACDeferredRegister<T> {

    private final DeferredRegister<T> delegate;
    private final ResourceKey<? extends Registry<T>> registryKey;
    private final String namespace;

    private ACDeferredRegister(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        this.delegate = DeferredRegister.create(registryKey, namespace);
        this.registryKey = registryKey;
        this.namespace = namespace;
    }

    public static <T> ACDeferredRegister<T> create(ResourceKey<? extends Registry<T>> registryKey, String namespace) {
        return new ACDeferredRegister<>(registryKey, namespace);
    }

    public DeferredRegister<T> raw() {
        return delegate;
    }

    public <I extends T> Supplier<I> register(String name, Supplier<? extends I> supplier) {
        ResourceKey<T> id = ResourceKey.create(registryKey, ACIdFactories.of(namespace, name));
        return delegate.register(name, () -> ACRegistryIds.<I>constructing(id, supplier));
    }
}
