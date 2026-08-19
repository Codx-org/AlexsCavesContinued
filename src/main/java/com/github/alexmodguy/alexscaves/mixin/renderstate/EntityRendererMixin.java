package com.github.alexmodguy.alexscaves.mixin.renderstate;

import com.github.alexmodguy.alexscaves.client.render.compat.ACStateAccess;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records the entity a render state was extracted from — see {@link ACStateAccess}.
 *
 * <p>{@code EntityRenderer#extractRenderState} is the single choke point: every subclass override
 * calls up to it, and it is reached exactly once per entity per frame from
 * {@code EntityRenderer#createRenderState(Entity, float)}.
 *
 * <p>⚠️ <b>HEAD, not TAIL.</b> {@code extractRenderState} calls {@code extractNameTags} partway
 * through its own body, and that is where the loader posts {@code RenderNameTagEvent.CanRender} —
 * so a TAIL capture happens <i>after</i> the one listener that needs it. Nothing reads the duck
 * before extraction finishes, so capturing at HEAD is strictly earlier and safe for every caller.
 */
// ⚠️ The target is spelled out FULLY QUALIFIED and this file must NEVER
// `import net.minecraft.client.renderer.entity.EntityRenderer;` — the `!mc2102-render-import-entity`
// replacement in stonecutter.gradle.kts rewrites exactly that statement to
// client.render.compat.EntityRenderer on every >=1.21.2 node. It does not know this is a mixin, so the
// import would retarget @Mixin at the mod's own compat class, whose extractRenderState takes an
// ACRenderState — a descriptor mismatch, i.e. a hard mixin-apply crash on every >=1.21.2 node.
// It compiles clean either way: @Mixin accepts any class and a handler's parameters are only checked at
// apply time. The same trap waits for LivingEntityRenderer, MobRenderer, RenderLayer and EntityModel.
@Mixin(net.minecraft.client.renderer.entity.EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void alexscaves$captureEntity(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        ((ACStateAccess) state).alexscaves$capture(entity, partialTick);
    }
}
