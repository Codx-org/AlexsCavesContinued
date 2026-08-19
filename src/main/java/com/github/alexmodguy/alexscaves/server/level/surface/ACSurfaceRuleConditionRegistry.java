package com.github.alexmodguy.alexscaves.server.level.surface;


import com.github.alexmodguy.alexscaves.AlexsCaves;
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

    // 1.20.5 retyped the MATERIAL_RULE / MATERIAL_CONDITION registries from Codec to MapCodec.
    //? if >=1.20.5 {
    /*public static final DeferredRegister<MapCodec<? extends SurfaceRules.ConditionSource>> DEF_REG = DeferredRegister.create(Registries.MATERIAL_CONDITION, AlexsCaves.MODID);

    public static final Supplier<MapCodec<? extends SurfaceRules.ConditionSource>> AC_SIMPLEX_CONDITION = DEF_REG.register("ac_simplex", () -> SimplexConditionSource.CODEC.codec());

    public static final Supplier<MapCodec<? extends SurfaceRules.ConditionSource>> AC_BIOME_CONDITION = DEF_REG.register("ac_biome", () -> ACBiomeConditionSource.CODEC.codec());
    *///?} else {
    public static final DeferredRegister<Codec<? extends SurfaceRules.ConditionSource>> DEF_REG = DeferredRegister.create(Registries.MATERIAL_CONDITION, AlexsCaves.MODID);

    public static final Supplier<Codec<? extends SurfaceRules.ConditionSource>> AC_SIMPLEX_CONDITION = DEF_REG.register("ac_simplex", () -> SimplexConditionSource.CODEC.codec());

    public static final Supplier<Codec<? extends SurfaceRules.ConditionSource>> AC_BIOME_CONDITION = DEF_REG.register("ac_biome", () -> ACBiomeConditionSource.CODEC.codec());
    //?}

    // 26.2 rewrote SurfaceRules#isBiome to take a leading HolderGetter<Biome> — it resolves the keys
    // eagerly into a HolderSet and BiomeConditionSource then compares Holder IDENTITY against the
    // Context's possibleBiomes set, so a fabricated standalone Holder can never match. There is no
    // HolderGetter anywhere AC builds its rules (ACSurfaceRules.setup runs at mod construction, and
    // the merge happens in NoiseGeneratorSettingsMixin#surfaceRule), so from 26.2 the mod supplies
    // its own key-comparing condition source instead. Below 26.2 this stays vanilla's, which keeps
    // SurfaceRulesManager's TerraBlender scan — it looks for a vanilla BiomeConditionSource — working.
    public static SurfaceRules.ConditionSource isBiome(ResourceKey<Biome> biome) {
        //? if >=26.2 {
        /*return new ACBiomeConditionSource(List.of(biome));
        *///?} else {
        return SurfaceRules.isBiome(biome);
        //?}
    }

    public static SurfaceRules.ConditionSource simplexCondition(float noiseMin, float noiseMax, float noiseScale, float yScale, int offsetType) {
        return new SimplexConditionSource(noiseMin, noiseMax, noiseScale, yScale, offsetType);
    }

    private record SimplexConditionSource(float noiseMin, float noiseMax, float noiseScale, float yScale,
                                          int offsetType) implements SurfaceRules.ConditionSource {
        private static final KeyDispatchDataCodec<SimplexConditionSource> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((group) -> {
            return group.group(Codec.floatRange(-1F, 1F).fieldOf("noise_min").forGetter(SimplexConditionSource::noiseMin), Codec.floatRange(-1F, 1F).fieldOf("noise_max").forGetter(SimplexConditionSource::noiseMax), Codec.floatRange(1F, 10000F).fieldOf("noise_scale").forGetter(SimplexConditionSource::noiseScale), Codec.floatRange(0F, 10000F).fieldOf("y_scale").forGetter(SimplexConditionSource::yScale), Codec.intRange(0, 128).fieldOf("offset_type").forGetter(SimplexConditionSource::offsetType)).apply(group, SimplexConditionSource::new);
        }));

        // See CitadelSurfaceRuleWrapper: 26.2 returns the MapCodec itself here. The DEF_REG line
        // above is unaffected — it already unwrapped the holder with .codec().
        //? if >=26.2 {
        /*public MapCodec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC.codec();
        }
        *///?} else {
        public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }
        //?}

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

    // The mod's own "is this one of these biomes" condition, holding the ResourceKeys rather than
    // resolved Holders. Registered on every version so the codec is never a dangling dispatch key,
    // but only handed out from #isBiome at and above 26.2; below that it delegates to vanilla's own
    // source, so behaviour is identical wherever it is reached from.
    private record ACBiomeConditionSource(List<ResourceKey<Biome>> biomes) implements SurfaceRules.ConditionSource {
        private static final KeyDispatchDataCodec<ACBiomeConditionSource> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((group) -> {
            return group.group(ResourceKey.codec(Registries.BIOME).listOf().fieldOf("biomes").forGetter(ACBiomeConditionSource::biomes)).apply(group, ACBiomeConditionSource::new);
        }));

        // See CitadelSurfaceRuleWrapper: 26.2 returns the MapCodec itself here.
        //? if >=26.2 {
        /*public MapCodec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC.codec();
        }
        *///?} else {
        public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }
        //?}

        // Vanilla's own version caches the answer per Y (LazyYCondition); this one asks the context
        // every test. The biome cannot change within a column anyway, and AC only puts one of these
        // at the head of each of its six cave rule chains, so the extra lookup is a map hit in
        // Context#getBiome, which memoises per position itself.
        public SurfaceRules.Condition apply(final SurfaceRules.Context contextIn) {
            //? if >=26.2 {
            /*return new SurfaceRules.Condition() {
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
            *///?} else {
            return SurfaceRules.isBiome(this.biomes.toArray(new ResourceKey[0])).apply(contextIn);
            //?}
        }
    }
}
