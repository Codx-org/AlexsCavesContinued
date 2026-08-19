package com.github.alexmodguy.alexscaves.client.render.item;

//? if <1.21.4
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * The loader hook that hands {@link ACItemstackRenderer} to the item pipeline below 1.21.4.
 * <p>
 * 1.21.4 deleted {@code getCustomRenderer} along with the ISTER mechanism, so from there this is an
 * empty extension object — the items still route their {@code initializeClient}/client-extension
 * registration here harmlessly, and the renderer is reached through {@link ACItemSpecialRenderer}
 * instead.
 */
public class ACItemRenderProperties implements IClientItemExtensions {

    //? if <1.21.4 {
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return new ACItemstackRenderer();
    }
    //?}
}
