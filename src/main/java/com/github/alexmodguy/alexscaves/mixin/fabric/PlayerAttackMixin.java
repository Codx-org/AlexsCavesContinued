package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.player.AttackEntityEvent;
import com.github.alexmodguy.alexscaves.server.item.ACSwingControlItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric's dispatcher for {@code Player#attack}'s two Forge-side vetoes: {@link AttackEntityEvent}
 * and {@link ACSwingControlItem}, in that order.
 *
 * <p>{@code Item#onLeftClickEntity} is a <b>loader patch</b> with no vanilla counterpart, and on Forge
 * it is not called from {@code Player#attack} at all: the method's first act is
 * {@code if (!ForgeHooks.onPlayerAttackTarget(this, target)) return;}, and that hook posts
 * {@code AttackEntityEvent} and then, if the event survived, asks the main-hand item. Read out of the
 * 1.20.1 universal jar it is
 *
 * <pre>
 * if (MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(player, target))) return false;
 * ItemStack main = player.getMainHandItem();
 * return main.isEmpty() || !main.getItem().onLeftClickEntity(main, player, target);
 * </pre>
 *
 * <p>So the patch is a guard spliced at offset 0 of {@code attack}, ahead of vanilla's own
 * {@code isAttackable} test — the two disassemblies are identical from there on — and a cancellable
 * {@code @At("HEAD")} inject reproduces it exactly. <b>The event goes first</b>, and that ordering is
 * observable: an item that vetoes the swing would otherwise pre-empt a listener that wants to see the
 * attempt.
 *
 * <p>Cancelling here rather than from a damage hook is deliberate and is what the event's own javadoc
 * asks for — a {@code LivingAttackEvent} cancellation stops the damage but still swings the arm.
 *
 * <p>⚠️ Written against 1.20.1. {@code Player#attack} keeps its name and shape across the range, but
 * re-derive the guard's position from each new Fabric node's own bytecode rather than assuming it.
 */
@Mixin(Player.class)
public class PlayerAttackMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void ac_onLeftClickEntity(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (MinecraftForge.EVENT_BUS.post(new AttackEntityEvent(player, target))) {
            ci.cancel();
            return;
        }
        ItemStack main = player.getMainHandItem();
        if (!main.isEmpty() && main.getItem() instanceof ACSwingControlItem typed
                && typed.onLeftClickEntity(main, player, target)) {
            ci.cancel();
        }
    }
}
