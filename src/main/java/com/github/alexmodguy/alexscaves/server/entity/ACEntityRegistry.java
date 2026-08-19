package com.github.alexmodguy.alexscaves.server.entity;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.entity.item.*;
import com.github.alexmodguy.alexscaves.server.entity.living.*;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;

// NeoForge lifted EventBusSubscriber out of @Mod into net.neoforged.fml.common in 1.20.5 and
// deleted the nested copy. Fully qualified so the import above stays as it is on every node.
// Then 21.6 (loader 9) dropped the mod-bus/game-bus distinction from the annotation altogether —
// there is no bus() attribute and no nested Bus enum any more, a subscriber is simply registered
// for whichever bus each of its events belongs to. NeoForge's own NetworkInitialization subscribes
// to the mod-bus RegisterPayloadHandlersEvent that way.
//? if neoforge && >=1.21.6
/*@net.neoforged.fml.common.EventBusSubscriber(modid = AlexsCaves.MODID)*/
//? if neoforge && >=1.20.5 && <1.21.6
/*@net.neoforged.fml.common.EventBusSubscriber(modid = AlexsCaves.MODID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)*/
// And Forge 59 (1.21.9) moved both of this class's events the other way, onto the GAME bus:
// EntityAttributeCreationEvent and SpawnPlacementRegisterEvent lost IModBusEvent and gained the
// static `BUS` field that marks an eventbus-7 game-bus event. Registering them on the mod bus is
// not a silent no-op there — FML rejects the whole subscriber with "BusGroup modBusFor<modid>
// requires all events on it to inherit from IModBusEvent", so the mod fails to load. javap is the
// only way to tell the two apart: a mod-bus event has getBus(BusGroup) and no static BUS.
//? if forge && >=1.21.9
/*@Mod.EventBusSubscriber(modid = AlexsCaves.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)*/
//? if (!neoforge || <1.20.5) && (!forge || <1.21.9)
@Mod.EventBusSubscriber(modid = AlexsCaves.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ACEntityRegistry {


    public static final DeferredRegister<EntityType<?>> DEF_REG = DeferredRegister.create(Registries.ENTITY_TYPE, AlexsCaves.MODID);

    /**
     * The argument of {@code EntityType.Builder#build}, which changed type in 1.21.2.
     *
     * <p>It used to be a bare {@code String} naming a DataFixerUpper choice type, and the result of
     * that lookup was discarded — see {@code Util#fetchChoiceType}, which returns null outright
     * unless {@code CHECK_DATA_FIXER_SCHEMA} is on. So on every version through 1.21.1 the string
     * was inert. 1.21.2 replaced it with the entity's real {@code ResourceKey}, and now uses it for
     * the description id and the loot-table id as well, so it must match the id this registry
     * registers the type under.
     *
     * <p>Making the helper's <em>return type</em> the version-dependent thing keeps all 83 call
     * sites identical on every node, which is why this is a gate rather than a replacement rule.
     *
     * <p>Four of those call sites used to pass something other than their registry name
     * ({@code ac_boat}, {@code ac_chest_boat}, and copy-paste leftovers on {@code floater} and
     * {@code gum_worm_segment}); all four now pass their own name. That is behaviourally inert
     * below 1.21.2 for the reason above, and required at or after it.
     */
    //? if >=1.21.2 {
    /*private static net.minecraft.resources.ResourceKey<EntityType<?>> id(String name) {
        return net.minecraft.resources.ResourceKey.create(Registries.ENTITY_TYPE,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, name));
    }
    *///?} else {
    private static String id(String name) {
        return name;
    }
    //?}
    // Declared in ACMobCategories rather than inline — see that class for why the two entries and
    // this registry cannot share an initialiser from 1.21 on.
    public static final MobCategory CAVE_CREATURE = ACMobCategories.caveCreature();
    public static final MobCategory DEEP_SEA_CREATURE = ACMobCategories.deepSeaCreature();
    public static final Supplier<EntityType<AlexsCavesBoatEntity>> BOAT = DEF_REG.register("boat", () -> (EntityType) EntityType.Builder.of(AlexsCavesBoatEntity::new, MobCategory.MISC).sized(1.375F, 0.5625F).clientTrackingRange(10).build(id("boat")));
    public static final Supplier<EntityType<AlexsCavesChestBoatEntity>> CHEST_BOAT = DEF_REG.register("chest_boat", () -> (EntityType) EntityType.Builder.of(AlexsCavesChestBoatEntity::new, MobCategory.MISC).sized(1.375F, 0.5625F).clientTrackingRange(10).build(id("chest_boat")));
    public static final Supplier<EntityType<MovingMetalBlockEntity>> MOVING_METAL_BLOCK = DEF_REG.register("moving_metal_block", () -> (EntityType) EntityType.Builder.of(MovingMetalBlockEntity::new, MobCategory.MISC).sized(0.99F, 0.99F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).updateInterval(10).clientTrackingRange(20).build(id("moving_metal_block")));
    public static final Supplier<EntityType<TeletorEntity>> TELETOR = DEF_REG.register("teletor", () -> (EntityType) EntityType.Builder.of(TeletorEntity::new, MobCategory.MONSTER).sized(0.99F, 1.99F).build(id("teletor")));
    public static final Supplier<EntityType<MagneticWeaponEntity>> MAGNETIC_WEAPON = DEF_REG.register("magnetic_weapon", () -> (EntityType) EntityType.Builder.of(MagneticWeaponEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).clientTrackingRange(20).build(id("magnetic_weapon")));
    public static final Supplier<EntityType<MagnetronEntity>> MAGNETRON = DEF_REG.register("magnetron", () -> (EntityType) EntityType.Builder.of(MagnetronEntity::new, MobCategory.MONSTER).sized(0.8F, 2.0F).build(id("magnetron")));
    public static final Supplier<EntityType<BoundroidEntity>> BOUNDROID = DEF_REG.register("boundroid", () -> (EntityType) EntityType.Builder.of(BoundroidEntity::new, MobCategory.MONSTER).sized(1.4F, 0.75F).build(id("boundroid")));
    public static final Supplier<EntityType<BoundroidWinchEntity>> BOUNDROID_WINCH = DEF_REG.register("boundroid_winch", () -> (EntityType) EntityType.Builder.of(BoundroidWinchEntity::new, MobCategory.MONSTER).sized(0.8F, 1.4F).build(id("boundroid_winch")));
    public static final Supplier<EntityType<FerrouslimeEntity>> FERROUSLIME = DEF_REG.register("ferrouslime", () -> (EntityType) EntityType.Builder.of(FerrouslimeEntity::new, MobCategory.MONSTER).sized(0.85F, 0.85F).build(id("ferrouslime")));
    public static final Supplier<EntityType<NotorEntity>> NOTOR = DEF_REG.register("notor", () -> (EntityType) EntityType.Builder.of(NotorEntity::new, MobCategory.AMBIENT).sized(0.5F, 0.65F).build(id("notor")));
    public static final Supplier<EntityType<QuarrySmasherEntity>> QUARRY_SMASHER = DEF_REG.register("quarry_smasher", () -> (EntityType) EntityType.Builder.of(QuarrySmasherEntity::new, MobCategory.MISC).sized(0.9F, 1.2F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).updateInterval(10).clientTrackingRange(20).build(id("quarry_smasher")));
    public static final Supplier<EntityType<SeekingArrowEntity>> SEEKING_ARROW = DEF_REG.register("seeking_arrow", () -> (EntityType) EntityType.Builder.of(SeekingArrowEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).build(id("seeking_arrow")));
    public static final Supplier<EntityType<SubterranodonEntity>> SUBTERRANODON = DEF_REG.register("subterranodon", () -> (EntityType) EntityType.Builder.of(SubterranodonEntity::new, CAVE_CREATURE).sized(1.75F, 1.2F).setTrackingRange(12).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).build(id("subterranodon")));
    public static final Supplier<EntityType<VallumraptorEntity>> VALLUMRAPTOR = DEF_REG.register("vallumraptor", () -> (EntityType) EntityType.Builder.of(VallumraptorEntity::new, CAVE_CREATURE).sized(0.8F, 1.5F).setTrackingRange(8).build(id("vallumraptor")));
    public static final Supplier<EntityType<GrottoceratopsEntity>> GROTTOCERATOPS = DEF_REG.register("grottoceratops", () -> (EntityType) EntityType.Builder.of(GrottoceratopsEntity::new, CAVE_CREATURE).sized(2.3F, 2.5F).setTrackingRange(8).build(id("grottoceratops")));
    public static final Supplier<EntityType<TrilocarisEntity>> TRILOCARIS = DEF_REG.register("trilocaris", () -> (EntityType) EntityType.Builder.of(TrilocarisEntity::new, MobCategory.WATER_AMBIENT).sized(0.9F, 0.4F).build(id("trilocaris")));
    public static final Supplier<EntityType<TremorsaurusEntity>> TREMORSAURUS = DEF_REG.register("tremorsaurus", () -> (EntityType) EntityType.Builder.of(TremorsaurusEntity::new, CAVE_CREATURE).sized(2.5F, 3.85F).setTrackingRange(8).build(id("tremorsaurus")));
    public static final Supplier<EntityType<RelicheirusEntity>> RELICHEIRUS = DEF_REG.register("relicheirus", () -> (EntityType) EntityType.Builder.of(RelicheirusEntity::new, CAVE_CREATURE).sized(2.65F, 5.9F).setTrackingRange(9).build(id("relicheirus")));
    public static final Supplier<EntityType<LuxtructosaurusEntity>> LUXTRUCTOSAURUS = DEF_REG.register("luxtructosaurus", () -> (EntityType) EntityType.Builder.of(LuxtructosaurusEntity::new, MobCategory.MONSTER).sized(6.0F, 8.5F).setTrackingRange(12).fireImmune().build(id("luxtructosaurus")));
    public static final Supplier<EntityType<TephraEntity>> TEPHRA = DEF_REG.register("tephra", () -> (EntityType) EntityType.Builder.of(TephraEntity::new, MobCategory.MISC).sized(0.6F, 0.6F).fireImmune().setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).build(id("tephra")));
    public static final Supplier<EntityType<AtlatitanEntity>> ATLATITAN = DEF_REG.register("atlatitan", () -> (EntityType) EntityType.Builder.of(AtlatitanEntity::new, CAVE_CREATURE).sized(5.0F, 8.0F).setTrackingRange(11).build(id("atlatitan")));
    public static final Supplier<EntityType<FallingTreeBlockEntity>> FALLING_TREE_BLOCK = DEF_REG.register("falling_tree_block", () -> (EntityType) EntityType.Builder.of(FallingTreeBlockEntity::new, MobCategory.MISC).sized(0.99F, 0.99F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).updateInterval(10).clientTrackingRange(20).build(id("falling_tree_block")));
    public static final Supplier<EntityType<CrushedBlockEntity>> CRUSHED_BLOCK = DEF_REG.register("crushed_block", () -> (EntityType) EntityType.Builder.of(CrushedBlockEntity::new, MobCategory.MISC).sized(0.99F, 0.99F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).updateInterval(10).clientTrackingRange(20).build(id("crushed_block")));
    public static final Supplier<EntityType<LimestoneSpearEntity>> LIMESTONE_SPEAR = DEF_REG.register("limestone_spear", () -> (EntityType) EntityType.Builder.of(LimestoneSpearEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).build(id("limestone_spear")));
    public static final Supplier<EntityType<ExtinctionSpearEntity>> EXTINCTION_SPEAR = DEF_REG.register("extinction_spear", () -> (EntityType) EntityType.Builder.of(ExtinctionSpearEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).fireImmune().build(id("extinction_spear")));
    public static final Supplier<EntityType<DinosaurSpiritEntity>> DINOSAUR_SPIRIT = DEF_REG.register("dinosaur_spirit", () -> (EntityType) EntityType.Builder.of(DinosaurSpiritEntity::new, MobCategory.MISC).sized(1.0F, 1.0F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).fireImmune().build(id("dinosaur_spirit")));
    public static final Supplier<EntityType<NuclearExplosionEntity>> NUCLEAR_EXPLOSION = DEF_REG.register("nuclear_explosion", () -> (EntityType) EntityType.Builder.of(NuclearExplosionEntity::new, MobCategory.MISC).sized(0.99F, 0.99F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).updateInterval(10).clientTrackingRange(20).build(id("nuclear_explosion")));
    public static final Supplier<EntityType<NuclearBombEntity>> NUCLEAR_BOMB = DEF_REG.register("nuclear_bomb", () -> (EntityType) EntityType.Builder.of(NuclearBombEntity::new, MobCategory.MISC).sized(0.98F, 0.98F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).updateInterval(10).clientTrackingRange(20).build(id("nuclear_bomb")));
    public static final Supplier<EntityType<NucleeperEntity>> NUCLEEPER = DEF_REG.register("nucleeper", () -> (EntityType) EntityType.Builder.of(NucleeperEntity::new, MobCategory.MONSTER).sized(0.98F, 3.95F).build(id("nucleeper")));
    public static final Supplier<EntityType<RadgillEntity>> RADGILL = DEF_REG.register("radgill", () -> (EntityType) EntityType.Builder.of(RadgillEntity::new, MobCategory.WATER_AMBIENT).sized(0.9F, 0.6F).build(id("radgill")));
    public static final Supplier<EntityType<BrainiacEntity>> BRAINIAC = DEF_REG.register("brainiac", () -> (EntityType) EntityType.Builder.of(BrainiacEntity::new, MobCategory.MONSTER).sized(1.3F, 2.5F).build(id("brainiac")));
    public static final Supplier<EntityType<ThrownWasteDrumEntity>> THROWN_WASTE_DRUM = DEF_REG.register("thrown_waste_drum", () -> (EntityType) EntityType.Builder.of(ThrownWasteDrumEntity::new, MobCategory.MISC).sized(0.98F, 0.98F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).updateInterval(10).clientTrackingRange(20).build(id("thrown_waste_drum")));
    public static final Supplier<EntityType<GammaroachEntity>> GAMMAROACH = DEF_REG.register("gammaroach", () -> (EntityType) EntityType.Builder.of(GammaroachEntity::new, MobCategory.AMBIENT).sized(1.25F, 0.9F).build(id("gammaroach")));
    public static final Supplier<EntityType<RaycatEntity>> RAYCAT = DEF_REG.register("raycat", () -> (EntityType) EntityType.Builder.of(RaycatEntity::new, CAVE_CREATURE).sized(0.85F, 0.6F).build(id("raycat")));
    public static final Supplier<EntityType<CinderBrickEntity>> CINDER_BRICK = DEF_REG.register("cinder_brick", () -> (EntityType) EntityType.Builder.of(CinderBrickEntity::new, MobCategory.MISC).sized(0.4F, 0.4F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).build(id("cinder_brick")));
    public static final Supplier<EntityType<TremorzillaEntity>> TREMORZILLA = DEF_REG.register("tremorzilla", () -> (EntityType) EntityType.Builder.of(TremorzillaEntity::new, CAVE_CREATURE).sized(4.5F, 11F).setTrackingRange(11).fireImmune().build(id("tremorzilla")));
    public static final Supplier<EntityType<LanternfishEntity>> LANTERNFISH = DEF_REG.register("lanternfish", () -> (EntityType) EntityType.Builder.of(LanternfishEntity::new, MobCategory.WATER_AMBIENT).sized(0.5F, 0.4F).build(id("lanternfish")));
    public static final Supplier<EntityType<SeaPigEntity>> SEA_PIG = DEF_REG.register("sea_pig", () -> (EntityType) EntityType.Builder.of(SeaPigEntity::new, DEEP_SEA_CREATURE).sized(0.5F, 0.65F).build(id("sea_pig")));
    public static final Supplier<EntityType<SubmarineEntity>> SUBMARINE = DEF_REG.register("submarine", () -> (EntityType) EntityType.Builder.of(SubmarineEntity::new, MobCategory.MISC).sized(3.5F, 3.3F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).updateInterval(10).clientTrackingRange(20).build(id("submarine")));
    public static final Supplier<EntityType<HullbreakerEntity>> HULLBREAKER = DEF_REG.register("hullbreaker", () -> (EntityType) EntityType.Builder.of(HullbreakerEntity::new, MobCategory.UNDERGROUND_WATER_CREATURE).sized(4.65F, 4.5F).setShouldReceiveVelocityUpdates(true).clientTrackingRange(20).build(id("hullbreaker")));
    public static final Supplier<EntityType<GossamerWormEntity>> GOSSAMER_WORM = DEF_REG.register("gossamer_worm", () -> (EntityType) EntityType.Builder.of(GossamerWormEntity::new, DEEP_SEA_CREATURE).sized(1.15F, 0.5F).build(id("gossamer_worm")));
    public static final Supplier<EntityType<TripodfishEntity>> TRIPODFISH = DEF_REG.register("tripodfish", () -> (EntityType) EntityType.Builder.of(TripodfishEntity::new, DEEP_SEA_CREATURE).sized(0.95F, 0.5F).build(id("tripodfish")));
    public static final Supplier<EntityType<DeepOneEntity>> DEEP_ONE = DEF_REG.register("deep_one", () -> (EntityType) EntityType.Builder.of(DeepOneEntity::new, MobCategory.MONSTER).sized(0.9F, 2.2F).setTrackingRange(12).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).build(id("deep_one")));
    public static final Supplier<EntityType<InkBombEntity>> INK_BOMB = DEF_REG.register("ink_bomb", () -> (EntityType) EntityType.Builder.of(InkBombEntity::new, MobCategory.MISC).sized(0.6F, 0.6F).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).build(id("ink_bomb")));
    public static final Supplier<EntityType<DeepOneKnightEntity>> DEEP_ONE_KNIGHT = DEF_REG.register("deep_one_knight", () -> (EntityType) EntityType.Builder.of(DeepOneKnightEntity::new, MobCategory.MONSTER).sized(1.15F, 2.5F).setTrackingRange(12).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).build(id("deep_one_knight")));
    public static final Supplier<EntityType<DeepOneMageEntity>> DEEP_ONE_MAGE = DEF_REG.register("deep_one_mage", () -> (EntityType) EntityType.Builder.of(DeepOneMageEntity::new, MobCategory.MONSTER).sized(1.35F, 2.5F).setTrackingRange(12).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).build(id("deep_one_mage")));
    public static final Supplier<EntityType<WaterBoltEntity>> WATER_BOLT = DEF_REG.register("water_bolt", () -> (EntityType) EntityType.Builder.of(WaterBoltEntity::new, MobCategory.MISC).sized(0.6F, 0.6F).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).build(id("water_bolt")));
    public static final Supplier<EntityType<WaveEntity>> WAVE = DEF_REG.register("wave", () -> (EntityType) EntityType.Builder.of(WaveEntity::new, MobCategory.MISC).sized(0.9F, 0.9F).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).build(id("wave")));
    public static final Supplier<EntityType<MineGuardianEntity>> MINE_GUARDIAN = DEF_REG.register("mine_guardian", () -> (EntityType) EntityType.Builder.of(MineGuardianEntity::new, MobCategory.MONSTER).sized(1.3F, 1.3F).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).build(id("mine_guardian")));
    public static final Supplier<EntityType<MineGuardianAnchorEntity>> MINE_GUARDIAN_ANCHOR = DEF_REG.register("mine_guardian_anchor", () -> (EntityType) EntityType.Builder.of(MineGuardianAnchorEntity::new, MobCategory.MISC).sized(0.6F, 1.35F).build(id("mine_guardian_anchor")));
    public static final Supplier<EntityType<DepthChargeEntity>> DEPTH_CHARGE = DEF_REG.register("depth_charge", () -> (EntityType) EntityType.Builder.of(DepthChargeEntity::new, MobCategory.MISC).sized(0.7F, 0.7F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).build(id("depth_charge")));
    public static final Supplier<EntityType<FloaterEntity>> FLOATER = DEF_REG.register("floater", () -> (EntityType) EntityType.Builder.of(FloaterEntity::new, MobCategory.MISC).sized(0.98F, 0.98F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).updateInterval(10).clientTrackingRange(20).build(id("floater")));
    public static final Supplier<EntityType<GuanoEntity>> GUANO = DEF_REG.register("guano", () -> (EntityType) EntityType.Builder.of(GuanoEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).build(id("guano")));
    public static final Supplier<EntityType<FallingGuanoEntity>> FALLING_GUANO = DEF_REG.register("falling_guano", () -> (EntityType) EntityType.Builder.of(FallingGuanoEntity::new, MobCategory.MISC).sized(0.8F, 0.9F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).updateInterval(10).clientTrackingRange(20).build(id("falling_guano")));
    public static final Supplier<EntityType<GloomothEntity>> GLOOMOTH = DEF_REG.register("gloomoth", () -> (EntityType) EntityType.Builder.of(GloomothEntity::new, MobCategory.AMBIENT).sized(0.99F, 0.99F).setTrackingRange(12).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).build(id("gloomoth")));
    public static final Supplier<EntityType<UnderzealotEntity>> UNDERZEALOT = DEF_REG.register("underzealot", () -> (EntityType) EntityType.Builder.of(UnderzealotEntity::new, MobCategory.MONSTER).sized(0.8F, 1.2F).build(id("underzealot")));
    public static final Supplier<EntityType<WatcherEntity>> WATCHER = DEF_REG.register("watcher", () -> (EntityType) EntityType.Builder.of(WatcherEntity::new, MobCategory.MONSTER).sized(0.9F, 1.9F).build(id("watcher")));
    public static final Supplier<EntityType<CorrodentEntity>> CORRODENT = DEF_REG.register("corrodent", () -> (EntityType) EntityType.Builder.of(CorrodentEntity::new, MobCategory.MONSTER).sized(0.9F, 0.9F).build(id("corrodent")));
    public static final Supplier<EntityType<VesperEntity>> VESPER = DEF_REG.register("vesper", () -> (EntityType) EntityType.Builder.of(VesperEntity::new, MobCategory.MONSTER).sized(1.2F, 1.65F).setTrackingRange(12).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).build(id("vesper")));
    public static final Supplier<EntityType<ForsakenEntity>> FORSAKEN = DEF_REG.register("forsaken", () -> (EntityType) EntityType.Builder.of(ForsakenEntity::new, MobCategory.MONSTER).sized(3F, 3.5F).setTrackingRange(12).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).build(id("forsaken")));
    public static final Supplier<EntityType<BeholderEyeEntity>> BEHOLDER_EYE = DEF_REG.register("beholder_eye", () -> (EntityType) EntityType.Builder.of(BeholderEyeEntity::new, MobCategory.MISC).sized(0.3F, 0.3F).build(id("beholder_eye")));
    public static final Supplier<EntityType<DesolateDaggerEntity>> DESOLATE_DAGGER = DEF_REG.register("desolate_dagger", () -> (EntityType) EntityType.Builder.of(DesolateDaggerEntity::new, MobCategory.MISC).sized(0.6F, 0.6F).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).build(id("desolate_dagger")));
    public static final Supplier<EntityType<BurrowingArrowEntity>> BURROWING_ARROW = DEF_REG.register("burrowing_arrow", () -> (EntityType) EntityType.Builder.of(BurrowingArrowEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).build(id("burrowing_arrow")));
    public static final Supplier<EntityType<DarkArrowEntity>> DARK_ARROW = DEF_REG.register("dark_arrow", () -> (EntityType) EntityType.Builder.of(DarkArrowEntity::new, MobCategory.MISC).sized(1.1F, 0.5F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).build(id("dark_arrow")));
    public static final Supplier<EntityType<SweetishFishEntity>> SWEETISH_FISH = DEF_REG.register("sweetish_fish", () -> (EntityType) EntityType.Builder.of(SweetishFishEntity::new, MobCategory.WATER_AMBIENT).sized(0.7F, 0.45F).build(id("sweetish_fish")));
    public static final Supplier<EntityType<CaniacEntity>> CANIAC = DEF_REG.register("caniac", () -> (EntityType) EntityType.Builder.of(CaniacEntity::new, MobCategory.MONSTER).sized(0.9F, 2.3F).setTrackingRange(12).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).build(id("caniac")));
    public static final Supplier<EntityType<GumbeeperEntity>> GUMBEEPER = DEF_REG.register("gumbeeper", () -> (EntityType) EntityType.Builder.of(GumbeeperEntity::new, MobCategory.MONSTER).sized(0.8F, 1.6F).build(id("gumbeeper")));
    public static final Supplier<EntityType<GumballEntity>> GUMBALL = DEF_REG.register("gumball", () -> (EntityType) EntityType.Builder.of(GumballEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).build(id("gumball")));
    public static final Supplier<EntityType<CandicornEntity>> CANDICORN = DEF_REG.register("candicorn", () -> (EntityType) EntityType.Builder.of(CandicornEntity::new, CAVE_CREATURE).sized(1.7F, 2.25F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).build(id("candicorn")));
    public static final Supplier<EntityType<GumWormEntity>> GUM_WORM = DEF_REG.register("gum_worm", () -> (EntityType) EntityType.Builder.of(GumWormEntity::new, MobCategory.MONSTER).sized(3.5F, 2.7F).setTrackingRange(14).setShouldReceiveVelocityUpdates(true).setUpdateInterval(1).fireImmune().build(id("gum_worm")));
    public static final Supplier<EntityType<GumWormSegmentEntity>> GUM_WORM_SEGMENT = DEF_REG.register("gum_worm_segment", () -> (EntityType) EntityType.Builder.of(GumWormSegmentEntity::new, MobCategory.MISC).sized(2.0F, 2.0F).setTrackingRange(14).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).fireImmune().build(id("gum_worm_segment")));
    public static final Supplier<EntityType<CaramelCubeEntity>> CARAMEL_CUBE = DEF_REG.register("caramel_cube", () -> (EntityType) EntityType.Builder.of(CaramelCubeEntity::new, MobCategory.MONSTER).sized(0.8F, 0.8F).build(id("caramel_cube")));
    public static final Supplier<EntityType<MeltedCaramelEntity>> MELTED_CARAMEL = DEF_REG.register("melted_caramel", () -> (EntityType) EntityType.Builder.of(MeltedCaramelEntity::new, MobCategory.MISC).sized(0.99F, 0.1F).build(id("melted_caramel")));
    public static final Supplier<EntityType<GummyBearEntity>> GUMMY_BEAR = DEF_REG.register("gummy_bear", () -> (EntityType) EntityType.Builder.of(GummyBearEntity::new, CAVE_CREATURE).sized(1.45F, 1.2F).build(id("gummy_bear")));
    public static final Supplier<EntityType<LicowitchEntity>> LICOWITCH = DEF_REG.register("licowitch", () -> (EntityType) EntityType.Builder.of(LicowitchEntity::new, MobCategory.MONSTER).sized(0.8F, 1.9F).build(id("licowitch")));
    public static final Supplier<EntityType<SpinningPeppermintEntity>> SPINNING_PEPPERMINT = DEF_REG.register("spinning_peppermint", () -> (EntityType) EntityType.Builder.of(SpinningPeppermintEntity::new, MobCategory.MISC).sized(0.8F, 0.4F).build(id("spinning_peppermint")));
    public static final Supplier<EntityType<SugarStaffHexEntity>> SUGAR_STAFF_HEX = DEF_REG.register("sugar_staff_hex", () -> (EntityType) EntityType.Builder.of(SugarStaffHexEntity::new, MobCategory.MISC).sized(4.0F, 0.25F).build(id("sugar_staff_hex")));
    public static final Supplier<EntityType<GingerbreadManEntity>> GINGERBREAD_MAN = DEF_REG.register("gingerbread_man", () -> (EntityType) EntityType.Builder.of(GingerbreadManEntity::new, MobCategory.MONSTER).sized(0.5F, 0.9F).build(id("gingerbread_man")));
    public static final Supplier<EntityType<ThrownIceCreamScoopEntity>> THROWN_ICE_CREAM_SCOOP = DEF_REG.register("thrown_ice_cream_scoop", () -> (EntityType) EntityType.Builder.of(ThrownIceCreamScoopEntity::new, MobCategory.MISC).sized(0.4F, 0.4F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).build(id("thrown_ice_cream_scoop")));
    public static final Supplier<EntityType<FallingFrostmintEntity>> FALLING_FROSTMINT = DEF_REG.register("falling_frostmint", () -> (EntityType) EntityType.Builder.of(FallingFrostmintEntity::new, MobCategory.MISC).sized(0.8F, 0.9F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).updateInterval(10).clientTrackingRange(20).build(id("falling_frostmint")));
    public static final Supplier<EntityType<CandyCaneHookEntity>> CANDY_CANE_HOOK = DEF_REG.register("candy_cane_hook", () -> (EntityType) EntityType.Builder.of(CandyCaneHookEntity::new, MobCategory.MISC).sized(0.6F, 0.6F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).updateInterval(10).clientTrackingRange(20).build(id("candy_cane_hook")));
    public static final Supplier<EntityType<SodaBottleRocketEntity>> SODA_BOTTLE_ROCKET = DEF_REG.register("soda_bottle_rocket", () -> (EntityType) EntityType.Builder.of(SodaBottleRocketEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).build(id("soda_bottle_rocket")));
    public static final Supplier<EntityType<FrostmintSpearEntity>> FROSTMINT_SPEAR = DEF_REG.register("frostmint_spear", () -> (EntityType) EntityType.Builder.of(FrostmintSpearEntity::new, MobCategory.MISC).sized(0.5F, 0.5F).setUpdateInterval(1).setShouldReceiveVelocityUpdates(true).build(id("frostmint_spear")));

    @SubscribeEvent
    public static void initializeAttributes(EntityAttributeCreationEvent event) {
        event.put(TELETOR.get(), TeletorEntity.createAttributes().build());
        event.put(MAGNETRON.get(), MagnetronEntity.createAttributes().build());
        event.put(BOUNDROID.get(), BoundroidEntity.createAttributes().build());
        event.put(BOUNDROID_WINCH.get(), BoundroidEntity.createAttributes().build());
        event.put(FERROUSLIME.get(), FerrouslimeEntity.createAttributes().build());
        event.put(NOTOR.get(), NotorEntity.createAttributes().build());
        event.put(SUBTERRANODON.get(), SubterranodonEntity.createAttributes().build());
        event.put(VALLUMRAPTOR.get(), VallumraptorEntity.createAttributes().build());
        event.put(GROTTOCERATOPS.get(), GrottoceratopsEntity.createAttributes().build());
        event.put(TRILOCARIS.get(), TrilocarisEntity.createAttributes().build());
        event.put(TREMORSAURUS.get(), TremorsaurusEntity.createAttributes().build());
        event.put(RELICHEIRUS.get(), RelicheirusEntity.createAttributes().build());
        event.put(LUXTRUCTOSAURUS.get(), LuxtructosaurusEntity.createAttributes().build());
        event.put(ATLATITAN.get(), AtlatitanEntity.createAttributes().build());
        event.put(NUCLEEPER.get(), NucleeperEntity.createAttributes().build());
        event.put(RADGILL.get(), RadgillEntity.createAttributes().build());
        event.put(BRAINIAC.get(), BrainiacEntity.createAttributes().build());
        event.put(GAMMAROACH.get(), GammaroachEntity.createAttributes().build());
        event.put(RAYCAT.get(), RaycatEntity.createAttributes().build());
        event.put(TREMORZILLA.get(), TremorzillaEntity.createAttributes().build());
        event.put(LANTERNFISH.get(), LanternfishEntity.createAttributes().build());
        event.put(SEA_PIG.get(), SeaPigEntity.createAttributes().build());
        event.put(HULLBREAKER.get(), HullbreakerEntity.createAttributes().build());
        event.put(GOSSAMER_WORM.get(), GossamerWormEntity.createAttributes().build());
        event.put(TRIPODFISH.get(), TripodfishEntity.createAttributes().build());
        event.put(DEEP_ONE.get(), DeepOneEntity.createAttributes().build());
        event.put(DEEP_ONE_KNIGHT.get(), DeepOneKnightEntity.createAttributes().build());
        event.put(DEEP_ONE_MAGE.get(), DeepOneMageEntity.createAttributes().build());
        event.put(MINE_GUARDIAN.get(), MineGuardianEntity.createAttributes().build());
        event.put(GLOOMOTH.get(), GloomothEntity.createAttributes().build());
        event.put(UNDERZEALOT.get(), UnderzealotEntity.createAttributes().build());
        event.put(WATCHER.get(), WatcherEntity.createAttributes().build());
        event.put(CORRODENT.get(), CorrodentEntity.createAttributes().build());
        event.put(VESPER.get(), VesperEntity.createAttributes().build());
        event.put(FORSAKEN.get(), ForsakenEntity.createAttributes().build());
        event.put(SWEETISH_FISH.get(), SweetishFishEntity.createAttributes().build());
        event.put(CANIAC.get(), CaniacEntity.createAttributes().build());
        event.put(GUMBEEPER.get(), GumbeeperEntity.createAttributes().build());
        event.put(CANDICORN.get(), CandicornEntity.createAttributes().build());
        event.put(GUM_WORM.get(), GumWormEntity.createAttributes().build());
        event.put(CARAMEL_CUBE.get(), CaramelCubeEntity.createAttributes().build());
        event.put(GUMMY_BEAR.get(), GummyBearEntity.createAttributes().build());
        event.put(LICOWITCH.get(), LicowitchEntity.createAttributes().build());
        event.put(GINGERBREAD_MAN.get(), GingerbreadManEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void spawnPlacements(SpawnPlacementRegisterEvent event) {
        // 1.20.5 turned the SpawnPlacements.Type enum into the SpawnPlacementType interface (vanilla's
        // four constants moved to SpawnPlacementTypes). A custom placement is no longer created
        // through a factory + name — the interface has one abstract method, so the same predicate is
        // now simply the value. Only the wrapper differs; the two lambda bodies are byte-identical
        // and the ON_GROUND/IN_WATER references elsewhere in this method are handled by the
        // !mc205-spawnplacement-* replacements.
        //
        // Below that, `Type.create` is Forge's, so Fabric reads the two constants its own mixin
        // appended to the enum instead — the predicates live with them, in fabric.entity
        // .ACSpawnPlacementTypes, since a constant added that way can carry no state of its own.
        //? if >=1.20.5 {
        /*net.minecraft.world.entity.SpawnPlacementType inAcid = (levelReader, blockPos, entityType) -> !levelReader.getFluidState(blockPos).isEmpty() && levelReader.getFluidState(blockPos).is(ACTagRegistry.ACID);
        net.minecraft.world.entity.SpawnPlacementType inSoda = (levelReader, blockPos, entityType) -> !levelReader.getFluidState(blockPos).isEmpty() && levelReader.getFluidState(blockPos).is(ACTagRegistry.PURPLE_SODA);
        *///?} elif fabric {
        /*SpawnPlacements.Type inAcid = com.github.alexmodguy.alexscaves.fabric.entity.ACSpawnPlacementTypes.IN_ACID;
        SpawnPlacements.Type inSoda = com.github.alexmodguy.alexscaves.fabric.entity.ACSpawnPlacementTypes.IN_SODA;
        *///?} else {
        SpawnPlacements.Type inAcid = SpawnPlacements.Type.create("in_acid", (levelReader, blockPos, entityType) -> !levelReader.getFluidState(blockPos).isEmpty() && levelReader.getFluidState(blockPos).is(ACTagRegistry.ACID));
        SpawnPlacements.Type inSoda = SpawnPlacements.Type.create("in_soda", (levelReader, blockPos, entityType) -> !levelReader.getFluidState(blockPos).isEmpty() && levelReader.getFluidState(blockPos).is(ACTagRegistry.PURPLE_SODA));
        //?}
        event.register(TELETOR.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, TeletorEntity::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(MAGNETRON.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MagnetronEntity::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(BOUNDROID.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BoundroidEntity::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(FERROUSLIME.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, FerrouslimeEntity::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(NOTOR.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, NotorEntity::checkNotorSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(SUBTERRANODON.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SubterranodonEntity::checkSubterranodonSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(VALLUMRAPTOR.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, VallumraptorEntity::checkPrehistoricSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(GROTTOCERATOPS.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, GrottoceratopsEntity::checkPrehistoricSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(TRILOCARIS.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, TrilocarisEntity::checkTrilocarisSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(TREMORSAURUS.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, TremorsaurusEntity::checkPrehistoricSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(RELICHEIRUS.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, RelicheirusEntity::checkPrehistoricSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(LUXTRUCTOSAURUS.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, LuxtructosaurusEntity::checkPrehistoricSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(ATLATITAN.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AtlatitanEntity::checkPrehistoricPostBossSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(NUCLEEPER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, NucleeperEntity::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(RADGILL.get(), inAcid, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, RadgillEntity::checkRadgillSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(BRAINIAC.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BrainiacEntity::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(GAMMAROACH.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, GammaroachEntity::checkGammaroachSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(RAYCAT.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, RaycatEntity::checkRaycatSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(LANTERNFISH.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, LanternfishEntity::checkLanternfishSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(SEA_PIG.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SeaPigEntity::checkSeaPigSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(HULLBREAKER.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, HullbreakerEntity::checkHullbreakerSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(GOSSAMER_WORM.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, GossamerWormEntity::checkGossamerWormSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(TRIPODFISH.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, TripodfishEntity::checkTripodfishSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(DEEP_ONE.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, DeepOneBaseEntity::checkDeepOneSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(DEEP_ONE_KNIGHT.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, DeepOneBaseEntity::checkDeepOneSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(DEEP_ONE_MAGE.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, DeepOneBaseEntity::checkDeepOneSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(MINE_GUARDIAN.get(), SpawnPlacements.Type.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MineGuardianEntity::checkMineGuardianSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(GLOOMOTH.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, GloomothEntity::checkGloomothSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(UNDERZEALOT.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, UnderzealotEntity::checkUnderzealotSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(WATCHER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, WatcherEntity::checkWatcherSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(CORRODENT.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CorrodentEntity::checkCorrodentSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(VESPER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, VesperEntity::checkVesperSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(FORSAKEN.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ForsakenEntity::checkForsakenSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(SWEETISH_FISH.get(), inSoda, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SweetishFishEntity::checkSweetishFishSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(CANIAC.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CaniacEntity::checkCaniacSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(GUMBEEPER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, GumbeeperEntity::checkGumbeeperSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(CANDICORN.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CandicornEntity::checkCandicornSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(GUM_WORM.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, GumWormEntity::checkGumWormSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(CARAMEL_CUBE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CaramelCubeEntity::checkCaramelCubeSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(GUMMY_BEAR.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, GummyBearEntity::checkGummyBearSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(LICOWITCH.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, LicowitchEntity::checkLicowitchSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
        event.register(GINGERBREAD_MAN.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, GingerbreadManEntity::checkGingerbreadManSpawnRules, SpawnPlacementRegisterEvent.Operation.AND);
    }
}

