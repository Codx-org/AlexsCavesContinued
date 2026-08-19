package com.github.alexmodguy.alexscaves.client.gui.book;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;

/**
 * One frame's worth of cave book, queued for the picture-in-picture pass.
 *
 * <p>From 1.21.6 a screen cannot draw a 3D model where it stands. The GUI is recorded as render
 * states first and rasterised afterwards, and the only door left open for model geometry is a
 * {@code PictureInPictureRenderer}, which renders to its own colour and depth texture and blits
 * that back into the frame. So the book's whole draw is deferred: the screen submits this, and
 * {@link CaveBookPipRenderer} calls back into it once the pass runs.
 *
 * <p>The box is deliberately the entire screen. A tighter one would be a smaller texture, but the
 * book fills most of the screen when open, its anchor drifts with the page-flip animation and its
 * scale grows over the opening animation — any fixed margin would be a guess that clips. Screen-sized
 * costs one framebuffer-sized RGBA8 plus a DEPTH32, reallocated only on a resize, and cannot clip.
 *
 * <p>{@code scale} is 1, so one model unit is one GUI pixel and the {@code bookScale} of 221 the
 * screen has always used stays as it is. The origin inside the texture is its centre (see
 * {@code CaveBookPipRenderer#getTranslateY}), which is not where the book is anchored, so
 * {@code offsetX}/{@code offsetY} carry the difference — in GUI pixels, sub-pixel part included, so
 * the flip animation stays as smooth as it was when it was drawn inline.
 *
 * <p>1.21.6 and up only: {@code PictureInPictureRenderState} does not exist below it, so
 * {@code ModPlatformPlugin} excludes this file from the compile there.
 */
public record CaveBookRenderState(CaveBookScreen screen, int mouseX, int mouseY, float partialTick,
                                  float offsetX, float offsetY,
                                  int x0, int y0, int x1, int y1, float scale,
                                  ScreenRectangle scissorArea,
                                  ScreenRectangle bounds) implements PictureInPictureRenderState {

    public CaveBookRenderState(CaveBookScreen screen, int mouseX, int mouseY, float partialTick,
                               float offsetX, float offsetY, int width, int height) {
        this(screen, mouseX, mouseY, partialTick, offsetX, offsetY, 0, 0, width, height, 1.0F, null,
                PictureInPictureRenderState.getBounds(0, 0, width, height, null));
    }
}
