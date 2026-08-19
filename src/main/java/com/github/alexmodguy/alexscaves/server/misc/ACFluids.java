package com.github.alexmodguy.alexscaves.server.misc;

import com.github.alexmodguy.alexscaves.server.block.fluid.ACFluidRegistry;
import net.minecraft.world.entity.Entity;

/**
 * The seven questions this mod asks about the fluid an entity is standing in.
 *
 * <p>Up to 1.21.11 those were answered by the loader: Forge and NeoForge gave every entity a
 * {@code FluidType} view of its surroundings ({@code isInFluidType}, {@code getEyeInFluidType},
 * {@code getFluidTypeHeight}, {@code getMaxHeightFluidType}) so that a modded fluid could be
 * queried exactly like water. <b>NeoForge 26.1 deleted all ten of those methods from
 * {@code IEntityExtension}</b> — and its patched {@code Entity} no longer mentions
 * {@code FluidType} at all — because 26 moved the bookkeeping into vanilla:
 * {@link net.minecraft.world.entity.EntityFluidInteraction} tracks height, eye-immersion and flow
 * per {@link net.minecraft.tags.TagKey}{@code <Fluid>}, and {@code Entity} exposes it through
 * {@code getFluidHeight(TagKey)} / {@code isEyeInFluid(TagKey)}.
 *
 * <p>That vanilla mechanism is open to anyone — the tracked set is a constructor argument — so from
 * 26 this class asks vanilla rather than the loader, and {@code mixin.EntityMixin} widens the set
 * from {@code {WATER, LAVA}} to include {@link ACTagRegistry#ACID} and
 * {@link ACTagRegistry#PURPLE_SODA}. The split is by MC version and not by loader on purpose: Forge
 * 62.0.9 still has the old methods, but pointing both loaders at the vanilla path means 26-forge
 * exercises the same code.
 *
 * <p><b>Fabric takes this arm on every one of its 22 nodes</b>, 1.20.1 included, because it has
 * never had the loader-side view and vanilla's has always been there: {@code getFluidHeight(TagKey)}
 * and {@code isEyeInFluid(TagKey)} are public on {@code Entity} the whole way back. Only the
 * <i>filling</i> of the two differs below 26. Eye-immersion needs nothing — vanilla's
 * {@code updateFluidOnEyes} records <i>every</i> tag of the fluid state at eye level, so a mod tag
 * lands in it for free — while the height map is filled by two hardcoded calls, for water and lava,
 * which is what {@code EntityMixin}'s {@code fabric && <26} arm adds this mod's two fluids to.
 * Both this mod's fluid tags list the source <i>and</i> the flowing form, so a tag test is exactly
 * the identity test it replaces.
 *
 * <p>⚠️ One narrowing comes with it. {@code isInFluidType()} meant "touching <i>any</i> fluid that
 * declares a type", so it saw other mods' fluids as well; {@link #isInAnyFluid} below knows only
 * water, lava and this mod's two. Every caller is a mob deciding whether it is swimming or walking,
 * so the difference is that an Alex's Caves mob in a third-party mod's fluid on 26+ behaves as if it
 * were in air. Widening it would mean tracking a tag no fluid opts into; when vanilla or a loader
 * offers a "touching anything" query again, this is the one method to revisit.
 *
 * <p>⚠️ Separately, and <b>not</b> fixed here: NeoForge 26.1.0.19-beta dropped the entity-side
 * {@code FluidType} <i>physics</i> along with the queries, so {@code AcidFluidType#move},
 * {@code canSwimInFluidType} and friends are no longer called on that loader. Motion in acid and
 * purple soda there is whatever vanilla does for an untracked fluid. That is a beta-build gap in
 * NeoForge, not something a query helper can paper over.
 */
public final class ACFluids {

    private ACFluids() {
    }

    /** Height of water at the entity, in blocks, 0 when it is not in any. */
    public static double waterHeight(Entity entity) {
        //? if >=26 || fabric {
        /*return entity.getFluidHeight(net.minecraft.tags.FluidTags.WATER);
        *///?} else {
        return entity.getFluidTypeHeight(ACPlatform.waterFluidType());
        //?}
    }

    /** Height of this mod's acid at the entity, in blocks, 0 when it is not in any. */
    public static double acidHeight(Entity entity) {
        //? if >=26 || fabric {
        /*return entity.getFluidHeight(ACTagRegistry.ACID);
        *///?} else {
        return entity.getFluidTypeHeight(ACFluidRegistry.ACID_FLUID_TYPE.get());
        //?}
    }

    /** Height of this mod's purple soda at the entity, in blocks, 0 when it is not in any. */
    public static double purpleSodaHeight(Entity entity) {
        //? if >=26 || fabric {
        /*return entity.getFluidHeight(ACTagRegistry.PURPLE_SODA);
        *///?} else {
        return entity.getFluidTypeHeight(ACFluidRegistry.PURPLE_SODA_FLUID_TYPE.get());
        //?}
    }

    /** The deepest of the fluids the entity is standing in, in blocks. */
    public static double maxFluidHeight(Entity entity) {
        //? if >=26 || fabric {
        /*return Math.max(Math.max(waterHeight(entity), entity.getFluidHeight(net.minecraft.tags.FluidTags.LAVA)),
                Math.max(acidHeight(entity), purpleSodaHeight(entity)));
        *///?} else {
        return entity.getFluidTypeHeight(entity.getMaxHeightFluidType());
        //?}
    }

    /** Whether the entity is standing in a fluid at all — see the class note on what "at all" means. */
    public static boolean isInAnyFluid(Entity entity) {
        //? if >=26 || fabric {
        /*return maxFluidHeight(entity) > 0;
        *///?} else {
        return entity.isInFluidType();
        //?}
    }

    /** Whether the entity's eyes are inside this mod's acid. */
    public static boolean isEyeInAcid(Entity entity) {
        //? if >=26 || fabric {
        /*return entity.isEyeInFluid(ACTagRegistry.ACID);
        *///?} else {
        return entity.getEyeInFluidType() != null && entity.getEyeInFluidType().equals(ACFluidRegistry.ACID_FLUID_TYPE.get());
        //?}
    }

    /** Whether the entity's eyes are inside this mod's purple soda. */
    public static boolean isEyeInPurpleSoda(Entity entity) {
        //? if >=26 || fabric {
        /*return entity.isEyeInFluid(ACTagRegistry.PURPLE_SODA);
        *///?} else {
        return entity.getEyeInFluidType() != null && entity.getEyeInFluidType().equals(ACFluidRegistry.PURPLE_SODA_FLUID_TYPE.get());
        //?}
    }
}
