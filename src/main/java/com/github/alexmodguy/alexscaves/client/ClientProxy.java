package com.github.alexmodguy.alexscaves.client;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.event.ClientEvents;
import com.github.alexmodguy.alexscaves.client.gui.NuclearFurnaceScreen;
import com.github.alexmodguy.alexscaves.client.gui.SpelunkeryTableScreen;
import com.github.alexmodguy.alexscaves.client.gui.book.CaveBookScreen;
import com.github.alexmodguy.alexscaves.client.model.baked.BakedModelShadeLayerFullbright;
import com.github.alexmodguy.alexscaves.client.particle.*;
import com.github.alexmodguy.alexscaves.client.render.ACInternalShaders;
import com.github.alexmodguy.alexscaves.client.render.blockentity.*;
import com.github.alexmodguy.alexscaves.client.render.entity.*;
import com.github.alexmodguy.alexscaves.client.render.entity.layer.ClientLayerRegistry;
import com.github.alexmodguy.alexscaves.client.render.item.ACArmorRenderProperties;
import com.github.alexmodguy.alexscaves.client.render.item.ACItemPredicates;
import com.github.alexmodguy.alexscaves.client.render.item.ACItemRenderProperties;
import com.github.alexmodguy.alexscaves.client.render.item.tooltip.ClientSackOfSatingTooltip;
import com.github.alexmodguy.alexscaves.client.sound.*;
import com.github.alexmodguy.alexscaves.server.CommonProxy;
import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.github.alexmodguy.alexscaves.server.block.AcidBlock;
import com.github.alexmodguy.alexscaves.server.block.ActivatedByAltar;
import com.github.alexmodguy.alexscaves.server.block.FrostedChocolateBlock;
import com.github.alexmodguy.alexscaves.server.block.blockentity.*;
import com.github.alexmodguy.alexscaves.server.block.fluid.ACFluidRegistry;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.item.QuarrySmasherEntity;
import com.github.alexmodguy.alexscaves.server.entity.item.SubmarineEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.*;
import com.github.alexmodguy.alexscaves.server.inventory.ACMenuRegistry;
import com.github.alexmodguy.alexscaves.server.item.*;
import com.github.alexmodguy.alexscaves.server.item.tooltip.SackOfSatingTooltip;
import com.github.alexmodguy.alexscaves.server.misc.ACKeybindRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import com.github.alexmodguy.alexscaves.citadel.client.shader.PostEffectRegistry;
import com.github.alexmodguy.alexscaves.citadel.client.tick.ClientTickRateTracker;
import com.github.alexmodguy.alexscaves.citadel.server.tick.ServerTickRateTracker;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
//? if <1.21.4
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

public class ClientProxy extends CommonProxy {

    private static final List<String> FULLBRIGHTS = ImmutableList.of("alexscaves:ambersol#", "alexscaves:radrock_uranium_ore#", "alexscaves:acidic_radrock#", "alexscaves:uranium_rod#axis=x", "alexscaves:uranium_rod#axis=y", "alexscaves:uranium_rod#axis=z", "alexscaves:block_of_uranium#", "alexscaves:abyssal_altar#active=true", "alexscaves:abyssmarine_", "alexscaves:peering_coprolith#", "alexscaves:forsaken_idol#", "alexscaves:magnetic_light#", "alexscaves:tremorzilla_egg#");
    public static final ResourceLocation BOMB_FLASH = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "textures/misc/bomb_flash.png");
    public static final ResourceLocation WATCHER_EFFECT = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "textures/misc/watcher_effect.png");
    // A post chain used to be addressed by its file path; from 1.21.2 it is a plain id that
    // ShaderManager.POST_CHAIN_ID_CONVERTER expands back into assets/<ns>/post_effect/<id>.json. The
    // files themselves are moved and rewritten at build time — see DataPackMigration.migrateShadersTo1212.
    //? if >=1.21.2 {
    /*public static final ResourceLocation IRRADIATED_SHADER = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "irradiated");
    public static final ResourceLocation HOLOGRAM_SHADER = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "hologram");
    public static final ResourceLocation PURPLE_WITCH_SHADER = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "purple_witch");
    *///?} else {
    public static final ResourceLocation IRRADIATED_SHADER = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "shaders/post/irradiated.json");
    public static final ResourceLocation HOLOGRAM_SHADER = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "shaders/post/hologram.json");
    public static final ResourceLocation PURPLE_WITCH_SHADER = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "shaders/post/purple_witch.json");
    //?}
    public static final RandomSource random = RandomSource.create();
    public static int lastTremorTick = -1;
    public static float[] randomTremorOffsets = new float[3];
    public static List<UUID> blockedEntityRenders = new ArrayList<>();
    public static Map<ClientLevel, List<BlockPos>> blockedParticleLocations = new HashMap<>();
    public static Map<LivingEntity, Vec3[]> darknessTrailPosMap = new HashMap<>();
    public static Map<LivingEntity, Integer> darknessTrailPointerMap = new HashMap<>();
    public static int muteNonNukeSoundsFor = 0;
    public static int renderNukeFlashFor = 0;
    public static boolean primordialBossActive = false;
    public static float prevPrimordialBossActiveAmount = 0;
    public static float primordialBossActiveAmount = 0;
    public static ClientLevel lastBossLevel;
    public static float prevNukeFlashAmount = 0;
    public static float nukeFlashAmount = 0;
    public static float prevPossessionStrengthAmount = 0;
    public static float possessionStrengthAmount = 0;
    public static int renderNukeSkyDarkFor = 0;
    public static float masterVolumeNukeModifier = 0.0F;
    public static final Int2ObjectMap<AbstractTickableSoundInstance> ENTITY_SOUND_INSTANCE_MAP = new Int2ObjectOpenHashMap<>();
    public static final Map<BlockEntity, AbstractTickableSoundInstance> BLOCK_ENTITY_SOUND_INSTANCE_MAP = new HashMap<>();
    private final ACItemRenderProperties isterProperties = new ACItemRenderProperties();
    private final ACArmorRenderProperties armorProperties = new ACArmorRenderProperties();
    public static boolean spelunkeryTutorialComplete;
    public static boolean hasACSplashText = false;
    public static CameraType lastPOV = CameraType.FIRST_PERSON;
    public static int shaderLoadAttemptCooldown = 0;
    public static Vec3 lastBiomeLightColor = Vec3.ZERO;
    public static float lastBiomeAmbientLightAmount = 0;
    public static Vec3 lastBiomeLightColorPrev = Vec3.ZERO;
    public static float lastBiomeAmbientLightAmountPrev = 0;
    public static Map<UUID, Integer> bossBarRenderTypes = new HashMap<>();
    private static Entity lastCameraEntity;
    public static float acSkyOverrideAmount;
    public static Vec3 acSkyOverrideColor = Vec3.ZERO;
    public static boolean disabledBiomeAmbientLightByOtherMod = false;

    @SuppressWarnings("removal")
    @Override
    public void commonInit() {
        // The 1.21.4 item pipeline reads everything dynamic about an item's look out of JSON, and
        // the three id-mappers it resolves those declarations through are open for mods to add to.
        //
        // ⚠️ This has to run at CONSTRUCT, not from clientInit. clientInit is FMLClientSetupEvent
        // #enqueueWork on Forge and NeoForge, and from 26.1 those enqueued tasks are pumped on the
        // render thread while Minecraft's FIRST resource reload is already running on the workers —
        // so ClientItemInfoLoader parsed the definitions before the mappers had the mod's entries
        // and logged 35 x "Unknown element id: alexscaves:tint|item_renderer|legacy", i.e. every
        // dynamic item silently fell back to the missing-model cube. Fabric never saw it, because
        // there clientInit really is mod-init and does precede the reload.
        //? if !neoforge && >=1.21.4
        /*com.github.alexmodguy.alexscaves.client.render.item.ACItemModelShims.register();*/
        // See AlexsCaves' constructor: NeoForge 1.21 reaches the mod bus through the ModContainer,
        // and Fabric has no loading context to reach one through at all — the mod's single bus is
        // its own singleton, fired from the Fabric client entrypoint.
        //? if fabric
        /*com.github.alexmodguy.alexscaves.fabric.ModBus bus = com.github.alexmodguy.alexscaves.fabric.ModBus.INSTANCE;*/
        //? if neoforge && >=1.21
        /*IEventBus bus = net.neoforged.fml.ModLoadingContext.get().getActiveContainer().getEventBus();*/
        //? if (!neoforge || <1.21) && !fabric
        IEventBus bus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        // NeoForge is the one loader on which the reflective register() above cannot run at
        // CONSTRUCT. It calls ClientBootstrap.bootstrap() from Minecraft.<init>, i.e. AFTER the mods
        // are loaded, where vanilla — so Fabric and Forge too — calls it from Main.main before any
        // mod exists. Reading SpecialModelRenderers' id-mapper by reflection forces that class's
        // <clinit>, which builds a BedSpecialRenderer.Unbaked and so forces Sheets.<clinit>; here
        // that lands inside mod construction and NeoForge logs "Sheets loaded too early". It only
        // logs, but Sheets.SIGN_MATERIALS is built in that <clinit> and getSignMaterial is a bare
        // Map.get with no fallback, so this mod's pewen and thornwood signs would resolve to a null
        // Material. NeoForge fires a mod-bus event for each of the three mappers at the right
        // moment, so it registers through those instead and never touches the class early.
        //? if neoforge && >=1.21.4
        /*com.github.alexmodguy.alexscaves.client.render.item.ACItemModelShims.registerNeoForge(bus);*/
        // 1.21.4 deleted RegisterColorHandlersEvent.Item on both loaders: an item's tint is declared
        // in its item model definition now, as a list of ItemTintSources. The five tints this mod
        // draws are dynamic, so they move to a mod-owned tint source rather than being lost — see
        // ACItemModelShims, registered from the top of this method. It is listed on its own here so the
        // EventBus 7 block below stays flat — a //? inside a //? arm is not legal.
        // Fabric's bus never infers a listener's event type from the method reference — both loaders'
        // buses read it off the handler's parameter reflectively, which needs a class the mod bus
        // does not have on this loader, so every Fabric arm names the event itself. That is the one
        // shape difference between the arms below; the handlers are shared verbatim.
        //? if fabric && <1.21.4
        /*bus.addListener(RegisterColorHandlersEvent.Item.class, this::onItemColors);*/
        //? if !fabric && <1.21.4
        bus.addListener(this::onItemColors);
        // Forge 56 (1.21.6) put the mod bus behind BusGroup: a listener is added through the event
        // type's own getBus(BusGroup) rather than to the bus object. See AlexsCaves' constructor.
        //? if forge && >=1.21.6 {
        /*net.minecraftforge.client.event.RegisterParticleProvidersEvent.getBus(bus).addListener(this::setupParticles);
        net.minecraftforge.client.event.RegisterKeyMappingsEvent.getBus(bus).addListener(this::registerKeybinds);
        net.minecraftforge.client.event.RegisterColorHandlersEvent.Block.getBus(bus).addListener(this::onBlockColors);
        net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent.getBus(bus).addListener(this::onRegisterTooltips);
        *///?} elif fabric {
        /*bus.addListener(RegisterParticleProvidersEvent.class, this::setupParticles);
        bus.addListener(RegisterKeyMappingsEvent.class, this::registerKeybinds);
        bus.addListener(RegisterColorHandlersEvent.Block.class, this::onBlockColors);
        bus.addListener(RegisterClientTooltipComponentFactoriesEvent.class, this::onRegisterTooltips);
        *///?} else {
        bus.addListener(this::setupParticles);
        bus.addListener(this::registerKeybinds);
        bus.addListener(this::onBlockColors);
        bus.addListener(this::onRegisterTooltips);
        //?}
        // NeoForge made MenuScreens.register private in 1.20.5 and gave the job its own mod-bus
        // event, which fires after this constructor. Registered here rather than in clientInit
        // because that already runs during client setup, by which point the event has passed.
        //? if neoforge && >=1.20.5
        /*bus.addListener(this::registerMenuScreens);*/
        // Likewise for client extensions, which 1.21.2 turned from a method on the item into a
        // registration event. Registered from here — reached through the proxy, so a dedicated
        // server never resolves the client-only event type — because CONSTRUCT is the only point
        // guaranteed to precede it: ClientHooks fires it while Minecraft itself is being built.
        //? if neoforge && >=1.21.2
        /*bus.addListener(this::registerClientExtensions);*/
        // 1.21.6 records the GUI as render states and rasterises it afterwards, so the cave book —
        // the mod's one 3D screen — has to go through a picture-in-picture renderer. NeoForge has a
        // registration event for those; Forge does not patch GuiRenderer at all and takes
        // mixin.client.GuiRendererMixin instead. Same reasoning as the two listeners above for why
        // it is added from here: the event fires while Minecraft is being built.
        //? if neoforge && >=1.21.6
        /*bus.addListener(this::registerPictureInPictureRenderers);*/
    }

    //? if neoforge && >=1.21.6 {
    /*public void registerPictureInPictureRenderers(net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent event) {
        event.register(com.github.alexmodguy.alexscaves.client.gui.book.CaveBookRenderState.class,
                com.github.alexmodguy.alexscaves.client.gui.book.CaveBookPipRenderer::new);
    }
    *///?}

    // What the loader used to do for itself: ask every item and fluid type of ours for its client
    // extension. The nineteen initializeClient bodies are unchanged and still @Override — they
    // implement ACClientExtensionItem now rather than IItemExtension. See that interface.
    //? if neoforge && >=1.21.2 {
    /*public void registerClientExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item instanceof ACClientExtensionItem extensionItem) {
                extensionItem.initializeClient(extensions -> event.registerItem(extensions, item));
            }
        }
        for (var supplier : java.util.List.of(ACFluidRegistry.ACID_FLUID_TYPE, ACFluidRegistry.PURPLE_SODA_FLUID_TYPE)) {
            var fluidType = supplier.get();
            if (fluidType instanceof com.github.alexmodguy.alexscaves.server.block.fluid.ACClientExtensionFluidType extensionFluid) {
                extensionFluid.initializeClient(extensions -> event.registerFluidType(extensions, fluidType));
            }
        }
    }
    *///?}

    //? if neoforge && >=1.20.5 {
    /*public void registerMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(ACMenuRegistry.SPELUNKERY_TABLE_MENU.get(), SpelunkeryTableScreen::new);
        event.register(ACMenuRegistry.NUCLEAR_FURNACE_MENU.get(), NuclearFurnaceScreen::new);
    }
    *///?}

    @SuppressWarnings("removal")
    @Override
    public void clientInit() {
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
        // See AlexsCaves' constructor: NeoForge 1.21 reaches the mod bus through the ModContainer,
        // and Fabric has no loading context to reach one through at all — the mod's single bus is
        // its own singleton, fired from the Fabric client entrypoint.
        //? if fabric
        /*com.github.alexmodguy.alexscaves.fabric.ModBus bus = com.github.alexmodguy.alexscaves.fabric.ModBus.INSTANCE;*/
        //? if neoforge && >=1.21
        /*IEventBus bus = net.neoforged.fml.ModLoadingContext.get().getActiveContainer().getEventBus();*/
        //? if (!neoforge || <1.21) && !fabric
        IEventBus bus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        //? if forge && >=1.21.6
        /*net.minecraftforge.client.event.EntityRenderersEvent.AddLayers.getBus(bus).addListener(ClientLayerRegistry::addLayers);*/
        //? if fabric
        /*bus.addListener(EntityRenderersEvent.AddLayers.class, ClientLayerRegistry::addLayers);*/
        //? if (!forge || <1.21.6) && !fabric
        bus.addListener(ClientLayerRegistry::addLayers);
        // See BakedModelShadeLayerFullbright: the post-bake model map is gone from 1.21.4.
        //? if fabric && <1.21.4
        /*bus.addListener(ModelEvent.ModifyBakingResult.class, this::bakeModels);*/
        //? if !fabric && <1.21.4
        bus.addListener(this::bakeModels);
        // Forge deleted RegisterShadersEvent in 1.21.2 along with the rest of the pre-frame-graph
        // render API. Nothing replaces it and nothing needs to: ShaderManager compiles a program
        // the first time getProgram() asks for it and caches it across resource reloads, so the
        // declarations in ACInternalShaders are enough on their own and registration only ever
        // bought eager preloading. NeoForge kept the event until 1.21.2 turned a shader declaration
        // into a whole RenderPipeline and the event became RegisterRenderPipelinesEvent — which
        // Forge does not have, so from 1.21.5 neither loader registers anything and both rely on
        // the same lazy compile. See ACInternalShaders.
        //
        // Fabric stops one version earlier than NeoForge, at 1.21.2, for the same reason Forge does:
        // its dispatcher (mixin.fabric.client.GameRendererShaderMixin) hooks the pre-1.21.2
        // reloadShaders list, and above that band the lazy compile in the paragraph above is the
        // whole story. Narrowing the gate rather than leaving the listener registered is what keeps
        // "a listener exists" and "something fires it" the same statement on this loader.
        //? if fabric && <1.21.2
        /*bus.addListener(RegisterShadersEvent.class, this::registerShaders);*/
        //? if !fabric && (<1.21.2 || (!forge && <1.21.5))
        bus.addListener(this::registerShaders);
        EntityRenderers.register(ACEntityRegistry.BOAT.get(), (context) -> {
            return new AlexsCavesBoatRenderer(context, false);
        });
        EntityRenderers.register(ACEntityRegistry.CHEST_BOAT.get(), (context) -> {
            return new AlexsCavesBoatRenderer(context, true);
        });
        BlockEntityRenderers.register(ACBlockEntityRegistry.MAGNET.get(), MagnetBlockRenderer::new);
        BlockEntityRenderers.register(ACBlockEntityRegistry.TESLA_BULB.get(), TelsaBulbBlockRenderer::new);
        BlockEntityRenderers.register(ACBlockEntityRegistry.HOLOGRAM_PROJECTOR.get(), HologramProjectorBlockRenderer::new);
        BlockEntityRenderers.register(ACBlockEntityRegistry.QUARRY.get(), QuarryBlockRenderer::new);
        BlockEntityRenderers.register(ACBlockEntityRegistry.AMBERSOL.get(), AmbersolBlockRenderer::new);
        BlockEntityRenderers.register(ACBlockEntityRegistry.AMBER_MONOLITH.get(), AmberMonolithBlockRenderer::new);
        BlockEntityRenderers.register(ACBlockEntityRegistry.NUCLEAR_FURNACE.get(), NuclearFurnaceBlockRenderer::new);
        BlockEntityRenderers.register(ACBlockEntityRegistry.SIREN_LIGHT.get(), SirenLightBlockRenderer::new);
        BlockEntityRenderers.register(ACBlockEntityRegistry.ABYSSAL_ALTAR.get(), AbyssalAltarBlockRenderer::new);
        BlockEntityRenderers.register(ACBlockEntityRegistry.COPPER_VALVE.get(), CopperValveBlockRenderer::new);
        BlockEntityRenderers.register(ACBlockEntityRegistry.BEHOLDER.get(), BeholderBlockRenderer::new);
        BlockEntityRenderers.register(ACBlockEntityRegistry.GOBTHUMPER.get(), GobthumperBlockRenderer::new);
        BlockEntityRenderers.register(ACBlockEntityRegistry.CONVERSION_CRUCIBLE.get(), ConversionCrucibleBlockRenderer::new);
        EntityRenderers.register(ACEntityRegistry.MOVING_METAL_BLOCK.get(), MovingMetalBlockRenderer::new);
        EntityRenderers.register(ACEntityRegistry.TELETOR.get(), TeletorRenderer::new);
        EntityRenderers.register(ACEntityRegistry.MAGNETIC_WEAPON.get(), MagneticWeaponRenderer::new);
        EntityRenderers.register(ACEntityRegistry.MAGNETRON.get(), MagnetronRenderer::new);
        EntityRenderers.register(ACEntityRegistry.BOUNDROID.get(), BoundroidRenderer::new);
        EntityRenderers.register(ACEntityRegistry.BOUNDROID_WINCH.get(), BoundroidWinchRenderer::new);
        EntityRenderers.register(ACEntityRegistry.FERROUSLIME.get(), FerrouslimeRenderer::new);
        EntityRenderers.register(ACEntityRegistry.NOTOR.get(), NotorRenderer::new);
        EntityRenderers.register(ACEntityRegistry.QUARRY_SMASHER.get(), QuarrySmasherRenderer::new);
        EntityRenderers.register(ACEntityRegistry.SEEKING_ARROW.get(), SeekingArrowRenderer::new);
        EntityRenderers.register(ACEntityRegistry.SUBTERRANODON.get(), SubterranodonRenderer::new);
        EntityRenderers.register(ACEntityRegistry.VALLUMRAPTOR.get(), VallumraptorRenderer::new);
        EntityRenderers.register(ACEntityRegistry.GROTTOCERATOPS.get(), GrottoceratopsRenderer::new);
        EntityRenderers.register(ACEntityRegistry.TRILOCARIS.get(), TrilocarisRenderer::new);
        EntityRenderers.register(ACEntityRegistry.TREMORSAURUS.get(), TremorsaurusRenderer::new);
        EntityRenderers.register(ACEntityRegistry.RELICHEIRUS.get(), RelicheirusRenderer::new);
        EntityRenderers.register(ACEntityRegistry.FALLING_TREE_BLOCK.get(), FallingTreeBlockRenderer::new);
        EntityRenderers.register(ACEntityRegistry.CRUSHED_BLOCK.get(), CrushedBlockRenderer::new);
        EntityRenderers.register(ACEntityRegistry.LIMESTONE_SPEAR.get(), LimestoneSpearRenderer::new);
        EntityRenderers.register(ACEntityRegistry.EXTINCTION_SPEAR.get(), ExtinctionSpearRenderer::new);
        EntityRenderers.register(ACEntityRegistry.DINOSAUR_SPIRIT.get(), DinosaurSpiritRenderer::new);
        EntityRenderers.register(ACEntityRegistry.LUXTRUCTOSAURUS.get(), LuxtructosaurusRenderer::new);
        EntityRenderers.register(ACEntityRegistry.TEPHRA.get(), TephraRenderer::new);
        EntityRenderers.register(ACEntityRegistry.ATLATITAN.get(), AtlatitanRenderer::new);
        EntityRenderers.register(ACEntityRegistry.NUCLEAR_EXPLOSION.get(), EmptyRenderer::new);
        EntityRenderers.register(ACEntityRegistry.NUCLEAR_BOMB.get(), NuclearBombRenderer::new);
        EntityRenderers.register(ACEntityRegistry.NUCLEEPER.get(), NucleeperRenderer::new);
        EntityRenderers.register(ACEntityRegistry.RADGILL.get(), RadgillRenderer::new);
        EntityRenderers.register(ACEntityRegistry.BRAINIAC.get(), BrainiacRenderer::new);
        EntityRenderers.register(ACEntityRegistry.THROWN_WASTE_DRUM.get(), ThrownWasteDrumEntityRenderer::new);
        EntityRenderers.register(ACEntityRegistry.GAMMAROACH.get(), GammaroachRenderer::new);
        EntityRenderers.register(ACEntityRegistry.RAYCAT.get(), RaycatRenderer::new);
        EntityRenderers.register(ACEntityRegistry.CINDER_BRICK.get(), (context) -> {
            return new ThrownItemRenderer<>(context, 1.25F, false);
        });
        EntityRenderers.register(ACEntityRegistry.TREMORZILLA.get(), TremorzillaRenderer::new);
        EntityRenderers.register(ACEntityRegistry.LANTERNFISH.get(), LanternfishRenderer::new);
        EntityRenderers.register(ACEntityRegistry.SEA_PIG.get(), SeaPigRenderer::new);
        EntityRenderers.register(ACEntityRegistry.SUBMARINE.get(), SubmarineRenderer::new);
        EntityRenderers.register(ACEntityRegistry.HULLBREAKER.get(), HullbreakerRenderer::new);
        EntityRenderers.register(ACEntityRegistry.GOSSAMER_WORM.get(), GossamerWormRenderer::new);
        EntityRenderers.register(ACEntityRegistry.TRIPODFISH.get(), TripodfishRenderer::new);
        EntityRenderers.register(ACEntityRegistry.DEEP_ONE.get(), DeepOneRenderer::new);
        EntityRenderers.register(ACEntityRegistry.INK_BOMB.get(), (context) -> {
            return new ThrownItemRenderer<>(context, 1.25F, false);
        });
        EntityRenderers.register(ACEntityRegistry.DEEP_ONE_KNIGHT.get(), DeepOneKnightRenderer::new);
        EntityRenderers.register(ACEntityRegistry.DEEP_ONE_MAGE.get(), DeepOneMageRenderer::new);
        EntityRenderers.register(ACEntityRegistry.WATER_BOLT.get(), WaterBoltRenderer::new);
        EntityRenderers.register(ACEntityRegistry.WAVE.get(), WaveRenderer::new);
        EntityRenderers.register(ACEntityRegistry.MINE_GUARDIAN.get(), MineGuardianRenderer::new);
        EntityRenderers.register(ACEntityRegistry.MINE_GUARDIAN_ANCHOR.get(), MineGuardianAnchorRenderer::new);
        EntityRenderers.register(ACEntityRegistry.DEPTH_CHARGE.get(), (context) -> {
            return new ThrownItemRenderer<>(context, 1.75F, true);
        });
        EntityRenderers.register(ACEntityRegistry.FLOATER.get(), FloaterRenderer::new);
        EntityRenderers.register(ACEntityRegistry.GUANO.get(), (context) -> {
            return new ThrownItemRenderer<>(context, 1.25F, false);
        });
        EntityRenderers.register(ACEntityRegistry.FALLING_GUANO.get(), FallingBlockRenderer::new);
        EntityRenderers.register(ACEntityRegistry.GLOOMOTH.get(), GloomothRenderer::new);
        EntityRenderers.register(ACEntityRegistry.UNDERZEALOT.get(), UnderzealotRenderer::new);
        EntityRenderers.register(ACEntityRegistry.WATCHER.get(), WatcherRenderer::new);
        EntityRenderers.register(ACEntityRegistry.CORRODENT.get(), CorrodentRenderer::new);
        EntityRenderers.register(ACEntityRegistry.VESPER.get(), VesperRenderer::new);
        EntityRenderers.register(ACEntityRegistry.FORSAKEN.get(), ForsakenRenderer::new);
        EntityRenderers.register(ACEntityRegistry.BEHOLDER_EYE.get(), EmptyRenderer::new);
        EntityRenderers.register(ACEntityRegistry.DESOLATE_DAGGER.get(), DesolateDaggerRenderer::new);
        EntityRenderers.register(ACEntityRegistry.BURROWING_ARROW.get(), BurrowingArrowRenderer::new);
        EntityRenderers.register(ACEntityRegistry.DARK_ARROW.get(), DarkArrowRenderer::new);
        EntityRenderers.register(ACEntityRegistry.SWEETISH_FISH.get(), SweetishFishRenderer::new);
        EntityRenderers.register(ACEntityRegistry.CANIAC.get(), CaniacRenderer::new);
        EntityRenderers.register(ACEntityRegistry.GUMBEEPER.get(), GumbeeperRenderer::new);
        EntityRenderers.register(ACEntityRegistry.GUMBALL.get(), GumballRenderer::new);
        EntityRenderers.register(ACEntityRegistry.CANDICORN.get(), CandicornRenderer::new);
        EntityRenderers.register(ACEntityRegistry.GUM_WORM.get(), GumWormRenderer::new);
        EntityRenderers.register(ACEntityRegistry.GUM_WORM_SEGMENT.get(), GumWormSegmentRenderer::new);
        EntityRenderers.register(ACEntityRegistry.CARAMEL_CUBE.get(), CaramelCubeRenderer::new);
        EntityRenderers.register(ACEntityRegistry.MELTED_CARAMEL.get(), MeltedCaramelRenderer::new);
        EntityRenderers.register(ACEntityRegistry.GUMMY_BEAR.get(), GummyBearRenderer::new);
        EntityRenderers.register(ACEntityRegistry.LICOWITCH.get(), LicowitchRenderer::new);
        EntityRenderers.register(ACEntityRegistry.SPINNING_PEPPERMINT.get(), SpinningPeppermintRenderer::new);
        EntityRenderers.register(ACEntityRegistry.SUGAR_STAFF_HEX.get(), SugarStaffHexRenderer::new);
        EntityRenderers.register(ACEntityRegistry.GINGERBREAD_MAN.get(), GingerbreadManRenderer::new);
        EntityRenderers.register(ACEntityRegistry.FALLING_FROSTMINT.get(), FallingBlockRenderer::new);
        EntityRenderers.register(ACEntityRegistry.CANDY_CANE_HOOK.get(), CandyCaneHookRenderer::new);
        EntityRenderers.register(ACEntityRegistry.SODA_BOTTLE_ROCKET.get(), (render) -> {
            return new ThrownItemRenderer<>(render, 1.25F, true);
        });
        EntityRenderers.register(ACEntityRegistry.FROSTMINT_SPEAR.get(), FrostmintSpearRenderer::new);
        EntityRenderers.register(ACEntityRegistry.THROWN_ICE_CREAM_SCOOP.get(), (context) -> {
            return new ThrownItemRenderer<>(context, 1.25F, false);
        });
        // 26.2 deleted Sheets#addWoodType along with the sign material sheet it fed, and there is no
        // successor to call: SignRenderer is gone, and AbstractSignRenderer/StandingSignRenderer/
        // HangingSignRenderer carry no sign Material at all — a sign's body is an ordinary block-state
        // model there and the renderer only draws the text. So the two calls are simply dropped, and
        // nothing is lost by not making them.
        //
        // Fabric drops them on EVERY version, because addWoodType is itself a loader addition and
        // vanilla needs no equivalent: Sheets builds SIGN_MATERIALS in its own <clinit>, from
        // WoodType.values(), and ACBlockRegistry registers both of these from ITS <clinit> — i.e. at
        // mod construction, long before anything on the client touches Sheets. Forge patches the
        // method in only because a Forge mod may register a wood type later than that. (Gates do not
        // nest, so the loader condition widens this one rather than sitting inside it.)
        //? if !fabric && <26.2 {
        Sheets.addWoodType(ACBlockRegistry.PEWEN_WOOD_TYPE);
        Sheets.addWoodType(ACBlockRegistry.THORNWOOD_WOOD_TYPE);
        //?}
        // 1.21.4 deleted ItemProperties along with the `overrides` list its values drove; the same
        // eleven states are a `minecraft:range_dispatch` on the item's model definition from there,
        // reading the very same code through ACItemModelShims. The values themselves live in
        // ACItemPredicates on every version so the two eras cannot drift apart.
        //? if <1.21.4 {
        ItemProperties.register(ACItemRegistry.HOLOCODER.get(), ResourceLocation.withDefaultNamespace("bound"), (stack, level, living, j) -> ACItemPredicates.bound(stack, level, living));
        ItemProperties.register(ACItemRegistry.DINOSAUR_NUGGET.get(), ResourceLocation.withDefaultNamespace("nugget"), (stack, level, living, j) -> ACItemPredicates.nugget(stack, level, living));
        ItemProperties.register(ACItemRegistry.LIMESTONE_SPEAR.get(), ResourceLocation.withDefaultNamespace("throwing"), (stack, level, living, j) -> ACItemPredicates.throwing(stack, level, living));
        ItemProperties.register(ACItemRegistry.EXTINCTION_SPEAR.get(), ResourceLocation.withDefaultNamespace("throwing"), (stack, level, living, j) -> ACItemPredicates.throwing(stack, level, living));
        ItemProperties.register(ACItemRegistry.REMOTE_DETONATOR.get(), ResourceLocation.withDefaultNamespace("active"), (stack, level, living, j) -> ACItemPredicates.active(stack, level, living));
        ItemProperties.register(ACItemRegistry.MAGIC_CONCH.get(), ResourceLocation.withDefaultNamespace("tooting"), (stack, level, living, j) -> ACItemPredicates.tooting(stack, level, living));
        ItemProperties.register(ACItemRegistry.ORTHOLANCE.get(), ResourceLocation.withDefaultNamespace("charging"), (stack, level, living, j) -> ACItemPredicates.charging(stack, level, living));
        ItemProperties.register(ACItemRegistry.TOTEM_OF_POSSESSION.get(), ResourceLocation.withDefaultNamespace("totem"), (stack, level, living, j) -> ACItemPredicates.totem(stack, level, living));
        ItemProperties.register(ACItemRegistry.CANDY_CANE_HOOK.get(), ResourceLocation.withDefaultNamespace("cast"), (stack, level, holder, i) -> ACItemPredicates.cast(stack, level, holder));
        ItemProperties.register(ACItemRegistry.SACK_OF_SATING.get(), ResourceLocation.withDefaultNamespace("open"), (stack, level, living, j) -> ACItemPredicates.open(stack, level, living));
        ItemProperties.register(ACItemRegistry.FROSTMINT_SPEAR.get(), ResourceLocation.withDefaultNamespace("throwing"), (stack, level, living, j) -> ACItemPredicates.throwing(stack, level, living));
        //?}
        blockedParticleLocations.clear();
        PostEffectRegistry.registerEffect(IRRADIATED_SHADER);
        PostEffectRegistry.registerEffect(HOLOGRAM_SHADER);
        PostEffectRegistry.registerEffect(PURPLE_WITCH_SHADER);
        //? if !neoforge || <1.20.5 {
        MenuScreens.register(ACMenuRegistry.SPELUNKERY_TABLE_MENU.get(), SpelunkeryTableScreen::new);
        MenuScreens.register(ACMenuRegistry.NUCLEAR_FURNACE_MENU.get(), NuclearFurnaceScreen::new);
        //?}
        hasACSplashText = random.nextInt(300) == 0;
        // 26 deleted ItemBlockRenderTypes outright. A fluid's two sprites and its chunk layer are
        // one baked FluidModel now, and FluidStateModelSet.bake hardcodes water and lava — anything
        // else falls back to the missing model — so a mod hands its own models to the bake. Both
        // loaders have an event for it and they take opposite ends of the same object: Forge's
        // ModelEvent.BakeFluidModels wants a baked FluidModel and lends its MaterialBaker, while
        // NeoForge's RegisterFluidModelsEvent takes the Unbaked and bakes it itself. The two
        // textures are still the ones each fluid type names for IClientFluidTypeExtensions, which
        // survives on both loaders for the fog and the underwater overlay but no longer feeds the
        // block renderer.
        //
        // Fabric's is the third spelling of the same registration and the closest to NeoForge's: 26
        // took fabric-api's BlockRenderLayerMap away along with vanilla's per-fluid chunk layer, and
        // what replaced it — FluidRenderingRegistry.register(source, flowing, FluidModel.Unbaked) —
        // takes the Unbaked and one call per fluid pair, so this arm differs from the NeoForge one
        // only in argument order. ⚠️ It must sit ABOVE the else arm: arms are evaluated in order and
        // the else is what every Fabric node below 26 takes.
        //? if forge && >=26 {
        /*net.minecraftforge.client.event.ModelEvent.BakeFluidModels.BUS.addListener((net.minecraftforge.client.event.ModelEvent.BakeFluidModels event) -> {
            net.minecraft.client.renderer.block.FluidModel acid = acFluidModel(
                    com.github.alexmodguy.alexscaves.server.block.fluid.AcidFluidType.FLUID_STILL,
                    com.github.alexmodguy.alexscaves.server.block.fluid.AcidFluidType.FLUID_FLOWING).bake(event.materials(), () -> "Acid");
            event.register(ACFluidRegistry.ACID_FLUID_SOURCE.get(), acid);
            event.register(ACFluidRegistry.ACID_FLUID_FLOWING.get(), acid);
            net.minecraft.client.renderer.block.FluidModel soda = acFluidModel(
                    com.github.alexmodguy.alexscaves.server.block.fluid.PurpleSodaFluidType.FLUID_STILL,
                    com.github.alexmodguy.alexscaves.server.block.fluid.PurpleSodaFluidType.FLUID_FLOWING).bake(event.materials(), () -> "Purple Soda");
            event.register(ACFluidRegistry.PURPLE_SODA_FLUID_SOURCE.get(), soda);
            event.register(ACFluidRegistry.PURPLE_SODA_FLUID_FLOWING.get(), soda);
        });
        *///?} elif neoforge && >=26 {
        /*bus.addListener((net.neoforged.neoforge.client.event.RegisterFluidModelsEvent event) -> {
            event.register(acFluidModel(
                    com.github.alexmodguy.alexscaves.server.block.fluid.AcidFluidType.FLUID_STILL,
                    com.github.alexmodguy.alexscaves.server.block.fluid.AcidFluidType.FLUID_FLOWING),
                    ACFluidRegistry.ACID_FLUID_SOURCE.get(), ACFluidRegistry.ACID_FLUID_FLOWING.get());
            event.register(acFluidModel(
                    com.github.alexmodguy.alexscaves.server.block.fluid.PurpleSodaFluidType.FLUID_STILL,
                    com.github.alexmodguy.alexscaves.server.block.fluid.PurpleSodaFluidType.FLUID_FLOWING),
                    ACFluidRegistry.PURPLE_SODA_FLUID_SOURCE.get(), ACFluidRegistry.PURPLE_SODA_FLUID_FLOWING.get());
        });
        *///?} elif fabric && >=26 {
        /*net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry.register(
                ACFluidRegistry.ACID_FLUID_SOURCE.get(), ACFluidRegistry.ACID_FLUID_FLOWING.get(),
                acFluidModel(com.github.alexmodguy.alexscaves.server.block.fluid.AcidFluidType.FLUID_STILL,
                        com.github.alexmodguy.alexscaves.server.block.fluid.AcidFluidType.FLUID_FLOWING));
        net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry.register(
                ACFluidRegistry.PURPLE_SODA_FLUID_SOURCE.get(), ACFluidRegistry.PURPLE_SODA_FLUID_FLOWING.get(),
                acFluidModel(com.github.alexmodguy.alexscaves.server.block.fluid.PurpleSodaFluidType.FLUID_STILL,
                        com.github.alexmodguy.alexscaves.server.block.fluid.PurpleSodaFluidType.FLUID_FLOWING));
        *///?} else {
        ItemBlockRenderTypes.setRenderLayer(ACFluidRegistry.ACID_FLUID_SOURCE.get(), RenderType.cutoutMipped());
        ItemBlockRenderTypes.setRenderLayer(ACFluidRegistry.ACID_FLUID_FLOWING.get(), RenderType.cutoutMipped());
        ItemBlockRenderTypes.setRenderLayer(ACFluidRegistry.PURPLE_SODA_FLUID_SOURCE.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ACFluidRegistry.PURPLE_SODA_FLUID_FLOWING.get(), RenderType.translucent());
        //?}
    }

    // Built the way vanilla builds its own lava model: the still and flowing sprites, no overlay
    // sprite and no tint source, since neither AC fluid overrides getOverlayTexture or getTintColor.
    // The chunk layer is deliberately NOT chosen here — FluidModel.Unbaked#bake derives it from the
    // stitched sprites' transparency — and that reproduces upstream's two choices exactly: acid's
    // texture carries no alpha channel at all, so it bakes to SOLID, which is what vanilla gives
    // lava and is indistinguishable from the cutout layer upstream asked for when there is not one
    // transparent texel; purple soda's is a uniform 75% alpha, so it bakes to TRANSLUCENT.
    // Material#withForceTranslucent is the lever if a future texture ever needs to override that.
    //? if >=26 {
    /*private static net.minecraft.client.renderer.block.FluidModel.Unbaked acFluidModel(ResourceLocation still, ResourceLocation flowing) {
        return new net.minecraft.client.renderer.block.FluidModel.Unbaked(
                new net.minecraft.client.resources.model.sprite.Material(still),
                new net.minecraft.client.resources.model.sprite.Material(flowing), null, null);
    }
    *///?}

    public void setupParticles(RegisterParticleProvidersEvent registry) {
        AlexsCaves.LOGGER.debug("Registered particle factories");
        registry.registerSpecial(ACParticleRegistry.SCARLET_MAGNETIC_ORBIT.get(), new MagneticOrbitParticle.ScarletFactory());
        registry.registerSpecial(ACParticleRegistry.AZURE_MAGNETIC_ORBIT.get(), new MagneticOrbitParticle.AzureFactory());
        registry.registerSpecial(ACParticleRegistry.SCARLET_MAGNETIC_FLOW.get(), new MagneticFlowParticle.ScarletFactory());
        registry.registerSpecial(ACParticleRegistry.AZURE_MAGNETIC_FLOW.get(), new MagneticFlowParticle.AzureFactory());
        registry.registerSpecial(ACParticleRegistry.TESLA_BULB_LIGHTNING.get(), new TeslaBulbLightningParticle.Factory());
        registry.registerSpecial(ACParticleRegistry.MAGNET_LIGHTNING.get(), new MagnetLightningParticle.Factory());
        registry.registerSpriteSet(ACParticleRegistry.GALENA_DEBRIS.get(), GalenaDebrisParticle.Factory::new);
        registry.registerSpecial(ACParticleRegistry.MAGNETIC_CAVES_AMBIENT.get(), new MagneticCavesAmbientParticle.Factory());
        registry.registerSpriteSet(ACParticleRegistry.FERROUSLIME.get(), FerrouslimeParticle.Factory::new);
        registry.registerSpecial(ACParticleRegistry.QUARRY_BORDER_LIGHTING.get(), new QuarryBorderLightningParticle.Factory());
        registry.registerSpecial(ACParticleRegistry.AZURE_SHIELD_LIGHTNING.get(), new ResistorShieldLightningParticle.AzureFactory());
        registry.registerSpecial(ACParticleRegistry.SCARLET_SHIELD_LIGHTNING.get(), new ResistorShieldLightningParticle.ScarletFactory());
        registry.registerSpriteSet(ACParticleRegistry.FLY.get(), FlyParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.WATER_TREMOR.get(), WaterTremorParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.AMBER_MONOLITH.get(), AmberMonolithParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.AMBER_EXPLOSION.get(), SmallExplosionParticle.AmberFactory::new);
        registry.registerSpecial(ACParticleRegistry.DINOSAUR_TRANSFORMATION_AMBER.get(), new DinosaurTransformParticle.AmberFactory());
        registry.registerSpecial(ACParticleRegistry.DINOSAUR_TRANSFORMATION_TECTONIC.get(), new DinosaurTransformParticle.TectonicFactory());
        registry.registerSpecial(ACParticleRegistry.STUN_STAR.get(), new StunStarParticle.Factory());
        registry.registerSpriteSet(ACParticleRegistry.TEPHRA.get(), TephraParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.TEPHRA_SMALL.get(), TephraParticle.SmallFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.TEPHRA_FLAME.get(), TephraParticle.FlameFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.LUXTRUCTOSAURUS_SPIT.get(), LuxtructosaurusSpitParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.LUXTRUCTOSAURUS_ASH.get(), LuxtructosaurusAshParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.HAPPINESS.get(), HappinessParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.ACID_BUBBLE.get(), AcidBubbleParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.BLACK_VENT_SMOKE.get(), VentSmokeParticle.BlackFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.WHITE_VENT_SMOKE.get(), VentSmokeParticle.WhiteFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.GREEN_VENT_SMOKE.get(), VentSmokeParticle.GreenFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.RED_VENT_SMOKE.get(), VentSmokeParticle.RedFactory::new);
        registry.registerSpecial(ACParticleRegistry.MUSHROOM_CLOUD.get(), new MushroomCloudParticle.Factory());
        registry.registerSpriteSet(ACParticleRegistry.MUSHROOM_CLOUD_SMOKE.get(), SmallExplosionParticle.NukeFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.MUSHROOM_CLOUD_EXPLOSION.get(), SmallExplosionParticle.NukeFactory::new);
        registry.registerSpecial(ACParticleRegistry.PROTON.get(), new ProtonParticle.Factory());
        registry.registerSpriteSet(ACParticleRegistry.FALLOUT.get(), FalloutParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.GAMMAROACH.get(), GammaroachParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.HAZMAT_BREATHE.get(), HazmatBreatheParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.BLUE_HAZMAT_BREATHE.get(), HazmatBreatheParticle.BlueFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.RADGILL_SPLASH.get(), RadgillSplashParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.ACID_DROP.get(), AcidDropParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.NUCLEAR_SIREN_SONAR.get(), NuclearSirenSonarParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.RAYGUN_EXPLOSION.get(), SmallExplosionParticle.RaygunFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.BLUE_RAYGUN_EXPLOSION.get(), SmallExplosionParticle.BlueRaygunFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.RAYGUN_BLAST.get(), RaygunBlastParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.TREMORZILLA_EXPLOSION.get(), SmallExplosionParticle.TremorzillaFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.TREMORZILLA_RETRO_EXPLOSION.get(), SmallExplosionParticle.TremorzillaRetroFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.TREMORZILLA_TECTONIC_EXPLOSION.get(), SmallExplosionParticle.TremorzillaTectonicFactory::new);
        registry.registerSpecial(ACParticleRegistry.TREMORZILLA_PROTON.get(), new TremorzillaProtonParticle.Factory());
        registry.registerSpecial(ACParticleRegistry.TREMORZILLA_RETRO_PROTON.get(), new TremorzillaProtonParticle.RetroFactory());
        registry.registerSpecial(ACParticleRegistry.TREMORZILLA_TECTONIC_PROTON.get(), new TremorzillaProtonParticle.TectonicFactory());
        registry.registerSpriteSet(ACParticleRegistry.TREMORZILLA_LIGHTNING.get(), TremorzillaLightningParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.TREMORZILLA_RETRO_LIGHTNING.get(), TremorzillaLightningParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.TREMORZILLA_TECTONIC_LIGHTNING.get(), TremorzillaLightningParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.TREMORZILLA_BLAST.get(), RaygunBlastParticle.TremorzillaFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.TREMORZILLA_STEAM.get(), TremorzillaSteamParticle.Factory::new);
        registry.registerSpecial(ACParticleRegistry.TUBE_WORM.get(), new TubeWormParticle.Factory());
        registry.registerSpriteSet(ACParticleRegistry.DEEP_ONE_MAGIC.get(), DeepOneMagicParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.WATER_FOAM.get(), WaterFoamParticle.Factory::new);
        registry.registerSpecial(ACParticleRegistry.BIG_SPLASH.get(), new BigSplashParticle.Factory());
        registry.registerSpriteSet(ACParticleRegistry.BIG_SPLASH_EFFECT.get(), BigSplashEffectParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.MINE_EXPLOSION.get(), SmallExplosionParticle.MineFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.BIO_POP.get(), BioPopParticle.Factory::new);
        registry.registerSpecial(ACParticleRegistry.WATCHER_APPEARANCE.get(), new WatcherAppearanceParticle.Factory());
        registry.registerSpecial(ACParticleRegistry.VOID_BEING_CLOUD.get(), new VoidBeingCloudParticle.Factory());
        registry.registerSpecial(ACParticleRegistry.VOID_BEING_TENDRIL.get(), new VoidBeingTendrilParticle.Factory());
        registry.registerSpecial(ACParticleRegistry.VOID_BEING_EYE.get(), new VoidBeingEyeParticle.Factory());
        registry.registerSpriteSet(ACParticleRegistry.UNDERZEALOT_MAGIC.get(), UnderzealotMagicParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.UNDERZEALOT_EXPLOSION.get(), SmallExplosionParticle.UnderzealotFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.FALLING_GUANO.get(), FallingGuanoParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.MOTH_DUST.get(), MothDustParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.FORSAKEN_SPIT.get(), ForsakenSpitParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.FORSAKEN_SONAR.get(), ForsakenSonarParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.FORSAKEN_SONAR_LARGE.get(), ForsakenSonarParticle.LargeFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.TOTEM_EXPLOSION.get(), SmallExplosionParticle.TotemFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.ICE_CREAM_DRIP.get(), IceCreamDripParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.ICE_CREAM_SPLASH.get(), IceCreamSplashParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.PURPLE_SODA_BUBBLE.get(), PurpleSodaBubbleParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.PURPLE_SODA_BUBBLE_EMITTER.get(), PurpleSodaBubbleEmitterParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.PURPLE_SODA_FIZZ.get(), PurpleSodaFizzParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.SUNDROP.get(), SundropParticle.Factory::new);
        registry.registerSpecial(ACParticleRegistry.RAINBOW.get(), new RainbowParticle.Factory());
        registry.registerSpecial(ACParticleRegistry.PLAYER_RAINBOW.get(), new PlayerRainbowParticle.Factory());
        registry.registerSpriteSet(ACParticleRegistry.CANDICORN_CHARGE.get(), CandicornChargeParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.BIG_BLOCK_DUST.get(), BigBlockDustParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.CARAMEL_DROP.get(), CaramelDropParticle.Factory::new);
        registry.registerSpecial(ACParticleRegistry.JELLY_BEAN_EAT.get(), new JellyBeanEatParticle.Factory());
        registry.registerSpriteSet(ACParticleRegistry.SLEEP.get(), SleepParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.WITCH_COOKIE.get(), WitchCookieParticle.Factory::new);
        registry.registerSpecial(ACParticleRegistry.PURPLE_WITCH_MAGIC.get(), new PurpleWitchMagicParticle.Factory());
        registry.registerSpriteSet(ACParticleRegistry.PURPLE_WITCH_EXPLOSION.get(), SmallExplosionParticle.PurpleWitchFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.GOBTHUMPER.get(), GobthumperParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.COLORED_DUST.get(), ColoredDustParticle.Factory::new);
        registry.registerSpriteSet(ACParticleRegistry.SMALL_COLORED_DUST.get(), ColoredDustParticle.SmallFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.CONVERSION_CRUCIBLE_EXPLOSION.get(), SmallExplosionParticle.ConversionCrucibleFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.FROSTMINT_EXPLOSION.get(), SmallExplosionParticle.FrostmintFactory::new);
        registry.registerSpriteSet(ACParticleRegistry.SUGAR_FLAKE.get(), SugarFlakeParticle.Factory::new);
    }

    // 1.21.4 deleted the item half of this event: a tint is a `tints` entry on the item's model
    // definition now. The same five colours are computed by ACItemModelShims.Tint there — the
    // colorIn index each lambda guards on became the entry's position in that list. The block half
    // is untouched on every version.
    //? if <1.21.4 {
    public void onItemColors(RegisterColorHandlersEvent.Item event) {
        AlexsCaves.LOGGER.info("loaded in item colorizer");
        event.register((stack, colorIn) -> colorIn != 1 ? -1 : CaveInfoItem.getBiomeColorOf(Minecraft.getInstance().level, stack, false), ACItemRegistry.CAVE_TABLET.get());
        event.register((stack, colorIn) -> colorIn != 1 ? -1 : CaveInfoItem.getBiomeColorOf(Minecraft.getInstance().level, stack, false), ACItemRegistry.CAVE_CODEX.get());
        event.register((stack, colorIn) -> colorIn != 0 ? -1 : GazingPearlItem.getPearlColor(stack), ACItemRegistry.GAZING_PEARL.get());
        event.register((stack, colorIn) -> colorIn != 0 ? -1 : JellyBeanItem.getBeanColor(stack), ACItemRegistry.JELLY_BEAN.get());
        event.register((stack, colorIn) -> colorIn != 1 ? -1 : BiomeTreatItem.getBiomeTreatColorOf(Minecraft.getInstance().level, stack), ACItemRegistry.BIOME_TREAT.get());
    }
    //?}

    // 26 replaced the tint-index callback with a LIST of BlockTintSource, one entry per tint index,
    // so the `colorIn != 0 ? -1 : …` dispatch the old four-argument shape forced is expressed by the
    // list itself: a one-element list is exactly "tint index 0 and nothing else". BlockTintSource
    // then splits the query in two — color(BlockState) for the position-independent case (an item
    // model, a block held in hand) and colorInWorld(…) for the placed one, which is the call that
    // used to carry the BlockPos. calculateFrostingColor already answers a null position with plain
    // white, which is what the old code gave an item too, since no ItemColor was ever registered.
    // Both loaders agree on this signature; they disagree only on which nested event class carries
    // it, which is the `!mc261-colorhandlers-block-nf` rule's job rather than a third arm's.
    //? if >=26 {
    /*public void onBlockColors(RegisterColorHandlersEvent.Block event) {
        AlexsCaves.LOGGER.info("loaded in block colorizer");
        java.util.List<net.minecraft.client.color.block.BlockTintSource> frosting = java.util.List.of(
                new net.minecraft.client.color.block.BlockTintSource() {
                    @Override
                    public int color(net.minecraft.world.level.block.state.BlockState blockState) {
                        return FrostedChocolateBlock.calculateFrostingColor(null);
                    }

                    @Override
                    public int colorInWorld(net.minecraft.world.level.block.state.BlockState blockState, net.minecraft.client.renderer.block.BlockAndTintGetter blockAndTintGetter, net.minecraft.core.BlockPos blockPos) {
                        return FrostedChocolateBlock.calculateFrostingColor(blockPos);
                    }
                });
        event.register(frosting, ACBlockRegistry.BLOCK_OF_FROSTED_CHOCOLATE.get(), ACBlockRegistry.BLOCK_OF_FROSTING.get());
    }
    *///?} else {
    public void onBlockColors(RegisterColorHandlersEvent.Block event) {
        AlexsCaves.LOGGER.info("loaded in block colorizer");
        event.register((blockState, blockAndTintGetter, blockPos, colorIn) -> colorIn != 0 ? -1 : FrostedChocolateBlock.calculateFrostingColor(blockPos), ACBlockRegistry.BLOCK_OF_FROSTED_CHOCOLATE.get());
        event.register((blockState, blockAndTintGetter, blockPos, colorIn) -> colorIn != 0 ? -1 : FrostedChocolateBlock.calculateFrostingColor(blockPos), ACBlockRegistry.BLOCK_OF_FROSTING.get());
    }
    //?}

    private void onRegisterTooltips(RegisterClientTooltipComponentFactoriesEvent registry) {
        registry.register(SackOfSatingTooltip.class, ClientSackOfSatingTooltip::new);
    }

    // Gone from 1.21.4: ModifyBakingResult stopped handing out a mutable model map on both loaders,
    // and NeoForge deleted the wrapper this hangs off. See BakedModelShadeLayerFullbright — the
    // emissiveBlockModels option is inert there. The loop variable is `var` so the whole method can
    // sit in one flat gate: 1.21 keys the result by ModelResourceLocation rather than plain
    // ResourceLocation, but the body matches on the string form, which both spell the same way.
    //? if <1.21.4 {
    private void bakeModels(final ModelEvent.ModifyBakingResult e) {
        if (AlexsCaves.CLIENT_CONFIG.emissiveBlockModels.get()) {
            long time = System.currentTimeMillis();
            for (var id : e.getModels().keySet()) {
                if (FULLBRIGHTS.stream().anyMatch(str -> id.toString().startsWith(str))) {
                    e.getModels().put(id, new BakedModelShadeLayerFullbright(e.getModels().get(id)));
                }
            }
            AlexsCaves.LOGGER.info("Loaded emissive block models in {} ms", System.currentTimeMillis() - time);

        }
    }
    //?}

    /**
     * 1.21.2 turned shader registration into a declaration: the event takes the
     * {@code ShaderProgram} record and the client compiles it, recompiles it on a resource reload
     * and hands it to whichever render type asked for it. There is nothing to construct, nothing to
     * store and no {@code IOException} to catch. See {@link ACInternalShaders}.
     */
    //? if >=1.21.5 || (!neoforge && >=1.21.2) {
    /*// No RegisterShadersEvent on this node; see the listener registration in clientInit().
    *///?} elif >=1.21.2 {
    /*private void registerShaders(final RegisterShadersEvent e) {
        ACInternalShaders.all().forEach(e::registerShader);
        AlexsCaves.LOGGER.info("registered internal shaders");
    }
    *///?} else {
    private void registerShaders(final RegisterShadersEvent e) {
        try {
            e.registerShader(new ShaderInstance(e.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "rendertype_ferrouslime_gel"), DefaultVertexFormat.NEW_ENTITY), ACInternalShaders::setRenderTypeFerrouslimeGelShader);
            e.registerShader(new ShaderInstance(e.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "rendertype_hologram"), DefaultVertexFormat.POSITION_COLOR), ACInternalShaders::setRenderTypeHologramShader);
            e.registerShader(new ShaderInstance(e.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "rendertype_irradiated"), DefaultVertexFormat.POSITION_COLOR_TEX), ACInternalShaders::setRenderTypeIrradiatedShader);
            e.registerShader(new ShaderInstance(e.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "rendertype_blue_irradiated"), DefaultVertexFormat.POSITION_COLOR_TEX), ACInternalShaders::setRenderTypeBlueIrradiatedShader);
            e.registerShader(new ShaderInstance(e.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "rendertype_bubbled"), DefaultVertexFormat.NEW_ENTITY), ACInternalShaders::setRenderTypeBubbledShader);
            e.registerShader(new ShaderInstance(e.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "rendertype_sepia"), DefaultVertexFormat.NEW_ENTITY), ACInternalShaders::setRenderTypeSepiaShader);
            e.registerShader(new ShaderInstance(e.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "rendertype_red_ghost"), DefaultVertexFormat.NEW_ENTITY), ACInternalShaders::setRenderTypeRedGhostShader);
            e.registerShader(new ShaderInstance(e.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "rendertype_purple_witch"), DefaultVertexFormat.NEW_ENTITY), ACInternalShaders::setRenderTypePurpleWitchShader);
            AlexsCaves.LOGGER.info("registered internal shaders");
        } catch (IOException exception) {
            AlexsCaves.LOGGER.error("could not register internal shaders");
            exception.printStackTrace();
        }
    }
    //?}

    private void registerKeybinds(RegisterKeyMappingsEvent e) {
        e.register(ACKeybindRegistry.KEY_SPECIAL_ABILITY);
    }

    public Player getClientSidePlayer() {
        return Minecraft.getInstance().player;
    }

    public void blockRenderingEntity(UUID id) {
        blockedEntityRenders.add(id);
    }

    public void releaseRenderingEntity(UUID id) {
        blockedEntityRenders.remove(id);
    }

    public void setVisualFlag(int flag) {
    }

    public float getNukeFlashAmount(float partialTicks) {
        return prevNukeFlashAmount + (nukeFlashAmount - prevNukeFlashAmount) * partialTicks;
    }

    public float getPrimordialBossActiveAmount(float partialTicks) {
        return prevPrimordialBossActiveAmount + (primordialBossActiveAmount - prevPrimordialBossActiveAmount) * partialTicks;
    }

    public float getPossessionStrengthAmount(float partialTicks) {
        return prevPossessionStrengthAmount + (possessionStrengthAmount - prevPossessionStrengthAmount) * partialTicks;
    }

    public boolean checkIfParticleAt(SimpleParticleType simpleParticleType, BlockPos at) {
        if (!blockedParticleLocations.containsKey(Minecraft.getInstance().level)) {
            blockedParticleLocations.clear();
            blockedParticleLocations.put(Minecraft.getInstance().level, new ArrayList<>());
        }
        List blocked = blockedParticleLocations.get(Minecraft.getInstance().level);
        if (blocked.contains(at)) {
            return false;
        } else {
            blocked.add(new BlockPos(at));
            return true;
        }
    }

    public void removeParticleAt(BlockPos at) {
        if (!blockedParticleLocations.containsKey(Minecraft.getInstance().level)) {
            blockedParticleLocations.clear();
            blockedParticleLocations.put(Minecraft.getInstance().level, new ArrayList<>());
        }
        blockedParticleLocations.get(Minecraft.getInstance().level).remove(at);
    }


    public boolean isKeyDown(int keyType) {
        if (keyType == -1) {
            return Minecraft.getInstance().options.keyLeft.isDown() || Minecraft.getInstance().options.keyRight.isDown() || Minecraft.getInstance().options.keyUp.isDown() || Minecraft.getInstance().options.keyDown.isDown() || Minecraft.getInstance().options.keyJump.isDown();
        }
        if (keyType == 0) {
            return Minecraft.getInstance().options.keyJump.isDown();
        }
        if (keyType == 1) {
            return Minecraft.getInstance().options.keySprint.isDown();
        }
        if (keyType == 2) {
            return ACKeybindRegistry.KEY_SPECIAL_ABILITY.isDown();
        }
        if (keyType == 3) {
            return Minecraft.getInstance().options.keyAttack.isDown();
        }
        if (keyType == 4) {
            return Minecraft.getInstance().options.keyShift.isDown();
        }
        return false;
    }

    @Override
    public Object getISTERProperties() {
        return isterProperties;
    }

    @Override
    public Object getArmorProperties() {
        return armorProperties;
    }

    public float getPartialTicks() {
        return ACClientCompat.partialTick();
    }

    public void setSpelunkeryTutorialComplete(boolean completedTutorial) {
        spelunkeryTutorialComplete = completedTutorial;
    }

    public boolean isSpelunkeryTutorialComplete() {
        return spelunkeryTutorialComplete;
    }

    public void setRenderViewEntity(Player player, Entity entity) {
        if (player == Minecraft.getInstance().player && Minecraft.getInstance().getCameraEntity() == Minecraft.getInstance().player) {
            lastPOV = Minecraft.getInstance().options.getCameraType();
            Minecraft.getInstance().setCameraEntity(entity);
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (lastCameraEntity != Minecraft.getInstance().getCameraEntity()) {
            ACClientCompat.invalidateChunkGeometry();
            lastCameraEntity = Minecraft.getInstance().getCameraEntity();
        }
    }

    public void resetRenderViewEntity(Player player) {
        if (player == Minecraft.getInstance().player) {
            Minecraft.getInstance().level = (ClientLevel) Minecraft.getInstance().player.level();
            Minecraft.getInstance().setCameraEntity(Minecraft.getInstance().player);
            Minecraft.getInstance().options.setCameraType(lastPOV);
        }
        if (lastCameraEntity != Minecraft.getInstance().getCameraEntity()) {
            ACClientCompat.invalidateChunkGeometry();
            lastCameraEntity = Minecraft.getInstance().getCameraEntity();
        }
    }

    @Override
    public void playWorldSound(@Nullable Object soundEmitter, byte type) {
        if (soundEmitter instanceof Entity entity && !entity.level().isClientSide()) {
            return;
        }
        switch (type) {
            case 0:
                if (soundEmitter instanceof NuclearSirenBlockEntity nuclearSiren) {
                    NuclearSirenSound sound;
                    AbstractTickableSoundInstance old = BLOCK_ENTITY_SOUND_INSTANCE_MAP.get(nuclearSiren);
                    if (old == null || !(old instanceof NuclearSirenSound nuclearSirenSound && nuclearSirenSound.isSameBlockEntity(nuclearSiren)) || old.isStopped()) {
                        sound = new NuclearSirenSound(nuclearSiren);
                        BLOCK_ENTITY_SOUND_INSTANCE_MAP.put(nuclearSiren, sound);
                    } else {
                        sound = (NuclearSirenSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
            case 1:
                if (soundEmitter instanceof NucleeperEntity nucleeper) {
                    NucleeperSound sound;
                    AbstractTickableSoundInstance old = ENTITY_SOUND_INSTANCE_MAP.get(nucleeper.getId());
                    if (old == null || !(old instanceof NucleeperSound nucleeperSound && nucleeperSound.isSameEntity(nucleeper))) {
                        sound = new NucleeperSound(nucleeper);
                        ENTITY_SOUND_INSTANCE_MAP.put(nucleeper.getId(), sound);
                    } else {
                        sound = (NucleeperSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
            case 2:
                if (soundEmitter instanceof NotorEntity notor) {
                    NotorHologramSound sound;
                    AbstractTickableSoundInstance old = ENTITY_SOUND_INSTANCE_MAP.get(notor.getId());
                    if (old == null || !(old instanceof NotorHologramSound hologramSound && hologramSound.isSameEntity(notor))) {
                        sound = new NotorHologramSound(notor);
                        ENTITY_SOUND_INSTANCE_MAP.put(notor.getId(), sound);
                    } else {
                        sound = (NotorHologramSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
            case 3:
                if (soundEmitter instanceof HologramProjectorBlockEntity hologramProjector) {
                    HologramProjectorSound sound;
                    AbstractTickableSoundInstance old = BLOCK_ENTITY_SOUND_INSTANCE_MAP.get(hologramProjector);
                    if (old == null || !(old instanceof HologramProjectorSound hologramSound && hologramSound.isSameBlockEntity(hologramProjector)) || old.isStopped()) {
                        sound = new HologramProjectorSound(hologramProjector);
                        BLOCK_ENTITY_SOUND_INSTANCE_MAP.put(hologramProjector, sound);
                    } else {
                        sound = (HologramProjectorSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
            case 4:
                if (soundEmitter instanceof MagnetBlockEntity magnet) {
                    MagnetSound sound;
                    AbstractTickableSoundInstance old = BLOCK_ENTITY_SOUND_INSTANCE_MAP.get(magnet);
                    if (old == null || !(old instanceof MagnetSound magnetSound && magnetSound.isSameBlockEntity(magnet)) || old.isStopped()) {
                        sound = new MagnetSound(magnet);
                        BLOCK_ENTITY_SOUND_INSTANCE_MAP.put(magnet, sound);
                    } else {
                        sound = (MagnetSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
            case 5:
                if (soundEmitter instanceof UnderzealotEntity underzealot) {
                    UnderzealotSound sound;
                    AbstractTickableSoundInstance old = ENTITY_SOUND_INSTANCE_MAP.get(underzealot.getId());
                    if (old == null || !(old instanceof UnderzealotSound underzealotSound && underzealotSound.isSameEntity(underzealot))) {
                        sound = new UnderzealotSound(underzealot);
                        ENTITY_SOUND_INSTANCE_MAP.put(underzealot.getId(), sound);
                    } else {
                        sound = (UnderzealotSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
            case 6:
                if (soundEmitter instanceof CorrodentEntity corrodent) {
                    CorrodentSound sound;
                    AbstractTickableSoundInstance old = ENTITY_SOUND_INSTANCE_MAP.get(corrodent.getId());
                    if (old == null || !(old instanceof CorrodentSound corrodentSound && corrodentSound.isSameEntity(corrodent))) {
                        sound = new CorrodentSound(corrodent);
                        ENTITY_SOUND_INSTANCE_MAP.put(corrodent.getId(), sound);
                    } else {
                        sound = (CorrodentSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
            case 7:
                if (soundEmitter instanceof NuclearFurnaceBlockEntity nuclearFurnace) {
                    NuclearFurnaceSound sound;
                    AbstractTickableSoundInstance old = BLOCK_ENTITY_SOUND_INSTANCE_MAP.get(nuclearFurnace);
                    if (old == null || !(old instanceof NuclearFurnaceSound furnaceSound && furnaceSound.isSameBlockEntity(nuclearFurnace)) || old.isStopped()) {
                        sound = new NuclearFurnaceSound(nuclearFurnace);
                        BLOCK_ENTITY_SOUND_INSTANCE_MAP.put(nuclearFurnace, sound);
                    } else {
                        sound = (NuclearFurnaceSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
            case 8:
                if (soundEmitter instanceof LivingEntity livingEntity) {
                    RaygunSound sound;
                    AbstractTickableSoundInstance old = ENTITY_SOUND_INSTANCE_MAP.get(livingEntity.getId());
                    if (old == null || !(old instanceof RaygunSound raygunSound && raygunSound.isSameEntity(livingEntity))) {
                        sound = new RaygunSound(livingEntity);
                        ENTITY_SOUND_INSTANCE_MAP.put(livingEntity.getId(), sound);
                    } else {
                        sound = (RaygunSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
            case 9:
                if (soundEmitter instanceof LivingEntity livingEntity) {
                    ResistorShieldSound sound;
                    AbstractTickableSoundInstance old = ENTITY_SOUND_INSTANCE_MAP.get(livingEntity.getId());
                    if (old == null || !(old instanceof ResistorShieldSound resistorShieldSound && resistorShieldSound.isSameEntity(livingEntity) && !resistorShieldSound.isAzure())) {
                        sound = new ResistorShieldSound(livingEntity, false);
                        ENTITY_SOUND_INSTANCE_MAP.put(livingEntity.getId(), sound);
                    } else {
                        sound = (ResistorShieldSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
            case 10:
                if (soundEmitter instanceof LivingEntity livingEntity) {
                    ResistorShieldSound sound;
                    AbstractTickableSoundInstance old = ENTITY_SOUND_INSTANCE_MAP.get(livingEntity.getId());
                    if (old == null || !(old instanceof ResistorShieldSound resistorShieldSound && resistorShieldSound.isSameEntity(livingEntity) && resistorShieldSound.isAzure())) {
                        sound = new ResistorShieldSound(livingEntity, true);
                        ENTITY_SOUND_INSTANCE_MAP.put(livingEntity.getId(), sound);
                    } else {
                        sound = (ResistorShieldSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
            case 11:
                if (soundEmitter instanceof LivingEntity livingEntity) {
                    GalenaGauntletSound sound;
                    AbstractTickableSoundInstance old = ENTITY_SOUND_INSTANCE_MAP.get(livingEntity.getId());
                    if (old == null || !(old instanceof GalenaGauntletSound gauntletSound && gauntletSound.isSameEntity(livingEntity))) {
                        sound = new GalenaGauntletSound(livingEntity);
                        ENTITY_SOUND_INSTANCE_MAP.put(livingEntity.getId(), sound);
                    } else {
                        sound = (GalenaGauntletSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
            case 12:
                if (soundEmitter instanceof BoundroidEntity boundroid) {
                    BoundroidSound sound;
                    AbstractTickableSoundInstance old = ENTITY_SOUND_INSTANCE_MAP.get(boundroid.getId());
                    if (old == null || !(old instanceof BoundroidSound boundroidSound && boundroidSound.isSameEntity(boundroid))) {
                        sound = new BoundroidSound(boundroid);
                        ENTITY_SOUND_INSTANCE_MAP.put(boundroid.getId(), sound);
                    } else {
                        sound = (BoundroidSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
            case 13:
                if (soundEmitter instanceof FerrouslimeEntity ferrouslime) {
                    FerrouslimeSound sound;
                    AbstractTickableSoundInstance old = ENTITY_SOUND_INSTANCE_MAP.get(ferrouslime.getId());
                    if (old == null || !(old instanceof FerrouslimeSound ferrouslimeSound && ferrouslimeSound.isSameEntity(ferrouslime))) {
                        sound = new FerrouslimeSound(ferrouslime);
                        ENTITY_SOUND_INSTANCE_MAP.put(ferrouslime.getId(), sound);
                    } else {
                        sound = (FerrouslimeSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
            case 14:
                if (soundEmitter instanceof QuarrySmasherEntity quarrySmasher) {
                    QuarrySmasherSound sound;
                    AbstractTickableSoundInstance old = ENTITY_SOUND_INSTANCE_MAP.get(quarrySmasher.getId());
                    if (old == null || !(old instanceof QuarrySmasherSound quarrySmasherSound && quarrySmasherSound.isSameEntity(quarrySmasher))) {
                        sound = new QuarrySmasherSound(quarrySmasher);
                        ENTITY_SOUND_INSTANCE_MAP.put(quarrySmasher.getId(), sound);
                    } else {
                        sound = (QuarrySmasherSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
            case 15:
                if (soundEmitter instanceof SubmarineEntity submarine) {
                    SubmarineSound sound;
                    AbstractTickableSoundInstance old = ENTITY_SOUND_INSTANCE_MAP.get(submarine.getId());
                    if (old == null || !(old instanceof SubmarineSound submarineSound && submarineSound.isSameEntity(submarine))) {
                        sound = new SubmarineSound(submarine);
                        ENTITY_SOUND_INSTANCE_MAP.put(submarine.getId(), sound);
                    } else {
                        sound = (SubmarineSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
            case 16:
                if (soundEmitter instanceof TremorzillaEntity tremorzilla) {
                    TremorzillaBeamSound sound;
                    AbstractTickableSoundInstance old = ENTITY_SOUND_INSTANCE_MAP.get(tremorzilla.getId());
                    if (old == null || !(old instanceof TremorzillaBeamSound tremorzillaBeamSound && tremorzillaBeamSound.isSameEntity(tremorzilla))) {
                        sound = new TremorzillaBeamSound(tremorzilla);
                        ENTITY_SOUND_INSTANCE_MAP.put(tremorzilla.getId(), sound);
                    } else {
                        sound = (TremorzillaBeamSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
            case 17:
                if (soundEmitter instanceof GumWormEntity gumWorm) {
                    GumWormSound sound;
                    AbstractTickableSoundInstance old = ENTITY_SOUND_INSTANCE_MAP.get(gumWorm.getId());
                    if (old == null || !(old instanceof GumWormSound gumWormSound && gumWormSound.isSameEntity(gumWorm))) {
                        sound = new GumWormSound(gumWorm);
                        ENTITY_SOUND_INSTANCE_MAP.put(gumWorm.getId(), sound);
                    } else {
                        sound = (GumWormSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
            case 18:
                if (soundEmitter instanceof LivingEntity livingEntity) {
                    SugarRushSound sound;
                    AbstractTickableSoundInstance old = ENTITY_SOUND_INSTANCE_MAP.get(livingEntity.getId());
                    if (old == null || !(old instanceof SugarRushSound sugarRushSound && sugarRushSound.isSameEntity(livingEntity))) {
                        sound = new SugarRushSound(livingEntity);
                        ENTITY_SOUND_INSTANCE_MAP.put(livingEntity.getId(), sound);
                    } else {
                        sound = (SugarRushSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
            case 19:
                if (soundEmitter instanceof CandicornEntity candicorn) {
                    CandicornSound sound;
                    AbstractTickableSoundInstance old = ENTITY_SOUND_INSTANCE_MAP.get(candicorn.getId());
                    if (old == null || !(old instanceof CandicornSound candicornSound && candicornSound.isSameEntity(candicorn))) {
                        sound = new CandicornSound(candicorn);
                        ENTITY_SOUND_INSTANCE_MAP.put(candicorn.getId(), sound);
                    } else {
                        sound = (CandicornSound) old;
                    }
                    if (!isSoundPlaying(sound) && sound.canPlaySound()) {
                        Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
                    }
                }
                break;
        }
    }

    private boolean isSoundPlaying(AbstractTickableSoundInstance sound) {
        return Minecraft.getInstance().getSoundManager().soundEngine.queuedTickableSounds.contains(sound) || Minecraft.getInstance().getSoundManager().soundEngine.tickingSounds.contains(sound);
    }

    public void playWorldEvent(int messageId, Level level, BlockPos pos) {
        if (messageId == 0 && AcidBlock.doesBlockCorrode(level.getBlockState(pos))) {
            level.playLocalSound((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, ACSoundRegistry.ACID_CORROSION.get(), SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.4F + 0.8F, false);
        }
        if (messageId == 1 && level.getBlockState(pos).getBlock() instanceof ActivatedByAltar) {
            level.playLocalSound((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, ACSoundRegistry.ABYSSMARINE_GLOW_ON.get(), SoundSource.BLOCKS, 1.5F, level.getRandom().nextFloat() * 0.4F + 0.8F, false);
        }
        if (messageId == 2 && level.getBlockState(pos).getBlock() instanceof ActivatedByAltar) {
            level.playLocalSound((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, ACSoundRegistry.ABYSSMARINE_GLOW_OFF.get(), SoundSource.BLOCKS, 1.5F, level.getRandom().nextFloat() * 0.4F + 0.8F, false);
        }
        if (messageId == 3 && level.getBlockState(pos).is(ACBlockRegistry.DRAIN.get())) {
            level.playLocalSound((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, ACSoundRegistry.DRAIN_START.get(), SoundSource.BLOCKS, 1.5F, level.getRandom().nextFloat() * 0.4F + 0.8F, false);
        }
        if (messageId == 4 && level.getBlockState(pos).is(ACBlockRegistry.DRAIN.get())) {
            level.playLocalSound((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, ACSoundRegistry.DRAIN_STOP.get(), SoundSource.BLOCKS, 1.5F, level.getRandom().nextFloat() * 0.4F + 0.8F, false);
        }
        if (messageId == 5 && level.getBlockState(pos).is(ACBlockRegistry.SPELUNKERY_TABLE.get())) {
            level.playLocalSound((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, ACSoundRegistry.SPELUNKERY_TABLE_FAIL.get(), SoundSource.BLOCKS, 1.5F, level.getRandom().nextFloat() * 0.4F + 0.8F, false);
            BlockParticleOption blockparticleoption = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
            for (int i = 0; i < 8; i++) {
                level.addParticle(blockparticleoption, pos.getX() + level.getRandom().nextFloat(), pos.getY() + 1.0F, pos.getZ() + level.getRandom().nextFloat(), 0, 0, 0);
            }
        }
        if (messageId == 6 && level.getBlockState(pos).is(ACBlockRegistry.ABYSSAL_ALTAR.get()) && level.getBlockEntity(pos) instanceof AbyssalAltarBlockEntity altarBlock) {
            altarBlock.resetSlideAnimation();
        }
        if (messageId == 7) {
            for (int i = 0; i < 8; i++) {
                level.addParticle(ACParticleRegistry.PURPLE_WITCH_EXPLOSION.get(), pos.getX() + level.getRandom().nextFloat(), pos.getY() + level.getRandom().nextFloat(), pos.getZ() + level.getRandom().nextFloat(), 0, 0, 0);
            }
        }
        if (messageId == 8) {
            for (int i = 0; i < 15; i++) {
                float particleX = random.nextFloat() * 8 - 4;
                float particleY = random.nextFloat() * 8 - 4;
                float particleZ = random.nextFloat() * 8 - 4;
                level.addAlwaysVisibleParticle(random.nextInt(5) == 0 ? ACParticleRegistry.FROSTMINT_EXPLOSION.get() : ParticleTypes.SNOWFLAKE, true, pos.getX() + particleX, pos.getY() + particleY, pos.getZ() + particleZ, 0, 0, 0);
            }
        }
        if (messageId == 9) {
            for (int i = 0; i < 30; i++) {
                float particleX = random.nextFloat() * 4 - 2;
                float particleY = random.nextFloat() * 4 - 2;
                float particleZ = random.nextFloat() * 4 - 2;
                level.addAlwaysVisibleParticle(ParticleTypes.SNOWFLAKE, true, pos.getX() + particleX, pos.getY() + particleY, pos.getZ() + particleZ, 0, 0, 0);
            }
        }
    }

    public void clearSoundCacheFor(Entity entity) {
        ENTITY_SOUND_INSTANCE_MAP.remove(entity.getId());
    }

    public void clearSoundCacheFor(BlockEntity entity) {
        BLOCK_ENTITY_SOUND_INSTANCE_MAP.remove(entity);
    }

    public Vec3 getDarknessTrailPosFor(LivingEntity living, int pointer, float partialTick) {
        if (living.isRemoved()) {
            partialTick = 1.0F;
        }
        Vec3[] trailPositions = darknessTrailPosMap.get(living);
        if (trailPositions == null || !darknessTrailPointerMap.containsKey(living)) {
            return living.position();
        }
        int trailPointer = darknessTrailPointerMap.get(living);
        int i = trailPointer - pointer & 63;
        int j = trailPointer - pointer - 1 & 63;
        Vec3 d0 = trailPositions[j];
        Vec3 d1 = trailPositions[i].subtract(d0);
        return d0.add(d1.scale(partialTick));
    }


    public int getPlayerTime() {
        return Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.tickCount;
    }

    public void preScreenRender(float partialTick) {
        float screenEffectIntensity = Minecraft.getInstance().options.screenEffectScale().get().floatValue();
        float watcherPossessionStrength = getPossessionStrengthAmount(partialTick);
        float nukeFlashAmount = getNukeFlashAmount(partialTick);
        if (nukeFlashAmount > 0 && (AlexsCaves.CLIENT_CONFIG.nuclearBombFlash.get())) {
            int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            ACClientCompat.setImmediateTint(1.0F, 1.0F, 1.0F, nukeFlashAmount * screenEffectIntensity);
            BufferBuilder bufferbuilder = ACClientCompat.beginImmediate(ACClientCompat.ImmediateDraw.SCREEN_OVERLAY_QUADS, ClientProxy.BOMB_FLASH);
            bufferbuilder.vertex(0.0F, screenHeight, -90.0F).uv(0.0F, 1.0F).endVertex();
            bufferbuilder.vertex(screenWidth, screenHeight, -90.0F).uv(1.0F, 1.0F).endVertex();
            bufferbuilder.vertex(screenWidth, 0.0F, -90.0F).uv(1.0F, 0.0F).endVertex();
            bufferbuilder.vertex(0.0F, 0.0F, -90.0F).uv(0.0F, 0.0F).endVertex();
            ACClientCompat.drawImmediate(ACClientCompat.ImmediateDraw.SCREEN_OVERLAY_QUADS, bufferbuilder, ClientProxy.BOMB_FLASH);
            ACClientCompat.setImmediateTint(1.0F, 1.0F, 1.0F, 1.0F);
        }
        if (watcherPossessionStrength > 0) {
            int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            ACClientCompat.setImmediateTint(1.0F, 1.0F, 1.0F, watcherPossessionStrength * screenEffectIntensity);
            BufferBuilder bufferbuilder = ACClientCompat.beginImmediate(ACClientCompat.ImmediateDraw.SCREEN_OVERLAY_QUADS, ClientProxy.WATCHER_EFFECT);
            bufferbuilder.vertex(0.0F, screenHeight, -90.0F).uv(0.0F, 1.0F).endVertex();
            bufferbuilder.vertex(screenWidth, screenHeight, -90.0F).uv(1.0F, 1.0F).endVertex();
            bufferbuilder.vertex(screenWidth, 0.0F, -90.0F).uv(1.0F, 0.0F).endVertex();
            bufferbuilder.vertex(0.0F, 0.0F, -90.0F).uv(0.0F, 0.0F).endVertex();
            ACClientCompat.drawImmediate(ACClientCompat.ImmediateDraw.SCREEN_OVERLAY_QUADS, bufferbuilder, ClientProxy.WATCHER_EFFECT);
            ACClientCompat.setImmediateTint(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public boolean isFirstPersonPlayer(Entity entity) {
        return entity.equals(Minecraft.getInstance().getCameraEntity()) && Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }


    public void openBookGUI(ItemStack itemStackIn) {
        Minecraft.getInstance().setScreen(new CaveBookScreen());
    }

    @Override
    public Vec3 getCameraRotation() {
        return Vec3.ZERO;
    }

    public void setPrimordialBossActive(Level level, int id, boolean active) {
        if (level.isClientSide()) {
            primordialBossActive = active;
            if (!active && id != -1) {
                Minecraft.getInstance().getMusicManager().stopPlaying(ACMusics.luxtructosaurusBossMusic());
            }
        } else {
            super.setPrimordialBossActive(level, id, active);
        }
    }

    public boolean isPrimordialBossActive(Level level) {
        return level.isClientSide() ? primordialBossActive : super.isPrimordialBossActive(level);
    }

    public static Vec3 processSkyColor(Vec3 colorIn, float partialTick) {
        float primordialAmount = AlexsCaves.PROXY.getPrimordialBossActiveAmount(partialTick);
        if (primordialAmount > 0.0F) {
            Vec3 targetColor = new Vec3(0.2F, 0.15F, 0.1F);
            colorIn = colorIn.add(targetColor.subtract(colorIn).scale(primordialAmount));
        }
        return colorIn;
    }

    public void removeBossBarRender(UUID bossBar) {
        bossBarRenderTypes.remove(bossBar);
    }

    public void setBossBarRender(UUID bossBar, int renderType) {
        bossBarRenderTypes.put(bossBar, renderType);
    }

    public boolean isTickRateModificationActive(Level level){
        return ClientTickRateTracker.getForClient(Minecraft.getInstance()).getClientTickRate() != 50;
    }

    @Override
    // The camera is pulled into a local rather than chained, and that is load-bearing: 1.21.11
    // de-get-ed Camera#getPosition and 26.2 de-get-ed GameRenderer#getMainCamera, so a single
    // expression would need two replacement rules whose matches overlap — and where two matches
    // overlap only the earlier-STARTING rule applies, which would leave the tail unrewritten. Split
    // across two statements each rule owns a span of its own. Same reason in WorldEventContext.
    public boolean isFarFromCamera(double x, double y, double z) {
        net.minecraft.client.Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        return camera.getPosition().distanceToSqr(x, y, z) >= 256.0D;
    }

    // Only ever reached from MapDecorationMixin, which is out of the build from 1.20.5 on — as is
    // the renderer it calls, since a registered MapDecorationType draws itself. The override stays
    // (CommonProxy still declares it) but its body has nothing left to do.
    public void renderVanillaMapDecoration(MapDecoration mapDecoration, int index){
        //? if <1.20.5
        com.github.alexmodguy.alexscaves.client.render.VanillaMapDecorationRenderer.render(mapDecoration, index + 1);
    }
}

