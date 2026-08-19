package com.github.alexmodguy.alexscaves.client.render.compat;

// 26.2 finished what 1.21.9 started and deleted immediate-mode rendering outright. There is no
// vanilla MultiBufferSource on that version, no VertexMultiConsumer, and
// RenderBuffers is down to a fixed pack plus a section pool — Minecraft#renderBuffers() itself is
// gone. Roughly 150 files in this tree name MultiBufferSource, almost all of them as a parameter
// type on a render body that has kept its pre-1.21.2 shape all the way down the walk.
//
// Rewriting those bodies is not the port: 1.21.9's ACSubmitBuffers already turned "draw into a
// MultiBufferSource" into "record per RenderType, then submitCustomGeometry", and that translation
// is unchanged on 26.2 — SubmitNodeCollector, submitCustomGeometry and every abstract on
// VertexConsumer are byte-for-byte what they were (javap'd on both loaders). The ONLY thing missing
// is the interface itself. So this package vendors it, and a single `replacements.string` rule
// rewrites the fully-qualified name — which subsumes all 144 import lines, since the FQN is a
// substring of each. Every short-form use in the tree (`MultiBufferSource buffers`,
// `MultiBufferSource.BufferSource`) is then byte-identical to what it was, and the mixin selector
// strings, which spell it slash-separated, are out of the rule's reach by construction.
//
// BufferSource is the immediate-mode handle a dozen legacy draw sites still ask for by name
// (`renderBuffers().bufferSource()`, `endBatch()`). It EXTENDS ACSubmitBuffers rather than wrapping
// one, so ACSubmitBuffers#of(...) — which several nested-render helpers use to recover the frame's
// collector out of the buffer source they were handed — keeps answering for those sites too. It
// resolves the collector lazily, through ACRenderContext, because it is a long-lived singleton
// while a collector lives for one frame.
//? if >=26.2 {
/*import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;

public interface MultiBufferSource {

    VertexConsumer getBuffer(RenderType renderType);

    // Citadel's pathfinding debug renderer builds its own source out of a map of per-type byte
    // buffers plus a shared fallback. Both arguments are deliberately Object: the call site spells
    // them as an Object2ObjectLinkedOpenHashMap and a ByteBufferBuilder, neither of which means
    // anything once the geometry is being recorded rather than written, and typing them here would
    // drag fastutil into this file for nothing. The buffers are discarded and the shared source is
    // handed back, so that debug renderer submits through the same path as everything else.
    static BufferSource immediateWithBuffers(Object perTypeBuffers, Object fallbackBuffer) {
        return ACRenderContext.bufferSource();
    }

    final class BufferSource extends ACSubmitBuffers implements MultiBufferSource {

        BufferSource() {
            super(null);
        }

        @Override
        public SubmitNodeCollector collector() {
            return ACRenderContext.collector();
        }

        public void endBatch() {
            this.flush();
        }

        // Vanilla's per-type flush. Nothing in this tree calls it, and there is no cheap way to end
        // one recorder without ending the rest, so it ends the batch — which is what the caller
        // wanted, only slightly earlier for the other types.
        public void endBatch(RenderType renderType) {
            this.flush();
        }
    }
}
*///?}
