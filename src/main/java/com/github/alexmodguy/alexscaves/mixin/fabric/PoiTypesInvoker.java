package com.github.alexmodguy.alexscaves.mixin.fabric;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

/**
 * Puts a modded point-of-interest type's blockstates into vanilla's blockstate&rarr;POI map.
 *
 * <p><b>Why this exists.</b> Registering a {@link PoiType} is only half of what makes a block a
 * point of interest. {@code PoiTypes} keeps a private static {@code TYPE_BY_STATE} map, filled
 * <i>only</i> by its own {@code bootstrap}, and {@code PoiTypes.forState} — the single question
 * {@code PoiSection} asks of every block it is handed — reads that map and nothing else. So a type
 * that reached the registry but not the map is a POI no chunk will ever record: no error, no log
 * line, every lookup simply comes back empty.
 *
 * <p>The other two loaders each close that gap themselves, which is why this is Fabric-only and why
 * the bug it fixes was invisible on the node the tree is developed on: Forge keeps its own
 * blockstate map behind {@code GameData$PointOfInterestTypeCallbacks}, NeoForge rebuilds vanilla's
 * from the registry in {@code PoiTypeExtender.extendPoiTypes}. Fabric has neither, and this mod's
 * {@code DeferredRegister} stand-in registers straight into {@code BuiltInRegistries}, so all nine
 * of {@code ACPOIRegistry}'s types were dead on all 22 Fabric nodes — magnetism (the neodymium
 * nodes, pillars and blocks are POIs, and {@code MagnetUtil} finds them through
 * {@code PoiManager.findAll}), moth balls, sundrops, the nuclear siren and furnace, the abyssal
 * altar, the conversion crucible and the gingerbarrel.
 *
 * <p>{@code registerBlockStates} is private, but its descriptor
 * ({@code (Lnet/minecraft/core/Holder;Ljava/util/Set;)V}) is identical on every MC version in the
 * matrix — verified with javap on 1.20.1, 1.20.5, 1.21.2, 1.21.5, 1.21.9, 1.21.11, 26.1 and 26.2 —
 * so no gate is needed. Calling vanilla's own method rather than writing to the map directly also
 * keeps its duplicate-state warning, which is the only diagnostic there is if two mods ever claim
 * one blockstate.
 *
 * <p>Existing worlds heal on their own: {@code checkConsistencyWithBlocks} runs from chunk
 * deserialization, so a chunk saved before the types were mapped re-scans on its next load.
 */
@Mixin(PoiTypes.class)
public interface PoiTypesInvoker {

    @Invoker("registerBlockStates")
    static void ac_registerBlockStates(Holder<PoiType> holder, Set<BlockState> states) {
        throw new AssertionError();
    }
}
