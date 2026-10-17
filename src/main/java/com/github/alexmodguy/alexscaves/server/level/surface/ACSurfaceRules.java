package com.github.alexmodguy.alexscaves.server.level.surface;

/**
 * The six cave biomes' surface rules, contributed to the vendored {@code SurfaceRulesManager} at
 * mod construction.
 *
 * <p>26.3 deleted {@code SurfaceRules} and spread the material-rule API across three packages, so
 * this file is two complete arms — see {@code CitadelSurfaceRuleWrapper} for why the rename cannot
 * be a {@code replacements.string} rule.
 *
 * <p>One thing does not port one-for-one. {@code SurfaceRules.ON_FLOOR}, {@code UNDER_FLOOR} and
 * {@code DEEP_UNDER_FLOOR} were ready-made constants; 26.3's successors in
 * {@code VanillaMaterialConditions} are {@code ResourceKey<MaterialCondition>}, i.e. registry keys
 * that need a {@code HolderGetter} to resolve — and there is none anywhere this mod builds its
 * rules, which is the same constraint that already forced {@code ACSurfaceRuleConditionRegistry} to
 * supply its own biome condition. So the 26.3 arm rebuilds each one inline from
 * {@code MaterialRules.stoneDepthCheck}, which is exactly what the vanilla constants are defined as:
 * {@code ON_FLOOR} is {@code (0, false, FLOOR)}, {@code UNDER_FLOOR} is {@code (0, true, FLOOR)} and
 * {@code DEEP_UNDER_FLOOR} is {@code (0, true, 6, FLOOR)}.
 *
 * <p>Those three triples are not remembered, they are read out of 26.3's own
 * {@code data/minecraft/worldgen/material_condition/} — {@code on_floor}, {@code under_floor} and
 * {@code deep_under_floor} —
 * each a {@code minecraft:stone_depth} with {@code offset} 0, {@code surface_type} floor and
 * {@code add_surface_depth} false/true/true, {@code secondary_depth_range} 0/0/6. Worth re-reading
 * on the next bump rather than assuming: a wrong depth here paints the six cave biomes wrong and
 * logs nothing at all.
 */
//? if >=26.3 {
/*import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import com.github.alexmodguy.alexscaves.citadel.server.generation.SurfaceRulesManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.placement.CaveSurface;

public class ACSurfaceRules {

    public static void setup() {
        SurfaceRulesManager.registerOverworldSurfaceRule(ACSurfaceRuleConditionRegistry.isBiome(ACBiomeRegistry.MAGNETIC_CAVES), createMagneticCavesRules());
        SurfaceRulesManager.registerOverworldSurfaceRule(ACSurfaceRuleConditionRegistry.isBiome(ACBiomeRegistry.PRIMORDIAL_CAVES), createPrimordialCavesRules());
        SurfaceRulesManager.registerOverworldSurfaceRule(ACSurfaceRuleConditionRegistry.isBiome(ACBiomeRegistry.TOXIC_CAVES), createToxicCavesRules());
        SurfaceRulesManager.registerOverworldSurfaceRule(ACSurfaceRuleConditionRegistry.isBiome(ACBiomeRegistry.ABYSSAL_CHASM), createAbyssalChasmRules());
        SurfaceRulesManager.registerOverworldSurfaceRule(ACSurfaceRuleConditionRegistry.isBiome(ACBiomeRegistry.FORLORN_HOLLOWS), createForlornHollowsRules());
        SurfaceRulesManager.registerOverworldSurfaceRule(ACSurfaceRuleConditionRegistry.isBiome(ACBiomeRegistry.CANDY_CAVITY), createCandyCavityRules());
    }

    // The three conditions vanilla used to hand out as constants. See the class javadoc.
    private static MaterialCondition onFloor() {
        return MaterialRules.stoneDepthCheck(0, false, CaveSurface.FLOOR);
    }

    private static MaterialCondition underFloor() {
        return MaterialRules.stoneDepthCheck(0, true, CaveSurface.FLOOR);
    }

    private static MaterialCondition deepUnderFloor() {
        return MaterialRules.stoneDepthCheck(0, true, 6, CaveSurface.FLOOR);
    }

    public static MaterialRule createMagneticCavesRules() {
        MaterialRule galena = MaterialRules.state(ACBlockRegistry.GALENA.get().defaultBlockState());
        MaterialRule scarlet = MaterialRules.state(ACBlockRegistry.ENERGIZED_GALENA_SCARLET.get().defaultBlockState());
        MaterialRule azure = MaterialRules.state(ACBlockRegistry.ENERGIZED_GALENA_AZURE.get().defaultBlockState());
        MaterialRule neutral = MaterialRules.state(ACBlockRegistry.ENERGIZED_GALENA_NEUTRAL.get().defaultBlockState());
        MaterialCondition azureCondition = ACSurfaceRuleConditionRegistry.simplexCondition(-0.025F, 0.025F, 90, 1F, 0);
        MaterialCondition scarletCondition = ACSurfaceRuleConditionRegistry.simplexCondition(-0.025F, 0.025F, 90, 1F, 1);
        return MaterialRules.sequence(bedrock(), MaterialRules.ifTrue(azureCondition, MaterialRules.ifTrue(scarletCondition, neutral)), MaterialRules.ifTrue(scarletCondition, scarlet), MaterialRules.ifTrue(azureCondition, azure), galena);
    }

    public static MaterialRule createPrimordialCavesRules() {
        MaterialRule limestone = MaterialRules.state(ACBlockRegistry.LIMESTONE.get().defaultBlockState());
        MaterialRule grass = MaterialRules.state(Blocks.GRASS_BLOCK.defaultBlockState());
        MaterialRule dirt = MaterialRules.state(Blocks.DIRT.defaultBlockState());
        MaterialRule packedMud = MaterialRules.state(Blocks.PACKED_MUD.defaultBlockState());
        MaterialRule dirtOrPackedMud = MaterialRules.sequence(MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.GRAVEL, -0.12D, 0.2D), packedMud), dirt);
        MaterialCondition isUnderwater = MaterialRules.waterBlockCheck(0, 0);
        MaterialRule grassWaterChecked = MaterialRules.sequence(MaterialRules.ifTrue(isUnderwater, grass), dirtOrPackedMud);
        MaterialRule floorRules = MaterialRules.sequence(MaterialRules.ifTrue(onFloor(), grassWaterChecked), MaterialRules.ifTrue(underFloor(), dirtOrPackedMud));
        return MaterialRules.sequence(bedrock(), floorRules, createBands(15, 1, 20, Blocks.SANDSTONE.defaultBlockState()), limestone);
    }

    public static MaterialRule createToxicCavesRules() {
        MaterialRule radrock = MaterialRules.state(ACBlockRegistry.RADROCK.get().defaultBlockState());
        return MaterialRules.sequence(bedrock(), radrock);
    }

    public static MaterialRule createAbyssalChasmRules() {
        MaterialRule abyssmarine = MaterialRules.state(ACBlockRegistry.ABYSSMARINE.get().defaultBlockState());
        MaterialRule deepslate = MaterialRules.state(Blocks.DEEPSLATE.defaultBlockState());
        MaterialRule stone = MaterialRules.state(Blocks.STONE.defaultBlockState());
        MaterialCondition normalDeepslateCondition = MaterialRules.verticalGradient("deepslate", VerticalAnchor.absolute(0), VerticalAnchor.absolute(8));
        MaterialRule stoneOrDeepslate = MaterialRules.sequence(MaterialRules.ifTrue(normalDeepslateCondition, deepslate), stone);
        return MaterialRules.sequence(bedrock(), MaterialRules.ifTrue(deepUnderFloor(), stoneOrDeepslate), MaterialRules.ifTrue(MaterialRules.abovePreliminarySurface(), deepslate), abyssmarine);
    }

    public static MaterialRule createForlornHollowsRules() {
        MaterialRule mud = MaterialRules.state(Blocks.PACKED_MUD.defaultBlockState());
        MaterialRule guanostone = MaterialRules.state(ACBlockRegistry.GUANOSTONE.get().defaultBlockState());
        MaterialRule corpolith = MaterialRules.state(ACBlockRegistry.COPROLITH.get().defaultBlockState());
        MaterialCondition corpolithCondition = ACSurfaceRuleConditionRegistry.simplexCondition(-0.2F, 0.4F, 40, 6F, 3);
        return MaterialRules.sequence(bedrock(), MaterialRules.ifTrue(onFloor(), mud), MaterialRules.ifTrue(corpolithCondition, corpolith), guanostone);
    }

    public static MaterialRule createCandyCavityRules() {
        MaterialRule chocolate = MaterialRules.state(ACBlockRegistry.BLOCK_OF_CHOCOLATE.get().defaultBlockState());
        MaterialRule frostedChocolate = MaterialRules.state(ACBlockRegistry.BLOCK_OF_FROSTED_CHOCOLATE.get().defaultBlockState());
        MaterialRule cake = MaterialRules.state(ACBlockRegistry.CAKE_LAYER.get().defaultBlockState());
        MaterialCondition isUnderwater = MaterialRules.waterBlockCheck(0, 0);
        MaterialRule frostedChocolateWaterChecked = MaterialRules.ifTrue(isUnderwater, frostedChocolate);
        MaterialRule floorRules = MaterialRules.sequence(MaterialRules.ifTrue(onFloor(), frostedChocolateWaterChecked), MaterialRules.ifTrue(underFloor(), chocolate));
        return MaterialRules.sequence(bedrock(), floorRules, createBands(20, 2, 10, ACBlockRegistry.BLOCK_OF_CHOCOLATE.get().defaultBlockState()), cake);
    }

    private static MaterialRule bedrock() {
        MaterialRule bedrock = MaterialRules.state(Blocks.BEDROCK.defaultBlockState());
        MaterialCondition bedrockCondition = MaterialRules.verticalGradient("bedrock", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5));
        return MaterialRules.ifTrue(bedrockCondition, bedrock);
    }

    private static MaterialRule createBands(int layers, int layerThickness, int layerDistance, BlockState state) {
        MaterialRule sandstone = MaterialRules.state(state);
        MaterialRule[] ruleSources = new MaterialRule[layers];
        for (int i = 1; i <= layers; i++) {
            int yDown = i * layerDistance;
            int extra = i % 3 == 0 ? 1 : 0;
            MaterialCondition layer1 = MaterialRules.yBlockCheck(VerticalAnchor.absolute(62 - yDown), 0);
            MaterialCondition layer2 = MaterialRules.yBlockCheck(VerticalAnchor.absolute(62 + extra + layerThickness - yDown), 0);
            ruleSources[i - 1] = MaterialRules.ifTrue(layer1, MaterialRules.ifTrue(MaterialRules.not(layer2), MaterialRules.ifTrue(MaterialRules.noiseCondition2d(Noises.ICE, -0.7D, 0.8D), sandstone)));
        }
        return MaterialRules.sequence(ruleSources);
    }

}
*///?} else {
import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import com.github.alexmodguy.alexscaves.citadel.server.generation.SurfaceRulesManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

public class ACSurfaceRules {

    public static void setup() {
        SurfaceRulesManager.registerOverworldSurfaceRule(ACSurfaceRuleConditionRegistry.isBiome(ACBiomeRegistry.MAGNETIC_CAVES), createMagneticCavesRules());
        SurfaceRulesManager.registerOverworldSurfaceRule(ACSurfaceRuleConditionRegistry.isBiome(ACBiomeRegistry.PRIMORDIAL_CAVES), createPrimordialCavesRules());
        SurfaceRulesManager.registerOverworldSurfaceRule(ACSurfaceRuleConditionRegistry.isBiome(ACBiomeRegistry.TOXIC_CAVES), createToxicCavesRules());
        SurfaceRulesManager.registerOverworldSurfaceRule(ACSurfaceRuleConditionRegistry.isBiome(ACBiomeRegistry.ABYSSAL_CHASM), createAbyssalChasmRules());
        SurfaceRulesManager.registerOverworldSurfaceRule(ACSurfaceRuleConditionRegistry.isBiome(ACBiomeRegistry.FORLORN_HOLLOWS), createForlornHollowsRules());
        SurfaceRulesManager.registerOverworldSurfaceRule(ACSurfaceRuleConditionRegistry.isBiome(ACBiomeRegistry.CANDY_CAVITY), createCandyCavityRules());
    }

    public static SurfaceRules.RuleSource createMagneticCavesRules() {
        SurfaceRules.RuleSource galena = SurfaceRules.state(ACBlockRegistry.GALENA.get().defaultBlockState());
        SurfaceRules.RuleSource scarlet = SurfaceRules.state(ACBlockRegistry.ENERGIZED_GALENA_SCARLET.get().defaultBlockState());
        SurfaceRules.RuleSource azure = SurfaceRules.state(ACBlockRegistry.ENERGIZED_GALENA_AZURE.get().defaultBlockState());
        SurfaceRules.RuleSource neutral = SurfaceRules.state(ACBlockRegistry.ENERGIZED_GALENA_NEUTRAL.get().defaultBlockState());
        SurfaceRules.ConditionSource azureCondition = ACSurfaceRuleConditionRegistry.simplexCondition(-0.025F, 0.025F, 90, 1F, 0);
        SurfaceRules.ConditionSource scarletCondition = ACSurfaceRuleConditionRegistry.simplexCondition(-0.025F, 0.025F, 90, 1F, 1);
        return SurfaceRules.sequence(bedrock(), SurfaceRules.ifTrue(azureCondition, SurfaceRules.ifTrue(scarletCondition, neutral)), SurfaceRules.ifTrue(scarletCondition, scarlet), SurfaceRules.ifTrue(azureCondition, azure), galena);
    }

    public static SurfaceRules.RuleSource createPrimordialCavesRules() {
        SurfaceRules.RuleSource limestone = SurfaceRules.state(ACBlockRegistry.LIMESTONE.get().defaultBlockState());
        SurfaceRules.RuleSource grass = SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState());
        SurfaceRules.RuleSource dirt = SurfaceRules.state(Blocks.DIRT.defaultBlockState());
        SurfaceRules.RuleSource packedMud = SurfaceRules.state(Blocks.PACKED_MUD.defaultBlockState());
        SurfaceRules.RuleSource dirtOrPackedMud = SurfaceRules.sequence(SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.GRAVEL, -0.12D, 0.2D), packedMud), dirt);
        SurfaceRules.ConditionSource isUnderwater = SurfaceRules.waterBlockCheck(0, 0);
        SurfaceRules.RuleSource grassWaterChecked = SurfaceRules.sequence(SurfaceRules.ifTrue(isUnderwater, grass), dirtOrPackedMud);
        SurfaceRules.RuleSource floorRules = SurfaceRules.sequence(SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, grassWaterChecked), SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, dirtOrPackedMud));
        return SurfaceRules.sequence(bedrock(), floorRules, createBands(15, 1, 20, Blocks.SANDSTONE.defaultBlockState()), limestone);
    }

    public static SurfaceRules.RuleSource createToxicCavesRules() {
        SurfaceRules.RuleSource radrock = SurfaceRules.state(ACBlockRegistry.RADROCK.get().defaultBlockState());
        return SurfaceRules.sequence(bedrock(), radrock);
    }

    public static SurfaceRules.RuleSource createAbyssalChasmRules() {
        SurfaceRules.RuleSource abyssmarine = SurfaceRules.state(ACBlockRegistry.ABYSSMARINE.get().defaultBlockState());
        SurfaceRules.RuleSource deepslate = SurfaceRules.state(Blocks.DEEPSLATE.defaultBlockState());
        SurfaceRules.RuleSource stone = SurfaceRules.state(Blocks.STONE.defaultBlockState());
        SurfaceRules.ConditionSource normalDeepslateCondition = SurfaceRules.verticalGradient("deepslate", VerticalAnchor.absolute(0), VerticalAnchor.absolute(8));
        SurfaceRules.RuleSource stoneOrDeepslate = SurfaceRules.sequence(SurfaceRules.ifTrue(normalDeepslateCondition, deepslate), stone);
        return SurfaceRules.sequence(bedrock(), SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, stoneOrDeepslate), SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), deepslate), abyssmarine);
    }

    public static SurfaceRules.RuleSource createForlornHollowsRules() {
        SurfaceRules.RuleSource mud = SurfaceRules.state(Blocks.PACKED_MUD.defaultBlockState());
        SurfaceRules.RuleSource guanostone = SurfaceRules.state(ACBlockRegistry.GUANOSTONE.get().defaultBlockState());
        SurfaceRules.RuleSource corpolith = SurfaceRules.state(ACBlockRegistry.COPROLITH.get().defaultBlockState());
        SurfaceRules.ConditionSource corpolithCondition = ACSurfaceRuleConditionRegistry.simplexCondition(-0.2F, 0.4F, 40, 6F, 3);
        return SurfaceRules.sequence(bedrock(), SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, mud), SurfaceRules.ifTrue(corpolithCondition, corpolith), guanostone);
    }

    public static SurfaceRules.RuleSource createCandyCavityRules() {
        SurfaceRules.RuleSource chocolate = SurfaceRules.state(ACBlockRegistry.BLOCK_OF_CHOCOLATE.get().defaultBlockState());
        SurfaceRules.RuleSource frostedChocolate = SurfaceRules.state(ACBlockRegistry.BLOCK_OF_FROSTED_CHOCOLATE.get().defaultBlockState());
        SurfaceRules.RuleSource cake = SurfaceRules.state(ACBlockRegistry.CAKE_LAYER.get().defaultBlockState());
        SurfaceRules.ConditionSource isUnderwater = SurfaceRules.waterBlockCheck(0, 0);
        SurfaceRules.RuleSource frostedChocolateWaterChecked = SurfaceRules.ifTrue(isUnderwater, frostedChocolate);
        SurfaceRules.RuleSource floorRules = SurfaceRules.sequence(SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, frostedChocolateWaterChecked), SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, chocolate));
        return SurfaceRules.sequence(bedrock(), floorRules, createBands(20, 2, 10, ACBlockRegistry.BLOCK_OF_CHOCOLATE.get().defaultBlockState()), cake);
    }

    private static SurfaceRules.RuleSource bedrock() {
        SurfaceRules.RuleSource bedrock = SurfaceRules.state(Blocks.BEDROCK.defaultBlockState());
        SurfaceRules.ConditionSource bedrockCondition = SurfaceRules.verticalGradient("bedrock", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5));
        return SurfaceRules.ifTrue(bedrockCondition, bedrock);
    }

    private static SurfaceRules.RuleSource createBands(int layers, int layerThickness, int layerDistance, BlockState state) {
        SurfaceRules.RuleSource sandstone = SurfaceRules.state(state);
        SurfaceRules.RuleSource[] ruleSources = new SurfaceRules.RuleSource[layers];
        for (int i = 1; i <= layers; i++) {
            int yDown = i * layerDistance;
            int extra = i % 3 == 0 ? 1 : 0;
            SurfaceRules.ConditionSource layer1 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(62 - yDown), 0);
            SurfaceRules.ConditionSource layer2 = SurfaceRules.yBlockCheck(VerticalAnchor.absolute(62 + extra + layerThickness - yDown), 0);
            ruleSources[i - 1] = SurfaceRules.ifTrue(layer1, SurfaceRules.ifTrue(SurfaceRules.not(layer2), SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.ICE, -0.7D, 0.8D), sandstone)));
        }
        return SurfaceRules.sequence(ruleSources);
    }

}
//?}
