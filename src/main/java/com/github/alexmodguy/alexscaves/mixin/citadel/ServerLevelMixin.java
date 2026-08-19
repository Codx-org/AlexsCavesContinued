package com.github.alexmodguy.alexscaves.mixin.citadel;

import com.github.alexmodguy.alexscaves.citadel.CitadelConstants;
import com.github.alexmodguy.alexscaves.citadel.server.tick.ServerTickRateTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    @ModifyConstant(
            method = "Lnet/minecraft/server/level/ServerLevel;tickTime()V",
            remap = CitadelConstants.REMAPREFS,
            constant = @Constant(longValue = 1L),
            //? if (neoforge && >=1.21) || >=26 {
            /*expect = 1)
            *///?} else {
            expect = 2)
            //?}
    private long citadel_clientSetDayTime(long timeIn) {
        return ServerTickRateTracker.getForServer(server).getDayTimeIncrement(timeIn);
    }

    // `tickTime` advances two clocks off the same `1L`, the game time and — when doDaylightCycle is
    // on — the day time, which is why the constant above is expected twice. NeoForge 1.21 added a
    // day-length gamerule and rewrote the second one as `getDayTime() + advanceDaytime()`, so on
    // those nodes there is one `1L` left and the day-time half of a CELESTIAL tick-rate modifier
    // silently stopped applying. Mixin only *warns* when `expect` is missed (require is 1), so
    // nothing crashed; it surfaced when verify_mixins.py started reading the patched NeoForge jar.
    // Scaling the new call's result restores exactly what the second constant used to do.
    //
    // 26 takes the day time out of this method on BOTH loaders — a level reads its sky time from a
    // WorldClock now, and `tickTime` advances only the game time, so the constant loads once there
    // too and `advanceDaytime` is gone entirely. The remaining constant is still scaled, which is
    // what every earlier version did to the game-time half as well; the day-time half moved to
    // ServerClockInstanceMixin.
    //? if neoforge && >=1.21 && <26 {
    /*@com.llamalad7.mixinextras.injector.ModifyExpressionValue(
            method = "Lnet/minecraft/server/level/ServerLevel;tickTime()V",
            remap = CitadelConstants.REMAPREFS,
            at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;advanceDaytime()J"))
    private long citadel_advanceDaytime(long original) {
        return ServerTickRateTracker.getForServer(server).getDayTimeIncrement(original);
    }
    *///?}
}
