package com.github.alexmodguy.alexscaves.mixin.client.citadel;

import com.github.alexmodguy.alexscaves.citadel.CitadelConstants;
import com.github.alexmodguy.alexscaves.citadel.client.tick.ClientTickRateTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.function.Supplier;

// Upstream also injected an EventGetStarBrightness hook here. Alex's Caves never listens for it, so
// neither the event nor its injection is vendored.
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin extends Level {

    // 1.21.2 dropped the Supplier<ProfilerFiller> from Level's constructor — the profiler is
    // reached through Profiler.get() now. A mixin's own constructor is discarded at apply time, so
    // this only has to keep the compiler happy against whichever Level is on the classpath.
    //? if >=1.21.2 {
    /*protected ClientLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> levelResourceKey, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeHolder, boolean b1, boolean b2, long seed, int i) {
        super(writableLevelData, levelResourceKey, registryAccess, dimensionTypeHolder, b1, b2, seed, i);
    }
    *///?} else {
    protected ClientLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> levelResourceKey, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeHolder, Supplier<ProfilerFiller> filler, boolean b1, boolean b2, long seed, int i) {
        super(writableLevelData, levelResourceKey, registryAccess, dimensionTypeHolder, filler, b1, b2, seed, i);
    }
    //?}

    @ModifyConstant(
            method = "Lnet/minecraft/client/multiplayer/ClientLevel;tickTime()V",
            remap = CitadelConstants.REMAPREFS,
            constant = @Constant(longValue = 1L),
            //? if (neoforge && >=1.21) || >=26 {
            /*expect = 1)
            *///?} else {
            expect = 2)
            //?}
    private long citadel_clientSetDayTime(long timeIn) {
        return ClientTickRateTracker.getForClient(Minecraft.getInstance()).getDayTimeIncrement(timeIn);
    }

    // The client half of the same NeoForge 1.21 change — see ServerLevelMixin for the full note,
    // including why 26 leaves one constant here and moves the day time somewhere else entirely.
    //? if neoforge && >=1.21 && <26 {
    /*@com.llamalad7.mixinextras.injector.ModifyExpressionValue(
            method = "Lnet/minecraft/client/multiplayer/ClientLevel;tickTime()V",
            remap = CitadelConstants.REMAPREFS,
            at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;advanceDaytime()J"))
    private long citadel_advanceDaytime(long original) {
        return ClientTickRateTracker.getForClient(Minecraft.getInstance()).getDayTimeIncrement(original);
    }
    *///?}
}
