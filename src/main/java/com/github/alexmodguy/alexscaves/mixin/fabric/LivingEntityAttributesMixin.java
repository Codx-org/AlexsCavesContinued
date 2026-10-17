package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.entity.ACFabricAttributes;
import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reproduces, on Fabric, the three places Forge patches {@code LivingEntity} to honour its two
 * extra attributes — see {@link ACFabricAttributes} for what they are and why this mod needs them.
 *
 * <p>Those three sites are the <b>complete</b> set: disassembling every class under
 * {@code net.minecraft.world.entity} in a Forge-patched jar and grepping for a {@code getstatic} of
 * either field finds {@code LivingEntity} and nothing else, in {@code createLivingAttributes},
 * {@code jumpInLiquid} and {@code travel}. So this one file is the whole of the behaviour, and the
 * mod's own call sites are then reading an attribute that means on Fabric exactly what it means on
 * the other two loaders rather than a value only this mod ever writes.
 *
 * <p>Each handler is deliberately a <i>multiplier</i> or a <i>replacement</i> of the same constant
 * vanilla already used, with the attribute defaulting to that constant — 1.0 for the two swim-speed
 * multiplications, 0.08 for gravity — so an entity with no modifier moves bit-for-bit as it did
 * before. Adding the attributes to every living entity therefore changes nothing until something
 * modifies one, which is the property that makes this safe to apply unconditionally.
 *
 * <p>This lives in the common {@code mixins} array rather than the client one: all three targets are
 * movement, which is simulated on both sides.
 *
 * <p>Every swim-speed reference goes through {@code ACCompat.attribute}, the tree's own shim for
 * 1.20.5 putting attributes behind {@code Holder}s in the vanilla signatures — both
 * {@code AttributeSupplier.Builder#add} and {@code LivingEntity#getAttributeValue} moved, so the
 * shim is what keeps this file free of a version gate per call site. The gravity references below
 * are already inside {@code <1.20.5} arms and so predate the move.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityAttributesMixin {

    /**
     * Forge appends both attributes to the shared living-entity supplier, after
     * {@code ARMOR_TOUGHNESS}. RETURN is the same thing said with a builder that is already
     * complete — {@code Builder#add} mutates and the caller chains off its own reference, so
     * appending here reaches every entity type, vanilla and modded alike.
     *
     * <p>Reaching every entity is the requirement, not a bonus: {@code AbstractMovingBlockEntity}
     * and {@code AcidFluidType} both call {@code getAttribute(...).getValue()} on an arbitrary
     * living entity with no null check, exactly as they may on Forge.
     */
    @Inject(method = "createLivingAttributes", at = @At("RETURN"))
    private static void ac_addLoaderAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.getReturnValue().add(ACCompat.attribute(ACFabricAttributes.SWIM_SPEED));
        //? if <1.20.5
        cir.getReturnValue().add(ACFabricAttributes.ENTITY_GRAVITY);
    }

    /**
     * Vanilla nudges a swimming entity up by a flat {@code 0.04}; Forge multiplies that term by the
     * swim speed. Redirecting the {@code Vec3#add} rather than modifying the constant keeps the
     * other two components untouched and needs no ordinal — {@code jumpInLiquid} makes exactly one
     * such call.
     */
    @Redirect(
            method = "jumpInLiquid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private Vec3 ac_swimSpeedJump(Vec3 delta, double x, double y, double z) {
        return delta.add(x, y * ((LivingEntity) (Object) this).getAttributeValue(ACCompat.attribute(ACFabricAttributes.SWIM_SPEED)), z);
    }

    /**
     * The water branch's {@code moveRelative} speed, multiplied by swim speed exactly where Forge
     * does it — immediately after the dolphin's-grace override, so a graced swimmer is scaled too.
     *
     * <p>{@code ordinal = 0} is the water call: the enclosing method makes two, and the second is
     * lava's fixed {@code 0.02F}, which Forge leaves alone. Confirmed against the bytecode rather
     * than the source order, since the branch this sits in is the one guarded by the dolphin's-grace
     * check.
     *
     * <p>1.21.2 broke {@code travel} into {@code travelInAir}/{@code travelInFluid}/{@code
     * travelRidden}/{@code travelFallFlying} and both of these calls went into {@code travelInFluid}
     * together — offsets 139 (behind the {@code DOLPHINS_GRACE} test) and 232 (the {@code 0.02f}
     * lava term) on the 1.21.2 jar, in that order. So only the selector moves; the ordinal, the
     * index and the handler are all unchanged, which is the point of having checked the order in
     * bytecode in the first place. The {@code <1.20.5} gravity hook below stays on {@code travel}:
     * its band ends four versions before the split.
     *
     * <p>1.21.11 split that method again — {@code travelInFluid} is four branch instructions now and
     * hands off to {@code travelInWater(Vec3,DZD)} or {@code travelInLava(Vec3,DZD)} — so the water
     * call moved once more, to offset 103 of {@code travelInWater}, still immediately after the
     * dolphin's-grace override. It is the ONLY {@code moveRelative} in that method (lava's {@code
     * 0.02f} term went to the sibling), so {@code ordinal = 0} is now merely redundant rather than
     * load-bearing; it is kept so the three arms differ in nothing but the selector.
     */
    @ModifyArg(
            //? if >=1.21.11 {
            /*method = "travelInWater",
            *///?} elif >=1.21.2 {
            /*method = "travelInFluid",
            *///?} else {
            method = "travel",
            //?}
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V",
                    ordinal = 0
            ),
            index = 0
    )
    private float ac_swimSpeedTravel(float amount) {
        return amount * (float) ((LivingEntity) (Object) this).getAttributeValue(ACCompat.attribute(ACFabricAttributes.SWIM_SPEED));
    }

    /**
     * The gravity term at the top of {@code travel}. Vanilla writes a literal {@code 0.08} into a
     * local and uses it in all three of the branches below; Forge replaces the literal with the
     * attribute's value, which is what this does. One occurrence in the method, so no ordinal.
     *
     * <p>Gated below 1.20.5 only, because from there vanilla owns the attribute itself and applies
     * it in this very place — a second application would square it.
     */
    //? if <1.20.5 {
    @ModifyConstant(method = "travel", constant = @Constant(doubleValue = 0.08D))
    private double ac_entityGravity(double original) {
        return ((LivingEntity) (Object) this).getAttributeValue(ACFabricAttributes.ENTITY_GRAVITY);
    }
    //?}
}
