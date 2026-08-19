package com.github.alexmodguy.alexscaves.mixin.client.citadel;

import com.github.alexmodguy.alexscaves.citadel.client.tick.ClientTickRateTracker;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The client half of {@code ServerClockInstanceMixin} — see that file for what MC 26 did to day
 * time.
 *
 * <p>The two sides scale independently, exactly as {@code ClientLevelMixin} and
 * {@code ServerLevelMixin} did before 26. A {@code ClientboundSetTimePacket} carries the clock's
 * absolute position ({@code totalTicks} + {@code partialTick}) and its <em>base</em> rate, and the
 * client extrapolates from there between packets; so the client applies the multiplier at its own
 * tick and never sees a scaled rate over the wire, which is what keeps the two from compounding.
 * That also means a vanilla client on a modded server behaves the way it did before: correct at
 * every sync, un-scaled in between.
 *
 * <p>Unlike the server's, this class is the same on every loader — {@code tick(long)} is untouched
 * by both Forge and NeoForge, whose divergence is confined to the server-side instance.
 */
@Mixin(net.minecraft.client.ClientClockManager.class)
public class ClientClockManagerMixin {

    @ModifyExpressionValue(
            method = "tick(J)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/ClientClockManager$ClockInstance;rate:F", opcode = org.objectweb.asm.Opcodes.GETFIELD))
    private float citadel_clockRate(float rate) {
        return rate * ClientTickRateTracker.getForClient(Minecraft.getInstance()).getDayTimeRateMultiplier();
    }
}
