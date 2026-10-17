package com.github.alexmodguy.alexscaves.server.misc;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataSerializer;

/**
 * The {@code CompoundTag} entity-data serializer, which vanilla stopped shipping in 1.21.9.
 *
 * <p>Four accessors in this tree sync a {@code CompoundTag}: the magnetron's two block-state/pose
 * maps, the moving-block entity's block data, and Citadel's per-entity data tag. 1.21.9 deleted
 * {@code EntityDataSerializers.COMPOUND_TAG} outright — the vanilla list now carries only typed
 * entries — so the serializer has to be supplied by the mod. It is byte-identical to the one that
 * was removed ({@code ByteBufCodecs.COMPOUND_TAG} over the wire, a deep {@code copy()} on read), so
 * nothing about how those four fields behave changes.
 *
 * <p>A serializer has to be <em>registered</em> before {@code SynchedEntityData.defineId} can name
 * it — {@code defineId} asks for its network id and throws otherwise — which is why this is a
 * registry entry rather than a bare constant. Both loaders keep the registry under the same key
 * name, so the Forge spelling here is rewritten for NeoForge by the {@code !nf-cls-registries} rule
 * like every other registry in the mod.
 *
 * <p>Below 1.21.9 the constant is simply an alias for the vanilla one and {@link #register} does
 * nothing, so the four call sites are version-blind.
 */
public class ACDataSerializers {

    // The register is hoisted out of the >=1.21.9 arm below because the loaders and Fabric build it
    // differently and Stonecutter arms do not nest: on the loaders it is keyed by a loader-owned
    // registry, on Fabric there is no registry to key it by and the serializer goes into vanilla's
    // own static id list instead (see the stand-in's entityDataSerializers factory). Declared first so
    // the entry below still initialises after it, and absent entirely below 1.21.9, where the
    // serializer is vanilla's own and nothing is registered at all.
    //? if fabric && >=1.21.9 {
    /*private static final net.minecraftforge.registries.DeferredRegister<EntityDataSerializer<?>> DEF_REG =
            net.minecraftforge.registries.DeferredRegister.entityDataSerializers(
                    com.github.alexmodguy.alexscaves.AlexsCaves.MODID);
    *///?} elif >=1.21.9 {
    /*private static final net.minecraftforge.registries.DeferredRegister<EntityDataSerializer<?>> DEF_REG =
            net.minecraftforge.registries.DeferredRegister.create(
                    net.minecraftforge.registries.ForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS,
                    com.github.alexmodguy.alexscaves.AlexsCaves.MODID);
    *///?}

    //? if <1.21.9 {
    public static final EntityDataSerializer<CompoundTag> COMPOUND_TAG = net.minecraft.network.syncher.EntityDataSerializers.COMPOUND_TAG;

    public static void register(net.minecraftforge.eventbus.api.IEventBus modEventBus) {
    }
    //?} else {
    /*public static final EntityDataSerializer<CompoundTag> COMPOUND_TAG = new EntityDataSerializer<>() {
        @Override
        public net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, CompoundTag> codec() {
            return net.minecraft.network.codec.ByteBufCodecs.COMPOUND_TAG;
        }

        @Override
        public CompoundTag copy(CompoundTag tag) {
            return tag.copy();
        }
    };

    private static final java.util.function.Supplier<EntityDataSerializer<?>> COMPOUND_TAG_ENTRY =
            DEF_REG.register("compound_tag", () -> COMPOUND_TAG);

    public static void register(net.minecraftforge.eventbus.api.IEventBus modEventBus) {
        DEF_REG.register(modEventBus);
    }
    *///?}
}
