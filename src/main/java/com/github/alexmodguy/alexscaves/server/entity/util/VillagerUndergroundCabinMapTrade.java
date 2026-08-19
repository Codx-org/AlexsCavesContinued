package com.github.alexmodguy.alexscaves.server.entity.util;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACVanillaMapUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import javax.annotation.Nullable;

public class VillagerUndergroundCabinMapTrade implements VillagerTrades.ItemListing {
    private final int emeraldCost;
    private final int maxUses;
    private final int villagerXp;

    public VillagerUndergroundCabinMapTrade(int emeraldCost, int maxUses, int villagerXp) {
        this.emeraldCost = emeraldCost;
        this.maxUses = maxUses;
        this.villagerXp = villagerXp;
    }

    // 1.21.11 hands an ItemListing the ServerLevel outright instead of making it dig one out of the
    // trader. The body keeps doing the latter, so it stays one implementation on every node — the
    // two are the same level, and the instanceof is still what guards a client-side call.
    @Nullable
    //? if >=1.21.11
    /*public MerchantOffer getOffer(ServerLevel serverLevel, Entity entity, RandomSource randomSource) {*/
    //? if <1.21.11
    public MerchantOffer getOffer(Entity entity, RandomSource randomSource) {
        if (!(entity.level() instanceof ServerLevel)) {
            return null;
        } else {
            ServerLevel serverlevel = (ServerLevel)entity.level();
            BlockPos blockpos = serverlevel.findNearestMapStructure(ACTagRegistry.ON_UNDERGROUND_CABIN_MAPS, entity.blockPosition(), 100, true);
            if (blockpos != null) {
                ItemStack itemstack = MapItem.create(serverlevel, blockpos.getX(), blockpos.getZ(), (byte)2, true, true);
                MapItem.renderBiomePreviewMap(serverlevel, itemstack);
                MapItemSavedData.addTargetDecoration(itemstack, blockpos, "+", ACVanillaMapUtil.undergroundCabin());
                ACCompat.setHoverName(itemstack, Component.translatable("item.alexscaves.underground_cabin_explorer_map"));
                return new MerchantOffer(new ItemStack(Items.EMERALD, this.emeraldCost), new ItemStack(Items.COMPASS), itemstack, this.maxUses, this.villagerXp, 0.2F);
            } else {
                return null;
            }
        }
    }
}