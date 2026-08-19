package com.github.alexmodguy.alexscaves.server.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that burns whatever stands in it, the way fire and lava do.
 *
 * <p>{@code Block#isBurning} is a <b>loader patch</b> ({@code IForgeBlock}, NeoForge's
 * {@code IBlockExtension}) whose whole body on the loaders' side is
 * {@code this == Blocks.FIRE || this == Blocks.LAVA} — read out of the 1.20.1 universal jar. Vanilla
 * has no such question at all: the two blocks that burn do it from their own class, so a modded one
 * cannot join them. Both primal magmas want to, which is what makes a mob treat them as fire rather
 * than as floor.
 *
 * <p>Unconditional, and declared with the loaders' exact name and signature, for the reasons spelled
 * out on {@link ACPathTypeBlock}. Nothing in this mod asks the question, so there is no
 * {@code ACCompat} wrapper for it either.
 *
 * <p><b>⚠️ And nothing on the loaders' side asks it either — this is a dead extension point, so
 * Fabric deliberately ships NO dispatcher for it.</b> The natural assumption is that the loaders'
 * own {@code WalkNodeEvaluator} patch is the caller, since that is where the neighbouring
 * {@link ACPathTypeBlock} and {@link ACAdjacentPathTypeBlock} hooks are spliced in; it is not.
 * {@code WalkNodeEvaluator} calls vanilla's {@code isBurningBlock(BlockState)}, whose body is
 * byte-identical to vanilla's on every loader checked, and {@code isBurning} itself is referenced
 * <em>only</em> by the two extension interfaces that declare it. Census: Forge 1.20.1 (merged jar
 * <em>and</em> universal jar) and NeoForge 21.1.216 / 21.8.54 / 26.2.0.35-beta. So the nine bytes of
 * behaviour this interface describes have never run anywhere, and a Fabric mixin dispatching it
 * would <em>add</em> a difference rather than port one. The interface stays because the two magmas'
 * {@code @Override}s are upstream's and cost nothing; do not "finish" it.
 *
 * <p>General lesson, recorded because it nearly cost a wrong mixin: <b>a loader extension point may
 * have no caller at all.</b> Grep the patched jar for the hook's own name before writing a Fabric
 * dispatcher for it — the presence of an {@code IForgeBlock} default is not evidence that anything
 * invokes it.
 */
public interface ACBurningBlock {

    boolean isBurning(BlockState state, BlockGetter level, BlockPos pos);
}
