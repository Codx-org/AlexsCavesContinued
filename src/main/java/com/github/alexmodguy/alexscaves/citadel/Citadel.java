package com.github.alexmodguy.alexscaves.citadel;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.message.ACNetwork;
import com.github.alexmodguy.alexscaves.citadel.compat.ModCompatBridge;
import com.github.alexmodguy.alexscaves.citadel.item.CitadelDisplayItems;
import com.github.alexmodguy.alexscaves.citadel.server.generation.CitadelSurfaceRuleWrapper;
import com.github.alexmodguy.alexscaves.citadel.server.message.AnimationMessage;
import com.github.alexmodguy.alexscaves.citadel.server.message.DanceJukeboxMessage;
import com.github.alexmodguy.alexscaves.citadel.server.message.MessageSyncPath;
import com.github.alexmodguy.alexscaves.citadel.server.message.MessageSyncPathReached;
import com.github.alexmodguy.alexscaves.citadel.server.message.PropertiesMessage;
import com.github.alexmodguy.alexscaves.citadel.server.message.SyncClientTickRateMessage;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.registries.DeferredRegister;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point of the Citadel runtime that Alex's Caves carries internally.
 *
 * <p>Upstream this was Citadel's {@code @Mod} class. Alex's Caves Continued vendors the subset of
 * Citadel it actually uses (relocated out of {@code com.github.alexthe666.citadel} so a player who
 * also installs the real Citadel does not end up with two copies of the same class), which leaves
 * this type as the seam the vendored code still talks to: the logger, the side proxy, and the three
 * packet senders — all of which now route through Alex's Caves' own mod id, channel and event bus.
 *
 * <p>Almost none of Citadel's own content is carried over: no lectern block, no config, no
 * patreon/capes GUI, no Tetris, no web helper. Only the library machinery Alex's Caves calls, plus
 * the two NBT-driven display items its advancement JSONs use as icons — see
 * {@link com.github.alexmodguy.alexscaves.citadel.item.CitadelDisplayItems}.
 */
public class Citadel {

    public static final Logger LOGGER = LogManager.getLogger("alexscaves-citadel");

    // See AlexsCaves#PROXY — DistExecutor is gone on NeoForge 1.21 and on Forge 62 (26).
    // The middle arm is Forge-scoped for the reason spelled out there; Fabric keeps the stub.
    //? if neoforge && >=1.21 {
    /*public static final CitadelProxy PROXY = net.neoforged.fml.loading.FMLEnvironment.dist.isClient() ? new CitadelClientProxy() : new CitadelProxy();
    *///?} elif forge && >=26 {
    /*public static final CitadelProxy PROXY = net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient() ? new CitadelClientProxy() : new CitadelProxy();
    *///?} else {
    public static final CitadelProxy PROXY = net.minecraftforge.fml.DistExecutor.runForDist(() -> CitadelClientProxy::new, () -> CitadelProxy::new);
    //?}

    private Citadel() {
    }

    /**
     * Mirrors what Citadel's mod constructor did, minus its own registries. Call from the mod
     * constructor.
     */
    public static void registerModBus(IEventBus modEventBus) {
        // SurfaceRulesManager wraps the overworld rule source in a CitadelSurfaceRuleWrapper, and a
        // RuleSource with an unregistered codec cannot be encoded — the world save would fail.
        // Registered under this mod's namespace, not Citadel's: the wrapper is swapped back out
        // before level data is written (see NoiseGeneratorSettingsMixin#onSaveData), so the id is
        // internal and never reaches disk.
        // 1.20.5 retyped the MATERIAL_RULE / MATERIAL_CONDITION registries from Codec to MapCodec.
        //? if >=1.20.5 {
        /*DeferredRegister<com.mojang.serialization.MapCodec<? extends SurfaceRules.RuleSource>> surfaceRules =
                DeferredRegister.create(Registries.MATERIAL_RULE, AlexsCaves.MODID);
        *///?} else {
        DeferredRegister<Codec<? extends SurfaceRules.RuleSource>> surfaceRules =
                DeferredRegister.create(Registries.MATERIAL_RULE, AlexsCaves.MODID);
        //?}
        surfaceRules.register("citadel_wrapper", CitadelSurfaceRuleWrapper.CODEC::codec);
        surfaceRules.register(modEventBus);

        // icon_item / effect_item — referenced by name from this mod's advancement JSONs.
        CitadelDisplayItems.register(modEventBus);

        // Server tick drives the tick-rate system; the client side adds the client tick and the
        // pathfinding debug renderer. The proxy only picks which listeners to put on the bus — it is
        // not one itself, see CitadelEvents.
        PROXY.registerEventHandlers();
    }

    /**
     * Registers Citadel's packets on Alex's Caves' channel. Returns the next free discriminator so
     * the caller can keep numbering its own messages from there.
     */
    /** Citadel's own packets share the mod's single channel; see {@link ACNetwork}. */
    public static void registerMessages() {
        ACNetwork.register(PropertiesMessage.class, PropertiesMessage::write, PropertiesMessage::read, PropertiesMessage.Handler::handle);
        ACNetwork.register(AnimationMessage.class, AnimationMessage::write, AnimationMessage::read, AnimationMessage.Handler::handle);
        ACNetwork.register(SyncClientTickRateMessage.class, SyncClientTickRateMessage::write, SyncClientTickRateMessage::read, SyncClientTickRateMessage.Handler::handle);
        ACNetwork.register(DanceJukeboxMessage.class, DanceJukeboxMessage::write, DanceJukeboxMessage::read, DanceJukeboxMessage.Handler::handle);
        ACNetwork.register(MessageSyncPath.class, MessageSyncPath::write, MessageSyncPath::read, MessageSyncPath.Handler::handle);
        ACNetwork.register(MessageSyncPathReached.class, MessageSyncPathReached::write, MessageSyncPathReached::read, MessageSyncPathReached.Handler::handle);
    }

    /** Must run after every mod is loaded — see {@link ModCompatBridge}. */
    public static void loadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(ModCompatBridge::afterAllModsLoaded);
    }

    public static <MSG> void sendMSGToServer(MSG message) {
        AlexsCaves.sendMSGToServer(message);
    }

    public static <MSG> void sendMSGToAll(MSG message) {
        AlexsCaves.sendMSGToAll(message);
    }

    public static <MSG> void sendNonLocal(MSG msg, ServerPlayer player) {
        AlexsCaves.sendNonLocal(msg, player);
    }
}
