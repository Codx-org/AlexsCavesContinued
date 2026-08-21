package com.github.alexmodguy.alexscaves.server.block.blockentity;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

public class ACBlockEntityRegistry {

    public static final DeferredRegister<BlockEntityType<?>> DEF_REG = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AlexsCaves.MODID);

    public static final Supplier<BlockEntityType<VolcanicCoreBlockEntity>> VOLCANIC_CORE = DEF_REG.register("volcanic_core", () -> BlockEntityType.Builder.of(VolcanicCoreBlockEntity::new, ACBlockRegistry.VOLCANIC_CORE.get()).build(null));
    public static final Supplier<BlockEntityType<MagnetBlockEntity>> MAGNET = DEF_REG.register("magnet", () -> BlockEntityType.Builder.of(MagnetBlockEntity::new, ACBlockRegistry.SCARLET_MAGNET.get(), ACBlockRegistry.AZURE_MAGNET.get()).build(null));
    public static final Supplier<BlockEntityType<TeslaBulbBlockEntity>> TESLA_BULB = DEF_REG.register("tesla_bulb", () -> BlockEntityType.Builder.of(TeslaBulbBlockEntity::new, ACBlockRegistry.TESLA_BULB.get()).build(null));
    public static final Supplier<BlockEntityType<HologramProjectorBlockEntity>> HOLOGRAM_PROJECTOR = DEF_REG.register("hologram_projector", () -> BlockEntityType.Builder.of(HologramProjectorBlockEntity::new, ACBlockRegistry.HOLOGRAM_PROJECTOR.get()).build(null));
    public static final Supplier<BlockEntityType<QuarryBlockEntity>> QUARRY = DEF_REG.register("quarry", () -> BlockEntityType.Builder.of(QuarryBlockEntity::new, ACBlockRegistry.QUARRY.get()).build(null));
    public static final Supplier<BlockEntityType<AmbersolBlockEntity>> AMBERSOL = DEF_REG.register("ambersol", () -> BlockEntityType.Builder.of(AmbersolBlockEntity::new, ACBlockRegistry.AMBERSOL.get()).build(null));
    public static final Supplier<BlockEntityType<AmberMonolithBlockEntity>> AMBER_MONOLITH = DEF_REG.register("amber_monolith", () -> BlockEntityType.Builder.of(AmberMonolithBlockEntity::new, ACBlockRegistry.AMBER_MONOLITH.get()).build(null));
    public static final Supplier<BlockEntityType<GeothermalVentBlockEntity>> GEOTHERMAL_VENT = DEF_REG.register("geothermal_vent", () -> BlockEntityType.Builder.of(GeothermalVentBlockEntity::new, ACBlockRegistry.GEOTHERMAL_VENT.get(), ACBlockRegistry.GEOTHERMAL_VENT_MEDIUM.get(), ACBlockRegistry.GEOTHERMAL_VENT_THIN.get()).build(null));
    public static final Supplier<BlockEntityType<NuclearFurnaceBlockEntity>> NUCLEAR_FURNACE = DEF_REG.register("nuclear_furnace", () -> BlockEntityType.Builder.of(NuclearFurnaceBlockEntity::new, ACBlockRegistry.NUCLEAR_FURNACE.get()).build(null));
    public static final Supplier<BlockEntityType<SirenLightBlockEntity>> SIREN_LIGHT = DEF_REG.register("siren_light", () -> BlockEntityType.Builder.of(SirenLightBlockEntity::new, ACBlockRegistry.SIREN_LIGHT.get()).build(null));
    public static final Supplier<BlockEntityType<NuclearSirenBlockEntity>> NUCLEAR_SIREN = DEF_REG.register("nuclear_siren", () -> BlockEntityType.Builder.of(NuclearSirenBlockEntity::new, ACBlockRegistry.NUCLEAR_SIREN.get()).build(null));
    public static final Supplier<BlockEntityType<MetalBarrelBlockEntity>> METAL_BARREL = DEF_REG.register("metal_barrel", () -> BlockEntityType.Builder.of(MetalBarrelBlockEntity::new, ACBlockRegistry.METAL_BARREL.get(), ACBlockRegistry.RUSTY_BARREL.get()).build(null));
    public static final Supplier<BlockEntityType<AbyssalAltarBlockEntity>> ABYSSAL_ALTAR = DEF_REG.register("abyssal_altar", () -> BlockEntityType.Builder.of(AbyssalAltarBlockEntity::new, ACBlockRegistry.ABYSSAL_ALTAR.get()).build(null));
    public static final Supplier<BlockEntityType<CopperValveBlockEntity>> COPPER_VALVE = DEF_REG.register("copper_valve", () -> BlockEntityType.Builder.of(CopperValveBlockEntity::new, ACBlockRegistry.COPPER_VALVE.get()).build(null));
    public static final Supplier<BlockEntityType<EnigmaticEngineBlockEntity>> ENIGMATIC_ENGINE = DEF_REG.register("enigmatic_engine", () -> BlockEntityType.Builder.of(EnigmaticEngineBlockEntity::new, ACBlockRegistry.ENIGMATIC_ENGINE.get()).build(null));
    public static final Supplier<BlockEntityType<BeholderBlockEntity>> BEHOLDER = DEF_REG.register("beholder", () -> BlockEntityType.Builder.of(BeholderBlockEntity::new, ACBlockRegistry.BEHOLDER.get()).build(null));
    public static final Supplier<BlockEntityType<GobthumperBlockEntity>> GOBTHUMPER = DEF_REG.register("gobthumper", () -> BlockEntityType.Builder.of(GobthumperBlockEntity::new, ACBlockRegistry.GOBTHUMPER.get()).build(null));
    public static final Supplier<BlockEntityType<ConversionCrucibleBlockEntity>> CONVERSION_CRUCIBLE = DEF_REG.register("conversion_crucible", () -> BlockEntityType.Builder.of(ConversionCrucibleBlockEntity::new, ACBlockRegistry.CONVERSION_CRUCIBLE.get()).build(null));
    public static final Supplier<BlockEntityType<GingerbarrelBlockEntity>> GINGERBARREL = DEF_REG.register("gingerbarrel", () -> BlockEntityType.Builder.of(GingerbarrelBlockEntity::new, ACBlockRegistry.GINGERBARREL.get()).build(null));
    public static final Supplier<BlockEntityType<ConfectionOvenBlockEntity>> CONFECTION_OVEN = DEF_REG.register("confection_oven", () -> BlockEntityType.Builder.of(ConfectionOvenBlockEntity::new, ACBlockRegistry.CONFECTION_OVEN.get()).build(null));

    // 26.2 moved every public static final BlockEntityType constant out of BlockEntityType into a
    // new BlockEntityTypes holder beside it — the same move it made for EntityType. The constants
    // are all this class needs; validBlocks is still a field of the type itself and is named in
    // both access transformers unchanged, so only where the handle comes from moves.
    private static BlockEntityType<?> vanillaSign() {
        //? if >=26.2 {
        /*return net.minecraft.world.level.block.entity.BlockEntityTypes.SIGN;
        *///?} else {
        return BlockEntityType.SIGN;
        //?}
    }

    private static BlockEntityType<?> vanillaHangingSign() {
        //? if >=26.2 {
        /*return net.minecraft.world.level.block.entity.BlockEntityTypes.HANGING_SIGN;
        *///?} else {
        return BlockEntityType.HANGING_SIGN;
        //?}
    }

    // The set is rebuilt MUTABLE on purpose. Fabric API hands other mods BlockEntityType
    // #addValidBlock, which adds straight to this field, and Farmer's Delight uses it for its
    // canvas signs. Assigning an ImmutableSet here made whichever of the two initialised second
    // die on UnsupportedOperationException - and Fabric orders mod initialisers by discovery, so
    // it crashed on some installs and not others. A LinkedHashSet keeps the iteration order the
    // builder gave and leaves the field open for everyone downstream.
    public static void expandVanillaDefinitions() {
        Set<Block> validSignBlocks = new LinkedHashSet<>(vanillaSign().validBlocks);
        validSignBlocks.add(ACBlockRegistry.PEWEN_SIGN.get());
        validSignBlocks.add(ACBlockRegistry.PEWEN_WALL_SIGN.get());
        validSignBlocks.add(ACBlockRegistry.THORNWOOD_SIGN.get());
        validSignBlocks.add(ACBlockRegistry.THORNWOOD_WALL_SIGN.get());
        vanillaSign().validBlocks = validSignBlocks;
        Set<Block> validHangingSignBlocks = new LinkedHashSet<>(vanillaHangingSign().validBlocks);
        validHangingSignBlocks.add(ACBlockRegistry.PEWEN_HANGING_SIGN.get());
        validHangingSignBlocks.add(ACBlockRegistry.PEWEN_WALL_HANGING_SIGN.get());
        validHangingSignBlocks.add(ACBlockRegistry.THORNWOOD_HANGING_SIGN.get());
        validHangingSignBlocks.add(ACBlockRegistry.THORNWOOD_WALL_HANGING_SIGN.get());
        vanillaHangingSign().validBlocks = validHangingSignBlocks;
    }
}
