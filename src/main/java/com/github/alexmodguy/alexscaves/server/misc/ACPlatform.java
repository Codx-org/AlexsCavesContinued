package com.github.alexmodguy.alexscaves.server.misc;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.jetbrains.annotations.Nullable;

/**
 * Vanilla and loader APIs that moved between the Minecraft versions this mod spans, funnelled
 * through one place so the per-version Stonecutter conditionals live here instead of being
 * scattered over a hundred entity and block classes.
 *
 * <p>The sibling mod Alex's Mobs Continued has the same class ({@code AMPlatform}); where a helper
 * exists in both, the body is deliberately identical.
 */
public class ACPlatform {

    /**
     * The packet that tells a client about a newly tracked entity.
     *
     * <p>Upstream routed 33 entities through {@code NetworkHooks.getEntitySpawningPacket}, which
     * Forge deleted in 1.20.2 (it lives on as {@code ForgeHooks.getEntitySpawnPacket}). NeoForge
     * has no hook at all — its {@code IEntityWithComplexSpawn} payload rides along with the vanilla
     * add-entity packet, sent separately by {@code ServerEntity} — and Fabric never had one.
     *
     * <p><b>The non-Forge arms must NOT call {@code entity.getAddEntityPacket()}.</b> Every caller
     * reaches this method <i>from</i> its own override of exactly that method, so delegating back
     * is unbounded mutual recursion — a {@code StackOverflowError} the first time the entity is
     * sent to a client. Build the vanilla packet directly, which is what
     * {@code Entity#getAddEntityPacket} itself does.
     *
     * <p>Nothing in Alex's Caves implements {@code IEntityAdditionalSpawnData} / writes spawn data,
     * so there is nothing for the vanilla packet to lose. (Measured: no {@code writeSpawnData} or
     * {@code readSpawnData} anywhere in the tree.)
     */
    @SuppressWarnings("unchecked")
    public static Packet<ClientGamePacketListener> getEntitySpawningPacket(Entity entity) {
        //? if forge && >=1.20.2 {
        /*return net.minecraftforge.common.ForgeHooks.getEntitySpawnPacket(entity);
        *///?}
        //? if !forge {
        /*return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(entity);
        *///?}
        //? if forge && <1.20.2
        return (Packet<ClientGamePacketListener>) net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(entity);
    }

    /**
     * 1.20.3 made {@code Block#codec()} abstract so blocks can be described in datapacks.
     * None of Alex's Caves' ~250 blocks is codec-serialised — no worldgen feature or datapack
     * references one by value — so they all return this placeholder rather than each growing a
     * real codec.
     */
    @SuppressWarnings("unchecked")
    public static <B extends Block> com.mojang.serialization.MapCodec<B> unsupportedBlockCodec() {
        return (com.mojang.serialization.MapCodec<B>) UNSUPPORTED_BLOCK_CODEC;
    }

    private static final com.mojang.serialization.MapCodec<?> UNSUPPORTED_BLOCK_CODEC =
            com.mojang.serialization.MapCodec.unit(() -> {
                throw new UnsupportedOperationException("Alex's Caves blocks are not codec-serializable");
            });

    /**
     * 1.20.3 replaced {@code BlockBehaviour.Properties.copy(Block)} with
     * {@code ofFullCopy(BlockBehaviour)}.
     */
    public static BlockBehaviour.Properties copyProperties(BlockBehaviour from) {
        //? if >=1.20.3
        /*return BlockBehaviour.Properties.ofFullCopy(from);*/
        //? if <1.20.3
        return BlockBehaviour.Properties.copy((Block) from);
    }

    /**
     * 1.20.2 changed {@code LootItemConditions.orConditions} from varargs to a {@code List}.
     */
    public static <T> java.util.function.Predicate<T> orConditions(java.util.function.Predicate<T>[] conditions) {
        // 1.20.5 dropped the loot-specific helper; the generic one on Util does the same thing.
        //? if >=1.20.5
        /*return net.minecraft.Util.anyOf(java.util.List.of(conditions));*/
        //? if >=1.20.2 && <1.20.5
        /*return net.minecraft.world.level.storage.loot.predicates.LootItemConditions.orConditions(java.util.List.of(conditions));*/
        //? if <1.20.2
        return net.minecraft.world.level.storage.loot.predicates.LootItemConditions.orConditions(conditions);
    }

    /**
     * 1.20.2 gave {@code BucketPickup#pickupBlock} a (nullable) {@code Player} so bucket-empty
     * game events can be attributed. Alex's Caves drains fluids without a player.
     */
    public static net.minecraft.world.item.ItemStack pickupBlock(
            net.minecraft.world.level.block.BucketPickup block,
            net.minecraft.world.level.LevelAccessor level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state) {
        //? if >=1.20.2
        /*return block.pickupBlock(null, level, pos, state);*/
        //? if <1.20.2
        return block.pickupBlock(level, pos, state);
    }

    /**
     * The passenger's own seating offset. 1.20.2 reworked entity attachment points:
     * {@code getMyRidingOffset()} gained the vehicle parameter (and returns float); 1.20.5
     * finished the job by turning it into an attachment point subtracted from the vehicle's seat
     * position, so the old scalar is its negated Y.
     */
    public static double myRidingOffset(Entity passenger, Entity vehicle) {
        //? if >=1.20.5
        /*return -passenger.getVehicleAttachmentPoint(vehicle).y;*/
        //? if >=1.20.2 && <1.20.5
        /*return passenger.getMyRidingOffset(vehicle);*/
        //? if <1.20.2
        return passenger.getMyRidingOffset();
    }

    /**
     * Looks a configured feature up in the level's dynamic registries.
     */
    public static net.minecraft.core.Holder<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> configuredFeature(
            net.minecraft.server.level.ServerLevel serverLevel,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> key) {
        if (key == null) {
            return null;
        }
        return serverLevel.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE)
                .getHolder(key).orElse(null);
    }

    /**
     * Fires the loader's "a sapling is about to grow into this feature" event, which lets other mods
     * substitute the feature or veto the growth entirely.
     *
     * @return the feature to place — possibly one a listener swapped in — or null if growth was
     *         vetoed or the feature did not resolve.
     */
    public static net.minecraft.core.Holder<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> onSaplingGrow(
            net.minecraft.world.level.LevelAccessor level,
            net.minecraft.util.RandomSource randomSource,
            net.minecraft.core.BlockPos pos,
            net.minecraft.core.Holder<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> feature) {
        //? if forge {
        net.minecraftforge.event.level.SaplingGrowTreeEvent event =
                net.minecraftforge.event.ForgeEventFactory.blockGrowFeature(level, randomSource, pos, feature);
        if (event.getResult() == net.minecraftforge.eventbus.api.Event.Result.DENY) {
            return null;
        }
        return event.getFeature();
        //?}
        //? if !forge
        /*return feature;*/
    }

    /**
     * Stamps a loot table onto the container at {@code pos} so it fills on first open.
     *
     * <p>1.20.3 pulled the loot-table plumbing out of {@code RandomizableContainerBlockEntity} into
     * the new {@code RandomizableContainer} interface, where the static helper is called
     * {@code setBlockEntityLootTable}.
     *
     * <p>1.20.3, not 1.20.2 — javap on the vanilla 1.20.2 jar shows the old shape still there.
     * Neither Forge nor NeoForge publishes a 1.20.2 or a 1.20.3 build, so this tree's walk went
     * straight from 1.20.1 to 1.20.4 and could not tell the two apart until Fabric reached them.
     */
    public static void setBlockEntityLootTable(
            net.minecraft.world.level.BlockGetter level,
            net.minecraft.util.RandomSource randomSource,
            net.minecraft.core.BlockPos pos,
            net.minecraft.resources.ResourceLocation lootTable) {
        //? if >=1.20.3
        /*net.minecraft.world.RandomizableContainer.setBlockEntityLootTable(level, randomSource, pos, ACCompat.lootKey(lootTable));*/
        //? if <1.20.3
        net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity.setLootTable(level, randomSource, pos, lootTable);
    }

    /**
     * A plain vanilla {@link net.minecraft.world.level.Explosion}, built the same way on every
     * version.
     *
     * <p>Two things in Alex's Caves need one. Its four bespoke blasts — {@code MineExplosion},
     * {@code TephraExplosion}, {@code FrostmintExplosion}, {@code TotemExplosion} — re-implement
     * {@code Explosion} rather than subclass it, so from 1.20.2 up, where the entity is told which
     * explosion is asking, they have nothing to hand {@link #ignoreExplosion}; they mirror themselves
     * into one of these, once per blast, and reuse it across the entity loop. Separately,
     * {@code NuclearExplosionEntity} and {@code TremorzillaEntity} keep a dummy around purely to ask
     * blocks for their explosion resistance.
     *
     * <p>The ctor overload used here is the one that has survived unchanged; the {@code List<BlockPos>}
     * one those two dummies used upstream did not.
     *
     * <p>1.21.2 turned {@code Explosion} itself into an interface and moved the implementation to
     * {@code ServerExplosion}, which — as the name says — only exists on a {@code ServerLevel}. That
     * is no restriction in practice: all six callers are inside a {@code !isClientSide} branch or on
     * an entity that only blasts server-side. Off a server level there is nothing to build, so the
     * result is {@code null} from then on and the two consumers below tolerate it.
     */
    @org.jetbrains.annotations.Nullable
    public static net.minecraft.world.level.Explosion explosion(
            net.minecraft.world.level.Level level,
            @org.jetbrains.annotations.Nullable Entity source,
            double x, double y, double z, float radius,
            net.minecraft.world.level.Explosion.BlockInteraction blockInteraction) {
        //? if >=1.21.2 {
        /*return level instanceof net.minecraft.server.level.ServerLevel serverLevel
                ? new net.minecraft.world.level.ServerExplosion(serverLevel, source, null, null, new net.minecraft.world.phys.Vec3(x, y, z), radius, false, blockInteraction)
                : null;
        *///?} else {
        return new net.minecraft.world.level.Explosion(level, source, x, y, z, radius, false, blockInteraction);
        //?}
    }

    /**
     * Whether an entity opts out of explosion damage. 1.20.3 gave {@code Entity#ignoreExplosion} the
     * explosion itself, so an entity can decide per blast.
     *
     * <p>1.20.3, not 1.20.2 — javap on the vanilla 1.20.2 jar shows the old shape still there.
     * Neither Forge nor NeoForge publishes a 1.20.2 or a 1.20.3 build, so this tree's walk went
     * straight from 1.20.1 to 1.20.4 and could not tell the two apart until Fabric reached them.
     *
     * @param explosion the blast doing the asking; for Alex's Caves' own explosion classes, an
     *                  {@link #explosion} mirroring it. From 1.21.2 that mirror is {@code null} off a
     *                  server level, and an entity that cannot be shown the blast is asked nothing —
     *                  {@code false}, the answer every vanilla entity but a few gives anyway.
     */
    public static boolean ignoreExplosion(Entity entity, @org.jetbrains.annotations.Nullable net.minecraft.world.level.Explosion explosion) {
        //? if >=1.20.3
        /*return explosion != null && entity.ignoreExplosion(explosion);*/
        //? if <1.20.3
        return entity.ignoreExplosion();
    }

    /**
     * Where the dispenser firing this behaviour is, what level it is in, and what state it has.
     *
     * <p>1.20.2 moved {@code BlockSource} from {@code net.minecraft.core} to
     * {@code net.minecraft.core.dispenser} and turned it into a record, so the three getters lost
     * their {@code get} prefixes. The <i>type</i> still has to be named by the two behaviours that
     * take one — they gate their import — but the accessors live here.
     */
    //? if >=1.20.2 {
    /*public static net.minecraft.core.BlockPos dispenserPos(net.minecraft.core.dispenser.BlockSource source) {
        return source.pos();
    }

    public static net.minecraft.world.level.Level dispenserLevel(net.minecraft.core.dispenser.BlockSource source) {
        return source.level();
    }

    public static net.minecraft.world.level.block.state.BlockState dispenserState(net.minecraft.core.dispenser.BlockSource source) {
        return source.state();
    }
    *///?}
    //? if <1.20.2 {
    public static net.minecraft.core.BlockPos dispenserPos(net.minecraft.core.BlockSource source) {
        return source.getPos();
    }

    public static net.minecraft.world.level.Level dispenserLevel(net.minecraft.core.BlockSource source) {
        return source.getLevel();
    }

    public static net.minecraft.world.level.block.state.BlockState dispenserState(net.minecraft.core.BlockSource source) {
        return source.getBlockState();
    }
    //?}

    /**
     * How long one tick is meant to take. {@code MinecraftServer.MS_PER_TICK} was a constant of
     * exactly this value until 1.20.2 removed it (the server's tick length became a variable, driven
     * by {@code /tick rate}). Citadel's tick-rate tracker only ever wanted the nominal figure.
     */
    public static final int MS_PER_TICK = 50;

    /**
     * Records who threw an item, for the two-second pickup delay and for kill attribution.
     * 1.20.3 changed {@code ItemEntity#setThrower} from taking the thrower's UUID to taking the
     * thrower itself.
     *
     * <p>1.20.3, not 1.20.2 — javap on the vanilla 1.20.2 jar shows the old shape still there.
     * Neither Forge nor NeoForge publishes a 1.20.2 or a 1.20.3 build, so this tree's walk went
     * straight from 1.20.1 to 1.20.4 and could not tell the two apart until Fabric reached them.
     */
    public static void setThrower(net.minecraft.world.entity.item.ItemEntity itemEntity, Entity thrower) {
        //? if >=1.20.3
        /*itemEntity.setThrower(thrower);*/
        //? if <1.20.3
        itemEntity.setThrower(thrower.getUUID());
    }

    /**
     * How far above the vehicle's own position its passengers sit.
     *
     * <p>1.20.2 deleted {@code getPassengersRidingOffset()} — the scalar every vehicle used to
     * override — and replaced it with {@code getPassengerRidingPosition(Entity)}, which answers with
     * a position in world space. The three callers here want the old relative number, and each is
     * inside a vehicle asking about its own seat, so subtracting the vehicle's Y recovers it.
     */
    public static double passengersRidingOffset(Entity vehicle, Entity passenger) {
        //? if >=1.20.2
        /*return vehicle.getPassengerRidingPosition(passenger).y - vehicle.getY();*/
        //? if <1.20.2
        return vehicle.getPassengersRidingOffset();
    }

    /**
     * Loads this world's copy of a {@link net.minecraft.world.level.saveddata.SavedData}, creating
     * it if the world has none yet.
     *
     * <p>1.20.2 folded the loader and the constructor into a {@code SavedData.Factory} record
     * (whose third component is the {@code DataFixTypes} to run on load — neither of the two saved
     * data types here is data-fixed, so it is null). 1.20.5 then widened the loader component from a
     * {@code Function<CompoundTag, T>} to a {@code BiFunction} taking the registry lookup as well;
     * neither of this mod's two saved data types needs it, so it is adapted away here and the
     * parameter stays a plain {@code Function} on every version.
     *
     * <p>1.21.5 replaced the factory record with {@link
     * net.minecraft.world.level.saveddata.SavedDataType} and made persistence codec-based —
     * {@code SavedData#save(CompoundTag)} is gone, so a subclass no longer writes its own tag on the
     * way out. Both of this mod's types keep their hand-rolled NBT (they are read back by old saves,
     * and a codec rewrite would change the format), so the {@code saver} handed in here is folded
     * together with the loader into a {@code CompoundTag.CODEC.xmap} — the identity codec, which
     * hands the two functions exactly the tag they always got.
     *
     * <p>The trailing {@code DataFixTypes} is null for the same reason it was on 1.20.2, but it has
     * to be spelled out: one loader added three-argument convenience constructors that default it,
     * the other did not, and the four-argument {@code (String, Supplier, Codec, DataFixTypes)} form
     * is the one both ship. Null is safe on both — {@code DimensionDataStorage#readSavedData} is the
     * only reader of the component and it passes the value straight to {@code readTagFromDisk},
     * which returns the tag unfixed when it is null (checked in the bytecode of both loaders).
     *
     * <p>26 retyped the id from a {@code String} to a {@code ResourceLocation}, and the file it
     * names is {@code data/<namespace>/<path>.dat} rather than {@code data/<name>.dat} — vanilla's
     * own saved data moved into {@code data/minecraft/} with it, so there is no old layout to keep.
     * The mod's namespace is used rather than {@code withDefaultNamespace}, which would drop both of
     * these files into vanilla's folder.
     *
     * @param saver writes a {@code T} back out; only consulted from 1.21.5, where it replaces the
     *              {@code save} override the older versions call instead
     */
    public static <T extends net.minecraft.world.level.saveddata.SavedData> T computeIfAbsent(
            net.minecraft.world.level.storage.DimensionDataStorage storage,
            java.util.function.Function<net.minecraft.nbt.CompoundTag, T> loader,
            java.util.function.Function<T, net.minecraft.nbt.CompoundTag> saver,
            java.util.function.Supplier<T> factory,
            String identifier) {
        //? if >=26 {
        /*com.mojang.serialization.Codec<T> acCodec = net.minecraft.nbt.CompoundTag.CODEC.xmap(loader::apply, saver::apply);
        return storage.computeIfAbsent(new net.minecraft.world.level.saveddata.SavedDataType<>(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.github.alexmodguy.alexscaves.AlexsCaves.MODID, identifier),
                factory, acCodec, null));
        *///?} elif >=1.21.5 {
        /*com.mojang.serialization.Codec<T> acCodec = net.minecraft.nbt.CompoundTag.CODEC.xmap(loader::apply, saver::apply);
        return storage.computeIfAbsent(new net.minecraft.world.level.saveddata.SavedDataType<>(identifier, factory, acCodec, null));
        *///?} elif >=1.20.5 {
        /*return storage.computeIfAbsent(new net.minecraft.world.level.saveddata.SavedData.Factory<>(factory, (tag, registries) -> loader.apply(tag), null), identifier);
        *///?} elif >=1.20.2 {
        /*return storage.computeIfAbsent(new net.minecraft.world.level.saveddata.SavedData.Factory<>(factory, loader, null), identifier);
        *///?} else {
        return storage.computeIfAbsent(loader, factory, identifier);
        //?}
    }

    /**
     * Fires the loader's "this entity is about to take fall damage" event, which lets other mods
     * scale the fall or cancel it outright.
     *
     * <p>Only the candicorn needs this: it overrides {@code causeFallDamage} wholesale, so it has to
     * fire the hook that the vanilla implementation it replaced would have fired. Forge moved the
     * hook from {@code ForgeHooks} to {@code ForgeEventFactory} in 1.20.2 and changed it from
     * returning the adjusted numbers to returning the event.
     *
     * @return {@code {distance, damageMultiplier}} to fall with, or null if a listener cancelled the
     *         fall entirely.
     */
    @org.jetbrains.annotations.Nullable
    public static float[] onLivingFall(net.minecraft.world.entity.LivingEntity entity, float distance, float damageMultiplier) {
        //? if forge && >=1.20.2 {
        /*net.minecraftforge.event.entity.living.LivingFallEvent event =
                net.minecraftforge.event.ForgeEventFactory.onLivingFall(entity, distance, damageMultiplier);
        // The cast is a no-op below 1.21.5, where getDistance() already answers a float; 1.21.5
        // widened the whole fall-distance path to double and the event moved with it.
        return event == null ? null : new float[]{(float) event.getDistance(), event.getDamageMultiplier()};
        *///?}
        //? if !forge {
        /*return new float[]{distance, damageMultiplier};
        *///?}
        //? if forge && <1.20.2
        return net.minecraftforge.common.ForgeHooks.onLivingFall(entity, distance, damageMultiplier);
    }

    /**
     * Fires the loader's "this entity is about to knock another one back" event, which lets other
     * mods scale the knockback or cancel it outright.
     *
     * <p>Only the tremorzilla needs this: it knocks its targets back through its own code rather
     * than through {@code LivingEntity#knockback}, so it has to fire the hook that the vanilla path
     * it replaced would have fired. The same shape as {@link #onLivingFall} above, and it lives here
     * for the same reason — the loader split is three deep and the tail that reads the adjusted
     * numbers back is shared, which is more than a rename rule can express and more than the caller
     * should have to carry.
     *
     * <p>Forge moved the hook from {@code ForgeHooks} to {@code ForgeEventFactory} in 1.20.5, and
     * 56.0.0 (1.21.6) folded cancellation into a null return — see {@code
     * GumWormEntity#dropAllDeathLoot} for why that is a gate and not a rename rule. NeoForge keeps
     * the pre-1.20.5 spelling on every version, which is what the final arm is for; its owner is
     * rewritten by the loader's own rename rules.
     *
     * @return {@code {strength, ratioX, ratioZ}} to knock back with, or null if a listener cancelled
     *         the knockback entirely.
     */
    @org.jetbrains.annotations.Nullable
    public static double[] onLivingKnockBack(net.minecraft.world.entity.LivingEntity entity, float strength, double x, double z) {
        //? if fabric {
        /*return new double[]{strength, x, z};
        *///?} elif forge && >=1.21.6 {
        /*net.minecraftforge.event.entity.living.LivingKnockBackEvent event =
                net.minecraftforge.event.ForgeEventFactory.onLivingKnockBack(entity, strength, x, z);
        return event == null ? null : new double[]{event.getStrength(), event.getRatioX(), event.getRatioZ()};
        *///?} elif forge && >=1.20.5 {
        /*net.minecraftforge.event.entity.living.LivingKnockBackEvent event =
                net.minecraftforge.event.ForgeEventFactory.onLivingKnockBack(entity, strength, x, z);
        return event.isCanceled() ? null : new double[]{event.getStrength(), event.getRatioX(), event.getRatioZ()};
        *///?} else {
        net.minecraftforge.event.entity.living.LivingKnockBackEvent event =
                net.minecraftforge.common.ForgeHooks.onLivingKnockBack(entity, strength, x, z);
        return event.isCanceled() ? null : new double[]{event.getStrength(), event.getRatioX(), event.getRatioZ()};
        //?}
    }

    /**
     * The attribute modifiers a mob effect grants at {@code amplifier}, for the jelly bean tooltip.
     *
     * <p>1.20.2 made the map hold {@code AttributeModifierTemplate}s — a modifier not yet bound to
     * an amplifier — instead of finished {@code AttributeModifier}s that the caller then had to
     * re-derive through {@code getAttributeModifierValue}.
     */
    public static java.util.Map<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> attributeModifiers(
            net.minecraft.world.effect.MobEffect effect, int amplifier) {
        java.util.Map<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> out =
                new java.util.LinkedHashMap<>();
        //? if >=1.20.5 {
        /*// 1.20.5 dropped the getter entirely — the map is private and the templates are only
        // reachable through this visitor, which binds the amplifier for us.
        effect.createModifiers(amplifier, (attribute, modifier) -> out.put(attribute.value(), modifier));
        *///?} elif >=1.20.2 {
        /*effect.getAttributeModifiers().forEach((attribute, template) -> out.put(attribute, template.create(amplifier)));
        *///?}
        //? if <1.20.2 {
        effect.getAttributeModifiers().forEach((attribute, modifier) -> out.put(attribute,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        modifier.getName(), effect.getAttributeModifierValue(amplifier, modifier), modifier.getOperation())));
        //?}
        return out;
    }

    /**
     * A mob effect's remaining duration, rendered as {@code m:ss}. 1.20.3 gave
     * {@code MobEffectUtil.formatDuration} the server's tick rate, which {@code /tick rate} can now
     * change; nothing here runs at a non-default rate.
     *
     * <p>1.20.3, not 1.20.2 — javap on the vanilla 1.20.2 jar shows the old shape still there.
     * Neither Forge nor NeoForge publishes a 1.20.2 or a 1.20.3 build, so this tree's walk went
     * straight from 1.20.1 to 1.20.4 and could not tell the two apart until Fabric reached them.
     */
    public static net.minecraft.network.chat.Component formatDuration(
            net.minecraft.world.effect.MobEffectInstance instance, float durationFactor) {
        //? if >=1.20.3
        /*return net.minecraft.world.effect.MobEffectUtil.formatDuration(instance, durationFactor, 20.0F);*/
        //? if <1.20.3
        return net.minecraft.world.effect.MobEffectUtil.formatDuration(instance, durationFactor);
    }

    /**
     * 1.20.3 deleted {@code new AABB(BlockPos, BlockPos)} in favour of
     * {@code AABB.encapsulatingFullBlocks}.
     *
     * <p>1.20.3, not 1.20.2 — javap on the vanilla 1.20.2 jar shows the old shape still there.
     * Neither Forge nor NeoForge publishes a 1.20.2 or a 1.20.3 build, so this tree's walk went
     * straight from 1.20.1 to 1.20.4 and could not tell the two apart until Fabric reached them.
     */
    public static net.minecraft.world.phys.AABB encapsulating(net.minecraft.core.BlockPos a, net.minecraft.core.BlockPos b) {
        //? if >=1.20.3
        /*return net.minecraft.world.phys.AABB.encapsulatingFullBlocks(a, b);*/
        //? if <1.20.3
        return new net.minecraft.world.phys.AABB(a, b);
    }

    // ── The loader's own built-in registrations ─────────────────────────────────
    //
    // Forge hands these out as deferred-registration handles (Supplier#get); NeoForge hands out
    // vanilla Holders (Holder#value). Both spellings are one token apart and neither type has the
    // other's accessor, so the difference cannot be a Stonecutter rule: the rename rule that turns
    // the class name into its NeoForge one has that name as a SUBSTRING of its replacement, and such
    // a rule cannot be shadowed by declaring a narrower one first (written up in
    // ../AlexsMobsContinued/docs/notes/stonecutter.md). A gated accessor in a compat class is the
    // documented escape, so the fifteen call sites go through these four.
    //
    // Each arm deliberately spells the FORGE name even on the NeoForge side — the package and class
    // rename rules rewrite it at generation time. Writing the NeoForge name here directly would be
    // rewritten in turn, since the rename's search text sits inside its own replacement.
    //
    // Fabric has neither loader's registrations, so the two attribute accessors below gain a third
    // arm answering with the mod's own stand-ins — see fabric/entity/ACFabricAttributes, which also
    // supplies the LivingEntity behaviour Forge patches in. That arm names the stand-in fully
    // qualified rather than through a rename rule, because the Fabric rule group is a whitelist of
    // Forge types with a stand-in and this class is not one of them: it is the mod's own.

    /**
     * The gravity attribute the loader adds to every living entity — except from 1.20.5 on, where
     * vanilla adopted it and the loaders dropped their own. Same attribute, different owner.
     */
    public static net.minecraft.world.entity.ai.attributes.Attribute entityGravityAttribute() {
        //? if >=1.20.5
        /*return net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY.value();*/
        //? if fabric && <1.20.5
        /*return com.github.alexmodguy.alexscaves.fabric.entity.ACFabricAttributes.ENTITY_GRAVITY;*/
        //? if neoforge && <1.20.5
        /*return net.minecraftforge.common.ForgeMod.ENTITY_GRAVITY.value();*/
        //? if forge && <1.20.5
        return net.minecraftforge.common.ForgeMod.ENTITY_GRAVITY.get();
    }

    /** The swim-speed attribute the loader adds to every living entity. Vanilla never adopted it. */
    public static net.minecraft.world.entity.ai.attributes.Attribute swimSpeedAttribute() {
        //? if fabric
        /*return com.github.alexmodguy.alexscaves.fabric.entity.ACFabricAttributes.SWIM_SPEED;*/
        //? if neoforge
        /*return net.minecraftforge.common.ForgeMod.SWIM_SPEED.value();*/
        //? if forge
        return net.minecraftforge.common.ForgeMod.SWIM_SPEED.get();
    }

    // The fluid type vanilla water and lava are registered under.
    //
    // On Fabric these answer with the two singletons on the FluidType stand-in, which are not
    // registered objects and are not meant to be — they exist so that the ten-entry interaction
    // table in ACFluidRegistry#postInit can name water and lava the way it names this mod's two
    // fluids, and stay one spelling on all three loaders. That is the whole reason this accessor
    // pair has a Fabric arm at all: the alternative was duplicating forty lines of table into an
    // arm of its own. The chain is flat, one arm per loader, because Stonecutter does not nest and
    // the arms differ in how the loader hands its constant over (get / value / a plain field).
    //? if forge {
    public static net.minecraftforge.fluids.FluidType waterFluidType() {
        return net.minecraftforge.common.ForgeMod.WATER_TYPE.get();
    }

    public static net.minecraftforge.fluids.FluidType lavaFluidType() {
        return net.minecraftforge.common.ForgeMod.LAVA_TYPE.get();
    }
    //?} elif neoforge {
    /*public static net.minecraftforge.fluids.FluidType waterFluidType() {
        return net.minecraftforge.common.ForgeMod.WATER_TYPE.value();
    }

    public static net.minecraftforge.fluids.FluidType lavaFluidType() {
        return net.minecraftforge.common.ForgeMod.LAVA_TYPE.value();
    }
    *///?} else {
    /*public static net.minecraftforge.fluids.FluidType waterFluidType() {
        return net.minecraftforge.fluids.FluidType.WATER;
    }

    public static net.minecraftforge.fluids.FluidType lavaFluidType() {
        return net.minecraftforge.fluids.FluidType.LAVA;
    }
    *///?}

    /**
     * The automation inventory an entity exposes, or null if it has none. Forge answers with a
     * LazyOptional keyed by a Capability constant; NeoForge answers with the value (or null) keyed by
     * a typed EntityCapability. NeoForge 21.9 then replaced the item-handler capability itself with
     * the transfer API, so from there the answer is a {@code ResourceHandler<ItemResource>} — hence
     * the neutral {@link ACItemAccess} return type rather than an item handler.
     *
     * <p>Fabric has no capability system to ask, and its transfer API covers blocks rather than
     * entities, so the question is put to the entity itself: a {@link net.minecraft.world.Container}
     * <i>is</i> one, and a player's is its inventory. That is not a reduction of what the loaders
     * answer — every vanilla entity they attach an item handler to (the chest minecart, the chest
     * boat, the chested horse, the player) is reached by exactly those two tests, and the providers
     * they attach are wrappers over the same {@code Container}. What is genuinely lost is a modded
     * entity that exposes an inventory only as a capability and never as a {@code Container}; the
     * gingerbread man will not steal from one of those on Fabric.
     */
    @javax.annotation.Nullable
    public static ACItemAccess entityItemHandler(Entity entity) {
        //? if neoforge && >=1.21.9 {
        /*net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> resourceHandler =
                entity.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.ENTITY_AUTOMATION, net.minecraft.core.Direction.DOWN);
        return resourceHandler == null ? null : new ResourceHandlerAccess(resourceHandler);
        *///?} elif fabric {
        /*net.minecraft.world.Container container;
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            container = player.getInventory();
        } else if (entity instanceof net.minecraft.world.Container asContainer) {
            container = asContainer;
        } else {
            container = null;
        }
        return container == null ? null : new ContainerAccess(container);
        *///?} elif !forge {
        /*net.minecraftforge.items.IItemHandler handler =
                entity.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ENTITY_AUTOMATION, net.minecraft.core.Direction.DOWN);
        return handler == null ? null : new ItemHandlerAccess(handler);
        *///?} else {
        return entity.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, net.minecraft.core.Direction.DOWN)
                .map(handler -> (ACItemAccess) new ItemHandlerAccess(handler)).orElse(null);
        //?}
    }

    // The item-handler backing. Both loaders keep IItemHandler on every version this tree builds —
    // NeoForge 21.9 only stopped exposing it as a *capability* — so it needs no version gate. It is
    // gated off Fabric only because IItemHandler is a loader type with no stand-in here: the two arms
    // that construct this record are both disabled on Fabric, so the record would be an unresolvable
    // import serving nobody.
    //? if !fabric {
    private record ItemHandlerAccess(net.minecraftforge.items.IItemHandler handler) implements ACItemAccess {

        @Override
        public int size() {
            return handler.getSlots();
        }

        @Override
        public net.minecraft.world.item.ItemStack stackInSlot(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public net.minecraft.world.item.ItemStack takeOne(int slot) {
            return handler.extractItem(slot, 1, false);
        }
    }
    //?}

    // The Fabric backing: vanilla's own Container, which is what the loaders' ENTITY_AUTOMATION
    // provider wraps for every vanilla entity that has one. All three members it calls —
    // getContainerSize, getItem(int) and removeItem(int, int) — are declared on Container unchanged
    // from 1.20.1 to 26.2, and Inventory has implemented Container over that whole range, so this
    // needs no version gate either. removeItem is the right spelling of takeOne: it removes up to n
    // from the slot and hands back what it actually removed, which is what extractItem(slot, 1,
    // false) does on the loaders.
    //? if fabric {
    /*private record ContainerAccess(net.minecraft.world.Container container) implements ACItemAccess {

        @Override
        public int size() {
            return container.getContainerSize();
        }

        @Override
        public net.minecraft.world.item.ItemStack stackInSlot(int slot) {
            return container.getItem(slot);
        }

        @Override
        public net.minecraft.world.item.ItemStack takeOne(int slot) {
            return container.removeItem(slot, 1);
        }
    }
    *///?}

    // The transfer-API backing. A slot is a resource plus an amount rather than a stack, and every
    // mutation runs in a transaction that only takes effect on commit — so a removal that extracts
    // nothing simply never commits, which is what returning EMPTY below means.
    //? if neoforge && >=1.21.9 {
    /*private record ResourceHandlerAccess(
            net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> handler) implements ACItemAccess {

        @Override
        public int size() {
            return handler.size();
        }

        @Override
        public net.minecraft.world.item.ItemStack stackInSlot(int slot) {
            return handler.getResource(slot).toStack(handler.getAmountAsInt(slot));
        }

        @Override
        public net.minecraft.world.item.ItemStack takeOne(int slot) {
            net.neoforged.neoforge.transfer.item.ItemResource resource = handler.getResource(slot);
            if (resource.isEmpty()) {
                return net.minecraft.world.item.ItemStack.EMPTY;
            }
            try (net.neoforged.neoforge.transfer.transaction.Transaction transaction =
                         net.neoforged.neoforge.transfer.transaction.Transaction.open(null)) {
                if (handler.extract(slot, resource, 1, transaction) != 1) {
                    return net.minecraft.world.item.ItemStack.EMPTY;
                }
                transaction.commit();
            }
            return resource.toStack(1);
        }
    }
    *///?}

    /**
     * Posts an event on the game bus and reports whether a listener cancelled it.
     *
     * <p>Forge's event bus (6) returns that boolean straight from {@code post}; NeoForge is on bus 7,
     * where {@code post} returns the event itself and cancellation lives on {@code ICancellableEvent}.
     * Only the handful of call sites that read the result go through here — a post whose result is
     * discarded compiles unchanged on both.
     *
     * <p>Forge 56 (1.21.6) is on its own bus 7, which has neither a shared {@code Event} supertype to
     * take as a parameter nor a bus-wide {@code post} to call: an event type owns its bus. There is
     * therefore nothing left for this method to be generic over, and the call sites take a
     * {@code forge && >=1.21.6} arm naming the event's own {@code BUS} instead.
     *
     * <p>Fabric joins the first arm rather than needing one of its own: the stand-in bus is modelled
     * on bus 6 — {@code ACEventBus#post} returns the cancelled flag — precisely so that the shared
     * code this tree is authored in keeps meaning what it says. A coinciding Fabric arm is a widened
     * predicate, never a duplicated body.
     */
    //? if (forge && <1.21.6) || fabric {
    public static boolean postCancelable(net.minecraftforge.eventbus.api.Event event) {
        return net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
    }
    //?} elif neoforge {
    /*public static boolean postCancelable(net.minecraftforge.eventbus.api.Event event) {
        return net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event) instanceof net.neoforged.bus.api.ICancellableEvent cancellable && cancellable.isCanceled();
    }
    *///?}

    /**
     * Removes every effect a bucket of milk would cure. Forge asks the question with the curing
     * ItemStack; NeoForge replaced that with named EffectCure tokens.
     *
     * <p>Fabric has never had a curative-item system at all, so it takes the same arm the modern
     * versions do. That is not an approximation: milk's curative list was every effect that did not
     * opt out, and nothing in vanilla ever opted out — which is exactly the reasoning by which both
     * loaders deleted the system at 1.20.5.
     */
    public static void cureWithMilk(net.minecraft.world.entity.LivingEntity living) {
        // 1.20.5 deleted the curative-item system outright on both loaders: milk is once again just
        // "clear everything", which is what a milk bucket cured in practice anyway.
        //? if >=1.20.5 || fabric
        /*living.removeAllEffects();*/
        //? if neoforge && <1.20.5
        /*living.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK);*/
        //? if forge && <1.20.5
        living.curePotionEffects(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MILK_BUCKET));
    }

    // ── Force-loading chunks ────────────────────────────────────────────────────
    //
    // Alex's Caves keeps four things loaded across chunk borders — the nuclear blast, the beholder
    // eye, the occult gem and the remote detonator. Forge exposes this as static calls on
    // ForgeChunkManager keyed by mod id; NeoForge replaced it with a TicketController instance that
    // the mod registers once, in RegisterTicketControllersEvent. Different shapes, same three
    // operations, so the loader entrypoints hand the controller to {@link #setTicketController} and
    // everything else goes through the two methods below.
    //
    // Forge 55.x (1.21.5) then deleted its side of that outright — every ForgeChunkManager method is
    // a stub that throws, pointing at vanilla's TicketType system — so from there the Forge arm does
    // the bookkeeping itself, in acForceChunk below. NeoForge 21.5 still ships TicketController.
    //
    // Fabric never had an API here at all, so it takes that same do-it-yourself route on every
    // version: the owner counting is shared, and only the vanilla ticket call underneath it differs.
    // Which of the two vanilla spellings it takes is a VANILLA split at 1.21.5, not a loader one —
    // TicketType stopped being a generic value class with a static create() and became a registry
    // entry — so from 1.21.5 Fabric joins the Forge arm wholesale rather than keeping one of its own.
    // That arm names DeferredRegister and IEventBus, both of which the Fabric rename rules already
    // re-point at this mod's own stand-ins, so it compiles unchanged; Registries.TICKET_TYPE is in
    // BuiltInRegistries on 1.21.5 (javap'd), which is what the vendored register looks it up in.

    //? if neoforge {
    /*private static Object ticketController;

    /^*^*
     * Called by the NeoForge entrypoint from RegisterTicketControllersEvent. Typed as Object so this
     * class does not have to name a type that only exists on one loader.
     *^/
    public static void setTicketController(Object controller) {
        ticketController = controller;
    }
    *///?}

    // A vanilla ticket has no owner, which is the one thing the deleted API did for us: two owners
    // forcing the same chunk share one ticket, and the ticket store would drop it the first time
    // either of them let go. So the owners are counted here and the ticket only moves on the first
    // add and the last remove. Weakly keyed on the level so a singleplayer world reload starts empty
    // — the ServerLevel instance is new, and holding a stale non-empty owner set would make the next
    // add think the chunk was already forced.
    //
    // Shared by the two arms below that talk to vanilla tickets directly (Forge from 55.x, and Fabric
    // on every version), because the bookkeeping is a statement about this mod's four callers rather
    // than about either loader.
    //? if (forge && >=1.21.5) || fabric {
    /*private static final java.util.Map<net.minecraft.server.level.ServerLevel, java.util.Map<Long, java.util.Set<java.util.UUID>>> FORCED_TICKING_OWNERS =
            new java.util.WeakHashMap<>();
    private static final java.util.Map<net.minecraft.server.level.ServerLevel, java.util.Map<Long, java.util.Set<java.util.UUID>>> FORCED_LOADING_OWNERS =
            new java.util.WeakHashMap<>();
    *///?}

    //? if !neoforge && >=1.21.5 {
    /*// Two ticket types, not one type plus a flag: a chunk ticks or merely stays loaded according to
    // its ticket LEVEL, and TicketStorage dedupes by (type, level), so the two spellings have to be
    // distinguishable from each other. Neither persists — the old callback below existed only to
    // throw away tickets that had survived a restart, so on this arm there is nothing left to clear.
    private static final net.minecraftforge.registries.DeferredRegister<net.minecraft.server.level.TicketType> TICKET_DEF_REG =
            net.minecraftforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.TICKET_TYPE, com.github.alexmodguy.alexscaves.AlexsCaves.MODID);

    private static final java.util.function.Supplier<net.minecraft.server.level.TicketType> FORCED_TICKING =
            TICKET_DEF_REG.register("forced_ticking", () -> new net.minecraft.server.level.TicketType(
                    net.minecraft.server.level.TicketType.NO_TIMEOUT, false, net.minecraft.server.level.TicketType.TicketUse.LOADING_AND_SIMULATION));

    private static final java.util.function.Supplier<net.minecraft.server.level.TicketType> FORCED_LOADING =
            TICKET_DEF_REG.register("forced_loading", () -> new net.minecraft.server.level.TicketType(
                    net.minecraft.server.level.TicketType.NO_TIMEOUT, false, net.minecraft.server.level.TicketType.TicketUse.LOADING));

    // Called from the AlexsCaves constructor. The registry is vanilla's, so this is an ordinary
    // DeferredRegister over Registries.TICKET_TYPE rather than anything loader-specific.
    public static void registerTicketTypes(net.minecraftforge.eventbus.api.IEventBus modEventBus) {
        TICKET_DEF_REG.register(modEventBus);
    }

    private static void acForceChunk(net.minecraft.server.level.ServerLevel level, java.util.UUID owner, int chunkX, int chunkZ, boolean add, boolean ticking) {
        java.util.Map<Long, java.util.Set<java.util.UUID>> perChunk =
                (ticking ? FORCED_TICKING_OWNERS : FORCED_LOADING_OWNERS).computeIfAbsent(level, l -> new java.util.HashMap<>());
        long chunk = ACCompat.chunkAsLong(chunkX, chunkZ);
        net.minecraft.server.level.TicketType type = (ticking ? FORCED_TICKING : FORCED_LOADING).get();
        // addTicketWithRadius puts the chunk at ChunkLevel.byStatus(FULL) - radius. Radius 2 is level
        // 31, ENTITY_TICKING — the same level vanilla's own forced chunks sit at; radius 0 leaves it
        // at 33, loaded but not ticking, which is what the ticking=false contract asked for.
        int radius = ticking ? 2 : 0;
        net.minecraft.world.level.ChunkPos pos = new net.minecraft.world.level.ChunkPos(chunkX, chunkZ);
        if (add) {
            java.util.Set<java.util.UUID> owners = perChunk.computeIfAbsent(chunk, c -> new java.util.HashSet<>());
            if (owners.add(owner) && owners.size() == 1) {
                level.getChunkSource().addTicketWithRadius(type, pos, radius);
                level.getChunk(chunkX, chunkZ);
            }
        } else {
            java.util.Set<java.util.UUID> owners = perChunk.get(chunk);
            if (owners != null && owners.remove(owner) && owners.isEmpty()) {
                perChunk.remove(chunk);
                level.getChunkSource().removeTicketWithRadius(type, pos, radius);
            }
        }
    }
    *///?}

    // Fabric has no force-loading API at all — Fabric API stops at chunk load/unload events — so this
    // arm goes straight to the vanilla tickets the other two loaders wrap, and is the same code the
    // Forge arm above became when Forge 55.x deleted its own. It is bounded at <1.21.5 because that
    // is where vanilla replaced TicketType.create with a registry entry, at which point the arm above
    // says the same thing in the modern spelling and Fabric simply joins it. Only the vanilla
    // spelling differs:
    // below 1.21.5 a ticket is added as addRegionTicket(type, pos, radius, value), which builds it at
    // ChunkLevel.byStatus(FULL) - radius — the identical formula addTicketWithRadius applies later —
    // so radius 2 is level 31, ENTITY_TICKING, where vanilla's own /forceload tickets sit, and radius
    // 0 leaves it at 33, loaded but not ticking. The value is the chunk position, which is also what
    // vanilla's FORCED type carries.
    //
    // Two types rather than one plus a flag, for the reason spelled out above: a Ticket's identity is
    // (type, level, value), so the ticking and the merely-loaded spelling must not dedupe into one.
    // Neither has a timeout — the two-argument TicketType.create leaves it at 0, meaning never.
    //? if fabric && <1.21.5 {
    /*private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> FORCED_TICKING =
            net.minecraft.server.level.TicketType.create("alexscaves_forced_ticking", java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong));

    private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> FORCED_LOADING =
            net.minecraft.server.level.TicketType.create("alexscaves_forced_loading", java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong));

    private static void acForceChunk(net.minecraft.server.level.ServerLevel level, java.util.UUID owner, int chunkX, int chunkZ, boolean add, boolean ticking) {
        java.util.Map<Long, java.util.Set<java.util.UUID>> perChunk =
                (ticking ? FORCED_TICKING_OWNERS : FORCED_LOADING_OWNERS).computeIfAbsent(level, l -> new java.util.HashMap<>());
        long chunk = ACCompat.chunkAsLong(chunkX, chunkZ);
        net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> type = ticking ? FORCED_TICKING : FORCED_LOADING;
        int radius = ticking ? 2 : 0;
        net.minecraft.world.level.ChunkPos pos = new net.minecraft.world.level.ChunkPos(chunkX, chunkZ);
        if (add) {
            java.util.Set<java.util.UUID> owners = perChunk.computeIfAbsent(chunk, c -> new java.util.HashSet<>());
            if (owners.add(owner) && owners.size() == 1) {
                level.getChunkSource().addRegionTicket(type, pos, radius, pos);
                level.getChunk(chunkX, chunkZ);
            }
        } else {
            java.util.Set<java.util.UUID> owners = perChunk.get(chunk);
            if (owners != null && owners.remove(owner) && owners.isEmpty()) {
                perChunk.remove(chunk);
                level.getChunkSource().removeRegionTicket(type, pos, radius, pos);
            }
        }
    }
    *///?}

    /**
     * Hands the loader the callback that drops every stale force-load ticket on world load. A no-op
     * on NeoForge, where the callback is baked into the controller at construction instead, and from
     * Forge 55.x, where this mod's tickets do not persist in the first place.
     */
    public static void registerForcedChunkCallback() {
        //? if forge && <1.21.5
        net.minecraftforge.common.world.ForgeChunkManager.setForcedChunkLoadingCallback(com.github.alexmodguy.alexscaves.AlexsCaves.MODID, com.github.alexmodguy.alexscaves.server.level.storage.ACWorldData::clearLoadedChunksCallback);
    }

    /**
     * Adds or removes a force-load ticket owned by an entity, for one chunk.
     *
     * @param ticking whether the chunk should also tick, not merely stay loaded.
     */
    public static void forceChunk(net.minecraft.server.level.ServerLevel level, Entity owner, int chunkX, int chunkZ, boolean add, boolean ticking) {
        //? if neoforge && <1.21.5 {
        /*if (ticketController != null) {
            ((net.neoforged.neoforge.common.world.chunk.TicketController) ticketController)
                    .forceChunk(level, owner, chunkX, chunkZ, add, ticking);
        }
        *///?}
        // 1.21.5 repurposed the trailing flag: every forced chunk now sits at the entity-ticking
        // level, so the boolean asks whether mobs should also spawn naturally there instead — which
        // is not something any of these four callers wants.
        //? if neoforge && >=1.21.5 {
        /*if (ticketController != null) {
            ((net.neoforged.neoforge.common.world.chunk.TicketController) ticketController)
                    .forceChunk(level, owner, chunkX, chunkZ, add, false);
        }
        *///?}
        //? if (forge && >=1.21.5) || fabric
        /*acForceChunk(level, owner.getUUID(), chunkX, chunkZ, add, ticking);*/
        //? if forge && <1.21.5
        net.minecraftforge.common.world.ForgeChunkManager.forceChunk(level, com.github.alexmodguy.alexscaves.AlexsCaves.MODID, owner, chunkX, chunkZ, add, ticking);
    }

    /**
     * Adds or removes a force-load ticket owned by a bare UUID, for one chunk. The occult gem and the
     * remote detonator hold their tickets this way, since the owning item can change hands.
     */
    public static void forceChunk(net.minecraft.server.level.ServerLevel level, java.util.UUID owner, int chunkX, int chunkZ, boolean add, boolean ticking) {
        //? if neoforge && <1.21.5 {
        /*if (ticketController != null) {
            ((net.neoforged.neoforge.common.world.chunk.TicketController) ticketController)
                    .forceChunk(level, owner, chunkX, chunkZ, add, ticking);
        }
        *///?}
        // See the entity overload for why the trailing flag stops being the ticking one in 1.21.5.
        //? if neoforge && >=1.21.5 {
        /*if (ticketController != null) {
            ((net.neoforged.neoforge.common.world.chunk.TicketController) ticketController)
                    .forceChunk(level, owner, chunkX, chunkZ, add, false);
        }
        *///?}
        //? if (forge && >=1.21.5) || fabric
        /*acForceChunk(level, owner, chunkX, chunkZ, add, ticking);*/
        //? if forge && <1.21.5
        net.minecraftforge.common.world.ForgeChunkManager.forceChunk(level, com.github.alexmodguy.alexscaves.AlexsCaves.MODID, owner, chunkX, chunkZ, add, ticking);
    }

    /**
     * A plain bucket of a modded fluid.
     *
     * <p>Same story as {@code AcidBlock}: NeoForge deleted the deferred-supplier {@code BucketItem}
     * constructor in 1.20.5, and FLUID precedes ITEM in {@code BuiltInRegistries}, so resolving the
     * supplier at item-construction time is safe. Wrapped here so the two registry lines stay one
     * expression each.
     *
     * <p>The deferred constructor is a Forge patch, so Fabric joins that arm on every version — and
     * the safety argument is the same one, made stronger: the vendored Fabric {@code DeferredRegister}
     * is immediate, so the fluids are already in the registry by the time the item registry flushes.
     */
    public static net.minecraft.world.item.BucketItem bucketItem(
            java.util.function.Supplier<? extends net.minecraft.world.level.material.Fluid> fluid,
            net.minecraft.world.item.Item.Properties properties) {
        //? if (neoforge && >=1.20.5) || fabric {
        /*return new net.minecraft.world.item.BucketItem(fluid.get(), properties);
        *///?} else {
        return new net.minecraft.world.item.BucketItem(fluid, properties);
        //?}
    }

    /**
     * The full save tag for an entity, as the possession totem and the beholder's grudge stash it.
     *
     * <p>1.20.5 made every NBT read and write registry-aware, so NeoForge's {@code serializeNBT}
     * takes a {@code HolderLookup.Provider}. The entity can supply its own — {@code registryAccess()}
     * is the level's — so no call site has to find one. Forge left the no-arg overload in place
     * through 1.20.6 and dropped it in 1.21, so the newer arm is the one both loaders end up on.
     */
    public static net.minecraft.nbt.CompoundTag serializeEntity(Entity entity) {
        // 1.21.5 took INBTSerializable off Entity on both loaders, so the hook is gone entirely.
        // What it did is spelled out here: the vanilla save plus the "id" key the loaders' default
        // added, which is the half the two call sites need in order to read the entity back.
        //? if >=1.21.5 {
        /*net.minecraft.nbt.CompoundTag acTag = new net.minecraft.nbt.CompoundTag();
        String acId = entity.getEncodeId();
        if (acId != null) {
            acTag.putString("id", acId);
        }
        entity.saveWithoutId(ACCompat.asOutput(acTag, entity.level().registryAccess()));
        return acTag;
        *///?} elif fabric {
        /*// serializeNBT is Forge's INBTSerializable default on Entity, and Fabric has no equivalent —
        // so below 1.21.5 this is that default written out, which is the same handful of lines the
        // arm above ended up with once both loaders had dropped the hook too.
        net.minecraft.nbt.CompoundTag acTag = new net.minecraft.nbt.CompoundTag();
        String acId = entity.getEncodeId();
        if (acId != null) {
            acTag.putString("id", acId);
        }
        entity.saveWithoutId(acTag);
        return acTag;
        *///?} elif >=1.21 || (neoforge && >=1.20.5) {
        /*return entity.serializeNBT(entity.registryAccess());
        *///?} else {
        return entity.serializeNBT();
        //?}
    }

    /**
     * Whether an entity is allowed to change the world around it — the {@code mobGriefing} game rule,
     * as a listener may have overridden it for this one entity.
     *
     * <p>Twelve entities and blocks ask this. Forge still spells it
     * {@code ForgeEventFactory.getMobGriefingEvent}; NeoForge renamed the hook to
     * {@code canEntityGrief} in 1.20.5 and moved the decision onto the event
     * ({@code EntityMobGriefingEvent#canGrief}), which is the same question asked once instead of a
     * tri-state the caller has to fold. The name here is NeoForge's, since it says what it returns.
     *
     * <p>1.21.2 narrowed the hook's level to a {@code ServerLevel} — griefing is a server decision,
     * and the same release took {@code getGameRules} off {@code Level} for the same reason. Off a
     * server level the answer is {@code mobGriefing}'s vanilla default, matching how
     * {@link ACCompat#gameRule} states a client answer at every one of its call sites.
     *
     * <p>Fabric has neither hook nor an event of its own to fire, so it answers the game rule
     * directly — which is exactly what the other two loaders' hooks answer when no listener has
     * anything to say, and no listener can exist on this loader. Routing it through
     * {@link ACCompat#gameRule} keeps the client fallback and the 1.21.2 {@code ServerLevel}
     * narrowing in one place rather than repeating them here.
     */
    public static boolean canEntityGrief(net.minecraft.world.level.Level level, Entity entity) {
        //? if fabric {
        /*return ACCompat.gameRule(level, GameRules.RULE_MOBGRIEFING, true);
        *///?} elif neoforge && >=1.21.2 {
        /*return level instanceof net.minecraft.server.level.ServerLevel serverLevel
                ? net.neoforged.neoforge.event.EventHooks.canEntityGrief(serverLevel, entity)
                : true;
        *///?} elif forge && >=1.21.2 {
        /*return !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)
                || net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(serverLevel, entity);
        *///?} elif neoforge && >=1.20.5 {
        /*return net.neoforged.neoforge.event.EventHooks.canEntityGrief(level, entity);
        *///?} else {
        return net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level, entity);
        //?}
    }

    /**
     * Fires the bone-meal event for the fertilizer, keeping the tri-state {@code int} the old Forge
     * hook returned: {@code 1} a listener handled it, {@code -1} a listener vetoed it, {@code 0} carry on.
     *
     * <p>NeoForge 1.20.5 replaced the {@code Event.Result} that encoded those three states with a
     * cancellable event carrying a {@code successful} flag, and returns the event itself rather than
     * a verdict. Folding it back to the {@code int} keeps the one call site unchanged.
     *
     * <p>Fabric has no bone-meal event on any version — Fabric API has never shipped one — so this
     * answers {@code 0}, "nobody handled it, carry on", which is what both other loaders return when
     * no listener is registered. The single caller is this mod's own fertilizer, so the only thing
     * lost is a third-party mod's chance to veto or pre-empt it.
     */
    public static int onApplyBonemeal(net.minecraft.world.entity.player.Player player,
                                      net.minecraft.world.level.Level level,
                                      net.minecraft.core.BlockPos pos,
                                      net.minecraft.world.level.block.state.BlockState state,
                                      net.minecraft.world.item.ItemStack stack) {
        //? if fabric {
        /*return 0;
        *///?} elif neoforge && >=1.20.5 {
        /*net.neoforged.neoforge.event.entity.player.BonemealEvent event =
                net.neoforged.neoforge.event.EventHooks.fireBonemealEvent(player, level, pos, state, stack);
        if (event.isCanceled()) {
            return -1;
        }
        return event.isSuccessful() ? 1 : 0;
        *///?} else {
        return net.minecraftforge.event.ForgeEventFactory.onApplyBonemeal(player, level, pos, state, stack);
        //?}
    }

    /**
     * Whether a block's path type carries no danger of its own — Citadel's raycoms pathfinder asks
     * this of every low block it is thinking of stepping onto.
     *
     * <p>{@code getDanger()} is a <em>Forge</em> patch on the path-type enum, added so an
     * {@code IExtensibleEnum} path type registered by a mod can name the danger variant it degrades
     * to. NeoForge never took it, and dropped {@code IExtensibleEnum} with it in 1.20.5.
     *
     * <p>The NeoForge arm answers {@code true} rather than reaching for
     * {@code getAdjacentBlockPathType}, which is the nearest thing it has: that hook reports the
     * danger a block poses to its <em>neighbours</em> (berry bushes, fire), which is a different
     * question and would make burning blocks impassable on NeoForge nodes only. Every vanilla path
     * type is built without a danger, so on Forge this returns {@code true} for all of them too —
     * the arms agree unless another mod registers an extensible path type, which cannot happen on
     * NeoForge at all.
     *
     * <p>Forge 62 (26) dropped {@code getDanger()} as well, while keeping {@code IExtensibleEnum} on
     * the enum itself — so from 26 both loaders take the constant arm, which is what every vanilla
     * path type answered on Forge anyway.
     *
     * <p>Fabric is the NeoForge case on every version: {@code getDanger()} is a patch, so it is not
     * there to call, and no extensible path type can exist for it to have answered about.
     */
    public static boolean pathTypeIsSafe(@Nullable BlockPathTypes pathType) {
        //? if (neoforge && >=1.20.5) || >=26 || fabric {
        /*return true;
        *///?} else {
        return pathType == null || pathType.getDanger() == null;
        //?}
    }
}
