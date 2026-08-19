package com.github.alexmodguy.alexscaves.server.misc;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationConfig;
import com.github.alexmodguy.alexscaves.server.item.ACItemRegistry;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CaveTabletLootModifier implements IGlobalLootModifier {

    private static final MapCodec<ResourceKey<Biome>> ENTRY_CODEC = ResourceKey.codec(Registries.BIOME).fieldOf("biome");

    // 1.20.5 retyped the global-loot-modifier registry (and IGlobalLootModifier#codec) to MapCodec.
    //? if >=1.20.5 {
    /*public static final Supplier<MapCodec<CaveTabletLootModifier>> CODEC = () ->
            RecordCodecBuilder.mapCodec(inst ->
    *///?} else {
    public static final Supplier<Codec<CaveTabletLootModifier>> CODEC = () ->
            RecordCodecBuilder.create(inst ->
    //?}
                    inst.group(
                                    ENTRY_CODEC.forGetter((configuration) -> configuration.biome),
                                    Codec.BOOL.fieldOf("replace").forGetter((configuration) -> configuration.replace),
                                    LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(lm -> lm.conditions)
                            )
                            .apply(inst, CaveTabletLootModifier::new));

    private final ResourceKey<Biome> biome;
    private final boolean replace;

    private final LootItemCondition[] conditions;

    private final Predicate<LootContext> orConditions;

    // public, not protected, so the Fabric dispatcher can build one — see ACFabricLootModifiers.
    // Forge and NeoForge only ever reach this through CODEC, which does not care either way.
    public CaveTabletLootModifier(ResourceKey<Biome> biome, boolean replace, LootItemCondition[] conditionsIn) {
        this.biome = biome;
        this.replace = replace;
        this.conditions = conditionsIn;
        this.orConditions = ACPlatform.orConditions(conditionsIn);
    }

    // Forge put the table being rolled at the head of the signature in 1.21.2, so that a modifier
    // can tell which one it is looking at; NeoForge did not. Neither of this mod's two cares — the
    // conditions the modifier is registered with have already answered that question — so the extra
    // argument is only accepted, never read.
    //? if forge && >=1.21.2 {
    /*@NotNull
    @Override
    public ObjectArrayList<ItemStack> apply(net.minecraft.world.level.storage.loot.LootTable lootTable, ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        return this.orConditions.test(context) ? this.doApply(generatedLoot, context) : generatedLoot;
    }
    *///?} else {
    @NotNull
    @Override
    public ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        return this.orConditions.test(context) ? this.doApply(generatedLoot, context) : generatedLoot;
    }
    //?}

    // public for the same reason as the constructor: the Fabric dispatcher calls THIS rather than
    // apply(), because on that loader the table-id selection apply()'s conditions encode has already
    // been made by the dispatcher's own map lookup — and an empty condition array would make
    // ACPlatform.orConditions return an always-FALSE predicate, i.e. a silent no-op.
    @Nonnull
    public ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() < getChance()) {
            if (replace) {
                generatedLoot.clear();
            }
            generatedLoot.add(getTablet());
        }
        return generatedLoot;
    }

    private float getChance() {
        if (biome == null || BiomeGenerationConfig.isBiomeDisabledCompletely(biome)) {
            return 0F;
        }
        if (biome.equals(ACBiomeRegistry.MAGNETIC_CAVES)) {
            return AlexsCaves.COMMON_CONFIG.magneticTabletLootChance.get().floatValue();
        }
        if (biome.equals(ACBiomeRegistry.PRIMORDIAL_CAVES)) {
            return AlexsCaves.COMMON_CONFIG.primordialTabletLootChance.get().floatValue();
        }
        if (biome.equals(ACBiomeRegistry.TOXIC_CAVES)) {
            return AlexsCaves.COMMON_CONFIG.toxicTabletLootChance.get().floatValue();
        }
        if (biome.equals(ACBiomeRegistry.ABYSSAL_CHASM)) {
            return AlexsCaves.COMMON_CONFIG.abyssalTabletLootChance.get().floatValue();
        }
        if (biome.equals(ACBiomeRegistry.FORLORN_HOLLOWS)) {
            return AlexsCaves.COMMON_CONFIG.forlornTabletLootChance.get().floatValue();
        }
        if (biome.equals(ACBiomeRegistry.CANDY_CAVITY)) {
            return AlexsCaves.COMMON_CONFIG.candyTabletLootChance.get().floatValue();
        }
        return 0F;
    }

    private ItemStack getTablet() {
        CompoundTag tag = new CompoundTag();
        ResourceKey<Biome> key = ACBiomeRegistry.MAGNETIC_CAVES;
        if (biome != null) {
            key = biome;
        }
        tag.putString("CaveBiome", key.location().toString());
        ItemStack stack = new ItemStack(ACItemRegistry.CAVE_TABLET.get());
        ACCompat.setTag(stack, tag);
        return stack;
    }

    // NeoForge 26.1.2 added an abstract int priority() to IGlobalLootModifier so that modifiers
    // over one table run in a defined order (lower runs first; DEFAULT_PRIORITY is 1000). Neither
    // of this mod's two shares a table with anything else it ships, so both take the default.
    //? if neoforge && >=26.1.2 {
    /*@Override
    public int priority() {
        return IGlobalLootModifier.DEFAULT_PRIORITY;
    }
    *///?}

    @Override
    //? if >=1.20.5 {
    /*public MapCodec<? extends IGlobalLootModifier> codec() {
    *///?} else {
    public Codec<? extends IGlobalLootModifier> codec() {
    //?}
        return CODEC.get();
    }
}
