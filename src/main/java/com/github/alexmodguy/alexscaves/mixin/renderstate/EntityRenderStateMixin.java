package com.github.alexmodguy.alexscaves.mixin.renderstate;

import com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Storage for {@link ACStateAccess} — see there for why this exists.
 *
 * <p>This package is compiled and added to the mixin config only on 1.21.2 and up, where
 * {@code EntityRenderState} exists; below that the renderers still receive the entity directly.
 */
@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements ACStateAccess {

    @Unique
    private Entity alexscaves$entity;

    @Unique
    private float alexscaves$partialTick;

    @Override
    public void alexscaves$capture(Entity entity, float partialTick) {
        this.alexscaves$entity = entity;
        this.alexscaves$partialTick = partialTick;
    }

    @Override
    public Entity alexscaves$entity() {
        return this.alexscaves$entity;
    }

    @Override
    public float alexscaves$partialTick() {
        return this.alexscaves$partialTick;
    }
}
