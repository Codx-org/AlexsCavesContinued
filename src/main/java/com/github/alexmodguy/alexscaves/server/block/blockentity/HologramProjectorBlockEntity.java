package com.github.alexmodguy.alexscaves.server.block.blockentity;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;
import java.util.function.Function;

public class HologramProjectorBlockEntity extends BlockEntity implements ACUpdatePacketReceiver {

    public int tickCount;
    private EntityType entityType;
    private CompoundTag entityTag;
    private Entity displayEntity;
    private Entity prevDisplayEntity;
    private UUID displayUUID;
    private UUID prevDisplayUUID;
    private float prevSwitchProgress;
    private float switchProgress;
    private float previousRotation;
    private float rotation;

    private UUID lastPlayerUUID;

    public HologramProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(ACBlockEntityRegistry.HOLOGRAM_PROJECTOR.get(), pos, state);
    }

    public static void tick(Level level, BlockPos blockPos, BlockState state, HologramProjectorBlockEntity entity) {
        entity.tickCount++;
        entity.prevSwitchProgress = entity.switchProgress;
        entity.previousRotation = entity.rotation;
        if (entity.prevDisplayUUID != entity.displayUUID) {
            if (entity.switchProgress < 10.0F) {
                if (entity.switchProgress == 0) {
                    level.playSound((Player) null, blockPos, ACSoundRegistry.HOLOGRAM_STOP.get(), SoundSource.BLOCKS);
                }
                entity.switchProgress++;
            } else {
                entity.prevDisplayEntity = entity.displayEntity;
                entity.prevDisplayUUID = entity.displayUUID;
                entity.markUpdated();
            }
            if (!entity.isRemoved() && level.isClientSide()) {
                AlexsCaves.PROXY.playWorldSound(entity, (byte) 3);
            }
        }
        if (entity.isPlayerRender() && entity.lastPlayerUUID == null) {
            entity.lastPlayerUUID = ACCompat.getUUID(entity.entityTag, "UUID");
            entity.markUpdated();
        }
        if (entity.isPlayerRender()) {
            if(entity.lastPlayerUUID == null || entity.displayUUID == null || !entity.lastPlayerUUID.equals(entity.displayUUID)){
                entity.displayUUID = entity.lastPlayerUUID;
                entity.switchProgress = 0.0F;
            }
        } else if (entity.displayEntity != null) {
            if(entity.displayUUID != entity.displayEntity.getUUID()){
                entity.displayUUID = entity.displayEntity.getUUID();
                entity.switchProgress = 0.0F;
            }
        }
        float redstoneSignal = level.getBestNeighborSignal(blockPos) * 1F;
        if (redstoneSignal > 0.0F) {
            entity.rotation = entity.rotation + redstoneSignal;
        }
    }

    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("EntityType")) {
            String str = ACCompat.getString(tag, "EntityType");
            this.entityType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(str));
        }
        if (tag.contains("EntityTag")) {
            this.entityTag = ACCompat.getCompound(tag, "EntityTag");
        }
        this.rotation = ACCompat.getFloat(tag, "Rotation");
        if (tag.contains("LastPlayerUUID")) {
            this.lastPlayerUUID = ACCompat.getUUID(tag, "LastPlayerUUID");
        }
    }

    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.entityType != null) {
            tag.putString("EntityType", BuiltInRegistries.ENTITY_TYPE.getKey(this.entityType).toString());
        }
        if (this.entityTag != null) {
            tag.put("EntityTag", this.entityTag);
        }
        tag.putFloat("Rotation", this.rotation);
        if (lastPlayerUUID != null) {
            ACCompat.putUUID(tag, "LastPlayerUUID", lastPlayerUUID);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public AABB getRenderBoundingBox() {
        BlockPos pos = this.getBlockPos();
        float f = displayEntity == null ? 1.0F : Math.max(displayEntity.getBbWidth(), displayEntity.getBbHeight());
        return ACPlatform.encapsulating(pos.offset(-1, -1, -1), pos.offset(2, 2, 2)).inflate(Math.max(f - 0.5F, 1F));
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        if (packet != null && packet.getTag() != null) {
            if (packet.getTag().contains("EntityType")) {
                String str = ACCompat.getString(packet.getTag(), "EntityType");
                this.entityType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(str));
            }
            this.entityTag = ACCompat.getCompound(packet.getTag(), "EntityTag");
            this.rotation = ACCompat.getFloat(packet.getTag(), "Rotation");
            if (packet.getTag().contains("LastPlayerUUID")) {
                this.lastPlayerUUID = ACCompat.getUUID(packet.getTag(), "LastPlayerUUID");
            }
        }
    }

    public CompoundTag getUpdateTag() {
        CompoundTag compoundtag = new CompoundTag();
        if (this.entityType != null) {
            compoundtag.putString("EntityType", BuiltInRegistries.ENTITY_TYPE.getKey(this.entityType).toString());
        }
        if (this.entityTag != null) {
            compoundtag.put("EntityTag", this.entityTag);
        }
        compoundtag.putFloat("Rotation", this.rotation);
        if (lastPlayerUUID != null) {
            ACCompat.putUUID(compoundtag, "LastPlayerUUID", lastPlayerUUID);
        }
        return compoundtag;
    }

    public void setEntity(EntityType entityType, CompoundTag entityTag, float playerRot) {
        this.entityType = entityType;
        this.entityTag = entityTag;
        this.rotation = playerRot;
        displayEntity = null;
        lastPlayerUUID = null;
    }

    public boolean isPlayerRender() {
        return entityType == EntityType.PLAYER;
    }

    public UUID getLastPlayerUUID() {
        return isPlayerRender() ? lastPlayerUUID : null;
    }

    public Entity getDisplayEntity(Level level) {
        if (isPlayerRender()) {
            return null;
        }
        if (displayEntity == null && entityType != null) {
            // 1.21.2 made every construction path state why the entity is appearing. This one is
            // read straight back out of saved NBT, which is what LOAD means; nothing about the
            // hologram's copy is a spawn, and no spawn rule ever sees it.
            // 1.21.11 replaced the trailing Function<Entity, Entity> with a named EntityProcessor
            // interface, so the identity function no longer conforms. EntityProcessor.NOP is that
            // interface's own identity — it returns the entity it is handed.
            // 26.2 wrapped the reason in an EntitySpawnRequest, which pairs it with an
            // ignoreChecks flag. `false` is what vanilla's own reason-only overload still passes,
            // i.e. exactly the previous behaviour.
            //? if >=26.2 {
            /*displayEntity = EntityType.loadEntityRecursive(entityTag, level, new net.minecraft.world.entity.EntitySpawnRequest(net.minecraft.world.entity.EntitySpawnReason.LOAD, false), net.minecraft.world.entity.EntityProcessor.NOP);
            *///?} elif >=1.21.11 {
            /*displayEntity = EntityType.loadEntityRecursive(entityTag, level, net.minecraft.world.entity.EntitySpawnReason.LOAD, net.minecraft.world.entity.EntityProcessor.NOP);
            *///?} elif >=1.21.2 {
            /*displayEntity = EntityType.loadEntityRecursive(entityTag, level, net.minecraft.world.entity.EntitySpawnReason.LOAD, Function.identity());
            *///?} else {
            displayEntity = EntityType.loadEntityRecursive(entityTag, level, Function.identity());
            //?}
        }
        if (displayEntity == null && prevDisplayEntity != null) {
            return prevDisplayEntity;
        }
        return displayEntity;
    }

    public float getSwitchAmount(float partialTicks) {
        return (prevSwitchProgress + (switchProgress - prevSwitchProgress) * partialTicks) * 0.1F;
    }

    public float getRotation(float partialTicks) {
        return previousRotation + (rotation - previousRotation) * partialTicks;
    }

    private void markUpdated() {
        this.setChanged();
        this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }

    public void setRemoved() {
        AlexsCaves.PROXY.clearSoundCacheFor(this);
        level.playSound((Player) null, this.getBlockPos(), ACSoundRegistry.HOLOGRAM_STOP.get(), SoundSource.BLOCKS);
        super.setRemoved();
    }
}
