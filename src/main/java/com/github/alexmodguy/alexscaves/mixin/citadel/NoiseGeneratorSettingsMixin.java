package com.github.alexmodguy.alexscaves.mixin.citadel;

/**
 * Swaps the dimension's surface rule source for the Citadel-merged one, and swaps it back out while
 * level data is being written so the internal wrapper never reaches disk.
 *
 * <p>Two arms. On 26.3 the record component this shadows was renamed {@code surfaceRule} →
 * {@code materialRule} and retyped from a bare rule source to a {@code Holder<MaterialRule>}, so the
 * shadow, the injected method name, the callback's type parameter and every {@code @Unique} field
 * move together — a mixin names its target by name and descriptor, so none of that can be a
 * {@code replacements.string} rule. The merge itself still works on the rule, not the holder, which
 * is why the 26.3 arm unwraps with {@code value()} and re-wraps with {@code Holder.direct}.
 */
//? if >=26.3 {
/*import com.github.alexmodguy.alexscaves.citadel.Citadel;
import com.github.alexmodguy.alexscaves.citadel.compat.ModCompatBridge;
import com.github.alexmodguy.alexscaves.citadel.server.generation.NoiseGeneratorSettingsAccessor;
import com.github.alexmodguy.alexscaves.citadel.server.generation.SurfaceRulesManager;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NoiseGeneratorSettings.class, priority = 300)
public class NoiseGeneratorSettingsMixin implements NoiseGeneratorSettingsAccessor {

    @Mutable
    @Final
    @Shadow
    private Holder<MaterialRule> materialRule;

    @Unique
    private Holder<MaterialRule> unmodifiedSurfaceRule;
    @Unique
    private Holder<MaterialRule> citadelSurfaceRule = null;
    @Unique
    private boolean hasModifiedRules = false;
    @Unique
    private boolean saving = false;
    @Unique
    private boolean requiresSurfaceRuleSwapping = false;
    @Unique
    private Holder<MaterialRule> swapSurfaceRule = null;


   @Inject(method = "materialRule", at = @At("HEAD"), cancellable = true)
    private void acc_citadel_surfaceRule(CallbackInfoReturnable<Holder<MaterialRule>> cir) {
        if (!hasModifiedRules && !saving && !ModCompatBridge.usingTerrablender()) { // initialized
            this.unmodifiedSurfaceRule = materialRule;
            if(SurfaceRulesManager.hasOverworldModifications()){
                this.requiresSurfaceRuleSwapping = true;
                this.citadelSurfaceRule = Holder.direct(SurfaceRulesManager.mergeOverworldRules(materialRule.value()));
                this.materialRule = citadelSurfaceRule;
            }else{
                Citadel.LOGGER.info("vanilla surface rule behavior unchanged");
                this.requiresSurfaceRuleSwapping = false;
            }
            this.hasModifiedRules = true;
        }
        if(this.hasModifiedRules && this.requiresSurfaceRuleSwapping){
            cir.setReturnValue(this.materialRule);
        }
    }

    @Override
    public void acOnSaveData(boolean saving) {
        this.saving = saving;
        if(!ModCompatBridge.usingTerrablender() && this.requiresSurfaceRuleSwapping){
            if(this.hasModifiedRules){
                if(saving){
                    this.swapSurfaceRule = this.materialRule;
                    this.materialRule = this.unmodifiedSurfaceRule;
                    Citadel.LOGGER.debug("saving unmodified surface rules as type {}", materialRule.value().getClass().getSimpleName());
                }else{
                    this.materialRule = this.swapSurfaceRule == null ? this.citadelSurfaceRule : this.swapSurfaceRule;
                    Citadel.LOGGER.debug("modified surface rules to type {}", materialRule.value().getClass().getSimpleName());
                }
            }
        }
    }
}
*///?} else {
import com.github.alexmodguy.alexscaves.citadel.Citadel;
import com.github.alexmodguy.alexscaves.citadel.compat.ModCompatBridge;
import com.github.alexmodguy.alexscaves.citadel.server.generation.NoiseGeneratorSettingsAccessor;
import com.github.alexmodguy.alexscaves.citadel.server.generation.SurfaceRulesManager;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NoiseGeneratorSettings.class, priority = 300)
public class NoiseGeneratorSettingsMixin implements NoiseGeneratorSettingsAccessor {

    @Mutable
    @Final
    @Shadow
    private SurfaceRules.RuleSource surfaceRule;

    @Unique
    private SurfaceRules.RuleSource unmodifiedSurfaceRule;
    @Unique
    private SurfaceRules.RuleSource citadelSurfaceRule = null;
    @Unique
    private boolean hasModifiedRules = false;
    @Unique
    private boolean saving = false;
    @Unique
    private boolean requiresSurfaceRuleSwapping = false;
    @Unique
    private SurfaceRules.RuleSource swapSurfaceRule = null;


   @Inject(method = "surfaceRule", at = @At("HEAD"), cancellable = true)
    private void acc_citadel_surfaceRule(CallbackInfoReturnable<SurfaceRules.RuleSource> cir) {
        if (!hasModifiedRules && !saving && !ModCompatBridge.usingTerrablender()) { // initialized
            this.unmodifiedSurfaceRule = surfaceRule;
            if(SurfaceRulesManager.hasOverworldModifications()){
                this.requiresSurfaceRuleSwapping = true;
                this.citadelSurfaceRule = SurfaceRulesManager.mergeOverworldRules(surfaceRule);
                this.surfaceRule = citadelSurfaceRule;
            }else{
                Citadel.LOGGER.info("vanilla surface rule behavior unchanged");
                this.requiresSurfaceRuleSwapping = false;
            }
            this.hasModifiedRules = true;
        }
        if(this.hasModifiedRules && this.requiresSurfaceRuleSwapping){
            cir.setReturnValue(this.surfaceRule);
        }
    }

    @Override
    public void acOnSaveData(boolean saving) {
        this.saving = saving;
        if(!ModCompatBridge.usingTerrablender() && this.requiresSurfaceRuleSwapping){
            if(this.hasModifiedRules){
                if(saving){
                    this.swapSurfaceRule = this.surfaceRule;
                    this.surfaceRule = this.unmodifiedSurfaceRule;
                    Citadel.LOGGER.debug("saving unmodified surface rules as type {}", surfaceRule.getClass().getSimpleName());
                }else{
                    this.surfaceRule = this.swapSurfaceRule == null ? this.citadelSurfaceRule : this.swapSurfaceRule;
                    Citadel.LOGGER.debug("modified surface rules to type {}", surfaceRule.getClass().getSimpleName());
                }
            }
        }
    }
}
//?}
