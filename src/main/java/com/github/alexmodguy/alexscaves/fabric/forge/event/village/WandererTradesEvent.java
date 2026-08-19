package com.github.alexmodguy.alexscaves.fabric.forge.event.village;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;
import net.minecraft.world.entity.npc.VillagerTrades;

import java.util.List;

/**
 * Fabric stand-in for "the wandering trader's trade pools are being assembled".
 *
 * <p>One handler, and it uses the <b>generic</b> pool — the one every wandering trader draws from —
 * to offer the underground-cabin map. The rare pool is modelled because the loader event has it and
 * a mod that later wants a rare trade should not have to change this class's shape.
 *
 * <p>Forge 1.21.5 replaced both lists with a pool abstraction; that arm is loader-gated in the
 * handler, so Fabric always takes the two-list shape.
 */
public class WandererTradesEvent extends Event {

    private final List<VillagerTrades.ItemListing> generic;
    private final List<VillagerTrades.ItemListing> rare;

    public WandererTradesEvent(List<VillagerTrades.ItemListing> generic, List<VillagerTrades.ItemListing> rare) {
        this.generic = generic;
        this.rare = rare;
    }

    public List<VillagerTrades.ItemListing> getGenericTrades() {
        return generic;
    }

    public List<VillagerTrades.ItemListing> getRareTrades() {
        return rare;
    }
}
