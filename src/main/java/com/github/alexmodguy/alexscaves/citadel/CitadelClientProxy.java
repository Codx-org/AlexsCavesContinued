package com.github.alexmodguy.alexscaves.citadel;

import com.github.alexmodguy.alexscaves.citadel.animation.IAnimatedEntity;
import com.github.alexmodguy.alexscaves.citadel.client.CitadelItemRenderProperties;
import com.github.alexmodguy.alexscaves.citadel.client.tick.ClientTickRateTracker;
import com.github.alexmodguy.alexscaves.citadel.server.entity.CitadelEntityData;
import com.github.alexmodguy.alexscaves.citadel.server.event.EventChangeEntityTickRate;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side half of the vendored Citadel runtime — upstream's {@code ClientProxy}, reduced to the
 * parts Alex's Caves depends on: the client tick-rate tracker, the entity animation/property
 * packets, and the pathfinding debug renderer.
 *
 * <p>Everything tied to Citadel-as-a-mod is gone: the Tabula "citadel model", the patreon follower
 * renderers and their config screen, the capes config screen, the guide-book GUI, the item
 * hover-animation bookkeeping, the rainbow-aura shader and the April Fools Tetris.
 */
public class CitadelClientProxy extends CitadelProxy {

    public CitadelClientProxy() {
        super();
    }

    @Override
    public void registerEventHandlers() {
        super.registerEventHandlers();
        //? if forge && >=1.21.6 {
        /*CitadelClientEvents.register();
        *///?} else {
        MinecraftForge.EVENT_BUS.register(new CitadelClientEvents());
        //?}
    }

    @Override
    public void handleAnimationPacket(int entityId, int index) {
        if (Minecraft.getInstance().level != null) {
            IAnimatedEntity entity = (IAnimatedEntity) Minecraft.getInstance().level.getEntity(entityId);
            if (entity != null) {
                if (index == -1) {
                    entity.setAnimation(IAnimatedEntity.NO_ANIMATION);
                } else {
                    entity.setAnimation(entity.getAnimations()[index]);
                }
                entity.setAnimationTick(0);
            }
        }
    }

    @Override
    public void handlePropertiesPacket(String propertyID, CompoundTag compound, int entityID) {
        if (compound == null || Minecraft.getInstance().level == null) {
            return;
        }
        Entity entity = Minecraft.getInstance().level.getEntity(entityID);
        if ((propertyID.equals("CitadelPatreonConfig") || propertyID.equals("CitadelTagUpdate")) && entity instanceof LivingEntity living) {
            CitadelEntityData.setCitadelTag(living, compound);
        }
    }

    @Override
    public void handleClientTickRatePacket(CompoundTag compound) {
        ClientTickRateTracker.getForClient(Minecraft.getInstance()).syncFromServer(compound);
    }

    @Override
    public boolean isGamePaused() {
        return Minecraft.getInstance().isPaused();
    }

    @Override
    public Player getClientSidePlayer() {
        return Minecraft.getInstance().player;
    }

    @Override
    public Object getISTERProperties() {
        return new CitadelItemRenderProperties();
    }

    @Override
    public boolean canEntityTickClient(Level level, Entity entity) {
        ClientTickRateTracker tracker = ClientTickRateTracker.getForClient(Minecraft.getInstance());
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
        return true;
    }

    @Nullable
    @Override
    public MinecraftServer getMinecraftServer() {
        return null;
    }
}
