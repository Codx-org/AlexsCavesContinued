package com.github.alexmodguy.alexscaves.citadel.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import java.util.function.Supplier;

public class BlockItemWithSupplier extends BlockItem {

    private final Supplier<Block> blockSupplier;

    public BlockItemWithSupplier(Supplier<Block> blockSupplier, Properties props) {
        super(null, props);
        this.blockSupplier = blockSupplier;
    }

    @Override
    public Block getBlock() {
        return blockSupplier.get();
    }

    public boolean canFitInsideContainerItems() {
        return !(blockSupplier.get() instanceof ShulkerBoxBlock);
    }

    // Spilling a broken shulker box's contents. 1.20.5 replaced the BlockEntityTag NBT this dug
    // through with the CONTAINER component, so the newer arm is vanilla ShulkerBoxBlockItem's own
    // one-liner: take the contents off the stack and drop them.
    //? if >=1.20.5 {
    /*public void onDestroyed(ItemEntity p_150700_) {
        if (this.blockSupplier.get() instanceof ShulkerBoxBlock) {
            net.minecraft.world.item.component.ItemContainerContents contents = p_150700_.getItem().set(
                    net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY);
            if (contents != null) {
                ItemUtils.onContainerDestroyed(p_150700_, contents.nonEmptyItems());
            }
        }
    }
    *///?} else {
    public void onDestroyed(ItemEntity p_150700_) {
        if (this.blockSupplier.get() instanceof ShulkerBoxBlock) {
            ItemStack itemstack = p_150700_.getItem();
            CompoundTag compoundtag = getBlockEntityData(itemstack);
            if (compoundtag != null && ACCompat.contains(compoundtag, "Items", 9)) {
                ListTag listtag = ACCompat.getList(compoundtag, "Items", 10);
                ItemUtils.onContainerDestroyed(p_150700_, listtag.stream().map(CompoundTag.class::cast).map(ItemStack::of));
            }
        }
    }
    //?}
}
