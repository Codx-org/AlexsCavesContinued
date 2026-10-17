package com.github.alexmodguy.alexscaves.server.block;

import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AbyssmarineWallBlock extends WallBlock implements ActivatedByAltar {

    /**
     * up (2) x east/north/west/south wall side (3 each). The four altar/water properties this block
     * also carries do not change its outline, so they are deliberately not part of the key.
     */
    private static final int SHAPE_COUNT = 2 * 3 * 3 * 3 * 3;

    private final VoxelShape[] shapeByIndex;
    private final VoxelShape[] collisionShapeByIndex;

    public AbyssmarineWallBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(UP, Boolean.valueOf(true)).setValue(BlockStateProperties.NORTH_WALL, WallSide.NONE).setValue(BlockStateProperties.EAST_WALL, WallSide.NONE).setValue(BlockStateProperties.SOUTH_WALL, WallSide.NONE).setValue(BlockStateProperties.WEST_WALL, WallSide.NONE).setValue(WATERLOGGED, Boolean.valueOf(false)).setValue(ACTIVE, Boolean.valueOf(false)).setValue(DISTANCE, MAX_DISTANCE));
        this.shapeByIndex = this.makeAbyssalShapes(4.0F, 3.0F, 16.0F, 0.0F, 14.0F, 16.0F);
        this.collisionShapeByIndex = this.makeAbyssalShapes(4.0F, 3.0F, 24.0F, 0.0F, 24.0F, 24.0F);
    }

    public void tick(BlockState state, ServerLevel serverLevel, BlockPos pos, RandomSource randomSource) {
        super.tick(state, serverLevel, pos, randomSource);
        serverLevel.setBlock(pos, updateDistance(state, serverLevel, pos), 3);
    }

    public BlockState updateShape(BlockState state, Direction direction, BlockState state1, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos1) {
        BlockState newState = super.updateShape(state, direction, state1, levelAccessor, blockPos, blockPos1);
        int i = ActivatedByAltar.getDistanceAt(state1) + 1;
        if (i != 1 || newState.getValue(DISTANCE) != i) {
            levelAccessor.scheduleTick(blockPos, this, 2);
        }
        return newState;
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return updateDistance(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DISTANCE, ACTIVE, UP, BlockStateProperties.NORTH_WALL, BlockStateProperties.EAST_WALL, BlockStateProperties.WEST_WALL, BlockStateProperties.SOUTH_WALL, WATERLOGGED);
    }

    private static int shapeIndex(boolean up, WallSide east, WallSide north, WallSide west, WallSide south) {
        int i = up ? 1 : 0;
        i = i * 3 + east.ordinal();
        i = i * 3 + north.ordinal();
        i = i * 3 + west.ordinal();
        return i * 3 + south.ordinal();
    }

    private static int shapeIndex(BlockState state) {
        return shapeIndex(state.getValue(UP).booleanValue(), state.getValue(BlockStateProperties.EAST_WALL), state.getValue(BlockStateProperties.NORTH_WALL), state.getValue(BlockStateProperties.WEST_WALL), state.getValue(BlockStateProperties.SOUTH_WALL));
    }

    private VoxelShape[] makeAbyssalShapes(float p_57966_, float p_57967_, float p_57968_, float p_57969_, float p_57970_, float p_57971_) {
        float f = 8.0F - p_57966_;
        float f1 = 8.0F + p_57966_;
        float f2 = 8.0F - p_57967_;
        float f3 = 8.0F + p_57967_;
        VoxelShape voxelshape = Block.box((double) f, 0.0D, (double) f, (double) f1, (double) p_57968_, (double) f1);
        VoxelShape voxelshape1 = Block.box((double) f2, (double) p_57969_, 0.0D, (double) f3, (double) p_57970_, (double) f3);
        VoxelShape voxelshape2 = Block.box((double) f2, (double) p_57969_, (double) f2, (double) f3, (double) p_57970_, 16.0D);
        VoxelShape voxelshape3 = Block.box(0.0D, (double) p_57969_, (double) f2, (double) f3, (double) p_57970_, (double) f3);
        VoxelShape voxelshape4 = Block.box((double) f2, (double) p_57969_, (double) f2, 16.0D, (double) p_57970_, (double) f3);
        VoxelShape voxelshape5 = Block.box((double) f2, (double) p_57969_, 0.0D, (double) f3, (double) p_57971_, (double) f3);
        VoxelShape voxelshape6 = Block.box((double) f2, (double) p_57969_, (double) f2, (double) f3, (double) p_57971_, 16.0D);
        VoxelShape voxelshape7 = Block.box(0.0D, (double) p_57969_, (double) f2, (double) f3, (double) p_57971_, (double) f3);
        VoxelShape voxelshape8 = Block.box((double) f2, (double) p_57969_, (double) f2, 16.0D, (double) p_57971_, (double) f3);
        VoxelShape[] shapes = new VoxelShape[SHAPE_COUNT];

        for (Boolean obool : UP.getPossibleValues()) {
            for (WallSide wallside : BlockStateProperties.EAST_WALL.getPossibleValues()) {
                for (WallSide wallside1 : BlockStateProperties.NORTH_WALL.getPossibleValues()) {
                    for (WallSide wallside2 : BlockStateProperties.WEST_WALL.getPossibleValues()) {
                        for (WallSide wallside3 : BlockStateProperties.SOUTH_WALL.getPossibleValues()) {
                            VoxelShape voxelshape9 = Shapes.empty();
                            voxelshape9 = applyWallShape(voxelshape9, wallside, voxelshape4, voxelshape8);
                            voxelshape9 = applyWallShape(voxelshape9, wallside2, voxelshape3, voxelshape7);
                            voxelshape9 = applyWallShape(voxelshape9, wallside1, voxelshape1, voxelshape5);
                            voxelshape9 = applyWallShape(voxelshape9, wallside3, voxelshape2, voxelshape6);
                            if (obool.booleanValue()) {
                                voxelshape9 = Shapes.or(voxelshape9, voxelshape);
                            }

                            shapes[shapeIndex(obool.booleanValue(), wallside, wallside1, wallside2, wallside3)] = voxelshape9;
                        }
                    }
                }
            }
        }

        return shapes;
    }

    private static VoxelShape applyWallShape(VoxelShape p_58034_, WallSide p_58035_, VoxelShape p_58036_, VoxelShape p_58037_) {
        if (p_58035_ == WallSide.TALL) {
            return Shapes.or(p_58034_, p_58037_);
        } else {
            return p_58035_ == WallSide.LOW ? Shapes.or(p_58034_, p_58036_) : p_58034_;
        }
    }

    public VoxelShape getShape(BlockState p_58050_, BlockGetter p_58051_, BlockPos p_58052_, CollisionContext p_58053_) {
        return this.shapeByIndex[shapeIndex(p_58050_)];
    }

    public VoxelShape getCollisionShape(BlockState p_58055_, BlockGetter p_58056_, BlockPos p_58057_, CollisionContext p_58058_) {
        return this.collisionShapeByIndex[shapeIndex(p_58055_)];
    }

}
