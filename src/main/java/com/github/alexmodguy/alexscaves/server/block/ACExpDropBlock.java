package com.github.alexmodguy.alexscaves.server.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that drops experience of its own when mined, without going through a loot table.
 *
 * <p>{@code Block#getExpDrop} is a <b>loader patch</b> ({@code IForgeBlock}, NeoForge's
 * {@code IBlockExtension}) whose default is a flat {@code 0}. Vanilla awards experience only from
 * the handful of ore classes that carry an {@code IntProvider} of their own, so a modded ore that is
 * not one of them silently drops none — which is what radrock uranium ore would do here.
 *
 * <p>Unconditional, and declared with the loaders' exact name and signature, for the reasons spelled
 * out on {@link ACPathTypeBlock}. Read through {@code ACCompat#getExpDrop}; a block that does not
 * implement it answers {@code 0}, which is the loaders' own default.
 *
 * <p>There are <b>two</b> readers, as with {@link ACPathTypeBlock}. This mod's own is
 * {@code MagneticWeaponEntity}, which mines a block itself and therefore pops the experience itself.
 * The other is a <em>player</em> breaking the block, which on Forge and NeoForge is the loader's
 * business — Forge computes it inside {@code BlockEvent$BreakEvent}'s constructor and pops it at the
 * tail of {@code ServerPlayerGameMode#destroyBlock} — and on Fabric is
 * {@code mixin.fabric.ServerPlayerGameModeMixin}, which re-creates that pop in the same branch.
 *
 * <p>The declaration is gated exactly as {@code RadrockUraniumOreBlock}'s override is: NeoForge
 * reshaped the hook at 1.21 into a form that takes the tool and the breaker instead of the two
 * enchantment levels, and there is nothing for this interface to say on that one band — Fabric never
 * reaches it, and on NeoForge the loader answers.
 */
public interface ACExpDropBlock {

    //? if !neoforge || <1.21 {
    int getExpDrop(BlockState state, LevelReader level, net.minecraft.util.RandomSource randomSource, BlockPos pos, int fortuneLevel, int silkTouchLevel);
    //?}
}
