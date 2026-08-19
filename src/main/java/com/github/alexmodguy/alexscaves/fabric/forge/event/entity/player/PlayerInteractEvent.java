package com.github.alexmodguy.alexscaves.fabric.forge.event.entity.player;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Cancelable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Fabric stand-in for the right-click family. Two shapes are used: {@link EntityInteract} (binding a
 * holocoder to the clicked entity) and {@link RightClickItem} (the cave map and the tablet).
 *
 * <p>Cancelling here means "this mod handled the click"; the {@link #setCancellationResult
 * cancellation result} is what the vanilla call site returns in that case, so a cancelled event with
 * {@code SUCCESS} swings the arm and a cancelled event with {@code PASS} does not. Both halves have
 * to reach the dispatcher's call site or an item that works on Forge would silently fall through to
 * vanilla here.
 */
@Cancelable
public class PlayerInteractEvent extends PlayerEvent {

    private final InteractionHand hand;
    private final ItemStack itemStack;
    private InteractionResult cancellationResult = InteractionResult.PASS;

    public PlayerInteractEvent(Player player, InteractionHand hand, ItemStack itemStack) {
        super(player);
        this.hand = hand;
        this.itemStack = itemStack;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public Level getLevel() {
        return getEntity().level();
    }

    public InteractionResult getCancellationResult() {
        return cancellationResult;
    }

    public void setCancellationResult(InteractionResult cancellationResult) {
        this.cancellationResult = cancellationResult;
    }

    /** The player right-clicked another entity. */
    public static class EntityInteract extends PlayerInteractEvent {

        private final Entity target;

        public EntityInteract(Player player, InteractionHand hand, Entity target) {
            super(player, hand, player.getItemInHand(hand));
            this.target = target;
        }

        public Entity getTarget() {
            return target;
        }
    }

    /** The player right-clicked with an item, hitting neither a block nor an entity. */
    public static class RightClickItem extends PlayerInteractEvent {

        public RightClickItem(Player player, InteractionHand hand) {
            super(player, hand, player.getItemInHand(hand));
        }
    }
}
