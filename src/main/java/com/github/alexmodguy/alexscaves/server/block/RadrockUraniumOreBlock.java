package com.github.alexmodguy.alexscaves.server.block;

import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public class RadrockUraniumOreBlock extends Block implements ACExpDropBlock {

    public RadrockUraniumOreBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).requiresCorrectToolForDrops().strength(5F, 11.0F).sound(ACSoundTypes.URANIUM));
    }

    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource randomSource) {
        if (randomSource.nextInt(80) == 0) {
            level.playLocalSound((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, ACSoundRegistry.URANIUM_HUM.get(), SoundSource.BLOCKS, 0.5F, randomSource.nextFloat() * 0.4F + 0.8F, false);
        }
    }

    // NeoForge reshaped this hook in 1.21: instead of the pre-resolved fortune and silk-touch levels
    // it hands over the block entity, the breaker and the tool, because enchantments stopped being
    // integers a caller could look up ahead of time. Silk touch is asked of the tool here instead;
    // fortune never mattered to this block. Forge kept the older signature, so only NeoForge splits.
    //? if neoforge && >=1.21 {
    /*@Override
    public int getExpDrop(BlockState state, net.minecraft.world.level.LevelAccessor level, BlockPos pos,
                          net.minecraft.world.level.block.entity.BlockEntity blockEntity,
                          net.minecraft.world.entity.Entity breaker, net.minecraft.world.item.ItemStack tool) {
        int silkTouchLevel = com.github.alexmodguy.alexscaves.server.misc.ACCompat.enchantLevel(
                tool, net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH);
        return silkTouchLevel == 0 ? level.getRandom().nextInt(2) : 0;
    }
    *///?} else {
    @Override
    public int getExpDrop(BlockState state, LevelReader level, net.minecraft.util.RandomSource randomSource, BlockPos pos, int fortuneLevel, int silkTouchLevel) {
        return silkTouchLevel == 0 ? randomSource.nextInt(2) : 0;
    }
    //?}
}
