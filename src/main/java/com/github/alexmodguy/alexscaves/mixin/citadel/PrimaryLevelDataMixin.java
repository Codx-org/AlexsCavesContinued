package com.github.alexmodguy.alexscaves.mixin.citadel;

import com.github.alexmodguy.alexscaves.citadel.CitadelConstants;
import com.github.alexmodguy.alexscaves.citadel.server.generation.NoiseGeneratorSettingsAccessor;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(PrimaryLevelData.class)
public class PrimaryLevelDataMixin {

    // 26 took the RegistryAccess off the whole level-data save path — createTag(RegistryAccess,
    // CompoundTag) is createTag(UUID) now and setTagData carries the single-player uuid instead. The
    // registries this needs therefore come from the running server, which CitadelProxy has held since
    // MinecraftServerMixin's constructor hook; that is always non-null here, because level data is
    // only ever written by a server that has finished constructing. The guard is belt-and-braces.
    //? if >=26 {
    /*@Inject(at = @At("HEAD"), remap = CitadelConstants.REMAPREFS,
            method = "Lnet/minecraft/world/level/storage/PrimaryLevelData;setTagData(Lnet/minecraft/nbt/CompoundTag;Ljava/util/UUID;)V")
    private void citadel_preSetTagData(CompoundTag compoundTag, java.util.UUID singlePlayerUUID, CallbackInfo ci) {
        citadelUpdateSurfaceRulesForServer(true);
    }

    @Inject(at = @At("TAIL"), remap = CitadelConstants.REMAPREFS,
            method = "Lnet/minecraft/world/level/storage/PrimaryLevelData;setTagData(Lnet/minecraft/nbt/CompoundTag;Ljava/util/UUID;)V")
    private void citadel_postSetTagData(CompoundTag compoundTag, java.util.UUID singlePlayerUUID, CallbackInfo ci) {
        citadelUpdateSurfaceRulesForServer(false);
    }

    @Unique
    private void citadelUpdateSurfaceRulesForServer(boolean saving) {
        net.minecraft.server.MinecraftServer server = com.github.alexmodguy.alexscaves.citadel.Citadel.PROXY.getMinecraftServer();
        if (server != null) {
            citadelUpdateSurfaceRules(server.registryAccess(), saving);
        }
    }
    *///?} else {
    @Inject(at = @At("HEAD"), remap = CitadelConstants.REMAPREFS,
            method = "Lnet/minecraft/world/level/storage/PrimaryLevelData;setTagData(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/nbt/CompoundTag;)V")
    private void citadel_preSetTagData(RegistryAccess registryAccess, CompoundTag compoundTag, CompoundTag compoundTag1, CallbackInfo ci) {
        citadelUpdateSurfaceRules(registryAccess, true);
    }

    @Inject(at = @At("TAIL"), remap = CitadelConstants.REMAPREFS,
            method = "Lnet/minecraft/world/level/storage/PrimaryLevelData;setTagData(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/nbt/CompoundTag;)V")
    private void citadel_postSetTagData(RegistryAccess registryAccess, CompoundTag compoundTag, CompoundTag compoundTag1, CallbackInfo ci) {
        citadelUpdateSurfaceRules(registryAccess, false);
    }
    //?}

    @Unique
    private void citadelUpdateSurfaceRules(RegistryAccess registryAccess, boolean saving) {
        Registry<LevelStem> registry = registryAccess.registryOrThrow(Registries.LEVEL_STEM);
        if (registry.containsKey(LevelStem.OVERWORLD)) {
            // Registry#get answers the Holder from 1.21.2; the nullable value is getValue now.
            //? if >=1.21.2 {
            /*LevelStem levelstem = registry.getValue(LevelStem.OVERWORLD);
            *///?} else {
            LevelStem levelstem = registry.get(LevelStem.OVERWORLD);
            //?}
            if (levelstem.generator() instanceof NoiseBasedChunkGenerator noiseBasedChunkGenerator && noiseBasedChunkGenerator.settings.isBound() && (Object) noiseBasedChunkGenerator.settings.value() instanceof NoiseGeneratorSettingsAccessor accessor) {
                accessor.onSaveData(saving);
            }
        }
    }

}
