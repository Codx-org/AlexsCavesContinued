package com.github.alexmodguy.alexscaves.client.render.compat;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;

/**
 * The single render state used by every shimmed renderer in this package.
 *
 * <p>1.21.2 split entity rendering into an "extract" pass that copies what the renderer needs out
 * of the entity into a plain data object, and a "render" pass that may only read that object.
 * Alex's Caves has 72 renderers, 41 layers and ~90 models written against the pre-1.21.2 API,
 * where every hook received the live entity. Rather than migrate all of them, this state carries
 * the entity itself plus the handful of per-frame values the old hooks were handed as parameters,
 * and the shims in this package hand them back.
 *
 * <p>That is deliberately the thing vanilla stopped doing. It is safe here because extraction and
 * rendering happen back to back on the render thread within one frame, and because
 * {@code EntityRenderer} reuses exactly one state instance per renderer, so this adds one strong
 * reference per renderer rather than one per entity.
 *
 * <p>{@link #chromeOnly} is the one piece of real machinery — see
 * {@link LivingEntityRenderer#render}.
 */
public class ACRenderState extends LivingEntityRenderState {

	/** The entity being rendered. Valid only between extract and render of the same frame. */
	public Entity entity;

	/** Partial tick for this frame — the old {@code partialTicks} / {@code partialTickTime} arg. */
	public float partialTick;

	/**
	 * Interpolated {@code getYRot()}, i.e. the old {@code entityYaw} argument that the entity
	 * render dispatcher used to pass in. Note this is the entity yaw, not the body yaw — living
	 * renderers took the body rotation from {@link LivingEntityRenderState#bodyRot} instead.
	 */
	public float entityYaw;

	/** The old {@code EntityModel#attackTime}; {@link LivingEntityRenderState} has no equivalent. */
	public float attackTime;

	/** The old {@code EntityModel#riding}. */
	public boolean riding;

	/**
	 * Set while vanilla's {@code LivingEntityRenderer#render} body is being run purely to reach
	 * the leash and name-tag rendering in its {@code EntityRenderer} tail. While it is set the
	 * shim suppresses everything else, so nothing is drawn or animated twice.
	 */
	public boolean chromeOnly;
}
