package com.github.alexmodguy.alexscaves.fabric.forge.client.event;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.EntityType;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Fabric stand-in for the two renderer-registration phases this mod hooks.
 *
 * <p><b>{@code RegisterRenderers} is deliberately absent.</b> This tree never subscribes to it —
 * every entity and block-entity renderer is registered through {@code ClientProxy} directly — so
 * declaring it would be a class the dispatcher has to fire for nobody.
 *
 * <p>Both phases here are <b>received</b> only, and both are registration-time rather than
 * per-frame, so the dispatcher fires them once from the client initializer, in the same order the
 * mod bus uses: layer definitions before layers, because the second reads renderers the first has
 * to have finished building.
 *
 * <p>The renderer types are the raw vanilla ones and are never imported; see {@link
 * RenderLivingEvent} for why. {@code ClientLayerRegistry} names them the same way and for the same
 * reason.
 */
public class EntityRenderersEvent extends Event {

    /** Where this mod's six armour and held-item model layers are declared. */
    public static class RegisterLayerDefinitions extends EntityRenderersEvent {

        private final java.util.Map<ModelLayerLocation, Supplier<LayerDefinition>> definitions;

        public RegisterLayerDefinitions(java.util.Map<ModelLayerLocation, Supplier<LayerDefinition>> definitions) {
            this.definitions = definitions;
        }

        public void registerLayerDefinition(ModelLayerLocation layer, Supplier<LayerDefinition> definition) {
            definitions.put(layer, definition);
        }
    }

    /**
     * Where the potion-effect glow layer is added to every living renderer that will take one.
     *
     * <p>The skin key is the one member with a version split, and the split is vanilla's own: a
     * free-form string up to 1.20.1, the nested player-skin model enum from 1.20.2, and the
     * top-level {@code PlayerModelType} from 1.21.9. {@code ClientLayerRegistry} and {@code
     * mixin.fabric.client.EntityRenderDispatcherMixin} carry the matching arms.
     *
     * <p>Keeping the key gated rather than widening it to {@code Object} is deliberate: a node that
     * needs a fourth shape should fail to compile, not silently hand the dispatcher a key it cannot
     * resolve. The method NAMES stay {@code getSkins}/{@code getSkin} on every arm — they are this
     * stub's own, not vanilla's, so nothing is gained by tracking the loaders' renamings of them.
     */
    public static class AddLayers extends EntityRenderersEvent {

        //? if >=1.21.9 {
        /*private final Set<net.minecraft.world.entity.player.PlayerModelType> skins;
        private final java.util.function.Function<net.minecraft.world.entity.player.PlayerModelType, net.minecraft.client.renderer.entity.EntityRenderer> skinLookup;
        private final java.util.function.Function<EntityType<?>, net.minecraft.client.renderer.entity.EntityRenderer> rendererLookup;

        public AddLayers(Set<net.minecraft.world.entity.player.PlayerModelType> skins,
                         java.util.function.Function<net.minecraft.world.entity.player.PlayerModelType, net.minecraft.client.renderer.entity.EntityRenderer> skinLookup,
                         java.util.function.Function<EntityType<?>, net.minecraft.client.renderer.entity.EntityRenderer> rendererLookup) {
            this.skins = skins;
            this.skinLookup = skinLookup;
            this.rendererLookup = rendererLookup;
        }

        public Set<net.minecraft.world.entity.player.PlayerModelType> getSkins() {
            return skins;
        }

        public net.minecraft.client.renderer.entity.EntityRenderer getSkin(net.minecraft.world.entity.player.PlayerModelType skin) {
            return skinLookup.apply(skin);
        }
        *///?} elif <1.20.2 {
        private final Set<String> skins;
        private final java.util.function.Function<String, net.minecraft.client.renderer.entity.EntityRenderer> skinLookup;
        private final java.util.function.Function<EntityType<?>, net.minecraft.client.renderer.entity.EntityRenderer> rendererLookup;

        public AddLayers(Set<String> skins,
                         java.util.function.Function<String, net.minecraft.client.renderer.entity.EntityRenderer> skinLookup,
                         java.util.function.Function<EntityType<?>, net.minecraft.client.renderer.entity.EntityRenderer> rendererLookup) {
            this.skins = skins;
            this.skinLookup = skinLookup;
            this.rendererLookup = rendererLookup;
        }

        public Set<String> getSkins() {
            return skins;
        }

        public net.minecraft.client.renderer.entity.EntityRenderer getSkin(String skin) {
            return skinLookup.apply(skin);
        }
        //?} else {
        /*private final Set<net.minecraft.client.resources.PlayerSkin.Model> skins;
        private final java.util.function.Function<net.minecraft.client.resources.PlayerSkin.Model, net.minecraft.client.renderer.entity.EntityRenderer> skinLookup;
        private final java.util.function.Function<EntityType<?>, net.minecraft.client.renderer.entity.EntityRenderer> rendererLookup;

        public AddLayers(Set<net.minecraft.client.resources.PlayerSkin.Model> skins,
                         java.util.function.Function<net.minecraft.client.resources.PlayerSkin.Model, net.minecraft.client.renderer.entity.EntityRenderer> skinLookup,
                         java.util.function.Function<EntityType<?>, net.minecraft.client.renderer.entity.EntityRenderer> rendererLookup) {
            this.skins = skins;
            this.skinLookup = skinLookup;
            this.rendererLookup = rendererLookup;
        }

        public Set<net.minecraft.client.resources.PlayerSkin.Model> getSkins() {
            return skins;
        }

        public net.minecraft.client.renderer.entity.EntityRenderer getSkin(net.minecraft.client.resources.PlayerSkin.Model skin) {
            return skinLookup.apply(skin);
        }
        *///?}

        /** Null for an entity type nothing registered a renderer for; the caller filters. */
        public net.minecraft.client.renderer.entity.EntityRenderer getRenderer(EntityType<?> entityType) {
            return rendererLookup.apply(entityType);
        }
    }
}
