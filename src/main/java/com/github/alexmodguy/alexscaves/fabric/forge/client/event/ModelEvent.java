package com.github.alexmodguy.alexscaves.fabric.forge.client.event;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;

/**
 * Fabric stand-in for the model-baking phases.
 *
 * <p>Only {@code ModifyBakingResult} is modelled. Its sibling that hands out baked fluid models is
 * used from a {@code forge && >=26} arm alone, so on this loader it would be a class nothing names.
 *
 * <p>Every type below is fully qualified and nothing is imported: the map's key type changes at 1.21
 * and its value type is deleted at 1.21.4, so an import would fail to resolve on precisely the nodes
 * whose arm does not use it.
 */
public class ModelEvent extends Event {

    /**
     * The post-bake model map, handed out mutable so a mod can wrap an entry.
     *
     * <p>This mod wraps every model whose id starts with one of a short list of prefixes, to draw its
     * second texture layer fullbright. The three shapes of this class are vanilla's own: a plain
     * resource location keys the map up to 1.20.6, a model resource location from 1.21, and from
     * 1.21.4 there is no post-bake map at all — the whole feature is inert there, on every loader,
     * and the class stays declared only so the enclosing name still resolves.
     */
    public static class ModifyBakingResult extends ModelEvent {

        //? if <1.21 {
        private final java.util.Map<net.minecraft.resources.ResourceLocation, net.minecraft.client.resources.model.BakedModel> models;

        public ModifyBakingResult(java.util.Map<net.minecraft.resources.ResourceLocation, net.minecraft.client.resources.model.BakedModel> models) {
            this.models = models;
        }

        public java.util.Map<net.minecraft.resources.ResourceLocation, net.minecraft.client.resources.model.BakedModel> getModels() {
            return models;
        }
        //?} elif <1.21.4 {
        /*private final java.util.Map<net.minecraft.client.resources.model.ModelResourceLocation, net.minecraft.client.resources.model.BakedModel> models;

        public ModifyBakingResult(java.util.Map<net.minecraft.client.resources.model.ModelResourceLocation, net.minecraft.client.resources.model.BakedModel> models) {
            this.models = models;
        }

        public java.util.Map<net.minecraft.client.resources.model.ModelResourceLocation, net.minecraft.client.resources.model.BakedModel> getModels() {
            return models;
        }
        *///?}
    }
}
