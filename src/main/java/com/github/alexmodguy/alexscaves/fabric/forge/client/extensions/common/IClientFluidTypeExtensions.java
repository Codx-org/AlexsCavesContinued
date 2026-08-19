package com.github.alexmodguy.alexscaves.fabric.forge.client.extensions.common;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Fabric stand-in for the client half of a fluid type — the three textures a modded fluid answers
 * with.
 *
 * <p>Only the three this tree overrides are declared. The loader's interface also carries a tint
 * colour, a fog colour and a fog-shape hook; neither of this mod's fluids overrides any of them, so
 * declaring them would be three more defaults nothing could reach.
 *
 * <p>The defaults return {@code null} rather than the missing-texture sprite the loader falls back
 * to, because on this loader a default can never actually run: both implementations here are
 * anonymous classes that override all three, and the only gate over any of them ({@code !neoforge ||
 * <26}) is true on every Fabric node. A default that returned a real texture would be a value nothing
 * asked for, arriving through a path that does not exist.
 *
 * <p>⚠️ The <b>consumer</b> of the two atlas sprites is not wired. On the other two loaders the
 * loader itself reads them when it bakes the fluid's model; this loader's answer is fabric-api's
 * {@code FluidRenderHandlerRegistry}, which wants the same two sprites plus a tint and belongs with
 * the rest of the dispatcher work. The overlay is different — it is a screen texture drawn by this
 * mod's own client code, so it needs no registration at all.
 */
public interface IClientFluidTypeExtensions {

    default ResourceLocation getStillTexture() {
        return null;
    }

    default ResourceLocation getFlowingTexture() {
        return null;
    }

    default ResourceLocation getRenderOverlayTexture(Minecraft mc) {
        return null;
    }
}
