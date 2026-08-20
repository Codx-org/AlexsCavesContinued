package com.github.alexmodguy.alexscaves.fabric;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import com.github.alexmodguy.alexscaves.fabric.forge.event.server.ServerAboutToStartEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.event.server.ServerStoppingEvent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;

/**
 * Fabric common entrypoint, named by {@code mod.fabric.entrypoint_main} in
 * stonecutter.properties.toml and emitted into fabric.mod.json by the mod-platform plugin.
 *
 * <p>This whole package is excluded from the compile on every non-Fabric node
 * ({@code ModPlatformPlugin.configureJava} excludes {@code **}{@code /alexscaves/fabric/**}),
 * because {@code net.fabricmc.**} is simply absent from a Forge/NeoForge classpath — so nothing
 * in here needs a Stonecutter loader gate, and everything in here may name Fabric API freely.
 *
 * <p><b>What this class is for</b>: ordering the handful of things Fabric has no event for.
 * Forge news up {@link AlexsCaves} from its {@code @Mod} annotation and then feeds it six mod-bus
 * events; Fabric has neither, so the constructor is an ordinary call from here and the four phases
 * that survive the translation are posted explicitly. Two of the six are gone — {@code
 * ModConfigEvent.Loading}/{@code .Reloading} have no counterpart, and the constructor's own fabric
 * arm reads the config once instead — and two more are client-only and posted from
 * {@link AlexsCavesFabricClient}. That ordering is load-bearing on this loader in a way it is not
 * on the other two: the vendored {@code fabric/registries/DeferredRegister} is <b>immediate</b> —
 * its {@code register(...)} resolves the supplier on the spot — so a registry that is flushed too
 * early reads another registry's unbound handles.
 *
 * <p>The one phase order Fabric cannot reproduce is {@code clientSetup} before {@code
 * loadComplete}: every {@code ModInitializer} runs before any {@code ClientModInitializer}, so the
 * client phase is necessarily last here. See the comment on the {@code FMLLoadCompleteEvent} post
 * below for why nothing depends on it.
 */
public class AlexsCavesFabric implements ModInitializer {

    private static volatile MinecraftServer server;

    /**
     * The running server, or {@code null} on a client that has not joined a world.
     *
     * <p>Stands in for Forge's {@code ServerLifecycleHooks.getCurrentServer()}, which is equally
     * nullable — so callers need no extra guarding on this loader.
     *
     * <p>{@code volatile} because the vendored Citadel pathfinding worker threads read it off the
     * server thread that writes it.
     */
    @Nullable
    public static MinecraftServer getServer() {
        return server;
    }

    @Override
    public void onInitialize() {
        AlexsCaves.LOGGER.info("Alex's Caves Continued: Fabric common init");

        // The two attributes Forge adds to every living entity and Fabric does not. Nothing in the
        // mod's own flush order depends on them, so they go first and stand on their own.
        com.github.alexmodguy.alexscaves.fabric.entity.ACFabricAttributes.register();

        // ⚠️ Both halves matter. The field is what ServerLifecycleHooks.getCurrentServer() reads;
        // the two posts are what put this mod's ~20 game-bus handlers on the air at all, since on
        // this loader nothing else ever constructs a Forge lifecycle event. Without the first one
        // CommonEvents#onServerAboutToStart never runs, so ACBiomeRarity is never initialised and
        // BiomeSourceAccessor never gets its key map — and the six cave biomes are then absent from
        // every world the mod generates, silently, with no log line. Fabric's SERVER_STARTING sits
        // exactly where Forge fires ServerAboutToStartEvent: registries loaded and frozen, no level
        // loaded yet, so nothing has asked the biome source a question the table cannot answer.
        ServerLifecycleEvents.SERVER_STARTING.register(starting -> {
            server = starting;
            MinecraftForge.EVENT_BUS.post(new ServerAboutToStartEvent(starting));
            // Forge's VillagerTradingManager rebuilds the trade tables off this same event, so the
            // cabin-map trades are added at exactly the moment they are added on the other two
            // loaders. Gone from 26, where trades are datapack registry entries on every loader.
            //? if <26
            com.github.alexmodguy.alexscaves.fabric.event.ACFabricVillagerTrades.loadTrades();
        });
        // STOPPING, not STOPPED: the handler clears the tick-rate modifiers off a tracker it looks
        // up from the server, so the server has to still be usable when it runs — which is the same
        // guarantee Forge's ServerStoppingEvent gives. The field is nulled a moment later, on
        // STOPPED, so anything running during shutdown still resolves the server.
        ServerLifecycleEvents.SERVER_STOPPING.register(stopping ->
                MinecraftForge.EVENT_BUS.post(new ServerStoppingEvent(stopping)));
        ServerLifecycleEvents.SERVER_STOPPED.register(stopped -> server = null);

        // The rest of the game bus. Everything with a first-class Fabric API callback is posted
        // from ACGameEvents; everything that needs an anchor inside a vanilla method is posted from
        // a mixin under mixin/fabric/. Both fill the same MinecraftForge.EVENT_BUS these two
        // lifecycle posts do.
        com.github.alexmodguy.alexscaves.fabric.event.ACGameEvents.register();

        // Everything Forge's @Mod annotation does, in order. The constructor registers this mod's
        // four Fabric-side mod-bus listeners and then flushes ~28 DeferredRegisters — which on this
        // loader really do register, immediately, in the order the calls appear.
        new AlexsCaves();

        // The two entity mod-bus events ACEntityRegistry declares handlers for. @Mod.EventBusSubscriber
        // is inert on this loader, so nothing would ever deliver them; they are called outright
        // instead. Immediately after the constructor because both keep a map keyed by entity type,
        // and the types are registered in it.
        com.github.alexmodguy.alexscaves.fabric.entity.ACFabricEntityRegistration.register();

        // Phase one. Forge would fire this off the mod bus after every mod's constructor has run;
        // here it runs at the end of this mod's own, which is the same guarantee for everything
        // commonSetup touches — all of it is this mod's own state.
        ModBus.INSTANCE.post(new com.github.alexmodguy.alexscaves.fabric.forge.fml.event.lifecycle.FMLCommonSetupEvent());

        // ...and immediately after it, because commonSetup is where ACNetwork.registerMessages()
        // builds the id table both receivers decode against. The clientbound half is registered
        // from the client entrypoint instead; see AlexsCavesFabricClient.
        com.github.alexmodguy.alexscaves.server.message.ACNetwork.registerServerReceiver();

        // The global-loot-modifier stand-in. Forge drives this from data and so needs no phase at
        // all; here it is a LootTableEvents.ALL_LOADED callback plus a roll-time mixin. It goes
        // after the constructor because its static initialiser builds the two modifier instances,
        // and those read ACBiomeRegistry's keys.
        com.github.alexmodguy.alexscaves.fabric.loot.ACFabricLootModifiers.register();

        // Phase three, inline rather than deferred. Forge fires it last of the six, after every
        // mod's clientSetup, and Fabric cannot reproduce that literally: it runs every
        // ModInitializer before any ClientModInitializer, so clientSetup necessarily comes later
        // here. That is harmless because nothing loadComplete drives reads another mod's
        // REGISTRATIONS — the two "after all mods loaded" hooks only ask CodxLib.isModLoaded, and
        // Fabric's mod list is complete before any entrypoint runs at all, while the fluid
        // interactions name only this mod's own fluids and blocks.
        ModBus.INSTANCE.post(new com.github.alexmodguy.alexscaves.fabric.forge.fml.event.lifecycle.FMLLoadCompleteEvent());
    }
}
