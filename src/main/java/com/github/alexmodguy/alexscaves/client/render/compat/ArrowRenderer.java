package com.github.alexmodguy.alexscaves.client.render.compat;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractArrow;

/**
 * Pre-1.21.2 {@code ArrowRenderer<T>} — one type parameter, and a texture hook keyed on the entity.
 *
 * <p>Unlike the other shims in this package this one keeps <em>vanilla's</em> drawing: an arrow is
 * a vanilla model posed from a plain {@link ArrowRenderState}, and this mod's arrow renderers only
 * override the texture and the block light. So the state type stays vanilla's and only the two
 * hooks are bridged back to the entity form, through the entity {@link ACStateAccess} stamps onto
 * every state.
 *
 * <p>⚠️ Reached by the {@code !mc2102-render-import-arrow} rule swapping the import — the same
 * mechanism as the renderers next door.
 */
public abstract class ArrowRenderer<T extends AbstractArrow>
		extends net.minecraft.client.renderer.entity.ArrowRenderer<T, ArrowRenderState> {

	public ArrowRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ArrowRenderState createRenderState() {
		return new ArrowRenderState();
	}

	@Override
	protected final ResourceLocation getTextureLocation(ArrowRenderState state) {
		@SuppressWarnings("unchecked")
		T entity = (T) ACStateAccess.entity(state);
		return this.getTextureLocation(entity);
	}

	/** Re-declared in the pre-1.21.2 shape; the subclasses in this mod implement this one. */
	public abstract ResourceLocation getTextureLocation(T entity);
}
