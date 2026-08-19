package com.github.alexmodguy.alexscaves;

import codx.codxlib.api.CodxLib;
import codx.codxlib.api.ModInfo;
import codx.codxlib.api.UpdateChecker;
import com.github.alexmodguy.alexscaves.citadel.Citadel;
import com.github.alexmodguy.alexscaves.client.ClientProxy;
import com.github.alexmodguy.alexscaves.client.config.ACClientConfig;
import com.github.alexmodguy.alexscaves.client.model.layered.ACModelLayers;
import com.github.alexmodguy.alexscaves.client.particle.ACParticleRegistry;
import com.github.alexmodguy.alexscaves.server.CommonProxy;
import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.github.alexmodguy.alexscaves.server.block.blockentity.ACBlockEntityRegistry;
import com.github.alexmodguy.alexscaves.server.block.fluid.ACFluidRegistry;
import com.github.alexmodguy.alexscaves.server.block.poi.ACPOIRegistry;
import com.github.alexmodguy.alexscaves.server.config.ACServerConfig;
import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationConfig;
import com.github.alexmodguy.alexscaves.server.enchantment.ACEnchantmentRegistry;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityDataRegistry;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.ACFrogRegistry;
import com.github.alexmodguy.alexscaves.server.event.CommonEvents;
import com.github.alexmodguy.alexscaves.server.inventory.ACMenuRegistry;
import com.github.alexmodguy.alexscaves.server.item.ACItemRegistry;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import com.github.alexmodguy.alexscaves.server.level.carver.ACCarverRegistry;
import com.github.alexmodguy.alexscaves.server.level.feature.ACFeatureRegistry;
import com.github.alexmodguy.alexscaves.server.level.storage.ACWorldData;
import com.github.alexmodguy.alexscaves.server.level.structure.ACStructureRegistry;
import com.github.alexmodguy.alexscaves.server.level.structure.piece.ACStructurePieceRegistry;
import com.github.alexmodguy.alexscaves.server.level.structure.processor.ACStructureProcessorRegistry;
import com.github.alexmodguy.alexscaves.server.level.surface.ACSurfaceRuleConditionRegistry;
import com.github.alexmodguy.alexscaves.server.level.surface.ACSurfaceRules;
import com.github.alexmodguy.alexscaves.server.message.*;
import com.github.alexmodguy.alexscaves.server.misc.*;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import com.github.alexmodguy.alexscaves.server.recipe.ACRecipeRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
// Fabric has no config subsystem to register a spec with, so the three types that describe that
// registration are named only from arms this loader does not take — see the constructor and the
// loadConfig/reloadConfig pair, both of which are already gated !fabric.
//? if !fabric {
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
//?}
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Mod(AlexsCaves.MODID)
public class AlexsCaves {
    public static final String MODID = "alexscaves";
    /**
     * Hardcoded on purpose. Every codx mod ships a mod-metadata.properties with the same name, so
     * reading the slug via getResourceAsStream returns an arbitrary mod's file on one classloader
     * and the update checker links players at the wrong project.
     */
    public static final String MODRINTH_SLUG = "alexs-caves-continued";
    public static final Logger LOGGER = LogUtils.getLogger();
    // NeoForge deleted DistExecutor in 1.21 and Forge deleted it in 62 (26). FMLEnvironment.dist is
    // the supported replacement on both, and it keeps the same property that mattered here: the
    // client branch is a constant-pool entry the JVM only resolves if it is taken, so a dedicated
    // server still never loads ClientProxy.
    //
    // ⚠️ The middle arm is scoped to Forge and must stay that way. Fabric has no FMLEnvironment
    // at any version — what it has is this tree's own stub of the class the else arm names, which
    // !fab-fml-distexecutor rewrites onto the fabric.forge.fml package. A bare `>=26` there sends every
    // Fabric node from 26 up at a Forge class that is not on its classpath.
    //? if neoforge && >=1.21 {
    /*public static CommonProxy PROXY = net.neoforged.fml.loading.FMLEnvironment.dist.isClient() ? new ClientProxy() : new CommonProxy();
    *///?} elif forge && >=26 {
    /*public static CommonProxy PROXY = net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient() ? new ClientProxy() : new CommonProxy();
    *///?} else {
    public static CommonProxy PROXY = net.minecraftforge.fml.DistExecutor.runForDist(() -> ClientProxy::new, () -> CommonProxy::new);
    //?}
    public static final ACServerConfig COMMON_CONFIG;
    private static final ForgeConfigSpec COMMON_CONFIG_SPEC;
    public static final ACClientConfig CLIENT_CONFIG;
    private static final ForgeConfigSpec CLIENT_CONFIG_SPEC;
    public static final List<String> MOD_GENERATION_CONFLICTS = new ArrayList<>();

    static {
        final Pair<ACServerConfig, ForgeConfigSpec> serverPair = new ForgeConfigSpec.Builder().configure(ACServerConfig::new);
        COMMON_CONFIG = serverPair.getLeft();
        COMMON_CONFIG_SPEC = serverPair.getRight();
        final Pair<ACClientConfig, ForgeConfigSpec> clientPair = new ForgeConfigSpec.Builder().configure(ACClientConfig::new);
        CLIENT_CONFIG = clientPair.getLeft();
        CLIENT_CONFIG_SPEC = clientPair.getRight();
    }

    @SuppressWarnings("removal")
    public AlexsCaves() {
        // Config registration and the mod event bus, which every loader answers differently.
        //
        // Fabric owns neither half: there is no config subsystem to hand a spec to, and no mod
        // event bus at all. So the spec loads itself — see the vendored config-spec class for what
        // that reproduces and, more to the point, what it does not (no config-changed event, so
        // BiomeGenerationConfig is read here rather than from loadConfig below) — and the bus
        // becomes a token, which is what keeps the ~28 register(modEventBus) lines further down
        // byte-identical on all three loaders.
        //
        // NeoForge 1.21 emptied ModLoadingContext down to the active ModContainer and deleted
        // FMLJavaModLoadingContext outright; config registration and the mod event bus both hang off
        // that container now. Going through it rather than through the constructor parameters the
        // 1.21 @Mod contract also offers keeps this constructor's signature the same on every node.
        //? if fabric {
        /*COMMON_CONFIG_SPEC.load(codx.codxlib.api.CodxLib.configDir().resolve("alexscaves-general.toml"));
        CLIENT_CONFIG_SPEC.load(codx.codxlib.api.CodxLib.configDir().resolve("alexscaves-client.toml"));
        BiomeGenerationConfig.reloadConfig();
        com.github.alexmodguy.alexscaves.fabric.ModBus modEventBus = com.github.alexmodguy.alexscaves.fabric.ModBus.INSTANCE;
        *///?} elif neoforge && >=1.21 {
        /*ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.COMMON, COMMON_CONFIG_SPEC, "alexscaves-general.toml");
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.CLIENT, CLIENT_CONFIG_SPEC, "alexscaves-client.toml");
        IEventBus modEventBus = ModLoadingContext.get().getActiveContainer().getEventBus();
        *///?} else {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_CONFIG_SPEC, "alexscaves-general.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_CONFIG_SPEC, "alexscaves-client.toml");
        IEventBus modEventBus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        //?}
        // Forge 56 (1.21.6) is the first Forge build on EventBus 7, where the mod bus is a BusGroup
        // rather than an object you can hang listeners on: every mod-bus event carries a static
        // getBus(BusGroup) that resolves the one bus for that event type. Same six listeners, same
        // order, just addressed through the event instead of through the bus.
        //
        // Fabric registers four of the six, and names each event type explicitly. Two of them are
        // simply absent: ModConfigEvent has no Fabric counterpart, and the spec is loaded in the
        // arm above instead, so loadConfig/reloadConfig are gated out entirely. The four that
        // remain are posted by hand from the two entrypoints (AlexsCavesFabric /
        // AlexsCavesFabricClient) in the order the mod bus would have fired them; going through the
        // token bus rather than calling the four methods directly is what lets them stay private
        // and keeps this list the single place that says which phases this mod answers.
        //
        // Registering the two CLIENT phases from common code is safe here and would not be on
        // Forge: Fabric ships one merged jar with no dist stripping and no RuntimeDistCleaner, so
        // resolving a method reference whose parameter type names client classes cannot fail on a
        // dedicated server. Nothing posts them there, so they never run.
        //? if forge && >=1.21.6 {
        /*net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent.getBus(modEventBus).addListener(this::commonSetup);
        net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent.getBus(modEventBus).addListener(this::clientSetup);
        net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent.getBus(modEventBus).addListener(this::loadComplete);
        net.minecraftforge.fml.event.config.ModConfigEvent.Loading.getBus(modEventBus).addListener(this::loadConfig);
        net.minecraftforge.fml.event.config.ModConfigEvent.Reloading.getBus(modEventBus).addListener(this::reloadConfig);
        net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions.getBus(modEventBus).addListener(this::registerLayerDefinitions);
        *///?} elif fabric {
        /*modEventBus.addListener(FMLCommonSetupEvent.class, this::commonSetup);
        modEventBus.addListener(FMLClientSetupEvent.class, this::clientSetup);
        modEventBus.addListener(FMLLoadCompleteEvent.class, this::loadComplete);
        modEventBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions.class, this::registerLayerDefinitions);
        *///?} else {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::loadComplete);
        modEventBus.addListener(this::loadConfig);
        modEventBus.addListener(this::reloadConfig);
        modEventBus.addListener(this::registerLayerDefinitions);
        //?}
        // Forge deleted the per-element HUD render events in 1.20.5; a mod now registers and
        // conditions named layers once, on the mod bus. See addGuiOverlayLayers below.
        //
        // The dist guard is load-bearing, not tidiness. `addListener(Consumer<T>)` resolves T by
        // loading the parameter type, and from Forge 52.x that pulls net.minecraft.client.gui
        // .LayeredDraw in behind AddGuiOverlayLayersEvent — which RuntimeDistCleaner refuses on a
        // dedicated server, killing CONSTRUCT with "Attempted to load class … for invalid dist".
        // 50.2.9 resolved the same event without reaching LayeredDraw, so 1.20.6 booted green and
        // 1.21.1 did not. The method reference itself is safe unloaded: its invokedynamic links
        // only when the branch runs.
        //? if forge && >=1.20.5 && !=1.21 && <1.21.6
        /*if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) modEventBus.addListener(this::addGuiOverlayLayers);*/
        // `neoforge`, not `!forge`: both handlers name net.neoforged types, so this gate was only
        // ever "the loader that is not Forge" because there were two of them.
        //? if neoforge {
        /*modEventBus.addListener(this::registerTicketControllers);
        modEventBus.addListener(this::registerCapabilities);
        *///?}
        // Forge 55.x deleted ForgeChunkManager, so from 1.21.5 the four things that force chunks ride
        // vanilla tickets and the mod has to own the two TicketTypes they use. Fabric is on that same
        // arm from 1.21.5 — it never had a force-loading API, and 1.21.5 is where the vanilla ticket
        // it uses instead became registry content that has to be registered. See ACPlatform.
        //? if !neoforge && >=1.21.5
        /*ACPlatform.registerTicketTypes(modEventBus);*/
        //? if neoforge
        /*modEventBus.addListener(ACNetwork::registerPayloads);*/
        // 1.21.2 turned "is this enchantable / what repairs it" from a method on the item into two
        // data components on its defaults, so the answers ACEnchantableItem and ACRepairableItem
        // still hold have to be pushed onto the items once, after registration. Both loaders deleted
        // the old hooks and each grew its own way back in, on different buses: NeoForge's
        // ModifyDefaultComponentsEvent walks the whole item registry once on the mod bus, Forge's
        // GatherComponentsEvent.Item fires per item on the game bus, lazily, the first time that
        // item's components are asked for. See modifyDefaultComponents / gatherItemComponents below.
        //? if neoforge && >=1.21.2
        /*modEventBus.addListener(this::modifyDefaultComponents);*/
        //? if forge && >=1.21.2 && <1.21.6
        /*MinecraftForge.EVENT_BUS.addListener(AlexsCaves::gatherItemComponents);*/
        // EventBus 7 has no bus-wide addListener: a game-bus event carries its own static BUS.
        //? if forge && >=1.21.6
        /*net.minecraftforge.event.GatherComponentsEvent.Item.BUS.addListener(AlexsCaves::gatherItemComponents);*/
        // …and 1.21.2's armour materials name an item tag for their repair ingredient, which
        // creating an armour item leaves unbound in the item registry. See bindModCreatedItemTags.
        //? if neoforge && >=1.21.2
        /*modEventBus.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST, net.minecraftforge.registries.RegisterEvent.class, ACItemRegistry::bindModCreatedItemTags);*/
        // Upstream also registered `this`, but this class declares no @SubscribeEvent method — all
        // the game-bus handlers live in CommonEvents. Forge's bus tolerates a listener-less object;
        // NeoForge's EventBus 7 rejects it outright ("has no @SubscribeEvent methods, but register
        // was called anyway"), so the dead registration is gone rather than gated.
        MinecraftForge.EVENT_BUS.register(new CommonEvents());
        // The two cabin-map trade handlers are a separate listener object because 26 deleted both of
        // their events and the file has to leave the source set whole — see ACVillagerTradeEvents.
        // Fully qualified deliberately: an import would outlive the class it names on >=26.
        //? if <26
        MinecraftForge.EVENT_BUS.register(new com.github.alexmodguy.alexscaves.server.event.ACVillagerTradeEvents());
        // Fabric is excluded rather than given a third arm: it has no brewing event to listen to,
        // so the eleven recipes stay in the vendored static registry on every version there and
        // mixin.fabric.PotionBrewingMixin consults it. See ACEffectRegistry#setup.
        //? if forge && >=1.21.6 {
        /*net.minecraftforge.event.brewing.BrewingRecipeRegisterEvent.BUS.addListener(AlexsCaves::registerBrewingRecipes);
        *///?} elif !fabric && >=1.20.5 {
        /*MinecraftForge.EVENT_BUS.addListener(AlexsCaves::registerBrewingRecipes);
        *///?}
        // No-op below 1.21.9; from there it supplies the CompoundTag entity-data serializer vanilla
        // stopped shipping. Registered first because SynchedEntityData.defineId asks the registry for
        // the serializer's network id, and the four accessors that name it are static finals on
        // entity classes.
        ACDataSerializers.register(modEventBus);
        // Sounds and the two fluids are flushed ahead of the blocks, which is a Fabric constraint and
        // a no-op on the other two loaders (there this call only subscribes to the mod bus, and the
        // loader decides the order registries are actually filled in). The chain that forces it:
        // AcidBlock and PurpleSodaBlock resolve their source fluid in their constructors on every
        // node that lacks Forge's deferred-supplier LiquidBlock constructor, each fluid names its
        // fluid type, and each fluid type dereferences ACSoundRegistry while it is built. Read the
        // comment on those two constructors before moving any of these three lines back down.
        ACSoundRegistry.DEF_REG.register(modEventBus);
        // Effects and potions likewise precede the blocks and items, and for the same kind of
        // reason: ACFoods reaches into ACEffectRegistry, and a food is built while the block or item
        // that carries it is constructed. MOB_EFFECT and POTION genuinely do precede BLOCK and ITEM
        // in BuiltInRegistries, so this is the loaders' own order written out, not a Fabric-only one.
        ACEffectRegistry.DEF_REG.register(modEventBus);
        ACEffectRegistry.POTION_DEF_REG.register(modEventBus);
        ACFluidRegistry.FLUID_TYPE_DEF_REG.register(modEventBus);
        ACFluidRegistry.FLUID_DEF_REG.register(modEventBus);
        ACBlockRegistry.DEF_REG.raw().register(modEventBus);
        ACBlockEntityRegistry.DEF_REG.register(modEventBus);
        // ⚠️ ENTITY_TYPE ahead of ITEM, which is again BuiltInRegistries' own order. The four
        // ModFishBucketItems resolve their entity type in their constructors on every node that
        // lacks Forge's deferred-supplier MobBucketItem, and so do the 43 spawn eggs from 1.21.3.
        // Nothing in ACEntityRegistry points back at an item or a block, so the edge is one-way.
        ACEntityRegistry.DEF_REG.register(modEventBus);
        ACItemRegistry.DEF_REG.raw().register(modEventBus);
        ACParticleRegistry.DEF_REG.register(modEventBus);
        ACEntityDataRegistry.DEF_REG.register(modEventBus);
        ACPOIRegistry.DEF_REG.register(modEventBus);
        ACFeatureRegistry.DEF_REG.register(modEventBus);
        ACSurfaceRuleConditionRegistry.DEF_REG.register(modEventBus);
        ACCarverRegistry.DEF_REG.register(modEventBus);
        ACStructureRegistry.DEF_REG.register(modEventBus);
        ACStructurePieceRegistry.DEF_REG.register(modEventBus);
        ACStructureProcessorRegistry.DEF_REG.register(modEventBus);
        // 1.21 loads the enchantments from the data pack instead, so there is no registry to attach.
        //? if <1.21
        ACEnchantmentRegistry.DEF_REG.register(modEventBus);
        ACMenuRegistry.DEF_REG.register(modEventBus);
        ACRecipeRegistry.DEF_REG.register(modEventBus);
        ACRecipeRegistry.TYPE_DEF_REG.register(modEventBus);
        // 1.21.5: FROG_VARIANT is a datapack registry, so there is no DeferredRegister to attach.
        //? if <1.21.5
        ACFrogRegistry.DEF_REG.register(modEventBus);
        ACLootTableRegistry.GLOBAL_LOOT_MODIFIER_DEF_REG.register(modEventBus);
        ACLootTableRegistry.LOOT_FUNCTION_DEF_REG.register(modEventBus);
        ACCreativeTabRegistry.DEF_REG.register(modEventBus);
        ACPotPatternRegistry.DEF_REG.register(modEventBus);
        // 1.20.3, not 1.20.2 — the criteria list is still a plain open BiMap on 1.20.2, so that one
        // version registers its triggers from ACAdvancementTriggerRegistry.setup() and has no
        // DeferredRegister to attach here. See the three-era note in ACAdvancementTrigger.
        //? if >=1.20.3
        /*ACAdvancementTriggerRegistry.DEF_REG.register(modEventBus);*/
        // Map decoration types only became a registry in 1.20.5; below that the cabin marker is an
        // enum constant a mixin splices in, with nothing to register. See ACVanillaMapUtil.
        //? if >=1.20.5
        /*ACVanillaMapUtil.DEF_REG.register(modEventBus);*/
        Citadel.registerModBus(modEventBus);
        // CodxLib's update checker: logs to the server console on start and notifies operators /
        // singleplayer on join. Also makes this mod show up in /codxlib versions and the
        // /codxlib help debug report.
        UpdateChecker.register(modInfo());
        PROXY.commonInit();
        ACBiomeRegistry.init();
    }

    /** Identity for CodxLib services such as the update checker. */
    public static ModInfo modInfo() {
        return new ModInfo(MODID, MODRINTH_SLUG, CodxLib.version(MODID), "[Alex's Caves]");
    }

    // From 1.20.5 the brewing recipe set is rebuilt on every world load instead of living in a
    // static registry, so the mod contributes its recipes from a game-bus event rather than from
    // commonSetup. The two loaders spell that event differently — Forge kept an addRecipe of its own
    // on the event object, NeoForge hands out vanilla's PotionBrewing.Builder — but both take an
    // IBrewingRecipe, so only the two lines below differ.
    //? if forge && >=1.20.5 {
    /*private static void registerBrewingRecipes(final net.minecraftforge.event.brewing.BrewingRecipeRegisterEvent event) {
        ACEffectRegistry.registerBrewing(event::addRecipe);
    }
    *///?}
    //? if neoforge && >=1.20.5 {
    /*private static void registerBrewingRecipes(final net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent event) {
        ACEffectRegistry.registerBrewing(event.getBuilder()::addRecipe);
    }
    *///?}

    // What Item#getEnchantmentValue/#isEnchantable and #isValidRepairItem used to do for themselves.
    // 1.21.2 deleted all three for the minecraft:enchantable and minecraft:repairable components, so
    // the mod's own answers — still declared on ACEnchantableItem and ACRepairableItem, unchanged on
    // every version — are stamped onto the items' default components here instead.
    //
    // This runs rather than Item.Properties#enchantable/#repairable at construction time on purpose:
    // repairable(Item) resolves builtInRegistryHolder() while the properties are being built, i.e.
    // while ACItemRegistry's own static fields are still initialising, which is exactly the
    // DeferredRegister ordering trap that made ACFoods reach an unbound ACEffectRegistry.RAGE. The
    // event fires once every registry is populated, and it is also the only shape that can *merge*
    // with the tier's existing repair materials instead of overwriting them.
    //? if neoforge && >=1.21.2 {
    /*private void modifyDefaultComponents(final net.neoforged.neoforge.event.ModifyDefaultComponentsEvent event) {
        for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item instanceof com.github.alexmodguy.alexscaves.server.item.ACEnchantableItem enchantable) {
                int value = enchantable.getEnchantmentValue();
                // The component's codec rejects a non-positive value, where the old hook read 0 as
                // "not enchantable" — none of this mod's items returns one, but say so out loud.
                if (value > 0) {
                    event.modify(item, builder -> builder.set(net.minecraft.core.component.DataComponents.ENCHANTABLE, new net.minecraft.world.item.enchantment.Enchantable(value)));
                }
            }
            if (item instanceof com.github.alexmodguy.alexscaves.server.item.ACRepairableItem repairable) {
                event.modify(item, builder -> {
                    java.util.List<net.minecraft.core.Holder<net.minecraft.world.item.Item>> materials = new java.util.ArrayList<>();
                    net.minecraft.world.item.enchantment.Repairable existing = acExistingRepairable(item, builder);
                    if (existing != null && !repairable.acReplacesTierRepairMaterials()) {
                        existing.items().forEach(materials::add);
                    }
                    for (net.minecraft.world.item.Item material : repairable.acExtraRepairMaterials()) {
                        materials.add(material.builtInRegistryHolder());
                    }
                    builder.set(net.minecraft.core.component.DataComponents.REPAIRABLE, new net.minecraft.world.item.enchantment.Repairable(net.minecraft.core.HolderSet.direct(materials)));
                });
            }
        }
    }
    *///?}

    // Where the tier's own repair materials are read from, and the one thing about that job which
    // differs across the NeoForge range.
    //
    // Up to 26 an Item kept its default components in a plain field, so item.components() answered at
    // any time. From 26.1 that getter delegates to builtInRegistryHolder().components(), which throws
    // "Components not bound yet" until DataComponentInitializers binds them — and this event fires
    // while those very components are still being assembled, so the old read is a hard boot failure on
    // every 26.x node, not just the newest. The builder the modifier is handed has already been through
    // vanilla's own initializers (DataComponentInitializers#createInitializerForRegistry runs them,
    // then calls NeoForge's DataComponentModifiers#apply on the same builder), so it answers exactly
    // the same question — and DataComponentMap.Builder#get arrives in 26.1, the very version that
    // broke the old read.
    //
    // The parameter type is per-arm on purpose: 26.1 is also where ModifyDefaultComponentsEvent#modify
    // swapped its Consumer from a DataComponentPatch.Builder to a DataComponentMap.Builder, so the
    // shared call site hands each arm exactly the type that version's event gives it. Neither arm uses
    // both parameters — the older one only needs the item, the newer one only the builder — but a
    // helper called from one place has to declare both.
    //? if neoforge && >=26.1 {
    /*private static net.minecraft.world.item.enchantment.Repairable acExistingRepairable(net.minecraft.world.item.Item item, net.minecraft.core.component.DataComponentMap.Builder builder) {
        return builder.get(net.minecraft.core.component.DataComponents.REPAIRABLE);
    }
    *///?} elif neoforge && >=1.21.2 {
    /*private static net.minecraft.world.item.enchantment.Repairable acExistingRepairable(net.minecraft.world.item.Item item, net.minecraft.core.component.DataComponentPatch.Builder builder) {
        return item.components().get(net.minecraft.core.component.DataComponents.REPAIRABLE);
    }
    *///?}

    // Forge's half of the same job. It fires once per item, from Item#components() the first time
    // anything asks — so it lands after registration is over, which is what acExtraRepairMaterials
    // needs, and there is no registry walk to do.
    //
    // The existing repair materials come off getOriginalComponentMap() and NOT off item.components():
    // that getter is the very method firing this event, and its cache is only filled once the event
    // returns, so reading it from in here recurses forever. Forge composites what is registered here
    // over the original map, so setting REPAIRABLE replaces the tier's own entry — the merge is this
    // method's to do, exactly as on NeoForge.
    //? if forge && >=1.21.2 {
    /*private static void gatherItemComponents(final net.minecraftforge.event.GatherComponentsEvent.Item event) {
        net.minecraft.world.item.Item item = event.getOwner();
        if (item instanceof com.github.alexmodguy.alexscaves.server.item.ACEnchantableItem enchantable) {
            int value = enchantable.getEnchantmentValue();
            // The component's codec rejects a non-positive value, where the old hook read 0 as
            // "not enchantable" — none of this mod's items returns one, but say so out loud.
            if (value > 0) {
                event.register(net.minecraft.core.component.DataComponents.ENCHANTABLE, new net.minecraft.world.item.enchantment.Enchantable(value));
            }
        }
        if (item instanceof com.github.alexmodguy.alexscaves.server.item.ACRepairableItem repairable) {
            java.util.List<net.minecraft.core.Holder<net.minecraft.world.item.Item>> materials = new java.util.ArrayList<>();
            net.minecraft.world.item.enchantment.Repairable existing = event.getOriginalComponentMap().get(net.minecraft.core.component.DataComponents.REPAIRABLE);
            if (existing != null && !repairable.acReplacesTierRepairMaterials()) {
                existing.items().forEach(materials::add);
            }
            for (net.minecraft.world.item.Item material : repairable.acExtraRepairMaterials()) {
                materials.add(material.builtInRegistryHolder());
            }
            event.register(net.minecraft.core.component.DataComponents.REPAIRABLE, new net.minecraft.world.item.enchantment.Repairable(net.minecraft.core.HolderSet.direct(materials)));
        }
    }
    *///?}

    // Both exist only to re-read the biome-generation JSON whenever the loader (re)loads a config
    // file. Fabric fires no such event — the vendored spec loads once and is never watched — so
    // this pair is gated out entirely there and the constructor's fabric arm does the one read.
    //? if !fabric {
    private void loadConfig(final ModConfigEvent.Loading event) {
        BiomeGenerationConfig.reloadConfig();
    }

    private void reloadConfig(final ModConfigEvent.Reloading event) {
        BiomeGenerationConfig.reloadConfig();
    }
    //?}

    private void commonSetup(final FMLCommonSetupEvent event) {
        PROXY.initPathfinding();
        ACNetwork.registerMessages();
        event.enqueueWork(() -> {
            ACSurfaceRules.setup();
            ACPlayerCapes.setup();
            ACEffectRegistry.setup();
            ACBlockRegistry.setup();
            ACItemRegistry.setup();
            ACAdvancementTriggerRegistry.setup();
            ACPotPatternRegistry.expandVanillaDefinitions();
            ACBlockEntityRegistry.expandVanillaDefinitions();
            ACPlatform.registerForcedChunkCallback();
        });
        readModIncompatibilities();
    }

    // NeoForge has no static per-mod-id chunk-forcing API: a mod builds one TicketController and
    // registers it on the mod bus, then keeps the instance to force chunks with. ACPlatform holds it
    // for the four things that force chunks (the blast, the beholder eye, the occult gem, the remote
    // detonator); on Forge the equivalent registration is the one-liner in registerForcedChunkCallback.
    //? if neoforge {
    /*private void registerTicketControllers(final net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent event) {
        net.neoforged.neoforge.common.world.chunk.TicketController controller =
                new net.neoforged.neoforge.common.world.chunk.TicketController(
                        ResourceLocation.fromNamespaceAndPath(MODID, "forced_chunks"), ACWorldData::clearLoadedChunksCallback);
        ACPlatform.setTicketController(controller);
        event.register(controller);
    }
    *///?}

    // The two container block entities that hoppers and pipes can reach into. On Forge each one
    // overrides getCapability itself; NeoForge dropped that hook, so the same SidedInvWrapper is
    // wired up front here instead. Both are WorldlyContainers, so the side-aware wrapper already
    // honours their canPlaceItemThroughFace / canTakeItemThroughFace rules.
    //
    // 21.9 replaced the item-handler capability with the transfer API. WorldlyContainerWrapper is
    // the successor to SidedInvWrapper and is registered exactly this way for vanilla's own sided
    // containers in NeoForge's CapabilityHooks, so the wiring is the same shape with both names
    // moved on.
    //? if neoforge && >=1.21.9 {
    /*private void registerCapabilities(final net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
                ACBlockEntityRegistry.ABYSSAL_ALTAR.get(),
                (blockEntity, side) -> side == null ? null : new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(blockEntity, side));
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
                ACBlockEntityRegistry.NUCLEAR_FURNACE.get(),
                (blockEntity, side) -> side == null ? null : new net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper(blockEntity, side));
    }
    *///?} elif neoforge {
    /*private void registerCapabilities(final net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ACBlockEntityRegistry.ABYSSAL_ALTAR.get(),
                (blockEntity, side) -> side == null ? null : new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(blockEntity, side));
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ACBlockEntityRegistry.NUCLEAR_FURNACE.get(),
                (blockEntity, side) -> side == null ? null : new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(blockEntity, side));
    }
    *///?}

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> PROXY.clientInit());
    }

    public static <MSG> void sendMSGToServer(MSG message) {
        ACNetwork.sendToServer(message);
    }

    public static <MSG> void sendMSGToAll(MSG message) {
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            sendNonLocal(message, player);
        }
    }

    private void loadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(ACFluidRegistry::postInit);
        event.enqueueWork(ACLoadedMods::afterAllModsLoaded);
        Citadel.loadComplete(event);
    }

    public static <MSG> void sendNonLocal(MSG msg, ServerPlayer player) {
        ACNetwork.sendToPlayer(msg, player);
    }

    private void registerLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        ACModelLayers.register(event);
    }

    // Forge 1.20.5+ has no RenderGuiOverlayEvent: the HUD is a tree of named LayeredDraw.Layers, and
    // a mod hangs its own layers off that tree — and attaches conditions to vanilla's — exactly once.
    // Everything the pre-1.20.5 Pre/Post handlers in ClientEvents did has an equivalent here bar one:
    // Forge's layer set is coarser than the old overlay set, so the selected item name (welded into
    // the "hotbar" layer, together with the status bars) cannot be hidden on its own while possessing
    // a mob. NeoForge kept a fine-grained per-layer event and does not have that gap.
    //
    // Forge 1.21 itself (51.0.33, the only build line for that version) is the exception to the
    // exception, and it is a per-BUILD gap rather than a version trend: 50.2.9 ships
    // AddGuiOverlayLayersEvent + ForgeLayeredDraw, 51.0.33 ships neither, 52.1.15 has them back.
    // There is no HUD extension point at all on that one node, so it ships without the riding
    // meter, the irradiated hearts and the possession overlay hiding.
    //
    // ...and from Forge 56.0.0 (1.21.6) the same gap reopens permanently: AddGuiOverlayLayersEvent
    // and ForgeLayeredDraw are gone, and the older RegisterGuiOverlaysEvent/RenderGuiOverlayEvent/
    // RenderGuiEvent files that would have replaced them are commented-out corpses in the Forge
    // sources ("Forge 1.20.5 - Removed, Mojang created a layered rendering system…"). So Forge has
    // no HUD extension point at all on 1.21.6+ and this node group ships without those three
    // pieces, the same way 1.21-forge does. The eventual fix is the ACLevelRenderStage treatment —
    // a loader-neutral hook fed from the loader event where one exists and from a mixin where it
    // does not — which the 22 Fabric nodes will need for exactly the same reason.
    //? if forge && >=1.20.5 && !=1.21 && <1.21.6 {
    /*private void addGuiOverlayLayers(final net.minecraftforge.client.event.AddGuiOverlayLayersEvent event) {
        net.minecraftforge.client.gui.overlay.ForgeLayeredDraw layers = event.getLayeredDraw();
        net.minecraft.resources.ResourceLocation stack = net.minecraftforge.client.gui.overlay.ForgeLayeredDraw.PRE_SLEEP_STACK;
        layers.addConditionTo(stack, net.minecraftforge.client.gui.overlay.ForgeLayeredDraw.CROSSHAIR,
                () -> !com.github.alexmodguy.alexscaves.client.event.ClientEvents.hidePossessedPlayerOverlay());
        // "experience" is Forge's name for the bar the jump meter shares, so this one condition covers
        // both of the elements the old handler cancelled separately.
        layers.addConditionTo(stack, net.minecraftforge.client.gui.overlay.ForgeLayeredDraw.EXPERIENCE,
                () -> !com.github.alexmodguy.alexscaves.client.event.ClientEvents.hidePossessedPlayerOverlay() && !com.github.alexmodguy.alexscaves.client.event.ClientEvents.hideExperienceBar());
        layers.addAbove(stack,
                ResourceLocation.fromNamespaceAndPath(MODID, "riding_meter"),
                net.minecraftforge.client.gui.overlay.ForgeLayeredDraw.CROSSHAIR,
                (guiGraphics, partialTick) -> com.github.alexmodguy.alexscaves.client.event.ClientEvents.renderRidingMeterHud(guiGraphics));
        // The hearts live inside "hotbar" here, so the repaint follows that whole layer instead of the
        // player-health element. Nothing else draws over the heart row, so it looks the same.
        layers.addAbove(stack,
                ResourceLocation.fromNamespaceAndPath(MODID, "irradiated_hearts"),
                net.minecraftforge.client.gui.overlay.ForgeLayeredDraw.HOTBAR,
                (guiGraphics, partialTick) -> com.github.alexmodguy.alexscaves.client.event.ClientEvents.renderIrradiatedHearts(guiGraphics));
    }
    *///?}

    private void readModIncompatibilities() {
        BufferedReader urlContents = WebHelper.getURLContents("https://raw.githubusercontent.com/AlexModGuy/AlexsCaves/main/src/main/resources/assets/alexscaves/warning/mod_generation_conflicts.txt", "assets/alexscaves/warning/mod_generation_conflicts.txt");
        if (urlContents != null) {
            try {
                String line;
                while ((line = urlContents.readLine()) != null) {
                    MOD_GENERATION_CONFLICTS.add(line);
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to load mod conflicts");
            }
        } else {
            LOGGER.warn("Failed to load mod conflicts");
        }
    }

}