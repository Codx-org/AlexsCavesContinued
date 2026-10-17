package com.github.alexmodguy.alexscaves.citadel.item;

import com.github.alexmodguy.alexscaves.citadel.Citadel;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import com.github.alexmodguy.alexscaves.server.item.ACClientExtensionItem;

/**
 * An item whose whole appearance comes from a {@code BlockEntityWithoutLevelRenderer}, driven by
 * NBT rather than a model. Alex's Caves uses two of these as advancement icons; see
 * {@link CitadelDisplayItems}.
 */
public class ItemCustomRender extends Item implements ACClientExtensionItem {

    public ItemCustomRender(Properties props) {
        super(props);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions) Citadel.PROXY.getISTERProperties());
    }
}
