package com.github.alexmodguy.alexscaves.mixin.citadel;

import com.github.alexmodguy.alexscaves.citadel.Citadel;
import com.github.alexmodguy.alexscaves.citadel.server.tick.ServerTickRateTracker;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Where a CELESTIAL tick-rate modifier applies from MC 26.
 *
 * <p>Until 26, day time was a second counter advanced beside the game time in
 * {@code ServerLevel#tickTime}, and {@code ServerLevelMixin} scaled the {@code 1L} it stepped by.
 * 26 moved it out entirely: a level's sky reads a {@code WorldClock} from the world's
 * {@code ServerClockManager}, whose per-clock instance keeps a {@code float rate} and a
 * {@code partialTick} accumulator and adds whole ticks out of it. So the modifier belongs on the
 * rate, which is both simpler and strictly better behaved than what it replaces — a rate below 1
 * now slows the sky smoothly, where {@code getDayTimeIncrement} could only drop every n-th tick.
 *
 * <p>{@code ClientClockManagerMixin} is the other half; the two scale independently, and only the
 * base rate travels over the wire, so they cannot compound.
 *
 * <p>The target is a private nested class, hence {@code targets =} rather than a class literal;
 * that is safe unremapped because 26 ships official names on all three loaders. The mod is not on
 * the classpath of the clock instance in any other way, so the server comes from
 * {@code CitadelProxy}, which has held it since {@code MinecraftServerMixin}'s constructor hook.
 *
 * <p>⚠️ The method is spelled differently per loader. Vanilla (so Forge and Fabric) tests the
 * ADVANCE_TIME game rule once in {@code ServerClockManager#tick} and gives the instance a bare
 * {@code tick()}; NeoForge moved that test per-clock so it can honour its own
 * {@code ignores_advance_time_rule} tag, and its instance takes {@code tick(boolean)} — with the
 * old no-arg one left behind as a deprecated delegate that nothing calls. Injecting into the
 * vanilla spelling on NeoForge would therefore succeed and never run.
 *
 * <p>⚠️⚠️ …and that patch tracks the NeoForge <em>build</em>, not the MC version: 26.1.0.19-beta
 * ships the bare vanilla {@code tick()} and nothing else, 26.1.1.15-beta is the first build with
 * the overload. The gate below can still be written as a version predicate only because the pin
 * table fixes one build per node; on a pin bump, javap the class rather than trusting the
 * predicate. The rate field and the {@code partialTick} accumulator around it are identical in
 * both spellings, so the two arms differ in nothing but the selector.
 */
@Mixin(targets = "net.minecraft.world.clock.ServerClockManager$ClockInstance")
public class ServerClockInstanceMixin {

    //? if neoforge && >=26.1.1 {
    /*@ModifyExpressionValue(
            method = "tick(Z)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/clock/ServerClockManager$ClockInstance;rate:F", opcode = org.objectweb.asm.Opcodes.GETFIELD))
    private float citadel_clockRate(float rate) {
        return citadel_scaleClockRate(rate);
    }
    *///?} else {
    @ModifyExpressionValue(
            method = "tick()V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/clock/ServerClockManager$ClockInstance;rate:F", opcode = org.objectweb.asm.Opcodes.GETFIELD))
    private float citadel_clockRate(float rate) {
        return citadel_scaleClockRate(rate);
    }
    //?}

    @Unique
    private float citadel_scaleClockRate(float rate) {
        MinecraftServer server = Citadel.PROXY.getMinecraftServer();
        if (server == null) {
            return rate;
        }
        return rate * ServerTickRateTracker.getForServer(server).getDayTimeRateMultiplier();
    }
}
