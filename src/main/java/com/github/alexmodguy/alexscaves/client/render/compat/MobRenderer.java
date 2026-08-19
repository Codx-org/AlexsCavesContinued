package com.github.alexmodguy.alexscaves.client.render.compat;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Mob;

/**
 * Pre-1.21.2 {@code MobRenderer<T, M>} — two type parameters, on top of
 * {@link LivingEntityRenderer}. Most of this mod's entity renderers extend it.
 */
public abstract class MobRenderer<T extends Mob, M extends EntityModel<?>>
		extends LivingEntityRenderer<T, M> {

	public MobRenderer(EntityRendererProvider.Context context, M model, float shadowRadius) {
		super(context, model, shadowRadius);
	}

	@Override
	protected float getShadowRadius(ACRenderState state) {
		return super.getShadowRadius(state) * state.ageScale;
	}

	/**
	 * Vanilla {@code MobRenderer}'s nameplate rule, which this shim would otherwise drop.
	 *
	 * <p>⚠️ This class extends {@link LivingEntityRenderer} — i.e. vanilla
	 * {@code LivingEntityRenderer}, <b>not</b> vanilla {@code MobRenderer} — because the shim exists
	 * to restore the two-type-parameter shape, and the three-parameter {@code MobRenderer} is not on
	 * that path. But "show a name only if the mob actually has one" lives <i>only</i> in
	 * {@code MobRenderer#shouldShowName}: the {@code LivingEntityRenderer} one it calls up to answers
	 * a different question (team visibility, invisibility, is-it-the-camera) and returns {@code true}
	 * for any ordinary visible mob. Inheriting from the wrong side would therefore give every mob in
	 * this mod a permanent floating type name on every node from 1.21.2 up — a fault the sibling
	 * AlexsMobsContinued tree shipped once before tracking it down.
	 */
	@Override
	protected boolean shouldShowName(T entity, double distanceToCameraSq) {
		return super.shouldShowName(entity, distanceToCameraSq)
				&& (entity.shouldShowName()
						|| entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity);
	}
}
