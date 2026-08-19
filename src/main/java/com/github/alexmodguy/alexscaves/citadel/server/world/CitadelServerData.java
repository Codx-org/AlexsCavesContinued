package com.github.alexmodguy.alexscaves.citadel.server.world;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;

import com.github.alexmodguy.alexscaves.citadel.server.tick.ServerTickRateTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class CitadelServerData extends SavedData {

    private static final String IDENTIFIER = "citadel_world_data";

    private final MinecraftServer server;

    private ServerTickRateTracker tickRateTracker = null;

    public CitadelServerData(MinecraftServer server) {
        super();
        this.server = server;
    }

    public CitadelServerData(MinecraftServer server, CompoundTag tag) {
        this(server);
        if (tag.contains("TickRateTracker")) {
            tickRateTracker = new ServerTickRateTracker(server, ACCompat.getCompound(tag, "TickRateTracker"));
        } else {
            tickRateTracker = new ServerTickRateTracker(server);
        }
    }

    @NotNull
    public static CitadelServerData get(MinecraftServer server) {
        DimensionDataStorage storage = server.getLevel(Level.OVERWORLD).getDataStorage();
        CitadelServerData data = ACPlatform.computeIfAbsent(storage, (tag) -> new CitadelServerData(server, tag), saved -> saved.acSave(new CompoundTag()), () -> new CitadelServerData(server), IDENTIFIER);
        data.setDirty();
        return data;
    }

    // See ACWorldData: 1.21.5 has no SavedData#save, so the body moved to acSave and only the
    // override that feeds the old API is gated.
    //? if <1.21.5 {
    @Override
    public CompoundTag save(CompoundTag tag) {
        return acSave(tag);
    }
    //?}

    public CompoundTag acSave(CompoundTag tag) {
        if (tickRateTracker != null) {
            tag.put("TickRateTracker", tickRateTracker.toTag());
        }
        return tag;
    }


    public ServerTickRateTracker getOrCreateTickRateTracker() {
        if (tickRateTracker == null) {
            tickRateTracker = new ServerTickRateTracker(server);
        }
        return tickRateTracker;
    }
}
