package com.github.alexmodguy.alexscaves.citadel;


import com.github.alexmodguy.alexscaves.citadel.server.entity.IDancesToJukebox;
import com.github.alexmodguy.alexscaves.citadel.server.event.EventChangeEntityTickRate;
import com.github.alexmodguy.alexscaves.citadel.server.tick.ServerTickRateTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

/**
 * Common-side half of the vendored Citadel runtime — upstream's {@code ServerProxy}.
 *
 * <p>Handlers that only mean something on a client are no-ops here and overridden in
 * {@link CitadelClientProxy}; the packet classes call through {@link Citadel#PROXY} either way.
 * The book-GUI and item-render-properties hooks upstream had are gone: Alex's Caves routes both
 * through its own {@code CommonProxy}.
 */
public class CitadelProxy {

    private static MinecraftServer minecraftServer;

    public CitadelProxy() {
    }

    public void handleAnimationPacket(int entityId, int index) {
    }

    public void handlePropertiesPacket(String propertyID, CompoundTag compound, int entityID) {
    }

    public void handleClientTickRatePacket(CompoundTag compound) {
    }

    public void handleJukeboxPacket(Level level, int entityId, BlockPos jukeBox, boolean dancing) {
        Entity entity = level.getEntity(entityId);
        if (entity instanceof IDancesToJukebox dancer) {
            dancer.setDancing(dancing);
            dancer.setJukeboxPos(dancing ? jukeBox : null);
        }
    }

    /**
     * Puts Citadel's game-bus listeners on the bus. The handlers deliberately live in their own
     * classes rather than on the proxy — see {@link CitadelEvents} for why the proxy cannot be a
     * listener itself.
     */
    public void registerEventHandlers() {
        //? if forge && >=1.21.6 {
        /*CitadelEvents.register();
        *///?} else {
        MinecraftForge.EVENT_BUS.register(new CitadelEvents());
        //?}
    }

    public boolean canEntityTickClient(Level level, Entity entity) {
        return true;
    }

    public boolean canEntityTickServer(Level level, Entity entity) {
        if (level instanceof ServerLevel serverLevel) {
            ServerTickRateTracker tracker = ServerTickRateTracker.getForServer(serverLevel.getServer());
            if (tracker.isTickingHandled(entity)) {
                return false;
            } else if (!tracker.hasNormalTickRate(entity)) {
                EventChangeEntityTickRate event = new EventChangeEntityTickRate(entity, tracker.getEntityTickLengthModifier(entity));
                if (EventChangeEntityTickRate.post(event)) {
                    return true;
                } else {
                    tracker.addTickBlockedEntity(entity);
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isGamePaused() {
        return false;
    }

    public Player getClientSidePlayer() {
        return null;
    }

    /**
     * The {@code IClientItemExtensions} used by {@code ItemCustomRender}. Returns null on a
     * dedicated server, where {@code Item#initializeClient} is never called.
     */
    @Nullable
    public Object getISTERProperties() {
        return null;
    }

    @Nullable
    public MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }

    public static void setMinecraftServer(MinecraftServer server) {
        minecraftServer = server;
    }
}
