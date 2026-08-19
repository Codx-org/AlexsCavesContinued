package com.github.alexmodguy.alexscaves.fabric.forge.common;

/**
 * The three fluid-sound actions this tree names.
 *
 * <p>The loader declares roughly a dozen; the nine nothing here mentions would be constants no call
 * site could reach. The names are the loader's own, so the tokens {@link SoundAction#get} interns are
 * the same ones anything else reasoning about these sounds by name would produce.
 *
 * <p>Unlike {@link ToolActions} there is no {@code canPerform}-style answer to supply here: a sound
 * action is only ever a key into the map a fluid type was built with, and that map lives on the
 * {@code FluidType} stand-in itself.
 */
public final class SoundActions {

    public static final SoundAction BUCKET_FILL = SoundAction.get("bucket_fill");
    public static final SoundAction BUCKET_EMPTY = SoundAction.get("bucket_empty");
    public static final SoundAction FLUID_VAPORIZE = SoundAction.get("fluid_vaporize");

    private SoundActions() {
    }
}
