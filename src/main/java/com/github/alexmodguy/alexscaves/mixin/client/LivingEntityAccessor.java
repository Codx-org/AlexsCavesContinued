package com.github.alexmodguy.alexscaves.mixin.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Reads an entity's save data into an entity the caller does not own.
 *
 * <p>The cave book's entity widget builds a display mob and then replays a stashed tag onto it —
 * the one place in the mod that calls {@code readAdditionalSaveData} on somebody else's entity
 * (the six bucketable fish call it on {@code this}, which needs nothing). That was legal up to
 * 1.21.5, where {@code LivingEntity#readAdditionalSaveData} is {@code public}; 1.21.6 narrowed it
 * back to {@code protected} along with the {@code ValueInput} rewrite.
 *
 * <p>An invoker rather than an access-transformer entry, for the reasons {@link CameraAccessor}
 * sets out: an AT line matches by exact descriptor, so this one would need two spellings, and no
 * {@code //?} gate can reach a resource file — a stale entry is a silent no-op on Forge and a hard
 * error on NeoForge. {@code Entity#load} is public on 1.21.6 and looks like the way out, but it is
 * not the same method: it resets position, motion and rotation, clears the entity's tags and reads
 * a UUID, none of which the widget wants.
 *
 * <p>Both arms are declared, so the call site needs no gate of its own — {@code ACCompat.asInput}
 * is the identity below 1.21.6 and hands back the tag unchanged.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    //? if >=1.21.6 {
    /*@Invoker("readAdditionalSaveData")
    void ac_readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input);
    *///?} else {
    @Invoker("readAdditionalSaveData")
    void ac_readAdditionalSaveData(CompoundTag tag);
    //?}
}
