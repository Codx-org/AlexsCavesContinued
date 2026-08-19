package com.github.alexmodguy.alexscaves.server.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mod-side counterpart of Forge's {@code IForgeBlock#onCaughtFire}, on the one-interface-per-hook
 * convention this tree uses throughout.
 *
 * <p>Both loaders patch {@code BlockState} with {@code onCaughtFire}, whose default body is
 * <em>empty</em> — everything it does for vanilla lives in {@code TntBlock}'s override, and vanilla
 * itself has no such method at all. So on Fabric the dispatch has to be written out: the mod's own
 * ignitable blocks answer through this interface, and plain {@code TntBlock} is special-cased in
 * {@code ACCompat#onCaughtFire} because its behaviour is the loader patch's, not vanilla's.
 *
 * <p>The single implementor is {@link NuclearBombBlock}; the single call site is the remote
 * detonator, which walks {@code ACTagRegistry.REMOTE_DETONATOR_ACTIVATES} (vanilla TNT + the
 * nuclear bomb).
 *
 * <p>The return type follows the loaders: 1.21.5 made the hook report whether the block actually
 * caught, so a TNT-like block can refuse. The gate is on the <em>method</em>, never on an
 * implementor's {@code implements} clause — a {@code default} body here would collide with the
 * {@code IForgeBlock} default on the loaders that have one.
 */
public interface ACIgnitableBlock {

    //? if >=1.21.5
    /*boolean onCaughtFire(BlockState state, Level level, BlockPos pos, Direction face, LivingEntity igniter);*/
    //? if <1.21.5
    void onCaughtFire(BlockState state, Level level, BlockPos pos, Direction face, LivingEntity igniter);
}
