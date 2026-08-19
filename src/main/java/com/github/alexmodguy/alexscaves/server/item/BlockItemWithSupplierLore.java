package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.citadel.item.BlockItemWithSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import java.util.List;

public class BlockItemWithSupplierLore extends BlockItemWithSupplier {

    private final Supplier<Block> block;

    public BlockItemWithSupplierLore(Supplier<Block> blockSupplier, Properties props) {
        super(blockSupplier, props);
        this.block = blockSupplier;
    }

    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block.get());
        String blockName = blockId.getNamespace() + "." + blockId.getPath();
        tooltip.add(Component.translatable("block." + blockName + ".desc").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
    }
}
