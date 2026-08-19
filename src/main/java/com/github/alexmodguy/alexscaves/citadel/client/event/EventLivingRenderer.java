package com.github.alexmodguy.alexscaves.citadel.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
//? if !forge || <1.21.6
import net.minecraftforge.eventbus.api.Event;

// The model type is spelled FULLY QUALIFIED and must never be imported: the
// `!mc2102-render-import-model` replacement rewrites that import to this mod's compat EntityModel
// on every >=1.21.2 node, and the model carried here comes off a *vanilla* renderer's shadowed
// field, so it is a vanilla EntityModel.
// Only the four concrete subclasses below are ever posted, and nothing listens to this base type,
// so on EventBus 7 it is a plain MutableEvent rather than an InheritableEvent — each posted
// subclass owns its own bus. See CitadelEvent for the wider note.
//? if forge && >=1.21.6
/*public class EventLivingRenderer extends net.minecraftforge.eventbus.api.event.MutableEvent {*/
//? if !forge || <1.21.6
public class EventLivingRenderer extends Event {

    private final LivingEntity entity;
    private final net.minecraft.client.model.EntityModel model;
    private final PoseStack poseStack;
    private final float partialTicks;

    public EventLivingRenderer(LivingEntity entity, net.minecraft.client.model.EntityModel model, PoseStack poseStack, float partialTicks) {
        this.entity = entity;
        this.model = model;
        this.poseStack = poseStack;
        this.partialTicks = partialTicks;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public net.minecraft.client.model.EntityModel getModel() {
        return model;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public static class SetupRotations extends EventLivingRenderer {
        private final float bodyYRot;

        public SetupRotations(LivingEntity entity, net.minecraft.client.model.EntityModel model, PoseStack poseStack, float bodyYRot, float partialTicks) {
            super(entity, model, poseStack, partialTicks);
            this.bodyYRot = bodyYRot;
        }

        public float getBodyYRot() {
            return bodyYRot;
        }

        //? if forge && >=1.21.6
        /*public static final net.minecraftforge.eventbus.api.bus.EventBus<SetupRotations> BUS = net.minecraftforge.eventbus.api.bus.EventBus.create(SetupRotations.class);*/

        /** @see EventPosePlayerHand#post */
        public static void post(SetupRotations event) {
            //? if forge && >=1.21.6
            /*BUS.post(event);*/
            //? if !forge || <1.21.6
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        }
    }

    public static class AccessToBufferSource extends EventLivingRenderer {
        private final float bodyYRot;
        private final MultiBufferSource bufferSource;
        private final int packedLight;

        public AccessToBufferSource(LivingEntity entity, net.minecraft.client.model.EntityModel model, PoseStack poseStack, float bodyYRot, float partialTicks, MultiBufferSource bufferSource, int packedLight) {
            super(entity, model, poseStack, partialTicks);
            this.bodyYRot = bodyYRot;
            this.bufferSource = bufferSource;
            this.packedLight = packedLight;
        }

        public float getBodyYRot() {
            return bodyYRot;
        }

        public MultiBufferSource getBufferSource() {
            return bufferSource;
        }

        public int getPackedLight() {
            return packedLight;
        }
    }

    public static class PreSetupAnimations extends AccessToBufferSource {

        public PreSetupAnimations(LivingEntity entity, net.minecraft.client.model.EntityModel model, PoseStack poseStack, float bodyYRot, float partialTicks, MultiBufferSource bufferSource, int packedLight) {
            super(entity, model, poseStack, bodyYRot, partialTicks, bufferSource, packedLight);
        }

        //? if forge && >=1.21.6
        /*public static final net.minecraftforge.eventbus.api.bus.EventBus<PreSetupAnimations> BUS = net.minecraftforge.eventbus.api.bus.EventBus.create(PreSetupAnimations.class);*/

        /** @see EventPosePlayerHand#post */
        public static void post(PreSetupAnimations event) {
            //? if forge && >=1.21.6
            /*BUS.post(event);*/
            //? if !forge || <1.21.6
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        }
    }

    public static class PostSetupAnimations extends AccessToBufferSource {

        public PostSetupAnimations(LivingEntity entity, net.minecraft.client.model.EntityModel model, PoseStack poseStack, float bodyYRot, float partialTicks, MultiBufferSource bufferSource, int packedLight) {
            super(entity, model, poseStack, bodyYRot, partialTicks, bufferSource, packedLight);
        }

        //? if forge && >=1.21.6
        /*public static final net.minecraftforge.eventbus.api.bus.EventBus<PostSetupAnimations> BUS = net.minecraftforge.eventbus.api.bus.EventBus.create(PostSetupAnimations.class);*/

        /** @see EventPosePlayerHand#post */
        public static void post(PostSetupAnimations event) {
            //? if forge && >=1.21.6
            /*BUS.post(event);*/
            //? if !forge || <1.21.6
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        }
    }

    public static class PostRenderModel extends AccessToBufferSource {

        public PostRenderModel(LivingEntity entity, net.minecraft.client.model.EntityModel model, PoseStack poseStack, float bodyYRot, float partialTicks, MultiBufferSource bufferSource, int packedLight) {
            super(entity, model, poseStack, bodyYRot, partialTicks, bufferSource, packedLight);
        }

        //? if forge && >=1.21.6
        /*public static final net.minecraftforge.eventbus.api.bus.EventBus<PostRenderModel> BUS = net.minecraftforge.eventbus.api.bus.EventBus.create(PostRenderModel.class);*/

        /** @see EventPosePlayerHand#post */
        public static void post(PostRenderModel event) {
            //? if forge && >=1.21.6
            /*BUS.post(event);*/
            //? if !forge || <1.21.6
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        }
    }
}
