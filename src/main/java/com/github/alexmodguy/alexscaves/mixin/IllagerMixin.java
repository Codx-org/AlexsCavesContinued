package com.github.alexmodguy.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 1.20.5 pulled isAlliedTo up to AbstractIllager; only Evoker kept an override (for its Vex). Mixin
// resolves a `method =` selector against the target class alone — an inherited match is not a match —
// so naming the four concrete illagers there is a hard "could not find any targets" crash from that
// version on. AbstractIllager covers exactly those four, and Evoker is listed alongside it so its
// override is caught at HEAD too, before the two early returns that precede its super call.
//
// ⚠️ This gate read >=1.20.6 until 1.20.5-fabric was ported, and was wrong by one version the whole
// time — harmlessly, because the two loaders that already walk this range have no 1.20.5 build at
// all, so the boundary had never been evaluated. Every >=1.20.2 / >=1.20.3 / >=1.20.5 gate in the
// tree is in that position; Fabric is the first loader to test any of them.
//? if >=1.20.5 {
/*@Mixin(value = {
        net.minecraft.world.entity.monster.AbstractIllager.class,
        net.minecraft.world.entity.monster.Evoker.class,
})
*///?} else {
@Mixin(value = {
        Pillager.class,
        Vindicator.class,
        Evoker.class,
        Illusioner.class,
})
//?}
public abstract class IllagerMixin {

    // 1.21.2 made Entity#isAlliedTo(Entity) final — it now asks both entities in turn through the new
    // overridable considersEntityAsAlly, which is where the illagers' logic moved. Asking from the
    // illager's side only is what this always did, so the rename is the whole change.
    @Inject(
            //? if >=1.21.2 {
            /*method = "considersEntityAsAlly(Lnet/minecraft/world/entity/Entity;)Z",
            *///?} else {
            method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z",
            //?}
            at = @At(value = "HEAD"),
            cancellable = true,
            remap = true
    )
    private void ac_isAlliedTo(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (isPossessed((Entity) (Object) this) || (other != null && isPossessed(other))) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static boolean isPossessed(Entity e) {
        return ACCompat.getBoolean(ACCompat.getPersistentData(e), "TotemPossessed");
    }
}
