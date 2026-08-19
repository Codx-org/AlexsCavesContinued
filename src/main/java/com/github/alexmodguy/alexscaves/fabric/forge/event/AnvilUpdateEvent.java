package com.github.alexmodguy.alexscaves.fabric.forge.event;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Cancelable;
import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric stand-in for the anvil's "what would these two inputs produce" hook.
 *
 * <p>One handler, and it exists to <b>add</b> a recipe rather than veto one: two of the same
 * {@code AlwaysCombinableOnAnvil} item always merge, where vanilla rejects a same-item pair with
 * nothing to repair. It re-implements vanilla's enchantment merge, so it sets both the output and
 * the cost.
 *
 * <p>Setting an output is what marks the event as answered; {@link #getOutput()} being non-empty is
 * the signal the dispatcher acts on, exactly as the loader's own anvil menu does. The event is
 * cancellable — a cancelled one means "no result at all", which nothing in this tree does, but the
 * shape has to match or a handler that refuses a combine would silently produce one.
 */
@Cancelable
public class AnvilUpdateEvent extends Event {

    private final ItemStack left;
    private final ItemStack right;
    private final String name;
    private final Player player;
    private ItemStack output = ItemStack.EMPTY;
    private int cost;
    private int materialCost;

    public AnvilUpdateEvent(ItemStack left, ItemStack right, String name, int cost, Player player) {
        this.left = left;
        this.right = right;
        this.name = name;
        this.cost = cost;
        this.player = player;
    }

    public ItemStack getLeft() {
        return left;
    }

    public ItemStack getRight() {
        return right;
    }

    /** The text in the anvil's rename box, or null when the player has typed nothing. */
    public String getName() {
        return name;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getOutput() {
        return output;
    }

    public void setOutput(ItemStack output) {
        this.output = output;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    /** How many of the right-hand stack are consumed. Left at 0 by this mod's only handler. */
    public int getMaterialCost() {
        return materialCost;
    }

    public void setMaterialCost(int materialCost) {
        this.materialCost = materialCost;
    }
}
