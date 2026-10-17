package com.github.alexmodguy.alexscaves.fabric.entity;

import net.minecraft.world.entity.MobCategory;

/**
 * Where this loader's two extra {@link MobCategory} constants live once they exist.
 *
 * <p>Forge and NeoForge extend that enum for a mod — a patched {@code MobCategory.create} up to
 * 1.20.6, FML's declarative enum extension from 1.21. Fabric has neither, and no access widener can
 * supply one, because the obstacle is that the type is an {@code enum} rather than that anything on
 * it is inaccessible. Alex's Caves cannot do without them: four of its biomes key their
 * {@code spawners} map on {@code alexscaves:cave_creature} and {@code alexscaves:deep_sea_creature},
 * and a biome JSON cannot be version-gated, so both have to be real constants on every node.
 *
 * <p>So {@code mixin.fabric.MobCategoryMixin} builds them at the tail of the enum's own class
 * initialiser, appends them to {@code $VALUES} and rebuilds the {@code CODEC} that the biome codec
 * decodes those two ids with — and parks them here on the way past. It is a plain mutable holder
 * rather than anything lazier because the write happens inside a class initialiser, where a
 * {@code Supplier} would only add a second thing that can be touched too early.
 *
 * <p>The initialiser below is what makes the read safe from either direction. Ordinarily
 * {@code MobCategory} is loaded first and fills the fields, which reaches this class and runs the
 * forcing call re-entrantly — free, since the array it asks for was assigned two instructions
 * earlier. If instead something reads this class first, the call is what loads {@code MobCategory},
 * and the fields are filled before the read completes.
 */
public final class ACMobCategoryExtension {

    public static MobCategory CAVE_CREATURE;
    public static MobCategory DEEP_SEA_CREATURE;

    static {
        MobCategory.values();
    }

    private ACMobCategoryExtension() {
    }
}
