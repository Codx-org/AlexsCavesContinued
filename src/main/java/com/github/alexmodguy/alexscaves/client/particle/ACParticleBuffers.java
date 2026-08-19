package com.github.alexmodguy.alexscaves.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * The bridge that keeps Alex's Caves' thirteen hand-drawn particles rendering on 1.21.9.
 *
 * <p>Up to 1.21.8 a particle could declare {@code ParticleRenderType.CUSTOM} and draw whatever it
 * liked from {@code render(VertexConsumer, Camera, float)} — every one of those thirteen ignores the
 * consumer it is handed, grabs the game's own global buffer source itself, draws a
 * model or a raw quad strip through it and calls {@code endBatch()}. 1.21.9 deleted
 * {@code Particle#render} outright: rendering is now two phases, an <em>extract</em> that builds an
 * immutable render state off the game state and a <em>submit</em> that feeds a
 * {@code SubmitNodeCollector}, and immediate-mode drawing in the middle of the level pass is no
 * longer a thing.
 *
 * <p>Rather than rewrite thirteen unrelated drawing routines against the new pipeline, this class
 * lets them keep their bodies verbatim and swaps what they are drawing <em>into</em>. Each one now
 * opens with {@link #source()} instead of {@code Minecraft.getInstance().renderBuffers()
 * .bufferSource()} and closes with {@link #endBatch(MultiBufferSource)} instead of
 * {@code …endBatch()}. Below 1.21.9 those are literally the old calls. From 1.21.9 up, while an
 * extract is running, {@code source()} hands back a recorder that captures every vertex per
 * {@code RenderType}, and the recording is replayed inside
 * {@code SubmitNodeCollector#submitCustomGeometry} during the submit phase.
 *
 * <p>The recorder is faithful rather than normalising: it remembers <em>which</em> vertex attributes
 * each vertex actually set and replays only those, because a {@code BufferBuilder} rejects both a
 * missing element and one its format does not have. Positions are already camera-relative when they
 * arrive — the particles subtract the camera position into their own {@code PoseStack} — which is
 * exactly the space the level pass draws in, so the geometry is submitted under an identity pose.
 *
 * <p>{@link #GROUP_TYPE} is the {@code ParticleRenderType} those particles report from
 * {@code getGroup()}. Vanilla's engine will happily create a bucket for an unknown type but only
 * ever extracts the three it knows about, so {@code mixin.client.ParticleEngineMixin} supplies the
 * group for it and appends its render state — see that class.
 *
 * <p>⚠️ The loaders' {@code makeParticleRenderTypeComparator} orders two unknown types by
 * {@code System.identityHashCode}, so the engine's {@code TreeMap} finds our bucket only because
 * {@link #GROUP_TYPE} is a singleton. Never construct a second equal instance.
 */
public final class ACParticleBuffers {

    private ACParticleBuffers() {
    }

    /**
     * The buffer source a custom particle should draw into: the recorder if one is collecting, and
     * the game's own immediate-mode source otherwise.
     */
    public static MultiBufferSource source() {
        //? if >=1.21.9 {
        /*Recorder recorder = active;
        if (recorder != null) {
            return recorder;
        }
        *///?}
        return Minecraft.getInstance().renderBuffers().bufferSource();
    }

    /**
     * Flushes a buffer source a custom particle drew into. A no-op for the recorder, whose contents
     * are replayed later rather than drawn now.
     */
    public static void endBatch(MultiBufferSource source) {
        if (source instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.endBatch();
        }
    }

    //? if >=1.21.9 {
    /*public static final net.minecraft.client.particle.ParticleRenderType GROUP_TYPE =
            new net.minecraft.client.particle.ParticleRenderType("alexscaves:custom");

    private static Recorder active;

    public static final class CustomGroup extends net.minecraft.client.particle.ParticleGroup<ACCustomParticle> {

        private final CustomState state = new CustomState();

        public CustomGroup(net.minecraft.client.particle.ParticleEngine engine) {
            super(engine);
        }

        @Override
        public net.minecraft.client.renderer.state.ParticleGroupRenderState extractRenderState(
                net.minecraft.client.renderer.culling.Frustum frustum,
                net.minecraft.client.Camera camera,
                float partialTick) {
            this.state.clear();
            Recorder recorder = new Recorder();
            Recorder previous = active;
            active = recorder;
            try {
                for (ACCustomParticle particle : this.getAll()) {
                    particle.render(null, camera, partialTick);
                }
            } finally {
                active = previous;
            }
            for (java.util.Map.Entry<net.minecraft.client.renderer.RenderType, Recorded> entry : recorder.buffers.entrySet()) {
                if (entry.getValue().count > 0) {
                    this.state.types.add(entry.getKey());
                    this.state.geometry.add(entry.getValue());
                }
            }
            return this.state;
        }
    }

    private static final class CustomState implements net.minecraft.client.renderer.state.ParticleGroupRenderState {

        private final java.util.List<net.minecraft.client.renderer.RenderType> types = new java.util.ArrayList<>();
        private final java.util.List<Recorded> geometry = new java.util.ArrayList<>();

        @Override
        public void submit(net.minecraft.client.renderer.SubmitNodeCollector collector,
                           net.minecraft.client.renderer.state.CameraRenderState camera) {
            com.mojang.blaze3d.vertex.PoseStack poseStack = new com.mojang.blaze3d.vertex.PoseStack();
            for (int i = 0; i < this.types.size(); i++) {
                Recorded recorded = this.geometry.get(i);
                collector.submitCustomGeometry(poseStack, this.types.get(i), (pose, consumer) -> recorded.replay(consumer));
            }
        }

        @Override
        public void clear() {
            this.types.clear();
            this.geometry.clear();
        }
    }

    private static final class Recorder implements MultiBufferSource {

        private final java.util.LinkedHashMap<net.minecraft.client.renderer.RenderType, Recorded> buffers = new java.util.LinkedHashMap<>();

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer getBuffer(net.minecraft.client.renderer.RenderType renderType) {
            return this.buffers.computeIfAbsent(renderType, type -> new Recorded());
        }
    }

    private static final class Recorded implements com.mojang.blaze3d.vertex.VertexConsumer {

        private static final int HAS_COLOR = 1;
        private static final int HAS_UV = 2;
        private static final int HAS_UV1 = 4;
        private static final int HAS_UV2 = 8;
        private static final int HAS_NORMAL = 16;
        private static final int HAS_LINE_WIDTH = 32;

        private static final int FLOATS = 9;
        private static final int INTS = 9;

        private float[] floats = new float[FLOATS * 256];
        private int[] ints = new int[INTS * 256];
        private int count;

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x, float y, float z) {
            if ((this.count + 1) * FLOATS > this.floats.length) {
                this.floats = java.util.Arrays.copyOf(this.floats, this.floats.length * 2);
            }
            if ((this.count + 1) * INTS > this.ints.length) {
                this.ints = java.util.Arrays.copyOf(this.ints, this.ints.length * 2);
            }
            int f = this.count * FLOATS;
            this.floats[f] = x;
            this.floats[f + 1] = y;
            this.floats[f + 2] = z;
            this.ints[this.count * INTS + 8] = 0;
            this.count++;
            return this;
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer setColor(int red, int green, int blue, int alpha) {
            int i = (this.count - 1) * INTS;
            this.ints[i] = red;
            this.ints[i + 1] = green;
            this.ints[i + 2] = blue;
            this.ints[i + 3] = alpha;
            this.ints[i + 8] |= HAS_COLOR;
            return this;
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer setUv(float u, float v) {
            int f = (this.count - 1) * FLOATS;
            this.floats[f + 3] = u;
            this.floats[f + 4] = v;
            this.ints[(this.count - 1) * INTS + 8] |= HAS_UV;
            return this;
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int u, int v) {
            int i = (this.count - 1) * INTS;
            this.ints[i + 4] = u;
            this.ints[i + 5] = v;
            this.ints[i + 8] |= HAS_UV1;
            return this;
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int u, int v) {
            int i = (this.count - 1) * INTS;
            this.ints[i + 6] = u;
            this.ints[i + 7] = v;
            this.ints[i + 8] |= HAS_UV2;
            return this;
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float x, float y, float z) {
            int f = (this.count - 1) * FLOATS;
            this.floats[f + 5] = x;
            this.floats[f + 6] = y;
            this.floats[f + 7] = z;
            this.ints[(this.count - 1) * INTS + 8] |= HAS_NORMAL;
            return this;
        }

        // New abstract method in 1.21.11, where a line's width became a per-vertex format element.
        // Recorded like every other attribute rather than dropped, to keep this recorder faithful —
        // a BufferBuilder rejects both a missing element and one its format does not have, so the
        // replay must reproduce exactly the set of attributes the caller wrote. Not annotated
        // @Override: this class body is inside a Stonecutter arm, which cannot nest a gate, and
        // below 1.21.11 the interface has no such method — see ACClientCompat#setLineWidth.
        public com.mojang.blaze3d.vertex.VertexConsumer setLineWidth(float width) {
            this.floats[(this.count - 1) * FLOATS + 8] = width;
            this.ints[(this.count - 1) * INTS + 8] |= HAS_LINE_WIDTH;
            return this;
        }

        // setColor(int) stopped being a default method in 1.21.11. Routed back through the recorder's
        // own four-channel setColor, exactly as the deleted default did, so a packed write is recorded
        // and replayed as the four-channel one the format holds. See ACClientCompat#setColorPacked.
        public com.mojang.blaze3d.vertex.VertexConsumer setColor(int argb) {
            return com.github.alexmodguy.alexscaves.client.ACClientCompat.setColorPacked(this, argb);
        }

        private void replay(com.mojang.blaze3d.vertex.VertexConsumer out) {
            for (int vertex = 0; vertex < this.count; vertex++) {
                int f = vertex * FLOATS;
                int i = vertex * INTS;
                int written = this.ints[i + 8];
                out.addVertex(this.floats[f], this.floats[f + 1], this.floats[f + 2]);
                if ((written & HAS_COLOR) != 0) {
                    out.setColor(this.ints[i], this.ints[i + 1], this.ints[i + 2], this.ints[i + 3]);
                }
                if ((written & HAS_UV) != 0) {
                    out.setUv(this.floats[f + 3], this.floats[f + 4]);
                }
                if ((written & HAS_UV1) != 0) {
                    out.setUv1(this.ints[i + 4], this.ints[i + 5]);
                }
                if ((written & HAS_UV2) != 0) {
                    out.setUv2(this.ints[i + 6], this.ints[i + 7]);
                }
                if ((written & HAS_NORMAL) != 0) {
                    out.setNormal(this.floats[f + 5], this.floats[f + 6], this.floats[f + 7]);
                }
                if ((written & HAS_LINE_WIDTH) != 0) {
                    com.github.alexmodguy.alexscaves.client.ACClientCompat.setLineWidth(out, this.floats[f + 8]);
                }
            }
        }
    }
    *///?}
}
