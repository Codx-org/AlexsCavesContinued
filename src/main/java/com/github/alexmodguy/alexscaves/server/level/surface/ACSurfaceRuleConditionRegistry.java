package com.github.alexmodguy.alexscaves.server.level.surface;

/**
 * The mod's two surface-rule condition sources: a 3D simplex mask, and an "is this one of these
 * biomes" test that holds {@code ResourceKey}s rather than resolved {@code Holder}s.
 *
 * <p>Four arms, one per era of this API:
 *
 * <ul>
 * <li><b>&lt;1.20.5</b> — the registry holds plain {@code Codec}s and {@code codec()} returns the
 *     {@code KeyDispatchDataCodec} holder.</li>
 * <li><b>&gt;=1.20.5</b> — the MATERIAL_RULE / MATERIAL_CONDITION registries were retyped from
 *     {@code Codec} to {@code MapCodec}.</li>
 * <li><b>&gt;=26.2</b> — {@code codec()} returns the {@code MapCodec} directly, and
 *     {@code SurfaceRules#isBiome} was rewritten to take a leading {@code HolderGetter<Biome>}: it
 *     resolves the keys eagerly into a {@code HolderSet} and vanilla's {@code BiomeConditionSource}
 *     then compares {@code Holder} IDENTITY against the context's possible-biomes set, so a
 *     fabricated standalone {@code Holder} can never match. There is no {@code HolderGetter}
 *     anywhere this mod builds its rules — {@code ACSurfaceRules.setup} runs at mod construction and
 *     the merge happens in {@code NoiseGeneratorSettingsMixin} — so from 26.2 the mod supplies its
 *     own key-comparing condition source. Below 26.2 it stays vanilla's, which keeps
 *     {@code SurfaceRulesManager}'s TerraBlender scan (it looks for a vanilla
 *     {@code BiomeConditionSource}) working.</li>
 * <li><b>&gt;=26.3</b> — {@code SurfaceRules} is gone: conditions are {@code MaterialCondition},
 *     their compiled form is {@code ConditionEvaluator}, {@code apply} is {@code compile}, the
 *     context is {@code MaterialRuleContext} and its {@code blockX}/{@code blockY}/{@code blockZ}
 *     became methods rather than public fields. {@code KeyDispatchDataCodec} does not exist at all,
 *     so each {@code CODEC} is a bare {@code MapCodec}. The registry also split in two — the
 *     codec-typed one is now {@code MATERIAL_CONDITION_TYPE}, while {@code MATERIAL_CONDITION} holds
 *     the conditions themselves. And the biome accessor is no longer needed: 26.3 made
 *     {@code MaterialRuleContext#getBiome()} public.</li>
 * </ul>
 *
 * <p>Both records are registered on every version so the codec is never a dangling dispatch key,
 * even on the versions where {@code isBiome} hands out vanilla's source instead.
 */
//? if >=26.3 {
/*import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.misc.ACMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.condition.ConditionEvaluator;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraftforge.registries.DeferredRegister;
import java.util.List;
import java.util.function.Supplier;

public class ACSurfaceRuleConditionRegistry {

    public static final DeferredRegister<MapCodec<? extends MaterialCondition>> DEF_REG = DeferredRegister.create(Registries.MATERIAL_CONDITION_TYPE, AlexsCaves.MODID);

    public static final Supplier<MapCodec<? extends MaterialCondition>> AC_SIMPLEX_CONDITION = DEF_REG.register("ac_simplex", () -> SimplexConditionSource.CODEC);

    public static final Supplier<MapCodec<? extends MaterialCondition>> AC_BIOME_CONDITION = DEF_REG.register("ac_biome", () -> ACBiomeConditionSource.CODEC);

    public static MaterialCondition isBiome(ResourceKey<Biome> biome) {
        return new ACBiomeConditionSource(List.of(biome));
    }

    public static MaterialCondition simplexCondition(float noiseMin, float noiseMax, float noiseScale, float yScale, int offsetType) {
        return new SimplexConditionSource(noiseMin, noiseMax, noiseScale, yScale, offsetType);
    }

    private record SimplexConditionSource(float noiseMin, float noiseMax, float noiseScale, float yScale,
                                          int offsetType) implements MaterialCondition {
        private static final MapCodec<SimplexConditionSource> CODEC = RecordCodecBuilder.mapCodec((group) -> {
            return group.group(Codec.floatRange(-1F, 1F).fieldOf("noise_min").forGetter(SimplexConditionSource::noiseMin), Codec.floatRange(-1F, 1F).fieldOf("noise_max").forGetter(SimplexConditionSource::noiseMax), Codec.floatRange(1F, 10000F).fieldOf("noise_scale").forGetter(SimplexConditionSource::noiseScale), Codec.floatRange(0F, 10000F).fieldOf("y_scale").forGetter(SimplexConditionSource::yScale), Codec.intRange(0, 128).fieldOf("offset_type").forGetter(SimplexConditionSource::offsetType)).apply(group, SimplexConditionSource::new);
        });

        public MapCodec<? extends MaterialCondition> codec() {
            return CODEC;
        }

        public ConditionEvaluator compile(final MaterialRuleContext contextIn) {
            class NoiseCondition implements ConditionEvaluator {

                private MaterialRuleContext context;

                NoiseCondition(MaterialRuleContext context) {
                    this.context = context;
                }

                public boolean test() {
                    double f = ACMath.sampleNoise3D(context.blockX() + (offsetType * 1000), (int) ((context.blockY() * yScale + offsetType * 2000)), context.blockZ() - (offsetType * 3000), SimplexConditionSource.this.noiseScale);
                    return f > SimplexConditionSource.this.noiseMin && f <= SimplexConditionSource.this.noiseMax;
                }
            }
            return new NoiseCondition(contextIn);
        }
    }

    private record ACBiomeConditionSource(List<ResourceKey<Biome>> biomes) implements MaterialCondition {
        private static final MapCodec<ACBiomeConditionSource> CODEC = RecordCodecBuilder.mapCodec((group) -> {
            return group.group(ResourceKey.codec(Registries.BIOME).listOf().fieldOf("biomes").forGetter(ACBiomeConditionSource::biomes)).apply(group, ACBiomeConditionSource::new);
        });

        public MapCodec<? extends MaterialCondition> codec() {
            return CODEC;
        }

        // Vanilla's own version caches the answer per Y; this one asks the context every test. The
        // biome cannot change within a column anyway, and AC only puts one of these at the head of
        // each of its six cave rule chains, so the extra lookup is a map hit in getBiome, which
        // memoises per position itself.
        public ConditionEvaluator compile(final MaterialRuleContext contextIn) {
            return new ConditionEvaluator() {
                public boolean test() {
                    Holder<Biome> biome = contextIn.getBiome();
                    for (ResourceKey<Biome> key : ACBiomeConditionSource.this.biomes) {
                        if (biome.is(key)) {
                            return true;
                        }
                    }
                    return false;
                }
            };
        }
    }
}
*///?} elif >=26.2 {
/*import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.misc.ACMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraftforge.registries.DeferredRegister;
import java.util.List;
import java.util.function.Supplier;

public class ACSurfaceRuleConditionRegistry {

    public static final DeferredRegister<MapCodec<? extends SurfaceRules.ConditionSource>> DEF_REG = DeferredRegister.create(Registries.MATERIAL_CONDITION, AlexsCaves.MODID);

    public static final Supplier<MapCodec<? extends SurfaceRules.ConditionSource>> AC_SIMPLEX_CONDITION = DEF_REG.register("ac_simplex", () -> SimplexConditionSource.CODEC.codec());

    public static final Supplier<MapCodec<? extends SurfaceRules.ConditionSource>> AC_BIOME_CONDITION = DEF_REG.register("ac_biome", () -> ACBiomeConditionSource.CODEC.codec());

    public static SurfaceRules.ConditionSource isBiome(ResourceKey<Biome> biome) {
        return new ACBiomeConditionSource(List.of(biome));
    }

    public static SurfaceRules.ConditionSource simplexCondition(float noiseMin, float noiseMax, float noiseScale, float yScale, int offsetType) {
        return new SimplexConditionSource(noiseMin, noiseMax, noiseScale, yScale, offsetType);
    }

    private record SimplexConditionSource(float noiseMin, float noiseMax, float noiseScale, float yScale,
                                          int offsetType) implements SurfaceRules.ConditionSource {
        private static final KeyDispatchDataCodec<SimplexConditionSource> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((group) -> {
            return group.group(Codec.floatRange(-1F, 1F).fieldOf("noise_min").forGetter(SimplexConditionSource::noiseMin), Codec.floatRange(-1F, 1F).fieldOf("noise_max").forGetter(SimplexConditionSource::noiseMax), Codec.floatRange(1F, 10000F).fieldOf("noise_scale").forGetter(SimplexConditionSource::noiseScale), Codec.floatRange(0F, 10000F).fieldOf("y_scale").forGetter(SimplexConditionSource::yScale), Codec.intRange(0, 128).fieldOf("offset_type").forGetter(SimplexConditionSource::offsetType)).apply(group, SimplexConditionSource::new);
        }));

        public MapCodec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC.codec();
        }

        public SurfaceRules.Condition apply(final SurfaceRules.Context contextIn) {
            class NoiseCondition implements SurfaceRules.Condition {

                private SurfaceRules.Context context;

                NoiseCondition(SurfaceRules.Context context) {
                    this.context = context;
                }

                public boolean test() {
                    double f = ACMath.sampleNoise3D(context.blockX + (offsetType * 1000), (int) ((context.blockY * yScale + offsetType * 2000)), context.blockZ - (offsetType * 3000), SimplexConditionSource.this.noiseScale);
                    return f > SimplexConditionSource.this.noiseMin && f <= SimplexConditionSource.this.noiseMax;
                }
            }
            return new NoiseCondition(contextIn);
        }
    }

    private record ACBiomeConditionSource(List<ResourceKey<Biome>> biomes) implements SurfaceRules.ConditionSource {
        private static final KeyDispatchDataCodec<ACBiomeConditionSource> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((group) -> {
            return group.group(ResourceKey.codec(Registries.BIOME).listOf().fieldOf("biomes").forGetter(ACBiomeConditionSource::biomes)).apply(group, ACBiomeConditionSource::new);
        }));

        public MapCodec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC.codec();
        }

        // Vanilla's own version caches the answer per Y (LazyYCondition); this one asks the context
        // every test. The biome cannot change within a column anyway, and AC only puts one of these
        // at the head of each of its six cave rule chains, so the extra lookup is a map hit in
        // Context#getBiome, which memoises per position itself.
        public SurfaceRules.Condition apply(final SurfaceRules.Context contextIn) {
            return new SurfaceRules.Condition() {
                public boolean test() {
                    Holder<Biome> biome = ((com.github.alexmodguy.alexscaves.mixin.SurfaceRulesContextAccessor) (Object) contextIn).ac_callGetBiome();
                    for (ResourceKey<Biome> key : ACBiomeConditionSource.this.biomes) {
                        if (biome.is(key)) {
                            return true;
                        }
                    }
                    return false;
                }
            };
        }
    }
}
*///?} elif >=1.20.5 {
/*import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.misc.ACMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraftforge.registries.DeferredRegister;
import java.util.List;
import java.util.function.Supplier;

public class ACSurfaceRuleConditionRegistry {

    public static final DeferredRegister<MapCodec<? extends SurfaceRules.ConditionSource>> DEF_REG = DeferredRegister.create(Registries.MATERIAL_CONDITION, AlexsCaves.MODID);

    public static final Supplier<MapCodec<? extends SurfaceRules.ConditionSource>> AC_SIMPLEX_CONDITION = DEF_REG.register("ac_simplex", () -> SimplexConditionSource.CODEC.codec());

    public static final Supplier<MapCodec<? extends SurfaceRules.ConditionSource>> AC_BIOME_CONDITION = DEF_REG.register("ac_biome", () -> ACBiomeConditionSource.CODEC.codec());

    public static SurfaceRules.ConditionSource isBiome(ResourceKey<Biome> biome) {
        return SurfaceRules.isBiome(biome);
    }

    public static SurfaceRules.ConditionSource simplexCondition(float noiseMin, float noiseMax, float noiseScale, float yScale, int offsetType) {
        return new SimplexConditionSource(noiseMin, noiseMax, noiseScale, yScale, offsetType);
    }

    private record SimplexConditionSource(float noiseMin, float noiseMax, float noiseScale, float yScale,
                                          int offsetType) implements SurfaceRules.ConditionSource {
        private static final KeyDispatchDataCodec<SimplexConditionSource> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((group) -> {
            return group.group(Codec.floatRange(-1F, 1F).fieldOf("noise_min").forGetter(SimplexConditionSource::noiseMin), Codec.floatRange(-1F, 1F).fieldOf("noise_max").forGetter(SimplexConditionSource::noiseMax), Codec.floatRange(1F, 10000F).fieldOf("noise_scale").forGetter(SimplexConditionSource::noiseScale), Codec.floatRange(0F, 10000F).fieldOf("y_scale").forGetter(SimplexConditionSource::yScale), Codec.intRange(0, 128).fieldOf("offset_type").forGetter(SimplexConditionSource::offsetType)).apply(group, SimplexConditionSource::new);
        }));

        public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

        public SurfaceRules.Condition apply(final SurfaceRules.Context contextIn) {
            class NoiseCondition implements SurfaceRules.Condition {

                private SurfaceRules.Context context;

                NoiseCondition(SurfaceRules.Context context) {
                    this.context = context;
                }

                public boolean test() {
                    double f = ACMath.sampleNoise3D(context.blockX + (offsetType * 1000), (int) ((context.blockY * yScale + offsetType * 2000)), context.blockZ - (offsetType * 3000), SimplexConditionSource.this.noiseScale);
                    return f > SimplexConditionSource.this.noiseMin && f <= SimplexConditionSource.this.noiseMax;
                }
            }
            return new NoiseCondition(contextIn);
        }
    }

    private record ACBiomeConditionSource(List<ResourceKey<Biome>> biomes) implements SurfaceRules.ConditionSource {
        private static final KeyDispatchDataCodec<ACBiomeConditionSource> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((group) -> {
            return group.group(ResourceKey.codec(Registries.BIOME).listOf().fieldOf("biomes").forGetter(ACBiomeConditionSource::biomes)).apply(group, ACBiomeConditionSource::new);
        }));

        public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

        public SurfaceRules.Condition apply(final SurfaceRules.Context contextIn) {
            return SurfaceRules.isBiome(this.biomes.toArray(new ResourceKey[0])).apply(contextIn);
        }
    }
}
*///?} else {
import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.misc.ACMath;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraftforge.registries.DeferredRegister;
import java.util.List;
import java.util.function.Supplier;

public class ACSurfaceRuleConditionRegistry {

    public static final DeferredRegister<Codec<? extends SurfaceRules.ConditionSource>> DEF_REG = DeferredRegister.create(Registries.MATERIAL_CONDITION, AlexsCaves.MODID);

    public static final Supplier<Codec<? extends SurfaceRules.ConditionSource>> AC_SIMPLEX_CONDITION = DEF_REG.register("ac_simplex", () -> SimplexConditionSource.CODEC.codec());

    public static final Supplier<Codec<? extends SurfaceRules.ConditionSource>> AC_BIOME_CONDITION = DEF_REG.register("ac_biome", () -> ACBiomeConditionSource.CODEC.codec());

    public static SurfaceRules.ConditionSource isBiome(ResourceKey<Biome> biome) {
        return SurfaceRules.isBiome(biome);
    }

    public static SurfaceRules.ConditionSource simplexCondition(float noiseMin, float noiseMax, float noiseScale, float yScale, int offsetType) {
        return new SimplexConditionSource(noiseMin, noiseMax, noiseScale, yScale, offsetType);
    }

    private record SimplexConditionSource(float noiseMin, float noiseMax, float noiseScale, float yScale,
                                          int offsetType) implements SurfaceRules.ConditionSource {
        private static final KeyDispatchDataCodec<SimplexConditionSource> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((group) -> {
            return group.group(Codec.floatRange(-1F, 1F).fieldOf("noise_min").forGetter(SimplexConditionSource::noiseMin), Codec.floatRange(-1F, 1F).fieldOf("noise_max").forGetter(SimplexConditionSource::noiseMax), Codec.floatRange(1F, 10000F).fieldOf("noise_scale").forGetter(SimplexConditionSource::noiseScale), Codec.floatRange(0F, 10000F).fieldOf("y_scale").forGetter(SimplexConditionSource::yScale), Codec.intRange(0, 128).fieldOf("offset_type").forGetter(SimplexConditionSource::offsetType)).apply(group, SimplexConditionSource::new);
        }));

        public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

        public SurfaceRules.Condition apply(final SurfaceRules.Context contextIn) {
            class NoiseCondition implements SurfaceRules.Condition {

                private SurfaceRules.Context context;

                NoiseCondition(SurfaceRules.Context context) {
                    this.context = context;
                }

                public boolean test() {
                    double f = ACMath.sampleNoise3D(context.blockX + (offsetType * 1000), (int) ((context.blockY * yScale + offsetType * 2000)), context.blockZ - (offsetType * 3000), SimplexConditionSource.this.noiseScale);
                    return f > SimplexConditionSource.this.noiseMin && f <= SimplexConditionSource.this.noiseMax;
                }
            }
            return new NoiseCondition(contextIn);
        }
    }

    private record ACBiomeConditionSource(List<ResourceKey<Biome>> biomes) implements SurfaceRules.ConditionSource {
        private static final KeyDispatchDataCodec<ACBiomeConditionSource> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((group) -> {
            return group.group(ResourceKey.codec(Registries.BIOME).listOf().fieldOf("biomes").forGetter(ACBiomeConditionSource::biomes)).apply(group, ACBiomeConditionSource::new);
        }));

        public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

        public SurfaceRules.Condition apply(final SurfaceRules.Context contextIn) {
            return SurfaceRules.isBiome(this.biomes.toArray(new ResourceKey[0])).apply(contextIn);
        }
    }
}
//?}
