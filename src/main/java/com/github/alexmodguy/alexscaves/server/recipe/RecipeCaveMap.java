package com.github.alexmodguy.alexscaves.server.recipe;

import com.github.alexmodguy.alexscaves.server.item.ACItemRegistry;
import com.github.alexmodguy.alexscaves.server.item.CaveInfoItem;
import com.github.alexmodguy.alexscaves.server.item.CaveMapItem;
import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.github.alexmodguy.alexscaves.citadel.recipe.SpecialRecipeInGuideBook;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.biome.Biome;

public class RecipeCaveMap extends ShapedRecipe implements SpecialRecipeInGuideBook {
    // 1.20.2 lifted a recipe's id out of Recipe into the RecipeHolder that wraps it, so recipe
    // constructors — and the serializer factories that call them — stopped being handed one.
    // 1.20.3 — a version later, not the same one — folded a shaped recipe's width, height and
    // ingredients into ShapedRecipePattern, which is why 1.20.2 gets an arm of its own here.
    // (javap on the vanilla 1.20.2 jar: ShapedRecipe(String, CraftingBookCategory, int, int,
    // NonNullList, ItemStack). No loader publishes a 1.20.2 or 1.20.3 build, so this tree's walk
    // went 1.20.1 → 1.20.4 and the two changes looked like one until Fabric reached them.)
    // 26 bundled a shaped recipe's constructor arguments: the notification flag became
    // Recipe$CommonInfo, the group and book category became CraftingRecipe$CraftingBookInfo, and the
    // result became an ItemStackTemplate. `new CommonInfo(true)` is what the old four-argument
    // constructor filled in, and the empty group is now the book info's second field.
    // ⚠ The template is built from the ITEM, never from `new ItemStack(...)`. A recipe is decoded
    // inside RecipeManager.prepare, and on 26.2 an item's default components are still unbound at that
    // point — ReloadableServerResources only calls PendingComponents::apply from
    // updateComponentsAndStaticRegistryTags(), after every reload listener has finished. `new
    // ItemStack(ItemLike)` copies item.components() eagerly, so it dies with "Components not bound yet"
    // and takes the whole datapack load with it. ItemStackTemplate(Item) only keeps the holder.
    //? if >=26 {
    /*public RecipeCaveMap(net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo bookInfo) {
        super(new net.minecraft.world.item.crafting.Recipe.CommonInfo(true), bookInfo,
                new net.minecraft.world.item.crafting.ShapedRecipePattern(3, 3, ingredients(), java.util.Optional.empty()),
                new net.minecraft.world.item.ItemStackTemplate(ACItemRegistry.CAVE_MAP.get()));
    }

    public static net.minecraft.world.item.crafting.RecipeSerializer<RecipeCaveMap> serializer() {
        return new net.minecraft.world.item.crafting.RecipeSerializer<>(
                net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo.MAP_CODEC.xmap(RecipeCaveMap::new, r -> r.bookInfo),
                net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo.STREAM_CODEC.map(RecipeCaveMap::new, r -> r.bookInfo));
    }
    *///?} elif >=1.20.3 {
    /*public RecipeCaveMap(CraftingBookCategory category) {
        super("", category, new net.minecraft.world.item.crafting.ShapedRecipePattern(3, 3, ingredients(), java.util.Optional.empty()), new ItemStack(ACItemRegistry.CAVE_MAP.get()));
    }
    *///?} elif >=1.20.2 {
    /*public RecipeCaveMap(CraftingBookCategory category) {
        super("", category, 3, 3, ingredients(), new ItemStack(ACItemRegistry.CAVE_MAP.get()));
    }
    *///?}
    //? if <1.20.2 {
    public RecipeCaveMap(ResourceLocation name, CraftingBookCategory category) {
        super(name, "", category, 3, 3, ingredients(), new ItemStack(ACItemRegistry.CAVE_MAP.get()));
    }
    //?}

    /** The nine slots, in grid order — the one shape both eras and the guide book all read. */
    private static java.util.List<Ingredient> slots() {
        Ingredient paper = Ingredient.of(Items.PAPER);
        return java.util.List.of(
                paper, paper, paper,
                paper, Ingredient.of(ACItemRegistry.CAVE_CODEX.get()), paper,
                paper, paper, paper);
    }

    // A shaped pattern took a NonNullList of ingredients until 1.21.2 made every slot optional.
    //? if >=1.21.2 {
    /*private static java.util.List<java.util.Optional<Ingredient>> ingredients() {
        return slots().stream().map(java.util.Optional::of).toList();
    }
    *///?} else {
    private static NonNullList<Ingredient> ingredients() {
        NonNullList<Ingredient> list = NonNullList.withSize(9, Ingredient.EMPTY);
        java.util.List<Ingredient> slots = slots();
        for (int i = 0; i < slots.size(); ++i) {
            list.set(i, slots.get(i));
        }
        return list;
    }
    //?}

    // The crafting input and the registry handle both changed under this method: 1.20.5 swapped
    // RegistryAccess for HolderLookup.Provider, and 1.21 replaced the CraftingContainer with a
    // CraftingInput. 26 dropped the registry handle again — nothing in vanilla's own assemble bodies
    // used it. Only the signature differs — the body reads a slot count and an item per slot.
    //? if >=26 {
    /*public ItemStack assemble(net.minecraft.world.item.crafting.CraftingInput container) {
        int slotCount = container.size();
    *///?} elif >=1.21 {
    /*public ItemStack assemble(net.minecraft.world.item.crafting.CraftingInput container, net.minecraft.core.HolderLookup.Provider registryAccess) {
        int slotCount = container.size();
    *///?} elif >=1.20.5 {
    /*public ItemStack assemble(CraftingContainer container, net.minecraft.core.HolderLookup.Provider registryAccess) {
        int slotCount = container.getContainerSize();
    *///?} else {
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        int slotCount = container.getContainerSize();
    //?}
        ItemStack scroll = ItemStack.EMPTY;
        for (int i = 0; i < slotCount && i < this.getWidth() * this.getHeight(); ++i) {
            if (!container.getItem(i).isEmpty() && container.getItem(i).is(ACItemRegistry.CAVE_CODEX.get())) {
                if (scroll.isEmpty()) {
                    scroll = container.getItem(i);
                }
            }
        }
        ResourceKey<Biome> key = CaveInfoItem.getCaveBiome(scroll);
        if (key != null) {
            return CaveMapItem.createMap(key);
        }
        return ItemStack.EMPTY;
    }

    // 1.21.2 narrowed the declared return type to the recipe's own family, so a plain wildcard no
    // longer overrides it. 26 narrowed it once more — ShapedRecipe declares RecipeSerializer<ShapedRecipe>
    // exactly, and RecipeSerializer is a final record now, so a wildcard is not a subtype of it either.
    //? if >=26 {
    /*public net.minecraft.world.item.crafting.RecipeSerializer<ShapedRecipe> getSerializer() {
        return (net.minecraft.world.item.crafting.RecipeSerializer<ShapedRecipe>) (net.minecraft.world.item.crafting.RecipeSerializer<?>) ACRecipeRegistry.CAVE_MAP.get();
    }
    *///?} elif >=1.21.2 {
    /*public RecipeSerializer<? extends ShapedRecipe> getSerializer() {
        return (RecipeSerializer<? extends ShapedRecipe>) (RecipeSerializer<?>) ACRecipeRegistry.CAVE_MAP.get();
    }
    *///?} else {
    public RecipeSerializer<?> getSerializer() {
        return ACRecipeRegistry.CAVE_MAP.get();
    }
    //?}

    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    public boolean isSpecial() {
        return true;
    }

    @Override
    public java.util.List<ItemStack[]> getDisplayIngredients() {
        java.util.List<ItemStack[]> display = new java.util.ArrayList<>();
        for (Ingredient ingredient : slots()) {
            display.add(ACCompat.ingredientItems(ingredient));
        }
        return display;
    }

    @Override
    public ItemStack getDisplayResultFor(NonNullList<ItemStack> nonNullList) {
        ItemStack scroll = ItemStack.EMPTY;
        for (int i = 0; i < nonNullList.size(); ++i) {
            if (!nonNullList.get(i).isEmpty() && nonNullList.get(i).is(ACItemRegistry.CAVE_CODEX.get())) {
                if (scroll.isEmpty()) {
                    scroll = nonNullList.get(i);
                }
            }
        }
        ResourceKey<Biome> key = CaveInfoItem.getCaveBiome(scroll);
        if (key != null) {
            return CaveMapItem.createMap(key);
        }
        return ItemStack.EMPTY;
    }
}
