package com.github.alexmodguy.alexscaves.mixin.citadel;

import com.github.alexmodguy.alexscaves.citadel.CitadelConstants;
import com.github.alexmodguy.alexscaves.citadel.CitadelProxy;
import com.github.alexmodguy.alexscaves.citadel.server.world.ModifiableTickRateServer;
import com.mojang.datafixers.DataFixer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.Proxy;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements ModifiableTickRateServer {

    private long modifiedMsPerTick = -1;
    private long masterMs;

    // 26 grew the constructor at both ends: an Optional<GameRules> in fifth place (the per-world rule
    // overrides a `/create`d world can carry) and a trailing `propagatesCrashes` boolean handed to the
    // ReentrantBlockableEventLoop super. Nothing this handler reads moves — it only needs `this` — but
    // an @Inject is matched by descriptor, so the whole list has to be restated.
    //
    // 26.2 grows it once more, by a trailing NotificationManager (the server-side half of the new
    // notification system). Same story: nothing here reads it, but the descriptor has to say so.
    //? if >=26.2 {
    /*@Inject(
            method = "Lnet/minecraft/server/MinecraftServer;<init>(Ljava/lang/Thread;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/server/WorldStem;Ljava/util/Optional;Ljava/net/Proxy;Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/server/Services;Lnet/minecraft/server/level/progress/ChunkProgressListenerFactory;ZLnet/minecraft/server/notifications/NotificationManager;)V",
            at = @At("TAIL")
    )
    private void citadel_init(Thread thread, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, java.util.Optional<?> gameRules, Proxy proxy, DataFixer dataFixer, Services services, ChunkProgressListenerFactory chunkProgressListenerFactory, boolean propagatesCrashes, net.minecraft.server.notifications.NotificationManager notificationManager, CallbackInfo ci) {
        CitadelProxy.setMinecraftServer((MinecraftServer) (Object) (this));
    }
    *///?} elif >=26 {
    /*@Inject(
            method = "Lnet/minecraft/server/MinecraftServer;<init>(Ljava/lang/Thread;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/server/WorldStem;Ljava/util/Optional;Ljava/net/Proxy;Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/server/Services;Lnet/minecraft/server/level/progress/ChunkProgressListenerFactory;Z)V",
            at = @At("TAIL")
    )
    private void citadel_init(Thread thread, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, java.util.Optional<?> gameRules, Proxy proxy, DataFixer dataFixer, Services services, ChunkProgressListenerFactory chunkProgressListenerFactory, boolean propagatesCrashes, CallbackInfo ci) {
        CitadelProxy.setMinecraftServer((MinecraftServer) (Object) (this));
    }
    *///?} else {
    @Inject(
            method = "Lnet/minecraft/server/MinecraftServer;<init>(Ljava/lang/Thread;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/server/WorldStem;Ljava/net/Proxy;Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/server/Services;Lnet/minecraft/server/level/progress/ChunkProgressListenerFactory;)V",
            at = @At("TAIL")
    )
    private void citadel_init(Thread thread, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, Proxy proxy, DataFixer dataFixer, Services services, ChunkProgressListenerFactory chunkProgressListenerFactory, CallbackInfo ci) {
        CitadelProxy.setMinecraftServer((MinecraftServer) (Object) (this));
    }
    //?}

    // Anchored on the tick itself rather than on the metrics call that used to precede it: 1.21.2
    // replaced startMetricsRecordingTick with a try-with-resources Profiler scope, so the old anchor
    // simply is not there any more. tickServer is the thing this wants to run just before anyway,
    // and it is the same call on every version in the matrix.
    //
    // 1.21.11 then moved the tick itself one frame down: runServer opens a Profiler scope and calls
    // the new processPacketsAndTick(boolean), which is what calls tickServer. So the loop body is
    // still entered exactly once per tick from runServer — only the callee's name and descriptor
    // change, and the anchor follows it there.
    //? if >=1.21.11 {
    /*@Inject(
            method = {"Lnet/minecraft/server/MinecraftServer;runServer()V"},
            remap = CitadelConstants.REMAPREFS,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;processPacketsAndTick(Z)V",
                    shift = At.Shift.BEFORE
            )
    )
    protected void citadel_beforeServerTick(CallbackInfo ci) {
        masterTick();
    }
    *///?} else {
    @Inject(
            method = {"Lnet/minecraft/server/MinecraftServer;runServer()V"},
            remap = CitadelConstants.REMAPREFS,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;tickServer(Ljava/util/function/BooleanSupplier;)V",
                    shift = At.Shift.BEFORE
            )
    )
    protected void citadel_beforeServerTick(CallbackInfo ci) {
        masterTick();
    }
    //?}

    private void masterTick() {
        masterMs += 50L;
    }

    // How the server-wide tick length is changed splits at 1.20.3, which rewrote runServer's timing
    // to nanoseconds and — more to the point — gave vanilla its own ServerTickRateManager (the /tick
    // command). Below that there is nothing to ask, so the four literal 50L millisecond constants in
    // the loop get rewritten; from 1.20.3 on, asking vanilla is both simpler and better behaved,
    // since the tick-rate manager also tells joining clients about the changed rate.
    //? if <1.20.3 {
    @ModifyConstant(
            method = {"Lnet/minecraft/server/MinecraftServer;runServer()V"},
            remap = CitadelConstants.REMAPREFS,
            constant = @Constant(longValue = 50L),
            expect = 4)
    private long citadel_serverMsPerTick(long value) {
        return modifiedMsPerTick == -1 ? value : modifiedMsPerTick;
    }
    //?}

    @Override
    public void setGlobalTickLengthMs(long msPerTick) {
        // Citadel calls this every server tick, and the 1.20.3+ path broadcasts the new rate to every
        // client, so only act on an actual change. -1 means "back to normal".
        if (modifiedMsPerTick == msPerTick) {
            return;
        }
        modifiedMsPerTick = msPerTick;
        //? if >=1.20.3
        /*((MinecraftServer) (Object) this).tickRateManager().setTickRate(msPerTick <= 0 ? 20.0F : 1000.0F / msPerTick);*/
    }

    @Override
    public long getMasterMs() {
        return masterMs;
    }
}
