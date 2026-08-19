package com.github.alexmodguy.alexscaves.citadel.client.render.pathfinding;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
//? if <1.21.5
import net.minecraft.client.renderer.RenderStateShard.DepthTestStateShard;
//? if >=1.21.5
/*import com.github.alexmodguy.alexscaves.AlexsCaves;*/
//? if >=1.21.5
/*import com.mojang.blaze3d.pipeline.BlendFunction;*/
//? if >=1.21.5
/*import com.mojang.blaze3d.pipeline.RenderPipeline;*/
//? if >=1.21.5
/*import net.minecraft.client.renderer.RenderPipelines;*/
//? if >=1.21.5 && <1.21.11
/*import net.minecraft.client.renderer.RenderStateShard;*/
//? if >=1.21.5
/*import net.minecraft.resources.ResourceLocation;*/
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.LinkedList;
import java.util.List;

public class WorldRenderMacros extends UiRenderMacros {
    private static final int MAX_DEBUG_TEXT_RENDER_DIST_SQUARED = 8 * 8 * 16;
    public static final RenderType LINES = RenderTypes.LINES;
    public static final RenderType LINES_WITH_WIDTH = RenderTypes.LINES_WITH_WIDTH;
    public static final RenderType GLINT_LINES = RenderTypes.GLINT_LINES;
    public static final RenderType GLINT_LINES_WITH_WIDTH = RenderTypes.GLINT_LINES_WITH_WIDTH;
    public static final RenderType COLORED_TRIANGLES = RenderTypes.COLORED_TRIANGLES;
    public static final RenderType COLORED_TRIANGLES_NC_ND = RenderTypes.COLORED_TRIANGLES_NC_ND;

    private static final LinkedList<RenderType> buffers = new LinkedList<>();
    /**
     * Always use {@link #getBufferSource} when actually using the buffer source
     */
    private static MultiBufferSource.BufferSource bufferSource;

    /**
     * Put type at the first position.
     *
     * @param bufferType type to put in
     */
    public static void putBufferHead(final RenderType bufferType) {
        buffers.addFirst(bufferType);
        bufferSource = null;
    }

    /**
     * Put type at the last position.
     *
     * @param bufferType type to put in
     */
    public static void putBufferTail(final RenderType bufferType) {
        buffers.addLast(bufferType);
        bufferSource = null;
    }

    /**
     * Put type before the given buffer or if not found then at first position.
     *
     * @param bufferType type to put in
     * @param putBefore  search for type to put before
     */
    public static void putBufferBefore(final RenderType bufferType, final RenderType putBefore) {
        buffers.add(Math.max(0, buffers.indexOf(putBefore)), bufferType);
        bufferSource = null;
    }

    /**
     * Put type after the given buffer or if not found then at last position.
     *
     * @param bufferType type to put in
     * @param putAfter   search for type to put after
     */
    public static void putBufferAfter(final RenderType bufferType, final RenderType putAfter) {
        final int index = buffers.indexOf(putAfter);
        if (index == -1) {
            buffers.add(bufferType);
        } else {
            buffers.add(index + 1, bufferType);
        }
        bufferSource = null;
    }

    static {
        putBufferTail(WorldRenderMacros.COLORED_TRIANGLES);
        putBufferTail(WorldRenderMacros.LINES);
        putBufferTail(WorldRenderMacros.LINES_WITH_WIDTH);
        putBufferTail(WorldRenderMacros.GLINT_LINES);
        putBufferTail(WorldRenderMacros.GLINT_LINES_WITH_WIDTH);
        putBufferTail(WorldRenderMacros.COLORED_TRIANGLES_NC_ND);
    }

    public static MultiBufferSource.BufferSource getBufferSource() {
        if (bufferSource == null) {
            // 1.21 split the old BufferBuilder in two: a ByteBufferBuilder owns the memory a batch
            // accumulates into, and BufferBuilder became the short-lived writer over it. A buffer
            // source holds the former, so this is the same map of per-type buffers plus a shared
            // fallback, built out of the new class. The shared one is sized as vanilla sizes its own.
            //
            // 26.2 owns no vertex memory at this level at all: the vendored immediateWithBuffers
            // discards both arguments and hands back the frame's recorder, so there is nothing to
            // size and RenderType#bufferSize() is gone along with the need for it.
            //? if >=26.2 {
            /*bufferSource = MultiBufferSource.immediateWithBuffers(null, null);
            *///?} elif >=1.21 {
            /*bufferSource = MultiBufferSource.immediateWithBuffers(Util.make(new Object2ObjectLinkedOpenHashMap<>(), map -> {
                buffers.forEach(type -> map.put(type, new com.mojang.blaze3d.vertex.ByteBufferBuilder(type.bufferSize())));
            }), new com.mojang.blaze3d.vertex.ByteBufferBuilder(256));
            *///?} else {
            bufferSource = MultiBufferSource.immediateWithBuffers(Util.make(new Object2ObjectLinkedOpenHashMap<>(), map -> {
                buffers.forEach(type -> map.put(type, new BufferBuilder(type.bufferSize())));
            }), Tesselator.getInstance().getBuilder());
            //?}
        }
        return bufferSource;
    }

    /**
     * Render a black box around two positions
     *
     * @param posA The first Position
     * @param posB The second Position
     */
    public static void renderBlackLineBox(final BufferSource buffer,
                                          final PoseStack ps,
                                          final BlockPos posA,
                                          final BlockPos posB,
                                          final float lineWidth) {
        renderLineBox(buffer.getBuffer(LINES_WITH_WIDTH), ps, posA, posB, 0x00, 0x00, 0x00, 0xff, lineWidth);
    }

    /**
     * Render a red glint box around two positions
     *
     * @param posA The first Position
     * @param posB The second Position
     */
    public static void renderRedGlintLineBox(final BufferSource buffer,
                                             final PoseStack ps,
                                             final BlockPos posA,
                                             final BlockPos posB,
                                             final float lineWidth) {
        renderLineBox(buffer.getBuffer(GLINT_LINES_WITH_WIDTH), ps, posA, posB, 0xff, 0x0, 0x0, 0xff, lineWidth);
    }

    /**
     * Render a white box around two positions
     *
     * @param posA The first Position
     * @param posB The second Position
     */
    public static void renderWhiteLineBox(final BufferSource buffer,
                                          final PoseStack ps,
                                          final BlockPos posA,
                                          final BlockPos posB,
                                          final float lineWidth) {
        renderLineBox(buffer.getBuffer(LINES_WITH_WIDTH), ps, posA, posB, 0xff, 0xff, 0xff, 0xff, lineWidth);
    }

    /**
     * Render a colored box around from aabb
     *
     * @param aabb the box
     */
    public static void renderLineAABB(final VertexConsumer buffer,
                                      final PoseStack ps,
                                      final AABB aabb,
                                      final int argbColor,
                                      final float lineWidth) {
        renderLineAABB(buffer,
                ps,
                aabb,
                (argbColor >> 16) & 0xff,
                (argbColor >> 8) & 0xff,
                argbColor & 0xff,
                (argbColor >> 24) & 0xff,
                lineWidth);
    }

    /**
     * Render a colored box around from aabb
     *
     * @param aabb the box
     */
    public static void renderLineAABB(final VertexConsumer buffer,
                                      final PoseStack ps,
                                      final AABB aabb,
                                      final int red,
                                      final int green,
                                      final int blue,
                                      final int alpha,
                                      final float lineWidth) {
        renderLineBox(buffer,
                ps,
                (float) aabb.minX,
                (float) aabb.minY,
                (float) aabb.minZ,
                (float) aabb.maxX,
                (float) aabb.maxY,
                (float) aabb.maxZ,
                red,
                green,
                blue,
                alpha,
                lineWidth);
    }

    /**
     * Render a colored box around position
     *
     * @param pos The Position
     */
    public static void renderLineBox(final VertexConsumer buffer,
                                     final PoseStack ps,
                                     final BlockPos pos,
                                     final int argbColor,
                                     final float lineWidth) {
        renderLineBox(buffer,
                ps,
                pos,
                pos,
                (argbColor >> 16) & 0xff,
                (argbColor >> 8) & 0xff,
                argbColor & 0xff,
                (argbColor >> 24) & 0xff,
                lineWidth);
    }

    /**
     * Render a colored box around two positions
     *
     * @param posA The first Position
     * @param posB The second Position
     */
    public static void renderLineBox(final VertexConsumer buffer,
                                     final PoseStack ps,
                                     final BlockPos posA,
                                     final BlockPos posB,
                                     final int argbColor,
                                     final float lineWidth) {
        renderLineBox(buffer,
                ps,
                posA,
                posB,
                (argbColor >> 16) & 0xff,
                (argbColor >> 8) & 0xff,
                argbColor & 0xff,
                (argbColor >> 24) & 0xff,
                lineWidth);
    }

    /**
     * Render a box around two positions
     *
     * @param posA First position
     * @param posB Second position
     */
    public static void renderLineBox(final VertexConsumer buffer,
                                     final PoseStack ps,
                                     final BlockPos posA,
                                     final BlockPos posB,
                                     final int red,
                                     final int green,
                                     final int blue,
                                     final int alpha,
                                     final float lineWidth) {
        renderLineBox(buffer,
                ps,
                Math.min(posA.getX(), posB.getX()),
                Math.min(posA.getY(), posB.getY()),
                Math.min(posA.getZ(), posB.getZ()),
                Math.max(posA.getX(), posB.getX()) + 1,
                Math.max(posA.getY(), posB.getY()) + 1,
                Math.max(posA.getZ(), posB.getZ()) + 1,
                red,
                green,
                blue,
                alpha,
                lineWidth);
    }

    /**
     * Render a box around two positions
     *
     * @param posA First position
     * @param posB Second position
     */
    public static void renderLineBox(final VertexConsumer buffer,
                                     final PoseStack ps,
                                     float minX,
                                     float minY,
                                     float minZ,
                                     float maxX,
                                     float maxY,
                                     float maxZ,
                                     final int red,
                                     final int green,
                                     final int blue,
                                     final int alpha,
                                     final float lineWidth) {
        if (alpha == 0) {
            return;
        }

        final float halfLine = lineWidth / 2.0f;
        minX -= halfLine;
        minY -= halfLine;
        minZ -= halfLine;
        final float minX2 = minX + lineWidth;
        final float minY2 = minY + lineWidth;
        final float minZ2 = minZ + lineWidth;

        maxX += halfLine;
        maxY += halfLine;
        maxZ += halfLine;
        final float maxX2 = maxX - lineWidth;
        final float maxY2 = maxY - lineWidth;
        final float maxZ2 = maxZ - lineWidth;

        final Matrix4f m = ps.last().pose();
        // defaultColor is gone from 1.21 — see DefaultColorVertexConsumer.
        //? if >=1.21 {
        /*final VertexConsumer acColoured = new DefaultColorVertexConsumer(
                buffer, com.github.alexmodguy.alexscaves.server.misc.ACColors.argb(alpha, red, green, blue));
        populateRenderLineBox(minX, minY, minZ, minX2, minY2, minZ2, maxX, maxY, maxZ, maxX2, maxY2, maxZ2, m, acColoured);
        *///?} else {
        buffer.defaultColor(red, green, blue, alpha);
        populateRenderLineBox(minX, minY, minZ, minX2, minY2, minZ2, maxX, maxY, maxZ, maxX2, maxY2, maxZ2, m, buffer);
        buffer.unsetDefaultColor();
        //?}
    }

    // TODO: ebo this, does vanilla have any ebo things?
    public static void populateRenderLineBox(final float minX,
                                             final float minY,
                                             final float minZ,
                                             final float minX2,
                                             final float minY2,
                                             final float minZ2,
                                             final float maxX,
                                             final float maxY,
                                             final float maxZ,
                                             final float maxX2,
                                             final float maxY2,
                                             final float maxZ2,
                                             final Matrix4f m,
                                             final VertexConsumer buf) {
        // z plane

        buf.vertex(m, minX, minY, minZ).endVertex();
        buf.vertex(m, maxX2, minY2, minZ).endVertex();
        buf.vertex(m, maxX, minY, minZ).endVertex();

        buf.vertex(m, minX, minY, minZ).endVertex();
        buf.vertex(m, minX2, minY2, minZ).endVertex();
        buf.vertex(m, maxX2, minY2, minZ).endVertex();

        buf.vertex(m, minX, minY, minZ).endVertex();
        buf.vertex(m, minX2, maxY2, minZ).endVertex();
        buf.vertex(m, minX2, minY2, minZ).endVertex();

        buf.vertex(m, minX, minY, minZ).endVertex();
        buf.vertex(m, minX, maxY, minZ).endVertex();
        buf.vertex(m, minX2, maxY2, minZ).endVertex();

        buf.vertex(m, maxX, maxY, minZ).endVertex();
        buf.vertex(m, minX2, maxY2, minZ).endVertex();
        buf.vertex(m, minX, maxY, minZ).endVertex();

        buf.vertex(m, maxX, maxY, minZ).endVertex();
        buf.vertex(m, maxX2, maxY2, minZ).endVertex();
        buf.vertex(m, minX2, maxY2, minZ).endVertex();

        buf.vertex(m, maxX, maxY, minZ).endVertex();
        buf.vertex(m, maxX2, minY2, minZ).endVertex();
        buf.vertex(m, maxX2, maxY2, minZ).endVertex();

        buf.vertex(m, maxX, maxY, minZ).endVertex();
        buf.vertex(m, maxX, minY, minZ).endVertex();
        buf.vertex(m, maxX2, minY2, minZ).endVertex();

        //

        buf.vertex(m, minX, maxY2, minZ2).endVertex();
        buf.vertex(m, minX2, minY2, minZ2).endVertex();
        buf.vertex(m, minX2, maxY2, minZ2).endVertex();

        buf.vertex(m, minX, maxY2, minZ2).endVertex();
        buf.vertex(m, minX, minY2, minZ2).endVertex();
        buf.vertex(m, minX2, minY2, minZ2).endVertex();

        buf.vertex(m, minX2, minY2, minZ2).endVertex();
        buf.vertex(m, minX2, minY, minZ2).endVertex();
        buf.vertex(m, maxX2, minY, minZ2).endVertex();

        buf.vertex(m, minX2, minY2, minZ2).endVertex();
        buf.vertex(m, maxX2, minY, minZ2).endVertex();
        buf.vertex(m, maxX2, minY2, minZ2).endVertex();

        buf.vertex(m, maxX, maxY2, minZ2).endVertex();
        buf.vertex(m, maxX2, maxY2, minZ2).endVertex();
        buf.vertex(m, maxX2, minY2, minZ2).endVertex();

        buf.vertex(m, maxX, maxY2, minZ2).endVertex();
        buf.vertex(m, maxX2, minY2, minZ2).endVertex();
        buf.vertex(m, maxX, minY2, minZ2).endVertex();

        buf.vertex(m, minX2, maxY2, minZ2).endVertex();
        buf.vertex(m, maxX2, maxY, minZ2).endVertex();
        buf.vertex(m, minX2, maxY, minZ2).endVertex();

        buf.vertex(m, minX2, maxY2, minZ2).endVertex();
        buf.vertex(m, maxX2, maxY2, minZ2).endVertex();
        buf.vertex(m, maxX2, maxY, minZ2).endVertex();

        //

        buf.vertex(m, minX, maxY2, maxZ2).endVertex();
        buf.vertex(m, minX2, maxY2, maxZ2).endVertex();
        buf.vertex(m, minX2, minY2, maxZ2).endVertex();

        buf.vertex(m, minX, maxY2, maxZ2).endVertex();
        buf.vertex(m, minX2, minY2, maxZ2).endVertex();
        buf.vertex(m, minX, minY2, maxZ2).endVertex();

        buf.vertex(m, minX2, minY2, maxZ2).endVertex();
        buf.vertex(m, maxX2, minY, maxZ2).endVertex();
        buf.vertex(m, minX2, minY, maxZ2).endVertex();

        buf.vertex(m, minX2, minY2, maxZ2).endVertex();
        buf.vertex(m, maxX2, minY2, maxZ2).endVertex();
        buf.vertex(m, maxX2, minY, maxZ2).endVertex();

        buf.vertex(m, maxX, maxY2, maxZ2).endVertex();
        buf.vertex(m, maxX2, minY2, maxZ2).endVertex();
        buf.vertex(m, maxX2, maxY2, maxZ2).endVertex();

        buf.vertex(m, maxX, maxY2, maxZ2).endVertex();
        buf.vertex(m, maxX, minY2, maxZ2).endVertex();
        buf.vertex(m, maxX2, minY2, maxZ2).endVertex();

        buf.vertex(m, minX2, maxY2, maxZ2).endVertex();
        buf.vertex(m, minX2, maxY, maxZ2).endVertex();
        buf.vertex(m, maxX2, maxY, maxZ2).endVertex();

        buf.vertex(m, minX2, maxY2, maxZ2).endVertex();
        buf.vertex(m, maxX2, maxY, maxZ2).endVertex();
        buf.vertex(m, maxX2, maxY2, maxZ2).endVertex();

        //

        buf.vertex(m, minX, minY, maxZ).endVertex();
        buf.vertex(m, maxX, minY, maxZ).endVertex();
        buf.vertex(m, maxX2, minY2, maxZ).endVertex();

        buf.vertex(m, minX, minY, maxZ).endVertex();
        buf.vertex(m, maxX2, minY2, maxZ).endVertex();
        buf.vertex(m, minX2, minY2, maxZ).endVertex();

        buf.vertex(m, minX, minY, maxZ).endVertex();
        buf.vertex(m, minX2, minY2, maxZ).endVertex();
        buf.vertex(m, minX2, maxY2, maxZ).endVertex();

        buf.vertex(m, minX, minY, maxZ).endVertex();
        buf.vertex(m, minX2, maxY2, maxZ).endVertex();
        buf.vertex(m, minX, maxY, maxZ).endVertex();

        buf.vertex(m, maxX, maxY, maxZ).endVertex();
        buf.vertex(m, minX, maxY, maxZ).endVertex();
        buf.vertex(m, minX2, maxY2, maxZ).endVertex();

        buf.vertex(m, maxX, maxY, maxZ).endVertex();
        buf.vertex(m, minX2, maxY2, maxZ).endVertex();
        buf.vertex(m, maxX2, maxY2, maxZ).endVertex();

        buf.vertex(m, maxX, maxY, maxZ).endVertex();
        buf.vertex(m, maxX2, maxY2, maxZ).endVertex();
        buf.vertex(m, maxX2, minY2, maxZ).endVertex();

        buf.vertex(m, maxX, maxY, maxZ).endVertex();
        buf.vertex(m, maxX2, minY2, maxZ).endVertex();
        buf.vertex(m, maxX, minY, maxZ).endVertex();

        // x plane

        buf.vertex(m, minX, minY, minZ).endVertex();
        buf.vertex(m, minX, minY, maxZ).endVertex();
        buf.vertex(m, minX, minY2, maxZ2).endVertex();

        buf.vertex(m, minX, minY, minZ).endVertex();
        buf.vertex(m, minX, minY2, maxZ2).endVertex();
        buf.vertex(m, minX, minY2, minZ2).endVertex();

        buf.vertex(m, minX, minY, minZ).endVertex();
        buf.vertex(m, minX, minY2, minZ2).endVertex();
        buf.vertex(m, minX, maxY2, minZ2).endVertex();

        buf.vertex(m, minX, minY, minZ).endVertex();
        buf.vertex(m, minX, maxY2, minZ2).endVertex();
        buf.vertex(m, minX, maxY, minZ).endVertex();

        buf.vertex(m, minX, maxY, maxZ).endVertex();
        buf.vertex(m, minX, maxY, minZ).endVertex();
        buf.vertex(m, minX, maxY2, minZ2).endVertex();

        buf.vertex(m, minX, maxY, maxZ).endVertex();
        buf.vertex(m, minX, maxY2, minZ2).endVertex();
        buf.vertex(m, minX, maxY2, maxZ2).endVertex();

        buf.vertex(m, minX, maxY, maxZ).endVertex();
        buf.vertex(m, minX, maxY2, maxZ2).endVertex();
        buf.vertex(m, minX, minY2, maxZ2).endVertex();

        buf.vertex(m, minX, maxY, maxZ).endVertex();
        buf.vertex(m, minX, minY2, maxZ2).endVertex();
        buf.vertex(m, minX, minY, maxZ).endVertex();

        //

        buf.vertex(m, minX2, maxY2, minZ).endVertex();
        buf.vertex(m, minX2, maxY2, minZ2).endVertex();
        buf.vertex(m, minX2, minY2, minZ2).endVertex();

        buf.vertex(m, minX2, maxY2, minZ).endVertex();
        buf.vertex(m, minX2, minY2, minZ2).endVertex();
        buf.vertex(m, minX2, minY2, minZ).endVertex();

        buf.vertex(m, minX2, minY2, minZ2).endVertex();
        buf.vertex(m, minX2, minY, maxZ2).endVertex();
        buf.vertex(m, minX2, minY, minZ2).endVertex();

        buf.vertex(m, minX2, minY2, minZ2).endVertex();
        buf.vertex(m, minX2, minY2, maxZ2).endVertex();
        buf.vertex(m, minX2, minY, maxZ2).endVertex();

        buf.vertex(m, minX2, maxY2, maxZ).endVertex();
        buf.vertex(m, minX2, minY2, maxZ2).endVertex();
        buf.vertex(m, minX2, maxY2, maxZ2).endVertex();

        buf.vertex(m, minX2, maxY2, maxZ).endVertex();
        buf.vertex(m, minX2, minY2, maxZ).endVertex();
        buf.vertex(m, minX2, minY2, maxZ2).endVertex();

        buf.vertex(m, minX2, maxY2, minZ2).endVertex();
        buf.vertex(m, minX2, maxY, minZ2).endVertex();
        buf.vertex(m, minX2, maxY, maxZ2).endVertex();

        buf.vertex(m, minX2, maxY2, minZ2).endVertex();
        buf.vertex(m, minX2, maxY, maxZ2).endVertex();
        buf.vertex(m, minX2, maxY2, maxZ2).endVertex();

        //

        buf.vertex(m, maxX2, maxY2, minZ).endVertex();
        buf.vertex(m, maxX2, minY2, minZ2).endVertex();
        buf.vertex(m, maxX2, maxY2, minZ2).endVertex();

        buf.vertex(m, maxX2, maxY2, minZ).endVertex();
        buf.vertex(m, maxX2, minY2, minZ).endVertex();
        buf.vertex(m, maxX2, minY2, minZ2).endVertex();

        buf.vertex(m, maxX2, minY2, minZ2).endVertex();
        buf.vertex(m, maxX2, minY, minZ2).endVertex();
        buf.vertex(m, maxX2, minY, maxZ2).endVertex();

        buf.vertex(m, maxX2, minY2, minZ2).endVertex();
        buf.vertex(m, maxX2, minY, maxZ2).endVertex();
        buf.vertex(m, maxX2, minY2, maxZ2).endVertex();

        buf.vertex(m, maxX2, maxY2, maxZ).endVertex();
        buf.vertex(m, maxX2, maxY2, maxZ2).endVertex();
        buf.vertex(m, maxX2, minY2, maxZ2).endVertex();

        buf.vertex(m, maxX2, maxY2, maxZ).endVertex();
        buf.vertex(m, maxX2, minY2, maxZ2).endVertex();
        buf.vertex(m, maxX2, minY2, maxZ).endVertex();

        buf.vertex(m, maxX2, maxY2, minZ2).endVertex();
        buf.vertex(m, maxX2, maxY, maxZ2).endVertex();
        buf.vertex(m, maxX2, maxY, minZ2).endVertex();

        buf.vertex(m, maxX2, maxY2, minZ2).endVertex();
        buf.vertex(m, maxX2, maxY2, maxZ2).endVertex();
        buf.vertex(m, maxX2, maxY, maxZ2).endVertex();

        //

        buf.vertex(m, maxX, minY, minZ).endVertex();
        buf.vertex(m, maxX, minY2, maxZ2).endVertex();
        buf.vertex(m, maxX, minY, maxZ).endVertex();

        buf.vertex(m, maxX, minY, minZ).endVertex();
        buf.vertex(m, maxX, minY2, minZ2).endVertex();
        buf.vertex(m, maxX, minY2, maxZ2).endVertex();

        buf.vertex(m, maxX, minY, minZ).endVertex();
        buf.vertex(m, maxX, maxY2, minZ2).endVertex();
        buf.vertex(m, maxX, minY2, minZ2).endVertex();

        buf.vertex(m, maxX, minY, minZ).endVertex();
        buf.vertex(m, maxX, maxY, minZ).endVertex();
        buf.vertex(m, maxX, maxY2, minZ2).endVertex();

        buf.vertex(m, maxX, maxY, maxZ).endVertex();
        buf.vertex(m, maxX, maxY2, minZ2).endVertex();
        buf.vertex(m, maxX, maxY, minZ).endVertex();

        buf.vertex(m, maxX, maxY, maxZ).endVertex();
        buf.vertex(m, maxX, maxY2, maxZ2).endVertex();
        buf.vertex(m, maxX, maxY2, minZ2).endVertex();

        buf.vertex(m, maxX, maxY, maxZ).endVertex();
        buf.vertex(m, maxX, minY2, maxZ2).endVertex();
        buf.vertex(m, maxX, maxY2, maxZ2).endVertex();

        buf.vertex(m, maxX, maxY, maxZ).endVertex();
        buf.vertex(m, maxX, minY, maxZ).endVertex();
        buf.vertex(m, maxX, minY2, maxZ2).endVertex();

        // y plane

        buf.vertex(m, minX, minY, minZ).endVertex();
        buf.vertex(m, minX2, minY, maxZ2).endVertex();
        buf.vertex(m, minX, minY, maxZ).endVertex();

        buf.vertex(m, minX, minY, minZ).endVertex();
        buf.vertex(m, minX2, minY, minZ2).endVertex();
        buf.vertex(m, minX2, minY, maxZ2).endVertex();

        buf.vertex(m, minX, minY, minZ).endVertex();
        buf.vertex(m, maxX2, minY, minZ2).endVertex();
        buf.vertex(m, minX2, minY, minZ2).endVertex();

        buf.vertex(m, minX, minY, minZ).endVertex();
        buf.vertex(m, maxX, minY, minZ).endVertex();
        buf.vertex(m, maxX2, minY, minZ2).endVertex();

        buf.vertex(m, maxX, minY, maxZ).endVertex();
        buf.vertex(m, maxX2, minY, minZ2).endVertex();
        buf.vertex(m, maxX, minY, minZ).endVertex();

        buf.vertex(m, maxX, minY, maxZ).endVertex();
        buf.vertex(m, maxX2, minY, maxZ2).endVertex();
        buf.vertex(m, maxX2, minY, minZ2).endVertex();

        buf.vertex(m, maxX, minY, maxZ).endVertex();
        buf.vertex(m, minX2, minY, maxZ2).endVertex();
        buf.vertex(m, maxX2, minY, maxZ2).endVertex();

        buf.vertex(m, maxX, minY, maxZ).endVertex();
        buf.vertex(m, minX, minY, maxZ).endVertex();
        buf.vertex(m, minX2, minY, maxZ2).endVertex();

        //

        buf.vertex(m, maxX2, minY2, minZ).endVertex();
        buf.vertex(m, minX2, minY2, minZ2).endVertex();
        buf.vertex(m, maxX2, minY2, minZ2).endVertex();

        buf.vertex(m, maxX2, minY2, minZ).endVertex();
        buf.vertex(m, minX2, minY2, minZ).endVertex();
        buf.vertex(m, minX2, minY2, minZ2).endVertex();

        buf.vertex(m, minX2, minY2, minZ2).endVertex();
        buf.vertex(m, minX, minY2, minZ2).endVertex();
        buf.vertex(m, minX, minY2, maxZ2).endVertex();

        buf.vertex(m, minX2, minY2, minZ2).endVertex();
        buf.vertex(m, minX, minY2, maxZ2).endVertex();
        buf.vertex(m, minX2, minY2, maxZ2).endVertex();

        buf.vertex(m, maxX2, minY2, maxZ).endVertex();
        buf.vertex(m, maxX2, minY2, maxZ2).endVertex();
        buf.vertex(m, minX2, minY2, maxZ2).endVertex();

        buf.vertex(m, maxX2, minY2, maxZ).endVertex();
        buf.vertex(m, minX2, minY2, maxZ2).endVertex();
        buf.vertex(m, minX2, minY2, maxZ).endVertex();

        buf.vertex(m, maxX2, minY2, minZ2).endVertex();
        buf.vertex(m, maxX, minY2, maxZ2).endVertex();
        buf.vertex(m, maxX, minY2, minZ2).endVertex();

        buf.vertex(m, maxX2, minY2, minZ2).endVertex();
        buf.vertex(m, maxX2, minY2, maxZ2).endVertex();
        buf.vertex(m, maxX, minY2, maxZ2).endVertex();

        //

        buf.vertex(m, maxX2, maxY2, minZ).endVertex();
        buf.vertex(m, maxX2, maxY2, minZ2).endVertex();
        buf.vertex(m, minX2, maxY2, minZ2).endVertex();

        buf.vertex(m, maxX2, maxY2, minZ).endVertex();
        buf.vertex(m, minX2, maxY2, minZ2).endVertex();
        buf.vertex(m, minX2, maxY2, minZ).endVertex();

        buf.vertex(m, minX2, maxY2, minZ2).endVertex();
        buf.vertex(m, minX, maxY2, maxZ2).endVertex();
        buf.vertex(m, minX, maxY2, minZ2).endVertex();

        buf.vertex(m, minX2, maxY2, minZ2).endVertex();
        buf.vertex(m, minX2, maxY2, maxZ2).endVertex();
        buf.vertex(m, minX, maxY2, maxZ2).endVertex();

        buf.vertex(m, maxX2, maxY2, maxZ).endVertex();
        buf.vertex(m, minX2, maxY2, maxZ2).endVertex();
        buf.vertex(m, maxX2, maxY2, maxZ2).endVertex();

        buf.vertex(m, maxX2, maxY2, maxZ).endVertex();
        buf.vertex(m, minX2, maxY2, maxZ).endVertex();
        buf.vertex(m, minX2, maxY2, maxZ2).endVertex();

        buf.vertex(m, maxX2, maxY2, minZ2).endVertex();
        buf.vertex(m, maxX, maxY2, minZ2).endVertex();
        buf.vertex(m, maxX, maxY2, maxZ2).endVertex();

        buf.vertex(m, maxX2, maxY2, minZ2).endVertex();
        buf.vertex(m, maxX, maxY2, maxZ2).endVertex();
        buf.vertex(m, maxX2, maxY2, maxZ2).endVertex();

        //

        buf.vertex(m, minX, maxY, minZ).endVertex();
        buf.vertex(m, minX, maxY, maxZ).endVertex();
        buf.vertex(m, minX2, maxY, maxZ2).endVertex();

        buf.vertex(m, minX, maxY, minZ).endVertex();
        buf.vertex(m, minX2, maxY, maxZ2).endVertex();
        buf.vertex(m, minX2, maxY, minZ2).endVertex();

        buf.vertex(m, minX, maxY, minZ).endVertex();
        buf.vertex(m, minX2, maxY, minZ2).endVertex();
        buf.vertex(m, maxX2, maxY, minZ2).endVertex();

        buf.vertex(m, minX, maxY, minZ).endVertex();
        buf.vertex(m, maxX2, maxY, minZ2).endVertex();
        buf.vertex(m, maxX, maxY, minZ).endVertex();

        buf.vertex(m, maxX, maxY, maxZ).endVertex();
        buf.vertex(m, maxX, maxY, minZ).endVertex();
        buf.vertex(m, maxX2, maxY, minZ2).endVertex();

        buf.vertex(m, maxX, maxY, maxZ).endVertex();
        buf.vertex(m, maxX2, maxY, minZ2).endVertex();
        buf.vertex(m, maxX2, maxY, maxZ2).endVertex();

        buf.vertex(m, maxX, maxY, maxZ).endVertex();
        buf.vertex(m, maxX2, maxY, maxZ2).endVertex();
        buf.vertex(m, minX2, maxY, maxZ2).endVertex();

        buf.vertex(m, maxX, maxY, maxZ).endVertex();
        buf.vertex(m, minX2, maxY, maxZ2).endVertex();
        buf.vertex(m, minX, maxY, maxZ).endVertex();
    }

    public static void renderBox(final BufferSource buffer,
                                 final PoseStack ps,
                                 final BlockPos posA,
                                 final BlockPos posB,
                                 final int argbColor) {
        renderBox(buffer.getBuffer(COLORED_TRIANGLES),
                ps,
                posA,
                posB,
                (argbColor >> 16) & 0xff,
                (argbColor >> 8) & 0xff,
                argbColor & 0xff,
                (argbColor >> 24) & 0xff);
    }

    public static void renderBox(final VertexConsumer buffer,
                                 final PoseStack ps,
                                 final BlockPos posA,
                                 final BlockPos posB,
                                 final int red,
                                 final int green,
                                 final int blue,
                                 final int alpha) {
        if (alpha == 0) {
            return;
        }

        final float minX = Math.min(posA.getX(), posB.getX());
        final float minY = Math.min(posA.getY(), posB.getY());
        final float minZ = Math.min(posA.getZ(), posB.getZ());

        final float maxX = Math.max(posA.getX(), posB.getX()) + 1;
        final float maxY = Math.max(posA.getY(), posB.getY()) + 1;
        final float maxZ = Math.max(posA.getZ(), posB.getZ()) + 1;

        final Matrix4f m = ps.last().pose();
        // defaultColor is gone from 1.21 — see DefaultColorVertexConsumer.
        //? if >=1.21 {
        /*final VertexConsumer acColoured = new DefaultColorVertexConsumer(
                buffer, com.github.alexmodguy.alexscaves.server.misc.ACColors.argb(alpha, red, green, blue));
        populateCuboid(minX, minY, minZ, maxX, maxY, maxZ, m, acColoured);
        *///?} else {
        buffer.defaultColor(red, green, blue, alpha);
        populateCuboid(minX, minY, minZ, maxX, maxY, maxZ, m, buffer);
        buffer.unsetDefaultColor();
        //?}
    }

    public static void populateCuboid(final float minX,
                                      final float minY,
                                      final float minZ,
                                      final float maxX,
                                      final float maxY,
                                      final float maxZ,
                                      final Matrix4f m,
                                      final VertexConsumer buf) {
        // z plane

        buf.vertex(m, minX, maxY, minZ).endVertex();
        buf.vertex(m, maxX, minY, minZ).endVertex();
        buf.vertex(m, minX, minY, minZ).endVertex();

        buf.vertex(m, minX, maxY, minZ).endVertex();
        buf.vertex(m, maxX, maxY, minZ).endVertex();
        buf.vertex(m, maxX, minY, minZ).endVertex();

        buf.vertex(m, minX, maxY, maxZ).endVertex();
        buf.vertex(m, minX, minY, maxZ).endVertex();
        buf.vertex(m, maxX, minY, maxZ).endVertex();

        buf.vertex(m, minX, maxY, maxZ).endVertex();
        buf.vertex(m, maxX, minY, maxZ).endVertex();
        buf.vertex(m, maxX, maxY, maxZ).endVertex();

        // y plane

        buf.vertex(m, minX, minY, maxZ).endVertex();
        buf.vertex(m, minX, minY, minZ).endVertex();
        buf.vertex(m, maxX, minY, minZ).endVertex();

        buf.vertex(m, minX, minY, maxZ).endVertex();
        buf.vertex(m, maxX, minY, minZ).endVertex();
        buf.vertex(m, maxX, minY, maxZ).endVertex();

        buf.vertex(m, minX, maxY, maxZ).endVertex();
        buf.vertex(m, maxX, maxY, minZ).endVertex();
        buf.vertex(m, minX, maxY, minZ).endVertex();

        buf.vertex(m, minX, maxY, maxZ).endVertex();
        buf.vertex(m, maxX, maxY, maxZ).endVertex();
        buf.vertex(m, maxX, maxY, minZ).endVertex();

        // x plane

        buf.vertex(m, minX, minY, maxZ).endVertex();
        buf.vertex(m, minX, maxY, minZ).endVertex();
        buf.vertex(m, minX, minY, minZ).endVertex();

        buf.vertex(m, minX, minY, maxZ).endVertex();
        buf.vertex(m, minX, maxY, maxZ).endVertex();
        buf.vertex(m, minX, maxY, minZ).endVertex();

        buf.vertex(m, maxX, minY, maxZ).endVertex();
        buf.vertex(m, maxX, minY, minZ).endVertex();
        buf.vertex(m, maxX, maxY, minZ).endVertex();

        buf.vertex(m, maxX, minY, maxZ).endVertex();
        buf.vertex(m, maxX, maxY, minZ).endVertex();
        buf.vertex(m, maxX, maxY, maxZ).endVertex();
    }

    public static void renderFillRectangle(final BufferSource buffer,
                                           final PoseStack ps,
                                           final int x,
                                           final int y,
                                           final int z,
                                           final int w,
                                           final int h,
                                           final int argbColor) {
        populateRectangle(x,
                y,
                z,
                w,
                h,
                (argbColor >> 16) & 0xff,
                (argbColor >> 8) & 0xff,
                argbColor & 0xff,
                (argbColor >> 24) & 0xff,
                buffer.getBuffer(COLORED_TRIANGLES_NC_ND),
                ps.last().pose());
    }

    public static void populateRectangle(final int x,
                                         final int y,
                                         final int z,
                                         final int w,
                                         final int h,
                                         final int red,
                                         final int green,
                                         final int blue,
                                         final int alpha,
                                         final VertexConsumer buffer,
                                         final Matrix4f m) {
        if (alpha == 0) {
            return;
        }

        buffer.vertex(m, x, y, z).color(red, green, blue, alpha).endVertex();
        buffer.vertex(m, x, y + h, z).color(red, green, blue, alpha).endVertex();
        buffer.vertex(m, x + w, y + h, z).color(red, green, blue, alpha).endVertex();

        buffer.vertex(m, x, y, z).color(red, green, blue, alpha).endVertex();
        buffer.vertex(m, x + w, y + h, z).color(red, green, blue, alpha).endVertex();
        buffer.vertex(m, x + w, y, z).color(red, green, blue, alpha).endVertex();
    }

    /**
     * Renders the given list of strings, 3 elements a row.
     *
     * @param pos                     position to render at
     * @param text                    text list
     * @param matrixStack             stack to use
     * @param buffer                  render buffer
     * @param forceWhite              force white for no depth rendering
     * @param mergeEveryXListElements merge every X elements of text list using a tostring call
     */
    @SuppressWarnings("resource")
    public static void renderDebugText(final BlockPos pos,
                                       final List<String> text,
                                       final PoseStack matrixStack,
                                       final boolean forceWhite,
                                       final int mergeEveryXListElements,
                                       final MultiBufferSource buffer) {
        if (mergeEveryXListElements < 1) {
            throw new IllegalArgumentException("mergeEveryXListElements is less than 1");
        }

        final EntityRenderDispatcher erm = Minecraft.getInstance().getEntityRenderDispatcher();
        final int cap = text.size();
        if (cap > 0 && com.github.alexmodguy.alexscaves.client.ACClientCompat.dispatcherDistanceToSqr(erm, pos.getX(), pos.getY(), pos.getZ()) <= MAX_DEBUG_TEXT_RENDER_DIST_SQUARED) {
            final Font fontrenderer = Minecraft.getInstance().font;

            matrixStack.pushPose();
            matrixStack.translate(pos.getX() + 0.5d, pos.getY() + 0.75d, pos.getZ() + 0.5d);
            matrixStack.mulPose(com.github.alexmodguy.alexscaves.client.ACClientCompat.cameraOrientation());
            matrixStack.scale(-0.014f, -0.014f, 0.014f);
            matrixStack.translate(0.0d, 18.0d, 0.0d);

            final float backgroundTextOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
            final int alphaMask = (int) (backgroundTextOpacity * 255.0F) << 24;

            final Matrix4f rawPosMatrix = matrixStack.last().pose();

            for (int i = 0; i < cap; i += mergeEveryXListElements) {
                final MutableComponent renderText = Component.literal(
                        mergeEveryXListElements == 1 ? text.get(i) : text.subList(i, Math.min(i + mergeEveryXListElements, cap)).toString());
                final float textCenterShift = (float) (-fontrenderer.width(renderText) / 2);

                com.github.alexmodguy.alexscaves.client.ACClientCompat.drawInBatch(fontrenderer, renderText,
                        textCenterShift,
                        0,
                        forceWhite ? 0xffffffff : 0x20ffffff,
                        false,
                        rawPosMatrix,
                        buffer,
                        Font.DisplayMode.SEE_THROUGH,
                        alphaMask,
                        0x00f000f0);
                if (!forceWhite) {
                    com.github.alexmodguy.alexscaves.client.ACClientCompat.drawInBatch(fontrenderer, renderText, textCenterShift, 0, 0xffffffff, false, rawPosMatrix, buffer, Font.DisplayMode.NORMAL, 0, 0x00f000f0);
                }
                matrixStack.translate(0.0d, fontrenderer.lineHeight + 1, 0.0d);
            }

            matrixStack.popPose();
        }
    }

    /**
     * The debug renderer's six render types.
     *
     * <p>Up to 1.21.4 this had to extend {@link RenderType} to reach the {@code protected static}
     * state shards and the {@code create} factory. 1.21.5 made every shard it uses {@code public}
     * on {@code RenderStateShard}, so the inheritance — and with it the never-called constructor
     * that only existed because {@code RenderType}'s was protected — is gone from that version.
     */
    private static final class RenderTypes
            //? if <1.21.5
            extends RenderType
    {
        //? if <1.21.5 {
        private RenderTypes(final String nameIn,
                            final VertexFormat formatIn,
                            final VertexFormat.Mode drawModeIn,
                            final int bufferSizeIn,
                            final boolean useDelegateIn,
                            final boolean needsSortingIn,
                            final Runnable setupTaskIn,
                            final Runnable clearTaskIn) {
            super(nameIn, formatIn, drawModeIn, bufferSizeIn, useDelegateIn, needsSortingIn, setupTaskIn, clearTaskIn);
            throw new IllegalStateException();
        }
        //?}

        //? if >=1.21.5 {
        /*// 1.21.5 folded the shader, blend, cull, depth-test, write-mask and vertex format of a
        // render type into a RenderPipeline, deleting every one of the state shards the other arm
        // uses. The pipelines here restate exactly what that arm asked for; the six CompositeStates
        // keep only what did not move (texture, lightmap, overlay, layering, output, texturing).
        //
        // The one thing that cannot be restated is AlwaysDepthTestStateShard — vanilla's new
        // depth-test enum has no always-pass member the pipelines use, so GLINT_LINES_WITH_WIDTH
        // asks for no depth test at all. Same visible result (the box draws through terrain); it
        // simply stops contributing depth, which nothing in this debug renderer reads back.
        //
        // The blend, depth and colour-mask setters are routed through ACPipelineState because 26.1
        // folded them into two record-valued ones; the boolean depthTest parameter replaces the
        // enum for the same reason, since from 26 "no depth test" is the ABSENCE of a depth state
        // rather than a constant. See that class.
        //
        // Line comments, not a block comment: this whole arm is a block comment on every node
        // below 1.21.5, and Java block comments do not nest.
        private static RenderPipeline pipeline(final String name,
                                               final VertexFormat.Mode mode,
                                               final BlendFunction blend,
                                               final boolean depthTest,
                                               final boolean cull,
                                               final boolean depthWrite) {
            RenderPipeline.Builder builder = com.github.alexmodguy.alexscaves.client.render.ACPipelineState.blend(RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_SNIPPET)
                    .withLocation(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "pipeline/" + name))
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, mode), blend)
                    .withCull(cull);
            builder = depthTest
                    ? com.github.alexmodguy.alexscaves.client.render.ACPipelineState.depth(builder, depthWrite)
                    : com.github.alexmodguy.alexscaves.client.render.ACPipelineState.noDepth(builder, depthWrite);
            return builder.build();
        }

        private static RenderType.CompositeState compositeState() {
            return RenderType.CompositeState.builder()
                    .setTextureState(RenderStateShard.NO_TEXTURE)
                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                    .setOverlayState(RenderStateShard.NO_OVERLAY)
                    .setLayeringState(RenderStateShard.NO_LAYERING)
                    .setOutputState(RenderStateShard.MAIN_TARGET)
                    .setTexturingState(RenderStateShard.DEFAULT_TEXTURING)
                    .createCompositeState(false);
        }

        private static final RenderType GLINT_LINES = RenderType.create("structurize_glint_lines",
                1 << 12,
                pipeline("structurize_glint_lines", VertexFormat.Mode.DEBUG_LINES, BlendFunction.GLINT, false, false, false),
                compositeState());

        private static final RenderType GLINT_LINES_WITH_WIDTH = RenderType.create("structurize_glint_lines_with_width",
                1 << 13,
                pipeline("structurize_glint_lines_with_width", VertexFormat.Mode.TRIANGLES, BlendFunction.GLINT, false, true, true),
                compositeState());

        private static final RenderType LINES = RenderType.create("structurize_lines",
                1 << 14,
                pipeline("structurize_lines", VertexFormat.Mode.DEBUG_LINES, BlendFunction.TRANSLUCENT, true, false, false),
                compositeState());

        private static final RenderType LINES_WITH_WIDTH = RenderType.create("structurize_lines_with_width",
                1 << 13,
                pipeline("structurize_lines_with_width", VertexFormat.Mode.TRIANGLES, BlendFunction.TRANSLUCENT, true, true, true),
                compositeState());

        private static final RenderType COLORED_TRIANGLES = RenderType.create("structurize_colored_triangles",
                1 << 13,
                pipeline("structurize_colored_triangles", VertexFormat.Mode.TRIANGLES, BlendFunction.TRANSLUCENT, true, true, true),
                compositeState());

        private static final RenderType COLORED_TRIANGLES_NC_ND = RenderType.create("structurize_colored_triangles_nc_nd",
                1 << 12,
                pipeline("structurize_colored_triangles_nc_nd", VertexFormat.Mode.TRIANGLES, BlendFunction.TRANSLUCENT, false, false, false),
                compositeState());
        *///?} else {
        private static final RenderType GLINT_LINES = create("structurize_glint_lines",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.DEBUG_LINES,
                1 << 12,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setTextureState(NO_TEXTURE)
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(GLINT_TRANSPARENCY)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setOverlayState(NO_OVERLAY)
                        .setLayeringState(NO_LAYERING)
                        .setOutputState(MAIN_TARGET)
                        .setTexturingState(DEFAULT_TEXTURING)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));

        private static final RenderType GLINT_LINES_WITH_WIDTH = create("structurize_glint_lines_with_width",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.TRIANGLES,
                1 << 13,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setTextureState(NO_TEXTURE)
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(GLINT_TRANSPARENCY)
                        .setDepthTestState(AlwaysDepthTestStateShard.ALWAYS_DEPTH_TEST)
                        .setCullState(CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setOverlayState(NO_OVERLAY)
                        .setLayeringState(NO_LAYERING)
                        .setOutputState(MAIN_TARGET)
                        .setTexturingState(DEFAULT_TEXTURING)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .createCompositeState(false));

        private static final RenderType LINES = create("structurize_lines",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.DEBUG_LINES,
                1 << 14,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setTextureState(NO_TEXTURE)
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setOverlayState(NO_OVERLAY)
                        .setLayeringState(NO_LAYERING)
                        .setOutputState(MAIN_TARGET)
                        .setTexturingState(DEFAULT_TEXTURING)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));

        private static final RenderType LINES_WITH_WIDTH = create("structurize_lines_with_width",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.TRIANGLES,
                1 << 13,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setTextureState(NO_TEXTURE)
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setOverlayState(NO_OVERLAY)
                        .setLayeringState(NO_LAYERING)
                        .setOutputState(MAIN_TARGET)
                        .setTexturingState(DEFAULT_TEXTURING)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .createCompositeState(false));

        private static final RenderType COLORED_TRIANGLES = create("structurize_colored_triangles",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.TRIANGLES,
                1 << 13,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setTextureState(NO_TEXTURE)
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setOverlayState(NO_OVERLAY)
                        .setLayeringState(NO_LAYERING)
                        .setOutputState(MAIN_TARGET)
                        .setTexturingState(DEFAULT_TEXTURING)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .createCompositeState(false));

        private static final RenderType COLORED_TRIANGLES_NC_ND = create("structurize_colored_triangles_nc_nd",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.TRIANGLES,
                1 << 12,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setTextureState(NO_TEXTURE)
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setOverlayState(NO_OVERLAY)
                        .setLayeringState(NO_LAYERING)
                        .setOutputState(MAIN_TARGET)
                        .setTexturingState(DEFAULT_TEXTURING)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));
        //?}
    }

    // A depth test that always passes but still writes depth — vanilla's own DepthTestStateShard
    // cannot express it, because it treats GL_ALWAYS (519) as "no depth state at all" and skips
    // both the enable and the depthFunc. Hence the sentinel -1 handed to super: it is never a real
    // GL func, so the clear side does the normal disable + depthFunc(LEQUAL) restore, and the setup
    // side is replaced wholesale below.
    //
    // Overriding setupRenderState() rather than assigning the setupState field: the field is
    // `protected` and assignable through 1.20.6, but 1.21 made it `public final` and set from the
    // constructor. The method has been public and overridable on every version in range, and it is
    // the only thing that ever reads the field, so one un-gated override covers all of them.
    //? if <1.21.5 {
    public static class AlwaysDepthTestStateShard extends DepthTestStateShard {
        public static final DepthTestStateShard ALWAYS_DEPTH_TEST = new AlwaysDepthTestStateShard();

        private AlwaysDepthTestStateShard() {
            super("true_always", -1);
        }

        @Override
        public void setupRenderState() {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_ALWAYS);
        }
    }
    //?}
}
