package com.github.alexmodguy.alexscaves.fabric.forge.client.event;

import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.EntityEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/**
 * Fabric stand-in for the loader's "should this entity's name tag be drawn, and with what text"
 * event.
 *
 * <p>The mod only ever <b>publishes</b> this one. Two of its renderers draw a name tag through a
 * path vanilla does not own — {@code WatcherRenderer}, which renders the entity it is impersonating,
 * and {@code ItemFrameRendererMixin}, which restores the tag the item-frame renderer skips — and
 * both post it so other mods still get their say. Nothing in this tree subscribes to it, so the
 * dispatcher has nothing to fire: {@code ACEventBus} carrying it to third-party listeners is the
 * whole job.
 *
 * <p>The verdict is the inherited tri-state {@code getResult()}, which {@code ACClientCompat
 * #shouldRenderNameTag} folds against vanilla's own answer — this is one of the two reasons
 * {@code Result} is kept in the stubbed bus at all.
 *
 * <p>The renderer parameter is the raw vanilla type and is deliberately never imported; see
 * {@link RenderLivingEvent} for both reasons.
 */
public class RenderNameTagEvent extends EntityEvent {

    private final net.minecraft.client.renderer.entity.EntityRenderer renderer;
    private final PoseStack poseStack;
    private final MultiBufferSource bufferSource;
    private final int packedLight;
    private final float partialTick;
    private Component content;

    public RenderNameTagEvent(Entity entity, Component content, net.minecraft.client.renderer.entity.EntityRenderer renderer,
                              PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTick) {
        super(entity);
        this.content = content;
        this.renderer = renderer;
        this.poseStack = poseStack;
        this.bufferSource = bufferSource;
        this.packedLight = packedLight;
        this.partialTick = partialTick;
    }

    /** The text to draw. A listener may replace it, and both publishers read it back. */
    public Component getContent() {
        return content;
    }

    public void setContent(Component content) {
        this.content = content;
    }

    public net.minecraft.client.renderer.entity.EntityRenderer getEntityRenderer() {
        return renderer;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public MultiBufferSource getMultiBufferSource() {
        return bufferSource;
    }

    public int getPackedLight() {
        return packedLight;
    }

    public float getPartialTick() {
        return partialTick;
    }
}
