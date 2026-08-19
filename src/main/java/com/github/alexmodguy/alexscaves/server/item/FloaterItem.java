package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.item.FloaterEntity;
import net.minecraft.world.InteractionHand;
//? if <1.21.2
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FloaterItem extends Item {
    public FloaterItem() {
        super(new Item.Properties());
    }

    // 1.21.2 merged the result-plus-stack pair back into a plain InteractionResult.
    //? if >=1.21.2
    /*public net.minecraft.world.InteractionResult use(Level level, Player player, InteractionHand hand) {*/
    //? if <1.21.2
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if(ACCompat.isInWaterOrBubble(player) && !player.isShiftKeyDown()){
            FloaterEntity floaterEntity = ACCompat.createEntity(ACEntityRegistry.FLOATER.get(), level);
            floaterEntity.copyPosition(player);
            if(!level.isClientSide()){
                level.addFreshEntity(floaterEntity);
            }
            player.getRootVehicle().startRiding(floaterEntity);
            if(!player.isCreative()){
                itemstack.shrink(1);
            }
            return ACCompat.useSidedSuccess(itemstack, level.isClientSide());
        }else{
            return ACCompat.usePass(itemstack);
        }
    }
}
