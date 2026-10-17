package com.github.alexmodguy.alexscaves.server.block;

import com.github.alexmodguy.alexscaves.server.block.blockentity.ACBlockEntityRegistry;
import com.github.alexmodguy.alexscaves.server.block.blockentity.ConversionCrucibleBlockEntity;
import com.github.alexmodguy.alexscaves.server.block.blockentity.CopperValveBlockEntity;
import com.github.alexmodguy.alexscaves.server.item.ACItemRegistry;
import com.github.alexmodguy.alexscaves.server.item.BiomeTreatItem;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ConversionCrucibleBlock extends BaseEntityBlock {

    // 1.20.3 made Block#codec() abstract for datapack-defined blocks; Alex's Caves' blocks
    // are never described by value, so they all share one placeholder. See ACPlatform.
    //? if >=1.20.3 && <26.3 {
    /*@Override
    public com.mojang.serialization.MapCodec<? extends ConversionCrucibleBlock> codec() {
        return com.github.alexmodguy.alexscaves.server.misc.ACPlatform.unsupportedBlockCodec();
    }
    *///?}


    private static final VoxelShape INSIDE = box(3.0D, 2.0D, 3.0D, 13.0D, 16.0D, 13.0D);
    private static final VoxelShape SHAPE = Shapes.join(Shapes.block(), Shapes.or(
            box(0.0D, 0.0D, 5.0D, 16.0D, 2.0D, 11.0D),
            box(5.0D, 0.0D, 0.0D, 11.0D, 2.0D, 16.0D),
            box(5.0D, 0.0D, 5.0D, 11.0D, 2.0D, 11.0D),
            INSIDE), BooleanOp.ONLY_FIRST);
    private static final VoxelShape ABOVE = Block.box(0.0D, 16.0D, 0.0D, 16.0D, 20.0D, 16.0D);
    private static final VoxelShape SUCK = Shapes.or(INSIDE, ABOVE);

    public ConversionCrucibleBlock() {
        super(Properties.of().mapColor(MapColor.GOLD).requiresCorrectToolForDrops().strength(5F, 12.0F).sound(SoundType.METAL));
    }

    public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public VoxelShape getInteractionShape(BlockState blockState, BlockGetter blockGetter, BlockPos pos) {
        return INSIDE;
    }

    // 1.21.4 deleted the "drawn by a block-entity renderer" render shape. A block whose whole look comes
    // from its BER now simply keeps the default MODEL and ships a particle-only block model — which is
    // precisely what this one's model already is (parent block/block, textures.particle, no elements). So
    // dropping the override is the faithful port, and it keeps break/landing particles working, which
    // INVISIBLE would not.
    //? if <1.21.4 {
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
    //?}

    @javax.annotation.Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_152180_, BlockState p_152181_, BlockEntityType<T> p_152182_) {
        return createTickerHelper(p_152182_, ACBlockEntityRegistry.CONVERSION_CRUCIBLE.get(), ConversionCrucibleBlockEntity::tick);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConversionCrucibleBlockEntity(pos, state);
    }


    // 1.20.5 split BlockBehaviour#use into useItemOn and useWithoutItem. Vanilla calls useItemOn
    // for every hand and every stack -- the empty one included -- and only falls through to
    // useWithoutItem when it answers "did nothing", so hanging the whole rule off useItemOn keeps
    // this block reachable with a full hotbar exactly as it was below 1.20.5. The body is shared;
    // only the entry point and the "we did nothing" return differ. See ACCompat#itemResult.
    //? if >=1.21.2 {
    /*protected net.minecraft.world.InteractionResult useItemOn(net.minecraft.world.item.ItemStack usedStack, BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        return com.github.alexmodguy.alexscaves.server.misc.ACCompat.itemResult(acUse(state, worldIn, pos, player, handIn, hit));
    }
    *///?} elif >=1.20.5 {
    /*protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack usedStack, BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        return com.github.alexmodguy.alexscaves.server.misc.ACCompat.itemResult(acUse(state, worldIn, pos, player, handIn, hit));
    }
    *///?} else {
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        InteractionResult acResult = acUse(state, worldIn, pos, player, handIn, hit);
        return acResult == InteractionResult.PASS ? super.use(state, worldIn, pos, player, handIn, hit) : acResult;
    }
    //?}

    private InteractionResult acUse(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        ItemStack playerItem = player.getItemInHand(handIn);
        if (worldIn.getBlockEntity(pos) instanceof ConversionCrucibleBlockEntity crucible && !player.isShiftKeyDown()) {
            if(crucible.getConvertingToBiome() != null){
                if(crucible.getWantItem().isEmpty()){
                    crucible.rerollWantedItem();
                    crucible.markUpdated();
                }else if(!crucible.getWantItem().isEmpty() && crucible.getWantItem().is(playerItem.getItem())){
                    if(!worldIn.isClientSide()){
                        ItemStack copy = playerItem.copy();
                        copy.setCount(1);
                        crucible.consumeItem(copy);
                        if(!player.getAbilities().instabuild){
                            playerItem.shrink(1);
                        }
                        crucible.markUpdated();
                    }
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }else if(playerItem.is(ACItemRegistry.BIOME_TREAT.get()) && BiomeTreatItem.getCaveBiome(playerItem) != null){
                if(!worldIn.isClientSide()){
                    crucible.setConvertingToBiome(BiomeTreatItem.getCaveBiome(playerItem));
                    crucible.setFilledLevel(1);
                    crucible.rerollWantedItem();
                    crucible.markUpdated();
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }


    public static VoxelShape getSuckShape() {
        return SUCK;
    }
}
