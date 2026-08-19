package com.github.alexmodguy.alexscaves.server.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * An item that decides for itself when a swing lands and when the arm animation plays.
 *
 * <p>Both methods are <b>loader patches</b> ({@code IForgeItem}, NeoForge's {@code IItemExtension}),
 * not vanilla. {@code onLeftClickEntity} runs before the attack resolves and cancels it by returning
 * {@code true}; {@code onEntitySwing} runs before the swing animation starts and cancels that the
 * same way. Vanilla offers neither hook, so the primitive club — whose whole character is that it
 * cannot be spammed and must be wound up — has nothing to hang itself on without them.
 *
 * <p>Unconditional, and declared with the loaders' exact names and signatures, for the reasons
 * spelled out on {@code ACUpdatePacketReceiver}: on Forge and NeoForge the inherited patch already
 * satisfies both, so the existing {@code @Override}s keep meaning what they always did, and on Fabric
 * the very same {@code @Override}s are satisfied by these declarations instead.
 *
 * <p>Both default to {@code false} — "no opinion, carry on" — which is the loaders' own default, and
 * on Fabric that is what an item not implementing this interface answers.
 */
public interface ACSwingControlItem {

    boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity);

    boolean onEntitySwing(ItemStack stack, LivingEntity entity);
}
