package com.github.alexmodguy.alexscaves.mixin.fabric.client;

import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.player.PlayerInteractEvent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric's client-side producer for {@code PlayerInteractEvent.RightClickItem}.
 * {@code ServerPlayerUseItemMixin} is the authoritative half; this one exists because Forge fires the
 * event on both sides and the mod's behaviour was written against that — without it the purple-soda
 * bottle would appear only after the server's inventory packet arrives, i.e. with a visible delay
 * that Forge and NeoForge players do not see.
 *
 * <p><b>Why the anchor is {@code ensureHasSentCarriedItem} and not Forge's own site.</b> Forge splices
 * {@code onItemRightClick} into the {@code PredictiveAction} lambda that {@code useItem} hands to
 * {@code startPrediction} — a synthetic whose name is {@code method_41929} on loom-mapped Forge and
 * {@code lambda$useItem$N} elsewhere, with an index that moves between versions. That is exactly the
 * class of selector this tree has been bitten by before, so the injection goes on the enclosing
 * method instead, immediately after the {@code ensureHasSentCarriedItem()V} call. Ordering is
 * unaffected: {@code startPrediction} invokes the action synchronously, so nothing runs between the
 * two positions, and both sit after the spectator guard. {@code ensureHasSentCarriedItem()V} is
 * byte-identical and in the same position on all 22 Fabric nodes.
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeUseItemMixin {

    @Inject(
            method = "useItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;ensureHasSentCarriedItem()V", shift = At.Shift.AFTER),
            cancellable = true
    )
    private void ac_rightClickItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        PlayerInteractEvent.RightClickItem event = new PlayerInteractEvent.RightClickItem(player, hand);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            cir.setReturnValue(event.getCancellationResult());
        }
    }
}
