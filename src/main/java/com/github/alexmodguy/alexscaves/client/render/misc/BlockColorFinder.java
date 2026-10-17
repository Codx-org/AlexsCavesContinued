package com.github.alexmodguy.alexscaves.client.render.misc;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.awt.*;
import java.awt.image.BufferedImage;

public class BlockColorFinder {

    public static final Object2IntMap<String> TEXTURES_TO_COLOR = new Object2IntOpenHashMap<>();

    public static int getBlockColor(BlockState blockState, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
        String blockName = blockState.toString();
        int colorizer = -1;
        if(!blockState.is(ACBlockRegistry.BLOCK_OF_FROSTED_CHOCOLATE.get())){
            try{
                // 26 turned BlockColors inside out: instead of asking it for a colour it hands back
                // the BlockTintSource registered for a tint index — null when the block has none,
                // which is what the old getColor answered with -1 — and the source itself is asked.
                // Which of its two queries to use is the caller's choice now rather than the
                // implementation's: colorInWorld wants a real level and position, and this method
                // is documented to take both as nullable, so a null one falls back to the
                // position-independent color(BlockState) rather than being passed through.
                //
                // ⚠️ That call is spelled as a METHOD REFERENCE on purpose. The !mc21-vc-color rule
                // rewrites the substring `.color(` to `.setColor(` on every node from 1.21 up — it
                // is the VertexConsumer rename, 212 call sites in this tree — and a replacement hits
                // every source file including the bodies of commented-out arms, so a plain
                // `tintSource.color(blockState)` reaches the compiler as `setColor` and 26 has no
                // such method. `tintSource::color` carries no dot before the name, so the rule
                // cannot see it, and unlike splitting the receiver across a line it stays correct
                // if anyone reformats this file.
                //? if >=26 {
                /*net.minecraft.client.color.block.BlockTintSource tintSource = Minecraft.getInstance().getBlockColors().getTintSource(blockState, 0);
                java.util.function.ToIntFunction<BlockState> flatColor = tintSource == null ? null : tintSource::color;
                colorizer = tintSource == null ? -1 : (level == null || pos == null ? flatColor.applyAsInt(blockState) : tintSource.colorInWorld(blockState, level, pos));
                *///?} else {
                colorizer = Minecraft.getInstance().getBlockColors().getColor(blockState, level, pos, 0);
                //?}
            }catch (Exception e){
                AlexsCaves.LOGGER.warn("Another mod did not use block colorizers correctly.");
            }
        }
        if (TEXTURES_TO_COLOR.containsKey(blockName)) {
            if(colorizer == -1){
                return TEXTURES_TO_COLOR.getInt(blockName);
            }else{
                return colorizer;
            }
        } else {
            int color = 0XFFFFFF;
            if(colorizer == -1){
                try {
                    Color texColour = getAverageColour(getTextureAtlas(blockState));
                    color = texColour.getRGB();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }else{
                color = colorizer;
            }
            TEXTURES_TO_COLOR.put(blockName, color);
            return color;
        }
    }

    private static Color getAverageColour(TextureAtlasSprite image) {
        float red = 0;
        float green = 0;
        float blue = 0;
        float count = 0;
        int uMax = image.contents().width();
        int vMax = image.contents().height();
        for (float i = 0; i < uMax; i++)
            for (float j = 0; j < vMax; j++) {
                int alpha = image.getPixelRGBA(0, (int) i, (int) j) >> 24 & 0xFF;
                if (alpha == 0) {
                    continue;
                }
                red += image.getPixelRGBA(0, (int) i, (int) j) >> 0 & 0xFF;
                green += image.getPixelRGBA(0, (int) i, (int) j) >> 8 & 0xFF;
                blue += image.getPixelRGBA(0, (int) i, (int) j) >> 16 & 0xFF;
                count++;
            }
        //Average color
        return new Color((int) (red / count), (int) (green / count), (int) (blue / count));
    }

    private static TextureAtlasSprite getTextureAtlas(BlockState state) {
        return ACClientCompat.blockParticleSprite(state);
    }
}
