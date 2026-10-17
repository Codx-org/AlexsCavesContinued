package com.github.alexmodguy.alexscaves.citadel;

/**
 * Compile-time constants shared by the vendored Citadel code and by Alex's Caves' own mixins
 * ({@code EntityMixin}, {@code FallingBlockEntityMixin}), which reference {@link #REMAPREFS}.
 *
 * <p>Trimmed against upstream: the April Fools switch is gone with the rest of Citadel's own
 * content, and there is no server config behind it any more.
 */
public class CitadelConstants {

    /** Every mixin here targets vanilla, so the annotation refs are always remapped. */
    public static final boolean REMAPREFS = true;

    public static final boolean DEBUG = false;

    private CitadelConstants() {
    }

    public static boolean debugShaders() {
        return false;
    }
}
