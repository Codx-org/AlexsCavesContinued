package com.github.alexmodguy.alexscaves.server.block;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.inventory.SpelunkeryTableMenu;
import com.github.alexmodguy.alexscaves.server.message.SpelunkeryTableCompleteTutorialMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

public class SpelunkeryTableBlock extends Block {
    private static final Component CONTAINER_TITLE = Component.translatable("alexscaves.container.spelunkery_table");

    public SpelunkeryTableBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD));
    }

    // 1.20.5 split BlockBehaviour#use into useItemOn and useWithoutItem. Vanilla calls useItemOn
    // for every hand and every stack -- the empty one included -- and only falls through to
    // useWithoutItem when it answers "did nothing", so hanging the whole rule off useItemOn keeps
    // this block reachable with a full hotbar exactly as it was below 1.20.5. The body is shared;
    // only the entry point and the "we did nothing" return differ. See ACCompat#itemResult.
    //? if >=1.21.2 {
    /*protected net.minecraft.world.InteractionResult useItemOn(net.minecraft.world.item.ItemStack usedStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
        return com.github.alexmodguy.alexscaves.server.misc.ACCompat.itemResult(acUse(state, level, pos, player, hand, result));
    }
    *///?} elif >=1.20.5 {
    /*protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack usedStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
        return com.github.alexmodguy.alexscaves.server.misc.ACCompat.itemResult(acUse(state, level, pos, player, hand, result));
    }
    *///?} else {
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
        InteractionResult acResult = acUse(state, level, pos, player, hand, result);
        return acResult == InteractionResult.PASS ? super.use(state, level, pos, player, hand, result) : acResult;
    }
    //?}

    private InteractionResult acUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        } else {
            player.openMenu(state.getMenuProvider(level, pos));
            player.awardStat(Stats.INTERACT_WITH_LOOM);
            if (player instanceof ServerPlayer serverPlayer) {
                AlexsCaves.sendNonLocal(new SpelunkeryTableCompleteTutorialMessage(SpelunkeryTableMenu.hasCompletedTutorial(serverPlayer)), serverPlayer);
            }
            return InteractionResult.CONSUME;
        }
    }

    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((i, inv, player) -> {
            return new SpelunkeryTableMenu(i, inv, ContainerLevelAccess.create(level, pos));
        }, CONTAINER_TITLE);
    }

}
