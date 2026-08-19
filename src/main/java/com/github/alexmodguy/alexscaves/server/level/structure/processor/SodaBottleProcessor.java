package com.github.alexmodguy.alexscaves.server.level.structure.processor;

import com.mojang.serialization.MapCodec;
import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;

public class SodaBottleProcessor extends StructureProcessor {

    public static final SodaBottleProcessor INSTANCE = new SodaBottleProcessor();

    // 1.20.5 narrowed the structure/processor codec surface from Codec to MapCodec.
    //? if >=1.20.5 {
    /*public static final MapCodec<SodaBottleProcessor> CODEC = MapCodec.unit(() -> {
    *///?} else {
    public static final Codec<SodaBottleProcessor> CODEC = Codec.unit(() -> {
    //?}
        return SodaBottleProcessor.INSTANCE;
    });

    public SodaBottleProcessor() {
    }

    @Nullable
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader levelReader, BlockPos blockPosUnused, BlockPos pos, StructureTemplate.StructureBlockInfo relativeInfo, StructureTemplate.StructureBlockInfo info, StructurePlaceSettings settings) {
        BlockState in = info.state();
        // 26.2 collapsed the sixteen dyed concretes into one ColorCollection<Block> addressed by
        // DyeColor. Hoisted into two locals so the branches below read the same on every node.
        //? if <26.2 {
        net.minecraft.world.level.block.Block purpleConcrete = Blocks.PURPLE_CONCRETE;
        net.minecraft.world.level.block.Block orangeConcrete = Blocks.ORANGE_CONCRETE;
        //?} else {
        /*net.minecraft.world.level.block.Block purpleConcrete = Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.PURPLE);
        net.minecraft.world.level.block.Block orangeConcrete = Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.ORANGE);
        *///?}
        // ⚠️ Both branches build a PURPLE_SODA block. That is upstream's, almost certainly a
        // copy-paste slip (the orange branch surely meant ORANGE_SODA), and is reproduced verbatim
        // — a port is the wrong place to change what a structure generates.
        if(in.is(purpleConcrete)){
            return new StructureTemplate.StructureBlockInfo(info.pos(), ACBlockRegistry.PURPLE_SODA.get().defaultBlockState(), info.nbt());
        }else if(in.is(orangeConcrete)){
            return new StructureTemplate.StructureBlockInfo(info.pos(), ACBlockRegistry.PURPLE_SODA.get().defaultBlockState(), info.nbt());
        }
        return info;
    }

    // See UndergroundCabinProcessor for what 26.2 did to StructureProcessor.
    //
    // ⚠️ The <26.2 arm is upstream's, and upstream names the wrong type here: this class is
    // registered as `soda_bottle` but reports `underground_cabin`, so serialising one would write
    // the cabin's id. Harmless in practice — every one of these processors is added in code
    // (SodaBottleStructurePiece), never encoded into a processor-list JSON — and it is left as
    // found rather than quietly changed, on the same footing as the extinction_spear model
    // override. The 26.2 arm cannot reproduce it: `codec()` is what rebuilds *this* instance, so
    // naming another processor's codec would be a real bug rather than a cosmetic one.
    //? if >=26.2 {
    /*@Override
    public MapCodec<? extends StructureProcessor> codec() {
        return CODEC;
    }
    *///?} else {
    @Override
    protected StructureProcessorType<?> getType() {
        return ACStructureProcessorRegistry.UNDERGROUND_CABIN.get();
    }
    //?}
}
