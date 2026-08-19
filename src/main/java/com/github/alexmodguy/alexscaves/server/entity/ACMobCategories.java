package com.github.alexmodguy.alexscaves.server.entity;

import net.minecraft.world.entity.MobCategory;

/**
 * The two spawn categories Alex's Caves adds to vanilla's {@link MobCategory}.
 *
 * <p>They are not a convenience: four of this mod's biomes key their {@code spawners} map on
 * {@code alexscaves:cave_creature} and {@code alexscaves:deep_sea_creature}, and a biome JSON
 * cannot be version-gated, so both entries have to exist as real enum constants on every node.
 *
 * <p>How they get there changed in 1.21. Up to 1.20.6 both loaders patched a static
 * {@code MobCategory.create} onto the enum and a mod called it whenever it liked. 1.21 replaced
 * that with FML's declarative enum extension: the entries are listed in
 * {@code META-INF/enumextensions.json}, a transformer adds them while the enum class is loaded,
 * and the mod's only job is to expose the constructor arguments as an {@code EnumProxy} the
 * transformer can read — which is also how it gets the finished constant back.
 *
 * <p>Fabric has neither mechanism on any version, so there the two constants are built by the mod
 * itself, at the tail of the enum's own class initialiser, and read back out of a holder — see
 * {@code mixin.fabric.MobCategoryMixin} and {@code fabric.entity.ACMobCategoryExtension}. That arm
 * therefore only reads; the arguments live over there with the code that passes them.
 *
 * <p>This class exists separately from {@link ACEntityRegistry} precisely because of that read:
 * FML resolves the proxy fields by reflection, forcing this class's initialiser to run before
 * {@code MobCategory} finishes loading. Nothing here may touch anything else of this mod's, and
 * nothing here may touch {@code MobCategory} itself outside the two accessors — which are called
 * from {@code ACEntityRegistry}'s initialiser, long after.
 */
public class ACMobCategories {

    // Arguments in both arms are the same five the enum constructor takes:
    //   serialized name, max instances per chunk, friendly, persistent, despawn distance.
    // The serialized name is what the biome JSONs spell.
    //
    // 26.2 inserts a sixth after the name — a short code for the F3 mob-count readout — into both
    // arms alike, which is why it is a replacement rule (!mc262-mobcategory-*) rather than a gate:
    // Stonecutter cannot nest a version condition inside the loader condition below.
    //? if neoforge && >=1.21 {
    /*public static final net.minecraftforge.fml.common.asm.enumextension.EnumProxy<MobCategory> CAVE_CREATURE_PROXY =
            new net.minecraftforge.fml.common.asm.enumextension.EnumProxy<>(
                    MobCategory.class, "alexscaves:cave_creature", 10, true, true, 128);

    public static final net.minecraftforge.fml.common.asm.enumextension.EnumProxy<MobCategory> DEEP_SEA_CREATURE_PROXY =
            new net.minecraftforge.fml.common.asm.enumextension.EnumProxy<>(
                    MobCategory.class, "alexscaves:deep_sea_creature", 20, true, false, 128);

    public static MobCategory caveCreature() {
        return CAVE_CREATURE_PROXY.getValue();
    }

    public static MobCategory deepSeaCreature() {
        return DEEP_SEA_CREATURE_PROXY.getValue();
    }
    *///?} elif fabric {
    /*public static MobCategory caveCreature() {
        return com.github.alexmodguy.alexscaves.fabric.entity.ACMobCategoryExtension.CAVE_CREATURE;
    }

    public static MobCategory deepSeaCreature() {
        return com.github.alexmodguy.alexscaves.fabric.entity.ACMobCategoryExtension.DEEP_SEA_CREATURE;
    }
    *///?} else {
    public static MobCategory caveCreature() {
        return MobCategory.create("cave_creature", "alexscaves:cave_creature", 10, true, true, 128);
    }

    public static MobCategory deepSeaCreature() {
        return MobCategory.create("deep_sea_creature", "alexscaves:deep_sea_creature", 20, true, false, 128);
    }
    //?}
}
