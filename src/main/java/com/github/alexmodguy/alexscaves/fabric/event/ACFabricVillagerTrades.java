package com.github.alexmodguy.alexscaves.fabric.event;

import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import com.github.alexmodguy.alexscaves.fabric.forge.event.village.VillagerTradesEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.event.village.WandererTradesEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.entity.npc.VillagerTrades;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The producer for {@link VillagerTradesEvent} and {@link WandererTradesEvent} — the last of the
 * Forge game-bus events this loader had no source for, and the one that puts the underground-cabin
 * map back in the cartographer's and the wandering trader's stock on Fabric.
 *
 * <p><b>Why this is not simply a copy of Forge's dispatcher.</b> Forge's {@code
 * VillagerTradingManager} rebuilds the trade tables at {@code ServerAboutToStartEvent} and then
 * <em>writes the result back</em> into {@code VillagerTrades.TRADES} and {@code
 * VillagerTrades.WANDERING_TRADER_TRADES}, which is why it also has to keep a pristine deep copy of
 * vanilla's own tables in a {@code static {}} block — without one, a second world load in the same
 * process would post the event over its own previous output and the mod's trade would accumulate.
 * That route is closed here twice over: both fields are {@code public static final}, so a mod cannot
 * assign them without the loader patching vanilla, and from 1.21.5 {@code WANDERING_TRADER_TRADES}
 * holds an {@code ImmutableList} that cannot be mutated in place either.
 *
 * <p>So the tables are never written. This class computes a <em>merged</em> replacement once per
 * server start and two mixins hand it back at the point vanilla reads the field —
 * {@code mixin.fabric.VillagerTradesTableMixin} and {@code mixin.fabric.WandererTradesTableMixin},
 * both {@code @ModifyExpressionValue} on the {@code GETSTATIC}. Vanilla's tables therefore stay
 * permanently pristine, which gets Forge's snapshot property for free: every rebuild starts from the
 * same input, so reloading a singleplayer world cannot accumulate anything.
 *
 * <p>The merge is deliberately conservative. A profession is included in the override map only if a
 * listener actually changed one of its levels, and a level's array is rebuilt only if its contents
 * differ — compared by <em>reference</em>, which is exact here because the lists handed to listeners
 * hold vanilla's own listing instances. Everything untouched is the identical array object vanilla
 * built, so the ~20 professions no handler cares about behave byte-for-byte as they do without the
 * mod. {@code EXPERIMENTAL_TRADES} (the trade-rebalance experiment, off by default) is deliberately
 * left alone, exactly as Forge leaves it alone on 1.20.1.
 *
 * <p>⚠️ From MC 26 villager trades are datapack registry entries, both loader events are gone and
 * this file leaves the source set along with the two stand-ins and the consumer — see
 * {@code ACVillagerTradeEvents}.
 */
public final class ACFabricVillagerTrades {

    /**
     * Pool indices for the wandering trader from 1.21.5, where the two lists became three weighted
     * pools. Index 2 is the one drawn five times, i.e. the generic pool the {@code Int2ObjectMap}
     * held at key 1 below 1.21.5 — and the same index the Forge arm of {@code ACVillagerTradeEvents}
     * appends to, so the cabin map is offered from the same pool on every loader.
     *
     * <p>The old single rare pool (key 2, drawn once) has no exact successor: 1.21.5 replaced it
     * with two pools drawn twice each. Index 0 is the nearest equivalent and is what the stand-in
     * event's {@code getRareTrades()} maps onto; nothing in this mod adds to it, so the choice is
     * only ever visible to another mod listening on the Fabric side, and pool 1 is left alone.
     */
    //? if >=1.21.5 {
    /*private static final int GENERIC_POOL = 2;
    private static final int RARE_POOL = 0;
    *///?}

    /** Null whenever no listener changed anything, in which case the mixins return vanilla's own map. */
    @SuppressWarnings("rawtypes")
    private static volatile Map villagerOverride;

    //? if >=1.21.5 {
    /*@SuppressWarnings("rawtypes")
    private static volatile List wandererOverride;
    *///?} else {
    @SuppressWarnings("rawtypes")
    private static volatile Int2ObjectMap wandererOverride;
    //?}

    private ACFabricVillagerTrades() {
    }

    /**
     * Called from {@code AlexsCavesFabric}'s {@code SERVER_STARTING} handler, which is where Forge
     * fires {@code ServerAboutToStartEvent} and therefore where Forge's own dispatcher runs.
     */
    public static void loadTrades() {
        villagerOverride = buildVillagerTrades();
        wandererOverride = buildWandererTrades();
    }

    @SuppressWarnings("rawtypes")
    public static Map villagerTrades(Map vanilla) {
        Map override = villagerOverride;
        return override == null ? vanilla : override;
    }

    //? if >=1.21.5 {
    /*@SuppressWarnings("rawtypes")
    public static List wandererTrades(List vanilla) {
        List override = wandererOverride;
        return override == null ? vanilla : override;
    }
    *///?} else {
    @SuppressWarnings("rawtypes")
    public static Int2ObjectMap wandererTrades(Int2ObjectMap vanilla) {
        Int2ObjectMap override = wandererOverride;
        return override == null ? vanilla : override;
    }
    //?}

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map buildVillagerTrades() {
        Map vanilla = VillagerTrades.TRADES;
        Map merged = null;
        for (Object object : vanilla.entrySet()) {
            Map.Entry entry = (Map.Entry) object;
            Object profession = entry.getKey();
            Int2ObjectMap<VillagerTrades.ItemListing[]> original =
                    (Int2ObjectMap<VillagerTrades.ItemListing[]>) entry.getValue();

            // Levels 1..5 always exist, empty or not: a listener adds to the level it wants without
            // having to know whether vanilla gave that profession anything there. Forge does the same.
            Int2ObjectMap<List<VillagerTrades.ItemListing>> mutable = new Int2ObjectOpenHashMap<>();
            for (int level = 1; level <= 5; level++) {
                mutable.put(level, new ArrayList<>());
            }
            for (Int2ObjectMap.Entry<VillagerTrades.ItemListing[]> level : original.int2ObjectEntrySet()) {
                List<VillagerTrades.ItemListing> list = mutable.get(level.getIntKey());
                if (list == null) {
                    list = new ArrayList<>();
                    mutable.put(level.getIntKey(), list);
                }
                Collections.addAll(list, level.getValue());
            }

            // 1.21.5 turned the profession constants into registry keys and the loader event moved
            // with them; the stand-in mirrors that, so the cast is all that differs.
            //? if >=1.21.5 {
            /*MinecraftForge.EVENT_BUS.post(new VillagerTradesEvent(mutable,
                    (net.minecraft.resources.ResourceKey<net.minecraft.world.entity.npc.VillagerProfession>) profession));
            *///?} else {
            MinecraftForge.EVENT_BUS.post(new VillagerTradesEvent(mutable,
                    (net.minecraft.world.entity.npc.VillagerProfession) profession));
            //?}

            Int2ObjectMap<VillagerTrades.ItemListing[]> rebuilt = null;
            for (Int2ObjectMap.Entry<List<VillagerTrades.ItemListing>> level : mutable.int2ObjectEntrySet()) {
                List<VillagerTrades.ItemListing> after = level.getValue();
                if (unchanged(original.get(level.getIntKey()), after)) {
                    continue;
                }
                if (rebuilt == null) {
                    rebuilt = new Int2ObjectOpenHashMap<>(original);
                }
                rebuilt.put(level.getIntKey(), after.toArray(new VillagerTrades.ItemListing[0]));
            }
            if (rebuilt != null) {
                if (merged == null) {
                    merged = new HashMap(vanilla);
                }
                merged.put(profession, rebuilt);
            }
        }
        return merged;
    }

    //? if >=1.21.5 {
    /*@SuppressWarnings("rawtypes")
    private static List buildWandererTrades() {
        List<org.apache.commons.lang3.tuple.Pair<VillagerTrades.ItemListing[], Integer>> vanilla =
                VillagerTrades.WANDERING_TRADER_TRADES;
        // Defensive: the pool list is vanilla's own and has had three entries since 1.21.5, but an
        // index that does not exist would be a crash at server start rather than a missing trade.
        if (vanilla.size() <= GENERIC_POOL || vanilla.size() <= RARE_POOL) {
            return null;
        }
        List<VillagerTrades.ItemListing> generic = mutableCopy(vanilla.get(GENERIC_POOL).getLeft());
        List<VillagerTrades.ItemListing> rare = mutableCopy(vanilla.get(RARE_POOL).getLeft());
        MinecraftForge.EVENT_BUS.post(new WandererTradesEvent(generic, rare));

        boolean genericMoved = !unchanged(vanilla.get(GENERIC_POOL).getLeft(), generic);
        boolean rareMoved = !unchanged(vanilla.get(RARE_POOL).getLeft(), rare);
        if (!genericMoved && !rareMoved) {
            return null;
        }
        // Every other pool — and every pool's weight — is round-tripped untouched.
        List<org.apache.commons.lang3.tuple.Pair<VillagerTrades.ItemListing[], Integer>> rebuilt = new ArrayList<>(vanilla);
        if (genericMoved) {
            rebuilt.set(GENERIC_POOL, org.apache.commons.lang3.tuple.Pair.of(
                    generic.toArray(new VillagerTrades.ItemListing[0]), vanilla.get(GENERIC_POOL).getRight()));
        }
        if (rareMoved) {
            rebuilt.set(RARE_POOL, org.apache.commons.lang3.tuple.Pair.of(
                    rare.toArray(new VillagerTrades.ItemListing[0]), vanilla.get(RARE_POOL).getRight()));
        }
        return rebuilt;
    }
    *///?} else {
    @SuppressWarnings("rawtypes")
    private static Int2ObjectMap buildWandererTrades() {
        Int2ObjectMap<VillagerTrades.ItemListing[]> vanilla = VillagerTrades.WANDERING_TRADER_TRADES;
        // 1 is the generic pool every wandering trader draws five of; 2 is the single rare pick.
        List<VillagerTrades.ItemListing> generic = mutableCopy(vanilla.get(1));
        List<VillagerTrades.ItemListing> rare = mutableCopy(vanilla.get(2));
        MinecraftForge.EVENT_BUS.post(new WandererTradesEvent(generic, rare));

        boolean genericMoved = !unchanged(vanilla.get(1), generic);
        boolean rareMoved = !unchanged(vanilla.get(2), rare);
        if (!genericMoved && !rareMoved) {
            return null;
        }
        Int2ObjectMap<VillagerTrades.ItemListing[]> rebuilt = new Int2ObjectOpenHashMap<>(vanilla);
        if (genericMoved) {
            rebuilt.put(1, generic.toArray(new VillagerTrades.ItemListing[0]));
        }
        if (rareMoved) {
            rebuilt.put(2, rare.toArray(new VillagerTrades.ItemListing[0]));
        }
        return rebuilt;
    }
    //?}

    private static List<VillagerTrades.ItemListing> mutableCopy(VillagerTrades.ItemListing[] array) {
        List<VillagerTrades.ItemListing> list = new ArrayList<>();
        if (array != null) {
            Collections.addAll(list, array);
        }
        return list;
    }

    /**
     * Reference comparison, not {@code equals}: an {@code ItemListing} is an anonymous lambda-ish
     * object with no value semantics, and the lists handed to listeners hold vanilla's own
     * instances — so identity is exactly "no listener added, removed or reordered anything here".
     */
    private static boolean unchanged(VillagerTrades.ItemListing[] before, List<VillagerTrades.ItemListing> after) {
        if (before == null) {
            return after.isEmpty();
        }
        if (before.length != after.size()) {
            return false;
        }
        for (int i = 0; i < before.length; i++) {
            if (before[i] != after.get(i)) {
                return false;
            }
        }
        return true;
    }
}
