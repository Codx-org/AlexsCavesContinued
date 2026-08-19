package com.github.alexmodguy.alexscaves.client.render.compat;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

/**
 * The entity behind a render state.
 *
 * <p>1.21.2 made {@code EntityRenderState} a pure data object: the renderer extracts what it needs
 * from the entity in one pass and may only read the state in the next. The loaders' render events
 * followed it, so they now hand out a state where they used to hand out the entity — but the hooks
 * in this mod genuinely need the entity (the possession layer's data, the darkness/irradiation
 * effects, the entity's own random source), and none of that is on any vanilla state.
 *
 * <p>{@code mixin.renderstate.EntityRendererMixin} therefore stashes the entity and the frame's
 * partial tick on every state it extracts, and this interface reads them back. It is a duck typed
 * onto {@code EntityRenderState} itself, so it works for vanilla entities too — the layers this mod
 * attaches through {@code EntityRenderersEvent.AddLayers} apply to players and vanilla mobs, not
 * only to this mod's own.
 *
 * <p>Valid only between extraction and rendering of the same frame, which is the whole window in
 * which anything here is asked for.
 */
public interface ACStateAccess {

	void alexscaves$capture(Entity entity, float partialTick);

	Entity alexscaves$entity();

	float alexscaves$partialTick();

	static Entity entity(EntityRenderState state) {
		return ((ACStateAccess) state).alexscaves$entity();
	}

	static float partialTick(EntityRenderState state) {
		return ((ACStateAccess) state).alexscaves$partialTick();
	}
}
