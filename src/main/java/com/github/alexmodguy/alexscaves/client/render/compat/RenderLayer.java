package com.github.alexmodguy.alexscaves.client.render.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;

/**
 * Pre-1.21.2 {@code RenderLayer<T, M>} — first parameter is the entity, as it used to be.
 *
 * <p>Vanilla's layer now receives only {@code (poseStack, bufferSource, packedLight, state, yRot,
 * xRot)}; the old ten-argument form is reconstructed here from {@link ACRenderState}. The values
 * are the same ones {@link LivingEntityRenderer} passes to the model, so a layer sees exactly what
 * it saw before.
 *
 * <p>{@code T} is bounded by {@code Entity}, matching the pre-1.21.2 vanilla bound — the layers in
 * this mod that are used raw declare their entity parameter as {@code Entity}, which only overrides
 * if the erasure agrees.
 *
 * <p>A layer attached to a <em>vanilla</em> renderer through {@code EntityRenderersEvent.AddLayers}
 * must extend {@link StateRenderLayer} instead; see there.
 */
public abstract class RenderLayer<T extends Entity, M extends EntityModel<?>>
		extends net.minecraft.client.renderer.entity.layers.RenderLayer<ACRenderState, M> {

	/** The state of the render currently in flight, for subclasses that reach the vanilla statics. */
	protected ACRenderState renderingState;

	/** Vanilla keeps its own copy private, and the pre-1.21.2 texture hook below needs it. */
	private final RenderLayerParent<ACRenderState, M> parent;

	public RenderLayer(RenderLayerParent<ACRenderState, M> parent) {
		super(parent);
		this.parent = parent;
	}

	/**
	 * Pre-1.21.2 {@code RenderLayer#getTextureLocation(T)}. 1.21.2 dropped it from the layer
	 * entirely — only {@code LivingEntityRenderer} still has a texture hook, and it is keyed on the
	 * render state — so it is reconstructed here from the parent renderer and the state in flight.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected net.minecraft.resources.ResourceLocation getTextureLocation(T entity) {
		if (this.parent instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer living
				&& this.renderingState != null) {
			return (net.minecraft.resources.ResourceLocation) living.getTextureLocation(this.renderingState);
		}
		return null;
	}

	// From 1.21.9 the vanilla entry point is submit(…, SubmitNodeCollector, …) rather than
	// render(…, MultiBufferSource, …), so the two spellings are separate gated overrides that both
	// funnel into dispatch. The submit arm wraps the collector in its own ACSubmitBuffers and
	// flushes it, rather than relying on the parent renderer's — a layer of this mod can be attached
	// to a *vanilla* renderer through EntityRenderersEvent.AddLayers, in which case there is no
	// compat renderer above it.
	//? if >=1.21.9 {
	/*@Override
	public final void submit(PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, int packedLight, ACRenderState state, float yRot, float xRot) {
		ACSubmitBuffers buffers = new ACSubmitBuffers(collector);
		this.dispatch(poseStack, buffers, packedLight, state, yRot, xRot);
		buffers.flush();
	}
	*///?} else {
	@Override
	public final void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, ACRenderState state, float yRot, float xRot) {
		this.dispatch(poseStack, bufferSource, packedLight, state, yRot, xRot);
	}
	//?}

	final void dispatch(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, ACRenderState state, float yRot, float xRot) {
		this.renderingState = state;
		@SuppressWarnings("unchecked")
		T entity = (T) state.entity;
		this.render(poseStack, bufferSource, packedLight, entity,
				state.walkAnimationPos, state.walkAnimationSpeed, state.partialTick,
				state.ageInTicks, yRot, xRot);
	}

	/**
	 * Pre-1.21.2 {@code renderColoredCutoutModel(model, tex, …, entity, r, g, b)}.
	 *
	 * <p>Deliberately <em>not</em> delegated to the vanilla static of that name: the static calls
	 * {@code Model#renderToBuffer}, which 1.21.2 made {@code final}, so it walks the empty root
	 * {@link EntityModel} hands vanilla and draws nothing. Reproducing the two-line body against the
	 * compat model's own eight-float {@code renderToBuffer} is what actually draws.
	 */
	protected final void renderColoredModel(EntityModel<?> model, net.minecraft.resources.ResourceLocation texture,
			PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int color) {
		VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
		int overlay = net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(this.renderingState, 0.0F);
		model.renderToBuffer(poseStack, consumer, packedLight, overlay,
				ARGB.red(color) / 255.0F, ARGB.green(color) / 255.0F, ARGB.blue(color) / 255.0F, ARGB.alpha(color) / 255.0F);
	}

	public abstract void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, T entity,
			float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch);
}
