package com.github.alexmodguy.alexscaves.server.misc;

import net.minecraft.resources.ResourceLocation;

/**
 * The three {@code ResourceLocation} factory methods, on every node.
 *
 * <p><b>Why this exists.</b> {@code fromNamespaceAndPath}, {@code parse} and
 * {@code withDefaultNamespace} are vanilla only from 1.21 on. On 1.20.x they are a <b>Forge
 * patch</b> — present in the Forge-patched jar, absent from NeoForge and from Fabric — which is
 * why 1.20.4-forge compiles ~500 call sites of them and 1.20.4-neoforge does not. Going the other
 * way is no better: 1.21 made the {@code ResourceLocation} constructor private, so the tree cannot
 * simply use {@code new} either. No single spelling works everywhere.
 *
 * <p>The call sites therefore keep the vanilla spelling and a Stonecutter rule group in
 * {@code stonecutter.gradle.kts} re-points them here — fully qualified, so no file needs an import
 * added — on exactly the nodes that lack the patch (non-Forge, below 1.21). Everywhere else the
 * rule does not fire and this class is dead code that merely has to compile.
 *
 * <p>⚠️ <b>The class name must not contain the vanilla type's name at all</b>, in any position.
 * A {@code replacements.string} match is <b>not</b> identifier-boundary-checked on either edge, so
 * 1.21.11's blanket rename of that type to {@code Identifier} rewrites it inside a longer
 * identifier just as happily as on its own — which silently renames <i>this class</i> and not its
 * file, and javac stops with "is public, should be declared in a file named …". That is what the
 * earlier name did, and why this one carries none of the token. Same reason the rules re-pointing
 * the call sites here can never rewrite their own output.
 *
 * <p>⚠️ Do not spell a qualified factory call anywhere in this file's prose — the rules rewrite
 * comments too, so the sentence would end up saying the opposite of what was written.
 */
public final class ACIdFactories {

    private ACIdFactories() {
    }

    /** The namespace+path factory. */
    public static ResourceLocation of(String namespace, String path) {
        //? if >=1.21
        /*return ResourceLocation.fromNamespaceAndPath(namespace, path);*/
        //? if <1.21
        return new ResourceLocation(namespace, path);
    }

    /** The parsing factory — splits a {@code namespace:path} string. */
    public static ResourceLocation parse(String location) {
        //? if >=1.21
        /*return ResourceLocation.parse(location);*/
        //? if <1.21
        return new ResourceLocation(location);
    }

    /** The default-namespace factory — a path in {@code minecraft}. */
    public static ResourceLocation vanilla(String path) {
        //? if >=1.21
        /*return ResourceLocation.withDefaultNamespace(path);*/
        //? if <1.21
        return new ResourceLocation("minecraft", path);
    }
}
