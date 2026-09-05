package com.github.alexmodguy.alexscaves.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

// A buffer source that answers EVERY request with the hologram render type, whatever was asked for.
//
// The hologram projector poses a mob's model by hand and draws it with one render type of its own,
// which only works when the dispatcher's renderer for that mob is a vanilla-shaped living renderer
// over an EntityModel. A great many modded mobs are not: GeckoLib's renderer, for one, extends the
// plain entity renderer and keeps its geometry to itself, so the hand-posed path finds no model and
// the projector displayed nothing at all. For those the mob is rendered through its OWN renderer --
// the only thing that knows how to draw it -- into this, so that every layer it emits (body, eyes,
// armour, saddle, whatever it has) lands in the hologram type instead of its own.
//
// One texture for the whole entity is deliberate rather than a limitation of the interface: the
// hologram type is a solid tint over the sampled alpha, so a second texture would only change which
// texels are cut away, and there is no version-portable way to read the texture back out of a
// RenderType. The renderer's own getTextureLocation is the mob's main sheet, which is the right one.
public final class ACHologramBuffers implements MultiBufferSource {

    private final MultiBufferSource delegate;
    private final RenderType hologram;

    public ACHologramBuffers(MultiBufferSource delegate, ResourceLocation texture) {
        this.delegate = delegate;
        this.hologram = ACRenderTypes.getHologram(texture);
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return this.delegate.getBuffer(this.hologram);
    }
}
