package com.github.alexmodguy.alexscaves.client.render.misc;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

public enum DefaultMapBackgrounds {

    DEFAULT,
    BORDER,
    WATER,
    FROZEN_OCEAN,
    PLAINS,
    DESERT,
    FOREST,
    JUNGLE,
    TAIGA,
    SNOWY,
    SNOWY_TAIGA,
    BADLANDS,
    MOUNTAIN,
    SNOWY_MOUNTAIN,
    ROOFED_FOREST,
    MUSHROOM,
    SWAMP,
    SAVANNA,
    ICE_SPIKES,
    BEACH,
    STONY_SHORE,
    DRIPSTONE_CAVES,
    LUSH_CAVES,
    DEEP_DARK,
    MAGNETIC_CAVES,
    PRIMORDIAL_CAVES,
    TOXIC_CAVES,
    ABYSSAL_CHASM,
    FORLORN_HOLLOWS,
    CANDY_CAVITY;

    private ResourceLocation texture;

    private static final HashMap<Integer, MapBackgroundTexture> TEXTURE_HASH_MAP = new HashMap<>();

    private static MapBackgroundTexture getBackgroundTexture(int id, ResourceLocation resourceLocation) {
        if (TEXTURE_HASH_MAP.containsKey(id)) {
            return TEXTURE_HASH_MAP.get(id);
        } else {
            MapBackgroundTexture simpleTexture = new MapBackgroundTexture(resourceLocation);
            Minecraft.getInstance().getTextureManager().register(resourceLocation, simpleTexture);
            TEXTURE_HASH_MAP.put(id, simpleTexture);
            return simpleTexture;
        }
    }

    public int getMapColor(int u, int v) {
        if(texture == null){
            texture = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "textures/misc/map/" + this.name().toLowerCase(Locale.ROOT) + "_background.png");
        }
        MapBackgroundTexture backgroundTexture = getBackgroundTexture(this.ordinal(), texture);
        return backgroundTexture.getNativeImage() == null ? 0 : clampNativeImg(backgroundTexture.getNativeImage(), u, v);
    }

    private static int clampNativeImg(NativeImage nativeImage, int u, int v) {
        return ACClientCompat.getPixelABGR(nativeImage, u % nativeImage.getWidth(), v % nativeImage.getHeight());
    }

    public static class MapBackgroundTexture extends SimpleTexture {

        private NativeImage nativeImage;

        public MapBackgroundTexture(ResourceLocation resourceLocation) {
            super(resourceLocation);
        }

        public NativeImage getNativeImage() {
            return nativeImage;
        }

        // 1.21.4 split loading from uploading: a texture now hands back TextureContents and the manager
        // uploads and closes it. Both eras therefore read the file a SECOND time for this class's own
        // copy — the uploaded one is disposed, and getNativeImage() is asked for pixels long after.
        //? if >=1.21.4 {
        /*@Override
        public net.minecraft.client.renderer.texture.TextureContents loadContents(ResourceManager resourceManager) throws IOException {
            nativeImage = net.minecraft.client.renderer.texture.TextureContents.load(resourceManager, this.resourceId()).image();
            return super.loadContents(resourceManager);
        }
        *///?} else {
        public void load(ResourceManager resourceManager) throws IOException {
            super.load(resourceManager);
            nativeImage = this.getTextureImage(resourceManager).getImage();
        }
        //?}
    }

}
