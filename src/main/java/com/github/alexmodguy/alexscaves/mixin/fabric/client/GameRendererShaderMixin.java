package com.github.alexmodguy.alexscaves.mixin.fabric.client;

import com.github.alexmodguy.alexscaves.fabric.ModBus;
import com.github.alexmodguy.alexscaves.fabric.forge.client.event.RegisterShadersEvent;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

/**
 * Fabric's stand-in for the point at which Forge fires {@code RegisterShadersEvent}.
 *
 * <p>Fabric API does have a core-shader hook — {@code CoreShaderRegistrationCallback} — and it is
 * deliberately not used, because it does not expose the {@code ResourceProvider}. That provider is
 * the first argument of every {@code ShaderInstance} this mod constructs, so a registration path
 * without it is not a path at all; the callback hands out a context that can only build a shader
 * from an id, which loses the vertex format the eight programs each name. Injecting into
 * {@code reloadShaders} recovers both the provider (it is that method's own parameter) and the very
 * list Forge's hook appends to.
 *
 * <p><b>The anchor and the one instruction of difference.</b> Vanilla builds a local
 * {@code List<Pair<ShaderInstance, Consumer<ShaderInstance>>>} of every core shader, inside a
 * {@code try} that catches {@code IOException}, and then — after the {@code catch} — calls
 * {@code shutdownShaders()} and drains the list into the shader map. Forge splices its event fire in
 * as the last statement <em>inside</em> the try; this injects at the {@code shutdownShaders()} call,
 * which is the first instruction <em>after</em> it. So an {@code IOException} thrown while building a
 * mod shader would become a crash here where Forge turns it into "could not reload shaders". That
 * costs this mod nothing — {@code ClientProxy#registerShaders} catches {@code IOException} itself on
 * exactly this version band — and it buys an anchor that is one distinctive call rather than an
 * ordinal into a run of sixty near-identical {@code List.add}s.
 *
 * <p>Injecting after {@code shutdownShaders()} is still early enough: that method only closes the
 * <em>previous</em> reload's programs, and the drain that consumes the list comes after it.
 *
 * <p><b>Only below 1.21.2</b>, which is narrower than the {@code <1.21.5} band NeoForge keeps the
 * event alive for. From 1.21.2 a shader is a declaration that {@code ShaderManager} compiles the
 * first time something asks for it, so registering only ever bought eager preloading — which is why
 * Forge dropped the event there too. {@code ClientProxy} registers the Fabric listener under the
 * matching {@code fabric && <1.21.2} gate, so there is no listener above this band rather than a
 * listener that never fires.
 */
@Mixin(GameRenderer.class)
public class GameRendererShaderMixin {

    //? if <1.21.2 {
    @Inject(method = "reloadShaders", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;shutdownShaders()V"))
    private void ac_registerShaders(ResourceProvider resourceProvider, CallbackInfo ci,
                                    @Local(ordinal = 1) List<Pair<net.minecraft.client.renderer.ShaderInstance, Consumer<net.minecraft.client.renderer.ShaderInstance>>> shaders) {
        ModBus.INSTANCE.post(new RegisterShadersEvent(resourceProvider,
                (shader, setter) -> shaders.add(Pair.of(shader, setter))));
    }
    //?}
}
