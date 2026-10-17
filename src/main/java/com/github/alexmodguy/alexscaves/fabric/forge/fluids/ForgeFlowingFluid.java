package com.github.alexmodguy.alexscaves.fabric.forge.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Fabric stand-in for the loader's data-driven {@link FlowingFluid} subclass.
 *
 * <p>This is the one file in the fluid batch that is a genuine reimplementation rather than a shim.
 * Vanilla's {@code FlowingFluid} leaves thirteen members abstract and answers them with a hand-written
 * class per fluid ({@code WaterFluid}, {@code LavaFluid}); the loader's contribution is to answer them
 * all from a {@link Properties} bag instead, so a mod can declare a fluid without writing a class. The
 * mod uses exactly that — {@code ACFluidRegistry} builds acid and purple soda out of two
 * {@code Properties} and the nested {@link Source}/{@link Flowing} pair — so reproducing the bag is
 * what keeps that registry one spelling on all three loaders.
 *
 * <p>Every default below is the loader's own, so the two fluids behave identically here: they do not
 * convert to sources, spread four blocks, lose one level per block, tick every five ticks and have an
 * explosion resistance of 1. Those are not vanilla water's numbers — water converts to a source and
 * ticks every five, lava does neither — they are the loader's neutral defaults, which is precisely
 * what {@code ACFluidRegistry} was written against.
 *
 * <p>The only member with no counterpart to copy is {@link #getFluidType}: on the loaders the fluid
 * type is reachable from any fluid because the loader patches {@code Fluid} itself, and here it is
 * reachable only from this class, which is why {@link FluidType#of} exists to ask the question of an
 * arbitrary {@link Fluid}.
 */
public abstract class ForgeFlowingFluid extends FlowingFluid {

    private final Supplier<? extends FluidType> fluidType;
    private final Supplier<? extends Fluid> flowing;
    private final Supplier<? extends Fluid> still;
    @Nullable
    private final Supplier<? extends Item> bucket;
    @Nullable
    private final Supplier<? extends LiquidBlock> block;
    private final boolean canConvertToSource;
    private final int slopeFindDistance;
    private final int levelDecreasePerBlock;
    private final float explosionResistance;
    private final int tickRate;

    protected ForgeFlowingFluid(Properties properties) {
        this.fluidType = properties.fluidType;
        this.flowing = properties.flowing;
        this.still = properties.still;
        this.bucket = properties.bucket;
        this.block = properties.block;
        this.canConvertToSource = properties.canConvertToSource;
        this.slopeFindDistance = properties.slopeFindDistance;
        this.levelDecreasePerBlock = properties.levelDecreasePerBlock;
        this.explosionResistance = properties.explosionResistance;
        this.tickRate = properties.tickRate;
    }

    public FluidType getFluidType() {
        return fluidType.get();
    }

    @Override
    public Fluid getFlowing() {
        return flowing.get();
    }

    @Override
    public Fluid getSource() {
        return still.get();
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == still.get() || fluid == flowing.get();
    }

    // 1.21.2 narrowed the level to a ServerLevel — infinite-source conversion is a server decision
    // — which is a signature change and so an arm rather than a rename rule. The answer is a
    // constant either way, so only the parameter moves.
    //? if >=1.21.2 {
    /*@Override
    protected boolean canConvertToSource(net.minecraft.server.level.ServerLevel level) {
        return canConvertToSource;
    }
    *///?} else {
    @Override
    protected boolean canConvertToSource(Level level) {
        return canConvertToSource;
    }
    //?}

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        Block.dropResources(state, level, pos, blockEntity);
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return slopeFindDistance;
    }

    @Override
    protected int getDropOff(LevelReader level) {
        return levelDecreasePerBlock;
    }

    @Override
    public Item getBucket() {
        return bucket == null ? Items.AIR : bucket.get();
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
        return direction == Direction.DOWN && !isSame(fluid);
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return tickRate;
    }

    @Override
    protected float getExplosionResistance() {
        return explosionResistance;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        if (block == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return block.get().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    public static class Properties {

        private final Supplier<? extends FluidType> fluidType;
        private final Supplier<? extends Fluid> still;
        private final Supplier<? extends Fluid> flowing;
        @Nullable
        private Supplier<? extends Item> bucket;
        @Nullable
        private Supplier<? extends LiquidBlock> block;
        private boolean canConvertToSource = false;
        private int slopeFindDistance = 4;
        private int levelDecreasePerBlock = 1;
        private float explosionResistance = 1.0F;
        private int tickRate = 5;

        public Properties(Supplier<? extends FluidType> fluidType, Supplier<? extends Fluid> still, Supplier<? extends Fluid> flowing) {
            this.fluidType = fluidType;
            this.still = still;
            this.flowing = flowing;
        }

        public Properties bucket(Supplier<? extends Item> bucket) {
            this.bucket = bucket;
            return this;
        }

        public Properties block(Supplier<? extends LiquidBlock> block) {
            this.block = block;
            return this;
        }

        public Properties canConvertToSource(boolean canConvertToSource) {
            this.canConvertToSource = canConvertToSource;
            return this;
        }

        public Properties slopeFindDistance(int slopeFindDistance) {
            this.slopeFindDistance = slopeFindDistance;
            return this;
        }

        public Properties levelDecreasePerBlock(int levelDecreasePerBlock) {
            this.levelDecreasePerBlock = levelDecreasePerBlock;
            return this;
        }

        public Properties explosionResistance(float explosionResistance) {
            this.explosionResistance = explosionResistance;
            return this;
        }

        public Properties tickRate(int tickRate) {
            this.tickRate = tickRate;
            return this;
        }
    }

    public static class Flowing extends ForgeFlowingFluid {

        public Flowing(Properties properties) {
            super(properties);
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }

    public static class Source extends ForgeFlowingFluid {

        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }
}
