package com.github.alexmodguy.alexscaves.fabric.forge.fluids;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric stand-in for the loader's "what happens where two fluids meet" table.
 *
 * <p>Vanilla hardcodes the one case it cares about — water beside lava makes stone, cobblestone or
 * obsidian, written inline in {@code LiquidBlock} and {@code FlowingFluid#spreadTo} — and the loader
 * generalises it into a registry keyed by fluid type. {@code ACFluidRegistry#postInit} fills it with
 * ten entries: acid and purple soda each meeting water, lava and each other.
 *
 * <p>The table is reproduced verbatim, including its asymmetry: the mod registers both directions of
 * every pair explicitly (acid-meets-water <i>and</i> water-meets-acid), because the loader looks up
 * only the type of the fluid that is spreading. Collapsing that into one symmetric entry would be a
 * behaviour change disguised as a simplification — the two directions are free to name different
 * blocks, and nothing here should decide that they never will.
 *
 * <p>⚠️ <b>The dispatch is not wired.</b> On the other two loaders the loader's own patch inside
 * {@code FlowingFluid#spreadTo} consults this table before a fluid spreads into a block; here that
 * call site is {@code mixin.FlowingFluidMixin#ac_spreadTo}, which already exists and already injects
 * at the head of the same method for an unrelated reason. Routing it through {@link #result} is
 * dispatcher work — until then acid and purple soda flow into each other and into water without
 * reacting, and this class is a table that is filled and never read.
 */
public final class FluidInteractionRegistry {

    private static final Map<FluidType, List<InteractionInformation>> INTERACTIONS = new ConcurrentHashMap<>();

    private FluidInteractionRegistry() {
    }

    /** The block a fluid of {@code type} turns into where it meets one of {@code other}. */
    @FunctionalInterface
    public interface BlockStateFunction {
        BlockState getBlockState(FluidState state);
    }

    public record InteractionInformation(FluidType type, BlockStateFunction interaction) {
    }

    public static void addInteraction(FluidType type, InteractionInformation information) {
        INTERACTIONS.computeIfAbsent(type, key -> Collections.synchronizedList(new ArrayList<>())).add(information);
    }

    public static List<InteractionInformation> getInteractions(FluidType type) {
        return INTERACTIONS.getOrDefault(type, List.of());
    }

    /**
     * The block a fluid of {@code source} becomes where it meets one of {@code neighbour}, or null
     * when the pair has no registered interaction. First match wins, which is the loader's own rule.
     */
    @Nullable
    public static BlockState result(@Nullable FluidType source, @Nullable FluidType neighbour, FluidState state) {
        if (source == null || neighbour == null) {
            return null;
        }
        for (InteractionInformation information : getInteractions(source)) {
            if (information.type() == neighbour) {
                return information.interaction().getBlockState(state);
            }
        }
        return null;
    }
}
