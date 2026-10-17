package com.github.alexmodguy.alexscaves.mixin.fabric.client;

import com.github.alexmodguy.alexscaves.fabric.forge.client.event.ComputeFovModifierEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fabric producer for {@link ComputeFovModifierEvent} — the multiplier vanilla applies to the fov
 * while an item is in use, which is how a drawn bow zooms. This mod's only consumer softens that
 * zoom for the dreadbow.
 *
 * <p>This is the one producer in the set that needs no version gate at all.
 * {@code AbstractClientPlayer#getFieldOfViewModifier} exists on every node in the range and merely
 * gained two parameters at 1.21.2 ({@code ()F} to {@code (boolean, float)F}) — and
 * {@code @ModifyReturnValue} capture is all-or-nothing, so a handler that captures none of the
 * target's arguments is legal against both descriptors. The selector is name-only for the same
 * reason: no node declares a second overload.
 *
 * <p>The event is constructed with the vanilla answer in both slots, exactly as Forge does: a
 * listener reads {@code getFovModifier()} for what vanilla decided and writes
 * {@code setNewFovModifier(...)}, so seeding both keeps a no-op listener a no-op.
 */
@Mixin(AbstractClientPlayer.class)
public class FovModifierMixin {

    @ModifyReturnValue(method = "getFieldOfViewModifier", remap = true, at = @At(value = "RETURN"))
    private float ac_fabricComputeFovModifier(float original) {
        ComputeFovModifierEvent event = new ComputeFovModifierEvent((AbstractClientPlayer) (Object) this, original, original);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getNewFovModifier();
    }
}
