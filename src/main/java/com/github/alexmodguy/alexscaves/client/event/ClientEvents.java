package com.github.alexmodguy.alexscaves.client.event;

import com.github.alexmodguy.alexscaves.client.ACClientCompat;
import com.github.alexmodguy.alexscaves.client.ACLevelRenderStage;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.ACClientPlatform;
import com.github.alexmodguy.alexscaves.client.ClientProxy;
import com.github.alexmodguy.alexscaves.client.gui.ACAdvancementTabs;
import com.github.alexmodguy.alexscaves.client.render.blockentity.AmbersolBlockRenderer;
import com.github.alexmodguy.alexscaves.client.render.blockentity.HologramProjectorBlockRenderer;
import com.github.alexmodguy.alexscaves.client.render.entity.CorrodentRenderer;
import com.github.alexmodguy.alexscaves.client.render.entity.LicowitchRenderer;
import com.github.alexmodguy.alexscaves.client.render.entity.SubmarineRenderer;
import com.github.alexmodguy.alexscaves.client.render.item.RaygunRenderHelper;
import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.github.alexmodguy.alexscaves.server.entity.item.BeholderEyeEntity;
import com.github.alexmodguy.alexscaves.server.entity.item.NuclearBombEntity;
import com.github.alexmodguy.alexscaves.server.entity.item.SubmarineEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.*;
import com.github.alexmodguy.alexscaves.server.entity.util.*;
import com.github.alexmodguy.alexscaves.server.item.*;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import com.github.alexmodguy.alexscaves.server.level.biome.BiomeSampler;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import com.github.alexmodguy.alexscaves.server.potion.DarknessIncarnateEffect;
import com.github.alexmodguy.alexscaves.server.potion.DeepsightEffect;
import com.github.alexmodguy.alexscaves.citadel.CitadelEvent;
import com.github.alexmodguy.alexscaves.citadel.client.event.EventGetOutlineColor;
import com.github.alexmodguy.alexscaves.citadel.client.event.EventLivingRenderer;
import com.github.alexmodguy.alexscaves.citadel.client.event.EventPosePlayerHand;
import com.github.alexmodguy.alexscaves.citadel.client.event.EventRenderSplashText;
import com.github.alexmodguy.alexscaves.citadel.client.tick.ClientTickRateTracker;
import com.github.alexmodguy.alexscaves.citadel.server.tick.TickRateTracker;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Axis;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientLevel;
//? if <1.21.6 {
import net.minecraft.client.renderer.FogRenderer;
//?}
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
// Both of these went away in 1.20.5 when the HUD became a stack of named LayeredDraw.Layers —
// see the "HUD overlays" section below.
//? if <1.20.5
import net.minecraftforge.client.gui.overlay.ForgeGui;
//? if <1.20.5
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
// Fabric keeps this on every version. The gate is about NeoForge, which folded the tick events
// into per-target ones at 1.20.5; Fabric's TickEvent is this tree's own vendored stub, fired by
// its own bus, so there is nothing there to fold and every listener below takes the else arm.
//? if forge || fabric || <1.20.5
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.UUID;

import static net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
import com.github.alexmodguy.alexscaves.server.misc.ACFluids;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;

public class ClientEvents {

    private static final ResourceLocation POTION_EFFECT_HUD_OVERLAYS = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "textures/misc/potion_effect_hud_overlays.png");
    private static final ResourceLocation BOSS_BAR_HUD_OVERLAYS = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "textures/misc/boss_bar_hud_overlays.png");
    private static final ResourceLocation DINOSAUR_HUD_OVERLAYS = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "textures/misc/dinosaur_hud_overlays.png");
    private static final ResourceLocation ARMOR_HUD_OVERLAYS = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "textures/misc/armor_hud_overlays.png");
    // See ClientProxy: from 1.21.2 a post chain is a bare id, not the path of its file.
    //? if >=1.21.2 {
    /*private static final ResourceLocation SUBMARINE_SHADER = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "submarine_light");
    private static final ResourceLocation WATCHER_SHADER = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "watcher_perspective");
    private static final ResourceLocation SUGAR_RUSH_SHADER = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "sugar_rush");
    *///?} else {
    private static final ResourceLocation SUBMARINE_SHADER = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "shaders/post/submarine_light.json");
    private static final ResourceLocation WATCHER_SHADER = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "shaders/post/watcher_perspective.json");
    private static final ResourceLocation SUGAR_RUSH_SHADER = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "shaders/post/sugar_rush.json");
    //?}
    private static final ResourceLocation TRAIL_TEXTURE = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "textures/particle/teletor_trail.png");

    private static float lastSampledFogNearness = 0.0F;
    private static float lastSampledWaterFogFarness = 0.0F;
    private static Vec3 lastSampledFogColor = Vec3.ZERO;
    private static Vec3 lastSampledWaterFogColor = Vec3.ZERO;

    public static PoseStack lastVanillaMapPoseStack;
    public static MultiBufferSource lastVanillaMapRenderBuffer;
    public static int lastVanillaMapRenderPackedLight;

    @SubscribeEvent
    public void setupEntityRotations(EventLivingRenderer.SetupRotations event) {
        if (event.getEntity() instanceof MagneticEntityAccessor magnetic) {
            float width = event.getEntity().getBbWidth();
            float height = event.getEntity().getBbHeight();
            float progress = magnetic.getAttachmentProgress(event.getPartialTicks());
            float prevProg = 1F - progress;
            float bodyRot = 180.0F - event.getBodyYRot();
            if (magnetic.getMagneticAttachmentFace().getAxis() != Direction.Axis.Y) {
                event.getPoseStack().mulPose(Axis.YN.rotationDegrees(bodyRot));
            }
            rotateForAngle(event.getEntity(), event.getPoseStack(), magnetic.getPrevMagneticAttachmentFace(), prevProg, width, height);
            rotateForAngle(event.getEntity(), event.getPoseStack(), magnetic.getMagneticAttachmentFace(), progress, width, height);
        }
    }

    // ── Cancelling listeners ───────────────────────────────────────────────────
    // See CommonEvents' note of the same name: EventBus 7 (Forge 56, every Forge node from 1.21.6)
    // deleted Event#setCanceled, so a cancelling handler returns its verdict instead. Each decision
    // is stated once in an ac-prefixed helper with two thin gated entry points around it.

    /** A render this mod has claimed for itself is skipped, having already been drawn once. */
    private boolean acPreRenderLiving(RenderLivingEvent.Pre event) {
        boolean cancel = false;
        LivingEntity rendered = ACClientCompat.renderedEntity(event);
        if (rendered instanceof HeadRotationEntityAccessor magnetic) {
            magnetic.setMagnetHeadRotation();
        }

        if (ClientProxy.blockedEntityRenders.contains(rendered.getUUID())) {
            if (!AlexsCaves.PROXY.isFirstPersonPlayer(rendered)) {
                // 1.21.2 swapped the entity out of the event's first slot for the render state it
                // was extracted from; everything else is passed straight through either way — except
                // on Forge, which dropped partialTicks from the constructor as well, the state being
                // where a renderer is meant to read it from.
                // …and 1.21.9 swapped the buffer source for a SubmitNodeCollector and dropped the
                // packed light, the state being where that lives now — so the relayed Post carries
                // the camera state in its place on Forge. NeoForge's 1.21.9 event took the
                // collector but neither the camera state nor Forge's partialTicks removal, so it is
                // its own arm rather than sharing either neighbour's.
                //? if forge && >=1.21.9 {
                /*RenderLivingEvent.Post.BUS.post(new RenderLivingEvent.Post(event.getState(), event.getRenderer(), event.getPoseStack(), event.getNodeCollector(), event.getCameraState()));
                *///?} elif neoforge && >=1.21.9 {
                /*MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post(event.getRenderState(), event.getRenderer(), event.getPartialTick(), event.getPoseStack(), event.getSubmitNodeCollector()));
                *///?} elif forge && >=1.21.6 {
                /*RenderLivingEvent.Post.BUS.post(new RenderLivingEvent.Post(event.getState(), event.getRenderer(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight()));
                *///?} elif forge && >=1.21.2 {
                /*MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post(event.getState(), event.getRenderer(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight()));
                *///?} elif >=1.21.2 {
                /*MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post(event.getRenderState(), event.getRenderer(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight()));
                *///?} else {
                MinecraftForge.EVENT_BUS.post(new RenderLivingEvent.Post(rendered, event.getRenderer(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight()));
                //?}
                cancel = true;
            }
            ClientProxy.blockedEntityRenders.remove(rendered.getUUID());
        }
        return cancel;
    }

    //? if forge && >=1.21.6 {
    /*@SubscribeEvent
    public boolean preRenderLiving(RenderLivingEvent.Pre event) {
        return acPreRenderLiving(event);
    }
    *///?} else {
    @SubscribeEvent
    public void preRenderLiving(RenderLivingEvent.Pre event) {
        if (acPreRenderLiving(event)) {
            event.setCanceled(true);
        }
    }
    //?}

    // 1.21.9 replaced the event's MultiBufferSource with a SubmitNodeCollector and dropped its
    // packed light, which now rides the render state. The body below is unchanged; each entry point
    // only says where those two come from. The >=1.21.9 one records what the body draws and hands
    // the recording to the collector once it returns — see ACSubmitBuffers.
    //? if forge && >=1.21.9 {
    /*@SubscribeEvent
    public void postRenderLiving(RenderLivingEvent.Post event) {
        com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers buffers =
                new com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers(event.getNodeCollector(), event.getCameraState());
        acPostRenderLiving(event, buffers, event.getState().lightCoords);
        buffers.flush();
    }
    *///?} elif neoforge && >=1.21.9 {
    /*@SubscribeEvent
    public void postRenderLiving(RenderLivingEvent.Post event) {
        com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers buffers =
                new com.github.alexmodguy.alexscaves.client.render.compat.ACSubmitBuffers(event.getSubmitNodeCollector());
        acPostRenderLiving(event, buffers, event.getRenderState().lightCoords);
        buffers.flush();
    }
    *///?} else {
    @SubscribeEvent
    public void postRenderLiving(RenderLivingEvent.Post event) {
        acPostRenderLiving(event, event.getMultiBufferSource(), event.getPackedLight());
    }
    //?}

    @SuppressWarnings("rawtypes")
    private void acPostRenderLiving(RenderLivingEvent.Post event, MultiBufferSource bufferSource, int packedLight) {
        LivingEntity entity = ACClientCompat.renderedEntity(event);
        float partialTick = ACClientCompat.renderPartialTick(event);
        if (entity instanceof HeadRotationEntityAccessor magnetic) {
            magnetic.resetMagnetHeadRotation();
        }
        if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            RaygunRenderHelper.renderRaysFor(entity, entity.getPosition(partialTick), event.getPoseStack(), bufferSource, partialTick, false, 0);
        }
        if (entity.hasEffect(ACCompat.effect(ACEffectRegistry.DARKNESS_INCARNATE.get())) && entity.isAlive()) {
            Vec3 trailOffset = new Vec3(0, entity.getBbHeight() * 0.5F, 0);
            double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
            double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
            double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
            int samples = 0;
            int sampleSize = 60;
            float trailHeight = entity.getBbHeight() * 0.8F;
            Vec3 topAngleVec = new Vec3(0, trailHeight, 0);
            Vec3 bottomAngleVec = new Vec3(0, -trailHeight, 0);
            Vec3 drawFrom = trailOffset;
            VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.entityTranslucent(TRAIL_TEXTURE));
            float trailA = DarknessIncarnateEffect.getIntensity(entity, partialTick, 20F);
            int packedLightIn = packedLight;
            while (samples < sampleSize) {
                Vec3 sample = AlexsCaves.PROXY.getDarknessTrailPosFor(entity, samples + 5, partialTick).subtract(x, y, z).add(trailOffset);
                float u1 = samples / (float) sampleSize;
                float u2 = u1 + 1 / (float) sampleSize;

                Vec3 draw1 = drawFrom;
                Vec3 draw2 = sample;

                PoseStack.Pose posestack$pose = event.getPoseStack().last();
                Matrix4f matrix4f = posestack$pose.pose();
                Matrix3f matrix3f = posestack$pose.normal();

                vertexconsumer.vertex(matrix4f, (float) draw1.x + (float) bottomAngleVec.x, (float) draw1.y + (float) bottomAngleVec.y, (float) draw1.z + (float) bottomAngleVec.z).color(0, 0, 0, trailA).uv(u1, 1F).overlayCoords(NO_OVERLAY).uv2(packedLightIn).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
                vertexconsumer.vertex(matrix4f, (float) draw2.x + (float) bottomAngleVec.x, (float) draw2.y + (float) bottomAngleVec.y, (float) draw2.z + (float) bottomAngleVec.z).color(0, 0, 0, trailA).uv(u2, 1F).overlayCoords(NO_OVERLAY).uv2(packedLightIn).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
                vertexconsumer.vertex(matrix4f, (float) draw2.x + (float) topAngleVec.x, (float) draw2.y + (float) topAngleVec.y, (float) draw2.z + (float) topAngleVec.z).color(0, 0, 0, trailA).uv(u2, 0).overlayCoords(NO_OVERLAY).uv2(packedLightIn).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
                vertexconsumer.vertex(matrix4f, (float) draw1.x + (float) topAngleVec.x, (float) draw1.y + (float) topAngleVec.y, (float) draw1.z + (float) topAngleVec.z).color(0, 0, 0, trailA).uv(u1, 0).overlayCoords(NO_OVERLAY).uv2(packedLightIn).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
                samples++;
                drawFrom = sample;
            }
        }
    }

    private static void attemptLoadShader(ResourceLocation resourceLocation) {
        GameRenderer renderer = Minecraft.getInstance().gameRenderer;
        if (ClientProxy.shaderLoadAttemptCooldown <= 0) {
            if (!ACClientCompat.loadPostEffect(renderer, resourceLocation)) {
                ClientProxy.shaderLoadAttemptCooldown = 12000;
                AlexsCaves.LOGGER.warn("Alex's Caves could not load the shader {}, will attempt to load shader in 30 seconds", resourceLocation);
            }
        }
    }

    /**
     * The level-render half of this listener, off the loader's event.
     *
     * <p>Forge deleted {@code RenderLevelStageEvent} in 1.21.2, and 1.21.6 made NeoForge's copy
     * unusable here too (see {@code ACClientCompat}'s import comment), so on those nodes nothing
     * posts an event and {@code mixin.client.LevelRenderStageMixin} calls {@link #renderStage}
     * directly instead. Fabric takes that path on every version — it never had the event, which is
     * why the mixin was written loader-neutral in the first place. See {@link ACLevelRenderStage}.
     */
    //? if !fabric && (!forge || <1.21.2) && <1.21.6 {
    @SubscribeEvent
    public void postRenderStage(RenderLevelStageEvent event) {
        ACLevelRenderStage stage = ACClientCompat.stageOf(event);
        if (stage != null) {
            renderStage(stage, event.getLevelRenderer(), ACClientCompat.poseStack(event), event.getRenderTick(), event.getCamera(), ACClientCompat.partialTick(event));
        }
    }
    //?}

    public static void renderStage(ACLevelRenderStage stage, net.minecraft.client.renderer.LevelRenderer levelRenderer, PoseStack poseStack, int renderTick, net.minecraft.client.Camera camera, float partialTick) {
        Entity player = Minecraft.getInstance().getCameraEntity();
        boolean firstPerson = Minecraft.getInstance().options.getCameraType().isFirstPerson();
        if (stage == ACLevelRenderStage.AFTER_SKY) {
            if (firstPerson && player instanceof LivingEntity living) {
                MultiBufferSource.BufferSource multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
                Vec3 cameraPos = camera.getPosition();
                RaygunRenderHelper.renderRaysFor(living, cameraPos, poseStack, multibuffersource$buffersource, partialTick, true, 2);
            }
            GameRenderer renderer = Minecraft.getInstance().gameRenderer;
            if (firstPerson && player.isPassenger() && player.getVehicle() instanceof SubmarineEntity submarine && SubmarineRenderer.isFirstPersonFloodlightsMode(submarine)) {
                if (!ACClientCompat.isPostEffect(renderer, SUBMARINE_SHADER)) {
                    attemptLoadShader(SUBMARINE_SHADER);
                }
            } else if (ACClientCompat.isPostEffect(renderer, SUBMARINE_SHADER)) {
                renderer.checkEntityPostEffect(null);
            }else if (firstPerson && player instanceof PossessesCamera || player instanceof LivingEntity afflicted && afflicted.hasEffect(ACCompat.effect(ACEffectRegistry.DARKNESS_INCARNATE.get()))) {
                if (!ACClientCompat.isPostEffect(renderer, WATCHER_SHADER)) {
                    attemptLoadShader(WATCHER_SHADER);
                }
            } else if (ACClientCompat.isPostEffect(renderer, WATCHER_SHADER)) {
                renderer.checkEntityPostEffect(null);
            }else if (player instanceof LivingEntity afflicted && afflicted.hasEffect(ACCompat.effect(ACEffectRegistry.SUGAR_RUSH.get())) && AlexsCaves.CLIENT_CONFIG.sugarRushSaturationEffect.get()) {
                if (!ACClientCompat.isPostEffect(renderer, SUGAR_RUSH_SHADER)) {
                    attemptLoadShader(SUGAR_RUSH_SHADER);
                }
            } else if (ACClientCompat.isPostEffect(renderer, SUGAR_RUSH_SHADER)) {
                renderer.checkEntityPostEffect(null);
            }
        }
        if (stage == ACLevelRenderStage.AFTER_ENTITIES) {
            if (firstPerson && player instanceof LivingEntity living) {
                MultiBufferSource.BufferSource multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
                Vec3 cameraPos = camera.getPosition();
                RaygunRenderHelper.renderRaysFor(living, cameraPos, poseStack, multibuffersource$buffersource, partialTick, true, 1);
            }
            ACClientCompat.runAsFancy(() -> HologramProjectorBlockRenderer.renderEntireBatch(levelRenderer, poseStack, renderTick, camera, partialTick));
        }
        if (stage == ACLevelRenderStage.AFTER_CUTOUT_BLOCKS) {
            ACClientCompat.runAsFancy(() -> CorrodentRenderer.renderEntireBatch(levelRenderer, poseStack, renderTick, camera, partialTick));
            ACClientCompat.runAsFancy(() -> LicowitchRenderer.renderEntireBatch(levelRenderer, poseStack, renderTick, camera, partialTick));
        }
        if (stage == ACLevelRenderStage.AFTER_TRANSLUCENT_BLOCKS && AlexsCaves.CLIENT_CONFIG.ambersolShines.get()) {
            ACClientCompat.runAsFancy(() -> AmbersolBlockRenderer.renderEntireBatch(levelRenderer, poseStack, renderTick, camera, partialTick));
        }
    }

    @SubscribeEvent
    public void computeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Entity player = Minecraft.getInstance().getCameraEntity();
        float partialTick = ACClientCompat.partialTick();
        float tremorAmount = ClientProxy.renderNukeSkyDarkFor > 0 ? 1.5F : 0F;
        if (player instanceof PossessesCamera watcherEntity) {
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
            tremorAmount = watcherEntity.isPossessionBreakable() ? AlexsCaves.PROXY.getPossessionStrengthAmount(partialTick) : 0F;
        }
        if (player != null && AlexsCaves.CLIENT_CONFIG.screenShaking.get()) {
            double shakeDistanceScale = 64;
            double distance = Double.MAX_VALUE;
            if (tremorAmount == 0) {
                AABB aabb = player.getBoundingBox().inflate(shakeDistanceScale);
                for (Mob screenShaker : Minecraft.getInstance().level.getEntitiesOfClass(Mob.class, aabb, (mob -> mob instanceof ShakesScreen))) {
                    ShakesScreen shakesScreen = (ShakesScreen) screenShaker;
                    if (shakesScreen.canFeelShake(player) && screenShaker.distanceTo(player) < distance) {
                        distance = screenShaker.distanceTo(player);
                        tremorAmount = Math.min((1F - (float) Math.min(1, distance / shakesScreen.getShakeDistance())) * Math.max(shakesScreen.getScreenShakeAmount(partialTick), 0F), 2.0F);
                    }
                }
            }
            if (tremorAmount > 0) {
                if (ClientProxy.lastTremorTick != player.tickCount) {
                    RandomSource rng = player.level().getRandom();
                    ClientProxy.randomTremorOffsets[0] = rng.nextFloat();
                    ClientProxy.randomTremorOffsets[1] = rng.nextFloat();
                    ClientProxy.randomTremorOffsets[2] = rng.nextFloat();
                    ClientProxy.lastTremorTick = player.tickCount;
                }
                double intensity = tremorAmount * Minecraft.getInstance().options.screenEffectScale().get();
                ACClientCompat.cameraMove(event.getCamera(), ClientProxy.randomTremorOffsets[0] * 0.2F * intensity, ClientProxy.randomTremorOffsets[1] * 0.2F * intensity, ClientProxy.randomTremorOffsets[2] * 0.5F * intensity);
            }
        }
        if (player != null && player.isPassenger() && player.getVehicle() instanceof SubmarineEntity && event.getCamera().isDetached()) {
            ACClientCompat.cameraMove(event.getCamera(), -ACClientCompat.cameraMaxZoom(event.getCamera(), 4F), 0, 0);
        }
        if (player != null && player.isPassenger() && player.getVehicle() instanceof TremorsaurusEntity && event.getCamera().isDetached()) {
            ACClientCompat.cameraMove(event.getCamera(), -ACClientCompat.cameraMaxZoom(event.getCamera(), 2F), 0, 0);
        }
        if (player != null && player.isPassenger() && player.getVehicle() instanceof AtlatitanEntity && event.getCamera().isDetached()) {
            ACClientCompat.cameraMove(event.getCamera(), -ACClientCompat.cameraMaxZoom(event.getCamera(), 4F), 0, 0);
        }
        if (player != null && player.isPassenger() && player.getVehicle() instanceof TremorzillaEntity && event.getCamera().isDetached()) {
            ACClientCompat.cameraMove(event.getCamera(), -ACClientCompat.cameraMaxZoom(event.getCamera(), 10F), 0, 0);
        }
        if (player != null && player.isPassenger() && player.getVehicle() instanceof GumWormSegmentEntity && event.getCamera().isDetached()) {
            ACClientCompat.cameraMove(event.getCamera(), -ACClientCompat.cameraMaxZoom(event.getCamera(), 12F), 0, 0);
        }
        if (player != null && player instanceof LivingEntity livingEntity && livingEntity.hasEffect(ACCompat.effect(ACEffectRegistry.STUNNED.get()))) {
            event.setRoll((float) (Math.sin((player.tickCount + partialTick) * 0.2F) * 10F));
        }
        Direction dir = MagnetUtil.getEntityMagneticDirection(player);

    }

    // 1.20.5 folded LivingEvent.LivingTickEvent into NeoForge's per-target tick events: every
    // entity now ticks through EntityTickEvent, so the listener takes the wider type and narrows
    // it back. Pre is where LivingTickEvent used to fire.
    //? if neoforge && >=1.20.5 {
    /*@SubscribeEvent
    public void clientLivingTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
    *///?} else {
    @SubscribeEvent
    public void clientLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
    //?}
        if (!entity.level().isClientSide()) {
            return;
        }
        if (entity.hasEffect(ACCompat.effect(ACEffectRegistry.DARKNESS_INCARNATE.get())) && entity.isAlive()) {
            int trailPointer = ClientProxy.darknessTrailPointerMap.getOrDefault(entity, -1);
            Vec3 latest = entity.position();
            if (ClientProxy.darknessTrailPosMap.get(entity) == null) {
                Vec3[] trailPositions = new Vec3[64];
                if (trailPointer == -1) {
                    Arrays.fill(trailPositions, latest);
                }
                ClientProxy.darknessTrailPosMap.put(entity, trailPositions);
            }
            if (++trailPointer == ClientProxy.darknessTrailPosMap.get(entity).length) {
                trailPointer = 0;
            }
            ClientProxy.darknessTrailPointerMap.put(entity, trailPointer);
            Vec3[] vector3ds = ClientProxy.darknessTrailPosMap.get(entity);
            vector3ds[trailPointer] = latest;
            ClientProxy.darknessTrailPosMap.put(entity, vector3ds);
        } else if (ClientProxy.darknessTrailPosMap.containsKey(entity)) {
            ClientProxy.darknessTrailPosMap.remove(entity);
            ClientProxy.darknessTrailPointerMap.remove(entity);
        }
    }

    /** Possessing a mob puts you behind its eyes; your own hand does not belong in that view. */
    private boolean acRenderHand() {
        return Minecraft.getInstance().getCameraEntity() instanceof PossessesCamera;
    }

    //? if forge && >=1.21.6 {
    /*@SubscribeEvent
    public boolean onRenderHand(RenderHandEvent event) {
        return acRenderHand();
    }
    *///?} else {
    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        if (acRenderHand()) {
            event.setCanceled(true);
        }
    }
    //?}

    // ── HUD overlays ───────────────────────────────────────────────────────────
    // What the mod suppresses is stated as intent, not as layer ids: 1.20.5 turned the HUD into a
    // stack of named LayeredDraw.Layers, renaming and re-partitioning everything in the process, and
    // the two loaders then named the pieces differently again. Each era's wiring maps its own ids
    // onto these two predicates.

    /** Possessing a mob puts you behind its eyes; the player's own reticle and HUD text do not belong there. */
    public static boolean hidePossessedPlayerOverlay() {
        return Minecraft.getInstance().getCameraEntity() instanceof PossessesCamera;
    }

    /** The riding meter is drawn where the experience bar would be, so one of them has to go. */
    public static boolean hideExperienceBar() {
        Entity player = Minecraft.getInstance().getCameraEntity();
        return player != null && player.getVehicle() instanceof RidingMeterMount mount && mount.hasRidingMeter();
    }

    //? if <1.20.5 {
    @SubscribeEvent
    public void onPreRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        ResourceLocation id = event.getOverlay().id();
        if (hidePossessedPlayerOverlay() && (id.equals(VanillaGuiOverlay.CROSSHAIR.id()) || id.equals(VanillaGuiOverlay.EXPERIENCE_BAR.id()) || id.equals(VanillaGuiOverlay.JUMP_BAR.id()) || id.equals(VanillaGuiOverlay.ITEM_NAME.id()))) {
            event.setCanceled(true);
        }
        if (hideExperienceBar() && id.equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
            event.setCanceled(true);
        }
    }
    //?}

    // NeoForge kept a per-layer Pre/Post pair, only renamed and keyed by the layer's own name, so the
    // 1.20.5+ arm reads almost the same. Forge went the other way — no render event at all, layers are
    // registered and conditioned once on the mod bus — which is handled in AlexsCaves#addGuiOverlayLayers.
    // 1.21.6 merged the experience bar, the jump meter and the new locator bar into one slot — the
    // "contextual info bar", drawn by whichever ContextualBarRenderer the state currently selects —
    // so EXPERIENCE_BAR and JUMP_METER are gone and the two ids that replace them are the bar and its
    // background. EXPERIENCE_LEVEL (the number above it) is its own layer on both eras and is left
    // alone on both, since it is not what the riding meter overlaps.
    //? if neoforge && >=1.21.6 {
    /*@SubscribeEvent
    public void onPreRenderGuiLayer(net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre event) {
        ResourceLocation id = event.getName();
        boolean contextualInfoBar = id.equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CONTEXTUAL_INFO_BAR) || id.equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND);
        if (hidePossessedPlayerOverlay() && (id.equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CROSSHAIR) || contextualInfoBar || id.equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.SELECTED_ITEM_NAME))) {
            event.setCanceled(true);
        }
        if (hideExperienceBar() && contextualInfoBar) {
            event.setCanceled(true);
        }
    }
    *///?} elif neoforge && >=1.20.5 {
    /*@SubscribeEvent
    public void onPreRenderGuiLayer(net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre event) {
        ResourceLocation id = event.getName();
        if (hidePossessedPlayerOverlay() && (id.equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CROSSHAIR) || id.equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.EXPERIENCE_BAR) || id.equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.JUMP_METER) || id.equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.SELECTED_ITEM_NAME))) {
            event.setCanceled(true);
        }
        if (hideExperienceBar() && id.equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.EXPERIENCE_BAR)) {
            event.setCanceled(true);
        }
    }
    *///?}

    //? if neoforge && >=1.20.5 {
    /*@SubscribeEvent
    public void onPostRenderGuiLayer(net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Post event) {
        if (event.getName().equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CROSSHAIR)) {
            renderRidingMeterHud(event.getGuiGraphics());
        }
        if (event.getName().equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.PLAYER_HEALTH)) {
            renderIrradiatedHearts(event.getGuiGraphics());
        }
    }
    *///?}

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onPoseHand(EventPosePlayerHand event) {
        LivingEntity player = (LivingEntity) event.getEntityIn();
        float f = ACClientCompat.frameTime();
        float rightHandResistorShieldUseProgress = 0.0F;
        float leftHandResistorShieldUseProgress = 0.0F;
        float rightHandGalenaGauntletUseProgress = 0.0F;
        float leftHandGalenaGauntletUseProgress = 0.0F;
        float rightHandSpearUseProgress = 0.0F;
        float leftHandSpearUseProgress = 0.0F;
        float rightHandRaygunUseProgress = 0.0F;
        float leftHandRaygunUseProgress = 0.0F;
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof ResistorShieldItem) {
            if (player.getMainArm() == HumanoidArm.RIGHT) {
                rightHandResistorShieldUseProgress = Math.max(rightHandResistorShieldUseProgress, ResistorShieldItem.getLerpedUseTime(player.getItemInHand(InteractionHand.MAIN_HAND), f));
            } else {
                leftHandResistorShieldUseProgress = Math.max(leftHandResistorShieldUseProgress, ResistorShieldItem.getLerpedUseTime(player.getItemInHand(InteractionHand.MAIN_HAND), f));
            }
        }
        if (player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof ResistorShieldItem) {
            if (player.getMainArm() == HumanoidArm.RIGHT) {
                leftHandResistorShieldUseProgress = Math.max(leftHandResistorShieldUseProgress, ResistorShieldItem.getLerpedUseTime(player.getItemInHand(InteractionHand.OFF_HAND), f));
            } else {
                rightHandResistorShieldUseProgress = Math.max(rightHandResistorShieldUseProgress, ResistorShieldItem.getLerpedUseTime(player.getItemInHand(InteractionHand.OFF_HAND), f));
            }
        }
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof GalenaGauntletItem) {
            if (player.getMainArm() == HumanoidArm.RIGHT) {
                rightHandGalenaGauntletUseProgress = Math.max(rightHandGalenaGauntletUseProgress, GalenaGauntletItem.getLerpedUseTime(player.getItemInHand(InteractionHand.MAIN_HAND), f));
            } else {
                leftHandGalenaGauntletUseProgress = Math.max(leftHandGalenaGauntletUseProgress, GalenaGauntletItem.getLerpedUseTime(player.getItemInHand(InteractionHand.MAIN_HAND), f));
            }
        }
        if (player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof GalenaGauntletItem) {
            if (player.getMainArm() == HumanoidArm.RIGHT) {
                leftHandGalenaGauntletUseProgress = Math.max(leftHandGalenaGauntletUseProgress, GalenaGauntletItem.getLerpedUseTime(player.getItemInHand(InteractionHand.OFF_HAND), f));
            } else {
                rightHandGalenaGauntletUseProgress = Math.max(rightHandGalenaGauntletUseProgress, GalenaGauntletItem.getLerpedUseTime(player.getItemInHand(InteractionHand.OFF_HAND), f));
            }
        }
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof SpearItem && player.isUsingItem() && player.getUseItemRemainingTicks() > 0) {
            // 1.21 asks who is holding the item — a use duration may depend on the user now.
            //? if >=1.21
            /*float f7 = (player.getItemInHand(InteractionHand.MAIN_HAND).getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - f + 1.0F)) / 10.0F;*/
            //? if <1.21
            float f7 = (player.getItemInHand(InteractionHand.MAIN_HAND).getUseDuration() - ((float) player.getUseItemRemainingTicks() - f + 1.0F)) / 10.0F;
            if (player.getMainArm() == HumanoidArm.RIGHT) {
                rightHandSpearUseProgress = Math.max(rightHandSpearUseProgress, f7);
            } else {
                leftHandSpearUseProgress = Math.max(leftHandSpearUseProgress, f7);
            }
        }
        if (player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof SpearItem && player.isUsingItem() && player.getUseItemRemainingTicks() > 0) {
            //? if >=1.21
            /*float f7 = (player.getItemInHand(InteractionHand.OFF_HAND).getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - f + 1.0F)) / 10.0F;*/
            //? if <1.21
            float f7 = (player.getItemInHand(InteractionHand.OFF_HAND).getUseDuration() - ((float) player.getUseItemRemainingTicks() - f + 1.0F)) / 10.0F;
            if (player.getMainArm() == HumanoidArm.RIGHT) {
                leftHandSpearUseProgress = Math.max(leftHandSpearUseProgress, f7);
            } else {
                rightHandSpearUseProgress = Math.max(rightHandSpearUseProgress, f7);
            }
        }
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof RaygunItem) {
            if (player.getMainArm() == HumanoidArm.RIGHT) {
                rightHandRaygunUseProgress = Math.max(rightHandRaygunUseProgress, RaygunItem.getLerpedUseTime(player.getItemInHand(InteractionHand.MAIN_HAND), f));
            } else {
                leftHandRaygunUseProgress = Math.max(leftHandRaygunUseProgress, RaygunItem.getLerpedUseTime(player.getItemInHand(InteractionHand.MAIN_HAND), f));
            }
        }
        if (player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof RaygunItem) {
            if (player.getMainArm() == HumanoidArm.RIGHT) {
                leftHandRaygunUseProgress = Math.max(leftHandRaygunUseProgress, RaygunItem.getLerpedUseTime(player.getItemInHand(InteractionHand.OFF_HAND), f));
            } else {
                rightHandRaygunUseProgress = Math.max(rightHandRaygunUseProgress, RaygunItem.getLerpedUseTime(player.getItemInHand(InteractionHand.OFF_HAND), f));
            }
        }
        if (player.isPassenger() && player.getVehicle() instanceof SubterranodonEntity subterranodon) {
            float flight = subterranodon.getFlyProgress(f) - subterranodon.getHoverProgress(f);
            if (flight > 0.0F) {
                event.getModel().leftArm.xRot = -(float) Math.toRadians(180F) * flight;
                event.getModel().leftArm.zRot = (float) Math.toRadians(-10F) * flight;
                event.getModel().rightArm.xRot = -(float) Math.toRadians(180F) * flight;
                event.getModel().rightArm.zRot = (float) Math.toRadians(10F) * flight;
            }
            event.setCitadelResult(CitadelEvent.Result.ALLOW);
        }
        if (leftHandResistorShieldUseProgress > 0.0F) {
            float useProgress = Math.min(10F, leftHandResistorShieldUseProgress) / 10F;
            float useProgressTurn = Math.min(useProgress * 4F, 1F);
            float useProgressUp = (float) Math.sin(useProgress * Math.PI);
            // The model's crouching flag became a render-state field in 1.21.2; the entity this
            // event is posing answers the same question on every version.
            float armTilt = player.isCrouching() ? 120F : 80F;
            event.getModel().leftArm.xRot = -(float) Math.toRadians(armTilt) - (float) Math.toRadians(80F) * useProgressUp;
            event.getModel().leftArm.yRot = (float) Math.toRadians(20F) * useProgressTurn;
            event.setCitadelResult(CitadelEvent.Result.ALLOW);
        }
        if (rightHandResistorShieldUseProgress > 0.0F) {
            float useProgress = Math.min(10F, rightHandResistorShieldUseProgress) / 10F;
            float useProgressTurn = Math.min(useProgress * 4F, 1F);
            float useProgressUp = (float) Math.sin(useProgress * Math.PI);
            float armTilt = player.isCrouching() ? 120F : 80F;
            event.getModel().rightArm.xRot = -(float) Math.toRadians(armTilt) - (float) Math.toRadians(80F) * useProgressUp;
            event.getModel().rightArm.yRot = -(float) Math.toRadians(20F) * useProgressTurn;
            event.setCitadelResult(CitadelEvent.Result.ALLOW);
        }
        if (leftHandGalenaGauntletUseProgress > 0.0F) {
            float useProgress = Math.min(5F, leftHandGalenaGauntletUseProgress) / 5F;
            event.getModel().leftArm.xRot = (event.getModel().head.xRot - (float) Math.toRadians(80F)) * useProgress;
            event.getModel().leftArm.yRot = event.getModel().head.yRot * useProgress;
            event.setCitadelResult(CitadelEvent.Result.ALLOW);
        }
        if (rightHandGalenaGauntletUseProgress > 0.0F) {
            float useProgress = Math.min(5F, rightHandGalenaGauntletUseProgress) / 5F;
            event.getModel().rightArm.xRot = (event.getModel().head.xRot - (float) Math.toRadians(80F)) * useProgress;
            event.getModel().rightArm.yRot = event.getModel().head.yRot * useProgress;
            event.setCitadelResult(CitadelEvent.Result.ALLOW);
        }
        if (leftHandSpearUseProgress > 0.0F) {
            float useProgress = Math.min(1F, leftHandSpearUseProgress);
            float useProgressMiddle = (float) Math.sin(useProgress * Math.PI);
            event.getModel().leftArm.xRot = useProgress * ((float) Math.toRadians(-180F) + event.getModel().head.xRot);
            event.getModel().leftArm.yRot = useProgressMiddle * ((float) Math.toRadians(-25F) - event.getModel().head.yRot);
            event.getModel().leftArm.zRot = useProgress * (float) Math.toRadians(50F) - (float) Math.toRadians(25F);
            event.setCitadelResult(CitadelEvent.Result.ALLOW);
        }
        if (rightHandSpearUseProgress > 0.0F) {
            float useProgress = Math.min(1F, rightHandSpearUseProgress);
            float useProgressMiddle = (float) Math.sin(useProgress * Math.PI);
            event.getModel().rightArm.xRot = useProgress * ((float) Math.toRadians(-180F) + event.getModel().head.xRot);
            event.getModel().rightArm.yRot = useProgressMiddle * ((float) Math.toRadians(25F) - event.getModel().head.yRot);
            event.getModel().rightArm.zRot = useProgress * -(float) Math.toRadians(50F) + (float) Math.toRadians(25F);
            event.setCitadelResult(CitadelEvent.Result.ALLOW);
        }
        if (event.getEntityIn().getVehicle() instanceof NuclearBombEntity) {
            float ageInTicks = event.getEntityIn().tickCount + f;
            event.getModel().rightArm.xRot = (float) Math.toRadians(-170F);
            event.getModel().rightArm.yRot = (float) Math.toRadians(100F) + (float) Math.cos(ageInTicks * 0.35F) * (float) Math.toRadians(20F);
            event.getModel().rightArm.zRot = (float) Math.sin(ageInTicks * 0.35F) * (float) Math.toRadians(50F) - (float) Math.toRadians(70F);
            event.getModel().leftArm.yRot = (float) Math.toRadians(30F);
            event.setCitadelResult(CitadelEvent.Result.ALLOW);
        }
        if (leftHandRaygunUseProgress > 0.0F) {
            float useProgress = Math.min(5F, leftHandRaygunUseProgress) / 5F;
            event.getModel().leftArm.xRot = (event.getModel().head.xRot - (float) Math.toRadians(80F)) * useProgress;
            event.getModel().leftArm.yRot = event.getModel().head.yRot * useProgress;
            event.getModel().leftArm.zRot = 0;
            event.setCitadelResult(CitadelEvent.Result.ALLOW);
        }
        if (rightHandRaygunUseProgress > 0.0F) {
            float useProgress = Math.min(5F, rightHandRaygunUseProgress) / 5F;
            event.getModel().rightArm.xRot = (event.getModel().head.xRot - (float) Math.toRadians(80F)) * useProgress;
            event.getModel().rightArm.yRot = event.getModel().head.yRot * useProgress;
            event.getModel().rightArm.zRot = 0;
            event.setCitadelResult(CitadelEvent.Result.ALLOW);
        }
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof ShotGumItem && ShotGumItem.shouldBeHeldUpright(player.getItemInHand(InteractionHand.MAIN_HAND))) {
            if (player.getMainArm() == HumanoidArm.RIGHT) {
                event.getModel().rightArm.xRot = (event.getModel().head.xRot - (float) Math.toRadians(70F));
                event.getModel().rightArm.yRot = event.getModel().head.yRot;
                event.getModel().rightArm.zRot = 0;
                event.getModel().leftArm.xRot = event.getModel().head.xRot - (float) Math.toRadians(70F);
                event.getModel().leftArm.yRot = event.getModel().head.yRot + (float) Math.toRadians(40F);
                event.getModel().leftArm.zRot = (float) Math.toRadians(20F);
            } else {
                event.getModel().leftArm.xRot = (event.getModel().head.xRot - (float) Math.toRadians(70F));
                event.getModel().leftArm.yRot = event.getModel().head.yRot;
                event.getModel().leftArm.zRot = 0;
                event.getModel().rightArm.xRot = event.getModel().head.xRot - (float) Math.toRadians(70F);
                event.getModel().rightArm.yRot = event.getModel().head.yRot + (float) Math.toRadians(-40F);
                event.getModel().rightArm.zRot = (float) Math.toRadians(-20F);
            }
            event.setCitadelResult(CitadelEvent.Result.ALLOW);
        }
        if (player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof ShotGumItem && ShotGumItem.shouldBeHeldUpright(player.getItemInHand(InteractionHand.OFF_HAND))) {
            if (player.getMainArm() == HumanoidArm.RIGHT) {
                event.getModel().leftArm.xRot = (event.getModel().head.xRot - (float) Math.toRadians(70F));
                event.getModel().leftArm.yRot = event.getModel().head.yRot;
                event.getModel().leftArm.zRot = 0;
                event.getModel().rightArm.xRot = event.getModel().head.xRot - (float) Math.toRadians(70F);
                event.getModel().rightArm.yRot = event.getModel().head.yRot + (float) Math.toRadians(-40F);
                event.getModel().rightArm.zRot = (float) Math.toRadians(-20F);

            } else {
                event.getModel().rightArm.xRot = (event.getModel().head.xRot - (float) Math.toRadians(70F));
                event.getModel().rightArm.yRot = event.getModel().head.yRot;
                event.getModel().rightArm.zRot = 0;
                event.getModel().leftArm.xRot = event.getModel().head.xRot - (float) Math.toRadians(70F);
                event.getModel().leftArm.yRot = event.getModel().head.yRot + (float) Math.toRadians(40F);
                event.getModel().leftArm.zRot = (float) Math.toRadians(20F);
            }
            event.setCitadelResult(CitadelEvent.Result.ALLOW);
        }
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof CandyCaneHookItem && CandyCaneHookItem.isActive(player.getItemInHand(InteractionHand.MAIN_HAND)) && player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof CandyCaneHookItem && CandyCaneHookItem.isActive(player.getItemInHand(InteractionHand.OFF_HAND)) && player.getVehicle() instanceof GumWormSegmentEntity) {
            float rightWiggle = -Math.min(player.xxa, 0F) * (float) Math.sin(player.tickCount + AlexsCaves.PROXY.getPartialTicks()) * 25;
            float leftWiggle = Math.max(player.xxa, 0F) * (float) Math.sin(player.tickCount + AlexsCaves.PROXY.getPartialTicks()) * 25;
            event.getModel().rightArm.xRot = (float) Math.toRadians(-100F + rightWiggle);
            event.getModel().leftArm.xRot = (float) Math.toRadians(-100F + leftWiggle);
            event.getModel().rightArm.yRot = (float) Math.toRadians(20F);
            event.getModel().leftArm.yRot = (float) Math.toRadians(-20F);
            event.getModel().rightLeg.xRot = (float) Math.toRadians(-20F);
            event.getModel().leftLeg.xRot = (float) Math.toRadians(20F);
            event.setCitadelResult(CitadelEvent.Result.ALLOW);
        }
        if (event.getCitadelResult() != CitadelEvent.Result.ALLOW && player.hasEffect(ACCompat.effect(ACEffectRegistry.SUGAR_RUSH.get())) && !AlexsCaves.PROXY.isFirstPersonPlayer(player)) {
            float speedModifier = 0.35F;
            if(AlexsCaves.COMMON_CONFIG.sugarRushSlowsTime.get() && AlexsCaves.PROXY.isTickRateModificationActive(Minecraft.getInstance().level)){
                float tickRate = ClientTickRateTracker.getForClient(Minecraft.getInstance()).getClientTickRate() / 50.0F;
                speedModifier *= tickRate;
            }
            float deltaSpeed = 1.0F;
            float partialTicks = AlexsCaves.PROXY.getPartialTicks();
            float walkPos = player.walkAnimation.position(partialTicks);
            float walkSpeed = player.walkAnimation.speed(partialTicks);
            float headXRot = player.getViewXRot(partialTicks);
            float headYRot = Mth.lerp(partialTicks, player.yHeadRotO, player.yHeadRot) - Mth.lerp(partialTicks, player.yBodyRotO, player.yBodyRot);
            event.getModel().rightArm.xRot = Mth.cos(walkPos * speedModifier + (float) Math.PI * 0.5F) * 2.0F * walkSpeed * 0.5F / deltaSpeed;
            event.getModel().leftArm.xRot = Mth.cos(walkPos * speedModifier) * 2.0F * walkSpeed * 0.5F / deltaSpeed;
            event.getModel().rightArm.zRot = (Mth.sin(walkPos * -speedModifier + (float) Math.PI * 0.5F) + 2.5F) * 1.5F * walkSpeed * 0.5F / deltaSpeed;
            event.getModel().leftArm.zRot = (Mth.sin(walkPos * -speedModifier) - 2.5F) * 1.5F * walkSpeed * 0.5F / deltaSpeed;
            event.getModel().head.xRot = headXRot * ((float) Math.PI / 180F) + Mth.cos(walkPos * speedModifier + (float) Math.PI) * 1.0F * walkSpeed * 0.5F / deltaSpeed;
            event.getModel().head.yRot = headYRot * ((float) Math.PI / 180F) + Mth.sin(walkPos * speedModifier + (float) Math.PI) * 1.0F * walkSpeed * 0.5F / deltaSpeed;
            event.getModel().leftLeg.xRot = Mth.cos(walkPos * speedModifier + (float) Math.PI) * 4.0F * walkSpeed * 0.5F / deltaSpeed;
            event.getModel().rightLeg.xRot = Mth.cos(walkPos * speedModifier) * 4.0F * walkSpeed * 0.5F / deltaSpeed;
            event.setCitadelResult(CitadelEvent.Result.ALLOW);
        }
    }

    //? if <1.20.5 {
    @SubscribeEvent
    public void onPostRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
            renderRidingMeterHud(event.getGuiGraphics());
        }
        if (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            renderIrradiatedHearts(event.getGuiGraphics());
        }
    }
    //?}

    /**
     * How tall the vanilla status-bar stack grew this frame — what the riding meter and the darkness
     * meter sit above.
     *
     * <p>Forge tracked it on {@code ForgeGui} until 1.20.5 deleted that class along with the whole
     * overlay system; nothing on Forge tracks it since, so there the 53px floor the callers apply does
     * all the work. NeoForge instead patched the two counters straight onto vanilla's {@code Gui}.
     *
     * <p>Fabric takes the same answer as Forge, for the same reason and not by omission: the two
     * counters are a NeoForge patch, vanilla's {@code Gui} carries nothing equivalent on any version,
     * and this mod ships no HUD-height mixin of its own — so the floor is all there is. The gate is
     * spelled {@code !neoforge} rather than listing the two loaders, because that is the statement
     * being made: everything that is not the loader with the patch.
     */
    private static int hudStackHeight() {
        //? if <1.20.5 {
        return Minecraft.getInstance().gui instanceof ForgeGui forgeGui ? Math.max(forgeGui.leftHeight, forgeGui.rightHeight) : 0;
        //?}
        //? if neoforge && >=1.20.5 {
        /*return Math.max(Minecraft.getInstance().gui.leftHeight, Minecraft.getInstance().gui.rightHeight);
        *///?}
        //? if !neoforge && >=1.20.5 {
        /*return 0;
        *///?}
    }

    /** The HUD's own tick counter, which drives the shaking hearts. Public on vanilla's Gui from 1.20.5. */
    private static int hudTicks() {
        //? if <1.20.5 {
        return Minecraft.getInstance().gui instanceof ForgeGui forgeGui ? forgeGui.getGuiTicks() : 0;
        //?}
        //? if >=1.20.5 {
        /*return Minecraft.getInstance().gui.getGuiTicks();
        *///?}
    }

    /**
     * The riding meter of a mount that has one, plus the darkness armour's charge meter beside it.
     *
     * <p>Drawn straight after the crosshair, which is where both of them are anchored — hence the two
     * living in one method: the darkness meter is offset by however tall the mount's meter turned out
     * to be. Called from whichever per-era hook can say "the crosshair has just been drawn".
     */
    public static void renderRidingMeterHud(GuiGraphics guiGraphics) {
        Player player = AlexsCaves.PROXY.getClientSidePlayer();
        if (player == null) {
            return;
        }
        int hudY = 0;
        if (player.getVehicle() instanceof RidingMeterMount mount && mount.hasRidingMeter()) {
            int screenWidth = guiGraphics.guiWidth();
            int screenHeight = guiGraphics.guiHeight();
            int forgeGuiY = hudStackHeight();
            if (player.getArmorValue() > 0 && mount instanceof SubterranodonEntity) {
                forgeGuiY += 25;
            }
            if (forgeGuiY < 53) {
                forgeGuiY = 53;
            }
            int j = screenWidth / 2 - AlexsCaves.CLIENT_CONFIG.subterranodonIndicatorX.get();
            int k = screenHeight - forgeGuiY - AlexsCaves.CLIENT_CONFIG.subterranodonIndicatorY.get();
            float f = mount.getMeterAmount();
            float invProgress = 1 - f;
            int uOffset = 0;
            int vOffset = 0;
            int dinoHeight = 31;
            if (mount instanceof TremorsaurusEntity) {
                vOffset = 63;
                k += 5;
                hudY = 20;
            } else if (mount instanceof AtlatitanEntity) {
                vOffset = 126;
                dinoHeight = 32;
                k += 3;
                hudY = 40;
            } else if (mount instanceof TremorzillaEntity tremorzilla) {
                vOffset = 193;
                if (tremorzilla.isPowered() && !tremorzilla.isFiring() && tremorzilla.getSpikesDownAmount() > 0) {
                    if (tremorzilla.tickCount / 2 % 2 == 1) {
                        vOffset = 251;
                    }
                    invProgress = 1F;
                }
                dinoHeight = 29;
                k += 5;
                hudY = 20;
            } else if (mount instanceof CandicornEntity) {
                vOffset = 280;
                dinoHeight = 25;
                hudY = 40;
                k += 4;
            } else {
                hudY = 40;
            }
            ACClientCompat.pushPose(guiGraphics);
            ACClientCompat.blit(guiGraphics, DINOSAUR_HUD_OVERLAYS, j, k, 50, uOffset, vOffset + dinoHeight, 43, dinoHeight, 128, 512);
            ACClientCompat.blit(guiGraphics, DINOSAUR_HUD_OVERLAYS, j, k, 50, uOffset, vOffset, 43, (int) Math.floor(dinoHeight * invProgress), 128, 512);
            ACClientCompat.popPose(guiGraphics);
        }
        if (DarknessArmorItem.hasMeter(player)) {
            ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
            int screenWidth = guiGraphics.guiWidth();
            int screenHeight = guiGraphics.guiHeight();
            int forgeGuiY = hudStackHeight();
            if (forgeGuiY < 53) {
                forgeGuiY = 53;
            }
            int j = screenWidth / 2 - AlexsCaves.CLIENT_CONFIG.subterranodonIndicatorX.get() + 13;
            int k = screenHeight - forgeGuiY - AlexsCaves.CLIENT_CONFIG.subterranodonIndicatorY.get() + 9 - hudY;
            float f = DarknessArmorItem.getMeterProgress(stack);
            float invProgress = 1 - f;
            int uvOffset = DarknessArmorItem.canChargeUp(stack) && f >= 1.0F ? 0 : 18;
            ACClientCompat.pushPose(guiGraphics);
            ACClientCompat.blit(guiGraphics, ARMOR_HUD_OVERLAYS, j, k, 50, uvOffset, 19, 18, 19, 128, 128);
            ACClientCompat.blit(guiGraphics, ARMOR_HUD_OVERLAYS, j, k, 50, 0, 0, 18, (int) Math.floor(19 * invProgress), 128, 128);
            ACClientCompat.popPose(guiGraphics);
        }
    }

    /**
     * Repaints the health bar in the sickly palette the irradiated effect gives it.
     *
     * <p>Drawn over the vanilla hearts rather than instead of them, so it has to follow them: on the
     * eras that expose a per-element hook that is the player-health element, and on Forge from 1.20.5
     * — where the hearts are welded into one coarse "hotbar" layer — it is that whole layer.
     */
    public static void renderIrradiatedHearts(GuiGraphics guiGraphics) {
        Player player = AlexsCaves.PROXY.getClientSidePlayer();
        if (player == null || !Minecraft.getInstance().gameMode.canHurtPlayer() || !(Minecraft.getInstance().getCameraEntity() instanceof Player) || !player.hasEffect(ACCompat.effect(ACEffectRegistry.IRRADIATED.get()))) {
            return;
        }
        int leftHeight = 39;
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        int health = Mth.ceil(player.getHealth());
        int forgeGuiTick = hudTicks();
        AttributeInstance attrMaxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        float healthMax = (float) attrMaxHealth.getValue();
        float absorb = Mth.ceil(player.getAbsorptionAmount());

        int healthRows = Mth.ceil((healthMax + absorb) / 2.0F / 10.0F);
        int rowHeight = Math.max(10 - (healthRows - 2), 3);

        ClientProxy.random.setSeed(forgeGuiTick * 312871L);
        int left = width / 2 - 91;
        int top = height - leftHeight;
        int regen = -1;
        if (player.hasEffect(MobEffects.REGENERATION)) {
            regen = forgeGuiTick % Mth.ceil(healthMax + 5.0F);
        }
        final int heartV = player.level().getLevelData().isHardcore() ? 9 : 0;
        int heartU = 0;
        float absorbRemaining = absorb;
        ACClientCompat.pushPose(guiGraphics);
        ACClientCompat.setImmediateTint(1.0F, 1.0F, 1.0F, 1.0F);
        // The shader and texture upstream bound here were dead: every draw in the loop below goes
        // through ACClientCompat.blit, i.e. GuiGraphics, which names its own texture and shader per
        // quad. 1.21.5 deleted both setters, which is what surfaced it.
        for (int i = Mth.ceil((healthMax + absorb) / 2.0F) - 1; i >= 0; --i) {
            int row = Mth.ceil((float) (i + 1) / 10.0F) - 1;
            int x = left + i % 10 * 8;
            int y = top - row * rowHeight;
            if (health <= 4) {
                y += ClientProxy.random.nextInt(2);
            }
            if (i == regen) {
                y -= 2;
            }
            ACClientCompat.blit(guiGraphics, POTION_EFFECT_HUD_OVERLAYS, x, y, 50, heartU, heartV + 18, 9, 9, 32, 32);
            if (absorbRemaining > 0.0F) {
                if (absorbRemaining == absorb && absorb % 2.0F == 1.0F) {
                    ACClientCompat.blit(guiGraphics, POTION_EFFECT_HUD_OVERLAYS, x, y, 50, heartU + 9, heartV, 9, 9, 32, 32);
                    absorbRemaining -= 1.0F;
                } else {
                    ACClientCompat.blit(guiGraphics, POTION_EFFECT_HUD_OVERLAYS, x, y, 50, heartU, heartV, 9, 9, 32, 32);
                    absorbRemaining -= 2.0F;
                }
            } else {
                if (i * 2 + 1 < health) {
                    ACClientCompat.blit(guiGraphics, POTION_EFFECT_HUD_OVERLAYS, x, y, 50, heartU, heartV, 9, 9, 32, 32);
                } else if (i * 2 + 1 == health) {
                    ACClientCompat.blit(guiGraphics, POTION_EFFECT_HUD_OVERLAYS, x, y, 50, heartU + 9, heartV, 9, 9, 32, 32);
                }
            }
        }
        ACClientCompat.popPose(guiGraphics);
    }

    /** Draws this mod's own boss bar in place of the vanilla one, when the boss asked for it. */
    private boolean acRenderBossOverlay(CustomizeGuiOverlayEvent.BossEventProgress event) {
        boolean cancel = false;
        if (ClientProxy.bossBarRenderTypes.containsKey(event.getBossEvent().getId())) {
            int renderTypeFor = ClientProxy.bossBarRenderTypes.get(event.getBossEvent().getId());
            int i = event.getGuiGraphics().guiWidth();
            int j = event.getY();
            Component component = event.getBossEvent().getName();
            if (renderTypeFor == 0) {
                cancel = true;
                ACClientCompat.blit(event.getGuiGraphics(), BOSS_BAR_HUD_OVERLAYS, event.getX(), event.getY(), 0, 0, 182, 15);
                int progressScaled = (int) (event.getBossEvent().getProgress() * 183.0F);
                ACClientCompat.blit(event.getGuiGraphics(), BOSS_BAR_HUD_OVERLAYS, event.getX(), event.getY(), 0, 15, progressScaled, 15);
                int l = Minecraft.getInstance().font.width(component);
                int i1 = i / 2 - l / 2;
                int j1 = j - 9;
                // Both colours carry their alpha byte, which upstream left at 0. Vanilla used to fill a
                // missing one in for you and stopped before 1.21.5, so an alpha-0 glyph is simply
                // invisible there; these are the values the versions that did fill it in produced.
                //? if >=1.21.6 {
                /*// drawInBatch8xOutline wants a MultiBufferSource, and none exists while the GUI is
                // being collected into render states. It is nine ordinary string draws — the text at
                // the eight ±1 glyph-shadow offsets in the outline colour, then the text over them —
                // so spell those out instead.
                net.minecraft.util.FormattedCharSequence bossName = component.getVisualOrderText();
                for (int outlineX = -1; outlineX <= 1; outlineX++) {
                    for (int outlineY = -1; outlineY <= 1; outlineY++) {
                        if (outlineX != 0 || outlineY != 0) {
                            event.getGuiGraphics().drawString(Minecraft.getInstance().font, bossName, i1 + outlineX, j1 + outlineY, 0XFF361515, false);
                        }
                    }
                }
                event.getGuiGraphics().drawString(Minecraft.getInstance().font, bossName, i1, j1, 0XFFFF5100, false);
                *///?} else {
                PoseStack poseStack = event.getGuiGraphics().pose();
                poseStack.pushPose();
                poseStack.translate(i1, j1, 0);
                ACClientCompat.drawSpecial(event.getGuiGraphics(), bufferSource -> Minecraft.getInstance().font.drawInBatch8xOutline(component.getVisualOrderText(), 0.0F, 0.0F, 0XFFFF5100, 0XFF361515, poseStack.last().pose(), bufferSource, 240));
                poseStack.popPose();
                //?}
                event.setIncrement(event.getIncrement() + 7);
            }
        }
        return cancel;
    }

    //? if forge && >=1.21.6 {
    /*@SubscribeEvent(priority = EventPriority.LOWEST)
    public boolean renderBossOverlay(CustomizeGuiOverlayEvent.BossEventProgress event) {
        return acRenderBossOverlay(event);
    }
    *///?} else {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void renderBossOverlay(CustomizeGuiOverlayEvent.BossEventProgress event) {
        if (acRenderBossOverlay(event)) {
            event.setCanceled(true);
        }
    }
    //?}

    /** Picks this mod's fog distances for the fluid or block the camera sits in. */
    private boolean acFogRender(ViewportEvent.RenderFog event) {
        //? if <1.21.6 {
        if (event.isCanceled()) {
            //another mod has cancelled fog rendering. Nothing to ask from 1.21.6 on: EventBus 7
            //(Forge 56) never calls a later listener once the event is cancelled, and NeoForge 21.6
            //made RenderFog non-cancellable outright — see the listener below.
            return false;
        }
        //?}
        // The band-aid below has nothing left to read from 1.21.6: the fog is a std140 block
        // FogRenderer writes straight into a GPU ring buffer, and RenderSystem's getter hands back
        // an opaque slice of it. The event's own getters are the values now — they read the very
        // FogData fields setFarPlaneDistance/setNearPlaneDistance write — so this is the same pair,
        // just no longer able to see a foreign mod that reached past the event.
        //? if >=1.21.6 {
        /*float defaultFarPlaneDistance = event.getFarPlaneDistance();
        float defaultNearPlaneDistance = event.getNearPlaneDistance();
        *///?} else {
        //some mods incorrectly set the RenderSystem fog start and end directly, so this will have to do as a band-aid...
        float defaultFarPlaneDistance = ACClientCompat.getShaderFogEnd();
        float defaultNearPlaneDistance = ACClientCompat.getShaderFogStart();
        //?}

        Entity player = Minecraft.getInstance().getCameraEntity();
        FluidState fluidstate = player.level().getFluidState(event.getCamera().getBlockPosition());
        BlockState blockState = player.level().getBlockState(event.getCamera().getBlockPosition());
        if (!fluidstate.isEmpty() && fluidstate.getType().is(ACTagRegistry.ACID)) {
            float farness = 10.0F;
            if (Minecraft.getInstance().player.hasEffect(ACCompat.effect(ACEffectRegistry.DEEPSIGHT.get()))) {
                farness *= 1.0F + 1.5F * DeepsightEffect.getIntensity(Minecraft.getInstance().player, (float) event.getPartialTick());
            }
            event.setFarPlaneDistance(farness);
            event.setNearPlaneDistance(0.0F);
            return true;
        }
        if (!fluidstate.isEmpty() && fluidstate.getType().is(ACTagRegistry.PURPLE_SODA)) {
            float farness = 20.0F;
            float nearness = -8.0F;
            if (Minecraft.getInstance().player.hasEffect(ACCompat.effect(ACEffectRegistry.DEEPSIGHT.get()))) {
                float f = DeepsightEffect.getIntensity(Minecraft.getInstance().player, (float) event.getPartialTick());
                farness *= 1.0F + 1.5F * f;
                nearness *= 1.0F - f;
            }
            event.setFarPlaneDistance(farness);
            event.setNearPlaneDistance(nearness);
            return true;
        }
        if (blockState.is(ACBlockRegistry.PRIMAL_MAGMA.get()) || blockState.is(ACBlockRegistry.FISSURE_PRIMAL_MAGMA.get())) {
            float farness = 2.0F;
            if (Minecraft.getInstance().player.hasEffect(ACCompat.effect(ACEffectRegistry.DEEPSIGHT.get()))) {
                farness *= 1.0F + 1.5F * DeepsightEffect.getIntensity(Minecraft.getInstance().player, (float) event.getPartialTick());
            }
            event.setFarPlaneDistance(farness);
            event.setNearPlaneDistance(0.0F);
            return true;
        }
        if (event.getCamera().getFluidInCamera() == FogType.WATER && AlexsCaves.CLIENT_CONFIG.biomeWaterFogOverrides.get()) {
            float farness = lastSampledWaterFogFarness;
            if (Minecraft.getInstance().player.hasEffect(ACCompat.effect(ACEffectRegistry.DEEPSIGHT.get()))) {
                farness *= 1.0F + 1.5F * DeepsightEffect.getIntensity(Minecraft.getInstance().player, (float) event.getPartialTick());
            }
            if (farness != 1.0F) {
                event.setFarPlaneDistance(defaultFarPlaneDistance * farness);
                return true;
            }
        // 1.21.6 moved FogRenderer into .renderer.fog and deleted FogMode with it: the sky pass no
        // longer sets up its own fog, so RenderFog fires once per frame and the surviving call is
        // the terrain one. The test that distinguished them is therefore vacuously true.
        //? if >=1.21.6 {
        /*} else if (AlexsCaves.CLIENT_CONFIG.biomeSkyFogOverrides.get()) {
        *///?} else {
        } else if (event.getMode() == FogRenderer.FogMode.FOG_TERRAIN && AlexsCaves.CLIENT_CONFIG.biomeSkyFogOverrides.get()) {
        //?}
            float nearness = lastSampledFogNearness;
            float primordialBossAmount = AlexsCaves.PROXY.getPrimordialBossActiveAmount((float) event.getPartialTick());
            boolean flag = Math.abs(nearness) - 1.0F < 0.01F;
            if (primordialBossAmount > 0.0F) {
                flag = true;
                nearness *= (1.0F - primordialBossAmount * 0.75F);
            }
            if (flag) {
                event.setNearPlaneDistance(defaultNearPlaneDistance * nearness);
                return true;
            }
        }
        return false;
    }

    // NeoForge 21.6 dropped ICancellableEvent from RenderFog: setFar/NearPlaneDistance write straight
    // into the shared FogData, so the override lands whether or not anything is "cancelled" and the
    // verdict has nowhere to go. The helper still computes it — it is what decides *whether* to write
    // anything — the listener simply has nothing to do with the answer on that loader.
    //? if forge && >=1.21.6 {
    /*@SubscribeEvent(priority = EventPriority.LOWEST)
    public boolean fogRender(ViewportEvent.RenderFog event) {
        return acFogRender(event);
    }
    *///?} elif >=1.21.6 {
    /*@SubscribeEvent(priority = EventPriority.LOWEST)
    public void fogRender(ViewportEvent.RenderFog event) {
        acFogRender(event);
    }
    *///?} else {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void fogRender(ViewportEvent.RenderFog event) {
        if (acFogRender(event)) {
            event.setCanceled(true);
        }
    }
    //?}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void fogColor(ViewportEvent.ComputeFogColor event) {
        Entity player = Minecraft.getInstance().player;
        BlockState blockState = player.level().getBlockState(event.getCamera().getBlockPosition());
        if (blockState.is(ACBlockRegistry.PRIMAL_MAGMA.get()) || blockState.is(ACBlockRegistry.FISSURE_PRIMAL_MAGMA.get())) {
            event.setRed(1F);
            event.setGreen(0.4F);
            event.setBlue((float) (0));
        } else if (ACFluids.isEyeInAcid(player)) {
            event.setRed((float) (0));
            event.setGreen((float) (1));
            event.setBlue((float) (0));
        } else if (ACFluids.isEyeInPurpleSoda(player)) {
            event.setRed(0.6F);
            event.setGreen(0.1F);
            event.setBlue(0.85F);
        } else if (event.getCamera().getFluidInCamera() == FogType.NONE && AlexsCaves.CLIENT_CONFIG.biomeSkyFogOverrides.get()) {
            float override = ClientProxy.acSkyOverrideAmount;
            float setR = event.getRed();
            float setG = event.getGreen();
            float setB = event.getBlue();

            boolean flag = false;
            if (override != 0.0F) {
                flag = true;
                Vec3 vec3 = lastSampledFogColor;
                setR = (float) (vec3.x - setR) * override + setR;
                setG = (float) (vec3.y - setG) * override + setG;
                setB = (float) (vec3.z - setB) * override + setB;
            }
            float primordialBossAmount = AlexsCaves.PROXY.getPrimordialBossActiveAmount((float) event.getPartialTick());
            if (primordialBossAmount > 0.0F) {
                flag = true;
                setR = (0.8F - setR) * primordialBossAmount + setR;
                setG = (0.2F - setG) * primordialBossAmount + setG;
                setB = (0.15F - setB) * primordialBossAmount + setB;
            }
            if (flag) {
                event.setRed(setR);
                event.setGreen(setG);
                event.setBlue(setB);
            }
        } else if (event.getCamera().getFluidInCamera() == FogType.WATER && AlexsCaves.CLIENT_CONFIG.biomeWaterFogOverrides.get()) {
            int i = Minecraft.getInstance().options.biomeBlendRadius().get();
            float override = ClientProxy.acSkyOverrideAmount;
            if (override != 0) {
                Vec3 vec3 = lastSampledWaterFogColor;
                event.setRed((float) (event.getRed() + (vec3.x - event.getRed()) * override));
                event.setGreen((float) (event.getGreen() + (vec3.y - event.getGreen()) * override));
                event.setBlue((float) (event.getBlue() + (vec3.z - event.getBlue()) * override));
            }
        }
    }

    private void rotateForAngle(LivingEntity entity, PoseStack matrixStackIn, Direction rotate, float f, float width, float height) {
        boolean down = entity.zza < 0.0F;
        switch (rotate) {
            case DOWN:
                break;
            case UP:
                matrixStackIn.translate(0.0D, height * f, 0.0D);
                matrixStackIn.mulPose(Axis.XP.rotationDegrees(-180.0F * f));
                matrixStackIn.mulPose(Axis.YP.rotationDegrees(-180.0F * f));
                break;
            case NORTH:
                matrixStackIn.mulPose(Axis.XP.rotationDegrees(90.0F * f));
                matrixStackIn.translate(0.0D, -0.25f * f, 0.0D);
                if (down) {
                    matrixStackIn.mulPose(Axis.YP.rotationDegrees(180.0F * f));
                }
                break;
            case SOUTH:
                matrixStackIn.mulPose(Axis.YP.rotationDegrees(180 * f));
                matrixStackIn.mulPose(Axis.XP.rotationDegrees(90.0F * f));
                matrixStackIn.translate(0.0D, -0.25f * f, 0.0D);
                if (down) {
                    matrixStackIn.mulPose(Axis.YP.rotationDegrees(180.0F * f));
                }
                break;
            case WEST:
                matrixStackIn.mulPose(Axis.YP.rotationDegrees(90 * f));
                matrixStackIn.mulPose(Axis.XP.rotationDegrees(90.0F * f));
                matrixStackIn.translate(0.0D, -0.25f * f, 0.0D);
                if (down) {
                    matrixStackIn.mulPose(Axis.YP.rotationDegrees(180.0F * f));
                }
                break;
            case EAST:
                matrixStackIn.mulPose(Axis.YP.rotationDegrees(-90 * f));
                matrixStackIn.mulPose(Axis.XP.rotationDegrees(90.0F * f));
                matrixStackIn.translate(0.0D, -0.25f * f, 0.0D);
                if (down) {
                    matrixStackIn.mulPose(Axis.YP.rotationDegrees(180.0F * f));
                }
                break;
        }
    }

    private static float calculateBiomeAmbientLight(Entity player) {
        int i = Minecraft.getInstance().options.biomeBlendRadius().get();
        if (i == 0) {
            return ACBiomeRegistry.getBiomeAmbientLight(player.level().getBiome(player.blockPosition()));
        } else {
            return BiomeSampler.sampleBiomesFloat(player.level(), player.position(), ACBiomeRegistry::getBiomeAmbientLight);
        }
    }

    private static Vec3 calculateBiomeLightColor(Entity player) {
        int i = Minecraft.getInstance().options.biomeBlendRadius().get();
        if (i == 0) {
            return ACBiomeRegistry.getBiomeLightColorOverride(player.level().getBiome(player.blockPosition()));
        } else {
            return BiomeSampler.sampleBiomesVec3(player.level(), player.position(), ACBiomeRegistry::getBiomeLightColorOverride);
        }
    }

    private static float calculateBiomeFogNearness(Entity player) {
        int i = Minecraft.getInstance().options.biomeBlendRadius().get();
        float nearness;
        if (i == 0) {
            nearness = ACBiomeRegistry.getBiomeFogNearness(player.level().getBiome(player.blockPosition()));
        } else {
            nearness = BiomeSampler.sampleBiomesFloat(player.level(), player.position(), ACBiomeRegistry::getBiomeFogNearness);
        }
        return nearness;
    }

    private static float calculateBiomeWaterFogFarness(Entity player) {
        int i = Minecraft.getInstance().options.biomeBlendRadius().get();
        float farness;
        if (i == 0) {
            farness = ACBiomeRegistry.getBiomeWaterFogFarness(player.level().getBiome(player.blockPosition()));
        } else {
            farness = BiomeSampler.sampleBiomesFloat(player.level(), player.position(), ACBiomeRegistry::getBiomeWaterFogFarness);
        }
        return farness;
    }

    // 1.21.11 deleted DimensionSpecialEffects and moved every colour a biome used to carry into the
    // environment-attribute system, so both the per-biome accessor and the getBrightnessDependentFogColor
    // wrapper are gone. Neither is missed. The wrapper was only ever the overworld's brightness ramp and
    // this mod always passed 1.0F, at which it is the identity (0.94*1+0.06 == 1, 0.91*1+0.09 == 1); and
    // the system already interpolates spatially across neighbouring biomes, which is exactly what the
    // biomeBlendRadius branch below hand-rolled through BiomeSampler. One call replaces both branches.
    private static Vec3 calculateBiomeFogColor(Entity player) {
        //? if >=1.21.11 {
        /*return new Vec3(net.minecraft.util.ARGB.vector3fFromRGB24(player.level().environmentAttributes()
                .getValue(net.minecraft.world.attribute.EnvironmentAttributes.FOG_COLOR, player.blockPosition())));
        *///?} else {
        int i = Minecraft.getInstance().options.biomeBlendRadius().get();
        Vec3 vec3;
        if (i == 0) {
            vec3 = ((ClientLevel) player.level()).effects().getBrightnessDependentFogColor(Vec3.fromRGB24(player.level().getBiomeManager().getNoiseBiomeAtPosition(player.blockPosition()).value().getFogColor()), 1.0F);
        } else {
            vec3 = ((ClientLevel) player.level()).effects().getBrightnessDependentFogColor(BiomeSampler.sampleBiomesVec3(player.level(), player.position(), biomeHolder -> Vec3.fromRGB24(biomeHolder.value().getFogColor())), 1.0F);
        }
        return vec3;
        //?}
    }

    // See calculateBiomeFogColor.
    private Vec3 calculateBiomeWaterFogColor(Entity player) {
        //? if >=1.21.11 {
        /*return new Vec3(net.minecraft.util.ARGB.vector3fFromRGB24(player.level().environmentAttributes()
                .getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_FOG_COLOR, player.blockPosition())));
        *///?} else {
        int i = Minecraft.getInstance().options.biomeBlendRadius().get();
        Vec3 vec3;
        if (i == 0) {
            vec3 = ((ClientLevel) player.level()).effects().getBrightnessDependentFogColor(Vec3.fromRGB24(player.level().getBiomeManager().getNoiseBiomeAtPosition(player.blockPosition()).value().getWaterFogColor()), 1.0F);
        } else {
            vec3 = ((ClientLevel) player.level()).effects().getBrightnessDependentFogColor(BiomeSampler.sampleBiomesVec3(player.level(), player.position(), biomeHolder -> Vec3.fromRGB24(biomeHolder.value().getWaterFogColor())), 1.0F);
        }
        return vec3;
        //?}
    }

    // See CitadelEvents#onServerTick: from 1.20.5 NeoForge has a ClientTickEvent of its own with
    // Pre/Post subclasses in place of TickEvent's phase field. END is Post.
    // Forge 59.x split it the same way in 1.21.9, but kept the event under net.minecraftforge.event.
    //? if forge && >=1.21.9 {
    /*@SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent.Post event) {
        tickClient();
    }
    *///?} elif neoforge && >=1.20.5 {
    /*@SubscribeEvent
    public void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        tickClient();
    }
    *///?} else {
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickClient();
        }
    }
    //?}

    private void tickClient() {
        Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
        float partialTicks = AlexsCaves.PROXY.getPartialTicks();
        if (ClientProxy.shaderLoadAttemptCooldown > 0) {
            ClientProxy.shaderLoadAttemptCooldown--;
        }
        ClientProxy.prevPrimordialBossActiveAmount = ClientProxy.primordialBossActiveAmount;
        ClientProxy.prevNukeFlashAmount = ClientProxy.nukeFlashAmount;
        if (cameraEntity != null) {
            ClientProxy.acSkyOverrideAmount = ACBiomeRegistry.calculateBiomeSkyOverride(cameraEntity);
            if (ClientProxy.acSkyOverrideAmount > 0) {
                //? if >=1.21.11 {
                /*ClientProxy.acSkyOverrideColor = new Vec3(net.minecraft.util.ARGB.vector3fFromRGB24(Minecraft.getInstance().level.environmentAttributes()
                        .getValue(net.minecraft.world.attribute.EnvironmentAttributes.SKY_COLOR, cameraEntity.blockPosition())));
                *///?} else {
                ClientProxy.acSkyOverrideColor = BiomeSampler.sampleBiomesVec3(Minecraft.getInstance().level, Minecraft.getInstance().getCameraEntity().position(), biomeHolder -> Vec3.fromRGB24(biomeHolder.value().getSkyColor()));
                //?}
            }
            ClientProxy.lastBiomeLightColorPrev = ClientProxy.lastBiomeLightColor;
            ClientProxy.lastBiomeLightColor = calculateBiomeLightColor(cameraEntity);
            ClientProxy.lastBiomeAmbientLightAmountPrev = ClientProxy.lastBiomeAmbientLightAmount;
            ClientProxy.lastBiomeAmbientLightAmount = calculateBiomeAmbientLight(cameraEntity);
            lastSampledFogNearness = calculateBiomeFogNearness(cameraEntity);
            lastSampledWaterFogFarness = calculateBiomeWaterFogFarness(cameraEntity);
            if (cameraEntity.level() instanceof ClientLevel) { //fixes crash with beholder
                lastSampledFogColor = calculateBiomeFogColor(cameraEntity);
                lastSampledWaterFogColor = calculateBiomeWaterFogColor(cameraEntity);
            }
        }
        if (ClientProxy.renderNukeSkyDarkFor > 0) {
            ClientProxy.renderNukeSkyDarkFor--;
        }
        if (ClientProxy.muteNonNukeSoundsFor > 0) {
            ClientProxy.muteNonNukeSoundsFor--;
            if (ClientProxy.masterVolumeNukeModifier < 1.0F) {
                ClientProxy.masterVolumeNukeModifier += 0.1F;
            }
        } else if (ClientProxy.masterVolumeNukeModifier > 0.0F) {
            ClientProxy.masterVolumeNukeModifier -= 0.1F;
        }
        if (ClientProxy.lastBossLevel != Minecraft.getInstance().level) {
            ClientProxy.primordialBossActive = false;
            ClientProxy.primordialBossActiveAmount = 0;
            ClientProxy.lastBossLevel = Minecraft.getInstance().level;
        }
        if (ClientProxy.primordialBossActive) {
            if (ClientProxy.primordialBossActiveAmount < 1.0F) {
                ClientProxy.primordialBossActiveAmount += 0.025F;
            }
        } else {
            if (ClientProxy.primordialBossActiveAmount > 0.0F) {
                ClientProxy.primordialBossActiveAmount -= 0.025F;
            }
        }
        if (ClientProxy.renderNukeFlashFor > 0) {
            if (ClientProxy.nukeFlashAmount < 1F) {
                ClientProxy.nukeFlashAmount = Math.min(ClientProxy.nukeFlashAmount + 0.4F, 1F);
            }
            ClientProxy.renderNukeFlashFor--;
        } else if (ClientProxy.nukeFlashAmount > 0F) {
            ClientProxy.nukeFlashAmount = Math.max(ClientProxy.nukeFlashAmount - 0.05F, 0F);
        }
        ClientProxy.prevPossessionStrengthAmount = ClientProxy.possessionStrengthAmount;
        if (Minecraft.getInstance().getCameraEntity() instanceof PossessesCamera watcherEntity) {
            if (watcherEntity.instant()) {
                ClientProxy.possessionStrengthAmount = watcherEntity.getPossessionStrength(partialTicks);
            } else {
                if (ClientProxy.possessionStrengthAmount < watcherEntity.getPossessionStrength(partialTicks)) {
                    ClientProxy.possessionStrengthAmount = Math.min(ClientProxy.possessionStrengthAmount + 0.2F, watcherEntity.getPossessionStrength(partialTicks));
                } else {
                    ClientProxy.possessionStrengthAmount = Math.max(ClientProxy.possessionStrengthAmount - 0.2F, watcherEntity.getPossessionStrength(partialTicks));
                }
            }
            if (watcherEntity instanceof BeholderEyeEntity beholderEye) {
                beholderEye.setOldRots();
                beholderEye.setEyeYRot(Minecraft.getInstance().player.getYHeadRot());
                beholderEye.setEyeXRot(Minecraft.getInstance().player.getXRot());
                if (AlexsCaves.PROXY.isKeyDown(4)) {
                    AlexsCaves.PROXY.resetRenderViewEntity(Minecraft.getInstance().player);
                }
            }
        } else if (ClientProxy.possessionStrengthAmount > 0F) {
            ClientProxy.possessionStrengthAmount = Math.max(ClientProxy.possessionStrengthAmount - 0.05F, 0F);
        }
        if (Minecraft.getInstance().screen instanceof AdvancementsScreen advancementsScreen && advancementsScreen.selectedTab != null && ACAdvancementTabs.isAlexsCavesWidget(ACClientPlatform.advancementId(advancementsScreen.selectedTab))) {
            ACAdvancementTabs.tick();
        }
        if (ClientProxy.primordialBossActive && Minecraft.getInstance().level != null && !Minecraft.getInstance().isPaused()) {
            ClientLevel level = Minecraft.getInstance().level;
            BlockPos cameraBlockPos = Minecraft.getInstance().getCameraEntity().blockPosition();
            BlockPos.MutableBlockPos trySpawnParticleBlockPos = new BlockPos.MutableBlockPos();
            int dist = 16;
            for (int particles = 0; particles < 100; ++particles) {
                int i = cameraBlockPos.getX() + level.getRandom().nextInt(dist) - level.getRandom().nextInt(dist);
                int j = cameraBlockPos.getY() + level.getRandom().nextInt(dist) - level.getRandom().nextInt(dist);
                int k = cameraBlockPos.getZ() + level.getRandom().nextInt(dist) - level.getRandom().nextInt(dist);
                trySpawnParticleBlockPos.set(i, j, k);
                BlockState blockstate = level.getBlockState(trySpawnParticleBlockPos);
                if (!blockstate.isCollisionShapeFullBlock(level, trySpawnParticleBlockPos)) {
                    level.addParticle(ParticleTypes.ASH, (double) trySpawnParticleBlockPos.getX() + level.getRandom().nextDouble(), (double) trySpawnParticleBlockPos.getY() + level.getRandom().nextDouble(), (double) trySpawnParticleBlockPos.getZ() + level.getRandom().nextDouble(), 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    /** A submarine keeps the water out, so the drowning overlay does not belong on its screen. */
    private boolean acRenderBlockScreenEffect(RenderBlockScreenEffectEvent event) {
        Player player = event.getPlayer();
        return player.isPassenger() && player.getVehicle() instanceof SubmarineEntity && event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.WATER;
    }

    //? if forge && >=1.21.6 {
    /*@SubscribeEvent
    public boolean onRenderBlockScreenEffect(RenderBlockScreenEffectEvent event) {
        return acRenderBlockScreenEffect(event);
    }
    *///?} else {
    @SubscribeEvent
    public void onRenderBlockScreenEffect(RenderBlockScreenEffectEvent event) {
        if (acRenderBlockScreenEffect(event)) {
            event.setCanceled(true);
        }
    }
    //?}

    @SubscribeEvent
    public void onComputeFOV(ViewportEvent.ComputeFov event) {
        if (event.getCamera().getEntity() instanceof PossessesCamera) {
            event.setFOV(90);
        }
        Player player = Minecraft.getInstance().player;
        FogType fogtype = event.getCamera().getFluidInCamera();
        if (player != null && player.isPassenger() && player.getVehicle() instanceof SubmarineEntity && fogtype == FogType.WATER) {
            float f = (float) Mth.lerp(Minecraft.getInstance().options.fovEffectScale().get(), 1.0D, 0.85714287F);
            event.setFOV(event.getFOV() / f);
        }
    }

    @SubscribeEvent
    public void onComputeFOVModifier(ComputeFovModifierEvent event) {
        ItemStack itemstack = event.getPlayer().getUseItem();
        if (event.getPlayer().isUsingItem()) {
            if (itemstack.is(ACItemRegistry.DREADBOW.get())) {
                int i = event.getPlayer().getTicksUsingItem();
                float f1 = (float) i / 20.0F;
                if (f1 > 1.0F) {
                    f1 = 1.0F;
                } else {
                    f1 *= f1;
                }
                event.setNewFovModifier(event.getFovModifier() * (1.0F - f1 * 0.15F));
            }
        }
    }

    @SubscribeEvent
    public void onSplashTextRender(EventRenderSplashText.Pre event) {
        if (ClientProxy.hasACSplashText) {
            event.setCitadelResult(CitadelEvent.Result.ALLOW);
            event.setSplashText("30k downloads max");
            event.setSplashTextColor(0X00B6D5);
        }
    }

    @SubscribeEvent
    public void outlineColor(EventGetOutlineColor event) {
        if (Minecraft.getInstance().player.getUseItem() != null && Minecraft.getInstance().player.getUseItem().is(ACItemRegistry.TOTEM_OF_POSSESSION.get())) {
            ItemStack stack = Minecraft.getInstance().player.getUseItem();
            UUID boundUUID = TotemOfPossessionItem.getBoundEntityUUID(stack);
            if (boundUUID != null && boundUUID.equals(event.getEntityIn().getUUID())) {
                event.setCitadelResult(CitadelEvent.Result.ALLOW);
                event.setColor(0xFF0000);
            }
        }
        if (event.getEntityIn() instanceof ItemEntity item) {
            if (item.getItem().is(ACItemRegistry.TECTONIC_SHARD.get())) {
                event.setCitadelResult(CitadelEvent.Result.ALLOW);
                event.setColor(0XFFDB00);
            }
            if (item.getItem().is(ACItemRegistry.SWEET_TOOTH.get())) {
                event.setCitadelResult(CitadelEvent.Result.ALLOW);
                event.setColor(0XFF8ACD);
            }
        }
    }

}
