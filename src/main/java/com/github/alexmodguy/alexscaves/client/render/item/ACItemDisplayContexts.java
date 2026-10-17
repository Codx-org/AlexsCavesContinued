package com.github.alexmodguy.alexscaves.client.render.item;

import net.minecraft.world.item.ItemDisplayContext;

/**
 * The display context of the item currently being submitted, for the 26-and-up special renderers.
 *
 * <p>Every era before 26 handed the display context straight to whatever drew an item — the ISTER's
 * {@code renderByItem} took it, and so did {@code SpecialModelRenderer#render}/{@code #submit} on
 * 1.21.4–1.21.11. <b>26 dropped it from that signature</b>: the interface is now
 * {@code submit(T, PoseStack, SubmitNodeCollector, int, int, boolean, int)} and vanilla's own
 * special renderers do not branch on it, because vanilla expresses per-context differences in the
 * item model definition instead (a {@code minecraft:select} on {@code minecraft:display_context}).
 *
 * <p>This mod's two legacy renderers cannot follow that: {@link ACItemstackRenderer} reads the
 * context per stack and per branch — left versus right hand, a flat sprite in the GUI, full-bright
 * lighting anywhere but on the ground — so expressing it in the model definition would mean a
 * separate registered renderer type for each of the thirteen contexts, and the branches are not
 * even a partition (some read it twice, for different decisions).
 *
 * <p>So the context is carried beside the call instead. {@code mixin.client.ItemStackRenderStateMixin}
 * publishes it around {@code ItemStackRenderState#submit}, which is the single funnel every special
 * renderer's submit runs inside — the layer loop that reaches {@code SpecialModelRenderer#submit} is
 * private to that class and called from nowhere else. Read it once at the top of a submit, the way
 * the parameter used to be read.
 *
 * <p>Render-thread only, and deliberately not a {@link ThreadLocal}: item submission happens on the
 * render thread and nowhere else, and a plain field is what the value is worth. Nesting is safe by
 * construction — a renderer that draws another item through {@code ACClientCompat#renderItemStatic}
 * passes the context it already read as an argument, so the inner submit resetting this to
 * {@link ItemDisplayContext#NONE} on its way out cannot disturb it.
 */
public final class ACItemDisplayContexts {

    private static ItemDisplayContext current = ItemDisplayContext.NONE;

    private ACItemDisplayContexts() {
    }

    /** The context of the item being submitted, or {@link ItemDisplayContext#NONE} outside one. */
    public static ItemDisplayContext current() {
        return current;
    }

    /** Called by the mixin at both ends of {@code ItemStackRenderState#submit}. */
    public static void set(ItemDisplayContext context) {
        current = context == null ? ItemDisplayContext.NONE : context;
    }
}
