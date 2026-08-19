package com.github.alexmodguy.alexscaves.client.render;

//? if <1.21.2
import net.minecraft.client.renderer.ShaderInstance;
//? if >=1.21.5
/*import com.github.alexmodguy.alexscaves.AlexsCaves;*/
//? if >=1.21.5
/*import com.mojang.blaze3d.pipeline.BlendFunction;*/
//? if >=1.21.5
/*import com.mojang.blaze3d.pipeline.RenderPipeline;*/
// 26.2 collapsed the source and destination blend-factor enums into one BlendFactor holding the
// union of their constants. The import is gated rather than renamed because two rules mapping two
// source spellings onto the same target string is exactly the "Ambiguous replacement" shape
// Stonecutter rejects at configuration time; the single use of the two, in EYES_ALPHA_BLEND, sits
// inside the big >=1.21.5 arm where a gate cannot nest, so that one line is a rule instead.
//? if >=26.2 {
/*import com.mojang.blaze3d.platform.BlendFactor;
*///?} elif >=1.21.5 {
/*import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
*///?}
//? if >=1.21.5
/*import com.mojang.blaze3d.shaders.UniformType;*/
//? if >=1.21.5
/*import com.mojang.blaze3d.vertex.DefaultVertexFormat;*/
//? if >=1.21.5
/*import com.mojang.blaze3d.vertex.VertexFormat;*/
//? if >=1.21.5
/*import net.minecraft.client.renderer.RenderPipelines;*/
//? if >=1.21.5
/*import net.minecraft.resources.ResourceLocation;*/

import javax.annotation.Nullable;

/**
 * This mod's eight core shaders — nine from 1.21.2, which adds a lightmap of its own.
 *
 * <p>Up to 1.21.1 a mod compiled its own {@code ShaderInstance} during {@code RegisterShadersEvent}
 * and handed the loader a callback to stash the compiled object in; a render type then reached for
 * it through a supplier, because the instance did not exist until the resource reload had run.
 * Hence the getter/setter pairs below.
 *
 * <p>1.21.2 inverted that. A shader is now declared as a {@link net.minecraft.client.renderer.ShaderProgram}
 * — a config id, a vertex format and a set of preprocessor defines — and the client compiles and
 * recompiles it on its own; {@code CompiledShaderProgram} never passes through mod code. So the
 * declarations become constants, registration is one call per constant with nothing to store, and
 * {@code ShaderStateShard} takes the program itself rather than a supplier.
 *
 * <p>The config id gains a {@code core/} segment because {@code ShaderManager} resolves it against
 * the whole {@code shaders} tree rather than {@code shaders/core} — the JSON files do not move. The
 * {@code vertex}/{@code fragment} ids inside them are rewritten to match at build time; see
 * {@code DataPackMigration.migrateShaderProgramsTo1212}.
 *
 * <p>1.21.5 went further and replaced the program declaration with a whole
 * {@code RenderPipeline}: the two shader stages plus every piece of fixed-function state a render
 * type used to carry in its composite — blend, cull, depth test and depth/colour write masks, and
 * the vertex format. So from 1.21.5 this class is not a list of nine programs but the list of
 * pipelines {@link ACRenderTypes} draws with, which is larger: the eight mod shaders each become
 * one pipeline per state combination they were used in, and several render types that ran a
 * <em>vanilla</em> shader with non-vanilla state need a pipeline of their own too.
 */
public class ACInternalShaders {

    //? if >=1.21.5 {
    /*// Nothing below is registered. RenderPipelines#register exists only to make ShaderManager
    // eagerly compile a pipeline on resource reload and shout if it fails; the lazy path —
    // GlRenderPass#setPipeline -> GpuDevice#getOrCompilePipeline -> the same ShaderManager source
    // resolver Minecraft hands the device — compiles an unregistered pipeline on first use just as
    // well. Skipping registration keeps this loader-neutral: NeoForge has
    // RegisterRenderPipelinesEvent, Forge does not.
    //
    // Every pipeline restates its vanilla base chain through RenderPipeline#builder rather than
    // deriving from the vanilla constant with RenderPipeline#toBuilder — toBuilder is a NeoForge
    // addition and does not exist on Forge.

    private static final BlendFunction EYES_ALPHA_BLEND = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);

    // --- the vanilla base chains, minus their locations -------------------------------------

    private static RenderPipeline.Builder entityTranslucent() {
        return ACPipelineState.blend(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                .withSampler("Sampler1"), BlendFunction.TRANSLUCENT)
                .withCull(false);
    }

    private static RenderPipeline.Builder entityTranslucentEmissive() {
        return ACPipelineState.depth(ACPipelineState.blend(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                .withShaderDefine("EMISSIVE")
                .withSampler("Sampler1"), BlendFunction.TRANSLUCENT)
                .withCull(false), false);
    }

    private static RenderPipeline.Builder energySwirl() {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_FOG_SNIPPET)
                .withVertexShader("core/entity")
                .withFragmentShader("core/entity")
                .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                .withShaderDefine("EMISSIVE")
                .withShaderDefine("NO_OVERLAY")
                .withShaderDefine("NO_CARDINAL_LIGHTING")
                .withShaderDefine("APPLY_TEXTURE_MATRIX")
                .withSampler("Sampler0")
    *///?}
    // 1.21.6 folded TextureMat into the DynamicTransforms uniform block, which every MATRICES_*
    // snippet already carries — so the pipeline must NOT declare it any more (UniformType.MATRIX4X4
    // is gone with the rest of the scalar types). This is a sibling gate, not a nested one: the
    // enclosing >=1.21.5 block is closed above and reopened below, and the builder chain simply
    // continues across the seam.
    //? if >=1.21.5 && <1.21.6 {
    /*                .withUniform("TextureMat", UniformType.MATRIX4X4)
    *///?}
    //? if >=1.21.5 {
    /*                .withCull(false)
                .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS);
        return ACPipelineState.depth(ACPipelineState.blend(builder, BlendFunction.ADDITIVE), true);
    }

    private static RenderPipeline.Builder eyes() {
        return ACPipelineState.depth(ACPipelineState.blend(RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_FOG_SNIPPET)
                .withVertexShader("core/entity")
                .withFragmentShader("core/entity")
                .withShaderDefine("EMISSIVE")
                .withShaderDefine("NO_OVERLAY")
                .withShaderDefine("NO_CARDINAL_LIGHTING")
                .withSampler("Sampler0"), BlendFunction.TRANSLUCENT), false)
                .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS);
    }

    private static RenderPipeline.Builder beaconBeamTranslucent() {
        return ACPipelineState.depth(ACPipelineState.blend(RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET), BlendFunction.TRANSLUCENT), false);
    }

    private static RenderPipeline.Builder waterMask() {
        return ACPipelineState.depth(ACPipelineState.noColorWrite(RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_SNIPPET)
                .withVertexShader("core/rendertype_water_mask")
                .withFragmentShader("core/rendertype_water_mask")), true)
                .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS);
    }

    *///?}
    // 26.2 folded the text base chain into one snippet. WORLD_TEXT_SNIPPET is TEXT_SNIPPET (globals,
    // matrices, Sampler0, translucent blend, the default depth state and a POSITION_TEX_LIGHTMAP_COLOR
    // quad binding) plus the fog bind group and Sampler2 — i.e. exactly the two snippets and the two
    // samplers this call used to name one at a time, which is why the arm is shorter rather than
    // wider. The shader also lost its rendertype_ prefix: vanilla's own TEXT is
    // builder(WORLD_TEXT_SNIPPET).withVertexShader("core/text").withFragmentShader("core/text"),
    // read out of RenderPipelines.<clinit> in the 26.2 jar.
    //
    // Sibling gate, as everywhere in this file: the enclosing >=1.21.5 arm is closed above and
    // reopened below. This one has to hold the whole method rather than a line, because the two
    // versions disagree about how many calls there are, not just what one of them says.
    //? if >=26.2 {
    /*private static RenderPipeline.Builder text() {
        return RenderPipeline.builder(RenderPipelines.WORLD_TEXT_SNIPPET)
                .withVertexShader("core/text")
                .withFragmentShader("core/text");
    }
    *///?} elif >=1.21.5 {
    /*private static RenderPipeline.Builder text() {
        return RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET, RenderPipelines.FOG_SNIPPET)
                .withVertexShader("core/rendertype_text")
                .withFragmentShader("core/rendertype_text")
                .withSampler("Sampler0")
                .withSampler("Sampler2");
    }
    *///?}
    //? if >=1.21.5 {
    /*
    private static RenderPipeline.Builder lightning() {
        return ACPipelineState.depth(ACPipelineState.blend(RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_FOG_SNIPPET)
                .withVertexShader("core/rendertype_lightning")
                .withFragmentShader("core/rendertype_lightning"), BlendFunction.LIGHTNING), true)
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS);
    }

    // --- vanilla shaders, non-vanilla state --------------------------------------------------

    // The energy-swirl shader blended translucently rather than additively: particle trails, the
    // void being's cloud, tesla bulbs and the tremorzilla beam.
    public static final RenderPipeline ENERGY_SWIRL_TRANSLUCENT = ACPipelineState.blend(energySwirl()
            .withLocation(acId("pipeline/energy_swirl_translucent")), BlendFunction.TRANSLUCENT)
            .build();

    // ...and the same thing drawn as a triangle strip, for the raygun ray.
    public static final RenderPipeline ENERGY_SWIRL_TRANSLUCENT_TRIANGLES = ACPipelineState.blend(energySwirl()
            .withLocation(acId("pipeline/energy_swirl_translucent_triangles")), BlendFunction.TRANSLUCENT)
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES)
            .build();

    // RenderType.entityTranslucentCull, which 1.21.2 deleted: plain entity-translucent, culled.
    public static final RenderPipeline ENTITY_TRANSLUCENT_CULL = entityTranslucent()
            .withLocation(acId("pipeline/entity_translucent_cull"))
            .withCull(true)
            .build();

    // The eyes shader with the mod's separate-alpha blend, drawn only where depth already matches.
    public static final RenderPipeline EYES_ALPHA = ACPipelineState.depthEqual(ACPipelineState.blend(eyes()
            .withLocation(acId("pipeline/eyes_alpha")), EYES_ALPHA_BLEND)
            .withCull(false), true)
            .build();

    // The lightning shader under that same blend: the ambersol shine and the nucleeper's lights.
    public static final RenderPipeline LIGHTNING_EYES_ALPHA = ACPipelineState.blend(lightning()
            .withLocation(acId("pipeline/lightning_eyes_alpha")), EYES_ALPHA_BLEND)
            .build();

    // ...and translucent: hologram lights, the crucible beam, submarine lights. This is also where
    // the mod's own HOLOGRAM program lands, because its JSON only ever pointed at vanilla's
    // lightning shader — there is no separate hologram-lights program to keep.
    public static final RenderPipeline LIGHTNING_TRANSLUCENT = ACPipelineState.blend(lightning()
            .withLocation(acId("pipeline/lightning_translucent")), BlendFunction.TRANSLUCENT)
            .build();

    // The submarine's depth-only mask, uncalled.
    public static final RenderPipeline WATER_MASK_NO_CULL = waterMask()
            .withLocation(acId("pipeline/water_mask_no_cull"))
            .withCull(false)
            .build();

    // Entity-translucent-emissive that still writes depth.
    public static final RenderPipeline GHOSTLY = ACPipelineState.depth(entityTranslucentEmissive()
            .withLocation(acId("pipeline/ghostly")), true)
            .build();

    // The beacon-beam shader over a plain textured quad, writing depth.
    public static final RenderPipeline HOLOGRAM = ACPipelineState.depth(beaconBeamTranslucent()
            .withLocation(acId("pipeline/hologram"))
            .withCull(false), true)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS)
            .build();

    // The text shader, both ways round — the cave map draws its background double-sided.
    public static final RenderPipeline CAVE_MAP_BACKGROUND_CULL = text()
            .withLocation(acId("pipeline/cave_map_background_cull"))
            .withCull(true)
            .build();

    public static final RenderPipeline CAVE_MAP_BACKGROUND_NO_CULL = text()
            .withLocation(acId("pipeline/cave_map_background_no_cull"))
            .withCull(false)
            .build();

    // --- this mod's own shaders ---------------------------------------------------------------

    public static final RenderPipeline FERROUSLIME_GEL = entityTranslucent()
            .withLocation(acId("pipeline/ferrouslime_gel"))
            .withVertexShader(acId("core/rendertype_ferrouslime_gel"))
            .withFragmentShader(acId("core/rendertype_ferrouslime_gel"))
            .withUniform("GameTime", UniformType.FLOAT)
            .build();

    public static final RenderPipeline FERROUSLIME_GEL_TRIANGLES = entityTranslucent()
            .withLocation(acId("pipeline/ferrouslime_gel_triangles"))
            .withVertexShader(acId("core/rendertype_ferrouslime_gel"))
            .withFragmentShader(acId("core/rendertype_ferrouslime_gel"))
            .withUniform("GameTime", UniformType.FLOAT)
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES)
            .build();

    public static final RenderPipeline BUBBLED_CULL = ACPipelineState.depth(entityTranslucent()
            .withLocation(acId("pipeline/bubbled_cull"))
            .withVertexShader(acId("core/rendertype_bubbled"))
            .withFragmentShader(acId("core/rendertype_bubbled"))
            .withUniform("GameTime", UniformType.FLOAT)
            .withCull(true), true)
            .build();

    public static final RenderPipeline BUBBLED_NO_CULL = ACPipelineState.depth(entityTranslucent()
            .withLocation(acId("pipeline/bubbled_no_cull"))
            .withVertexShader(acId("core/rendertype_bubbled"))
            .withFragmentShader(acId("core/rendertype_bubbled"))
            .withUniform("GameTime", UniformType.FLOAT), true)
            .build();

    // Sepia and red-ghost replace only the fragment stage; their JSONs always named a vanilla
    // vertex shader, which on 1.21.5 is core/entity for both.
    public static final RenderPipeline SEPIA = entityTranslucent()
            .withLocation(acId("pipeline/sepia"))
            .withFragmentShader(acId("core/rendertype_sepia"))
            .build();

    public static final RenderPipeline RED_GHOST = ACPipelineState.depth(ACPipelineState.blend(entityTranslucentEmissive()
            .withLocation(acId("pipeline/red_ghost"))
            .withFragmentShader(acId("core/rendertype_red_ghost")), EYES_ALPHA_BLEND), true)
            .build();

    public static final RenderPipeline IRRADIATED = irradiated("irradiated");

    public static final RenderPipeline BLUE_IRRADIATED = irradiated("blue_irradiated");

    public static final RenderPipeline PURPLE_WITCH = ACPipelineState.depth(ACPipelineState.blend(RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_SNIPPET)
            .withLocation(acId("pipeline/purple_witch"))
            .withVertexShader(acId("core/rendertype_purple_witch"))
            .withFragmentShader(acId("core/rendertype_purple_witch"))
            .withSampler("Sampler0")
            .withUniform("GameTime", UniformType.FLOAT), BlendFunction.LIGHTNING)
            .withCull(false), true)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS)
            .build();

    // The ninth shader, which exists only from 1.21.2: vanilla's lightmap with this mod's two
    // insertions. See mixin.client.LightTextureMixin.
    public static final RenderPipeline LIGHTMAP = ACPipelineState.noDepth(RenderPipeline.builder()
            .withLocation(acId("pipeline/lightmap"))
            .withVertexShader("core/blit_screen")
            .withFragmentShader(acId("core/ac_lightmap"))
    *///?}
    // 1.21.6 replaced every scalar uniform type with a std140 uniform *block*, so the eleven
    // declarations below collapse into one buffer — vanilla's own LightmapInfo plus this mod's two
    // additions, in the order `LightTextureMixin` writes them. Sibling gate, as above.
    //? if >=1.21.6 {
    /*            .withUniform("LightmapInfo", UniformType.UNIFORM_BUFFER)
    *///?} elif >=1.21.5 {
    /*            .withUniform("AmbientLightFactor", UniformType.FLOAT)
            .withUniform("SkyFactor", UniformType.FLOAT)
            .withUniform("BlockFactor", UniformType.FLOAT)
            .withUniform("UseBrightLightmap", UniformType.INT)
            .withUniform("SkyLightColor", UniformType.VEC3)
            .withUniform("NightVisionFactor", UniformType.FLOAT)
            .withUniform("DarknessScale", UniformType.FLOAT)
            .withUniform("DarkenWorldFactor", UniformType.FLOAT)
            .withUniform("BrightnessFactor", UniformType.FLOAT)
            .withUniform("ACAmbientLight", UniformType.FLOAT)
            .withUniform("ACLightColor", UniformType.VEC3)
    *///?}
    // 1.21.9 deleted the shared full-screen quad (RenderSystem.getQuadVertexBuffer) and the
    // core/blit_screen *vertex* stage that consumed it. Every full-screen pass now runs
    // core/screenquad, which builds one oversized triangle out of gl_VertexID alone — hence an
    // empty vertex format, TRIANGLES, and a bare draw(0, 3) at the call site. The !mc219-screenquad
    // rule swaps the vertex shader; only the format has to be gated, because the same POSITION/QUADS
    // pair is also what waterMask() legitimately uses. Sibling gate, as everywhere in this file.
    //? if >=1.21.9 {
    /*            .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
    *///?} elif >=1.21.5 {
    /*            .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
    *///?}
    //? if >=1.21.5 {
    /*            , false).build();

    // The blit PostEffectRegistry uses to lay a finished post-effect buffer back over the world.
    // Byte for byte vanilla's ENTITY_OUTLINE_BLIT with a different blend function: BlendFunction
    // .OVERLAY is (SRC_ALPHA, ONE, ONE, ZERO), exactly the blendFuncSeparate the pre-1.21.5 path
    // set by hand. Vanilla's own blitAndBlendToScreen already forced colorMask(_, _, _, false)
    // and disabled the depth test, so withColorWrite(true, false) + NO_DEPTH_TEST is what that
    // path did, not a new choice.
    public static final RenderPipeline POST_EFFECT_BLIT = ACPipelineState.noDepth(ACPipelineState.blendNoAlphaWrite(RenderPipeline.builder()
            .withLocation(acId("pipeline/post_effect_blit"))
            .withVertexShader("core/blit_screen")
            .withFragmentShader("core/blit_screen")
            .withSampler("InSampler"), BlendFunction.OVERLAY), false)
    *///?}
    //? if >=1.21.9 {
    /*            .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
    *///?} elif >=1.21.5 {
    /*            .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
    *///?}
    //? if >=1.21.5 {
    /*            .build();

    // --- the immediate-mode draws -------------------------------------------------------------
    //
    // Up to 1.21.4 a hand-rolled tesselator draw named a core shader and set its own blend/depth/
    // cull state through RenderSystem, then went out through BufferUploader. 1.21.5 deleted all of
    // that: RenderSystem's fixed-function setters and BufferUploader are gone, and the only way to
    // put a MeshData on screen is RenderType#draw — which means every such draw needs a pipeline
    // carrying the state its call site used to set by hand. These are those pipelines, one per
    // (shader, vertex format, primitive mode, state) shape the mod actually draws; ACRenderTypes
    // wraps each in a RenderType and ACClientCompat#beginImmediate picks between them.
    //
    // All of them are the vanilla core shader the old code named, over MATRICES_COLOR_SNIPPET,
    // which is what those shaders declare: ModelViewMat, ProjMat and ColorModulator — so
    // RenderSystem#setShaderColor, which survives 1.21.5, still tints them exactly as before.

    // Split in two only so that IMMEDIATE_SCREEN_OVERLAY, the one caller that wants no depth test
    // at all, can reach a builder that has never had a depth state set: from 26 "no depth test" is
    // the ABSENCE of one (ACPipelineState#noDepth is a no-op there), so it cannot undo a depth
    // state a shared base chain already applied.
    private static RenderPipeline.Builder immediate(String name, String shader) {
        return ACPipelineState.depth(immediateNoDepth(name, shader), true);
    }

    private static RenderPipeline.Builder immediateNoDepth(String name, String shader) {
        return ACPipelineState.blend(RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_SNIPPET)
                .withLocation(acId("pipeline/immediate_" + name))
                .withVertexShader("core/" + shader)
                .withFragmentShader("core/" + shader), BlendFunction.TRANSLUCENT);
    }

    // The pathfinding debug overlay's filled shapes and lines. Upstream enabled blending only when
    // the requested alpha was not 255; translucent blending at full alpha is the identity, so one
    // always-blending pipeline covers both branches.
    public static final RenderPipeline IMMEDIATE_POSITION_COLOR_FAN = immediate("position_color_fan", "position_color")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_FAN)
            .build();

    public static final RenderPipeline IMMEDIATE_POSITION_COLOR_LINES = immediate("position_color_lines", "position_color")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.DEBUG_LINES)
            .build();

    public static final RenderPipeline IMMEDIATE_POSITION_TEX_FAN = immediate("position_tex_fan", "position_tex")
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLE_FAN)
            .build();

    public static final RenderPipeline IMMEDIATE_POSITION_TEX_TRIANGLES = immediate("position_tex_triangles", "position_tex")
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLES)
            .build();

    public static final RenderPipeline IMMEDIATE_POSITION_TEX_QUADS = immediate("position_tex_quads", "position_tex")
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
            .build();

    // The two full-screen overlays ClientProxy draws over the world before the GUI — the nuke
    // flash and the watcher's possession vignette. They ran with the depth test off and the depth
    // mask closed, which is exactly vanilla's GUI_OVERLAY state.
    public static final RenderPipeline IMMEDIATE_SCREEN_OVERLAY = ACPipelineState.noDepth(immediateNoDepth("screen_overlay", "position_tex")
            .withSampler("Sampler0"), false)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
            .build();

    // Citadel's item-stack renderer draws its quads with the PARTICLE format but the plain
    // position_tex shader, so the per-vertex colour and lightmap it writes are ignored — that is
    // upstream's behaviour, reproduced rather than corrected. The format is kept because the
    // vertex calls that fill it are shared with every other version.
    public static final RenderPipeline IMMEDIATE_PARTICLE_QUADS = immediate("particle_quads", "position_tex")
            .withSampler("Sampler0")
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.PARTICLE, VertexFormat.Mode.QUADS)
            .build();

    // ColorBlitHelper's tinted blit: vanilla's own GUI-textured shape, which is why the format is
    // POSITION_TEX_COLOR and not the POSITION_COLOR_TEX the call site spells on older versions.
    public static final RenderPipeline IMMEDIATE_POSITION_TEX_COLOR_QUADS = immediate("position_tex_color_quads", "position_tex_color")
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .build();

    private static RenderPipeline irradiated(String name) {
        return ACPipelineState.depth(ACPipelineState.blend(RenderPipeline.builder(RenderPipelines.MATRICES_COLOR_SNIPPET)
                .withLocation(acId("pipeline/" + name))
                .withVertexShader(acId("core/ac_position_color_tex"))
                .withFragmentShader(acId("core/rendertype_" + name))
                .withSampler("Sampler0")
                .withUniform("GameTime", UniformType.FLOAT), BlendFunction.TRANSLUCENT)
                .withCull(false), true)
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS)
                .build();
    }

    private static ResourceLocation acId(String path) {
        return ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, path);
    }
    *///?} elif >=1.21.2 {
    /*public static final net.minecraft.client.renderer.ShaderProgram FERROUSLIME_GEL = program("rendertype_ferrouslime_gel", com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY);
    public static final net.minecraft.client.renderer.ShaderProgram HOLOGRAM = program("rendertype_hologram", com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
    public static final net.minecraft.client.renderer.ShaderProgram IRRADIATED = program("rendertype_irradiated", com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR_TEX);
    public static final net.minecraft.client.renderer.ShaderProgram BLUE_IRRADIATED = program("rendertype_blue_irradiated", com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR_TEX);
    public static final net.minecraft.client.renderer.ShaderProgram BUBBLED = program("rendertype_bubbled", com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY);
    public static final net.minecraft.client.renderer.ShaderProgram SEPIA = program("rendertype_sepia", com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY);
    public static final net.minecraft.client.renderer.ShaderProgram RED_GHOST = program("rendertype_red_ghost", com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY);
    public static final net.minecraft.client.renderer.ShaderProgram PURPLE_WITCH = program("rendertype_purple_witch", com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY);

    // A ninth shader that exists only from 1.21.2, where the lightmap moved onto the GPU: a copy of
    // vanilla's core/lightmap carrying this mod's two insertions. See mixin.client.LightTextureMixin.
    // Its JSON is already written in the 1.21.2 layout — fully qualified namespace:core/name vertex
    // and fragment ids — so the build-time rewrite leaves it alone.
    public static final net.minecraft.client.renderer.ShaderProgram LIGHTMAP = program("ac_lightmap", com.mojang.blaze3d.vertex.DefaultVertexFormat.BLIT_SCREEN);

    private static net.minecraft.client.renderer.ShaderProgram program(String name, com.mojang.blaze3d.vertex.VertexFormat format) {
        return new net.minecraft.client.renderer.ShaderProgram(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.github.alexmodguy.alexscaves.AlexsCaves.MODID, "core/" + name),
                format,
                net.minecraft.client.renderer.ShaderDefines.EMPTY);
    }

    public static java.util.List<net.minecraft.client.renderer.ShaderProgram> all() {
        return java.util.List.of(FERROUSLIME_GEL, HOLOGRAM, IRRADIATED, BLUE_IRRADIATED, BUBBLED, SEPIA, RED_GHOST, PURPLE_WITCH, LIGHTMAP);
    }
    *///?} else {

    private static ShaderInstance renderTypeFerrouslimeGelShader;
    private static ShaderInstance renderTypeHologramShader;
    private static ShaderInstance renderTypeIrradiatedShader;
    private static ShaderInstance renderTypeBlueIrradiatedShader;
    private static ShaderInstance renderTypeBubbledShader;
    private static ShaderInstance renderTypeSepiaShader;
    private static ShaderInstance renderTypeSepiaOutlineShader;
    private static ShaderInstance renderTypeRedGhostShader;
    private static ShaderInstance renderTypePurpleWitchShader;

    @Nullable
    public static ShaderInstance getRenderTypeFerrouslimeGelShader() {
        return renderTypeFerrouslimeGelShader;
    }

    public static void setRenderTypeFerrouslimeGelShader(ShaderInstance instance) {
        renderTypeFerrouslimeGelShader = instance;
    }

    public static void setRenderTypeHologramShader(ShaderInstance instance) {
        renderTypeHologramShader = instance;
    }

    @Nullable
    public static ShaderInstance getRenderTypeHologramShader() {
        return renderTypeHologramShader;
    }

    @Nullable
    public static ShaderInstance getRenderTypeIrradiatedShader() {
        return renderTypeIrradiatedShader;
    }

    public static void setRenderTypeIrradiatedShader(ShaderInstance instance) {
        renderTypeIrradiatedShader = instance;
    }

    @Nullable
    public static ShaderInstance getRenderTypeBlueIrradiatedShader() {
        return renderTypeBlueIrradiatedShader;
    }

    public static void setRenderTypeBlueIrradiatedShader(ShaderInstance instance) {
        renderTypeBlueIrradiatedShader = instance;
    }

    @Nullable
    public static ShaderInstance getRenderTypeBubbledShader() {
        return renderTypeBubbledShader;
    }

    public static void setRenderTypeBubbledShader(ShaderInstance instance) {
        renderTypeBubbledShader = instance;
    }

    @Nullable
    public static ShaderInstance getRenderTypeSepiaShader() {
        return renderTypeSepiaShader;
    }

    public static void setRenderTypeSepiaShader(ShaderInstance instance) {
        renderTypeSepiaShader = instance;
    }

    @Nullable
    public static ShaderInstance getRenderTypeRedGhostShader() {
        return renderTypeRedGhostShader;
    }

    public static void setRenderTypeRedGhostShader(ShaderInstance instance) {
        renderTypeRedGhostShader = instance;
    }

    @Nullable
    public static ShaderInstance getRenderTypePurpleWitchShader() {
        return renderTypePurpleWitchShader;
    }

    public static void setRenderTypePurpleWitchShader(ShaderInstance instance) {
        renderTypePurpleWitchShader = instance;
    }
    //?}

}
