package com.github.alexmodguy.alexscaves.client.render;

//? if >=1.21.11 {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.ResourceLocation;
*///?}

/**
 * The 1.21.5-era {@code RenderType.CompositeState} builder, re-implemented on top of 1.21.11's
 * {@code RenderSetup}.
 *
 * <p><b>Why this exists.</b> 1.21.11 dissolved {@code RenderStateShard} into
 * {@code LayeringTransform} / {@code OutputTarget} / {@code TextureTransform}, replaced
 * {@code CompositeState} with a differently-shaped {@code RenderSetup} builder, and moved
 * {@code RenderType} into its own package keeping only the instance surface plus a two-argument
 * {@code create}. This tree builds <b>27</b> render types by hand across two files
 * ({@link ACRenderTypes} and Citadel's {@code WorldRenderMacros}); giving each one its own gated
 * arm would be 27 duplicated bodies to keep in step forever. One shim with the old builder's shape
 * keeps every call site on a single code path from 1.21.5 up, and a handful of
 * {@code replacements.string} rules re-point the shard constants and the {@code create} calls at
 * it. The three shards that moved rather than dissolved — the layering, output and texturing
 * constants — stay vanilla and are renamed by rules of their own, so this class only has to model
 * the parts that lost a home: the texture binding and the lightmap/overlay toggles.
 *
 * <p>The whole body is gated {@code >=1.21.11}; below that this is an empty utility class nothing
 * references, because the rules that name it do not fire.
 *
 * <p><b>Three deliberate lossy spots</b>, all verified irrelevant to what this mod draws:
 * <ul>
 *   <li>the old {@code TextureStateShard}'s <i>mipmap</i> flag has no successor — a texture's
 *       filtering now comes from the sampler its own {@code AbstractTexture} carries, so the flag
 *       is accepted and ignored;</li>
 *   <li>the old blur flag is honoured by handing the binding a {@code LINEAR}/{@code LINEAR}
 *       sampler instead of calling the deleted {@code AbstractTexture#setFilter}. Its max-LOD is
 *       left empty, i.e. the GL default — {@code GlSampler} only touches {@code TEXTURE_MAX_LOD}
 *       when the {@code OptionalDouble} is present. One render type asks for it (the particle
 *       trail);</li>
 *   <li>{@code createCompositeState(boolean)} mapped "affects outline" onto what is now a
 *       tri-state {@code OutlineProperty}; nothing here is itself an outline type, so it is
 *       {@code AFFECTS_OUTLINE} or {@code NONE}.</li>
 * </ul>
 *
 * <p>⚠️ Never write {@code RenderType} immediately followed by {@code .create} and a string literal
 * in this file. The rule that re-points the call sites here matches exactly that, opening quote
 * included, so the vanilla call below is safe only because its first argument is a variable — pass
 * a literal and the shim calls itself forever.
 *
 * <p>⚠️ Nothing inside the gated arm may carry a comment. Stonecutter wraps an inactive arm in one
 * block comment (which a nested one would close early) and un-prefixes an active one line by line
 * (which turns prose into source). All commentary therefore lives up here.
 */
public final class ACRenderSetup {

    private ACRenderSetup() {
    }

    //? if >=1.21.11 {
    /*private static final String SAMPLER = "Sampler0";

    private static GpuSampler blurSampler;

    public static final class Toggle {
        final boolean on;

        private Toggle(boolean on) {
            this.on = on;
        }
    }

    public static final Toggle LIGHTMAP = new Toggle(true);
    public static final Toggle NO_LIGHTMAP = new Toggle(false);
    public static final Toggle OVERLAY = new Toggle(true);
    public static final Toggle NO_OVERLAY = new Toggle(false);

    public static final class Texture {
        final ResourceLocation location;
        final boolean blur;

        private Texture(ResourceLocation location, boolean blur) {
            this.location = location;
            this.blur = blur;
        }
    }

    public static final Texture NO_TEXTURE = new Texture(null, false);

    public static Texture texture(ResourceLocation location, boolean blur, boolean mipmap) {
        return new Texture(location, blur);
    }

    public static final class Composite {
        final Texture texture;
        final boolean lightmap;
        final boolean overlay;
        final LayeringTransform layering;
        final OutputTarget output;
        final TextureTransform texturing;
        final boolean affectsOutline;

        private Composite(Builder builder, boolean affectsOutline) {
            this.texture = builder.texture;
            this.lightmap = builder.lightmap;
            this.overlay = builder.overlay;
            this.layering = builder.layering;
            this.output = builder.output;
            this.texturing = builder.texturing;
            this.affectsOutline = affectsOutline;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Texture texture = NO_TEXTURE;
        private boolean lightmap;
        private boolean overlay;
        private LayeringTransform layering;
        private OutputTarget output;
        private TextureTransform texturing;

        private Builder() {
        }

        public Builder setTextureState(Texture texture) {
            this.texture = texture;
            return this;
        }

        public Builder setLightmapState(Toggle toggle) {
            this.lightmap = toggle.on;
            return this;
        }

        public Builder setOverlayState(Toggle toggle) {
            this.overlay = toggle.on;
            return this;
        }

        public Builder setLayeringState(LayeringTransform layering) {
            this.layering = layering;
            return this;
        }

        public Builder setOutputState(OutputTarget output) {
            this.output = output;
            return this;
        }

        public Builder setTexturingState(TextureTransform texturing) {
            this.texturing = texturing;
            return this;
        }

        public Composite createCompositeState(boolean affectsOutline) {
            return new Composite(this, affectsOutline);
        }
    }

    public static RenderType create(String name, int bufferSize, RenderPipeline pipeline, Composite state) {
        return create(name, bufferSize, false, false, pipeline, state);
    }

    public static RenderType create(String name, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, RenderPipeline pipeline, Composite state) {
        RenderSetup.RenderSetupBuilder builder = RenderSetup.builder(pipeline).bufferSize(bufferSize);
        if (state.texture != null && state.texture.location != null) {
            if (state.texture.blur) {
                builder.withTexture(SAMPLER, state.texture.location, ACRenderSetup::blurSampler);
            } else {
                builder.withTexture(SAMPLER, state.texture.location);
            }
        }
        if (state.lightmap) {
            builder.useLightmap();
        }
        if (state.overlay) {
            builder.useOverlay();
        }
        if (state.layering != null) {
            builder.setLayeringTransform(state.layering);
        }
        if (state.output != null) {
            builder.setOutputTarget(state.output);
        }
        if (state.texturing != null) {
            builder.setTextureTransform(state.texturing);
        }
        builder.setOutline(state.affectsOutline ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE : RenderSetup.OutlineProperty.NONE);
        if (affectsCrumbling) {
            builder.affectsCrumbling();
        }
        if (sortOnUpload) {
            builder.sortOnUpload();
        }
        return RenderType.create(name, builder.createRenderSetup());
    }

    private static GpuSampler blurSampler() {
        if (blurSampler == null) {
            blurSampler = RenderSystem.getDevice().createSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.LINEAR, FilterMode.LINEAR, 0, java.util.OptionalDouble.empty());
        }
        return blurSampler;
    }
    *///?}
}
