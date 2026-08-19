package com.github.alexmodguy.alexscaves.citadel.client.render.pathfinding;

import com.github.alexmodguy.alexscaves.client.ACLevelRenderStage;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class WorldEventContext {
    public static final WorldEventContext INSTANCE = new WorldEventContext();

    private WorldEventContext() {
        // singleton
    }

    public MultiBufferSource.BufferSource bufferSource;
    public PoseStack poseStack;
    public float partialTicks;
    public ClientLevel clientLevel;
    public LocalPlayer clientPlayer;
    public ItemStack mainHandItem;


    /**
     * In chunks
     */
    int clientRenderDist;

    /**
     * Draws the pathfinding debug overlay for one render stage.
     *
     * <p>Was {@code renderWorldLastEvent(RenderLevelStageEvent)}. It takes the stage as this mod's
     * own {@link ACLevelRenderStage} now because Forge deleted that event in 1.21.2 and Fabric
     * never had it — see {@link ACLevelRenderStage} for who calls this on which node.
     */
    public void renderStage(final ACLevelRenderStage stage, final PoseStack stackIn, final float partialTicksIn) {
        bufferSource = WorldRenderMacros.getBufferSource();
        poseStack = stackIn;
        partialTicks = partialTicksIn;
        clientLevel = Minecraft.getInstance().level;
        clientPlayer = Minecraft.getInstance().player;
        mainHandItem = clientPlayer.getMainHandItem();
        clientRenderDist = Minecraft.getInstance().options.renderDistance().get();

        // Two statements, not one chain: see ClientProxy#isFarFromCamera for why the camera has to
        // be pulled into a local before its position is read.
        final net.minecraft.client.Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        final Vec3 cameraPos = camera.getPosition();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());

        if (stage == ACLevelRenderStage.AFTER_CUTOUT_MIPPED_BLOCKS) {
            PathfindingDebugRenderer.render(this);

            bufferSource.endBatch();
        } else if (stage == ACLevelRenderStage.AFTER_TRIPWIRE_BLOCKS) {
            bufferSource.endBatch();
        }

        poseStack.popPose();
    }

}
