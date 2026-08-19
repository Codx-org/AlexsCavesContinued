package com.github.alexmodguy.alexscaves.fabric.forge.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Fabric stand-in for the one loader render type this mod draws with.
 *
 * <p>Ten call sites ask for {@link #getUnlitTranslucent}, all of them glow: the cave book's page
 * quad, the extinction spear (held and thrown), the dark arrow, the nucleeper's and gumbeeper's
 * glass, the amber monolith, and three particles. None is gated, so whatever this answers has to be
 * right on all 22 Fabric nodes.
 *
 * <h2>Why this is a one-line delegate and not a copy of the loader's render type</h2>
 *
 * <p>The loader's own implementation is not one thing across this range — it was rewritten, and the
 * two halves disagree about what "unlit" means:
 *
 * <ul>
 *   <li>up to and including 26.1 it is a composite over a <b>loader-supplied core shader</b>
 *       ({@code rendertype_entity_unlit_translucent}, shipped in the universal jar under the
 *       loader's own asset namespace). That shader is vanilla's entity-translucent one with the
 *       directional diffuse mix dropped from the vertex stage — so it is unlit in the sense of
 *       <i>no diffuse shading</i>, and it still multiplies by the lightmap;
 *   <li>from 26.2 the shader is gone from the code path (the asset still ships, unused) and the
 *       type is pure vanilla: the entity-translucent pipeline, sorted, overlay bound, and
 *       <b>no lightmap</b> — i.e. unlit now means <i>ignores the light level</i>, and the diffuse
 *       shading it used to remove is back.
 * </ul>
 *
 * <p>Neither half is reachable here. The old one needs an asset this mod does not ship and a
 * core-shader registration path Fabric does not have without a mixin of its own; the new one names
 * a pipeline whose lightmap sampler is fed by the loader's patched setup, not by anything a mod can
 * ask vanilla for. So this answers with the vanilla render type that both halves are approximating,
 * {@code entityTranslucentEmissive}: sorted translucent, overlay bound, cull off, and fullbright.
 *
 * <p>Two deliberate divergences follow, and they are the whole cost of this class:
 *
 * <ul>
 *   <li><b>The lightmap is ignored on every node</b>, where below 26.2 the loader applied it. Every
 *       one of the ten call sites is an emissive effect that reads as self-lit, so fullbright is
 *       the intent at all of them; the visible difference is that these effects no longer darken in
 *       an unlit cave. It also makes the 22 Fabric nodes agree with each other and with the 26.2+
 *       loader nodes, rather than splitting the port down the middle at 26.2 to reproduce a
 *       distinction the loader itself stopped making.
 *   <li><b>Depth is not written</b> (vanilla's emissive type masks it off, the loader's did not).
 *       Both composites sort on upload, so overlapping quads inside one draw still resolve; what is
 *       lost is these effects occluding <i>other</i> translucent geometry drawn after them.
 * </ul>
 *
 * <p>Nothing here is version-gated. {@code RenderType.entityTranslucentEmissive} is in the
 * {@code !mc2111-rendertypes-*} rename list, so the 1.21.11 split of that class into a factory
 * holder carries this call and its import across on its own.
 */
public final class ForgeRenderTypes {

    private ForgeRenderTypes() {
    }

    public static RenderType getUnlitTranslucent(ResourceLocation textureLocation) {
        return RenderType.entityTranslucentEmissive(textureLocation);
    }
}
