package com.github.alexmodguy.alexscaves.client.render.compat;

// Pre-1.21.9 BlockEntityRenderer<T>, on top of 1.21.9's extract/submit architecture.
//
// The thirteen tile renderers in this mod keep their single type parameter and their
// render(T, float, PoseStack, MultiBufferSource, int, int, Vec3) body; the >=1.21.9
// `!mc219-tile-import` replacement points their `import …blockentity.BlockEntityRenderer;` at this
// interface instead of the vanilla one, exactly the way the 1.21.2 render-state rewrite was absorbed
// for entity renderers. Below 1.21.9 this file is just a package declaration and their import
// resolves to vanilla as before.
//
// NOTE the simple name deliberately matches vanilla's — that is what makes the import swap work, and
// it is also a trap: no mixin may `import` this name, and a mixin that has to name a vanilla
// block-entity renderer must spell it out fully.
//? if >=1.21.9 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public interface BlockEntityRenderer<T extends BlockEntity>
        extends net.minecraft.client.renderer.blockentity.BlockEntityRenderer<T, ACBlockEntityRenderState> {

    @Override
    default ACBlockEntityRenderState createRenderState() {
        return new ACBlockEntityRenderState();
    }

    @Override
    default void extractRenderState(T tile, ACBlockEntityRenderState state, float partialTick, Vec3 camPos, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        net.minecraft.client.renderer.blockentity.BlockEntityRenderer.super.extractRenderState(tile, state, partialTick, camPos, crumbling);
        state.tile = tile;
        state.partialTick = partialTick;
        state.camPos = camPos;
    }

    @Override
    default void submit(ACBlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        ACSubmitBuffers buffers = new ACSubmitBuffers(collector, camera);
        @SuppressWarnings("unchecked")
        T tile = (T) state.tile;
        // The old signature's packedOverlay was NO_OVERLAY at every vanilla call site, and the new
        // one does not carry one at all.
        this.render(tile, state.partialTick, poseStack, buffers, state.lightCoords, OverlayTexture.NO_OVERLAY, state.camPos);
        buffers.flush();
    }

    void render(T tile, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, Vec3 camPos);
}
*///?}
