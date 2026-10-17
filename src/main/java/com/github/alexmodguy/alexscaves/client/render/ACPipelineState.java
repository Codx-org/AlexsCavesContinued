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

    // 26.3 moved this whole surface out of com.mojang.blaze3d into com.mojang.renderpearl.api
    // .pipeline — RenderPipeline, BlendFunction, ColorTargetState and DepthStencilState from
    // blaze3d.pipeline, CompareOp from blaze3d.platform. Nothing else changed: every builder
    // method keeps its name, both ColorTargetState constructors keep their shapes, and all eight
    // CompareOp constants survive. So this arm is a pure rename of the one below it, plus one
    // addition — the GpuFormat argument that !mc262-colortarget-* inserts on 26.2 is written out
    // here, because a rule matches the ORIGINAL file text and this arm is not the text it was
    // written against.
    //
    // It has to be an arm and not more rules. A rule on com.mojang.blaze3d.pipeline
    // .ColorTargetState would start EARLIER in the line than !mc262-colortarget-blend-no-alpha's
    // match and consume its span, so the GpuFormat insertion would silently stop happening on
    // 26.3 and nothing would point at the cause. These five types are named in no other file, so
    // an arm here costs nothing elsewhere.
    //? if >=26.3 {
    /*public static com.mojang.renderpearl.api.pipeline.RenderPipeline.Builder blend(com.mojang.renderpearl.api.pipeline.RenderPipeline.Builder builder, com.mojang.renderpearl.api.pipeline.BlendFunction blend) {
        return builder.withColorTargetState(new com.mojang.renderpearl.api.pipeline.ColorTargetState(blend));
    }

    public static com.mojang.renderpearl.api.pipeline.RenderPipeline.Builder blendNoAlphaWrite(com.mojang.renderpearl.api.pipeline.RenderPipeline.Builder builder, com.mojang.renderpearl.api.pipeline.BlendFunction blend) {
        return builder.withColorTargetState(new com.mojang.renderpearl.api.pipeline.ColorTargetState(java.util.Optional.of(blend), com.mojang.renderpearl.api.GpuFormat.RGBA8_UNORM, com.mojang.renderpearl.api.pipeline.ColorTargetState.WRITE_COLOR));
    }

    public static com.mojang.renderpearl.api.pipeline.RenderPipeline.Builder noColorWrite(com.mojang.renderpearl.api.pipeline.RenderPipeline.Builder builder) {
        return builder.withColorTargetState(new com.mojang.renderpearl.api.pipeline.ColorTargetState(java.util.Optional.empty(), com.mojang.renderpearl.api.GpuFormat.RGBA8_UNORM, com.mojang.renderpearl.api.pipeline.ColorTargetState.WRITE_NONE));
    }

    public static com.mojang.renderpearl.api.pipeline.RenderPipeline.Builder depth(com.mojang.renderpearl.api.pipeline.RenderPipeline.Builder builder, boolean write) {
        return builder.withDepthStencilState(new com.mojang.renderpearl.api.pipeline.DepthStencilState(NEARER_OR_EQUAL, write));
    }

    public static com.mojang.renderpearl.api.pipeline.RenderPipeline.Builder depthEqual(com.mojang.renderpearl.api.pipeline.RenderPipeline.Builder builder, boolean write) {
        return builder.withDepthStencilState(new com.mojang.renderpearl.api.pipeline.DepthStencilState(com.mojang.renderpearl.api.pipeline.CompareOp.EQUAL, write));
    }

    public static com.mojang.renderpearl.api.pipeline.RenderPipeline.Builder noDepth(com.mojang.renderpearl.api.pipeline.RenderPipeline.Builder builder, boolean write) {
        return write ? builder.withDepthStencilState(new com.mojang.renderpearl.api.pipeline.DepthStencilState(com.mojang.renderpearl.api.pipeline.CompareOp.ALWAYS_PASS, true)) : builder;
    }
    *///?} elif >=26 {
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
        return builder.withDepthStencilState(new com.mojang.blaze3d.pipeline.DepthStencilState(NEARER_OR_EQUAL, write));
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

    // A third arm chain, for the same reason as the second: this moves at 26.2, where the state
    // setters above moved at 26. It is one enum constant and it is the whole of this mod's
    // depth-test semantics.
    //
    // ⚠️ 26.2 switched Minecraft to a REVERSED-Z depth buffer — near is 1.0 and far is 0.0 — and
    // flipped every one of vanilla's own depth tests to match: RenderPipelines states
    // LESS_THAN_OR_EQUAL 15 times on 26.1.2 and GREATER_THAN_OR_EQUAL 14 times on 26.2, and
    // DepthStencilState.DEFAULT moved with them. Nothing about the API changed, so a pipeline
    // that keeps naming LESS_THAN_OR_EQUAL still compiles, still has a depth state, still has a
    // depth attachment, and passes for every fragment that is FARTHER away than what is already
    // in the buffer — i.e. it draws through solid terrain, at any distance, with no log line.
    // Since every render type in this mod is built through depth(), that is the whole mod
    // x-raying on exactly one node.
    //
    // The projection is vanilla's (it arrives through the MATRICES_PROJECTION bind group), so the
    // depth values themselves are already reversed for this mod's draws too; only the comparison
    // was left behind. depthEqual and noDepth need no arm — EQUAL and ALWAYS_PASS mean the same
    // thing whichever way the axis points.
    // 26.3 keeps the reversed-Z sense 26.2 introduced and all eight of CompareOp's constants; only
    // the package moved, from com.mojang.blaze3d.platform to com.mojang.renderpearl.api.pipeline.
    // No rule covers CompareOp — this file is the only place in the tree that names it, and the arm
    // above needs one anyway — so the spelling is written out here.
    //? if >=26.3 {
    /*private static final com.mojang.renderpearl.api.pipeline.CompareOp NEARER_OR_EQUAL =
            com.mojang.renderpearl.api.pipeline.CompareOp.GREATER_THAN_OR_EQUAL;
    *///?} elif >=26.2 {
    /*private static final com.mojang.blaze3d.platform.CompareOp NEARER_OR_EQUAL =
            com.mojang.blaze3d.platform.CompareOp.GREATER_THAN_OR_EQUAL;
    *///?} elif >=26 {
    /*private static final com.mojang.blaze3d.platform.CompareOp NEARER_OR_EQUAL =
            com.mojang.blaze3d.platform.CompareOp.LESS_THAN_OR_EQUAL;
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
    // Two changes at 26.3, not one. The pipeline package moved as everywhere else in this file, and
    // BindGroupLayouts dropped MATRICES_PROJECTION in favour of a plain PROJECTION — 26.2 declares
    // both, 26.3 only the short one.
    //
    // That second half has to be a gated arm rather than a replacements.string rule: matching is
    // plain substring with no boundary check on either edge, and the surviving name PROJECTION is a
    // SUFFIX of the deleted name MATRICES_PROJECTION, so a rule keyed on the old name would also
    // fire inside anything spelling the new one, and a rule keyed on the new name would fire inside
    // the old. There is no spelling of that rule that is not ambiguous.
    // ...and 26.3 does not merely RENAME that layout, it SPLITS it. 26.2's MATRICES_PROJECTION is
    // one BindGroupLayout declaring two uniforms, DynamicTransforms and Projection; 26.3 deletes it
    // and keeps the two as separate layouts, PROJECTION and DYNAMIC_TRANSFORMS, which vanilla's own
    // MATRICES_FOG_SNIPPET adds in that order (read in RenderPipelines.<clinit>, not guessed).
    // Adding only PROJECTION compiles and builds a pipeline that is missing a uniform every core
    // shader this mod names actually declares, and nothing says so until the pipeline is first
    // COMPILED -- i.e. the first frame something draws through it, which for the immediate-mode
    // pipelines is a cave-biome item icon, not startup. The failure is a hard crash reading
    // "Unable to find shader defined uniform (DynamicTransforms)", which names the uniform the
    // SHADER declares and the pipeline lacks, not the other way round.
    //? if >=26.3 {
    /*public static final com.mojang.renderpearl.api.pipeline.RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET =
            com.mojang.renderpearl.api.pipeline.RenderPipeline.builder(net.minecraft.client.renderer.RenderPipelines.GLOBALS_SNIPPET)
                    .withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.PROJECTION)
                    .withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.DYNAMIC_TRANSFORMS)
                    .buildSnippet();
    *///?} elif >=26.2 {
    /*public static final com.mojang.blaze3d.pipeline.RenderPipeline.Snippet MATRICES_PROJECTION_SNIPPET =
            com.mojang.blaze3d.pipeline.RenderPipeline.builder(net.minecraft.client.renderer.RenderPipelines.GLOBALS_SNIPPET)
                    .withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.MATRICES_PROJECTION)
                    .buildSnippet();
    *///?}
}
