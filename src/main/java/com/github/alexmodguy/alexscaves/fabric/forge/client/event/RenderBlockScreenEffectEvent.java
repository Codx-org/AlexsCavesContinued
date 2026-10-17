package com.github.alexmodguy.alexscaves.fabric.forge.client.event;

import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.player.PlayerEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Cancelable;
import net.minecraft.world.entity.player.Player;

/**
 * Fabric stand-in for the full-screen overlay vanilla draws when the camera is inside fire, water or
 * a block.
 *
 * <p>Received only, and only to cancel: this mod's own fluids draw their own overlay, and the
 * possessed-player camera must not inherit the host's. The dispatcher fires it from the mixin that
 * wraps vanilla's overlay draw and skips the draw when cancelled.
 *
 * <p>{@link OverlayType} is reproduced rather than mapped onto anything of Fabric's, because it
 * names the three overlays vanilla itself distinguishes and there is no loader-neutral enum for
 * them. Only {@code WATER} is compared against in this tree; the other two exist so a comparison
 * against them stays possible without a second edit here.
 */
@Cancelable
public class RenderBlockScreenEffectEvent extends PlayerEvent {

    public enum OverlayType {
        FIRE,
        BLOCK,
        WATER,
    }

    private final OverlayType overlayType;

    public RenderBlockScreenEffectEvent(Player player, OverlayType overlayType) {
        super(player);
        this.overlayType = overlayType;
    }

    public Player getPlayer() {
        return getEntity();
    }

    public OverlayType getOverlayType() {
        return overlayType;
    }
}
