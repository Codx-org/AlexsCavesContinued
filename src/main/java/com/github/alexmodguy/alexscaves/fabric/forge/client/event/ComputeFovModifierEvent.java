package com.github.alexmodguy.alexscaves.fabric.forge.client.event;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;
import net.minecraft.world.entity.player.Player;

/**
 * Fabric stand-in for the multiplier vanilla applies to the field of view while an item is in use.
 *
 * <p>Received only. This mod's handler flattens the bow-style zoom for its own charged weapons, so
 * it reads the player and the vanilla-computed modifier and writes a replacement.
 *
 * <p>Two separate values, both kept: the loader hands in {@code getFovModifier()} as vanilla's
 * untouched answer and {@code getNewFovModifier()} as what listeners have made of it so far, so a
 * handler that scales rather than replaces still composes with other mods. Collapsing them into one
 * field would silently change that.
 *
 * <p>This one does <b>not</b> extend the player event base, matching the loader — it fires from the
 * game renderer rather than from anything on the player, and nothing dispatches on the supertype.
 */
public class ComputeFovModifierEvent extends Event {

    private final Player player;
    private final float fovModifier;
    private float newFovModifier;

    public ComputeFovModifierEvent(Player player, float fovModifier, float newFovModifier) {
        this.player = player;
        this.fovModifier = fovModifier;
        this.newFovModifier = newFovModifier;
    }

    public Player getPlayer() {
        return player;
    }

    /** Vanilla's own answer, before any listener touched it. */
    public float getFovModifier() {
        return fovModifier;
    }

    public float getNewFovModifier() {
        return newFovModifier;
    }

    public void setNewFovModifier(float newFovModifier) {
        this.newFovModifier = newFovModifier;
    }
}
