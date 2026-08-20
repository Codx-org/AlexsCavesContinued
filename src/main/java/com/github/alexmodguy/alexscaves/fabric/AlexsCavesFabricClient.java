package com.github.alexmodguy.alexscaves.fabric;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.fabric.forge.client.event.EntityRenderersEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.client.event.RegisterColorHandlersEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.client.event.RegisterKeyMappingsEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.client.event.RegisterParticleProvidersEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteProvider;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fabric client entrypoint, named by {@code mod.fabric.entrypoint_client}.
 *
 * <p>Fabric runs this only on the client dist, which is this loader's replacement for the
 * {@code CommonProxy}/{@code ClientProxy} split the mod uses on Forge — so the client half of
 * registration (renderers, model layers, particle providers, colour handlers, key binds) lands
 * here rather than behind a {@code DistExecutor}-style indirection.
 *
 * <p>⚠️ This runs <b>inside {@code Minecraft.<init>}</b>, before the game's own client bootstrap
 * has finished. Anything that reads {@code Minecraft.getInstance()} state — the level, the
 * options, the render buffers — must be deferred rather than done here. Nothing below does: every
 * phase posted here is a registration phase, and the two that genuinely need a built game — the
 * feature layers and the baked model map — are posted from mixins on the objects that build them.
 *
 * <p>{@link AlexsCavesFabric#onInitialize()} has already run by this point: Fabric orders main
 * entrypoints ahead of client ones, so every registry the client half names is filled — including
 * {@code ClientProxy.commonInit()}, which is where the four registration listeners posted below
 * were added to {@link ModBus}.
 *
 * <p><b>Why the mod bus rather than direct calls.</b> Each phase below could equally well call the
 * Fabric API directly and skip the event, but then the eight {@code ClientProxy} handlers would
 * need a Fabric arm apiece. Posting instead keeps those handlers byte-for-byte the Forge ones and
 * confines the whole translation to the sinks in this file.
 */
public class AlexsCavesFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AlexsCaves.LOGGER.info("Alex's Caves Continued: Fabric client init");

        // The clientbound half of the id table the common entrypoint built the serverbound half of.
        // Registered here rather than beside it because a dedicated server must never resolve the
        // client handlers; see ACNetwork.
        com.github.alexmodguy.alexscaves.server.message.ACNetwork.registerClientReceiver();

        // The client game bus. Both tick phases; see ACClientGameEvents.
        com.github.alexmodguy.alexscaves.fabric.event.ACClientGameEvents.register();

        // Phase two of the six, and the one that has to come first here: ClientProxy.clientInit()
        // is what adds the AddLayers, ModifyBakingResult and RegisterShaders listeners, so nothing
        // below would have a listener if this were posted later.
        ModBus.INSTANCE.post(new com.github.alexmodguy.alexscaves.fabric.forge.fml.event.lifecycle.FMLClientSetupEvent());

        registerLayerDefinitions();
        registerParticleProviders();
        ModBus.INSTANCE.post(new RegisterKeyMappingsEvent(KeyBindingHelper::registerKeyBinding));
        registerColorHandlers();
        registerTooltipComponents();

        // The ISTER path. Below 1.21.4 an item's 3D renderer arrives through its client-extension
        // object, which nothing on this loader asks for; this walks the item registry, asks, and
        // forwards what comes back to Fabric API's builtin-renderer registry. Ungated because the
        // method is simply empty from 1.21.4, where a model definition names the renderer instead.
        com.github.alexmodguy.alexscaves.fabric.client.ACFabricItemRenderers.register();
    }

    /**
     * The mod's model layers.
     *
     * <p>Forge collects these into its own map and hands that to the {@code EntityModelSet} when it
     * is built; Fabric registers them one at a time into an equivalent map of its own. Same
     * lifetime either way — a layer definition is baked once per resource reload — so the event is
     * posted once, here, and its contents forwarded.
     */
    private static void registerLayerDefinitions() {
        Map<ModelLayerLocation, Supplier<LayerDefinition>> definitions = new LinkedHashMap<>();
        ModBus.INSTANCE.post(new EntityRenderersEvent.RegisterLayerDefinitions(definitions));
        definitions.forEach((layer, definition) -> EntityModelLayerRegistry.registerModelLayer(layer, definition::get));
    }

    /**
     * The seventeen particle factories.
     *
     * <p>Both sinks are anonymous classes rather than lambdas: the sink methods are generic, which
     * is what ties a particle type to its provider at each of the seventeen call sites, and a
     * lambda cannot declare a type variable.
     *
     * <p>The sprite-set sink needs one adapter. Fabric hands a factory its sprites as a {@code
     * FabricSpriteProvider}, which extends vanilla's {@code SpriteSet} — so the mod's own
     * registrations, which are written against the vanilla type Forge passes, take it unchanged.
     */
    private static void registerParticleProviders() {
        ParticleFactoryRegistry particles = ParticleFactoryRegistry.getInstance();
        ModBus.INSTANCE.post(new RegisterParticleProvidersEvent(
                new RegisterParticleProvidersEvent.SpecialSink() {
                    @Override
                    public <T extends ParticleOptions> void accept(ParticleType<T> type, ParticleProvider<T> provider) {
                        particles.register(type, provider);
                    }
                },
                new RegisterParticleProvidersEvent.SpriteSetSink() {
                    @Override
                    public <T extends ParticleOptions> void accept(ParticleType<T> type, ParticleEngine.SpriteParticleRegistration<T> registration) {
                        particles.register(type, (FabricSpriteProvider sprites) -> registration.create(sprites));
                    }
                }));
    }

    /**
     * The block and item tints.
     *
     * <p>The item half exists only below 1.21.4 — from there an item's tint is declared in its own
     * model definition and this mod's five dynamic colours go through a tint source instead — so
     * the post is gated exactly like the event class it names.
     */
    private static void registerColorHandlers() {
        ModBus.INSTANCE.post(new RegisterColorHandlersEvent.Block(ColorProviderRegistry.BLOCK::register));
        //? if <1.21.4
        ModBus.INSTANCE.post(new RegisterColorHandlersEvent.Item(ColorProviderRegistry.ITEM::register));
    }

    /**
     * The one server-side tooltip component this mod ships, and how the client draws it.
     *
     * <p>Forge keeps a class-keyed map and looks a component up in it by its exact class; Fabric
     * fires a callback per component and takes the first non-null answer. Collecting the
     * registrations into a map of our own and answering out of it preserves Forge's semantics
     * exactly, including "not ours, ask the next listener" for everything else.
     */
    private static void registerTooltipComponents() {
        Map<Class<?>, Function<TooltipComponent, ClientTooltipComponent>> factories = new HashMap<>();
        ModBus.INSTANCE.post(new RegisterClientTooltipComponentFactoriesEvent(
                new RegisterClientTooltipComponentFactoriesEvent.Sink() {
                    @Override
                    public <T extends TooltipComponent> void accept(Class<T> type, Function<? super T, ? extends ClientTooltipComponent> factory) {
                        factories.put(type, component -> factory.apply(type.cast(component)));
                    }
                }));
        if (!factories.isEmpty()) {
            TooltipComponentCallback.EVENT.register(data -> {
                Function<TooltipComponent, ClientTooltipComponent> factory = factories.get(data.getClass());
                return factory == null ? null : factory.apply(data);
            });
        }
    }
}
