package com.github.alexmodguy.alexscaves.mixin.client;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Public handles on the two camera movers this mod drives itself.
 *
 * <p>{@code move} is protected and {@code getMaxZoom} is private, and upstream reached both through
 * an access transformer. That stopped working at 1.21, which retyped the pair from {@code double} to
 * {@code float}: an AT entry names a member by its exact descriptor, a stale one is a silent no-op on
 * Forge and a hard error on NeoForge, and the file is a resource that no {@code //?} gate can reach.
 * An invoker moves the same problem into Java, where the version split can simply be written out —
 * and it needs no widening at all, so the Camera lines are gone from both AT files.
 *
 * <p>Call it through {@link com.github.alexmodguy.alexscaves.client.ACClientCompat#cameraMove} rather
 * than directly; that is where the {@code double} the call sites all speak meets whichever width the
 * game wants.
 */
@Mixin(Camera.class)
public interface CameraAccessor {

    //? if >=1.21 {
    /*@Invoker("move")
    void ac$move(float forwards, float up, float side);

    @Invoker("getMaxZoom")
    float ac$getMaxZoom(float startingDistance);
    *///?} else {
    @Invoker("move")
    void ac$move(double forwards, double up, double side);

    @Invoker("getMaxZoom")
    double ac$getMaxZoom(double startingDistance);
    //?}
}
