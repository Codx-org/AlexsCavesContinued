package com.github.alexmodguy.alexscaves.server.entity;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.entity.util.GummyColors;
import com.github.alexmodguy.alexscaves.server.misc.ACMath;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
//? if !fabric
import net.minecraftforge.registries.ForgeRegistries;
import java.util.function.Supplier;

import java.util.Optional;

public class ACEntityDataRegistry {

    // A serializer is registry content on the other two loaders, under a registry the loader owns and
    // vanilla has never had. On Fabric the list it has to reach is the static incremental id map in
    // EntityDataSerializers, which is not a registry at all — so there is no key to hand create() and
    // the difference lives in the factory call, exactly as it does for the fluid types. The flush line
    // in AlexsCaves's constructor is shared either way.
    //? if fabric {
    /*public static final DeferredRegister<EntityDataSerializer<?>> DEF_REG = DeferredRegister.entityDataSerializers(AlexsCaves.MODID);
    *///?} else {
    public static final DeferredRegister<EntityDataSerializer<?>> DEF_REG = DeferredRegister.create(ForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, AlexsCaves.MODID);
    //?}
    // 1.20.5 rebuilt entity data serializers on StreamCodec and dropped the two convenience
    // factories these used. The wire format is the same in both arms — an optional Vec3 and an
    // enum ordinal — only the plumbing that describes it changed.
    //? if >=1.20.5 {
    /*public static final Supplier<EntityDataSerializer<Optional<Vec3>>> OPTIONAL_VEC_3 = DEF_REG.register("optional_vec_3", () -> EntityDataSerializer.forValueType(
            net.minecraft.network.codec.ByteBufCodecs.optional(
                    net.minecraft.network.codec.StreamCodec.<net.minecraft.network.RegistryFriendlyByteBuf, Vec3>of(ACMath::writeVec3, ACMath::readVec3))));
    public static final Supplier<EntityDataSerializer<GummyColors>> GUMMY_COLOR = DEF_REG.register("gummy_color", () -> EntityDataSerializer.forValueType(
            net.minecraft.network.codec.StreamCodec.<net.minecraft.network.RegistryFriendlyByteBuf, GummyColors>of(
                    (buf, value) -> buf.writeEnum(value), buf -> buf.readEnum(GummyColors.class))));
    *///?} else {
    public static final Supplier<EntityDataSerializer<Optional<Vec3>>> OPTIONAL_VEC_3 = DEF_REG.register("optional_vec_3", () -> EntityDataSerializer.optional(ACMath::writeVec3, ACMath::readVec3));
    public static final Supplier<EntityDataSerializer<GummyColors>> GUMMY_COLOR = DEF_REG.register("gummy_color", () -> EntityDataSerializer.simpleEnum(GummyColors.class));
    //?}

    // 1.21.5 deleted EntityDataSerializers.OPTIONAL_UUID: vanilla now syncs "which entity" as an
    // EntityReference instead. The mod's 21 accessors want a plain Optional<UUID>, so it registers
    // the very same serializer itself — an optional 128-bit UUID, byte-for-byte what vanilla wrote —
    // and the accessor declarations read the same on every node.
    //? if >=1.21.5 {
    /*public static final Supplier<EntityDataSerializer<Optional<java.util.UUID>>> OPTIONAL_UUID = DEF_REG.register("optional_uuid", () -> EntityDataSerializer.forValueType(
            net.minecraft.network.codec.ByteBufCodecs.optional(net.minecraft.core.UUIDUtil.STREAM_CODEC)));
    *///?} else {
    public static final Supplier<EntityDataSerializer<Optional<java.util.UUID>>> OPTIONAL_UUID =
            () -> net.minecraft.network.syncher.EntityDataSerializers.OPTIONAL_UUID;
    //?}

}
