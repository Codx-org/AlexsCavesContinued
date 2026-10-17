package com.github.alexmodguy.alexscaves.fabric.forge.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

/**
 * Fabric stand-in for Forge's factory of <i>outbound</i> game events — the eight it fires from this
 * mod's own code, and no others.
 *
 * <p><b>Why every method here answers the "no listener" default, and why that is exact rather than a
 * compromise.</b> Everything in this class is a place where the mod <i>publishes</i> a Forge event
 * so that some <i>other</i> mod may steer it: veto a projectile's impact, rewrite the block lava
 * turns into, adjust a break speed. This tree's own bus does exist on Fabric — see
 * {@link com.github.alexmodguy.alexscaves.fabric.event.ACEventBus} — but the eight events below have
 * no listener anywhere in it: grepping every {@code @SubscribeEvent} handler in this mod finds
 * {@code MobSpawnEvent.FinalizeSpawn}, never its sibling {@code PositionCheck}, and none of
 * {@code ProjectileImpactEvent}, {@code LivingDestroyBlockEvent}, {@code BlockEvent.FluidPlaceBlockEvent},
 * {@code PlayerEvent.BreakSpeed}, {@code PlayerEvent.ItemSmeltedEvent},
 * {@code PlayerDestroyItemEvent} or {@code LivingConversionEvent.Post} at all. So posting them here
 * would dispatch to an empty list and then take the very branch each method takes below. Answering
 * the default directly is the same behaviour with none of the machinery, and it is the reason no
 * event class had to be vendored to support this file.
 *
 * <p>A third-party Fabric mod therefore cannot intercept these eight moments. That is a real
 * difference from Forge and it is unavoidable in kind: those interception points are Forge's API,
 * and a Fabric mod would not be looking for them here.
 *
 * <p>Each default was read out of {@code forge-1.20.1-47.4.21-universal.jar} with {@code javap -c}
 * rather than recalled — the branch a Forge method takes when {@code post} returns false is the only
 * thing this class reproduces, so it is the only thing worth being exact about.
 */
public final class ForgeEventFactory {

    private ForgeEventFactory() {
    }

    /**
     * Forge's {@code MobSpawnEvent.PositionCheck}. With the result left at {@code DEFAULT} — which
     * is what an unlistened event returns — Forge answers vanilla's own pair of spawn tests, so
     * that pair is what this returns.
     */
    public static boolean checkSpawnPosition(Mob mob, ServerLevelAccessor level, MobSpawnType spawnType) {
        return mob.checkSpawnRules(level, spawnType) && mob.checkSpawnObstruction(level);
    }

    /**
     * Forge's {@code PlayerDestroyItemEvent}, which is purely a notification — it carries no result
     * and Forge ignores the return of {@code post}. Nothing to do on this loader.
     */
    public static void onPlayerDestroyItem(Player player, ItemStack original, InteractionHand hand) {
    }

    /**
     * Forge's {@code LivingConversionEvent.Post}, fired after one mob has already been replaced by
     * another. Notification only, exactly like the one above.
     */
    public static void onLivingConvert(LivingEntity before, LivingEntity after) {
    }

    /**
     * Forge's {@code ProjectileImpactEvent}, whose return is "was the impact cancelled". Both call
     * sites read it as a veto, so {@code false} is "carry on and hit the thing".
     */
    public static boolean onProjectileImpact(Projectile projectile, HitResult ray) {
        return false;
    }

    /**
     * Forge's {@code LivingDestroyBlockEvent}. The polarity is the opposite of the one above —
     * Forge returns {@code !event.isCanceled()}, so an unlistened event means "yes, break it".
     */
    public static boolean onEntityDestroyBlock(LivingEntity entity, BlockPos pos, BlockState state) {
        return true;
    }

    /**
     * Forge's {@code BlockEvent.FluidPlaceBlockEvent}, which exists so a mod can change what lava
     * meeting water turns into. Its return is the event's new state, which starts out as the state
     * handed in — so this is the identity.
     */
    public static BlockState fireFluidPlaceBlockEvent(LevelAccessor level, BlockPos pos, BlockPos liquidPos, BlockState state) {
        return state;
    }

    /**
     * Forge's {@code PlayerEvent.ItemSmeltedEvent}. Notification only; the vanilla awarding it
     * accompanies has already happened at the call site.
     */
    public static void firePlayerSmeltedEvent(Player player, ItemStack smelted) {
    }

    /**
     * Forge's {@code PlayerEvent.BreakSpeed}. Its return is the event's speed, which begins as the
     * one passed in — and {@code -1} only when a listener cancels, which none can here.
     */
    public static float getBreakSpeed(Player player, BlockState state, float original, BlockPos pos) {
        return original;
    }
}
