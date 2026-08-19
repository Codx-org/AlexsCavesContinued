package com.github.alexmodguy.alexscaves.mixin.client.citadel;

import com.github.alexmodguy.alexscaves.citadel.CitadelConstants;
import com.github.alexmodguy.alexscaves.citadel.client.shader.PostEffectRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rebuilds the three post-processing effects whenever the shaders behind them are (re)loaded.
 *
 * <p>Up to 26.1 that moment was {@code LevelRenderer#initOutline()}, which the level renderer called
 * from its own {@code onResourceManagerReload} — so the hook sat in {@code LevelRendererMixin} beside
 * the rest of the post-effect plumbing.
 *
 * <p>26.2 deleted it. The level renderer is not a reload listener any more: its entity-outline target
 * is a {@code final} field built in the constructor, and the outline post chain is fetched lazily from
 * {@code ShaderManager} inside the frame that wants it. So there is nothing left on {@code
 * LevelRenderer} to anchor to, and the equivalent moment moves one class over to {@code
 * ShaderManager#apply}, which is where the reload actually lands: at its TAIL the new
 * {@code CompilationCache} has replaced the old one — and closing the old one has already closed the
 * chains this registry was holding — so rebuilding here picks up the fresh ones and never hands back
 * a chain that has been disposed.
 *
 * <p>This lives in a file of its own, with the {@code @Mixin} target itself gated, because the
 * injection moved to a different class rather than to a different member — the same shape as
 * {@code OutlineColorMixin} and {@code SkyTimeOfDayMixin}. Everything else in
 * {@code client.citadel.LevelRendererMixin} stayed where it was.
 */
//? if >=26.2 {
/*@Mixin(net.minecraft.client.renderer.ShaderManager.class)
*///?} else {
@Mixin(net.minecraft.client.renderer.LevelRenderer.class)
//?}
public class PostEffectInitMixin {

    //? if >=26.2 {
    /*@Inject(method = "Lnet/minecraft/client/renderer/ShaderManager;apply(Lnet/minecraft/client/renderer/ShaderManager$Configs;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            remap = CitadelConstants.REMAPREFS,
            at = @At("TAIL"))
    private void citadel_initOutline(net.minecraft.client.renderer.ShaderManager.Configs configs, net.minecraft.server.packs.resources.ResourceManager resourceManager, net.minecraft.util.profiling.ProfilerFiller profiler, CallbackInfo ci) {
        PostEffectRegistry.onInitializeOutline();
    }
    *///?} else {
    @Inject(method = "Lnet/minecraft/client/renderer/LevelRenderer;initOutline()V",
            remap = CitadelConstants.REMAPREFS,
            at = @At("TAIL"))
    private void citadel_initOutline(CallbackInfo ci) {
        PostEffectRegistry.onInitializeOutline();
    }
    //?}
}
