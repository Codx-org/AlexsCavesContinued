package com.github.alexmodguy.alexscaves.citadel;

import com.github.alexmodguy.alexscaves.citadel.client.render.pathfinding.WorldEventContext;
import com.github.alexmodguy.alexscaves.citadel.client.tick.ClientTickRateTracker;
import com.github.alexmodguy.alexscaves.citadel.server.entity.pathfinding.raycoms.Pathfinding;
import net.minecraft.client.Minecraft;
// Forge deleted RenderLevelStageEvent in 1.21.2, and from 1.21.6 NeoForge's copy is unusable to a
// mod that wants all six stages; those nodes drive the stages from mixin.client.LevelRenderStageMixin
// instead — and so does Fabric, on every version, having never had the event. See ACClientCompat's
// import comment and ACLevelRenderStage.
//? if !fabric && (!forge || <1.21.2) && <1.21.6
import net.minecraftforge.client.event.RenderLevelStageEvent;
// Fabric keeps this on every version. The gate is about NeoForge, which folded the tick events
// into per-target ones at 1.20.5; Fabric's TickEvent is this tree's own vendored stub, fired by
// its own bus, so there is nothing there to fold and every listener below takes the else arm.
//? if forge || fabric || <1.20.5
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Citadel's client game-bus listener, split off {@link CitadelClientProxy} for the reason described
 * on {@link CitadelEvents}. Client-only: it is instantiated from
 * {@link CitadelClientProxy#registerEventHandlers()}, which never runs on a dedicated server.
 */
public final class CitadelClientEvents {

    // From 1.21.6 the render-stage listener below is gated out on Forge, leaving this class with a
    // single listener — which is exactly what Forge's eventbus 7 refuses to scan. Same treatment as
    // CitadelEvents: register against ClientTickEvent's own bus and drop the annotation.
    //
    // The middle arm is the other half of CitadelEvents#onServerTick's story: from 1.20.5 NeoForge
    // has a ClientTickEvent of its own with Pre/Post subclasses in place of TickEvent's phase field.
    // …and from 1.21.9 Forge's own split lands, so START becomes ClientTickEvent.Pre and its bus.
    //? if forge && >=1.21.9 {
    /*public static void register() {
        TickEvent.ClientTickEvent.Pre.BUS.addListener(event -> tickClient());
    }
    *///?} elif forge && >=1.21.6 {
    /*public static void register() {
        TickEvent.ClientTickEvent.BUS.addListener(event -> {
            if (event.phase == TickEvent.Phase.START) {
                tickClient();
            }
        });
    }
    *///?} elif neoforge && >=1.20.5 {
    /*@SubscribeEvent
    public void clientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
        tickClient();
    }
    *///?} else {
    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            tickClient();
        }
    }
    //?}

    private static void tickClient() {
        if (!Citadel.PROXY.isGamePaused() && Minecraft.getInstance().isRunning()
                && Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            ClientTickRateTracker.getForClient(Minecraft.getInstance()).masterTick();
        }
    }

    /**
     * Adapts the loader's render-stage event onto {@link com.github.alexmodguy.alexscaves.client.ACLevelRenderStage},
     * which is what {@link WorldEventContext#renderStage} speaks. Absent where the loader has no
     * such event — mixin.client.LevelRenderStageMixin dispatches there instead.
     */
    //? if !fabric && (!forge || <1.21.2) && <1.21.6 {
    @SubscribeEvent
    public void renderWorldLastEvent(RenderLevelStageEvent event) {
        if (Pathfinding.isDebug()) {
            com.github.alexmodguy.alexscaves.client.ACLevelRenderStage stage = com.github.alexmodguy.alexscaves.client.ACClientCompat.stageOf(event);
            if (stage != null) {
                WorldEventContext.INSTANCE.renderStage(stage, com.github.alexmodguy.alexscaves.client.ACClientCompat.poseStack(event), com.github.alexmodguy.alexscaves.client.ACClientCompat.partialTick(event));
            }
        }
    }
    //?}
}
