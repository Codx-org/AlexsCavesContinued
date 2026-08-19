package com.github.alexmodguy.alexscaves.server.entity;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;

// 1.21.5 turned FrogVariant into a datapack registry (RegistryDataLoader, not BuiltInRegistries),
// and the variant itself into a record of a ClientAsset plus spawn-priority selectors. There is
// nothing left to register from code on that line: the variant is authored as
// data/alexscaves/frog_variant/primordial.json and addressed by ResourceKey.
public class ACFrogRegistry {

    //? if >=1.21.5 {
    /*public static final net.minecraft.resources.ResourceKey<FrogVariant> PRIMORDIAL =
            net.minecraft.resources.ResourceKey.create(Registries.FROG_VARIANT, ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "primordial"));

    public static boolean isPrimordial(Frog frog) {
        return frog.getVariant().is(PRIMORDIAL);
    }
    *///?} elif >=1.20.5 {
    /*public static final DeferredRegister<FrogVariant> DEF_REG = DeferredRegister.create(Registries.FROG_VARIANT, AlexsCaves.MODID);

    public static final Supplier<FrogVariant> PRIMORDIAL = DEF_REG.register("primordial", () -> new FrogVariant(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "textures/entity/primordial_frog.png")));

    public static boolean isPrimordial(Frog frog) {
        return frog.getVariant().value() == PRIMORDIAL.get();
    }
    *///?} else {
    public static final DeferredRegister<FrogVariant> DEF_REG = DeferredRegister.create(Registries.FROG_VARIANT, AlexsCaves.MODID);

    public static final Supplier<FrogVariant> PRIMORDIAL = DEF_REG.register("primordial", () -> new FrogVariant(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "textures/entity/primordial_frog.png")));

    public static boolean isPrimordial(Frog frog) {
        return frog.getVariant() == PRIMORDIAL.get();
    }
    //?}

}
