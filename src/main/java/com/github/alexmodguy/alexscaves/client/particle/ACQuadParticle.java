package com.github.alexmodguy.alexscaves.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Consumer;

/**
 * Base class for every sprite-quad particle in Alex's Caves — the mod's stand-in for what vanilla
 * called {@code TextureSheetParticle} up to 1.21.8 and deleted in 1.21.9, folding its whole surface
 * (the sprite field, {@code setSprite}, {@code setSpriteFromAge}, the four UV getters, the colour
 * and roll fields) down into {@code SingleQuadParticle}.
 *
 * <p>Two things it adds over a plain rebase:
 *
 * <ul>
 *   <li>{@code pickSprite(SpriteSet)} — {@code TextureSheetParticle} declared this <em>public</em>
 *       and several nested {@code Factory} classes call it on an instance they have just built.
 *       {@code setSprite} alone would not do: it is {@code protected}, and a {@code Factory} is not
 *       a subclass of the particle it creates.</li>
 *   <li>{@link #acLayer()} — a mod-vocabulary blend bucket ({@link ACParticleLayer}) translated here
 *       into whichever vanilla spelling the node has. That keeps the subclasses free of {@code //?}
 *       gates: each states its bucket once and the same source compiles on all 58 nodes.</li>
 * </ul>
 *
 * <p>⚠️ Deliberately <em>not</em> named {@code TextureSheetParticle}. A compat class that shares a
 * vanilla simple name silently retargets any mixin or import rule that mentions it — the failure
 * {@code AlexsMobsContinued} recorded when its own shim was named after the class it replaced.
 */
//? if >=1.21.9 {
/*public abstract class ACQuadParticle extends net.minecraft.client.particle.SingleQuadParticle {

    protected ACQuadParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, null);
    }

    protected ACQuadParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd, null);
    }

    public void pickSprite(SpriteSet sprites) {
        this.setSprite(sprites.get(this.random));
    }

    @Override
    public net.minecraft.client.particle.SingleQuadParticle.Layer getLayer() {
        return switch (this.acLayer()) {
            case TRANSLUCENT -> net.minecraft.client.particle.SingleQuadParticle.Layer.TRANSLUCENT;
            case OPAQUE, LIT -> net.minecraft.client.particle.SingleQuadParticle.Layer.OPAQUE;
        };
    }

    @Override
    public void extract(net.minecraft.client.renderer.state.QuadParticleRenderState state, Camera camera, float partialTick) {
        List<Consumer<Quaternionf>> rotations = this.acQuadRotations();
        if (rotations.isEmpty()) {
            super.extract(state, camera, partialTick);
            return;
        }
        for (Consumer<Quaternionf> rotation : rotations) {
            Quaternionf quaternionf = acOrientation(rotation);
            this.extractRotatedQuad(state, camera, quaternionf, partialTick);
        }
    }
*///?} else {
public abstract class ACQuadParticle extends net.minecraft.client.particle.TextureSheetParticle {

    protected ACQuadParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    protected ACQuadParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
    }

    @Override
    public net.minecraft.client.particle.ParticleRenderType getRenderType() {
        return switch (this.acLayer()) {
            case TRANSLUCENT -> net.minecraft.client.particle.ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
            case OPAQUE -> net.minecraft.client.particle.ParticleRenderType.PARTICLE_SHEET_OPAQUE;
            case LIT -> net.minecraft.client.particle.ParticleRenderType.PARTICLE_SHEET_LIT;
        };
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTick) {
        List<Consumer<Quaternionf>> rotations = this.acQuadRotations();
        if (rotations.isEmpty()) {
            super.render(consumer, camera, partialTick);
            return;
        }
        Vec3 vec3 = camera.getPosition();
        float f = (float) (Mth.lerp((double) partialTick, this.xo, this.x) - vec3.x());
        float f1 = (float) (Mth.lerp((double) partialTick, this.yo, this.y) - vec3.y());
        float f2 = (float) (Mth.lerp((double) partialTick, this.zo, this.z) - vec3.z());
        float f3 = this.getQuadSize(partialTick);
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTick);
        for (Consumer<Quaternionf> rotation : rotations) {
            Quaternionf quaternionf = acOrientation(rotation);
            Vector3f[] corners = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
            for (Vector3f corner : corners) {
                corner.rotate(quaternionf);
                corner.mul(f3);
                corner.add(f, f1, f2);
            }
            consumer.vertex(corners[0].x(), corners[0].y(), corners[0].z()).uv(u1, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
            consumer.vertex(corners[1].x(), corners[1].y(), corners[1].z()).uv(u1, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
            consumer.vertex(corners[2].x(), corners[2].y(), corners[2].z()).uv(u0, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
            consumer.vertex(corners[3].x(), corners[3].y(), corners[3].z()).uv(u0, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
        }
    }
//?}

    /**
     * The blend bucket this particle draws in. Overridden by every subclass; stated once, un-gated.
     */
    protected abstract ACParticleLayer acLayer();

    /**
     * The orientations this particle's quads take, one per quad, or an empty list — the default — to
     * draw the ordinary camera-facing quad vanilla would.
     *
     * <p>Five particles here (the two sonar cones, the candicorn charge, the gobthumper thump and the
     * raygun blast) are world-oriented rather than billboarded: they draw two fixed-rotation quads and
     * never face the camera at all. Upstream each carried its own copy of the same forty-line
     * {@code renderSignal}, which 1.21.9 would have turned into dead code without a word — vanilla no
     * longer declares {@code Particle#render}, so an override of it compiles as a brand-new method that
     * nothing calls. Stating just the rotations here keeps those five free of version gates and lets
     * this class spell the drawing once per era: immediate-mode vertices below 1.21.9, and
     * {@code extractRotatedQuad} — the very method vanilla's own {@code extract} uses — from 1.21.9 up.
     */
    protected List<Consumer<Quaternionf>> acQuadRotations() {
        return List.of();
    }

    /**
     * The zero-rotation quaternion the five world-oriented particles start from, with a caller's
     * rotations applied. The axis is upstream's: a zero angle about the normalised (0.5, 0.5, 0.5), so
     * the value is identity and only what {@code rotation} does to it matters.
     */
    private static Quaternionf acOrientation(Consumer<Quaternionf> rotation) {
        Vector3f axis = (new Vector3f(0.5F, 0.5F, 0.5F)).normalize();
        Quaternionf quaternionf = (new Quaternionf()).setAngleAxis(0.0F, axis.x(), axis.y(), axis.z());
        rotation.accept(quaternionf);
        return quaternionf;
    }
}
