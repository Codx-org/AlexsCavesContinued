package com.github.alexmodguy.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.entity.util.MinecartAccessor;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartBehavior;
import net.minecraft.world.entity.vehicle.OldMinecartBehavior;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The 1.21.2-and-up half of {@link AbstractMinecartMixin}'s magnetic-levitation rendering.
 *
 * <p>1.21.2 rewrote minecarts around {@code MinecartBehavior}, of which there are two: the classic
 * {@code OldMinecartBehavior} and the experimental {@code NewMinecartBehavior}. The rail-position
 * pair {@code getPos}/{@code getPosOffs} went with the old one — the new one has neither, and
 * {@code AbstractMinecartRenderer} calls them on the behavior rather than on the cart. So the two
 * injections that lift a cart onto its hover height had to follow them here.
 *
 * <p>Nothing is done for the experimental behavior. It interpolates a stored render position
 * instead of recomputing one from the rail, so there is no equivalent hook; a levitating cart
 * simply renders on the track under {@code minecart_improvements}.
 *
 * <p>This class is excluded from the compile and pruned out of the mixin config below 1.21.2, where
 * its target does not exist — see {@code ModPlatformPlugin}.
 */
@Mixin(OldMinecartBehavior.class)
public abstract class OldMinecartBehaviorMixin extends MinecartBehavior {

    // Mixin never merges a constructor; this exists only so that `minecart` — declared protected on
    // MinecartBehavior, and therefore not shadowable on OldMinecartBehavior — is in scope, the same
    // trick AbstractMinecartMixin plays with `extends Entity`.
    protected OldMinecartBehaviorMixin(AbstractMinecart minecart) {
        super(minecart);
    }

    @Inject(
            method = {"Lnet/minecraft/world/entity/vehicle/OldMinecartBehavior;getPos(DDD)Lnet/minecraft/world/phys/Vec3;"},
            remap = true,
            at = @At(value = "RETURN"),
            cancellable = true
    )
    public void ac_getPos(double x, double y, double z, CallbackInfoReturnable<Vec3> cir) {
        cir.setReturnValue(MinecartAccessor.applyMagLev(this.minecart, cir.getReturnValue()));
    }

    @Inject(
            method = {"Lnet/minecraft/world/entity/vehicle/OldMinecartBehavior;getPosOffs(DDDD)Lnet/minecraft/world/phys/Vec3;"},
            remap = true,
            at = @At(value = "RETURN"),
            cancellable = true
    )
    public void ac_getPosOffs(double x, double y, double z, double offset, CallbackInfoReturnable<Vec3> cir) {
        cir.setReturnValue(MinecartAccessor.applyMagLev(this.minecart, cir.getReturnValue()));
    }
}
