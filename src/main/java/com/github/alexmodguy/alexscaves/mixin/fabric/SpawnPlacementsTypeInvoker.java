package com.github.alexmodguy.alexscaves.mixin.fabric;

import net.minecraft.world.entity.SpawnPlacements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Reaches {@code SpawnPlacements.Type}'s constructor, the same way {@link MobCategoryInvoker}
 * reaches {@code MobCategory}'s. That enum carries no fields of its own, so the constructor takes
 * nothing but javac's synthetic pair — the constant's name and its ordinal.
 *
 * <p>The enum is gone from 1.20.5, replaced by an interface a mod can just implement, so the
 * {@code @Mixin} annotation itself has to be gated: above that the target is the enclosing class,
 * which exists on every version and gains nothing here.
 */
//? if <1.20.5 {
@Mixin(SpawnPlacements.Type.class)
//?} else {
/*@Mixin(SpawnPlacements.class)
*///?}
public interface SpawnPlacementsTypeInvoker {

    //? if <1.20.5 {
    @Invoker("<init>")
    static SpawnPlacements.Type ac_new(String constantName, int ordinal) {
        throw new AssertionError();
    }
    //?}
}
