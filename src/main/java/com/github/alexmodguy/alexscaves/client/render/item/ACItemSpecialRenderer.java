package com.github.alexmodguy.alexscaves.client.render.item;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.citadel.client.CitadelItemstackRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The ISTER's replacement on &gt;=1.21.4: {@code minecraft:special} item-model renderers that route
 * straight back into the legacy renderer bodies, whose drawing code is deliberately kept compiling on
 * every node for exactly this re-wiring.
 *
 * <p>Two type ids, because Alex's Caves has two legacy renderers:
 * <ul>
 * <li>{@code alexscaves:item_renderer} → {@link ACItemstackRenderer}, the mod's own 21 hand-held 3D
 *     items (galena gauntlet, resistor shield, raygun, the four spears, beholder, dreadbow, …). Each
 *     of those branches reads the display context to decide between its model and a flat
 *     {@code *_SPRITE} item, and the context is a parameter of {@code render} on this era, so the
 *     port is faithful rather than a GUI-tuned approximation.</li>
 * <li>{@code alexscaves:icon} → {@link CitadelItemstackRenderer}, the two vendored Citadel display
 *     items ({@code icon_item}, {@code effect_item}) the mod's 30 advancement icons are drawn with.</li>
 * </ul>
 *
 * <p>Which items get which is decided entirely by {@code DataPackMigration.writeItemModelDefinitions}
 * — it is the set of item models that used to name the deleted {@code builtin/entity} parent — not by
 * anything here.
 *
 * <p>Both type ids are registered by {@link ACItemModelShims}, along with the mod's tint source and
 * range-select property.
 *
 * <p>Below 1.21.4 this compiles to a dead plain class: the implements clauses and every override are
 * gated, and nothing registers or references it there (the ISTER still exists and is wired through
 * {@link ACItemRenderProperties} / {@code CitadelItemRenderProperties}).
 */
public class ACItemSpecialRenderer
        //? if >=1.21.4
        /*implements net.minecraft.client.renderer.special.SpecialModelRenderer<ItemStack>*/
{

    /**
     * One shared legacy renderer instance, the way the single wired ISTER used to be: it holds the
     * dreadbow's cached arrow entity and the sepia flag.
     */
    private static final ACItemstackRenderer RENDERER = new ACItemstackRenderer();

    /** The type id of the mod's own item renderer, as written into the item model definitions. */
    public static ResourceLocation itemRendererId() {
        return ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "item_renderer");
    }

    /** The type id of the vendored Citadel icon renderer, as written into the item model definitions. */
    public static ResourceLocation iconId() {
        return ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "icon");
    }

    //? if >=1.21.4
    /*@Override*/
    public ItemStack extractArgument(ItemStack stack) {
        // The stack IS the argument: every branch of renderByItem reads the stack (its item, its
        // custom data, whether its holder is using it). Copy so the render state never aliases a
        // live stack.
        return stack.copy();
    }

    // 1.21.4–1.21.8: the special renderer still *draws*, straight into the buffer source the item
    // pipeline handed it.
    //? if >=1.21.4 && <1.21.9 {
    /*@Override
    public void render(ItemStack stack, net.minecraft.world.item.ItemDisplayContext displayContext,
                       com.mojang.blaze3d.vertex.PoseStack poseStack,
                       net.minecraft.client.renderer.MultiBufferSource buffers,
                       int packedLight, int packedOverlay, boolean hasFoil) {
        if (stack != null && !stack.isEmpty()) {
            RENDERER.renderByItem(stack, displayContext, poseStack, buffers, packedLight, packedOverlay);
        }
    }
    *///?}

    // 1.21.9 turned drawing into submitting: the legacy body records into an ACSubmitBuffers and the
    // flush replays it through SubmitNodeCollector#submitCustomGeometry. The trailing outlineColor is
    // dropped for the same reason the entity renderers drop it — submitCustomGeometry carries no
    // outline, so a glowing item of this mod draws normally but without its outline.
    //
    // 26 then dropped the ItemDisplayContext from the signature. The legacy body needs it — it picks
    // the hand, the GUI sprite and the ground lighting from it — so it comes from
    // ACItemDisplayContexts, which mixin.client.ItemStackRenderStateMixin publishes around the submit
    // this call runs inside. Read once, exactly where the parameter used to be read.
    //? if >=26 {
    /*@Override
    public void submit(ItemStack stack, com.mojang.blaze3d.vertex.PoseStack poseStack,
                       net.minecraft.client.renderer.SubmitNodeCollector collector,
                       int packedLight, int packedOverlay, boolean hasFoil, int outlineColor) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers buffers =
                new com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers(collector);
        RENDERER.renderByItem(stack, ACItemDisplayContexts.current(), poseStack, buffers, packedLight, packedOverlay);
        buffers.flush();
    }
    *///?} elif >=1.21.9 {
    /*@Override
    public void submit(ItemStack stack, net.minecraft.world.item.ItemDisplayContext displayContext,
                       com.mojang.blaze3d.vertex.PoseStack poseStack,
                       net.minecraft.client.renderer.SubmitNodeCollector collector,
                       int packedLight, int packedOverlay, boolean hasFoil, int outlineColor) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers buffers =
                new com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers(collector);
        RENDERER.renderByItem(stack, displayContext, poseStack, buffers, packedLight, packedOverlay);
        buffers.flush();
    }
    *///?}

    // 1.21.11 turned the collector inside out: getExtents is handed a Consumer<Vector3fc> to push
    // corners into rather than a Set<Vector3f> to add them to.
    //? if >=1.21.11 {
    /*@Override
    public void getExtents(java.util.function.Consumer<org.joml.Vector3fc> extents) {
        unitCube(extents);
    }
    *///?} elif >=1.21.6 {
    /*@Override
    public void getExtents(java.util.Set<org.joml.Vector3f> extents) {
        unitCube(extents::add);
    }
    *///?}

    /**
     * The corners of the box a plain item model would occupy, in the centred model space
     * {@code FaceBakery} bakes into (its {@code x - 0.5F}): {@code -0.5} to {@code 0.5} on each axis.
     *
     * <p>1.21.6 made every {@code SpecialModelRenderer} declare its own extents, because they are no
     * longer derivable from quads the renderer does not have. Two things read them, and both go wrong
     * on an empty set rather than degrading: {@code ItemEntityRenderer} translates a dropped item by
     * {@code -minY}, which is {@code +Infinity} for an empty {@code AABB.Builder} and puts the item
     * nowhere, and the GUI sizes an oversized item's scissor rectangle from the same box.
     *
     * <p>The mod's twenty-odd hand-held models are drawn from entity geometry that is picked per
     * display context and per stack, so there is nothing cheap and honest to measure — a spear is
     * longer than this and an icon smaller. The plain item box is what these items got on every
     * earlier version, where the box came from the model file they still nominally carry, so it keeps
     * them looking the same rather than making a new claim.
     */
    // Takes a Consumer<? super Vector3f> so that both collector shapes fit it: 1.21.6's
    // Set<Vector3f> as a method reference, and 1.21.11's Consumer<Vector3fc> directly.
    //? if >=1.21.6 {
    /*private static void unitCube(java.util.function.Consumer<? super org.joml.Vector3f> extents) {
        for (int corner = 0; corner < 8; corner++) {
            extents.accept(new org.joml.Vector3f(
                    (corner & 1) == 0 ? -0.5F : 0.5F,
                    (corner & 2) == 0 ? -0.5F : 0.5F,
                    (corner & 4) == 0 ? -0.5F : 0.5F));
        }
    }
    *///?}

    /** Stateless: the codec is a unit and bake ignores its context. */
    public static final class Unbaked
            //? if >=1.21.4
            /*implements net.minecraft.client.renderer.special.SpecialModelRenderer.Unbaked*/
    {
        public static final com.mojang.serialization.MapCodec<Unbaked> MAP_CODEC =
                com.mojang.serialization.MapCodec.unit(new Unbaked());

        // 1.21.9 replaced bake's bare EntityModelSet with a BakingContext carrying it plus the
        // atlases and the item model resolver. Neither renderer reads any of it.
        //? if >=1.21.4 && <1.21.9 {
        /*@Override
        public net.minecraft.client.renderer.special.SpecialModelRenderer<?> bake(net.minecraft.client.model.geom.EntityModelSet models) {
            return new ACItemSpecialRenderer();
        }
        *///?}
        //? if >=1.21.9 {
        /*@Override
        public net.minecraft.client.renderer.special.SpecialModelRenderer<?> bake(net.minecraft.client.renderer.special.SpecialModelRenderer.BakingContext context) {
            return new ACItemSpecialRenderer();
        }
        *///?}

        //? if >=1.21.4
        /*@Override*/
        public com.mojang.serialization.MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }

    /**
     * The vendored Citadel display items, drawn by {@link CitadelItemstackRenderer}. Separate from the
     * outer renderer only because a {@code minecraft:special} definition names exactly one type id and
     * the two legacy renderers are two different objects.
     */
    public static class Icon
            //? if >=1.21.4
            /*implements net.minecraft.client.renderer.special.SpecialModelRenderer<ItemStack>*/
    {

        private static final CitadelItemstackRenderer RENDERER = new CitadelItemstackRenderer();

        //? if >=1.21.4
        /*@Override*/
        public ItemStack extractArgument(ItemStack stack) {
            return stack.copy();
        }

        //? if >=1.21.4 && <1.21.9 {
        /*@Override
        public void render(ItemStack stack, net.minecraft.world.item.ItemDisplayContext displayContext,
                           com.mojang.blaze3d.vertex.PoseStack poseStack,
                           net.minecraft.client.renderer.MultiBufferSource buffers,
                           int packedLight, int packedOverlay, boolean hasFoil) {
            if (stack != null && !stack.isEmpty()) {
                RENDERER.renderByItem(stack, displayContext, poseStack, buffers, packedLight, packedOverlay);
            }
        }
        *///?}

        /** @see ACItemSpecialRenderer#submit */
        //? if >=26 {
        /*@Override
        public void submit(ItemStack stack, com.mojang.blaze3d.vertex.PoseStack poseStack,
                           net.minecraft.client.renderer.SubmitNodeCollector collector,
                           int packedLight, int packedOverlay, boolean hasFoil, int outlineColor) {
            if (stack == null || stack.isEmpty()) {
                return;
            }
            com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers buffers =
                    new com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers(collector);
            RENDERER.renderByItem(stack, ACItemDisplayContexts.current(), poseStack, buffers, packedLight, packedOverlay);
            buffers.flush();
        }
        *///?} elif >=1.21.9 {
        /*@Override
        public void submit(ItemStack stack, net.minecraft.world.item.ItemDisplayContext displayContext,
                           com.mojang.blaze3d.vertex.PoseStack poseStack,
                           net.minecraft.client.renderer.SubmitNodeCollector collector,
                           int packedLight, int packedOverlay, boolean hasFoil, int outlineColor) {
            if (stack == null || stack.isEmpty()) {
                return;
            }
            com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers buffers =
                    new com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers(collector);
            RENDERER.renderByItem(stack, displayContext, poseStack, buffers, packedLight, packedOverlay);
            buffers.flush();
        }
        *///?}

        /** @see ACItemSpecialRenderer#unitCube */
        //? if >=1.21.11 {
        /*@Override
        public void getExtents(java.util.function.Consumer<org.joml.Vector3fc> extents) {
            unitCube(extents);
        }
        *///?} elif >=1.21.6 {
        /*@Override
        public void getExtents(java.util.Set<org.joml.Vector3f> extents) {
            unitCube(extents::add);
        }
        *///?}

        /** Stateless: the codec is a unit and bake ignores its context. */
        public static final class Unbaked
                //? if >=1.21.4
                /*implements net.minecraft.client.renderer.special.SpecialModelRenderer.Unbaked*/
        {
            public static final com.mojang.serialization.MapCodec<Unbaked> MAP_CODEC =
                    com.mojang.serialization.MapCodec.unit(new Unbaked());

            //? if >=1.21.4 && <1.21.9 {
            /*@Override
            public net.minecraft.client.renderer.special.SpecialModelRenderer<?> bake(net.minecraft.client.model.geom.EntityModelSet models) {
                return new Icon();
            }
            *///?}
            //? if >=1.21.9 {
            /*@Override
            public net.minecraft.client.renderer.special.SpecialModelRenderer<?> bake(net.minecraft.client.renderer.special.SpecialModelRenderer.BakingContext context) {
                return new Icon();
            }
            *///?}

            //? if >=1.21.4
            /*@Override*/
            public com.mojang.serialization.MapCodec<Unbaked> type() {
                return MAP_CODEC;
            }
        }
    }
}
