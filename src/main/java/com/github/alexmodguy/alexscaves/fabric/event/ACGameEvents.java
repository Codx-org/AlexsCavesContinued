package com.github.alexmodguy.alexscaves.fabric.event;

import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import com.github.alexmodguy.alexscaves.fabric.forge.event.TickEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.EntityTravelToDimensionEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.player.PlayerEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * The server half of the producer side of {@link com.github.alexmodguy.alexscaves.fabric.event.ACEventBus} —
 * the Fabric API callbacks that turn into loader game-bus events.
 *
 * <p>{@code MinecraftForge.EVENT_BUS} is filled from two directions. Everything that has a natural
 * anchor <em>inside</em> a vanilla method is posted from a mixin under {@code mixin/fabric/}; the
 * handful of things Fabric API already models as a first-class callback are posted from here, which
 * is cheaper and — for the tick pair especially — far more stable across the 22 pinned API builds
 * than mixing into {@code MinecraftServer#tickServer} would be.
 *
 * <p>⚠️ The server tick pair is the highest-value entry in this class and the reason it exists:
 * {@code CommonEvents#serverTick} is the sole driver of {@code ACWorldWorkerManager}, so without it
 * a cave map is handed to the player and its biome search never runs a single step. It budgets work
 * across the two phases, so <b>both</b> must be fired — see {@link TickEvent} for which events want
 * which phases.
 *
 * <p>{@code ServerLifecycleEvents} is deliberately <em>not</em> driven from here: those two posts sit
 * in {@code AlexsCavesFabric#onInitialize} beside the server-instance capture they share a callback
 * with.
 */
public final class ACGameEvents {

    private ACGameEvents() {
    }

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server ->
                MinecraftForge.EVENT_BUS.post(new TickEvent.ServerTickEvent(TickEvent.Phase.START, server)));
        ServerTickEvents.END_SERVER_TICK.register(server ->
                MinecraftForge.EVENT_BUS.post(new TickEvent.ServerTickEvent(TickEvent.Phase.END, server)));

        // getPlayer() rather than the public `player` field: the field's visibility is not stable
        // across the range, and ServerGamePacketListenerImpl has implemented ServerPlayerConnection
        // (which declares the getter) on every version in it.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(handler.getPlayer())));

        registerDimensionChange();
    }

    /**
     * {@code EntityTravelToDimensionEvent}, which {@code CommonEvents#travelToDimension} uses to take a
     * player out of the sugar-rush slow-motion when they leave the dimension it was applied in.
     *
     * <p><b>This is the one producer in the tree that fires AFTER the thing it describes</b>, where
     * Forge's event fires before and is cancellable. Fabric API models only the completed change, and
     * that is behaviourally identical <em>here</em>: {@code SugarRushEffect#leaveSlowMotion} resolves
     * the {@code ServerTickRateTracker} from {@code level.getServer()} — a per-server object, not a
     * per-level one — and then matches the modifier to remove by entity id, so which of the two levels
     * it is handed cannot change the outcome. ⚠️ A future listener that reads {@code getDimension()} as
     * "where I am about to go", cancels the event, or expects the entity to still be in the origin
     * level would <b>not</b> be served correctly by this, and would need a mixin on the vanilla
     * teleport path instead. The event is not cancellable-honoured for that reason.
     *
     * <p>The class was renamed at <b>26.1</b> along with the whole {@code World} → {@code Level} sweep
     * — {@code ServerEntityWorldChangeEvents.AFTER_*_CHANGE_WORLD} on the 18 pinned API builds from
     * 1.20.1 to 1.21.11, {@code ServerEntityLevelChangeEvents.AFTER_*_CHANGE_LEVEL} on the four 26.x
     * ones — and the functional method moved with it ({@code afterChangeWorld} → {@code afterChangeLevel}).
     * Both spellings are fully qualified and the lambdas take implicit parameters, so the arm is two
     * statements wide and needs no gated import. Read out of each pinned {@code fabric-entity-events-v1}
     * jar; do not infer the boundary from a vanilla rename sitting near it.
     */
    private static void registerDimensionChange() {
        //? if >=26.1 {
        /*net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) ->
                MinecraftForge.EVENT_BUS.post(new EntityTravelToDimensionEvent(player, destination.dimension())));
        net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents.AFTER_ENTITY_CHANGE_LEVEL.register((originalEntity, newEntity, origin, destination) ->
                MinecraftForge.EVENT_BUS.post(new EntityTravelToDimensionEvent(newEntity, destination.dimension())));
        *///?} else {
        net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
                MinecraftForge.EVENT_BUS.post(new EntityTravelToDimensionEvent(player, destination.dimension())));
        net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents.AFTER_ENTITY_CHANGE_WORLD.register((originalEntity, newEntity, origin, destination) ->
                MinecraftForge.EVENT_BUS.post(new EntityTravelToDimensionEvent(newEntity, destination.dimension())));
        //?}
    }
}
