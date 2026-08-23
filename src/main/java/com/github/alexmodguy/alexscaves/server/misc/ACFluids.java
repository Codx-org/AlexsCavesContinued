package com.github.alexmodguy.alexscaves.server.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

/**
 * The seven questions this mod asks about the fluid an entity is standing in, answered by reading
 * the world directly rather than by asking the loader or vanilla for a cached answer.
 *
 * <p><b>Why it does its own scan.</b> There have been three different answers to "how deep is the
 * modded fluid around this entity", and no source spelling covers them all:
 *
 * <ul>
 *   <li>Up to 1.21.11, Forge and NeoForge gave every entity a {@code FluidType} view of its
 *       surroundings ({@code isInFluidType}, {@code getEyeInFluidType}, {@code getFluidTypeHeight},
 *       {@code getMaxHeightFluidType}), so a modded fluid could be queried exactly like water.</li>
 *   <li>26 moved the bookkeeping into vanilla: {@code EntityFluidInteraction} tracks height,
 *       eye-immersion and flow per {@code TagKey<Fluid>}, for exactly the tags it is handed at
 *       construction, and {@code Entity} exposes it as {@code getFluidHeight(TagKey)} /
 *       {@code isEyeInFluid(TagKey)}. NeoForge 26.1 deleted all ten {@code FluidType} methods from
 *       {@code IEntityExtension}, so the vanilla path was the only one left.</li>
 *   <li>⚠️ <b>NeoForge put {@code FluidType} back between builds 26.1.2.87 and 26.1.2.97</b> — same
 *       MC version, both live in the wild. On .97 its patched {@code EntityFluidInteraction} keys
 *       {@code trackerByFluid} by {@code FluidType} instead of by tag, comments out the constructor's
 *       {@code Set<TagKey<Fluid>>} loop, and routes every {@code TagKey}-shaped method through
 *       {@code getFluidTypeByTag}, which <b>hard-throws</b> for anything that is not
 *       {@code FluidTags.WATER} or {@code FluidTags.LAVA}:
 *       {@code IllegalArgumentException: Cannot look up tracker by tag for non-vanilla fluid}.
 *       That is a crash on world join for every player on .97 (reported against 1.0.0, from
 *       {@code ClientEvents#fogColor} → {@link #isEyeInAcid}).</li>
 * </ul>
 *
 * <p>The last one is what forces this shape. It tracks the <b>loader build, not the MC version</b>,
 * so no {@code //?} gate and no {@code replacements.string} rule can express it — one jar has to be
 * right on .87 and on .97 at once. Vanilla's own algorithms, run against
 * {@code Level#getFluidState}, are the only thing all three eras agree on: {@code FluidState},
 * {@code FluidState#getHeight}, {@code FluidState#isEmpty} and {@code FluidState#is(TagKey)} are
 * present and identical from 1.20.1 to 26.2 (at 26 the {@code is} overloads moved onto
 * {@code TypedInstance}, which {@code FluidState} implements, so they still resolve). Hence: no gate
 * in this class at all, one implementation on all 58 nodes — and, usefully, one the active node
 * ({@code 1.20.1-forge}) actually compiles and runs, so it can be tested in a dev client instead of
 * only in the 58-node sweep.
 *
 * <p>{@link #fluidHeight} mirrors vanilla {@code Entity#updateFluidHeightAndDoFluidPushing} (deflate
 * the bounding box by 0.001, walk its blocks, keep the greatest fluid surface above the box floor)
 * and {@link #isEyeInFluid} mirrors {@code updateFluidOnEyes} (one probe just below eye level). Both
 * this mod's fluid tags list the source <i>and</i> the flowing form, so a tag test is exactly the
 * identity test it replaces. The difference from a tracker is that these are instantaneous rather
 * than sampled once per tick, which no caller can tell apart.
 *
 * <p>The one deliberate widening: {@link #isInAnyFluid} and {@link #maxFluidHeight} test for
 * <i>any</i> non-empty fluid, not a fixed list. That restores what {@code isInFluidType()} meant
 * below 26 — an Alex's Caves mob in a third-party mod's fluid swims rather than behaving as if it
 * were in air — which the 26 rewrite had narrowed to water, lava and this mod's two.
 *
 * <p>Note that <b>pushing</b> is a separate matter and is still the loader's/vanilla's:
 * {@code mixin.EntityMixin} widens the tracked tag set on 26 and calls
 * {@code updateFluidHeightAndDoFluidPushing} on Fabric below 26. Those arms stay; this class only
 * answers questions.
 */
public final class ACFluids {

    /** Vanilla's own epsilon in {@code updateFluidHeightAndDoFluidPushing}. */
    private static final double BOX_SHRINK = 0.001D;

    /** Vanilla's own drop below the eye position in {@code updateFluidOnEyes}. */
    private static final double EYE_DROP = 0.11111111D;

    private ACFluids() {
    }

    /** Height of water at the entity, in blocks, 0 when it is not in any. */
    public static double waterHeight(Entity entity) {
        return fluidHeight(entity, FluidTags.WATER);
    }

    /** Height of this mod's acid at the entity, in blocks, 0 when it is not in any. */
    public static double acidHeight(Entity entity) {
        return fluidHeight(entity, ACTagRegistry.ACID);
    }

    /** Height of this mod's purple soda at the entity, in blocks, 0 when it is not in any. */
    public static double purpleSodaHeight(Entity entity) {
        return fluidHeight(entity, ACTagRegistry.PURPLE_SODA);
    }

    /** The deepest of the fluids the entity is standing in, in blocks — any fluid, not a fixed list. */
    public static double maxFluidHeight(Entity entity) {
        return fluidHeight(entity, null);
    }

    /** Whether the entity is standing in a fluid at all — any fluid, this mod's or another's. */
    public static boolean isInAnyFluid(Entity entity) {
        return maxFluidHeight(entity) > 0.0D;
    }

    /** Whether the entity's eyes are inside this mod's acid. */
    public static boolean isEyeInAcid(Entity entity) {
        return isEyeInFluid(entity, ACTagRegistry.ACID);
    }

    /** Whether the entity's eyes are inside this mod's purple soda. */
    public static boolean isEyeInPurpleSoda(Entity entity) {
        return isEyeInFluid(entity, ACTagRegistry.PURPLE_SODA);
    }

    /**
     * How far the surface of {@code tag}'s fluid stands above the bottom of the entity's bounding
     * box, or 0 when there is none. A {@code null} tag means "any fluid", which is what
     * {@link #maxFluidHeight} wants.
     *
     * <p>This is vanilla's {@code Entity#updateFluidHeightAndDoFluidPushing} with the pushing taken
     * out: shrink the box so a fluid merely touched edge-on does not count, walk every block it
     * covers, and keep the greatest {@code blockY + fluidHeight} that is at or above the box floor.
     */
    private static double fluidHeight(Entity entity, TagKey<Fluid> tag) {
        Level level = entity.level();
        AABB box = entity.getBoundingBox().deflate(BOX_SHRINK);
        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.ceil(box.maxX);
        int minY = (int) Math.floor(box.minY);
        int maxY = (int) Math.ceil(box.maxY);
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.ceil(box.maxZ);
        double deepest = 0.0D;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    pos.set(x, y, z);
                    if (!level.hasChunkAt(pos)) {
                        continue;
                    }
                    FluidState fluid = level.getFluidState(pos);
                    if (fluid.isEmpty() || (tag != null && !fluid.is(tag))) {
                        continue;
                    }
                    double surface = y + fluid.getHeight(level, pos);
                    if (surface >= box.minY) {
                        deepest = Math.max(deepest, surface - box.minY);
                    }
                }
            }
        }
        return deepest;
    }

    /**
     * Whether {@code tag}'s fluid covers the entity's eyes — vanilla's {@code updateFluidOnEyes},
     * which probes the single block just below eye level rather than the whole box.
     *
     * <p>⚠️ Vanilla's version carries a <b>boat exemption</b> (skip the check entirely when the
     * vehicle is a {@code Boat} that is not itself underwater and whose box straddles the eye), and
     * this does not reproduce it, on purpose. Doing it properly needs an {@code instanceof} on a
     * class that is {@code Boat} below 1.21.2 and {@code AbstractBoat} from there, i.e. another
     * gate; and the tempting ungated approximation — "skip whenever any vehicle's box contains the
     * eye" — is <b>worse than omitting it</b>, because this mod has large rideable mobs whose boxes
     * swallow the rider's eye, so it would suppress acid fog for anyone riding one. Vanilla's
     * exemption almost never fires in practice (a floating boat keeps the eye above the surface; a
     * submerged one fails {@code isUnderWater()}), so the whole cost of leaving it out is at most a
     * frame of fog while a boat bobs in acid. Do not "fix" this without the gated check.
     */
    private static boolean isEyeInFluid(Entity entity, TagKey<Fluid> tag) {
        Level level = entity.level();
        double eyeY = entity.getEyeY() - EYE_DROP;
        BlockPos pos = BlockPos.containing(entity.getX(), eyeY, entity.getZ());
        if (!level.hasChunkAt(pos)) {
            return false;
        }
        FluidState fluid = level.getFluidState(pos);
        if (fluid.isEmpty() || !fluid.is(tag)) {
            return false;
        }
        return pos.getY() + fluid.getHeight(level, pos) > eyeY;
    }
}
