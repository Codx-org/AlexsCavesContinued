package com.github.alexmodguy.alexscaves.server.level.feature;

/**
 * The fifty feature registrations, and the one place the 26.3 feature rewrite is visible.
 *
 * <p>Below 26.3 a feature is an instance registered into {@code Registries.FEATURE}, generic over a
 * configuration supplied per placement. At 26.3 that registry holds {@code MapCodec}s instead — it
 * is spelled {@code FEATURE_TYPE}, and the name {@code FEATURE} was reused for what used to be
 * {@code CONFIGURED_FEATURE} — so what gets registered is a codec that decodes a feature, and the
 * feature carries its own config.
 *
 * <p>Both registry keys are written out literally on their own arm rather than rewritten by a
 * {@code replacements.string} rule, because the 26.2 and 26.3 spellings of these two constants cross
 * over: a rule mapping {@code Registries.FEATURE} to {@code Registries.FEATURE_TYPE} would sit beside
 * the rule mapping {@code Registries.CONFIGURED_FEATURE} to {@code Registries.FEATURE}, and rules do
 * not chain. Naming them per arm removes the question entirely.
 *
 * <p>{@code CODECS} is read back by {@code ACSimpleFeature.codec()} and {@code ACFeature.codec()}
 * through {@code codecFor}. 26.3 asks every feature instance for the codec that produced it, and the
 * fifty subclasses are otherwise identical across all 60 nodes — so the lookup lives here, beside the
 * registrations, rather than becoming a gated override in each one.
 *
 * <p>The whole file is gated because the two arms share no import: the 26.3 arm cannot name
 * {@code NoneFeatureConfiguration}, which that version deletes.
 */
//? if >=26.3 {
/*import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.level.feature.config.*;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class ACFeatureRegistry {

    public static final DeferredRegister<MapCodec<? extends Feature>> DEF_REG = DeferredRegister.create(Registries.FEATURE_TYPE, AlexsCaves.MODID);

    private static final Map<Class<?>, MapCodec<? extends Feature>> CODECS = new HashMap<>();

    static {
        reg("galena_hexagon", GalenaHexagonFeature.class, GalenaHexagonFeatureConfiguration.MAP_CODEC, GalenaHexagonFeature::new);
        reg("magnetic_node", MagneticNodeFeature.class, MagneticNodeFeatureConfiguration.MAP_CODEC, MagneticNodeFeature::new);
        reg("underground_ruins", UndergroundRuinsFeature.class, UndergroundRuinsFeatureConfiguration.MAP_CODEC, UndergroundRuinsFeature::new);
        reg("floating_orb", FloatingOrbFeature.class, FloatingOrbFeatureConfig.MAP_CODEC, FloatingOrbFeature::new);
        regSimple("tesla_bulb", TeslaBulbFeature.class, () -> new TeslaBulbFeature(null));
        reg("covered_block_blob", CoveredBlockBlobFeature.class, CoveredBlockBlobConfiguration.MAP_CODEC, CoveredBlockBlobFeature::new);
        regSimple("ambersol", AmbersolFeature.class, () -> new AmbersolFeature(null));
        regSimple("pewen_tree", PewenTreeFeature.class, () -> new PewenTreeFeature(null));
        regSimple("ancient_tree", AncientTreeFeature.class, () -> new AncientTreeFeature(null));
        regSimple("giant_ancient_tree", GiantAncientTreeFeature.class, () -> new GiantAncientTreeFeature(null));
        regSimple("cycad", CycadFeature.class, () -> new CycadFeature(null));
        regSimple("amber_monolith", AmberMonolithFeature.class, () -> new AmberMonolithFeature(null));
        regSimple("subterranodon_roost", SubterranodonRoostFeature.class, () -> new SubterranodonRoostFeature(null));
        regSimple("volcano_boulder", VolcanoBoulderFeature.class, () -> new VolcanoBoulderFeature(null));
        regSimple("acid_vent", AcidVentFeature.class, () -> new AcidVentFeature(null));
        regSimple("sulfur_stack", SulfurStackFeature.class, () -> new SulfurStackFeature(null));
        regSimple("nuclear_siren", NuclearSirenFeature.class, () -> new NuclearSirenFeature(null));
        reg("fill_biome_above", FillBiomeAboveFeature.class, FillBiomeAboveConfiguration.MAP_CODEC, FillBiomeAboveFeature::new);
        regSimple("fill_in_bubbles_with_water", FillInBubblesWithWaterFeature.class, () -> new FillInBubblesWithWaterFeature(null));
        regSimple("black_vent", BlackVentFeature.class, () -> new BlackVentFeature(null));
        regSimple("tube_worm", TubeWormFeature.class, () -> new TubeWormFeature(null));
        reg("whalefall", WhalefallFeature.class, WhalefallFeatureConfiguration.MAP_CODEC, WhalefallFeature::new);
        regSimple("ping_pong_sponge", PingPongSpongeFeature.class, () -> new PingPongSpongeFeature(null));
        reg("abyssal_flora", AbyssalFloraFeature.class, AbyssalFloraFeatureConfiguration.MAP_CODEC, AbyssalFloraFeature::new);
        regSimple("abyssal_boulder", AbyssalBoulderFeature.class, () -> new AbyssalBoulderFeature(null));
        regSimple("mussel", MusselFeature.class, () -> new MusselFeature(null));
        reg("deep_one_ruins", DeepOnesRuinsFeature.class, UndergroundRuinsFeatureConfiguration.MAP_CODEC, DeepOnesRuinsFeature::new);
        regSimple("peering_coprolith", PeeringCoprolithFeature.class, () -> new PeeringCoprolithFeature(null));
        regSimple("thornwood_tree", ThornwoodTreeFeature.class, () -> new ThornwoodTreeFeature(null));
        regSimple("thornwood_tree_with_branches", ThornwoodTreeWithBranchesFeature.class, () -> new ThornwoodTreeWithBranchesFeature(null));
        regSimple("thornwood_roots", ThornwoodRootsFeature.class, () -> new ThornwoodRootsFeature(null));
        regSimple("guano_pile", GuanoPileFeature.class, () -> new GuanoPileFeature(null));
        reg("forlorn_ruins", ForlornRuinsFeature.class, UndergroundRuinsFeatureConfiguration.MAP_CODEC, ForlornRuinsFeature::new);
        regSimple("peppermint_patch", PeppermintPileFeature.class, () -> new PeppermintPileFeature(null));
        regSimple("encrusted_peppermint", EncrustedPeppermintFeature.class, () -> new EncrustedPeppermintFeature(null));
        regSimple("licoroot_tree", LicorootTreeFeature.class, () -> new LicorootTreeFeature(null));
        regSimple("licoroot_tree_with_sprouts", LicorootTreeWithSproutsFeature.class, () -> new LicorootTreeWithSproutsFeature(null));
        regSimple("spilled_ice_cream_cone", SpilledIceCreamConeFeature.class, () -> new SpilledIceCreamConeFeature(null));
        reg("ice_cream_scoop", IceCreamScoopFeature.class, IceCreamScoopFeatureConfiguration.MAP_CODEC, IceCreamScoopFeature::new);
        regSimple("ceiling_ice_cream_cone", CeilingIceCreamConeFeature.class, () -> new CeilingIceCreamConeFeature(null));
        regSimple("sweet_puff", SweetPuffFeature.class, () -> new SweetPuffFeature(null));
        regSimple("sprinkles_pile", SprinklesPileFeature.class, () -> new SprinklesPileFeature(null));
        regSimple("candy_cane", CandyCaneFeature.class, () -> new CandyCaneFeature(null));
        regSimple("sundrop_patch", SundropPatchFeature.class, () -> new SundropPatchFeature(null));
        reg("lollipop", LollipopFeature.class, LollipopFeatureConfiguration.MAP_CODEC, LollipopFeature::new);
        regSimple("cookie_shelf", CookieShelfFeature.class, () -> new CookieShelfFeature(null));
        reg("candy_ruins", CandyRuinsFeature.class, UndergroundRuinsFeatureConfiguration.MAP_CODEC, CandyRuinsFeature::new);
        regSimple("gobstopper_geode", GobstopperGeodeFeature.class, () -> new GobstopperGeodeFeature(null));
        regSimple("ceiling_frostmint", CeilingFrostmintFeature.class, () -> new CeilingFrostmintFeature(null));
        regSimple("floating_gummy_ring", FloatingGummyRingFeature.class, () -> new FloatingGummyRingFeature(null));
    }

    // A configured feature decodes its config and hands it to the constructor, which stores it; the
    // reverse direction reads it back for encoding. The constructor takes Object on both arms, so the
    // method reference needs the explicit Function to pin FC.
    private static <FC extends ACFeatureConfiguration, F extends ACFeature<FC>> void reg(String name, Class<F> type, MapCodec<FC> configCodec, Function<Object, F> factory) {
        MapCodec<F> codec = configCodec.xmap(factory::apply, ACFeature::acConfig);
        CODECS.put(type, codec);
        DEF_REG.register(name, () -> codec);
    }

    // A config-free feature carries nothing to encode, so its codec reads and writes an empty map.
    private static <F extends ACSimpleFeature> void regSimple(String name, Class<F> type, Supplier<F> factory) {
        MapCodec<F> codec = MapCodec.unit(factory);
        CODECS.put(type, codec);
        DEF_REG.register(name, () -> codec);
    }

    public static MapCodec<? extends Feature> codecFor(Class<?> type) {
        return CODECS.get(type);
    }
}
*///?} else {
import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.level.feature.config.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;

public class ACFeatureRegistry {
    public static final DeferredRegister<Feature<?>> DEF_REG = DeferredRegister.create(Registries.FEATURE, AlexsCaves.MODID);

    public static final Supplier<Feature<GalenaHexagonFeatureConfiguration>> GALENA_HEXAGON = DEF_REG.register("galena_hexagon", () -> new GalenaHexagonFeature(GalenaHexagonFeatureConfiguration.CODEC));
    public static final Supplier<Feature<MagneticNodeFeatureConfiguration>> MAGNETIC_NODE = DEF_REG.register("magnetic_node", () -> new MagneticNodeFeature(MagneticNodeFeatureConfiguration.CODEC));
    public static final Supplier<Feature<UndergroundRuinsFeatureConfiguration>> UNDERGROUND_RUINS = DEF_REG.register("underground_ruins", () -> new UndergroundRuinsFeature(UndergroundRuinsFeatureConfiguration.CODEC));
    public static final Supplier<Feature<FloatingOrbFeatureConfig>> FLOATING_ORB = DEF_REG.register("floating_orb", () -> new FloatingOrbFeature(FloatingOrbFeatureConfig.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> TESLA_BULB = DEF_REG.register("tesla_bulb", () -> new TeslaBulbFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<CoveredBlockBlobConfiguration>> COVERED_BLOCK_BLOB = DEF_REG.register("covered_block_blob", () -> new CoveredBlockBlobFeature(CoveredBlockBlobConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> AMBERSOL = DEF_REG.register("ambersol", () -> new AmbersolFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> PEWEN_TREE = DEF_REG.register("pewen_tree", () -> new PewenTreeFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> ANCIENT_TREE = DEF_REG.register("ancient_tree", () -> new AncientTreeFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> GIANT_ANCIENT_TREE = DEF_REG.register("giant_ancient_tree", () -> new GiantAncientTreeFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> CYCAD = DEF_REG.register("cycad", () -> new CycadFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> AMBER_MONOLITH = DEF_REG.register("amber_monolith", () -> new AmberMonolithFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> SUBTERRANODON_ROOST = DEF_REG.register("subterranodon_roost", () -> new SubterranodonRoostFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> VOLCANO_BOULDER = DEF_REG.register("volcano_boulder", () -> new VolcanoBoulderFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> ACID_VENT = DEF_REG.register("acid_vent", () -> new AcidVentFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> SULFUR_STACK = DEF_REG.register("sulfur_stack", () -> new SulfurStackFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> NUCLEAR_SIREN = DEF_REG.register("nuclear_siren", () -> new NuclearSirenFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<FillBiomeAboveConfiguration>> FILL_BIOME_ABOVE = DEF_REG.register("fill_biome_above", () -> new FillBiomeAboveFeature(FillBiomeAboveConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> FILL_IN_BUBBLES_WITH_WATER = DEF_REG.register("fill_in_bubbles_with_water", () -> new FillInBubblesWithWaterFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> BLACK_VENT = DEF_REG.register("black_vent", () -> new BlackVentFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> TUBE_WORM = DEF_REG.register("tube_worm", () -> new TubeWormFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<WhalefallFeatureConfiguration>> WHALEFALL = DEF_REG.register("whalefall", () -> new WhalefallFeature(WhalefallFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> PING_PONG_SPONGE = DEF_REG.register("ping_pong_sponge", () -> new PingPongSpongeFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<AbyssalFloraFeatureConfiguration>> ABYSSAL_FLORA = DEF_REG.register("abyssal_flora", () -> new AbyssalFloraFeature(AbyssalFloraFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> ABYSSAL_BOULDER = DEF_REG.register("abyssal_boulder", () -> new AbyssalBoulderFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> MUSSEL = DEF_REG.register("mussel", () -> new MusselFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<UndergroundRuinsFeatureConfiguration>> DEEP_ONE_RUINS = DEF_REG.register("deep_one_ruins", () -> new DeepOnesRuinsFeature(UndergroundRuinsFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> PEERING_COPROLITH = DEF_REG.register("peering_coprolith", () -> new PeeringCoprolithFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> THORNWOOD_TREE = DEF_REG.register("thornwood_tree", () -> new ThornwoodTreeFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> THORNWOOD_TREE_WITH_BRANCHES = DEF_REG.register("thornwood_tree_with_branches", () -> new ThornwoodTreeWithBranchesFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> THORNWOOD_ROOTS = DEF_REG.register("thornwood_roots", () -> new ThornwoodRootsFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> GUANO_PILE = DEF_REG.register("guano_pile", () -> new GuanoPileFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<UndergroundRuinsFeatureConfiguration>> FORLORN_RUINS = DEF_REG.register("forlorn_ruins", () -> new ForlornRuinsFeature(UndergroundRuinsFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> PEPPERMINT_PILE = DEF_REG.register("peppermint_patch", () -> new PeppermintPileFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> ENCRUSTED_PEPPERMINT = DEF_REG.register("encrusted_peppermint", () -> new EncrustedPeppermintFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> LICOROOT_TREE = DEF_REG.register("licoroot_tree", () -> new LicorootTreeFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> LICOROOT_TREE_WITH_SPROUTS = DEF_REG.register("licoroot_tree_with_sprouts", () -> new LicorootTreeWithSproutsFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> SPILLED_ICE_CREAM_CONE = DEF_REG.register("spilled_ice_cream_cone", () -> new SpilledIceCreamConeFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<IceCreamScoopFeatureConfiguration>> ICE_CREAM_SCOOP = DEF_REG.register("ice_cream_scoop", () -> new IceCreamScoopFeature(IceCreamScoopFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> CEILING_ICE_CREAM_CONE = DEF_REG.register("ceiling_ice_cream_cone", () -> new CeilingIceCreamConeFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> SWEET_PUFF = DEF_REG.register("sweet_puff", () -> new SweetPuffFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> SPRINKLES_PILE = DEF_REG.register("sprinkles_pile", () -> new SprinklesPileFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> CANDY_CANE = DEF_REG.register("candy_cane", () -> new CandyCaneFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> SUNDROP_PATCH = DEF_REG.register("sundrop_patch", () -> new SundropPatchFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<LollipopFeatureConfiguration>> LOLLIPOP = DEF_REG.register("lollipop", () -> new LollipopFeature(LollipopFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> COOKIE_SHELF = DEF_REG.register("cookie_shelf", () -> new CookieShelfFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<UndergroundRuinsFeatureConfiguration>> CANDY_RUINS = DEF_REG.register("candy_ruins", () -> new CandyRuinsFeature(UndergroundRuinsFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> GOBSTOPPER_GEODE = DEF_REG.register("gobstopper_geode", () -> new GobstopperGeodeFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> CEILING_FROSTMINT = DEF_REG.register("ceiling_frostmint", () -> new CeilingFrostmintFeature(NoneFeatureConfiguration.CODEC));
    public static final Supplier<Feature<NoneFeatureConfiguration>> FLOATING_GUMMY_RING = DEF_REG.register("floating_gummy_ring", () -> new FloatingGummyRingFeature(NoneFeatureConfiguration.CODEC));

}
//?}
