package com.github.alexmodguy.alexscaves.server.message;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * The mod's play-phase channel, and the one place that knows how any given loader and Minecraft
 * version wants packets registered and sent.
 *
 * <p>Upstream talked to Forge's {@code SimpleChannel} directly from {@code AlexsCaves.commonSetup}
 * and from every handler's {@code NetworkEvent.Context}. That surface does not survive the version
 * range this mod now spans: Forge moved {@code SimpleChannel} out of {@code network.simple} and
 * replaced {@code NetworkRegistry.ChannelBuilder} with {@code ChannelBuilder} in 1.20.2, deleted
 * {@code NetworkEvent} in the same release, and neither NeoForge nor Fabric ever had any of it.
 *
 * <p>CodxLib's {@code CodxNetwork} is the natural home for this and will back the 1.20.5-and-up
 * arms, but it cannot back all of them: it is built on {@code CustomPacketPayload} +
 * {@code StreamCodec}, so CodxLib itself excludes the whole package below 1.20.5 (see the
 * {@code exclude("codx/codxlib/api/network/**")} line in each of its five buildscripts). Alex's
 * Caves goes down to 1.20.1, hence this façade.
 *
 * <p>The twenty-two messages keep their upstream shape — a {@code write}/{@code read} pair over a
 * plain {@code FriendlyByteBuf} and a {@code handle} — with only their context type replaced by
 * {@link ACNetworkContext}.
 */
public class ACNetwork {

    private static final ResourceLocation CHANNEL_NAME = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "main_channel");

    /** Packet discriminators are assigned in registration order, so registration order is protocol. */
    private static int nextId = 0;

    //? if forge && <1.20.2 {

    private static final String PROTOCOL_VERSION = "1";

    private static final net.minecraftforge.network.simple.SimpleChannel CHANNEL =
            net.minecraftforge.network.NetworkRegistry.ChannelBuilder
                    .named(CHANNEL_NAME)
                    .clientAcceptedVersions(PROTOCOL_VERSION::equals)
                    .serverAcceptedVersions(PROTOCOL_VERSION::equals)
                    .networkProtocolVersion(() -> PROTOCOL_VERSION)
                    .simpleChannel();

    public static <MSG> void register(Class<MSG> type,
                                      BiConsumer<MSG, FriendlyByteBuf> encoder,
                                      Function<FriendlyByteBuf, MSG> decoder,
                                      BiConsumer<MSG, ACNetworkContext> handler) {
        CHANNEL.registerMessage(nextId++, type, encoder, decoder,
                (message, contextSupplier) -> handler.accept(message, new ForgeContext(contextSupplier.get())));
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        CHANNEL.sendTo(message, player.connection.connection, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
    }

    private record ForgeContext(net.minecraftforge.network.NetworkEvent.Context context) implements ACNetworkContext {

        @Override
        public void enqueueWork(Runnable runnable) {
            context.enqueueWork(runnable);
        }

        @Override
        public ServerPlayer getSender() {
            return context.getSender();
        }

        @Override
        public boolean isClientSide() {
            return context.getDirection().getReceptionSide() == net.minecraftforge.fml.LogicalSide.CLIENT;
        }

        @Override
        public void setPacketHandled(boolean handled) {
            context.setPacketHandled(handled);
        }
    }

    //?}

    //? if forge && >=1.20.2 {

    /*private static final int PROTOCOL_VERSION = 1;

    private static final net.minecraftforge.network.SimpleChannel CHANNEL =
            net.minecraftforge.network.ChannelBuilder
                    .named(CHANNEL_NAME)
                    .networkProtocolVersion(PROTOCOL_VERSION)
                    .clientAcceptedVersions(net.minecraftforge.network.Channel.VersionTest.exact(PROTOCOL_VERSION))
                    .serverAcceptedVersions(net.minecraftforge.network.Channel.VersionTest.exact(PROTOCOL_VERSION))
                    .simpleChannel();

    public static <MSG> void register(Class<MSG> type,
                                      BiConsumer<MSG, FriendlyByteBuf> encoder,
                                      Function<FriendlyByteBuf, MSG> decoder,
                                      BiConsumer<MSG, ACNetworkContext> handler) {
        // consumerNetworkThread, not consumerMainThread: upstream's registerMessage handed the
        // handler the network thread and the handlers that need the main thread call enqueueWork
        // themselves. Switching to consumerMainThread here would change when they run.
        // Bound to a variable rather than written inline: consumerNetworkThread is overloaded on
        // BiConsumer and ToBooleanBiFunction, and an implicit lambda is not pertinent to
        // applicability, so javac calls the inline form ambiguous.
        BiConsumer<MSG, net.minecraftforge.event.network.CustomPayloadEvent.Context> consumer =
                (message, context) -> handler.accept(message, new ForgeContext(context));
        CHANNEL.messageBuilder(type, nextId++)
                .encoder(encoder)
                .decoder(decoder)
                .consumerNetworkThread(consumer)
                .add();
    }

    public static void sendToServer(Object message) {
        CHANNEL.send(message, net.minecraftforge.network.PacketDistributor.SERVER.noArg());
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        CHANNEL.send(message, net.minecraftforge.network.PacketDistributor.PLAYER.with(player));
    }

    private record ForgeContext(net.minecraftforge.event.network.CustomPayloadEvent.Context context) implements ACNetworkContext {

        @Override
        public void enqueueWork(Runnable runnable) {
            context.enqueueWork(runnable);
        }

        @Override
        public ServerPlayer getSender() {
            return context.getSender();
        }

        @Override
        public boolean isClientSide() {
            return context.isClientSide();
        }

        @Override
        public void setPacketHandled(boolean handled) {
            context.setPacketHandled(handled);
        }
    }

    *///?}

    //? if neoforge && <1.20.5 {

    /*// NeoForge has no SimpleChannel: 1.20.2 moved it to vanilla CustomPacketPayloads, one registered
    // type per packet, and the payload is decoded on the network thread by a reader handed in at
    // registration. Rather than turn twenty-two message classes into twenty-two payload types, one
    // payload type carries a varint discriminator plus whatever the message's own write() emits — the
    // wire format Forge's SimpleChannel already used, so the message classes stay untouched.
    //
    // 1.20.5 replaced FriendlyByteBuf.Reader with StreamCodec, at which point CodxNetwork can back
    // this instead; that is why this arm is version-gated rather than a plain !forge one.

    private record Entry<MSG>(BiConsumer<MSG, FriendlyByteBuf> encoder,
                              Function<FriendlyByteBuf, MSG> decoder,
                              BiConsumer<MSG, ACNetworkContext> handler) {

        @SuppressWarnings("unchecked")
        void encode(Object message, FriendlyByteBuf buf) {
            encoder.accept((MSG) message, buf);
        }

        @SuppressWarnings("unchecked")
        void handle(Object message, ACNetworkContext context) {
            handler.accept((MSG) message, context);
        }
    }

    private static final java.util.List<Entry<?>> ENTRIES = new java.util.ArrayList<>();
    private static final java.util.Map<Class<?>, Integer> DISCRIMINATORS = new java.util.HashMap<>();

    public static <MSG> void register(Class<MSG> type,
                                      BiConsumer<MSG, FriendlyByteBuf> encoder,
                                      Function<FriendlyByteBuf, MSG> decoder,
                                      BiConsumer<MSG, ACNetworkContext> handler) {
        DISCRIMINATORS.put(type, nextId++);
        ENTRIES.add(new Entry<>(encoder, decoder, handler));
    }

    private record ACPayload(int discriminator, Object message)
            implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(discriminator);
            ENTRIES.get(discriminator).encode(message, buf);
        }

        @Override
        public ResourceLocation id() {
            return CHANNEL_NAME;
        }
    }

    private static ACPayload wrap(Object message) {
        Integer discriminator = DISCRIMINATORS.get(message.getClass());
        if (discriminator == null) {
            throw new IllegalArgumentException("Unregistered message type " + message.getClass().getName());
        }
        return new ACPayload(discriminator, message);
    }

    // Called from the mod bus. Fills the message table first: the payload registration event can fire
    // before common setup, and the reader below indexes straight into that table.
    public static void registerPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent event) {
        registerMessages();
        event.registrar(AlexsCaves.MODID).play(CHANNEL_NAME, buf -> {
            int discriminator = buf.readVarInt();
            return new ACPayload(discriminator, ENTRIES.get(discriminator).decoder().apply(buf));
        }, (payload, context) -> ENTRIES.get(payload.discriminator()).handle(payload.message(), new NeoForgeContext(context)));
    }

    public static void sendToServer(Object message) {
        net.neoforged.neoforge.network.PacketDistributor.SERVER.noArg().send(wrap(message));
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        net.neoforged.neoforge.network.PacketDistributor.PLAYER.with(player).send(wrap(message));
    }

    private record NeoForgeContext(net.neoforged.neoforge.network.handling.PlayPayloadContext context)
            implements ACNetworkContext {

        @Override
        public void enqueueWork(Runnable runnable) {
            context.workHandler().submitAsync(runnable);
        }

        @Override
        public ServerPlayer getSender() {
            return context.player().filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast).orElse(null);
        }

        @Override
        public boolean isClientSide() {
            return context.flow().isClientbound();
        }

        @Override
        public void setPacketHandled(boolean handled) {
            // NeoForge has no such concept — a payload that reaches its handler is handled.
        }
    }

    *///?}

    //? if neoforge && >=1.20.5 {

    /*// The 1.20.5 payload rework: the FriendlyByteBuf.Reader handed to registrar.play() became a
    // StreamCodec, every payload gained a typed CustomPacketPayload.Type key in place of its bare
    // ResourceLocation id, and the three flow-specific registration methods replaced the single
    // one. The one-payload-carrying-a-discriminator design of the arm above is untouched by all of
    // that — only the plumbing around it is rewritten — so the wire format and the twenty-two
    // message classes stay exactly as they are.
    //
    // Handlers now run on the main thread by default (PayloadRegistrar starts on HandlerThread.MAIN
    // and wraps every handler in a MainThreadPayloadHandler). The messages that call enqueueWork
    // still behave: on the main thread it runs the task immediately.

    private record Entry<MSG>(BiConsumer<MSG, FriendlyByteBuf> encoder,
                              Function<FriendlyByteBuf, MSG> decoder,
                              BiConsumer<MSG, ACNetworkContext> handler) {

        @SuppressWarnings("unchecked")
        void encode(Object message, FriendlyByteBuf buf) {
            encoder.accept((MSG) message, buf);
        }

        @SuppressWarnings("unchecked")
        void handle(Object message, ACNetworkContext context) {
            handler.accept((MSG) message, context);
        }
    }

    private static final java.util.List<Entry<?>> ENTRIES = new java.util.ArrayList<>();
    private static final java.util.Map<Class<?>, Integer> DISCRIMINATORS = new java.util.HashMap<>();

    public static <MSG> void register(Class<MSG> type,
                                      BiConsumer<MSG, FriendlyByteBuf> encoder,
                                      Function<FriendlyByteBuf, MSG> decoder,
                                      BiConsumer<MSG, ACNetworkContext> handler) {
        DISCRIMINATORS.put(type, nextId++);
        ENTRIES.add(new Entry<>(encoder, decoder, handler));
    }

    private record ACPayload(int discriminator, Object message)
            implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

        static final Type<ACPayload> TYPE = new Type<>(CHANNEL_NAME);

        // A RegistryFriendlyByteBuf is a FriendlyByteBuf, so the messages' own write/read pairs
        // take it unchanged — they simply never reach for the registry access it adds.
        static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ACPayload> CODEC =
                net.minecraft.network.codec.StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeVarInt(payload.discriminator());
                            ENTRIES.get(payload.discriminator()).encode(payload.message(), buf);
                        },
                        buf -> {
                            int discriminator = buf.readVarInt();
                            return new ACPayload(discriminator, ENTRIES.get(discriminator).decoder().apply(buf));
                        });

        @Override
        public Type<ACPayload> type() {
            return TYPE;
        }
    }

    private static ACPayload wrap(Object message) {
        Integer discriminator = DISCRIMINATORS.get(message.getClass());
        if (discriminator == null) {
            throw new IllegalArgumentException("Unregistered message type " + message.getClass().getName());
        }
        return new ACPayload(discriminator, message);
    }

    // Called from the mod bus. Fills the message table first: the payload registration event can fire
    // before common setup, and the codec below indexes straight into that table.
    public static void registerPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        registerMessages();
        // Bidirectional because the discriminator payload carries messages that travel each way, and
        // registrar() now takes the protocol version rather than the namespace — the namespace comes
        // from the payload type's own id.
        //
        // ⚠️ The handler is hoisted into a local purely so `!mc217-bidirectional-nf` can name the
        // call's tail as one token. NeoForge 21.7 gave `register` a SECOND handler — serverbound
        // first, clientbound second — and made this three-argument convenience pass `null` for the
        // clientbound one, so the same source that registers both directions below 1.21.7 registers
        // only the serverbound one from it, and the client dies at load with "Some clientbound
        // payloads are missing client-side handlers". Same name, same arity, opposite meaning: it
        // compiles everywhere and no server ever notices, since the check runs on the client dist.
        net.neoforged.neoforge.network.handling.IPayloadHandler<ACPayload> handler =
                (payload, context) -> ENTRIES.get(payload.discriminator()).handle(payload.message(), new NeoForgeContext(context));
        event.registrar("1").playBidirectional(ACPayload.TYPE, ACPayload.CODEC, handler);
    }

    public static void sendToServer(Object message) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(wrap(message));
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, wrap(message));
    }

    private record NeoForgeContext(net.neoforged.neoforge.network.handling.IPayloadContext context)
            implements ACNetworkContext {

        @Override
        public void enqueueWork(Runnable runnable) {
            context.enqueueWork(runnable);
        }

        @Override
        public ServerPlayer getSender() {
            // On a clientbound payload this is the receiving LocalPlayer, which is not a ServerPlayer;
            // the handlers that ask for a sender are the serverbound ones.
            return context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        }

        @Override
        public boolean isClientSide() {
            return context.flow().isClientbound();
        }

        @Override
        public void setPacketHandled(boolean handled) {
            // NeoForge has no such concept — a payload that reaches its handler is handled.
        }
    }

    *///?}

    //? if fabric && <1.20.5 {

    /*// Fabric has no channel abstraction of its own: a play-phase packet is a channel id plus a raw
    // FriendlyByteBuf, and a receiver is registered per id. That is precisely the shape the two
    // NeoForge arms below already reduce to, so this arm reuses their design unchanged — ONE channel
    // carrying a varint discriminator plus whatever the message's own write() emits, i.e. the wire
    // format Forge's SimpleChannel used. The twenty-two message classes stay untouched.
    //
    // Gated below 1.20.5 because fabric-api followed vanilla's payload rework there: the raw-buffer
    // send/receive overloads used here were deprecated and removed in favour of CustomPacketPayload,
    // which is the arm the 1.20.5-and-up Fabric nodes will need (and where CodxNetwork can back it).

    private record Entry<MSG>(BiConsumer<MSG, FriendlyByteBuf> encoder,
                              Function<FriendlyByteBuf, MSG> decoder,
                              BiConsumer<MSG, ACNetworkContext> handler) {

        @SuppressWarnings("unchecked")
        void encode(Object message, FriendlyByteBuf buf) {
            encoder.accept((MSG) message, buf);
        }

        @SuppressWarnings("unchecked")
        void handle(Object message, ACNetworkContext context) {
            handler.accept((MSG) message, context);
        }
    }

    private static final java.util.List<Entry<?>> ENTRIES = new java.util.ArrayList<>();
    private static final java.util.Map<Class<?>, Integer> DISCRIMINATORS = new java.util.HashMap<>();

    public static <MSG> void register(Class<MSG> type,
                                      BiConsumer<MSG, FriendlyByteBuf> encoder,
                                      Function<FriendlyByteBuf, MSG> decoder,
                                      BiConsumer<MSG, ACNetworkContext> handler) {
        DISCRIMINATORS.put(type, nextId++);
        ENTRIES.add(new Entry<>(encoder, decoder, handler));
    }

    private static FriendlyByteBuf encode(Object message) {
        Integer discriminator = DISCRIMINATORS.get(message.getClass());
        if (discriminator == null) {
            throw new IllegalArgumentException("Unregistered message type " + message.getClass().getName());
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeVarInt(discriminator);
        ENTRIES.get(discriminator).encode(message, buf);
        return buf;
    }

    // Decoding happens on the network thread, while the buffer is still valid; the handlers that
    // need the main thread call enqueueWork themselves, exactly as they do on Forge's
    // consumerNetworkThread. Do not move the decode behind the executor — the buffer is released
    // as soon as the receiver returns.
    private static void receive(FriendlyByteBuf buf, java.util.concurrent.Executor executor, ServerPlayer sender, boolean clientSide) {
        int discriminator = buf.readVarInt();
        Entry<?> entry = ENTRIES.get(discriminator);
        entry.handle(entry.decoder().apply(buf), new FabricContext(executor, sender, clientSide));
    }

    // Serverbound half. Called from the common Fabric entrypoint, after registerMessages().
    public static void registerServerReceiver() {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(CHANNEL_NAME,
                (server, player, listener, buf, responseSender) -> receive(buf, server, player, false));
    }

    // Clientbound half. Called from the CLIENT Fabric entrypoint only — every type this method
    // names is client-only, and a method body's constant-pool entries resolve when the body first
    // runs, so merely having it on a common class is safe on a dedicated server.
    public static void registerClientReceiver() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(CHANNEL_NAME,
                (client, listener, buf, responseSender) -> receive(buf, client, null, true));
    }

    public static void sendToServer(Object message) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(CHANNEL_NAME, encode(message));
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, CHANNEL_NAME, encode(message));
    }

    private record FabricContext(java.util.concurrent.Executor executor, ServerPlayer sender, boolean clientSide)
            implements ACNetworkContext {

        @Override
        public void enqueueWork(Runnable runnable) {
            executor.execute(runnable);
        }

        @Override
        public ServerPlayer getSender() {
            return sender;
        }

        @Override
        public boolean isClientSide() {
            return clientSide;
        }

        @Override
        public void setPacketHandled(boolean handled) {
            // Fabric has no such concept — a packet that reaches its receiver is handled.
        }
    }

    *///?}

    //? if fabric && >=1.20.5 {

    /*// The same one-channel, one-discriminator design as every other arm, carried on vanilla's
    // payload types. fabric-api followed vanilla's 1.20.5 rework: the raw-buffer send and receive
    // overloads the arm above uses were replaced by CustomPacketPayload + StreamCodec, a payload
    // type has to be declared per direction before it may travel, and a receiver is keyed by that
    // type rather than by a bare channel id. That is the identical rework NeoForge went through, so
    // this arm is the NeoForge one with fabric-api's registration and send calls in place of the
    // registrar and PacketDistributor. The wire format — one varint discriminator followed by
    // whatever the message's own write() emits — and the twenty-two message classes are unchanged.
    //
    // Handlers run on the main thread: fabric-api schedules a play payload handler onto the
    // server's or the client's own executor before calling it. The messages that call enqueueWork
    // anyway still behave, because a BlockableEventLoop runs a task immediately when the caller is
    // already on its thread.

    private record Entry<MSG>(BiConsumer<MSG, FriendlyByteBuf> encoder,
                              Function<FriendlyByteBuf, MSG> decoder,
                              BiConsumer<MSG, ACNetworkContext> handler) {

        @SuppressWarnings("unchecked")
        void encode(Object message, FriendlyByteBuf buf) {
            encoder.accept((MSG) message, buf);
        }

        @SuppressWarnings("unchecked")
        void handle(Object message, ACNetworkContext context) {
            handler.accept((MSG) message, context);
        }
    }

    private static final java.util.List<Entry<?>> ENTRIES = new java.util.ArrayList<>();
    private static final java.util.Map<Class<?>, Integer> DISCRIMINATORS = new java.util.HashMap<>();

    public static <MSG> void register(Class<MSG> type,
                                      BiConsumer<MSG, FriendlyByteBuf> encoder,
                                      Function<FriendlyByteBuf, MSG> decoder,
                                      BiConsumer<MSG, ACNetworkContext> handler) {
        DISCRIMINATORS.put(type, nextId++);
        ENTRIES.add(new Entry<>(encoder, decoder, handler));
    }

    private record ACPayload(int discriminator, Object message)
            implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

        static final Type<ACPayload> TYPE = new Type<>(CHANNEL_NAME);

        // A RegistryFriendlyByteBuf is a FriendlyByteBuf, so the messages' own write/read pairs take
        // it unchanged — they simply never reach for the registry access it adds.
        static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ACPayload> CODEC =
                net.minecraft.network.codec.StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeVarInt(payload.discriminator());
                            ENTRIES.get(payload.discriminator()).encode(payload.message(), buf);
                        },
                        buf -> {
                            int discriminator = buf.readVarInt();
                            return new ACPayload(discriminator, ENTRIES.get(discriminator).decoder().apply(buf));
                        });

        @Override
        public Type<ACPayload> type() {
            return TYPE;
        }
    }

    private static ACPayload wrap(Object message) {
        Integer discriminator = DISCRIMINATORS.get(message.getClass());
        if (discriminator == null) {
            throw new IllegalArgumentException("Unregistered message type " + message.getClass().getName());
        }
        return new ACPayload(discriminator, message);
    }

    // Serverbound half, plus the type registration for both directions. Called from the common
    // Fabric entrypoint, so it is the one of the pair that runs on a dedicated server as well —
    // which is where both directions have to be declared: PayloadTypeRegistry is a common registry,
    // and a server that has not declared the clientbound type is not allowed to send it.
    //
    // registerMessages() first because the codec indexes straight into that table and nothing
    // orders this call against common setup; it is guarded, so the second call is free.
    //
    // ⚠️ The server is reached through Level#getServer(), not through ServerPlayer's `server` field
    // and not through Entity#getServer(): the field went public -> private at 1.21.6 and the
    // entity-side convenience getter was DELETED at 1.21.9, while Level has declared getServer()
    // returning MinecraftServer across the whole range. So the two-hop spelling is the only one that
    // needs no gate anywhere — and it is exactly what Entity#getServer() used to do.
    public static void registerServerReceiver() {
        registerMessages();
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(ACPayload.TYPE, ACPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(ACPayload.TYPE, ACPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(ACPayload.TYPE,
                (payload, context) -> ENTRIES.get(payload.discriminator())
                        .handle(payload.message(), new FabricContext(context.player().level().getServer(), context.player(), false)));
    }

    // Clientbound half. Called from the CLIENT Fabric entrypoint only — every type this method
    // names is client-only, and a method body's constant-pool entries resolve when the body first
    // runs, so merely having it on a common class is safe on a dedicated server.
    public static void registerClientReceiver() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(ACPayload.TYPE,
                (payload, context) -> ENTRIES.get(payload.discriminator())
                        .handle(payload.message(), new FabricContext(context.client(), null, true)));
    }

    public static void sendToServer(Object message) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(wrap(message));
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, wrap(message));
    }

    private record FabricContext(java.util.concurrent.Executor executor, ServerPlayer sender, boolean clientSide)
            implements ACNetworkContext {

        @Override
        public void enqueueWork(Runnable runnable) {
            executor.execute(runnable);
        }

        @Override
        public ServerPlayer getSender() {
            return sender;
        }

        @Override
        public boolean isClientSide() {
            return clientSide;
        }

        @Override
        public void setPacketHandled(boolean handled) {
            // Fabric has no such concept — a packet that reaches its receiver is handled.
        }
    }

    *///?}

    /** Guards against the double call the NeoForge and Fabric payload arms make: registering the
     *  payload type primes the table the codec indexes into. */
    private static boolean messagesRegistered = false;

    /** Registers every Alex's Caves and Citadel message. Call once, during mod setup. */
    public static void registerMessages() {
        if (messagesRegistered) {
            return;
        }
        messagesRegistered = true;
        register(SpelunkeryTableChangeMessage.class, SpelunkeryTableChangeMessage::write, SpelunkeryTableChangeMessage::read, SpelunkeryTableChangeMessage::handle);
        register(SpelunkeryTableCompleteTutorialMessage.class, SpelunkeryTableCompleteTutorialMessage::write, SpelunkeryTableCompleteTutorialMessage::read, SpelunkeryTableCompleteTutorialMessage::handle);
        register(PlayerJumpFromMagnetMessage.class, PlayerJumpFromMagnetMessage::write, PlayerJumpFromMagnetMessage::read, PlayerJumpFromMagnetMessage::handle);
        register(MultipartEntityMessage.class, MultipartEntityMessage::write, MultipartEntityMessage::read, MultipartEntityMessage::handle);
        register(MountedEntityKeyMessage.class, MountedEntityKeyMessage::write, MountedEntityKeyMessage::read, MountedEntityKeyMessage::handle);
        register(UpdateEffectVisualityEntityMessage.class, UpdateEffectVisualityEntityMessage::write, UpdateEffectVisualityEntityMessage::read, UpdateEffectVisualityEntityMessage::handle);
        register(PossessionKeyMessage.class, PossessionKeyMessage::write, PossessionKeyMessage::read, PossessionKeyMessage::handle);
        register(UpdateItemTagMessage.class, UpdateItemTagMessage::write, UpdateItemTagMessage::read, UpdateItemTagMessage::handle);
        register(BeholderSyncMessage.class, BeholderSyncMessage::write, BeholderSyncMessage::read, BeholderSyncMessage::handle);
        register(BeholderRotateMessage.class, BeholderRotateMessage::write, BeholderRotateMessage::read, BeholderRotateMessage::handle);
        register(ArmorKeyMessage.class, ArmorKeyMessage::write, ArmorKeyMessage::read, ArmorKeyMessage::handle);
        register(WorldEventMessage.class, WorldEventMessage::write, WorldEventMessage::read, WorldEventMessage::handle);
        register(UpdateCaveBiomeMapTagMessage.class, UpdateCaveBiomeMapTagMessage::write, UpdateCaveBiomeMapTagMessage::read, UpdateCaveBiomeMapTagMessage::handle);
        register(UpdateBossEruptionStatus.class, UpdateBossEruptionStatus::write, UpdateBossEruptionStatus::read, UpdateBossEruptionStatus::handle);
        register(UpdateBossBarMessage.class, UpdateBossBarMessage::write, UpdateBossBarMessage::read, UpdateBossBarMessage::handle);
        register(SundropRainbowMessage.class, SundropRainbowMessage::write, SundropRainbowMessage::read, SundropRainbowMessage::handle);
        com.github.alexmodguy.alexscaves.citadel.Citadel.registerMessages();
    }
}
