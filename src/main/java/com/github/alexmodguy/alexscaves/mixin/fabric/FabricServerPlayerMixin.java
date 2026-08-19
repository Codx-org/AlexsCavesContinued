package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Carries the persistent player tag across a respawn — the half of
 * {@link ACCompat#getPersistentData} that a store cannot supply on its own.
 *
 * <p>Forge patches {@code restoreFrom} to copy the {@code PERSISTED_NBT_TAG} sub-tag from the dying
 * player onto the fresh one, unconditionally; surviving a death is the entire reason that sub-tag
 * exists, as against the rest of the bag. Fabric's stand-in store is the vendored Citadel
 * {@code LivingEntity} tag, which is saved and loaded faithfully but belongs to the <em>old</em>
 * entity, so without this the watcher's possession cooldown and the spelunkery table's tutorial flag
 * would both reset every time the player died.
 *
 * <p>Only that one sub-tag is copied, matching Forge exactly — everything else in the Citadel tag is
 * per-entity state that is meant to die with the entity. It runs regardless of {@code keepEverything}
 * for the same reason Forge's does.
 *
 * <p>{@code restoreFrom(ServerPlayer, boolean)} is byte-identical from 1.20.1 to 26.2, so this needs
 * no arms. TAIL is safe: vanilla's body only copies fields across and never reloads the new player
 * from NBT, so nothing downstream can overwrite what is written here.
 */
@Mixin(ServerPlayer.class)
public abstract class FabricServerPlayerMixin {

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void ac_copyPersistentData(ServerPlayer that, boolean keepEverything, CallbackInfo ci) {
        CompoundTag carried = ACCompat.getCompound(ACCompat.getPersistentData(that), ACCompat.PERSISTED_NBT_TAG);
        if (!carried.isEmpty()) {
            ACCompat.getPersistentData((ServerPlayer) (Object) this).put(ACCompat.PERSISTED_NBT_TAG, carried);
        }
    }
}
