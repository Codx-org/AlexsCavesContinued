package com.github.alexmodguy.alexscaves.server.misc;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.item.ACItemRegistry;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;

/**
 * The four pottery sherds this mod adds, and the sprite each one paints onto a decorated pot.
 *
 * <p>1.21 gave the pot-pattern registry a real element type — {@code DecoratedPotPattern}, a record
 * around the sprite's {@code ResourceLocation} — where 1.20.x stored the sprite name as a bare
 * {@code String}, and dropped the plural from the registry key with it. Every declaration in the
 * first arm chain below names that element type, so its arms hold the whole registration block
 * rather than a line each; they register the same four sprites under the same four names either way.
 *
 * <p>The second chain is the separate question of how a sherd is bound to a pattern, which moves at
 * a different version — see the comment above it.
 */
public class ACPotPatternRegistry {

    //? if >=1.21 {
    /*public static final DeferredRegister<net.minecraft.world.level.block.entity.DecoratedPotPattern> DEF_REG =
            DeferredRegister.create(Registries.DECORATED_POT_PATTERN, AlexsCaves.MODID);

    public static final Supplier<net.minecraft.world.level.block.entity.DecoratedPotPattern> DINOSAUR = register("dinosaur_pottery_pattern");
    public static final Supplier<net.minecraft.world.level.block.entity.DecoratedPotPattern> FOOTPRINT = register("footprint_pottery_pattern");
    public static final Supplier<net.minecraft.world.level.block.entity.DecoratedPotPattern> GUARDIAN = register("guardian_pottery_pattern");
    public static final Supplier<net.minecraft.world.level.block.entity.DecoratedPotPattern> HERO = register("hero_pottery_pattern");

    private static Supplier<net.minecraft.world.level.block.entity.DecoratedPotPattern> register(String name) {
        return DEF_REG.register(name, () -> new net.minecraft.world.level.block.entity.DecoratedPotPattern(
                ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, name)));
    }

    // The pattern keys, spelled from the registration names — a Supplier handle has no getKey().
    private static ResourceKey<net.minecraft.world.level.block.entity.DecoratedPotPattern> patternKey(String name) {
        return ResourceKey.create(Registries.DECORATED_POT_PATTERN, ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, name));
    }
    *///?} else {
    public static final DeferredRegister<String> DEF_REG = DeferredRegister.create(Registries.DECORATED_POT_PATTERNS, AlexsCaves.MODID);

    public static final Supplier<String> DINOSAUR = DEF_REG.register("dinosaur_pottery_pattern", () -> AlexsCaves.MODID + ":dinosaur_pottery_pattern");
    public static final Supplier<String> FOOTPRINT = DEF_REG.register("footprint_pottery_pattern", () -> AlexsCaves.MODID + ":footprint_pottery_pattern");
    public static final Supplier<String> GUARDIAN = DEF_REG.register("guardian_pottery_pattern", () -> AlexsCaves.MODID + ":guardian_pottery_pattern");
    public static final Supplier<String> HERO = DEF_REG.register("hero_pottery_pattern", () -> AlexsCaves.MODID + ":hero_pottery_pattern");

    // The pattern keys, spelled from the registration names — a Supplier handle has no getKey().
    private static ResourceKey<String> patternKey(String name) {
        return ResourceKey.create(Registries.DECORATED_POT_PATTERNS, ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, name));
    }
    //?}

    // How the four sherds reach the renderer is the second axis, and it moves at a different version
    // from the element-type change above — hence a second arm chain rather than more arms in that one.
    //
    // 26.2 deleted the mutable ITEM_TO_POT_TEXTURE map outright, along with getPatternFromItem. What
    // replaced it is DecoratedPotPatterns#itemToPatternMappings(BiConsumer<ResourceKey<Item>,
    // ResourceKey<DecoratedPotPattern>>), a static enumeration of vanilla's own pairs whose only
    // consumer anywhere in the jar is the client DecoratedPotRenderer — so there is nothing left to
    // put a mapping *into*, and the mod contributes by appending to that enumeration instead, from
    // mixin.DecoratedPotPatternsMixin. That the keys are ResourceKeys rather than Item instances is
    // a bonus: nothing here touches the item registry any more, so nothing here can run too early.
    //? if >=26.2 {
    /*public static void expandVanillaDefinitions() {
    }

    public static void contributeItemToPatternMappings(java.util.function.BiConsumer<ResourceKey<Item>, ResourceKey<net.minecraft.world.level.block.entity.DecoratedPotPattern>> consumer) {
        consumer.accept(itemKey("dinosaur_pottery_sherd"), patternKey("dinosaur_pottery_pattern"));
        consumer.accept(itemKey("footprint_pottery_sherd"), patternKey("footprint_pottery_pattern"));
        consumer.accept(itemKey("guardian_pottery_sherd"), patternKey("guardian_pottery_pattern"));
        consumer.accept(itemKey("hero_pottery_sherd"), patternKey("hero_pottery_pattern"));
    }

    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, name));
    }
    *///?} elif >=1.21 {
    /*public static void expandVanillaDefinitions() {
        ImmutableMap.Builder<Item, ResourceKey<net.minecraft.world.level.block.entity.DecoratedPotPattern>> itemsToPot = new ImmutableMap.Builder<>();
        itemsToPot.putAll(DecoratedPotPatterns.ITEM_TO_POT_TEXTURE);
        itemsToPot.put(ACItemRegistry.DINOSAUR_POTTERY_SHERD.get(), patternKey("dinosaur_pottery_pattern"));
        itemsToPot.put(ACItemRegistry.FOOTPRINT_POTTERY_SHERD.get(), patternKey("footprint_pottery_pattern"));
        itemsToPot.put(ACItemRegistry.GUARDIAN_POTTERY_SHERD.get(), patternKey("guardian_pottery_pattern"));
        itemsToPot.put(ACItemRegistry.HERO_POTTERY_SHERD.get(), patternKey("hero_pottery_pattern"));
        DecoratedPotPatterns.ITEM_TO_POT_TEXTURE = itemsToPot.build();
    }
    *///?} else {
    public static void expandVanillaDefinitions() {
        ImmutableMap.Builder<Item, ResourceKey<String>> itemsToPot = new ImmutableMap.Builder<>();
        itemsToPot.putAll(DecoratedPotPatterns.ITEM_TO_POT_TEXTURE);
        itemsToPot.put(ACItemRegistry.DINOSAUR_POTTERY_SHERD.get(), patternKey("dinosaur_pottery_pattern"));
        itemsToPot.put(ACItemRegistry.FOOTPRINT_POTTERY_SHERD.get(), patternKey("footprint_pottery_pattern"));
        itemsToPot.put(ACItemRegistry.GUARDIAN_POTTERY_SHERD.get(), patternKey("guardian_pottery_pattern"));
        itemsToPot.put(ACItemRegistry.HERO_POTTERY_SHERD.get(), patternKey("hero_pottery_pattern"));
        DecoratedPotPatterns.ITEM_TO_POT_TEXTURE = itemsToPot.build();
    }
    //?}
}
