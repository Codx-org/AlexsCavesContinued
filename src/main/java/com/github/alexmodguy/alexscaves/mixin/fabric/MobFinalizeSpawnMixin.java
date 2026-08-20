package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living.MobSpawnEvent;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric's producer for {@code MobSpawnEvent.FinalizeSpawn} — the event {@code CommonEvents#
 * onEntityJoinWorld} uses to teach creepers to avoid raycats, to dress abyssal-chasm drowned in
 * diving gear, and to give foxes their fox behaviour.
 *
 * <p><b>Why HEAD of the method rather than the call sites.</b> Forge fires its hook from each of the
 * ~8 vanilla places that call {@code finalizeSpawn} (natural spawning, spawn eggs, spawners, structure
 * templates, …), immediately <i>before</i> the call. Injecting at HEAD of the one method they all funnel
 * into reproduces that position exactly and covers every caller — including any a future MC version
 * adds — for one injection point instead of eight moving ones. It matters that it is HEAD and not
 * RETURN: {@code Drowned#finalizeSpawn} is what fills its own equipment slots, and the handler's
 * diving-gear branch is written to run while those slots are still empty.
 *
 * <p><b>Why the selector carries no descriptor.</b> {@code finalizeSpawn} has three signatures over this
 * range — a trailing {@code CompoundTag} below 1.20.5, {@code MobSpawnType} up to 1.21.1 and
 * {@code EntitySpawnReason} from 1.21.2 — but {@code Mob} declares exactly <b>one</b> overload on every
 * one of the 22 Fabric nodes, so a name-only selector matches it on all of them and the whole
 * three-band gate disappears. Nothing this handler reads comes from the parameters, so none of them is
 * captured.
 *
 * <p>The event is not cancellable in this tree's stand-in (Forge's is, through its {@code Result}), and
 * no listener here would cancel it; the mod only ever adds goals and equipment.
 */
@Mixin(Mob.class)
public class MobFinalizeSpawnMixin {

    @Inject(method = "finalizeSpawn", at = @At("HEAD"))
    private void ac_finalizeSpawn(CallbackInfoReturnable<?> cir) {
        MinecraftForge.EVENT_BUS.post(new MobSpawnEvent.FinalizeSpawn((Mob) (Object) this));
    }
}
