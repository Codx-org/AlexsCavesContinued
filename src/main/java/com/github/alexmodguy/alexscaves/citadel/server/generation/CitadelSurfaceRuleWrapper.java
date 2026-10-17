package com.github.alexmodguy.alexscaves.citadel.server.generation;

/**
 * Merges Citadel's contributed surface rules with whatever rule source the dimension already had,
 * preferring the Citadel answer and falling back to vanilla's for any column it declines.
 *
 * <p>This file is three complete arms rather than one class with gated members, because 26.3
 * deleted {@code SurfaceRules} outright. The whole material-rule API moved out of that one holder
 * class into three packages — {@code levelgen.material}, {@code levelgen.material.rule} and
 * {@code levelgen.material.condition} — and every type in it was renamed: {@code RuleSource} is now
 * {@code MaterialRule}, {@code SurfaceRule} is {@code RuleEvaluator}, {@code Context} is
 * {@code MaterialRuleContext}, and {@code apply} is {@code compile}. A {@code replacements.string}
 * rule cannot express that: {@code SurfaceRules.Condition} is a strict prefix of
 * {@code SurfaceRules.ConditionSource} and matching is boundary-checked on neither edge, so the two
 * rules would collide at the same offset; and the method rename moves the name, the parameter type
 * and the return type together. So the arms carry the spellings literally.
 *
 * <p>The middle arm exists because 26.2 had already unwrapped the dispatch codec — {@code codec()}
 * returns the {@code MapCodec} directly rather than the {@code KeyDispatchDataCodec} holder. On 26.3
 * the holder class {@code net.minecraft.util.KeyDispatchDataCodec} is gone from the jar entirely, so
 * {@code CODEC} is a bare {@code MapCodec} there. That is why {@code Citadel#registerModBus}
 * registers this codec as {@code () -> CODEC} on 26.3 and {@code CODEC::codec} below it.
 */
//? if >=26.3 {
/*import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.material.rule.RuleEvaluator;

public record CitadelSurfaceRuleWrapper(MaterialRule vanillaRules,
                                        MaterialRule citadelRules) implements MaterialRule {

    public static final MapCodec<CitadelSurfaceRuleWrapper> CODEC = RecordCodecBuilder.mapCodec((builder) -> builder.group(MaterialRule.DIRECT_CODEC.fieldOf("vanilla_rules").forGetter(CitadelSurfaceRuleWrapper::vanillaRules), MaterialRule.DIRECT_CODEC.fieldOf("citadel_rules").forGetter(CitadelSurfaceRuleWrapper::citadelRules)).apply(builder, CitadelSurfaceRuleWrapper::new));

    @Override
    public MapCodec<? extends MaterialRule> codec() {
        return CODEC;
    }

    @Override
    public RuleEvaluator compile(MaterialRuleContext context) {
        if(vanillaRules == null){
            return this.citadelRules.compile(context);
        }else if(citadelRules == null){
            return vanillaRules.compile(context);
        }
        return new CitadelSurfaceRule(context, this.vanillaRules.compile(context), this.citadelRules.compile(context));
    }

    record CitadelSurfaceRule(MaterialRuleContext context, RuleEvaluator vanillaRule,
                              RuleEvaluator citadelRule) implements RuleEvaluator {
        public BlockState tryApply(int x, int y, int z) {
            BlockState citadelState = this.citadelRule.tryApply(x, y, z);
            return citadelState == null ? this.vanillaRule.tryApply(x, y, z) : citadelState;
        }
    }
}
*///?} elif >=26.2 {
/*import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;

public record CitadelSurfaceRuleWrapper(SurfaceRules.RuleSource vanillaRules,
                                        SurfaceRules.RuleSource citadelRules) implements SurfaceRules.RuleSource {

    public static final KeyDispatchDataCodec<CitadelSurfaceRuleWrapper> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((builder) -> builder.group(SurfaceRules.RuleSource.CODEC.fieldOf("vanilla_rules").forGetter(CitadelSurfaceRuleWrapper::vanillaRules), SurfaceRules.RuleSource.CODEC.fieldOf("citadel_rules").forGetter(CitadelSurfaceRuleWrapper::citadelRules)).apply(builder, CitadelSurfaceRuleWrapper::new)));

    @Override
    public MapCodec<? extends SurfaceRules.RuleSource> codec() {
        return CODEC.codec();
    }

    @Override
    public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
        if(vanillaRules == null){
            return this.citadelRules.apply(context);
        }else if(citadelRules == null){
            return vanillaRules.apply(context);
        }
        return new CitadelSurfaceRule(context, this.vanillaRules.apply(context), this.citadelRules.apply(context));
    }

    record CitadelSurfaceRule(SurfaceRules.Context context, SurfaceRules.SurfaceRule vanillaRule,
                              SurfaceRules.SurfaceRule citadelRule) implements SurfaceRules.SurfaceRule {
        public BlockState tryApply(int x, int y, int z) {
            BlockState citadelState = this.citadelRule.tryApply(x, y, z);
            return citadelState == null ? this.vanillaRule.tryApply(x, y, z) : citadelState;
        }
    }
}
*///?} else {
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;

public record CitadelSurfaceRuleWrapper(SurfaceRules.RuleSource vanillaRules,
                                        SurfaceRules.RuleSource citadelRules) implements SurfaceRules.RuleSource {

    public static final KeyDispatchDataCodec<CitadelSurfaceRuleWrapper> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((builder) -> builder.group(SurfaceRules.RuleSource.CODEC.fieldOf("vanilla_rules").forGetter(CitadelSurfaceRuleWrapper::vanillaRules), SurfaceRules.RuleSource.CODEC.fieldOf("citadel_rules").forGetter(CitadelSurfaceRuleWrapper::citadelRules)).apply(builder, CitadelSurfaceRuleWrapper::new)));

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() {
        return CODEC;
    }

    @Override
    public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
        if(vanillaRules == null){
            return this.citadelRules.apply(context);
        }else if(citadelRules == null){
            return vanillaRules.apply(context);
        }
        return new CitadelSurfaceRule(context, this.vanillaRules.apply(context), this.citadelRules.apply(context));
    }

    record CitadelSurfaceRule(SurfaceRules.Context context, SurfaceRules.SurfaceRule vanillaRule,
                              SurfaceRules.SurfaceRule citadelRule) implements SurfaceRules.SurfaceRule {
        public BlockState tryApply(int x, int y, int z) {
            BlockState citadelState = this.citadelRule.tryApply(x, y, z);
            return citadelState == null ? this.vanillaRule.tryApply(x, y, z) : citadelState;
        }
    }
}
//?}
