package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.entity.ACMobCategoryExtension;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds Alex's Caves' two spawn categories to vanilla's {@link MobCategory} on the one loader that
 * offers no way to do it — see {@code fabric.entity.ACMobCategoryExtension} for why they cannot
 * simply be left out.
 *
 * <p>The whole thing happens at the tail of the enum's own class initialiser, which is the only
 * moment at which the constants can be added without anything having observed the enum first. Three
 * things are done there and all three are necessary:
 *
 * <ul>
 *   <li>the constants are built through {@link MobCategoryInvoker}, taking the next two ordinals;
 *   <li>{@code $VALUES} is replaced with a copy holding them, so {@code values()} — which is what
 *       {@code NaturalSpawner} iterates every spawn cycle and what {@code Enum.valueOf} reflects
 *       over — reports ten categories rather than eight;
 *   <li>{@code CODEC} is rebuilt. It is <em>not</em> lazy: {@code StringRepresentable.fromEnum}
 *       calls the supplier once, immediately, and keeps the array and a name lookup built from it.
 *       The instruction that assigns it is two before this injection point, so without a rebuild the
 *       codec every biome's {@code spawners} map is decoded with would still know eight names and
 *       the four biomes that name these two would fail to parse.
 * </ul>
 *
 * <p>The ordinals are taken from the array's current length rather than written as 8 and 9, so a
 * vanilla version that adds a ninth category of its own needs no change here.
 */
@Mixin(MobCategory.class)
public class MobCategoryMixin {

    @Shadow
    @Final
    @Mutable
    private static MobCategory[] $VALUES;

    @Shadow
    @Final
    @Mutable
    public static Codec<MobCategory> CODEC;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void ac_addModCategories(CallbackInfo ci) {
        int first = $VALUES.length;
        MobCategory cave = MobCategoryInvoker.ac_new("ALEXSCAVES_CAVE_CREATURE", first, "alexscaves:cave_creature", 10, true, true, 128);
        MobCategory deepSea = MobCategoryInvoker.ac_new("ALEXSCAVES_DEEP_SEA_CREATURE", first + 1, "alexscaves:deep_sea_creature", 20, true, false, 128);

        MobCategory[] extended = java.util.Arrays.copyOf($VALUES, first + 2);
        extended[first] = cave;
        extended[first + 1] = deepSea;
        $VALUES = extended;

        CODEC = StringRepresentable.fromEnum(MobCategory::values);

        ACMobCategoryExtension.CAVE_CREATURE = cave;
        ACMobCategoryExtension.DEEP_SEA_CREATURE = deepSea;
    }
}
