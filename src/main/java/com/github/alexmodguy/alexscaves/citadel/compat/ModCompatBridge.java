package com.github.alexmodguy.alexscaves.citadel.compat;

import codx.codxlib.api.CodxLib;
import com.github.alexmodguy.alexscaves.citadel.Citadel;
import com.github.alexmodguy.alexscaves.citadel.server.generation.SurfaceRulesManager;
import net.minecraft.world.level.levelgen.SurfaceRules;

import java.lang.reflect.Method;
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
            Method addToDefaults = manager.getMethod("addToDefaultSurfaceRulesAtStage", categoryType, stageType, int.class, SurfaceRules.RuleSource.class);
            Method addRules = manager.getMethod("addSurfaceRules", categoryType, String.class, SurfaceRules.RuleSource.class);

            Map<String, SurfaceRules.RuleSource> vanillaBiomeRules = SurfaceRulesManager.getOverworldRulesByBiomeForTerrablender(true);
            for (Map.Entry<String, SurfaceRules.RuleSource> entry : vanillaBiomeRules.entrySet()) {
                addToDefaults.invoke(null, overworld, beforeBedrock, 0, entry.getValue());
            }
            Citadel.LOGGER.info("Added {} vanilla biome surface rule types via terrablender", vanillaBiomeRules.size());

            Map<String, SurfaceRules.RuleSource> moddedBiomeRules = SurfaceRulesManager.getOverworldRulesByBiomeForTerrablender(false);
            for (Map.Entry<String, SurfaceRules.RuleSource> entry : moddedBiomeRules.entrySet()) {
                addRules.invoke(null, overworld, entry.getKey(), entry.getValue());
            }
            Citadel.LOGGER.info("Added {} modded biome surface rule types via terrablender", moddedBiomeRules.size());

            terrablender = true;
        } catch (ReflectiveOperationException e) {
            Citadel.LOGGER.warn("TerraBlender is installed but its surface rule API could not be reached; "
                    + "falling back to the built-in surface rule merge, which TerraBlender may override.", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(Class<?> type, String name) {
        return Enum.valueOf((Class<Enum>) type, name);
    }

    public static boolean usingTerrablender() {
        return terrablender;
    }
}
