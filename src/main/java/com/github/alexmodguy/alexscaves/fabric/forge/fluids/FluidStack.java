package com.github.alexmodguy.alexscaves.fabric.forge.fluids;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Fabric stand-in for the loader's "some amount of a fluid" value.
 *
 * <p>This tree names the type in exactly two places, and both are <i>parameters</i>: the fluid types'
 * {@code isVaporizedOnPlacement} and {@code onVaporize} take one describing what is being placed, and
 * neither body reads it. So this is deliberately the smallest thing that can carry that meaning — a
 * fluid and an amount in the loader's millibucket unit — rather than a port of the real class, which
 * is a full item-stack analogue with data components, NBT, codecs and stream codecs behind it.
 *
 * <p>Nothing constructs one yet. It becomes reachable when the dispatcher gets far enough to call the
 * vaporise hooks (a bucket emptied into the Nether), at which point the call site builds one from the
 * fluid it is placing; until then this exists so the two overrides keep their signatures rather than
 * being gated out and silently losing acid's "turns to unrefined waste" behaviour.
 */
public final class FluidStack {

    /** One bucket, in the loader's millibucket unit — the amount a placement hook is handed. */
    public static final int BUCKET_VOLUME = 1000;

    public static final FluidStack EMPTY = new FluidStack(Fluids.EMPTY, 0);

    private final Fluid fluid;
    private final int amount;

    public FluidStack(Fluid fluid, int amount) {
        this.fluid = fluid;
        this.amount = amount;
    }

    public Fluid getFluid() {
        return fluid;
    }

    public int getAmount() {
        return amount;
    }

    public boolean isEmpty() {
        return fluid == Fluids.EMPTY || amount <= 0;
    }
}
