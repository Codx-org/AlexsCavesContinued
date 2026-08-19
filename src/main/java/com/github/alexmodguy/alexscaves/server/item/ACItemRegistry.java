package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACDeferredRegister;
import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.github.alexmodguy.alexscaves.server.block.fluid.ACFluidRegistry;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.item.*;
import com.github.alexmodguy.alexscaves.server.entity.util.AlexsCavesBoat;
import com.github.alexmodguy.alexscaves.server.entity.util.GummyColors;
import com.github.alexmodguy.alexscaves.server.item.dispenser.FluidContainerDispenseItemBehavior;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import com.github.alexmodguy.alexscaves.citadel.server.block.LecternBooks;
import net.minecraft.core.registries.Registries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
// 1.20.2 moved BlockSource out of net.minecraft.core (covered by the wildcard above) into
// net.minecraft.core.dispenser, and made it a record.
//? if >=1.20.2
/*import net.minecraft.core.dispenser.BlockSource;*/
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
//? if !fabric && <1.21.3
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ACItemRegistry {
    private static Map<Supplier<Item>, ResourceKey<Biome>> creativeTabSpawnEggMap = new LinkedHashMap<>();
    // Alex's Caves gives four groups of items their own rarity tier, purely so the item name is
    // tinted: dark red for the abyssal/darkness gear, green for the irradiated things, pink for the
    // candy, and a hue that cycles for the rainbow ones.
    //
    // Rarity.create is Forge's extension to the vanilla enum, and 1.20.5 removed it — rarity became
    // a data component holding a plain four-value enum with no extension point on any loader. So
    // from 1.20.5 the four tiers collapse onto the nearest vanilla ones and the names lose their
    // bespoke colours. Restoring them means colouring the name rather than the rarity, which is a
    // separate piece of work; the constants stay so that the ~15 call sites read the same on every
    // version.
    //
    // Fabric takes the same collapse on every version, for the same reason one version early: Rarity is
    // an enum and there is no extension point for one here at all, so below 1.20.5 that loader arrives
    // where the other two arrive at 1.20.5. Widening this arm's condition rather than adding a fourth
    // is exact — the four vanilla constants it names are what that loader would have written anyway.
    //? if >=1.20.5 || fabric {
    /*public static final Rarity RARITY_DEMONIC = Rarity.EPIC;
    public static final Rarity RARITY_NUCLEAR = Rarity.RARE;
    public static final Rarity RARITY_SWEET = Rarity.RARE;
    public static final Rarity RARITY_RAINBOW = Rarity.EPIC;
    *///?} else {
    public static final Rarity RARITY_DEMONIC = Rarity.create("alexscaves:demonic", ChatFormatting.DARK_RED);
    public static final Rarity RARITY_NUCLEAR = Rarity.create("alexscaves:nuclear", ChatFormatting.GREEN);
    public static final Rarity RARITY_SWEET = Rarity.create("alexscaves:sweet", style -> style.withColor(0XFF8ACD));
    public static final Rarity RARITY_RAINBOW = Rarity.create("alexscaves:rainbow", style -> style.withColor(Color.HSBtoRGB((System.currentTimeMillis() % 5000) / 5000F, 1f, 1F)));
    //?}
    public static final ACArmorMaterial PRIMORDIAL_ARMOR_MATERIAL = new ACArmorMaterial("primordial", 20, new int[]{3, 4, 3, 2}, 25, ACCompat.rawSound(SoundEvents.ARMOR_EQUIP_LEATHER), 0F);
    public static final ACArmorMaterial HAZMAT_SUIT_ARMOR_MATERIAL = new ACArmorMaterial("hazmat_suit", 20, new int[]{2, 4, 5, 2}, 25, ACCompat.rawSound(SoundEvents.ARMOR_EQUIP_IRON), 0.5F);
    public static final ACArmorMaterial DIVING_SUIT_ARMOR_MATERIAL = new ACArmorMaterial("diving_suit", 20, new int[]{2, 6, 5, 2}, 25, ACCompat.rawSound(SoundEvents.ARMOR_EQUIP_IRON), 0.0F);
    // 1.20.5's Holder sweep through SoundEvents skipped EMPTY, which is still the bare SoundEvent it
    // always was on every version through 1.21.2 (javap-checked) — there is no registry entry to
    // hold, that being the point of it. So this one alone skips the unwrapping helper.
    //? if >=1.20.5 {
    /*public static final ACArmorMaterial DARKNESS_ARMOR_MATERIAL = new ACArmorMaterial("darkness", 15, new int[]{4, 5, 1, 1}, 40, SoundEvents.EMPTY, 0.5F);
    *///?} else {
    public static final ACArmorMaterial DARKNESS_ARMOR_MATERIAL = new ACArmorMaterial("darkness", 15, new int[]{4, 5, 1, 1}, 40, ACCompat.rawSound(SoundEvents.EMPTY), 0.5F);
    //?}
    public static final ACArmorMaterial RAINBOUNCE_ARMOR_MATERIAL = new ACArmorMaterial("rainbounce", 6, new int[]{2, 2, 1, 2}, 40, ACCompat.rawSound(SoundEvents.ARMOR_EQUIP_GENERIC), 0.0F);
    public static final ACArmorMaterial GINGERBREAD_ARMOR_MATERIAL = new ACArmorMaterial("gingerbread", 10, new int[]{2, 4, 5, 2}, 25, ACCompat.rawSound(SoundEvents.ARMOR_EQUIP_LEATHER), 0.0F);
    public static final ACDeferredRegister<Item> DEF_REG = ACDeferredRegister.create(Registries.ITEM, AlexsCaves.MODID);
    public static final Supplier<Item> ADVANCEMENT_TAB_ICON = DEF_REG.register("advancement_tab_icon", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> CAVE_TABLET = DEF_REG.register("cave_tablet", () -> new CaveInfoItem(new Item.Properties(), true));
    public static final Supplier<Item> CAVE_CODEX = DEF_REG.register("cave_codex", () -> new CaveInfoItem(new Item.Properties(), false));
    public static final Supplier<Item> CAVE_BOOK = DEF_REG.register("cave_book", () -> new CaveBookItem());
    public static final Supplier<Item> CAVE_MAP = DEF_REG.register("cave_map", () -> new CaveMapItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> CAVE_MAP_SPRITE = DEF_REG.register("cave_map_inventory", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> CAVE_MAP_LOADING_SPRITE = DEF_REG.register("cave_map_loading", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> CAVE_MAP_FILLED_SPRITE = DEF_REG.register("cave_map_filled", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> RAW_SCARLET_NEODYMIUM = DEF_REG.register("raw_scarlet_neodymium", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> RAW_AZURE_NEODYMIUM = DEF_REG.register("raw_azure_neodymium", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> SCARLET_NEODYMIUM_INGOT = DEF_REG.register("scarlet_neodymium_ingot", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> AZURE_NEODYMIUM_INGOT = DEF_REG.register("azure_neodymium_ingot", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> TELECORE = DEF_REG.register("telecore", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> NOTOR_COMPONENT = DEF_REG.register("notor_gizmo", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> HEAVYWEIGHT = DEF_REG.register("heavyweight", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> FERROUSLIME_BALL = DEF_REG.register("ferrouslime_ball", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> QUARRY_SMASHER = DEF_REG.register("quarry_smasher", () -> new QuarrySmasherItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> HOLOCODER = DEF_REG.register("holocoder", () -> new HolocoderItem(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> SEEKING_ARROW = DEF_REG.register("seeking_arrow", () -> new SeekingArrowItem());
    public static final Supplier<Item> GALENA_GAUNTLET = DEF_REG.register("galena_gauntlet", () -> new GalenaGauntletItem());
    public static final Supplier<Item> RESISTOR_SHIELD = DEF_REG.register("resistor_shield", () -> new ResistorShieldItem());
    // The trim id used to be passed in only to title the tooltip (trim_pattern.alexscaves.polarity);
    // 1.21.2 dropped that line from the template item outright and takes the Properties instead.
    // Which pattern the template applies is decided by the trim_pattern datapack entry naming
    // this item, not by anything said here, so the template still works — it just no longer
    // repeats its own name above the slot descriptions, exactly as vanilla's own templates now do.
    //? if >=1.21.2 {
    /*public static final Supplier<Item> POLARITY_ARMOR_TRIM_SMITHING_TEMPLATE = DEF_REG.register("polarity_armor_trim_smithing_template", () -> SmithingTemplateItem.createArmorTrimTemplate(new Item.Properties()));
    *///?} else {
    public static final Supplier<Item> POLARITY_ARMOR_TRIM_SMITHING_TEMPLATE = DEF_REG.register("polarity_armor_trim_smithing_template", () -> SmithingTemplateItem.createArmorTrimTemplate(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "polarity")));
    //?}
    public static final Supplier<Item> PEWEN_DOOR = DEF_REG.register("pewen_door", () -> new DoubleHighBlockItem(ACBlockRegistry.PEWEN_DOOR.get(), (new Item.Properties())));
    // SignItem and StandingAndWallBlockItem both moved their Properties to last, matching every
    // other item constructor. Same three arguments, same order otherwise.
    //? if >=1.21.2 {
    /*public static final Supplier<Item> PEWEN_SIGN = DEF_REG.register("pewen_sign", () -> new SignItem(ACBlockRegistry.PEWEN_SIGN.get(), ACBlockRegistry.PEWEN_WALL_SIGN.get(), (new Item.Properties()).stacksTo(16)));
    *///?} else {
    public static final Supplier<Item> PEWEN_SIGN = DEF_REG.register("pewen_sign", () -> new SignItem((new Item.Properties()).stacksTo(16), ACBlockRegistry.PEWEN_SIGN.get(), ACBlockRegistry.PEWEN_WALL_SIGN.get()));
    //?}
    public static final Supplier<Item> PEWEN_HANGING_SIGN = DEF_REG.register("pewen_hanging_sign", () -> new HangingSignItem(ACBlockRegistry.PEWEN_HANGING_SIGN.get(), ACBlockRegistry.PEWEN_WALL_HANGING_SIGN.get(), (new Item.Properties()).stacksTo(16)));
    public static final Supplier<Item> PEWEN_BOAT = DEF_REG.register("pewen_boat", () -> new CaveBoatItem(false, AlexsCavesBoat.Type.PEWEN, new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> PEWEN_CHEST_BOAT = DEF_REG.register("pewen_chest_boat", () -> new CaveBoatItem(true, AlexsCavesBoat.Type.PEWEN, new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> TRILOCARIS_BUCKET = DEF_REG.register("trilocaris_bucket", () -> new ModFishBucketItem(ACEntityRegistry.TRILOCARIS, () -> Fluids.WATER, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final Supplier<Item> TRILOCARIS_TAIL = DEF_REG.register("trilocaris_tail", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.TRILOCARIS_TAIL)));
    public static final Supplier<Item> COOKED_TRILOCARIS_TAIL = DEF_REG.register("cooked_trilocaris_tail", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.TRILOCARIS_TAIL_COOKED)));
    public static final Supplier<Item> PINE_NUTS = DEF_REG.register("pine_nuts", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.PINE_NUTS)));
    public static final Supplier<Item> PEWEN_SAP = DEF_REG.register("pewen_sap", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> AMBER_CURIOSITY = DEF_REG.register("amber_curiosity", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> DINOSAUR_NUGGET = DEF_REG.register("dinosaur_nugget", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.DINOSAUR_NUGGETS)));
    public static final Supplier<Item> SERENE_SALAD = DEF_REG.register("serene_salad", () -> new PrehistoricMixtureItem(ACFoodBuilder.food(new Item.Properties().stacksTo(1), ACFoods.SERENE_SALAD)));
    public static final Supplier<Item> SEETHING_STEW = DEF_REG.register("seething_stew", () -> new PrehistoricMixtureItem(ACFoodBuilder.food(new Item.Properties().stacksTo(1), ACFoods.SEETHING_STEW)));
    public static final Supplier<Item> PRIMORDIAL_SOUP = DEF_REG.register("primordial_soup", () -> new PrehistoricMixtureItem(ACFoodBuilder.food(new Item.Properties().stacksTo(1), ACFoods.PRIMORDIAL_SOUP)));
    public static final Supplier<Item> TOUGH_HIDE = DEF_REG.register("tough_hide", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> HEAVY_BONE = DEF_REG.register("heavy_bone", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final Supplier<Item> PRIMITIVE_CLUB = DEF_REG.register("primitive_club", () -> new PrimitiveClubItem(new Item.Properties().durability(120)));
    public static final Supplier<Item> PRIMITIVE_CLUB_SPRITE = DEF_REG.register("primitive_club_inventory", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> PRIMORDIAL_HELMET = DEF_REG.register("primordial_helmet", () -> new PrimordialArmorItem(PRIMORDIAL_ARMOR_MATERIAL, ArmorItem.Type.HELMET));
    public static final Supplier<Item> PRIMORDIAL_TUNIC = DEF_REG.register("primordial_tunic", () -> new PrimordialArmorItem(PRIMORDIAL_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE));
    public static final Supplier<Item> PRIMORDIAL_PANTS = DEF_REG.register("primordial_pants", () -> new PrimordialArmorItem(PRIMORDIAL_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS));
    public static final Supplier<Item> LIMESTONE_SPEAR = DEF_REG.register("limestone_spear", () -> new LimestoneSpearItem(new Item.Properties().stacksTo(16)));
    public static final Supplier<Item> LIMESTONE_SPEAR_SPRITE = DEF_REG.register("limestone_spear_inventory", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> OMINOUS_CATALYST = DEF_REG.register("ominous_catalyst", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON).fireResistant()));
    public static final Supplier<Item> TECTONIC_SHARD = DEF_REG.register("tectonic_shard", () -> new Item(new Item.Properties().rarity(RARITY_DEMONIC).fireResistant()));
    public static final Supplier<Item> EXTINCTION_SPEAR = DEF_REG.register("extinction_spear", () -> new ExtinctionSpearItem(new Item.Properties().durability(1300).rarity(RARITY_DEMONIC).fireResistant()));
    public static final Supplier<Item> EXTINCTION_SPEAR_SPRITE = DEF_REG.register("extinction_spear_inventory", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> DINOSAUR_POTTERY_SHERD = DEF_REG.register("dinosaur_pottery_sherd", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> FOOTPRINT_POTTERY_SHERD = DEF_REG.register("footprint_pottery_sherd", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> DINOSAUR_TRAIN = DEF_REG.register("dinosaur_train", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> ACID_BUCKET = DEF_REG.register("acid_bucket", () -> ACPlatform.bucketItem(ACFluidRegistry.ACID_FLUID_SOURCE, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final Supplier<Item> RADGILL_BUCKET = DEF_REG.register("radgill_bucket", () -> new ModFishBucketItem(ACEntityRegistry.RADGILL, ACFluidRegistry.ACID_FLUID_SOURCE, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final Supplier<Item> RADGILL = DEF_REG.register("radgill", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.RADGILL)));
    public static final Supplier<Item> COOKED_RADGILL = DEF_REG.register("cooked_radgill", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.RADGILL_COOKED)));
    public static final Supplier<Item> URANIUM = DEF_REG.register("uranium", () -> new RadioactiveItem(new Item.Properties(), 0.001F));
    public static final Supplier<Item> URANIUM_SHARD = DEF_REG.register("uranium_shard", () -> new RadioactiveItem(new Item.Properties(), 0.001F));
    public static final Supplier<Item> SULFUR_DUST = DEF_REG.register("sulfur_dust", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> RADON_BOTTLE = DEF_REG.register("radon_bottle", () -> new Item(new Item.Properties().craftRemainder(Items.GLASS_BOTTLE).stacksTo(16)));
    public static final Supplier<Item> CINDER_BRICK = DEF_REG.register("cinder_brick", () -> new ThrownProjectileItem(new Item.Properties(), player -> new CinderBrickEntity(player.level(), player), -20.0F, 0.65F, 0.9F));
    public static final Supplier<Item> SPELUNKIE = DEF_REG.register("spelunkie", () -> new RadiationRemovingFoodItem(ACFoodBuilder.food(new Item.Properties(), ACFoods.SPELUNKIE)));
    public static final Supplier<Item> SLAM = DEF_REG.register("slam", () -> new RadiationRemovingFoodItem(ACFoodBuilder.food(new Item.Properties(), ACFoods.SLAM)));
    public static final Supplier<Item> GREEN_SOYLENT = DEF_REG.register("green_soylent", () -> new RadiationRemovingFoodItem(ACFoodBuilder.food(new Item.Properties(), ACFoods.SOYLENT_GREEN)));
    public static final Supplier<Item> TOXIC_PASTE = DEF_REG.register("toxic_paste", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> POLYMER_PLATE = DEF_REG.register("polymer_plate", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> HAZMAT_MASK = DEF_REG.register("hazmat_mask", () -> new HazmatArmorItem(HAZMAT_SUIT_ARMOR_MATERIAL, ArmorItem.Type.HELMET));
    public static final Supplier<Item> HAZMAT_CHESTPLATE = DEF_REG.register("hazmat_chestplate", () -> new HazmatArmorItem(HAZMAT_SUIT_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE));
    public static final Supplier<Item> HAZMAT_LEGGINGS = DEF_REG.register("hazmat_leggings", () -> new HazmatArmorItem(HAZMAT_SUIT_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS));
    public static final Supplier<Item> HAZMAT_BOOTS = DEF_REG.register("hazmat_boots", () -> new HazmatArmorItem(HAZMAT_SUIT_ARMOR_MATERIAL, ArmorItem.Type.BOOTS));
    public static final Supplier<Item> FISSILE_CORE = DEF_REG.register("fissile_core", () -> new RadioactiveItem(new Item.Properties().rarity(Rarity.UNCOMMON), 0.001F));
    public static final Supplier<Item> CHARRED_REMNANT = DEF_REG.register("charred_remnant", () -> new RadioactiveItem(new Item.Properties(), 0.0005F));
    public static final Supplier<Item> REMOTE_DETONATOR = DEF_REG.register("remote_detonator", () -> new RemoteDetonatorItem());
    public static final Supplier<Item> RAYGUN = DEF_REG.register("raygun", () -> new RaygunItem());
    public static final Supplier<Item> MUSIC_DISC_FUSION_FRAGMENT = DEF_REG.register("disc_fragment_fusion", () -> new DiscFragmentItem(new Item.Properties()));
    // RecordItem is gone in 1.21: a music disc is a plain Item carrying a JUKEBOX_PLAYABLE
    // component that points at a JukeboxSong, and the sound, length, comparator output and
    // description all moved into the data pack — see data/alexscaves/jukebox_song/music_disc_fusion.json,
    // which restates exactly the four arguments the constructor used to take.
    //? if >=1.21
    /*public static final Supplier<Item> MUSIC_DISC_FUSION = DEF_REG.register("music_disc_fusion", () -> new Item(new Item.Properties().stacksTo(1).rarity(RARITY_NUCLEAR).jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG, ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "music_disc_fusion")))));*/
    //? if <1.21
    public static final Supplier<Item> MUSIC_DISC_FUSION = DEF_REG.register("music_disc_fusion", () -> new RecordItem(14, ACSoundRegistry.FUSIONC_MUSIC_DISC.get(), new Item.Properties().stacksTo(1).rarity(RARITY_NUCLEAR), 237 * 20));
    public static final Supplier<Item> LANTERNFISH_BUCKET = DEF_REG.register("lanternfish_bucket", () -> new ModFishBucketItem(ACEntityRegistry.LANTERNFISH, () -> Fluids.WATER, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final Supplier<Item> LANTERNFISH = DEF_REG.register("lanternfish", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.LANTERNFISH)));
    public static final Supplier<Item> COOKED_LANTERNFISH = DEF_REG.register("cooked_lanternfish", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.LANTERNFISH_COOKED)));
    public static final Supplier<Item> TRIPODFISH_BUCKET = DEF_REG.register("tripodfish_bucket", () -> new ModFishBucketItem(ACEntityRegistry.TRIPODFISH, () -> Fluids.WATER, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final Supplier<Item> TRIPODFISH = DEF_REG.register("tripodfish", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.TRIPODFISH)));
    public static final Supplier<Item> COOKED_TRIPODFISH = DEF_REG.register("cooked_tripodfish", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.TRIPODFISH_COOKED)));
    public static final Supplier<Item> SEA_PIG_BUCKET = DEF_REG.register("sea_pig_bucket", () -> new ModFishBucketItem(ACEntityRegistry.SEA_PIG, () -> Fluids.WATER, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final Supplier<Item> SEA_PIG = DEF_REG.register("sea_pig", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.SEA_PIG)));
    public static final Supplier<Item> MARINE_SNOW = DEF_REG.register("marine_snow", () -> new MarineSnowItem());
    public static final Supplier<Item> GOSSAMER_WORM_BUCKET = DEF_REG.register("gossamer_worm_bucket", () -> new ModFishBucketItem(ACEntityRegistry.GOSSAMER_WORM, () -> Fluids.WATER, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final Supplier<Item> BIOLUMINESSCENCE = DEF_REG.register("bioluminesscence", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> PEARL = DEF_REG.register("pearl", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> COOKED_MUSSEL = DEF_REG.register("cooked_mussel", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.MUSSEL_COOKED)));
    public static final Supplier<Item> DEEP_SEA_SUSHI_ROLL = DEF_REG.register("deep_sea_sushi_roll", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.DEEP_SEA_SUSHI_ROLL)));
    public static final Supplier<Item> SEA_GLASS_SHARDS = DEF_REG.register("sea_glass_shards", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> SUBMARINE = DEF_REG.register("submarine", () -> new SubmarineItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> DIVING_HELMET = DEF_REG.register("diving_helmet", () -> new DivingArmorItem(DIVING_SUIT_ARMOR_MATERIAL, ArmorItem.Type.HELMET));
    public static final Supplier<Item> DIVING_CHESTPLATE = DEF_REG.register("diving_chestplate", () -> new DivingArmorItem(DIVING_SUIT_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE));
    public static final Supplier<Item> DIVING_LEGGINGS = DEF_REG.register("diving_leggings", () -> new DivingArmorItem(DIVING_SUIT_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS));
    public static final Supplier<Item> DIVING_BOOTS = DEF_REG.register("diving_boots", () -> new DivingArmorItem(DIVING_SUIT_ARMOR_MATERIAL, ArmorItem.Type.BOOTS));
    public static final Supplier<Item> FLOATER = DEF_REG.register("floater", () -> new FloaterItem());
    public static final Supplier<Item> GAZING_PEARL = DEF_REG.register("gazing_pearl", () -> new GazingPearlItem());
    public static final Supplier<Item> INK_BOMB = DEF_REG.register("ink_bomb", () -> new InkBombItem(new Item.Properties(), false));
    public static final Supplier<Item> GLOW_INK_BOMB = DEF_REG.register("glow_ink_bomb", () -> new InkBombItem(new Item.Properties(), true));
    public static final Supplier<Item> MAGIC_CONCH = DEF_REG.register("magic_conch", () -> new MagicConchItem(new Item.Properties().durability(5).rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> SEA_STAFF = DEF_REG.register("sea_staff", () -> new SeaStaffItem(new Item.Properties().durability(850).rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> SEA_STAFF_SPRITE = DEF_REG.register("sea_staff_inventory", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> ORTHOLANCE = DEF_REG.register("ortholance", () -> new OrtholanceItem(new Item.Properties().durability(340).rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> ORTHOLANCE_SPRITE = DEF_REG.register("ortholance_inventory", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> DEPTH_CHARGE = DEF_REG.register("depth_charge", () -> new ThrownProjectileItem(new Item.Properties(), player -> new DepthChargeEntity(player.level(), player), -10.0F, 0.65F, 1.5F));
    public static final Supplier<Item> GUARDIAN_POTTERY_SHERD = DEF_REG.register("guardian_pottery_sherd", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> HERO_POTTERY_SHERD = DEF_REG.register("hero_pottery_sherd", () -> new Item(new Item.Properties()));
    // See PEWEN_SIGN.
    //? if >=1.21.2 {
    /*public static final Supplier<Item> BIOLUMINESCENT_TORCH = DEF_REG.register("bioluminescent_torch", () -> new StandingAndWallBlockItem(ACBlockRegistry.BIOLUMINESCENT_TORCH.get(), ACBlockRegistry.BIOLUMINESCENT_WALL_TORCH.get(), Direction.DOWN, new Item.Properties()));
    *///?} else {
    public static final Supplier<Item> BIOLUMINESCENT_TORCH = DEF_REG.register("bioluminescent_torch", () -> new StandingAndWallBlockItem(ACBlockRegistry.BIOLUMINESCENT_TORCH.get(), ACBlockRegistry.BIOLUMINESCENT_WALL_TORCH.get(), new Item.Properties(), Direction.DOWN));
    //?}
    public static final Supplier<Item> GAME_CONTROLLER = DEF_REG.register("game_controller", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final Supplier<Item> STINKY_FISH = DEF_REG.register("stinky_fish", () -> new Item(ACFoodBuilder.food(new Item.Properties().rarity(Rarity.RARE), ACFoods.STINKY_FISH)));
    public static final Supplier<Item> IMMORTAL_EMBRYO = DEF_REG.register("immortal_embryo", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final Supplier<Item> GUANO = DEF_REG.register("guano", () -> new ThrownProjectileItem(new Item.Properties(), player -> new GuanoEntity(player.level(), player), 0.0F, 1.0F, 1.0F));
    public static final Supplier<Item> MOTH_DUST = DEF_REG.register("moth_dust", () -> new MothDustItem());
    public static final Supplier<Item> FERTILIZER = DEF_REG.register("fertilizer", () -> new FertilizerItem());
    public static final Supplier<Item> DARK_TATTERS = DEF_REG.register("dark_tatters", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> OCCULT_GEM = DEF_REG.register("occult_gem", () -> new OccultGemItem());
    public static final Supplier<Item> TOTEM_OF_POSSESSION = DEF_REG.register("totem_of_possession", () -> new TotemOfPossessionItem());
    public static final Supplier<Item> DESOLATE_DAGGER = DEF_REG.register("desolate_dagger", () -> new DesolateDaggerItem());
    public static final Supplier<Item> CORRODENT_TEETH = DEF_REG.register("corrodent_teeth", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> BURROWING_ARROW = DEF_REG.register("burrowing_arrow", () -> new BurrowingArrowItem());
    public static final Supplier<Item> VESPER_WING = DEF_REG.register("vesper_wing", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.VESPER_WING)));
    public static final Supplier<Item> VESPER_STEW = DEF_REG.register("vesper_stew", () -> ACCompat.bowlFood(ACFoodBuilder.food(new Item.Properties(), ACFoods.VESPER_SOUP).stacksTo(1)));
    public static final Supplier<Item> PURE_DARKNESS = DEF_REG.register("pure_darkness", () -> new Item(new Item.Properties().rarity(RARITY_DEMONIC)));
    public static final Supplier<Item> SHADOW_SILK = DEF_REG.register("shadow_silk", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> HOOD_OF_DARKNESS = DEF_REG.register("hood_of_darkness", () -> new DarknessArmorItem(DARKNESS_ARMOR_MATERIAL, ArmorItem.Type.HELMET));
    public static final Supplier<Item> CLOAK_OF_DARKNESS = DEF_REG.register("cloak_of_darkness", () -> new DarknessArmorItem(DARKNESS_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE));
    public static final Supplier<Item> DARKENED_APPLE = DEF_REG.register("darkened_apple", () -> new DarkenedAppleItem());
    public static final Supplier<Item> DREADBOW = DEF_REG.register("dreadbow", () -> new DreadbowItem());
    public static final Supplier<Item> DREADBOW_SPRITE = DEF_REG.register("dreadbow_inventory", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> DREADBOW_PULLING_0_SPRITE = DEF_REG.register("dreadbow_pulling_0_inventory", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> DREADBOW_PULLING_1_SPRITE = DEF_REG.register("dreadbow_pulling_1_inventory", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> DREADBOW_PULLING_2_SPRITE = DEF_REG.register("dreadbow_pulling_2_inventory", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> THORNWOOD_DOOR = DEF_REG.register("thornwood_door", () -> new DoubleHighBlockItem(ACBlockRegistry.THORNWOOD_DOOR.get(), (new Item.Properties())));
    // See PEWEN_SIGN.
    //? if >=1.21.2 {
    /*public static final Supplier<Item> THORNWOOD_SIGN = DEF_REG.register("thornwood_sign", () -> new SignItem(ACBlockRegistry.THORNWOOD_SIGN.get(), ACBlockRegistry.THORNWOOD_WALL_SIGN.get(), (new Item.Properties()).stacksTo(16)));
    *///?} else {
    public static final Supplier<Item> THORNWOOD_SIGN = DEF_REG.register("thornwood_sign", () -> new SignItem((new Item.Properties()).stacksTo(16), ACBlockRegistry.THORNWOOD_SIGN.get(), ACBlockRegistry.THORNWOOD_WALL_SIGN.get()));
    //?}
    public static final Supplier<Item> THORNWOOD_HANGING_SIGN = DEF_REG.register("thornwood_hanging_sign", () -> new HangingSignItem(ACBlockRegistry.THORNWOOD_HANGING_SIGN.get(), ACBlockRegistry.THORNWOOD_WALL_HANGING_SIGN.get(), (new Item.Properties()).stacksTo(16)));
    public static final Supplier<Item> THORNWOOD_BOAT = DEF_REG.register("thornwood_boat", () -> new CaveBoatItem(false, AlexsCavesBoat.Type.THORNWOOD, new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> THORNWOOD_CHEST_BOAT = DEF_REG.register("thornwood_chest_boat", () -> new CaveBoatItem(true, AlexsCavesBoat.Type.THORNWOOD, new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> PURPLE_SODA_BUCKET = DEF_REG.register("purple_soda_bucket", () -> ACPlatform.bucketItem(ACFluidRegistry.PURPLE_SODA_FLUID_SOURCE, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final Supplier<Item> PURPLE_SODA_BOTTLE = DEF_REG.register("purple_soda_bottle", () -> new DrinkableBottledItem(ACFoodBuilder.food(new Item.Properties().stacksTo(16), ACFoods.PURPLE_SODA_BOTTLE)));
    public static final Supplier<Item> SWEETISH_FISH_RED_BUCKET = DEF_REG.register("sweetish_fish_red_bucket", () -> new SweetishFishBucketItem(GummyColors.RED));
    public static final Supplier<Item> SWEETISH_FISH_GREEN_BUCKET = DEF_REG.register("sweetish_fish_green_bucket", () -> new SweetishFishBucketItem(GummyColors.GREEN));
    public static final Supplier<Item> SWEETISH_FISH_BLUE_BUCKET = DEF_REG.register("sweetish_fish_blue_bucket", () -> new SweetishFishBucketItem(GummyColors.BLUE));
    public static final Supplier<Item> SWEETISH_FISH_YELLOW_BUCKET = DEF_REG.register("sweetish_fish_yellow_bucket", () -> new SweetishFishBucketItem(GummyColors.YELLOW));
    public static final Supplier<Item> SWEETISH_FISH_PINK_BUCKET = DEF_REG.register("sweetish_fish_pink_bucket", () -> new SweetishFishBucketItem(GummyColors.PINK));
    public static final Supplier<Item> SWEETISH_FISH_RED = DEF_REG.register("sweetish_fish_red", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.SWEETISH_FISH)));
    public static final Supplier<Item> SWEETISH_FISH_GREEN = DEF_REG.register("sweetish_fish_green", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.SWEETISH_FISH)));
    public static final Supplier<Item> SWEETISH_FISH_BLUE = DEF_REG.register("sweetish_fish_blue", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.SWEETISH_FISH)));
    public static final Supplier<Item> SWEETISH_FISH_YELLOW = DEF_REG.register("sweetish_fish_yellow", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.SWEETISH_FISH)));
    public static final Supplier<Item> SWEETISH_FISH_PINK = DEF_REG.register("sweetish_fish_pink", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.SWEETISH_FISH)));
    public static final Supplier<Item> GELATIN_RED = DEF_REG.register("gelatin_red", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.GELATIN)));
    public static final Supplier<Item> GELATIN_GREEN = DEF_REG.register("gelatin_green", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.GELATIN)));
    public static final Supplier<Item> GELATIN_BLUE = DEF_REG.register("gelatin_blue", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.GELATIN)));
    public static final Supplier<Item> GELATIN_YELLOW = DEF_REG.register("gelatin_yellow", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.GELATIN)));
    public static final Supplier<Item> GELATIN_PINK = DEF_REG.register("gelatin_pink", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.GELATIN)));
    public static final Supplier<Item> HOT_CHOCOLATE_BOTTLE = DEF_REG.register("hot_chocolate_bottle", () -> new HotChocolateBottleItem());
    public static final Supplier<Item> VANILLA_ICE_CREAM_SCOOP = DEF_REG.register("vanilla_ice_cream_scoop", () -> new ThrownProjectileItem(new Item.Properties(), player -> new ThrownIceCreamScoopEntity(player.level(), player), -10.0F, 1.0F, 0.2F));
    public static final Supplier<Item> CHOCOLATE_ICE_CREAM_SCOOP = DEF_REG.register("chocolate_ice_cream_scoop", () -> new ThrownProjectileItem(new Item.Properties(), player -> new ThrownIceCreamScoopEntity(player.level(), player), -10.0F, 1.0F, 0.2F));
    public static final Supplier<Item> SWEETBERRY_ICE_CREAM_SCOOP = DEF_REG.register("sweetberry_ice_cream_scoop", () -> new ThrownProjectileItem(new Item.Properties(), player -> new ThrownIceCreamScoopEntity(player.level(), player), -10.0F, 1.0F, 0.2F));
    public static final Supplier<Item> SUNDAE = DEF_REG.register("sundae", () -> ACCompat.bowlFood(ACFoodBuilder.food(new Item.Properties(), ACFoods.SUNDAE).rarity(RARITY_SWEET).stacksTo(1)));
    public static final Supplier<Item> SHARPENED_CANDY_CANE = DEF_REG.register("sharpened_candy_cane", () -> new SharpenedCandyCaneItem(ACFoodBuilder.food(new Item.Properties(), ACFoods.CANDY_CANE)));
    public static final Supplier<Item> PEPPERMINT_POWDER = DEF_REG.register("peppermint_powder", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.PEPPERMINT_POWDER)));
    public static final Supplier<Item> RAINBOUNCE_BOOTS = DEF_REG.register("rainbounce_boots", () -> new RainbounceBootsItem(RAINBOUNCE_ARMOR_MATERIAL, ArmorItem.Type.BOOTS));
    public static final Supplier<Item> GUMBALL_PILE = DEF_REG.register("gumball_pile", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.GUMBALL_PILE)));
    public static final Supplier<Item> SHOT_GUM = DEF_REG.register("shot_gum", () -> new ShotGumItem());
    public static final Supplier<Item> CARAMEL = DEF_REG.register("caramel", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.CARAMEL)));
    public static final Supplier<Item> CARAMEL_APPLE = DEF_REG.register("caramel_apple", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.CARAMEL_APPLE)));
    public static final Supplier<Item> CANDY_CANE_HOOK = DEF_REG.register("candy_cane_hook", () -> new CandyCaneHookItem());
    public static final Supplier<Item> SWEET_TOOTH = DEF_REG.register("sweet_tooth", () -> new Item(new Item.Properties().rarity(RARITY_SWEET)));
    public static final Supplier<Item> RADIANT_ESSENCE = DEF_REG.register("radiant_essence", () -> new RadiantEssenceItem());
    public static final Supplier<Item> LICOWITCH_RADIANT_ESSENCE = DEF_REG.register("licowitch_radiant_essence", () -> new RadiantEssenceItem());
    public static final Supplier<Item> SACK_OF_SATING = DEF_REG.register("sack_of_sating", () -> new SackOfSatingItem());
    public static final Supplier<Item> SUGAR_STAFF = DEF_REG.register("sugar_staff", () -> new SugarStaffItem(new Item.Properties().durability(100).rarity(Rarity.UNCOMMON)));
    public static final Supplier<Item> SUGAR_STAFF_SPRITE = DEF_REG.register("sugar_staff_inventory", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> GINGERBREAD_CRUMBS = DEF_REG.register("gingerbread_crumbs", () -> new Item(ACFoodBuilder.food(new Item.Properties(), ACFoods.GINGERBREAD_CRUMBS)));
    public static final Supplier<Item> GINGERBREAD_HELMET = DEF_REG.register("gingerbread_helmet", () -> new GingerbreadArmorItem(GINGERBREAD_ARMOR_MATERIAL, ArmorItem.Type.HELMET));
    public static final Supplier<Item> GINGERBREAD_CHESTPLATE = DEF_REG.register("gingerbread_chestplate", () -> new GingerbreadArmorItem(GINGERBREAD_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE));
    public static final Supplier<Item> GINGERBREAD_LEGGINGS = DEF_REG.register("gingerbread_leggings", () -> new GingerbreadArmorItem(GINGERBREAD_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS));
    public static final Supplier<Item> GINGERBREAD_BOOTS = DEF_REG.register("gingerbread_boots", () -> new GingerbreadArmorItem(GINGERBREAD_ARMOR_MATERIAL, ArmorItem.Type.BOOTS));
    public static final Supplier<Item> PURPLE_SODA_BOTTLE_ROCKET = DEF_REG.register("purple_soda_bottle_rocket", () -> new SodaBottleRocketItem());
    public static final Supplier<Item> FROSTMINT_SPEAR = DEF_REG.register("frostmint_spear", () -> new FrostmintSpearItem(new Item.Properties().stacksTo(16)));
    public static final Supplier<Item> FROSTMINT_SPEAR_SPRITE = DEF_REG.register("frostmint_spear_inventory", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> MUSIC_DISC_TASTY_FRAGMENT = DEF_REG.register("disc_fragment_tasty", () -> new DiscFragmentItem(new Item.Properties()));
    // RecordItem is gone in 1.21: a music disc is a plain Item carrying a JUKEBOX_PLAYABLE
    // component that points at a JukeboxSong, and the sound, length, comparator output and
    // description all moved into the data pack — see data/alexscaves/jukebox_song/music_disc_tasty.json,
    // which restates exactly the four arguments the constructor used to take.
    //? if >=1.21
    /*public static final Supplier<Item> MUSIC_DISC_TASTY = DEF_REG.register("music_disc_tasty", () -> new Item(new Item.Properties().stacksTo(1).rarity(RARITY_SWEET).jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG, ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "music_disc_tasty")))));*/
    //? if <1.21
    public static final Supplier<Item> MUSIC_DISC_TASTY = DEF_REG.register("music_disc_tasty", () -> new RecordItem(14, ACSoundRegistry.TASTY_MUSIC_DISC.get(), new Item.Properties().stacksTo(1).rarity(RARITY_SWEET), 183 * 20));
    public static final Supplier<Item> ALEX_MEAL = DEF_REG.register("alex_meal", () -> new AlexMealItem());
    public static final Supplier<Item> BIOME_TREAT = DEF_REG.register("biome_treat", () -> new BiomeTreatItem());
    public static final Supplier<Item> JELLY_BEAN = DEF_REG.register("jelly_bean", () -> new JellyBeanItem());

    static {
        spawnEgg("teletor", ACEntityRegistry.TELETOR, 0X433B4A, 0X0060EF, ACBiomeRegistry.MAGNETIC_CAVES);
        spawnEgg("magnetron", ACEntityRegistry.MAGNETRON, 0XFF002A, 0X203070, ACBiomeRegistry.MAGNETIC_CAVES);
        spawnEgg("boundroid", ACEntityRegistry.BOUNDROID, 0XBB1919, 0XFFFFFF, ACBiomeRegistry.MAGNETIC_CAVES);
        spawnEgg("ferrouslime", ACEntityRegistry.FERROUSLIME, 0X26272D, 0X53556C, ACBiomeRegistry.MAGNETIC_CAVES);
        spawnEgg("notor", ACEntityRegistry.NOTOR, 0X5F5369, 0XC6C6C6, ACBiomeRegistry.MAGNETIC_CAVES);
        spawnEgg("subterranodon", ACEntityRegistry.SUBTERRANODON, 0X00B1B2, 0XFFF11C, ACBiomeRegistry.PRIMORDIAL_CAVES);
        spawnEgg("vallumraptor", ACEntityRegistry.VALLUMRAPTOR, 0X22389A, 0XEEE5AB, ACBiomeRegistry.PRIMORDIAL_CAVES);
        spawnEgg("grottoceratops", ACEntityRegistry.GROTTOCERATOPS, 0XAC3B03, 0XD39B4E, ACBiomeRegistry.PRIMORDIAL_CAVES);
        spawnEgg("trilocaris", ACEntityRegistry.TRILOCARIS, 0X713E0D, 0X8B2010, ACBiomeRegistry.PRIMORDIAL_CAVES);
        spawnEgg("tremorsaurus", ACEntityRegistry.TREMORSAURUS, 0X53780E, 0XDFA211, ACBiomeRegistry.PRIMORDIAL_CAVES);
        spawnEgg("relicheirus", ACEntityRegistry.RELICHEIRUS, 0X6AE4F9, 0X5B2152, ACBiomeRegistry.PRIMORDIAL_CAVES);
        spawnEgg("luxtructosaurus", ACEntityRegistry.LUXTRUCTOSAURUS, 0X1F0E15, 0XB30C03, ACBiomeRegistry.PRIMORDIAL_CAVES);
        spawnEgg("atlatitan", ACEntityRegistry.ATLATITAN, 0XB67000, 0XBFBAA4, ACBiomeRegistry.PRIMORDIAL_CAVES);
        spawnEgg("nucleeper", ACEntityRegistry.NUCLEEPER, 0X95A1A5, 0X00FF00, ACBiomeRegistry.TOXIC_CAVES);
        spawnEgg("radgill", ACEntityRegistry.RADGILL, 0X43302C, 0XE8E400, ACBiomeRegistry.TOXIC_CAVES);
        spawnEgg("brainiac", ACEntityRegistry.BRAINIAC, 0X3E5136, 0XE87C9E, ACBiomeRegistry.TOXIC_CAVES);
        spawnEgg("gammaroach", ACEntityRegistry.GAMMAROACH, 0X56682A, 0X2A2B19, ACBiomeRegistry.TOXIC_CAVES);
        spawnEgg("raycat", ACEntityRegistry.RAYCAT, 0X67FF00, 0X030A00, ACBiomeRegistry.TOXIC_CAVES);
        spawnEgg("tremorzilla", ACEntityRegistry.TREMORZILLA, 0X574D2F, 0X8CFF08, ACBiomeRegistry.TOXIC_CAVES);
        spawnEgg("lanternfish", ACEntityRegistry.LANTERNFISH, 0X182538, 0XECA500, ACBiomeRegistry.ABYSSAL_CHASM);
        spawnEgg("sea_pig", ACEntityRegistry.SEA_PIG, 0XFFA3B9, 0XF88672, ACBiomeRegistry.ABYSSAL_CHASM);
        spawnEgg("hullbreaker", ACEntityRegistry.HULLBREAKER, 0X182538, 0X76FFFD, ACBiomeRegistry.ABYSSAL_CHASM);
        spawnEgg("gossamer_worm", ACEntityRegistry.GOSSAMER_WORM, 0XC8F1FF, 0X96DEF6, ACBiomeRegistry.ABYSSAL_CHASM);
        spawnEgg("tripodfish", ACEntityRegistry.TRIPODFISH, 0X34529D, 0X81A1CF, ACBiomeRegistry.ABYSSAL_CHASM);
        spawnEgg("deep_one", ACEntityRegistry.DEEP_ONE, 0X0D2547, 0X0A843B, ACBiomeRegistry.ABYSSAL_CHASM);
        spawnEgg("deep_one_knight", ACEntityRegistry.DEEP_ONE_KNIGHT, 0X472C3B, 0XD4CCC3, ACBiomeRegistry.ABYSSAL_CHASM);
        spawnEgg("deep_one_mage", ACEntityRegistry.DEEP_ONE_MAGE, 0X96DEF6, 0XD1FF00, ACBiomeRegistry.ABYSSAL_CHASM);
        spawnEgg("mine_guardian", ACEntityRegistry.MINE_GUARDIAN, 0X404253, 0XE62008, ACBiomeRegistry.ABYSSAL_CHASM);
        spawnEgg("gloomoth", ACEntityRegistry.GLOOMOTH, 0X5E463D, 0XEBD3BE, ACBiomeRegistry.FORLORN_HOLLOWS);
        spawnEgg("underzealot", ACEntityRegistry.UNDERZEALOT, 0X291C17, 0XF27C68, ACBiomeRegistry.FORLORN_HOLLOWS);
        spawnEgg("watcher", ACEntityRegistry.WATCHER, 0X291C17, 0XEC1900, ACBiomeRegistry.FORLORN_HOLLOWS);
        spawnEgg("corrodent", ACEntityRegistry.CORRODENT, 0X351A14, 0X593B33, ACBiomeRegistry.FORLORN_HOLLOWS);
        spawnEgg("vesper", ACEntityRegistry.VESPER, 0X884E2A, 0XA54A6B, ACBiomeRegistry.FORLORN_HOLLOWS);
        spawnEgg("forsaken", ACEntityRegistry.FORSAKEN, 0X000000, 0X110909, ACBiomeRegistry.FORLORN_HOLLOWS);
        spawnEgg("sweetish_fish", ACEntityRegistry.SWEETISH_FISH, 0XE9132C, 0XFF364D, ACBiomeRegistry.CANDY_CAVITY);
        spawnEgg("caniac", ACEntityRegistry.CANIAC, 0XF9F0FF, 0XFF3F56, ACBiomeRegistry.CANDY_CAVITY);
        spawnEgg("gumbeeper", ACEntityRegistry.GUMBEEPER, 0XFF2B44, 0XE7BAFF, ACBiomeRegistry.CANDY_CAVITY);
        spawnEgg("candicorn", ACEntityRegistry.CANDICORN, 0XE86B00, 0XFFEF57, ACBiomeRegistry.CANDY_CAVITY);
        spawnEgg("gum_worm", ACEntityRegistry.GUM_WORM, 0X92FFD9, 0XFFA1DC, ACBiomeRegistry.CANDY_CAVITY);
        spawnEgg("caramel_cube", ACEntityRegistry.CARAMEL_CUBE, 0XCC8015, 0XB86A0D, ACBiomeRegistry.CANDY_CAVITY);
        spawnEgg("gummy_bear", ACEntityRegistry.GUMMY_BEAR, 0XFF463F, 0XFDA09E, ACBiomeRegistry.CANDY_CAVITY);
        spawnEgg("licowitch", ACEntityRegistry.LICOWITCH, 0X681182, 0XFF6CD7, ACBiomeRegistry.CANDY_CAVITY);
        spawnEgg("gingerbread_man", ACEntityRegistry.GINGERBREAD_MAN, 0XBB581D, 0XFFFFFF, ACBiomeRegistry.CANDY_CAVITY);
    }

    // 1.21.2 turned "what repairs this armour" into a data component that ArmorMaterial fills from
    // an item TAG, through Item.Properties#repairable — and that call *creates* the tag inside
    // BuiltInRegistries.ITEM, as an unbound HolderSet, the moment an armour item is constructed.
    //
    // Vanilla never trips over its own because BuiltInRegistries.freeze() binds every bootstrapped
    // tag to empty before freezing each registry, long before any mod registers anything. NeoForge
    // then unfreezes the registries for mod registration, which drops that binding, so the six
    // repairs_*_armor tags this mod's materials name are still unbound when GameData.freezeData()
    // runs — and the game dies with "Unbound tags in registry ResourceKey[minecraft:root /
    // minecraft:item]" before a single data pack has been read.
    //
    // Binding them to empty here is exactly what vanilla does for its own, and a no-op for every
    // tag that already has a binding; the data pack rebinds all of them with real contents on world
    // load, so anvil repair still works. It has to run after this mod's items are registered but
    // before the freeze — hence LOWEST priority on the ITEM RegisterEvent, DeferredRegister
    // listening at NORMAL.
    //
    // NeoForge-only, and the gate says so rather than leaving it at >=1.21.2: the unfreeze is
    // NeoForge's, its only registration is AlexsCaves#addListener under the same predicate, and the
    // event type it takes is a Forge one that Fabric does not stub. Gated by version alone it was
    // dead code on Forge that compiled anyway, and only failed once a third loader arrived. If a
    // Fabric boot ever reports the same unbound tags, give it an arm of its own here rather than
    // widening this one — the hook it would hang off is not this event.
    //? if neoforge && >=1.21.2 {
    /*@SuppressWarnings("unchecked")
    public static void bindModCreatedItemTags(net.minecraftforge.registries.RegisterEvent event) {
        if (Registries.ITEM.equals(event.getRegistryKey())) {
            ((MappedRegistry<Item>) BuiltInRegistries.ITEM).bindAllTagsToEmpty();
        }
    }
    *///?}

    private static void spawnEgg(String entityName, Supplier type, int color1, int color2, ResourceKey<Biome> biomeTab) {
        // NeoForge deleted DeferredSpawnEggItem (Forge's ForgeSpawnEggItem, renamed) in 21.3, and the
        // vanilla item is enough: DEF_REG's supplier runs at ITEM registration time, and
        // BuiltInRegistries declares ENTITY_TYPE (145) before ITEM (148), so type.get() is bound.
        // Fabric never had the item at all, so it joins that arm from 1.20.1 rather than at 1.21.3 —
        // the ordering argument above is vanilla's, so it holds a version band earlier just as well.
        // 1.21.4 took the two colours off the constructor: a spawn egg's look is an item model
        // definition now, and DataPackMigration#retemplateSpawnEggs writes both tints into
        // assets/alexscaves/items/spawn_egg_<name>.json from this very call's hex literals — which is
        // why color1/color2 stay parameters here rather than being dropped.
        // 1.21.9 took the entity type off the constructor as well: a spawn egg carries it as a
        // default ENTITY_DATA component, which Item.Properties#spawnEgg installs. Same value, one
        // step later — and it is what getType(stack) reads back, so spawnEggType below follows.
        //? if >=1.21.9 {
        /*Supplier<Item> item = DEF_REG.register("spawn_egg_" + entityName, () -> new SpawnEggItem(new Item.Properties().spawnEgg((EntityType<?>) type.get())));
        *///?} elif >=1.21.4 {
        /*@SuppressWarnings("unchecked")
        Supplier<Item> item = DEF_REG.register("spawn_egg_" + entityName, () -> new SpawnEggItem((EntityType<? extends net.minecraft.world.entity.Mob>) type.get(), new Item.Properties()));
        *///?} elif >=1.21.3 || fabric {
        /*@SuppressWarnings("unchecked")
        Supplier<Item> item = DEF_REG.register("spawn_egg_" + entityName, () -> new SpawnEggItem((EntityType<? extends net.minecraft.world.entity.Mob>) type.get(), color1, color2, new Item.Properties()));
        *///?} else {
        Supplier<Item> item = DEF_REG.register("spawn_egg_" + entityName, () -> new ForgeSpawnEggItem(type, color1, color2, new Item.Properties()));
        //?}
        creativeTabSpawnEggMap.put(item, biomeTab);
    }

    public static void setup() {
        PRIMORDIAL_ARMOR_MATERIAL.setRepairMaterial(Ingredient.of(TOUGH_HIDE.get()));
        HAZMAT_SUIT_ARMOR_MATERIAL.setRepairMaterial(Ingredient.of(POLYMER_PLATE.get()));
        DIVING_SUIT_ARMOR_MATERIAL.setRepairMaterial(Ingredient.of(Items.COPPER_INGOT));
        DARKNESS_ARMOR_MATERIAL.setRepairMaterial(Ingredient.of(DARK_TATTERS.get()));
        GINGERBREAD_ARMOR_MATERIAL.setRepairMaterial(Ingredient.of(GINGERBREAD_CRUMBS.get()));
        DispenserBlock.registerBehavior(SEEKING_ARROW.get(), new AbstractProjectileDispenseBehavior() {
            protected Projectile getProjectile(Level level, Position position, ItemStack itemStack) {
                AbstractArrow abstractarrow = new SeekingArrowEntity(level, position.x(), position.y(), position.z());
                abstractarrow.pickup = AbstractArrow.Pickup.ALLOWED;
                return abstractarrow;
            }
        });
        DispenserBlock.registerBehavior(GALENA_GAUNTLET.get(), ArmorItem.DISPENSE_ITEM_BEHAVIOR);
        DispenserBlock.registerBehavior(TRILOCARIS_BUCKET.get(), new FluidContainerDispenseItemBehavior());
        DispenserBlock.registerBehavior(ACID_BUCKET.get(), new FluidContainerDispenseItemBehavior());
        DispenserBlock.registerBehavior(RADGILL_BUCKET.get(), new FluidContainerDispenseItemBehavior());
        DispenserBlock.registerBehavior(CINDER_BRICK.get(), new AbstractProjectileDispenseBehavior() {
            protected Projectile getProjectile(Level level, Position position, ItemStack itemStack) {
                return new CinderBrickEntity(level, position.x(), position.y(), position.z());
            }
        });
        DispenserBlock.registerBehavior(LANTERNFISH_BUCKET.get(), new FluidContainerDispenseItemBehavior());
        DispenserBlock.registerBehavior(TRIPODFISH_BUCKET.get(), new FluidContainerDispenseItemBehavior());
        DispenserBlock.registerBehavior(SEA_PIG_BUCKET.get(), new FluidContainerDispenseItemBehavior());
        DispenserBlock.registerBehavior(GOSSAMER_WORM_BUCKET.get(), new FluidContainerDispenseItemBehavior());
        DispenserBlock.registerBehavior(INK_BOMB.get(), new AbstractProjectileDispenseBehavior() {
            protected Projectile getProjectile(Level level, Position position, ItemStack itemStack) {
                return new InkBombEntity(level, position.x(), position.y(), position.z());
            }
        });
        DispenserBlock.registerBehavior(GLOW_INK_BOMB.get(), new AbstractProjectileDispenseBehavior() {
            protected Projectile getProjectile(Level level, Position position, ItemStack itemStack) {
                InkBombEntity inkBombEntity = new InkBombEntity(level, position.x(), position.y(), position.z());
                inkBombEntity.setGlowingBomb(true);
                inkBombEntity.setItem(itemStack);
                return inkBombEntity;
            }
        });
        DispenserBlock.registerBehavior(GUANO.get(), new AbstractProjectileDispenseBehavior() {
            protected Projectile getProjectile(Level level, Position position, ItemStack itemStack) {
                return new GuanoEntity(level, position.x(), position.y(), position.z());
            }
        });
        DispenserBlock.registerBehavior(BURROWING_ARROW.get(), new AbstractProjectileDispenseBehavior() {
            protected Projectile getProjectile(Level level, Position position, ItemStack itemStack) {
                AbstractArrow abstractarrow = new BurrowingArrowEntity(level, position.x(), position.y(), position.z());
                abstractarrow.pickup = AbstractArrow.Pickup.ALLOWED;
                return abstractarrow;
            }
        });
        DispenserBlock.registerBehavior(ACBlockRegistry.NUCLEAR_BOMB.get(), new DefaultDispenseItemBehavior() {
            protected ItemStack execute(BlockSource blockSource, ItemStack itemStack) {
                Level level = ACPlatform.dispenserLevel(blockSource);
                BlockPos blockpos = ACPlatform.dispenserPos(blockSource).relative(ACPlatform.dispenserState(blockSource).getValue(DispenserBlock.FACING));
                NuclearBombEntity nuclearBomb = new NuclearBombEntity(level, (double)blockpos.getX() + 0.5D, (double)blockpos.getY(), (double)blockpos.getZ() + 0.5D);
                level.addFreshEntity(nuclearBomb);
                level.playSound((Player)null, nuclearBomb.getX(), nuclearBomb.getY(), nuclearBomb.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent((Entity)null, GameEvent.ENTITY_PLACE, blockpos);
                itemStack.shrink(1);
                return itemStack;
            }
        });
        DispenserBlock.registerBehavior(PURPLE_SODA_BUCKET.get(), new FluidContainerDispenseItemBehavior());
        DispenserBlock.registerBehavior(SWEETISH_FISH_RED_BUCKET.get(), new FluidContainerDispenseItemBehavior());
        DispenserBlock.registerBehavior(SWEETISH_FISH_GREEN_BUCKET.get(), new FluidContainerDispenseItemBehavior());
        DispenserBlock.registerBehavior(SWEETISH_FISH_BLUE_BUCKET.get(), new FluidContainerDispenseItemBehavior());
        DispenserBlock.registerBehavior(SWEETISH_FISH_YELLOW_BUCKET.get(), new FluidContainerDispenseItemBehavior());
        DispenserBlock.registerBehavior(SWEETISH_FISH_PINK_BUCKET.get(), new FluidContainerDispenseItemBehavior());
        LecternBooks.BOOKS.put(BuiltInRegistries.ITEM.getKey(CAVE_BOOK.get()), new LecternBooks.BookData(0X81301C, 0XFDF8EC));
        ComposterBlock.COMPOSTABLES.put(PINE_NUTS.get(), 0.5F);
        ComposterBlock.COMPOSTABLES.put(PEWEN_SAP.get(), 0.2F);
        ComposterBlock.COMPOSTABLES.put(ACBlockRegistry.PEWEN_SAPLING.get().asItem(), 0.3F);
        ComposterBlock.COMPOSTABLES.put(ACBlockRegistry.PEWEN_PINES.get().asItem(), 0.3F);
        ComposterBlock.COMPOSTABLES.put(ACBlockRegistry.PEWEN_BRANCH.get().asItem(), 0.3F);
        ComposterBlock.COMPOSTABLES.put(ACBlockRegistry.ANCIENT_SAPLING.get().asItem(), 0.3F);
        ComposterBlock.COMPOSTABLES.put(ACBlockRegistry.ANCIENT_LEAVES.get().asItem(), 0.3F);
        ComposterBlock.COMPOSTABLES.put(ACBlockRegistry.FIDDLEHEAD.get().asItem(), 0.4F);
        ComposterBlock.COMPOSTABLES.put(ACBlockRegistry.CURLY_FERN.get().asItem(), 0.3F);
        ComposterBlock.COMPOSTABLES.put(ACBlockRegistry.FLYTRAP.get().asItem(), 0.65F);
        ComposterBlock.COMPOSTABLES.put(ACBlockRegistry.CYCAD.get().asItem(), 0.65F);
        ComposterBlock.COMPOSTABLES.put(ACBlockRegistry.TREE_STAR.get().asItem(), 0.65F);
        ComposterBlock.COMPOSTABLES.put(ACBlockRegistry.ARCHAIC_VINE.get().asItem(), 0.5F);
        ComposterBlock.COMPOSTABLES.put(ACBlockRegistry.FERN_THATCH.get().asItem(), 0.85F);
        ComposterBlock.COMPOSTABLES.put(ACBlockRegistry.UNDERWEED.get().asItem(), 0.3F);
        ComposterBlock.COMPOSTABLES.put(ACBlockRegistry.THORNWOOD_BRANCH.get().asItem(), 0.3F);
        ComposterBlock.COMPOSTABLES.put(ACBlockRegistry.THORNWOOD_SAPLING.get().asItem(), 0.3F);

    }

    public static List<Supplier<Item>> getSpawnEggsForTab(ResourceKey<Biome> tabName) {
        List<Supplier<Item>> list = new ArrayList();
        for (Map.Entry<Supplier<Item>, ResourceKey<Biome>> entry : creativeTabSpawnEggMap.entrySet()) {
            if (entry.getValue().equals(tabName)) {
                list.add(entry.getKey());
            }
        }
        return list;
    }

    public static Item getSpawnEggFor(EntityType type) {
        for (Map.Entry<Supplier<Item>, ResourceKey<Biome>> entry : creativeTabSpawnEggMap.entrySet()) {
            if (entry.getKey().get() instanceof SpawnEggItem spawnEggItem && spawnEggType(spawnEggItem) == type) {
                return spawnEggItem;
            }
        }
        return Items.AIR;
    }

    // Matching on the vanilla supertype rather than the loader subclass keeps this one line on every
    // node. getType took a @Nullable CompoundTag up to 1.20.4 and an ItemStack from 1.20.5, where it
    // dereferences the stack immediately — so upstream's null argument is an NPE on every node above
    // 1.20.4. ItemStack.EMPTY carries a real (empty) component map and reads back the default type.
    // 1.21.4 threads a HolderLookup.Provider in front of the stack, used only to resolve an
    // entity-type id written onto the stack; an empty stack never reaches it, so null is safe here
    // (javap-checked: getOrDefault → CustomData.EMPTY → isEmpty → returns the default type).
    private static EntityType<?> spawnEggType(SpawnEggItem egg) {
        // 1.21.9 dropped the lookup argument again, and with the type now living in the stack's
        // ENTITY_DATA component there is no default left to fall back on — an empty stack reads
        // back null. The egg's own default instance is the stack that carries it.
        //? if >=1.21.9 {
        /*return egg.getType(egg.getDefaultInstance());
        *///?} elif >=1.21.4 {
        /*return egg.getType(null, ItemStack.EMPTY);
        *///?} elif >=1.20.5 {
        /*return egg.getType(ItemStack.EMPTY);
        *///?} else {
        return egg.getType(null);
        //?}
    }
}
