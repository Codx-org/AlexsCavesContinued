package com.github.alexmodguy.alexscaves.citadel.client;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.citadel.item.CitadelDisplayItems;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
//? if <1.21.4
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//? if <1.21.6
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws {@link CitadelDisplayItems#ICON_ITEM} and {@link CitadelDisplayItems#EFFECT_ITEM} as a flat
 * textured quad. Citadel's {@code fancy_item} branch is not vendored — Alex's Caves never uses it.
 * <p>
 * Unlike upstream there is no bundled default icon texture: an {@code icon_item} with no
 * {@code IconLocation} tag simply renders nothing rather than dragging a Citadel PNG along.
 * <p>
 * 1.21.4 deleted {@code BlockEntityWithoutLevelRenderer}; from there the two items reach this body
 * through {@link com.github.alexmodguy.alexscaves.client.render.item.ACItemSpecialRenderer.Icon}
 * instead. The drawing code itself is version-independent — only the header and constructor are gated.
 */
public class CitadelItemstackRenderer
        //? if <1.21.4
        extends BlockEntityWithoutLevelRenderer
{

    private static final Map<String, ResourceLocation> LOADED_ICONS = new HashMap<>();

    //? if <1.21.4 {
    public CitadelItemstackRenderer() {
        super(null, null);
    }
    //?}

    //? if <1.21.4
    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack matrixStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        if (stack.getItem() == CitadelDisplayItems.EFFECT_ITEM.get()) {
            MobEffect effect = null;
            if (ACCompat.getTag(stack) != null && ACCompat.getTag(stack).contains("DisplayEffect")) {
                effect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.parse(ACCompat.getString(ACCompat.getTag(stack), "DisplayEffect")));
            }
            if (effect == null) {
                effect = ACCompat.vanillaEffect(MobEffects.MOVEMENT_SPEED);
            }
            // 1.21.6 deleted MobEffectTextureManager and folded the effect icons into the GUI
            // atlas: the id is derived from the effect's registry key (Gui.getMobEffectSprite)
            // and looked up there. Same sprite, so the quad below is unchanged. 1.21.9 then folded
            // every stitched atlas into one AtlasManager, so the GUI sheet is fetched by id — the
            // same TextureAtlas GuiGraphics itself holds — and asked for the very same sprite.
            //? if <1.21.6 {
            MobEffectTextureManager sprites = Minecraft.getInstance().getMobEffectTextures();
            TextureAtlasSprite sprite = sprites.get(ACCompat.effect(effect));
            //?} elif <1.21.9 {
            /*TextureAtlasSprite sprite = Minecraft.getInstance().getGuiSprites()
                    .getSprite(net.minecraft.client.gui.Gui.getMobEffectSprite(ACCompat.effect(effect)));
            *///?} else {
            /*TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
                    .getAtlasOrThrow(net.minecraft.data.AtlasIds.GUI)
                    .getSprite(net.minecraft.client.gui.Gui.getMobEffectSprite(ACCompat.effect(effect)));
            *///?}
            matrixStack.pushPose();
            matrixStack.translate(0, 0, 0.5F);
            drawQuad(matrixStack, sprite.atlasLocation(), combinedLight, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1());
            matrixStack.popPose();
        } else if (stack.getItem() == CitadelDisplayItems.ICON_ITEM.get()) {
            if (ACCompat.getTag(stack) == null || !ACCompat.getTag(stack).contains("IconLocation")) {
                return;
            }
            String iconLocationStr = ACCompat.getString(ACCompat.getTag(stack), "IconLocation");
            ResourceLocation texture = LOADED_ICONS.computeIfAbsent(iconLocationStr, id -> ResourceLocation.parse(id));
            matrixStack.pushPose();
            matrixStack.translate(0, 0, 0.5F);
            drawQuad(matrixStack, texture, combinedLight, 0, 0, 1, 1);
            matrixStack.popPose();
        }
    }

    // The texture is a parameter rather than a RenderSystem.setShaderTexture call at the two
    // call sites: from 1.21.5 the texture is part of the RenderType the facade resolves, so it
    // has to arrive with the draw rather than being bound as ambient state beforehand.
    private static void drawQuad(PoseStack matrixStack, ResourceLocation texture, int combinedLight, float u0, float v0, float u1, float v1) {
        ACClientCompat.setImmediateTint(1.0F, 1.0F, 1.0F, 1.0F);
        BufferBuilder bufferbuilder = ACClientCompat.beginImmediate(ACClientCompat.ImmediateDraw.PARTICLE_QUADS, texture);
        Matrix4f mx = matrixStack.last().pose();
        int br = 255;
        bufferbuilder.vertex(mx, 1, 1, 0).uv(u1, v0).color(br, br, br, 255).uv2(combinedLight).endVertex();
        bufferbuilder.vertex(mx, 0, 1, 0).uv(u0, v0).color(br, br, br, 255).uv2(combinedLight).endVertex();
        bufferbuilder.vertex(mx, 0, 0, 0).uv(u0, v1).color(br, br, br, 255).uv2(combinedLight).endVertex();
        bufferbuilder.vertex(mx, 1, 0, 0).uv(u1, v1).color(br, br, br, 255).uv2(combinedLight).endVertex();
        ACClientCompat.drawImmediate(ACClientCompat.ImmediateDraw.PARTICLE_QUADS, bufferbuilder, texture);
    }
}
