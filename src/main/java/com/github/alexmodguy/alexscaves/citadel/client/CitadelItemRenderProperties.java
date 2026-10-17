package com.github.alexmodguy.alexscaves.citadel.client;

//? if <1.21.4
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * The loader hook that hands {@link CitadelItemstackRenderer} to the item pipeline below 1.21.4.
 * Empty from 1.21.4, where the ISTER mechanism is gone and the two display items are drawn by
 * {@link com.github.alexmodguy.alexscaves.client.render.item.ACItemSpecialRenderer.Icon}.
 */
public class CitadelItemRenderProperties implements IClientItemExtensions {

    //? if <1.21.4 {
    private final BlockEntityWithoutLevelRenderer renderer = new CitadelItemstackRenderer();

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return renderer;
    }
    //?}
}
