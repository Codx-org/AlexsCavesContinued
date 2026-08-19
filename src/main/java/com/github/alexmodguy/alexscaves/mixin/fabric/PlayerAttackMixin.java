package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.server.item.ACSwingControlItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric's dispatcher for the first half of {@link ACSwingControlItem} — the veto an item holds over
 * its own attack.
 *
 * <p>{@code Item#onLeftClickEntity} is a <b>loader patch</b> with no vanilla counterpart, and on Forge
 * it is not called from {@code Player#attack} at all: the method's first act is
 * {@code if (!ForgeHooks.onPlayerAttackTarget(this, target)) return;}, and that hook posts
 * {@code AttackEntityEvent} and then, if the event survived, asks the main-hand item. Read out of the
 * 1.20.1 universal jar it is
 *
 * <pre>
 * ItemStack main = player.getMainHandItem();
 * return main.isEmpty() || !main.getItem().onLeftClickEntity(main, player, target);
 * </pre>
 *
 * <p>So the patch is a guard spliced at offset 0 of {@code attack}, ahead of vanilla's own
 * {@code isAttackable} test — the two disassemblies are identical from there on — and a cancellable
 * {@code @At("HEAD")} inject reproduces it exactly. Only the item side is reproduced; the event has no
 * Fabric counterpart and this mod never listens to it.
 *
 * <p>⚠️ Written against 1.20.1. {@code Player#attack} keeps its name and shape across the range, but
 * re-derive the guard's position from each new Fabric node's own bytecode rather than assuming it.
 */
@Mixin(Player.class)
public class PlayerAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void ac_onLeftClickEntity(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        ItemStack main = player.getMainHandItem();
        if (!main.isEmpty() && main.getItem() instanceof ACSwingControlItem typed
                && typed.onLeftClickEntity(main, player, target)) {
            ci.cancel();
        }
    }
}
