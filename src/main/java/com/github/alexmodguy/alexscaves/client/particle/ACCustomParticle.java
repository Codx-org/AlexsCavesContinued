package com.github.alexmodguy.alexscaves.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

/**
 * Base class for the Alex's Caves particles that draw themselves rather than a sprite quad — the
 * lightning arcs, the void-being parts, the mushroom cloud, the rainbow, the trail particles.
 *
 * <p>Up to 1.21.8 these declared {@code ParticleRenderType.CUSTOM} and vanilla simply called
 * {@code Particle#render} once per frame with a consumer they were free to ignore. 1.21.9 split
 * rendering into extract-then-submit and deleted {@code Particle#render}, {@code renderCustom} and
 * the {@code CUSTOM} render type along with it, and moved the colour and roll fields down onto
 * {@code SingleQuadParticle} — which these are not.
 *
 * <p>So from 1.21.9 this class re-declares {@code render} as its own abstract method (vanilla no
 * longer has one to clash with, and none of the subclasses ever wrote {@code @Override} on it),
 * carries the six fields and two setters that moved away, and reports
 * {@link ACParticleBuffers#GROUP_TYPE} as its group. {@link ACParticleBuffers} then replays what the
 * unchanged {@code render} bodies drew into the new submission pipeline.
 *
 * @see ACParticleBuffers
 * @see ACQuadParticle
 */
public abstract class ACCustomParticle extends net.minecraft.client.particle.Particle {

    protected ACCustomParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    protected ACCustomParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
    }

    /**
     * Whether this particle draws nothing at all. Overridden by the handful that exist only to spawn
     * others, so they stay out of the recording pass entirely; stated once, un-gated, because the
     * method that reads it is spelled differently on either side of 1.21.9.
     */
    protected boolean acNoRender() {
        return false;
    }


    // A raycast that belongs to no entity. Upstream spelled that `new ClipContext(from, to, block,
    // fluid, (Entity) null)` and vanilla tolerated it: CollisionContext.of(null) simply produced an
    // EntityCollisionContext with a null entity, and every shape query it fed was null-safe.
    //
    // ⚠️ 1.21.2 put an Objects.requireNonNull in CollisionContext.of(Entity) (javap'd across the
    // whole matrix: absent 1.20.1…1.21.1, present 1.21.2…26.2), so that same call now throws an NPE
    // from inside the constructor. It is a *runtime* break with no compile-time tell at all — the
    // Entity overload still exists on every node — and it lands on the two lightning particles, i.e.
    // on approaching a magnetic cave or a tesla bulb, which is where it was found: the whole client
    // died with "Exception while adding particle" the moment the magnetic-caves ambient particle
    // spawned its first arc.
    //
    // CollisionContext.empty() is what null always meant, and the ClipContext overload taking one
    // exists from 1.21.1 (checked, not assumed) — comfortably below the 1.21.2 boundary, so the two
    // arms meet with a version to spare. The live arm is the pre-1.21.2 spelling because the active
    // node is 1.20.1-forge.
    protected static ClipContext ownerlessClip(Vec3 from, Vec3 to, ClipContext.Block block, ClipContext.Fluid fluid) {
        //? if >=1.21.2 {
        /*return new ClipContext(from, to, block, fluid, net.minecraft.world.phys.shapes.CollisionContext.empty());
        *///?} else {
        return new ClipContext(from, to, block, fluid, (net.minecraft.world.entity.Entity) null);
        //?}
    }

    //? if >=1.21.9 {
    /*protected float rCol = 1.0F;
    protected float gCol = 1.0F;
    protected float bCol = 1.0F;
    protected float alpha = 1.0F;
    protected float roll;
    protected float oRoll;

    public void setColor(float red, float green, float blue) {
        this.rCol = red;
        this.gCol = green;
        this.bCol = blue;
    }

    protected void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public abstract void render(VertexConsumer consumer, Camera camera, float partialTick);

    @Override
    public net.minecraft.client.particle.ParticleRenderType getGroup() {
        return this.acNoRender()
                ? net.minecraft.client.particle.ParticleRenderType.NO_RENDER
                : ACParticleBuffers.GROUP_TYPE;
    }
    *///?} else {
    @Override
    public net.minecraft.client.particle.ParticleRenderType getRenderType() {
        return this.acNoRender()
                ? net.minecraft.client.particle.ParticleRenderType.NO_RENDER
                : net.minecraft.client.particle.ParticleRenderType.CUSTOM;
    }
    //?}
}
