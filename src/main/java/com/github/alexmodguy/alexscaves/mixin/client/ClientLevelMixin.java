package com.github.alexmodguy.alexscaves.mixin.client;


import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.ClientProxy;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin extends Level {

    // 1.21.2 dropped the Supplier<ProfilerFiller> from Level's constructor — the profiler is
    // reached through Profiler.get() now. A mixin's own constructor is discarded at apply time, so
    // this only has to keep the compiler happy against whichever Level is on the classpath.
    //? if >=1.21.2 {
    /*protected ClientLevelMixin(WritableLevelData p_270739_, ResourceKey<Level> p_270683_, RegistryAccess p_270200_, Holder<DimensionType> p_270240_, boolean p_270904_, boolean p_270470_, long p_270248_, int p_270466_) {
        super(p_270739_, p_270683_, p_270200_, p_270240_, p_270904_, p_270470_, p_270248_, p_270466_);
    }
    *///?} else {
    protected ClientLevelMixin(WritableLevelData p_270739_, ResourceKey<Level> p_270683_, RegistryAccess p_270200_, Holder<DimensionType> p_270240_, Supplier<ProfilerFiller> p_270692_, boolean p_270904_, boolean p_270470_, long p_270248_, int p_270466_) {
        super(p_270739_, p_270683_, p_270200_, p_270240_, p_270692_, p_270904_, p_270470_, p_270248_, p_270466_);
    }
    //?}

    //use the "time of day" to get daytime independent sky of cave biomes.
    // 1.21.2 packs the sky colour into an ARGB int instead of returning a Vec3. The lerp is the same
    // one, done per channel and back through the same packing vanilla now uses.
    // 1.21.11 deletes both getSkyColor and getSkyDarken: the sky is an EnvironmentAttributes lookup
    // now. Both arms move to mixin.client.EnvironmentAttributeProbeMixin, which covers the same set
    // of callers in one injection — see that class. Hence the two empty arms here.
    //? if >=1.21.11 {
    /*
    *///?} elif >=1.21.2 {
    /*@Inject(method = "Lnet/minecraft/client/multiplayer/ClientLevel;getSkyColor(Lnet/minecraft/world/phys/Vec3;F)I",
            at = @At(
                    value = "RETURN"
            ),
            cancellable = true)
    private void ac_getSkyColor_timeOfDay(Vec3 position, float partialTick, CallbackInfoReturnable<Integer> cir) {
        if (AlexsCaves.CLIENT_CONFIG.biomeSkyOverrides.get()) {
            if (ClientProxy.acSkyOverrideAmount > 0.0F) {
                int packed = cir.getReturnValue();
                Vec3 prevVec3 = new Vec3(
                        net.minecraft.util.ARGB.red(packed) / 255.0D,
                        net.minecraft.util.ARGB.green(packed) / 255.0D,
                        net.minecraft.util.ARGB.blue(packed) / 255.0D);
                Vec3 sampledVec3 = ClientProxy.acSkyOverrideColor;
                sampledVec3 = ClientProxy.processSkyColor(sampledVec3, partialTick);
                Vec3 mixed = prevVec3.add(sampledVec3.subtract(prevVec3).scale(ClientProxy.acSkyOverrideAmount));
                cir.setReturnValue(net.minecraft.util.ARGB.colorFromFloat(
                        net.minecraft.util.ARGB.alpha(packed) / 255.0F,
                        (float) mixed.x, (float) mixed.y, (float) mixed.z));
            }
        }
    }
    *///?} else {
    @Inject(method = "Lnet/minecraft/client/multiplayer/ClientLevel;getSkyColor(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;",
            at = @At(
                    value = "RETURN"
            ),
            cancellable = true)
    private void ac_getSkyColor_timeOfDay(Vec3 position, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (AlexsCaves.CLIENT_CONFIG.biomeSkyOverrides.get()) {
            if (ClientProxy.acSkyOverrideAmount > 0.0F) {
                Vec3 prevVec3 = cir.getReturnValue();
                Vec3 sampledVec3 = ClientProxy.acSkyOverrideColor;
                sampledVec3 = ClientProxy.processSkyColor(sampledVec3, partialTick);
                cir.setReturnValue(prevVec3.add(sampledVec3.subtract(prevVec3).scale(ClientProxy.acSkyOverrideAmount)));
            }
        }
    }
    //?}

    //? if <1.21.11 {
    @Inject(method = "Lnet/minecraft/client/multiplayer/ClientLevel;getSkyDarken(F)F",
            at = @At(
                    value = "RETURN"
            ),
            cancellable = true)
    private void ac_getSkyDarken_timeOfDay(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (AlexsCaves.CLIENT_CONFIG.biomeSkyOverrides.get()) {
            float skyDarken = cir.getReturnValue();
            if (ClientProxy.acSkyOverrideAmount > 0.0F) {
                cir.setReturnValue(Math.max(skyDarken, ClientProxy.acSkyOverrideAmount));
            }
        }
    }
    //?}
}
