package com.github.alexmodguy.alexscaves.fabric.forge.client.event;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Cancelable;
import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;

/**
 * Fabric stand-in for the first-person hand render.
 *
 * <p><b>Deliberately empty.</b> The loader hands over the pose stack, the buffer source, the hand,
 * the swing progress and the equip time; this mod reads none of them — its one handler answers a
 * single yes/no question (is the player possessing something, or riding something that hides the
 * arm) and cancels. Modelling the rest would be inventing values for a dispatcher that never needs
 * them, and every one of those values is a different shape on the two ends of the version range.
 *
 * <p>The dispatcher fires this before vanilla's own hand render and skips it when cancelled.
 */
@Cancelable
public class RenderHandEvent extends Event {
}
