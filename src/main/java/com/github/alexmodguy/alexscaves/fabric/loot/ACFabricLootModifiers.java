package com.github.alexmodguy.alexscaves.fabric.loot;

import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import com.github.alexmodguy.alexscaves.server.misc.CabinMapLootModifier;
import com.github.alexmodguy.alexscaves.server.misc.CaveTabletLootModifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Fabric's stand-in for Forge's global-loot-modifier system — the near half; the roll-time half is
 * {@code mixin.fabric.LootTableModifierMixin}.
 *
 * <p><b>What is being replaced.</b> Forge adds a {@code lootTableId} field to {@code LootTable} and
 * rewrites the tail of its private {@code getRandomItems(LootContext)} to
 * {@code return ForgeHooks.modifyLoot(getLootTableId(), list, context)}; the hook walks the modifiers
 * a data-driven manager decoded out of {@code data/forge/loot_modifiers/global_loot_modifiers.json}
 * and the seven {@code data/alexscaves/loot_modifiers/*.json} files it names. Fabric has none of
 * that: no modifier registry, no manager, no id on the table.
 *
 * <p><b>Why the seven files can be replaced by a hardcoded table, losing nothing.</b> Every one of
 * them carries nothing but {@code forge:loot_table_id} conditions — the modifiers ask no other
 * question. So the whole of their data content is a (table id → modifier) mapping, and this class is
 * that mapping written in Java. ⚠️ Note a modifier file's {@code conditions} array is an <em>OR</em>
 * list, so one file can name many tables: the seven name <b>seventeen</b> ids between them, two of
 * which belong to foreign mods ({@code betterwitchhuts}, {@code nova_structures}) and simply resolve
 * to nothing when those mods are absent. {@code DataPackMigration.dropForgeLootModifiers} drops all
 * eight files on this loader rather than shipping data no Fabric mechanism reads.
 *
 * <p><b>{@code doApply}, not {@code apply}.</b> {@code apply} gates on
 * {@code ACPlatform.orConditions(conditions)}, and all three of vanilla's or-condition helpers map an
 * <em>empty</em> array to an always-FALSE predicate — so handing the modifiers a degenerate condition
 * array and then calling {@code apply} would be a silent no-op on every band. The selection those
 * conditions encode has already been made by this class's own map lookup, so the gate is skipped
 * rather than faked. That is what both modifiers' constructor and {@code doApply} are public for.
 *
 * <p><b>Why {@code ALL_LOADED} and not {@code MODIFY}.</b> {@code MODIFY} is a load-time hook handing
 * over a {@code LootTable.Builder} — it can neither see a {@code LootContext} nor return rolled
 * items, so it cannot host {@code doApply} at all. It could only host a <em>registered</em> pool
 * entry or loot function, which would need a per-band codec/serializer shape across 1.20.1→26.2 for
 * no gain. Stamping the table's id onto the table from {@code REPLACE} does not work either:
 * fabric-api's {@code LootManagerMixin} rebuilds <em>every</em> table unconditionally
 * ({@code FabricLootTableBuilder.copyOf(table)} → fire {@code MODIFY} → {@code builder.build()} →
 * put), so anything written onto the instance handed to {@code REPLACE} is discarded. The rebuilt
 * map is {@code putfield}-ed <em>before</em> {@code ALL_LOADED} is invoked, which makes
 * {@code ALL_LOADED} the first hook that can see the final {@code LootTable} instances — hence
 * identity as the key, and no id on the table needed.
 *
 * <p>The resolved map is rebuilt from scratch on every reload and swapped in atomically, so a
 * {@code /reload} that removes or replaces a table cannot leave a stale instance behind.
 *
 * <p>⚠️ <b>This class was expected to stop compiling at 1.21, and it did not — the move it was
 * bracing for happened at 1.20.5 instead.</b> {@code LootDataType} and {@code LootDataManager} are
 * 1.20.4-and-below; the datapack registry that replaced them (and the {@code Registry<LootTable>}
 * {@code LootTableEvents.ALL_LOADED} hands over with it) arrived a version earlier than this note
 * predicted, and the {@code >=1.20.5} arm on {@link #register()} is what absorbed it. 1.21 changed
 * nothing here, and both Fabric nodes at and above it compile against the same arm.
 *
 * <p>The class is still deliberately ungated, for the original reason: a hard compile failure on the
 * first node that outgrows this shape is the intended outcome, because the alternative — an empty
 * Stonecutter arm — would ship a jar in which no chest anywhere gains a cave tablet and nothing says
 * so.
 */
public final class ACFabricLootModifiers {

    /** The shape both halves of the map speak in: the tail of a roll, as {@code doApply} has it. */
    private interface Modifier extends BiFunction<ObjectArrayList<ItemStack>, LootContext, ObjectArrayList<ItemStack>> {
    }

    /**
     * The condition array both modifiers are built with here. It is never consulted — see the
     * {@code doApply} note in the class javadoc — but the constructors want one, and a shared empty
     * array is cheaper than seven. Declared before {@link #TARGETS} because that field's initialiser
     * reads it.
     */
    private static final LootItemCondition[] NO_CONDITIONS = new LootItemCondition[0];

    /**
     * The seventeen (table id → modifier) pairs, read straight off the seven modifier JSONs.
     * {@code primordial_caves} is the only one whose {@code replace} flag is true — its five
     * archaeology tables yield the tablet <em>instead of</em> their normal loot, not alongside it.
     */
    private static final Map<ResourceLocation, Modifier> TARGETS = buildTargets();

    /** Rebuilt on every reload by the {@code ALL_LOADED} callback; read by the roll-time mixin. */
    private static volatile Map<LootTable, Modifier> resolved = Collections.emptyMap();

    private ACFabricLootModifiers() {
    }

    // 1.20.5 made loot tables a real datapack registry, so what the callback hands over stopped
    // being a LootDataManager keyed by (LootDataType, id) and became a plain Registry<LootTable>.
    // The two arms differ only in that lookup — same map, same identity keying, same volatile
    // handoff to the roll-time mixin — but the whole registration is duplicated rather than hoisting
    // the lookup into a helper, because a helper would have to name the manager's type in its own
    // signature and that type is exactly what moved. The lambda parameter is inferred, so neither
    // arm has to spell it.
    public static void register() {
        //? if >=1.20.5 {
        /*LootTableEvents.ALL_LOADED.register((resourceManager, lootData) -> {
            Map<LootTable, Modifier> byTable = new IdentityHashMap<>();
            TARGETS.forEach((id, modifier) ->
                    lootData.getOptional(id).ifPresent(table -> byTable.put(table, modifier)));
            resolved = byTable;
        });
        *///?} else {
        LootTableEvents.ALL_LOADED.register((resourceManager, lootData) -> {
            Map<LootTable, Modifier> byTable = new IdentityHashMap<>();
            TARGETS.forEach((id, modifier) ->
                    lootData.getElementOptional(LootDataType.TABLE, id).ifPresent(table -> byTable.put(table, modifier)));
            resolved = byTable;
        });
        //?}
    }

    /**
     * Called from the tail of {@code LootTable#getRandomItems(LootContext)}, where Forge calls
     * {@code ForgeHooks.modifyLoot}. Returns the list it was handed when this table has no modifier,
     * which is the overwhelmingly common case and the one worth keeping cheap.
     */
    public static ObjectArrayList<ItemStack> modifyLoot(LootTable table, ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        Modifier modifier = resolved.get(table);
        return modifier == null ? generatedLoot : modifier.apply(generatedLoot, context);
    }

    private static Map<ResourceLocation, Modifier> buildTargets() {
        Map<ResourceLocation, Modifier> map = new HashMap<>();

        CabinMapLootModifier cabinMap = new CabinMapLootModifier(NO_CONDITIONS);
        put(map, cabinMap::doApply, "minecraft:chests/abandoned_mineshaft");

        put(map, tablet(ACBiomeRegistry.ABYSSAL_CHASM, false),
                "minecraft:chests/underwater_ruin_big",
                "minecraft:chests/underwater_ruin_small",
                "minecraft:chests/buried_treasure");
        put(map, tablet(ACBiomeRegistry.CANDY_CAVITY, false),
                "alexscaves:chests/witch_hut",
                "betterwitchhuts:chests/hut_0",
                "nova_structures:chests/mangrove_witchhud");
        put(map, tablet(ACBiomeRegistry.FORLORN_HOLLOWS, false),
                "minecraft:chests/woodland_mansion");
        put(map, tablet(ACBiomeRegistry.MAGNETIC_CAVES, false),
                "minecraft:chests/bastion_treasure",
                "minecraft:chests/bastion_other",
                "minecraft:chests/bastion_bridge");
        put(map, tablet(ACBiomeRegistry.PRIMORDIAL_CAVES, true),
                "minecraft:archaeology/desert_well",
                "minecraft:archaeology/desert_pyramid",
                "minecraft:archaeology/trail_ruins_rare",
                "minecraft:archaeology/ocean_ruin_warm",
                "minecraft:archaeology/ocean_ruin_cold");
        put(map, tablet(ACBiomeRegistry.TOXIC_CAVES, false),
                "minecraft:chests/jungle_temple");

        return Map.copyOf(map);
    }

    private static Modifier tablet(ResourceKey<Biome> biome, boolean replace) {
        return new CaveTabletLootModifier(biome, replace, NO_CONDITIONS)::doApply;
    }

    private static void put(Map<ResourceLocation, Modifier> map, Modifier modifier, String... tableIds) {
        for (String tableId : tableIds) {
            map.put(ResourceLocation.parse(tableId), modifier);
        }
    }
}
