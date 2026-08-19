package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.client.gui.book.CaveBookPipRenderer;
import com.github.alexmodguy.alexscaves.client.gui.book.CaveBookRenderState;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Teaches a vanilla-shaped {@code GuiRenderer} about the cave book's picture-in-picture renderer.
 *
 * <p>NeoForge rewrote this class to hold registrations rather than instances and gave mods
 * {@code RegisterPictureInPictureRenderersEvent} to add to them. Forge 56.0.0 ships no patch for it
 * whatsoever — {@code GuiRenderer} takes a {@code List<PictureInPictureRenderer<?>>} in its
 * constructor and freezes it into an {@code ImmutableMap} keyed by render-state class, and there is
 * no hook of any kind on the way in. Fabric leaves the class alone too, so both of those loaders
 * need this and the target is plain vanilla on each.
 *
 * <p>So this does not try to get into the map. {@code preparePictureInPictureState} looks a renderer
 * up by {@code state.getClass()}; a state vanilla has never heard of yields {@code null}, and that
 * is the value replaced here. Rewriting the lookup rather than the map also means nothing is built
 * until a book is actually on screen, which is the reason the renderer is created lazily instead of
 * in a constructor injection — Forge's Mixin refuses {@code @Inject} into a constructor anywhere but
 * RETURN/TAIL, so there is no clean way to do that either.
 *
 * <p>It also survives fabric-api being installed alongside. That mod's own {@code GuiRendererMixin}
 * only swaps the frozen map for a mutable {@code IdentityHashMap} in the constructor so its
 * {@code SpecialGuiElementRegistry} can add to it; the lookup this rewrites is untouched, and
 * nothing else registers a renderer for {@code CaveBookRenderState}, so the two never contend.
 * fabric-api's registry would be the sanctioned route on that loader — it is declined only because
 * this mixin already exists, is loader-neutral, and keeps one code path for both.
 *
 * <p>Everywhere except Forge and Fabric from 1.21.6: {@code ModPlatformPlugin} excludes this file
 * from the compile and prunes the entry back out of the mixin config.
 */
@Mixin(GuiRenderer.class)
public class GuiRendererMixin {

    // 26.2 took the buffer source off GuiRenderer along with PictureInPictureRenderer's constructor
    // argument — the GUI is a submit-and-replay graph there too — so there is nothing left to shadow.
    // A @Shadow that matches no field is a class-load failure, not a compile one, which is why this
    // is gated out rather than left to be harmlessly unused.
    //? if <26.2 {
    @Shadow
    @Final
    private MultiBufferSource.BufferSource bufferSource;
    //?}

    @Unique
    private PictureInPictureRenderer<CaveBookRenderState> ac_caveBookRenderer;

    @ModifyExpressionValue(
            method = "preparePictureInPictureState",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object ac_pictureInPictureRenderer(Object original, @Local(argsOnly = true) PictureInPictureRenderState state) {
        if (original == null && state instanceof CaveBookRenderState) {
            if (ac_caveBookRenderer == null) {
                // 26.2 took the buffer source off PictureInPictureRenderer — it owns a
                // SubmitNodeStorage and is handed the frame's collector per draw instead.
                //? if >=26.2
                /*ac_caveBookRenderer = new CaveBookPipRenderer();*/
                //? if <26.2
                ac_caveBookRenderer = new CaveBookPipRenderer(this.bufferSource);
            }
            return ac_caveBookRenderer;
        }
        return original;
    }
}
