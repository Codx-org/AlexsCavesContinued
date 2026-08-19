package com.github.alexmodguy.alexscaves.client.render.compat;

// The frame's SubmitNodeCollector, parked where the legacy draw sites can reach it.
//
// Up to 26.1 a mod could ask the game for a buffer source at any point in the level pass and draw
// into it, off the game instance's RenderBuffers. Thirteen sites in this tree do
// exactly that: the raygun beams, the hologram / ambersol / licowitch batches, the cave book's page
// widgets, the in-hand item renderer, Citadel's pathfinding debug overlay. 26.2 deleted
// RenderBuffers' immediate side and Minecraft#renderBuffers() with it, so there is nothing global
// left to ask; the only handle that draws anything is the SubmitNodeCollector the level renderer
// threads through its submission phase.
//
// So this class makes that collector ambient for exactly the length of the submission phase.
// mixin.client.LevelRenderStageMixin pushes it at the head of LevelRenderer#submitFeatures — the one
// method that owns the whole frame's submission, and which runs strictly before
// FeatureRenderDispatcher#prepareFrame, addSkyPass and addMainPass (javap'd at offsets 69 / ~90 /
// 353 / 387 on Forge, identically ordered on NeoForge) — dispatches the mod's render stages inside
// it, and pops at the end.
//
// ⚠️ Two consequences of that, both accepted deliberately and neither reversible on 26.2:
//   * the four render stages this mod fires below 26.2 (AFTER_SKY, AFTER_ENTITIES,
//     AFTER_CUTOUT_BLOCKS, AFTER_TRANSLUCENT_BLOCKS) collapse onto one moment in the frame. Since
//     1.21.9 the deferred pipeline batches purely by RenderType and decides real draw order itself,
//     so "which stage did this get submitted from" no longer describes when it is rasterised — the
//     stage enum survives only as the mod's own dispatch vocabulary.
//   * anything drawn while no collector is pushed is discarded rather than crashing
//     (ACSubmitBuffers#flush tolerates a null collector). A GUI screen that draws a model outside
//     the level pass is the case that matters, and on 26.2 those go through the picture-in-picture
//     renderer instead.
//? if >=26.2 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;

public final class ACRenderContext {

    // Singletons, not per-call instances: WorldRenderMacros caches the source it is handed for the
    // lifetime of the game, so a source that captured a collector at construction would be stale
    // from the second frame on. BufferSource#collector() reads this class instead, every time.
    private static final MultiBufferSource.BufferSource SHARED = new MultiBufferSource.BufferSource();
    private static final MultiBufferSource.BufferSource CRUMBLING = new MultiBufferSource.BufferSource();

    private static SubmitNodeCollector collector;

    private ACRenderContext() {
    }

    public static void push(SubmitNodeCollector submitNodeCollector) {
        collector = submitNodeCollector;
    }

    // Flush before dropping the collector: several of the legacy sites — the raygun, the cave book's
    // page renderer, the in-hand item renderer — never called endBatch() at all, because vanilla's
    // own global flush at the end of the level pass covered them. This is that flush.
    public static void pop() {
        SHARED.endBatch();
        CRUMBLING.endBatch();
        collector = null;
    }

    public static SubmitNodeCollector collector() {
        return collector;
    }

    public static MultiBufferSource.BufferSource bufferSource() {
        return SHARED;
    }

    // Kept separate from the one above only so an endBatch() on either does not flush the other,
    // matching what RenderBuffers' two sources did.
    public static MultiBufferSource.BufferSource crumblingBufferSource() {
        return CRUMBLING;
    }
}
*///?}
