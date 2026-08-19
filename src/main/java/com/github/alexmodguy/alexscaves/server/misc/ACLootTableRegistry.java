package com.github.alexmodguy.alexscaves.server.misc;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
//? if <26
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
//? if !fabric
import net.minecraftforge.registries.ForgeRegistries;
import java.util.function.Supplier;

public class ACLootTableRegistry {

    // The global-loot-modifier register is hoisted out of the version chain below because the loader
    // difference and the version difference are independent and Stonecutter arms do not nest: on Forge
    // and NeoForge it is keyed by a loader-owned registry, and on Fabric there is no such registry — no
    // global-loot-modifier system exists there at all. The codecs it holds are dead weight on Fabric:
    // nothing there decodes a modifier from JSON, because ACFabricLootModifiers constructs the two
    // modifiers directly against a hardcoded (table id -> modifier) table — which is the selection the
    // modifiers' forge:loot_table_id conditions encode on the other loaders — and a roll-time mixin
    // runs them. The register is kept unregistered rather than gated away so the CODEC suppliers below
    // stay one shape on all three loaders. Declared before the entries that populate it.
    //? if fabric && >=1.20.5 {
    /*public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIER_DEF_REG = DeferredRegister.unregistered(AlexsCaves.MODID);
    *///?} elif >=1.20.5 {
    /*public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIER_DEF_REG = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, AlexsCaves.MODID);
    *///?} elif fabric {
    /*public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIER_DEF_REG = DeferredRegister.unregistered(AlexsCaves.MODID);
    *///?} else {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIER_DEF_REG = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, AlexsCaves.MODID);
    //?}

    // Two independent shifts land on the same declarations, so they share one four-way gate rather
    // than nesting (Stonecutter blocks cannot nest): 1.20.2 made LootItemFunctionType a record over a
    // Codec instead of a hand-written Serializer, and 1.20.5 retyped the global-loot-modifier registry
    // to MapCodec and made LootItemFunctionType generic in the function it deserialises.
    // 26 then deleted the wrapper altogether — Registries.LOOT_FUNCTION_TYPE holds the
    // MapCodec<? extends LootItemFunction> itself, so the register call hands over the codec directly.
    //? if >=26 {
    /*public static final DeferredRegister<MapCodec<? extends net.minecraft.world.level.storage.loot.functions.LootItemFunction>> LOOT_FUNCTION_DEF_REG = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, AlexsCaves.MODID);

    public static final Supplier<MapCodec<CaveTabletLootModifier>> CAVE_TABLET_LOOT_MODIFIER = GLOBAL_LOOT_MODIFIER_DEF_REG.register("cave_tablet", CaveTabletLootModifier.CODEC);
    public static final Supplier<MapCodec<CabinMapLootModifier>> CABIN_MAP_LOOT_MODIFIER = GLOBAL_LOOT_MODIFIER_DEF_REG.register("cabin_map", CabinMapLootModifier.CODEC);
    public static final Supplier<MapCodec<GummyColorLootFunction>> GUMMY_COLORS_LOOT_FUNCTION = LOOT_FUNCTION_DEF_REG.register("gummy_colors", () -> GummyColorLootFunction.CODEC);
    *///?} elif >=1.20.5 {
    /*public static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTION_DEF_REG = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, AlexsCaves.MODID);

    public static final Supplier<MapCodec<CaveTabletLootModifier>> CAVE_TABLET_LOOT_MODIFIER = GLOBAL_LOOT_MODIFIER_DEF_REG.register("cave_tablet", CaveTabletLootModifier.CODEC);
    public static final Supplier<MapCodec<CabinMapLootModifier>> CABIN_MAP_LOOT_MODIFIER = GLOBAL_LOOT_MODIFIER_DEF_REG.register("cabin_map", CabinMapLootModifier.CODEC);
    public static final Supplier<LootItemFunctionType<GummyColorLootFunction>> GUMMY_COLORS_LOOT_FUNCTION = LOOT_FUNCTION_DEF_REG.register("gummy_colors", () -> new LootItemFunctionType<>(GummyColorLootFunction.CODEC));
    *///?} elif >=1.20.2 {
    /*public static final DeferredRegister<LootItemFunctionType> LOOT_FUNCTION_DEF_REG = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, AlexsCaves.MODID);

    public static final Supplier<Codec<CaveTabletLootModifier>> CAVE_TABLET_LOOT_MODIFIER = GLOBAL_LOOT_MODIFIER_DEF_REG.register("cave_tablet", CaveTabletLootModifier.CODEC);
    public static final Supplier<Codec<CabinMapLootModifier>> CABIN_MAP_LOOT_MODIFIER = GLOBAL_LOOT_MODIFIER_DEF_REG.register("cabin_map", CabinMapLootModifier.CODEC);
    public static final Supplier<LootItemFunctionType> GUMMY_COLORS_LOOT_FUNCTION = LOOT_FUNCTION_DEF_REG.register("gummy_colors", () -> new LootItemFunctionType(GummyColorLootFunction.CODEC));
    *///?} else {
    public static final DeferredRegister<LootItemFunctionType> LOOT_FUNCTION_DEF_REG = DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, AlexsCaves.MODID);

    public static final Supplier<Codec<CaveTabletLootModifier>> CAVE_TABLET_LOOT_MODIFIER = GLOBAL_LOOT_MODIFIER_DEF_REG.register("cave_tablet", CaveTabletLootModifier.CODEC);
    public static final Supplier<Codec<CabinMapLootModifier>> CABIN_MAP_LOOT_MODIFIER = GLOBAL_LOOT_MODIFIER_DEF_REG.register("cabin_map", CabinMapLootModifier.CODEC);
    public static final Supplier<LootItemFunctionType> GUMMY_COLORS_LOOT_FUNCTION = LOOT_FUNCTION_DEF_REG.register("gummy_colors", () -> new LootItemFunctionType(new GummyColorLootFunction.Serializer()));
    //?}

    public static final ResourceLocation ABYSSAL_RUINS_CHEST = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "chests/abyssal_ruins");
    public static final ResourceLocation WITCH_HUT_CHEST = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "chests/witch_hut");
    public static final ResourceLocation LICOWITCH_TOWER_CHEST = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "chests/licowitch_tower");
    public static final ResourceLocation SECRET_LICOWITCH_TOWER_CHEST = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "chests/licowitch_tower_secret");
    public static final ResourceLocation GINGERBREAD_TOWN_CHEST = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "chests/gingerbread_town");

}
