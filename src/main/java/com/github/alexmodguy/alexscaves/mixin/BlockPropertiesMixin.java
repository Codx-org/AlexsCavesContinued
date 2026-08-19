package com.github.alexmodguy.alexscaves.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Stamps the id of the block being registered onto its {@code Properties}, so that 1.21.2's two
 * eager reads of it — the loot table and the description id, both derived from the id the moment
 * {@code BlockBehaviour}'s constructor runs — resolve instead of throwing "Block id not set".
 *
 * <p>Injecting into these two getters rather than into {@code setId} is deliberate: a
 * {@code Properties} constant in {@code ACBlockRegistry} is shared by a dozen blocks, so the id is
 * only correct for the length of one construction. See {@link
 * com.github.alexmodguy.alexscaves.server.misc.ACRegistryIds}.
 *
 * <p>Empty below 1.21.2, where {@code Properties} carries no id at all.
 */
@Mixin(BlockBehaviour.Properties.class)
public class BlockPropertiesMixin {

    //? if >=1.21.2 {
    /*@org.spongepowered.asm.mixin.Shadow
    private net.minecraft.resources.ResourceKey<net.minecraft.world.level.block.Block> id;

    @org.spongepowered.asm.mixin.injection.Inject(method = "effectiveDrops", at = @org.spongepowered.asm.mixin.injection.At("HEAD"))
    private void ac_stampIdForDrops(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<java.util.Optional<net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable>>> cir) {
        ac_stampPendingId();
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "effectiveDescriptionId", at = @org.spongepowered.asm.mixin.injection.At("HEAD"))
    private void ac_stampIdForDescription(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<String> cir) {
        ac_stampPendingId();
    }

    @org.spongepowered.asm.mixin.Unique
    @SuppressWarnings("unchecked")
    private void ac_stampPendingId() {
        net.minecraft.resources.ResourceKey<?> pending = com.github.alexmodguy.alexscaves.server.misc.ACRegistryIds.pending();
        if (pending != null && pending.isFor(net.minecraft.core.registries.Registries.BLOCK)) {
            this.id = (net.minecraft.resources.ResourceKey<net.minecraft.world.level.block.Block>) pending;
        }
    }
    *///?}
}
