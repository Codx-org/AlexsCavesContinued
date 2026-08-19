package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.server.entity.util.MinecartAccessor;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.RidingMinecartSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RidingMinecartSoundInstance.class)
public abstract class RidingMinecartSoundInstanceMixin extends AbstractTickableSoundInstance {

    @Shadow
    @Final
    private AbstractMinecart minecart;

    protected RidingMinecartSoundInstanceMixin(SoundEvent soundEvent, SoundSource soundSource, RandomSource randomSource) {
        super(soundEvent, soundSource, randomSource);
    }

    // 1.21.11 pulled the whole tick body up into a new RidingEntitySoundInstance superclass, so
    // there is no tick() on this class to inject into any more — and a @Inject cannot reach an
    // inherited method. What it left behind is the one hook the base tick asks this subclass for:
    // shoudlPlaySound() (vanilla's spelling), whose false answer sets volume to volumeMin. Both
    // instances LocalPlayer constructs pass volumeMin = 0.0F, so answering false here is exactly
    // the old "volume = 0" — and it lets the base tick still stop() the instance on dismount,
    // which the old HEAD-cancel skipped.
    //? if >=1.21.11 {
    /*@Inject(
            method = {"Lnet/minecraft/client/resources/sounds/RidingMinecartSoundInstance;shoudlPlaySound()Z"},
            remap = true,
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void ac_shoudlPlaySound(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (((MinecartAccessor) minecart).isOnMagLevRail()) {
            cir.setReturnValue(false);
        }
    }
    *///?} else {
    @Inject(
            method = {"Lnet/minecraft/client/resources/sounds/RidingMinecartSoundInstance;tick()V"},
            remap = true,
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void ac_tick(CallbackInfo ci) {
        if (((MinecartAccessor) minecart).isOnMagLevRail()) {
            volume = 0.0F;
            ci.cancel();
        }
    }
    //?}
}
