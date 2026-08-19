package com.github.alexmodguy.alexscaves.citadel.server.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class LecternBooks {

    public static Map<ResourceLocation, BookData> BOOKS = new HashMap<>();

    // Upstream's init() seeded this with Citadel's own guide book; that item is not vendored, so
    // the map starts empty and ACItemRegistry registers the cave book into it during setup.

    public static boolean isLecternBook(ItemStack stack) {
        return BOOKS.containsKey(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static class BookData {
        int bindingColor;
        int pageColor;

        public BookData(int bindingColor, int pageColor) {
            this.bindingColor = bindingColor;
            this.pageColor = pageColor;
        }

        public int getBindingColor() {
            return bindingColor;
        }

        public int getPageColor() {
            return pageColor;
        }
    }
}
