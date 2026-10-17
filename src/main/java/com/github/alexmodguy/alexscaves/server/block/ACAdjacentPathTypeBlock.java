package com.github.alexmodguy.alexscaves.server.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.jetbrains.annotations.Nullable;

/**
 * A block that tells a pathfinder what standing <i>next to</i> it costs — the danger halo vanilla
 * gives fire and cactus, extended to this mod's own hazards.
 *
 * <p>The sibling of {@link ACPathTypeBlock}, and a loader patch for the same reason: vanilla's
 * {@code WalkNodeEvaluator} works the neighbourhood out from a fixed list of blocks, so primal magma
 * would read as safe ground to walk beside and hazmat casing as ordinary wall. Both magmas answer
 * {@code DANGER_FIRE} and hazmat {@code UNPASSABLE_RAIL}.
 *
 * <p>Unconditional, and declared with the loaders' exact name and signature, for the reasons spelled
 * out on {@link ACPathTypeBlock} — including the ⚠️ there about re-checking that claim per band.
 * Nothing in this mod <i>asks</i> the question — the loaders' own {@code WalkNodeEvaluator} patch is
 * the only caller, verified in the disassembly rather than assumed — so there is no
 * {@code ACCompat} wrapper for it and Fabric's dispatcher is
 * {@code mixin.fabric.WalkNodeEvaluatorMixin}, which re-creates that call inside the same 3×3×3
 * neighbour loop at the same instruction offset. A block that does not implement it answers
 * {@code null}, i.e. "leave the original type alone", which is what the loaders' own default says for
 * everything but a sweet berry bush or a burning block — both of which vanilla already handles for
 * itself.
 */
public interface ACAdjacentPathTypeBlock {

    BlockPathTypes getAdjacentBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob, BlockPathTypes originalType);
}
