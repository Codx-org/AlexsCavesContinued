package com.github.alexmodguy.alexscaves.server.level.feature.config;

/**
 * The marker interface this mod's ten feature configs implement, standing in for vanilla's
 * {@code FeatureConfiguration}.
 *
 * <p>26.3 deleted {@code FeatureConfiguration} outright — not merely {@code NoneFeatureConfiguration}
 * — because a 26.3 {@code Feature} is no longer generic over its config at all: it holds the config
 * as ordinary state and is itself the thing a {@code MapCodec} decodes. There is therefore no vanilla
 * type left for the ten configs to implement, and no vanilla bound for {@code ACFeature<FC>} to
 * satisfy.
 *
 * <p>Naming the bound ourselves keeps {@code ACFeature<FC extends ACFeatureConfiguration>} legal on
 * both sides of the boundary with a single gate in a single file, instead of one gate per config.
 * Below 26.3 this extends the vanilla interface, so the ten configs remain vanilla configs and
 * {@code Feature<FC>} still accepts them; at 26.3 it is a bare marker.
 *
 * <p>Safe to introduce because {@code FeatureConfiguration} and {@code NoneFeatureConfiguration}
 * appear in no file outside {@code server/level/feature/**}, and {@code FeatureConfiguration.NONE}
 * is referenced nowhere in the tree.
 */
//? if >=26.3 {
/*public interface ACFeatureConfiguration {
}
*///?} else {
public interface ACFeatureConfiguration extends net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration {
}
//?}
