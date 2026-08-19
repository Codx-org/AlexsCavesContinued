package com.github.alexmodguy.alexscaves.client.render.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * A render layer that can be attached to <em>any</em> renderer, this mod's or vanilla's.
 *
 * <p>{@link RenderLayer} next door cannot: it is bound to {@link ACRenderState}, so the bridge
 * method the compiler generates casts whatever state vanilla hands it to that class. Attaching one
 * to a vanilla renderer therefore throws a {@code ClassCastException} on the first frame — which is
 * exactly what this mod's potion-effect layer needs to do, since it is registered against every
 * living entity type in the game through {@code EntityRenderersEvent.AddLayers}.
 *
 * <p>So this base is typed on the vanilla {@link EntityRenderState} and is deliberately <em>raw</em>
 * with respect to its supertype: the erasure of vanilla's {@code render} is what the arm below
 * declares, which is what makes it an override. The entity behind the state comes back through
 * {@link ACStateAccess}, which {@code mixin.renderstate.EntityRendererMixin} stamps onto every
 * state, vanilla ones included.
 *
 * <p>⚠️ The supertype is spelled fully qualified and must <strong>never</strong> be imported: the
 * {@code !mc2102-render-import-layer} rule rewrites that exact import statement to {@link
 * RenderLayer} on every >=1.21.2 node.
 */
@SuppressWarnings("rawtypes")
public abstract class StateRenderLayer extends net.minecraft.client.renderer.entity.layers.RenderLayer {

	/** Vanilla keeps its own copy private, and the texture hook below needs it. */
	private final net.minecraft.client.renderer.entity.RenderLayerParent parent;

	/** The state of the render currently in flight. */
	protected EntityRenderState renderingState;

	@SuppressWarnings("unchecked")
	protected StateRenderLayer(net.minecraft.client.renderer.entity.RenderLayerParent parent) {
		super(parent);
		this.parent = parent;
	}

	// From 1.21.9 the vanilla entry point is submit(…, SubmitNodeCollector, …). The split lives here
	// rather than in the subclasses because Stonecutter blocks are siblings and never nest.
	//? if >=1.21.9 {
	/*@Override
	public final void submit(PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, int packedLight, EntityRenderState state, float yRot, float xRot) {
		this.renderingState = state;
		ACSubmitBuffers buffers = new ACSubmitBuffers(collector);
		this.draw(poseStack, buffers, packedLight, state, yRot, xRot);
		buffers.flush();
	}
	*///?} else {
	@Override
	public final void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, EntityRenderState state, float yRot, float xRot) {
		this.renderingState = state;
		this.draw(poseStack, bufferSource, packedLight, state, yRot, xRot);
	}
	//?}

	/**
	 * The default unpacks the state back into the pre-1.21.2 argument list, so a subclass that
	 * predates the rewrite keeps its old ten-argument {@code render} and nothing else changes.
	 * Subclasses that want the state itself override this instead.
	 */
	protected void draw(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
			EntityRenderState state, float yRot, float xRot) {
		net.minecraft.world.entity.Entity entity = ACStateAccess.entity(state);
		if (entity == null) {
			return;
		}
		// The walk animation lives on LivingEntityRenderState, not on the base state this layer is
		// typed on — it can be attached to a non-living renderer, which simply has no gait.
		float walkPos = 0.0F;
		float walkSpeed = 0.0F;
		if (state instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState living) {
			walkPos = living.walkAnimationPos;
			walkSpeed = living.walkAnimationSpeed;
		}
		this.render(poseStack, bufferSource, packedLight, entity,
				walkPos, walkSpeed, ACStateAccess.partialTick(state),
				state.ageInTicks, yRot, xRot);
	}

	/** Pre-1.21.2 {@code RenderLayer#render}. */
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, net.minecraft.world.entity.Entity entity,
			float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
	}

	/**
	 * Pre-1.21.2 {@code RenderLayer#getTextureLocation(T)} — see the note on the sibling
	 * {@link RenderLayer#getTextureLocation}. Here the parent is a <em>vanilla</em> renderer, so the
	 * lookup goes through its state-taking hook with the state in flight.
	 */
	@SuppressWarnings("unchecked")
	protected net.minecraft.resources.ResourceLocation getTextureLocation(net.minecraft.world.entity.Entity entity) {
		if (this.parent instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer living
				&& this.renderingState instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState livingState) {
			return (net.minecraft.resources.ResourceLocation) living.getTextureLocation(livingState);
		}
		return null;
	}
}
