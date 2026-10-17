package com.github.alexmodguy.alexscaves.mixin.fabric;

import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Reaches {@link MobCategory}'s private constructor, which is the one thing a Fabric mod cannot do
 * for itself and the only thing standing between it and the two categories Alex's Caves needs. See
 * {@code fabric.entity.ACMobCategoryExtension} for why they are needed and
 * {@link MobCategoryMixin} for what is done with them.
 *
 * <p>The two leading parameters are the synthetic pair javac gives every enum constructor — the
 * constant's own name and its ordinal — which javap prints in the {@code descriptor:} line and
 * omits from the pretty-printed signature above it. Read the descriptor, not the signature.
 *
 * <p>26.2 inserted a short F3-readout abbreviation after the serialized name. The call sites carry
 * the same string literals {@code ACMobCategories} does, so the {@code !mc262-mobcategory-*}
 * replacement rules that widen its calls widen theirs too; only this declaration has to say so.
 */
@Mixin(MobCategory.class)
public interface MobCategoryInvoker {

    //? if >=26.2 {
    /*@Invoker("<init>")
    static MobCategory ac_new(String constantName, int ordinal, String name, String debugAbbreviation, int max, boolean friendly, boolean persistent, int despawnDistance) {
        throw new AssertionError();
    }
    *///?} else {
    @Invoker("<init>")
    static MobCategory ac_new(String constantName, int ordinal, String name, int max, boolean friendly, boolean persistent, int despawnDistance) {
        throw new AssertionError();
    }
    //?}
}
