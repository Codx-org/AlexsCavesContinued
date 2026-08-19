package com.github.alexmodguy.alexscaves.server.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.jetbrains.annotations.Nullable;

/**
 * A block that tells a pathfinder what standing <i>on</i> it costs.
 *
 * <p>{@code Block#getBlockPathType} is a <b>loader patch</b> ({@code IForgeBlock}, NeoForge's
 * {@code IBlockExtension}), not vanilla. Vanilla decides a node's {@code BlockPathTypes} from a
 * fixed cascade of {@code instanceof} tests and block tags inside {@code WalkNodeEvaluator}, with no
 * hook a block can answer — so a modded block that is not one of the shapes vanilla knows about is
 * simply plain ground to every mob. The two implementors want otherwise: hazmat blocks are
 * {@code UNPASSABLE_RAIL} so nothing walks the reactor casing, and a gingerbread door reports the
 * door types its vanilla counterpart would.
 *
 * <p>The interface is <b>unconditional</b> and declares the method with the loaders' exact name and
 * signature. On Forge and NeoForge the inherited patch already satisfies it, so implementing it
 * costs those loaders nothing and the existing overrides keep meaning what they always did; on
 * Fabric the very same overrides are satisfied by this declaration instead. Same trick as
 * {@code ACUpdatePacketReceiver} and {@code ACTickingItem}, and the reason no block needed a gate.
 *
 * <p><b>⚠️ "Unconditional" is only ever a claim about ONE band of the patch being mirrored</b> — see
 * {@code ACUpdatePacketReceiver}, which declared its method unconditionally and broke every node
 * ≥1.20.5 the moment Fabric entered the picture, because the {@code replacements.string} rules that
 * rewrite the nine implementations are anchored on a {@code public … ) {} declaration an interface
 * method can never match. This hook has held its shape so far, and the one rename it has faced is
 * covered: {@code BlockPathTypes} became {@code PathType} at <b>1.20.5</b> (not 1.21.2, as this note
 * claimed until the Fabric 1.20.5 node was ported), and {@code !mc205-pathtype-enum} is a bare-token
 * rule, so it rewrites this declaration exactly as it rewrites the implementors'. What that rule
 * does <em>not</em> cover is the vanilla method the loaders splice into — {@code getBlockPathTypeRaw}
 * was renamed {@code getPathTypeFromState} in the same version with no rule of its own, and
 * {@code checkNeighbourBlocks} was reshaped around a {@code PathfindingContext}; both are gated by
 * hand in {@code mixin.fabric.WalkNodeEvaluatorMixin}.
 *
 * <p>There are <b>two</b> readers, and Fabric needs a stand-in for each. This mod's own read goes
 * through {@code ACCompat#getBlockPathType}, which asks the loader on Forge/NeoForge and this
 * interface on Fabric — one caller, Citadel's raycoms {@code AbstractPathJob}. Vanilla's <em>own</em>
 * pathfinder is the other, and there the loaders splice the call into
 * {@code WalkNodeEvaluator#getBlockPathTypeRaw} ahead of their cascade; {@code
 * mixin.fabric.WalkNodeEvaluatorMixin} is Fabric's copy of that splice, at the same instruction.
 *
 * <p>A block that does not implement it answers {@code null}, meaning "no opinion" — which is what
 * the loaders' own default effectively says for anything that is not lava or on fire, and both of
 * those vanilla already handles for itself.
 */
public interface ACPathTypeBlock {

    BlockPathTypes getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob);
}
