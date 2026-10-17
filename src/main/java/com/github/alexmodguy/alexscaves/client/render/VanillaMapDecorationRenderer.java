package com.github.alexmodguy.alexscaves.client.render;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.event.ClientEvents;
import com.github.alexmodguy.alexscaves.server.misc.ACVanillaMapUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

/**
 * Draws the underground-cabin marker onto a map held in hand or hung in a frame.
 *
 * <p>Vanilla's map renderer only knows how to index its own icon sheet, so up to 1.20.4 the mod's
 * marker — an enum constant appended to {@code MapDecoration.Type} by {@code MapDecorationTypeMixin}
 * — had to be drawn by hand: {@code MapDecorationMixin} intercepts the vanilla per-decoration render
 * and hops through {@code AlexsCaves.PROXY} to the quad below.
 *
 * <p>1.20.5 makes all of that unnecessary. A {@code MapDecorationType} is a registry object carrying
 * its own sprite id, and vanilla looks that sprite up on the {@code map_decorations} atlas, so the
 * marker draws itself. Both mixins and this whole class are therefore dropped from the build from
 * 1.20.5 on (source-set exclude in {@code ModPlatformPlugin.configureJava}, config entries pruned in
 * {@code configureProcessResources}) rather than gated: their bodies already carry {@code //?}
 * branches from the 1.20.2 wave, and Stonecutter blocks cannot nest.
 *
 * <p>Split out of {@code ClientEvents} for exactly that reason — a file can be excluded, a method in
 * the middle of a 1100-line one cannot.
 */
public class VanillaMapDecorationRenderer {

    private static final RenderType UNDERGROUND_CABIN_MAP_ICONS = RenderType.text(ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "textures/misc/underground_cabin_map_icons.png"));

    public static void render(MapDecoration mapdecoration, int k) {
        // 1.20.2 turned MapDecoration into a record, so its five accessors lost their get prefixes.
        //? if >=1.20.2 {
        /*MapDecoration.Type decoType = mapdecoration.type();
        byte decoX = mapdecoration.x();
        byte decoY = mapdecoration.y();
        byte decoRot = mapdecoration.rot();
        Component decoName = mapdecoration.name();
        *///?}
        //? if <1.20.2 {
        MapDecoration.Type decoType = mapdecoration.getType();
        byte decoX = mapdecoration.getX();
        byte decoY = mapdecoration.getY();
        byte decoRot = mapdecoration.getRot();
        Component decoName = mapdecoration.getName();
        //?}
        if(decoType == ACVanillaMapUtil.UNDERGROUND_CABIN_MAP_DECORATION){
            MultiBufferSource multiBufferSource = ClientEvents.lastVanillaMapRenderBuffer == null ? Minecraft.getInstance().renderBuffers().bufferSource() : ClientEvents.lastVanillaMapRenderBuffer;
            PoseStack poseStack = ClientEvents.lastVanillaMapPoseStack == null ? new PoseStack() : ClientEvents.lastVanillaMapPoseStack;
            poseStack.pushPose();
            poseStack.translate(0.0F + (float)decoX / 2.0F + 64.0F, 0.0F + (float)decoY / 2.0F + 64.0F, -0.02F);
            poseStack.mulPose(Axis.ZP.rotationDegrees((float)(decoRot * 360) / 16.0F));
            poseStack.scale(4.0F, 4.0F, 3.0F);
            poseStack.translate(-0.125F, 0.125F, 0.0F);
            byte b0 = ACVanillaMapUtil.getMapIconRenderOrdinal(decoType);
            float f1 = (float)(b0 % 16 + 0) / 16.0F;
            float f2 = (float)(b0 / 16 + 0) / 16.0F;
            float f3 = (float)(b0 % 16 + 1) / 16.0F;
            float f4 = (float)(b0 / 16 + 1) / 16.0F;
            Matrix4f matrix4f1 = poseStack.last().pose();
            float f5 = -0.001F;
            VertexConsumer vertexconsumer1 = multiBufferSource.getBuffer(UNDERGROUND_CABIN_MAP_ICONS);
            vertexconsumer1.vertex(matrix4f1, -1.0F, 1.0F, (float)k * -0.001F).color(255, 255, 255, 255).uv(f1, f2).uv2(ClientEvents.lastVanillaMapRenderPackedLight).endVertex();
            vertexconsumer1.vertex(matrix4f1, 1.0F, 1.0F, (float)k * -0.001F).color(255, 255, 255, 255).uv(f3, f2).uv2(ClientEvents.lastVanillaMapRenderPackedLight).endVertex();
            vertexconsumer1.vertex(matrix4f1, 1.0F, -1.0F, (float)k * -0.001F).color(255, 255, 255, 255).uv(f3, f4).uv2(ClientEvents.lastVanillaMapRenderPackedLight).endVertex();
            vertexconsumer1.vertex(matrix4f1, -1.0F, -1.0F, (float)k * -0.001F).color(255, 255, 255, 255).uv(f1, f4).uv2(ClientEvents.lastVanillaMapRenderPackedLight).endVertex();
            poseStack.popPose();
            if (decoName != null) {
                Font font = Minecraft.getInstance().font;
                Component component = decoName;
                float f6 = (float)font.width(component);
                float f7 = Mth.clamp(25.0F / f6, 0.0F, 6.0F / 9.0F);
                poseStack.pushPose();
                poseStack.translate(0.0F + (float)decoX / 2.0F + 64.0F - f6 * f7 / 2.0F, 0.0F + (float)decoY / 2.0F + 64.0F + 4.0F, -0.025F);
                poseStack.scale(f7, f7, 1.0F);
                poseStack.translate(0.0F, 0.0F, -0.1F);
                font.drawInBatch(component, 0.0F, 0.0F, -1, false, poseStack.last().pose(), multiBufferSource, Font.DisplayMode.NORMAL, Integer.MIN_VALUE, ClientEvents.lastVanillaMapRenderPackedLight);
                poseStack.popPose();
            }
        }
    }
}
