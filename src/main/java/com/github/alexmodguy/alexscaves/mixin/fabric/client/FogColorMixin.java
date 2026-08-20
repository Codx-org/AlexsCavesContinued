package com.github.alexmodguy.alexscaves.mixin.fabric.client;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.github.alexmodguy.alexscaves.fabric.forge.client.event.ViewportEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric producer for {@link ViewportEvent.ComputeFogColor} — the loader event that lets a mod pick
 * the colour of the fog the camera is sitting in.
 *
 * <p>Nothing on Fabric ever posts a game-bus event, so {@code ClientEvents#fogColor} had never run
 * on any of the 22 Fabric nodes: the six cave biomes rendered with vanilla's fog, and standing in
 * acid, purple soda or primal magma looked like standing in air. That handler is the whole visual
 * identity of the biomes, which is why this is the first fog hook to be wired.
 *
 * <p><b>The target moves four times across the range, and it is a different CLASS above 1.21.6</b>
 * ({@code net.minecraft.client.renderer.FogRenderer} → {@code ...renderer.fog.FogRenderer}), so the
 * {@code @Mixin} annotation itself is gated — the shape this tree already uses for
 * {@code OutlineColorMixin} and {@code ShaderTransparencyMixin}. The four bands, read out of each
 * node's own jar with javap rather than assumed:
 *
 * <ul>
 *   <li><b>&lt;1.21.2</b> — {@code static void setupColor(...)}, which writes three
 *       {@code private static float} fields and then hands them to {@code RenderSystem.clearColor}
 *       as its very last instruction. So the injection has to write the fields <i>and</i> repeat
 *       that call; TAIL plus a second {@code clearColor} is exactly equivalent and needs no
 *       instruction-level anchor.</li>
 *   <li><b>&gt;=1.21.2 &amp;&amp; &lt;1.21.6</b> — {@code static Vector4f computeFogColor(...)},
 *       with no side effect left inside it (checked: no {@code clearColor} in the class), so
 *       modifying the return value is the whole job.</li>
 *   <li><b>&gt;=1.21.6 &amp;&amp; &lt;26.1</b> — the same method, now an <i>instance</i> method on
 *       the new class. Mixin matches a handler's static-ness against its target's, hence the split
 *       from the band above even though the body is identical.</li>
 *   <li><b>&gt;=26.1</b> — {@code void computeFogColor(..., Vector4f)}: the result is written into
 *       an out-parameter instead of returned, so this arm is an {@code @Inject} at TAIL that
 *       rewrites that vector in place.</li>
 * </ul>
 *
 * <p>⚠️ The 1.21.6 band's {@code computeFogColor} carries a trailing {@code boolean} that 1.21.11
 * drops. Capturing the target's parameters would therefore have split that band in two for a
 * difference the handler does not read, so the middle arm captures none of them and takes the
 * camera from {@code gameRenderer.getMainCamera()} — the very object vanilla passes in, since the
 * fog is only ever computed for the main camera. The bands that <i>do</i> mirror their parameters
 * do so because they need one (the out-vector) or because the list is stable across the whole band.
 */
//? if >=1.21.6 {
/*@Mixin(net.minecraft.client.renderer.fog.FogRenderer.class)
*///?} else {
@Mixin(net.minecraft.client.renderer.FogRenderer.class)
//?}
public class FogColorMixin {

    //? if <1.21.2 {
    @Shadow
    private static float fogRed;

    @Shadow
    private static float fogGreen;

    @Shadow
    private static float fogBlue;
    //?}

    //? if >=26.1 {
    /*@Inject(method = "computeFogColor", remap = true, at = @At(value = "TAIL"))
    private void ac_fabricComputeFogColor(Camera camera, float partialTicks, net.minecraft.client.multiplayer.ClientLevel level, int renderDistance, float darkenAmount, org.joml.Vector4f out, CallbackInfo ci) {
        ViewportEvent.ComputeFogColor event = new ViewportEvent.ComputeFogColor(camera, partialTicks, out.x, out.y, out.z);
        MinecraftForge.EVENT_BUS.post(event);
        out.set(event.getRed(), event.getGreen(), event.getBlue(), out.w);
    }
    *///?} elif >=1.21.6 {
    /*@com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "computeFogColor", remap = true, at = @At(value = "RETURN"))
    private org.joml.Vector4f ac_fabricComputeFogColor(org.joml.Vector4f original) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        ViewportEvent.ComputeFogColor event = new ViewportEvent.ComputeFogColor(camera, ACClientCompat.partialTick(), original.x, original.y, original.z);
        MinecraftForge.EVENT_BUS.post(event);
        return original.set(event.getRed(), event.getGreen(), event.getBlue(), original.w);
    }
    *///?} elif >=1.21.2 {
    /*@com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "computeFogColor", remap = true, at = @At(value = "RETURN"))
    private static org.joml.Vector4f ac_fabricComputeFogColor(org.joml.Vector4f original, Camera camera, float partialTicks, net.minecraft.client.multiplayer.ClientLevel level, int renderDistance, float darkenAmount) {
        ViewportEvent.ComputeFogColor event = new ViewportEvent.ComputeFogColor(camera, partialTicks, original.x, original.y, original.z);
        MinecraftForge.EVENT_BUS.post(event);
        return original.set(event.getRed(), event.getGreen(), event.getBlue(), original.w);
    }
    *///?} else {
    @Inject(method = "setupColor", remap = true, at = @At(value = "TAIL"))
    private static void ac_fabricComputeFogColor(Camera camera, float partialTicks, net.minecraft.client.multiplayer.ClientLevel level, int renderDistance, float darkenAmount, CallbackInfo ci) {
        ViewportEvent.ComputeFogColor event = new ViewportEvent.ComputeFogColor(camera, partialTicks, fogRed, fogGreen, fogBlue);
        MinecraftForge.EVENT_BUS.post(event);
        fogRed = event.getRed();
        fogGreen = event.getGreen();
        fogBlue = event.getBlue();
        com.mojang.blaze3d.systems.RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
    }
    //?}
}
