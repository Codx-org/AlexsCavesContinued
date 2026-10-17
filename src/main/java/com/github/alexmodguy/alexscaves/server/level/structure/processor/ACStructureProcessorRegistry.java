package com.github.alexmodguy.alexscaves.server.level.structure.processor;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;

public class ACStructureProcessorRegistry {

    // 26.2 deleted the wrapper layer: Registries.STRUCTURE_PROCESSOR is a
    // Registry<MapCodec<? extends StructureProcessor>> now (it was Registry<StructureProcessorType<?>>),
    // StructureProcessorType is a non-generic interface holding four static codec constants, and a
    // processor points at its own codec through StructureProcessor#codec(). So the registered value
    // *is* each processor's CODEC, with no `() -> () ->` double lambda around it.
    //
    // Only DEF_REG is read outside this package; the five constants exist for the pre-26.2
    // getType() bodies, which is why the modern arm can type them loosely.
    //? if >=26.2 {
    /*public static final DeferredRegister<MapCodec<? extends StructureProcessor>> DEF_REG = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, AlexsCaves.MODID);

    public static final Supplier<? extends MapCodec<? extends StructureProcessor>> UNDERGROUND_CABIN = DEF_REG.register("underground_cabin", () -> UndergroundCabinProcessor.CODEC);
    public static final Supplier<? extends MapCodec<? extends StructureProcessor>> WHALEFALL = DEF_REG.register("whalefall", () -> WhalefallProcessor.CODEC);
    public static final Supplier<? extends MapCodec<? extends StructureProcessor>> WHALEFALL_SKULL = DEF_REG.register("whalefall_skull", () -> WhalefallProcessor.CODEC_SKULL);
    public static final Supplier<? extends MapCodec<? extends StructureProcessor>> LOLLIPOP = DEF_REG.register("lollipop", () -> LollipopProcessor.CODEC);
    public static final Supplier<? extends MapCodec<? extends StructureProcessor>> SODA_BOTTLE = DEF_REG.register("soda_bottle", () -> SodaBottleProcessor.CODEC);
    *///?} else {
    public static final DeferredRegister<StructureProcessorType<?>> DEF_REG = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, AlexsCaves.MODID);

    public static final Supplier<StructureProcessorType<UndergroundCabinProcessor>> UNDERGROUND_CABIN = DEF_REG.register("underground_cabin", () -> () -> UndergroundCabinProcessor.CODEC);
    public static final Supplier<StructureProcessorType<WhalefallProcessor>> WHALEFALL = DEF_REG.register("whalefall", () -> () -> WhalefallProcessor.CODEC);
    public static final Supplier<StructureProcessorType<WhalefallProcessor>> WHALEFALL_SKULL = DEF_REG.register("whalefall_skull", () -> () -> WhalefallProcessor.CODEC_SKULL);
    public static final Supplier<StructureProcessorType<LollipopProcessor>> LOLLIPOP = DEF_REG.register("lollipop", () -> () -> LollipopProcessor.CODEC);
    public static final Supplier<StructureProcessorType<SodaBottleProcessor>> SODA_BOTTLE = DEF_REG.register("soda_bottle", () -> () -> SodaBottleProcessor.CODEC);
    //?}

}
