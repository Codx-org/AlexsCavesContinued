package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.entity.ACSpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds Alex's Caves' two spawn placements to {@code SpawnPlacements.Type} at the tail of that
 * enum's class initialiser — see {@code fabric.entity.ACSpawnPlacementTypes} for the whole
 * argument, and {@link MobCategoryMixin} for the same shape done to a serialised enum.
 *
 * <p>Only {@code $VALUES} has to be lengthened here: this enum has no codec, so nothing derived
 * from it needs rebuilding. Doing it at all is defensive rather than required — {@code values()} is
 * what sizes any {@code EnumSet} or {@code EnumMap} built over the type later, and leaving it short
 * would make an ordinal past the end of one of those an out-of-bounds error rather than a miss.
 *
 * <p>Gone from 1.20.5, so the annotation is gated onto the enclosing class above that version,
 * where this mixin contributes nothing.
 */
//? if <1.20.5 {
@Mixin(SpawnPlacements.Type.class)
//?} else {
/*@Mixin(SpawnPlacements.class)
*///?}
public class SpawnPlacementsTypeMixin {

    //? if <1.20.5 {
    @Shadow
    @Final
    @Mutable
    private static SpawnPlacements.Type[] $VALUES;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void ac_addModPlacementTypes(CallbackInfo ci) {
        int first = $VALUES.length;
        SpawnPlacements.Type inAcid = SpawnPlacementsTypeInvoker.ac_new("ALEXSCAVES_IN_ACID", first);
        SpawnPlacements.Type inSoda = SpawnPlacementsTypeInvoker.ac_new("ALEXSCAVES_IN_SODA", first + 1);

        SpawnPlacements.Type[] extended = java.util.Arrays.copyOf($VALUES, first + 2);
        extended[first] = inAcid;
        extended[first + 1] = inSoda;
        $VALUES = extended;

        ACSpawnPlacementTypes.IN_ACID = inAcid;
        ACSpawnPlacementTypes.IN_SODA = inSoda;
    }
    //?}
}
