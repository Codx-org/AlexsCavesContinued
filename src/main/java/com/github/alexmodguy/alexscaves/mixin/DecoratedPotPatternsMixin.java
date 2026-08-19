package com.github.alexmodguy.alexscaves.mixin;

import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import org.spongepowered.asm.mixin.Mixin;

// 26.2 replaced the mutable ITEM_TO_POT_TEXTURE map with a static enumeration —
// DecoratedPotPatterns#itemToPatternMappings(BiConsumer) — so a mod's sherds are appended to that
// enumeration rather than put into a map. TAIL is the whole of it: the method's only body is
// twenty-four accept calls, and the one consumer in the jar (the client DecoratedPotRenderer)
// builds its lookup from whatever arrives.
//
// The target class exists on every node, but the method does not, so the injection lives in an arm
// and this is an empty (harmless) mixin below 26.2 — the file is listed in alexscaves.mixins.json
// unconditionally, and an @Inject naming a missing method is a hard apply failure with
// defaultRequire: 1. Same shape, and the same reason, as SurfaceRulesContextAccessor.
@Mixin(DecoratedPotPatterns.class)
public class DecoratedPotPatternsMixin {

    //? if >=26.2 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "itemToPatternMappings", at = @org.spongepowered.asm.mixin.injection.At("TAIL"))
    private static void ac_itemToPatternMappings(java.util.function.BiConsumer<net.minecraft.resources.ResourceKey<net.minecraft.world.item.Item>, net.minecraft.resources.ResourceKey<net.minecraft.world.level.block.entity.DecoratedPotPattern>> consumer, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        com.github.alexmodguy.alexscaves.server.misc.ACPotPatternRegistry.contributeItemToPatternMappings(consumer);
    }
    *///?}
}
