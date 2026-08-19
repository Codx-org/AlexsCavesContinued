package com.github.alexmodguy.alexscaves.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The render state a {@code GuiGraphics} is recording into.
 *
 * <p>1.21.6 turned every GUI draw into a submission to that state, and the mod has one thing to
 * submit that vanilla has no method for: the cave book's picture-in-picture state (see
 * {@link com.github.alexmodguy.alexscaves.client.gui.book.CaveBookRenderState}). NeoForge exposes
 * {@code GuiGraphics#submitPictureInPictureRenderState} for exactly this; Forge does not patch the
 * class at all. The field itself is private on both, and
 * {@code GuiRenderState#submitPicturesInPictureState} is public on both — so reaching the state and
 * submitting to it directly is the one path that is the same on either loader.
 *
 * <p>1.21.6 and up only: {@code GuiRenderState} does not exist below it, so
 * {@code ModPlatformPlugin} excludes this file from the compile and prunes the entry back out of the
 * mixin config there.
 */
@Mixin(GuiGraphics.class)
public interface GuiRenderStateAccessor {

    @Accessor("guiRenderState")
    GuiRenderState ac_getGuiRenderState();
}
