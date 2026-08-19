package com.github.alexmodguy.alexscaves.server.level.storage;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.entity.living.LuxtructosaurusEntity;
import com.github.alexmodguy.alexscaves.server.level.map.CaveBiomeMapWorldWorker;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import com.github.alexmodguy.alexscaves.server.level.map.ACWorldWorkerManager;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class ACWorldData extends SavedData {

    private static final String IDENTIFIER = "alexscaves_world_data";
    private Map<UUID, Integer> deepOneReputations = new HashMap<>();
    private boolean primordialBossDefeatedOnce = false;
    private long firstPrimordialBossDefeatTimestamp = -1;
    private Set<Integer> trackedLuxtructosaurusIds = new ObjectArraySet();

    private CaveBiomeMapWorldWorker lastMapWorker = null;

    private ACWorldData() {
        super();
    }

    public static ACWorldData get(Level world) {
        if (world instanceof ServerLevel) {
            ServerLevel overworld = world.getServer().getLevel(Level.OVERWORLD);
            DimensionDataStorage storage = overworld.getDataStorage();
            ACWorldData data = ACPlatform.computeIfAbsent(storage, ACWorldData::load, saved -> saved.acSave(new CompoundTag()), ACWorldData::new, IDENTIFIER);
            if (data != null) {
                data.setDirty();
            }
            return data;
        }
        return null;
    }

    public static ACWorldData load(CompoundTag nbt) {
        ACWorldData data = new ACWorldData();
        if (nbt.contains("DeepOneReputations")) {
            ListTag listtag = ACCompat.getList(nbt, "DeepOneReputations", 10);
            for (int i = 0; i < listtag.size(); ++i) {
                CompoundTag innerTag = ACCompat.getCompound(listtag, i);
                data.deepOneReputations.put(ACCompat.getUUID(innerTag, "UUID"), ACCompat.getInt(innerTag, "Reputation"));
            }
        }
        data.primordialBossDefeatedOnce = ACCompat.getBoolean(nbt, "PrimordialBossDefeatedOnce");
        data.firstPrimordialBossDefeatTimestamp = ACCompat.getLong(nbt, "FirstPrimordialBossDefeatTimestamp");
        data.trackedLuxtructosaurusIds = Arrays.stream(ACCompat.getIntArray(nbt, "TrackedLuxtructosaurusIds")).boxed().collect(Collectors.toSet());
        return data;
    }

    // 1.21.5 deleted SavedData#save — persistence is a Codec now (see ACPlatform#computeIfAbsent,
    // which folds this method and #load into one). The body therefore lives under a name vanilla
    // never had, and only the two-line override that hands it to the old API is gated. The
    // parameter name matters: the !mc205-saveddata-compound rule keys on it to add the registry
    // provider 1.20.5 gave the override, and that rule must not reach the body's own signature.
    //? if <1.21.5 {
    @Override
    public CompoundTag save(CompoundTag compound) {
        return acSave(compound);
    }
    //?}

    public CompoundTag acSave(CompoundTag compound) {
        if (!this.deepOneReputations.isEmpty()) {
            ListTag listTag = new ListTag();
            for (Map.Entry<UUID, Integer> reputations : deepOneReputations.entrySet()) {
                CompoundTag tag = new CompoundTag();
                ACCompat.putUUID(tag, "UUID", reputations.getKey());
                tag.putInt("Reputation", reputations.getValue());
                listTag.add(tag);
            }
            compound.put("DeepOneReputations", listTag);
        }
        compound.putBoolean("PrimordialBossDefeatedOnce", primordialBossDefeatedOnce);
        compound.putLong("FirstPrimordialBossDefeatTimestamp", firstPrimordialBossDefeatTimestamp);
        compound.putIntArray("TrackedLuxtructosaurusIds", trackedLuxtructosaurusIds.stream().mapToInt(Integer::intValue).toArray());
        return compound;
    }

    public int getDeepOneReputation(@Nullable UUID uuid) {
        return uuid == null ? 0 : deepOneReputations.getOrDefault(uuid, 0);
    }

    public void setDeepOneReputation(UUID uuid, int reputation) {
        deepOneReputations.put(uuid, Mth.clamp(reputation, -100, 100));
    }

    public boolean isPrimordialBossActive(Level level){
        for(int i : trackedLuxtructosaurusIds){
            if(level.getEntity(i) instanceof LuxtructosaurusEntity lux && lux.isAlive() && lux.isLoadedInWorld()){
                return true;
            }
        }
        return false;
    }

    public void trackPrimordialBoss(int id, boolean add){
        if(add){
            trackedLuxtructosaurusIds.add(id);
        }else{
            trackedLuxtructosaurusIds.remove(id);
        }
    }


    public boolean isPrimordialBossDefeatedOnce(){
        return primordialBossDefeatedOnce;
    }

    public void setPrimordialBossDefeatedOnce(boolean defeatedOnce){
        this.primordialBossDefeatedOnce = defeatedOnce;
    }

    public long getFirstPrimordialBossDefeatTimestamp(){
        return firstPrimordialBossDefeatTimestamp;
    }

    public void setFirstPrimordialBossDefeatTimestamp(long time){
        this.firstPrimordialBossDefeatTimestamp = time;
    }

    public void fillOutCaveMap(UUID uuid, ItemStack map, ServerLevel serverLevel, BlockPos center, Player player){
        if(lastMapWorker != null){
            lastMapWorker.onWorkComplete(lastMapWorker.getLastFoundBiome());
        }
        lastMapWorker = new CaveBiomeMapWorldWorker(map, serverLevel, center, player, uuid);
        ACWorldWorkerManager.addWorker(lastMapWorker);
    }

    public boolean isCaveMapTicking(){
        return lastMapWorker != null && lastMapWorker.hasWork();
    }

    // Drops every stale force-load ticket on world load. Only the signature diverges: both loaders'
    // TicketHelper has the same three methods, but the ticket maps have different value types (Forge
    // Pair<LongSet, LongSet>, NeoForge TicketSet), so the body reads keys only and never names
    // either. The List.copyOf is not decoration — removeAllTickets writes through to the very maps
    // being iterated.
    //
    // Forge 62.0.9 (26.1) deleted ForgeChunkManager outright, so the Forge arm stops below 26 and
    // this method simply does not exist there. Nothing is lost: registering it goes through
    // ACPlatform#registerForcedChunkCallback, which has been `forge && <1.21.5`-only ever since
    // Forge 55.x made its own ForgeChunkManager throw — i.e. on every Forge node above 1.21.4 this
    // was already dead code, and from 1.21.5 up this mod's tickets do not persist to begin with.
    // The body is duplicated rather than shared because an arm chain cannot host a shared body.
    //
    // Fabric has no arm either, for the same reason Forge >=26 does not: it never had a persistent
    // ticket registry to be handed back, so there is no callback to register and no signature to
    // write. Nothing forces a chunk across a restart on that loader.
    //? if neoforge {
    /*public static void clearLoadedChunksCallback(ServerLevel serverLevel, net.neoforged.neoforge.common.world.chunk.TicketHelper ticketHelper) {
        int i = 0;
        for(UUID owner : List.copyOf(ticketHelper.getEntityTickets().keySet())){
            ticketHelper.removeAllTickets(owner);
            i++;
        }
        for(BlockPos owner : List.copyOf(ticketHelper.getBlockTickets().keySet())){
            ticketHelper.removeAllTickets(owner);
            i++;
        }
        if(i > 0){
            AlexsCaves.LOGGER.debug("unloaded {} forced chunks", i);
        }
    }
    *///?} elif forge && <26 {
    public static void clearLoadedChunksCallback(ServerLevel serverLevel, net.minecraftforge.common.world.ForgeChunkManager.TicketHelper ticketHelper) {
        int i = 0;
        for(UUID owner : List.copyOf(ticketHelper.getEntityTickets().keySet())){
            ticketHelper.removeAllTickets(owner);
            i++;
        }
        for(BlockPos owner : List.copyOf(ticketHelper.getBlockTickets().keySet())){
            ticketHelper.removeAllTickets(owner);
            i++;
        }
        if(i > 0){
            AlexsCaves.LOGGER.debug("unloaded {} forced chunks", i);
        }
    }
    //?}
}
