package com.github.alexmodguy.alexscaves.client.particle;

/**
 * The blend/atlas bucket a sprite-quad particle belongs in, named in the mod's own vocabulary so a
 * particle class can state it once, un-gated, and have it mean the right thing on all 58 nodes.
 *
 * <p>Vanilla has spelled this three different ways in four MC versions:
 *
 * <ul>
 *   <li>&le;1.21.1 — {@code ParticleRenderType.PARTICLE_SHEET_OPAQUE} / {@code _TRANSLUCENT} /
 *       {@code _LIT}, each an object with its own {@code begin}/{@code end} GL bracket.</li>
 *   <li>1.21.2&ndash;1.21.8 — the same class, minus {@code _LIT} (see the
 *       {@code !mc2102-particle-sheet-lit} rule in {@code stonecutter.gradle.kts}); it and
 *       {@code _OPAQUE} had byte-identical bodies bar the shader bind.</li>
 *   <li>&ge;1.21.9 — {@code SingleQuadParticle.Layer}, a record of
 *       {@code (translucent, atlas, pipeline)}. {@code ParticleRenderType} still exists but now
 *       names the <em>group</em> (which extractor collects the particle), not the blend state, and
 *       every sprite quad shares one group.</li>
 * </ul>
 *
 * <p>{@link ACQuadParticle} translates a constant of this enum into whichever of those the node
 * being built actually has, so the ~50 subclasses carry one plain
 * {@code protected ACParticleLayer acLayer()} override apiece and no {@code //?} gate at all.
 *
 * <p>Deliberately not an alias for either vanilla type: a blanket rename of {@code getRenderType} is
 * exactly the trap that {@code AlexsMobsContinued} bug #58 records, because the mod's non-quad
 * particles keep a method of that name whose return type went the other way.
 */
public enum ACParticleLayer {
    /** Opaque particle atlas — the default for solid sprites. */
    OPAQUE,
    /** Translucent particle atlas — alpha-blended sprites. */
    TRANSLUCENT,
    /**
     * Fullbright sprites. Its own render type only existed up to 1.21.1; from 1.21.2 vanilla folded
     * it into the opaque pass, and these particles override {@code getLightColor} to return 240
     * anyway, which is where the "lit" part actually comes from.
     */
    LIT
}
