package com.github.alexmodguy.alexscaves.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;

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
