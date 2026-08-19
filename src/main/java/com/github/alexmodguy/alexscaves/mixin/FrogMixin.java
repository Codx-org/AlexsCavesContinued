package com.github.alexmodguy.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.entity.ACFrogRegistry;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frog.class)
public abstract class FrogMixin extends Animal {

    // 1.20.5 turned Frog into a VariantHolder over Holder<FrogVariant> rather than the variant
    // itself, so setVariant's parameter is a Holder from that version on. A @Shadow has to mirror
    // the target's descriptor exactly or Mixin refuses to bind it, hence the split.
    //? if >=1.20.5 {
    /*@Shadow
    public abstract void setVariant(net.minecraft.core.Holder<FrogVariant> variant);
    *///?} else {
    @Shadow
    public abstract void setVariant(FrogVariant variant);
    //?}

    protected FrogMixin(EntityType<? extends Animal> animal, Level level) {
        super(animal, level);
    }

    @Inject(
            method = {"Lnet/minecraft/world/entity/animal/frog/Frog;checkFrogSpawnRules(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)Z"},
            remap = true,
            cancellable = true,
            at = @At(value = "TAIL")
    )
    private static void ac_checkFrogSpawnRules(EntityType<? extends Animal> type, LevelAccessor levelAccessor, MobSpawnType mobType, BlockPos pos, RandomSource randomSource, CallbackInfoReturnable<Boolean> cir) {
        if (levelAccessor.getBiome(pos).is(ACBiomeRegistry.PRIMORDIAL_CAVES)) {
            cir.setReturnValue(levelAccessor.getBlockState(pos.below()).is(ACTagRegistry.DINOSAURS_SPAWNABLE_ON) && levelAccessor.getFluidState(pos).isEmpty() && levelAccessor.getFluidState(pos.below()).isEmpty());
        }
    }

    @Inject(
            // 1.20.5 dropped the trailing spawn-data CompoundTag from finalizeSpawn.
            //? if >=1.20.5 {
            /*method = {"Lnet/minecraft/world/entity/animal/frog/Frog;finalizeSpawn(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/world/entity/SpawnGroupData;)Lnet/minecraft/world/entity/SpawnGroupData;"},
            *///?} else {
            method = {"Lnet/minecraft/world/entity/animal/frog/Frog;finalizeSpawn(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/world/entity/SpawnGroupData;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/entity/SpawnGroupData;"},
            //?}
            remap = true,
            at = @At(value = "TAIL")
    )
    //? if >=1.20.5 {
    /*private void ac_finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficultyIn, MobSpawnType reason, @javax.annotation.Nullable SpawnGroupData spawnDataIn, CallbackInfoReturnable<SpawnGroupData> cir) {
    *///?} else {
    private void ac_finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficultyIn, MobSpawnType reason, @javax.annotation.Nullable SpawnGroupData spawnDataIn, @javax.annotation.Nullable CompoundTag dataTag, CallbackInfoReturnable<SpawnGroupData> cir) {
    //?}
        Holder<Biome> holder = level.getBiome(this.blockPosition());
        if (holder.is(ACBiomeRegistry.PRIMORDIAL_CAVES)) {
            // See the shadow above. FROG_VARIANT is a datapack registry from 1.21.5, so the holder
            // has to come out of the level's registry access; below that it is still a built-in
            // registry and the instance can be wrapped directly.
            //? if >=1.21.5 {
            /*setVariant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.FROG_VARIANT).getOrThrow(ACFrogRegistry.PRIMORDIAL));
            *///?} elif >=1.20.5 {
            /*setVariant(net.minecraft.core.registries.BuiltInRegistries.FROG_VARIANT.wrapAsHolder(ACFrogRegistry.PRIMORDIAL.get()));
            *///?} else {
            setVariant(ACFrogRegistry.PRIMORDIAL.get());
            //?}
        }
    }
}
