package com.github.alexmodguy.alexscaves.server.block.poi;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;

import java.util.Set;

public class ACPOIRegistry {

    public static final DeferredRegister<PoiType> DEF_REG = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, AlexsCaves.MODID);
    public static final Supplier<PoiType> ATTRACTING_MAGNETS = DEF_REG.register("attracting_magnets", () -> new PoiType(getAllAttractingMagnets(), 32, 6));
    public static final Supplier<PoiType> REPELLING_MAGNETS = DEF_REG.register("repelling_magnets", () -> new PoiType(getAllRepellingMagnets(), 32, 6));
    public static final Supplier<PoiType> NUCLEAR_SIREN = DEF_REG.register("nuclear_siren", () -> new PoiType(getAllStatesOf(ACBlockRegistry.NUCLEAR_SIREN.get()), 0, 6));
    public static final Supplier<PoiType> NUCLEAR_FURNACE = DEF_REG.register("nuclear_furnace", () -> new PoiType(getAllStatesOf(ACBlockRegistry.NUCLEAR_FURNACE.get()), 0, 6));
    public static final Supplier<PoiType> ABYSSAL_ALTAR = DEF_REG.register("abyssal_altar", () -> new PoiType(getAllStatesOf(ACBlockRegistry.ABYSSAL_ALTAR.get()), 0, 6));
    public static final Supplier<PoiType> MOTH_BALL = DEF_REG.register("moth_ball", () -> new PoiType(getAllStatesOf(ACBlockRegistry.MOTH_BALL.get()), 32, 6));
    public static final Supplier<PoiType> SUNDROP = DEF_REG.register("sundrop", () -> new PoiType(getAllStatesOf(ACBlockRegistry.SUNDROP.get()), 32, 6));
    public static final Supplier<PoiType> CONVERSION_CRUCIBLE = DEF_REG.register("conversion_crucible", () -> new PoiType(getAllStatesOf(ACBlockRegistry.CONVERSION_CRUCIBLE.get()), 0, 6));
    public static final Supplier<PoiType> GINGERBARREL = DEF_REG.register("gingerbarrel", () -> new PoiType(getAllStatesOf(ACBlockRegistry.GINGERBARREL.get()), 0, 6));

    // Nine PoiManager queries match on a ResourceKey, which a plain Supplier handle cannot give:
    // Forge's RegistryObject#getKey has no NeoForge twin (see ACIdFactories' sibling problem
    // — NeoForge's DeferredHolder takes two type parameters, so the handles are declared as plain
    // Suppliers). Spelling the key from the very name the entry is registered under is exact, needs
    // no registry access, and so is safe to do in a static initialiser.
    private static ResourceKey<PoiType> key(String name) {
        return ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, name));
    }

    public static final ResourceKey<PoiType> ATTRACTING_MAGNETS_KEY = key("attracting_magnets");
    public static final ResourceKey<PoiType> REPELLING_MAGNETS_KEY = key("repelling_magnets");
    public static final ResourceKey<PoiType> NUCLEAR_SIREN_KEY = key("nuclear_siren");
    public static final ResourceKey<PoiType> NUCLEAR_FURNACE_KEY = key("nuclear_furnace");
    public static final ResourceKey<PoiType> ABYSSAL_ALTAR_KEY = key("abyssal_altar");
    public static final ResourceKey<PoiType> MOTH_BALL_KEY = key("moth_ball");
    public static final ResourceKey<PoiType> SUNDROP_KEY = key("sundrop");
    public static final ResourceKey<PoiType> CONVERSION_CRUCIBLE_KEY = key("conversion_crucible");
    public static final ResourceKey<PoiType> GINGERBARREL_KEY = key("gingerbarrel");

    private static Set<BlockState> getAllAttractingMagnets() {
        ImmutableSet.Builder<BlockState> builder = ImmutableSet.builder();
        builder.addAll(getAllStatesOf(ACBlockRegistry.SCARLET_NEODYMIUM_NODE.get()));
        builder.addAll(getAllStatesOf(ACBlockRegistry.SCARLET_NEODYMIUM_PILLAR.get()));
        builder.addAll(getAllStatesOf(ACBlockRegistry.BLOCK_OF_SCARLET_NEODYMIUM.get()));
        return builder.build();
    }

    private static Set<BlockState> getAllRepellingMagnets() {
        ImmutableSet.Builder<BlockState> builder = ImmutableSet.builder();
        builder.addAll(getAllStatesOf(ACBlockRegistry.AZURE_NEODYMIUM_NODE.get()));
        builder.addAll(getAllStatesOf(ACBlockRegistry.AZURE_NEODYMIUM_PILLAR.get()));
        builder.addAll(getAllStatesOf(ACBlockRegistry.BLOCK_OF_AZURE_NEODYMIUM.get()));
        return builder.build();
    }

    private static Set<BlockState> getAllStatesOf(Block block) {
        return ImmutableSet.copyOf(block.getStateDefinition().getPossibleStates());
    }
}
