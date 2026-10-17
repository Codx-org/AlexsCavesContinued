package com.github.alexmodguy.alexscaves.mixin.fabric.client;

import com.github.alexmodguy.alexscaves.server.block.blockentity.ACUpdatePacketReceiver;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

/**
 * Fabric's stand-in for the Forge patch that routes a block-entity update packet through
 * {@code BlockEntity#onDataPacket} — see {@link ACUpdatePacketReceiver} for why the two are not
 * interchangeable with vanilla's plain {@code load}.
 *
 * <p>The obvious anchor, the {@code load} call this replaces, is inside the {@code ifPresent}
 * lambda and would have to be selected as a {@code lambda$handleBlockEntityData$N} synthetic —
 * an index that is not an API and shifts without warning. The {@code getBlockEntity} call one
 * statement earlier is in the enclosing method itself, so redirecting <em>that</em> and handing
 * back an empty {@code Optional} skips the lambda wholesale: our block entity is served here and
 * vanilla's body never runs for it. Every other block entity, the command block included, sees the
 * real {@code Optional} and is untouched.
 *
 * <p>The {@code Connection} is passed as {@code null}. Forge hands over the live one, but none of
 * the nine implementations reads it, and reaching it here would mean shadowing a field that moved
 * onto {@code ClientCommonPacketListenerImpl} in 1.20.2 for no gain.
 *
 * <p>The handler's type parameter is spelled {@code BlockEntityType<BlockEntity>} rather than the
 * more natural {@code <?>} because a wildcard makes {@code getBlockEntity} return an
 * {@code Optional} of a capture variable, which will not assign. Erasure is what a redirect's
 * descriptor is matched against, so the two spellings are indistinguishable to Mixin.
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Redirect(
            method = "handleBlockEntityData",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntityType;)Ljava/util/Optional;"
            )
    )
    private Optional<BlockEntity> ac_dispatchUpdatePacket(ClientLevel level, BlockPos pos, BlockEntityType<BlockEntity> type, ClientboundBlockEntityDataPacket packet) {
        Optional<BlockEntity> found = level.getBlockEntity(pos, type);
        if (found.isPresent() && found.get() instanceof ACUpdatePacketReceiver receiver) {
            // The argument list tracks ACUpdatePacketReceiver's, which tracks the loader patch's —
            // see that interface for the four bands. Only the middle one differs on Fabric: the
            // version-scoped !mc205-be-datapacket rule adds a HolderLookup.Provider on every loader,
            // where the two 1.21.6 rules are loader-scoped and leave Fabric on the original shape.
            // A block entity cannot supply that provider itself (its level is still null while it
            // loads, which is what ACCompat.BE_REGISTRIES exists for), but a dispatcher can: the
            // level is right here, and it is the same RegistryAccess the loader patch hands over.
            //? if <1.20.5 || >=1.21.6
            receiver.onDataPacket(null, packet);
            //? if >=1.20.5 && <1.21.6
            /*receiver.onDataPacket(null, packet, level.registryAccess());*/
            return Optional.empty();
        }
        return found;
    }
}
