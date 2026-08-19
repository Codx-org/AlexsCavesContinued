package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric's dispatcher for {@link com.github.alexmodguy.alexscaves.server.block.ACExpDropBlock} —
 * the experience a block drops when a player breaks it.
 *
 * <p>{@code Block#getExpDrop} is a <b>loader patch</b> ({@code IForgeBlock}, NeoForge's
 * {@code IBlockExtension}), and its one implementor here is {@code RadrockUraniumOreBlock}, which
 * {@code extends Block} rather than {@code DropExperienceBlock} — so vanilla awards it nothing at all
 * and without this mixin radrock uranium ore is silently XP-free on Fabric.
 *
 * <p><b>Where the loaders actually call it, which is not where it looks.</b> Nothing in
 * {@code ServerPlayerGameMode#destroyBlock} invokes {@code getExpDrop} on Forge: the call is two
 * frames away, in {@code BlockEvent$BreakEvent}'s constructor, which
 * {@code ForgeHooks.onBlockBreakEvent} builds at the very top of {@code destroyBlock} and whose
 * {@code exp} field {@code destroyBlock} then pops at its tail. Read out of the 1.20.1 universal jar,
 * the constructor is
 *
 * <pre>
 * if (state == null || !ForgeHooks.isCorrectToolForDrops(state, player)) exp = 0;
 * else exp = state.getExpDrop(level, level.random, pos,
 *                             player.getMainHandItem().getEnchantmentLevel(BLOCK_FORTUNE),
 *                             player.getMainHandItem().getEnchantmentLevel(SILK_TOUCH));
 * </pre>
 *
 * and the pop is {@code if (flag && exp > 0) state.getBlock().popExperience(level, pos, exp)}, where
 * {@code flag} is the {@code removeBlock} result.
 *
 * <p><b>Why the anchor is vanilla's {@code Block#playerDestroy} call rather than TAIL.</b> Forge's
 * guard reads as {@code flag &&} (correct tool, folded into {@code exp}); vanilla's own
 * {@code playerDestroy} branch is guarded by {@code flag && flag1}, and {@code flag1} is
 * {@code player.hasCorrectToolForDrops(state)} — the same question {@code isCorrectToolForDrops}
 * asks, modulo Forge's harvest-check event. So injecting immediately after that call reproduces
 * Forge's condition exactly, inside the branch vanilla already computed, and needs no {@code @Local}
 * on a <em>boolean</em>: the merged Mojmap jars carry no {@code LocalVariableTable}, so booleans and
 * ints are indistinguishable in the StackMapTable frames MixinExtras infers from, and an ordinal over
 * them would be a guess. Creative mode returns before this point on both sides.
 *
 * <p>The tool read for the two enchantment levels is {@code itemstack1} — the <i>copy</i> taken
 * before {@code ItemStack#mineBlock} (local ordinal 1, slot 7 in 1.20.1), not the live main-hand
 * stack. Forge's is the main-hand stack as it was at the top of the method, i.e. before
 * {@code mineBlock} could break it, so the copy is the faithful reading rather than a convenient one.
 *
 * <p>⚠️ Written against 1.20.1. {@code Block#playerDestroy} and the shape of {@code destroyBlock}
 * both move across the range, so re-derive the {@code @At} target from each new Fabric node's own
 * bytecode rather than assuming this selector survives.
 */
@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {

    @Shadow
    protected ServerLevel level;

    @Inject(
            method = "destroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;playerDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void ac_popExpDrop(BlockPos pos, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 0) BlockState state, @Local(ordinal = 1) ItemStack tool) {
        int fortuneLevel = ACCompat.enchantLevel(tool, Enchantments.BLOCK_FORTUNE);
        int silkTouchLevel = ACCompat.enchantLevel(tool, Enchantments.SILK_TOUCH);
        // getRandom() rather than the `random` field: MC 26 made that field protected, and a mixin
        // reads it through the target's own class, not through an accessor, so the narrowing is a
        // compile error here. The getter has been public on Level across the whole 1.20.1->26.x range
        // and returns the same instance, so this needs no gate.
        int exp = ACCompat.getExpDrop(state, this.level, this.level.getRandom(), pos, fortuneLevel, silkTouchLevel);
        if (exp > 0) {
            state.getBlock().popExperience(this.level, pos, exp);
        }
    }
}
