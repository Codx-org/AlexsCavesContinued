package com.github.alexmodguy.alexscaves.server.entity.util;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public interface MinecartAccessor {

    boolean isOnMagLevRail();

    /**
     * How far this minecart has risen onto a magnetic levitation rail, interpolated for the frame:
     * zero on ordinary track, one once it is fully hovering.
     */
    float getMagLevAmount(float partialTicks);

    /**
     * Lifts a rail position towards the minecart's own height by however much of the way onto a
     * levitation rail it is — the cart is drawn hovering above the track rather than sitting in it.
     *
     * <p>Static because 1.21.2 moved {@code getPos}/{@code getPosOffs} off {@code AbstractMinecart}
     * onto {@code OldMinecartBehavior}, so the return-value injection that applies this is made from
     * two different mixins depending on the version, and only one of them can be compiled at a time.
     */
    @Nullable
    static Vec3 applyMagLev(AbstractMinecart minecart, @Nullable Vec3 railPos) {
        float partialTicks = AlexsCaves.PROXY.getPartialTicks();
        double magLevAmount = ((MinecartAccessor) minecart).getMagLevAmount(partialTicks);
        if (magLevAmount < 0.0D) {
            return railPos;
        }
        double yClientSide = minecart.yOld + (minecart.getY() - minecart.yOld) * partialTicks;
        return railPos == null
                ? minecart.getPosition(partialTicks)
                : new Vec3(railPos.x, railPos.y + (yClientSide - railPos.y) * magLevAmount, railPos.z);
    }
}
