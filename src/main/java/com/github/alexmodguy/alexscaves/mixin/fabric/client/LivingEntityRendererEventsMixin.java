package com.github.alexmodguy.alexscaves.mixin.fabric.client;

import com.github.alexmodguy.alexscaves.fabric.forge.client.event.RenderLivingEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric's producer for {@code RenderLivingEvent.Pre} and {@code .Post}, which Forge and NeoForge
 * post from their own patch of the same two points in {@code LivingEntityRenderer}.
 *
 * <p>Three consumers depend on it, none of them cosmetic: {@code ClientEvents#preRenderLiving}
 * applies a magnetised mob's head rotation and suppresses the vanilla draw of an entity this mod
 * has already drawn itself (the watcher, the possession camera), and {@code #postRenderLiving}
 * resets that rotation, draws every raygun beam in the world and draws the darkness-incarnate
 * trail. Without a producer all three are dead on Fabric — the beams and the trail simply never
 * appear, and a blocked render is drawn twice.
 *
 * <p>⚠️ The renderer type is spelled out inline and is never imported. {@code
 * !mc2102-render-import-living} rewrites the whole {@code import
 * net.minecraft.client.renderer.entity.LivingEntityRenderer;} statement onto this mod's render shim
 * from 1.21.2, which would silently retarget the {@code @Mixin} at a class that is not the one
 * vanilla draws with — it compiles clean and dies at mixin-apply. The stand-in event's own javadoc
 * carries the same warning for the same reason.
 *
 * <p>Four bands, all read out of the bytecode rather than inferred:
 *
 * <ul>
 *   <li>{@code <1.21.2} — {@code render(T, float, float, PoseStack, MultiBufferSource, int)}, the
 *       entity in the first slot and the partial tick in the third;</li>
 *   <li>{@code 1.21.2 – 1.21.8} — {@code render(S, PoseStack, MultiBufferSource, int)}. The entity
 *       moved out of the signature entirely, so the partial tick comes from {@link
 *       com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess}, which {@code
 *       mixin.renderstate.EntityRendererMixin} stashes on every state it extracts;</li>
 *   <li>{@code 1.21.9 – 26.1.2} and {@code >=26.2} — {@code submit(S, PoseStack,
 *       SubmitNodeCollector, CameraRenderState)}. Two arms rather than one only because {@code
 *       CameraRenderState} moved to {@code …renderer.state.level} at 26.2, and a {@code method}
 *       selector is matched by descriptor.</li>
 * </ul>
 *
 * <p>The stand-in event keeps one six-argument constructor across all four, taking a {@code
 * MultiBufferSource} and a packed light — which is what {@code ClientEvents}' Fabric arms read back
 * — so the two submit bands wrap the frame's collector in an {@link
 * com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers} and take the light off
 * {@code state.lightCoords}, exactly as the loaders' own {@code >=1.21.9} arms do. Each injection
 * flushes the recording it made before returning, including on the cancelling path — {@code
 * ClientEvents} relays a {@code Post} into the very same buffers when it cancels a {@code Pre}, and
 * a recording that is never flushed draws nothing at all.
 */
@Mixin(net.minecraft.client.renderer.entity.LivingEntityRenderer.class)
@SuppressWarnings({"rawtypes", "unchecked"})
public class LivingEntityRendererEventsMixin {

    //? if >=26.2 {
    /*@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", remap = true, cancellable = true, at = @At(value = "HEAD"))
    private void ac_fabricPreRenderLiving(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.level.CameraRenderState camera, CallbackInfo ci) {
        com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers buffers =
                new com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers(collector, camera);
        boolean cancel = MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Pre(state, (net.minecraft.client.renderer.entity.LivingEntityRenderer) (Object) this, com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess.partialTick(state), poseStack, buffers, state.lightCoords));
        buffers.flush();
        if (cancel) {
            ci.cancel();
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", remap = true, at = @At(value = "TAIL"))
    private void ac_fabricPostRenderLiving(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.level.CameraRenderState camera, CallbackInfo ci) {
        com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers buffers =
                new com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers(collector, camera);
        MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post(state, (net.minecraft.client.renderer.entity.LivingEntityRenderer) (Object) this, com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess.partialTick(state), poseStack, buffers, state.lightCoords));
        buffers.flush();
    }
    *///?} elif >=1.21.9 {
    /*@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", remap = true, cancellable = true, at = @At(value = "HEAD"))
    private void ac_fabricPreRenderLiving(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.CameraRenderState camera, CallbackInfo ci) {
        com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers buffers =
                new com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers(collector, camera);
        boolean cancel = MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Pre(state, (net.minecraft.client.renderer.entity.LivingEntityRenderer) (Object) this, com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess.partialTick(state), poseStack, buffers, state.lightCoords));
        buffers.flush();
        if (cancel) {
            ci.cancel();
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", remap = true, at = @At(value = "TAIL"))
    private void ac_fabricPostRenderLiving(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.CameraRenderState camera, CallbackInfo ci) {
        com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers buffers =
                new com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers(collector, camera);
        MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post(state, (net.minecraft.client.renderer.entity.LivingEntityRenderer) (Object) this, com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess.partialTick(state), poseStack, buffers, state.lightCoords));
        buffers.flush();
    }
    *///?} elif >=1.21.2 {
    /*@Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", remap = true, cancellable = true, at = @At(value = "HEAD"))
    private void ac_fabricPreRenderLiving(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Pre(state, (net.minecraft.client.renderer.entity.LivingEntityRenderer) (Object) this, com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess.partialTick(state), poseStack, bufferSource, packedLight))) {
            ci.cancel();
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", remap = true, at = @At(value = "TAIL"))
    private void ac_fabricPostRenderLiving(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post(state, (net.minecraft.client.renderer.entity.LivingEntityRenderer) (Object) this, com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess.partialTick(state), poseStack, bufferSource, packedLight));
    }
    *///?} else {
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", remap = true, cancellable = true, at = @At(value = "HEAD"))
    private void ac_fabricPreRenderLiving(LivingEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Pre(entity, (net.minecraft.client.renderer.entity.LivingEntityRenderer) (Object) this, partialTick, poseStack, bufferSource, packedLight))) {
            ci.cancel();
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", remap = true, at = @At(value = "TAIL"))
    private void ac_fabricPostRenderLiving(LivingEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post(entity, (net.minecraft.client.renderer.entity.LivingEntityRenderer) (Object) this, partialTick, poseStack, bufferSource, packedLight));
    }
    //?}
}
