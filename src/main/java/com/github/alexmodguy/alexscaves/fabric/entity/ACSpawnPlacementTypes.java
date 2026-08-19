package com.github.alexmodguy.alexscaves.fabric.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LevelReader;

/**
 * The two spawn placements Alex's Caves adds — "in acid" for the radgill and "in purple soda" for
 * the sweetish fish — on the one loader with no way to add a constant to an enum.
 *
 * <p>This is the same obstacle {@link ACMobCategoryExtension} describes and the same answer, minus
 * one step: {@code SpawnPlacements.Type} is never serialised, so there is no codec to rebuild, and
 * {@code mixin.fabric.SpawnPlacementsTypeMixin} only has to build the constants and lengthen
 * {@code $VALUES}. What it does need instead is somewhere for the behaviour to go. Forge's
 * {@code Type.create} gives the constant a predicate and patches the one method that reads it;
 * a constant added this way carries no state at all, so the predicate lives here and
 * {@code mixin.fabric.NaturalSpawnerMixin} routes {@code isSpawnPositionOk} through
 * {@link #test} before vanilla's switch — which is precisely the line Forge patches.
 *
 * <p>Both fields are written from a class initialiser that runs long before the mod's own, so
 * nothing here may reach for anything of this mod's until it is called: {@code ACTagRegistry} is
 * named inside {@link #test} rather than held in a field, so touching this class cannot drag the
 * tag registry into existence early.
 *
 * <p>From 1.20.5 {@code SpawnPlacements.Type} is gone — the enum became the one-method
 * {@code SpawnPlacementType} interface, which a mod simply implements — so everything below is
 * gated out and {@code ACEntityRegistry} writes the same two lambdas the other loaders do.
 */
public final class ACSpawnPlacementTypes {

    //? if <1.20.5 {
    public static SpawnPlacements.Type IN_ACID;
    public static SpawnPlacements.Type IN_SODA;

    static {
        SpawnPlacements.Type.values();
    }

    /**
     * Answers for the two constants above and returns {@code null} for every vanilla one, which is
     * the caller's signal to let vanilla's own switch run.
     */
    public static Boolean test(SpawnPlacements.Type type, LevelReader level, BlockPos pos, EntityType<?> entityType) {
        if (type == IN_ACID) {
            return !level.getFluidState(pos).isEmpty()
                    && level.getFluidState(pos).is(com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry.ACID);
        }
        if (type == IN_SODA) {
            return !level.getFluidState(pos).isEmpty()
                    && level.getFluidState(pos).is(com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry.PURPLE_SODA);
        }
        return null;
    }
    //?}

    private ACSpawnPlacementTypes() {
    }
}
