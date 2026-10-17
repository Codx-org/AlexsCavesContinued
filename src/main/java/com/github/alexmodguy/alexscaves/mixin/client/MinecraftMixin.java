package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.client.ClientProxy;
import com.github.alexmodguy.alexscaves.client.sound.ACMusics;
import com.github.alexmodguy.alexscaves.server.entity.util.PossessesCamera;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(value = Minecraft.class, priority = -100)
public abstract class MinecraftMixin {

    @Shadow
    @Nullable
    public abstract Entity getCameraEntity();

    @Shadow @Nullable public LocalPlayer player;

    @Shadow @Final public Gui gui;

    @Inject(method = "Lnet/minecraft/client/Minecraft;startAttack()Z",
            at = @At("HEAD"),
            cancellable = true)
    private void ac_startAttack(CallbackInfoReturnable<Boolean> cir) {
        if (getCameraEntity() instanceof PossessesCamera) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "Lnet/minecraft/client/Minecraft;startUseItem()V",
            at = @At("HEAD"),
            cancellable = true)
    private void ac_startUseItem(CallbackInfo ci) {
        if (getCameraEntity() instanceof PossessesCamera) {
            ci.cancel();
        }
    }

    // How long a client tick lasts, which is how the vendored Citadel slow-motion works. Through
    // 1.20.6 the tracker wrote the number straight into the timer's msPerTick field; 1.21 made that
    // field final and gave the timer a provider to ask instead, so the multiplier has to be applied
    // where the answer is produced. getClientTickRate() returns milliseconds against a 50 ms tick,
    // so it becomes a ratio here and leaves whatever the tick-rate manager decided intact.
    //? if >=1.21 {
    /*@Inject(method = "getTickTargetMillis", at = @At("RETURN"), cancellable = true)
    private void ac_getTickTargetMillis(float defaultValue, CallbackInfoReturnable<Float> cir) {
        float clientRate = com.github.alexmodguy.alexscaves.citadel.client.tick.ClientTickRateTracker
                .getForClient(Minecraft.getInstance()).getClientTickRate();
        if (clientRate != 50.0F) {
            cir.setReturnValue(cir.getReturnValueF() * (clientRate / 50.0F));
        }
    }

    *///?}
    // The modern half of ACClientCompat#runAsFancy used to live here, as a >=1.21.11 arm forcing
    // Minecraft#useShaderTransparency. 26.2 moved that query onto GameRenderState, so it is a mixin
    // of its own now — see ShaderTransparencyMixin, which carries both spellings.

    // 1.21.4 rewrote this return type: a biome now offers a SimpleWeightedRandomList<Music> and the
    // method answers a MusicInfo, i.e. a track plus the biome's music volume. The 1.21.4 arm mirrors
    // what vanilla does with those two — one random draw from the list, the biome's own volume —
    // rather than approximating it, and the boss track keeps MusicInfo's implicit full volume.
    //
    // 1.21.11 undoes the MusicInfo half: getSituationalMusic answers a plain Music again and the
    // volume is a separate getMusicVolume(). What replaced the biome accessors is the environment
    // attribute system — BACKGROUND_MUSIC holds a BackgroundMusic record of default/creative/
    // underwater tracks, and vanilla picks between them with select(creative, underwater). The
    // 1.21.11 arm reads the same attribute off the level at the player's position (the level reader
    // folds the dimension's own value in under the biome's, which is what the camera probe vanilla
    // uses does) and makes the same selection, so a tagged biome behaves exactly as vanilla would
    // have, only forced past whatever vanilla would otherwise have chosen.
    //? if >=1.21.11 {
    /*@Inject(method = "Lnet/minecraft/client/Minecraft;getSituationalMusic()Lnet/minecraft/sounds/Music;",
            at = @At("HEAD"),
            cancellable = true)
    private void ac_getSituationalMusic(CallbackInfoReturnable<Music> cir) {
        if(this.player != null){
            if(this.gui.getBossOverlay() != null && this.gui.getBossOverlay().shouldPlayMusic() && ClientProxy.primordialBossActive){
                cir.setReturnValue(ACMusics.luxtructosaurusBossMusic());
            }else{
                Holder<Biome> holder = this.player.level().getBiome(this.player.blockPosition());
                if(holder.is(ACTagRegistry.OVERRIDE_ALL_VANILLA_MUSIC_IN)){
                    net.minecraft.world.attribute.BackgroundMusic background = this.player.level().environmentAttributes()
                            .getValue(net.minecraft.world.attribute.EnvironmentAttributes.BACKGROUND_MUSIC, this.player.blockPosition());
                    boolean creative = this.player.getAbilities().instabuild && this.player.getAbilities().mayfly;
                    cir.setReturnValue(background.select(creative, this.player.isUnderWater()).orElse(Musics.GAME));
                }
            }
        }
    }
    *///?} elif >=1.21.4 {
    /*@Inject(method = "Lnet/minecraft/client/Minecraft;getSituationalMusic()Lnet/minecraft/client/sounds/MusicInfo;",
            at = @At("HEAD"),
            cancellable = true)
    private void ac_getSituationalMusic(CallbackInfoReturnable<net.minecraft.client.sounds.MusicInfo> cir) {
        if(this.player != null){
            if(this.gui.getBossOverlay() != null && this.gui.getBossOverlay().shouldPlayMusic() && ClientProxy.primordialBossActive){
                cir.setReturnValue(new net.minecraft.client.sounds.MusicInfo(ACMusics.luxtructosaurusBossMusic()));
            }else{
                Holder<Biome> holder = this.player.level().getBiome(this.player.blockPosition());
                if(holder.is(ACTagRegistry.OVERRIDE_ALL_VANILLA_MUSIC_IN)){
                    Music music = holder.value().getBackgroundMusic()
                            .flatMap(list -> list.getRandomValue(this.player.level().getRandom()))
                            .orElse(Musics.GAME);
                    cir.setReturnValue(new net.minecraft.client.sounds.MusicInfo(music, holder.value().getBackgroundMusicVolume()));
                }
            }
        }
    }
    *///?} else {
    @Inject(method = "Lnet/minecraft/client/Minecraft;getSituationalMusic()Lnet/minecraft/sounds/Music;",
            at = @At("HEAD"),
            cancellable = true)
    private void ac_getSituationalMusic(CallbackInfoReturnable<Music> cir) {
        if(this.player != null){
            if(this.gui.getBossOverlay() != null && this.gui.getBossOverlay().shouldPlayMusic() && ClientProxy.primordialBossActive){
                cir.setReturnValue(ACMusics.luxtructosaurusBossMusic());
            }else{
                Holder<Biome> holder = this.player.level().getBiome(this.player.blockPosition());
                if(holder.is(ACTagRegistry.OVERRIDE_ALL_VANILLA_MUSIC_IN)){
                    cir.setReturnValue(holder.value().getBackgroundMusic().orElse(Musics.GAME));
                }
            }
        }
    }
    //?}
}
