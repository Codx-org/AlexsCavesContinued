package com.github.alexmodguy.alexscaves.mixin.fabric.client;

import com.github.alexmodguy.alexscaves.fabric.ModBus;
import com.github.alexmodguy.alexscaves.fabric.forge.client.event.ModelEvent;
import net.minecraft.client.resources.model.ModelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fabric's stand-in for the point at which Forge fires {@code ModelEvent.ModifyBakingResult}.
 *
 * <p>Forge patches {@code ModelManager.loadModels} with a {@code forge_modify_baking_result}
 * profiler section wedged between the {@code baking} section and the {@code dispatch} one, and
 * fires the event there. That position is the whole point of the hook rather than a detail: the
 * {@code dispatch} section immediately afterwards walks every block state and builds the
 * {@code IdentityHashMap} the {@code BlockModelShaper} then renders out of, so a model wrapped
 * <em>after</em> it would be modified in the top-level map and ignored by every block on screen.
 * Fabric API has nothing equivalent on this version — its model-loading plugin API arrives much
 * later, and even then is a per-model modifier rather than the whole map this mod iterates.
 *
 * <p>Vanilla calls {@code getBakedTopLevelModels()} exactly once inside {@code loadModels}, as the
 * first act of the {@code dispatch} section — i.e. at precisely the offset Forge's hook precedes —
 * so redirecting that one call hands the event the same map at the same moment. The map is a plain
 * {@code HashMap} built in {@code ModelBakery}'s constructor, so handing it out mutable is what
 * Forge does too and not a liberty taken here.
 *
 * <p><b>No arm from 1.21.4.</b> There is no post-bake map to modify from that version on any
 * loader — {@code ModelEvent.ModifyBakingResult} is a bodyless class there and
 * {@code ClientProxy} registers no listener — so this mixin is deliberately empty rather than
 * excluded, exactly like the other targets that come and go across the range. The {@code <1.21.4}
 * arm below is written from vanilla 1.20.1's bytecode; if either the enclosing method or the call
 * moved at 1.21, {@code scripts/verify_mixins.py} says so the moment that node is uncommented,
 * which is why no guess is hedged here.
 */
@Mixin(ModelManager.class)
public class ModelManagerMixin {

    //? if <1.21 {
    @Redirect(method = "loadModels", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/ModelBakery;getBakedTopLevelModels()Ljava/util/Map;"))
    private java.util.Map<net.minecraft.resources.ResourceLocation, net.minecraft.client.resources.model.BakedModel> ac_modifyBakingResult(net.minecraft.client.resources.model.ModelBakery bakery) {
        java.util.Map<net.minecraft.resources.ResourceLocation, net.minecraft.client.resources.model.BakedModel> models = bakery.getBakedTopLevelModels();
        ModBus.INSTANCE.post(new ModelEvent.ModifyBakingResult(models));
        return models;
    }
    //?} elif <1.21.4 {
    /*@Redirect(method = "loadModels", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/ModelBakery;getBakedTopLevelModels()Ljava/util/Map;"))
    private java.util.Map<net.minecraft.client.resources.model.ModelResourceLocation, net.minecraft.client.resources.model.BakedModel> ac_modifyBakingResult(net.minecraft.client.resources.model.ModelBakery bakery) {
        java.util.Map<net.minecraft.client.resources.model.ModelResourceLocation, net.minecraft.client.resources.model.BakedModel> models = bakery.getBakedTopLevelModels();
        ModBus.INSTANCE.post(new ModelEvent.ModifyBakingResult(models));
        return models;
    }
    *///?}
}
