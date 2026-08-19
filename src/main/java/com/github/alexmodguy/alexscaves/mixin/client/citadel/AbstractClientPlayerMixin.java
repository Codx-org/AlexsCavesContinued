package com.github.alexmodguy.alexscaves.mixin.client.citadel;

import com.github.alexmodguy.alexscaves.citadel.CitadelConstants;
import com.github.alexmodguy.alexscaves.citadel.client.rewards.CitadelCapes;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin extends Player {

    // Only ever called by Mixin's own bookkeeping — a mixin extending a class has to be able to name
    // one of its constructors, and this one is never executed. 1.21.6 dropped the spawn position and
    // yaw from Player's: the entity is positioned by the caller now rather than in the constructor.
    //? if >=1.21.6 {
    /*public AbstractClientPlayerMixin(Level p_250508_, GameProfile p_252153_) {
        super(p_250508_, p_252153_);
    }
    *///?} else {
    public AbstractClientPlayerMixin(Level p_250508_, BlockPos p_250289_, float p_251702_, GameProfile p_252153_) {
        super(p_250508_, p_250289_, p_251702_, p_252153_);
    }
    //?}

    // 1.20.2 folded the two texture getters into one PlayerSkin record returned by getSkin(), so
    // instead of cancelling two lookups there is a single record to rebuild with the cape swapped in.
    //? if <1.20.2 {
    @Inject(at = @At("HEAD"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/client/player/AbstractClientPlayer;getCloakTextureLocation()Lnet/minecraft/resources/ResourceLocation;", cancellable = true)
    private void citadel_getCapeLocation(CallbackInfoReturnable<ResourceLocation> cir) {
        CitadelCapes.Cape cape = CitadelCapes.getCurrentCape(this);
        if (cape != null) {
            cir.setReturnValue(cape.getTexture());
        }
    }

    @Inject(at = @At("HEAD"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/client/player/AbstractClientPlayer;getElytraTextureLocation()Lnet/minecraft/resources/ResourceLocation;", cancellable = true)
    private void citadel_getElytraLocation(CallbackInfoReturnable<ResourceLocation> cir) {
        CitadelCapes.Cape cape = CitadelCapes.getCurrentCape(this);
        if (cape != null) {
            cir.setReturnValue(cape.getTexture());
        }
    }
    //?}

    //? if >=1.20.2 && <1.21.9 {
    /*@Inject(at = @At("RETURN"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/client/player/AbstractClientPlayer;getSkin()Lnet/minecraft/client/resources/PlayerSkin;", cancellable = true)
    private void citadel_getSkin(CallbackInfoReturnable<net.minecraft.client.resources.PlayerSkin> cir) {
        CitadelCapes.Cape cape = CitadelCapes.getCurrentCape(this);
        if (cape != null) {
            net.minecraft.client.resources.PlayerSkin skin = cir.getReturnValue();
            cir.setReturnValue(new net.minecraft.client.resources.PlayerSkin(
                    skin.texture(), skin.textureUrl(), cape.getTexture(), cape.getTexture(),
                    skin.model(), skin.secure()));
        }
    }
    *///?} elif >=1.21.9 {
    /*// 1.21.9 moved PlayerSkin to net.minecraft.world.entity.player and retyped it: the four bare
    // ResourceLocations became three ClientAsset.Textures (body/cape/elytra), the download URL is
    // folded into the asset, and the arm model is a PlayerModelType. A cape is authored here as a
    // plain texture path, so it is wrapped as a ResourceTexture whose id and path are the same
    // location — nothing looks the id up, it only has to be stable for equality.
    @Inject(at = @At("RETURN"), remap = CitadelConstants.REMAPREFS, method = "Lnet/minecraft/client/player/AbstractClientPlayer;getSkin()Lnet/minecraft/world/entity/player/PlayerSkin;", cancellable = true)
    private void citadel_getSkin(CallbackInfoReturnable<net.minecraft.world.entity.player.PlayerSkin> cir) {
        CitadelCapes.Cape cape = CitadelCapes.getCurrentCape(this);
        if (cape != null) {
            net.minecraft.world.entity.player.PlayerSkin skin = cir.getReturnValue();
            net.minecraft.core.ClientAsset.Texture capeAsset =
                    new net.minecraft.core.ClientAsset.ResourceTexture(cape.getTexture(), cape.getTexture());
            cir.setReturnValue(new net.minecraft.world.entity.player.PlayerSkin(
                    skin.body(), capeAsset, capeAsset, skin.model(), skin.secure()));
        }
    }
    *///?}
}
