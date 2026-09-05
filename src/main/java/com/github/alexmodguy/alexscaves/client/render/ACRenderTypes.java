package com.github.alexmodguy.alexscaves.client.render;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.github.alexmodguy.alexscaves.client.ClientProxy;
import com.github.alexmodguy.alexscaves.citadel.client.shader.PostEffectRegistry;
import com.mojang.blaze3d.pipeline.RenderTarget;
//? if <1.21.5
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
//? if <1.21.11
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ForgeRenderTypes;


/**
 * The mod's render types.
 *
 * <p>Up to 1.21.4 this class extends {@link RenderType} purely so the inherited {@code create}
 * factory and the protected {@code RenderStateShard} constants resolve unqualified, the way
 * upstream wrote them. From 1.21.5 it stops: half of those constants — {@code NO_CULL},
 * {@code CULL}, {@code TRANSLUCENT_TRANSPARENCY}, {@code LEQUAL_DEPTH_TEST}, {@code DEPTH_WRITE},
 * {@code COLOR_DEPTH_WRITE} — no longer exist, because the state they named moved out of the
 * composite and into the {@code RenderPipeline} (see {@link ACInternalShaders}), and inheriting
 * would mean implementing five new abstract methods to keep a handful of statics in scope. The
 * newer arms name {@code RenderType.create} and {@code RenderStateShard.X} in full instead.
 */
public class ACRenderTypes
        //? if <1.21.5
        extends RenderType
{
    // Every factory below returns ONE instance per distinct argument tuple instead of a fresh
    // RenderType per call, which is what upstream did in all of them. See the note on
    // EYES_ALPHA_ENABLED further down: RenderType declares no equals/hashCode on any version in
    // this range, so every map vanilla keys by one -- MultiBufferSource$BufferSource#startedBuilders
    // and #fixedBuffers, and from 26.2 RenderTypeFeatureRenderer$Group#lastRenderType -- compares
    // by IDENTITY, and a per-call instance makes every one of those lookups miss. On the additive
    // types that is visible: it is what made the tremorzilla's glow draw twice in some frames and
    // flash. A ConcurrentHashMap rather than Util.memoize because several of these take two
    // arguments and some take none.
    private static final java.util.Map<Object, RenderType> TYPE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private static RenderType cached(Object key, java.util.function.Supplier<RenderType> builder) {
        RenderType existing = TYPE_CACHE.get(key);
        return existing != null ? existing : TYPE_CACHE.computeIfAbsent(key, k -> builder.get());
    }

    // 1.21.2 made a shader state shard hold the ShaderProgram declaration itself rather than a
    // supplier of the compiled instance — the client owns compilation now. See ACInternalShaders.
    // 1.21.5 deleted ShaderStateShard outright: a render type names one whole RenderPipeline, so
    // there is nothing left to hold here.
    //? if >=1.21.5 {
    /*
    *///?} elif >=1.21.2 {
    /*protected static final RenderStateShard.ShaderStateShard RENDERTYPE_FEROUSSLIME_GEL_SHADER = new RenderStateShard.ShaderStateShard(ACInternalShaders.FERROUSLIME_GEL);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_HOLOGRAM_SHADER = new RenderStateShard.ShaderStateShard(ACInternalShaders.HOLOGRAM);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_IRRADIATED_SHADER = new RenderStateShard.ShaderStateShard(ACInternalShaders.IRRADIATED);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_BLUE_IRRADIATED_SHADER = new RenderStateShard.ShaderStateShard(ACInternalShaders.BLUE_IRRADIATED);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_BUBBLED_SHADER = new RenderStateShard.ShaderStateShard(ACInternalShaders.BUBBLED);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_SEPIA_SHADER = new RenderStateShard.ShaderStateShard(ACInternalShaders.SEPIA);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_RED_GHOST_SHADER = new RenderStateShard.ShaderStateShard(ACInternalShaders.RED_GHOST);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_PURPLE_WITCH_SHADER = new RenderStateShard.ShaderStateShard(ACInternalShaders.PURPLE_WITCH);
    *///?} else {
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_FEROUSSLIME_GEL_SHADER = new RenderStateShard.ShaderStateShard(ACInternalShaders::getRenderTypeFerrouslimeGelShader);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_HOLOGRAM_SHADER = new RenderStateShard.ShaderStateShard(ACInternalShaders::getRenderTypeHologramShader);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_IRRADIATED_SHADER = new RenderStateShard.ShaderStateShard(ACInternalShaders::getRenderTypeIrradiatedShader);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_BLUE_IRRADIATED_SHADER = new RenderStateShard.ShaderStateShard(ACInternalShaders::getRenderTypeBlueIrradiatedShader);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_BUBBLED_SHADER = new RenderStateShard.ShaderStateShard(ACInternalShaders::getRenderTypeBubbledShader);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_SEPIA_SHADER = new RenderStateShard.ShaderStateShard(ACInternalShaders::getRenderTypeSepiaShader);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_RED_GHOST_SHADER = new RenderStateShard.ShaderStateShard(ACInternalShaders::getRenderTypeRedGhostShader);
    protected static final RenderStateShard.ShaderStateShard RENDERTYPE_PURPLE_WITCH_SHADER = new RenderStateShard.ShaderStateShard(ACInternalShaders::getRenderTypePurpleWitchShader);
    //?}

    // 1.21.5 turned an output shard from a bind/unbind pair of Runnables into a Supplier of the
    // target itself — CompositeRenderType#draw asks for it and renders into it, so there is no
    // "restore the main target afterwards" half any more. The depth copy stays inside the supplier
    // because that is the same moment the old setup Runnable ran, and the null fallback is not
    // optional: draw() dereferences the answer immediately, so returning null NPEs where the old
    // shard simply left the main target bound.
    //? if >=1.21.5 {
    /*protected static final RenderStateShard.OutputStateShard IRRADIATED_OUTPUT = acTarget("irradiated_target", ClientProxy.IRRADIATED_SHADER);
    protected static final RenderStateShard.OutputStateShard HOLOGRAM_OUTPUT = acTarget("hologram_target", ClientProxy.HOLOGRAM_SHADER);
    protected static final RenderStateShard.OutputStateShard PURPLE_WITCH_OUTPUT = acTarget("purple_witch_target", ClientProxy.PURPLE_WITCH_SHADER);

    private static RenderStateShard.OutputStateShard acTarget(String name, ResourceLocation shader) {
        return new RenderStateShard.OutputStateShard(name, () -> {
            RenderTarget target = PostEffectRegistry.getRenderTargetFor(shader);
            if (target == null) {
                return Minecraft.getInstance().getMainRenderTarget();
            }
            target.copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
            return target;
        });
    }
    *///?} else {
    protected static final RenderStateShard.OutputStateShard IRRADIATED_OUTPUT = new RenderStateShard.OutputStateShard("irradiated_target", () -> {
        RenderTarget target = PostEffectRegistry.getRenderTargetFor(ClientProxy.IRRADIATED_SHADER);
        if (target != null) {
            target.copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
            target.bindWrite(false);
        }
    }, () -> {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
    });
    protected static final RenderStateShard.OutputStateShard HOLOGRAM_OUTPUT = new RenderStateShard.OutputStateShard("hologram_target", () -> {
        RenderTarget target = PostEffectRegistry.getRenderTargetFor(ClientProxy.HOLOGRAM_SHADER);
        if (target != null) {
            target.copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
            target.bindWrite(false);
        }
    }, () -> {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
    });

    protected static final RenderStateShard.OutputStateShard PURPLE_WITCH_OUTPUT = new RenderStateShard.OutputStateShard("purple_witch_target", () -> {
        RenderTarget target = PostEffectRegistry.getRenderTargetFor(ClientProxy.PURPLE_WITCH_SHADER);
        if (target != null) {
            target.copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
            target.bindWrite(false);
        }
    }, () -> {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
    });

    // The mod's one non-vanilla blend function. From 1.21.5 it is a BlendFunction on the pipeline
    // instead — see ACInternalShaders.EYES_ALPHA_BLEND — and the two GlStateManager factor enums
    // it names moved package with GlStateManager itself (blaze3d.platform -> blaze3d.opengl).
    protected static final RenderStateShard.TransparencyStateShard EYES_ALPHA_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("eyes_alpha_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });


    // Nothing constructs this class — every factory below returns a vanilla RenderType. The
    // constructor only ever existed to satisfy the superclass, and from 1.21.5 there is no
    // superclass to satisfy.
    public ACRenderTypes(String s, VertexFormat format, VertexFormat.Mode mode, int i, boolean b1, boolean b2, Runnable runnable1, Runnable runnable2) {
        super(s, format, mode, i, b1, b2, runnable1, runnable2);
    }
    //?}

    // The texture shard, whose signature changed twice in three versions: two booleans (blur,
    // mipmap) up to 1.21.4, a TriState blur on 1.21.5, and from 1.21.6 no blur argument at all —
    // the shard stopped touching the filter and only sets mipmapping. Every call site above 1.21.4
    // goes through this, so the choice is made once; below 1.21.5 the arms still spell the vanilla
    // constructor inline and nothing calls this.
    //
    // Nineteen of the twenty ask for no blur, which is the default state of every texture the mod
    // loads, so on 1.21.6 they are exactly a TextureStateShard. The twentieth — the particle trail
    // — asks for it, and dropping it there would visibly harden the trail's edges, so that one
    // keeps a shard of its own: vanilla's setup body plus the setFilter call vanilla removed.
    // AbstractTexture#setFilter is still there and still public; it is only the render type that no
    // longer calls it. cutoutTexture() reverting to empty is harmless — it feeds RenderType#outline,
    // which nothing ever asks of the particle trail.
    //
    // The middle arm is >=1.21.2, not >=1.21.5: the TriState blur flag arrives with 1.21.2 on BOTH
    // loaders (javap-checked on forge 53.1.11/54.1.17 and neoforge 21.2.1/21.3.97/21.4.157). The
    // eighteen literal call sites are carried over the same boundary by the four !mc2102-blur-* string
    // rules, which cannot help here because this one passes a variable.
    private static RenderStateShard.EmptyTextureStateShard acTexture(ResourceLocation location, boolean blur, boolean mipmap) {
        //? if >=1.21.11 {
        /*return ACRenderSetup.texture(location, blur, mipmap);
        *///?} elif >=1.21.6 {
        /*if (blur) {
            return new RenderStateShard.EmptyTextureStateShard(() -> {
                net.minecraft.client.renderer.texture.AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(location);
                texture.setFilter(true, mipmap);
                RenderSystem.setShaderTexture(0, texture.getTextureView());
            }, () -> {
            });
        }
        return new RenderStateShard.TextureStateShard(location, mipmap);
        *///?} elif >=1.21.2 {
        /*return new RenderStateShard.TextureStateShard(location, blur ? net.minecraft.util.TriState.TRUE : net.minecraft.util.TriState.FALSE, mipmap);
        *///?} else {
        return new RenderStateShard.TextureStateShard(location, blur, mipmap);
        //?}
    }

    public static RenderType getParticleTrail(ResourceLocation resourceLocation) {
        return cached(java.util.Arrays.asList("getParticleTrail", resourceLocation), () -> buildGetParticleTrail(resourceLocation));
    }

    private static RenderType buildGetParticleTrail(ResourceLocation resourceLocation) {
        //? if >=1.21.5 {
        /*return RenderType.create("particle_trail", 256, true, true, ACInternalShaders.ENERGY_SWIRL_TRANSLUCENT, RenderType.CompositeState.builder().setTextureState(acTexture(resourceLocation, true, true)).setLightmapState(RenderStateShard.LIGHTMAP).setOverlayState(RenderStateShard.OVERLAY).createCompositeState(true));
        *///?} else {
        return create("particle_trail", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, RenderType.CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_ENERGY_SWIRL_SHADER).setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, true, true)).setLightmapState(LIGHTMAP).setCullState(RenderStateShard.NO_CULL).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setOverlayState(OVERLAY).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(true));
        //?}
    }

    public static RenderType getVoidBeingCloud(ResourceLocation resourceLocation) {
        return cached(java.util.Arrays.asList("getVoidBeingCloud", resourceLocation), () -> buildGetVoidBeingCloud(resourceLocation));
    }

    private static RenderType buildGetVoidBeingCloud(ResourceLocation resourceLocation) {
        //? if >=1.21.5 {
        /*return RenderType.create("void_being", 256, true, true, ACInternalShaders.ENERGY_SWIRL_TRANSLUCENT, RenderType.CompositeState.builder().setTextureState(acTexture(resourceLocation, false, true)).setLightmapState(RenderStateShard.LIGHTMAP).setOverlayState(RenderStateShard.OVERLAY).createCompositeState(true));
        *///?} else {
        return create("void_being", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, RenderType.CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_ENERGY_SWIRL_SHADER).setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, false, true)).setLightmapState(LIGHTMAP).setCullState(RenderStateShard.NO_CULL).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setOverlayState(OVERLAY).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(true));
        //?}
    }

    // RenderType.entityTranslucentCull, which 1.21.2 deleted along with its dedicated shader
    // program — vanilla folded the culled variant back into the plain entity-translucent one, whose
    // composite is identical bar the NO_CULL shard. This is that composite with NO_CULL left off,
    // i.e. exactly what the deleted type was, memoized for the same reason vanilla memoized it: it
    // is called once per entity per frame.
    //? if >=1.21.5 {
    /*private static final java.util.function.Function<ResourceLocation, RenderType> ENTITY_TRANSLUCENT_CULL = net.minecraft.Util.memoize(
            (ResourceLocation locationIn) -> RenderType.create("entity_translucent_cull", 1536, true, true, ACInternalShaders.ENTITY_TRANSLUCENT_CULL, RenderType.CompositeState.builder()
                    .setTextureState(acTexture(locationIn, false, false))
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .createCompositeState(true)));
    *///?} elif >=1.21.2 {
    /*private static final java.util.function.Function<ResourceLocation, RenderType> ENTITY_TRANSLUCENT_CULL = net.minecraft.Util.memoize(
            (ResourceLocation locationIn) -> create("entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(true)));
    *///?}

    // The render types behind ACClientCompat#beginImmediate — one per hand-rolled immediate-mode
    // draw shape in the mod, each wrapping the like-named pipeline in ACInternalShaders. They
    // exist only from 1.21.5, because below it those draws still go out through BufferUploader
    // with the state set by hand and never touch a render type at all.
    //
    // Both caches matter: RenderType.create builds a new object every call, and these are called
    // per frame — several times per frame for the pathfinding overlay. The textured kinds go
    // through Util.memoize for the same reason vanilla memoizes GUI_TEXTURED; the untextured ones
    // cannot, because its cache is a ConcurrentHashMap and would reject the null key.
    //? if >=1.21.5 {
    /*private static final java.util.EnumMap<ACClientCompat.ImmediateDraw, RenderType> IMMEDIATE_UNTEXTURED =
            new java.util.EnumMap<>(ACClientCompat.ImmediateDraw.class);
    private static final java.util.EnumMap<ACClientCompat.ImmediateDraw, java.util.function.Function<ResourceLocation, RenderType>> IMMEDIATE_TEXTURED =
            new java.util.EnumMap<>(ACClientCompat.ImmediateDraw.class);

    public static RenderType getImmediate(ACClientCompat.ImmediateDraw kind, ResourceLocation texture) {
        if (texture == null) {
            return IMMEDIATE_UNTEXTURED.computeIfAbsent(kind, k -> immediateType(k, null));
        }
        return IMMEDIATE_TEXTURED.computeIfAbsent(kind, k -> net.minecraft.Util.memoize((ResourceLocation t) -> immediateType(k, t))).apply(texture);
    }

    private static RenderType immediateType(ACClientCompat.ImmediateDraw kind, ResourceLocation texture) {
        return cached(java.util.Arrays.asList("immediateType", kind, texture), () -> buildImmediateType(kind, texture));
    }

    private static RenderType buildImmediateType(ACClientCompat.ImmediateDraw kind, ResourceLocation texture) {
        var state = RenderType.CompositeState.builder();
        if (texture != null) {
            state.setTextureState(acTexture(texture, false, false));
        }
        return RenderType.create("ac_immediate_" + kind.name().toLowerCase(java.util.Locale.ROOT), 1536, immediatePipeline(kind), state.createCompositeState(false));
    }

    private static com.mojang.blaze3d.pipeline.RenderPipeline immediatePipeline(ACClientCompat.ImmediateDraw kind) {
        return switch (kind) {
            case POSITION_COLOR_FAN -> ACInternalShaders.IMMEDIATE_POSITION_COLOR_FAN;
            case POSITION_COLOR_LINES -> ACInternalShaders.IMMEDIATE_POSITION_COLOR_LINES;
            case POSITION_TEX_FAN -> ACInternalShaders.IMMEDIATE_POSITION_TEX_FAN;
            case POSITION_TEX_TRIANGLES -> ACInternalShaders.IMMEDIATE_POSITION_TEX_TRIANGLES;
            case POSITION_TEX_QUADS, POSITION_TEX_QUADS_BLEND -> ACInternalShaders.IMMEDIATE_POSITION_TEX_QUADS;
            case SCREEN_OVERLAY_QUADS -> ACInternalShaders.IMMEDIATE_SCREEN_OVERLAY;
            case PARTICLE_QUADS -> ACInternalShaders.IMMEDIATE_PARTICLE_QUADS;
            case POSITION_TEX_COLOR_QUADS -> ACInternalShaders.IMMEDIATE_POSITION_TEX_COLOR_QUADS;
        };
    }
    *///?}

    // Two vanilla factories a dozen call sites reach through this class rather than through
    // RenderType. Below 1.21.5 they arrived by inheritance; from 1.21.5 this class no longer
    // extends RenderType (a render type names a whole RenderPipeline now, so there is nothing
    // left to inherit), which would have made those calls disappear on exactly one node. Both
    // exist unchanged on every version, so a plain ungated delegate answers them all — on the
    // old nodes it merely hides the identical inherited method.
    public static RenderType itemEntityTranslucentCull(ResourceLocation locationIn) {
        return RenderType.itemEntityTranslucentCull(locationIn);
    }

    public static RenderType entityTranslucent(ResourceLocation locationIn) {
        return RenderType.entityTranslucent(locationIn);
    }

    public static RenderType getEntityTranslucentCull(ResourceLocation locationIn) {
        //? if >=1.21.2 {
        /*return ENTITY_TRANSLUCENT_CULL.apply(locationIn);
        *///?} else {
        return RenderType.entityTranslucentCull(locationIn);
        //?}
    }

    // Memoized for the reason ENTITY_TRANSLUCENT_CULL above is, and for a second one that is a
    // player-visible bug: upstream built a FRESH RenderType on every call, and this one is called
    // once per glowing entity per frame.
    //
    // RenderType declares neither equals nor hashCode on any version in this range, so every map
    // vanilla keys by it -- MultiBufferSource$BufferSource#startedBuilders and #fixedBuffers, and
    // from 26.2 RenderTypeFeatureRenderer$Group's lastRenderType fast path -- is comparing by
    // IDENTITY. A per-frame instance therefore never matches the previous frame's: the buffer
    // lookup always misses, the shared batch is torn down and rebuilt every frame, and a batch
    // ended by a key that is no longer the one in the map is left to be flushed later. The result
    // is that the eye-alpha pass is intermittently drawn twice in one frame, and because
    // EYES_ALPHA_BLEND is additive that reads as the glow flashing at several times its brightness
    // -- the "tremorzilla glow flickers" report. Measured on 1.21.11-fabric with ticks frozen, so
    // every frame should be identical: 2 of 30 frames spiked from peak green 58 to 213 with a
    // fresh type per call, twice over, and 0 of 30 with this memoized -- and on 26.2, 10 of 10
    // frames byte-identical instead of alternating between two states.
    //
    // One instance per texture is also what vanilla itself does for every type it ships.
    //? if >=1.21.5 {
    /*private static final java.util.function.Function<ResourceLocation, RenderType> EYES_ALPHA_ENABLED = net.minecraft.Util.memoize(
            (ResourceLocation locationIn) -> RenderType.create("eye_alpha", 256, true, false, ACInternalShaders.EYES_ALPHA, RenderType.CompositeState.builder()
                    .setTextureState(acTexture(locationIn, false, false))
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .createCompositeState(true)));
    *///?} else {
    private static final java.util.function.Function<ResourceLocation, RenderType> EYES_ALPHA_ENABLED = net.minecraft.Util.memoize(
            (ResourceLocation locationIn) -> create("eye_alpha", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false, RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
                    .setTransparencyState(EYES_ALPHA_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setDepthTestState(EQUAL_DEPTH_TEST)
                    .createCompositeState(true)));
    //?}

    public static RenderType getEyesAlphaEnabled(ResourceLocation locationIn) {
        return EYES_ALPHA_ENABLED.apply(locationIn);
    }

    /**
     * The loaders' {@code getUnlitTranslucent} — entity-translucent with the
     * cardinal (diffuse) lighting term switched off, so a model's faces are not shaded darker
     * for facing away from the two hard-coded light directions. The Cave Compendium's book
     * model, the extinction spear, the dark arrow, the nucleeper/gumbeeper glass, the amber
     * monolith and three particles all draw with it.
     *
     * <p>Two loader nodes do not supply a working one, so the mod supplies its own there:
     * <ul>
     *   <li><b>Fabric</b> (all 22 nodes) has no such type at all. The stand-in used to answer it
     *       with vanilla's entity-translucent-<i>emissive</i> type, which is the wrong switch:
     *       {@code EMISSIVE} drops the <i>lightmap</i> and leaves the diffuse term in place —
     *       verified in {@code core/entity.vsh} from 1.21.5 up and in
     *       {@code rendertype_entity_translucent_emissive.vsh} below it, where the
     *       {@code minecraft_mix_light} call sits outside the emissive branch on every version.
     *       That is what made the book render with a hard bright/dark step per face.</li>
     *   <li><b>Forge at 26.2</b> (65.1.0) rewrote its own type onto plain
     *       {@code RenderPipelines.ENTITY_TRANSLUCENT} — no unlit define, no lightmap — so it
     *       regressed into the same diffuse shading. Forge below 26.2 and NeoForge on every
     *       node keep a real unlit pipeline and are left delegating.</li>
     * </ul>
     *
     * <p>One accepted divergence: below 1.21.5 the mod's arm borrows vanilla's {@code eyes}
     * shader, which is genuinely unlit but declares no {@code Sampler2}, so the lightmap shard
     * is a no-op and the model is drawn fullbright. That is the same divergence the Fabric
     * stand-in has always carried; from 1.21.5 the pipeline arm keeps the lightmap properly.
     */
    public static RenderType getUnlitTranslucent(ResourceLocation locationIn) {
        return cached(java.util.Arrays.asList("getUnlitTranslucent", locationIn), () -> buildGetUnlitTranslucent(locationIn));
    }

    private static RenderType buildGetUnlitTranslucent(ResourceLocation locationIn) {
        //? if fabric && <1.21.5 {
        /*RenderType.CompositeState unlit$compositestate = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_EYES_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
        return create("unlit_translucent", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, unlit$compositestate);
        *///?} elif fabric || (forge && >=26.2) {
        /*return RenderType.create("unlit_translucent", 256, true, true, ACInternalShaders.ENTITY_UNLIT_TRANSLUCENT, RenderType.CompositeState.builder()
                .setTextureState(acTexture(locationIn, false, false))
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .createCompositeState(true));
        *///?} else {
        return ForgeRenderTypes.getUnlitTranslucent(locationIn);
        //?}
    }

    public static RenderType getAmbersolShine() {
        return cached(java.util.Arrays.asList("getAmbersolShine"), () -> buildGetAmbersolShine());
    }

    private static RenderType buildGetAmbersolShine() {
        // 1.21.9 deleted the separate particles framebuffer along with the fabulous-graphics path
        // that was the only thing distinguishing it, so PARTICLES_TARGET is gone and its output
        // simply is the main target now.
        //? if >=1.21.9 {
        /*return RenderType.create("ambersol_shine", 256, true, true, ACInternalShaders.LIGHTNING_EYES_ALPHA, RenderType.CompositeState.builder()
                .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                .setOutputState(RenderStateShard.MAIN_TARGET)
                .createCompositeState(true));
        *///?} elif >=1.21.5 {
        /*return RenderType.create("ambersol_shine", 256, true, true, ACInternalShaders.LIGHTNING_EYES_ALPHA, RenderType.CompositeState.builder()
                .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                .setOutputState(RenderStateShard.PARTICLES_TARGET)
                .createCompositeState(true));
        *///?} else {
        return create("ambersol_shine", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, true, true, RenderType.CompositeState.builder()
                .setShaderState(RenderType.RENDERTYPE_LIGHTNING_SHADER)
                .setTransparencyState(EYES_ALPHA_TRANSPARENCY)
                .setCullState(CULL)
                .setLightmapState(NO_LIGHTMAP)
                .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                .setOutputState(RenderStateShard.PARTICLES_TARGET)
                .createCompositeState(true));
        //?}
    }

    public static RenderType getNucleeperLights() {
        return cached(java.util.Arrays.asList("getNucleeperLights"), () -> buildGetNucleeperLights());
    }

    private static RenderType buildGetNucleeperLights() {
        //? if >=1.21.5 {
        /*return RenderType.create("nucleeper_lights", 256, true, true, ACInternalShaders.LIGHTNING_EYES_ALPHA, RenderType.CompositeState.builder()
                .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                .createCompositeState(true));
        *///?} else {
        return create("nucleeper_lights", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, true, true, RenderType.CompositeState.builder()
                .setShaderState(RenderType.RENDERTYPE_LIGHTNING_SHADER)
                .setTransparencyState(EYES_ALPHA_TRANSPARENCY)
                .setCullState(CULL)
                .setLightmapState(NO_LIGHTMAP)
                .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                .createCompositeState(true));
        //?}
    }

    public static RenderType getHologramLights() {
        return cached(java.util.Arrays.asList("getHologramLights"), () -> buildGetHologramLights());
    }

    private static RenderType buildGetHologramLights() {
        //? if >=1.21.5 {
        /*return RenderType.create("hologram_lights", 256, true, true, ACInternalShaders.LIGHTNING_TRANSLUCENT, RenderType.CompositeState.builder()
                .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                .setOutputState(HOLOGRAM_OUTPUT)
                .createCompositeState(false));
        *///?} else {
        return create("hologram_lights", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, true, true, RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_HOLOGRAM_SHADER)
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setCullState(CULL)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setLightmapState(NO_LIGHTMAP)
                .setOutputState(HOLOGRAM_OUTPUT)
                .createCompositeState(false));
        //?}
    }

    public static RenderType getCrucibleItemBeam() {
        return cached(java.util.Arrays.asList("getCrucibleItemBeam"), () -> buildGetCrucibleItemBeam());
    }

    private static RenderType buildGetCrucibleItemBeam() {
        //? if >=1.21.5 {
        /*return RenderType.create("crucible_item_beam", 256, true, true, ACInternalShaders.LIGHTNING_TRANSLUCENT, RenderType.CompositeState.builder()
                .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                .createCompositeState(true));
        *///?} else {
        return create("crucible_item_beam", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, true, true, RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                .setCullState(CULL)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(NO_LIGHTMAP)
                .createCompositeState(true));
        //?}
    }

    public static RenderType getSubmarineLights() {
        return cached(java.util.Arrays.asList("getSubmarineLights"), () -> buildGetSubmarineLights());
    }

    private static RenderType buildGetSubmarineLights() {
        //? if >=1.21.5 {
        /*return RenderType.create("submarine_lights", 256, true, true, ACInternalShaders.LIGHTNING_TRANSLUCENT, RenderType.CompositeState.builder()
                .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                .createCompositeState(false));
        *///?} else {
        return create("submarine_lights", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, true, true, RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setCullState(CULL)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setLightmapState(NO_LIGHTMAP)
                .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                .createCompositeState(false));
        //?}
    }


    public static RenderType getGel(ResourceLocation locationIn) {
        return cached(java.util.Arrays.asList("getGel", locationIn), () -> buildGetGel(locationIn));
    }

    private static RenderType buildGetGel(ResourceLocation locationIn) {
        //? if >=1.21.5 {
        /*return RenderType.create("ferrouslime_gel", 256, true, true, ACInternalShaders.FERROUSLIME_GEL, RenderType.CompositeState.builder()
                .setTextureState(acTexture(locationIn, false, false))
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .createCompositeState(true));
        *///?} else {
        return create("ferrouslime_gel", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
                .setCullState(NO_CULL)
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setShaderState(RENDERTYPE_FEROUSSLIME_GEL_SHADER)
                .setLightmapState(LIGHTMAP)
                .createCompositeState(true));
        //?}
    }

    public static RenderType getRadiationGlow(ResourceLocation locationIn) {
        return cached(java.util.Arrays.asList("getRadiationGlow", locationIn), () -> buildGetRadiationGlow(locationIn));
    }

    private static RenderType buildGetRadiationGlow(ResourceLocation locationIn) {
        //? if >=1.21.5 {
        /*return RenderType.create("radiation_glow", 256, false, true, ACInternalShaders.IRRADIATED, RenderType.CompositeState.builder()
                .setTextureState(acTexture(locationIn, false, false))
                .setOutputState(IRRADIATED_OUTPUT)
                .createCompositeState(false));
        *///?} else {
        return create("radiation_glow", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_IRRADIATED_SHADER)
                .setCullState(NO_CULL)
                .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setOutputState(IRRADIATED_OUTPUT)
                .createCompositeState(false));
        //?}
    }

    public static RenderType getBlueRadiationGlow(ResourceLocation locationIn) {
        return cached(java.util.Arrays.asList("getBlueRadiationGlow", locationIn), () -> buildGetBlueRadiationGlow(locationIn));
    }

    private static RenderType buildGetBlueRadiationGlow(ResourceLocation locationIn) {
        //? if >=1.21.5 {
        /*return RenderType.create("blue_radiation_glow", 256, false, true, ACInternalShaders.BLUE_IRRADIATED, RenderType.CompositeState.builder()
                .setTextureState(acTexture(locationIn, false, false))
                .setOutputState(IRRADIATED_OUTPUT)
                .createCompositeState(false));
        *///?} else {
        return create("blue_radiation_glow", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_BLUE_IRRADIATED_SHADER)
                .setCullState(NO_CULL)
                .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setOutputState(IRRADIATED_OUTPUT)
                .createCompositeState(false));
        //?}
    }
    public static RenderType getGelTriangles(ResourceLocation locationIn) {
        return cached(java.util.Arrays.asList("getGelTriangles", locationIn), () -> buildGetGelTriangles(locationIn));
    }

    private static RenderType buildGetGelTriangles(ResourceLocation locationIn) {
        // sortOnUpload is FALSE here on purpose: this type's pipeline is TRIANGLES, and depth
        // sorting has only ever applied to QUADS. Through 26.1 MeshData#sortQuads returned null
        // for any other mode, so the flag was a silent no-op; 26.2 routes the same draw through
        // StagedVertexBuffer#appendDraw, which throws IllegalArgumentException "Cannot sort draw
        // with TRIANGLES" the first frame the type is used. Passing false is byte-identical
        // behaviour on every version and is what keeps 26.2 from crashing.
        //? if >=1.21.5 {
        /*return RenderType.create("ferrouslime_gel_triangles", 256, true, false, ACInternalShaders.FERROUSLIME_GEL_TRIANGLES, RenderType.CompositeState.builder()
                .setTextureState(acTexture(locationIn, false, false))
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .createCompositeState(false));
        *///?} else {
        return create("ferrouslime_gel_triangles", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 256, true, true, RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
                .setCullState(NO_CULL)
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setShaderState(RENDERTYPE_FEROUSSLIME_GEL_SHADER)
                .setLightmapState(LIGHTMAP)
                .createCompositeState(false));
        //?}
    }


    public static RenderType getSubmarineMask() {
        return cached(java.util.Arrays.asList("getSubmarineMask"), () -> buildGetSubmarineMask());
    }

    private static RenderType buildGetSubmarineMask() {
        //? if >=1.21.5 {
        /*return RenderType.create("submarine_mask", 256, true, true, ACInternalShaders.WATER_MASK_NO_CULL, RenderType.CompositeState.builder().setTextureState(RenderStateShard.NO_TEXTURE).createCompositeState(false));
        *///?} else {
        return create("submarine_mask", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 256, true, true, RenderType.CompositeState.builder().setShaderState(RENDERTYPE_WATER_MASK_SHADER).setTextureState(NO_TEXTURE).setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setWriteMaskState(DEPTH_WRITE).setCullState(NO_CULL).createCompositeState(false));
        //?}
    }

    public static RenderType getGhostly(ResourceLocation texture) {
        return cached(java.util.Arrays.asList("getGhostly", texture), () -> buildGetGhostly(texture));
    }

    private static RenderType buildGetGhostly(ResourceLocation texture) {
        //? if >=1.21.5 {
        /*return RenderType.create("ghostly", 256, true, true, ACInternalShaders.GHOSTLY, RenderType.CompositeState.builder()
                .setTextureState(acTexture(texture, false, false))
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .createCompositeState(true));
        *///?} else {
        CompositeState renderState = CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
                .setCullState(NO_CULL)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .createCompositeState(true);
        return create("ghostly", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, renderState);
        //?}
    }


    public static RenderType getTeslaBulb(ResourceLocation resourceLocation) {
        return cached(java.util.Arrays.asList("getTeslaBulb", resourceLocation), () -> buildGetTeslaBulb(resourceLocation));
    }

    private static RenderType buildGetTeslaBulb(ResourceLocation resourceLocation) {
        //? if >=1.21.5 {
        /*return RenderType.create("tesla_bulb", 256, false, true, ACInternalShaders.ENERGY_SWIRL_TRANSLUCENT, RenderType.CompositeState.builder().setTextureState(acTexture(resourceLocation, false, true)).setLightmapState(RenderStateShard.LIGHTMAP).createCompositeState(true));
        *///?} else {
        return create("tesla_bulb", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_ENERGY_SWIRL_SHADER).setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, false, true)).setLightmapState(LIGHTMAP).setCullState(RenderStateShard.NO_CULL).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(true));
        //?}
    }

    public static RenderType getHologram(ResourceLocation locationIn) {
        return cached(java.util.Arrays.asList("getHologram", locationIn), () -> buildGetHologram(locationIn));
    }

    private static RenderType buildGetHologram(ResourceLocation locationIn) {
        //? if >=1.21.5 {
        /*return RenderType.create("hologram", 256, false, true, ACInternalShaders.HOLOGRAM, RenderType.CompositeState.builder()
                .setTextureState(acTexture(locationIn, false, false))
                .setOutputState(HOLOGRAM_OUTPUT)
                .createCompositeState(false));
        *///?} else {
        return create("hologram", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                .setCullState(NO_CULL)
                .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setOutputState(HOLOGRAM_OUTPUT)
                .createCompositeState(false));
        //?}
    }


    public static RenderType getRedGhost(ResourceLocation locationIn) {
        return cached(java.util.Arrays.asList("getRedGhost", locationIn), () -> buildGetRedGhost(locationIn));
    }

    private static RenderType buildGetRedGhost(ResourceLocation locationIn) {
        //? if >=1.21.5 {
        /*return RenderType.create("red_ghost", 256, false, true, ACInternalShaders.RED_GHOST, RenderType.CompositeState.builder()
                .setTextureState(acTexture(locationIn, false, false))
                .setOverlayState(RenderStateShard.OVERLAY)
                .createCompositeState(true));
        *///?} else {
        return create("red_ghost", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_RED_GHOST_SHADER)
                .setCullState(NO_CULL)
                .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
                .setTransparencyState(EYES_ALPHA_TRANSPARENCY)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setOverlayState(OVERLAY)
                .createCompositeState(true));
        //?}
    }

    public static RenderType getCaveMapBackground(ResourceLocation locationIn, boolean showBackground) {
        return cached(java.util.Arrays.asList("getCaveMapBackground", locationIn, showBackground), () -> buildGetCaveMapBackground(locationIn, showBackground));
    }

    private static RenderType buildGetCaveMapBackground(ResourceLocation locationIn, boolean showBackground) {
        //? if >=1.21.5 {
        /*return RenderType.create("cave_map_background", 256, false, true, showBackground ? ACInternalShaders.CAVE_MAP_BACKGROUND_NO_CULL : ACInternalShaders.CAVE_MAP_BACKGROUND_CULL, RenderType.CompositeState.builder()
                .setTextureState(acTexture(locationIn, false, false))
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .createCompositeState(false));
        *///?} else {
        RenderType.CompositeState rendertype$state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_TEXT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setCullState(showBackground ? NO_CULL : CULL)
                .createCompositeState(false);
        return create("cave_map_background", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, true, rendertype$state);
        //?}
    }

    public static RenderType getBookWidget(ResourceLocation locationIn, boolean sepia) {
        return cached(java.util.Arrays.asList("getBookWidget", locationIn, sepia), () -> buildGetBookWidget(locationIn, sepia));
    }

    private static RenderType buildGetBookWidget(ResourceLocation locationIn, boolean sepia) {
        if(sepia){
            //? if >=1.21.5 {
            /*return RenderType.create("book_widget", 256, false, true, ACInternalShaders.SEPIA, RenderType.CompositeState.builder()
                    .setTextureState(acTexture(locationIn, false, false))
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .createCompositeState(true));
            *///?} else {
            return create("book_widget", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_SEPIA_SHADER)
                    .setCullState(NO_CULL)
                    .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(true));
            //?}
        }else{
            return getUnlitTranslucent(locationIn);
        }

    }

    public static RenderType getBubbledCull(ResourceLocation locationIn) {
        return cached(java.util.Arrays.asList("getBubbledCull", locationIn), () -> buildGetBubbledCull(locationIn));
    }

    private static RenderType buildGetBubbledCull(ResourceLocation locationIn) {
        //? if >=1.21.5 {
        /*return RenderType.create("bubbled_cull", 256, true, true, ACInternalShaders.BUBBLED_CULL, RenderType.CompositeState.builder().setTextureState(acTexture(locationIn, false, false)).setLightmapState(RenderStateShard.LIGHTMAP).setOutputState(RenderStateShard.ITEM_ENTITY_TARGET).setOverlayState(RenderStateShard.OVERLAY).createCompositeState(true));
        *///?} else {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder().setShaderState(RENDERTYPE_BUBBLED_SHADER).setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setLightmapState(LIGHTMAP).setOutputState(RenderStateShard.ITEM_ENTITY_TARGET).setOverlayState(OVERLAY).setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE).createCompositeState(true);
        return create("bubbled_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, rendertype$compositestate);
        //?}
    }

    public static RenderType getBubbledNoCull(ResourceLocation locationIn) {
        return cached(java.util.Arrays.asList("getBubbledNoCull", locationIn), () -> buildGetBubbledNoCull(locationIn));
    }

    private static RenderType buildGetBubbledNoCull(ResourceLocation locationIn) {
        //? if >=1.21.5 {
        /*return RenderType.create("bubbled_no_cull", 256, false, false, ACInternalShaders.BUBBLED_NO_CULL, RenderType.CompositeState.builder().setTextureState(acTexture(locationIn, false, false)).setLightmapState(RenderStateShard.LIGHTMAP).setOutputState(RenderStateShard.ITEM_ENTITY_TARGET).setOverlayState(RenderStateShard.OVERLAY).createCompositeState(true));
        *///?} else {
        RenderType.CompositeState rendertype$compositestate = RenderType.CompositeState.builder().setShaderState(RENDERTYPE_BUBBLED_SHADER).setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOutputState(RenderStateShard.ITEM_ENTITY_TARGET).setOverlayState(OVERLAY).setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE).createCompositeState(true);
        return create("bubbled_no_cull", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, false, rendertype$compositestate);
        //?}
    }

    public static RenderType getRaygunRay(ResourceLocation locationIn, boolean irradiated) {
        return cached(java.util.Arrays.asList("getRaygunRay", locationIn, irradiated), () -> buildGetRaygunRay(locationIn, irradiated));
    }

    private static RenderType buildGetRaygunRay(ResourceLocation locationIn, boolean irradiated) {
        // sortOnUpload is FALSE here on purpose: this type's pipeline is TRIANGLES, and depth
        // sorting has only ever applied to QUADS. Through 26.1 MeshData#sortQuads returned null
        // for any other mode, so the flag was a silent no-op; 26.2 routes the same draw through
        // StagedVertexBuffer#appendDraw, which throws IllegalArgumentException "Cannot sort draw
        // with TRIANGLES" the first frame the type is used. Passing false is byte-identical
        // behaviour on every version and is what keeps 26.2 from crashing.
        //? if >=1.21.5 {
        /*return RenderType.create("raygun_ray", 256, true, false, ACInternalShaders.ENERGY_SWIRL_TRANSLUCENT_TRIANGLES, RenderType.CompositeState.builder()
                .setTextureState(acTexture(locationIn, false, false))
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOutputState(irradiated ? IRRADIATED_OUTPUT : RenderStateShard.ITEM_ENTITY_TARGET)
                .createCompositeState(false));
        *///?} else {
        return create("raygun_ray", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 256, true, true, RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
                .setShaderState(RenderType.RENDERTYPE_ENERGY_SWIRL_SHADER)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOutputState(irradiated ? IRRADIATED_OUTPUT : ITEM_ENTITY_TARGET)
                .createCompositeState(false));
        //?}
    }

    public static RenderType getTremorzillaBeam(ResourceLocation locationIn, boolean irradiated) {
        return cached(java.util.Arrays.asList("getTremorzillaBeam", locationIn, irradiated), () -> buildGetTremorzillaBeam(locationIn, irradiated));
    }

    private static RenderType buildGetTremorzillaBeam(ResourceLocation locationIn, boolean irradiated) {
        //? if >=1.21.5 {
        /*return RenderType.create("tremorzilla_beam", 256, true, true, ACInternalShaders.ENERGY_SWIRL_TRANSLUCENT, RenderType.CompositeState.builder()
                .setTextureState(acTexture(locationIn, false, false))
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOutputState(irradiated ? IRRADIATED_OUTPUT : RenderStateShard.ITEM_ENTITY_TARGET)
                .createCompositeState(false));
        *///?} else {
        return create("tremorzilla_beam", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
                .setShaderState(RenderType.RENDERTYPE_ENERGY_SWIRL_SHADER)
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOutputState(irradiated ? IRRADIATED_OUTPUT : ITEM_ENTITY_TARGET)
                .createCompositeState(false));
        //?}
    }

    public static RenderType getPurpleWitch(ResourceLocation locationIn) {
        return cached(java.util.Arrays.asList("getPurpleWitch", locationIn), () -> buildGetPurpleWitch(locationIn));
    }

    private static RenderType buildGetPurpleWitch(ResourceLocation locationIn) {
        //? if >=1.21.5 {
        /*return RenderType.create("purple_witch", 256, false, true, ACInternalShaders.PURPLE_WITCH, RenderType.CompositeState.builder()
                .setTextureState(acTexture(locationIn, false, false))
                .setOutputState(PURPLE_WITCH_OUTPUT)
                .createCompositeState(false));
        *///?} else {
        return create("purple_witch", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_PURPLE_WITCH_SHADER)
                .setCullState(NO_CULL)
                .setTextureState(new RenderStateShard.TextureStateShard(locationIn, false, false))
                .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setOutputState(PURPLE_WITCH_OUTPUT)
                .createCompositeState(false));
        //?}
    }

}
