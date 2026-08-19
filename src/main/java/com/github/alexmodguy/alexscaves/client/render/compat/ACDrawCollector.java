package com.github.alexmodguy.alexscaves.client.render.compat;

// The exact inverse of ACSubmitBuffers: a SubmitNodeCollector that draws every node it is handed
// straight into a MultiBufferSource, instead of deferring it to the frame's feature-render pass.
//
// It exists because 1.21.9 deleted the last immediate-mode item entry points — ItemRenderer#renderStatic
// and ItemStackRenderState#render(PoseStack, Function<RenderType, VertexConsumer>, int, int) — and the
// only replacement is ItemStackRenderState#submit(…, SubmitNodeCollector, …). Three of this mod's
// idioms need an item drawn *now*, into a buffer the caller chose:
//
//   * ACItemRenderCompat#renderTinted — every layer multiplied by an (r, g, b, a) the caller supplies;
//   * ACItemRenderCompat#renderSepia — the two standard item sheets swapped for the cave book's sepia
//     render type, everything else left alone;
//   * ACClientCompat#renderItemStatic — the plain "draw this stack here" the mod uses in five places.
//
// The sibling AlexsMobsContinued tree solved only the third of those, and did it by requiring the
// MultiBufferSource it is handed to already BE an AMSubmitBuffers, so it could pull the frame's real
// collector back out. That does not work here: the cave book draws into
// the game's own global buffer source directly, and a tint or a render-type swap has to sit
// between the item pipeline and the buffer, which a real collector gives no way to do.
//
// Only the four submit kinds an item can produce are implemented — submitItem (the plain baked-quad
// layers), submitCustomGeometry (this mod's own special renderers, via ACSubmitBuffers), and
// submitModel / submitModelPart (what every vanilla special renderer emits: shields, chests, banners,
// heads, tridents). The rest are entity-frame furniture — hitboxes, shadows, name tags, flames,
// leashes, particles, block models — that an item render never reaches, so they are no-ops rather than
// throws: a vanilla addition that starts using one should degrade to a missing detail, not a crash in
// the middle of a GUI.
//
// Two fidelity notes, both accepted:
//   * outlineColor is ignored throughout. These call sites are a book page, a crucible's contents and
//     two thrown-item renderers; none of them is ever the spectated entity.
//   * the crumbling overlay is ignored, for the same reason — an item is never block-breaking geometry.
//
// 26.2 reshaped seven of OrderedSubmitNodeCollector's members at once — submitNameTag lost its
// distance, submitMovingBlock gained a light, submitBreakingBlockModel takes the parts rather than
// the model, submitParticleGroup became submitQuadParticleGroup, submitShapeOutline and
// submitGizmoPrimitives are new, and submitModelPart stopped being abstract at all (every overload
// is a default now, funnelling into submitModel, so this class simply inherits them). That is too
// many independent spans for replacement rules — several of them overlap one another and one
// changes a body, not just a signature — so the file grows a third whole-class arm, which is the
// shape it was already written in.
//? if >=26.2 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.function.Function;

public final class ACDrawCollector implements SubmitNodeCollector {

    private final Function<RenderType, VertexConsumer> lookup;

    public ACDrawCollector(Function<RenderType, VertexConsumer> lookup) {
        this.lookup = lookup;
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        return this;
    }

    // 26.2 turned ItemFeatureRenderer into a RenderTypeFeatureRenderer and took its two public
    // static foil helpers private with it — getFoilBuffer is an instance method of the frame's own
    // renderer now and getFoilRenderType is gone. The choice it made is four lines, so it is
    // reproduced here rather than reached for: glintTranslucent when the frame is drawing item
    // entities with shader transparency on, otherwise the sheeted or the entity glint. The one
    // thing that moved with it is where "shader transparency" is read — Minecraft's static
    // useShaderTransparency() became GameRenderState#useShaderTransparency().
    // Public and static so ACClientCompat's own >=26.2 arms can reuse it — armorFoilBuffer and the
    // cave book's item renderer make the same choice, and this is the one file in the tree that
    // already imports the whole rendertype package and so can spell it without qualifying anything.
    public static VertexConsumer foilBuffer(Function<RenderType, VertexConsumer> lookup, RenderType renderType, boolean sheeted) {
        RenderType glint;
        if (Minecraft.getInstance().gameRenderer.gameRenderState().useShaderTransparency()
                && renderType.outputTarget() == OutputTarget.ITEM_ENTITY_TARGET) {
            glint = RenderTypes.glintTranslucent();
        } else {
            glint = sheeted ? RenderTypes.glint() : RenderTypes.entityGlint();
        }
        return com.mojang.blaze3d.vertex.VertexMultiConsumer.create(lookup.apply(glint), lookup.apply(renderType));
    }

    private VertexConsumer ac_foilBuffer(RenderType renderType, boolean sheeted) {
        return foilBuffer(this.lookup, renderType, sheeted);
    }

    @Override
    public void submitItem(PoseStack poseStack, net.minecraft.world.item.ItemDisplayContext displayContext,
                           int light, int overlay, int outlineColor, int[] tintLayers,
                           java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> quads,
                           net.minecraft.client.renderer.item.ItemStackRenderState.FoilType foilType) {
        PoseStack.Pose pose = poseStack.last();
        com.mojang.blaze3d.vertex.QuadInstance quadInstance = new com.mojang.blaze3d.vertex.QuadInstance();
        quadInstance.setLightCoords(light);
        quadInstance.setOverlayCoords(overlay);
        for (net.minecraft.client.renderer.block.model.BakedQuad quad : quads) {
            net.minecraft.client.renderer.block.model.BakedQuad.MaterialInfo info = quad.materialInfo();
            RenderType quadType = info.itemRenderType();
            int tintIndex = info.tintIndex();
            quadInstance.setColor(info.isTinted() && tintIndex >= 0 && tintIndex < tintLayers.length ? tintLayers[tintIndex] : -1);
            if (foilType != net.minecraft.client.renderer.item.ItemStackRenderState.FoilType.NONE) {
                this.ac_foilBuffer(quadType, false).putBakedQuad(pose, quad, quadInstance);
            }
            this.lookup.apply(quadType).putBakedQuad(pose, quad, quadInstance);
        }
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
        renderer.render(poseStack.last(), this.lookup.apply(renderType));
    }

    // Every submitModelPart overload is a default in 26.2 that wraps the part in a Model.Simple and
    // comes back through here, so the part path this class used to implement by hand is inherited.
    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
                                int light, int overlay, int tintedColor, TextureAtlasSprite sprite,
                                int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        VertexConsumer consumer = this.lookup.apply(renderType);
        if (sprite != null) {
            consumer = sprite.wrap(consumer);
        }
        model.setupAnim(state);
        model.renderToBuffer(poseStack, consumer, light, overlay, tintedColor);
    }

    // ---- entity-frame furniture an item render never reaches ----

    @Override
    public void submitShadow(PoseStack poseStack, float radius,
                             java.util.List<net.minecraft.client.renderer.entity.state.EntityRenderState.ShadowPiece> pieces) {
    }

    @Override
    public void submitNameTag(PoseStack poseStack, net.minecraft.world.phys.Vec3 offset, int light,
                              net.minecraft.network.chat.Component text, boolean discrete, int backgroundColor,
                              net.minecraft.client.renderer.state.CameraRenderState camera) {
    }

    @Override
    public void submitText(PoseStack poseStack, float x, float y, net.minecraft.util.FormattedCharSequence text,
                           boolean dropShadow, net.minecraft.client.gui.Font.DisplayMode displayMode,
                           int light, int color, int backgroundColor, int outlineColor) {
    }

    @Override
    public void submitFlame(PoseStack poseStack, net.minecraft.client.renderer.entity.state.EntityRenderState state,
                            org.joml.Quaternionf rotation) {
    }

    @Override
    public void submitLeash(PoseStack poseStack, net.minecraft.client.renderer.entity.state.EntityRenderState.LeashState leash) {
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, net.minecraft.client.renderer.block.MovingBlockRenderState state, int light) {
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType,
                                 java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> parts,
                                 int[] tintLayers, int light, int overlay, int outlineColor) {
    }

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack,
                                         java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> parts,
                                         int light) {
    }

    @Override
    public void submitShapeOutline(PoseStack poseStack, net.minecraft.world.phys.shapes.VoxelShape shape,
                                   RenderType renderType, int color, float lineWidth, boolean depthTest) {
    }

    @Override
    public void submitQuadParticleGroup(net.minecraft.client.renderer.state.level.QuadParticleRenderState particles) {
    }

    @Override
    public void submitGizmoPrimitives(net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives.Group group,
                                      net.minecraft.client.renderer.state.CameraRenderState camera, boolean opaque) {
    }
}
*///?} elif >=26 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.function.Function;

public final class ACDrawCollector implements SubmitNodeCollector {

    // Sits between a node's RenderType and the buffer it lands in, so renderSepia can swap the two
    // item sheets for its own type and renderTinted can wrap the consumer. Identity by default.
    private final Function<RenderType, VertexConsumer> lookup;

    // Deliberately the ONLY constructor: MultiBufferSource is itself a RenderType -> VertexConsumer
    // functional interface, so a second overload taking one would make `new ACDrawCollector(type -> …)`
    // ambiguous. Callers with a buffer source pass `source::getBuffer`.
    public ACDrawCollector(Function<RenderType, VertexConsumer> lookup) {
        this.lookup = lookup;
    }

    // A MultiBufferSource view of this collector's lookup, for the vanilla helpers that take one
    // (ItemRenderer#renderItem, ItemRenderer#getFoilBuffer) rather than a VertexConsumer.
    private MultiBufferSource source() {
        return this.lookup::apply;
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        // Ordering only matters to a deferred pass that later sorts its nodes; drawing immediately
        // already preserves submission order.
        return this;
    }

    // 26.1 took the RenderType off this signature: a quad carries its own on its MaterialInfo, so
    // one submitted item can now span several types. It also deleted ItemFeatureRenderer's public
    // static draw — the surviving one is private and reads a SubmitNodeStorage$ItemSubmit — so the
    // quad walk is inlined here. It mirrors that private method exactly, minus the outline pass
    // (see the fidelity note above) and minus the SPECIAL foil decal pose, which needs the private
    // computeFoilDecalPose; the public 4-arg getFoilBuffer is what the model-part path already uses.
    @Override
    public void submitItem(PoseStack poseStack, net.minecraft.world.item.ItemDisplayContext displayContext,
                           int light, int overlay, int outlineColor, int[] tintLayers,
                           java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> quads,
                           net.minecraft.client.renderer.item.ItemStackRenderState.FoilType foilType) {
        PoseStack.Pose pose = poseStack.last();
        com.mojang.blaze3d.vertex.QuadInstance quadInstance = new com.mojang.blaze3d.vertex.QuadInstance();
        quadInstance.setLightCoords(light);
        quadInstance.setOverlayCoords(overlay);
        for (net.minecraft.client.renderer.block.model.BakedQuad quad : quads) {
            net.minecraft.client.renderer.block.model.BakedQuad.MaterialInfo info = quad.materialInfo();
            RenderType quadType = info.itemRenderType();
            int tintIndex = info.tintIndex();
            quadInstance.setColor(info.isTinted() && tintIndex >= 0 && tintIndex < tintLayers.length ? tintLayers[tintIndex] : -1);
            if (foilType != net.minecraft.client.renderer.item.ItemStackRenderState.FoilType.NONE) {
                net.minecraft.client.renderer.feature.ItemFeatureRenderer
                        .getFoilBuffer(this.source(), quadType, false, true)
                        .putBakedQuad(pose, quad, quadInstance);
            }
            this.lookup.apply(quadType).putBakedQuad(pose, quad, quadInstance);
        }
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
        renderer.render(poseStack.last(), this.lookup.apply(renderType));
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
                                int light, int overlay, int tintedColor, TextureAtlasSprite sprite,
                                int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        VertexConsumer consumer = this.lookup.apply(renderType);
        if (sprite != null) {
            consumer = sprite.wrap(consumer);
        }
        model.setupAnim(state);
        model.renderToBuffer(poseStack, consumer, light, overlay, tintedColor);
    }

    @Override
    public void submitModelPart(ModelPart part, PoseStack poseStack, RenderType renderType, int light, int overlay,
                                TextureAtlasSprite sprite, boolean sheeted, boolean hasFoil, int tintedColor,
                                ModelFeatureRenderer.CrumblingOverlay crumbling, int outlineColor) {
        VertexConsumer consumer = hasFoil
                ? net.minecraft.client.renderer.feature.ItemFeatureRenderer.getFoilBuffer(this.source(), renderType, sheeted, true)
                : this.lookup.apply(renderType);
        if (sprite != null) {
            consumer = sprite.wrap(consumer);
        }
        part.render(poseStack, consumer, light, overlay, tintedColor);
    }

    // ---- entity-frame furniture an item render never reaches ----

    // 1.21.11 dropped submitHitbox from the interface (hitboxes are drawn by a debug renderer of
    // their own now) and deleted HitboxesRenderState with it. This whole class already lives inside
    // one `//? if >=1.21.9 {` arm and Stonecutter does not nest gates, so the removal is a
    // `!mc2111-drawcollector-hitbox` replacement rule instead — which is why the annotation and the
    // signature share a line: a rule matches a span of text, and that span has to include the
    // now-wrong @Override.
    @Override public void submitHitbox(PoseStack poseStack, net.minecraft.client.renderer.entity.state.EntityRenderState state, net.minecraft.client.renderer.entity.state.HitboxesRenderState hitboxes) {
    }

    @Override
    public void submitShadow(PoseStack poseStack, float radius,
                             java.util.List<net.minecraft.client.renderer.entity.state.EntityRenderState.ShadowPiece> pieces) {
    }

    @Override
    public void submitNameTag(PoseStack poseStack, net.minecraft.world.phys.Vec3 offset, int light,
                              net.minecraft.network.chat.Component text, boolean discrete, int backgroundColor,
                              double distance, net.minecraft.client.renderer.state.CameraRenderState camera) {
    }

    @Override
    public void submitText(PoseStack poseStack, float x, float y, net.minecraft.util.FormattedCharSequence text,
                           boolean dropShadow, net.minecraft.client.gui.Font.DisplayMode displayMode,
                           int light, int color, int backgroundColor, int outlineColor) {
    }

    @Override
    public void submitFlame(PoseStack poseStack, net.minecraft.client.renderer.entity.state.EntityRenderState state,
                            org.joml.Quaternionf rotation) {
    }

    @Override
    public void submitLeash(PoseStack poseStack, net.minecraft.client.renderer.entity.state.EntityRenderState.LeashState leash) {
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, net.minecraft.client.renderer.block.MovingBlockRenderState state) {
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType,
                                 java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> parts,
                                 int[] tintLayers, int light, int overlay, int outlineColor) {
    }

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack,
                                         net.minecraft.client.renderer.block.model.BlockStateModel model,
                                         long seed, int light) {
    }

    @Override
    public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer renderer) {
    }
}
*///?} elif >=1.21.9 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.function.Function;

public final class ACDrawCollector implements SubmitNodeCollector {

    // Sits between a node's RenderType and the buffer it lands in, so renderSepia can swap the two
    // item sheets for its own type and renderTinted can wrap the consumer. Identity by default.
    private final Function<RenderType, VertexConsumer> lookup;

    // Deliberately the ONLY constructor: MultiBufferSource is itself a RenderType -> VertexConsumer
    // functional interface, so a second overload taking one would make `new ACDrawCollector(type -> …)`
    // ambiguous. Callers with a buffer source pass `source::getBuffer`.
    public ACDrawCollector(Function<RenderType, VertexConsumer> lookup) {
        this.lookup = lookup;
    }

    // A MultiBufferSource view of this collector's lookup, for the vanilla helpers that take one
    // (ItemRenderer#renderItem, ItemRenderer#getFoilBuffer) rather than a VertexConsumer.
    private MultiBufferSource source() {
        return this.lookup::apply;
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        // Ordering only matters to a deferred pass that later sorts its nodes; drawing immediately
        // already preserves submission order.
        return this;
    }

    @Override
    public void submitItem(PoseStack poseStack, net.minecraft.world.item.ItemDisplayContext displayContext,
                           int light, int overlay, int outlineColor, int[] tintLayers,
                           java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> quads,
                           RenderType renderType,
                           net.minecraft.client.renderer.item.ItemStackRenderState.FoilType foilType) {
        ItemRenderer.renderItem(displayContext, poseStack, this.source(), light, overlay, tintLayers, quads, renderType, foilType);
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
        renderer.render(poseStack.last(), this.lookup.apply(renderType));
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
                                int light, int overlay, int tintedColor, TextureAtlasSprite sprite,
                                int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        VertexConsumer consumer = this.lookup.apply(renderType);
        if (sprite != null) {
            consumer = sprite.wrap(consumer);
        }
        model.setupAnim(state);
        model.renderToBuffer(poseStack, consumer, light, overlay, tintedColor);
    }

    @Override
    public void submitModelPart(ModelPart part, PoseStack poseStack, RenderType renderType, int light, int overlay,
                                TextureAtlasSprite sprite, boolean sheeted, boolean hasFoil, int tintedColor,
                                ModelFeatureRenderer.CrumblingOverlay crumbling, int outlineColor) {
        VertexConsumer consumer = hasFoil
                ? ItemRenderer.getFoilBuffer(this.source(), renderType, sheeted, true)
                : this.lookup.apply(renderType);
        if (sprite != null) {
            consumer = sprite.wrap(consumer);
        }
        part.render(poseStack, consumer, light, overlay, tintedColor);
    }

    // ---- entity-frame furniture an item render never reaches ----

    // 1.21.11 dropped submitHitbox from the interface (hitboxes are drawn by a debug renderer of
    // their own now) and deleted HitboxesRenderState with it. This whole class already lives inside
    // one `//? if >=1.21.9 {` arm and Stonecutter does not nest gates, so the removal is a
    // `!mc2111-drawcollector-hitbox` replacement rule instead — which is why the annotation and the
    // signature share a line: a rule matches a span of text, and that span has to include the
    // now-wrong @Override.
    @Override public void submitHitbox(PoseStack poseStack, net.minecraft.client.renderer.entity.state.EntityRenderState state, net.minecraft.client.renderer.entity.state.HitboxesRenderState hitboxes) {
    }

    @Override
    public void submitShadow(PoseStack poseStack, float radius,
                             java.util.List<net.minecraft.client.renderer.entity.state.EntityRenderState.ShadowPiece> pieces) {
    }

    @Override
    public void submitNameTag(PoseStack poseStack, net.minecraft.world.phys.Vec3 offset, int light,
                              net.minecraft.network.chat.Component text, boolean discrete, int backgroundColor,
                              double distance, net.minecraft.client.renderer.state.CameraRenderState camera) {
    }

    @Override
    public void submitText(PoseStack poseStack, float x, float y, net.minecraft.util.FormattedCharSequence text,
                           boolean dropShadow, net.minecraft.client.gui.Font.DisplayMode displayMode,
                           int light, int color, int backgroundColor, int outlineColor) {
    }

    @Override
    public void submitFlame(PoseStack poseStack, net.minecraft.client.renderer.entity.state.EntityRenderState state,
                            org.joml.Quaternionf rotation) {
    }

    @Override
    public void submitLeash(PoseStack poseStack, net.minecraft.client.renderer.entity.state.EntityRenderState.LeashState leash) {
    }

    @Override
    public void submitBlock(PoseStack poseStack, net.minecraft.world.level.block.state.BlockState state,
                            int light, int overlay, int outlineColor) {
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, net.minecraft.client.renderer.block.MovingBlockRenderState state) {
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType,
                                 net.minecraft.client.renderer.block.model.BlockStateModel model,
                                 float red, float green, float blue, int light, int overlay, int outlineColor) {
    }

    @Override
    public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer renderer) {
    }
}
*///?}
