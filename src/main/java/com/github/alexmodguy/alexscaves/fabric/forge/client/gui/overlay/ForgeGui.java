package com.github.alexmodguy.alexscaves.fabric.forge.client.gui.overlay;

/**
 * Fabric stand-in for the loader's extended HUD, reached through two {@code instanceof} tests in
 * {@code ClientEvents}.
 *
 * <p><b>An interface, not a class</b> — the call sites are
 * {@code Minecraft.getInstance().gui instanceof ForgeGui forgeGui}, and vanilla's own {@code Gui} is
 * what that field holds on this loader. Only an interface can be pattern-matched against it, and
 * only an interface lets a {@code mixin/fabric} on {@code Gui} answer the one question that has a
 * real answer here.
 *
 * <p><b>The two heights are constants, and that is the shipped behaviour rather than a shortcut.</b>
 * They exist on the loader so a mod can stack its own row above vanilla's status bars; the two
 * fields are read once, by {@code hudStackHeight()}, to place the riding meter. Reproducing them
 * would mean re-deriving vanilla's per-frame bar layout from outside vanilla, and the mod already
 * ships an arm that declines to: every node from 1.20.5 up on one of the other loaders returns a
 * flat {@code 0} there and lets the meter's own floor do the placing. Answering {@code 0} here is
 * byte-for-byte that arm. Reading a static field through an instance reference is legal Java
 * (JLS 15.11.1), so the call sites need no change at all.
 *
 * <p>{@link #getGuiTicks()} is a real method because it does have an answer: it drives the shaking
 * of the irradiated hearts, and a {@code Gui} mixin implementing this interface can hand back the
 * counter vanilla already keeps.
 */
public interface ForgeGui {

    int leftHeight = 0;

    int rightHeight = 0;

    int getGuiTicks();
}
