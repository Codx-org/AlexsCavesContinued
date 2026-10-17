package com.github.alexmodguy.alexscaves.fabric.forge.client.event;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;

/**
 * Fabric stand-in for the two colour-handler registration phases.
 *
 * <p>Both nested classes take a sink rather than vanilla's colour registries: on this loader the
 * destination is a Fabric API call, and routing through a sink keeps the stub indifferent to which.
 *
 * <p>Every type below is fully qualified deliberately. Two of them move — the baked-colour interfaces
 * are replaced outright at 26 — and one of the arms here is compiled on nodes where the other's type
 * does not exist, so an import would be a hard error on exactly the nodes the gate exists to spare.
 */
public class RegisterColorHandlersEvent extends Event {

    /**
     * Item tints, up to 1.21.3.
     *
     * <p>1.21.4 moved a tint into the item's own model definition, so the whole phase is gone from
     * that version and this mod's five dynamic colours are computed by a mod-owned tint source
     * instead. Gated to match: above 1.21.3 the interface it registers no longer exists.
     */
    //? if <1.21.4 {
    public static class Item extends RegisterColorHandlersEvent {

        public interface Sink {
            void accept(net.minecraft.client.color.item.ItemColor itemColor, net.minecraft.world.level.ItemLike[] items);
        }

        private final Sink sink;

        public Item(Sink sink) {
            this.sink = sink;
        }

        public void register(net.minecraft.client.color.item.ItemColor itemColor, net.minecraft.world.level.ItemLike... items) {
            sink.accept(itemColor, items);
        }
    }
    //?}

    /**
     * Block tints, on every version — this mod tints two blocks and has done since 1.20.1.
     *
     * <p>26 replaced the tint-index callback with a list of tint sources, one entry per index. That
     * is a different argument type, not a different phase, so it is an arm here rather than a second
     * class; {@code ClientProxy} carries the matching pair.
     */
    public static class Block extends RegisterColorHandlersEvent {

        //? if >=26 {
        /*public interface Sink {
            void accept(java.util.List<net.minecraft.client.color.block.BlockTintSource> tintSources, net.minecraft.world.level.block.Block[] blocks);
        }

        private final Sink sink;

        public Block(Sink sink) {
            this.sink = sink;
        }

        public void register(java.util.List<net.minecraft.client.color.block.BlockTintSource> tintSources, net.minecraft.world.level.block.Block... blocks) {
            sink.accept(tintSources, blocks);
        }
        *///?} else {
        public interface Sink {
            void accept(net.minecraft.client.color.block.BlockColor blockColor, net.minecraft.world.level.block.Block[] blocks);
        }

        private final Sink sink;

        public Block(Sink sink) {
            this.sink = sink;
        }

        public void register(net.minecraft.client.color.block.BlockColor blockColor, net.minecraft.world.level.block.Block... blocks) {
            sink.accept(blockColor, blocks);
        }
        //?}
    }
}
