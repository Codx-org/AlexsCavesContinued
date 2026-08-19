package com.github.alexmodguy.alexscaves.fabric.forge.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric stand-in for the loader's named fluid-sound token, the exact shape of {@link ToolAction}.
 *
 * <p>The original is an interned string with a global name table, and the only thing done with one in
 * this tree is to key a fluid type's sound map: three constants go in when the two fluid types are
 * built and one comes back out when acid vaporises. Identity is therefore the whole contract, which
 * is why {@link #get(String)} is the only way to make one and the map is concurrent —
 * {@code SoundActions}' constants initialise from a class initialiser any thread may trigger.
 *
 * <p>It is a separate type from {@code ToolAction} rather than a shared "named token" because the two
 * name tables are separate on the loaders too: nothing should be able to pass an axe action where a
 * bucket sound is wanted and have it merely fail to match.
 */
public final class SoundAction {

    private static final Map<String, SoundAction> ACTIONS = new ConcurrentHashMap<>();

    private final String name;

    private SoundAction(String name) {
        this.name = name;
    }

    public static SoundAction get(String name) {
        return ACTIONS.computeIfAbsent(name, SoundAction::new);
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
