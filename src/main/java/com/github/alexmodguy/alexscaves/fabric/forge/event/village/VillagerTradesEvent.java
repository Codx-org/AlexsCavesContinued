package com.github.alexmodguy.alexscaves.fabric.forge.event.village;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;

import java.util.List;

/**
 * Fabric stand-in for "a villager profession's trade table is being assembled".
 *
 * <p>One handler: the cartographer's level-2 pool gains the underground-cabin map. The map is keyed
 * by profession level, which is why the trades arrive as an {@link Int2ObjectMap} of lists rather
 * than one list — level 2 is the journeyman tier, and appending to any other list would put the map
 * behind the wrong amount of villager XP.
 *
 * <p>⚠️ The profession is a {@code ResourceKey<VillagerProfession>} from 1.21.5, not the profession
 * object — 1.21.5 turned {@code VillagerProfession}'s constants into registry keys, and Forge's own
 * event moved with them (javap'd: 1.21.4 takes and returns the object, 1.21.5 the key). The stub has
 * to mirror that, because the handler in {@code ACVillagerTradeEvents} is shared with the loaders and
 * compares whatever this hands back against {@code VillagerProfession.CARTOGRAPHER}, whose type
 * changed on the same version.
 *
 * <p>The dispatcher builds this once per profession at server start and copies the result back into
 * vanilla's static trade table, which is where every villager reads it from. From MC 26 trades are
 * datapack entries and this whole file is out of the source set — see the class it stands in for.
 */
public class VillagerTradesEvent extends Event {

    private final Int2ObjectMap<List<VillagerTrades.ItemListing>> trades;
    //? if >=1.21.5 {
    /*private final net.minecraft.resources.ResourceKey<VillagerProfession> type;
    *///?} else {
    private final VillagerProfession type;
    //?}

    //? if >=1.21.5 {
    /*public VillagerTradesEvent(Int2ObjectMap<List<VillagerTrades.ItemListing>> trades, net.minecraft.resources.ResourceKey<VillagerProfession> type) {
    *///?} else {
    public VillagerTradesEvent(Int2ObjectMap<List<VillagerTrades.ItemListing>> trades, VillagerProfession type) {
    //?}
        this.trades = trades;
        this.type = type;
    }

    public Int2ObjectMap<List<VillagerTrades.ItemListing>> getTrades() {
        return trades;
    }

    //? if >=1.21.5 {
    /*public net.minecraft.resources.ResourceKey<VillagerProfession> getType() {
    *///?} else {
    public VillagerProfession getType() {
    //?}
        return type;
    }
}
