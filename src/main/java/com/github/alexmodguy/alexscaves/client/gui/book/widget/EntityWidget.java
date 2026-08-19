package com.github.alexmodguy.alexscaves.client.gui.book.widget;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.github.alexmodguy.alexscaves.client.render.ACRenderTypes;
import com.github.alexmodguy.alexscaves.client.render.entity.CustomBookEntityRenderer;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.math.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public class EntityWidget extends BookWidget {

    @Expose
    @SerializedName("entity_id")
    private String entityId;
    @Expose
    private String nbt;
    @Expose
    private boolean sepia;
    @Expose
    @SerializedName("rot_x")
    private float rotX;
    @Expose
    @SerializedName("rot_y")
    private float rotY;
    @Expose
    @SerializedName("rot_z")
    private float rotZ;

    @Expose(serialize = false, deserialize = false)
    private Entity actualRenderEntity = null;

    public EntityWidget(int displayPage, String entityId, boolean sepia, String entityNBT, int x, int y, float scale) {
        this(displayPage, Type.ENTITY, entityId, sepia, entityNBT, x, y, scale);
    }

    public EntityWidget(int displayPage, Type type, String entityId, boolean sepia, String entityNBT, int x, int y, float scale) {
        super(displayPage, type, x, y, scale);
        this.entityId = entityId;
        this.sepia = sepia;
        this.nbt = entityNBT;
    }


    public void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float partialTicks, boolean onFlippingPage) {
        if (actualRenderEntity == null) {
            EntityType type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(entityId));
            if (type != null) {
                actualRenderEntity = ACClientCompat.displayEntity(type, Minecraft.getInstance().level);
                if (actualRenderEntity instanceof LivingEntity living && nbt != null && !nbt.isEmpty()) {
                    try {
                        // Through the invoker, not the entity: 1.21.6 narrowed readAdditionalSaveData
                        // back to protected. See mixin.client.LivingEntityAccessor.
                        ((com.github.alexmodguy.alexscaves.mixin.client.LivingEntityAccessor) living)
                                .ac_readAdditionalSaveData(ACCompat.asInput(ACCompat.parseTag(nbt), Minecraft.getInstance().level.registryAccess()));
                    } catch (CommandSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        float entityScale = 100.0F * getScale();
        float entityBBSize = Math.max(actualRenderEntity.getBbWidth(), actualRenderEntity.getBbHeight());
        if ((double) entityBBSize > 1.0D) {
            entityScale /= entityBBSize * 1.5F;
        }
        poseStack.pushPose();
        poseStack.translate(getX(), getY(), 120);
        poseStack.scale(entityScale, entityScale, entityScale);
        poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));
        ACClientCompat.setupForEntityInInventory();
        renderEntityInSepia(actualRenderEntity, 0, partialTicks, poseStack, bufferSource, 240);
        ACClientCompat.setupFor3DItems();
        poseStack.popPose();
    }


    protected boolean isSepia() {
        return sepia;
    }


    private void renderEntityInSepia(Entity entityIn, float yaw, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn, int packedLight) {
        // Object, not a typed renderer: from 1.21.2 the dispatcher's lookup is parameterised on a
        // render state, so the renderer type cannot be named at a call site.
        Object render = null;
        EntityRenderDispatcher manager = Minecraft.getInstance().getEntityRenderDispatcher();
        try {
            render = manager.getRenderer(entityIn);
            if (entityIn instanceof LivingEntity living) {
                if(render instanceof CustomBookEntityRenderer customBookEntityRenderer){
                    if(sepia){
                        customBookEntityRenderer.setSepiaFlag(true);
                    }
                    matrixStack.mulPose(Axis.YP.rotationDegrees(180));
                    matrixStack.mulPose(Axis.ZP.rotationDegrees(180));
                    ACClientCompat.runAsFancy(() -> {
                        ACClientCompat.dispatcherRender(manager, entityIn, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, matrixStack, bufferIn, packedLight);
                    });
                    if(sepia) {
                        customBookEntityRenderer.setSepiaFlag(false);
                    }
                }else if (render instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer renderer) {
                    net.minecraft.client.model.EntityModel model = (net.minecraft.client.model.EntityModel) renderer.getModel();
                    VertexConsumer ivertexbuilder = bufferIn.getBuffer(ACRenderTypes.getBookWidget(ACClientCompat.rendererTexture(entityIn, partialTicks), sepia));
                    matrixStack.pushPose();
                    boolean shouldSit = entityIn.isPassenger() && (entityIn.getVehicle() != null && ACCompat.shouldRiderSit(entityIn.getVehicle()));
                    ACClientCompat.setupAnim(model, living, living.isBaby(), shouldSit, 0.0F,
                            0.0F, 0.0F, 1.0F, 0.0F, 0.0F, partialTicks);
                    matrixStack.scale(living.getScale(), living.getScale(), living.getScale());
                    ACClientCompat.renderToBuffer(model, matrixStack, ivertexbuilder, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
                    matrixStack.popPose();
                }
            }
            ACClientCompat.bindMainRenderTargetForWrite(true);
        } catch (Throwable throwable3) {
            CrashReport crashreport = CrashReport.forThrowable(throwable3, "Rendering entity in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Entity being rendered");
            entityIn.fillCrashReportCategory(crashreportcategory);
            CrashReportCategory crashreportcategory1 = crashreport.addCategory("Renderer details");
            crashreportcategory1.setDetail("Assigned renderer", render);
            crashreportcategory1.setDetail("Rotation", Float.valueOf(yaw));
            crashreportcategory1.setDetail("Delta", Float.valueOf(partialTicks));
            throw new ReportedException(crashreport);
        }
    }

}
