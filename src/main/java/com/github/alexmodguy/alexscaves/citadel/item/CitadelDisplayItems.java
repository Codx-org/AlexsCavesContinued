package com.github.alexmodguy.alexscaves.citadel.item;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.misc.ACDeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;

/**
 * Citadel's two NBT-driven display items, which Alex's Caves' advancement JSONs use as icons:
 * {@code icon_item} (draws the texture named by the {@code IconLocation} tag) and
 * {@code effect_item} (draws the mob-effect sprite named by the {@code DisplayEffect} tag).
 * <p>
 * They are registered under the {@code alexscaves} namespace, not {@code citadel} — the advancement
 * JSONs were rewritten to match. Citadel's third display item, {@code fancy_item}, is not used by
 * this mod and is not vendored. Neither item is placed in a creative tab.
 */
public class CitadelDisplayItems {

    public static final ACDeferredRegister<Item> DEF_REG =
            ACDeferredRegister.create(Registries.ITEM, AlexsCaves.MODID);

    public static final Supplier<Item> EFFECT_ITEM =
            DEF_REG.register("effect_item", () -> new ItemCustomRender(new Item.Properties().stacksTo(1)));

    public static final Supplier<Item> ICON_ITEM =
            DEF_REG.register("icon_item", () -> new ItemCustomRender(new Item.Properties().stacksTo(1)));

    private CitadelDisplayItems() {
    }

    public static void register(IEventBus modEventBus) {
        DEF_REG.raw().register(modEventBus);
    }
}
