package com.github.alexmodguy.alexscaves.mixin.fabric.client;

import com.github.alexmodguy.alexscaves.fabric.ModBus;
import com.github.alexmodguy.alexscaves.fabric.forge.client.event.EntityRenderersEvent;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric's stand-in for the point at which Forge fires {@code EntityRenderersEvent.AddLayers}.
 *
 * <p>Forge fires it from inside this very method, right after the two renderer maps are rebuilt,
 * and it fires on <em>every</em> resource reload rather than once — which is correct and not a
 * leak, because the maps hold freshly-constructed renderers each time, so the layer this mod adds
 * goes onto a renderer that has none yet. Injecting at {@code TAIL} reproduces both properties.
 *
 * <p>Fabric API's {@code LivingEntityFeatureRendererRegistrationCallback} covers the same ground
 * and is not used: it is a per-renderer callback, whereas {@code ClientLayerRegistry} walks the
 * entity-type registry itself and asks for a renderer by type. Handing it the two maps keeps that
 * class byte-for-byte the one the other two loaders compile.
 *
 * <p>The renderer types are spelled fully qualified and must NOT be imported — the
 * {@code !mc2102-render-import-entity} rule rewrites exactly that import to this mod's render shim
 * from 1.21.2, and these maps hold vanilla's and other mods' renderers. Same reasoning as
 * {@code ClientLayerRegistry}.
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    // 1.21.2 gave EntityRenderer a second type parameter — the render state it extracts into — so
    // both maps' value types gain a wildcard. Nothing else about either field moves, and neither
    // does what is posted below: AddLayers holds the renderer raw for exactly this reason.
    //? if >=1.21.2 {
    /*@Shadow
    private java.util.Map<EntityType<?>, net.minecraft.client.renderer.entity.EntityRenderer<?, ?>> renderers;
    *///?} else {
    @Shadow
    private java.util.Map<EntityType<?>, net.minecraft.client.renderer.entity.EntityRenderer<?>> renderers;
    //?}

    // The skin key is vanilla's own split: a free-form string up to 1.20.1, the player-skin model
    // enum from 1.20.2 and the top-level PlayerModelType from 1.21.9, whose renderer type is
    // AvatarRenderer rather than PlayerRenderer and which is joined by a second mannequinRenderers
    // map this mod has no interest in. EntityRenderersEvent.AddLayers carries the matching arms, so
    // the post below is textually the same under every one of them — only this field's type moves.
    //
    // The FIELD itself survived 1.21.9 with the same Ljava/util/Map; descriptor, which is why the
    // access-widener entry is ungated: only the Java generics moved.
    //? if >=1.21.9 {
    /*@Shadow
    private java.util.Map<net.minecraft.world.entity.player.PlayerModelType, net.minecraft.client.renderer.entity.player.AvatarRenderer<net.minecraft.client.player.AbstractClientPlayer>> playerRenderers;
    *///?} elif >=1.21.2 {
    /*@Shadow
    private java.util.Map<net.minecraft.client.resources.PlayerSkin.Model, net.minecraft.client.renderer.entity.EntityRenderer<? extends net.minecraft.world.entity.player.Player, ?>> playerRenderers;
    *///?} elif <1.20.2 {
    @Shadow
    private java.util.Map<String, net.minecraft.client.renderer.entity.EntityRenderer<? extends net.minecraft.world.entity.player.Player>> playerRenderers;
    //?} else {
    /*@Shadow
    private java.util.Map<net.minecraft.client.resources.PlayerSkin.Model, net.minecraft.client.renderer.entity.EntityRenderer<? extends net.minecraft.world.entity.player.Player>> playerRenderers;
    *///?}

    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void ac_addLayers(ResourceManager resourceManager, CallbackInfo ci) {
        ModBus.INSTANCE.post(new EntityRenderersEvent.AddLayers(
                playerRenderers.keySet(), playerRenderers::get, renderers::get));
    }
}
