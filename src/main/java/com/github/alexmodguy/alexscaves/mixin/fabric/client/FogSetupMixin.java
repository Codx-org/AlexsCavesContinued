package com.github.alexmodguy.alexscaves.mixin.fabric.client;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.github.alexmodguy.alexscaves.fabric.forge.client.event.ViewportEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric producer for {@link ViewportEvent.RenderFog} — the near and far fog planes.
 *
 * <p>The companion to {@code FogColorMixin}: that one picks the colour of the fog, this one picks
 * how far you can see through it. {@code ClientEvents#acFogRender} is what makes acid, purple soda
 * and primal magma close in around the camera, what lets Deepsight push that back, and what applies
 * each cave biome's own fog nearness — none of which had ever run on a Fabric node, because nothing
 * on Fabric posts a game-bus event.
 *
 * <p><b>Which vanilla value <i>is</i> the near/far plane changes three times</b>, so this needs its
 * own arm chain rather than riding the colour mixin's:
 *
 * <ul>
 *   <li><b>&lt;1.21.2</b> — {@code setupFog} ends by pushing two floats into GL state through
 *       {@code RenderSystem.setShaderFogStart/End}. There is nothing to modify, so the injection
 *       reads them back at TAIL and writes them again — which is also exactly what the handler's own
 *       "band-aid" comment expects, since it reads its defaults from those same getters.</li>
 *   <li><b>&gt;=1.21.2 &amp;&amp; &lt;1.21.6</b> — {@code setupFog} returns a {@code FogParameters}
 *       record, so the whole job is rebuilding it with two components replaced.</li>
 *   <li><b>&gt;=1.21.6</b> — the fog left the CPU: it is a std140 block written into a GPU ring
 *       buffer, and the planes exist only as the {@code environmentalStart} / {@code environmentalEnd}
 *       fields of a {@code FogData}. That is the same pair NeoForge's own 21.6 event writes into,
 *       which is why {@code acFogRender}'s {@code >=1.21.6} arm reads its defaults straight off the
 *       event.</li>
 * </ul>
 *
 * <p>⚠️ The 1.21.6 band is split at <b>26.1</b> for the anchor, not for the values. Below it,
 * {@code FogData} is a local that {@code setupFog} unpacks field-by-field into a private
 * {@code updateBuffer} — the fields are already on the stack by then, so the six floats have to be
 * caught as call arguments ({@code @ModifyArgs}; argument order is
 * {@code envStart, envEnd, rdStart, rdEnd, skyEnd, cloudEnd}, read off the bytecode, <i>not</i> the
 * field declaration order, which differs). From 26.1 {@code setupFog} returns the {@code FogData}
 * and a public {@code updateBuffer(FogData)} consumes it, so the object itself can simply be edited
 * at HEAD. Both spellings are one injection and neither depends on {@code setupFog}'s own parameter
 * list, which moves twice inside this band for reasons the handler does not care about.
 *
 * <p>Cancellation is not modelled: the stand-in event is {@code @Cancelable} only so
 * {@code ClientEvents} can keep the one code path it shares with the loaders, and this mod is the
 * sole listener on the Fabric bus, so "stop later listeners" has nothing to stop.
 */
//? if >=1.21.6 {
/*@Mixin(net.minecraft.client.renderer.fog.FogRenderer.class)
*///?} else {
@Mixin(net.minecraft.client.renderer.FogRenderer.class)
//?}
public class FogSetupMixin {

    //? if >=26.1 {
    /*@Inject(method = "updateBuffer(Lnet/minecraft/client/renderer/fog/FogData;)V", remap = true, at = @At(value = "HEAD"))
    private void ac_fabricRenderFog(net.minecraft.client.renderer.fog.FogData data, CallbackInfo ci) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        ViewportEvent.RenderFog event = new ViewportEvent.RenderFog(camera, ACClientCompat.partialTick(), data.environmentalStart, data.environmentalEnd);
        MinecraftForge.EVENT_BUS.post(event);
        data.environmentalStart = event.getNearPlaneDistance();
        data.environmentalEnd = event.getFarPlaneDistance();
    }
    *///?} elif >=1.21.6 {
    /*@org.spongepowered.asm.mixin.injection.ModifyArgs(method = "setupFog", remap = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
    private void ac_fabricRenderFog(org.spongepowered.asm.mixin.injection.invoke.arg.Args args) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        ViewportEvent.RenderFog event = new ViewportEvent.RenderFog(camera, ACClientCompat.partialTick(), args.get(3), args.get(4));
        MinecraftForge.EVENT_BUS.post(event);
        args.set(3, event.getNearPlaneDistance());
        args.set(4, event.getFarPlaneDistance());
    }
    *///?} elif >=1.21.2 {
    /*@com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "setupFog", remap = true, at = @At(value = "RETURN"))
    private static net.minecraft.client.renderer.FogParameters ac_fabricRenderFog(net.minecraft.client.renderer.FogParameters original, Camera camera, net.minecraft.client.renderer.FogRenderer.FogMode mode, org.joml.Vector4f color, float renderDistance, boolean thickFog, float partialTicks) {
        ViewportEvent.RenderFog event = new ViewportEvent.RenderFog(camera, partialTicks, mode, original.start(), original.end());
        MinecraftForge.EVENT_BUS.post(event);
        return new net.minecraft.client.renderer.FogParameters(event.getNearPlaneDistance(), event.getFarPlaneDistance(), original.shape(), original.red(), original.green(), original.blue(), original.alpha());
    }
    *///?} else {
    @Inject(method = "setupFog", remap = true, at = @At(value = "TAIL"))
    private static void ac_fabricRenderFog(Camera camera, net.minecraft.client.renderer.FogRenderer.FogMode mode, float renderDistance, boolean thickFog, float partialTicks, CallbackInfo ci) {
        ViewportEvent.RenderFog event = new ViewportEvent.RenderFog(camera, partialTicks, mode, ACClientCompat.getShaderFogStart(), ACClientCompat.getShaderFogEnd());
        MinecraftForge.EVENT_BUS.post(event);
        com.mojang.blaze3d.systems.RenderSystem.setShaderFogStart(event.getNearPlaneDistance());
        com.mojang.blaze3d.systems.RenderSystem.setShaderFogEnd(event.getFarPlaneDistance());
    }
    //?}
}
