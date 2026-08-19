package com.github.alexmodguy.alexscaves.fabric.forge.common;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.Collection;

/**
 * Fabric stand-in for the two Forge hooks {@code GumWormEntity} calls out of its hand-written
 * {@code dropAllDeathLoot} — the only two this tree reaches on {@code ForgeHooks} rather than on
 * {@code ForgeEventFactory}.
 *
 * <p>The reasoning is the same one spelled out in
 * {@link com.github.alexmodguy.alexscaves.fabric.forge.event.ForgeEventFactory}: both fire an event
 * that no {@code @SubscribeEvent} handler in this mod listens to, so posting it on this loader's bus
 * would dispatch to nothing and return the default. What the default <i>is</i> was read out of
 * {@code forge-1.20.1-47.4.21-universal.jar} with {@code javap -c}.
 */
public final class ForgeHooks {

    private ForgeHooks() {
    }

    // Forge's own looting hook is gone from 1.21 — looting became an enchantment effect applied
    // through the loot context — and so is the vanilla helper this delegates to, so the method is
    // gated to the band where a caller exists. GumWormEntity's own local is gated the same way.
    /**
     * Forge's {@code LootingLevelEvent}. Its seed is the killer's own looting enchantment, and the
     * unlistened event hands that straight back.
     *
     * <p>Note that Forge's seed is <i>wider</i> than vanilla's: vanilla only consults the killer
     * when it is a {@code Player}, Forge whenever it is a {@code LivingEntity}. This reproduces
     * Forge's, since Forge's is what the caller sees on the other two loaders.
     */
    //? if <1.21 {
    public static int getLootingLevel(Entity target, Entity killer, DamageSource source) {
        return killer instanceof LivingEntity living
                ? net.minecraft.world.item.enchantment.EnchantmentHelper.getMobLooting(living)
                : 0;
    }
    //?}

    /**
     * Forge's {@code LivingDropsEvent}, whose return is "was the drop cancelled" — so {@code false}
     * lets the caller scatter the collected items itself.
     *
     * <p>Both arities are declared unconditionally rather than gated: 1.21 dropped the looting
     * parameter, and an {@code int} in that position cannot be confused with the {@code boolean}
     * that follows it, so the two overloads coexist and each version's call site simply picks one.
     */
    public static boolean onLivingDrops(LivingEntity entity, DamageSource source, Collection<ItemEntity> drops, int lootingLevel, boolean recentlyHit) {
        return false;
    }

    /** The 1.21-and-up shape of the method above. */
    public static boolean onLivingDrops(LivingEntity entity, DamageSource source, Collection<ItemEntity> drops, boolean recentlyHit) {
        return false;
    }
}
