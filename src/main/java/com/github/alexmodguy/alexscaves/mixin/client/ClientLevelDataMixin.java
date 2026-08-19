package com.github.alexmodguy.alexscaves.mixin.client;


import com.github.alexmodguy.alexscaves.AlexsCaves;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.ClientLevelData.class)
public abstract class ClientLevelDataMixin  {

    @Inject(method = "Lnet/minecraft/client/multiplayer/ClientLevel$ClientLevelData;getHorizonHeight(Lnet/minecraft/world/level/LevelHeightAccessor;)D",
            at = @At(
                    value = "HEAD"
            ),
            cancellable = true)
    private void ac_getSkyDarken_timeOfDay(LevelHeightAccessor heightAccessor, CallbackInfoReturnable<Double> cir) {
        if (AlexsCaves.CLIENT_CONFIG.biomeSkyOverrides.get()) {
            // The parentheses are load-bearing: from 1.21.2 the !mc2102-maxbuildheight replacement
            // rule expands the call below into a call plus one, and unary minus binds tighter than
            // that plus. (Naming either accessor in this comment would rewrite the comment too.)
            cir.setReturnValue((double) -(heightAccessor.getMaxBuildHeight()));

        }
    }
}
