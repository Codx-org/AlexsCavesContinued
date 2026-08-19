package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.client.ACLevelRenderStage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The chunk-layer half of {@link ACLevelRenderStage}, from 1.21.6 up. Compiled on those nodes only —
 * {@code ChunkSectionsToRender} does not exist below them, so the file is excluded from the source set
 * and the entry dropped from the mixin config rather than being gated inside.
 *
 * <p>Up to 1.21.5 the anchor was {@code LevelRenderer#renderSectionLayer}, once per layer, and lives in
 * {@code LevelRenderStageMixin} with the other two. 1.21.6 replaced it with
 * {@code ChunkSectionsToRender#renderGroup}, called three times for the three
 * {@code ChunkSectionLayerGroup}s. That is a better target than the call sites: one method, the same
 * descriptor on both loaders, and the group arrives as an argument, so nothing depends on the ordinal
 * of a call inside a frame-graph lambda whose synthetic name differs per loader.
 *
 * <p>{@code RETURN} rather than an {@code INVOKE} shift because the group's {@code RenderPass} is a
 * try-with-resources: it is still open at every point inside the method, and an immediate draw needs it
 * closed. It is closed by the time the method returns — which is exactly where NeoForge posts its own
 * events from.
 */
@Mixin(ChunkSectionsToRender.class)
public class ChunkSectionsToRenderMixin {

    // 1.21.11 hands renderGroup the sampler the group's textures are bound with. An @Inject handler
    // mirrors its target's whole argument list, so the extra parameter has to be declared even
    // though nothing here reads it; only the signature moves.
    @Inject(method = "renderGroup", at = @At("RETURN"))
    //? if >=1.21.11 {
    /*private void ac_afterLayerGroup(ChunkSectionLayerGroup group, com.mojang.blaze3d.textures.GpuSampler sampler, CallbackInfo ci) {
    *///?} else {
    private void ac_afterLayerGroup(ChunkSectionLayerGroup group, CallbackInfo ci) {
    //?}
        ACLevelRenderStage stage = ACLevelRenderStage.ofLayerGroup(group);
        if (stage == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        ACLevelRenderStage.dispatch(stage, minecraft.levelRenderer, new PoseStack(), com.github.alexmodguy.alexscaves.client.ACClientCompat.levelRendererTicks(minecraft.levelRenderer),
                minecraft.gameRenderer.getMainCamera(), minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }
}
