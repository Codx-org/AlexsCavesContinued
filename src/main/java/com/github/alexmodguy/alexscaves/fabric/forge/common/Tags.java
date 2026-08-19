package com.github.alexmodguy.alexscaves.fabric.forge.common;

import com.github.alexmodguy.alexscaves.server.misc.ACIdFactories;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;

/**
 * Fabric stand-in for the loader's convention-tag constants — the eleven this tree reads, and no
 * others.
 *
 * <p><b>These declare ids; they do not delegate.</b> The sibling repo's version of this file hands
 * every constant straight to Fabric API's {@code ConventionalBiomeTags}, which is right when the
 * module is the only thing that ever defines the tag. It is not right here: the constants below are
 * spelled to match what {@code DataPackMigration.migrateConventionTags} already produces for this
 * mod's own data files, and what {@code backfillFabricConventionTags} then writes out — so the Java
 * side and the data side name the same tag by construction. Delegating instead would pin the
 * spelling to whichever Fabric API build a node is compiled against, and the module renamed all of
 * these once already (the v1 module says {@code c:snowy}, v2 says {@code c:is_snowy}); a constant
 * that moves under the mod is exactly what the migration exists to prevent.
 *
 * <p>The ids are therefore the modern ones — v2's, which are also NeoForge's and Forge-26's, so one
 * spelling covers the whole matrix. Tag JSONs merge, so on a node whose Fabric API defines the same
 * id the two are unioned and third-party contributions are seen; the backfill supplies the vanilla
 * content either way, which is what makes these safe to read on a node with no Fabric API convention
 * module at all.
 *
 * <p>⚠️ On the five nodes below 1.20.5 the pinned module is v1, which spells the item tag
 * {@code c:shears}. Vanilla shears are in the backfill so the wire-cutting check still works there;
 * a <i>modded</i> pair registered only into the v1 id is missed on those five nodes. A gate here
 * would fix it, and is deliberately not written — the biome constants have the same split with ten
 * non-mechanical renames, so gating one and not the other would be a worse kind of inconsistency
 * than the miss it buys.
 *
 * <p>Ids are built through {@link ACIdFactories} rather than the vanilla factory: the factory
 * methods are a loader patch below 1.21 and the constructor is private from it, so the helper is the
 * one spelling that resolves on all 22 nodes without a rule.
 */
public final class Tags {

    private Tags() {
    }

    public static final class Biomes {

        private Biomes() {
        }

        public static final TagKey<Biome> IS_SNOWY = biome("is_snowy");
        public static final TagKey<Biome> IS_WATER = biome("is_aquatic");
        public static final TagKey<Biome> IS_DESERT = biome("is_desert");
        public static final TagKey<Biome> IS_MOUNTAIN = biome("is_mountain");
        public static final TagKey<Biome> IS_CONIFEROUS = biome("is_tree/coniferous");
        public static final TagKey<Biome> IS_SWAMP = biome("is_swamp");
        public static final TagKey<Biome> IS_RARE = biome("is_rare");
        public static final TagKey<Biome> IS_MUSHROOM = biome("is_mushroom");
        public static final TagKey<Biome> IS_SPOOKY = biome("is_spooky");
        public static final TagKey<Biome> IS_PLAINS = biome("is_plains");

        private static TagKey<Biome> biome(String path) {
            return TagKey.create(Registries.BIOME, ACIdFactories.of("c", path));
        }
    }

    public static final class Items {

        private Items() {
        }

        public static final TagKey<Item> SHEARS = TagKey.create(Registries.ITEM, ACIdFactories.of("c", "tools/shear"));
    }
}
