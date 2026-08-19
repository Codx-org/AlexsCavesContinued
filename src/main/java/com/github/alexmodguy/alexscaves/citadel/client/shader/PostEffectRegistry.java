package com.github.alexmodguy.alexscaves.citadel.client.shader;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;

import com.github.alexmodguy.alexscaves.citadel.Citadel;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
//? if <1.21.5
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The three post-processing effects this mod draws into buffers of their own — the irradiated glow,
 * the hologram and the purple-witch aura — and blits back over the world additively.
 *
 * <p>1.21.2 rebuilt {@code PostChain} around the frame graph. It is no longer constructed from a
 * {@code ResourceLocation}, it no longer owns named temp targets a caller can hold on to, and it no
 * longer has {@code resize}, {@code close} or {@code process(float)}. The shape here survives
 * intact because the pieces map one for one:
 *
 * <ul>
 * <li>the chain itself comes from {@code ShaderManager#getPostChain}, which loads and caches it;
 * <li>the {@code "final"} temp target this used to borrow from the chain is now a
 *     {@code TextureTarget} this class owns, created and resized alongside the window — the chain
 *     reaches it as its one external target, {@code minecraft:main};
 * <li>{@code process(float)}'s accumulating {@code Time} uniform is kept by hand and pushed with
 *     {@code setUniform}, since nothing sets it automatically any more.
 * </ul>
 *
 * <p>1.21.5 kept that shape and took away the fixed-function plumbing underneath it. There is no
 * {@code RenderSystem.enableBlend}/{@code blendFuncSeparate} any more — blend state belongs to a
 * {@code RenderPipeline} — and a {@code RenderTarget} lost {@code clear}, {@code setClearColor},
 * {@code bindWrite}/{@code unbindWrite} and {@code blitAndBlendToScreen}. So each of those becomes
 * one small gated helper below: the blit is hand-rolled against
 * {@code ACInternalShaders.POST_EFFECT_BLIT} (vanilla's own {@code blitAndBlendToTexture} body with
 * this mod's blend function), the clear is a command-encoder call that takes the clear colour as an
 * argument, and binding is simply gone, because a render pass names the texture it draws into. The
 * {@code Time} uniform moves onto {@code process}'s new {@code Consumer<RenderPass>} parameter,
 * {@code PostChain} having lost {@code setUniform}.
 *
 * <p>1.21.6 removed that {@code Consumer<RenderPass>} again without replacing it: a {@code PostPass}
 * builds its uniform buffers once, in its constructor, straight from the JSON, so there is no
 * per-frame uniform push left anywhere. The hologram's clock therefore stops being a uniform at all
 * and is derived in the fragment shader from the vanilla {@code Globals} block's {@code GameTime},
 * which {@code PostPass} binds for free. Textures also all became {@code GpuTextureView}s, which is
 * the whole of the change to the blit.
 *
 * <p>The post-chain JSONs themselves are rewritten to the 1.21.2 format at build time, again to the
 * 1.21.5 one on top of it, and again to 1.21.6's uniform blocks — see {@code DataPackMigration}.
 */
public class PostEffectRegistry {

    private static final List<ResourceLocation> registry = new ArrayList<>();

    private static final Map<ResourceLocation, PostEffect> postEffects = new HashMap<>();

    public static void clear() {
        for (PostEffect postEffect : postEffects.values()) {
            postEffect.close();
        }
        postEffects.clear();
    }

    public static void registerEffect(ResourceLocation resourceLocation) {
        registry.add(resourceLocation);
    }

    public static void onInitializeOutline() {
        clear();
        Minecraft minecraft = Minecraft.getInstance();
        for (ResourceLocation resourceLocation : registry) {
            //? if >=1.21.2 {
            /*PostChain postChain = minecraft.getShaderManager().getPostChain(resourceLocation, java.util.Set.of(PostChain.MAIN_TARGET_ID));
            RenderTarget renderTarget = null;
            if (postChain == null) {
                // getPostChain reports the failure itself, through ShaderManager's error consumer.
                Citadel.LOGGER.warn("Failed to load shader: {}", resourceLocation);
            } else {
                renderTarget = createEffectTarget(minecraft, resourceLocation);
            }
            postEffects.put(resourceLocation, new PostEffect(postChain, renderTarget, false));
            *///?} else {
            PostChain postChain;
            RenderTarget renderTarget;
            try {
                postChain = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(), minecraft.getMainRenderTarget(), resourceLocation);
                postChain.resize(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
                renderTarget = postChain.getTempTarget("final");
            } catch (IOException ioexception) {
                Citadel.LOGGER.warn("Failed to load shader: {}", resourceLocation, ioexception);
                postChain = null;
                renderTarget = null;
            } catch (JsonSyntaxException jsonsyntaxexception) {
                Citadel.LOGGER.warn("Failed to parse shader: {}", resourceLocation, jsonsyntaxexception);
                postChain = null;
                renderTarget = null;
            }
            postEffects.put(resourceLocation, new PostEffect(postChain, renderTarget, false));
            //?}
        }
    }

    public static void resize(int x, int y) {
        for (PostEffect postEffect : postEffects.values()) {
            postEffect.resize(x, y);
        }
    }

    public static RenderTarget getRenderTargetFor(ResourceLocation resourceLocation) {
        PostEffect effect = postEffects.get(resourceLocation);
        return effect == null ? null : effect.getRenderTarget();
    }

    public static void renderEffectForNextTick(ResourceLocation resourceLocation) {
        PostEffect effect = postEffects.get(resourceLocation);
        if (effect != null) {
            effect.setEnabled(true);
        }
    }

    public static void blitEffects() {
        // From 1.21.5 blend state is a property of the pipeline the blit draws with, so there is
        // nothing to set up or put back around the loop; see ACInternalShaders.POST_EFFECT_BLIT.
        //? if <1.21.5 {
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        //?}
        for (PostEffect postEffect : postEffects.values()) {
            if (postEffect.postChain != null && postEffect.isEnabled()) {
                blitEffect(postEffect.getRenderTarget());
                clearEffectTarget(postEffect.getRenderTarget());
                bindMainForWrite(Minecraft.getInstance().getMainRenderTarget());
                postEffect.setEnabled(false);
            }
        }
        //? if <1.21.5 {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        //?}
    }

    public static void clearAndBindWrite(RenderTarget mainTarget) {
        for (PostEffect postEffect : postEffects.values()) {
            if (postEffect.isEnabled() && postEffect.postChain != null) {
                clearEffectTarget(postEffect.getRenderTarget());
                bindMainForWrite(mainTarget);
            }
        }
    }

    // No partial tick parameter: every version's post chain is driven off ACClientCompat.frameTime()
    // rather than the caller's tick, and the caller — a redirect on endOutlineBatch from 1.21.2 —
    // has none to give.
    public static void processEffects(RenderTarget mainTarget) {
        for (PostEffect postEffect : postEffects.values()) {
            if (postEffect.isEnabled() && postEffect.postChain != null) {
                // Each pass used to apply the blend mode from its own program JSON — every one of them
                // `add / one / zero`, i.e. a straight overwrite. 1.21.2 dropped blend state from
                // ShaderProgramConfig and never touches it, so the passes inherit whatever the caller
                // left set; disabling it here is what "one / zero" meant. blitEffects sets its own.
                // 1.21.5 took setUniform off PostChain and gave process a Consumer<RenderPass>
                // instead, applied to every pass just before its own declared uniforms — so the
                // clock is read once here rather than once per pass.
                // 1.21.6 took that away too, and left nothing in its place: a PostPass builds its
                // uniform buffers once, in its constructor, from the JSON. So there is no per-frame
                // uniform push at all any more, and the hologram's clock comes out of the vanilla
                // Globals block instead — see DataPackMigration.migrateShadersTo1216.
                //? if >=1.21.6 {
                /*postEffect.postChain.process(postEffect.getRenderTarget(), com.mojang.blaze3d.resource.GraphicsResourceAllocator.UNPOOLED);
                *///?} elif >=1.21.5 {
                /*final float time = postEffect.advanceTime(ACClientCompat.frameTime());
                postEffect.postChain.process(postEffect.getRenderTarget(), com.mojang.blaze3d.resource.GraphicsResourceAllocator.UNPOOLED, pass -> pass.setUniform("Time", time));
                *///?} elif >=1.21.2 {
                /*RenderSystem.disableBlend();
                postEffect.postChain.setUniform("Time", postEffect.advanceTime(ACClientCompat.frameTime()));
                postEffect.postChain.process(postEffect.getRenderTarget(), com.mojang.blaze3d.resource.GraphicsResourceAllocator.UNPOOLED);
                *///?} else {
                postEffect.postChain.process(ACClientCompat.frameTime());
                //?}
                bindMainForWrite(mainTarget);
            }
        }
    }

    //? if >=26.2 {
    /*private static RenderTarget createEffectTarget(Minecraft minecraft, ResourceLocation id) {
        // 26.2 made the colour format explicit rather than always-RGBA8. RGBA8_UNORM is what
        // PostChain gives every one of its own internal targets, and it is what the old constructor
        // hardcoded, so this is the same texture spelled out.
        return new com.mojang.blaze3d.pipeline.TextureTarget(id.toString(), minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(), true, com.mojang.blaze3d.GpuFormat.RGBA8_UNORM);
    }
    *///?} elif >=1.21.5 {
    /*private static RenderTarget createEffectTarget(Minecraft minecraft, ResourceLocation id) {
        // 1.21.5 gave every render target a debug label, and dropped setClearColor — the clear
        // colour is an argument to the clear command itself now, see clearEffectTarget.
        return new com.mojang.blaze3d.pipeline.TextureTarget(id.toString(), minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(), true);
    }
    *///?} elif >=1.21.2 {
    /*private static RenderTarget createEffectTarget(Minecraft minecraft, ResourceLocation id) {
        RenderTarget renderTarget = new com.mojang.blaze3d.pipeline.TextureTarget(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(), true);
        renderTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        return renderTarget;
    }
    *///?}

    //? if >=26.2 {
    /*private static void clearEffectTarget(RenderTarget target) {
        // 26.2 takes the clear colour as a float vector rather than a packed ARGB int; ARGB's own
        // converter is what vanilla's RenderTargetDescriptor uses for exactly this, so zero still
        // means fully transparent black.
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), net.minecraft.util.ARGB.vector4fFromARGB32(0), target.getDepthTexture(), 1.0);
    }

    private static void bindMainForWrite(RenderTarget mainTarget) {
        // Nothing to put back: a render pass names the texture it draws into, so there is no
        // globally bound framebuffer for the effect to have taken away.
    }
    *///?} elif >=1.21.5 {
    /*private static void clearEffectTarget(RenderTarget target) {
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), 0, target.getDepthTexture(), 1.0);
    }

    private static void bindMainForWrite(RenderTarget mainTarget) {
        // Nothing to put back: a render pass names the texture it draws into, so there is no
        // globally bound framebuffer for the effect to have taken away.
    }
    *///?} elif >=1.21.2 {
    /*private static void clearEffectTarget(RenderTarget target) {
        target.clear();
    }

    private static void bindMainForWrite(RenderTarget mainTarget) {
        mainTarget.bindWrite(false);
    }
    *///?} else {
    private static void clearEffectTarget(RenderTarget target) {
        target.clear(Minecraft.ON_OSX);
    }

    private static void bindMainForWrite(RenderTarget mainTarget) {
        mainTarget.bindWrite(false);
    }
    //?}

    // Its own gate chain rather than an arm of the one above, because 1.21.6 changes only this one
    // helper of the three and a sibling gate cannot be opened inside an elif-chained arm — the
    // reopened block would re-attach to the leading `if`.
    //
    // From 1.21.5 this is vanilla's own RenderTarget#blitAndBlendToTexture, drawing into the main
    // target's colour texture with this mod's blend function instead of the outline one. 1.21.6
    // moved every texture binding onto GpuTextureView: a render pass names a view to draw into and
    // samplers take one, while the clear command above still takes the texture itself. It also gave
    // the pass a debug label supplier and drawIndexed a base-vertex and instance-count.
    // 1.21.9 draws every full-screen pass as a single vertex-shader-generated triangle: the
    // pipeline binds DefaultVertexFormat.EMPTY over core/screenquad, which derives its position
    // and texCoord from gl_VertexID, so there is no vertex buffer and no index buffer left to
    // set and the whole draw is draw(0, 3). Vanilla's own PostPass and LightTexture do exactly
    // this — RenderSystem.getQuadVertexBuffer() is gone with the quad it fed.
    // Binding the source target as the shader's InSampler. Its own gate chain, and called from both
    // arms of blitEffect below, because arms do not nest and 1.21.11 changes only this one line.
    //
    // 1.21.11 split the sampler out of the texture: RenderPass#bindSampler is gone and bindTexture
    // takes a GpuSampler alongside the view. NEAREST clamp-to-edge is what vanilla's own PostPass
    // gives a non-bilinear input, and this blit is a 1:1 full-screen copy, so there is nothing to
    // filter. The cache hands back a shared instance, so nothing here owns or closes it.
    //? if >=1.21.11 {
    /*private static void bindEffectSource(com.mojang.blaze3d.systems.RenderPass pass, RenderTarget source) {
        pass.bindTexture("InSampler", source.getColorTextureView(),
                RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.NEAREST));
    }
    *///?} elif >=1.21.6 {
    /*private static void bindEffectSource(com.mojang.blaze3d.systems.RenderPass pass, RenderTarget source) {
        pass.bindSampler("InSampler", source.getColorTextureView());
    }
    *///?}

    //? if >=26.2 {
    /*private static void blitEffect(RenderTarget source) {
        // 26.2 spells the pass's optional clear colour as an Optional<Vector4fc> rather than an
        // OptionalInt, and draw's arguments became (vertexCount, instanceCount, firstVertex,
        // firstInstance) — vanilla's own PostPass draws the same screen-quad triangle as (3, 1, 0, 0).
        try (com.mojang.blaze3d.systems.RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "alexscaves post effect blit",
                        Minecraft.getInstance().getMainRenderTarget().getColorTextureView(), java.util.Optional.empty())) {
            pass.setPipeline(com.github.alexmodguy.alexscaves.client.render.ACInternalShaders.POST_EFFECT_BLIT);
            bindEffectSource(pass, source);
            pass.draw(3, 1, 0, 0);
        }
    }
    *///?} elif >=1.21.9 {
    /*private static void blitEffect(RenderTarget source) {
        try (com.mojang.blaze3d.systems.RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "alexscaves post effect blit",
                        Minecraft.getInstance().getMainRenderTarget().getColorTextureView(), java.util.OptionalInt.empty())) {
            pass.setPipeline(com.github.alexmodguy.alexscaves.client.render.ACInternalShaders.POST_EFFECT_BLIT);
            bindEffectSource(pass, source);
            pass.draw(0, 3);
        }
    }
    *///?} elif >=1.21.6 {
    /*private static void blitEffect(RenderTarget source) {
        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        com.mojang.blaze3d.buffers.GpuBuffer indexBuffer = indices.getBuffer(6);
        com.mojang.blaze3d.buffers.GpuBuffer vertexBuffer = RenderSystem.getQuadVertexBuffer();
        try (com.mojang.blaze3d.systems.RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "alexscaves post effect blit",
                        Minecraft.getInstance().getMainRenderTarget().getColorTextureView(), java.util.OptionalInt.empty())) {
            pass.setPipeline(com.github.alexmodguy.alexscaves.client.render.ACInternalShaders.POST_EFFECT_BLIT);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.setIndexBuffer(indexBuffer, indices.type());
            bindEffectSource(pass, source);
            pass.drawIndexed(0, 0, 6, 1);
        }
    }
    *///?} elif >=1.21.5 {
    /*private static void blitEffect(RenderTarget source) {
        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        com.mojang.blaze3d.buffers.GpuBuffer indexBuffer = indices.getBuffer(6);
        com.mojang.blaze3d.buffers.GpuBuffer vertexBuffer = RenderSystem.getQuadVertexBuffer();
        try (com.mojang.blaze3d.systems.RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(Minecraft.getInstance().getMainRenderTarget().getColorTexture(), java.util.OptionalInt.empty())) {
            pass.setPipeline(com.github.alexmodguy.alexscaves.client.render.ACInternalShaders.POST_EFFECT_BLIT);
            pass.setVertexBuffer(0, vertexBuffer);
            pass.setIndexBuffer(indexBuffer, indices.type());
            pass.bindSampler("InSampler", source.getColorTexture());
            pass.drawIndexed(0, 6);
        }
    }
    *///?} elif >=1.21.2 {
    /*private static void blitEffect(RenderTarget source) {
        // 1.21.2 split the old blitToScreen(w, h, disableBlend) in two: blitToScreen is a raw
        // glBlitFramebuffer that ignores blending entirely, and blitAndBlendToScreen is the
        // shader-driven draw that respects the blend func blitEffects sets — which is the one this
        // has always wanted, having passed disableBlend = false.
        source.blitAndBlendToScreen(Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow().getHeight());
    }
    *///?} else {
    private static void blitEffect(RenderTarget source) {
        source.blitToScreen(Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow().getHeight(), false);
    }
    //?}

    private static class PostEffect {
        private final PostChain postChain;
        private final RenderTarget renderTarget;
        private boolean enabled;

        // PostChain#process(float) used to keep this itself, wrapping at twenty seconds and handing
        // the passes time/20. Nothing does that in 1.21.2, so the effect keeps its own clock.
        //? if >=1.21.2
        /*private float time;*/

        public PostEffect(PostChain postChain, RenderTarget renderTarget, boolean enabled) {
            this.postChain = postChain;
            this.renderTarget = renderTarget;
            this.enabled = enabled;
        }

        public PostChain getPostChain() {
            return postChain;
        }

        public RenderTarget getRenderTarget() {
            return renderTarget;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        //? if >=1.21.2 {
        /*public float advanceTime(float partialTick) {
            this.time += partialTick;
            while (this.time > 20.0F) {
                this.time -= 20.0F;
            }
            return this.time / 20.0F;
        }
        *///?}

        public void close() {
            //? if >=1.21.2 {
            /*// The chain belongs to ShaderManager, which closes it on reload; the target is ours.
            if (renderTarget != null) {
                renderTarget.destroyBuffers();
            }
            *///?} else {
            if (postChain != null) {
                postChain.close();
            }
            //?}
        }

        public void resize(int x, int y) {
            //? if >=1.21.2 {
            /*if (renderTarget != null) {
                renderTarget.resize(x, y);
            }
            *///?} else {
            if (postChain != null) {
                postChain.resize(x, y);
            }
            //?}
        }
    }
}
