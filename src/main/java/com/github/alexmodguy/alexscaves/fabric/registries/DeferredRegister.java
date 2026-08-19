package com.github.alexmodguy.alexscaves.fabric.registries;

import com.github.alexmodguy.alexscaves.fabric.ModBus;
import com.github.alexmodguy.alexscaves.server.misc.ACIdFactories;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Fabric stand-in for Forge's deferred-registration helper.
 *
 * <p><b>Why this exists.</b> Two dozen files in this tree declare one, and between them they make
 * roughly seven hundred {@code register(name, supplier)} calls — every block, item, entity type,
 * sound, particle, effect, menu and structure piece the mod has. Fabric has no deferred
 * registration at all: its registries are immediate, you call {@code Registry.register} and the
 * entry is live, so there is no loader type to rename those declarations onto. This class
 * reproduces exactly the slice of the Forge API the mod uses, under the mod's own package, and the
 * Fabric-only {@code !fab-deferredregister} rule re-points the type name at it. All the consumers
 * stay byte-identical across all three loaders — the same relocated-compat-namespace pattern as
 * the vendored Citadel classes and {@code client/render/compat/**}.
 *
 * <p><b>The API surface is deliberately closed</b>, exactly as wide as this mod's call sites:
 * {@link #create}, {@link #register(String, Supplier)}, {@link #getEntries()}, the no-arg
 * {@link #register()} flush and its {@link #register(ModBus)} token-taking twin — plus
 * {@link #unregistered} and {@link #entityDataSerializers}, the two members with no counterpart on
 * the other loaders. Each documents its own reason for existing, and they share one: a register the
 * loaders key by a <i>loader-owned</i> registry has no key to be given here, so the difference moves
 * into the factory call and all its consumers stay identical on all three loaders. Nothing here ever
 * asks a handle for its registry key — every handle in this tree is declared as a plain
 * {@code java.util.function.Supplier}, never Forge's {@code RegistryObject} or NeoForge's
 * {@code DeferredHolder} (the places that would have wanted a key spell the {@code ResourceKey}
 * out by hand; see {@code ACPOIRegistry}). If a future call site needs more, widen this class —
 * never the Stonecutter rule.
 *
 * <p><b>The one behavioural difference that matters: ORDER.</b> On Forge and NeoForge the loader
 * decides when each registry is filled and resolves the cross-references for you. Here
 * {@link #register()} runs the suppliers on the spot, so a registry must be flushed <i>after</i>
 * everything it dereferences — item suppliers call {@code ACBlockRegistry.X.get()}, the creative
 * tab walks the item registry, and {@code ACFoods} reaches into {@code ACEffectRegistry}. That
 * order is already written down once, in {@code AlexsCaves}'s constructor, which runs on this
 * loader too (the Fabric entrypoint simply news it up); {@link #register(ModBus)} makes each of
 * those ~28 lines the flush, so there is no second copy of the order to keep in sync. A handle
 * used before its flush throws with its own name in the message rather than returning null, so
 * getting the order wrong fails loudly instead of as an NPE somewhere else entirely.
 *
 * <p>Ids go through {@code ACIdFactories} rather than being built directly, for the reason that
 * class documents: the factory this needs is a Forge patch below 1.21 and a private constructor
 * from 1.21, so no single spelling compiles across the range.
 */
public final class DeferredRegister<T> {

    @Nullable
    private final ResourceKey<? extends Registry<T>> registryKey;
    @Nullable
    private final java.util.function.BiConsumer<net.minecraft.resources.ResourceLocation, ? super T> sink;
    private final String modid;
    private final List<Entry<? extends T>> entries = new ArrayList<>();
    private boolean flushed;

    private DeferredRegister(@Nullable ResourceKey<? extends Registry<T>> registryKey,
                             @Nullable java.util.function.BiConsumer<net.minecraft.resources.ResourceLocation, ? super T> sink,
                             String modid) {
        this.registryKey = registryKey;
        this.sink = sink;
        this.modid = modid;
    }

    public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> registryKey, String modid) {
        return new DeferredRegister<>(registryKey, null, modid);
    }

    /**
     * A register whose entries are built on flush and handed back through their handles, but put
     * into no game registry — because on this loader there is no registry for them to go into.
     *
     * <p>Exactly one thing needs it: {@code ACFluidRegistry}'s fluid types. On the other two loaders
     * a {@code FluidType} is a registered object with an id, and the mod declares a register for it
     * keyed by a loader-owned registry key; here the type is this mod's own stand-in and the only
     * thing anything ever does with one is dereference the handle. So rather than gate the
     * declaration <i>and</i> the flush line in {@code AlexsCaves}'s constructor into loader arms,
     * this keeps both shapes identical and moves the difference into the factory call.
     *
     * <p>The entries are still built at flush time and in flush order, which is the load-bearing
     * part: acid's fluid type dereferences {@code ACSoundRegistry} while it is built, and a handle
     * used before its flush still throws with its own name. The id the entry would have been
     * registered under is simply not used — nothing on this loader can look one of these up by id,
     * which is exactly the property that makes an unregistered register honest rather than lossy.
     */
    public static <T> DeferredRegister<T> unregistered(String modid) {
        return new DeferredRegister<>(null, null, modid);
    }

    /**
     * A register for entity data serializers, which are not registry content on this loader.
     *
     * <p>On Forge and NeoForge the serializer list is a loader-owned registry
     * ({@code ForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS}) and a register is keyed by it like any
     * other. Vanilla's list is not a {@link Registry} at all — it is the static incremental id map in
     * {@link net.minecraft.network.syncher.EntityDataSerializers}, whose index is what goes over the
     * wire — so there is no key to hand {@link #create} and a sentinel one would be a lie. The entry
     * is therefore built like an {@link #unregistered} one and then pushed into vanilla's own list.
     *
     * <p><b>Through which of three doors depends on the Fabric API build, not on vanilla.</b> Below
     * 1.21.5 the push is vanilla's own {@code EntityDataSerializers.registerSerializer}. From
     * <b>1.21.5</b> fabric-object-builder-api-v1 mixes a hard refusal into that method — <i>"Tried to
     * register tracked data handler … using TrackedDataHandlerRegistry.register. This is not allowed
     * as it can lead to desynchronization issues"</i>, thrown out of the {@code main} entrypoint, so
     * it is a boot failure rather than a warning. Its point is that vanilla's list hands out
     * incremental network ids in registration order, which two mods loading in different orders on
     * client and server would disagree about; Fabric's registry keys the serializer by id and syncs
     * it. That is why this takes the entry's {@link net.minecraft.resources.ResourceLocation} —
     * {@code alexscaves:compound_tag} — and why the sink is a two-argument one.
     *
     * <p>⚠ The class it points you at is <b>renamed halfway through the range</b>, which is why there
     * are three arms and not two. It is {@code FabricTrackedDataRegistry} (a Yarn-era name, kept even
     * in the Mojmap-facing API) from object-builder <b>21.1.2</b> — 1.21.5's pinned fabric-api
     * 0.128.2 — up to and including 1.21.11's 21.1.40, and {@code FabricEntityDataRegistry} from
     * <b>23.0.13</b>, which is 26.1's 0.145.1. Both spell {@code register(Identifier,
     * EntityDataSerializer&lt;?&gt;)}, so only the owner moves. 1.21.4's pinned 18.0.14 has neither
     * class and no refusal, so the vanilla call is still correct below 1.21.5.
     *
     * <p>⚠⚠ Neither boundary can be inferred from a vanilla change sitting nearby — this gate said
     * {@code >=26.1} for a whole milestone because that is where the <i>rename</i> was found, and
     * every Fabric node from 1.21.5 up died at {@code onInitialize} the first time one was booted.
     * <b>It tracks the Fabric API build.</b> Enumerate the nested
     * {@code fabric-object-builder-api-v1} jar of each pinned bundle rather than reasoning about it.
     *
     * <p>Registering really is required rather than tidy: {@code SynchedEntityData.defineId} asks the
     * list for a serializer's network id and throws when it has none, so a serializer that skipped
     * this would take down the first entity class that names it.
     *
     * <p><b>Order matters here in the same way it does for every other register</b>, and rather more
     * sharply: the flush has to happen before any entity class initialises, since {@code defineId}
     * runs in those classes' static initialisers. That is already true of where
     * {@code ACEntityDataRegistry}'s flush sits in {@code AlexsCaves}'s constructor.
     */
    public static DeferredRegister<net.minecraft.network.syncher.EntityDataSerializer<?>> entityDataSerializers(String modid) {
        //? if >=26.1 {
        /*return new DeferredRegister<net.minecraft.network.syncher.EntityDataSerializer<?>>(null,
                (id, serializer) -> net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityDataRegistry
                        .register(id, serializer), modid);
        *///?} elif >=1.21.5 {
        /*return new DeferredRegister<net.minecraft.network.syncher.EntityDataSerializer<?>>(null,
                (id, serializer) -> net.fabricmc.fabric.api.object.builder.v1.entity.FabricTrackedDataRegistry
                        .register(id, serializer), modid);
        *///?} else {
        return new DeferredRegister<net.minecraft.network.syncher.EntityDataSerializer<?>>(null,
                (id, serializer) -> net.minecraft.network.syncher.EntityDataSerializers.registerSerializer(serializer), modid);
        //?}
    }

    /**
     * Generic shape copied from Forge's, so a {@code DeferredRegister<EntityType<?>>} still infers
     * {@code Supplier<EntityType<TremorzillaEntity>>} at the call site and the ~700 declarations
     * keep their precise types. {@code ACDeferredRegister} delegates straight to this.
     */
    public <I extends T> Supplier<I> register(String name, Supplier<? extends I> supplier) {
        if (flushed) {
            throw new IllegalStateException("Registered " + modid + ":" + name + " after "
                    + (registryKey == null ? "its unregistered register" : registryKey.toString()) + " was flushed");
        }
        Entry<I> entry = new Entry<>(name, supplier);
        entries.add(entry);
        return entry;
    }

    /**
     * Performs the real registration — the moment Forge's mod bus would have fired its
     * {@code RegisterEvent} for this registry.
     *
     * <p>A register with no entries is skipped <i>without ever looking its registry up</i>. That is
     * not an optimisation: several registers in this tree are declared unconditionally but only
     * populated below some MC version, because vanilla turned the registry into datapack content
     * ({@code ENCHANTMENT} at 1.21, {@code FROG_VARIANT} at 1.21.5). Those keys are not in
     * {@link BuiltInRegistries} at all on a modern node, so an eager lookup would throw for a
     * registry the mod deliberately no longer uses.
     */
    public void register() {
        if (flushed || entries.isEmpty()) {
            flushed = true;
            return;
        }
        flushed = true;
        if (registryKey == null) {
            for (Entry<? extends T> entry : entries) {
                T created = entry.resolveUnregistered();
                if (sink != null) {
                    sink.accept(ACIdFactories.of(modid, entry.name), created);
                }
            }
            return;
        }
        Registry<T> registry = lookupRegistry();
        for (Entry<? extends T> entry : entries) {
            entry.resolve(registry, modid);
        }
    }

    /**
     * The shape {@code AlexsCaves}'s constructor spells ~28 times, once per registry. On the other
     * two loaders the argument is the mod event bus and the call only schedules the fill; here the
     * token carries nothing (see {@link ModBus}) and the fill happens immediately. Taking it anyway
     * is what keeps those call sites identical on all three loaders — and it makes that constructor
     * the single place the load-bearing flush order is written down.
     */
    public void register(ModBus bus) {
        register();
    }

    @SuppressWarnings("unchecked")
    public Collection<Supplier<T>> getEntries() {
        return Collections.unmodifiableCollection((List<Supplier<T>>) (List<?>) entries);
    }

    /**
     * {@link BuiltInRegistries} exposes no key-to-registry lookup that keeps the element type, so
     * this walks the registry-of-registries and matches on {@link Registry#key()}. Cheap enough: it
     * happens once per register, i.e. under thirty times per launch.
     */
    @SuppressWarnings("unchecked")
    private Registry<T> lookupRegistry() {
        for (Registry<?> candidate : BuiltInRegistries.REGISTRY) {
            if (candidate.key().equals(registryKey)) {
                return (Registry<T>) candidate;
            }
        }
        throw new IllegalStateException("No built-in registry for " + registryKey
                + " — it is datapack content on this version, so nothing may be registered to it from code");
    }

    private static final class Entry<I> implements Supplier<I> {

        private final String name;
        private final Supplier<? extends I> factory;
        private I value;

        private Entry(String name, Supplier<? extends I> factory) {
            this.name = name;
            this.factory = factory;
        }

        @Override
        public I get() {
            if (value == null) {
                throw new IllegalStateException("Used " + name + " before its registry was flushed — "
                        + "check the flush order in the AlexsCaves constructor");
            }
            return value;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void resolve(Registry<?> registry, String modid) {
            I created = factory.get();
            Registry.register((Registry) registry, ACIdFactories.of(modid, name), created);
            this.value = created;
        }

        private I resolveUnregistered() {
            this.value = factory.get();
            return this.value;
        }
    }
}
