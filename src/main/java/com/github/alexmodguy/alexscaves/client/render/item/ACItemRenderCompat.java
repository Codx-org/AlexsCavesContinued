package com.github.alexmodguy.alexscaves.client.render.item;

/**
 * The pieces of the &gt;=1.21.4 item pipeline that cannot be spelled at all on older versions.
 *
 * <p>1.21.4 deleted {@code ItemRenderer#getModel} and with it the whole "hand me the baked model and
 * I'll draw its quads myself" idiom this mod used in five places. What replaced it is
 * {@code ItemModelResolver} + {@code ItemStackRenderState}: a stack is resolved into a small list of
 * layers, and rendering a layer pulls its buffer out of a {@code MultiBufferSource}. That last detail
 * is what makes the port faithful — a mod that wants an item drawn into <em>its own</em> render type
 * no longer intercepts the quads, it intercepts the buffer lookup.
 *
 * <p>Everything below 1.21.4 is served by {@code ACClientCompat}'s own arm, which still walks the
 * baked model. This class compiles to an empty shell there and nothing references it, which is also
 * why every member lives inside one gate written with line comments only: a nested block comment
 * would not survive being commented out.
 */
public final class ACItemRenderCompat {

    private ACItemRenderCompat() {
    }

    //? if >=1.21.4 {
    /*// Resolves a stack the way vanilla's own top-level item draw does: no holder, no seed, not
    // left-handed. The render state is a scratch object, so a fresh one per call.
    private static net.minecraft.client.renderer.item.ItemStackRenderState resolve(
            net.minecraft.world.item.ItemStack stack,
            net.minecraft.world.level.Level level,
            net.minecraft.world.item.ItemDisplayContext ctx) {
        net.minecraft.client.renderer.item.ItemStackRenderState state =
                new net.minecraft.client.renderer.item.ItemStackRenderState();
        net.minecraft.client.Minecraft.getInstance().getItemModelResolver()
                .updateForTopItem(state, stack, ctx, false, level, null, 0);
        return state;
    }

    // Draws a resolved item, every layer of it, through a caller-supplied RenderType -> VertexConsumer
    // lookup. Written as its own one-line method because that line is what the `!mc219-itemstate-submit`
    // replacement rule rewrites: 1.21.9 deleted the immediate-mode render in favour of submitting to a
    // SubmitNodeCollector, so the lookup has to be wrapped in ACDrawCollector, which draws it straight
    // back out. This class already sits inside a `>=1.21.4` Stonecutter arm and arms do not nest.
    private static void draw(net.minecraft.client.renderer.item.ItemStackRenderState state,
                             com.mojang.blaze3d.vertex.PoseStack poseStack,
                             java.util.function.Function<net.minecraft.client.renderer.RenderType, com.mojang.blaze3d.vertex.VertexConsumer> lookup,
                             int light, int overlay) {
        state.render(poseStack, lookup::apply, light, overlay);
    }

    // Whether the item draws as a block-shaped model — BakedModel#isGui3d's replacement.
    public static boolean isGui3d(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.Level level) {
        return resolve(stack, level, net.minecraft.world.item.ItemDisplayContext.FIXED).isGui3d();
    }

    // The render state a breaking-item particle picks its sprite from. Mirrors vanilla's own provider,
    // down to the GROUND display context and the null holder.
    public static net.minecraft.client.renderer.item.ItemStackRenderState particleState(
            net.minecraft.world.item.ItemStack stack, net.minecraft.client.multiplayer.ClientLevel level) {
        return resolve(stack, level, net.minecraft.world.item.ItemDisplayContext.GROUND);
    }

    // Draws every layer of the item into one caller-chosen buffer, multiplied by (r, g, b, alpha).
    //
    // The layer render applies the display transform and vanilla's -0.5 recentre itself, so the result
    // lands where ItemRenderer#render would have put it — which is what the old quad walk did too, once
    // its caller had applied the transform by hand.
    public static void renderTinted(net.minecraft.world.item.ItemStack stack,
                                    net.minecraft.world.level.Level level,
                                    net.minecraft.world.item.ItemDisplayContext ctx,
                                    com.mojang.blaze3d.vertex.PoseStack poseStack,
                                    com.mojang.blaze3d.vertex.VertexConsumer target,
                                    float r, float g, float b, float alpha,
                                    int light, int overlay) {
        com.mojang.blaze3d.vertex.VertexConsumer tinted = new Tinted(target, r, g, b, alpha);
        draw(resolve(stack, level, ctx), poseStack, type -> tinted, light, overlay);
    }

    // Draws the item with the two standard item sheets swapped for sepia, leaving every other render
    // type alone.
    //
    // Below 1.21.4 the equivalent was "walk the quads unless the model is a custom renderer", and the
    // reason for that guard was exactly this: a custom renderer draws entity models out of its own
    // textures, and forcing those through the sepia sheet would paint them with the block atlas. The
    // layers of a plain item model, on the other hand, only ever ask for translucentItemSheet or
    // cutoutBlockSheet — the two answers ItemBlockRenderTypes gives — so redirecting just those two
    // reproduces the old split without having to ask whether a renderer is custom.
    // ACItemstackRenderer.sepiaFlag still does the sepia work for the custom ones.
    public static void renderSepia(net.minecraft.world.item.ItemStack stack,
                                   net.minecraft.world.level.Level level,
                                   net.minecraft.world.item.ItemDisplayContext ctx,
                                   com.mojang.blaze3d.vertex.PoseStack poseStack,
                                   net.minecraft.client.renderer.MultiBufferSource buffers,
                                   net.minecraft.client.renderer.RenderType sepia,
                                   int light, int overlay) {
        net.minecraft.client.renderer.RenderType translucent = net.minecraft.client.renderer.Sheets.translucentItemSheet();
        net.minecraft.client.renderer.RenderType cutout = net.minecraft.client.renderer.Sheets.cutoutBlockSheet();
        draw(resolve(stack, level, ctx), poseStack,
                type -> buffers.getBuffer(type == translucent || type == cutout ? sepia : type),
                light, overlay);
    }

    // A pass-through vertex consumer that scales every colour written through it. The old quad walk
    // handed putBulkData a flat (r, g, b, alpha) and told it to ignore the quad's own tint; scaling
    // instead of replacing is the same thing for an untinted model and the better answer for a tinted
    // one, so nothing that used to work changes.
    //
    // Only setColor(int, int, int, int) is overridden: every other colour entry point on the interface
    // (setColor(int), setColor(float, float, float, float), addVertex(...), putBulkData(...)) is a
    // default method that funnels into it, and a default dispatches on this.
    private static final class Tinted implements com.mojang.blaze3d.vertex.VertexConsumer {

        private final com.mojang.blaze3d.vertex.VertexConsumer delegate;
        private final float r;
        private final float g;
        private final float b;
        private final float a;

        private Tinted(com.mojang.blaze3d.vertex.VertexConsumer delegate, float r, float g, float b, float a) {
            this.delegate = delegate;
            this.r = net.minecraft.util.Mth.clamp(r, 0.0F, 1.0F);
            this.g = net.minecraft.util.Mth.clamp(g, 0.0F, 1.0F);
            this.b = net.minecraft.util.Mth.clamp(b, 0.0F, 1.0F);
            this.a = net.minecraft.util.Mth.clamp(a, 0.0F, 1.0F);
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x, float y, float z) {
            this.delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.delegate.setColor((int) (red * this.r), (int) (green * this.g), (int) (blue * this.b), (int) (alpha * this.a));
            return this;
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer setUv(float u, float v) {
            this.delegate.setUv(u, v);
            return this;
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int u, int v) {
            this.delegate.setUv1(u, v);
            return this;
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int u, int v) {
            this.delegate.setUv2(u, v);
            return this;
        }

        @Override
        public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float x, float y, float z) {
            this.delegate.setNormal(x, y, z);
            return this;
        }

        // New abstract method in 1.21.11. Not annotated @Override: this class body is inside a
        // Stonecutter arm, which cannot nest a gate, and below 1.21.11 the interface has no such
        // method — see ACClientCompat#setLineWidth.
        public com.mojang.blaze3d.vertex.VertexConsumer setLineWidth(float width) {
            com.github.alexmodguy.alexscaves.client.ACClientCompat.setLineWidth(this.delegate, width);
            return this;
        }

        // setColor(int) stopped being a default method in 1.21.11. Routed back through this class's
        // own four-channel override, which is what the deleted default did — and the only spelling
        // that keeps the tint multiplying a packed call. See ACClientCompat#setColorPacked.
        public com.mojang.blaze3d.vertex.VertexConsumer setColor(int argb) {
            return com.github.alexmodguy.alexscaves.client.ACClientCompat.setColorPacked(this, argb);
        }
    }
    *///?}
}
