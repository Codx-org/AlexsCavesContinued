package com.github.alexmodguy.alexscaves.server.block.fluid;

import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

import java.util.function.Consumer;

/**
 * The fluid-type half of {@link com.github.alexmodguy.alexscaves.server.item.ACClientExtensionItem}
 * — see there for why the hook the loader deleted in 1.21.2 is redeclared as a mod interface.
 */
public interface ACClientExtensionFluidType {

    void initializeClient(Consumer<IClientFluidTypeExtensions> consumer);
}
