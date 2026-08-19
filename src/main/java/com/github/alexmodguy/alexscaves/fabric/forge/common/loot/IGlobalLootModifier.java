package com.github.alexmodguy.alexscaves.fabric.forge.common.loot;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Fabric stand-in for Forge's global-loot-modifier interface.
 *
 * <p><b>What the mod uses it for.</b> Two classes implement it — {@code CaveTabletLootModifier} and
 * {@code CabinMapLootModifier} — and between them they add a cave tablet to six vanilla structure
 * chests and an underground-cabin map to mineshaft chests. Both are pure "append to this table's
 * roll" modifiers; neither reads anything the loader hands it beyond the {@link LootContext}.
 *
 * <p><b>What is reproduced and what is not.</b> Everything the three consumers name: the
 * {@link #LOOT_CONDITIONS_CODEC} their record codecs build a {@code conditions} field from,
 * {@link #apply} and {@link #codec()}. Forge's {@code DIRECT_CODEC} (the registry-dispatching codec
 * its loot-modifier manager decodes files with) and its {@code DEFAULT_PRIORITY} are deliberately
 * absent: the first belongs to the manager this loader does not have, and the second is named only
 * inside a {@code neoforge && >=26.1.2} arm, which is comment text on every node of this loader.
 *
 * <p><b>How the modifiers actually reach a loot table on Fabric.</b> Not through this interface, and
 * not through {@link #apply} either — see {@code fabric.loot.ACFabricLootModifiers}, which owns the
 * whole argument. In outline: all seven modifier files carry nothing but {@code forge:loot_table_id}
 * predicates, so their entire data content is a (table id → modifier) mapping, and that mapping is
 * written in Java there. A {@code LootTableEvents.ALL_LOADED} callback resolves the seventeen ids to
 * live {@code LootTable} instances after every reload, and a mixin at the tail of the private
 * {@code LootTable#getRandomItems(LootContext)} — the same method Forge rewrites — looks the table up
 * by identity and runs the modifier's {@code doApply}. ⚠️ {@code doApply}, not {@link #apply}: the
 * latter gates on an or-condition over the condition array, and every band's or-helper maps an
 * <em>empty</em> array to an always-FALSE predicate, so faking the conditions away would be a silent
 * no-op. The seven {@code data/alexscaves/loot_modifiers/*.json} files and
 * {@code data/forge/loot_modifiers/global_loot_modifiers.json} are Forge-format data no Fabric
 * mechanism reads, and {@code DataPackMigration.dropForgeLootModifiers} drops them on this loader
 * rather than shipping dead weight.
 *
 * <p>That is also why {@code ACLootTableRegistry}'s register is an
 * {@linkplain com.github.alexmodguy.alexscaves.fabric.registries.DeferredRegister#unregistered
 * unregistered} one here: nothing on this loader ever looks a modifier up by id, because the file
 * that would have named one by id is not read.
 */
public interface IGlobalLootModifier {

    // Forge builds this three different ways over the range, because vanilla's own condition codec
    // moved twice and did not exist at all to begin with (read out of forge-universal's <clinit> on
    // 1.20.1, 1.20.2, 1.20.6 and 26.2, and cross-checked against each vanilla jar):
    //
    //   <1.20.2          no codec exists — loot conditions are Gson-only, so the bridge below is
    //                    what Forge does too, over vanilla's own condition Gson adapter
    //   >=1.20.2 <1.20.5 LootItemConditions.CODEC
    //   >=1.20.5 <1.21   LootItemConditions.DIRECT_CODEC — 1.20.5 wrapped conditions in Holders and
    //                    the plain CODEC became the Holder-taking one
    //   >=1.21           LootItemCondition.DIRECT_CODEC — the same codec, moved onto the interface
    //
    // The array/list conversion is Forge's, verbatim: a list codec xmapped both ways.
    //? if >=1.21 {
    /*Codec<LootItemCondition[]> LOOT_CONDITIONS_CODEC = LootItemCondition.DIRECT_CODEC.listOf()
            .xmap(list -> list.toArray(new LootItemCondition[0]), java.util.Arrays::asList);
    *///?} elif >=1.20.5 {
    /*Codec<LootItemCondition[]> LOOT_CONDITIONS_CODEC = net.minecraft.world.level.storage.loot.predicates.LootItemConditions.DIRECT_CODEC.listOf()
            .xmap(list -> list.toArray(new LootItemCondition[0]), java.util.Arrays::asList);
    *///?} elif >=1.20.2 {
    /*Codec<LootItemCondition[]> LOOT_CONDITIONS_CODEC = net.minecraft.world.level.storage.loot.predicates.LootItemConditions.CODEC.listOf()
            .xmap(list -> list.toArray(new LootItemCondition[0]), java.util.Arrays::asList);
    *///?} else {
    Codec<LootItemCondition[]> LOOT_CONDITIONS_CODEC = Codec.PASSTHROUGH.comapFlatMap(
            dynamic -> {
                try {
                    return com.mojang.serialization.DataResult.success(
                            ConditionGson.INSTANCE.fromJson(asJson(dynamic), LootItemCondition[].class));
                } catch (com.google.gson.JsonSyntaxException e) {
                    return com.mojang.serialization.DataResult.error(e::getMessage);
                }
            },
            conditions -> new com.mojang.serialization.Dynamic<>(com.mojang.serialization.JsonOps.INSTANCE,
                    ConditionGson.INSTANCE.toJsonTree(conditions)));

    // An interface may not hold a private field, so the Gson lives in a holder — which also makes it
    // lazy, so a node that never decodes a modifier never builds it.
    class ConditionGson {
        static final com.google.gson.Gson INSTANCE =
                net.minecraft.world.level.storage.loot.Deserializers.createConditionSerializer().create();
    }

    // Generic in the dynamic's own type so getValue() and getOps() agree — calling it with a
    // Dynamic<?> capture-converts at the call site, which is the only shape that compiles.
    private static <T> com.google.gson.JsonElement asJson(com.mojang.serialization.Dynamic<T> dynamic) {
        return dynamic.getValue() instanceof com.google.gson.JsonElement json
                ? json
                : dynamic.getOps().convertTo(com.mojang.serialization.JsonOps.INSTANCE, dynamic.getValue());
    }
    //?}

    /**
     * Forge put the table being rolled at the head of this signature in 1.21.2 and NeoForge did not;
     * this loader follows vanilla's shape, which is the one both implementations already declare in
     * their non-Forge arm.
     */
    ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> generatedLoot, LootContext context);

    // 1.20.5 retyped the global-loot-modifier registry, and this with it.
    //? if >=1.20.5 {
    /*com.mojang.serialization.MapCodec<? extends IGlobalLootModifier> codec();
    *///?} else {
    Codec<? extends IGlobalLootModifier> codec();
    //?}
}
