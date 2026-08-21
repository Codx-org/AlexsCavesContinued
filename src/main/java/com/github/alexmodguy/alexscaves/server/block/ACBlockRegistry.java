package com.github.alexmodguy.alexscaves.server.block;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.model.ConversionCrucibleModel;
import com.github.alexmodguy.alexscaves.server.block.fluid.ACFluidRegistry;
import com.github.alexmodguy.alexscaves.server.block.grower.AncientTreeGrower;
import com.github.alexmodguy.alexscaves.server.block.grower.LicorootGrower;
import com.github.alexmodguy.alexscaves.server.block.grower.PewenGrower;
import com.github.alexmodguy.alexscaves.server.block.grower.ThornwoodGrower;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACDeferredRegister;
import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;
import com.github.alexmodguy.alexscaves.server.item.*;
import com.github.alexmodguy.alexscaves.citadel.item.BlockItemWithSupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ACBlockRegistry {

    public static final BlockBehaviour.Properties GALENA_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(3.5F, 10.0F).sound(SoundType.DEEPSLATE);
    public static final BlockBehaviour.Properties ENERGIZED_GALENA_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops().strength(3F, 10.0F).lightLevel(state -> 5).sound(SoundType.DEEPSLATE);
    public static final BlockBehaviour.Properties LIMESTONE_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_YELLOW).requiresCorrectToolForDrops().strength(1.2F, 4.5F).sound(SoundType.DRIPSTONE_BLOCK);
    public static final BlockBehaviour.Properties PEWEN_LOG_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.CHERRY_WOOD).instrument(NoteBlockInstrument.BASS);
    public static final BlockBehaviour.Properties PEWEN_PLANKS_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.CHERRY_WOOD).instrument(NoteBlockInstrument.BASS);
    public static final BlockBehaviour.Properties RADROCK_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).requiresCorrectToolForDrops().strength(4F, 11.0F).sound(ACSoundTypes.RADROCK);
    public static final BlockBehaviour.Properties CINDER_BLOCK_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).requiresCorrectToolForDrops().strength(5F, 20.0F).sound(ACSoundTypes.CINDER_BLOCK);
    public static final BlockBehaviour.Properties RADON_LAMP_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).requiresCorrectToolForDrops().lightLevel(state -> 15).strength(2F, 11.0F).sound(SoundType.GLASS);
    public static final BlockBehaviour.Properties SMOOTH_BONE_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.SAND).requiresCorrectToolForDrops().strength(2.0F).sound(SoundType.BONE_BLOCK);
    public static final BlockBehaviour.Properties ABYSSMARINE_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).requiresCorrectToolForDrops().strength(2.5F, 50.0F).sound(SoundType.DEEPSLATE);
    public static final BlockBehaviour.Properties GUANOSTONE_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_CYAN).requiresCorrectToolForDrops().strength(1.3F, 2.0F).sound(SoundType.BASALT);
    public static final BlockBehaviour.Properties COPROLITH_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).requiresCorrectToolForDrops().strength(1.75F, 4.0F).sound(SoundType.CALCITE);
    public static final BlockBehaviour.Properties POROUS_COPROLITH_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).requiresCorrectToolForDrops().strength(1.75F, 4.0F).sound(SoundType.CALCITE).noOcclusion();
    public static final BlockBehaviour.Properties PEERING_COPROLITH_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).requiresCorrectToolForDrops().strength(1.75F, 4.0F).sound(ACSoundTypes.PEERING_COPROLITH).noOcclusion();
    public static final BlockBehaviour.Properties THORNWOOD_LOG_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).instrument(NoteBlockInstrument.BASS);
    public static final BlockBehaviour.Properties THORNWOOD_PLANKS_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD).instrument(NoteBlockInstrument.BASS);
    public static final BlockBehaviour.Properties CHOCOLATE_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(1.0F, 2.0F).sound(ACSoundTypes.DENSE_CANDY).instrument(NoteBlockInstrument.BASEDRUM);
    public static final BlockBehaviour.Properties COOKIE_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F, 1.5F).sound(ACSoundTypes.DENSE_CANDY).instrument(NoteBlockInstrument.BASEDRUM);
    public static final BlockBehaviour.Properties DOUGH_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0F, 1.5F).sound(SoundType.WOOL).instrument(NoteBlockInstrument.BASS);
    public static final BlockBehaviour.Properties LICOROOT_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.0F, 1.5F).sound(SoundType.NETHER_WOOD).instrument(NoteBlockInstrument.SNARE);
    public static final BlockBehaviour.Properties ROCK_CANDY_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(1.0F, 1.5F).sound(SoundType.STONE).instrument(NoteBlockInstrument.BASS);
    public static final BlockBehaviour.Properties GINGERBREAD_PROPERTIES = BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(1.0F, 1.5F).sound(ACSoundTypes.DENSE_CANDY).instrument(NoteBlockInstrument.BASS);
    public static final WoodType PEWEN_WOOD_TYPE = WoodType.register(new WoodType("alexscaves:pewen", BlockSetType.OAK));
    public static final WoodType THORNWOOD_WOOD_TYPE = WoodType.register(new WoodType("alexscaves:thornwood", BlockSetType.OAK));

    public static final ACDeferredRegister<Block> DEF_REG = ACDeferredRegister.create(Registries.BLOCK, AlexsCaves.MODID);
    public static final Supplier<Block> SPELUNKERY_TABLE = registerBlockAndItem("spelunkery_table", () -> new SpelunkeryTableBlock());
    public static final Supplier<Block> GALENA = registerBlockAndItem("galena", () -> new Block(GALENA_PROPERTIES));
    public static final Supplier<Block> GALENA_STAIRS = registerBlockAndItem("galena_stairs", () -> new StairBlock(GALENA.get().defaultBlockState(), GALENA_PROPERTIES));
    public static final Supplier<Block> GALENA_SLAB = registerBlockAndItem("galena_slab", () -> new SlabBlock(GALENA_PROPERTIES));
    public static final Supplier<Block> GALENA_WALL = registerBlockAndItem("galena_wall", () -> new WallBlock(GALENA_PROPERTIES));
    public static final Supplier<Block> PACKED_GALENA = registerBlockAndItem("packed_galena", () -> new Block(GALENA_PROPERTIES));
    public static final Supplier<Block> GALENA_BRICKS = registerBlockAndItem("galena_bricks", () -> new Block(GALENA_PROPERTIES));
    public static final Supplier<Block> GALENA_BRICK_STAIRS = registerBlockAndItem("galena_brick_stairs", () -> new StairBlock(GALENA_BRICKS.get().defaultBlockState(), GALENA_PROPERTIES));
    public static final Supplier<Block> GALENA_BRICK_SLAB = registerBlockAndItem("galena_brick_slab", () -> new SlabBlock(GALENA_PROPERTIES));
    public static final Supplier<Block> GALENA_BRICK_WALL = registerBlockAndItem("galena_brick_wall", () -> new WallBlock(GALENA_PROPERTIES));
    public static final Supplier<Block> GALENA_PILLAR = registerBlockAndItem("galena_pillar", () -> new GalenaPillarBlock());
    public static final Supplier<Block> GALENA_IRON_ORE = registerBlockAndItem("galena_iron_ore", () -> new Block(ACPlatform.copyProperties(Blocks.IRON_ORE).sound(SoundType.DEEPSLATE)));
    public static final Supplier<Block> ENERGIZED_GALENA_NEUTRAL = registerBlockAndItem("energized_galena_neutral", () -> new EnergizedGalenaBlock(ENERGIZED_GALENA_PROPERTIES));
    public static final Supplier<Block> ENERGIZED_GALENA_SCARLET = registerBlockAndItem("energized_galena_scarlet", () -> new EnergizedGalenaBlock(ENERGIZED_GALENA_PROPERTIES));
    public static final Supplier<Block> ENERGIZED_GALENA_AZURE = registerBlockAndItem("energized_galena_azure", () -> new EnergizedGalenaBlock(ENERGIZED_GALENA_PROPERTIES));
    public static final Supplier<Block> GALENA_SPIRE = registerBlockAndItem("galena_spire", () -> new GalenaSpireBlock());
    public static final Supplier<Block> TESLA_BULB = registerBlockAndItem("tesla_bulb", () -> new TeslaBulbBlock());
    public static final Supplier<Block> METAL_SWARF = registerBlockAndItem("metal_swarf", () -> new FallingBlockWithColor(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.6F).sound(ACSoundTypes.METAL_SWARF), 0X404253));
    public static final Supplier<Block> SCRAP_METAL = registerBlockAndItem("scrap_metal", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5F, 15.0F).sound(ACSoundTypes.SCRAP_METAL)));
    public static final Supplier<Block> SCRAP_METAL_PLATE = registerBlockAndItem("scrap_metal_plate", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5F, 15.0F).sound(ACSoundTypes.SCRAP_METAL)));
    public static final Supplier<Block> METAL_REBAR = registerBlockAndItem("metal_rebar", () -> new RebarBlock());
    public static final Supplier<Block> METAL_SCAFFOLDING = registerBlockAndItem("metal_scaffolding", () -> new MetalScaffoldingBlock(), 2);
    public static final Supplier<Block> MAGNETIC_ACTIVATOR = registerBlockAndItem("magnetic_activator", () -> new MagneticActivatorBlock());
    public static final Supplier<Block> SCARLET_NEODYMIUM_NODE = registerBlockAndItem("scarlet_neodymium_node", () -> new NeodymiumNodeBlock(false));
    public static final Supplier<Block> AZURE_NEODYMIUM_NODE = registerBlockAndItem("azure_neodymium_node", () -> new NeodymiumNodeBlock(true));
    public static final Supplier<Block> SCARLET_NEODYMIUM_PILLAR = registerBlockAndItem("scarlet_neodymium_pillar", () -> new NeodymiumPillarBlock(false));
    public static final Supplier<Block> AZURE_NEODYMIUM_PILLAR = registerBlockAndItem("azure_neodymium_pillar", () -> new NeodymiumPillarBlock(true));
    public static final Supplier<Block> BLOCK_OF_SCARLET_NEODYMIUM = registerBlockAndItem("block_of_scarlet_neodymium", () -> new NeodymiumOreBlock(false));
    public static final Supplier<Block> BLOCK_OF_AZURE_NEODYMIUM = registerBlockAndItem("block_of_azure_neodymium", () -> new NeodymiumOreBlock(true));
    public static final Supplier<Block> SCARLET_MAGNET = registerBlockAndItem("scarlet_magnet", () -> new MagnetBlock(false));
    public static final Supplier<Block> AZURE_MAGNET = registerBlockAndItem("azure_magnet", () -> new MagnetBlock(true));
    public static final Supplier<Block> HEART_OF_IRON = registerBlockAndItem("heart_of_iron", () -> new HeartOfIronBlock());
    public static final Supplier<Block> HOLOGRAM_PROJECTOR = registerBlockAndItem("hologram_projector", () -> new HologramProjectorBlock());
    public static final Supplier<Block> MAGNETIC_LIGHT = registerBlockAndItem("magnetic_light", () -> new MagneticLightBlock());
    public static final Supplier<Block> MAGNETIC_LEVITATION_RAIL = registerBlockAndItem("magnetic_levitation_rail", () -> new MagneticLevitationRailBlock());
    public static final Supplier<Block> QUARRY = registerBlockAndItem("quarry", () -> new QuarryBlock());
    public static final Supplier<Block> LIMESTONE = registerBlockAndItem("limestone", () -> new Block(LIMESTONE_PROPERTIES));
    public static final Supplier<Block> LIMESTONE_STAIRS = registerBlockAndItem("limestone_stairs", () -> new StairBlock(LIMESTONE.get().defaultBlockState(), LIMESTONE_PROPERTIES));
    public static final Supplier<Block> LIMESTONE_SLAB = registerBlockAndItem("limestone_slab", () -> new SlabBlock(LIMESTONE_PROPERTIES));
    public static final Supplier<Block> LIMESTONE_WALL = registerBlockAndItem("limestone_wall", () -> new WallBlock(LIMESTONE_PROPERTIES));
    public static final Supplier<Block> LIMESTONE_PILLAR = registerBlockAndItem("limestone_pillar", () -> new RotatedPillarBlock(LIMESTONE_PROPERTIES));
    public static final Supplier<Block> LIMESTONE_CHISELED = registerBlockAndItem("limestone_chiseled", () -> new DirectionalFacingBlock(LIMESTONE_PROPERTIES, true));
    public static final Supplier<Block> SMOOTH_LIMESTONE = registerBlockAndItem("smooth_limestone", () -> new SmoothLimestoneBlock(LIMESTONE_PROPERTIES));
    public static final Supplier<Block> SMOOTH_LIMESTONE_STAIRS = registerBlockAndItem("smooth_limestone_stairs", () -> new StairBlock(SMOOTH_LIMESTONE.get().defaultBlockState(), LIMESTONE_PROPERTIES));
    public static final Supplier<Block> SMOOTH_LIMESTONE_SLAB = registerBlockAndItem("smooth_limestone_slab", () -> new SlabBlock(LIMESTONE_PROPERTIES));
    public static final Supplier<Block> SMOOTH_LIMESTONE_WALL = registerBlockAndItem("smooth_limestone_wall", () -> new WallBlock(LIMESTONE_PROPERTIES));
    public static final Supplier<Block> CAVE_PAINTING_AMBERSOL = registerBlockAndItem("cave_painting_ambersol", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_DARK = registerBlockAndItem("cave_painting_dark", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_FOOTPRINT = registerBlockAndItem("cave_painting_footprint", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_FOOTPRINTS = registerBlockAndItem("cave_painting_footprints", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_TREE_STARS = registerBlockAndItem("cave_painting_tree_stars", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_PEWEN = registerBlockAndItem("cave_painting_pewen", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_TRILOCARIS = registerBlockAndItem("cave_painting_trilocaris", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_GROTTOCERATOPS = registerBlockAndItem("cave_painting_grottoceratops", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_GROTTOCERATOPS_FRIEND = registerBlockAndItem("cave_painting_grottoceratops_friend", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_DINO_NUGGETS = registerBlockAndItem("cave_painting_dino_nuggets", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_VALLUMRAPTOR_CHEST = registerBlockAndItem("cave_painting_vallumraptor_chest", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_VALLUMRAPTOR_FRIEND = registerBlockAndItem("cave_painting_vallumraptor_friend", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_RELICHEIRUS = registerBlockAndItem("cave_painting_relicheirus", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_RELICHEIRUS_SLASH = registerBlockAndItem("cave_painting_relicheirus_slash", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_ENDERMAN = registerBlockAndItem("cave_painting_enderman", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_PORTAL = registerBlockAndItem("cave_painting_portal", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_SUBTERRANODON = registerBlockAndItem("cave_painting_subterranodon", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_SUBTERRANODON_RIDE = registerBlockAndItem("cave_painting_subterranodon_ride", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_TREMORSAURUS = registerBlockAndItem("cave_painting_tremorsaurus", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_TREMORSAURUS_FRIEND = registerBlockAndItem("cave_painting_tremorsaurus_friend", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_MYSTERY_1 = registerBlockAndItem("cave_painting_mystery_1", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_MYSTERY_2 = registerBlockAndItem("cave_painting_mystery_2", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_MYSTERY_3 = registerBlockAndItem("cave_painting_mystery_3", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_MYSTERY_4 = registerBlockAndItem("cave_painting_mystery_4", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_MYSTERY_5 = registerBlockAndItem("cave_painting_mystery_5", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_MYSTERY_6 = registerBlockAndItem("cave_painting_mystery_6", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_MYSTERY_7 = registerBlockAndItem("cave_painting_mystery_7", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_MYSTERY_8 = registerBlockAndItem("cave_painting_mystery_8", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> CAVE_PAINTING_MYSTERY_9 = registerBlockAndItem("cave_painting_mystery_9", () -> new CavePaintingBlock(), 1);
    public static final Supplier<Block> AMBER = registerBlockAndItem("amber", () -> new ACTransparentBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).noOcclusion().requiresCorrectToolForDrops().strength(0.3F, 2.0F).sound(ACSoundTypes.AMBER)));
    public static final Supplier<Block> AMBERSOL = registerBlockAndItem("ambersol", () -> new AmbersolBlock());
    public static final Supplier<Block> AMBERSOL_LIGHT = DEF_REG.register("ambersol_light", () -> new AmbersolLightBlock(BlockBehaviour.Properties.of().noOcclusion().strength(-1.0F, 3600000.8F).noLootTable().noOcclusion().replaceable().lightLevel(((state -> 15)))));
    public static final Supplier<Block> AMBER_MONOLITH = registerBlockAndItem("amber_monolith", () -> new AmberMonolithBlock());
    public static final Supplier<Block> SUBTERRANODON_EGG = registerBlockAndItem("subterranodon_egg", () -> new MultipleDinosaurEggsBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(0.5F).sound(SoundType.METAL).randomTicks(), ACEntityRegistry.SUBTERRANODON, 4));
    public static final Supplier<Block> VALLUMRAPTOR_EGG = registerBlockAndItem("vallumraptor_egg", () -> new MultipleDinosaurEggsBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(0.5F).sound(SoundType.METAL).randomTicks(), ACEntityRegistry.VALLUMRAPTOR, 4));
    public static final Supplier<Block> GROTTOCERATOPS_EGG = registerBlockAndItem("grottoceratops_egg", () -> new DinosaurEggBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(0.5F).sound(SoundType.METAL).randomTicks(), ACEntityRegistry.GROTTOCERATOPS, 8, 10));
    public static final Supplier<Block> TREMORSAURUS_EGG = registerBlockAndItem("tremorsaurus_egg", () -> new DinosaurEggBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(0.5F).sound(SoundType.METAL).randomTicks(), ACEntityRegistry.TREMORSAURUS, 10, 16));
    public static final Supplier<Block> RELICHEIRUS_EGG = registerBlockAndItem("relicheirus_egg", () -> new DinosaurEggBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(0.5F).sound(SoundType.METAL).randomTicks(), ACEntityRegistry.RELICHEIRUS, 14, 16));
    public static final Supplier<Block> ATLATITAN_EGG = registerBlockAndItem("atlatitan_egg", () -> new DinosaurEggBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(0.5F).sound(SoundType.METAL).randomTicks(), ACEntityRegistry.ATLATITAN, 16, 16));
    public static final Supplier<Block> DINOSAUR_CHOP = registerBlockAndItem("dinosaur_chop", () -> new DinosaurChopBlock(3, 0.2F));
    public static final Supplier<Block> COOKED_DINOSAUR_CHOP = registerBlockAndItem("cooked_dinosaur_chop", () -> new DinosaurChopBlock(7, 0.35F));
    public static final Supplier<Block> CARMINE_FROGLIGHT = registerBlockAndItem("carmine_froglight", () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.3F).lightLevel((blockState) -> 15).sound(SoundType.FROGLIGHT)));
    public static final Supplier<Block> PEWEN_LOG = registerBlockAndItem("pewen_log", () -> new StrippableLogBlock(PEWEN_LOG_PROPERTIES));
    public static final Supplier<Block> PEWEN_WOOD = registerBlockAndItem("pewen_wood", () -> new StrippableLogBlock(PEWEN_LOG_PROPERTIES));
    public static final Supplier<Block> STRIPPED_PEWEN_LOG = registerBlockAndItem("stripped_pewen_log", () -> new RotatedPillarBlock(PEWEN_LOG_PROPERTIES));
    public static final Supplier<Block> STRIPPED_PEWEN_WOOD = registerBlockAndItem("stripped_pewen_wood", () -> new RotatedPillarBlock(PEWEN_LOG_PROPERTIES));
    public static final Supplier<Block> PEWEN_PLANKS = registerBlockAndItem("pewen_planks", () -> new Block(PEWEN_PLANKS_PROPERTIES));
    public static final Supplier<Block> PEWEN_PLANKS_STAIRS = registerBlockAndItem("pewen_stairs", () -> new StairBlock(PEWEN_PLANKS.get().defaultBlockState(), PEWEN_PLANKS_PROPERTIES));
    public static final Supplier<Block> PEWEN_PLANKS_SLAB = registerBlockAndItem("pewen_slab", () -> new SlabBlock(PEWEN_PLANKS_PROPERTIES));
    public static final Supplier<Block> PEWEN_PLANKS_FENCE = registerBlockAndItem("pewen_fence", () -> new FenceBlock(PEWEN_PLANKS_PROPERTIES));
    public static final Supplier<Block> PEWEN_SIGN = DEF_REG.register("pewen_sign", () -> ACBlockFactory.standingSign(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).noCollission().strength(1.0F).sound(SoundType.CHERRY_WOOD), PEWEN_WOOD_TYPE));
    public static final Supplier<Block> PEWEN_WALL_SIGN = DEF_REG.register("pewen_wall_sign", () -> ACBlockFactory.wallSign(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).noCollission().strength(1.0F).sound(SoundType.CHERRY_WOOD), PEWEN_WOOD_TYPE));
    public static final Supplier<Block> PEWEN_HANGING_SIGN = DEF_REG.register("pewen_hanging_sign", () -> ACBlockFactory.hangingSign(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollission().strength(1.0F), PEWEN_WOOD_TYPE));
    public static final Supplier<Block> PEWEN_WALL_HANGING_SIGN = DEF_REG.register("pewen_wall_hanging_sign", () -> ACBlockFactory.wallHangingSign(ACCompat.dropsLike(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollission().strength(1.0F), PEWEN_HANGING_SIGN.get()), PEWEN_WOOD_TYPE));
    public static final Supplier<Block> PEWEN_PRESSURE_PLATE = registerBlockAndItem("pewen_pressure_plate", () -> ACBlockFactory.pressurePlate(ACPlatform.copyProperties(PEWEN_PLANKS.get()).noCollission().strength(0.5F).sound(SoundType.CHERRY_WOOD), BlockSetType.CHERRY));
    public static final Supplier<Block> PEWEN_TRAPDOOR = registerBlockAndItem("pewen_trapdoor", () -> ACBlockFactory.trapDoor(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(3.0F).sound(SoundType.CHERRY_WOOD).noOcclusion(), BlockSetType.CHERRY));
    public static final Supplier<Block> PEWEN_BUTTON = registerBlockAndItem("pewen_button", () -> ACBlockFactory.button(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).noCollission().strength(0.5F).sound(SoundType.CHERRY_WOOD), BlockSetType.CHERRY, 30, true));
    public static final Supplier<Block> PEWEN_FENCE_GATE = registerBlockAndItem("pewen_fence_gate", () -> ACBlockFactory.fenceGate(ACPlatform.copyProperties(PEWEN_PLANKS.get()).strength(2.0F, 3.0F).sound(SoundType.CHERRY_WOOD).forceSolidOn(), PEWEN_WOOD_TYPE, SoundEvents.CHERRY_WOOD_FENCE_GATE_CLOSE, SoundEvents.CHERRY_WOOD_FENCE_GATE_CLOSE));
    public static final Supplier<Block> PEWEN_DOOR = DEF_REG.register("pewen_door", () -> ACBlockFactory.door(ACPlatform.copyProperties(PEWEN_PLANKS.get()).strength(3.0F).sound(SoundType.CHERRY_WOOD).noOcclusion(), BlockSetType.CHERRY));
    public static final Supplier<Block> PEWEN_BRANCH = registerBlockAndItem("pewen_branch", () -> new PewenBranchBlock());
    public static final Supplier<Block> PEWEN_PINES = registerBlockAndItem("pewen_pines", () -> new PewenPinesBlock());
    public static final Supplier<Block> POTTED_PEWEN_PINES = DEF_REG.register("potted_pewen_pines", () -> ACBlockFactory.flowerPot(PEWEN_PINES, BlockBehaviour.Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final Supplier<Block> PEWEN_SAPLING = registerBlockAndItem("pewen_sapling", () -> new CaveSaplingBlock(new PewenGrower(), BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).noCollission().randomTicks().instabreak().sound(SoundType.GRASS), true));
    public static final Supplier<Block> POTTED_PEWEN_SAPLING = DEF_REG.register("potted_pewen_sapling", () -> ACBlockFactory.flowerPot(PEWEN_SAPLING, BlockBehaviour.Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final Supplier<Block> FIDDLEHEAD = registerBlockAndItem("fiddlehead", () -> new FiddleheadBlock());
    public static final Supplier<Block> POTTED_FIDDLEHEAD = DEF_REG.register("potted_fiddlehead", () -> ACBlockFactory.flowerPot(FIDDLEHEAD, BlockBehaviour.Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final Supplier<Block> CURLY_FERN = registerBlockAndItem("curly_fern", () -> new DoublePlantWithRotationBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).noCollission().instabreak().sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ)));
    public static final Supplier<Block> POTTED_CURLY_FERN = DEF_REG.register("potted_curly_fern", () -> ACBlockFactory.flowerPot(CURLY_FERN, BlockBehaviour.Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final Supplier<Block> FLYTRAP = registerBlockAndItem("flytrap", () -> new FlytrapBlock());
    public static final Supplier<Block> POTTED_FLYTRAP = DEF_REG.register("potted_flytrap", () -> new PottedFlytrapBlock());
    public static final Supplier<Block> CYCAD = registerBlockAndItem("cycad", () -> new CycadBlock());
    public static final Supplier<Block> POTTED_CYCAD = DEF_REG.register("potted_cycad", () -> ACBlockFactory.flowerPot(CYCAD, BlockBehaviour.Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final Supplier<Block> ARCHAIC_VINE = registerBlockAndItem("archaic_vine", () -> new ArchaicVineBlock());
    public static final Supplier<Block> ARCHAIC_VINE_PLANT = DEF_REG.register("archaic_vine_plant", () -> new ArchaicVinePlantBlock());
    // 1.21.5 made LeavesBlock abstract, because a leaf block now says how its falling-leaf particle
    // is coloured. A chance of zero is the pre-1.21.5 behaviour exactly — LeavesBlock#randomTick
    // tests `random.nextFloat() >= chance`, which is always true at 0 — so nothing spawns and the
    // subclass choice is invisible. Ancient leaves have no colour handler (their texture carries its
    // own colour rather than being biome-tinted), so giving them vanilla's 1% would need a leaf
    // colour that is spelled nowhere in this mod; that is a look-and-feel decision, not a port.
    //? if >=1.21.5
    /*public static final Supplier<Block> ANCIENT_LEAVES = registerBlockAndItem("ancient_leaves", () -> new net.minecraft.world.level.block.TintedParticleLeavesBlock(0.0F, BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).strength(0.2F).randomTicks().sound(SoundType.GRASS).noOcclusion().isSuffocating((blockState, getter, pos) -> false)));*/
    //? if <1.21.5
    public static final Supplier<Block> ANCIENT_LEAVES = registerBlockAndItem("ancient_leaves", () -> new LeavesBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).strength(0.2F).randomTicks().sound(SoundType.GRASS).noOcclusion().isSuffocating((blockState, getter, pos) -> false)));
    public static final Supplier<Block> ANCIENT_SAPLING = registerBlockAndItem("ancient_sapling", () -> new CaveSaplingBlock(new AncientTreeGrower(), BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).noCollission().randomTicks().instabreak().sound(SoundType.GRASS), true));
    public static final Supplier<Block> POTTED_ANCIENT_SAPLING = DEF_REG.register("potted_ancient_sapling", () -> ACBlockFactory.flowerPot(ANCIENT_SAPLING, BlockBehaviour.Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final Supplier<Block> TREE_STAR = registerBlockAndItem("tree_star", () -> new TreeStarBlock());
    public static final Supplier<Block> FERN_THATCH = registerBlockAndItem("fern_thatch", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.5F).sound(SoundType.GRASS).noOcclusion()));
    public static final Supplier<Block> PRIMAL_MAGMA = registerBlockAndItem("primal_magma", () -> new PrimalMagmaBlock());
    public static final Supplier<Block> FISSURE_PRIMAL_MAGMA = DEF_REG.register("fissure_primal_magma", () -> new FissurePrimalMagmaBlock());
    public static final Supplier<Block> FLOOD_BASALT = registerBlockAndItem("flood_basalt", () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_RED).strength(3.0F, 100.0F).sound(ACSoundTypes.FLOOD_BASALT).requiresCorrectToolForDrops()));
    public static final Supplier<Block> VOLCANIC_CORE = registerBlockAndItem("volcanic_core", () -> new VolcanicCoreBlock(), 7);
    public static final Supplier<Block> RADROCK = registerBlockAndItem("radrock", () -> new Block(RADROCK_PROPERTIES));
    public static final Supplier<Block> RADROCK_STAIRS = registerBlockAndItem("radrock_stairs", () -> new StairBlock(RADROCK.get().defaultBlockState(), RADROCK_PROPERTIES));
    public static final Supplier<Block> RADROCK_SLAB = registerBlockAndItem("radrock_slab", () -> new SlabBlock(RADROCK_PROPERTIES));
    public static final Supplier<Block> RADROCK_WALL = registerBlockAndItem("radrock_wall", () -> new WallBlock(RADROCK_PROPERTIES));
    public static final Supplier<Block> RADROCK_BRICKS = registerBlockAndItem("radrock_bricks", () -> new Block(RADROCK_PROPERTIES));
    public static final Supplier<Block> RADROCK_BRICK_STAIRS = registerBlockAndItem("radrock_brick_stairs", () -> new StairBlock(RADROCK_BRICKS.get().defaultBlockState(), RADROCK_PROPERTIES));
    public static final Supplier<Block> RADROCK_BRICK_SLAB = registerBlockAndItem("radrock_brick_slab", () -> new SlabBlock(RADROCK_PROPERTIES));
    public static final Supplier<Block> RADROCK_BRICK_WALL = registerBlockAndItem("radrock_brick_wall", () -> new WallBlock(RADROCK_PROPERTIES));
    public static final Supplier<Block> RADROCK_CHISELED = registerBlockAndItem("radrock_chiseled", () -> new Block(RADROCK_PROPERTIES));
    public static final Supplier<Block> RADROCK_URANIUM_ORE = registerBlockAndItem("radrock_uranium_ore", () -> new RadrockUraniumOreBlock(), 4);
    public static final Supplier<Block> ACIDIC_RADROCK = registerBlockAndItem("acidic_radrock", () -> new AcidicRadrockBlock());
    public static final Supplier<Block> GEOTHERMAL_VENT = registerBlockAndItem("geothermal_vent", () -> new GeothermalVentBlock());
    public static final Supplier<Block> GEOTHERMAL_VENT_MEDIUM = registerBlockAndItem("geothermal_vent_medium", () -> new ThinGeothermalVentBlock(12));
    public static final Supplier<Block> GEOTHERMAL_VENT_THIN = registerBlockAndItem("geothermal_vent_thin", () -> new ThinGeothermalVentBlock(8));
    public static final Supplier<LiquidBlock> ACID = DEF_REG.register("acid", () -> new AcidBlock(ACFluidRegistry.ACID_FLUID_SOURCE, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).noCollission().strength(100.0F).lightLevel(state -> 7).emissiveRendering((state, world, pos) -> false).noLootTable().replaceable().liquid().pushReaction(PushReaction.DESTROY)));
    public static final Supplier<Block> UNDERWEED = registerBlockAndItem("underweed", () -> new CavePlantBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).instabreak().offsetType(BlockBehaviour.OffsetType.XZ).sound(SoundType.GRASS).noOcclusion().noCollission().replaceable(), false));
    public static final Supplier<Block> POTTED_UNDERWEED = DEF_REG.register("potted_underweed", () -> ACBlockFactory.flowerPot(UNDERWEED, BlockBehaviour.Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final Supplier<Block> METAL_BARREL = registerBlockAndItem("metal_barrel", () -> new MetalBarrelBlock());
    public static final Supplier<Block> WASTE_DRUM = registerBlockAndItem("waste_drum", () -> new WasteDrumBlock(), 5);
    public static final Supplier<Block> RUSTY_SCRAP_METAL = registerBlockAndItem("rusty_scrap_metal", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5F, 15.0F).sound(ACSoundTypes.SCRAP_METAL)));
    public static final Supplier<Block> RUSTY_SCRAP_METAL_PLATE = registerBlockAndItem("rusty_scrap_metal_plate", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).requiresCorrectToolForDrops().strength(5F, 15.0F).sound(ACSoundTypes.SCRAP_METAL)));
    public static final Supplier<Block> RUSTY_BARREL = registerBlockAndItem("rusty_barrel", () -> new MetalBarrelBlock());
    public static final Supplier<Block> RUSTY_REBAR = registerBlockAndItem("rusty_rebar", () -> new RebarBlock());
    public static final Supplier<Block> RUSTY_SCAFFOLDING = registerBlockAndItem("rusty_scaffolding", () -> new MetalScaffoldingBlock(), 2);
    public static final Supplier<Block> URANIUM_ROD = registerBlockAndItem("uranium_rod", () -> new UraniumRodBlock());
    public static final Supplier<Block> BLOCK_OF_URANIUM = registerBlockAndItem("block_of_uranium", () -> new UraniumFullBlock(), 4);
    public static final Supplier<Block> NUCLEAR_BOMB = registerBlockAndItem("nuclear_bomb", () -> new NuclearBombBlock());
    public static final Supplier<Block> UNREFINED_WASTE = registerBlockAndItem("unrefined_waste", () -> new UnrefinedWasteBlock(), 4);
    public static final Supplier<Block> NUCLEAR_FURNACE_COMPONENT = registerBlockAndItem("nuclear_furnace_component", () -> new NuclearFurnaceComponentBlock());
    public static final Supplier<Block> NUCLEAR_FURNACE = DEF_REG.register("nuclear_furnace", () -> new NuclearFurnaceBlock());
    public static final Supplier<Block> SULFUR = registerBlockAndItem("sulfur", () -> new SulfurBlock());
    public static final Supplier<Block> SULFUR_BUD_SMALL = registerBlockAndItem("sulfur_bud_small", () -> new SulfurBudBlock(6, 4));
    public static final Supplier<Block> SULFUR_BUD_MEDIUM = registerBlockAndItem("sulfur_bud_medium", () -> new SulfurBudBlock(6, 8));
    public static final Supplier<Block> SULFUR_BUD_LARGE = registerBlockAndItem("sulfur_bud_large", () -> new SulfurBudBlock(6, 12));
    public static final Supplier<Block> SULFUR_CLUSTER = registerBlockAndItem("sulfur_cluster", () -> new SulfurBudBlock(6, 14));
    public static final Supplier<Block> CINDER_BLOCK = registerBlockAndItem("cinder_block", () -> new Block(CINDER_BLOCK_PROPERTIES));
    public static final Supplier<Block> CINDER_BLOCK_STAIRS = registerBlockAndItem("cinder_block_stairs", () -> new StairBlock(CINDER_BLOCK.get().defaultBlockState(), CINDER_BLOCK_PROPERTIES));
    public static final Supplier<Block> CINDER_BLOCK_SLAB = registerBlockAndItem("cinder_block_slab", () -> new SlabBlock(CINDER_BLOCK_PROPERTIES));
    public static final Supplier<Block> CINDER_BLOCK_WALL = registerBlockAndItem("cinder_block_wall", () -> new WallBlock(CINDER_BLOCK_PROPERTIES));
    public static final Supplier<Block> HAZMAT_BLOCK = registerBlockAndItem("hazmat_block", () -> new HazmatBlock());
    public static final Supplier<Block> HAZMAT_WARNING_BLOCK = registerBlockAndItem("hazmat_warning_block", () -> new HazmatBlock());
    public static final Supplier<Block> HAZMAT_SKULL_BLOCK = registerBlockAndItem("hazmat_skull_block", () -> new HazmatBlock());
    public static final Supplier<Block> SIREN_LIGHT = registerBlockAndItem("siren_light", () -> new SirenLightBlock(), 3);
    public static final Supplier<Block> NUCLEAR_SIREN = registerBlockAndItem("nuclear_siren", () -> new NuclearSirenBlock());
    public static final Supplier<Block> WHITE_RADON_LAMP = registerBlockAndItem("radon_lamp_white", () -> new Block(RADON_LAMP_PROPERTIES));
    public static final Supplier<Block> ORANGE_RADON_LAMP = registerBlockAndItem("radon_lamp_orange", () -> new Block(RADON_LAMP_PROPERTIES));
    public static final Supplier<Block> MAGENTA_RADON_LAMP = registerBlockAndItem("radon_lamp_magenta", () -> new Block(RADON_LAMP_PROPERTIES));
    public static final Supplier<Block> LIGHT_BLUE_RADON_LAMP = registerBlockAndItem("radon_lamp_light_blue", () -> new Block(RADON_LAMP_PROPERTIES));
    public static final Supplier<Block> YELLOW_RADON_LAMP = registerBlockAndItem("radon_lamp_yellow", () -> new Block(RADON_LAMP_PROPERTIES));
    public static final Supplier<Block> LIME_RADON_LAMP = registerBlockAndItem("radon_lamp_lime", () -> new Block(RADON_LAMP_PROPERTIES));
    public static final Supplier<Block> PINK_RADON_LAMP = registerBlockAndItem("radon_lamp_pink", () -> new Block(RADON_LAMP_PROPERTIES));
    public static final Supplier<Block> GRAY_RADON_LAMP = registerBlockAndItem("radon_lamp_gray", () -> new Block(RADON_LAMP_PROPERTIES));
    public static final Supplier<Block> LIGHT_GRAY_RADON_LAMP = registerBlockAndItem("radon_lamp_light_gray", () -> new Block(RADON_LAMP_PROPERTIES));
    public static final Supplier<Block> CYAN_RADON_LAMP = registerBlockAndItem("radon_lamp_cyan", () -> new Block(RADON_LAMP_PROPERTIES));
    public static final Supplier<Block> PURPLE_RADON_LAMP = registerBlockAndItem("radon_lamp_purple", () -> new Block(RADON_LAMP_PROPERTIES));
    public static final Supplier<Block> BLUE_RADON_LAMP = registerBlockAndItem("radon_lamp_blue", () -> new Block(RADON_LAMP_PROPERTIES));
    public static final Supplier<Block> BROWN_RADON_LAMP = registerBlockAndItem("radon_lamp_brown", () -> new Block(RADON_LAMP_PROPERTIES));
    public static final Supplier<Block> GREEN_RADON_LAMP = registerBlockAndItem("radon_lamp_green", () -> new Block(RADON_LAMP_PROPERTIES));
    public static final Supplier<Block> RED_RADON_LAMP = registerBlockAndItem("radon_lamp_red", () -> new Block(RADON_LAMP_PROPERTIES));
    public static final Supplier<Block> BLACK_RADON_LAMP = registerBlockAndItem("radon_lamp_black", () -> new Block(RADON_LAMP_PROPERTIES));
    public static final Supplier<Block> TREMORZILLA_EGG = registerBlockAndItem("tremorzilla_egg", () -> new TremorzillaEggBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(2.0F, 5000.0F).sound(SoundType.METAL).randomTicks()), 8);
    public static final Supplier<Block> ABYSSMARINE = registerBlockAndItem("abyssmarine", () -> new Block(ABYSSMARINE_PROPERTIES));
    public static final Supplier<Block> ABYSSMARINE_STAIRS = registerBlockAndItem("abyssmarine_stairs", () -> new StairBlock(ABYSSMARINE.get().defaultBlockState(), ABYSSMARINE_PROPERTIES));
    public static final Supplier<Block> ABYSSMARINE_SLAB = registerBlockAndItem("abyssmarine_slab", () -> new SlabBlock(ABYSSMARINE_PROPERTIES));
    public static final Supplier<Block> ABYSSMARINE_WALL = registerBlockAndItem("abyssmarine_wall", () -> new WallBlock(ABYSSMARINE_PROPERTIES));
    public static final Supplier<Block> ABYSSMARINE_BRICKS = registerBlockAndItem("abyssmarine_bricks", () -> new GlowingAbyssmarineBlock(ABYSSMARINE_PROPERTIES));
    public static final Supplier<Block> ABYSSMARINE_BRICK_STAIRS = registerBlockAndItem("abyssmarine_brick_stairs", () -> new AbyssmarineStairBlock(ABYSSMARINE_BRICKS.get().defaultBlockState(), ABYSSMARINE_PROPERTIES));
    public static final Supplier<Block> ABYSSMARINE_BRICK_SLAB = registerBlockAndItem("abyssmarine_brick_slab", () -> new AbyssmarineSlabBlock(ABYSSMARINE_PROPERTIES));
    public static final Supplier<Block> ABYSSMARINE_BRICK_WALL = registerBlockAndItem("abyssmarine_brick_wall", () -> new AbyssmarineWallBlock(ABYSSMARINE_PROPERTIES));
    public static final Supplier<Block> ABYSSMARINE_PILLAR = registerBlockAndItem("abyssmarine_pillar", () -> new AbyssmarinePillarBlock(ABYSSMARINE_PROPERTIES));
    public static final Supplier<Block> ABYSSMARINE_TILES = registerBlockAndItem("abyssmarine_tiles", () -> new GlowingAbyssmarineBlock(ABYSSMARINE_PROPERTIES));
    public static final Supplier<Block> ABYSSAL_ALTAR = registerBlockAndItem("abyssal_altar", () -> new AbyssalAltarBlock());
    public static final Supplier<Block> MUCK = registerBlockAndItem("muck", () -> new MuckBlock(BlockBehaviour.Properties.of().mapColor(DyeColor.LIGHT_GRAY).strength(0.5F).sound(SoundType.FROGSPAWN)));
    public static final Supplier<Block> TUBE_WORM = registerBlockAndItem("tube_worm", () -> new TubeWormBlock());
    public static final Supplier<Block> HOLLOW_BONE = registerBlockAndItem("hollow_bone", () -> new HollowBoneBlock());
    public static final Supplier<Block> THIN_BONE = registerBlockAndItem("thin_bone", () -> new ThinBoneBlock());
    public static final Supplier<Block> BONE_NODULE = registerBlockAndItem("bone_nodule", () -> new BoneNoduleBlock());
    public static final Supplier<Block> BONE_RIBS = registerBlockAndItem("bone_ribs", () -> new BoneRibsBlock());
    public static final Supplier<Block> BALEEN_BONE = registerBlockAndItem("baleen_bone", () -> new BaleenBoneBlock());
    public static final Supplier<Block> SMOOTH_BONE = registerBlockAndItem("smooth_bone", () -> new Block(SMOOTH_BONE_PROPERTIES));
    public static final Supplier<Block> SMOOTH_BONE_STAIRS = registerBlockAndItem("smooth_bone_stairs", () -> new StairBlock(SMOOTH_BONE.get().defaultBlockState(), SMOOTH_BONE_PROPERTIES));
    public static final Supplier<Block> SMOOTH_BONE_SLAB = registerBlockAndItem("smooth_bone_slab", () -> new SlabBlock(SMOOTH_BONE_PROPERTIES));
    public static final Supplier<Block> SMOOTH_BONE_WALL = registerBlockAndItem("smooth_bone_wall", () -> new WallBlock(SMOOTH_BONE_PROPERTIES));
    public static final Supplier<Block> BONE_WORMS = registerBlockAndItem("bone_worms", () -> new BoneWormsBlock());
    public static final Supplier<Block> PING_PONG_SPONGE = registerBlockAndItem("ping_pong_sponge", () -> new PingPongSpongeBlock());
    public static final Supplier<Block> DUSK_ANEMONE = registerBlockAndItem("dusk_anemone", () -> new OceanFloraBlock());
    public static final Supplier<Block> TWILIGHT_ANEMONE = registerBlockAndItem("twilight_anemone", () -> new OceanFloraBlock());
    public static final Supplier<Block> MIDNIGHT_ANEMONE = registerBlockAndItem("midnight_anemone", () -> new OceanFloraBlock());
    public static final Supplier<Block> MUSSEL = registerBlockAndItem("mussel", () -> new MusselBlock());
    public static final Supplier<Block> BLOCK_OF_PEARL = registerBlockAndItem("block_of_pearl", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(3.5F).sound(SoundType.AMETHYST)));
    public static final Supplier<Block> BIOLUMINESCENT_TORCH = DEF_REG.register("bioluminescent_torch", () -> new BioluminescentTorch());
    public static final Supplier<Block> BIOLUMINESCENT_WALL_TORCH = DEF_REG.register("bioluminescent_wall_torch", () -> new BioluminescentWallTorch());
    public static final Supplier<Block> DRAIN = registerBlockAndItem("drain", () -> new DrainBlock());
    public static final Supplier<Block> DEPTH_GLASS = registerBlockAndItem("depth_glass", () -> new DepthGlassBlock());
    public static final Supplier<Block> COPPER_VALVE = registerBlockAndItem("copper_valve", () -> new CopperValveBlock(), 3);
    public static final Supplier<Block> ENIGMATIC_ENGINE = registerBlockAndItem("enigmatic_engine", () -> new EnigmaticEngineBlock(), 6);
    public static final Supplier<Block> GUANO_BLOCK = registerBlockAndItem("guano_block", () -> new GuanoBlock());
    public static final Supplier<Block> GUANO_LAYER = registerBlockAndItem("guano_layer", () -> new GuanoLayerBlock());
    public static final Supplier<Block> GUANOSTONE = registerBlockAndItem("guanostone", () -> new Block(GUANOSTONE_PROPERTIES));
    public static final Supplier<Block> GUANOSTONE_STAIRS = registerBlockAndItem("guanostone_stairs", () -> new StairBlock(GUANOSTONE.get().defaultBlockState(), GUANOSTONE_PROPERTIES));
    public static final Supplier<Block> GUANOSTONE_SLAB = registerBlockAndItem("guanostone_slab", () -> new SlabBlock(GUANOSTONE_PROPERTIES));
    public static final Supplier<Block> GUANOSTONE_WALL = registerBlockAndItem("guanostone_wall", () -> new WallBlock(GUANOSTONE_PROPERTIES));
    public static final Supplier<Block> GUANOSTONE_BRICKS = registerBlockAndItem("guanostone_bricks", () -> new Block(GUANOSTONE_PROPERTIES));
    public static final Supplier<Block> GUANOSTONE_BRICK_STAIRS = registerBlockAndItem("guanostone_brick_stairs", () -> new StairBlock(GUANOSTONE_BRICKS.get().defaultBlockState(), GUANOSTONE_PROPERTIES));
    public static final Supplier<Block> GUANOSTONE_BRICK_SLAB = registerBlockAndItem("guanostone_brick_slab", () -> new SlabBlock(GUANOSTONE_PROPERTIES));
    public static final Supplier<Block> GUANOSTONE_BRICK_WALL = registerBlockAndItem("guanostone_brick_wall", () -> new WallBlock(GUANOSTONE_PROPERTIES));
    public static final Supplier<Block> GUANOSTONE_CHISELED = registerBlockAndItem("guanostone_chiseled", () -> new Block(GUANOSTONE_PROPERTIES));
    public static final Supplier<Block> GUANOSTONE_TILES = registerBlockAndItem("guanostone_tiles", () -> new Block(GUANOSTONE_PROPERTIES));
    public static final Supplier<Block> GUANOSTONE_REDSTONE_ORE = registerBlockAndItem("guanostone_redstone_ore", () -> new RedStoneOreBlock(ACPlatform.copyProperties(Blocks.REDSTONE_ORE).sound(SoundType.BASALT)));
    public static final Supplier<Block> COPROLITH = registerBlockAndItem("coprolith", () -> new Block(COPROLITH_PROPERTIES));
    public static final Supplier<Block> COPROLITH_STAIRS = registerBlockAndItem("coprolith_stairs", () -> new StairBlock(COPROLITH.get().defaultBlockState(), COPROLITH_PROPERTIES));
    public static final Supplier<Block> COPROLITH_SLAB = registerBlockAndItem("coprolith_slab", () -> new SlabBlock(COPROLITH_PROPERTIES));
    public static final Supplier<Block> COPROLITH_WALL = registerBlockAndItem("coprolith_wall", () -> new WallBlock(COPROLITH_PROPERTIES));
    public static final Supplier<Block> SMOOTH_COPROLITH = registerBlockAndItem("smooth_coprolith", () -> new Block(COPROLITH_PROPERTIES));
    public static final Supplier<Block> SMOOTH_COPROLITH_STAIRS = registerBlockAndItem("smooth_coprolith_stairs", () -> new StairBlock(SMOOTH_COPROLITH.get().defaultBlockState(), COPROLITH_PROPERTIES));
    public static final Supplier<Block> SMOOTH_COPROLITH_SLAB = registerBlockAndItem("smooth_coprolith_slab", () -> new SlabBlock(COPROLITH_PROPERTIES));
    public static final Supplier<Block> SMOOTH_COPROLITH_WALL = registerBlockAndItem("smooth_coprolith_wall", () -> new WallBlock(COPROLITH_PROPERTIES));
    // 1.20.3 moved DropExperienceBlock's xp range in front of its Properties, and deleted the
    // Properties-only overload that stood beside it. javap on the vanilla 1.20.1 and 1.20.2 jars
    // shows both overloads in the old order on each, so the boundary is 1.20.3 — neither loader
    // publishes a 1.20.2 or a 1.20.3 build, so no earlier node in this tree could have said so.
    //? if >=1.20.3
    /*public static final Supplier<Block> COPROLITH_COAL_ORE = registerBlockAndItem("coprolith_coal_ore", () -> new DropExperienceBlock(UniformInt.of(0, 2), ACPlatform.copyProperties(Blocks.COAL_ORE).sound(SoundType.CALCITE)));*/
    //? if <1.20.3
    public static final Supplier<Block> COPROLITH_COAL_ORE = registerBlockAndItem("coprolith_coal_ore", () -> new DropExperienceBlock(ACPlatform.copyProperties(Blocks.COAL_ORE).sound(SoundType.CALCITE), UniformInt.of(0, 2)));
    public static final Supplier<Block> POROUS_COPROLITH = registerBlockAndItem("porous_coprolith", () -> new Block(POROUS_COPROLITH_PROPERTIES));
    public static final Supplier<Block> PEERING_COPROLITH = registerBlockAndItem("peering_coprolith", () -> new Block(PEERING_COPROLITH_PROPERTIES));
    public static final Supplier<Block> FORSAKEN_IDOL = registerBlockAndItem("forsaken_idol", () -> new ForsakenIdolBlock());
    public static final Supplier<Block> THORNWOOD_LOG = registerBlockAndItem("thornwood_log", () -> new StrippableLogBlock(THORNWOOD_LOG_PROPERTIES));
    public static final Supplier<Block> THORNWOOD_BRANCH = registerBlockAndItem("thornwood_branch", () -> new ThornwoodBranchBlock());
    public static final Supplier<Block> POTTED_THORNWOOD_BRANCH = DEF_REG.register("potted_thornwood_branch", () -> ACBlockFactory.flowerPot(THORNWOOD_BRANCH, BlockBehaviour.Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final Supplier<Block> THORNWOOD_WOOD = registerBlockAndItem("thornwood_wood", () -> new StrippableLogBlock(THORNWOOD_LOG_PROPERTIES));
    public static final Supplier<Block> STRIPPED_THORNWOOD_LOG = registerBlockAndItem("stripped_thornwood_log", () -> new RotatedPillarBlock(THORNWOOD_LOG_PROPERTIES));
    public static final Supplier<Block> STRIPPED_THORNWOOD_WOOD = registerBlockAndItem("stripped_thornwood_wood", () -> new RotatedPillarBlock(THORNWOOD_LOG_PROPERTIES));
    public static final Supplier<Block> THORNWOOD_PLANKS = registerBlockAndItem("thornwood_planks", () -> new Block(THORNWOOD_PLANKS_PROPERTIES));
    public static final Supplier<Block> THORNWOOD_PLANKS_STAIRS = registerBlockAndItem("thornwood_stairs", () -> new StairBlock(THORNWOOD_PLANKS.get().defaultBlockState(), THORNWOOD_PLANKS_PROPERTIES));
    public static final Supplier<Block> THORNWOOD_PLANKS_SLAB = registerBlockAndItem("thornwood_slab", () -> new SlabBlock(THORNWOOD_PLANKS_PROPERTIES));
    public static final Supplier<Block> THORNWOOD_PLANKS_FENCE = registerBlockAndItem("thornwood_fence", () -> new FenceBlock(THORNWOOD_PLANKS_PROPERTIES));
    public static final Supplier<Block> THORNWOOD_SIGN = DEF_REG.register("thornwood_sign", () -> ACBlockFactory.standingSign(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).noCollission().strength(1.0F).sound(SoundType.WOOD), THORNWOOD_WOOD_TYPE));
    public static final Supplier<Block> THORNWOOD_WALL_SIGN = DEF_REG.register("thornwood_wall_sign", () -> ACBlockFactory.wallSign(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).noCollission().strength(1.0F).sound(SoundType.WOOD), THORNWOOD_WOOD_TYPE));
    public static final Supplier<Block> THORNWOOD_HANGING_SIGN = DEF_REG.register("thornwood_hanging_sign", () -> ACBlockFactory.hangingSign(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollission().strength(1.0F), THORNWOOD_WOOD_TYPE));
    public static final Supplier<Block> THORNWOOD_WALL_HANGING_SIGN = DEF_REG.register("thornwood_wall_hanging_sign", () -> ACBlockFactory.wallHangingSign(ACCompat.dropsLike(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollission().strength(1.0F), THORNWOOD_HANGING_SIGN.get()), THORNWOOD_WOOD_TYPE));
    public static final Supplier<Block> THORNWOOD_PRESSURE_PLATE = registerBlockAndItem("thornwood_pressure_plate", () -> ACBlockFactory.pressurePlate(ACPlatform.copyProperties(THORNWOOD_PLANKS.get()).noCollission().strength(0.5F).sound(SoundType.WOOD), BlockSetType.OAK));
    public static final Supplier<Block> THORNWOOD_TRAPDOOR = registerBlockAndItem("thornwood_trapdoor", () -> ACBlockFactory.trapDoor(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.0F).sound(SoundType.WOOD).noOcclusion(), BlockSetType.OAK));
    public static final Supplier<Block> THORNWOOD_BUTTON = registerBlockAndItem("thornwood_button", () -> ACBlockFactory.button(ACPlatform.copyProperties(THORNWOOD_PLANKS.get()).noCollission().strength(0.5F).sound(SoundType.WOOD), BlockSetType.OAK, 30, true));
    public static final Supplier<Block> THORNWOOD_FENCE_GATE = registerBlockAndItem("thornwood_fence_gate", () -> ACBlockFactory.fenceGate(ACPlatform.copyProperties(THORNWOOD_PLANKS.get()).strength(2.0F, 3.0F).sound(SoundType.WOOD).forceSolidOn(), THORNWOOD_WOOD_TYPE, SoundEvents.FENCE_GATE_CLOSE, SoundEvents.FENCE_GATE_OPEN));
    public static final Supplier<Block> THORNWOOD_DOOR = DEF_REG.register("thornwood_door", () -> ACBlockFactory.door(ACPlatform.copyProperties(THORNWOOD_PLANKS.get()).strength(3.0F).sound(SoundType.WOOD).noOcclusion(), BlockSetType.OAK));
    public static final Supplier<Block> THORNWOOD_SAPLING = registerBlockAndItem("thornwood_sapling", () -> new CaveSaplingBlock(new ThornwoodGrower(), BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).noCollission().randomTicks().instabreak().sound(SoundType.GRASS), true));
    public static final Supplier<Block> POTTED_THORNWOOD_SAPLING = DEF_REG.register("potted_thornwood_sapling", () -> ACBlockFactory.flowerPot(THORNWOOD_SAPLING, BlockBehaviour.Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final Supplier<Block> MOTH_BALL = registerBlockAndItem("moth_ball", () -> new MothBallBlock());
    public static final Supplier<Block> BEHOLDER = registerBlockAndItem("beholder", () -> new BeholderBlock(), 3);
    public static final Supplier<Block> BLOCK_OF_CHOCOLATE = registerBlockAndItemEdible("block_of_chocolate", () -> new ChocolateBlock(CHOCOLATE_PROPERTIES), () -> ACFoods.BLOCK_OF_CHOCOLATE);
    public static final Supplier<Block> BLOCK_OF_POLISHED_CHOCOLATE = registerBlockAndItemEdible("block_of_polished_chocolate", () -> new Block(CHOCOLATE_PROPERTIES), () -> ACFoods.BLOCK_OF_CHOCOLATE);
    public static final Supplier<Block> BLOCK_OF_CHISELED_CHOCOLATE = registerBlockAndItemEdible("block_of_chiseled_chocolate", () -> new Block(CHOCOLATE_PROPERTIES), () -> ACFoods.BLOCK_OF_CHOCOLATE);
    public static final Supplier<Block> BLOCK_OF_FROSTED_CHOCOLATE = registerBlockAndItemEdible("block_of_frosted_chocolate", () -> new FrostedChocolateBlock(), () -> ACFoods.BLOCK_OF_CHOCOLATE);
    public static final Supplier<Block> BLOCK_OF_FROSTING = registerBlockAndItemEdible("block_of_frosting", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.0F, 1.0F).sound(ACSoundTypes.SQUISHY_CANDY).instrument(NoteBlockInstrument.BASEDRUM)), () -> ACFoods.BLOCK_OF_FROSTING);
    public static final Supplier<Block> BLOCK_OF_VANILLA_FROSTING = registerBlockAndItemEdible("block_of_vanilla_frosting", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(1.0F, 1.0F).sound(ACSoundTypes.SQUISHY_CANDY).instrument(NoteBlockInstrument.BASEDRUM)), () -> ACFoods.BLOCK_OF_FROSTING);
    public static final Supplier<Block> BLOCK_OF_CHOCOLATE_FROSTING = registerBlockAndItemEdible("block_of_chocolate_frosting", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(1.0F, 1.0F).sound(ACSoundTypes.SQUISHY_CANDY).instrument(NoteBlockInstrument.BASEDRUM)), () -> ACFoods.BLOCK_OF_FROSTING);
    public static final Supplier<Block> SWEET_PUFF = registerBlockAndItemEdible("sweet_puff", () -> new CavePlantBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).instabreak().offsetType(BlockBehaviour.OffsetType.XYZ).sound(SoundType.GRASS).noOcclusion().noCollission().replaceable(), true), () -> ACFoods.SWEET_PUFF);
    public static final Supplier<Block> CAKE_LAYER = registerBlockAndItemEdible("cake_layer", () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(1.5F, 1.0F).sound(ACSoundTypes.SOFT_CANDY).instrument(NoteBlockInstrument.BASEDRUM)), () -> ACFoods.CAKE_LAYER);
    public static final Supplier<Block> DOUGH_BLOCK = registerBlockAndItemEdible("dough_block", () -> new Block(DOUGH_PROPERTIES), () -> ACFoods.DOUGH);
    public static final Supplier<Block> COOKIE_BLOCK = registerBlockAndItemEdible("cookie_block", () -> new Block(COOKIE_PROPERTIES), () -> ACFoods.COOKIE);
    public static final Supplier<Block> WAFER_COOKIE_BLOCK = registerBlockAndItemEdible("wafer_cookie_block", () -> new Block(COOKIE_PROPERTIES), () -> ACFoods.COOKIE);
    public static final Supplier<Block> WAFER_COOKIE_STAIRS = registerBlockAndItemEdible("wafer_cookie_stairs", () -> new StairBlock(WAFER_COOKIE_BLOCK.get().defaultBlockState(), COOKIE_PROPERTIES), () -> ACFoods.COOKIE_HALF);
    public static final Supplier<Block> WAFER_COOKIE_SLAB = registerBlockAndItemEdible("wafer_cookie_slab", () -> new SlabBlock(COOKIE_PROPERTIES), () -> ACFoods.COOKIE_HALF);
    public static final Supplier<Block> WAFER_COOKIE_WALL = registerBlockAndItemEdible("wafer_cookie_wall", () -> new WallBlock(COOKIE_PROPERTIES), () -> ACFoods.COOKIE_HALF);
    public static final Supplier<Block> LICOROOT = registerBlockAndItemEdible("licoroot", () -> new RotatedPillarBlock(LICOROOT_PROPERTIES), () -> ACFoods.LICOROOT);
    public static final Supplier<Block> LICOROOT_VINE = registerBlockAndItemEdible("licoroot_vine", () -> new LicorootVineBlock(), () -> ACFoods.LICOROOT_VINE);
    public static final Supplier<Block> LICOROOT_SPROUT = registerBlockAndItemEdible("licoroot_sprout", () -> new CaveSaplingBlock(new LicorootGrower(), BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instabreak().offsetType(BlockBehaviour.OffsetType.XZ).sound(SoundType.GRASS).noOcclusion().noCollission().replaceable(), false), () -> ACFoods.LICOROOT_VINE);
    public static final Supplier<Block> SMALL_PEPPERMINT = registerBlockAndItemEdible("small_peppermint", () -> new PeppermintBlock(2.0D, 6.0D), () -> ACFoods.SMALL_PEPPERMINT);
    public static final Supplier<Block> LARGE_PEPPERMINT = registerBlockAndItemEdible("large_peppermint", () -> new PeppermintBlock(0.0D, 8.0D), () -> ACFoods.LARGE_PEPPERMINT);
    public static final Supplier<Block> VANILLA_ICE_CREAM = registerBlockAndItemEdible("vanilla_ice_cream", () -> new IceCreamBlock(0), () -> ACFoods.VANILLA_ICE_CREAM);
    public static final Supplier<Block> CHOCOLATE_ICE_CREAM = registerBlockAndItemEdible("chocolate_ice_cream", () -> new IceCreamBlock(1), () -> ACFoods.CHOCOLATE_ICE_CREAM);
    public static final Supplier<Block> SWEETBERRY_ICE_CREAM = registerBlockAndItemEdible("sweetberry_ice_cream", () -> new IceCreamBlock(2), () -> ACFoods.SWEETBERRY_ICE_CREAM);
    public static final Supplier<Block> SPRINKLES = registerBlockAndItemEdible("sprinkles", () -> new SprinklesBlock(), () -> ACFoods.SPRINKLES);
    public static final Supplier<Block> GIANT_SWEETBERRY = registerBlockAndItemEdible("giant_sweetberry", () -> new GiantSweetberryBlock(), () -> ACFoods.GIANT_SWEETBERRY);
    public static final Supplier<Block> CANDY_CANE = registerBlockAndItemEdible("candy_cane", () -> new SmallCandyCaneBlock(), () -> ACFoods.CANDY_CANE);
    public static final Supplier<Block> CANDY_CANE_BLOCK = registerBlockAndItemEdible("candy_cane_block", () -> new CandyCaneBlock(), () -> ACFoods.CANDY_CANE);
    public static final Supplier<Block> CHISELED_CANDY_CANE_BLOCK = registerBlockAndItemEdible("chiseled_candy_cane_block", () -> new CandyCaneBlock(), () -> ACFoods.CANDY_CANE);
    public static final Supplier<Block> STRIPPED_CANDY_CANE_BLOCK = registerBlockAndItemEdible("stripped_candy_cane_block", () -> new CandyCaneBlock(), () -> ACFoods.CANDY_CANE);
    public static final Supplier<Block> CANDY_CANE_POLE = registerBlockAndItemEdible("candy_cane_pole", () -> new CandyCanePoleBlock(), () -> ACFoods.CANDY_CANE_POLE);
    public static final Supplier<Block> STRIPPED_CANDY_CANE_POLE = registerBlockAndItemEdible("stripped_candy_cane_pole", () -> new CandyCanePoleBlock(), () -> ACFoods.CANDY_CANE_POLE);
    public static final Supplier<Block> LOLLIPOP_BUNCH = registerBlockAndItemEdible("lollipop_bunch", () -> new CavePlantBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).instabreak().offsetType(BlockBehaviour.OffsetType.XZ).sound(SoundType.STONE).noOcclusion().noCollission(), true), () -> ACFoods.LOLLIPOP_BUNCH);
    public static final Supplier<Block> FROSTMINT = registerBlockAndItemEdible("frostmint", () -> new FrostmintBlock(), () -> ACFoods.FROSTMINT);
    public static final Supplier<Block> SUGAR_GLASS = registerBlockAndItemEdible("sugar_glass", () -> new SugarGlassBlock(), () -> ACFoods.SUGAR_GLASS);
    public static final Supplier<LiquidBlock> PURPLE_SODA = DEF_REG.register("purple_soda", () -> new PurpleSodaBlock(ACFluidRegistry.PURPLE_SODA_FLUID_SOURCE, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollission().strength(100.0F).emissiveRendering((state, world, pos) -> false).noLootTable().replaceable().liquid().pushReaction(PushReaction.DESTROY)));
    public static final Supplier<Block> SUNDROP = registerBlockAndItemEdible("sundrop", () -> new SundropBlock(), () -> ACFoods.SUNDROP);
    public static final Supplier<Block> GUMMY_RING_RED = registerBlockAndItemEdible("gummy_ring_red", () -> new GummyRingBlock(), () -> ACFoods.GUMMY_RING);
    public static final Supplier<Block> GUMMY_RING_GREEN = registerBlockAndItemEdible("gummy_ring_green", () -> new GummyRingBlock(), () -> ACFoods.GUMMY_RING);
    public static final Supplier<Block> GUMMY_RING_YELLOW = registerBlockAndItemEdible("gummy_ring_yellow", () -> new GummyRingBlock(), () -> ACFoods.GUMMY_RING);
    public static final Supplier<Block> GUMMY_RING_BLUE = registerBlockAndItemEdible("gummy_ring_blue", () -> new GummyRingBlock(), () -> ACFoods.GUMMY_RING);
    public static final Supplier<Block> GUMMY_RING_PINK = registerBlockAndItemEdible("gummy_ring_pink", () -> new GummyRingBlock(), () -> ACFoods.GUMMY_RING);
    public static final Supplier<Block> GOBTHUMPER = registerBlockAndItem("gobthumper", () -> new GobthumperBlock(), 3);
    public static final Supplier<Block> CONVERSION_CRUCIBLE = registerBlockAndItem("conversion_crucible", () -> new ConversionCrucibleBlock(), 6);
    public static final Supplier<Block> WHITE_ROCK_CANDY = registerBlockAndItemEdible("rock_candy_white", () -> new Block(ROCK_CANDY_PROPERTIES), () -> ACFoods.ROCK_CANDY);
    public static final Supplier<Block> ORANGE_ROCK_CANDY = registerBlockAndItemEdible("rock_candy_orange", () -> new Block(ROCK_CANDY_PROPERTIES), () -> ACFoods.ROCK_CANDY);
    public static final Supplier<Block> MAGENTA_ROCK_CANDY = registerBlockAndItemEdible("rock_candy_magenta", () -> new Block(ROCK_CANDY_PROPERTIES), () -> ACFoods.ROCK_CANDY);
    public static final Supplier<Block> LIGHT_BLUE_ROCK_CANDY = registerBlockAndItemEdible("rock_candy_light_blue", () -> new Block(ROCK_CANDY_PROPERTIES), () -> ACFoods.ROCK_CANDY);
    public static final Supplier<Block> YELLOW_ROCK_CANDY = registerBlockAndItemEdible("rock_candy_yellow", () -> new Block(ROCK_CANDY_PROPERTIES), () -> ACFoods.ROCK_CANDY);
    public static final Supplier<Block> LIME_ROCK_CANDY = registerBlockAndItemEdible("rock_candy_lime", () -> new Block(ROCK_CANDY_PROPERTIES), () -> ACFoods.ROCK_CANDY);
    public static final Supplier<Block> PINK_ROCK_CANDY = registerBlockAndItemEdible("rock_candy_pink", () -> new Block(ROCK_CANDY_PROPERTIES), () -> ACFoods.ROCK_CANDY);
    public static final Supplier<Block> GRAY_ROCK_CANDY = registerBlockAndItemEdible("rock_candy_gray", () -> new Block(ROCK_CANDY_PROPERTIES), () -> ACFoods.ROCK_CANDY);
    public static final Supplier<Block> LIGHT_GRAY_ROCK_CANDY = registerBlockAndItemEdible("rock_candy_light_gray", () -> new Block(ROCK_CANDY_PROPERTIES), () -> ACFoods.ROCK_CANDY);
    public static final Supplier<Block> CYAN_ROCK_CANDY = registerBlockAndItemEdible("rock_candy_cyan", () -> new Block(ROCK_CANDY_PROPERTIES), () -> ACFoods.ROCK_CANDY);
    public static final Supplier<Block> PURPLE_ROCK_CANDY = registerBlockAndItemEdible("rock_candy_purple", () -> new Block(ROCK_CANDY_PROPERTIES), () -> ACFoods.ROCK_CANDY);
    public static final Supplier<Block> BLUE_ROCK_CANDY = registerBlockAndItemEdible("rock_candy_blue", () -> new Block(ROCK_CANDY_PROPERTIES), () -> ACFoods.ROCK_CANDY);
    public static final Supplier<Block> BROWN_ROCK_CANDY = registerBlockAndItemEdible("rock_candy_brown", () -> new Block(ROCK_CANDY_PROPERTIES), () -> ACFoods.ROCK_CANDY);
    public static final Supplier<Block> GREEN_ROCK_CANDY = registerBlockAndItemEdible("rock_candy_green", () -> new Block(ROCK_CANDY_PROPERTIES), () -> ACFoods.ROCK_CANDY);
    public static final Supplier<Block> RED_ROCK_CANDY = registerBlockAndItemEdible("rock_candy_red", () -> new Block(ROCK_CANDY_PROPERTIES), () -> ACFoods.ROCK_CANDY);
    public static final Supplier<Block> BLACK_ROCK_CANDY = registerBlockAndItemEdible("rock_candy_black", () -> new Block(ROCK_CANDY_PROPERTIES), () -> ACFoods.ROCK_CANDY);
    public static final Supplier<Block> GINGERBREAD_BLOCK = registerBlockAndItemEdible("gingerbread_block", () -> new Block(GINGERBREAD_PROPERTIES), () -> ACFoods.GINGERBREAD);
    public static final Supplier<Block> GINGERBREAD_STAIRS = registerBlockAndItemEdible("gingerbread_stairs", () -> new StairBlock(GINGERBREAD_BLOCK.get().defaultBlockState(), GINGERBREAD_PROPERTIES), () -> ACFoods.GINGERBREAD_HALF);
    public static final Supplier<Block> GINGERBREAD_SLAB = registerBlockAndItemEdible("gingerbread_slab", () -> new SlabBlock(GINGERBREAD_PROPERTIES), () -> ACFoods.GINGERBREAD_HALF);
    public static final Supplier<Block> GINGERBREAD_WALL = registerBlockAndItemEdible("gingerbread_wall", () -> new WallBlock(GINGERBREAD_PROPERTIES), () -> ACFoods.GINGERBREAD_HALF);
    public static final Supplier<Block> GINGERBREAD_DOOR = registerBlockAndItemEdible("gingerbread_door", () -> new GingerbreadDoorBlock(GINGERBREAD_PROPERTIES), () -> ACFoods.GINGERBREAD_HALF);
    public static final Supplier<Block> GINGERBARREL = registerBlockAndItemEdible("gingerbarrel", () -> new GingerbarrelBlock(), () -> ACFoods.GINGERBREAD_HALF);
    public static final Supplier<Block> FROSTED_GINGERBREAD_BLOCK = registerBlockAndItemEdible("frosted_gingerbread_block", () -> new Block(GINGERBREAD_PROPERTIES), () -> ACFoods.GINGERBREAD);
    public static final Supplier<Block> FROSTED_GINGERBREAD_STAIRS = registerBlockAndItemEdible("frosted_gingerbread_stairs", () -> new StairBlock(GINGERBREAD_BLOCK.get().defaultBlockState(), GINGERBREAD_PROPERTIES), () -> ACFoods.GINGERBREAD_HALF);
    public static final Supplier<Block> FROSTED_GINGERBREAD_SLAB = registerBlockAndItemEdible("frosted_gingerbread_slab", () -> new SlabBlock(GINGERBREAD_PROPERTIES), () -> ACFoods.GINGERBREAD_HALF);
    public static final Supplier<Block> FROSTED_GINGERBREAD_WALL = registerBlockAndItemEdible("frosted_gingerbread_wall", () -> new WallBlock(GINGERBREAD_PROPERTIES), () -> ACFoods.GINGERBREAD_HALF);
    public static final Supplier<Block> FROSTED_GINGERBREAD_DOOR = registerBlockAndItemEdible("frosted_gingerbread_door", () -> new GingerbreadDoorBlock(GINGERBREAD_PROPERTIES), () -> ACFoods.GINGERBREAD_HALF);
    public static final Supplier<Block> GINGERBREAD_BRICKS = registerBlockAndItemEdible("gingerbread_bricks", () -> new Block(GINGERBREAD_PROPERTIES), () -> ACFoods.GINGERBREAD);
    public static final Supplier<Block> GINGERBREAD_BRICK_STAIRS = registerBlockAndItemEdible("gingerbread_brick_stairs", () -> new StairBlock(GINGERBREAD_BRICKS.get().defaultBlockState(), GINGERBREAD_PROPERTIES), () -> ACFoods.GINGERBREAD_HALF);
    public static final Supplier<Block> GINGERBREAD_BRICK_SLAB = registerBlockAndItemEdible("gingerbread_brick_slab", () -> new SlabBlock(GINGERBREAD_PROPERTIES), () -> ACFoods.GINGERBREAD_HALF);
    public static final Supplier<Block> GINGERBREAD_BRICK_WALL = registerBlockAndItemEdible("gingerbread_brick_wall", () -> new WallBlock(GINGERBREAD_PROPERTIES), () -> ACFoods.GINGERBREAD_HALF);
    public static final Supplier<Block> FROSTED_GINGERBREAD_BRICKS = registerBlockAndItemEdible("frosted_gingerbread_bricks", () -> new Block(GINGERBREAD_PROPERTIES), () -> ACFoods.GINGERBREAD);
    public static final Supplier<Block> FROSTED_GINGERBREAD_BRICK_STAIRS = registerBlockAndItemEdible("frosted_gingerbread_brick_stairs", () -> new StairBlock(GINGERBREAD_BRICKS.get().defaultBlockState(), GINGERBREAD_PROPERTIES), () -> ACFoods.GINGERBREAD_HALF);
    public static final Supplier<Block> FROSTED_GINGERBREAD_BRICK_SLAB = registerBlockAndItemEdible("frosted_gingerbread_brick_slab", () -> new SlabBlock(GINGERBREAD_PROPERTIES), () -> ACFoods.GINGERBREAD_HALF);
    public static final Supplier<Block> FROSTED_GINGERBREAD_BRICK_WALL = registerBlockAndItemEdible("frosted_gingerbread_brick_wall", () -> new WallBlock(GINGERBREAD_PROPERTIES), () -> ACFoods.GINGERBREAD_HALF);
    public static final Supplier<Block> CONFECTION_OVEN = registerBlockAndItem("confection_oven", () -> new ConfectionOvenBlock());

    private static Supplier<Block> registerBlockAndItem(String name, Supplier<Block> block) {
        return registerBlockAndItem(name, block, 0);
    }

    private static Supplier<Block> registerBlockAndItem(String name, Supplier<Block> block, int itemType) {
        Supplier<Block> blockObj = DEF_REG.register(name, block);
        ACItemRegistry.DEF_REG.register(name, getBlockSupplier(itemType, blockObj));
        return blockObj;
    }

    // The FoodProperties arrive as a supplier, not as a value, so that touching this class does not
    // drag ACFoods' <clinit> along with it. From 1.20.5 FoodProperties.Builder#effect takes the
    // MobEffectInstance eagerly (see the !mc205-food-effect replacement), so building ACFoods calls
    // ACEffectRegistry.RAGE.get() — and every field here is initialised while the mod object is
    // being constructed, long before the effect registry is populated: "Trying to access unbound
    // value: alexscaves:rage". Deferring into the item supplier moves it to item-registration time,
    // which BuiltInRegistries runs after MOB_EFFECT.
    private static Supplier<Block> registerBlockAndItemEdible(String name, Supplier<Block> block, Supplier<FoodProperties> foodProperties) {
        Supplier<Block> blockObj = DEF_REG.register(name, block);
        ACItemRegistry.DEF_REG.register(name, () -> new BlockItemWithSupplier(blockObj, ACFoodBuilder.food(blockItemProperties(), foodProperties.get())));
        return blockObj;
    }

    /**
     * The {@code Item.Properties} every one of this mod's BlockItems is built from.
     *
     * <p>1.21.2 stopped deriving a BlockItem's name from its block. {@code BlockItem} used to
     * override {@code getDescriptionId()} to return {@code getBlock().getDescriptionId()}; now the
     * name comes off {@code Properties} like any other item's, and an item that does not ask for
     * the block prefix is called {@code item.<ns>.<path>}. Every lang key this mod ships for a
     * block is {@code block.alexscaves.*}, so all ~360 of them displayed as raw keys on 1.21.2 and
     * up — icons and models were unaffected, which is why it survived a client boot test.
     *
     * <p>Below 1.21.2 the method does not exist and the old override is still doing the work.
     */
    private static Item.Properties blockItemProperties() {
        //? if >=1.21.2 {
        /*return new Item.Properties().useBlockDescriptionPrefix();
        *///?} else {
        return new Item.Properties();
        //?}
    }

    private static Supplier<? extends BlockItemWithSupplier> getBlockSupplier(int itemType, Supplier<Block> blockObj) {
        switch (itemType) {
            default:
                return () -> new BlockItemWithSupplier(blockObj, blockItemProperties());
            case 1:
                return () -> new BlockItemWithSupplierLore(blockObj, blockItemProperties());
            case 2:
                return () -> new BlockItemWithScaffolding(blockObj, blockItemProperties());
            case 3:
                return () -> new BlockItemWithISTER(blockObj, blockItemProperties());
            case 4:
                return () -> new RadioactiveBlockItem(blockObj, blockItemProperties(), 0.001F);
            case 5:
                return () -> new RadioactiveOnDestroyedBlockItem(blockObj, blockItemProperties(), 0.01F);
            case 6:
                return () -> new BlockItemWithSupplier(blockObj, blockItemProperties().rarity(Rarity.UNCOMMON));
            case 7:
                return () -> new BlockItemWithSupplier(blockObj, blockItemProperties().rarity(Rarity.UNCOMMON).fireResistant());
            case 8:
                return () -> new BlockItemWithSupplier(blockObj, blockItemProperties().rarity(Rarity.UNCOMMON).fireResistant().rarity(ACItemRegistry.RARITY_NUCLEAR));
            case 9:
                return () -> new BlockItemWithISTER(blockObj, blockItemProperties().rarity(Rarity.UNCOMMON));
        }
    }


    /**
     * Files each of the mod's pots under the plant it holds, so that right-clicking that plant onto
     * an empty pot produces it.
     *
     * <p>This exists only because both Forge and NeoForge replace vanilla's eager
     * {@code FlowerPotBlock(Block, Properties)} constructor — which does this filing itself — with a
     * deferred pair that cannot, since a mod's pot is built while the block registry is still
     * filling. Fabric keeps the vanilla constructor, so every one of these calls has already
     * happened by the time this method would run and the whole body is gated out there. See
     * {@code ACBlockFactory#flowerPot}.
     */
    public static void setup() {
        //? if !fabric {
        FlowerPotBlock flowerPotBlock = (FlowerPotBlock) Blocks.FLOWER_POT;
        flowerPotBlock.addPlant(BuiltInRegistries.BLOCK.getKey(FLYTRAP.get()), POTTED_FLYTRAP);
        flowerPotBlock.addPlant(BuiltInRegistries.BLOCK.getKey(CURLY_FERN.get()), POTTED_CURLY_FERN);
        flowerPotBlock.addPlant(BuiltInRegistries.BLOCK.getKey(CYCAD.get()), POTTED_CYCAD);
        flowerPotBlock.addPlant(BuiltInRegistries.BLOCK.getKey(PEWEN_SAPLING.get()), POTTED_PEWEN_SAPLING);
        flowerPotBlock.addPlant(BuiltInRegistries.BLOCK.getKey(PEWEN_PINES.get()), POTTED_PEWEN_PINES);
        flowerPotBlock.addPlant(BuiltInRegistries.BLOCK.getKey(FIDDLEHEAD.get()), POTTED_FIDDLEHEAD);
        flowerPotBlock.addPlant(BuiltInRegistries.BLOCK.getKey(ANCIENT_SAPLING.get()), POTTED_ANCIENT_SAPLING);
        flowerPotBlock.addPlant(BuiltInRegistries.BLOCK.getKey(UNDERWEED.get()), POTTED_UNDERWEED);
        flowerPotBlock.addPlant(BuiltInRegistries.BLOCK.getKey(THORNWOOD_BRANCH.get()), POTTED_THORNWOOD_BRANCH);
        flowerPotBlock.addPlant(BuiltInRegistries.BLOCK.getKey(THORNWOOD_SAPLING.get()), POTTED_THORNWOOD_SAPLING);
        //?}
    }
}
