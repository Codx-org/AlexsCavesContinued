package com.github.alexmodguy.alexscaves.server.event;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;

import codx.codxlib.api.CodxLib;
import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.github.alexmodguy.alexscaves.server.enchantment.ACEnchantmentRegistry;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.ACFrogRegistry;
import com.github.alexmodguy.alexscaves.server.entity.item.SeekingArrowEntity;
import com.github.alexmodguy.alexscaves.server.entity.item.SubmarineEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.*;
import com.github.alexmodguy.alexscaves.server.entity.util.*;
import com.github.alexmodguy.alexscaves.server.item.ACItemRegistry;
import com.github.alexmodguy.alexscaves.server.item.AlwaysCombinableOnAnvil;
import com.github.alexmodguy.alexscaves.server.item.ExtinctionSpearItem;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRarity;
import com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRegistry;
import com.github.alexmodguy.alexscaves.server.level.biome.BiomeSourceAccessor;
import com.github.alexmodguy.alexscaves.server.level.map.ACWorldWorkerManager;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import com.github.alexmodguy.alexscaves.server.potion.DarknessIncarnateEffect;
import com.github.alexmodguy.alexscaves.server.potion.SugarRushEffect;
import com.github.alexmodguy.alexscaves.citadel.server.tick.ServerTickRateTracker;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AnvilUpdateEvent;
// Fabric keeps this on every version. The gate is about NeoForge, which folded the tick events
// into per-target ones at 1.20.5; Fabric's TickEvent is this tree's own vendored stub, fired by
// its own bus, so there is nothing there to fold and every listener below takes the else arm.
//? if forge || fabric || <1.20.5
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.github.alexmodguy.alexscaves.server.misc.ACFluids;

public class CommonEvents {
    @SubscribeEvent
    public void livingDie(LivingDeathEvent event) {
        if (event.getEntity().getType() == EntityType.MAGMA_CUBE && event.getSource() != null && event.getSource().getEntity() instanceof Frog frog) {
            if (ACFrogRegistry.isPrimordial(frog)) {
                ACCompat.spawnAtLocation(event.getEntity(), new ItemStack(ACBlockRegistry.CARMINE_FROGLIGHT.get()));
            }
        }
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof Mob mob && event.getSource() != null && event.getSource().getDirectEntity() instanceof LivingEntity directSource && directSource.getItemInHand(InteractionHand.MAIN_HAND).is(ACItemRegistry.PRIMITIVE_CLUB.get())) {
            if (ACCompat.enchantLevel(directSource.getItemInHand(InteractionHand.MAIN_HAND), ACEnchantmentRegistry.BONKING) > 0 && event.getEntity().level().getRandom().nextFloat() < 0.33F) {
                Creeper fakeCreeperForSkullDrop = ACCompat.createEntity(EntityType.CREEPER, mob.level());
                if (fakeCreeperForSkullDrop != null) {
                    if (event.getEntity().level() instanceof ServerLevel serverLevel) {
                        LightningBolt fakeThunder = ACCompat.createEntity(EntityType.LIGHTNING_BOLT, serverLevel);
                        if (fakeThunder != null) {
                            fakeThunder.setVisualOnly(true);
                            fakeCreeperForSkullDrop.thunderHit(serverLevel, fakeThunder);
                        }
                    }
                    DamageSource fakeCreeperDamage = mob.level().damageSources().mobAttack(fakeCreeperForSkullDrop);
                    HashMap<EquipmentSlot, Float> prevLootDropChances = new HashMap<>();
                    EntityDropChanceAccessor dropChanceAccessor = (EntityDropChanceAccessor) mob;
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        prevLootDropChances.put(slot, dropChanceAccessor.ac_getEquipmentDropChance(slot));
                        dropChanceAccessor.ac_setDropChance(slot, 0.0F);
                    }
                    dropChanceAccessor.ac_dropCustomDeathLoot(fakeCreeperDamage, 0, false);
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        dropChanceAccessor.ac_setDropChance(slot, prevLootDropChances.get(slot));
                    }
                }


            }
        }
        if (event.getEntity() instanceof Player) {
            if (event.getEntity().getUUID().toString().equals("71363abe-fd03-49c9-940d-aae8b8209b7c")) {
                ACCompat.spawnAtLocation(event.getEntity(), new ItemStack(ACItemRegistry.GREEN_SOYLENT.get(), 1 + event.getEntity().getRandom().nextInt(9)));
            }
            if (event.getEntity().getUUID().toString().equals("4a463319-625c-4b86-a4e7-8b700f023a60")) {
                ACCompat.spawnAtLocation(event.getEntity(), new ItemStack(ACItemRegistry.STINKY_FISH.get(), 1));
            }
        }
    }

    // ── Cancelling listeners ───────────────────────────────────────────────────
    // EventBus 7 (Forge 56, i.e. every Forge node from 1.21.6) deleted Event#setCanceled outright:
    // cancellation is no longer state carried on the event, it is the value a listener *returns*.
    // So a cancelling handler is `public boolean` there and `public void` everywhere else, and that
    // signature is the only thing that differs — each decision below is therefore stated once in an
    // ac-prefixed helper that answers "should this be cancelled?", with two thin gated entry points
    // around it. The same split acCancelDamage already uses for the NeoForge Pre/Post divergence.
    //
    // One consequence worth knowing: EB7 stops dispatching as soon as a listener cancels (see
    // InvokerFactory#createCancellableInvokerFromUnwrapped), so a handler that used to ask whether
    // somebody else had already cancelled simply is not called in that case.

    /** Irradiation suppresses natural healing, unless the entity's type shrugs radiation off. */
    private boolean acLivingHeal(LivingHealEvent event) {
        return event.getEntity().hasEffect(ACCompat.effect(ACEffectRegistry.IRRADIATED.get())) && !event.getEntity().getType().builtInRegistryHolder().is(ACTagRegistry.RESISTS_RADIATION);
    }

    //? if forge && >=1.21.6 {
    /*@SubscribeEvent
    public boolean livingHeal(LivingHealEvent event) {
        return acLivingHeal(event);
    }
    *///?} else {
    @SubscribeEvent
    public void livingHeal(LivingHealEvent event) {
        if (acLivingHeal(event)) {
            event.setCanceled(true);
        }
    }
    //?}

    /** Binding a holocoder to the clicked entity consumes the interaction. */
    private boolean acPlayerEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(ACItemRegistry.HOLOCODER.get()) && event.getTarget() instanceof LivingEntity && !(event.getTarget() instanceof ArmorStand) && event.getTarget().isAlive()) {
            CompoundTag tag = ACCompat.getOrCreateTag(stack);
            ACCompat.putUUID(tag, "BoundEntityUUID", event.getTarget().getUUID());
            CompoundTag entityTag = event.getTarget() instanceof Player ? new CompoundTag() : ACPlatform.serializeEntity(event.getTarget());
            entityTag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(event.getTarget().getType()).toString());
            if (event.getTarget() instanceof Player) {
                ACCompat.putUUID(entityTag, "UUID", event.getTarget().getUUID());
            }
            tag.put("BoundEntityTag", entityTag);
            ItemStack stackReplacement = new ItemStack(ACItemRegistry.HOLOCODER.get());
            stack.shrink(1);
            ACCompat.setTag(stackReplacement, tag);
            event.getEntity().swing(event.getHand());
            if (!event.getEntity().addItem(stackReplacement)) {
                ItemEntity itementity = event.getEntity().drop(stackReplacement, false);
                if (itementity != null) {
                    itementity.setNoPickUpDelay();
                    ACPlatform.setThrower(itementity, event.getEntity());
                }
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            return true;
        }
        return false;
    }

    //? if forge && >=1.21.6 {
    /*@SubscribeEvent
    public boolean playerEntityInteract(PlayerInteractEvent.EntityInteract event) {
        return acPlayerEntityInteract(event);
    }
    *///?} else {
    @SubscribeEvent
    public void playerEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (acPlayerEntityInteract(event)) {
            event.setCanceled(true);
        }
    }
    //?}

    /** A vallumraptor that is still hiding is not a target anything may pick. */
    private boolean acLivingFindTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && event.getNewTarget() instanceof VallumraptorEntity vallumraptor && vallumraptor.getHideFor() > 0) {
            mob.setTarget(null);
            return true;
        }
        return false;
    }

    //? if forge && >=1.21.6 {
    /*@SubscribeEvent
    public boolean livingFindTarget(LivingChangeTargetEvent event) {
        return acLivingFindTarget(event);
    }
    *///?} else {
    @SubscribeEvent
    public void livingFindTarget(LivingChangeTargetEvent event) {
        if (acLivingFindTarget(event)) {
            event.setCanceled(true);
        }
    }
    //?}

    /**
     * The four situations in which this mod refuses damage outright.
     *
     * <p>Split out of the listener because NeoForge 1.21 replaced the one cancellable
     * {@code LivingDamageEvent} with a {@code Pre}/{@code Post} pair, and {@code Pre} is not
     * cancellable — the way to stop the damage there is to set its amount to zero. That is the only
     * thing that differs, so only the listener is gated and the decision itself is stated once.
     *
     * <p>Each case is evaluated even after an earlier one has already answered yes, exactly as the
     * chain of four independent {@code if}s did before: two of them have side effects (the spear
     * kills the ghosts it blocked, the boots clear fall distance) that a short-circuit would drop.
     */
    private static boolean acCancelDamage(LivingEntity hurt, DamageSource source) {
        boolean cancel = false;
        if (hurt.isPassenger() && hurt instanceof FlyingMount && (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL))) {
            cancel = true;
        }
        if (hurt instanceof WatcherPossessionAccessor possessed && possessed.isPossessedByWatcher() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !(source.getEntity() instanceof WatcherEntity)) {
            cancel = true;
        }
        if (hurt instanceof Player player && player.getUseItem().is(ACItemRegistry.EXTINCTION_SPEAR.get()) && ExtinctionSpearItem.killGrottoGhostsFor(player, true)) {
            cancel = true;
            player.playSound(ACCompat.rawSound(SoundEvents.SHIELD_BLOCK));
        }
        if (hurt instanceof Player player && source.is(DamageTypes.FALL) && player.getItemBySlot(EquipmentSlot.FEET).is(ACItemRegistry.RAINBOUNCE_BOOTS.get())) {
            player.fallDistance = 0.0F;
            cancel = true;
        }
        return cancel;
    }

    //? if neoforge && >=1.21 {
    /*@SubscribeEvent
    public void livingHurt(LivingDamageEvent.Pre event) {
        if (acCancelDamage(event.getEntity(), event.getSource())) {
            event.setNewDamage(0.0F);
        }
    }
    *///?} elif forge && >=1.21.6 {
    /*@SubscribeEvent
    public boolean livingHurt(LivingDamageEvent event) {
        return acCancelDamage(event.getEntity(), event.getSource());
    }
    *///?} else {
    @SubscribeEvent
    public void livingHurt(LivingDamageEvent event) {
        if (acCancelDamage(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }
    //?}


    /** A resistor shield turns an arrow back on its shooter; a stunned attacker lands nothing at all. */
    private boolean acLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow && event.getEntity().isBlocking() && event.getEntity().getUseItem().is(ACItemRegistry.RESISTOR_SHIELD.get())) {
            ItemStack shield = event.getEntity().getUseItem();
            if (ACCompat.enchantLevel(shield, ACEnchantmentRegistry.ARROW_INDUCTING) > 0 && arrow.getType() != ACEntityRegistry.SEEKING_ARROW.get()) {
                SeekingArrowEntity seekingArrowEntity = new SeekingArrowEntity(event.getEntity().level(), event.getEntity());
                seekingArrowEntity.copyPosition(arrow);
                seekingArrowEntity.setDeltaMovement(arrow.getDeltaMovement().scale(-0.4D));
                seekingArrowEntity.setYRot(arrow.getYRot() + 180.0F);
                event.getEntity().level().addFreshEntity(seekingArrowEntity);
                arrow.discard();
            }
        }
        return event.getSource() != null && event.getSource().getDirectEntity() instanceof LivingEntity directSource && directSource.hasEffect(ACCompat.effect(ACEffectRegistry.STUNNED.get()));
    }

    //? if forge && >=1.21.6 {
    /*@SubscribeEvent
    public boolean livingAttack(LivingAttackEvent event) {
        return acLivingAttack(event);
    }
    *///?} else {
    @SubscribeEvent
    public void livingAttack(LivingAttackEvent event) {
        if (acLivingAttack(event)) {
            event.setCanceled(true);
        }
    }
    //?}

    /** You cannot punch the dinosaur you are riding alongside somebody else. */
    private boolean acPlayerAttack(AttackEntityEvent event) {
        return event.getTarget() instanceof DinosaurEntity && event.getEntity().isPassengerOfSameVehicle(event.getTarget());
    }

    //? if forge && >=1.21.6 {
    /*@SubscribeEvent
    public boolean playerAttack(AttackEntityEvent event) {
        return acPlayerAttack(event);
    }
    *///?} else {
    @SubscribeEvent
    public void playerAttack(AttackEntityEvent event) {
        if (acPlayerAttack(event)) {
            event.setCanceled(true);
        }
    }
    //?}

    // See ClientEvents#clientLivingTick: from 1.20.5 NeoForge ticks every entity through one
    // EntityTickEvent, so the listener takes the wider type and narrows it back to a LivingEntity.
    //? if neoforge && >=1.20.5 {
    /*@SubscribeEvent
    public void livingTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
    *///?} else {
    @SubscribeEvent
    public void livingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
    //?}
        if (entity.hasEffect(ACCompat.effect(ACEffectRegistry.BUBBLED.get())) && ACFluids.isInAnyFluid(entity)) {
            entity.removeEffect(ACCompat.effect(ACEffectRegistry.BUBBLED.get()));
        }
        if (entity.hasEffect(ACCompat.effect(ACEffectRegistry.DARKNESS_INCARNATE.get())) && entity.tickCount % 5 == 0 && DarknessIncarnateEffect.isInLight(entity, 11)) {
            entity.removeEffect(ACCompat.effect(ACEffectRegistry.DARKNESS_INCARNATE.get()));
        }
        if (entity.getItemBySlot(EquipmentSlot.HEAD).is(ACItemRegistry.DIVING_HELMET.get()) && (!entity.isEyeInFluid(FluidTags.WATER) || entity.getVehicle() instanceof SubmarineEntity)) {
            entity.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 810, 0, false, false, true));
        }
        if (!entity.level().isClientSide() && entity instanceof Mob mob && mob.getTarget() instanceof VallumraptorEntity vallumraptor && vallumraptor.getHideFor() > 0) {
            mob.setTarget(null);
        }
    }

    // MobSpawnEvent's nested FinalizeSpawn was promoted to a top-level FinalizeSpawnEvent on
    // NeoForge in 1.20.5. Same event, same getters — only the name a listener spells changes, so
    // just the header is gated and the body below is shared.
    @SubscribeEvent
    //? if neoforge && >=1.20.5 {
    /*public void onEntityJoinWorld(net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent event) {
    *///?} else {
    public void onEntityJoinWorld(MobSpawnEvent.FinalizeSpawn event) {
    //?}
        try {
            if (event.getEntity() instanceof Creeper creeper) {
                creeper.targetSelector.addGoal(3, new AvoidEntityGoal<>(creeper, RaycatEntity.class, 10.0F, 1.0D, 1.2D));
            }
            if (event.getEntity() instanceof Drowned drowned && drowned.level().getBiome(drowned.blockPosition()).is(ACBiomeRegistry.ABYSSAL_CHASM)) {
                if (drowned.getItemBySlot(EquipmentSlot.FEET).isEmpty() && drowned.getItemBySlot(EquipmentSlot.LEGS).isEmpty() && drowned.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && drowned.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                    if (drowned.getRandom().nextFloat() < AlexsCaves.COMMON_CONFIG.drownedDivingGearSpawnChance.get()) {
                        drowned.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ACItemRegistry.DIVING_HELMET.get()));
                        drowned.setDropChance(EquipmentSlot.HEAD, 0.5F);
                    }
                    if (drowned.getRandom().nextFloat() < AlexsCaves.COMMON_CONFIG.drownedDivingGearSpawnChance.get()) {
                        drowned.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ACItemRegistry.DIVING_CHESTPLATE.get()));
                        drowned.setDropChance(EquipmentSlot.CHEST, 0.5F);
                    }
                    if (drowned.getRandom().nextFloat() < AlexsCaves.COMMON_CONFIG.drownedDivingGearSpawnChance.get()) {
                        drowned.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ACItemRegistry.DIVING_LEGGINGS.get()));
                        drowned.setDropChance(EquipmentSlot.LEGS, 0.5F);
                    }
                    if (drowned.getRandom().nextFloat() < AlexsCaves.COMMON_CONFIG.drownedDivingGearSpawnChance.get()) {
                        drowned.setItemSlot(EquipmentSlot.FEET, new ItemStack(ACItemRegistry.DIVING_BOOTS.get()));
                        drowned.setDropChance(EquipmentSlot.FEET, 0.5F);
                    }
                }
            }
            if (event.getEntity() instanceof Fox fox) {
                fox.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(fox, GingerbreadManEntity.class, 40, false, false, null));
            }
        } catch (Exception e) {
            AlexsCaves.LOGGER.warn("Tried to add unique behaviors to vanilla mobs and encountered an error");
        }
    }

    @SubscribeEvent
    public void livingRemoveEffect(MobEffectEvent.Remove event) {
        if (ACCompat.vanillaEffect(event.getEffect()) instanceof DarknessIncarnateEffect darknessIncarnateEffect) {
            darknessIncarnateEffect.toggleFlight(event.getEntity(), false);
            event.getEntity().level().playSound(null, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), ACSoundRegistry.DARKNESS_INCARNATE_EXIT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (ACCompat.vanillaEffect(event.getEffect()) instanceof SugarRushEffect) {
            event.getEntity().level().playSound(null, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), ACSoundRegistry.SUGAR_RUSH_EXIT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }


    @SubscribeEvent
    public void livingAddEffect(MobEffectEvent.Added event) {
        if (ACCompat.rawEffect(event.getEffectInstance()) instanceof DarknessIncarnateEffect) {
            event.getEntity().level().playSound(null, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), ACSoundRegistry.DARKNESS_INCARNATE_ENTER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (ACCompat.rawEffect(event.getEffectInstance()) instanceof SugarRushEffect) {
            event.getEntity().level().playSound(null, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), ACSoundRegistry.SUGAR_RUSH_ENTER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (event.getEntity() instanceof Player player && ACCompat.isAddedToWorld(player) && ACCompat.rawEffect(event.getEffectInstance()) instanceof SugarRushEffect && AlexsCaves.COMMON_CONFIG.sugarRushSlowsTime.get()) {
            float timeBetweenTicksIncrease = 2F;
            SugarRushEffect.enterSlowMotion(player, player.level(), Mth.ceil(event.getEffectInstance().getDuration() * timeBetweenTicksIncrease), timeBetweenTicksIncrease);
        }
    }

    @SubscribeEvent
    public void livingExpireEffect(MobEffectEvent.Expired event) {
        if (ACCompat.rawEffect(event.getEffectInstance()) instanceof DarknessIncarnateEffect darknessIncarnateEffect) {
            darknessIncarnateEffect.toggleFlight(event.getEntity(), false);
            event.getEntity().playSound(ACSoundRegistry.DARKNESS_INCARNATE_EXIT.get());
        }
        if (event.getEntity() instanceof Player player && ACCompat.rawEffect(event.getEffectInstance()) instanceof SugarRushEffect && AlexsCaves.COMMON_CONFIG.sugarRushSlowsTime.get()) {
            SugarRushEffect.leaveSlowMotion(player, player.level());
        }
    }

    @SubscribeEvent
    public void travelToDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof Player player && player.hasEffect(ACCompat.effect(ACEffectRegistry.SUGAR_RUSH.get()))) {
            SugarRushEffect.leaveSlowMotion(player, player.level());
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (AlexsCaves.COMMON_CONFIG.sugarRushSlowsTime.get()) {
            ServerTickRateTracker tracker = ServerTickRateTracker.getForServer(event.getServer());
            tracker.tickRateModifierList.clear();
        }
        ACWorldWorkerManager.clear();
    }

    // The cave-map biome search runs on a tick-budgeted worker queue. That queue used to be the
    // loader's — Forge's WorldWorkerManager, which the server itself ticked — but NeoForge deleted
    // it in 21.9 and Fabric never had one, so the mod owns the queue now and has to drive it: the
    // clock is stamped at the start of the tick and the remainder of the tick's 50 ms is spent on
    // workers at the end of it, exactly where the loader used to call in.
    //
    // The three arms are the same tick-event split documented on CitadelEvents#onServerTick: one
    // event per target with Pre/Post subclasses from NeoForge 1.20.5 and from Forge 59.x, a single
    // event carrying `phase` before that.
    //? if forge && >=1.21.9 {
    /*@SubscribeEvent
    public void serverTickPre(TickEvent.ServerTickEvent.Pre event) {
        ACWorldWorkerManager.tick(true);
    }

    @SubscribeEvent
    public void serverTickPost(TickEvent.ServerTickEvent.Post event) {
        ACWorldWorkerManager.tick(false);
    }
    *///?} elif neoforge && >=1.20.5 {
    /*@SubscribeEvent
    public void serverTickPre(net.neoforged.neoforge.event.tick.ServerTickEvent.Pre event) {
        ACWorldWorkerManager.tick(true);
    }

    @SubscribeEvent
    public void serverTickPost(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        ACWorldWorkerManager.tick(false);
    }
    *///?} else {
    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        ACWorldWorkerManager.tick(event.phase == TickEvent.Phase.START);
    }
    //?}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        ACBiomeRarity.init();
        //moved from citadel
        RegistryAccess registryAccess = event.getServer().registryAccess();
        Registry<Biome> allBiomes = registryAccess.registryOrThrow(Registries.BIOME);
        Registry<LevelStem> levelStems = registryAccess.registryOrThrow(Registries.LEVEL_STEM);
        Map<ResourceKey<Biome>, Holder<Biome>> biomeMap = new HashMap<>();
        for (ResourceKey<Biome> biomeResourceKey : allBiomes.registryKeySet()) {
            Optional<Holder.Reference<Biome>> holderOptional = allBiomes.getHolder(biomeResourceKey);
            holderOptional.ifPresent(biomeHolder -> biomeMap.put(biomeResourceKey, biomeHolder));
        }
        for (ResourceKey<LevelStem> levelStemResourceKey : levelStems.registryKeySet()) {
            Optional<Holder.Reference<LevelStem>> holderOptional = levelStems.getHolder(levelStemResourceKey);
            if (holderOptional.isPresent() && holderOptional.get().value().generator().getBiomeSource() instanceof BiomeSourceAccessor expandedBiomeSource) {
                expandedBiomeSource.setResourceKeyMap(biomeMap);
                if (levelStemResourceKey.equals(LevelStem.OVERWORLD)) {
                    ImmutableSet.Builder<Holder<Biome>> biomeHolders = ImmutableSet.builder();
                    for (ResourceKey<Biome> biomeResourceKey : ACBiomeRegistry.ALEXS_CAVES_BIOMES) {
                        allBiomes.getHolder(biomeResourceKey).ifPresent(biomeHolders::add);
                    }
                    expandedBiomeSource.expandBiomesWith(biomeHolders.build());
                }
            }
        }
    }

    // See CitadelEvents#onServerTick for the 1.20.5 tick-event split. Forge's single event carries
    // the player in a public field and fires in both phases, so upstream ran this twice a tick;
    // Post alone is enough, and that is all NeoForge's Post does differently.
    // Forge 59.x split it the same way in 1.21.9; the field became the record accessor player().
    //? if forge && >=1.21.9 {
    /*@SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent.Post event) {
        tickPlayer(event.player());
    }
    *///?} elif neoforge && >=1.20.5 {
    /*@SubscribeEvent
    public void playerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        tickPlayer(event.getEntity());
    }
    *///?} else {
    @SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent event) {
        tickPlayer(event.player);
    }
    //?}

    private void tickPlayer(Player player) {
        if (!player.isCreative()) {
            if (player.getItemInHand(InteractionHand.MAIN_HAND).is(ACTagRegistry.RESTRICTED_BIOME_LOCATORS)) {
                checkAndDestroyExploitItem(player, EquipmentSlot.MAINHAND);
            }
            if (player.getItemInHand(InteractionHand.OFF_HAND).is(ACTagRegistry.RESTRICTED_BIOME_LOCATORS)) {
                checkAndDestroyExploitItem(player, EquipmentSlot.OFFHAND);
            }
        }
    }

    // ⚠️ Upstream wrote `isClientSide()` here. PlayerLoggedInEvent only ever fires for a
    // ServerPlayer, so that guard is unconditionally false and the warning has never been sent on
    // any loader or any version — including upstream 2.0.2. Inverted, which is plainly what was
    // meant: the message is built with a translation key and delivered over the network, i.e. it is
    // a server-side send.
    @SubscribeEvent
    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (AlexsCaves.COMMON_CONFIG.warnGenerationIncompatibility.get() && !AlexsCaves.MOD_GENERATION_CONFLICTS.isEmpty() && !event.getEntity().level().isClientSide()) {
            for (String modid : AlexsCaves.MOD_GENERATION_CONFLICTS) {
                if (CodxLib.isModLoaded(modid)) {
                    ACCompat.sendSystemMessage(event.getEntity(), Component.translatable("alexscaves.startup_warning.generation_incompatible", modid).withStyle(ChatFormatting.RED));
                }
            }
        }
    }

    private static void checkAndDestroyExploitItem(Player player, EquipmentSlot slot) {
        ItemStack itemInHand = player.getItemBySlot(slot);
        if (itemInHand.is(ACTagRegistry.RESTRICTED_BIOME_LOCATORS)) {
            CompoundTag tag = ACCompat.getTag(itemInHand);
            if (tag != null) {
                if (itemTagContainsAC(tag, "BiomeKey", false) || itemTagContainsAC(tag, "Structure", true) || itemTagContainsAC(tag, "structurecompass:structureName", true) || itemTagContainsAC(tag, "StructureKey", true)) {
                    // broadcastBreakEvent became onEquippedItemBroken, which wants the item as well
                    // as the slot. It has to be read before the shrink: an ItemStack that shrinks to
                    // zero reports Items.AIR from getItem(), and the item is what picks the break
                    // sound and the particle texture.
                    //? if >=1.21
                    /*net.minecraft.world.item.Item acBrokenItem = itemInHand.getItem();*/
                    itemInHand.shrink(1);
                    //? if >=1.21
                    /*player.onEquippedItemBroken(acBrokenItem, slot);*/
                    //? if <1.21
                    player.broadcastBreakEvent(slot);
                    player.playSound(ACSoundRegistry.DISAPPOINTMENT.get());
                    if (!player.level().isClientSide()) {
                        ACCompat.displayClientMessage(player, Component.translatable("item.alexscaves.natures_compass_warning"), true);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (event.getItemStack().getItem() == Items.GLASS_BOTTLE) {
            HitResult raytraceresult = getPlayerPOVHitResult(event.getLevel(), player, ClipContext.Fluid.SOURCE_ONLY);
            if (raytraceresult.getType() == HitResult.Type.BLOCK) {
                BlockPos blockpos = ((BlockHitResult) raytraceresult).getBlockPos();
                if (event.getLevel().mayInteract(player, blockpos)) {
                    if (event.getLevel().getFluidState(blockpos).is(ACTagRegistry.PURPLE_SODA)) {
                        player.gameEvent(GameEvent.ITEM_INTERACT_START);
                        event.getLevel().playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
                        player.awardStat(Stats.ITEM_USED.get(Items.GLASS_BOTTLE));
                        if (!player.addItem(new ItemStack(ACItemRegistry.PURPLE_SODA_BOTTLE.get()))) {
                            ACCompat.spawnAtLocation(player, new ItemStack(ACItemRegistry.PURPLE_SODA_BOTTLE.get()));
                        }
                        player.swing(event.getHand());
                        if (!player.isCreative()) {
                            event.getItemStack().shrink(1);
                        }
                    }
                }
            }
        }
    }

    private static BlockHitResult getPlayerPOVHitResult(Level level, Player player, ClipContext.Fluid fluid) {
        float f = player.getXRot();
        float f1 = player.getYRot();
        Vec3 vec3 = player.getEyePosition();
        float f2 = Mth.cos(-f1 * ((float) Math.PI / 180F) - (float) Math.PI);
        float f3 = Mth.sin(-f1 * ((float) Math.PI / 180F) - (float) Math.PI);
        float f4 = -Mth.cos(-f * ((float) Math.PI / 180F));
        float f5 = Mth.sin(-f * ((float) Math.PI / 180F));
        float f6 = f3 * f4;
        float f7 = f2 * f4;
        double d0 = ACCompat.blockReach(player);
        Vec3 vec31 = vec3.add((double) f6 * d0, (double) f5 * d0, (double) f7 * d0);
        return level.clip(new ClipContext(vec3, vec31, ClipContext.Block.OUTLINE, fluid, player));
    }

    // Vanilla's own anvil combine, re-implemented so that two of the same AlwaysCombinableOnAnvil
    // item always merge instead of being rejected as "same item, nothing to do".
    //
    // 1.20.5 replaced the Map<Enchantment, Integer> that this whole method is written around with
    // the ItemEnchantments component, and 1.21 rekeyed that component on Holder<Enchantment> and moved
    // the compatibility test onto the class as a static — so the three arms differ line by line rather
    // than in one call. The behaviour is identical throughout: same level-merge, same incompatibility
    // count, same cost.
    //? if >=1.21 {
    /*@SubscribeEvent
    public void onUpdateAnvil(AnvilUpdateEvent event) {
        if (event.getLeft().getItem() instanceof AlwaysCombinableOnAnvil && event.getLeft().getItem() == event.getRight().getItem() && !EnchantmentHelper.getEnchantmentsForCrafting(event.getLeft()).isEmpty() && !EnchantmentHelper.getEnchantmentsForCrafting(event.getRight()).isEmpty()) {
            ItemStack copy = event.getLeft().copy();
            net.minecraft.world.item.enchantment.ItemEnchantments.Mutable map = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(copy));
            net.minecraft.world.item.enchantment.ItemEnchantments map1 = EnchantmentHelper.getEnchantmentsForCrafting(event.getRight());
            boolean canCombine = true;
            int i = 0;
            for (net.minecraft.core.Holder<Enchantment> holder1 : map1.keySet()) {
                Enchantment enchantment1 = holder1.value();
                if (enchantment1 != null) {
                    int i2 = map.getLevel(holder1);
                    int j2 = map1.getLevel(holder1);
                    j2 = i2 == j2 ? j2 + 1 : Math.max(j2, i2);

                    for (net.minecraft.core.Holder<Enchantment> holder : map.keySet()) {
                        if (!holder.equals(holder1) && !Enchantment.areCompatible(holder1, holder)) {
                            canCombine = false;
                            ++i;
                        }
                    }

                    if (canCombine) {
                        if (j2 > enchantment1.getMaxLevel()) {
                            j2 = enchantment1.getMaxLevel();
                        }

                        map.set(holder1, j2);
                        // The Rarity enum this used to switch on is gone; an enchantment's scarcity
                        // is now just its weight in the random-enchant pool. These four weights are
                        // vanilla's old COMMON/UNCOMMON/RARE/VERY_RARE, so the costs are unchanged.
                        int k3 = 0;
                        switch (enchantment1.getWeight()) {
                            case 10:
                                k3 = 1;
                                break;
                            case 5:
                                k3 = 2;
                                break;
                            case 2:
                                k3 = 4;
                                break;
                            case 1:
                                k3 = 8;
                        }
                        i += k3 * j2;
                    }
                }
            }
            event.setCost(i);
            EnchantmentHelper.setEnchantments(copy, map.toImmutable());
            event.setOutput(copy);
        }
    }
    *///?} elif >=1.20.5 {
    /*@SubscribeEvent
    public void onUpdateAnvil(AnvilUpdateEvent event) {
        if (event.getLeft().getItem() instanceof AlwaysCombinableOnAnvil && event.getLeft().getItem() == event.getRight().getItem() && !EnchantmentHelper.getEnchantmentsForCrafting(event.getLeft()).isEmpty() && !EnchantmentHelper.getEnchantmentsForCrafting(event.getRight()).isEmpty()) {
            ItemStack copy = event.getLeft().copy();
            net.minecraft.world.item.enchantment.ItemEnchantments.Mutable map = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(copy));
            net.minecraft.world.item.enchantment.ItemEnchantments map1 = EnchantmentHelper.getEnchantmentsForCrafting(event.getRight());
            boolean canCombine = true;
            int i = 0;
            for (net.minecraft.core.Holder<Enchantment> holder1 : map1.keySet()) {
                Enchantment enchantment1 = holder1.value();
                if (enchantment1 != null) {
                    int i2 = map.getLevel(enchantment1);
                    int j2 = map1.getLevel(enchantment1);
                    j2 = i2 == j2 ? j2 + 1 : Math.max(j2, i2);

                    for (net.minecraft.core.Holder<Enchantment> holder : map.keySet()) {
                        Enchantment enchantment = holder.value();
                        if (enchantment != enchantment1 && !enchantment1.isCompatibleWith(enchantment)) {
                            canCombine = false;
                            ++i;
                        }
                    }

                    if (canCombine) {
                        if (j2 > enchantment1.getMaxLevel()) {
                            j2 = enchantment1.getMaxLevel();
                        }

                        map.set(enchantment1, j2);
                        // The Rarity enum this used to switch on is gone; an enchantment's scarcity
                        // is now just its weight in the random-enchant pool. These four weights are
                        // vanilla's old COMMON/UNCOMMON/RARE/VERY_RARE, so the costs are unchanged.
                        int k3 = 0;
                        switch (enchantment1.getWeight()) {
                            case 10:
                                k3 = 1;
                                break;
                            case 5:
                                k3 = 2;
                                break;
                            case 2:
                                k3 = 4;
                                break;
                            case 1:
                                k3 = 8;
                        }
                        i += k3 * j2;
                    }
                }
            }
            event.setCost(i);
            EnchantmentHelper.setEnchantments(copy, map.toImmutable());
            event.setOutput(copy);
        }
    }
    *///?} else {
    @SubscribeEvent
    public void onUpdateAnvil(AnvilUpdateEvent event) {
        if (event.getLeft().getItem() instanceof AlwaysCombinableOnAnvil && event.getLeft().getItem() == event.getRight().getItem() && !EnchantmentHelper.getEnchantments(event.getLeft()).isEmpty() && !EnchantmentHelper.getEnchantments(event.getRight()).isEmpty()) {
            Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(event.getLeft());
            Map<Enchantment, Integer> map1 = EnchantmentHelper.getEnchantments(event.getRight());
            boolean canCombine = true;
            int i = 0;
            for (Enchantment enchantment1 : map1.keySet()) {
                if (enchantment1 != null) {
                    int i2 = map.getOrDefault(enchantment1, 0);
                    int j2 = map1.get(enchantment1);
                    j2 = i2 == j2 ? j2 + 1 : Math.max(j2, i2);

                    for (Enchantment enchantment : map.keySet()) {
                        if (enchantment != enchantment1 && !enchantment1.isCompatibleWith(enchantment)) {
                            canCombine = false;
                            ++i;
                        }
                    }

                    if (canCombine) {
                        if (j2 > enchantment1.getMaxLevel()) {
                            j2 = enchantment1.getMaxLevel();
                        }

                        map.put(enchantment1, j2);
                        int k3 = 0;
                        switch (enchantment1.getRarity()) {
                            case COMMON:
                                k3 = 1;
                                break;
                            case UNCOMMON:
                                k3 = 2;
                                break;
                            case RARE:
                                k3 = 4;
                                break;
                            case VERY_RARE:
                                k3 = 8;
                        }
                        i += k3 * j2;
                    }
                }
            }
            event.setCost(i);
            ItemStack copy = event.getLeft().copy();
            EnchantmentHelper.setEnchantments(map, copy);
            event.setOutput(copy);
        }
    }
    //?}

    private static boolean itemTagContainsAC(CompoundTag tag, String tagID, boolean allowUndergroundCabin) {
        if (tag.contains(tagID)) {
            String resourceLocation = ACCompat.getString(tag, tagID);
            return resourceLocation.contains("alexscaves:") && (!allowUndergroundCabin || !resourceLocation.contains("underground_cabin"));
        }
        return false;
    }
}
