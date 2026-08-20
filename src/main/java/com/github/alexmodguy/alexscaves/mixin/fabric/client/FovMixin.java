package com.github.alexmodguy.alexscaves.mixin.fabric.client;

import com.github.alexmodguy.alexscaves.fabric.forge.client.event.ViewportEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fabric producer for {@link ViewportEvent.ComputeFov} — the field-of-view the world is drawn with.
 *
 * <p>Two consumers in this mod: a possessed entity ({@code PossessesCamera}) is always drawn at 90,
 * and a player riding a submarine has the vanilla underwater fov squeeze undone. Forge fires this
 * event from whatever method computes the final fov, and that method has moved once in this range.
 *
 * <p>Below 26 it is {@code GameRenderer#getFov(Camera, float, boolean)}, which returned a
 * {@code double} up to 1.21.1 and a {@code float} from 1.21.2 — the same method either way, so the
 * only difference between those two arms is the return type they modify.
 *
 * <p>From 26 the whole fov calculation moved onto {@link Camera}, and it is deliberately anchored at
 * {@code modifyFovBasedOnDeathOrFluid} rather than at {@code calculateFov}: the disassembly shows
 * {@code calculateFov} ends with that call and {@code calculateHudFov} is nothing but that call over
 * a constant 70, so the private helper is the single point that covers both the world fov and the
 * hud fov — which is exactly the pair {@code getFov(camera, partialTick, useFovSetting)} covered
 * below 26. Anchoring on {@code calculateFov} alone would leave the hud half unmodified, which is
 * not what Forge does.
 *
 * <p>The event carries {@code double}s (that is Forge's 1.20.1 shape and the stand-in mirrors it),
 * so the float-returning arms cast on the way out.
 */
//? if >=26 {
/*@Mixin(net.minecraft.client.Camera.class)
*///?} else {
@Mixin(net.minecraft.client.renderer.GameRenderer.class)
//?}
public class FovMixin {

    //? if >=26 {
    /*@com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "modifyFovBasedOnDeathOrFluid", remap = true, at = @At(value = "RETURN"))
    private float ac_fabricComputeFov(float original, float partialTick, float fov) {
        ViewportEvent.ComputeFov event = new ViewportEvent.ComputeFov((Camera) (Object) this, partialTick, original);
        MinecraftForge.EVENT_BUS.post(event);
        return (float) event.getFOV();
    }
    *///?} elif >=1.21.2 {
    /*@com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "getFov", remap = true, at = @At(value = "RETURN"))
    private float ac_fabricComputeFov(float original, Camera camera, float partialTicks, boolean useFovSetting) {
        ViewportEvent.ComputeFov event = new ViewportEvent.ComputeFov(camera, partialTicks, original);
        MinecraftForge.EVENT_BUS.post(event);
        return (float) event.getFOV();
    }
    *///?} else {
    @com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "getFov", remap = true, at = @At(value = "RETURN"))
    private double ac_fabricComputeFov(double original, Camera camera, float partialTicks, boolean useFovSetting) {
        ViewportEvent.ComputeFov event = new ViewportEvent.ComputeFov(camera, partialTicks, original);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getFOV();
    }
    //?}
}
