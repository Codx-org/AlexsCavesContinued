package com.github.alexmodguy.alexscaves.server.event;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.entity.util.VillagerUndergroundCabinMapTrade;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/**
 * The two handlers that sell an underground-cabin explorer map — a cartographer's level-2 trade and
 * one of the wandering trader's generic offers.
 *
 * <p>They live apart from {@link CommonEvents} for a build reason rather than a design one. 26
 * deleted both of these events along with the whole notion of a code-registered trade, so the two
 * methods have to be gated out there — but {@code onWanderingTradeSetup} already carries a
 * {@code //? if forge &amp;&amp; >=1.21.5} arm inside it, and Stonecutter disables a multi-line arm by
 * wrapping it in a block comment, which cannot nest. A whole file the source set drops is the way
 * this tree expresses that (see {@code ModPlatformPlugin.configureJava}), so the pair moved here.
 *
 * <p><b>What replaces them from 26.</b> Trades became datapack registry entries —
 * {@code Registries.VILLAGER_TRADE} plus a {@code TradeSet} per profession level, whose contents are
 * a {@code HolderSet}, i.e. a tag. So the mod ships the two trades as
 * {@code data/alexscaves/villager_trade/underground_cabin_map*.json} and adds them to vanilla's
 * {@code minecraft:cartographer/level_2} and {@code minecraft:wandering_trader/common} trade tags.
 * The explorer map itself is no longer built by hand either: {@code minecraft:exploration_map} is a
 * loot function, and a trade may name loot functions to run over the item it gives.
 *
 * <p>⚠️ <b>The two config options do not apply from 26.</b> {@code cartographersSellCabinMaps} and
 * {@code wanderingTradersSellCabinMaps} exist because these handlers could read them; a datapack
 * file cannot. Neither loader kept a hook to add or remove a trade — Forge 62.0.9 and NeoForge
 * 26.1.0.19-beta ship only {@code TradeWithVillagerEvent}, which fires after the fact — so honouring
 * them again would mean mixing into vanilla's offer assembly for an option two booleans wide. A
 * datapack that empties {@code alexscaves:villager_trade/…} is the supported answer there.
 */
public class ACVillagerTradeEvents {

    @SubscribeEvent
    public void onVillagerTradeSetup(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.CARTOGRAPHER && AlexsCaves.COMMON_CONFIG.cartographersSellCabinMaps.get()) {
            int level = 2;
            List<VillagerTrades.ItemListing> list = event.getTrades().get(level);
            list.add(new VillagerUndergroundCabinMapTrade(5, 10, 6));
            event.getTrades().put(level, list);
        }
    }

    @SubscribeEvent
    public void onWanderingTradeSetup(WandererTradesEvent event) {
        if (AlexsCaves.COMMON_CONFIG.wanderingTradersSellCabinMaps.get()) {
            //? if forge && >=1.21.5 {
            /*// 55.x replaced the three named trade lists with a flat list of pools taken verbatim
            // from vanilla's WANDERING_TRADER_TRADES. Index 2 is the generic pool — that is the
            // same index the other loader reads to fill its getGenericTrades() list.
            event.getPools().get(2).getEntries().add(new VillagerUndergroundCabinMapTrade(8, 1, 10));
            *///?} else {
            event.getGenericTrades().add(new VillagerUndergroundCabinMapTrade(8, 1, 10));
            //?}
        }
    }
}
