package com.github.alexmodguy.alexscaves.client.model.baked;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
//? if <1.21.4
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
//? if <1.21.4
import net.minecraftforge.client.model.BakedModelWrapper;
//? if <1.21.4
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a baked block model so its unshaded quads draw fullbright — the ambersol, uranium and
 * abyssmarine blocks, behind the {@code emissiveBlockModels} client option.
 *
 * <p>Inert from 1.21.4: the post-bake mutation seam it hangs off is gone. NeoForge deleted
 * {@code BakedModelWrapper} outright, and on both loaders {@code ModelEvent.ModifyBakingResult}
 * stopped handing out a mutable model map — it exposes the immutable {@code ModelBakery.BakingResult}
 * record instead. Doing this properly on the new pipeline is a different architecture, not a
 * signature fix, so the emissive-model option is a cosmetic loss on 1.21.4+ (see
 * {@code ClientProxy#bakeModels}). The class shell stays so its one call site is the only thing
 * that has to be gated.
 */
public class BakedModelShadeLayerFullbright
        //? if <1.21.4
        extends BakedModelWrapper
{

    //? if <1.21.4 {
    public BakedModelShadeLayerFullbright(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
        if (state == null) {
            return ACClientCompat.modelQuads(originalModel, state, side, rand, extraData, renderType);
        }
        return transformUnshadedQuad(ACClientCompat.modelQuads(originalModel, state, side, rand, extraData, renderType));
    }

    // The two quad helpers are inside the same gate as their one caller. They used to be left
    // ungated — they named nothing the loaders had taken away — but 1.21.5 turned BakedQuad into a
    // record, so every accessor below is spelled differently there and dead code stopped compiling.
    // The 1.21.2 light-emission difference that used to be a gate inside setFullbright is a rename
    // rule now (`!mc2102-bakedquad-lightemission`), because a gate cannot nest inside this one:
    // a disabled arm is a block comment, and Java block comments do not nest.
    private static List<BakedQuad> transformUnshadedQuad(List<BakedQuad> oldQuads) {
        List<BakedQuad> quads = new ArrayList<>(oldQuads);
        if (!quads.isEmpty()) {
            quads.replaceAll(quad -> quad.isShade() ? quad : setFullbright(quad));
        }
        return quads;
    }

    private static BakedQuad setFullbright(BakedQuad quad) {
        int[] vertexData = quad.getVertices().clone();
        int step = vertexData.length / 4;

        vertexData[6] = 0x00F000F0;
        vertexData[6 + step] = 0x00F000F0;
        vertexData[6 + 2 * step] = 0x00F000F0;
        vertexData[6 + 3 * step] = 0x00F000F0;
        return new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
    }
    //?}
}
