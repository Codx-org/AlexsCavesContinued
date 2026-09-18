package com.github.alexmodguy.alexscaves.citadel.server.generation;

/**
 * The vendored Citadel registry that mods contribute surface rules to, and the merge that folds them
 * into the dimension's own rule source.
 *
 * <p>Three arms. 26.3 deleted {@code SurfaceRules} and renamed every type in it — see
 * {@code CitadelSurfaceRuleWrapper} for why that cannot be a {@code replacements.string} rule —
 * so {@code RuleSource} becomes {@code MaterialRule}, {@code ConditionSource} becomes
 * {@code MaterialCondition}, and the pattern-matched subtypes {@code TestRuleSource},
 * {@code SequenceRuleSource} and {@code BiomeConditionSource} become {@code ConditionRule},
 * {@code SequenceRule} and {@code BiomeCondition}. Their accessors — {@code ifTrue}, {@code thenRun},
 * {@code sequence}, {@code biomes} — keep their names.
 *
 * <p>The middle arm exists for the TerraBlender scan alone: 26.2 made {@code BiomeConditionSource} a
 * record over a {@code HolderSet<Biome>} rather than a {@code List<ResourceKey<Biome>>}, so the
 * field is reached through an accessor and a key has to come back out of the {@code Holder}. 26.3
 * keeps that shape. Nothing AC itself registers matches the scan from 26.2 up — it supplies its own
 * {@code ACBiomeConditionSource}, because vanilla's needs a {@code HolderGetter} this code path has
 * no way to obtain — but another mod registering through this manager still can, so the scan stays
 * rather than being stubbed.
 */
//? if >=26.3 {
/*import com.github.alexmodguy.alexscaves.citadel.Citadel;
import com.google.common.collect.ImmutableList;
import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.material.condition.BiomeCondition;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraft.world.level.levelgen.material.rule.ConditionRule;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.material.rule.SequenceRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurfaceRulesManager {
    private static final List<MaterialRule> OVERWORLD_REGISTRY = new ArrayList();
    private static final List<MaterialRule> NETHER_REGISTRY = new ArrayList();
    private static final List<MaterialRule> END_REGISTRY = new ArrayList();
    private static final List<MaterialRule> CAVE_REGISTRY = new ArrayList();

    public SurfaceRulesManager() {
    }

    public static void registerOverworldSurfaceRule(MaterialCondition condition, MaterialRule rule) {
        registerOverworldSurfaceRule(MaterialRules.ifTrue(condition, rule));
    }

    public static void registerOverworldSurfaceRule(MaterialRule rule) {
        OVERWORLD_REGISTRY.add(rule);
    }

    @Deprecated
    public static void registerNetherSurfaceRule(MaterialCondition condition, MaterialRule rule) {
        registerNetherSurfaceRule(MaterialRules.ifTrue(condition, rule));
    }

    @Deprecated
    public static void registerNetherSurfaceRule(MaterialRule rule) {
        NETHER_REGISTRY.add(rule);
    }

    @Deprecated
    public static void registerEndSurfaceRule(MaterialCondition condition, MaterialRule rule) {
        registerEndSurfaceRule(MaterialRules.ifTrue(condition, rule));
    }

    @Deprecated
    public static void registerEndSurfaceRule(MaterialRule rule) {
        END_REGISTRY.add(rule);
    }

    @Deprecated
    public static void registerCaveSurfaceRule(MaterialCondition condition, MaterialRule rule) {
        registerCaveSurfaceRule(MaterialRules.ifTrue(condition, rule));
    }

    @Deprecated
    public static void registerCaveSurfaceRule(MaterialRule rule) {
        CAVE_REGISTRY.add(rule);
    }

    public static boolean hasOverworldModifications(){
        return !OVERWORLD_REGISTRY.isEmpty();
    }

    public static MaterialRule mergeOverworldRules(MaterialRule rulesIn) {
        Citadel.LOGGER.info("merged {} surface rules with vanilla rule {}", OVERWORLD_REGISTRY.size(), rulesIn.getClass().getSimpleName());
        return mergeRules(rulesIn, MaterialRules.sequence(OVERWORLD_REGISTRY.toArray(MaterialRule[]::new)));
    }

    // Needed for terrablender compatibility.
    public static Map<String, MaterialRule> getOverworldRulesByBiomeForTerrablender(boolean vanilla) {
        Map<String, MaterialRule> map = new HashMap<>();
        for (MaterialRule ruleSource : OVERWORLD_REGISTRY) {
            if (ruleSource instanceof ConditionRule testRuleSource && testRuleSource.ifTrue() instanceof BiomeCondition biomeRule && biomeRule.biomes().size() > 0) {
                String namespace = biomeRule.biomes().get(0).unwrapKey().orElseThrow().location().getNamespace();
                boolean vanillaBiome = namespace.equals("minecraft");

                if (vanilla && vanillaBiome) {
                    map.put(namespace, testRuleSource);
                }
                if (!vanilla && !vanillaBiome) {
                    if (map.containsKey(namespace)) {
                        MaterialRule ruleSource1 = map.get(namespace);
                        if (ruleSource1 instanceof SequenceRule sequenceRuleSource) {
                            ImmutableList.Builder<MaterialRule> ruleSources = ImmutableList.builder();
                            ruleSources.addAll(sequenceRuleSource.sequence());
                            ruleSources.add(testRuleSource);
                            map.put(namespace, MaterialRules.sequence(ruleSources.build().toArray(MaterialRule[]::new)));
                        } else {
                            map.put(namespace, MaterialRules.sequence(ruleSource1, testRuleSource));
                        }
                    } else {
                        map.put(namespace, testRuleSource);
                    }
                }
            }
        }
        return map;
    }


    private static MaterialRule mergeRules(MaterialRule prev, MaterialRule toMerge) {
        CitadelSurfaceRuleWrapper result;
        if (prev instanceof CitadelSurfaceRuleWrapper wrapper) {
            result = new CitadelSurfaceRuleWrapper(wrapper.vanillaRules(), toMerge);
        } else {
            result = new CitadelSurfaceRuleWrapper(prev, toMerge);
        }
        Citadel.LOGGER.debug("surface rule recursive depth: {}", calculateSurfaceRuleDepth(result, 1));
        return result;
    }

    private static int calculateSurfaceRuleDepth(MaterialRule source, int depthIn) {
        if (source instanceof SequenceRule sequenceRuleSource) {
            int j = depthIn;
            for (MaterialRule ruleSource : sequenceRuleSource.sequence()) {
                j = Math.max(calculateSurfaceRuleDepth(ruleSource, depthIn + 1), j);
            }
            return j;
        } else if (source instanceof ConditionRule testRuleSource) {
            depthIn = Math.max(calculateSurfaceRuleDepth(testRuleSource.thenRun(), depthIn + 1), depthIn);
        } else if (source instanceof CitadelSurfaceRuleWrapper citadelSurfaceRuleWrapper) {
            depthIn = Math.max(calculateSurfaceRuleDepth(citadelSurfaceRuleWrapper.vanillaRules(), depthIn + 1), depthIn);
        }
        return depthIn;
    }
}
*///?} elif >=26.2 {
/*import com.github.alexmodguy.alexscaves.citadel.Citadel;
import com.google.common.collect.ImmutableList;
import net.minecraft.world.level.levelgen.SurfaceRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurfaceRulesManager {
    private static final List<SurfaceRules.RuleSource> OVERWORLD_REGISTRY = new ArrayList();
    private static final List<SurfaceRules.RuleSource> NETHER_REGISTRY = new ArrayList();
    private static final List<SurfaceRules.RuleSource> END_REGISTRY = new ArrayList();
    private static final List<SurfaceRules.RuleSource> CAVE_REGISTRY = new ArrayList();

    public SurfaceRulesManager() {
    }

    public static void registerOverworldSurfaceRule(SurfaceRules.ConditionSource condition, SurfaceRules.RuleSource rule) {
        registerOverworldSurfaceRule(SurfaceRules.ifTrue(condition, rule));
    }

    public static void registerOverworldSurfaceRule(SurfaceRules.RuleSource rule) {
        OVERWORLD_REGISTRY.add(rule);
    }

    @Deprecated
    public static void registerNetherSurfaceRule(SurfaceRules.ConditionSource condition, SurfaceRules.RuleSource rule) {
        registerNetherSurfaceRule(SurfaceRules.ifTrue(condition, rule));
    }

    @Deprecated
    public static void registerNetherSurfaceRule(SurfaceRules.RuleSource rule) {
        NETHER_REGISTRY.add(rule);
    }

    @Deprecated
    public static void registerEndSurfaceRule(SurfaceRules.ConditionSource condition, SurfaceRules.RuleSource rule) {
        registerEndSurfaceRule(SurfaceRules.ifTrue(condition, rule));
    }

    @Deprecated
    public static void registerEndSurfaceRule(SurfaceRules.RuleSource rule) {
        END_REGISTRY.add(rule);
    }

    @Deprecated
    public static void registerCaveSurfaceRule(SurfaceRules.ConditionSource condition, SurfaceRules.RuleSource rule) {
        registerCaveSurfaceRule(SurfaceRules.ifTrue(condition, rule));
    }

    @Deprecated
    public static void registerCaveSurfaceRule(SurfaceRules.RuleSource rule) {
        CAVE_REGISTRY.add(rule);
    }

    public static boolean hasOverworldModifications(){
        return !OVERWORLD_REGISTRY.isEmpty();
    }

    public static SurfaceRules.RuleSource mergeOverworldRules(SurfaceRules.RuleSource rulesIn) {
        Citadel.LOGGER.info("merged {} surface rules with vanilla rule {}", OVERWORLD_REGISTRY.size(), rulesIn.getClass().getSimpleName());
        return mergeRules(rulesIn, SurfaceRules.sequence(OVERWORLD_REGISTRY.toArray(SurfaceRules.RuleSource[]::new)));
    }

    // Needed for terrablender compatibility.
    public static Map<String, SurfaceRules.RuleSource> getOverworldRulesByBiomeForTerrablender(boolean vanilla) {
        Map<String, SurfaceRules.RuleSource> map = new HashMap<>();
        for (SurfaceRules.RuleSource ruleSource : OVERWORLD_REGISTRY) {
            if (ruleSource instanceof SurfaceRules.TestRuleSource testRuleSource && testRuleSource.ifTrue() instanceof SurfaceRules.BiomeConditionSource biomeRule && biomeRule.biomes().size() > 0) {
                String namespace = biomeRule.biomes().get(0).unwrapKey().orElseThrow().location().getNamespace();
                boolean vanillaBiome = namespace.equals("minecraft");

                if (vanilla && vanillaBiome) {
                    map.put(namespace, testRuleSource);
                }
                if (!vanilla && !vanillaBiome) {
                    if (map.containsKey(namespace)) {
                        SurfaceRules.RuleSource ruleSource1 = map.get(namespace);
                        if (ruleSource1 instanceof SurfaceRules.SequenceRuleSource sequenceRuleSource) {
                            ImmutableList.Builder<SurfaceRules.RuleSource> ruleSources = ImmutableList.builder();
                            ruleSources.addAll(sequenceRuleSource.sequence());
                            ruleSources.add(testRuleSource);
                            map.put(namespace, SurfaceRules.sequence(ruleSources.build().toArray(SurfaceRules.RuleSource[]::new)));
                        } else {
                            map.put(namespace, SurfaceRules.sequence(ruleSource1, testRuleSource));
                        }
                    } else {
                        map.put(namespace, testRuleSource);
                    }
                }
            }
        }
        return map;
    }


    private static SurfaceRules.RuleSource mergeRules(SurfaceRules.RuleSource prev, SurfaceRules.RuleSource toMerge) {
        CitadelSurfaceRuleWrapper result;
        if (prev instanceof CitadelSurfaceRuleWrapper wrapper) {
            result = new CitadelSurfaceRuleWrapper(wrapper.vanillaRules(), toMerge);
        } else {
            result = new CitadelSurfaceRuleWrapper(prev, toMerge);
        }
        Citadel.LOGGER.debug("surface rule recursive depth: {}", calculateSurfaceRuleDepth(result, 1));
        return result;
    }

    private static int calculateSurfaceRuleDepth(SurfaceRules.RuleSource source, int depthIn) {
        if (source instanceof SurfaceRules.SequenceRuleSource sequenceRuleSource) {
            int j = depthIn;
            for (SurfaceRules.RuleSource ruleSource : sequenceRuleSource.sequence()) {
                j = Math.max(calculateSurfaceRuleDepth(ruleSource, depthIn + 1), j);
            }
            return j;
        } else if (source instanceof SurfaceRules.TestRuleSource testRuleSource) {
            depthIn = Math.max(calculateSurfaceRuleDepth(testRuleSource.thenRun(), depthIn + 1), depthIn);
        } else if (source instanceof CitadelSurfaceRuleWrapper citadelSurfaceRuleWrapper) {
            depthIn = Math.max(calculateSurfaceRuleDepth(citadelSurfaceRuleWrapper.vanillaRules(), depthIn + 1), depthIn);
        }
        return depthIn;
    }
}
*///?} else {
import com.github.alexmodguy.alexscaves.citadel.Citadel;
import com.google.common.collect.ImmutableList;
import net.minecraft.world.level.levelgen.SurfaceRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurfaceRulesManager {
    private static final List<SurfaceRules.RuleSource> OVERWORLD_REGISTRY = new ArrayList();
    private static final List<SurfaceRules.RuleSource> NETHER_REGISTRY = new ArrayList();
    private static final List<SurfaceRules.RuleSource> END_REGISTRY = new ArrayList();
    private static final List<SurfaceRules.RuleSource> CAVE_REGISTRY = new ArrayList();

    public SurfaceRulesManager() {
    }

    public static void registerOverworldSurfaceRule(SurfaceRules.ConditionSource condition, SurfaceRules.RuleSource rule) {
        registerOverworldSurfaceRule(SurfaceRules.ifTrue(condition, rule));
    }

    public static void registerOverworldSurfaceRule(SurfaceRules.RuleSource rule) {
        OVERWORLD_REGISTRY.add(rule);
    }

    @Deprecated
    public static void registerNetherSurfaceRule(SurfaceRules.ConditionSource condition, SurfaceRules.RuleSource rule) {
        registerNetherSurfaceRule(SurfaceRules.ifTrue(condition, rule));
    }

    @Deprecated
    public static void registerNetherSurfaceRule(SurfaceRules.RuleSource rule) {
        NETHER_REGISTRY.add(rule);
    }

    @Deprecated
    public static void registerEndSurfaceRule(SurfaceRules.ConditionSource condition, SurfaceRules.RuleSource rule) {
        registerEndSurfaceRule(SurfaceRules.ifTrue(condition, rule));
    }

    @Deprecated
    public static void registerEndSurfaceRule(SurfaceRules.RuleSource rule) {
        END_REGISTRY.add(rule);
    }

    @Deprecated
    public static void registerCaveSurfaceRule(SurfaceRules.ConditionSource condition, SurfaceRules.RuleSource rule) {
        registerCaveSurfaceRule(SurfaceRules.ifTrue(condition, rule));
    }

    @Deprecated
    public static void registerCaveSurfaceRule(SurfaceRules.RuleSource rule) {
        CAVE_REGISTRY.add(rule);
    }

    public static boolean hasOverworldModifications(){
        return !OVERWORLD_REGISTRY.isEmpty();
    }

    public static SurfaceRules.RuleSource mergeOverworldRules(SurfaceRules.RuleSource rulesIn) {
        Citadel.LOGGER.info("merged {} surface rules with vanilla rule {}", OVERWORLD_REGISTRY.size(), rulesIn.getClass().getSimpleName());
        return mergeRules(rulesIn, SurfaceRules.sequence(OVERWORLD_REGISTRY.toArray(SurfaceRules.RuleSource[]::new)));
    }

    // Needed for terrablender compatibility.
    public static Map<String, SurfaceRules.RuleSource> getOverworldRulesByBiomeForTerrablender(boolean vanilla) {
        Map<String, SurfaceRules.RuleSource> map = new HashMap<>();
        for (SurfaceRules.RuleSource ruleSource : OVERWORLD_REGISTRY) {
            if (ruleSource instanceof SurfaceRules.TestRuleSource testRuleSource && testRuleSource.ifTrue() instanceof SurfaceRules.BiomeConditionSource biomeRule && !biomeRule.biomes.isEmpty()) {
                String namespace = biomeRule.biomes.get(0).location().getNamespace();
                boolean vanillaBiome = namespace.equals("minecraft");

                if (vanilla && vanillaBiome) {
                    map.put(namespace, testRuleSource);
                }
                if (!vanilla && !vanillaBiome) {
                    if (map.containsKey(namespace)) {
                        SurfaceRules.RuleSource ruleSource1 = map.get(namespace);
                        if (ruleSource1 instanceof SurfaceRules.SequenceRuleSource sequenceRuleSource) {
                            ImmutableList.Builder<SurfaceRules.RuleSource> ruleSources = ImmutableList.builder();
                            ruleSources.addAll(sequenceRuleSource.sequence());
                            ruleSources.add(testRuleSource);
                            map.put(namespace, SurfaceRules.sequence(ruleSources.build().toArray(SurfaceRules.RuleSource[]::new)));
                        } else {
                            map.put(namespace, SurfaceRules.sequence(ruleSource1, testRuleSource));
                        }
                    } else {
                        map.put(namespace, testRuleSource);
                    }
                }
            }
        }
        return map;
    }


    private static SurfaceRules.RuleSource mergeRules(SurfaceRules.RuleSource prev, SurfaceRules.RuleSource toMerge) {
        CitadelSurfaceRuleWrapper result;
        if (prev instanceof CitadelSurfaceRuleWrapper wrapper) {
            result = new CitadelSurfaceRuleWrapper(wrapper.vanillaRules(), toMerge);
        } else {
            result = new CitadelSurfaceRuleWrapper(prev, toMerge);
        }
        Citadel.LOGGER.debug("surface rule recursive depth: {}", calculateSurfaceRuleDepth(result, 1));
        return result;
    }

    private static int calculateSurfaceRuleDepth(SurfaceRules.RuleSource source, int depthIn) {
        if (source instanceof SurfaceRules.SequenceRuleSource sequenceRuleSource) {
            int j = depthIn;
            for (SurfaceRules.RuleSource ruleSource : sequenceRuleSource.sequence()) {
                j = Math.max(calculateSurfaceRuleDepth(ruleSource, depthIn + 1), j);
            }
            return j;
        } else if (source instanceof SurfaceRules.TestRuleSource testRuleSource) {
            depthIn = Math.max(calculateSurfaceRuleDepth(testRuleSource.thenRun(), depthIn + 1), depthIn);
        } else if (source instanceof CitadelSurfaceRuleWrapper citadelSurfaceRuleWrapper) {
            depthIn = Math.max(calculateSurfaceRuleDepth(citadelSurfaceRuleWrapper.vanillaRules(), depthIn + 1), depthIn);
        }
        return depthIn;
    }
}
//?}
