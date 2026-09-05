package com.github.alexmodguy.alexscaves.citadel.server.entity;

import net.minecraft.nbt.CompoundTag;

/**
 * @author Alexthe666
 * @since 1.7.0
 */
public interface ICitadelDataEntity {

    CompoundTag acGetCitadelEntityData();

    void acSetCitadelEntityData(CompoundTag nbt);
}
