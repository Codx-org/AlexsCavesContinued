package com.github.alexmodguy.alexscaves.server.misc;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class CabinMapLootModifier implements IGlobalLootModifier {
    // 1.20.5 retyped the global-loot-modifier registry (and IGlobalLootModifier#codec) to MapCodec.
    //? if >=1.20.5 {
    /*public static final Supplier<MapCodec<CabinMapLootModifier>> CODEC = () ->
            RecordCodecBuilder.mapCodec(inst ->
    *///?} else {
    public static final Supplier<Codec<CabinMapLootModifier>> CODEC = () ->
            RecordCodecBuilder.create(inst ->
    //?}
                    inst.group(
                                    LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(lm -> lm.conditions)
                            )
                            .apply(inst, CabinMapLootModifier::new));

    private final LootItemCondition[] conditions;

    private final Predicate<LootContext> orConditions;

    // public, not protected, so the Fabric dispatcher can build one — see ACFabricLootModifiers.
    // Forge and NeoForge only ever reach this through CODEC, which does not care either way.
    public CabinMapLootModifier(LootItemCondition[] conditionsIn) {
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
        if (context.getRandom().nextFloat() < getChance() && context.hasParam(LootContextParams.ORIGIN)) {
            ServerLevel serverlevel = context.getLevel();
            BlockPos chestPos = BlockPos.containing(context.getParam(LootContextParams.ORIGIN));
            BlockPos blockpos = serverlevel.findNearestMapStructure(ACTagRegistry.ON_UNDERGROUND_CABIN_MAPS, chestPos, 100, true);
            if(blockpos != null){
                ItemStack itemstack = MapItem.create(serverlevel, blockpos.getX(), blockpos.getZ(), (byte)2, true, true);
                MapItem.renderBiomePreviewMap(serverlevel, itemstack);
                MapItemSavedData.addTargetDecoration(itemstack, blockpos, "+", ACVanillaMapUtil.undergroundCabin());
                ACCompat.setHoverName(itemstack, Component.translatable("item.alexscaves.underground_cabin_explorer_map"));
                generatedLoot.add(itemstack);
            }

        }
        return generatedLoot;
    }

    private float getChance() {
        return AlexsCaves.COMMON_CONFIG.cabinMapLootChance.get().floatValue();
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
