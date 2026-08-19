package com.github.alexmodguy.alexscaves.client.gui.book;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Draws the cave book into its own texture, which the GUI then blits — the only way a 3D model
 * reaches the screen from 1.21.6. See {@link CaveBookRenderState} for why the book had to move here
 * and how the box is sized.
 *
 * <p>Registration differs by loader and neither path is shared: NeoForge fires
 * {@code RegisterPictureInPictureRenderersEvent} (see {@code ClientProxy}), while Forge 56.0.0 does
 * not patch {@code GuiRenderer} at all — its renderer map is built in the constructor from an
 * immutable list, and Forge's Mixin refuses an {@code @Inject} into a constructor outside
 * RETURN/TAIL — so that node hands this renderer back from the map lookup instead, in
 * {@code mixin.client.GuiRendererMixin}.
 *
 * <p>1.21.6 and up only; excluded from the compile below it by {@code ModPlatformPlugin}.
 */
public class CaveBookPipRenderer extends PictureInPictureRenderer<CaveBookRenderState> {

    // 26.2 finished deferring the GUI the way it deferred the level: a picture-in-picture renderer
    // no longer holds a buffer source of its own — it owns a SubmitNodeStorage and is handed the
    // frame's collector per draw — so the constructor lost its argument. The two registration sites
    // pass nothing there; see GuiRendererMixin and ClientProxy.
    //? if >=26.2 {
    /*public CaveBookPipRenderer() {
    }
    *///?} else {
    public CaveBookPipRenderer(MultiBufferSource.BufferSource bufferSource) {
        super(bufferSource);
    }
    //?}

    @Override
    public Class<CaveBookRenderState> getRenderStateClass() {
        return CaveBookRenderState.class;
    }

    // The body below is shared verbatim by both bands — only the signature and the buffer source it
    // draws into differ, so the gate rides on the annotation + signature line and the collector is
    // wrapped back into a MultiBufferSource by ACSubmitBuffers, which is what the page widgets and
    // the book model expect.
    //? if >=26.2 {
    /*@Override
    protected void renderToTexture(CaveBookRenderState state, PoseStack poseStack,
                                   net.minecraft.client.renderer.SubmitNodeCollector collector) {
        net.minecraft.client.renderer.MultiBufferSource bufferSource =
                new com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers(collector);
    *///?} else {
    @Override
    protected void renderToTexture(CaveBookRenderState state, PoseStack poseStack) {
        net.minecraft.client.renderer.MultiBufferSource bufferSource = this.bufferSource;
        //?}
        // The offset from the texture's centre to where the book is actually anchored on screen.
        poseStack.translate(state.offsetX(), state.offsetY(), 0.0F);
        // And back out of the picture-in-picture depth convention. Its projection is built as
        // setOrtho(…, zNear = -1000, zFar = 1000), where the near plane sits at a POSITIVE eye z,
        // and prepare() has already applied scale(f, f, -f) — so a larger model z is further away.
        // Every screen this mod drew before 1.21.6 was the opposite: larger z meant nearer, which is
        // what the book's own chain and the page widgets (an item lifted off the page, an entity in
        // front of it) are written against. One more flip restores exactly the handedness and the
        // winding they had, rather than reversing the layering inside every page.
        poseStack.scale(1.0F, 1.0F, -1.0F);
        state.screen().renderBookModel(poseStack, bufferSource, state.mouseX(), state.mouseY(), state.partialTick());
    }

    @Override
    protected float getTranslateY(int textureHeight, int guiScale) {
        // Centre, not the default bottom edge — the book is anchored near the middle of the screen
        // and the box is the whole screen, so its centre is the useful origin. The x origin is always
        // the centre and is not overridable, so this keeps the two axes consistent.
        return textureHeight / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "alexscaves cave book";
    }
}
