package com.github.alexmodguy.alexscaves.mixin;

import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;

/**
 * The item half of {@link BlockPropertiesMixin}: 1.21.2's {@code Item} constructor reads the
 * description id and the model id off {@code Properties} immediately, both derived from an id that
 * nothing in this mod sets, and both throw "Item id not set" without one.
 */
@Mixin(Item.Properties.class)
public class ItemPropertiesMixin {

    //? if >=1.21.2 {
    /*@org.spongepowered.asm.mixin.Shadow
    private net.minecraft.resources.ResourceKey<Item> id;

    @org.spongepowered.asm.mixin.injection.Inject(method = "effectiveDescriptionId", at = @org.spongepowered.asm.mixin.injection.At("HEAD"))
    private void ac_stampIdForDescription(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<String> cir) {
        ac_stampPendingId();
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "effectiveModel", at = @org.spongepowered.asm.mixin.injection.At("HEAD"))
    private void ac_stampIdForModel(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.resources.ResourceLocation> cir) {
        ac_stampPendingId();
    }

    @org.spongepowered.asm.mixin.Unique
    @SuppressWarnings("unchecked")
    private void ac_stampPendingId() {
        net.minecraft.resources.ResourceKey<?> pending = com.github.alexmodguy.alexscaves.server.misc.ACRegistryIds.pending();
        if (pending != null && pending.isFor(net.minecraft.core.registries.Registries.ITEM)) {
            this.id = (net.minecraft.resources.ResourceKey<Item>) pending;
        }
    }
    *///?}
}
