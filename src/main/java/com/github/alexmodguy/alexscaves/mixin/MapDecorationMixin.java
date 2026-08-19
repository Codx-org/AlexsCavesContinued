package com.github.alexmodguy.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.misc.ACVanillaMapUtil;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MapDecoration.class)
public abstract class MapDecorationMixin {

    // 1.20.2 turned MapDecoration into a record, so the accessor lost its get prefix.
    //? if <1.20.2
    @Shadow public abstract MapDecoration.Type getType();
    //? if >=1.20.2
    /*@Shadow public abstract MapDecoration.Type type();*/

    // render(int) is not vanilla — it is the loader's hook for drawing a modded decoration itself,
    // and only some builds carry it. Forge 1.20.1 has it and so does NeoForge 1.20.2-1.20.4, but
    // Forge dropped the patch when MapDecoration became a record, so on 1.20.4-forge there is
    // nothing to inject into and the cabin marker simply falls back to the vanilla icon. 1.20.5
    // replaces the whole Type enum with a MapDecorationType registry, at which point this feature
    // gets a vanilla-native implementation and this arm goes away.
    // Fabric never had it at all, so every Fabric node below 1.20.5 takes the same fallback
    // 1.20.4-forge does: the marker draws with the vanilla icon. There is no vanilla-portable
    // substitute — MapRenderer$MapInstance.draw short-circuits renderOnFrame() outside a frame, and a
    // getImage() redirect cannot skip the vanilla draw.
    //? if (!fabric && <1.20.2) || (neoforge && <1.20.5) {
    @Inject(
            method = {"Lnet/minecraft/world/level/saveddata/maps/MapDecoration;render(I)Z"},
            remap = false, //LOADER METHOD
            cancellable = true,
            at = @At(value = "HEAD")
    )
    private void ac_render(int index, CallbackInfoReturnable<Boolean> cir) {
        if(this.ac_type() == ACVanillaMapUtil.UNDERGROUND_CABIN_MAP_DECORATION){
            AlexsCaves.PROXY.renderVanillaMapDecoration((MapDecoration)(Object)this, index);
            cir.setReturnValue(true);
        }
    }
    //?}

    @Unique
    private MapDecoration.Type ac_type() {
        //? if <1.20.2
        return this.getType();
        //? if >=1.20.2
        /*return this.type();*/
    }
}
