package com.github.alexmodguy.alexscaves.mixin.citadel;


import com.github.alexmodguy.alexscaves.citadel.Citadel;
import com.github.alexmodguy.alexscaves.citadel.CitadelConstants;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(Level.class)
public class LevelMixin {

    @Shadow
    @Final
    public boolean isClientSide;

    @Inject(at = @At("HEAD"), remap = CitadelConstants.REMAPREFS, cancellable = true,
            method = "Lnet/minecraft/world/level/Level;guardEntityTick(Ljava/util/function/Consumer;Lnet/minecraft/world/entity/Entity;)V")
    private void citadel_guardEntityTick(Consumer<Entity> ticker, Entity entity, CallbackInfo ci) {
        if (!isClientSide) {
            if (!Citadel.PROXY.canEntityTickServer((Level) (Object) this, entity)) {
                ci.cancel();
            }
        } else {
            if (!Citadel.PROXY.canEntityTickClient((Level) (Object) this, entity)) {
                ci.cancel();
            }
        }
    }
}
