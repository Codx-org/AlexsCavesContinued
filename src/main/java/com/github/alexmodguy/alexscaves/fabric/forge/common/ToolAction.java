package com.github.alexmodguy.alexscaves.fabric.forge.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric stand-in for the loader's named tool-action token.
 *
 * <p>The original is an interned string with a global name table and identity comparison, and that
 * is all this tree ever asks of it: every one of the six call sites either compares two of these
 * with {@code ==} or passes one to a question about an item. So the type is reproduced rather than
 * abstracted away — a mod-specific enum would read the same at the call sites and would then have to
 * be translated back at the two seams that hand one to the loader.
 *
 * <p>Interning is what makes {@code ==} correct, so {@link #get(String)} is the only way to make one
 * and the map is concurrent: {@code ToolActions}' constants are initialised from a class initialiser
 * that any thread may trigger.
 *
 * <p>⚠️ What is <b>not</b> here is the dispatch. On the other two loaders the loader patches vanilla
 * with {@code ItemStack#canPerformAction} and {@code Block#getToolModifiedState}, so a tool action is
 * a question the game itself knows how to answer. On this loader nothing does, and the mod's answer
 * lives in {@link ToolActions#canPerform} — reachable from the two call sites that are player-facing
 * (the submarine's axe scrape and the shield check). The three {@code getToolModifiedState} overrides
 * compile and are never called: stripping is a vanilla registry on this loader, and wiring the mod's
 * six strippable blocks into it belongs with the rest of the dispatcher work.
 */
public final class ToolAction {

    private static final Map<String, ToolAction> ACTIONS = new ConcurrentHashMap<>();

    private final String name;

    private ToolAction(String name) {
        this.name = name;
    }

    public static ToolAction get(String name) {
        return ACTIONS.computeIfAbsent(name, ToolAction::new);
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
