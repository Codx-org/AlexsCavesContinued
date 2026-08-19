package com.github.alexmodguy.alexscaves.client.render.compat;

// The other casualty of 26.2 deleting immediate-mode rendering: com.mojang.blaze3d.vertex
// .VertexMultiConsumer, which fanned one stream of vertices out to two consumers. Vanilla used it
// for exactly one thing — drawing an enchanted item's glint pass and its base pass from a single
// model traversal — and 26.2 does that through the submit pipeline instead, which takes the glint
// as a property of the submitted node rather than as a second consumer.
//
// This mod's two sites are ACArmorRenderProperties' foil branches, and they are one model
// traversal each, so the cheap faithful translation is to keep fanning out: the pair below
// forwards every abstract on VertexConsumer to both delegates. The delegates on 26.2 are
// ACSubmitBuffers recorders, so "write twice" costs one extra recorded vertex list rather than a
// second GPU draw, and the two render types are submitted independently at flush.
//
// Only the eight abstracts are overridden. Every other method on VertexConsumer is a default that
// funnels into them, so overriding more would double-forward.
//? if >=26.2 {
/*import com.mojang.blaze3d.vertex.VertexConsumer;

public class VertexMultiConsumer {

    public static VertexConsumer create(VertexConsumer first, VertexConsumer second) {
        return new Pair(first, second);
    }

    private record Pair(VertexConsumer first, VertexConsumer second) implements VertexConsumer {

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.first.addVertex(x, y, z);
            this.second.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.first.setColor(red, green, blue, alpha);
            this.second.setColor(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setColor(int packed) {
            this.first.setColor(packed);
            this.second.setColor(packed);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.first.setUv(u, v);
            this.second.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            this.first.setUv1(u, v);
            this.second.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.first.setUv2(u, v);
            this.second.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.first.setNormal(x, y, z);
            this.second.setNormal(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            this.first.setLineWidth(width);
            this.second.setLineWidth(width);
            return this;
        }
    }
}
*///?}
