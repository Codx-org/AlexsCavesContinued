package com.github.alexmodguy.alexscaves.client.gui.book.widget;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.render.ACRenderTypes;
import com.github.alexmodguy.alexscaves.citadel.recipe.SpecialRecipeInGuideBook;
import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import static net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

public class CraftingRecipeWidget extends BookWidget {

    @Expose
    @SerializedName("recipe_id")
    private String recipeId;
    @Expose
    private boolean sepia;

    @Expose(serialize = false, deserialize = false)
    private Recipe recipe;

    private static final int GRID_TEXTURE_SIZE = 64;

    @Expose(serialize = false, deserialize = false)
    private boolean smelting = false;

    private static final ResourceLocation CRAFTING_GRID_TEXTURE = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "textures/gui/book/crafting_grid.png");
    private static final ResourceLocation SMELTING_GRID_TEXTURE = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "textures/gui/book/smelting_grid.png");

    public CraftingRecipeWidget(int displayPage, String recipeId, boolean sepia, int x, int y, float scale) {
        super(displayPage, Type.CRAFTING_RECIPE, x, y, scale);
        this.recipeId = recipeId;
        this.sepia = sepia;
    }

    public void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float partialTicks, boolean onFlippingPage) {
        if (recipe == null && recipeId != null) {
            recipe = getRecipeByName(recipeId);
            if(recipe instanceof AbstractCookingRecipe){
                smelting = true;
            }
        }
        if(recipe != null){
            float itemScale = 16.0F;
            float playerTicks = Minecraft.getInstance().player.tickCount;
            VertexConsumer vertexconsumer = bufferSource.getBuffer(ACRenderTypes.getBookWidget(smelting ? SMELTING_GRID_TEXTURE : CRAFTING_GRID_TEXTURE, sepia));
            poseStack.pushPose();
            poseStack.translate(getX(), getY(), 0);
            poseStack.scale(getScale(), getScale(), 1);
            poseStack.pushPose();
            poseStack.scale(1.5F, 1.5F, 1);
            PoseStack.Pose posestack$pose = poseStack.last();
            Matrix4f matrix4f = posestack$pose.pose();
            Matrix3f matrix3f = posestack$pose.normal();
            float scaledU1 = 55 / (float)GRID_TEXTURE_SIZE;
            float scaledV1 = 37 / (float)GRID_TEXTURE_SIZE;
            float texWidth = 55 / 2F;
            float texHeight = 37 / 2F;
            vertexconsumer.vertex(matrix4f, -texWidth, -texHeight, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F).uv(0, 0).overlayCoords(NO_OVERLAY).uv2(240).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
            vertexconsumer.vertex(matrix4f, texWidth, -texHeight, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F).uv(scaledU1, 0).overlayCoords(NO_OVERLAY).uv2(240).normal(matrix3f, 0.0F, 1.0F, 0.0F).uv(0, scaledV1).endVertex();
            vertexconsumer.vertex(matrix4f, texWidth, texHeight, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F).uv(scaledU1, scaledV1).overlayCoords(NO_OVERLAY).uv2(240).normal(matrix3f, 0.0F, 1.0F, 0.0F).uv(0, 0).endVertex();
            vertexconsumer.vertex(matrix4f, -texWidth, texHeight, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F).uv(0, scaledV1).overlayCoords(NO_OVERLAY).uv2(240).normal(matrix3f, 0.0F, 1.0F, 0.0F).uv(scaledU1, 0).endVertex();
            poseStack.popPose();


            if(smelting){
                poseStack.pushPose();
                poseStack.translate(43, -15, 0);
                poseStack.scale(1.35F, 1.35F, 1);
                ItemWidget.renderItem(ACCompat.recipeResult(recipe, Minecraft.getInstance().level), poseStack, bufferSource, sepia, itemScale * 1.25F);
                poseStack.popPose();

                java.util.List<ItemStack[]> slots = ACCompat.recipeDisplaySlots(recipe, Minecraft.getInstance().level);
                ItemStack stack = slots.isEmpty() ? ItemStack.EMPTY : cycle(slots.get(0), playerTicks);

                poseStack.pushPose();
                poseStack.translate(-27.5F, -12.5F, 0);
                ItemWidget.renderItem(stack, poseStack, bufferSource, sepia, itemScale);
                poseStack.popPose();

            }else{
                poseStack.pushPose();
                poseStack.translate(57, 2, 0);
                poseStack.scale(1.35F, 1.35F, 1);
                ItemWidget.renderItem(ACCompat.recipeResult(recipe, Minecraft.getInstance().level), poseStack, bufferSource, sepia, itemScale * 1.25F);
                poseStack.popPose();

                java.util.List<ItemStack[]> ingredients = recipe instanceof SpecialRecipeInGuideBook ? ((SpecialRecipeInGuideBook)recipe).getDisplayIngredients() : ACCompat.recipeDisplaySlots(recipe, Minecraft.getInstance().level);
                NonNullList<ItemStack> displayedStacks = NonNullList.create();
                int width = 3;
                int height = 3;
                if(recipe instanceof ShapedRecipe shapedRecipe){
                    width = shapedRecipe.getWidth();
                    height = shapedRecipe.getHeight();
                }
                int renderY = 0;
                int renderX = 0;
                for (int i = 0; i < ingredients.size(); i++) {
                    ItemStack stack = cycle(ingredients.get(i), playerTicks);
                    if(i % width == 0){
                        if(i != 0){
                            renderY++;
                        }
                        renderX = 0;
                    }else{
                        renderX++;
                    }
                    if (!stack.isEmpty()) {
                        poseStack.pushPose();
                        poseStack.translate(-33 + renderX * 18.75F, -18.5F + renderY * 19.5F, 0);
                        ItemWidget.renderItem(stack, poseStack, bufferSource, sepia, itemScale);
                        poseStack.popPose();
                    }
                    displayedStacks.add(i, stack);
                }
            }
            poseStack.popPose();
        }
    }

    /** The stack a display slot is showing right now — slots with alternatives cycle once a second. */
    private static ItemStack cycle(ItemStack[] options, float playerTicks) {
        if (options == null || options.length == 0) {
            return ItemStack.EMPTY;
        }
        if (options.length == 1) {
            return options[0];
        }
        return options[(int) ((playerTicks / 20F) % options.length)];
    }

    // 1.21.2 took the RecipeManager off the client entirely — a connected client is sent only the
    // recipes it needs for its recipe book, and Level#getRecipeManager is gone (ServerLevel answers
    // it as recipeAccess()). The book is a static illustration of this mod's own recipes, so from
    // that version it reads the recipe JSON straight out of the jar and decodes it, which needs no
    // server at all. Every recipe_id in this mod's book pages is alexscaves:-namespaced, so nothing
    // outside the jar is ever asked for.
    private Recipe getRecipeByName(String registryName) {
        try {
            //? if >=1.21.2 {
            /*ResourceLocation id = ResourceLocation.parse(registryName);
            // DataPackMigration renames the folder to the 1.21 singular spelling at build time.
            String path = "/data/" + id.getNamespace() + "/recipe/" + id.getPath() + ".json";
            try (java.io.InputStream in = CraftingRecipeWidget.class.getResourceAsStream(path)) {
                if (in == null) {
                    return null;
                }
                com.google.gson.JsonElement json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
                com.mojang.serialization.DynamicOps<com.google.gson.JsonElement> ops = Minecraft.getInstance().level.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
                return Recipe.CODEC.parse(ops, json).result().orElse(null);
            }
            *///?} elif >=1.20.2 {
            /*// 1.20.2: byKey answers with the RecipeHolder rather than the recipe itself. (Note the
            // opening of the block comment above: a `//` line between the gate and the `/*` makes
            // Stonecutter read the whole branch as line-commented, so activating it strips the `//`
            // off the prose as well and the text compiles as code.)
            RecipeManager manager = Minecraft.getInstance().level.getRecipeManager();
            if (manager.byKey(ResourceLocation.parse(registryName)).isPresent()) {
                return manager.byKey(ResourceLocation.parse(registryName)).get().value();
            }
            *///?} else {
            RecipeManager manager = Minecraft.getInstance().level.getRecipeManager();
            if (manager.byKey(ResourceLocation.parse(registryName)).isPresent()) {
                return manager.byKey(ResourceLocation.parse(registryName)).get();
            }
            //?}
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
