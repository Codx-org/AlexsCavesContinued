package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.client.render.misc.CaveMapRenderer;
import com.github.alexmodguy.alexscaves.server.item.ACItemRegistry;
import com.github.alexmodguy.alexscaves.server.item.CaveMapItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
// 26.1 took the block-model dispatch out of the renderer: an item frame's frame model is
// resolved into the render state during extraction (BlockModelResolver#updateForItemFrame) and
// drawn straight off it, so the dispatcher is neither shadowed nor named from 26 up.
//? if <26
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelManager;
//? if <1.21.5
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameRendererMixin {

    // 1.21.2 dropped ModelResourceLocation.vanilla and moved the frame model ids onto
    // BlockStateModelLoader, which already declares exactly the two this needs. 1.21.5 deleted
    // ModelResourceLocation outright: a frame's model is looked up from a fake BlockState handed
    // to BlockRenderDispatcher, so there is no id to name here at all.
    //? if >=1.21.5 {
    /*
    *///?} elif >=1.21.2 {
    /*private static final ModelResourceLocation MAP_FRAME_LOCATION = net.minecraft.client.resources.model.BlockStateModelLoader.MAP_FRAME_LOCATION;
    private static final ModelResourceLocation GLOW_MAP_FRAME_LOCATION = net.minecraft.client.resources.model.BlockStateModelLoader.GLOW_MAP_FRAME_LOCATION;
    *///?} else {
    private static final ModelResourceLocation MAP_FRAME_LOCATION = ModelResourceLocation.vanilla("item_frame", "map=true");
    private static final ModelResourceLocation GLOW_MAP_FRAME_LOCATION = ModelResourceLocation.vanilla("glow_item_frame", "map=true");
    //?}

    //? if >=26 {
    /*@Shadow
    @Final
    private net.minecraft.client.renderer.block.BlockModelResolver blockModelResolver;
    *///?} else {
    @Shadow
    @Final
    private BlockRenderDispatcher blockRenderer;
    //?}

    // ── 1.21.2 and up ─────────────────────────────────────────────────────────────────────────
    // The render-state rewrite changed both what render() receives and who draws the name tag:
    // EntityRenderer#render now does the tag itself (and posts RenderNameTagEvent while doing it),
    // and ItemFrameRenderer#render calls up to it first thing. So instead of cancelling at HEAD and
    // re-firing the event by hand, this arm lets the super call run and takes over immediately
    // after it — the tag is already drawn, and everything below it is ours to replace.
    //
    // 1.21.5 then rewrote the model layer: ModelResourceLocation and ModelManager#getModel are gone,
    // and a frame's model is fetched as a BlockStateModel from a fake BlockState. Only that lookup
    // and the renderModel call (now static on ModelBlockRenderer) differ, but the arms are flat here
    // — Stonecutter cannot nest a gate inside a commented-out branch.
    // 1.21.9 renamed render to submit and swapped the MultiBufferSource + packed light for a
    // SubmitNodeCollector + the CameraRenderState; the light now lives on the render state, as
    // vanilla's own body reads it (state.lightCoords). Everything else this draws is unchanged —
    // getItemFrameFakeState, BlockRenderDispatcher#getBlockModel and the static
    // ModelBlockRenderer#renderModel all survive — so the body only differs in how it gets a
    // VertexConsumer, which is ACSubmitBuffers' job.
    // 26.1 finished the job: the frame's own model is resolved during extraction into a
    // BlockModelRenderState hanging off the render state, and submitting it is one call that carries
    // the outline colour with it. So there is no BlockStateModel to look up, no ModelBlockRenderer
    // to drive and no atlas RenderType to name — and the isInvisible test folds into isEmpty(),
    // since vanilla's extract clears the model when the frame is invisible.
    //? if >=26 {
    /*@Shadow
    public abstract Vec3 getRenderOffset(net.minecraft.client.renderer.entity.state.ItemFrameRenderState state);

    @Inject(
            method = {"Lnet/minecraft/client/renderer/entity/ItemFrameRenderer;submit(Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V"},
            remap = true,
            cancellable = true,
            at = @At(
                    value = "INVOKE",
                    shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V"
            )
    )
    private void ac_renderArmWithItem(net.minecraft.client.renderer.entity.state.ItemFrameRenderState state, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.CameraRenderState cameraState, CallbackInfo ci) {
        ItemStack itemstack = ac_framedStack(state);
        if (itemstack.getItem() == ACItemRegistry.CAVE_MAP.get() && CaveMapItem.isFilled(itemstack)) {
            ci.cancel();
            com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers bufferSource =
                    new com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers(collector, cameraState);
            int packedLight = state.lightCoords;
            poseStack.pushPose();
            Direction direction = state.direction;
            Vec3 vec3 = this.getRenderOffset(state);
            poseStack.translate(-vec3.x(), -vec3.y(), -vec3.z());
            poseStack.translate((double) direction.getStepX() * 0.46875D, (double) direction.getStepY() * 0.46875D, (double) direction.getStepZ() * 0.46875D);
            float xRot;
            float yRot;
            if (direction.getAxis().isHorizontal()) {
                xRot = 0.0F;
                yRot = 180.0F - direction.toYRot();
            } else {
                xRot = (float) (-90 * direction.getAxisDirection().getStep());
                yRot = 180.0F;
            }
            poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
            if (!state.frameModel.isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(-0.5F, -0.5F, -0.5F);
                state.frameModel.submitWithZOffset(poseStack, collector, packedLight, OverlayTexture.NO_OVERLAY, state.outlineColor);
                poseStack.popPose();
            }
            int j = state.rotation % 4 * 2;
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) j * 360.0F / 8.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            float scale = 1F / 128F;
            poseStack.scale(scale, scale, scale);
            CaveMapRenderer.getMapFor(itemstack, false).render(poseStack, bufferSource, itemstack, true, packedLight);
            poseStack.popPose();
            bufferSource.flush();
        }
    }
    *///?} elif >=1.21.9 {
    /*@Shadow
    public abstract Vec3 getRenderOffset(net.minecraft.client.renderer.entity.state.ItemFrameRenderState state);

    @Inject(
            method = {"Lnet/minecraft/client/renderer/entity/ItemFrameRenderer;submit(Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V"},
            remap = true,
            cancellable = true,
            at = @At(
                    value = "INVOKE",
                    shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V"
            )
    )
    private void ac_renderArmWithItem(net.minecraft.client.renderer.entity.state.ItemFrameRenderState state, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.CameraRenderState cameraState, CallbackInfo ci) {
        ItemStack itemstack = ac_framedStack(state);
        if (itemstack.is(ACItemRegistry.CAVE_MAP.get()) && CaveMapItem.isFilled(itemstack)) {
            ci.cancel();
            com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers bufferSource =
                    new com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers(collector, cameraState);
            int packedLight = state.lightCoords;
            poseStack.pushPose();
            Direction direction = state.direction;
            Vec3 vec3 = this.getRenderOffset(state);
            poseStack.translate(-vec3.x(), -vec3.y(), -vec3.z());
            poseStack.translate((double) direction.getStepX() * 0.46875D, (double) direction.getStepY() * 0.46875D, (double) direction.getStepZ() * 0.46875D);
            float xRot;
            float yRot;
            if (direction.getAxis().isHorizontal()) {
                xRot = 0.0F;
                yRot = 180.0F - direction.toYRot();
            } else {
                xRot = (float) (-90 * direction.getAxisDirection().getStep());
                yRot = 180.0F;
            }
            poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
            if (!state.isInvisible) {
                BlockState frameState = net.minecraft.client.resources.model.BlockStateDefinitions.getItemFrameFakeState(state.isGlowFrame, true);
                net.minecraft.client.renderer.block.model.BlockStateModel frameModel = this.blockRenderer.getBlockModel(frameState);
                poseStack.pushPose();
                poseStack.translate(-0.5F, -0.5F, -0.5F);
                net.minecraft.client.renderer.block.ModelBlockRenderer.renderModel(poseStack.last(), bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.entitySolidZOffsetForward(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)), frameModel, 1.0F, 1.0F, 1.0F, packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }
            int j = state.rotation % 4 * 2;
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) j * 360.0F / 8.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            float scale = 1F / 128F;
            poseStack.scale(scale, scale, scale);
            CaveMapRenderer.getMapFor(itemstack, false).render(poseStack, bufferSource, itemstack, true, packedLight);
            poseStack.popPose();
            bufferSource.flush();
        }
    }
    *///?} elif >=1.21.5 {
    /*@Shadow
    public abstract Vec3 getRenderOffset(net.minecraft.client.renderer.entity.state.ItemFrameRenderState state);

    @Inject(
            method = {"Lnet/minecraft/client/renderer/entity/ItemFrameRenderer;render(Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
            remap = true,
            cancellable = true,
            at = @At(
                    value = "INVOKE",
                    shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private void ac_renderArmWithItem(net.minecraft.client.renderer.entity.state.ItemFrameRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        ItemStack itemstack = ac_framedStack(state);
        if (itemstack.is(ACItemRegistry.CAVE_MAP.get()) && CaveMapItem.isFilled(itemstack)) {
            ci.cancel();
            poseStack.pushPose();
            Direction direction = state.direction;
            Vec3 vec3 = this.getRenderOffset(state);
            poseStack.translate(-vec3.x(), -vec3.y(), -vec3.z());
            poseStack.translate((double) direction.getStepX() * 0.46875D, (double) direction.getStepY() * 0.46875D, (double) direction.getStepZ() * 0.46875D);
            // The state carries no xRot/yRot, but an item frame's are a pure function of its facing
            // — this is vanilla's own reconstruction, and it agrees with the entity on every value.
            float xRot;
            float yRot;
            if (direction.getAxis().isHorizontal()) {
                xRot = 0.0F;
                yRot = 180.0F - direction.toYRot();
            } else {
                xRot = (float) (-90 * direction.getAxisDirection().getStep());
                yRot = 180.0F;
            }
            poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
            if (!state.isInvisible) {
                // true = "holds a map": this branch only runs for a filled cave map, which is
                // exactly what the old MAP_FRAME_LOCATION / GLOW_MAP_FRAME_LOCATION pair named.
                BlockState frameState = net.minecraft.client.resources.model.BlockStateDefinitions.getItemFrameFakeState(state.isGlowFrame, true);
                net.minecraft.client.renderer.block.model.BlockStateModel frameModel = this.blockRenderer.getBlockModel(frameState);
                poseStack.pushPose();
                poseStack.translate(-0.5F, -0.5F, -0.5F);
                net.minecraft.client.renderer.block.ModelBlockRenderer.renderModel(poseStack.last(), bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.entitySolidZOffsetForward(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)), frameModel, 1.0F, 1.0F, 1.0F, packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }
            int j = state.rotation % 4 * 2;
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) j * 360.0F / 8.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            float scale = 1F / 128F;
            poseStack.scale(scale, scale, scale);
            CaveMapRenderer.getMapFor(itemstack, false).render(poseStack, bufferSource, itemstack, true, packedLight);
            poseStack.popPose();
        }
    }
    *///?} elif >=1.21.2 {
    /*@Shadow
    public abstract Vec3 getRenderOffset(net.minecraft.client.renderer.entity.state.ItemFrameRenderState state);

    @Inject(
            method = {"Lnet/minecraft/client/renderer/entity/ItemFrameRenderer;render(Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
            remap = true,
            cancellable = true,
            at = @At(
                    value = "INVOKE",
                    shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            )
    )
    private void ac_renderArmWithItem(net.minecraft.client.renderer.entity.state.ItemFrameRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        ItemStack itemstack = ac_framedStack(state);
        if (itemstack.is(ACItemRegistry.CAVE_MAP.get()) && CaveMapItem.isFilled(itemstack)) {
            ci.cancel();
            poseStack.pushPose();
            Direction direction = state.direction;
            Vec3 vec3 = this.getRenderOffset(state);
            poseStack.translate(-vec3.x(), -vec3.y(), -vec3.z());
            poseStack.translate((double) direction.getStepX() * 0.46875D, (double) direction.getStepY() * 0.46875D, (double) direction.getStepZ() * 0.46875D);
            // The state carries no xRot/yRot, but an item frame's are a pure function of its facing
            // — this is vanilla's own reconstruction, and it agrees with the entity on every value.
            float xRot;
            float yRot;
            if (direction.getAxis().isHorizontal()) {
                xRot = 0.0F;
                yRot = 180.0F - direction.toYRot();
            } else {
                xRot = (float) (-90 * direction.getAxisDirection().getStep());
                yRot = 180.0F;
            }
            poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
            if (!state.isInvisible) {
                ModelManager modelmanager = this.blockRenderer.getBlockModelShaper().getModelManager();
                ModelResourceLocation modelresourcelocation = state.isGlowFrame ? GLOW_MAP_FRAME_LOCATION : MAP_FRAME_LOCATION;
                poseStack.pushPose();
                poseStack.translate(-0.5F, -0.5F, -0.5F);
                this.blockRenderer.getModelRenderer().renderModel(poseStack.last(), bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.entitySolidZOffsetForward(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)), (BlockState) null, modelmanager.getModel(modelresourcelocation), 1.0F, 1.0F, 1.0F, packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }
            int j = state.rotation % 4 * 2;
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) j * 360.0F / 8.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            float scale = 1F / 128F;
            poseStack.scale(scale, scale, scale);
            CaveMapRenderer.getMapFor(itemstack, false).render(poseStack, bufferSource, itemstack, true, packedLight);
            poseStack.popPose();
        }
    }
    *///?} else {

    @Shadow
    protected abstract void renderNameTag(ItemFrame entity, Component tag, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight);

    @Shadow
    protected abstract boolean shouldShowName(ItemFrame entity);

    @Shadow
    public abstract Vec3 getRenderOffset(ItemFrame entity, float partialTicks);

    @Inject(
            method = {"Lnet/minecraft/client/renderer/entity/ItemFrameRenderer;render(Lnet/minecraft/world/entity/decoration/ItemFrame;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
            remap = true,
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private void ac_renderArmWithItem(ItemFrame entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        ItemStack itemstack = entity.getItem();
        if (itemstack.is(ACItemRegistry.CAVE_MAP.get()) && CaveMapItem.isFilled(itemstack)) {
            ci.cancel();
            var renderNameTagEvent = new net.minecraftforge.client.event.RenderNameTagEvent(entity, entity.getDisplayName(), (ItemFrameRenderer) (Object) this, poseStack, bufferSource, packedLight, partialTicks);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(renderNameTagEvent);
            if (com.github.alexmodguy.alexscaves.client.ACClientCompat.shouldRenderNameTag(renderNameTagEvent, shouldShowName(entity))) {
                renderNameTag(entity, renderNameTagEvent.getContent(), poseStack, bufferSource, packedLight);
            }
            poseStack.pushPose();
            Direction direction = entity.getDirection();
            Vec3 vec3 = this.getRenderOffset(entity, partialTicks);
            poseStack.translate(-vec3.x(), -vec3.y(), -vec3.z());
            poseStack.translate((double) direction.getStepX() * 0.46875D, (double) direction.getStepY() * 0.46875D, (double) direction.getStepZ() * 0.46875D);
            poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
            if (!entity.isInvisible()) {
                ModelManager modelmanager = this.blockRenderer.getBlockModelShaper().getModelManager();
                ModelResourceLocation modelresourcelocation = entity.getType() == EntityType.GLOW_ITEM_FRAME ? GLOW_MAP_FRAME_LOCATION : MAP_FRAME_LOCATION;
                poseStack.pushPose();
                poseStack.translate(-0.5F, -0.5F, -0.5F);
                this.blockRenderer.getModelRenderer().renderModel(poseStack.last(), bufferSource.getBuffer(Sheets.solidBlockSheet()), (BlockState) null, modelmanager.getModel(modelresourcelocation), 1.0F, 1.0F, 1.0F, packedLight, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }
            int j = entity.getRotation() % 4 * 2;
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) j * 360.0F / 8.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            float scale = 1F / 128F;
            poseStack.scale(scale, scale, scale);
            CaveMapRenderer.getMapFor(itemstack, false).render(poseStack, bufferSource, itemstack, true, packedLight);
            poseStack.popPose();
        }
    }
    //?}

    // Where the framed item comes from. Through 1.21.3 the render state simply carried the stack;
    // 1.21.4 replaced that field with a resolved ItemStackRenderState, which is enough to draw the
    // item but says nothing about which item it is — and this mixin has to recognise a filled cave
    // map. The stack is therefore captured as the state is extracted, keyed by the state itself so a
    // recycled instance always answers with its current contents and a dropped one is collected.
    // From 26 the same hook does double duty. Vanilla decides which of the two frame models to
    // resolve from whether the framed item is a VANILLA filled map, which a cave map is not — so
    // without this the map would be drawn inside a bordered frame. Re-resolving with hasMap=true is
    // exactly the call vanilla makes, so nothing else about the state changes.
    //? if >=26 {
    /*@org.spongepowered.asm.mixin.Unique
    private static final java.util.Map<net.minecraft.client.renderer.entity.state.ItemFrameRenderState, ItemStack> ac_framedStacks =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    @Inject(
            method = {"Lnet/minecraft/client/renderer/entity/ItemFrameRenderer;extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V"},
            remap = true,
            at = @At(value = "TAIL")
    )
    private void ac_captureFramedStack(ItemFrame frame, net.minecraft.client.renderer.entity.state.ItemFrameRenderState state, float partialTicks, CallbackInfo ci) {
        ItemStack framed = frame.getItem();
        ac_framedStacks.put(state, framed);
        if (!state.isInvisible && framed.getItem() == ACItemRegistry.CAVE_MAP.get() && CaveMapItem.isFilled(framed)) {
            this.blockModelResolver.updateForItemFrame(state.frameModel, state.isGlowFrame, true);
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private static ItemStack ac_framedStack(net.minecraft.client.renderer.entity.state.ItemFrameRenderState state) {
        return ac_framedStacks.getOrDefault(state, ItemStack.EMPTY);
    }
    *///?} elif >=1.21.4 {
    /*@org.spongepowered.asm.mixin.Unique
    private static final java.util.Map<net.minecraft.client.renderer.entity.state.ItemFrameRenderState, ItemStack> ac_framedStacks =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    @Inject(
            method = {"Lnet/minecraft/client/renderer/entity/ItemFrameRenderer;extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V"},
            remap = true,
            at = @At(value = "TAIL")
    )
    private void ac_captureFramedStack(ItemFrame frame, net.minecraft.client.renderer.entity.state.ItemFrameRenderState state, float partialTicks, CallbackInfo ci) {
        ac_framedStacks.put(state, frame.getItem());
    }

    @org.spongepowered.asm.mixin.Unique
    private static ItemStack ac_framedStack(net.minecraft.client.renderer.entity.state.ItemFrameRenderState state) {
        return ac_framedStacks.getOrDefault(state, ItemStack.EMPTY);
    }
    *///?} elif >=1.21.2 {
    /*@org.spongepowered.asm.mixin.Unique
    private static ItemStack ac_framedStack(net.minecraft.client.renderer.entity.state.ItemFrameRenderState state) {
        return state.itemStack;
    }
    *///?}
}
