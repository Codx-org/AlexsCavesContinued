package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import com.github.alexmodguy.alexscaves.fabric.forge.event.AnvilUpdateEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric's producer for {@code AnvilUpdateEvent} — the event {@code CommonEvents#onUpdateAnvil} uses
 * to let two {@code AlwaysCombinableOnAnvil} items (the three gauntlets) merge their enchantments,
 * which vanilla refuses because a same-item pair has nothing to repair.
 *
 * <p><b>Where Forge fires it, and why HEAD is the same place.</b> Forge patches one call into
 * {@code AnvilMenu#createResult}: disassembled on {@code 1.20.1-forge}, {@code ForgeHooks
 * .onAnvilChange(AnvilMenu, ItemStack, ItemStack, Container, String, int, Player)Z} sits at offset
 * 130 under {@code if (!…) return;}, i.e. after the empty-left guard and after the base-repair-cost
 * sum, and everything vanilla has done up to that point is either read-only or is re-done from
 * scratch when the method runs again. So HEAD plus an explicit empty-left guard reproduces the
 * position without depending on a bytecode offset that moves between versions — {@code createResult()V}
 * is identical on all 22 Fabric nodes, so this needs no gate.
 *
 * <p><b>The incoming cost is passed as 0, deliberately.</b> Forge hands the hook its running
 * {@code j} (the two stacks' base repair costs summed), a local this injection cannot see at HEAD.
 * That value is only ever a <i>starting</i> cost for a handler that wants to add to it; this tree's
 * one handler ignores it and calls {@code setCost(i)} with an absolute figure. ⚠️ A future handler
 * that did {@code setCost(event.getCost() + n)} would undercharge on Fabric — it would need the
 * injection moved to the {@code repairItemCountCost = 0} store and {@code j} captured with an
 * {@code @Local}, which costs a three-band gate and buys nothing today.
 *
 * <p>The write-back mirrors {@code ForgeHooks.onAnvilChange} exactly: a cancelled event clears the
 * result and zeroes the cost, a non-empty output is installed with its cost and material cost, and
 * either way vanilla's own computation is skipped. Neither branch broadcasts — Forge does not
 * either; the menu's own tick sends the change.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuUpdateMixin {

    @Shadow
    private String itemName;

    @Shadow
    private int repairItemCountCost;

    @Shadow
    @org.spongepowered.asm.mixin.Final
    private DataSlot cost;

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void ac_createResult(CallbackInfo ci) {
        ItemCombinerMenuAccessor self = (ItemCombinerMenuAccessor) this;
        ItemStack left = self.ac_getInputSlots().getItem(0);
        if (left.isEmpty()) {
            return;
        }
        ItemStack right = self.ac_getInputSlots().getItem(1);
        AnvilUpdateEvent event = new AnvilUpdateEvent(left, right, itemName, 0, self.ac_getPlayer());
        if (MinecraftForge.EVENT_BUS.post(event)) {
            self.ac_getResultSlots().setItem(0, ItemStack.EMPTY);
            cost.set(0);
            ci.cancel();
            return;
        }
        if (!event.getOutput().isEmpty()) {
            self.ac_getResultSlots().setItem(0, event.getOutput());
            cost.set(event.getCost());
            repairItemCountCost = event.getMaterialCost();
            ci.cancel();
        }
    }
}
