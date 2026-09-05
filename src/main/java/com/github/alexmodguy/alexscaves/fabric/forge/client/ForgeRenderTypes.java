package com.github.alexmodguy.alexscaves.fabric.forge.client;

import com.github.alexmodguy.alexscaves.client.render.ACRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Fabric stand-in for the one loader render type this mod draws with.
 *
 * <p>Ten call sites want an unlit-translucent type, all of them glow: the cave book's model and its
 * page widget, the extinction spear (held and thrown), the dark arrow, the nucleeper's and
 * gumbeeper's glass, the amber monolith, and three particles. They no longer come here — they call
 * {@code ACRenderTypes.getUnlitTranslucent}, which supplies the mod's own type on the nodes whose
 * loader has none and delegates to the loader everywhere else. This class survives only because the
 * Fabric rename rule points that delegate's <i>import</i> at it, and it forwards so that any future
 * caller reaching for the loader spelling still gets the right thing.
 *
 * <p><b>Do not reinstate the old body.</b> It answered with vanilla's entity-translucent-emissive
 * type, and {@code EMISSIVE} is the wrong switch: in {@code core/entity.vsh} (1.21.5 and up) and in
 * {@code rendertype_entity_translucent_emissive.vsh} (below it) the define gates only the
 * <i>lightmap</i>, while the {@code minecraft_mix_light} call that applies the two hard-coded
 * directional lights sits outside it on every version. So the type was fullbright <i>and</i>
 * diffuse-shaded — which is what made the Cave Compendium's book render with a hard bright/dark
 * step from one face to the next, reported against 1.0.2.
 *
 * <p>For the record, since the loader's own type is not one thing across this range and the shape
 * of it decides which nodes need the mod's own:
 *
 * <ul>
 *   <li><b>Forge up to 26.1</b> and <b>NeoForge on every node</b> supply a real one: a composite
 *       (later a pipeline) over a loader-supplied shader that is vanilla's entity-translucent with
 *       the directional diffuse mix dropped and the lightmap kept. Those 35 nodes still delegate.
 *   <li><b>Forge 26.2</b> (65.1.0) rewrote its two {@code unlitTranslucent} factories onto plain
 *       {@code RenderPipelines.ENTITY_TRANSLUCENT} — no unlit define, no lightmap bound — so that
 *       one node regressed into exactly the diffuse shading the type exists to remove, and it gets
 *       the mod's type alongside Fabric. NeoForge 26.2 did not: it still names
 *       {@code neoforge:pipeline/entity_unlit_translucent} with {@code NO_CARDINAL_LIGHTING} and
 *       still binds the lightmap (read out of 26.2.0.66's bytecode, not out of this comment's
 *       previous revision, which claimed the opposite for both loaders).
 * </ul>
 */
public final class ForgeRenderTypes {

    private ForgeRenderTypes() {
    }

    // Not recursive: this class is compiled on Fabric only, and ACRenderTypes' delegate-to-the-
    // loader arm is gated off on every Fabric node. Keep it that way if that gate is ever widened.
    public static RenderType getUnlitTranslucent(ResourceLocation textureLocation) {
        return ACRenderTypes.getUnlitTranslucent(textureLocation);
    }
}
