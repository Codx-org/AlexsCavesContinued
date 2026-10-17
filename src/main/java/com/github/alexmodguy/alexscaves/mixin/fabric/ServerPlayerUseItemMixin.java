package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.player.PlayerInteractEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric's server-side producer for {@code PlayerInteractEvent.RightClickItem} — the event that
 * turns a glass bottle held over purple soda into a bottle of purple soda
 * ({@code CommonEvents#onRightClickItem}).
 *
 * <p><b>Where Forge puts it.</b> {@code ForgeHooks.onItemRightClick} is spliced into
 * {@code ServerPlayerGameMode#useItem} at bytecode offset 35 on 1.20.1 — <i>after</i> the spectator
 * and item-cooldown guards and <i>before</i> the {@code ItemStack#getCount} snapshot that precedes
 * {@code ItemStack#use}. Anchoring on that {@code getCount} call reproduces the position exactly and
 * is the one selector that survives the whole range: the cooldown test above it is
 * {@code isOnCooldown(Item)} below 1.21.11 and {@code isOnCooldown(ItemStack)} from it, and
 * {@code ItemStack#use} below it returns {@code InteractionResultHolder} up to 1.21.1 and
 * {@code InteractionResult} after — while {@code ItemStack#getCount()I} is byte-identical on all 22
 * Fabric nodes, and this is its first occurrence in the method on every one of them.
 *
 * <p>{@code MultiPlayerGameModeUseItemMixin} is the client half; Forge fires this event on both
 * sides, and the mod's handler is written for that (its sound is played through
 * {@code Level#playSound(Player, …)}, which skips the local player, and its inventory changes are the
 * ordinary client-side prediction the server then confirms).
 *
 * <p>The cancellation path is honoured for shape-fidelity only — no listener in this mod cancels a
 * {@code RightClickItem}.
 */
@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerUseItemMixin {

    @Inject(
            method = "useItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getCount()I", ordinal = 0),
            cancellable = true
    )
    private void ac_rightClickItem(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        PlayerInteractEvent.RightClickItem event = new PlayerInteractEvent.RightClickItem(player, hand);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            cir.setReturnValue(event.getCancellationResult());
        }
    }
}
