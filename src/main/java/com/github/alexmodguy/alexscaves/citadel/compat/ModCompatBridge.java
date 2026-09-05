package com.github.alexmodguy.alexscaves.citadel.compat;

import codx.codxlib.api.CodxLib;
import com.github.alexmodguy.alexscaves.citadel.Citadel;
import com.github.alexmodguy.alexscaves.citadel.server.generation.SurfaceRulesManager;
import net.minecraft.world.level.levelgen.SurfaceRules;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * TerraBlender hand-off for the vendored surface-rule system.
 *
 * <p>TerraBlender replaces the overworld chunk generator's rule source wholesale, so the
 * {@code NoiseGeneratorSettings} mixin path would be thrown away when it is installed; the rules
 * have to be handed to TerraBlender's own registry instead. Upstream Citadel did this against the
 * compiled {@code terrablender.api.SurfaceRuleManager}. This tree has no TerraBlender on the
 * compile classpath and is not going to add one for two calls, so the same hand-off is done
 * reflectively — a missing or renamed API degrades to a logged warning instead of a hard
 * {@code NoClassDefFoundError} at load.
 *
 * <p>Must run after every mod has loaded (Alex's Caves calls it from {@code FMLLoadCompleteEvent}),
 * because the rules themselves are contributed during common setup.
 */
public class ModCompatBridge {

    private static boolean terrablender;

    private ModCompatBridge() {
    }

    public static void afterAllModsLoaded() {
        if (!CodxLib.isModLoaded("terrablender")) {
            return;
        }
        Citadel.LOGGER.info("adding surface rules via terrablender...");
        try {
            Class<?> manager = Class.forName("terrablender.api.SurfaceRuleManager");
            Class<?> categoryType = Class.forName("terrablender.api.SurfaceRuleManager$RuleCategory");
            Class<?> stageType = Class.forName("terrablender.api.SurfaceRuleManager$RuleStage");
            Object overworld = enumValue(categoryType, "OVERWORLD");
            Object beforeBedrock = enumValue(stageType, "BEFORE_BEDROCK");

            // TerraBlender 26.2.0.0.2 replaced the RuleSource parameter of both entry points with a
            // RuleBuilder (a Function<HolderGetter<Biome>, RuleSource>) so that rules can be rebuilt
            // against a live registry. Try the pre-26.2 signature first, then the builder one; the
            // builder is supplied as a proxy, since the interface is not on this tree's classpath.
            Class<?> ruleBuilderType = null;
            Method addToDefaults;
            Method addRules;
            try {
                addToDefaults = manager.getMethod("addToDefaultSurfaceRulesAtStage", categoryType, stageType, int.class, SurfaceRules.RuleSource.class);
                addRules = manager.getMethod("addSurfaceRules", categoryType, String.class, SurfaceRules.RuleSource.class);
            } catch (NoSuchMethodException noLegacyApi) {
                ruleBuilderType = Class.forName("terrablender.api.SurfaceRuleManager$RuleBuilder");
                addToDefaults = manager.getMethod("addToDefaultSurfaceRulesAtStage", categoryType, stageType, int.class, ruleBuilderType);
                addRules = manager.getMethod("addSurfaceRules", categoryType, String.class, ruleBuilderType);
            }

            Map<String, SurfaceRules.RuleSource> vanillaBiomeRules = SurfaceRulesManager.getOverworldRulesByBiomeForTerrablender(true);
            for (Map.Entry<String, SurfaceRules.RuleSource> entry : vanillaBiomeRules.entrySet()) {
                addToDefaults.invoke(null, overworld, beforeBedrock, 0, ruleArgument(ruleBuilderType, entry.getValue()));
            }
            Citadel.LOGGER.info("Added {} vanilla biome surface rule types via terrablender", vanillaBiomeRules.size());

            Map<String, SurfaceRules.RuleSource> moddedBiomeRules = SurfaceRulesManager.getOverworldRulesByBiomeForTerrablender(false);
            for (Map.Entry<String, SurfaceRules.RuleSource> entry : moddedBiomeRules.entrySet()) {
                addRules.invoke(null, overworld, entry.getKey(), ruleArgument(ruleBuilderType, entry.getValue()));
            }
            Citadel.LOGGER.info("Added {} modded biome surface rule types via terrablender", moddedBiomeRules.size());

            terrablender = true;
        } catch (ReflectiveOperationException e) {
            Citadel.LOGGER.warn("TerraBlender is installed but its surface rule API could not be reached; "
                    + "falling back to the built-in surface rule merge, which TerraBlender may override.", e);
        }
    }

    /**
     * The rule source itself on the pre-26.2 API, or a {@code RuleBuilder} handing it back on the
     * newer one. The rules this tree contributes are already fully built, so the builder ignores the
     * registry it is handed.
     */
    private static Object ruleArgument(Class<?> ruleBuilderType, SurfaceRules.RuleSource rule) {
        if (ruleBuilderType == null) {
            return rule;
        }
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "apply":
                    return rule;
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "AlexsCavesRuleBuilder[" + rule + "]";
                default:
                    if (method.isDefault()) {
                        return InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    throw new UnsupportedOperationException(method.toString());
            }
        };
        return Proxy.newProxyInstance(ruleBuilderType.getClassLoader(), new Class<?>[]{ruleBuilderType}, handler);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(Class<?> type, String name) {
        return Enum.valueOf((Class<Enum>) type, name);
    }

    public static boolean usingTerrablender() {
        return terrablender;
    }
}
