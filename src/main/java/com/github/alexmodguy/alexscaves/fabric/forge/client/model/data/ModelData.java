package com.github.alexmodguy.alexscaves.fabric.forge.client.model.data;

/**
 * Fabric stand-in for the per-block-position model data bag the other two loaders thread through
 * their baked-model API.
 *
 * <p>On the loader it is an immutable map of model properties, filled by a block entity that wants
 * its model to bake differently — a pipe knowing which neighbours to connect to, say. Nothing in
 * this mod ever fills one: all seven call sites hand over {@link #EMPTY}, and the two that receive
 * one ({@code BakedModelShadeLayerFullbright}, and the block-breaking decal in
 * {@code CorrodentRenderer}) only pass it straight on. So the bag can be empty here and the type
 * exists purely to keep those signatures spelled the same on every loader.
 *
 * <p>There is deliberately no builder and no property type. Adding either would suggest the data
 * reaches a model on this loader, and it does not — the calls that consume it are loader patches on
 * {@code BakedModel}, which {@code ACClientCompat#modelQuads} answers with vanilla's own
 * data-less overload here.
 *
 * <p>Inert from 1.21.4 along with everything that names it, but unlike its call sites this needs no
 * gate: it mentions nothing that any version deleted.
 */
public final class ModelData {

    public static final ModelData EMPTY = new ModelData();

    private ModelData() {
    }
}
