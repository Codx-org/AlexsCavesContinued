package com.github.alexmodguy.alexscaves.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;

/**
 * The points during a level render at which this mod draws, named by the mod rather than by a loader.
 *
 * <p>Six of Forge's {@code RenderLevelStageEvent.Stage} constants, under this mod's own name. The
 * indirection exists because that event is not something every node has: <b>Forge deleted
 * {@code RenderLevelStageEvent} outright in 1.21.2</b> when the level renderer became a frame graph,
 * and it has not come back (checked through 61.1.0/1.21.11). NeoForge kept it. Fabric never had it.
 *
 * <p>So the stages are the mod's own vocabulary and each node supplies them however it can — from
 * the loader event where one exists ({@link com.github.alexmodguy.alexscaves.client.event.ClientEvents#postRenderStage}
 * and {@link com.github.alexmodguy.alexscaves.citadel.CitadelClientEvents#renderWorldLastEvent}), and
 * from {@code mixin.client.LevelRenderStageMixin} where it does not. That mixin is the path the
 * Fabric nodes will use as well, which is the other reason this is not a Forge-shaped abstraction.
 */
public enum ACLevelRenderStage {
    AFTER_SKY,
    AFTER_ENTITIES,
    AFTER_CUTOUT_MIPPED_BLOCKS,
    AFTER_CUTOUT_BLOCKS,
    AFTER_TRANSLUCENT_BLOCKS,
    AFTER_TRIPWIRE_BLOCKS;

    /**
     * Runs everything this mod has to draw at {@code stage}.
     *
     * <p>Only the mixin path calls this. Where the loader posts an event, the two listeners are
     * independent subscribers and each reaches its own half directly, exactly as before.
     */
    public static void dispatch(ACLevelRenderStage stage, LevelRenderer levelRenderer, PoseStack poseStack, int renderTick, Camera camera, float partialTick) {
        com.github.alexmodguy.alexscaves.client.event.ClientEvents.renderStage(stage, levelRenderer, poseStack, renderTick, camera, partialTick);
        if (com.github.alexmodguy.alexscaves.citadel.server.entity.pathfinding.raycoms.Pathfinding.isDebug()) {
            com.github.alexmodguy.alexscaves.citadel.client.render.pathfinding.WorldEventContext.INSTANCE.renderStage(stage, poseStack, partialTick);
        }
    }

    /**
     * The stage that follows a chunk render layer, or null for one nothing here draws after.
     *
     * <p>The same mapping Forge's {@code Stage.fromRenderType} makes; the render types are singletons,
     * so identity is the comparison. {@code solid} is deliberately absent — no renderer in this mod
     * or in the vendored Citadel asks for it, and inventing a constant for it would only make the
     * enum claim coverage the mixin does not exercise.
     *
     * <p>From 1.21.6 the layers are no longer drawn one at a time. {@code ChunkSectionLayerGroup}
     * bundles them into OPAQUE ({@code SOLID}, {@code CUTOUT_MIPPED}, {@code CUTOUT}), TRANSLUCENT and
     * TRIPWIRE, and {@code ChunkSectionsToRender#renderGroup} draws a whole group inside a single open
     * {@code RenderPass} — so there is no longer a point between two opaque layers at which anything
     * else could draw. The argument is a group there, and the three opaque layers collapse onto the
     * last of them: <b>{@code AFTER_CUTOUT_MIPPED_BLOCKS} has no anchor above 1.21.6</b>, which costs
     * nothing here because no renderer in this mod or the vendored Citadel asks for it. NeoForge made
     * exactly the same collapse — its {@code RenderLevelStageEvent} lost the per-cutout subclasses and
     * kept one {@code AfterOpaqueBlocks}.
     *
     * <p>26 goes one step further and deletes the tripwire layer itself: {@code ChunkSectionLayer} is
     * down to {@code SOLID}, {@code CUTOUT} and {@code TRANSLUCENT}, and the group enum to
     * {@code OPAQUE} and {@code TRANSLUCENT}. So that {@code case} is gone from the 26 arm — the whole
     * method is repeated rather than gated inside, since Stonecutter arms cannot nest. The
     * {@code AFTER_TRIPWIRE_BLOCKS} constant itself stays: it is still what the older nodes and both
     * loaders' events map onto, it simply never fires from the chunk layers on 26.
     */
    //? if >=26 {
    /*public static ACLevelRenderStage ofLayerGroup(net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup group) {
        switch (group) {
            case OPAQUE:
                return AFTER_CUTOUT_BLOCKS;
            case TRANSLUCENT:
                return AFTER_TRANSLUCENT_BLOCKS;
            default:
                return null;
        }
    }
    *///?} elif >=1.21.6 {
    /*public static ACLevelRenderStage ofLayerGroup(net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup group) {
        switch (group) {
            case OPAQUE:
                return AFTER_CUTOUT_BLOCKS;
            case TRANSLUCENT:
                return AFTER_TRANSLUCENT_BLOCKS;
            case TRIPWIRE:
                return AFTER_TRIPWIRE_BLOCKS;
            default:
                return null;
        }
    }
    *///?} else {
    public static ACLevelRenderStage ofChunkLayer(RenderType layer) {
        if (layer == RenderType.cutoutMipped()) {
            return AFTER_CUTOUT_MIPPED_BLOCKS;
        } else if (layer == RenderType.cutout()) {
            return AFTER_CUTOUT_BLOCKS;
        } else if (layer == RenderType.translucent()) {
            return AFTER_TRANSLUCENT_BLOCKS;
        } else if (layer == RenderType.tripwire()) {
            return AFTER_TRIPWIRE_BLOCKS;
        }
        return null;
    }
    //?}
}
