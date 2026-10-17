package com.github.alexmodguy.alexscaves.server.enchantment;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.enchantment.ACWeaponEnchantment.Grade;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTab;
// 1.21.2 deleted EnchantedBookItem — an enchanted book is a plain Item with components now, and the
// one static this used moved to EnchantmentHelper#createBook. See the !mc2102-enchantedbook rule.
//? if <1.21.2
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.Enchantment;
//? if <1.20.5 {
import com.github.alexmodguy.alexscaves.server.item.ACItemRegistry;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
//?}
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;

public class ACEnchantmentRegistry {
    //? if <1.21 {
    public static final DeferredRegister<Enchantment> DEF_REG = DeferredRegister.create(Registries.ENCHANTMENT, AlexsCaves.MODID);
    //?}

    // "Which items accept this enchantment". EnchantmentCategory (a code-built Predicate<Item>) was
    // deleted in 1.20.5 in favour of a TagKey<Item> on the enchantment definition, so from there on
    // these fourteen names resolve to the equivalent item tags instead. Everything downstream — the
    // 51 registrations below, addAllEnchantsToCreativeTab, and its callers in ACCreativeTabRegistry —
    // only ever passes these constants around, so the type change is invisible at the call sites.
    //
    // Below 1.20.5 Fabric needs a third spelling, because EnchantmentCategory is an ENUM and
    // EnchantmentCategory#create is a Forge enum extension: there is no Fabric equivalent, and no
    // access widener can supply one, since the problem is the enum rather than access to it. So on
    // that loader the constants are the Predicate<Item> Forge's factory would have wrapped, held
    // directly. ACWeaponEnchantment stores the predicate, hands vanilla's VANISHABLE to super as a
    // placeholder, and answers canEnchant(ItemStack) out of the predicate instead of out of the
    // placeholder — which is what the anvil, enchanted books and loot all consult. The enchanting
    // table is the single caller that reads the raw `category` field rather than canEnchant, and
    // mixin.fabric.EnchantmentHelperMixin routes it through canEnchant, which is exactly the line
    // Forge patches. Identity of these fourteen objects is load-bearing in
    // addAllEnchantsToCreativeTab below, the same way the enum constants' identity was.
    //? if >=1.20.5 {
    /*public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> GALENA_GAUNTLET = ACTagRegistry.ENCHANTABLE_GALENA_GAUNTLET;
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> RESISTOR_SHIELD = ACTagRegistry.ENCHANTABLE_RESISTOR_SHIELD;
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> PRIMITIVE_CLUB = ACTagRegistry.ENCHANTABLE_PRIMITIVE_CLUB;
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> EXTINCTION_SPEAR = ACTagRegistry.ENCHANTABLE_EXTINCTION_SPEAR;
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> RAYGUN = ACTagRegistry.ENCHANTABLE_RAYGUN;
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> ORTHOLANCE = ACTagRegistry.ENCHANTABLE_ORTHOLANCE;
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> MAGIC_CONCH = ACTagRegistry.ENCHANTABLE_MAGIC_CONCH;
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> SEA_STAFF = ACTagRegistry.ENCHANTABLE_SEA_STAFF;
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> TOTEM_OF_POSSESSION = ACTagRegistry.ENCHANTABLE_TOTEM_OF_POSSESSION;
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> DESOLATE_DAGGER = ACTagRegistry.ENCHANTABLE_DESOLATE_DAGGER;
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> DREADBOW = ACTagRegistry.ENCHANTABLE_DREADBOW;
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> SHOT_GUM = ACTagRegistry.ENCHANTABLE_SHOT_GUM;
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> CANDY_CANE_HOOK = ACTagRegistry.ENCHANTABLE_CANDY_CANE_HOOK;
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> SUGAR_STAFF = ACTagRegistry.ENCHANTABLE_SUGAR_STAFF;
    *///?} elif fabric {
    /*public static final java.util.function.Predicate<net.minecraft.world.item.Item> GALENA_GAUNTLET = item -> item == ACItemRegistry.GALENA_GAUNTLET.get();
    public static final java.util.function.Predicate<net.minecraft.world.item.Item> RESISTOR_SHIELD = item -> item == ACItemRegistry.RESISTOR_SHIELD.get();
    public static final java.util.function.Predicate<net.minecraft.world.item.Item> PRIMITIVE_CLUB = item -> item == ACItemRegistry.PRIMITIVE_CLUB.get();
    public static final java.util.function.Predicate<net.minecraft.world.item.Item> EXTINCTION_SPEAR = item -> item == ACItemRegistry.EXTINCTION_SPEAR.get();
    public static final java.util.function.Predicate<net.minecraft.world.item.Item> RAYGUN = item -> item == ACItemRegistry.RAYGUN.get();
    public static final java.util.function.Predicate<net.minecraft.world.item.Item> ORTHOLANCE = item -> item == ACItemRegistry.ORTHOLANCE.get();
    public static final java.util.function.Predicate<net.minecraft.world.item.Item> MAGIC_CONCH = item -> item == ACItemRegistry.MAGIC_CONCH.get();
    public static final java.util.function.Predicate<net.minecraft.world.item.Item> SEA_STAFF = item -> item == ACItemRegistry.SEA_STAFF.get();
    public static final java.util.function.Predicate<net.minecraft.world.item.Item> TOTEM_OF_POSSESSION = item -> item == ACItemRegistry.TOTEM_OF_POSSESSION.get();
    public static final java.util.function.Predicate<net.minecraft.world.item.Item> DESOLATE_DAGGER = item -> item == ACItemRegistry.DESOLATE_DAGGER.get();
    public static final java.util.function.Predicate<net.minecraft.world.item.Item> DREADBOW = item -> item == ACItemRegistry.DREADBOW.get();
    public static final java.util.function.Predicate<net.minecraft.world.item.Item> SHOT_GUM = item -> item == ACItemRegistry.SHOT_GUM.get();
    public static final java.util.function.Predicate<net.minecraft.world.item.Item> CANDY_CANE_HOOK = item -> item == ACItemRegistry.CANDY_CANE_HOOK.get();
    public static final java.util.function.Predicate<net.minecraft.world.item.Item> SUGAR_STAFF = item -> item == ACItemRegistry.SUGAR_STAFF.get();
    *///?} else {
    public static final EnchantmentCategory GALENA_GAUNTLET = EnchantmentCategory.create("galena_gauntlet", (item -> item == ACItemRegistry.GALENA_GAUNTLET.get()));
    public static final EnchantmentCategory RESISTOR_SHIELD = EnchantmentCategory.create("resistor_shield", (item -> item == ACItemRegistry.RESISTOR_SHIELD.get()));
    public static final EnchantmentCategory PRIMITIVE_CLUB = EnchantmentCategory.create("primitive_club", (item -> item == ACItemRegistry.PRIMITIVE_CLUB.get()));
    public static final EnchantmentCategory EXTINCTION_SPEAR = EnchantmentCategory.create("extinction_spear", (item -> item == ACItemRegistry.EXTINCTION_SPEAR.get()));
    public static final EnchantmentCategory RAYGUN = EnchantmentCategory.create("raygun", (item -> item == ACItemRegistry.RAYGUN.get()));
    public static final EnchantmentCategory ORTHOLANCE = EnchantmentCategory.create("ortholance", (item -> item == ACItemRegistry.ORTHOLANCE.get()));
    public static final EnchantmentCategory MAGIC_CONCH = EnchantmentCategory.create("magic_conch", (item -> item == ACItemRegistry.MAGIC_CONCH.get()));
    public static final EnchantmentCategory SEA_STAFF = EnchantmentCategory.create("sea_staff", (item -> item == ACItemRegistry.SEA_STAFF.get()));
    public static final EnchantmentCategory TOTEM_OF_POSSESSION = EnchantmentCategory.create("totem_of_possession", (item -> item == ACItemRegistry.TOTEM_OF_POSSESSION.get()));
    public static final EnchantmentCategory DESOLATE_DAGGER = EnchantmentCategory.create("desolate_dagger", (item -> item == ACItemRegistry.DESOLATE_DAGGER.get()));
    public static final EnchantmentCategory DREADBOW = EnchantmentCategory.create("dreadbow", (item -> item == ACItemRegistry.DREADBOW.get()));
    public static final EnchantmentCategory SHOT_GUM = EnchantmentCategory.create("shot_gum", (item -> item == ACItemRegistry.SHOT_GUM.get()));
    public static final EnchantmentCategory CANDY_CANE_HOOK = EnchantmentCategory.create("candy_cane_hook", (item -> item == ACItemRegistry.CANDY_CANE_HOOK.get()));
    public static final EnchantmentCategory SUGAR_STAFF = EnchantmentCategory.create("sugar_staff", (item -> item == ACItemRegistry.SUGAR_STAFF.get()));
    //?}

    // From 1.21 an enchantment is a data-pack entry: Enchantment became a final record read from JSON,
    // so there is nothing left to construct in code and nothing to put in a DeferredRegister. The same
    // 51 names live under data/alexscaves/enchantment/ and this arm holds only the keys that address
    // them — the numbers each one used to be built from (grade, level cap, cost curve, supported-items
    // tag, slots) plus the pairwise exclusions of areCompatible below are all in those files instead.
    // Call sites only ever pass these constants to ACCompat#enchantLevel, so the change of handle type
    // is the only thing they see.
    //? if >=1.21 {
    /*public static final net.minecraft.resources.ResourceKey<Enchantment> FIELD_EXTENSION = key("field_extension");
    public static final net.minecraft.resources.ResourceKey<Enchantment> CRYSTALLIZATION = key("crystallization");
    public static final net.minecraft.resources.ResourceKey<Enchantment> FERROUS_HASTE = key("ferrous_haste");
    public static final net.minecraft.resources.ResourceKey<Enchantment> ARROW_INDUCTING = key("arrow_inducting");
    public static final net.minecraft.resources.ResourceKey<Enchantment> HEAVY_SLAM = key("heavy_slam");
    public static final net.minecraft.resources.ResourceKey<Enchantment> SWIFTWOOD = key("swiftwood");
    public static final net.minecraft.resources.ResourceKey<Enchantment> BONKING = key("bonking");
    public static final net.minecraft.resources.ResourceKey<Enchantment> DAZING_SWEEP = key("dazing_sweep");
    public static final net.minecraft.resources.ResourceKey<Enchantment> PLUMMETING_FLIGHT = key("plummeting_flight");
    public static final net.minecraft.resources.ResourceKey<Enchantment> HERD_PHALANX = key("herd_phalanx");
    public static final net.minecraft.resources.ResourceKey<Enchantment> CHOMPING_SPIRIT = key("chomping_spirit");
    public static final net.minecraft.resources.ResourceKey<Enchantment> ENERGY_EFFICIENCY = key("energy_efficiency");
    public static final net.minecraft.resources.ResourceKey<Enchantment> SOLAR = key("solar");
    public static final net.minecraft.resources.ResourceKey<Enchantment> X_RAY = key("x_ray");
    public static final net.minecraft.resources.ResourceKey<Enchantment> GAMMA_RAY = key("gamma_ray");
    public static final net.minecraft.resources.ResourceKey<Enchantment> SECOND_WAVE = key("second_wave");
    public static final net.minecraft.resources.ResourceKey<Enchantment> FLINGING = key("flinging");
    public static final net.minecraft.resources.ResourceKey<Enchantment> SEA_SWING = key("sea_swing");
    public static final net.minecraft.resources.ResourceKey<Enchantment> TSUNAMI = key("tsunami");
    public static final net.minecraft.resources.ResourceKey<Enchantment> CHARTING_CALL = key("charting_call");
    public static final net.minecraft.resources.ResourceKey<Enchantment> LASTING_MORALE = key("lasting_morale");
    public static final net.minecraft.resources.ResourceKey<Enchantment> TAXING_BELLOW = key("taxing_bellow");
    public static final net.minecraft.resources.ResourceKey<Enchantment> ENVELOPING_BUBBLE = key("enveloping_bubble");
    public static final net.minecraft.resources.ResourceKey<Enchantment> BOUNCING_BOLT = key("bouncing_bolt");
    public static final net.minecraft.resources.ResourceKey<Enchantment> SEAPAIRING = key("seapairing");
    public static final net.minecraft.resources.ResourceKey<Enchantment> TRIPLE_SPLASH = key("triple_splash");
    public static final net.minecraft.resources.ResourceKey<Enchantment> SOAK_SEEKING = key("soak_seeking");
    public static final net.minecraft.resources.ResourceKey<Enchantment> DETONATING_DEATH = key("detonating_death");
    public static final net.minecraft.resources.ResourceKey<Enchantment> RAPID_POSSESSION = key("rapid_possession");
    public static final net.minecraft.resources.ResourceKey<Enchantment> SIGHTLESS = key("sightless");
    public static final net.minecraft.resources.ResourceKey<Enchantment> ASTRAL_TRANSFERRING = key("astral_transferring");
    public static final net.minecraft.resources.ResourceKey<Enchantment> IMPENDING_STAB = key("impending_stab");
    public static final net.minecraft.resources.ResourceKey<Enchantment> SATED_BLADE = key("sated_blade");
    public static final net.minecraft.resources.ResourceKey<Enchantment> DOUBLE_STAB = key("double_stab");
    public static final net.minecraft.resources.ResourceKey<Enchantment> PRECISE_VOLLEY = key("precise_volley");
    public static final net.minecraft.resources.ResourceKey<Enchantment> DARK_NOCK = key("dark_nock");
    public static final net.minecraft.resources.ResourceKey<Enchantment> RELENTLESS_DARKNESS = key("relentless_darkness");
    public static final net.minecraft.resources.ResourceKey<Enchantment> TWILIGHT_PERFECTION = key("twilight_perfection");
    public static final net.minecraft.resources.ResourceKey<Enchantment> SHADED_RESPITE = key("shaded_respite");
    public static final net.minecraft.resources.ResourceKey<Enchantment> TARGETED_RICOCHET = key("targeted_ricochet");
    public static final net.minecraft.resources.ResourceKey<Enchantment> TRIPLE_SPLIT = key("triple_split");
    public static final net.minecraft.resources.ResourceKey<Enchantment> BOUNCY_BALL = key("bouncy_ball");
    public static final net.minecraft.resources.ResourceKey<Enchantment> EXPLOSIVE_FLAVOR = key("explosive_flavor");
    public static final net.minecraft.resources.ResourceKey<Enchantment> FAR_FLUNG = key("far_flung");
    public static final net.minecraft.resources.ResourceKey<Enchantment> SHARP_CANE = key("sharp_cane");
    public static final net.minecraft.resources.ResourceKey<Enchantment> STRAIGHT_HOOK = key("straight_hook");
    public static final net.minecraft.resources.ResourceKey<Enchantment> SPELL_LASTING = key("spell_lasting");
    public static final net.minecraft.resources.ResourceKey<Enchantment> PEPPERMINT_PUNTING = key("peppermint_punting");
    public static final net.minecraft.resources.ResourceKey<Enchantment> HUMUNGOUS_HEX = key("humungous_hex");
    public static final net.minecraft.resources.ResourceKey<Enchantment> MULTIPLE_MINT = key("multiple_mint");
    public static final net.minecraft.resources.ResourceKey<Enchantment> SEEKCANDY = key("seekcandy");

    private static net.minecraft.resources.ResourceKey<Enchantment> key(String name) {
        return net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, name));
    }
    *///?} else {
    public static final Supplier<Enchantment> FIELD_EXTENSION = DEF_REG.register("field_extension", () -> new ACWeaponEnchantment("field_extension", Grade.COMMON, GALENA_GAUNTLET, 4, 6, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));
    public static final Supplier<Enchantment> CRYSTALLIZATION = DEF_REG.register("crystallization", () -> new ACWeaponEnchantment("crystallization", Grade.RARE, GALENA_GAUNTLET, 1, 15, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));
    public static final Supplier<Enchantment> FERROUS_HASTE = DEF_REG.register("ferrous_haste", () -> new ACWeaponEnchantment("ferrous_haste", Grade.RARE, GALENA_GAUNTLET, 1, 15, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));
    public static final Supplier<Enchantment> ARROW_INDUCTING = DEF_REG.register("arrow_inducting", () -> new ACWeaponEnchantment("arrow_inducting", Grade.RARE, RESISTOR_SHIELD, 1, 18, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));
    public static final Supplier<Enchantment> HEAVY_SLAM = DEF_REG.register("heavy_slam", () -> new ACWeaponEnchantment("heavy_slam", Grade.COMMON, RESISTOR_SHIELD, 3, 6, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));
    public static final Supplier<Enchantment> SWIFTWOOD = DEF_REG.register("swiftwood", () -> new ACWeaponEnchantment("swiftwood", Grade.RARE, PRIMITIVE_CLUB, 3, 8, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> BONKING = DEF_REG.register("bonking", () -> new ACWeaponEnchantment("bonking", Grade.VERY_RARE, PRIMITIVE_CLUB, 1, 18, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> DAZING_SWEEP = DEF_REG.register("dazing_sweep", () -> new ACWeaponEnchantment("dazing_sweep", Grade.RARE, PRIMITIVE_CLUB, 2, 10, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> PLUMMETING_FLIGHT = DEF_REG.register("plummeting_flight", () -> new ACWeaponEnchantment("plummeting_flight", Grade.RARE, EXTINCTION_SPEAR, 3, 13, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> HERD_PHALANX = DEF_REG.register("herd_phalanx", () -> new ACWeaponEnchantment("herd_phalanx", Grade.RARE, EXTINCTION_SPEAR, 3, 13, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> CHOMPING_SPIRIT = DEF_REG.register("chomping_spirit", () -> new ACWeaponEnchantment("chomping_spirit", Grade.RARE, EXTINCTION_SPEAR, 2, 10, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> ENERGY_EFFICIENCY = DEF_REG.register("energy_efficiency", () -> new ACWeaponEnchantment("energy_efficiency", Grade.COMMON, RAYGUN, 3, 5, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> SOLAR = DEF_REG.register("solar", () -> new ACWeaponEnchantment("solar", Grade.COMMON, RAYGUN, 1, 14, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> X_RAY = DEF_REG.register("x_ray", () -> new ACWeaponEnchantment("x_ray", Grade.COMMON, RAYGUN, 1, 12, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> GAMMA_RAY = DEF_REG.register("gamma_ray", () -> new ACWeaponEnchantment("gamma_ray", Grade.RARE, RAYGUN, 1, 18, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> SECOND_WAVE = DEF_REG.register("second_wave", () -> new ACWeaponEnchantment("second_wave", Grade.RARE, ORTHOLANCE, 1, 12, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> FLINGING = DEF_REG.register("flinging", () -> new ACWeaponEnchantment("flinging", Grade.COMMON, ORTHOLANCE, 3, 8, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> SEA_SWING = DEF_REG.register("sea_swing", () -> new ACWeaponEnchantment("sea_swing", Grade.RARE, ORTHOLANCE, 1, 10, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> TSUNAMI = DEF_REG.register("tsunami", () -> new ACWeaponEnchantment("tsunami", Grade.VERY_RARE, ORTHOLANCE, 1, 20, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> CHARTING_CALL = DEF_REG.register("charting_call", () -> new ACWeaponEnchantment("charting_call", Grade.COMMON, MAGIC_CONCH, 4, 7, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> LASTING_MORALE = DEF_REG.register("lasting_morale", () -> new ACWeaponEnchantment("lasting_morale", Grade.RARE, MAGIC_CONCH, 3, 8, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> TAXING_BELLOW = DEF_REG.register("taxing_bellow", () -> new ACWeaponEnchantment("taxing_bellow", Grade.RARE, MAGIC_CONCH, 1, 19, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> ENVELOPING_BUBBLE = DEF_REG.register("enveloping_bubble", () -> new ACWeaponEnchantment("enveloping_bubble", Grade.RARE, SEA_STAFF, 1, 13, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> BOUNCING_BOLT = DEF_REG.register("bouncing_bolt", () -> new ACWeaponEnchantment("bouncing_bolt", Grade.RARE, SEA_STAFF, 1, 13, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> SEAPAIRING = DEF_REG.register("seapairing", () -> new ACWeaponEnchantment("seapairing", Grade.VERY_RARE, SEA_STAFF, 1, 10, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> TRIPLE_SPLASH = DEF_REG.register("triple_splash", () -> new ACWeaponEnchantment("triple_splash", Grade.RARE, SEA_STAFF, 1, 15, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> SOAK_SEEKING = DEF_REG.register("soak_seeking", () -> new ACWeaponEnchantment("soak_seeking", Grade.COMMON, SEA_STAFF, 3, 5, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> DETONATING_DEATH = DEF_REG.register("detonating_death", () -> new ACWeaponEnchantment("detonating_death", Grade.RARE, TOTEM_OF_POSSESSION, 1, 11, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> RAPID_POSSESSION = DEF_REG.register("rapid_possession", () -> new ACWeaponEnchantment("rapid_possession", Grade.COMMON, TOTEM_OF_POSSESSION, 3, 5, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> SIGHTLESS = DEF_REG.register("sightless", () -> new ACWeaponEnchantment("sightless", Grade.RARE, TOTEM_OF_POSSESSION, 1, 13, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> ASTRAL_TRANSFERRING = DEF_REG.register("astral_transferring", () -> new ACWeaponEnchantment("astral_transferring", Grade.RARE, TOTEM_OF_POSSESSION, 1, 12, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> IMPENDING_STAB = DEF_REG.register("impending_stab", () -> new ACWeaponEnchantment("impending_stab", Grade.COMMON, DESOLATE_DAGGER, 3, 6, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> SATED_BLADE = DEF_REG.register("sated_blade", () -> new ACWeaponEnchantment("sated_blade", Grade.COMMON, DESOLATE_DAGGER, 2, 11, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> DOUBLE_STAB = DEF_REG.register("double_stab", () -> new ACWeaponEnchantment("double_stab", Grade.RARE, DESOLATE_DAGGER, 1, 12, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> PRECISE_VOLLEY = DEF_REG.register("precise_volley", () -> new ACWeaponEnchantment("precise_volley", Grade.RARE, DREADBOW, 1, 18, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> DARK_NOCK = DEF_REG.register("dark_nock", () -> new ACWeaponEnchantment("dark_nock", Grade.RARE, DREADBOW, 3, 10, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> RELENTLESS_DARKNESS = DEF_REG.register("relentless_darkness", () -> new ACWeaponEnchantment("relentless_darkness", Grade.VERY_RARE, DREADBOW, 1, 20, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> TWILIGHT_PERFECTION = DEF_REG.register("twilight_perfection", () -> new ACWeaponEnchantment("twilight_perfection", Grade.RARE, DREADBOW, 3, 7, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> SHADED_RESPITE = DEF_REG.register("shaded_respite", () -> new ACWeaponEnchantment("shaded_respite", Grade.VERY_RARE, DREADBOW, 1, 9, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> TARGETED_RICOCHET = DEF_REG.register("targeted_ricochet", () -> new ACWeaponEnchantment("targeted_ricochet", Grade.RARE, SHOT_GUM, 1, 16, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> TRIPLE_SPLIT = DEF_REG.register("triple_split", () -> new ACWeaponEnchantment("triple_split", Grade.RARE, SHOT_GUM, 1, 12, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> BOUNCY_BALL = DEF_REG.register("bouncy_ball", () -> new ACWeaponEnchantment("bouncy_ball", Grade.COMMON, SHOT_GUM, 3, 7, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> EXPLOSIVE_FLAVOR = DEF_REG.register("explosive_flavor", () -> new ACWeaponEnchantment("explosive_flavor", Grade.VERY_RARE, SHOT_GUM, 1, 16, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> FAR_FLUNG = DEF_REG.register("far_flung", () -> new ACWeaponEnchantment("far_flung", Grade.COMMON, CANDY_CANE_HOOK, 3, 6, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> SHARP_CANE = DEF_REG.register("sharp_cane", () -> new ACWeaponEnchantment("sharp_cane", Grade.COMMON, CANDY_CANE_HOOK, 2, 8, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> STRAIGHT_HOOK = DEF_REG.register("straight_hook", () -> new ACWeaponEnchantment("straight_hook", Grade.RARE, CANDY_CANE_HOOK, 1, 12, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> SPELL_LASTING = DEF_REG.register("spell_lasting", () -> new ACWeaponEnchantment("spell_lasting", Grade.COMMON, SUGAR_STAFF, 3, 8, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> PEPPERMINT_PUNTING = DEF_REG.register("peppermint_punting", () -> new ACWeaponEnchantment("peppermint_punting", Grade.RARE, SUGAR_STAFF, 1, 12, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> HUMUNGOUS_HEX = DEF_REG.register("humungous_hex", () -> new ACWeaponEnchantment("humungous_hex", Grade.UNCOMMON, SUGAR_STAFF, 2, 9, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> MULTIPLE_MINT = DEF_REG.register("multiple_mint", () -> new ACWeaponEnchantment("multiple_mint", Grade.UNCOMMON, SUGAR_STAFF, 2, 9, EquipmentSlot.MAINHAND));
    public static final Supplier<Enchantment> SEEKCANDY = DEF_REG.register("seekcandy", () -> new ACWeaponEnchantment("seekcandy", Grade.RARE, SUGAR_STAFF, 1, 16, EquipmentSlot.MAINHAND));
    //?}

    // Gone from 1.21: an enchantment states its own incompatibilities as an `exclusive_set` in its JSON,
    // and there is no checkCompatibility hook left to route through this. The same pairs are encoded
    // there, so the behaviour survives even though this method does not.
    //? if <1.21 {
    public static boolean areCompatible(ACWeaponEnchantment enchantment1, Enchantment enchantment2) {
        if(enchantment1 == X_RAY.get() && enchantment2 == GAMMA_RAY.get()){
            return false;
        }
        if(enchantment1 == GAMMA_RAY.get() && enchantment2 == X_RAY.get()){
            return false;
        }
        if(enchantment1 == SECOND_WAVE.get() && enchantment2 == TSUNAMI.get()){
            return false;
        }
        if(enchantment1 == TSUNAMI.get() && enchantment2 == SECOND_WAVE.get()){
            return false;
        }
        if(enchantment1 == TAXING_BELLOW.get() && (enchantment2 == Enchantments.UNBREAKING || enchantment2 == Enchantments.MENDING)){
            return false;
        }
        if((enchantment1 == Enchantments.UNBREAKING || enchantment1 == Enchantments.MENDING) && enchantment2 == TAXING_BELLOW.get()){
            return false;
        }
        if(enchantment1 == BOUNCING_BOLT.get() && enchantment2 == TRIPLE_SPLASH.get()){
            return false;
        }
        if(enchantment1 == TRIPLE_SPLASH.get() && enchantment2 == BOUNCING_BOLT.get()){
            return false;
        }
        if(enchantment1 == DETONATING_DEATH.get() && enchantment2 == ASTRAL_TRANSFERRING.get()){
            return false;
        }
        if(enchantment1 == ASTRAL_TRANSFERRING.get() && enchantment2 == DETONATING_DEATH.get()){
            return false;
        }
        if(enchantment1 == IMPENDING_STAB.get() && enchantment2 == DOUBLE_STAB.get()){
            return false;
        }
        if(enchantment1 == DOUBLE_STAB.get() && enchantment2 == IMPENDING_STAB.get()){
            return false;
        }
        if(enchantment1 == RELENTLESS_DARKNESS.get() && (enchantment2 == PRECISE_VOLLEY.get() || enchantment2 == DARK_NOCK.get() || enchantment2 == TWILIGHT_PERFECTION.get())){
            return false;
        }
        if((enchantment1 == PRECISE_VOLLEY.get() || enchantment1 == DARK_NOCK.get()  || enchantment1 == TWILIGHT_PERFECTION.get()) && enchantment2 == RELENTLESS_DARKNESS.get()){
            return false;
        }
        if(enchantment1 == TARGETED_RICOCHET.get() && enchantment2 == TRIPLE_SPLIT.get()){
            return false;
        }
        if(enchantment1 == TRIPLE_SPLIT.get() && enchantment2 == TARGETED_RICOCHET.get()){
            return false;
        }
        return true;
    }
    //?}

    // The isPresent() guard is gone with Forge's RegistryObject: getEntries only ever yields
    // entries that were registered, and the creative tab is built long after registration.
    // The wildcard matters: Forge's handle is RegistryObject<Enchantment> but NeoForge's is
    // DeferredHolder<Enchantment, ? extends Enchantment>, and only Supplier<? extends Enchantment>
    // is a supertype of both.
    //
    // Four arms, because both halves of "walk the mod's enchantments, keep the ones in this category"
    // moved. 1.20.5 turned the public `category` field into the definition's supportedItems tag (TagKey
    // is a record, so equals() is the right comparison); 1.21 took the registry away from the mod
    // entirely, so the enchantments have to be read out of the tab's own HolderLookup — which is why
    // every version takes the ItemDisplayParameters that ACCreativeTabRegistry already had in hand.
    // The fourth is Fabric below 1.20.5, where `category` holds the shared VANISHABLE placeholder and
    // so says nothing: the question is asked of the predicate the enchantment was built with, by the
    // same reference identity the enum comparison next to it uses.
    //? if >=1.21 {
    /*public static void addAllEnchantsToCreativeTab(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> enchantmentCategory){
        params.holders().lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(holder -> {
            if(holder.key().location().getNamespace().equals(AlexsCaves.MODID) && holder.value().getSupportedItems().unwrapKey().filter(enchantmentCategory::equals).isPresent()){
                EnchantmentInstance instance = new EnchantmentInstance(holder, holder.value().getMaxLevel());
                output.accept(EnchantedBookItem.createForEnchantment(instance));
            }
        });
    }
    *///?} elif >=1.20.5 {
    /*public static void addAllEnchantsToCreativeTab(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> enchantmentCategory){
        for (Supplier<? extends Enchantment> enchantObject : DEF_REG.getEntries()) {
            Enchantment enchant = enchantObject.get();
            if(enchant.getSupportedItems().equals(enchantmentCategory)){
                EnchantmentInstance instance = new EnchantmentInstance(enchant, enchant.getMaxLevel());
                output.accept(EnchantedBookItem.createForEnchantment(instance));
            }
        }
    }
    *///?} elif fabric {
    /*public static void addAllEnchantsToCreativeTab(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output, java.util.function.Predicate<net.minecraft.world.item.Item> enchantmentCategory){
        for (Supplier<? extends Enchantment> enchantObject : DEF_REG.getEntries()) {
            Enchantment enchant = enchantObject.get();
            if(enchant instanceof ACWeaponEnchantment weapon && weapon.acCategory() == enchantmentCategory){
                EnchantmentInstance instance = new EnchantmentInstance(enchant, enchant.getMaxLevel());
                output.accept(EnchantedBookItem.createForEnchantment(instance));
            }
        }
    }
    *///?} else {
    public static void addAllEnchantsToCreativeTab(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output, EnchantmentCategory enchantmentCategory){
        for (Supplier<? extends Enchantment> enchantObject : DEF_REG.getEntries()) {
            Enchantment enchant = enchantObject.get();
            if(enchant.category == enchantmentCategory){
                EnchantmentInstance instance = new EnchantmentInstance(enchant, enchant.getMaxLevel());
                output.accept(EnchantedBookItem.createForEnchantment(instance));
            }
        }
    }
    //?}
}
