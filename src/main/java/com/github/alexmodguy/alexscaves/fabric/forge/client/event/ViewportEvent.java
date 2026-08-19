package com.github.alexmodguy.alexscaves.fabric.forge.client.event;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Cancelable;
import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;
import net.minecraft.client.Camera;

/**
 * Fabric stand-in for the four camera-and-atmosphere hooks the loader groups under one outer class.
 *
 * <p>All four are <b>received</b> only: {@code ClientEvents} tilts the camera for the shaking
 * effects, picks this mod's fog distances and colour for the fluid or block the camera sits in, and
 * widens the field of view underwater. So every one of them is a value the dispatcher hands in and
 * reads back out — there is no cancellation to honour except on {@link RenderFog}, which the mod
 * cancels to mean "these distances are final, do not let anything else move them".
 *
 * <p><b>{@code getPartialTick()} is a {@code double}</b>, not a float, on every loader that has this
 * event; four call sites cast it. Keeping the width is what makes those casts still say something.
 */
public class ViewportEvent extends Event {

    private final Camera camera;
    private final double partialTick;

    public ViewportEvent(Camera camera, double partialTick) {
        this.camera = camera;
        this.partialTick = partialTick;
    }

    public Camera getCamera() {
        return camera;
    }

    public double getPartialTick() {
        return partialTick;
    }

    /**
     * The camera's orientation, after vanilla has placed it.
     *
     * <p>Only the roll is modelled. The loader also carries yaw and pitch, and this mod writes
     * neither — a getter nothing reads is a value the dispatcher would have to invent, and inventing
     * one here would mean deciding what "the yaw vanilla would have used" is at a point in the frame
     * where that question has more than one answer.
     */
    public static class ComputeCameraAngles extends ViewportEvent {

        private float roll;

        public ComputeCameraAngles(Camera camera, double partialTick, float roll) {
            super(camera, partialTick);
            this.roll = roll;
        }

        public float getRoll() {
            return roll;
        }

        public void setRoll(float roll) {
            this.roll = roll;
        }
    }

    /**
     * The near and far fog planes for this frame.
     *
     * <p>The mode is the one member of this stub that could not be declared unconditionally:
     * 1.21.6 moved the fog off the CPU entirely and deleted {@code FogRenderer.FogMode} with it, so
     * the field, its getter and the constructor that takes it are gated {@code <1.21.6} — which
     * costs nothing, because from 1.21.6 up this mod reads the fog through the loader's own event
     * and never constructs this one.
     */
    @Cancelable
    public static class RenderFog extends ViewportEvent {

        //? if <1.21.6 {
        private final net.minecraft.client.renderer.FogRenderer.FogMode mode;
        //?}

        private float nearPlaneDistance;
        private float farPlaneDistance;

        //? if <1.21.6 {
        public RenderFog(Camera camera, double partialTick, net.minecraft.client.renderer.FogRenderer.FogMode mode,
                         float nearPlaneDistance, float farPlaneDistance) {
            super(camera, partialTick);
            this.mode = mode;
            this.nearPlaneDistance = nearPlaneDistance;
            this.farPlaneDistance = farPlaneDistance;
        }

        public net.minecraft.client.renderer.FogRenderer.FogMode getMode() {
            return mode;
        }
        //?} else {
        /*public RenderFog(Camera camera, double partialTick, float nearPlaneDistance, float farPlaneDistance) {
            super(camera, partialTick);
            this.nearPlaneDistance = nearPlaneDistance;
            this.farPlaneDistance = farPlaneDistance;
        }
        *///?}

        public float getNearPlaneDistance() {
            return nearPlaneDistance;
        }

        public void setNearPlaneDistance(float nearPlaneDistance) {
            this.nearPlaneDistance = nearPlaneDistance;
        }

        public float getFarPlaneDistance() {
            return farPlaneDistance;
        }

        public void setFarPlaneDistance(float farPlaneDistance) {
            this.farPlaneDistance = farPlaneDistance;
        }
    }

    /** The fog colour, as three separate channels — which is the shape the loader uses. */
    public static class ComputeFogColor extends ViewportEvent {

        private float red;
        private float green;
        private float blue;

        public ComputeFogColor(Camera camera, double partialTick, float red, float green, float blue) {
            super(camera, partialTick);
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public float getRed() {
            return red;
        }

        public void setRed(float red) {
            this.red = red;
        }

        public float getGreen() {
            return green;
        }

        public void setGreen(float green) {
            this.green = green;
        }

        public float getBlue() {
            return blue;
        }

        public void setBlue(float blue) {
            this.blue = blue;
        }
    }

    /** The field of view, in degrees. A {@code double} on the loader, and the call sites rely on it. */
    public static class ComputeFov extends ViewportEvent {

        private double fov;

        public ComputeFov(Camera camera, double partialTick, double fov) {
            super(camera, partialTick);
            this.fov = fov;
        }

        public double getFOV() {
            return fov;
        }

        public void setFOV(double fov) {
            this.fov = fov;
        }
    }
}
