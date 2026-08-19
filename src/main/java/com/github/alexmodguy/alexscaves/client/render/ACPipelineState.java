package com.github.alexmodguy.alexscaves.client.render;

/**
 * The blend, depth and colour-mask half of {@code RenderPipeline.Builder}, behind names of this
 * mod's own.
 *
 * <p>26.1 collapsed six per-aspect setters into two record-valued ones:
 * {@code withDepthTestFunction} + {@code withDepthWrite} + {@code withDepthBias} became
 * {@code withDepthStencilState(DepthStencilState)}, and {@code withBlend} + {@code withColorWrite}
 * became {@code withColorTargetState(ColorTargetState)}. That cannot be a
 * {@code replacements.string} rule: two adjacent calls have to fold into one, and a rule rewrites
 * one span at a time. Nor can the difference be gated where it is used — every call site already
 * lives inside a {@code >=1.21.5} arm and Stonecutter cannot nest a {@code >=26} gate in it. So it
 * is hoisted here, into a class whose whole body is a two-armed chain.
 *
 * <p>The second reason this is not a rename: <b>26.1 made the depth state optional and defaulted it
 * to <em>absent</em></b> ({@code RenderPipeline$Builder#build} resolves it with
 * {@code Optional.orElse(null)}), where 1.21.5–1.21.11 defaulted to LEQUAL with depth writing on.
 * A chain that never mentioned depth therefore silently loses depth testing on 26 unless its
 * snippet supplies one — {@code ENTITY_SNIPPET}, {@code ENTITY_EMISSIVE_SNIPPET},
 * {@code BEACON_BEAM_SNIPPET} and {@code TEXT_SNIPPET} do ({@code DepthStencilState.DEFAULT}),
 * while {@code MATRICES_FOG_SNIPPET} and {@code MATRICES_PROJECTION_SNIPPET} do not. Every caller
 * consequently states its depth explicitly through {@link #depth}; below 26 that restates the old
 * default and is a no-op, so the pre-26 nodes render exactly as they did.
 *
 * <p>{@code NO_DEPTH_TEST} has no successor constant. {@code CompareOp.ALWAYS_PASS} exists but
 * vanilla's own {@code RenderPipelines} never uses it — {@code core/gui}, which was
 * {@code NO_DEPTH_TEST} on 1.21.11, simply omits {@code withDepthStencilState} on 26.1. So
 * {@link #noDepth} <em>deletes</em> the call rather than translating it, which is why it must be
 * handed a builder that has never had a depth state set: there is no way to unset one. The single
 * exception is a pipeline that wants no depth <em>test</em> while still <em>writing</em> depth —
 * only {@code WorldRenderMacros}' glint lines do — which cannot be expressed by absence and is the
 * one place {@code ALWAYS_PASS} is used.
 */
public class ACPipelineState {

    //? if >=26 {
    /*public static com.mojang.blaze3d.pipeline.RenderPipeline.Builder blend(com.mojang.blaze3d.pipeline.RenderPipeline.Builder builder, com.mojang.blaze3d.pipeline.BlendFunction blend) {
        return builder.withColorTargetState(new com.mojang.blaze3d.pipeline.ColorTargetState(blend));
    }

    public static com.mojang.blaze3d.pipeline.RenderPipeline.Builder blendNoAlphaWrite(com.mojang.blaze3d.pipeline.RenderPipeline.Builder builder, com.mojang.blaze3d.pipeline.BlendFunction blend) {
        return builder.withColorTargetState(new com.mojang.blaze3d.pipeline.ColorTargetState(java.util.Optional.of(blend), com.mojang.blaze3d.pipeline.ColorTargetState.WRITE_COLOR));
    }

    public static com.mojang.blaze3d.pipeline.RenderPipeline.Builder noColorWrite(com.mojang.blaze3d.pipeline.RenderPipeline.Builder builder) {
        return builder.withColorTargetState(new com.mojang.blaze3d.pipeline.ColorTargetState(java.util.Optional.empty(), com.mojang.blaze3d.pipeline.ColorTargetState.WRITE_NONE));
    }

    public static com.mojang.blaze3d.pipeline.RenderPipeline.Builder depth(com.mojang.blaze3d.pipeline.RenderPipeline.Builder builder, boolean write) {
        return builder.withDepthStencilState(new com.mojang.blaze3d.pipeline.DepthStencilState(com.mojang.blaze3d.platform.CompareOp.LESS_THAN_OR_EQUAL, write));
    }

    public static com.mojang.blaze3d.pipeline.RenderPipeline.Builder depthEqual(com.mojang.blaze3d.pipeline.RenderPipeline.Builder builder, boolean write) {
        return builder.withDepthStencilState(new com.mojang.blaze3d.pipeline.DepthStencilState(com.mojang.blaze3d.platform.CompareOp.EQUAL, write));
    }

    public static com.mojang.blaze3d.pipeline.RenderPipeline.Builder noDepth(com.mojang.blaze3d.pipeline.RenderPipeline.Builder builder, boolean write) {
        return write ? builder.withDepthStencilState(new com.mojang.blaze3d.pipeline.DepthStencilState(com.mojang.blaze3d.platform.CompareOp.ALWAYS_PASS, true)) : builder;
    }
    *///?} elif >=1.21.5 {
    /*public static com.mojang.blaze3d.pipeline.RenderPipeline.Builder blend(com.mojang.blaze3d.pipeline.RenderPipeline.Builder builder, com.mojang.blaze3d.pipeline.BlendFunction blend) {
        return builder.withBlend(blend);
    }

    public static com.mojang.blaze3d.pipeline.RenderPipeline.Builder blendNoAlphaWrite(com.mojang.blaze3d.pipeline.RenderPipeline.Builder builder, com.mojang.blaze3d.pipeline.BlendFunction blend) {
        return builder.withBlend(blend).withColorWrite(true, false);
    }

    public static com.mojang.blaze3d.pipeline.RenderPipeline.Builder noColorWrite(com.mojang.blaze3d.pipeline.RenderPipeline.Builder builder) {
        return builder.withColorWrite(false);
    }

    public static com.mojang.blaze3d.pipeline.RenderPipeline.Builder depth(com.mojang.blaze3d.pipeline.RenderPipeline.Builder builder, boolean write) {
        return builder.withDepthTestFunction(com.mojang.blaze3d.platform.DepthTestFunction.LEQUAL_DEPTH_TEST).withDepthWrite(write);
    }

    public static com.mojang.blaze3d.pipeline.RenderPipeline.Builder depthEqual(com.mojang.blaze3d.pipeline.RenderPipeline.Builder builder, boolean write) {
        return builder.withDepthTestFunction(com.mojang.blaze3d.platform.DepthTestFunction.EQUAL_DEPTH_TEST).withDepthWrite(write);
    }

    public static com.mojang.blaze3d.pipeline.RenderPipeline.Builder noDepth(com.mojang.blaze3d.pipeline.RenderPipeline.Builder builder, boolean write) {
        return builder.withDepthTestFunction(com.mojang.blaze3d.platform.DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(write);
    }
    *///?}

    // A second, independent arm chain: the snippet below moves at a different version from the
    // state setters above, so it gets its own gate rather than more arms in theirs.
    //
    // 26.2 deleted RenderPipelines.MATRICES_PROJECTION_SNIPPET along with every other snippet whose
    // name described a *uniform set* rather than a draw shape — a pipeline names BindGroupLayouts
    // now, and a snippet exists only to bundle a whole base chain. The one this mod wants still has
    // a name in vanilla's own code, just not a public one: MATRICES_FOG_SNIPPET is built as
    // builder(GLOBALS_SNIPPET).withBindGroupLayout(MATRICES_PROJECTION).withBindGroupLayout(FOG),
    // and WATER_MASK — which needs no fog — restates the first two calls inline. This is that
    // composition, under the name the rest of the mod already spells, so !mc216-snippet-matrices
    // can keep pointing every MATRICES_COLOR_SNIPPET at a live constant.
    //
    // GLOBALS is at the root deliberately: it is what carries GameTime, which the six animated
    // shaders read, and declaring it here is what makes deleting their withUniform("GameTime")
    // calls a translation rather than a loss.
    //? if >=26.2 {
    /*public static final com.mojang.blaze3d.pipeline.RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET =
            com.mojang.blaze3d.pipeline.RenderPipeline.builder(net.minecraft.client.renderer.RenderPipelines.GLOBALS_SNIPPET)
                    .withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.MATRICES_PROJECTION)
                    .buildSnippet();
    *///?}
}
