package com.github.alexmodguy.alexscaves.mixin.client;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.BiomeAmbientSoundsHandler;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BiomeAmbientSoundsHandler.class)
public abstract class BiomeAmbientSoundsHandlerMixin  {


    // 1.21.11 rebuilt this handler on the environment-attribute system: the BiomeManager field is gone
    // (the ambient loop is read off the level at the player's position now, so neighbouring biomes blend),
    // loopSounds is keyed by the sound holder rather than by the Biome, and Biome#getAmbientLoop went with
    // the rest of the per-biome accessors. A @Shadow is matched by name AND descriptor against the target
    // class alone, so all three declarations have to move with the body.
    //? if >=1.21.11 {
    /*@Shadow @Final private LocalPlayer player;

    @Shadow @Final private Object2ObjectArrayMap<net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent>, BiomeAmbientSoundsHandler.LoopSoundInstance> loopSounds;

    @Inject(method = "Lnet/minecraft/client/resources/sounds/BiomeAmbientSoundsHandler;tick()V",
            at = @At("TAIL"))
    private void ac_tick(CallbackInfo ci) {
        boolean hasLoop = this.player.level().environmentAttributes()
                .getValue(net.minecraft.world.attribute.EnvironmentAttributes.AMBIENT_SOUNDS, this.player.blockPosition())
                .loop().isPresent();
        if(hasLoop && !player.isAlive()){
            //fixes biome loop sounds playing after death and respawn
            this.loopSounds.values().forEach(BiomeAmbientSoundsHandler.LoopSoundInstance::fadeOut);
        }
    }
    *///?} else {
    @Shadow @Final private BiomeManager biomeManager;

    @Shadow @Final private LocalPlayer player;

    @Shadow @Final private Object2ObjectArrayMap<Biome, BiomeAmbientSoundsHandler.LoopSoundInstance> loopSounds;

    @Inject(method = "Lnet/minecraft/client/resources/sounds/BiomeAmbientSoundsHandler;tick()V",
            at = @At("TAIL"))
    private void ac_tick(CallbackInfo ci) {
        Biome biome = this.biomeManager.getNoiseBiomeAtPosition(this.player.getX(), this.player.getY(), this.player.getZ()).value();
        if(biome.getAmbientLoop().isPresent() && !player.isAlive()){
            //fixes biome loop sounds playing after death and respawn
            this.loopSounds.values().forEach(BiomeAmbientSoundsHandler.LoopSoundInstance::fadeOut);
        }
    }
    //?}
}
