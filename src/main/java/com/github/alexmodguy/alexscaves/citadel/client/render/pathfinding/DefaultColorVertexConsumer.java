package com.github.alexmodguy.alexscaves.citadel.client.render.pathfinding;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Gives one colour to every vertex written through it.
 *
 * <p>{@code VertexConsumer#defaultColor}/{@code unsetDefaultColor} were removed in 1.21. The
 * pathfinding debug renderer leans on them heavily — it emits 330 bare positions and lets the
 * default fill in the colour — and rewriting all of those to carry a tint would be a far larger
 * change than wrapping the buffer. So this is that pair, reimplemented as a decorator.
 *
 * <p>Below 1.21 the class is an empty shell: {@code VertexConsumer} has a dozen abstract methods
 * there, none of which this would implement, and the two call sites use the real thing anyway.
 */
//? if >=1.21 {
/*public class DefaultColorVertexConsumer implements VertexConsumer {

    private final VertexConsumer delegate;
    private final int argb;

    public DefaultColorVertexConsumer(VertexConsumer delegate, int argb) {
        this.delegate = delegate;
        this.argb = argb;
    }

    // The colour goes on immediately after the position, exactly where the buffer would otherwise
    // have taken it from the default. A caller that sets its own colour afterwards still wins.
    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        this.delegate.addVertex(x, y, z).setColor(this.argb);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        this.delegate.setColor(red, green, blue, alpha);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        this.delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        this.delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        this.delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        this.delegate.setNormal(x, y, z);
        return this;
    }

    // New abstract method in 1.21.11 (a line's width became a per-vertex format element), and the
    // pathfinding debug renderer draws lines, so it has to pass through. Deliberately not annotated
    // @Override — below 1.21.11 the interface has no such method and this is just an extra public
    // one — because this whole class body sits inside a Stonecutter arm, which cannot nest a gate.
    public VertexConsumer setLineWidth(float width) {
        com.github.alexmodguy.alexscaves.client.ACClientCompat.setLineWidth(this.delegate, width);
        return this;
    }

    // setColor(int) was a default method that decomposed and called setColor(int,int,int,int) until
    // 1.21.11 made it abstract. Routing it back through this class's own four-channel override is
    // what that default did, and is what keeps the fixed colour overriding a packed call too. Not
    // annotated @Override for the same reason as setLineWidth above.
    public VertexConsumer setColor(int argb) {
        return com.github.alexmodguy.alexscaves.client.ACClientCompat.setColorPacked(this, argb);
    }
}
*///?} else {
public class DefaultColorVertexConsumer {
}
//?}
