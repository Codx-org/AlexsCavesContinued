package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.server.item.ACArmorTickItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric's dispatcher for {@link ACArmorTickItem} — the per-tick call a worn piece of armour gets.
 *
 * <p>{@code Item#onArmorTick} is a <b>loader patch</b> with an empty default and no vanilla
 * counterpart, and it exists only below 1.21; from there both implementors switch to
 * {@code ACTickingItem} with a slot guard, which is why the injection below is gated to the same band
 * as the interface's own method and this mixin is simply empty above it.
 *
 * <p><b>Why this does not reproduce the loaders' anchor literally.</b> Forge does not call
 * {@code onArmorTick} from anywhere in vanilla code. It rewrites {@code Inventory#tick} to walk the
 * compartments with one <em>flat</em> running index and call its own
 * {@code ItemStack#onInventoryTick(Level, Player, int, int)}; the default body of that, read out of
 * the 1.20.1 universal jar, is nothing but the arithmetic that turns the flat index back into a
 * per-compartment one —
 *
 * <pre>
 * if (slot &gt;= inv.items.size()) {
 *     slot -= inv.items.size();
 *     if (slot &gt;= inv.armor.size()) slot -= inv.armor.size();
 *     else onArmorTick(stack, level, player);
 * }
 * stack.inventoryTick(level, player, slot, selected == slot);
 * </pre>
 *
 * — i.e. the hook fires once per tick for every non-empty stack in the armour compartment, and the
 * re-indexing exists only to hand vanilla back the arguments it already had. Walking
 * {@code inventory.armor} at the head of the same method says exactly that and survives a version
 * bump, where a {@code @Local} on the flat index would not. Ordering against the vanilla
 * {@code inventoryTick} calls is immaterial: neither implementor overrides that on this band.
 */
@Mixin(Inventory.class)
public class InventoryArmorTickMixin {

    //? if <1.21 {
    @Inject(method = "tick", at = @At("HEAD"))
    private void ac_onArmorTick(CallbackInfo ci) {
        Inventory self = (Inventory) (Object) this;
        for (ItemStack stack : self.armor) {
            if (!stack.isEmpty() && stack.getItem() instanceof ACArmorTickItem typed) {
                typed.onArmorTick(stack, self.player.level(), self.player);
            }
        }
    }
    //?}
}
