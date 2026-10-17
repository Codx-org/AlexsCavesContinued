package com.github.alexmodguy.alexscaves.client.render.compat;

import com.github.alexmodguy.alexscaves.server.entity.util.CullingBoundsEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Pre-1.21.2 {@code EntityRenderer<T>} on top of the render-state architecture.
 *
 * <p>Subclasses keep their single type parameter, their {@code render(T, float, float, PoseStack,
 * MultiBufferSource, int)} override and their {@code getTextureLocation(T)}; this class does the
 * extract pass for them and dispatches.
 */
public abstract class EntityRenderer<T extends Entity>
		extends net.minecraft.client.renderer.entity.EntityRenderer<T, ACRenderState> {

	/**
	 * The state currently being rendered, so the legacy hooks can reach fields that used to be
	 * parameters. {@code EntityRenderer} reuses exactly one state instance per renderer and
	 * rendering is single-threaded, so this is simply the argument of the enclosing
	 * {@link #render(ACRenderState, PoseStack, MultiBufferSource, int)} call.
	 */
	protected ACRenderState renderingState;

	protected EntityRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ACRenderState createRenderState() {
		return new ACRenderState();
	}

	@Override
	public void extractRenderState(T entity, ACRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.entity = entity;
		state.partialTick = partialTick;
		state.entityYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
	}

	// 1.21.9 replaces this entry point with submit(state, pose, SubmitNodeCollector, CameraRenderState):
	// a renderer no longer writes vertices, it submits nodes that a later pass turns into them. The
	// legacy bodies below still want a MultiBufferSource, so the collector is wrapped in the recorder
	// shim next door and flushed once the body has finished drawing. See ACSubmitBuffers.
	//? if >=1.21.9 {
	/*@Override
	public final void submit(ACRenderState state, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.CameraRenderState camera) {
		ACSubmitBuffers buffers = new ACSubmitBuffers(collector, camera);
		this.dispatch(state, poseStack, buffers, state.lightCoords);
		buffers.flush();
	}
	*///?} else {
	@Override
	public final void render(ACRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		this.dispatch(state, poseStack, bufferSource, packedLight);
	}
	//?}

	private void dispatch(ACRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		this.renderingState = state;
		@SuppressWarnings("unchecked")
		T entity = (T) state.entity;
		this.render(entity, state.entityYaw, state.partialTick, poseStack, bufferSource, packedLight);
	}

	/**
	 * The pre-1.21.2 entry point. The default reproduces what the old {@code EntityRenderer#render}
	 * did — leash and name tag — which is all the modern one does too, so subclasses calling
	 * {@code super.render(...)} still get it.
	 */
	public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		//? if >=1.21.9 {
		/*ACSubmitBuffers buffers = ACSubmitBuffers.of(bufferSource);
		if (buffers != null) {
			buffers.flush();
			super.submit(this.renderingState, poseStack, buffers.collector(), buffers.camera());
		}
		*///?} else {
		super.render(this.renderingState, poseStack, bufferSource, packedLight);
		//?}
	}

	/**
	 * Re-declared because the modern {@code EntityRenderer} dropped it; the 29 subclasses in this
	 * mod still implement and call it.
	 */
	public abstract ResourceLocation getTextureLocation(T entity);

	// ---------------------------------------------------------------------------------------
	// Culling. 1.21.2 moved both questions off the entity and onto the renderer; the entities
	// still answer them, through CullingBoundsEntity. See there.
	// ---------------------------------------------------------------------------------------

	@Override
	protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(T entity) {
		return entity instanceof CullingBoundsEntity culled ? culled.getBoundingBoxForCulling() : super.getBoundingBoxForCulling(entity);
	}

	@Override
	protected boolean affectedByCulling(T entity) {
		return !(entity instanceof CullingBoundsEntity culled) || culled.isAffectedByCulling();
	}

	// ---------------------------------------------------------------------------------------
	// Legacy name-tag bridges. 1.21.2 made shouldShowName take a squared distance and renderNameTag
	// take the render state; the renderers that reimplement render(T, …) still call the old
	// entity-only forms. Route them to the modern ones using the state currently being rendered.
	// ---------------------------------------------------------------------------------------

	// `this`, not `super` — a legacy call must reach the most-derived two-arg override rather than
	// step over it.
	protected boolean shouldShowName(T entity) {
		return this.shouldShowName(entity, this.entityRenderDispatcher.distanceToSqr(entity));
	}

	protected void renderNameTag(T entity, net.minecraft.network.chat.Component name, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		// 1.21.9's submitNameTag reads the text off the render state, so `name` is unused from there
		// on — it carries the same value either way, since the state's copy is what the legacy call
		// sites were handed in the first place.
		//? if >=1.21.9 {
		/*ACSubmitBuffers buffers = ACSubmitBuffers.of(bufferSource);
		if (buffers != null) {
			super.submitNameTag(this.renderingState, poseStack, buffers.collector(), buffers.camera());
		}
		*///?} else {
		super.renderNameTag(this.renderingState, name, poseStack, bufferSource, packedLight);
		//?}
	}

	/**
	 * The 1.20.5–1.21.1 shape, which carried the partial tick. 1.21.2 dropped it again, but the
	 * {@code !mc205-nametag} replacement rule adds the argument on every node from 1.20.5 up, so the
	 * overload has to exist here for the call sites it rewrites.
	 */
	protected void renderNameTag(T entity, net.minecraft.network.chat.Component name, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTick) {
		this.renderNameTag(entity, name, poseStack, bufferSource, packedLight);
	}
}
