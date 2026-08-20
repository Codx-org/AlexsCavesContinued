package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.event.ACFabricVillagerTrades;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The wandering trader's half of {@link ACFabricVillagerTrades} — same substitution-at-the-read
 * shape as {@code VillagerTradesTableMixin}, but the field it replaces changed type twice, so this
 * one has three arms rather than two.
 *
 * <p>Below 1.21.5 {@code WANDERING_TRADER_TRADES} is an {@code Int2ObjectMap} of level → listings
 * and {@code updateTrades} reads it twice (generic pool then rare pool); from 1.21.5 it is a
 * {@code List} of weighted pools read once, and from 1.21.11 the owning class moved sub-package.
 * All three are the same substitution and all three are covered without an {@code ordinal}.
 */
//? if >=1.21.11 {
/*@Mixin(net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader.class)
*///?} else {
@Mixin(net.minecraft.world.entity.npc.WanderingTrader.class)
//?}
public class WandererTradesTableMixin {

    //? if >=1.21.11 {
    /*@com.llamalad7.mixinextras.injector.ModifyExpressionValue(
            method = "updateTrades",
            at = @At(value = "FIELD", opcode = org.objectweb.asm.Opcodes.GETSTATIC,
                    target = "Lnet/minecraft/world/entity/npc/villager/VillagerTrades;WANDERING_TRADER_TRADES:Ljava/util/List;"))
    @SuppressWarnings("rawtypes")
    private java.util.List ac_wandererTrades(java.util.List original) {
        return ACFabricVillagerTrades.wandererTrades(original);
    }
    *///?} elif >=1.21.5 {
    /*@com.llamalad7.mixinextras.injector.ModifyExpressionValue(
            method = "updateTrades",
            at = @At(value = "FIELD", opcode = org.objectweb.asm.Opcodes.GETSTATIC,
                    target = "Lnet/minecraft/world/entity/npc/VillagerTrades;WANDERING_TRADER_TRADES:Ljava/util/List;"))
    @SuppressWarnings("rawtypes")
    private java.util.List ac_wandererTrades(java.util.List original) {
        return ACFabricVillagerTrades.wandererTrades(original);
    }
    *///?} else {
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(
            method = "updateTrades",
            at = @At(value = "FIELD", opcode = org.objectweb.asm.Opcodes.GETSTATIC,
                    target = "Lnet/minecraft/world/entity/npc/VillagerTrades;WANDERING_TRADER_TRADES:Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;"))
    @SuppressWarnings("rawtypes")
    private it.unimi.dsi.fastutil.ints.Int2ObjectMap ac_wandererTrades(it.unimi.dsi.fastutil.ints.Int2ObjectMap original) {
        return ACFabricVillagerTrades.wandererTrades(original);
    }
    //?}
}
