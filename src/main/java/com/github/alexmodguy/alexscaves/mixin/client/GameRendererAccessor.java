package com.github.alexmodguy.alexscaves.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * A public handle on {@code GameRenderer#setPostEffect}, which is private in vanilla.
 *
 * <p>Only the loaders that leave vanilla alone need it, from 1.21.2. Before that version the shader
 * was selected through {@code loadEffect}, which is public; from 1.21.2 selecting one is just
 * "record the id and raise the flag", and NeoForge widened the method to public in its patches
 * while Forge did not — and neither does Fabric, whose access widener could, but need not: an
 * {@code @Invoker} reaches a private method on its own, so one invoker serves both loaders and the
 * widener stays one entry shorter.
 *
 * <p>Called from {@link com.github.alexmodguy.alexscaves.client.ACClientCompat#loadPostEffect},
 * which is where the three shapes of "select this post-processing shader" meet.
 */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    // 26.3 renamed the target to say whose effect is being selected, so the invoker needs the new
    // string — an @Invoker names its target by hand and no replacement rule can see it, which would
    // have been a clean build and a failure at mixin apply.
    //
    // This arm deliberately carries NO loader condition, unlike the one below it. That split exists
    // only because NeoForge widened the old method to public while Forge and Fabric left it private;
    // whether NeoForge widened the renamed one cannot be read out of a vanilla jar. It need not be:
    // an @Invoker reaches a private method on any loader, so all three take this arm and there is no
    // per-loader claim left to get wrong.
    //? if >=26.3 {
    /*@org.spongepowered.asm.mixin.gen.Invoker("setSpectatedEntityPostEffect")
    void ac$setPostEffect(net.minecraft.resources.ResourceLocation effect);
    *///?} elif !neoforge && >=1.21.2 {
    /*@org.spongepowered.asm.mixin.gen.Invoker("setPostEffect")
    void ac$setPostEffect(net.minecraft.resources.ResourceLocation effect);
    *///?}
}
