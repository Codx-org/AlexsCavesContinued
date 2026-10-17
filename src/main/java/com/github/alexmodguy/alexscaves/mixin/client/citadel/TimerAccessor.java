package com.github.alexmodguy.alexscaves.mixin.client.citadel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * A setter for how long the client thinks a tick is — the slow-motion effect vendored from Citadel.
 *
 * <p>Upstream reached the field through an access transformer, and that cannot survive 1.21: the
 * class {@code net.minecraft.client.Timer} is gone (its replacement is nested, {@code
 * DeltaTracker.Timer}), and an AT entry naming a class that no longer exists is a hard error on
 * NeoForge. The AT file is a resource no {@code //?} gate can reach, so the widening moves here,
 * where the version split can be written out — the same reasoning as
 * {@link com.github.alexmodguy.alexscaves.mixin.client.CameraAccessor}.
 *
 * <p>On 1.21 and up there is nothing to set: the field is final and the timer asks
 * {@code Minecraft#getTickTargetMillis} for the length of each tick instead, which
 * {@link com.github.alexmodguy.alexscaves.mixin.client.MinecraftMixin} scales. The mixin still has
 * to name a target class there, so it names the timer it would have widened.
 */
//? if <1.21
@Mixin(net.minecraft.client.Timer.class)
//? if >=1.21
/*@Mixin(net.minecraft.client.DeltaTracker.Timer.class)*/
public interface TimerAccessor {

    //? if <1.21 {
    @Mutable
    @Accessor("msPerTick")
    void ac$setMsPerTick(float msPerTick);
    //?}
}
