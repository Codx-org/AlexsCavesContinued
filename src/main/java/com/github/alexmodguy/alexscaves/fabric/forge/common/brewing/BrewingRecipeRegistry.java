package com.github.alexmodguy.alexscaves.fabric.forge.common.brewing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fabric stand-in for the loaders' static brewing registry, which is what {@code ACEffectRegistry
 * #setup} pours its eleven recipes into below 1.20.5.
 *
 * <p>On the loader this class <i>is</i> the brewing stand's lookup: its patched
 * {@code PotionBrewing} asks it before falling back to the vanilla table. Vanilla has no such hook,
 * so here it is only the near half of that — a collection with an adder. The far half is
 * {@code mixin.fabric.PotionBrewingMixin}, which consults {@link #getRecipes()} after vanilla has
 * failed to match; with it in place these recipes are live rather than inert.
 *
 * <p>Unlike its two neighbours this class survives above 1.20.5 with no gate, even though the loader
 * deleted it there — and on this loader it is still the only path. 1.20.5 replaced the loaders'
 * static registry with a per-world {@code PotionBrewing.Builder} whose {@code Mix} triples cannot
 * express an {@code IBrewingRecipe} that matches its input bottle on the whole stack, and Fabric has
 * no registration event either way, so {@code ACEffectRegistry#setup} keeps filling this on every
 * version there. Both halves are gated to match; see that method's javadoc.
 *
 * <p><b>…and on this loader the eleven cannot be BUILT when they are declared.</b> An
 * {@code IBrewingRecipe} holds finished {@code ItemStack}s, and from MC 26.1 {@code ItemStack}'s
 * constructor reads {@code Holder$Reference#components()}, which throws
 * {@code NullPointerException: Components not bound yet} until the first datapack reload binds them
 * ({@code ReloadableServerResources#updateComponentsAndStaticRegistryTags}). Fabric runs every
 * {@code ModInitializer} long before that, so {@code ACEffectRegistry#setup} — which stands in for
 * Forge's {@code FMLCommonSetupEvent} here — is inside the unbound window and the mod died at
 * {@code PotionContents.createItemStack}. The loaders escape it because from 1.20.5 they register
 * brewing from an event that fires after the reload, i.e. their arm of {@code setup} is empty.
 * Hence {@link #deferRecipes}: the declaration stays where it reads well and the eleven stacks are
 * built on the first question a brewing stand asks, which is necessarily after a world exists.
 */
public final class BrewingRecipeRegistry {

    private static final List<IBrewingRecipe> RECIPES = new ArrayList<>();

    /** Set by {@link #deferRecipes}, run and cleared by the first {@link #getRecipes()}. */
    private static Runnable pending;

    private BrewingRecipeRegistry() {
    }

    public static synchronized void addRecipe(IBrewingRecipe recipe) {
        RECIPES.add(recipe);
    }

    /**
     * Registers {@code filler} to run the first time {@link #getRecipes()} is asked, rather than
     * now. {@code filler} is expected to call {@link #addRecipe} once per recipe.
     */
    public static synchronized void deferRecipes(Runnable filler) {
        pending = filler;
    }

    public static synchronized List<IBrewingRecipe> getRecipes() {
        Runnable filler = pending;
        if (filler != null) {
            // cleared first: a filler that somehow asked again would otherwise recurse forever.
            pending = null;
            filler.run();
        }
        return Collections.unmodifiableList(RECIPES);
    }
}
