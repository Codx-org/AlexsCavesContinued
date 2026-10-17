package com.github.alexmodguy.alexscaves.client.render.entity.layer;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.registries.BuiltInRegistries;
// LivingEntityRenderer and EntityRenderer are spelled fully qualified below and must NOT be
// imported: the !mc2102-render-import-{living,entity} rules would rewrite those imports to the
// compat shims, and every renderer this class touches is somebody else's — vanilla's or another
// mod's — so it has to name the vanilla types.
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class ClientLayerRegistry {

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        List<EntityType<? extends LivingEntity>> entityTypes = ImmutableList.copyOf(
                BuiltInRegistries.ENTITY_TYPE.stream()
                        .filter(DefaultAttributes::hasSupplier)
                        .map(entityType -> (EntityType<? extends LivingEntity>) entityType)
                        .collect(Collectors.toList()));
        entityTypes.forEach((entityType -> {
            addLayerIfApplicable(entityType, event);
        }));
        // The skin-type key has been three things: a free-form string up to 1.20.1, the nested
        // PlayerSkin.Model enum from 1.20.2, and from 1.21.9 a top-level PlayerModelType — which
        // Forge followed by renaming getSkins/getPlayerSkin to getModelTypes/getPlayerRenderer.
        // Before that, NeoForge widened getSkin/getRenderer to return EntityRenderer rather than
        // LivingEntityRenderer, so the addLayer call needs the narrowing cast spelled out either
        // way; the player renderers are LivingEntityRenderers on every version this mod targets.
        // Forge renamed the pair to getPlayerSkin/getEntityRenderer in 1.21, so the split is by
        // loader as well as by version.
        // NeoForge took the same 1.21.9 type change but only half the renaming: its accessor is
        // still getSkins(), now handing back PlayerModelTypes, while the per-skin lookup became
        // getPlayerRenderer like Forge's. It returns an AvatarRenderer — 1.21.9's replacement for
        // PlayerRenderer, and still a LivingEntityRenderer — so the cast below is unchanged.
        // Fabric takes the type change and neither renaming: the event is this tree's own stub, so
        // its accessors keep the names they have carried since 1.20.1 and only the key type moves.
        // Its per-skin lookup hands back an AvatarRenderer through a raw EntityRenderer, exactly as
        // the two loaders' do, so the cast is the same on all three arms.
        //? if forge && >=1.21.9 {
        /*for (net.minecraft.world.entity.player.PlayerModelType skinType : event.getModelTypes()) {
            net.minecraft.client.renderer.entity.LivingEntityRenderer skinRenderer = (net.minecraft.client.renderer.entity.LivingEntityRenderer) event.getPlayerRenderer(skinType);
            skinRenderer.addLayer(new ACPotionEffectLayer(skinRenderer));
        }
        *///?} elif neoforge && >=1.21.9 {
        /*for (net.minecraft.world.entity.player.PlayerModelType skinType : event.getSkins()) {
            net.minecraft.client.renderer.entity.LivingEntityRenderer skinRenderer = (net.minecraft.client.renderer.entity.LivingEntityRenderer) event.getPlayerRenderer(skinType);
            skinRenderer.addLayer(new ACPotionEffectLayer(skinRenderer));
        }
        *///?} elif fabric && >=1.21.9 {
        /*for (net.minecraft.world.entity.player.PlayerModelType skinType : event.getSkins()) {
            net.minecraft.client.renderer.entity.LivingEntityRenderer skinRenderer = (net.minecraft.client.renderer.entity.LivingEntityRenderer) event.getSkin(skinType);
            skinRenderer.addLayer(new ACPotionEffectLayer(skinRenderer));
        }
        *///?} else {
        //? if >=1.20.2
        /*for (net.minecraft.client.resources.PlayerSkin.Model skinType : event.getSkins()) {*/
        //? if <1.20.2
        for (String skinType : event.getSkins()) {
            //? if forge && >=1.21
            /*net.minecraft.client.renderer.entity.LivingEntityRenderer skinRenderer = (net.minecraft.client.renderer.entity.LivingEntityRenderer) event.getPlayerSkin(skinType);*/
            //? if !(forge && >=1.21)
            net.minecraft.client.renderer.entity.LivingEntityRenderer skinRenderer = (net.minecraft.client.renderer.entity.LivingEntityRenderer) event.getSkin(skinType);
            skinRenderer.addLayer(new ACPotionEffectLayer(skinRenderer));
        }
        //?}
    }

    private static void addLayerIfApplicable(EntityType<? extends LivingEntity> entityType, EntityRenderersEvent.AddLayers event) {
        net.minecraft.client.renderer.entity.LivingEntityRenderer renderer = null;
        if (entityType != EntityType.ENDER_DRAGON) {
            try {
                // Assigning straight into a LivingEntityRenderer leaves NeoForge's inference variable
                // with incompatible upper bounds; take the declared type and narrow explicitly. The
                // instanceof also replaces the catch below as the real filter — most entity types
                // simply do not have a LivingEntityRenderer.
                //? if forge && >=1.21
                /*net.minecraft.client.renderer.entity.EntityRenderer found = event.getEntityRenderer(entityType);*/
                //? if !(forge && >=1.21)
                net.minecraft.client.renderer.entity.EntityRenderer found = event.getRenderer(entityType);
                if (found instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer) {
                    renderer = (net.minecraft.client.renderer.entity.LivingEntityRenderer) found;
                }
            } catch (Exception e) {
                AlexsCaves.LOGGER.warn("Could not apply radiation glow layer to " + BuiltInRegistries.ENTITY_TYPE.getKey(entityType) + ", has custom renderer that is not LivingEntityRenderer.");
            }
            if (renderer != null) {
                renderer.addLayer(new ACPotionEffectLayer(renderer));
            }
        }
    }
}
