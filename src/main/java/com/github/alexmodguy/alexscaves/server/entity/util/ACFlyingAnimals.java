package com.github.alexmodguy.alexscaves.server.entity.util;

import net.minecraft.world.entity.Entity;

/**
 * "Is that entity a flying animal?" — the one question three sites in this mod ask of
 * {@code FlyingAnimal}: the tremorsaurus' melee goal (it prefers airborne prey), the tremorsaurus'
 * rider attack, and the totem of possession (it flies whatever it is riding).
 *
 * <p>Up to 26.1 that is a plain {@code instanceof} against vanilla's marker interface, which bees,
 * parrots and allays implement. 26.2 deleted the interface with no successor, and this mod's own
 * copy (see {@code server.compat.FlyingAnimal}) can only ever answer for this mod's own mobs — so
 * on that version the test falls back to the navigator, which is what actually made those three
 * vanilla mobs fly and which every modded flier that behaves the same way also uses. It is a
 * broader net than the marker was (a happy ghast now counts, and it did not before), and that is
 * the right side to err on for all three call sites: each of them is asking whether the thing is
 * in the air, not which class it descends from.
 */
public class ACFlyingAnimals {

    public static boolean isFlyingAnimal(Entity entity) {
        //? if <26.2 {
        return entity instanceof net.minecraft.world.entity.animal.FlyingAnimal;
        //?} else {
        /*return entity instanceof com.github.alexmodguy.alexscaves.server.compat.FlyingAnimal
                || entity instanceof net.minecraft.world.entity.Mob mob
                        && mob.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
        *///?}
    }
}
