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

    //? if !neoforge && >=1.21.2 {
    /*@org.spongepowered.asm.mixin.gen.Invoker("setPostEffect")
    void ac$setPostEffect(net.minecraft.resources.ResourceLocation effect);
    *///?}
}
