package com.github.alexmodguy.alexscaves.fabric.forge.common;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

/**
 * The four tool actions this tree names, and the answer to "can this item perform one".
 *
 * <p>The names are the loader's own — they are what {@link ToolAction#get} interns on, so anything
 * else in the game that reasons about the same actions by name still meets these constants. Only the
 * four are declared: the loader's list is thirteen, and the nine nothing here mentions would be
 * constants no call site could reach.
 *
 * <p>{@link #canPerform} is the part that has no counterpart to copy. Two of its three answers are
 * deliberately tag- and type-based rather than a lookup table:
 *
 * <ul>
 *   <li>the axe actions read {@code minecraft:axes}, which exists on all 22 nodes (checked, not
 *       assumed) and includes modded axes — the thing an {@code instanceof} on the vanilla class
 *       would miss;</li>
 *   <li>blocking asks whether the item <i>is</i> a shield, which is the same question the callers
 *       above 1.21.5 ask of the data component that replaced this action entirely;</li>
 *   <li>casting has no portable test and no reachable caller here, so it answers {@code false}
 *       rather than guessing. The one place this tree asks it is an override the loader calls and
 *       this one does not.</li>
 * </ul>
 */
public final class ToolActions {

    public static final ToolAction AXE_STRIP = ToolAction.get("axe_strip");
    public static final ToolAction AXE_SCRAPE = ToolAction.get("axe_scrape");
    public static final ToolAction FISHING_ROD_CAST = ToolAction.get("fishing_rod_cast");
    public static final ToolAction SHIELD_BLOCK = ToolAction.get("shield_block");

    private ToolActions() {
    }

    public static boolean canPerform(ItemStack stack, ToolAction action) {
        if (action == AXE_STRIP || action == AXE_SCRAPE) {
            return stack.is(ItemTags.AXES);
        }
        if (action == SHIELD_BLOCK) {
            return stack.getItem() instanceof ShieldItem;
        }
        return false;
    }
}
