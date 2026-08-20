package com.github.alexmodguy.alexscaves.mixin.fabric.client;

import com.github.alexmodguy.alexscaves.client.event.ClientEvents;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric replacement for the mod's two HUD-overlay hooks. Forge and NeoForge express these as
 * {@code RenderGuiOverlayEvent.Pre}/{@code .Post} against named overlay ids; Fabric has no such event
 * and nothing in this tree posts one, so this mixin drives the same two loader-neutral predicates
 * ({@code ClientEvents#hidePossessedPlayerOverlay} and {@code #hideExperienceBar}) and the same two
 * loader-neutral draws straight off vanilla's own HUD methods.
 *
 * <p>What it must reproduce, and it is deliberately the NeoForge mapping rather than the 1.20.1 one
 * because that is the one that still exists on every band: possession hides the reticle, the held-item
 * name, and whichever bar sits above the hotbar; a mount with a riding meter hides that bar too. The
 * experience <em>number</em> is never hidden on any loader, so it is left alone here as well.
 *
 * <p>Three things make this one file rather than four. The HUD class is vanilla's {@code Gui} until
 * 26.2 splits the drawing half out into {@code Hud}, so the {@code @Mixin} target itself is gated.
 * Every method it touches was renamed {@code render*} to {@code extract*} at 26.1, which is a gate on
 * the selector alone. And an {@code @Inject} handler may omit the target's arguments entirely, so one
 * argument-free handler is legal against all four descriptor shapes a method like the reticle's has
 * across the range — the graphics argument the two post-draws need comes back through
 * {@code @Local(argsOnly = true)} instead, and its type token is rewritten for 26 by the tree's own
 * replacement rule exactly as it is in every shared mixin.
 *
 * <p>Cancelling at HEAD skips this file's own RETURN injections, which is the behaviour the loaders
 * have: a cancelled {@code Pre} means no {@code Post}. RETURN rather than TAIL because the reticle
 * method returns early in third person, and the riding meter still belongs on screen there.
 */
//? if >=26.2 {
/*@Mixin(net.minecraft.client.gui.Hud.class)
*///?} else {
@Mixin(net.minecraft.client.gui.Gui.class)
//?}
public class GuiHudMixin {

    @Unique
    private static boolean ac_fabricHideContextualBar() {
        return ClientEvents.hidePossessedPlayerOverlay() || ClientEvents.hideExperienceBar();
    }

    @Inject(
            //? if >=26.1 {
            /*method = "extractCrosshair",
            *///?} else {
            method = "renderCrosshair",
            //?}
            at = @At("HEAD"), cancellable = true, remap = true
    )
    private void ac_fabricHideCrosshair(CallbackInfo ci) {
        if (ClientEvents.hidePossessedPlayerOverlay()) {
            ci.cancel();
        }
    }

    @Inject(
            //? if >=26.1 {
            /*method = "extractSelectedItemName",
            *///?} else {
            method = "renderSelectedItemName",
            //?}
            at = @At("HEAD"), cancellable = true, remap = true
    )
    private void ac_fabricHideSelectedItemName(CallbackInfo ci) {
        if (ClientEvents.hidePossessedPlayerOverlay()) {
            ci.cancel();
        }
    }

    // Below 1.21.6 the two bars above the hotbar are drawn by methods of their own, so each is simply
    // cancelled. Only the first of them answers to a mount's meter, which is what the 1.20.1 wiring did.
    //? if <1.21.6 {
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true, remap = true)
    private void ac_fabricHideExperienceBar(CallbackInfo ci) {
        if (ac_fabricHideContextualBar()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderJumpMeter", at = @At("HEAD"), cancellable = true, remap = true)
    private void ac_fabricHideJumpMeter(CallbackInfo ci) {
        if (ClientEvents.hidePossessedPlayerOverlay()) {
            ci.cancel();
        }
    }
    //?}

    // From 1.21.6 both of those, plus the locator bar, are one "contextual info bar" whose renderer is
    // selected per frame and cached in a pair. Handing back the interface's own no-op constant in place
    // of the cached renderer suppresses the bar without touching the cache or the selection, and one
    // handler covers the background and the bar alike because both draws are preceded by the same read.
    // The pair's accessor is matched by its bytecode return type, which is Object on every band, so the
    // handler needs no arms of its own — only the enclosing method's name, the pair's owner (26.2 swapped
    // commons-lang for DataFixerUpper) and the interface's name (renamed at 26.2) move.
    //? if >=26.2 {
    /*@com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "extractHotbarAndDecorations", remap = true, at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/util/Pair;getSecond()Ljava/lang/Object;"))
    private Object ac_fabricContextualBar(Object original) {
        return ac_fabricHideContextualBar() ? net.minecraft.client.gui.contextualbar.ContextualBar.EMPTY : original;
    }
    *///?} elif >=26.1 {
    /*@com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "extractHotbarAndDecorations", remap = true, at = @At(value = "INVOKE", target = "Lorg/apache/commons/lang3/tuple/Pair;getValue()Ljava/lang/Object;"))
    private Object ac_fabricContextualBar(Object original) {
        return ac_fabricHideContextualBar() ? net.minecraft.client.gui.contextualbar.ContextualBarRenderer.EMPTY : original;
    }
    *///?} elif >=1.21.6 {
    /*@com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "renderHotbarAndDecorations", remap = true, at = @At(value = "INVOKE", target = "Lorg/apache/commons/lang3/tuple/Pair;getValue()Ljava/lang/Object;"))
    private Object ac_fabricContextualBar(Object original) {
        return ac_fabricHideContextualBar() ? net.minecraft.client.gui.contextualbar.ContextualBarRenderer.EMPTY : original;
    }
    *///?}

    @Inject(
            //? if >=26.1 {
            /*method = "extractCrosshair",
            *///?} else {
            method = "renderCrosshair",
            //?}
            at = @At("RETURN"), remap = true
    )
    private void ac_fabricAfterCrosshair(CallbackInfo ci, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) GuiGraphics guiGraphics) {
        ClientEvents.renderRidingMeterHud(guiGraphics);
    }

    @Inject(
            //? if >=26.1 {
            /*method = "extractPlayerHealth",
            *///?} else {
            method = "renderPlayerHealth",
            //?}
            at = @At("RETURN"), remap = true
    )
    private void ac_fabricAfterPlayerHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
        ClientEvents.renderIrradiatedHearts(guiGraphics);
    }
}
