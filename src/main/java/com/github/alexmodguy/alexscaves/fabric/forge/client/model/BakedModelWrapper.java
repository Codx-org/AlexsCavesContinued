package com.github.alexmodguy.alexscaves.fabric.forge.client.model;

import com.github.alexmodguy.alexscaves.fabric.forge.client.model.data.ModelData;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
//? if <1.21.4
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//? if <1.21.4
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Fabric stand-in for the loader's delegating baked-model base class. One subclass,
 * {@code BakedModelShadeLayerFullbright}, which is itself gated {@code <1.21.4} — so this is too,
 * and above that band only the shell survives.
 *
 * <h2>The one thing this class does that a field and seven delegates would not</h2>
 *
 * <p>The loaders patch {@code BakedModel} with a five-argument {@code getQuads} that carries a
 * {@link ModelData} and the {@code RenderType} being drawn, give it a default that throws the two
 * extra arguments away, and then <b>call the five-argument one from their block renderer</b>. So on
 * those loaders a subclass overriding the wide overload is reached. Vanilla's renderer calls the
 * three-argument one and knows nothing about the other, which would leave the subclass's override
 * dead here.
 *
 * <p>This class closes that by declaring the wide overload itself and having the <b>narrow, vanilla
 * one delegate into it</b> with {@link ModelData#EMPTY} and a null render type. That reproduces the
 * loaders' dispatch exactly where it matters — the subclass's quads are the ones vanilla gets — and
 * the two extra arguments are as empty as they always were, since nothing in this mod ever fills a
 * {@code ModelData} (see that class).
 *
 * <p>Note the direction: the loaders' default goes wide → narrow, this one goes narrow → wide. A
 * subclass that overrides <i>only</i> the narrow overload would recurse; none does, and the wide one
 * is the whole reason to extend this.
 *
 * <h2>Two differences from the loader's version, both deliberate</h2>
 *
 * <ul>
 *   <li>It is <b>not generic</b>. The loader's is {@code BakedModelWrapper<T extends BakedModel>}
 *       with a {@code protected final T originalModel}, and the one subclass here uses it raw — so
 *       the field is a plain {@code BakedModel} on every loader either way, and a type parameter
 *       nothing reads would only make the raw {@code extends} clause warn.
 *   <li>{@code getTransforms()} is <b>declared and does not delegate</b>. It is abstract on the
 *       vanilla interface — the loader patches it to a default whose whole body is {@code getstatic
 *       ItemTransforms.NO_TRANSFORMS} — so on Fabric a concrete class has to say it, and saying
 *       anything else would change behaviour. Note what that means on the loaders, since it is
 *       easy to misread as an oversight and "fix" it: a wrapped model reports NO transforms, not the
 *       wrapped model's, and {@code ACClientCompat#applyItemTransform} therefore applies none to
 *       one. The stand-in reproduces that rather than improving on it.
 * </ul>
 *
 * <p>{@code getOverrides()} is spelled in the 1.20.1 form and rewritten to {@code overrides()}
 * returning {@code BakedOverrides} from 1.21.2 by {@code !mc2102-bakedoverrides-*}. It is abstract
 * on the vanilla interface below that and a default from it, so the delegate is required in the
 * first half of the band and merely faithful in the second. The exact version of the change is an
 * inference: 1.21.1 has the old shape and 1.21.3 the new, and no Forge build exists for 1.21.2 to
 * read directly.
 *
 * <p>Installing a wrapper at all still depends on a Fabric model sink, which does not exist yet —
 * {@code ClientProxy#bakeModels} listens for the stand-in {@code ModelEvent.ModifyBakingResult} and
 * nothing fires it. Fabric API's {@code ModelLoadingPlugin.Context#modifyModelAfterBake} is the
 * seam that will, in the event-dispatch batch.
 */
public class BakedModelWrapper
        //? if <1.21.4
        implements BakedModel
{

    //? if <1.21.4 {
    protected final BakedModel originalModel;

    public BakedModelWrapper(BakedModel originalModel) {
        this.originalModel = originalModel;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
        return originalModel.getQuads(state, side, rand);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return originalModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return originalModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return originalModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return originalModel.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return originalModel.getParticleIcon();
    }

    @Override
    public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() {
        return net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS;
    }

    @Override
    public ItemOverrides getOverrides() {
        return originalModel.getOverrides();
    }
    //?}
}
