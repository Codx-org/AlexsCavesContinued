package com.github.alexmodguy.alexscaves.mixin.client;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Supplies {@link com.github.alexmodguy.alexscaves.client.ACLevelRenderStage} on nodes whose loader
 * has no render-stage event of its own.
 *
 * <p>Forge deleted {@code RenderLevelStageEvent} in 1.21.2, when the level renderer became a frame
 * graph, and has not brought it back. NeoForge kept the event, so its nodes up to 1.21.5 still go
 * through {@code ClientEvents#postRenderStage} and {@code CitadelClientEvents#renderWorldLastEvent}
 * and this mixin does nothing there. Fabric never had the event at all, so those nodes will take this
 * path too — which is why the body is written against the mod's own stage enum rather than Forge's.
 *
 * <p><b>From 1.21.6 this is the path on every loader, NeoForge included.</b> 1.21.6 collapsed the
 * chunk layers into three {@code ChunkSectionLayerGroup}s drawn inside one {@code RenderPass} each, and
 * NeoForge's event followed: it became an abstract base with nine concrete subclasses and <i>no</i>
 * per-cutout-layer one, so it can no longer name two of the six stages this mod uses. One mixin that
 * serves both loaders beats an event that covers two thirds of the enum on one of them.
 *
 * <p>The injection points are the ones NeoForge posts its own events from, checked against each
 * loader's own bytecode:
 * <ul>
 *   <li>the sky pass — the lambda {@code addSkyPass} hands the frame graph, whose single {@code
 *       return} is where NeoForge fires {@code AFTER_SKY}. Neither its name nor its descriptor is
 *       portable: loom's layered mappings call it {@code method_62215} and capture vanilla's four
 *       arguments, while NeoForge's Mojmap jar calls it {@code lambda$addSkyPass$13} and captures six,
 *       because their day-length patch added an {@code addSkyPass} overload taking the model-view
 *       matrix and the lambda closes over it. Hence one arm per loader;</li>
 *   <li>{@code renderEntities}, whose {@code RETURN} is {@code AFTER_ENTITIES}. Its descriptor is
 *       unchanged from 1.21.2 through 1.21.8 on both loaders, so that arm spans the whole range;
 *       1.21.9 splits the method into {@code extractVisibleEntities} and {@code submitEntities} and
 *       the anchor moves to the {@code RETURN} of the second, which is the one the main pass calls;</li>
 *   <li>the chunk layers — {@code renderSectionLayer} once per layer up to 1.21.5, mapped the same way
 *       {@code Stage#fromRenderType} does (its early {@code shader == null} bail jumps to the same
 *       single {@code return}, so {@code RETURN} fires exactly once per layer). From 1.21.6 that method
 *       is gone and the anchor moved to another class entirely, {@code ChunkSectionsToRenderMixin}.</li>
 * </ul>
 *
 * <p>No pose stack is reconstructed <b>on those nodes</b>. NeoForge passes {@code null} for the sky
 * and block-layer stages — the event constructor substitutes a fresh one — and a fresh one for the
 * entity stage, so the stack listeners see has always been identity and a new one here is the same
 * thing. ⚠️ That is <b>not</b> true of the Fabric band below 1.21.6, which passes the real one; see
 * the block at the top of the class body.
 */
@Mixin(LevelRenderer.class)
public class LevelRenderStageMixin {

    // ── Fabric below 1.21.6: vanilla's own immediate-mode level render ──────────────────────────
    // The band the arms further down do not reach. Forge and NeoForge both have the event here, and
    // >=1.21.6 is covered on every loader by the arms below; what is left is Fabric, which never had
    // an event at all, on the versions where the level renderer still draws as it walks.
    //
    // Every anchor below was read out of the VANILLA UNPATCHED 1.20.1 jar and then checked against
    // the Forge-patched one, so that each fires exactly where Forge posts its own event — the point
    // being to reproduce the loader, not to pick a plausible-looking hook:
    //
    //   * AFTER_SKY is NOT a TAIL inject on renderSky. ⚠️ renderSky has FIVE returns on 1.20.1
    //     (offsets 12, 45, 70, 90, 1125), so TAIL would fire only on the ordinary overworld path and
    //     go silent in the End, under a custom sky and on the two other early bails — while Forge
    //     dispatches unconditionally after the call returns, at offset 456. So the anchor is that
    //     call site, inside renderLevel, with shift AFTER. It occurs exactly once, hence no ordinal.
    //     General form, worth carrying: before translating a loader hook to a TAIL, count the
    //     target's returns.
    //   * AFTER_ENTITIES has no method of its own to hang off — Forge posts all seven of its
    //     Stage-overload dispatches from inside renderLevel — so the anchor is the profiler push
    //     that immediately follows the one Forge sits behind: the entity batches are ended, then
    //     `popPush("blockentities")`. Also exactly one occurrence, so INVOKE_STRING needs no ordinal.
    //   * The chunk layers come from renderChunkLayer, which 1.20.2 renamed renderSectionLayer
    //     without touching its descriptor or its position. That rename cannot be a //? gate — the
    //     selector is inside this arm and Stonecutter does not nest — so the !mc202-rendersectionlayer
    //     replacement rule rewrites the one `method = "…"` fragment. The handler still takes the
    //     1.20.1 parameter list because the descriptor never moved. It has a single return, so
    //     RETURN fires exactly once per layer, which is how the >=1.21.2 arm reads it too.
    //
    // ⚠️ The PoseStack handed over is the REAL, camera-rotated one, not a fresh identity stack, and
    // that is the difference from every other arm in this file. Up to 1.20.4 vanilla threads a stack
    // through renderLevel and renderChunkLayer and Forge's event carries that very object — which is
    // what ACClientCompat.poseStack(event)'s `else` arm returns, and what the raygun, hologram,
    // corrodent, licowitch and ambersol batch draws in ClientEvents#renderStage were written
    // against. From 1.20.5 vanilla stopped threading one, which is why the newer arms legitimately
    // pass identity. A new band must match its own era rather than copying the newest arm.
    //
    // ⚠️ This arm was written bounded at <1.21.6 — the true semantic boundary — on the reasoning that
    // a gate which is too WIDE fails loudly in verify_mixins.py on the first node that needs
    // splitting, while one that is too narrow leaves a Fabric node with no render stages at all and
    // says nothing. 1.20.5-fabric is that node and it did fail loudly, so the bound is <1.20.5 now
    // and the band above it is the arm that follows. The reasoning is left here because it is the
    // reason the failure was cheap.
    //
    // partialTick is ACClientCompat.partialTick() rather than renderLevel's own argument, because
    // renderChunkLayer has none — and it is the same value regardless: javap on ForgeHooksClient
    // shows the event is built with Minecraft#getPartialTick(), which is precisely what that helper
    // returns on both of its arms.
    //? if fabric && <1.20.5 {
    /*@org.spongepowered.asm.mixin.Shadow
    private int ticks;

    @org.spongepowered.asm.mixin.Unique
    private void ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage stage, com.mojang.blaze3d.vertex.PoseStack poseStack) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.dispatch(stage, (LevelRenderer) (Object) this, poseStack, this.ticks, minecraft.gameRenderer.getMainCamera(), com.github.alexmodguy.alexscaves.client.ACClientCompat.partialTick());
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "renderLevel", at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V", shift = org.spongepowered.asm.mixin.injection.At.Shift.AFTER))
    private void ac_afterSky(com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, net.minecraft.client.Camera camera, net.minecraft.client.renderer.GameRenderer gameRenderer, net.minecraft.client.renderer.LightTexture lightTexture, org.joml.Matrix4f projectionMatrix, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_SKY, poseStack);
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "renderLevel", at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=blockentities"))
    private void ac_afterEntities(com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, net.minecraft.client.Camera camera, net.minecraft.client.renderer.GameRenderer gameRenderer, net.minecraft.client.renderer.LightTexture lightTexture, org.joml.Matrix4f projectionMatrix, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_ENTITIES, poseStack);
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "renderChunkLayer", at = @org.spongepowered.asm.mixin.injection.At("RETURN"))
    private void ac_afterChunkLayer(net.minecraft.client.renderer.RenderType layer, com.mojang.blaze3d.vertex.PoseStack poseStack, double camX, double camY, double camZ, org.joml.Matrix4f projectionMatrix, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        com.github.alexmodguy.alexscaves.client.ACLevelRenderStage stage = com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.ofChunkLayer(layer);
        if (stage != null) {
            ac_dispatch(stage, poseStack);
        }
    }
    *///?}

    // 1.20.5 through 1.21.1 on Fabric. The three anchors are the same three places — after the sky
    // call, at the profiler's handover to the block entities, and at the end of each chunk layer —
    // and every one of them moved for the same reason: 1.20.5 stopped threading a PoseStack through
    // the level render. renderLevel drops it (and gains the frustum matrix beside the projection
    // one), renderSky takes the two matrices instead, and renderSectionLayer follows suit. Nothing
    // about WHERE the stages fire changed, so the two dispatch sites inside renderLevel are still
    // the single renderSky call (offset 449 on 1.20.5) and the single ldc "blockentities" (1152).
    //
    // ⚠️ The stack handed to a listener is therefore a fresh identity one from here up, exactly as
    // in the >=1.21.2 arms — see the warning above the previous arm, which explains why the older
    // band is the odd one out and why a new band must match its own era rather than the newest arm.
    // Every draw ClientEvents#renderStage makes is camera-relative already, so nothing reads it.
    //
    // partialTick still comes from ACClientCompat rather than a DeltaTracker: that type does not
    // exist below 1.21, which is the whole reason this band cannot simply share the >=1.21.2 helper.
    //
    // ⚠️ Bounded at <1.21.2, not <1.21.6, and the bound is a LOADER-ROUTING decision rather than a
    // vanilla one. 1.21.2 turned the level render into a frame graph on every loader: renderLevel
    // gained a leading GraphicsResourceAllocator, renderSky left it for a pass lambda, and
    // renderChunkLayer's successor renderSectionLayer is what the graph calls. NeoForge is the only
    // loader that does not care, because it kept RenderLevelStageEvent and this mixin contributes
    // nothing there until 1.21.6. Forge and Fabric both need the frame-graph anchors from 1.21.2,
    // so the three arms below that used to read `forge && >=1.21.2` read `!neoforge && >=1.21.2`
    // now — Fabric joins Forge rather than getting arms of its own, since every anchor those arms
    // name (renderEntities, renderSectionLayer, the private int ticks, and the sky lambda's
    // loom-intermediary name method_62215) was javap'd on the Fabric jar and is byte-identical.
    //? if fabric && >=1.20.5 && <1.21.2 {
    /*@org.spongepowered.asm.mixin.Shadow
    private int ticks;

    @org.spongepowered.asm.mixin.Unique
    private void ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage stage) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.dispatch(stage, (LevelRenderer) (Object) this, new com.mojang.blaze3d.vertex.PoseStack(), this.ticks, minecraft.gameRenderer.getMainCamera(), com.github.alexmodguy.alexscaves.client.ACClientCompat.partialTick());
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "renderChunkLayer", at = @org.spongepowered.asm.mixin.injection.At("RETURN"))
    private void ac_afterChunkLayer(net.minecraft.client.renderer.RenderType layer, double camX, double camY, double camZ, org.joml.Matrix4f frustumMatrix, org.joml.Matrix4f projectionMatrix, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        com.github.alexmodguy.alexscaves.client.ACLevelRenderStage stage = com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.ofChunkLayer(layer);
        if (stage != null) {
            ac_dispatch(stage);
        }
    }
    *///?}

    // The two dispatch sites inside renderLevel are split off from the block above because
    // renderLevel's own parameter list moves inside that band while everything else in it does not:
    // 1.21 folded `float partialTick, long finishNanoTime` into a single DeltaTracker. Neither
    // anchor moved with it — the sole renderSky call is at offset 437 on 1.21 against 449 on 1.20.5,
    // the sole ldc "blockentities" at 1137 against 1152 — and renderSectionLayer is untouched, which
    // is why only these two handlers are duplicated and the shadow, the helper and the chunk-layer
    // hook stay shared. An @Inject handler must mirror its target's arguments, so this is a
    // signature split and nothing more.
    //? if fabric && >=1.20.5 && <1.21 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "renderLevel", at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V", shift = org.spongepowered.asm.mixin.injection.At.Shift.AFTER))
    private void ac_afterSky(float partialTick, long finishNanoTime, boolean renderBlockOutline, net.minecraft.client.Camera camera, net.minecraft.client.renderer.GameRenderer gameRenderer, net.minecraft.client.renderer.LightTexture lightTexture, org.joml.Matrix4f frustumMatrix, org.joml.Matrix4f projectionMatrix, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_SKY);
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "renderLevel", at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=blockentities"))
    private void ac_afterEntities(float partialTick, long finishNanoTime, boolean renderBlockOutline, net.minecraft.client.Camera camera, net.minecraft.client.renderer.GameRenderer gameRenderer, net.minecraft.client.renderer.LightTexture lightTexture, org.joml.Matrix4f frustumMatrix, org.joml.Matrix4f projectionMatrix, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_ENTITIES);
    }
    *///?}

    //? if fabric && >=1.21 && <1.21.2 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "renderLevel", at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V", shift = org.spongepowered.asm.mixin.injection.At.Shift.AFTER))
    private void ac_afterSky(net.minecraft.client.DeltaTracker deltaTracker, boolean renderBlockOutline, net.minecraft.client.Camera camera, net.minecraft.client.renderer.GameRenderer gameRenderer, net.minecraft.client.renderer.LightTexture lightTexture, org.joml.Matrix4f frustumMatrix, org.joml.Matrix4f projectionMatrix, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_SKY);
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "renderLevel", at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=blockentities"))
    private void ac_afterEntities(net.minecraft.client.DeltaTracker deltaTracker, boolean renderBlockOutline, net.minecraft.client.Camera camera, net.minecraft.client.renderer.GameRenderer gameRenderer, net.minecraft.client.renderer.LightTexture lightTexture, org.joml.Matrix4f frustumMatrix, org.joml.Matrix4f projectionMatrix, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_ENTITIES);
    }
    *///?}

    // The entity anchor, up to 1.21.8.
    //? if ((!neoforge && >=1.21.2) || >=1.21.6) && <1.21.9 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "renderEntities", at = @org.spongepowered.asm.mixin.injection.At("RETURN"))
    private void ac_afterEntities(com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource, net.minecraft.client.Camera camera, net.minecraft.client.DeltaTracker deltaTracker, java.util.List<net.minecraft.world.entity.Entity> entities, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_ENTITIES);
    }
    *///?}

    // And from 1.21.9, which split renderEntities in two: extractVisibleEntities fills the
    // LevelRenderState, submitEntities queues the draws, and it is the latter the main pass calls.
    //? if >=1.21.9 && <26.2 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "submitEntities", at = @org.spongepowered.asm.mixin.injection.At("RETURN"))
    private void ac_afterEntities(com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.state.LevelRenderState levelRenderState, net.minecraft.client.renderer.SubmitNodeCollector collector, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_ENTITIES);
    }
    *///?}

    // The shared half: the tick counter and the dispatch helper, on every node this mixin is live for
    // — bounded above at 26.2, which deleted LevelRenderer#ticks along with #getTicks(). The 26.2
    // block at the bottom of this file carries its own copy of both.
    //? if ((!neoforge && >=1.21.2) || >=1.21.6) && <26.2 {
    /*@org.spongepowered.asm.mixin.Shadow
    private int ticks;

    @org.spongepowered.asm.mixin.Unique
    private void ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage stage) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.dispatch(stage, (LevelRenderer) (Object) this, new com.mojang.blaze3d.vertex.PoseStack(), this.ticks, minecraft.gameRenderer.getMainCamera(), minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }
    *///?}

    // The sky lambda, up to 1.21.5: vanilla's capture, with FogParameters ahead of the sky type.
    // Both loom-mapped loaders spell it method_62215 — checked in the Fabric 1.21.2 jar, not assumed
    // from Forge — which is why one arm serves them both.
    //? if !neoforge && >=1.21.2 && <1.21.6 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "method_62215", at = @org.spongepowered.asm.mixin.injection.At("TAIL"))
    private void ac_afterSky(net.minecraft.client.renderer.FogParameters fogParameters, net.minecraft.client.renderer.DimensionSpecialEffects.SkyType skyType, float partialTick, net.minecraft.client.renderer.DimensionSpecialEffects effects, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_SKY);
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "renderSectionLayer", at = @org.spongepowered.asm.mixin.injection.At("RETURN"))
    private void ac_afterSectionLayer(net.minecraft.client.renderer.RenderType layer, double camX, double camY, double camZ, org.joml.Matrix4f frustumMatrix, org.joml.Matrix4f projectionMatrix, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        com.github.alexmodguy.alexscaves.client.ACLevelRenderStage stage = com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.ofChunkLayer(layer);
        if (stage != null) {
            ac_dispatch(stage);
        }
    }
    *///?}

    // And from 1.21.6, where FogParameters gave way to a GpuBufferSlice of fog uniforms. Every
    // loom-mapped loader — Forge here, Fabric when it lands — keeps the intermediary lambda name.
    //? if !neoforge && >=1.21.6 && <1.21.9 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "method_62215", at = @org.spongepowered.asm.mixin.injection.At("TAIL"))
    private void ac_afterSky(com.mojang.blaze3d.buffers.GpuBufferSlice fog, net.minecraft.client.renderer.DimensionSpecialEffects.SkyType skyType, float partialTick, net.minecraft.client.renderer.DimensionSpecialEffects effects, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_SKY);
    }
    *///?}

    // NeoForge's own arm: Mojmap lambda name, and two more captured arguments than vanilla because
    // their overload of addSkyPass takes the model-view matrix the day-length patch needs.
    //? if neoforge && >=1.21.6 && <1.21.9 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "lambda$addSkyPass$13", at = @org.spongepowered.asm.mixin.injection.At("TAIL"))
    private void ac_afterSky(float partialTick, org.joml.Matrix4f modelViewMatrix, net.minecraft.client.Camera camera, com.mojang.blaze3d.buffers.GpuBufferSlice fog, net.minecraft.client.renderer.DimensionSpecialEffects.SkyType skyType, net.minecraft.client.renderer.DimensionSpecialEffects effects, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_SKY);
    }
    *///?}

    // 1.21.9 gives the sky a render state of its own, so both lambdas' captures collapse to it plus
    // the fog slice — and NeoForge's index moves from 13 to 8 with the lambdas the rewrite deleted.
    // The two loaders still disagree about the capture ORDER (and NeoForge still closes over the
    // model-view matrix their day-length patch threads through), so it is still one arm each.
    //? if !neoforge && >=1.21.9 && <1.21.11 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "method_62215", at = @org.spongepowered.asm.mixin.injection.At("TAIL"))
    private void ac_afterSky(com.mojang.blaze3d.buffers.GpuBufferSlice fog, net.minecraft.client.renderer.state.SkyRenderState skyState, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_SKY);
    }
    *///?}

    //? if neoforge && >=1.21.9 && <1.21.11 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "lambda$addSkyPass$8", at = @org.spongepowered.asm.mixin.injection.At("TAIL"))
    private void ac_afterSky(net.minecraft.client.renderer.state.SkyRenderState skyState, org.joml.Matrix4f modelViewMatrix, com.mojang.blaze3d.buffers.GpuBufferSlice fog, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_SKY);
    }
    *///?}

    // 1.21.11 hands the lambda the SkyRenderer itself — the renderer is a field of the level
    // renderer no longer, so the capture carries it. On the loom-mapped loaders it also became
    // STATIC, having stopped closing over `this` with it, and Mixin matches a handler's static-ness
    // against its target's: hence a static handler here, which cannot use ac_dispatch and reaches
    // the level renderer the same way ChunkSectionsToRenderMixin does. NeoForge's lambda still
    // closes over the model-view matrix their day-length patch threads through, so it stays an
    // instance method and keeps its own arm.
    //? if !neoforge && >=1.21.11 && <26.2 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "method_62215", at = @org.spongepowered.asm.mixin.injection.At("TAIL"))
    private static void ac_afterSky(com.mojang.blaze3d.buffers.GpuBufferSlice fog, net.minecraft.client.renderer.state.SkyRenderState skyState, net.minecraft.client.renderer.SkyRenderer skyRenderer, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_SKY, minecraft.levelRenderer, new com.mojang.blaze3d.vertex.PoseStack(), com.github.alexmodguy.alexscaves.client.ACClientCompat.levelRendererTicks(minecraft.levelRenderer), minecraft.gameRenderer.getMainCamera(), minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }
    *///?}

    //? if neoforge && >=1.21.11 && <26 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "lambda$addSkyPass$8", at = @org.spongepowered.asm.mixin.injection.At("TAIL"))
    private void ac_afterSky(net.minecraft.client.renderer.state.SkyRenderState skyState, org.joml.Matrix4f modelViewMatrix, com.mojang.blaze3d.buffers.GpuBufferSlice fog, net.minecraft.client.renderer.SkyRenderer skyRenderer, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_SKY);
    }
    *///?}

    // 26 renumbers NeoForge's lambda back to 0 — the sky pass is built from one overload again — and
    // narrows the matrix it closes over to the read-only Matrix4fc. Two differences, so this is a
    // fresh arm rather than a rename rule. Forge needs no arm of its own here: from 26 it ships
    // official names instead of remapping, so its lambda is javac's `lambda$addSkyPass$0` with
    // vanilla's capture, which is what the `!neoforge && >=1.21.11` arm above already describes —
    // the `!mc261-skypass-lambda` replacement rule is the whole of its port.
    //? if neoforge && >=26 && <26.2 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "lambda$addSkyPass$0", at = @org.spongepowered.asm.mixin.injection.At("TAIL"))
    private void ac_afterSky(net.minecraft.client.renderer.state.SkyRenderState skyState, org.joml.Matrix4fc modelViewMatrix, com.mojang.blaze3d.buffers.GpuBufferSlice fog, net.minecraft.client.renderer.SkyRenderer skyRenderer, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_SKY);
    }
    *///?}

    // ── 26.2: every stage collapses onto one injection ──────────────────────────────────────────
    // 26.2 deleted immediate-mode rendering outright. There is no MultiBufferSource, no
    // Minecraft#renderBuffers(), and the only handle that draws anything is the SubmitNodeCollector
    // the level renderer threads through its submission phase — so a stage fired anywhere that
    // collector is not in scope has nothing to hand its listeners. It also deleted
    // LevelRenderer#ticks and #getTicks(), which is why the @Shadow above and the static sky arm's
    // getTicks() call are both bounded below this.
    //
    // The anchor is the RETURN of LevelRenderer#submitFeatures, the private method that owns the
    // whole frame's submission. javap on both loaders puts its call at offset 69 (Forge) / 67
    // (NeoForge), ahead of FeatureRenderDispatcher#prepareFrame and well ahead of addSkyPass (353 /
    // 379) — so nodes submitted at its RETURN are still picked up. Its descriptor is byte-identical
    // on the two loaders, so this is ONE arm rather than the two every sky anchor since 1.21.2 has
    // needed.
    //
    // All four stages this mod still fires on 26 are dispatched from that one point. What that
    // costs, and why it is acceptable on a pipeline that batches by RenderType and orders the draws
    // itself, is written up on ACRenderContext — which is also where the collector is parked so the
    // legacy `renderBuffers().bufferSource()` sites can reach it.
    //? if >=26.2 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "submitFeatures", at = @org.spongepowered.asm.mixin.injection.At("RETURN"))
    private void ac_submitFeatures(net.minecraft.client.renderer.state.LevelRenderState levelRenderState, net.minecraft.client.renderer.SubmitNodeCollector collector, boolean flag, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        com.github.alexmodguy.alexscaves.client.render.compat.ACRenderContext.push(collector);
        try {
            ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_SKY);
            ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_ENTITIES);
            ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_CUTOUT_BLOCKS);
            ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.AFTER_TRANSLUCENT_BLOCKS);
        } finally {
            com.github.alexmodguy.alexscaves.client.render.compat.ACRenderContext.pop();
        }
    }

    // The render tick the deleted `ticks` field used to carry. It counted client ticks and nothing
    // else, so the level's own game time answers the same question for every consumer of it in this
    // tree — the raygun's beam phase and the hologram batch, both of which only take it modulo a
    // period. The cast is lossy after 2^31 ticks, i.e. after about three and a half years of
    // continuous play, and wraps rather than saturating, so a modulo still behaves.
    @org.spongepowered.asm.mixin.Unique
    private void ac_dispatch(com.github.alexmodguy.alexscaves.client.ACLevelRenderStage stage) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        com.github.alexmodguy.alexscaves.client.ACLevelRenderStage.dispatch(stage, (LevelRenderer) (Object) this, new com.mojang.blaze3d.vertex.PoseStack(), (int) minecraft.level.getGameTime(), minecraft.gameRenderer.getMainCamera(), minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }
    *///?}
}
