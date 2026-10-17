package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.entity.ACSpawnPlacementTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives the two spawn placements {@code mixin.fabric.SpawnPlacementsTypeMixin} adds their behaviour.
 *
 * <p>A constant appended to an enum carries no state, so the {@code switch} in
 * {@code isSpawnPositionOk} — which is exactly the method Forge patches to consult a
 * {@code Type}'s own predicate — has no arm for either of them and would fall through to its
 * default. {@link ACSpawnPlacementTypes#test} answers for the two and returns {@code null} for
 * every vanilla constant, so this runs at HEAD and cancels only when it recognised the type;
 * otherwise vanilla's switch runs untouched.
 *
 * <p>From 1.20.5 a spawn placement is an interface a mod simply implements and this contributes
 * nothing, so the body is gated out there. The target class needs no gate of its own — it is
 * vanilla on the whole 1.20.1&nbsp;→&nbsp;26.2 range.
 */
@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin {

    //? if <1.20.5 {
    @Inject(method = "isSpawnPositionOk", at = @At("HEAD"), cancellable = true)
    private static void ac_isSpawnPositionOk(SpawnPlacements.Type type, LevelReader level, BlockPos pos, EntityType<?> entityType, CallbackInfoReturnable<Boolean> cir) {
        Boolean answer = ACSpawnPlacementTypes.test(type, level, pos, entityType);
        if (answer != null) {
            cir.setReturnValue(answer);
        }
    }
    //?}
}
