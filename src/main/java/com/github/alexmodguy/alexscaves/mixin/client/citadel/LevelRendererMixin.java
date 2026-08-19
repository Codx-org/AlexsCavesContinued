package com.github.alexmodguy.alexscaves.mixin.client.citadel;

import com.github.alexmodguy.alexscaves.citadel.CitadelConstants;
import com.github.alexmodguy.alexscaves.citadel.client.shader.PostEffectRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
// Only the <1.21.2 handler signatures name this type as a Java parameter; from 1.21.2 renderLevel's
// injection handlers take a bare CallbackInfo and every remaining mention is a descriptor string
// inside a gated-out `method =`. 26.1 renamed the class to Lightmap, so the import has to go with it.
//? if <1.21.2
import net.minecraft.client.renderer.LightTexture;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    // Both places below want the main render target, which up to 26.1 was reached through
    // LevelRenderer's own `minecraft` field. 26.2 deleted that field — the renderer is handed the
    // half-dozen services it actually needs instead — and a @Shadow that matches nothing is a
    // class-load failure. Minecraft.getInstance() is what the field held on every version, so
    // asking for it directly is the same value with no gate and one code path everywhere.

    @Inject(method = "Lnet/minecraft/client/renderer/LevelRenderer;resize(II)V",
            remap = CitadelConstants.REMAPREFS,
            at = @At("TAIL"))
    private void citadel_resize(int x, int y, CallbackInfo ci) {
        PostEffectRegistry.resize(x, y);
    }


    // renderLevel's signature moved four times in this range and every injection below has to
    // follow. 1.20.5 dropped the leading PoseStack (the renderer builds its own now) and appended
    // the projection matrix next to the frustum matrix; 1.21 then replaced the partial tick and
    // nano time with a DeltaTracker; 1.21.2 put a GraphicsResourceAllocator in front of it; and
    // 1.21.4 dropped the LightTexture, which by then only the TAIL injection still names — the
    // other three had moved onto renderEntities or into a pass lambda.
    //
    // 1.21.2 is more than a signature change, though: renderLevel stopped drawing anything. It now
    // *builds* a FrameGraphBuilder and calls execute() on it at the end, so the world is drawn from
    // inside pass lambdas rather than in renderLevel's own body, and three of these four injection
    // points went with it into addMainPass. Two of them landed in real methods that the pass lambda
    // calls — renderEntities carries both the buffer-source fetch and the outline colour — and only
    // the pre-endOutlineBatch hook is stranded inside the lambda itself. That one is reached with
    // Mixin's regex target selector, `/lambda\$addMainPass\$/`, which is stable in a way naming the
    // synthetic outright is not: the trailing index shifts whenever a loader patch adds or removes
    // a lambda anywhere in addMainPass. Nothing is lost by the regex also matching the pass's other
    // (static) lambdas — Mixin only validates a target it actually found an injection point in.
    //
    // blitEffects still runs at renderLevel's TAIL, which is after execute() and therefore after
    // every pass has drawn, exactly as before.
    //
    // 1.21.9 moves the two survivors again. renderEntities is gone, split into an extraction pass
    // (extractVisibleEntities, which fills the LevelRenderState) and submitEntities, which is what
    // the main-pass lambda actually calls — so the pre-entity hook follows it there. And Forge's
    // mapping of that lambda stopped being `lambda$addMainPass$N`: it is `method_62214` now, which
    // the regex selector does not match, so Forge needs the name spelled out from 1.21.9 while
    // NeoForge still keeps javac's. The endOutlineBatch call itself is untouched on both.
    //
    // The descriptor is spelled out at each site rather than hoisted into a constant: a mixin
    // annotation whose `method` is a constant reference is invisible to scripts/verify_mixins.py,
    // which reads the literal — and a target that silently stops being checked is exactly what
    // that script exists to prevent.
    @Inject(
            //? if >=1.21.9 {
            /*method = "Lnet/minecraft/client/renderer/LevelRenderer;submitEntities(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/state/LevelRenderState;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
            *///?} elif >=1.21.2 {
            /*method = "Lnet/minecraft/client/renderer/LevelRenderer;renderEntities(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/Camera;Lnet/minecraft/client/DeltaTracker;Ljava/util/List;)V",
            *///?} elif >=1.21 {
            /*method = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            *///?} elif >=1.20.5 {
            /*method = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            *///?} else {
            method = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V",
            //?}
            remap = CitadelConstants.REMAPREFS,
            // Below 1.21.2 this is the line before the buffer source is fetched; from 1.21.2 it is
            // the head of the method that fetch feeds, two statements later. Nothing draws in
            // between either way.
            //? if >=1.21.2 {
            /*at = @At(value = "HEAD")
            *///?} else {
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderBuffers;bufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;",
                    shift = At.Shift.BEFORE
            )
            //?}
    )
    //? if >=1.21.2 {
    /*private void citadel_renderLevel_beforeEntities(CallbackInfo ci) {
    *///?} elif >=1.21 {
    /*private void citadel_renderLevel_beforeEntities(net.minecraft.client.DeltaTracker deltaTracker, boolean b, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
    *///?} elif >=1.20.5 {
    /*private void citadel_renderLevel_beforeEntities(float f, long l, boolean b, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
    *///?} else {
    private void citadel_renderLevel_beforeEntities(PoseStack poseStack, float f, long l, boolean b, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci) {
    //?}
        PostEffectRegistry.clearAndBindWrite(Minecraft.getInstance().getMainRenderTarget());
    }

    // A redirect rather than an @Inject shifted BEFORE, which is what this was up to 1.21.1: from
    // 1.21.2 the call sits inside addMainPass' pass lambda, whose synthetic parameter list is every
    // variable that lambda captures — an @Inject handler would have to mirror it, and it changes
    // with any loader patch. A redirect handler mirrors the redirected *call* instead, so this one
    // signature serves every version. endOutlineBatch is called exactly once in the whole client.
    // …and 26 hands it back: Forge ships official names from 26 rather than remapping, so the
    // synthetic is javac's `lambda$addMainPass$0` on both loaders — the same index, since 26 leaves
    // addMainPass with exactly one lambda on each. The `method_62214` spelling is therefore a
    // 1.21.9–1.21.11 Forge window, which is why it carries an upper bound the other arms do not,
    // and 26 can name the lambda outright rather than matching it by pattern. Prefer the literal:
    // verify_mixins.py can assert that a named selector exists and can assert nothing at all about
    // a regex, so the regex arm is a hole in the checker that the older nodes have to live with.
    // ⚠️ FABRIC reaches the method_62214 spelling four versions EARLIER than Forge does — it is
    // already that on the 1.21.2 jar, where Forge still remaps to `lambda$addMainPass$N`. Two
    // loom-mapped loaders, two different answers for the same class, so this is not a `!neoforge`
    // widening but a second predicate in the same arm. javap the jar on every new Fabric node
    // rather than trusting the number: an intermediary index is not an API.
    @Redirect(
            //? if >=26 {
            /*method = "lambda$addMainPass$0",
            *///?} elif (forge && >=1.21.9) || (fabric && >=1.21.2) {
            /*method = "method_62214",
            *///?} elif >=1.21.2 {
            /*method = "/lambda\\$addMainPass\\$/",
            *///?} elif >=1.21 {
            /*method = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            *///?} elif >=1.20.5 {
            /*method = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            *///?} else {
            method = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V",
            //?}
            remap = CitadelConstants.REMAPREFS,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V")
    )
    private void citadel_renderLevel_process(net.minecraft.client.renderer.OutlineBufferSource outlineBufferSource) {
        PostEffectRegistry.processEffects(Minecraft.getInstance().getMainRenderTarget());
        outlineBufferSource.endOutlineBatch();
    }

    @Inject(
            // 1.21.6 dropped the GameRenderer argument — the fog is a GpuBufferSlice of uniforms and
            // the sky colour a Vector4f, both computed by the caller — and appended the flag saying
            // whether the level is being drawn into the world-preview panorama.
            // 1.21.9 inserts a third Matrix4f — the model-view the deferred submit replays each
            // node against, alongside the frustum and projection matrices already there.
            // 26 finishes the move: the Camera and the two matrices that described it collapse into
            // the frame's CameraRenderState, the surviving model-view matrix is the read-only
            // Matrix4fc interface, and the terrain the pass will draw arrives as a prepared
            // ChunkSectionsToRender rather than being gathered inside. TAIL is still after
            // execute(), so blitEffects still runs last.
            // 26.2 renames it to plain `render` and takes the ChunkSectionsToRender back out: the
            // terrain is prepared by a public prepareChunkRenders(Matrix4fc) the caller invokes
            // separately and hands to addMainPass. Everything else about the signature, and TAIL's
            // position after execute(), is unchanged.
            //? if >=26.2 {
            /*method = "Lnet/minecraft/client/renderer/LevelRenderer;render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
            *///?} elif >=26 {
            /*method = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;ZLnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;)V",
            *///?} elif >=1.21.9 {
            /*method = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
            *///?} elif >=1.21.6 {
            /*method = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
            *///?} elif >=1.21.4 {
            /*method = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            *///?} elif >=1.21.2 {
            /*method = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            *///?} elif >=1.21 {
            /*method = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            *///?} elif >=1.20.5 {
            /*method = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
            *///?} else {
            method = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V",
            //?}
            remap = CitadelConstants.REMAPREFS,
            at = @At(
                    value = "TAIL"
            ))
    //? if >=1.21.2 {
    /*private void citadel_renderLevel_end(CallbackInfo ci) {
    *///?} elif >=1.21 {
    /*private void citadel_renderLevel_end(net.minecraft.client.DeltaTracker deltaTracker, boolean b, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
    *///?} elif >=1.20.5 {
    /*private void citadel_renderLevel_end(float f, long l, boolean b, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
    *///?} else {
    private void citadel_renderLevel_end(PoseStack poseStack, float f, long l, boolean b, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci) {
    //?}
        PostEffectRegistry.blitEffects();
    }

}
