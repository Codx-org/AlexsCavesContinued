package com.github.alexmodguy.alexscaves.server.item;

import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * An item that carries a client extension — in this mod, always either the custom item renderer or
 * the custom armour renderer.
 *
 * <p>Up to 1.21.1 both loaders asked the item itself: {@code IForgeItem#initializeClient} was a
 * default method every {@code Item} inherited, and the nineteen classes below simply overrode it.
 * NeoForge deleted that hook in 1.21.2 in favour of a mod-bus {@code RegisterClientExtensionsEvent},
 * where the extension is registered <em>for</em> an item rather than by it. Forge kept asking the
 * item, only moving the method off {@code IForgeItem} onto a patched {@code Item#initializeClient}
 * with the same signature — so nothing changes there.
 *
 * <p>Declaring the same method here keeps all nineteen bodies — and their {@code @Override}s —
 * identical on every version and both loaders: wherever the loader still declares the method the
 * override satisfies both it and this interface, and on NeoForge from 1.21.2 it satisfies only this
 * one, with {@code ClientProxy#registerClientExtensions} walking the item registry and handing each
 * implementor's consumer straight to the event.
 */
public interface ACClientExtensionItem {

    void initializeClient(Consumer<IClientItemExtensions> consumer);
}
