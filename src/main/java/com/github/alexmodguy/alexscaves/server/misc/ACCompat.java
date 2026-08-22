package com.github.alexmodguy.alexscaves.server.misc;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Vanilla APIs that 1.20.5 "component-ified" away, funnelled through one place.
 *
 * <p>Every method keeps the <em>pre</em>-1.20.5 signature, so a call site reads identically on all
 * nodes and the root source stays free of {@code //?} gates. Only the body is gated. This is the
 * same shape as the sibling repo's {@code AMCompat}; the divergences are noted per method.
 *
 * <p>⚠️ <b>Stack NBT does not round-trip for free.</b> On {@code >=1.20.5} the free-form stack tag is
 * the {@code custom_data} component, and {@link #getTag}/{@link #getOrCreateTag} hand back a
 * <em>copy</em> of it — mutating the returned tag does <em>not</em> touch the stack. Every caller
 * that changes anything must finish with {@link #setTag}. On 1.20.1 the same call sequence is a
 * harmless no-op write-back of the live tag, so one spelling is correct everywhere.
 *
 * <p>1.20.5–1.21.8 also had {@code CustomData#getUnsafe()}, which returned the live tag and would
 * have let callers skip the write-back; 1.21.9 deleted it. Taking the copy path on every node from
 * the start means there is no era where "it happens to persist" hides a missing {@link #setTag}.
 */
public class ACCompat {

    private ACCompat() {
    }

    // ── ItemStack NBT ──────────────────────────────────────────────────────────

    @Nullable
    public static CompoundTag getTag(ItemStack stack) {
        //? if >=1.20.5 {
        /*net.minecraft.world.item.component.CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
        *///?} else {
        return stack.getTag();
        //?}
    }

    public static CompoundTag getOrCreateTag(ItemStack stack) {
        //? if >=1.20.5 {
        /*net.minecraft.world.item.component.CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
        *///?} else {
        return stack.getOrCreateTag();
        //?}
    }

    public static void setTag(ItemStack stack, @Nullable CompoundTag tag) {
        //? if >=1.20.5 {
        /*if (tag == null) {
            stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        } else {
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        }
        *///?} else {
        stack.setTag(tag);
        //?}
    }

    public static boolean hasTag(ItemStack stack) {
        //? if >=1.20.5 {
        /*net.minecraft.world.item.component.CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data != null && !data.isEmpty();
        *///?} else {
        return stack.hasTag();
        //?}
    }

    // ── Custom names ───────────────────────────────────────────────────────────

    public static ItemStack setHoverName(ItemStack stack, @Nullable Component name) {
        //? if >=1.20.5 {
        /*stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, name);
        return stack;
        *///?} else {
        return stack.setHoverName(name);
        //?}
    }

    public static boolean hasCustomHoverName(ItemStack stack) {
        //? if >=1.20.5 {
        /*return stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        *///?} else {
        return stack.hasCustomHoverName();
        //?}
    }

    // ── Durability damage ──────────────────────────────────────────────────────
    // 1.20.1: hurtAndBreak(amount, entity, Consumer<T> onBroken) — the caller is responsible for
    // announcing the break, and every one of this mod's ~26 call sites does exactly the same
    // thing in that consumer: broadcastBreakEvent(slot). 1.20.5 replaced the consumer with the
    // EquipmentSlot itself and broadcasts the break internally. Three overloads, one per way the
    // call sites name the slot, so the pre-1.20.5 lambda body is reproduced verbatim.

    public static void hurtAndBreak(ItemStack stack, int amount, net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.entity.EquipmentSlot slot) {
        //? if >=1.20.5 {
        /*stack.hurtAndBreak(amount, entity, slot);
        *///?} else {
        stack.hurtAndBreak(amount, entity, e -> e.broadcastBreakEvent(slot));
        //?}
    }

    public static void hurtAndBreak(ItemStack stack, int amount, net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.InteractionHand hand) {
        //? if >=1.20.5 {
        /*stack.hurtAndBreak(amount, entity, slotOf(hand));
        *///?} else {
        stack.hurtAndBreak(amount, entity, e -> e.broadcastBreakEvent(hand));
        //?}
    }

    /**
     * The {@code e -> e.broadcastBreakEvent(e.getUsedItemHand())} shape: the hand is read off the
     * damaged entity itself. Kept as its own overload rather than folded into the one above so the
     * pre-1.20.5 nodes still resolve the hand lazily, at break time, exactly as upstream did.
     */
    public static void hurtAndBreakUsedHand(ItemStack stack, int amount, net.minecraft.world.entity.LivingEntity entity) {
        //? if >=1.20.5 {
        /*stack.hurtAndBreak(amount, entity, slotOf(entity.getUsedItemHand()));
        *///?} else {
        stack.hurtAndBreak(amount, entity, e -> e.broadcastBreakEvent(e.getUsedItemHand()));
        //?}
    }

    //? if >=1.20.5 {
    /*private static net.minecraft.world.entity.EquipmentSlot slotOf(net.minecraft.world.InteractionHand hand) {
        return hand == net.minecraft.world.InteractionHand.MAIN_HAND
                ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                : net.minecraft.world.entity.EquipmentSlot.OFFHAND;
    }
    *///?}

    // ── Registry holders ───────────────────────────────────────────────────────
    // 1.20.5 put effects and attributes behind Holders in most vanilla signatures, but this mod's
    // DeferredRegister handles still hand out the bare object, so wrap on the way out of the
    // registry. Below 1.20.5 both of these are the identity.
    //
    // ⚠️ vanillaEffect is not only for vanilla's own constants: MobEffectInstance#getEffect() is a
    // Holder from 1.20.5 too, and everything that inspects one has to come back through here. That
    // was not obvious until 26.2, because the two shapes that got it wrong both COMPILED on
    // 1.20.5→1.21.11 — Holder was a plain interface and MobEffect is not final, so javac had to
    // assume some subclass might implement both and allowed `holder != someMobEffect` (a reference
    // comparison that can never be true, silently disabling the seven magnetizing/irradiated
    // immunities) as well as `(SomeEffect) holder` (a downcast that throws ClassCastException the
    // moment it runs, in DarknessIncarnateEffect#getIntensity).
    //
    // 26.2 SEALED Holder — flags 0x0601 plus a PermittedSubclasses attribute, javap -v'd on both
    // nodes — so javac can now enumerate the implementors, prove no cast exists and reject both as
    // "incomparable types". That is how these were found, and routing them through vanillaEffect
    // repairs every node from 1.20.5 up, not just 26.2. Anywhere else this mod compares or casts a
    // Holder against a value type is worth the same look.

    //? if >=1.20.5 {
    /*public static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect(net.minecraft.world.effect.MobEffect effect) {
        return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
    }

    public static net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute(net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        return net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute);
    }

    public static net.minecraft.world.effect.MobEffect vanillaEffect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        return effect.value();
    }

    // ...and an identity overload beside it, because the two loaders disagree about one caller:
    // NeoForge moved MobEffectEvent.Remove#getEffect() to a Holder in the 1.20.5 sweep, Forge left it
    // returning the bare MobEffect (javap-checked on 52.1.15). Overload resolution picks the right
    // one per node, so the call site stays loader-neutral.
    public static net.minecraft.world.effect.MobEffect vanillaEffect(net.minecraft.world.effect.MobEffect effect) {
        return effect;
    }
    *///?} else {
    public static net.minecraft.world.effect.MobEffect effect(net.minecraft.world.effect.MobEffect effect) {
        return effect;
    }

    /** The inverse of {@link #effect}, for the {@code MobEffects} constants that became Holders. */
    public static net.minecraft.world.effect.MobEffect vanillaEffect(net.minecraft.world.effect.MobEffect effect) {
        return effect;
    }

    public static net.minecraft.world.entity.ai.attributes.Attribute attribute(net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        return attribute;
    }
    //?}

    // ── Mob types ──────────────────────────────────────────────────────────────
    // 1.20.5 deleted MobType. Everything it drove — the smite/bane-of-arthropods/impaling damage
    // bonuses, potion inversion, drowning — reads entity type tags instead, so this mod's eight
    // `getMobType` overrides became eight entries in data/minecraft/tags/entity_types/. These two
    // helpers cover the places that *asked* the question rather than answered it.

    /**
     * The enchantment damage bonus {@code stack} gets against {@code target}.
     *
     * <p>1.21 has no "what would this weapon add" query left: an enchantment's damage effect is a
     * list of conditional value-effects folded over a base number, so the only way to ask is to fold
     * them over zero and keep the result. That is exact for the {@code add} effects sharpness, smite
     * and bane of arthropods are built from, which is the whole of what this asked before.
     */
    public static float damageBonus(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.LivingEntity target) {
        //? if >=1.21 {
        /*if (target.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            return net.minecraft.world.item.enchantment.EnchantmentHelper.modifyDamage(
                    serverLevel, stack, target, target.damageSources().generic(), 0.0F);
        }
        return 0.0F;
        *///?} elif >=1.20.5 {
        /*return net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageBonus(stack, target.getType());
        *///?} else {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getDamageBonus(stack, target.getMobType());
        //?}
    }

    /** Whether {@code living} counts as a water creature — the old {@code MobType.WATER}. */
    public static boolean isAquatic(net.minecraft.world.entity.LivingEntity living) {
        //? if >=1.20.5 {
        /*return living.getType().builtInRegistryHolder().is(net.minecraft.tags.EntityTypeTags.AQUATIC);
        *///?} else {
        return living.getMobType() == net.minecraft.world.entity.MobType.WATER;
        //?}
    }

    // ── Enchantments ───────────────────────────────────────────────────────────
    // 1.21 turned enchantments into data-pack entries: Enchantment is a final record loaded from
    // JSON, code holds a ResourceKey or a Holder rather than the object, and most of the old
    // EnchantmentHelper conveniences were either deleted or turned into attributes. The handle type
    // therefore differs per version, which is why the level lookups below take a gated parameter
    // type — a call site passing `Enchantments.SILK_TOUCH` or an `ACEnchantmentRegistry` constant
    // reads the same on every node because the constant itself changes type with the version.
    //
    // See ACEnchantmentRegistry for the other half: this mod's own 51 enchantments are code-built
    // objects up to 1.20.6 and JSON under data/alexscaves/enchantment/ from 1.21.

    /**
     * The level of {@code ench} on {@code stack}, or 0.
     *
     * <p>The 1.21 arm reads the enchantments component directly rather than resolving the key
     * against the registry, so it needs no {@code RegistryAccess} and works off a bare stack the way
     * the old {@code ItemStack#getEnchantmentLevel} did. Like that method it looks only at applied
     * enchantments, not the stored ones an enchanted book carries.
     */
    //? if >=1.21 {
    /*public static int enchantLevel(ItemStack stack, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ench) {
        for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>> entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().is(ench)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }
    *///?} elif fabric {
    /*public static int enchantLevel(ItemStack stack, net.minecraft.world.item.enchantment.Enchantment ench) {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(ench, stack);
    }

    public static int enchantLevel(ItemStack stack, java.util.function.Supplier<net.minecraft.world.item.enchantment.Enchantment> ench) {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(ench.get(), stack);
    }
    *///?} else {
    public static int enchantLevel(ItemStack stack, net.minecraft.world.item.enchantment.Enchantment ench) {
        return stack.getEnchantmentLevel(ench);
    }

    // Up to 1.20.6 this mod's own enchantments are registry objects behind a Supplier, so they need
    // an overload of their own; from 1.21 they are ResourceKeys and the method above already serves.
    public static int enchantLevel(ItemStack stack, java.util.function.Supplier<net.minecraft.world.item.enchantment.Enchantment> ench) {
        return stack.getEnchantmentLevel(ench.get());
    }
    //?}

    /** The highest level of {@code ench} across everything {@code entity} has equipped. */
    //? if >=1.21 {
    /*public static int enchantLevelOn(net.minecraft.world.entity.LivingEntity entity, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> ench) {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(
                entity.level().registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                        .getHolderOrThrow(ench),
                entity);
    }
    *///?} else {
    public static int enchantLevelOn(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.item.enchantment.Enchantment ench) {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(ench, entity);
    }
    //?}

    /** Whether {@code entity} walks on water. Freezing is a location effect from 1.21, not a query. */
    public static boolean hasFrostWalker(net.minecraft.world.entity.LivingEntity entity) {
        return enchantLevelOn(entity, net.minecraft.world.item.enchantment.Enchantments.FROST_WALKER) > 0;
    }

    /**
     * Depth strider's strength as the 0–3 level the swim maths is written against.
     *
     * <p>1.21 replaced the enchantment lookup with the {@code water_movement_efficiency} attribute,
     * which the enchantment drives from 0 to 1 across its three levels — so the level is the
     * attribute times three, and unlike the old lookup it also picks up any other source.
     */
    public static float depthStriderLevel(net.minecraft.world.entity.LivingEntity entity) {
        //? if >=1.21 {
        /*return 3.0F * (float) entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.WATER_MOVEMENT_EFFICIENCY);
        *///?} else {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getDepthStrider(entity);
        //?}
    }

    /**
     * What efficiency adds to a dig speed, for the copies of {@code Player#getDestroySpeed} this mod
     * keeps. The {@code level*level + 1} curve moved into the {@code mining_efficiency} attribute in
     * 1.21, so the two arms compute the same number from either side of that move.
     */
    public static float miningEfficiencyBonus(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        //? if >=1.21 {
        /*return (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MINING_EFFICIENCY);
        *///?} else {
        int level = net.minecraft.world.item.enchantment.EnchantmentHelper.getBlockEfficiency(player);
        return level > 0 && !stack.isEmpty() ? (float) (level * level + 1) : 0.0F;
        //?}
    }

    /**
     * What to multiply a dig speed by while the digger's head is underwater — the aqua affinity
     * question, which 1.21 answers with the {@code submerged_mining_speed} attribute whose default is
     * the same one-fifth penalty the boolean used to select.
     */
    public static float submergedMiningFactor(net.minecraft.world.entity.player.Player player) {
        //? if >=1.21 {
        /*return (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SUBMERGED_MINING_SPEED);
        *///?} else {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.hasAquaAffinity(player) ? 1.0F : 0.2F;
        //?}
    }

    /**
     * Runs both directions of post-attack enchantment effects — the victim's armour against the
     * attacker (thorns) and the attacker's weapon against the victim (fire aspect, and friends).
     *
     * <p>1.21 merged the two calls into one that walks every participant's effects itself, and it
     * only exists server-side; before that they were two static helpers with no such requirement.
     */
    public static void postAttackEffects(net.minecraft.world.entity.LivingEntity victim, net.minecraft.world.entity.LivingEntity attacker, net.minecraft.world.damagesource.DamageSource source) {
        //? if >=1.21 {
        /*if (victim.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(serverLevel, victim, source);
        }
        *///?} else {
        net.minecraft.world.item.enchantment.EnchantmentHelper.doPostHurtEffects(victim, attacker);
        net.minecraft.world.item.enchantment.EnchantmentHelper.doPostDamageEffects(attacker, victim);
        //?}
    }

    /**
     * The same thing for the one caller that reached it through {@code LivingEntity}, which had a
     * method of its own until 1.21 folded it into the helper above.
     */
    public static void postAttackEffects(net.minecraft.world.entity.LivingEntity attacker, net.minecraft.world.entity.Entity target, net.minecraft.world.damagesource.DamageSource source) {
        //? if >=1.21 {
        /*if (target.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(serverLevel, target, source);
        }
        *///?} else {
        attacker.doEnchantDamageEffects(attacker, target);
        //?}
    }

    /** An explosion's knockback on {@code living} after protection dampens it. */
    public static double explosionKnockback(net.minecraft.world.entity.LivingEntity living, double knockback) {
        //? if >=1.21 {
        /*return knockback * (1.0D - living.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.EXPLOSION_KNOCKBACK_RESISTANCE));
        *///?} else {
        return net.minecraft.world.item.enchantment.ProtectionEnchantment.getExplosionKnockbackAfterDampener(living, knockback);
        //?}
    }

    /**
     * Enchants {@code stack} as loot would, at the given cost.
     *
     * <p>Three arms, because this signature changed twice. 1.20.5 prepended the level's feature flags
     * so enchantments from a disabled pack are skipped; 1.21 dropped the flags again and asks instead
     * for the registry to draw from plus the set to draw out of — and {@code #on_random_loot} is the
     * set the old {@code allowTreasure = false} form selected implicitly.
     *
     * <p>1.21.2 is the same call with two renamed lookups: {@code Registry#getTag} is {@code get}
     * (its {@code getTag} name went to the {@code getTagOrEmpty} shape, which is not this one — it
     * returns the holders rather than the {@code Optional<HolderSet>} the enchant call wants).
     */
    public static ItemStack enchantRandomly(net.minecraft.util.RandomSource random, ItemStack stack, int cost, net.minecraft.world.level.Level level) {
        //? if >=1.21.2 {
        /*net.minecraft.core.RegistryAccess access = level.registryAccess();
        return net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(random, stack, cost, access,
                access.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                        .get(net.minecraft.tags.EnchantmentTags.ON_RANDOM_LOOT));
        *///?} elif >=1.21 {
        /*net.minecraft.core.RegistryAccess access = level.registryAccess();
        return net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(random, stack, cost, access,
                access.registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                        .getTag(net.minecraft.tags.EnchantmentTags.ON_RANDOM_LOOT));
        *///?} elif >=1.20.5 {
        /*return net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(level.enabledFeatures(), random, stack, cost, false);
        *///?} else {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(random, stack, cost, false);
        //?}
    }

    /** Whether {@code stack} keeps itself out of the drops — the curse of vanishing. */
    public static boolean hasVanishingCurse(ItemStack stack) {
        //? if >=1.21 {
        /*return net.minecraft.world.item.enchantment.EnchantmentHelper.has(stack, net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP);
        *///?} else {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.hasVanishingCurse(stack);
        //?}
    }

    // ── Item attribute modifiers ───────────────────────────────────────────────
    // Up to 1.20.4 an item answered "what do I add to the wearer" with a
    // Multimap<Attribute, AttributeModifier> per equipment slot. 1.20.5 replaced that with the
    // ItemAttributeModifiers data component: one flat list of (attribute, modifier, slot group)
    // entries for the whole item.
    //
    // The mod keeps building multimaps — several of these items vary their modifiers with damage or
    // an NBT value, so a static component would not do — and converts at the boundary. The multimap
    // type itself is version-independent in source thanks to the !mc205-attrmap-* replacements,
    // which retype its key to Holder<Attribute>.

    /** {@code AttributeModifier} became a record in 1.20.5; these two are its renamed accessors. */
    public static double amount(net.minecraft.world.entity.ai.attributes.AttributeModifier modifier) {
        //? if >=1.20.5 {
        /*return modifier.amount();
        *///?} else {
        return modifier.getAmount();
        //?}
    }

    public static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation(net.minecraft.world.entity.ai.attributes.AttributeModifier modifier) {
        //? if >=1.20.5 {
        /*return modifier.operation();
        *///?} else {
        return modifier.getOperation();
        //?}
    }

    // Presents a slot's worth of multimap entries as the component 1.20.5 expects, mapping the
    // equipment slot to the single-slot EquipmentSlotGroup that matches it. `fallback` is the
    // superclass' answer, used when this item has nothing of its own to say for the slot — a
    // supplier because evaluating it means walking the whole item hierarchy.
    //? if >=1.20.5 {
    /*public static net.minecraft.world.item.component.ItemAttributeModifiers itemAttributes(
            com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier> modifiers,
            net.minecraft.world.entity.EquipmentSlot slot,
            java.util.function.Supplier<net.minecraft.world.item.component.ItemAttributeModifiers> fallback) {
        if (modifiers == null) {
            return fallback.get();
        }
        net.minecraft.world.entity.EquipmentSlotGroup group = net.minecraft.world.entity.EquipmentSlotGroup.bySlot(slot);
        net.minecraft.world.item.component.ItemAttributeModifiers.Builder builder = net.minecraft.world.item.component.ItemAttributeModifiers.builder();
        modifiers.forEach((attribute, modifier) -> builder.add(attribute, modifier, group));
        return builder.build();
    }
    *///?}

    // The same thing where the answer has to cover the whole item at once rather than one slot.
    // NeoForge's IItemExtension kept only that shape from 1.20.5 — getAttributeModifiers(ItemStack),
    // with no slot to answer for — and Forge from 1.21.2 has no per-item hook at all, so
    // ItemStackAttributeModifiersMixin reads the finished component and needs the same merge. The
    // seven items are asked once for every slot and the answers are combined. Each of them speaks
    // for exactly one slot and says "nothing" for the rest, so the merge is the union; `fallback` is
    // reached only if an item declines every slot, which none of them do.
    //? if >=1.20.5 {
    /*public static net.minecraft.world.item.component.ItemAttributeModifiers itemAttributes(
            java.util.function.BiFunction<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.ItemStack,
                    com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier>> modifiers,
            net.minecraft.world.item.ItemStack stack,
            java.util.function.Supplier<net.minecraft.world.item.component.ItemAttributeModifiers> fallback) {
        net.minecraft.world.item.component.ItemAttributeModifiers.Builder builder = net.minecraft.world.item.component.ItemAttributeModifiers.builder();
        boolean answered = false;
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            com.google.common.collect.Multimap<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>, net.minecraft.world.entity.ai.attributes.AttributeModifier> mine =
                    modifiers.apply(slot, stack);
            if (mine == null) {
                continue;
            }
            answered = true;
            net.minecraft.world.entity.EquipmentSlotGroup group = net.minecraft.world.entity.EquipmentSlotGroup.bySlot(slot);
            mine.forEach((attribute, modifier) -> builder.add(attribute, modifier, group));
        }
        return answered ? builder.build() : fallback.get();
    }
    *///?}

    /**
     * The total attack damage a held stack adds — the sum of its {@code ATTACK_DAMAGE} modifiers in
     * the main hand, ignoring their operations exactly as the two callers always have.
     *
     * <p>Up to 1.20.4 this meant asking the stack for its main-hand multimap and reading one key out
     * of it. 1.20.5 removed {@code ItemStack#getAttributeModifiers} along with the multimap, leaving
     * {@code forEachModifier} as the way to enumerate the finished set.
     */
    public static double attackDamageBonus(net.minecraft.world.item.ItemStack stack) {
        //? if >=1.20.5 {
        /*double[] total = new double[1];
        stack.forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.value() == net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE.value()) {
                total[0] += modifier.amount();
            }
        });
        return total[0];
        *///?} else {
        double total = 0;
        for (net.minecraft.world.entity.ai.attributes.AttributeModifier modifier :
                stack.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND).get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
            total += modifier.getAmount();
        }
        return total;
        //?}
    }

    // ── Potions ────────────────────────────────────────────────────────────────
    // 1.20.5 replaced PotionUtils' NBT juggling with the `potion_contents` data component, and turned
    // the vanilla Potions constants into Holders at the same time. This mod still thinks in bare
    // Potion objects — its own eleven come out of a DeferredRegister that way — so the conversion
    // happens here rather than at each of the twelve call sites.

    /** The stack a brewing recipe hands out for {@code potion}. */
    public static ItemStack potionStack(net.minecraft.world.item.Item item, net.minecraft.world.item.alchemy.Potion potion) {
        //? if >=1.20.5 {
        /*return net.minecraft.world.item.alchemy.PotionContents.createItemStack(item, potionHolder(potion));
        *///?} else {
        return net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(item), potion);
        //?}
    }

    // The vanilla constants (Potions.AWKWARD and friends) are the one place the mod reads a potion
    // it did not register itself, and they are exactly what changed type — hence a helper whose
    // *parameter* is gated rather than only its body.
    //? if >=1.20.5 {
    /*public static net.minecraft.world.item.alchemy.Potion vanillaPotion(net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
        return potion.value();
    }

    private static net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potionHolder(net.minecraft.world.item.alchemy.Potion potion) {
        return net.minecraft.core.registries.BuiltInRegistries.POTION.wrapAsHolder(potion);
    }

    private static net.minecraft.world.item.alchemy.PotionContents contentsOf(ItemStack stack) {
        return stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, net.minecraft.world.item.alchemy.PotionContents.EMPTY);
    }
    *///?} else {
    public static net.minecraft.world.item.alchemy.Potion vanillaPotion(net.minecraft.world.item.alchemy.Potion potion) {
        return potion;
    }
    //?}

    /**
     * The potion a stack carries, or {@code null} if it carries none.
     *
     * <p>Below 1.20.5 "none" was the {@code Potions.EMPTY} sentinel, which 1.20.5 deleted in favour of
     * an absent component; both are normalised to {@code null} so the callers' guards read the same.
     */
    @Nullable
    public static net.minecraft.world.item.alchemy.Potion potionOf(ItemStack stack) {
        //? if >=1.20.5 {
        /*return contentsOf(stack).potion().map(net.minecraft.core.Holder::value).orElse(null);
        *///?} else {
        net.minecraft.world.item.alchemy.Potion potion = net.minecraft.world.item.alchemy.PotionUtils.getPotion(stack);
        return potion == net.minecraft.world.item.alchemy.Potions.EMPTY ? null : potion;
        //?}
    }

    /** Every effect a stack would apply — its potion's, plus any custom ones written onto it. */
    public static Iterable<net.minecraft.world.effect.MobEffectInstance> potionEffects(ItemStack stack) {
        //? if >=1.20.5 {
        /*return contentsOf(stack).getAllEffects();
        *///?} else {
        return net.minecraft.world.item.alchemy.PotionUtils.getMobEffects(stack);
        //?}
    }

    /**
     * The tint a potion is drawn with. 1.21.4 deleted the static {@code getColor(Holder<Potion>)} —
     * the colour is a property of the effect list now, and {@code getColorOptional} over the potion's
     * own effects is exactly what the deleted method computed, base colour and all.
     */
    public static int potionColor(net.minecraft.world.item.alchemy.Potion potion) {
        //? if >=1.21.4 {
        /*return net.minecraft.world.item.alchemy.PotionContents.getColorOptional(potion.getEffects())
                .orElse(net.minecraft.world.item.alchemy.PotionContents.BASE_POTION_COLOR);
        *///?} elif >=1.20.5 {
        /*return net.minecraft.world.item.alchemy.PotionContents.getColor(potionHolder(potion));
        *///?} else {
        return net.minecraft.world.item.alchemy.PotionUtils.getColor(potion);
        //?}
    }

    /** …and the tint of whatever potion a stack happens to hold. */
    public static int potionColor(ItemStack stack) {
        //? if >=1.20.5 {
        /*return contentsOf(stack).getColor();
        *///?} else {
        return net.minecraft.world.item.alchemy.PotionUtils.getColor(stack);
        //?}
    }

    /** {@code ItemStack.isSameItemSameTags}, renamed when stack NBT became components. */
    public static boolean sameItemSameData(ItemStack a, ItemStack b) {
        //? if >=1.20.5 {
        /*return ItemStack.isSameItemSameComponents(a, b);
        *///?} else {
        return ItemStack.isSameItemSameTags(a, b);
        //?}
    }

    /**
     * The container a stack leaves behind when it is consumed, as a fresh stack — {@code EMPTY} when
     * it leaves none.
     *
     * <p>26 gave the remainder a type of its own: it is an {@code ItemStackTemplate} (item + count +
     * component patch, no mutation), stored on the {@code Item} and reached through Forge's own
     * {@code IForgeItemStack#getCraftingRemainder}, which is {@code null} for an item that declared
     * no {@code craftRemainder}. {@code create()} turns it into the ordinary stack the two callers
     * here want to drop, so the null and the old {@code EMPTY} collapse back into one answer.
     *
     * <p>Vanilla's own answer to the same question moved in 1.21.2: {@code Item#craftingRemainingItem}
     * became the {@code minecraft:use_remainder}-adjacent {@code Item#getCraftingRemainder()}, which
     * hands back an {@code ItemStack} — {@code EMPTY} for an item that leaves nothing — and allocates
     * it fresh on every call (read in the bytecode, not assumed), so the Fabric arm neither copies it
     * nor needs a null check. Below that version the pair it replaces is what Fabric reads.
     */
    public static ItemStack craftingRemainder(ItemStack stack) {
        //? if >=26 {
        /*net.minecraft.world.item.ItemStackTemplate remainder = stack.getCraftingRemainder();
        return remainder == null ? ItemStack.EMPTY : remainder.create();
        *///?} elif fabric && >=1.21.2 {
        /*return stack.getItem().getCraftingRemainder();
        *///?} elif fabric {
        /*net.minecraft.world.item.Item item = stack.getItem();
        return item.hasCraftingRemainingItem() ? new ItemStack(item.getCraftingRemainingItem()) : ItemStack.EMPTY;
        *///?} else {
        return stack.getCraftingRemainingItem().copy();
        //?}
    }

    /**
     * The dye colour a stack applies, or {@code null} if it is not a dye.
     *
     * <p>26 moved the colour off {@code DyeItem} and onto the {@code minecraft:dye} data component —
     * {@code getDyeColor()} is gone, and vanilla's own {@code interactLivingEntity} reads
     * {@code stack.get(DataComponents.DYE)} instead — which also means a stack can now carry a colour
     * its item does not, so the answer belongs to the stack rather than to the item on every version.
     */
    public static net.minecraft.world.item.DyeColor dyeColorOf(ItemStack stack) {
        //? if >=26 {
        /*return stack.get(net.minecraft.core.component.DataComponents.DYE);
        *///?} else {
        return stack.getItem() instanceof net.minecraft.world.item.DyeItem dye ? dye.getDyeColor() : null;
        //?}
    }

    // ── Sound events ───────────────────────────────────────────────────────────
    // 1.20.5 turned every constant in `SoundEvents` into a Holder. Most of this mod's own sounds
    // come out of its DeferredRegister as bare SoundEvents, so the two worlds meet constantly;
    // these two convert in either direction and are the identity below 1.20.5.

    //? if >=1.20.5 {
    /*public static net.minecraft.sounds.SoundEvent rawSound(net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound) {
        return sound.value();
    }

    // 1.20.5 only converted the constants that something asked for as a Holder; 1.21.5 finished the
    // job, so nine of them changed type under call sites that want the sound itself. An identity
    // overload beside the unwrapping one lets those sites read the same on every node from 1.20.1 up.
    public static net.minecraft.sounds.SoundEvent rawSound(net.minecraft.sounds.SoundEvent sound) {
        return sound;
    }

    public static net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> soundHolder(net.minecraft.sounds.SoundEvent sound) {
        return net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
    }
    *///?} else {
    public static net.minecraft.sounds.SoundEvent rawSound(net.minecraft.sounds.SoundEvent sound) {
        return sound;
    }

    public static net.minecraft.sounds.SoundEvent soundHolder(net.minecraft.sounds.SoundEvent sound) {
        return sound;
    }
    //?}

    // ── Block-entity NBT ───────────────────────────────────────────────────────
    // 1.20.5 threaded a HolderLookup.Provider through every BlockEntity read/write, because item
    // stacks inside them now serialise data components and components hold registry references.
    // Inside a BlockEntity the provider arrives as a parameter, so those sites are handled by the
    // !mc205-be-* replacements. These two helpers are for the handful of *outside* callers, which
    // have a BlockEntity but no provider of their own and must fetch one from the level.

    /**
     * The registries {@code be} should (de)serialise against — {@code EMPTY} if it isn't in a level yet.
     *
     * <p>Public because from 1.21.6 the {@code !mc216-be-savesig-*} rule uses it too: a
     * {@code ValueOutput} publishes no lookup of its own (see the note further down), and unlike the
     * load path — which is exactly why this returns {@code EMPTY} rather than throwing — a block
     * entity being saved is in a level and can simply be asked.
     */
    public static net.minecraft.core.HolderLookup.Provider registriesOf(net.minecraft.world.level.block.entity.BlockEntity be) {
        net.minecraft.world.level.Level level = be.getLevel();
        return level == null ? net.minecraft.core.RegistryAccess.EMPTY : level.registryAccess();
    }

    /** Snapshot a block entity's contents without its id/position — the moving-block machinery's format. */
    public static net.minecraft.nbt.CompoundTag saveBlockEntity(net.minecraft.world.level.block.entity.BlockEntity be) {
        //? if >=1.20.5 {
        /*return be.saveWithoutMetadata(registriesOf(be));
        *///?} else {
        return be.saveWithoutMetadata();
        //?}
    }

    /** The inverse of {@link #saveBlockEntity}: restore that snapshot onto a freshly placed block entity. */
    public static void loadBlockEntity(net.minecraft.world.level.block.entity.BlockEntity be, net.minecraft.nbt.CompoundTag tag) {
        //? if >=1.21.6 {
        /*be.loadWithComponents(asInput(tag, registriesOf(be)));
        *///?} elif >=1.20.5 {
        /*be.loadWithComponents(tag, registriesOf(be));
        *///?} else {
        be.load(tag);
        //?}
    }

    // ── Spawn placement ────────────────────────────────────────────────────────

    /**
     * "Could {@code type} legally spawn on the block at {@code pos}" — the check both the amber
     * monolith and {@code NaturalSpawnerMixin} do before summoning.
     *
     * <p>Up to 1.20.4 this was {@code NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type, …)}: the
     * caller resolved the entity's placement type itself and passed it alongside the type. 1.20.5
     * deleted that method — {@code SpawnPlacements.Type} became an interface owning the check, so the
     * resolution is now internal and the entry point moved to {@code SpawnPlacements} with the
     * placement argument gone.
     */
    public static boolean spawnPositionOk(net.minecraft.world.entity.EntityType<?> type, net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos) {
        //? if >=1.20.5 {
        /*return net.minecraft.world.entity.SpawnPlacements.isSpawnPositionOk(type, level, pos);
        *///?} else {
        return net.minecraft.world.level.NaturalSpawner.isSpawnPositionOk(net.minecraft.world.entity.SpawnPlacements.getPlacementType(type), level, pos, type);
        //?}
    }

    /** …and the inverse, for the places that want the bare {@code MobEffect} back out of an instance. */
    public static net.minecraft.world.effect.MobEffect rawEffect(net.minecraft.world.effect.MobEffectInstance instance) {
        //? if >=1.20.5 {
        /*return instance.getEffect().value();
        *///?} else {
        return instance.getEffect();
        //?}
    }

    // ── EntityDimensions ───────────────────────────────────────────────────────
    // 1.20.5 turned EntityDimensions into a record, so the two public final fields became accessor
    // methods of the same name. Nothing else about them changed.

    public static float width(net.minecraft.world.entity.EntityDimensions dimensions) {
        //? if >=1.20.5 {
        /*return dimensions.width();
        *///?} else {
        return dimensions.width;
        //?}
    }

    public static float height(net.minecraft.world.entity.EntityDimensions dimensions) {
        //? if >=1.20.5 {
        /*return dimensions.height();
        *///?} else {
        return dimensions.height;
        //?}
    }

    /**
     * {@code new EntityDimensions(w, h, fixed)} — the record grew an eye height and an attachment set
     * in 1.20.5, and the two static factories are the only supported way in.
     */
    public static net.minecraft.world.entity.EntityDimensions dimensions(float width, float height, boolean fixed) {
        return fixed
                ? net.minecraft.world.entity.EntityDimensions.fixed(width, height)
                : net.minecraft.world.entity.EntityDimensions.scalable(width, height);
    }

    /**
     * {@code mob.finalizeSpawn(level, difficulty, reason, data, tag)} for the call sites that only ever
     * passed {@code null} for the tag — which is all of them here.
     *
     * <p>1.20.5 dropped the trailing spawn-data {@code CompoundTag}: {@code EntityType} applies the
     * stack's {@code entity_data} component itself now, after the mob is finalised. The overrides
     * scattered through the entity classes still have to gate their own signatures, but a plain call
     * can go through here instead.
     */
    @Nullable
    public static net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.entity.Mob mob,
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.MobSpawnType reason,
            @Nullable net.minecraft.world.entity.SpawnGroupData data) {
        //? if >=1.20.5 {
        /*return mob.finalizeSpawn(level, difficulty, reason, data);
        *///?} else {
        return mob.finalizeSpawn(level, difficulty, reason, data, null);
        //?}
    }

    // ── 1.21.2 threaded a ServerLevel through the damage and drop chain ────────
    // Entity#hurt is final and void from 1.21.2: it checks that the entity is on a ServerLevel and
    // forwards to the new abstract Entity#hurtServer(ServerLevel, DamageSource, float). Every
    // override in this mod is renamed to hurtServer by the !mc2102-hurt-* rules, and the ones that
    // call super go with it, because the level is a parameter there. These helpers are for the
    // other side — the ~20 call sites that ask something else to take damage, or to drop an item,
    // from a context that has no ServerLevel to hand.
    //
    // Each derives the level from the entity it is acting on, which is what the method itself used
    // to do. On the client that leaves nothing to do, so the boolean answers are false and the item
    // spawns are null — the same outcome the old server-only bodies produced, one frame earlier.

    /**
     * {@code entity.hurt(source, amount)} for the call sites that read the boolean back.
     *
     * <p>Only those: a call that ignores the result still compiles on every version, since 1.21.2's
     * {@code hurt} is void rather than gone, and it still routes to {@code hurtServer} exactly as
     * before. {@code hurtOrSimulate} is vanilla's own shim for this — deprecated in the sense of
     * "new code should know which side it is on", which these call sites do not.
     */
    public static boolean hurt(net.minecraft.world.entity.Entity entity, net.minecraft.world.damagesource.DamageSource source, float amount) {
        //? if >=1.21.2 {
        /*return entity.hurtOrSimulate(source, amount);
        *///?} else {
        return entity.hurt(source, amount);
        //?}
    }

    /**
     * {@code level.getGameRules().getBoolean(rule)}.
     *
     * <p>1.21.2 moved {@code getGameRules} off {@code Level} and onto {@code ServerLevel}: the
     * client never had a real copy of the rules, it had whatever the integrated server happened to
     * be holding, which is nothing at all in multiplayer. So the answer a client gets is now the
     * caller's problem, and every call site states it — always the rule's own vanilla default,
     * which is what the old code effectively assumed a remote client would see.
     *
     * <p>Every one of this mod's uses is a "may I break/drop this" question asked while resolving
     * something the server decides anyway, so the fallback only ever colours a client-side
     * prediction.
     */
    //? if >=1.21.11 {
    /*public static boolean gameRule(net.minecraft.world.level.Level level, net.minecraft.world.level.gamerules.GameRule<Boolean> rule, boolean clientAnswer) {
        return level instanceof net.minecraft.server.level.ServerLevel serverLevel ? serverLevel.getGameRules().get(rule) : clientAnswer;
    }

    public static int gameRule(net.minecraft.world.level.Level level, net.minecraft.world.level.gamerules.GameRule<Integer> rule, int clientAnswer) {
        return level instanceof net.minecraft.server.level.ServerLevel serverLevel ? serverLevel.getGameRules().get(rule) : clientAnswer;
    }
    *///?} elif >=1.21.2 {
    /*public static boolean gameRule(net.minecraft.world.level.Level level, net.minecraft.world.level.GameRules.Key<net.minecraft.world.level.GameRules.BooleanValue> rule, boolean clientAnswer) {
        return level instanceof net.minecraft.server.level.ServerLevel serverLevel ? serverLevel.getGameRules().getBoolean(rule) : clientAnswer;
    }

    public static int gameRule(net.minecraft.world.level.Level level, net.minecraft.world.level.GameRules.Key<net.minecraft.world.level.GameRules.IntegerValue> rule, int clientAnswer) {
        return level instanceof net.minecraft.server.level.ServerLevel serverLevel ? serverLevel.getGameRules().getInt(rule) : clientAnswer;
    }
    *///?} else {
    public static boolean gameRule(net.minecraft.world.level.Level level, net.minecraft.world.level.GameRules.Key<net.minecraft.world.level.GameRules.BooleanValue> rule, boolean clientAnswer) {
        return level.getGameRules().getBoolean(rule);
    }

    public static int gameRule(net.minecraft.world.level.Level level, net.minecraft.world.level.GameRules.Key<net.minecraft.world.level.GameRules.IntegerValue> rule, int clientAnswer) {
        return level.getGameRules().getInt(rule);
    }
    //?}

    /**
     * {@code entity.isInvulnerableTo(source)} asked about <em>another</em> entity.
     *
     * <p>1.21.2 split this in two: {@code LivingEntity} kept an overridable
     * {@code isInvulnerableTo}, which gained a {@code ServerLevel} because it consults enchantment
     * immunities, while {@code Entity}'s became {@code protected final isInvulnerableToBase} and
     * stopped being reachable from outside the class. The two call sites here — a boundroid asking
     * about its winch's body, a sauropod part asking about its parent — hold an {@code Entity} that
     * is in practice always living, so the living branch is the one that runs.
     *
     * <p>The other branch reimplements {@code isInvulnerableToBase} from its public parts. It is
     * faithful except for the loader's own hook (NeoForge routes the result through
     * {@code CommonHooks.isEntityInvulnerableTo}), which no mod can call in its place.
     *
     * <p>Both call sites sit inside a {@code hurt} override, which from 1.21.2 is
     * {@code hurtServer} and only ever runs on the server, so the {@code false} a client-side
     * living entity gets back is unreachable from either of them.
     */
    public static boolean isInvulnerableTo(net.minecraft.world.entity.Entity entity, net.minecraft.world.damagesource.DamageSource source) {
        //? if >=1.21.2 {
        /*if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
            return living.level() instanceof net.minecraft.server.level.ServerLevel serverLevel && living.isInvulnerableTo(serverLevel, source);
        }
        return entity.isRemoved()
                || entity.isInvulnerable() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.isCreativePlayer()
                || source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) && entity.fireImmune()
                || source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) && entity.getType().builtInRegistryHolder().is(net.minecraft.tags.EntityTypeTags.FALL_DAMAGE_IMMUNE);
        *///?} else {
        return entity.isInvulnerableTo(source);
        //?}
    }

    public static boolean doHurtTarget(net.minecraft.world.entity.LivingEntity attacker, net.minecraft.world.entity.Entity target) {
        //? if >=1.21.2 {
        /*return attacker.level() instanceof net.minecraft.server.level.ServerLevel serverLevel && attacker.doHurtTarget(serverLevel, target);
        *///?} else {
        return attacker.doHurtTarget(target);
        //?}
    }

    /**
     * Removes {@code entity} outright, as {@code /kill} does.
     *
     * <p>Part of the same 1.21.2 sweep as {@link #hurt}: {@code Entity#kill} takes the
     * {@code ServerLevel} it is dying on, because the death loot and the advancement triggers it
     * runs need one. A client-side call is a no-op, which it effectively was before too.
     */
    public static void kill(net.minecraft.world.entity.Entity entity) {
        //? if >=1.21.2 {
        /*if (entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            entity.kill(serverLevel);
        }
        *///?} else {
        entity.kill();
        //?}
    }

    /**
     * Turns {@code mob} into a fresh {@code type}, as a zombie villager becomes a villager.
     *
     * <p>1.21.2 replaced the single {@code transferInventory} flag with a {@code ConversionParams}
     * record and a post-conversion callback. The flag used to mean two things at once — carry the
     * equipment over <i>and</i> carry {@code canPickUpLoot} — which is exactly the pair
     * {@code ConversionParams.single} now takes separately, so {@code true} maps to
     * {@code (true, true)}. The callback is where vanilla's own conversions do their extra setup;
     * both of this mod's callers do theirs on the returned mob instead, so it stays empty.
     *
     * <p>One real behaviour difference, and it costs nothing here: the old call re-seated the new
     * mob on whatever the old one was riding, and the new one does not. Both callers
     * ({@code GloomothEntity} → watcher, {@code VesperEntity} → forsaken) call {@code stopRiding}
     * on the line above and {@code stopRiding} on the result afterwards, so neither wanted it.
     */
    @Nullable
    public static <T extends net.minecraft.world.entity.Mob> T convertTo(net.minecraft.world.entity.Mob mob, net.minecraft.world.entity.EntityType<T> type, boolean transferInventory) {
        //? if >=1.21.2 {
        /*return mob.convertTo(type, net.minecraft.world.entity.ConversionParams.single(mob, transferInventory, transferInventory), converted -> {
        });
        *///?} else {
        return mob.convertTo(type, transferInventory);
        //?}
    }

    /**
     * Knocks a blocking player's shield out of action for the usual five seconds.
     *
     * <p>Three shapes across this range. Up to 1.20.4 it was the loaders'
     * {@code disableShield(boolean)}, whose flag asked whether the attacker swung an axe; 1.20.5
     * dropped the question — a disabling hit always disables — and 1.21.2 asks instead *which stack*
     * goes on cooldown, cooldowns having moved from the item to the stack. That stack is the one
     * being blocked with, i.e. the item in use, which is what vanilla's own callers pass.
     *
     * <p>1.21.5 deleted the method outright — blocking is a {@code BlocksAttacks} data component
     * now, and disabling it is that component's own job. This is the body of vanilla's
     * {@code Player#blockUsingItem} with the attacker's {@code getSecondsToDisableBlocking} replaced
     * by the five seconds every earlier version hardcoded.
     */
    public static void disableShield(net.minecraft.world.entity.player.Player player) {
        //? if >=1.21.5 {
        /*net.minecraft.world.item.ItemStack acBlocking = player.getItemBlockingWith();
        net.minecraft.world.item.component.BlocksAttacks acBlocksAttacks =
                acBlocking == null ? null : acBlocking.get(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS);
        if (acBlocksAttacks != null && player.level() instanceof net.minecraft.server.level.ServerLevel acLevel) {
            acBlocksAttacks.disable(acLevel, player, 5.0F, acBlocking);
        }
        *///?} elif >=1.21.2 {
        /*player.disableShield(player.getUseItem());
        *///?} elif >=1.20.5 {
        /*player.disableShield();
        *///?} else {
        player.disableShield(true);
        //?}
    }

    /**
     * Whether {@code living} is holding a raised shield against {@code source}.
     *
     * <p>1.21.5 removed {@code LivingEntity#isDamageSourceBlocked} along with the rest of the
     * hardcoded shield logic; what is left is the undirected {@code isBlocking()}, the directional
     * test having moved inside the {@code BlocksAttacks} component's own damage handling. The one
     * caller here only asks "should this hit be treated as blocked", so the difference is that a
     * shield raised away from the sauropod now also counts.
     */
    public static boolean isDamageSourceBlocked(net.minecraft.world.entity.LivingEntity living, net.minecraft.world.damagesource.DamageSource source) {
        //? if >=1.21.5 {
        /*return living.isBlocking();
        *///?} else {
        return living.isDamageSourceBlocked(source);
        //?}
    }

    /**
     * The chance that {@code mob} drops what it is wearing in {@code slot}.
     *
     * <p>1.21.5 folded the two {@code float[]} chance arrays into one immutable {@code DropChances}
     * record, so the per-slot getter is gone; {@code setDropChance} survived unchanged, which is why
     * only the read needs a facade.
     */
    public static float equipmentDropChance(net.minecraft.world.entity.Mob mob, net.minecraft.world.entity.EquipmentSlot slot) {
        //? if >=1.21.5 {
        /*return mob.getDropChances().byEquipment(slot);
        *///?} else {
        return ((com.github.alexmodguy.alexscaves.server.entity.util.EntityDropChanceAccessor) mob).ac_getEquipmentDropChance(slot);
        //?}
    }

    /**
     * Puts {@code stack} on the player's use cooldown for {@code ticks}.
     *
     * <p>1.21.2 rekeyed {@code ItemCooldowns} from the {@code Item} to a cooldown *group*, a
     * {@code ResourceLocation} that defaults to the item's own id but can be shared by a
     * {@code use_cooldown} component — so the overload that reads it takes the stack. Every call
     * site in this mod already had the stack to hand; below 1.21.2 its item is the same key the
     * old call used.
     */
    public static void addCooldown(net.minecraft.world.entity.player.Player player, ItemStack stack, int ticks) {
        //? if >=1.21.2 {
        /*player.getCooldowns().addCooldown(stack, ticks);
        *///?} else {
        player.getCooldowns().addCooldown(stack.getItem(), ticks);
        //?}
    }

    /**
     * Applies an instantaneous effect (harming, healing) directly, without an effect instance.
     *
     * <p>1.21.2 prepended the {@code ServerLevel} — the effect may need to spawn particles or fire
     * criteria. The one caller is the jelly bean, which is already inside a {@code !isClientSide}
     * branch, so the cast can never fail; the guard is there so the client arm compiles rather than
     * to defend against anything.
     */
    public static void applyInstantenousEffect(net.minecraft.world.effect.MobEffect effect, net.minecraft.world.level.Level level,
                                               @Nullable net.minecraft.world.entity.Entity source, @Nullable net.minecraft.world.entity.Entity indirectSource,
                                               net.minecraft.world.entity.LivingEntity target, int amplifier, double health) {
        //? if >=1.21.2 {
        /*if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            effect.applyInstantenousEffect(serverLevel, source, indirectSource, target, amplifier, health);
        }
        *///?} else {
        effect.applyInstantenousEffect(source, indirectSource, target, amplifier, health);
        //?}
    }

    @Nullable
    public static net.minecraft.world.entity.item.ItemEntity spawnAtLocation(net.minecraft.world.entity.Entity entity, ItemStack stack) {
        //? if >=1.21.2 {
        /*return entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel ? entity.spawnAtLocation(serverLevel, stack) : null;
        *///?} else {
        return entity.spawnAtLocation(stack);
        //?}
    }

    @Nullable
    public static net.minecraft.world.entity.item.ItemEntity spawnAtLocation(net.minecraft.world.entity.Entity entity, ItemStack stack, float yOffset) {
        //? if >=1.21.2 {
        /*return entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel ? entity.spawnAtLocation(serverLevel, stack, yOffset) : null;
        *///?} else {
        return entity.spawnAtLocation(stack, yOffset);
        //?}
    }

    @Nullable
    public static net.minecraft.world.entity.item.ItemEntity spawnAtLocation(net.minecraft.world.entity.Entity entity, net.minecraft.world.level.ItemLike item) {
        //? if >=1.21.2 {
        /*return entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel ? entity.spawnAtLocation(serverLevel, item) : null;
        *///?} else {
        return entity.spawnAtLocation(item);
        //?}
    }

    @Nullable
    public static net.minecraft.world.entity.item.ItemEntity spawnAtLocation(net.minecraft.world.entity.Entity entity, net.minecraft.world.level.ItemLike item, int yOffset) {
        //? if >=1.21.2 {
        /*return entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel ? entity.spawnAtLocation(serverLevel, item, yOffset) : null;
        *///?} else {
        return entity.spawnAtLocation(item, yOffset);
        //?}
    }

    /**
     * {@code type.create(level)} — building an entity without placing it in the world.
     *
     * <p>1.21.2 added a spawn reason to every {@code create} overload. On this one it is <em>not
     * used</em>: {@code create(Level, EntitySpawnReason)} only checks the feature flags and calls
     * the type's factory, and nothing downstream sees the value — the reason is only consulted by
     * the {@code create(ServerLevel, …, BlockPos, …)} overload, which passes it to
     * {@code Mob#finalizeSpawn}. So one constant covers all 62 call sites in this mod regardless of
     * what each is really doing, and {@code MOB_SUMMONED} is the honest description of the majority
     * of them (a block, an item or another mob spawning something on purpose).
     *
     * <p>If a call site ever needs to <em>place</em> an entity with a meaningful reason, it wants
     * {@code EntityType#spawn}, not this.
     */
    @Nullable
    public static <T extends net.minecraft.world.entity.Entity> T createEntity(
            net.minecraft.world.entity.EntityType<T> type, net.minecraft.world.level.Level level) {
        //? if >=1.21.2 {
        /*return type.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        *///?} else {
        return type.create(level);
        //?}
    }

    // 26.2 stopped handing an entity its id at construction. Entity#getId throws
    // "Tried to access entity ID before ID assignment" while the field is still 0, and the field is
    // only filled in when the entity is added to a level. A display entity is never added to one --
    // it exists purely to be drawn -- while 26.x's living render-state extraction reads the id
    // unconditionally (ItemModelResolver#updateForLiving uses it as the seed that picks an item
    // model variant), so drawing one threw out of the block entity renderer. Stamping a negative id
    // keeps every display entity distinct from every other and from every real, level-assigned id,
    // which are always positive.
    private static final java.util.concurrent.atomic.AtomicInteger DISPLAY_ENTITY_IDS =
            new java.util.concurrent.atomic.AtomicInteger(-1);

    public static <T extends net.minecraft.world.entity.Entity> T markDisplayEntity(T entity) {
        //? if >=26.2 {
        /*if (entity != null) {
            entity.setId(DISPLAY_ENTITY_IDS.getAndDecrement());
        }
        *///?}
        return entity;
    }

    // ── Block interaction ──────────────────────────────────────────────────────
    // 1.20.5 split BlockBehaviour#use into useItemOn — item in hand, new ItemInteractionResult
    // return — and useWithoutItem. Every block here branches on the held item, so each keeps one
    // pre-1.20.5-shaped body and routes it through whichever entry point the version has; this maps
    // the result on the way out. PASS means "we did nothing", which is now spelled as a fall-through
    // to the itemless interaction rather than a super.use call.

    // 1.21.2 undid the split: ItemInteractionResult is gone again and useItemOn is back to
    // returning an InteractionResult — but the enum became a sealed interface, and the
    // "we did nothing, try the itemless interaction" answer is now its own constant rather than
    // PASS (which from that version simply fails).
    //? if >=1.21.2 {
    /*public static net.minecraft.world.InteractionResult itemResult(net.minecraft.world.InteractionResult result) {
        return result == net.minecraft.world.InteractionResult.PASS ? net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND : result;
    }
    *///?} elif >=1.20.5 {
    /*public static net.minecraft.world.ItemInteractionResult itemResult(net.minecraft.world.InteractionResult result) {
        return switch (result) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> net.minecraft.world.ItemInteractionResult.SUCCESS;
            case CONSUME -> net.minecraft.world.ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> net.minecraft.world.ItemInteractionResult.CONSUME_PARTIAL;
            case FAIL -> net.minecraft.world.ItemInteractionResult.FAIL;
            case PASS -> net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }
    *///?}

    // ── Item use results ───────────────────────────────────────────────────────
    // Item#use answered with an InteractionResultHolder — a result paired with the stack to put
    // back in the hand — until 1.21.2 merged the two into the InteractionResult interface, where a
    // replaced stack is carried by Success#heldItemTransformedTo instead. Every use() in this mod
    // hands back the same stack it was given, so the pairing carried no information and these
    // helpers simply drop it from 1.21.2 on. Only the *declared return type* of the ~28 overrides
    // still has to be gated per file.
    //
    // sidedSuccess is gone with it: 1.21.2's SUCCESS already means "swing on whichever side ran
    // this", which is exactly what the old client/server pair spelled out by hand.

    //? if >=1.21.2 {
    /*public static net.minecraft.world.InteractionResult useSuccess(net.minecraft.world.item.ItemStack stack) {
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    public static net.minecraft.world.InteractionResult useSidedSuccess(net.minecraft.world.item.ItemStack stack, boolean clientSide) {
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    public static net.minecraft.world.InteractionResult useConsume(net.minecraft.world.item.ItemStack stack) {
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    public static net.minecraft.world.InteractionResult useFail(net.minecraft.world.item.ItemStack stack) {
        return net.minecraft.world.InteractionResult.FAIL;
    }

    public static net.minecraft.world.InteractionResult usePass(net.minecraft.world.item.ItemStack stack) {
        return net.minecraft.world.InteractionResult.PASS;
    }

    public static net.minecraft.world.InteractionResult sidedSuccess(boolean clientSide) {
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
    *///?} else {
    public static net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> useSuccess(net.minecraft.world.item.ItemStack stack) {
        return net.minecraft.world.InteractionResultHolder.success(stack);
    }

    public static net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> useSidedSuccess(net.minecraft.world.item.ItemStack stack, boolean clientSide) {
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, clientSide);
    }

    public static net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> useConsume(net.minecraft.world.item.ItemStack stack) {
        return net.minecraft.world.InteractionResultHolder.consume(stack);
    }

    public static net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> useFail(net.minecraft.world.item.ItemStack stack) {
        return net.minecraft.world.InteractionResultHolder.fail(stack);
    }

    public static net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> usePass(net.minecraft.world.item.ItemStack stack) {
        return net.minecraft.world.InteractionResultHolder.pass(stack);
    }

    public static net.minecraft.world.InteractionResult sidedSuccess(boolean clientSide) {
        return net.minecraft.world.InteractionResult.sidedSuccess(clientSide);
    }
    //?}

    // ── Pathfinding ────────────────────────────────────────────────────────────
    // 1.20.5 rebuilt the pathfinder's plumbing around a PathfindingContext: the level and mob a
    // node evaluator works against travel together in one object now, rather than being threaded
    // through every call. Three surfaces this mod touches changed with it, and each keeps its
    // pre-1.20.5 argument list here so the call sites read the same on every node.
    //
    // (BlockBehaviour#isPathfindable — the *override* side — is not here: it lost the same pair of
    // arguments but there are twelve of them and they are handled by !mc205-pathfindable-sig.)

    /** Whether {@code state} may be pathed through. The caller-side overload on BlockStateBase. */
    public static boolean isPathfindable(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter getter, net.minecraft.core.BlockPos pos, net.minecraft.world.level.pathfinder.PathComputationType type) {
        //? if >=1.20.5 {
        /*return state.isPathfindable(type);
        *///?} else {
        return state.isPathfindable(getter, pos, type);
        //?}
    }

    /**
     * The path type of {@code pos}, as the four teleport/jump checks that ask about a block they
     * are not currently pathing to want it.
     *
     * <p>The mob is unused below 1.20.5 — the static lookup read nothing but the block there — but
     * every call site has one to hand, and from 1.20.5 on the context cannot be built without it.
     */
    public static net.minecraft.world.level.pathfinder.BlockPathTypes pathTypeStatic(net.minecraft.world.entity.Mob mob, net.minecraft.world.level.CollisionGetter level, net.minecraft.core.BlockPos.MutableBlockPos pos) {
        //? if >=1.20.5 {
        /*return net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getPathTypeStatic(
                new net.minecraft.world.level.pathfinder.PathfindingContext(level, mob), pos);
        *///?} else {
        return net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getBlockPathTypeStatic(level, pos);
        //?}
    }

    // ── Attribute modifiers ────────────────────────────────────────────────────
    // 1.21 replaced an AttributeModifier's (UUID, String name) identity with a single
    // ResourceLocation. The two spellings never coexist, so the constructions that are not a plain
    // vanilla constant funnel through here and the version drops whichever half it has no use for.
    //
    // The id keeps the same *uniqueness* the UUID had — one per (attribute, source) pair — which is
    // all either scheme is asked for; the pre-1.21 display name is unchanged so no existing save's
    // serialised modifier changes shape on the versions that still read it.

    public static net.minecraft.world.entity.ai.attributes.AttributeModifier attributeModifier(
            java.util.UUID legacyId, String legacyName, String id, double amount,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation) {
        //? if >=1.21 {
        /*return new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.github.alexmodguy.alexscaves.AlexsCaves.MODID, id),
                amount, operation);
        *///?} else {
        return new net.minecraft.world.entity.ai.attributes.AttributeModifier(legacyId, legacyName, amount, operation);
        //?}
    }

    // ── Food ───────────────────────────────────────────────────────────────────
    // 1.20.5 moved food onto the FOOD data component and deleted Forge's Item/ItemStack food
    // extensions with it, so everything reads off the stack now. The record's accessors also lost
    // their get- prefix, and saturation() is the resolved value where the old getSaturationModifier()
    // — and FoodData#eat(int, float), on every version — speaks in the modifier.

    /**
     * The stack's food, or {@code null} if it is not edible.
     *
     * <p>The Fabric arm is what makes {@link com.github.alexmodguy.alexscaves.server.item.ACFoodPropertiesItem}
     * mean anything below 1.20.5: vanilla's own lookup is {@code Item#getFoodProperties()}, which
     * cannot vary per stack, so an item that wants to (the biome treat feeds 20 while its biome is
     * undiscovered and 1 afterwards) answers through the interface instead. That is exactly what the
     * loaders' {@code ItemStack#getFoodProperties(LivingEntity)} does, and the three vanilla call
     * sites they patch to use it are redirected here as well — {@code FoodDataMixin} and
     * {@code mixin.fabric.LivingEntityFoodMixin}.
     */
    @Nullable
    public static net.minecraft.world.food.FoodProperties food(ItemStack stack, @Nullable net.minecraft.world.entity.LivingEntity eater) {
        //? if >=1.20.5 {
        /*return stack.get(net.minecraft.core.component.DataComponents.FOOD);
        *///?} elif fabric {
        /*return stack.getItem() instanceof com.github.alexmodguy.alexscaves.server.item.ACFoodPropertiesItem acFoodItem ? acFoodItem.getFoodProperties(stack, eater) : stack.getItem().getFoodProperties();
        *///?} else {
        return stack.getFoodProperties(eater);
        //?}
    }

    public static boolean isEdible(ItemStack stack) {
        //? if >=1.20.5 {
        /*return stack.has(net.minecraft.core.component.DataComponents.FOOD);
        *///?} else {
        return stack.getItem().isEdible();
        //?}
    }

    public static int nutrition(net.minecraft.world.food.FoodProperties food) {
        //? if >=1.20.5 {
        /*return food.nutrition();
        *///?} else {
        return food.getNutrition();
        //?}
    }

    /** The saturation <em>modifier</em>, which is what {@code FoodData#eat(int, float)} still wants. */
    public static float saturationModifier(net.minecraft.world.food.FoodProperties food) {
        //? if >=1.20.5 {
        /*return food.nutrition() > 0 ? food.saturation() / (2.0F * food.nutrition()) : 0.0F;
        *///?} else {
        return food.getSaturationModifier();
        //?}
    }

    public static boolean canAlwaysEat(net.minecraft.world.food.FoodProperties food) {
        //? if >=1.20.5 {
        /*return food.canAlwaysEat();
        *///?} else {
        return food.canAlwaysEat();
        //?}
    }

    /**
     * The effects eating this stack may apply, with the per-effect probability dropped — no caller
     * used it.
     *
     * <p>Takes the stack rather than the {@code FoodProperties} because from 1.21.2 the food no
     * longer carries the effects at all: they moved onto the {@code CONSUMABLE} component as
     * {@code ConsumeEffect}s, of which the status-effect kind is one of several.
     */
    public static java.util.List<net.minecraft.world.effect.MobEffectInstance> foodEffects(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity entity) {
        //? if >=1.21.2 {
        /*net.minecraft.world.item.component.Consumable consumable = stack.get(net.minecraft.core.component.DataComponents.CONSUMABLE);
        if (consumable == null) {
            return java.util.List.of();
        }
        java.util.List<net.minecraft.world.effect.MobEffectInstance> effects = new java.util.ArrayList<>();
        for (net.minecraft.world.item.consume_effects.ConsumeEffect effect : consumable.onConsumeEffects()) {
            if (effect instanceof net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect apply) {
                effects.addAll(apply.effects());
            }
        }
        return effects;
        *///?} else if >=1.20.5 {
        /*net.minecraft.world.food.FoodProperties food = food(stack, entity);
        return food == null ? java.util.List.of() : food.effects().stream().map(net.minecraft.world.food.FoodProperties.PossibleEffect::effect).toList();
        *///?} else {
        net.minecraft.world.food.FoodProperties food = food(stack, entity);
        return food == null ? java.util.List.of() : food.getEffects().stream().map(com.mojang.datafixers.util.Pair::getFirst).toList();
        //?}
    }

    /**
     * Whether a stack counts as meat.
     *
     * <p>1.20.5 dropped {@code FoodProperties#isMeat} in favour of the {@code minecraft:meat} item
     * tag, so this mod's own meats have to be listed in that tag from 1.20.5 on — they are, in
     * {@code data/minecraft/tags/items/meat.json} (the singular-folder rename to {@code tags/item}
     * on 1.21+ is done by DataPackMigration). The five entries are exactly the five ACFoods that
     * spell {@code .meat()} on 1.20.1.
     *
     * <p>That one file also restores wolf feeding, which is the other thing the flag used to drive:
     * vanilla's {@code minecraft:wolf_food} is defined as {@code ["#minecraft:meat"]}, so there is
     * nothing extra to ship for it. The tag is written on every node — below 1.20.5 it simply
     * creates an unused {@code minecraft:meat}, which is cheaper than a version-gated resource.
     */
    public static boolean isMeat(ItemStack stack) {
        //? if >=1.20.5 {
        /*return stack.is(net.minecraft.tags.ItemTags.MEAT);
        *///?} else {
        net.minecraft.world.food.FoodProperties food = food(stack, null);
        return food != null && food.isMeat();
        //?}
    }

    /**
     * Whether eating this stack hands a bowl back.
     *
     * <p>Up to 1.20.6 that was the whole job of {@code BowlFoodItem}. 1.21 deleted the class and moved
     * the leftover onto the food record as {@code usingConvertsTo}; 1.21.2 moved it once more, off the
     * food and onto the item's own {@code USE_REMAINDER} component. Either way the question stopped
     * being about the item's type.
     */
    public static boolean returnsBowl(ItemStack stack) {
        //? if >=1.21.2 {
        /*net.minecraft.world.item.component.UseRemainder remainder = stack.get(net.minecraft.core.component.DataComponents.USE_REMAINDER);
        return remainder != null && remainder.convertInto().is(net.minecraft.world.item.Items.BOWL);
        *///?} else if >=1.21 {
        /*net.minecraft.world.food.FoodProperties food = food(stack, null);
        return food != null && food.usingConvertsTo().filter(left -> left.is(net.minecraft.world.item.Items.BOWL)).isPresent();
        *///?} else {
        return stack.getItem() instanceof net.minecraft.world.item.BowlFoodItem;
        //?}
    }

    /** Builds a bowl food item — see {@link #returnsBowl}: from 1.21 it is a plain {@code Item}. */
    public static net.minecraft.world.item.Item bowlFood(net.minecraft.world.item.Item.Properties properties) {
        //? if >=1.21 {
        /*return new net.minecraft.world.item.Item(properties);
        *///?} else {
        return new net.minecraft.world.item.BowlFoodItem(properties);
        //?}
    }

    // ── Single-stack NBT ───────────────────────────────────────────────────────
    // 1.20.5 needs a registry lookup to write or read one ItemStack, because a component can hold
    // registry references. HolderLookup.Provider and RegistryAccess (which extends it) both exist
    // on every version this mod spans, so the helpers take one and simply ignore it below 1.20.5 —
    // no gate on the signatures, only on the bodies.
    //
    // Entities pass level().registryAccess(). Block entities cannot: their level is still null
    // while they load, so they pass BE_REGISTRIES, which the !mc205-be-stackregistries replacement
    // rewrites to the `acRegistries` parameter the other block-entity rules already added.

    /**
     * Placeholder for "the provider a block entity's load/save was handed".
     *
     * <p>Only ever evaluated below 1.20.5, where the helpers ignore it; from 1.20.5 on the
     * replacement rule has already swapped every occurrence for the real parameter.
     */
    @Nullable
    public static final net.minecraft.core.HolderLookup.Provider BE_REGISTRIES = null;

    public static ItemStack loadStack(net.minecraft.core.HolderLookup.Provider registries, CompoundTag tag) {
        // parse(...) is the one that survived 1.21.5, which deleted parseOptional — and the empty-tag
        // case it special-cased is exactly what an empty Optional means here anyway. 1.21.6 then took
        // parse/save away too, in the same sweep that moved every save path onto ValueInput/
        // ValueOutput: what those two did is now expected to be spelled through ItemStack.CODEC, over
        // ops the provider builds. Same codec, same result — it is only the convenience that went.
        //? if >=1.21.6 {
        /*return ItemStack.CODEC.parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag)
                .result().orElse(ItemStack.EMPTY);
        *///?} elif >=1.20.5 {
        /*return ItemStack.parse(registries, tag).orElse(ItemStack.EMPTY);
        *///?} else {
        return ItemStack.of(tag);
        //?}
    }

    public static CompoundTag saveStack(net.minecraft.core.HolderLookup.Provider registries, ItemStack stack) {
        // See loadStack. getOrThrow keeps the old contract: ItemStack#save threw on an empty stack
        // too, and every caller here guards emptiness before asking.
        //? if >=1.21.6 {
        /*return (CompoundTag) ItemStack.CODEC
                .encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack).getOrThrow();
        *///?} elif >=1.20.5 {
        /*return (CompoundTag) stack.save(registries);
        *///?} else {
        return stack.save(new CompoundTag());
        //?}
    }

    // ── ValueInput / ValueOutput ───────────────────────────────────────────────
    // 1.21.6 replaced the CompoundTag on every save/load signature with the ValueInput/ValueOutput
    // pair — an abstraction over "a keyed tree of values" that also carries a ProblemReporter, so a
    // malformed field is reported rather than silently defaulted.
    //
    // This mod has ~150 of those overrides and every one of their BODIES already reads through the
    // ACCompat.getX helpers above, so porting them one at a time would be ~150 identical rewrites
    // for no behavioural gain. The replacement rules in stonecutter.gradle.kts instead rewrite only
    // the method HEADER, binding the original CompoundTag parameter name to the bridge below and
    // leaving every body untouched on all 58 nodes.
    //
    // What makes that legal is that both directions are ZERO-COPY:
    //   * TagValueOutput#buildResult returns the live backing CompoundTag, so a write made through
    //     the bridge is the write vanilla goes on to persist — there is nothing to flush back;
    //   * TagValueInput's backing tag is private, so mixin.TagValueInputAccessor hands it over.

    //? if >=1.21.6 {
    /*// The live CompoundTag a ValueOutput is filling. Writes to it are the writes that persist.
    //
    // TagValueOutput is the only implementation: its child() hands back another one and its list
    // wrapper builds them too, so a save path cannot produce anything else. A ClassCastException
    // here would mean a fourth-party implementation appeared, and failing loudly is the right
    // answer — the alternative, writing into a detached tag, would silently stop persisting every
    // field of the affected object.
    public static CompoundTag tagOf(net.minecraft.world.level.storage.ValueOutput output) {
        return ((net.minecraft.world.level.storage.TagValueOutput) output).buildResult();
    }

    // The CompoundTag a ValueInput is reading from.
    //
    // ⚠️ Unlike the output side there IS a second implementation — the anonymous constant
    // ValueInputContextHelper#empty, which TagValueInput hands out in place of an empty compound.
    // Nothing should ever pass it here (an entity or block entity with no tag at all is never
    // constructed in the first place), but a cast that crashed world loading if one ever did would
    // be a poor trade: a fresh empty tag reads back exactly the defaults that input would have
    // returned, so the fallback is the same behaviour rather than a papering-over.
    public static CompoundTag tagOf(net.minecraft.world.level.storage.ValueInput input) {
        return input instanceof com.github.alexmodguy.alexscaves.mixin.TagValueInputAccessor accessor
                ? accessor.ac_getInput()
                : new CompoundTag();
    }

    // ⚠️ There is deliberately no registriesOf(ValueOutput) here, though it looks like the obvious
    // counterpart to ValueInput#lookup(). A ValueOutput publishes no lookup, NeoForge's extension of
    // it adds none, and the only route through the implementation is a dead end: TagValueOutput does
    // hold the provider inside the RegistryOps it encodes with, but BOTH RegistryOps#lookupProvider
    // and RegistryOps.HolderLookupAdapter are inaccessible from outside the package (their mapped
    // sources read `public`, which is NeoForge's access transformer talking — the compile classpath
    // is not). The three block entities that need a provider while they save are in a level and are
    // asked for it directly instead; see registriesOf(BlockEntity) above.

    // Reads an entity's save data from a tag the mod is holding itself.
    //
    // Four call sites keep a stashed entity tag — the possession totem, the holocoder, the hologram
    // projector and the cave book's entity widget — and hand it straight to readAdditionalSaveData.
    // Below 1.21.6 that method takes the tag as it stands, so this is the identity; from 1.21.6 it
    // needs the tag wrapped. Problems are discarded rather than logged: the callers are reading a
    // tag they wrote themselves, not user data.
    public static net.minecraft.world.level.storage.ValueInput asInput(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        return net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, registries, tag);
    }

    // The mirror of asInput: a ValueOutput writing into a tag the caller already holds.
    //
    // Both of vanilla's factories mint their own empty tag, which is no use to the six bucketable
    // fish or the possession totem — each builds a tag, asks the entity to fill it and then stores
    // it somewhere of its own. mixin.TagValueOutputAccessor invokes the package-private constructor
    // that takes the tag. Zero-copy in the same sense as tagOf: writes land in `tag` directly.
    public static net.minecraft.world.level.storage.ValueOutput asOutput(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        return com.github.alexmodguy.alexscaves.mixin.TagValueOutputAccessor.ac_new(
                net.minecraft.util.ProblemReporter.DISCARDING, registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag);
    }

    // The entity type a stashed "BoundEntityTag" names — the holocoder, the hologram projector and
    // the possession totem all read one. RegistryAccess.EMPTY is the right lookup on 1.21.6: the
    // read is EntityType.CODEC on the "id" key, which resolves out of BuiltInRegistries and never
    // touches the ops' registries.
    public static java.util.Optional<net.minecraft.world.entity.EntityType<?>> entityTypeFrom(CompoundTag tag) {
        return net.minecraft.world.entity.EntityType.by(asInput(tag, net.minecraft.core.RegistryAccess.EMPTY));
    }

    // The block-entity update tag, in the shape onDataPacket bodies expect.
    //
    // 1.21.6 changed that method's second parameter from the packet to a ValueInput, and the nine
    // bodies here all spell it packet.getTag(). Handing them this keeps them identical on every
    // node; getTag() is never null from 1.21.6, since a ValueInput is only ever built around a real
    // tag, and the null checks those bodies make simply stop firing.
    public static PacketData packetData(net.minecraft.world.level.storage.ValueInput input) {
        return new PacketData(tagOf(input));
    }

    // See packetData.
    public record PacketData(CompoundTag tag) {
        public CompoundTag getTag() {
            return this.tag;
        }
    }
    *///?} else {
    /** Identity below 1.21.6, where {@code readAdditionalSaveData} still takes the tag itself. */
    public static CompoundTag asInput(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        return tag;
    }

    /** Identity below 1.21.6, where {@code addAdditionalSaveData} still takes the tag itself. */
    public static CompoundTag asOutput(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        return tag;
    }

    /** See the 1.21.6 arm: {@code EntityType#by} took the tag directly until then. */
    public static java.util.Optional<net.minecraft.world.entity.EntityType<?>> entityTypeFrom(CompoundTag tag) {
        return net.minecraft.world.entity.EntityType.by(tag);
    }
    //?}

    // ── Area effect clouds ─────────────────────────────────────────────────────

    /**
     * Tints a cloud, overriding the colour its effects would otherwise mix.
     *
     * <p>1.20.5 folded the cloud's colour, potion and effect list into one {@code PotionContents}
     * component and deleted {@code setFixedColor}. There is no getter for the current contents, so
     * this writes a fresh one — which is only lossless if it is called <em>before</em> any
     * {@code addEffect}. All four call sites do exactly that, right after construction, and
     * {@code addEffect} then merges into what this leaves behind.
     */
    public static void setCloudColor(net.minecraft.world.entity.AreaEffectCloud cloud, int color) {
        //? if >=1.21.2 {
        /*// 1.21.2 added a fourth field, the custom potion name; an empty leaves the name alone.
        cloud.setPotionContents(new net.minecraft.world.item.alchemy.PotionContents(
                java.util.Optional.empty(), java.util.Optional.of(color), java.util.List.of(), java.util.Optional.empty()));
        *///?} elif >=1.20.5 {
        /*cloud.setPotionContents(new net.minecraft.world.item.alchemy.PotionContents(
                java.util.Optional.empty(), java.util.Optional.of(color), java.util.List.of()));
        *///?} else {
        cloud.setFixedColor(color);
        //?}
    }

    // ── Loot tables ────────────────────────────────────────────────────────────
    // 1.20.5 turned loot tables into a datapack registry. Three things changed together:
    // MinecraftServer#getLootData() became #reloadableRegistries(), the lookup is keyed by
    // ResourceKey<LootTable> rather than ResourceLocation, and every vanilla accessor that used
    // to hand out a table's id — Entity#getLootTable, StructurePiece#createChest — changed type
    // with it. This mod names its own tables by ResourceLocation throughout, so the id stays the
    // currency here and the key only exists inside these three helpers.

    /** The table registered under {@code id}. */
    public static net.minecraft.world.level.storage.loot.LootTable lootTable(net.minecraft.server.MinecraftServer server, net.minecraft.resources.ResourceLocation id) {
        //? if >=1.20.5 {
        /*return server.reloadableRegistries().getLootTable(lootKey(id));
        *///?} else {
        return server.getLootData().getLootTable(id);
        //?}
    }

    /** Wraps a table id in whatever the version's lookups and vanilla accessors want. */
    //? if >=1.21.2 {
    /*public static net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> lootKey(net.minecraft.resources.ResourceLocation id) {
        return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, id);
    }

    // Unwraps what a vanilla accessor handed back, so callers can keep declaring ResourceLocation.
    // 1.21.2 made Entity#getLootTable an Optional — "this entity drops nothing" stopped being a
    // null and became an empty — so this arm answers null for that case and the two callers say
    // what they do about it, which is the same nothing the vanilla drop path would have done.
    @Nullable
    public static net.minecraft.resources.ResourceLocation lootId(java.util.Optional<net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable>> key) {
        return key.map(net.minecraft.resources.ResourceKey::location).orElse(null);
    }
    *///?} elif >=1.20.5 {
    /*public static net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> lootKey(net.minecraft.resources.ResourceLocation id) {
        return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, id);
    }

    // Unwraps what a vanilla accessor handed back, so callers can keep declaring ResourceLocation.
    public static net.minecraft.resources.ResourceLocation lootId(net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> key) {
        return key.location();
    }
    *///?} else {
    public static net.minecraft.resources.ResourceLocation lootKey(net.minecraft.resources.ResourceLocation id) {
        return id;
    }

    /** Unwraps what a vanilla accessor handed back, so callers can keep declaring ResourceLocation. */
    public static net.minecraft.resources.ResourceLocation lootId(net.minecraft.resources.ResourceLocation id) {
        return id;
    }
    //?}

    // ── NBT ────────────────────────────────────────────────────────────────────

    /**
     * The key of the sub-compound of a player's persistent data that survives death.
     *
     * <p>Between Forge and NeoForge this is purely a move: 1.20.5 relocated the constant from
     * {@code Player} to {@code ServerPlayer}. Both the string and the copy-on-respawn behaviour are
     * unchanged, and the two call sites ({@code WatcherEntity}'s last-possessed timestamp and
     * {@code SpelunkeryTableMenu}'s tutorial flag) both want that behaviour.
     *
     * <p>Fabric has no such constant, so the Fabric arm binds Forge's own literal. Spelling it out
     * rather than inventing a mod-specific key means a world carried between loaders keeps reading
     * the same sub-tag, and the copy-on-respawn half is supplied by
     * {@code mixin.fabric.FabricServerPlayerMixin}.
     */
    //? if fabric {
    /*public static final String PERSISTED_NBT_TAG = "PlayerPersisted";
    *///?} elif >=1.20.5 {
    /*public static final String PERSISTED_NBT_TAG = net.minecraft.server.level.ServerPlayer.PERSISTED_NBT_TAG;
    *///?} else {
    public static final String PERSISTED_NBT_TAG = net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG;
    //?}

    /**
     * A per-entity NBT bag that the mod owns and the game persists — Forge's
     * {@code Entity#getPersistentData}, and on Fabric the vendored Citadel entity tag standing in
     * for it.
     *
     * <p>The two are not quite the same object and the differences are worth stating, because every
     * caller mutates the returned tag <em>in place</em> and expects that to stick:
     *
     * <ul>
     * <li><b>It persists either way.</b> Citadel's tag is written out of
     *     {@code citadel.LivingEntityMixin} under {@code "CitadelData"} on every save, and it is the
     *     stored object that is written, so an in-place edit reaches disk exactly as Forge's does.
     * <li><b>Only a {@code LivingEntity} has one.</b> Citadel installs its accessor on
     *     {@code LivingEntity}, so anything else gets a throwaway tag here. Every read this mod
     *     performs then answers "absent", which is the same answer an entity that was never written
     *     to would give — and both writers ({@code TotemOfPossessionItem}, and the two player-only
     *     sites) only ever touch mobs and players.
     * <li><b>It is synched, but nothing here relies on that.</b> Citadel's store is an
     *     {@code EntityDataAccessor}, and an in-place edit never calls {@code entityData.set}, so it
     *     does not push an update packet — the Forge behaviour, not a regression from it. Both
     *     readers of the possession flag run in server-side AI ({@code isAlliedTo} /
     *     {@code hurt}), and the tutorial flag is already carried to the client by
     *     {@code SpelunkeryTableCompleteTutorialMessage} rather than by any sync of this bag.
     * </ul>
     */
    public static net.minecraft.nbt.CompoundTag getPersistentData(net.minecraft.world.entity.Entity entity) {
        //? if fabric {
        /*if (entity instanceof com.github.alexmodguy.alexscaves.citadel.server.entity.ICitadelDataEntity dataEntity) {
            net.minecraft.nbt.CompoundTag data = dataEntity.getCitadelEntityData();
            if (data == null) {
                data = new net.minecraft.nbt.CompoundTag();
                dataEntity.setCitadelEntityData(data);
            }
            return data;
        }
        return new net.minecraft.nbt.CompoundTag();
        *///?} else {
        return entity.getPersistentData();
        //?}
    }

    /**
     * A BlockPos ↔ NBT round-trip that keeps the pre-1.20.5 {@code {X,Y,Z}} compound shape on every
     * version.
     *
     * <p>1.20.5 changed both halves of {@code NbtUtils}' pair at once: {@code writeBlockPos} now
     * returns an {@code IntArrayTag} instead of a compound, and the compound-only
     * {@code readBlockPos(CompoundTag)} is gone in favour of {@code readBlockPos(parent, key)}
     * returning an {@code Optional}. Deferring to whichever exists would silently change this mod's
     * own saved format — and the NBT list <em>type</em> it is stored under in
     * {@code MagnetronEntity} — halfway through the version walk, so the format is pinned here
     * instead. Nothing vanilla reads these tags.
     */
    public static void putBlockPos(CompoundTag parent, String key, net.minecraft.core.BlockPos pos) {
        CompoundTag child = new CompoundTag();
        child.putInt("X", pos.getX());
        child.putInt("Y", pos.getY());
        child.putInt("Z", pos.getZ());
        parent.put(key, child);
    }

    /** Inverse of {@link #putBlockPos}; {@code null} when the key holds no compound. */
    @Nullable
    public static net.minecraft.core.BlockPos getBlockPos(CompoundTag parent, String key) {
        return contains(parent, key, 10) ? posFromTag(getCompound(parent, key)) : null;
    }

    /** The {@code {X,Y,Z}} compound for a pos, for the list-of-positions case that has no key. */
    public static CompoundTag posToTag(net.minecraft.core.BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        putBlockPos(tag, "P", pos);
        return getCompound(tag, "P");
    }

    /** Inverse of {@link #posToTag}; missing components default to 0, as the old vanilla reader did. */
    public static net.minecraft.core.BlockPos posFromTag(CompoundTag tag) {
        return new net.minecraft.core.BlockPos(getInt(tag, "X"), getInt(tag, "Y"), getInt(tag, "Z"));
    }

    /**
     * Whether this exact stack is the one an entity is wearing.
     *
     * <p>Forge dropped its {@code onArmorTick} hook in 1.21; the per-tick call that reaches worn
     * armour there is vanilla's own {@code Item#inventoryTick}, which runs over every inventory
     * slot rather than only the four armour ones. This is the guard that puts the old contract
     * back — the armour items only ever wanted the tick while the piece was actually on.
     *
     * <p>Identity rather than {@code equals}: two identical helmets, one worn and one in the
     * hotbar, must not both count.
     */
    public static boolean isWornArmor(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        return entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD) == stack
                || entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST) == stack
                || entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS) == stack
                || entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET) == stack;
    }

    // ── Recipes ─────────────────────────────────────────────────────────────────────────────
    //
    // 1.21.2 rewrote the recipe system around display objects: an Ingredient became a HolderSet of
    // items with no EMPTY and no getItems(); a shaped recipe's ingredient list became
    // List<Optional<Ingredient>>; Recipe lost getResultItem in favour of display(); and the
    // RecipeManager left the client entirely (Level#getRecipeManager is gone, ServerLevel answers
    // it as recipeAccess()). Only this mod's furnace, its cave-map recipe and the guide book need
    // any of it, so the differences are bridged here rather than gated at ~20 call sites.

    /**
     * One entry per display slot of a recipe, each holding every stack that slot accepts — what the
     * guide book cycles through. Slot order is the crafting grid's, row by row; a cooking recipe
     * has the single entry.
     *
     * <p>Below 1.21.2 that is just the recipe's ingredient list. From 1.21.2 the ingredient list
     * either does not exist on the recipe class (shapeless keeps it package-private) or is a
     * {@code List<Optional<Ingredient>>}, and the portable answer is the {@code RecipeDisplay} the
     * recipe builds for exactly this purpose.
     */
    public static java.util.List<net.minecraft.world.item.ItemStack[]> recipeDisplaySlots(
            net.minecraft.world.item.crafting.Recipe<?> recipe, net.minecraft.world.level.Level level) {
        java.util.List<net.minecraft.world.item.ItemStack[]> out = new java.util.ArrayList<>();
        //? if >=1.21.2 {
        /*java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> displays = recipe.display();
        if (displays.isEmpty()) {
            return out;
        }
        net.minecraft.util.context.ContextMap context = net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(level);
        java.util.List<net.minecraft.world.item.crafting.display.SlotDisplay> slots;
        if (displays.get(0) instanceof net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay shaped) {
            slots = shaped.ingredients();
        } else if (displays.get(0) instanceof net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay shapeless) {
            slots = shapeless.ingredients();
        } else if (displays.get(0) instanceof net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay furnace) {
            slots = java.util.List.of(furnace.ingredient());
        } else {
            slots = java.util.List.of();
        }
        for (net.minecraft.world.item.crafting.display.SlotDisplay slot : slots) {
            out.add(slot.resolveForStacks(context).toArray(new net.minecraft.world.item.ItemStack[0]));
        }
        *///?} else {
        for (net.minecraft.world.item.crafting.Ingredient ingredient : recipe.getIngredients()) {
            out.add(ingredientItems(ingredient));
        }
        //?}
        return out;
    }

    /** Every stack an ingredient accepts, for cycling through in a display slot. */
    public static net.minecraft.world.item.ItemStack[] ingredientItems(net.minecraft.world.item.crafting.Ingredient ingredient) {
        if (ingredient == null) {
            return new net.minecraft.world.item.ItemStack[0];
        }
        //? if >=1.21.4 {
        /*return ingredient.items().map(net.minecraft.world.item.ItemStack::new).toArray(net.minecraft.world.item.ItemStack[]::new);
        *///?} elif >=1.21.2 {
        /*return ingredient.items().stream().map(net.minecraft.world.item.ItemStack::new).toArray(net.minecraft.world.item.ItemStack[]::new);
        *///?} else {
        return ingredient.getItems();
        //?}
    }

    /**
     * What a recipe produces, for display only. From 1.21.2 that has to come off the recipe's
     * {@code RecipeDisplay}, since the result is no longer readable from the recipe itself.
     */
    public static net.minecraft.world.item.ItemStack recipeResult(net.minecraft.world.item.crafting.Recipe<?> recipe, net.minecraft.world.level.Level level) {
        //? if >=1.21.2 {
        /*java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> displays = recipe.display();
        if (displays.isEmpty()) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        return displays.get(0).result().resolveForFirstStack(net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(level));
        *///?} else {
        return recipe.getResultItem(level.registryAccess());
        //?}
    }

    /** A cooking recipe's experience award — renamed off the {@code get} prefix in 1.21.2. */
    public static float cookingExperience(net.minecraft.world.item.crafting.AbstractCookingRecipe recipe) {
        //? if >=1.21.2 {
        /*return recipe.experience();
        *///?} else {
        return recipe.getExperience();
        //?}
    }

    /** A cooking recipe's duration in ticks — renamed off the {@code get} prefix in 1.21.2. */
    public static int cookingTime(net.minecraft.world.item.crafting.AbstractCookingRecipe recipe) {
        //? if >=1.21.2 {
        /*return recipe.cookingTime();
        *///?} else {
        return recipe.getCookingTime();
        //?}
    }

    /** The server's recipe book. 1.21.2 renamed the accessor and narrowed it to {@code ServerLevel}. */
    public static net.minecraft.world.item.crafting.RecipeManager recipes(net.minecraft.server.level.ServerLevel level) {
        //? if >=1.21.2 {
        /*return level.recipeAccess();
        *///?} else {
        return level.getRecipeManager();
        //?}
    }

    /**
     * The single ingredient of a cooking recipe.
     *
     * <p>Up to 1.21.1 a cooking recipe answered a one-entry ingredient list; from 1.21.2 it is a
     * {@code SingleItemRecipe} and names its ingredient directly.
     */
    public static net.minecraft.world.item.crafting.Ingredient cookingIngredient(net.minecraft.world.item.crafting.AbstractCookingRecipe recipe) {
        //? if >=1.21.2 {
        /*return recipe.input();
        *///?} else {
        return recipe.getIngredients().get(0);
        //?}
    }

    /**
     * One recipe out of the server's book, by id.
     *
     * <p>1.20.2 wrapped the answer in a {@code RecipeHolder}, and 1.21.2 keyed the lookup by
     * {@code ResourceKey} rather than by plain id — this mod's nuclear furnace tallies the recipes
     * it has run by id, so it goes back the other way here.
     */
    //? if >=1.21.2 {
    /*public static java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder<?>> recipeById(net.minecraft.server.level.ServerLevel level, net.minecraft.resources.ResourceLocation id) {
        return level.recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id));
    }
    *///?} elif >=1.20.2 {
    /*public static java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder<?>> recipeById(net.minecraft.server.level.ServerLevel level, net.minecraft.resources.ResourceLocation id) {
        return level.getRecipeManager().byKey(id);
    }
    *///?} else {
    public static java.util.Optional<? extends net.minecraft.world.item.crafting.Recipe<?>> recipeById(net.minecraft.server.level.ServerLevel level, net.minecraft.resources.ResourceLocation id) {
        return level.getRecipeManager().byKey(id);
    }
    //?}

    /**
     * The per-position render offset of a block state ({@code MapColor}-style wobble for plants).
     *
     * <p>1.21.2 dropped the {@code BlockGetter} argument — the offset only ever depended on the
     * position — so the getter is simply unused from there on.
     */
    public static net.minecraft.world.phys.Vec3 blockOffset(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter getter, net.minecraft.core.BlockPos pos) {
        //? if >=1.21.2 {
        /*return state.getOffset(pos);
        *///?} else {
        return state.getOffset(getter, pos);
        //?}
    }

    /** {@code BlockState#isSolidRender}, which lost the same two arguments in 1.21.2. */
    public static boolean isSolidRender(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter getter, net.minecraft.core.BlockPos pos) {
        //? if >=1.21.2 {
        /*return state.isSolidRender();
        *///?} else {
        return state.isSolidRender(getter, pos);
        //?}
    }

    // ── Targeting ──────────────────────────────────────────────────────────────
    //
    // 1.21.2 moved every TargetingConditions-shaped lookup off EntityGetter and onto the new
    // ServerEntityGetter, which ServerLevel implements and Level does not: the conditions test now
    // takes a ServerLevel, because a targeting rule may need to ask the server about teams and
    // difficulty. That is a fair description of where these searches always ran — a Goal only ever
    // ticks server-side — but the *type* the call sites hold is Level, so each one needs the cast
    // that the old default method did not.
    //
    // The four wrappers below all read a Level and answer "nothing" off-thread of a server, which
    // is the honest answer: on a client level there are no server-side targeting conditions to
    // evaluate. None of the call sites can reach them from the client anyway.

    /**
     * Adapts one of this mod's {@code Predicate<LivingEntity>} target filters to whatever type the
     * goal constructors take.
     *
     * <p>1.21.2 replaced the predicate with {@code TargetingConditions.Selector}, which is the same
     * question plus the {@code ServerLevel} it is being asked on. Nothing in this mod's filters
     * looks at the level, so the extra argument is dropped and every filter — and every signature
     * that carries one around — stays a plain {@code Predicate<LivingEntity>}.
     */
    //? if >=1.21.2 {
    /*@Nullable
    public static net.minecraft.world.entity.ai.targeting.TargetingConditions.Selector targetSelector(@Nullable java.util.function.Predicate<net.minecraft.world.entity.LivingEntity> predicate) {
        return predicate == null ? null : (entity, level) -> predicate.test(entity);
    }
    *///?} else {
    @Nullable
    public static java.util.function.Predicate<net.minecraft.world.entity.LivingEntity> targetSelector(@Nullable java.util.function.Predicate<net.minecraft.world.entity.LivingEntity> predicate) {
        return predicate;
    }
    //?}

    /** {@code TargetingConditions#test}, which gained the {@code ServerLevel} it is judged on. */
    public static boolean testTargetConditions(net.minecraft.world.entity.ai.targeting.TargetingConditions conditions, net.minecraft.world.entity.LivingEntity attacker, net.minecraft.world.entity.LivingEntity target) {
        //? if >=1.21.2 {
        /*return attacker.level() instanceof net.minecraft.server.level.ServerLevel serverLevel && conditions.test(serverLevel, attacker, target);
        *///?} else {
        return conditions.test(attacker, target);
        //?}
    }

    /** {@code EntityGetter#getNearestEntity(List, …)}, now on {@code ServerEntityGetter}. */
    @Nullable
    public static <T extends net.minecraft.world.entity.LivingEntity> T getNearestEntity(net.minecraft.world.level.Level level, java.util.List<? extends T> candidates, net.minecraft.world.entity.ai.targeting.TargetingConditions conditions, @Nullable net.minecraft.world.entity.LivingEntity attacker, double x, double y, double z) {
        //? if >=1.21.2 {
        /*return level instanceof net.minecraft.server.level.ServerLevel serverLevel ? serverLevel.getNearestEntity(candidates, conditions, attacker, x, y, z) : null;
        *///?} else {
        return level.getNearestEntity(candidates, conditions, attacker, x, y, z);
        //?}
    }

    /** {@code EntityGetter#getNearestPlayer(TargetingConditions, …)}, now on {@code ServerEntityGetter}. */
    @Nullable
    public static net.minecraft.world.entity.player.Player getNearestPlayer(net.minecraft.world.level.Level level, net.minecraft.world.entity.ai.targeting.TargetingConditions conditions, @Nullable net.minecraft.world.entity.LivingEntity attacker, double x, double y, double z) {
        //? if >=1.21.2 {
        /*return level instanceof net.minecraft.server.level.ServerLevel serverLevel ? serverLevel.getNearestPlayer(conditions, attacker, x, y, z) : null;
        *///?} else {
        return level.getNearestPlayer(conditions, attacker, x, y, z);
        //?}
    }

    /** {@code EntityGetter#getNearbyPlayers}, now on {@code ServerEntityGetter}. */
    public static java.util.List<net.minecraft.world.entity.player.Player> getNearbyPlayers(net.minecraft.world.level.Level level, net.minecraft.world.entity.ai.targeting.TargetingConditions conditions, net.minecraft.world.entity.LivingEntity attacker, net.minecraft.world.phys.AABB area) {
        //? if >=1.21.2 {
        /*return level instanceof net.minecraft.server.level.ServerLevel serverLevel ? serverLevel.getNearbyPlayers(conditions, attacker, area) : java.util.List.of();
        *///?} else {
        return level.getNearbyPlayers(conditions, attacker, area);
        //?}
    }

    // ── World interaction narrowed to the server ───────────────────────────────
    //
    // 1.21.2 swept a family of "this only ever happens on the server" methods from Level to
    // ServerLevel. Every call site below already sits behind an isClientSide check or on a
    // server-only code path, so the cast always succeeds; these wrappers exist so the sites keep
    // spelling the plain Level they hold.

    /**
     * {@code BlockState#onBlockExploded}, whose level narrowed in 1.21.2. A blast is server-only.
     *
     * <p>The hook is a loader patch rather than vanilla, so Fabric runs the two statements that were
     * Forge's own interface default — read out of {@code IForgeBlock} in the 1.20.1 universal jar,
     * not guessed: clear the block with flag 3, then hand it to {@code Block#wasExploded}, which is
     * vanilla and is what actually gives TNT and the like their chain reaction.
     *
     * <p>That arm has to come FIRST from 1.21.2, not fall through to the version one: the version
     * arm calls the loader patch, which Fabric does not have, so an arm chain that asks the version
     * question before the loader question silently hands Fabric a method that is not there.
     * {@code wasExploded} took the same narrowing to {@code ServerLevel} in that version, so the
     * Fabric arm gains the same guard the loader arm has rather than a different one.
     */
    public static void onBlockExploded(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, @Nullable net.minecraft.world.level.Explosion explosion) {
        //? if fabric && >=1.21.2 {
        /*if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && explosion != null) {
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            state.getBlock().wasExploded(serverLevel, pos, explosion);
        }
        *///?} elif >=1.21.2 {
        /*if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && explosion != null) {
            state.onBlockExploded(serverLevel, pos, explosion);
        }
        *///?} elif fabric {
        /*level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        state.getBlock().wasExploded(level, pos, explosion);
        *///?} else {
        state.onBlockExploded(level, pos, explosion);
        //?}
    }

    /** {@code Projectile#mayInteract} — "is this projectile's owner allowed to change the world here". */
    public static boolean mayInteract(net.minecraft.world.entity.projectile.Projectile projectile, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        //? if >=1.21.2 {
        /*return level instanceof net.minecraft.server.level.ServerLevel serverLevel && projectile.mayInteract(serverLevel, pos);
        *///?} else {
        return projectile.mayInteract(level, pos);
        //?}
    }

    /** {@code PiglinAi#angerNearbyPiglins}, which gained the server level the sweep runs in. */
    public static void angerNearbyPiglins(net.minecraft.world.entity.player.Player player, boolean angerOnlyIfCanSee) {
        //? if >=1.21.2 {
        /*if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.world.entity.monster.piglin.PiglinAi.angerNearbyPiglins(serverLevel, player, angerOnlyIfCanSee);
        }
        *///?} else {
        net.minecraft.world.entity.monster.piglin.PiglinAi.angerNearbyPiglins(player, angerOnlyIfCanSee);
        //?}
    }

    // ── Members that moved, or lost their loader extension, in 1.21.2 ──────────

    /**
     * {@code BlockBehaviour.Properties#dropsLike}, renamed {@code overrideLootTable} in 1.21.2 when
     * the loot-table handle became a {@code ResourceKey}. A wrapper rather than a rename rule
     * because the two spellings take different arguments: the new one wants the source block's own
     * (optional) table, which {@code BlockBehaviour#getLootTable} hands back in exactly that shape.
     */
    public static net.minecraft.world.level.block.state.BlockBehaviour.Properties dropsLike(net.minecraft.world.level.block.state.BlockBehaviour.Properties properties, net.minecraft.world.level.block.Block source) {
        //? if >=1.21.2 {
        /*return properties.overrideLootTable(source.getLootTable());
        *///?} else {
        return properties.dropsLike(source);
        //?}
    }

    /**
     * A system message to one player. 1.21.2 took {@code sendSystemMessage} off {@code Entity} — it
     * lives only on {@code ServerPlayer} now — which is also the only place it ever did anything:
     * on every earlier version {@code Entity}'s implementation is an empty default. Routing through
     * {@code ServerPlayer} therefore states exactly what the old call already meant.
     */
    public static void sendSystemMessage(net.minecraft.world.entity.player.Player player, Component message) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(message);
        }
    }

    // The particle-and-sound burst LivingEntity#triggerItemUseEffects played while an entity was
    // eating or drinking. 1.21.2 moved the whole thing onto the item's consumable component, and
    // Consumable#emitParticlesAndSounds is what vanilla itself calls from ItemStack#onUseTick — so
    // this is the vanilla path, not a re-implementation. The old method is protected, which is why
    // there is no pre-1.21.2 arm here and the one call site gates rather than always coming
    // through this class.
    //? if >=1.21.2 {
    /*public static void emitItemUseEffects(net.minecraft.world.entity.LivingEntity entity, ItemStack stack, int particleCount) {
        net.minecraft.world.item.component.Consumable consumable = stack.get(net.minecraft.core.component.DataComponents.CONSUMABLE);
        if (consumable != null) {
            consumable.emitParticlesAndSounds(entity.getRandom(), entity, stack, particleCount);
        }
    }
    *///?}

    // ── The minecart rewrite ───────────────────────────────────────────────────
    //
    // canBeRidden/shouldDoRailFunctions/getSlopeAdjustment were never vanilla: all three came from
    // the loaders' minecart extension interface (NeoForge's IAbstractMinecartExtension, Forge's
    // IForgeAbstractMinecart), which 1.21.2 deleted along with the AbstractMinecart.Type enum the
    // first of them was written against. Vanilla grew isRideable() in the same release; the other
    // two were plain interface defaults, so their constants are inlined below. Fabric never had the
    // interface at all, so it takes the same answers on every version — the two constants directly,
    // and for rideability the instanceof test that was Forge's own default body.

    /** Whether this cart carries riders — the plain minecart, as opposed to a chest/furnace/TNT one. */
    public static boolean minecartRideable(net.minecraft.world.entity.vehicle.AbstractMinecart minecart) {
        //? if >=1.21.2 {
        /*return minecart.isRideable();
        *///?} elif fabric {
        /*return minecart instanceof net.minecraft.world.entity.vehicle.Minecart;
        *///?} else {
        return minecart.canBeRidden();
        //?}
    }

    /** Whether powered rails and {@code onMinecartPass} apply. The loader default was always true. */
    public static boolean minecartRailFunctions(net.minecraft.world.entity.vehicle.AbstractMinecart minecart) {
        //? if >=1.21.2 || fabric {
        /*return true;
        *///?} else {
        return minecart.shouldDoRailFunctions();
        //?}
    }

    /** Per-tick speed added climbing a sloped rail. The loader default was always 0.0078125. */
    public static double minecartSlopeAdjustment(net.minecraft.world.entity.vehicle.AbstractMinecart minecart) {
        //? if >=1.21.2 || fabric {
        /*return 0.0078125D;
        *///?} else {
        return minecart.getSlopeAdjustment();
        //?}
    }

    // ── The rail hooks NeoForge 26.1.2 deleted ─────────────────────────────────
    //
    // getRailMaxSpeed and onMinecartPass were loader extensions on BaseRailBlock (NeoForge's
    // IBaseRailBlockExtension, Forge's IForgeBaseRailBlock), letting a rail cap the cart on it and
    // be told when one passes. NeoForge 26.1.2.87 removed both with no successor — a full string
    // scan of the universal jar finds neither name anywhere, and vanilla's BaseRailBlock offers
    // nothing in their place (max speed now lives on MinecartBehavior#getMaxSpeed, which no rail
    // can reach). Forge 64.0.12 still has both, so the gates below are NeoForge-only.

    /**
     * The speed cap a plain rail imposes, as the two loaders' interface default computed it. Read
     * out of the bytecode of <i>both</i> NeoForge 26.1.1.15-beta and Forge 64.0.12 before inlining:
     * the two are instruction-for-instruction identical, so this is not a Forge-vs-NeoForge guess.
     */
    public static float defaultRailMaxSpeed(net.minecraft.world.entity.vehicle.AbstractMinecart cart) {
        if (cart instanceof net.minecraft.world.entity.vehicle.MinecartFurnace) {
            return cart.isInWater() ? 0.15F : 0.2F;
        }
        return cart.isInWater() ? 0.2F : 0.4F;
    }

    /**
     * Tells the rail a cart just passed over it. The loader default was an empty body, so on the
     * versions that dropped the hook this is a no-op — vanilla rails never wanted it, and there is
     * no channel left through which a third-party rail could be notified.
     */
    public static void railOnMinecartPass(net.minecraft.world.level.block.BaseRailBlock rail,
                                          net.minecraft.world.level.block.state.BlockState state,
                                          net.minecraft.world.level.Level level,
                                          net.minecraft.core.BlockPos pos,
                                          net.minecraft.world.entity.vehicle.AbstractMinecart cart) {
        //? if fabric || (neoforge && >=26.1.2) {
        /*return;
        *///?} else {
        rail.onMinecartPass(state, level, pos, cart);
        //?}
    }

    /**
     * Whether a vehicle wants its passengers drawn sitting.
     *
     * <p>{@code Entity#shouldRiderSit()} is a loader patch ({@code IForgeEntity}, NeoForge's
     * {@code IEntityExtension}) with no vanilla counterpart at all — vanilla sits every passenger and
     * offers the vehicle no say. On Fabric the question is asked of
     * {@link com.github.alexmodguy.alexscaves.server.entity.util.ACRiderSitEntity} instead, and a
     * vehicle that does not implement it answers {@code true}, which is the loaders' own interface
     * default (read out of {@code IForgeEntity} in the 1.20.1 universal jar: {@code iconst_1;
     * ireturn}).
     */
    public static boolean shouldRiderSit(net.minecraft.world.entity.Entity vehicle) {
        //? if fabric {
        /*return !(vehicle instanceof com.github.alexmodguy.alexscaves.server.entity.util.ACRiderSitEntity sitter) || sitter.shouldRiderSit();
        *///?} else {
        return vehicle.shouldRiderSit();
        //?}
    }

    /**
     * What a pathfinder should make of standing on this block.
     *
     * <p>{@code BlockState#getBlockPathType} is a loader patch ({@code IForgeBlockState}, NeoForge's
     * {@code IBlockStateExtension}); vanilla works the type out inside {@code WalkNodeEvaluator} from
     * a fixed cascade and gives a block no way to answer. On Fabric the question goes to
     * {@link com.github.alexmodguy.alexscaves.server.block.ACPathTypeBlock}, and a block that does
     * not implement it answers {@code null} — "no opinion", the loaders' own default for anything
     * that is neither lava nor on fire, both of which vanilla already handles for itself.
     */
    public static net.minecraft.world.level.pathfinder.BlockPathTypes getBlockPathType(
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.BlockGetter level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.entity.Mob mob) {
        //? if fabric {
        /*return state.getBlock() instanceof com.github.alexmodguy.alexscaves.server.block.ACPathTypeBlock typed ? typed.getBlockPathType(state, level, pos, mob) : null;
        *///?} else {
        return state.getBlockPathType(level, pos, mob);
        //?}
    }

    /**
     * The experience a block drops when it is mined, outside its loot table.
     *
     * <p>{@code BlockState#getExpDrop} is a loader patch ({@code IForgeBlockState}, NeoForge's
     * {@code IBlockStateExtension}) whose default is a flat {@code 0}; vanilla awards experience only
     * from the ore classes that carry an {@code IntProvider} of their own. On Fabric the question
     * goes to {@link com.github.alexmodguy.alexscaves.server.block.ACExpDropBlock} and anything that
     * does not implement it answers {@code 0}, matching the loaders.
     *
     * <p>This is the pre-1.21 shape, and the only one the mod ever asks for. Two callers: this mod's
     * own {@code MagneticWeaponEntity} — whose NeoForge-from-1.21 arm names the reshaped hook
     * directly, so it never reaches this method there — and, on Fabric only,
     * {@code mixin.fabric.ServerPlayerGameModeMixin}, which is where a <em>player</em> breaking a
     * block gets the experience the loaders' own patch would have popped.
     */
    //? if fabric {
    /*public static int getExpDrop(net.minecraft.world.level.block.state.BlockState state,
                                 net.minecraft.world.level.LevelReader level,
                                 net.minecraft.util.RandomSource randomSource,
                                 net.minecraft.core.BlockPos pos,
                                 int fortuneLevel,
                                 int silkTouchLevel) {
        return state.getBlock() instanceof com.github.alexmodguy.alexscaves.server.block.ACExpDropBlock typed ? typed.getExpDrop(state, level, randomSource, pos, fortuneLevel, silkTouchLevel) : 0;
    }
    *///?} elif !neoforge || <1.21 {
    public static int getExpDrop(net.minecraft.world.level.block.state.BlockState state,
                                 net.minecraft.world.level.LevelReader level,
                                 net.minecraft.util.RandomSource randomSource,
                                 net.minecraft.core.BlockPos pos,
                                 int fortuneLevel,
                                 int silkTouchLevel) {
        return state.getExpDrop(level, randomSource, pos, fortuneLevel, silkTouchLevel);
    }
    //?}

    /**
     * Answers whether {@code entity} can climb {@code state}.
     *
     * <p>{@code IForgeBlock#isLadder} is a loader patch whose default body — read out of
     * {@code forge-1.20.1-47.4.21-universal.jar} with {@code javap -c} — is exactly
     * {@code state.is(BlockTags.CLIMBABLE)}. Nothing in this mod or the vendored Citadel overrides
     * it, so the tag test <em>is</em> the whole behaviour on Fabric.
     *
     * <p>One wrapper serves both spellings the four call sites use: Forge declares the hook on
     * {@code BlockState} and on {@code Block}, and the {@code Block} receiver carries no
     * information the state does not, so it simply drops out here.
     */
    public static boolean isLadder(net.minecraft.world.level.block.state.BlockState state,
                                   net.minecraft.world.level.LevelReader level,
                                   net.minecraft.core.BlockPos pos,
                                   net.minecraft.world.entity.LivingEntity entity) {
        //? if fabric {
        /*return state.is(net.minecraft.tags.BlockTags.CLIMBABLE);
        *///?} else {
        return state.isLadder(level, pos, entity);
        //?}
    }

    /**
     * Answers whether an entity is currently part of its level.
     *
     * <p>Both loaders patch {@code Entity} with an {@code addedToWorld} flag they set either side of
     * the level's own add/remove. Vanilla tracks the same thing in the level's entity lookup, so
     * asking the level whether it still hands this id back is the same question — and, unlike the
     * flag, it cannot go stale on an entity that was removed without the patch firing.
     */
    public static boolean isAddedToWorld(net.minecraft.world.entity.Entity entity) {
        //? if fabric {
        /*return entity.level().getEntity(entity.getId()) == entity;
        *///?} else {
        return entity.isAddedToWorld();
        //?}
    }

    /**
     * Installs {@code value} as the entity's drop-capture list (or {@code null} to stop capturing)
     * and returns whatever list was installed before.
     *
     * <p>While a list is installed, {@code spawnAtLocation} adds the {@code ItemEntity} to it
     * instead of to the level — the only way to get a mob's own death drops as objects, which is
     * what the gum worm needs: it dies underground and re-drops everything at the surface. On
     * Fabric the field and the diversion are supplied by {@code mixin.fabric.EntityDropCaptureMixin}
     * through {@link com.github.alexmodguy.alexscaves.fabric.entity.ACDropCapture}.
     */
    public static java.util.Collection<net.minecraft.world.entity.item.ItemEntity> captureDrops(
            net.minecraft.world.entity.Entity entity,
            java.util.Collection<net.minecraft.world.entity.item.ItemEntity> value) {
        //? if fabric {
        /*return ((com.github.alexmodguy.alexscaves.fabric.entity.ACDropCapture) entity).ac_captureDrops(value);
        *///?} else {
        return entity.captureDrops(value);
        //?}
    }

    /**
     * Sets how much a lightning bolt hurts what it strikes.
     *
     * <p>Vanilla's {@code Entity#thunderHit} deals a hardcoded {@code 5.0F}; both loaders add a
     * {@code damage} field to {@code LightningBolt} and hurt for that. The tesla bulb builds a
     * visual-only bolt purely as a carrier for a value of {@code 1}. On Fabric the field lives in
     * {@code mixin.fabric.LightningBoltDamageMixin} and the constant is redirected in
     * {@code mixin.fabric.EntityThunderHitMixin}, so every vanilla override of {@code thunderHit} —
     * creeper charging, villager-to-witch, pig-to-piglin, turtle eggs — keeps working unchanged.
     */
    public static void setLightningDamage(net.minecraft.world.entity.LightningBolt bolt, float damage) {
        //? if fabric {
        /*((com.github.alexmodguy.alexscaves.fabric.entity.ACLightningDamage) bolt).ac_setLightningDamage(damage);
        *///?} else {
        bolt.setDamage(damage);
        //?}
    }

    /**
     * Answers whether a block drags its neighbours the way slime does.
     *
     * <p>{@code IForgeBlock#isStickyBlock}'s default, out of the universal jar, is the pair of
     * identity tests vanilla's piston code writes inline; the magnet block is the only caller here.
     */
    public static boolean isStickyBlock(net.minecraft.world.level.block.state.BlockState state) {
        //? if fabric {
        /*return state.getBlock() == net.minecraft.world.level.block.Blocks.SLIME_BLOCK || state.getBlock() == net.minecraft.world.level.block.Blocks.HONEY_BLOCK;
        *///?} else {
        return state.isStickyBlock();
        //?}
    }

    /**
     * Answers whether two blocks stick to each other.
     *
     * <p>{@code IForgeBlock#canStickTo}'s default reproduces vanilla's one exception — honey and
     * slime never stick to <em>each other</em>, though each sticks to everything else it touches.
     */
    public static boolean canStickTo(net.minecraft.world.level.block.state.BlockState state,
                                     net.minecraft.world.level.block.state.BlockState other) {
        //? if fabric {
        /*if (state.getBlock() == net.minecraft.world.level.block.Blocks.HONEY_BLOCK && other.getBlock() == net.minecraft.world.level.block.Blocks.SLIME_BLOCK) {
            return false;
        }
        if (state.getBlock() == net.minecraft.world.level.block.Blocks.SLIME_BLOCK && other.getBlock() == net.minecraft.world.level.block.Blocks.HONEY_BLOCK) {
            return false;
        }
        return isStickyBlock(state) || isStickyBlock(other);
        *///?} else {
        return state.canStickTo(other);
        //?}
    }

    /**
     * How far a player can reach a block.
     *
     * <p>Forge patches {@code Player#getBlockReach()}; from 1.20.5 vanilla has the same number as an
     * attribute, and below it the value is the two constants the game mode used. Both spellings come
     * out at 5 blocks in creative and 4.5 otherwise, so the Fabric arm needs no version split.
     */
    public static double blockReach(net.minecraft.world.entity.player.Player player) {
        //? if fabric {
        /*return player.isCreative() ? 5.0D : 4.5D;
        *///?} else {
        return player.getBlockReach();
        //?}
    }

    /**
     * Answers whether {@code entity} is allowed to destroy {@code state}.
     *
     * <p>{@code IForgeBlock#canEntityDestroy}'s default answers {@code true} for everything except
     * the ender dragon (which honours {@code BlockTags.DRAGON_IMMUNE}) and the wither. Neither is
     * reachable here: the sole call site is the forsaken, which passes itself, and a block that
     * wants to refuse says so through {@code ACTagRegistry.UNMOVEABLE}, which that call site tests
     * separately. So the Fabric arm is the default's third branch, and reproducing the other two
     * would only add two class references that move house repeatedly over the version range.
     */
    public static boolean canEntityDestroy(net.minecraft.world.level.block.state.BlockState state,
                                           net.minecraft.world.level.Level level,
                                           net.minecraft.core.BlockPos pos,
                                           net.minecraft.world.entity.Entity entity) {
        //? if fabric {
        /*return true;
        *///?} else {
        return state.canEntityDestroy(level, pos, entity);
        //?}
    }

    /**
     * Ignites a block the way the remote detonator does.
     *
     * <p>{@code IForgeBlock#onCaughtFire}'s default body is <em>empty</em> — vanilla has no such
     * method, and everything the hook does for vanilla lives in {@code TntBlock}'s override of it.
     * So on Fabric the dispatch is written out: mod blocks answer through
     * {@link com.github.alexmodguy.alexscaves.server.block.ACIgnitableBlock} and plain TNT primes
     * itself, which together are exactly the two entries of
     * {@code ACTagRegistry.REMOTE_DETONATOR_ACTIVATES}.
     *
     * <p>The TNT half calls the <em>private</em> igniter-carrying overload, widened by the access
     * widener, because that is what Forge's own {@code TntBlock#onCaughtFire} calls — its whole body
     * is {@code explode(level, pos, igniter)}. The public overload hardcodes a {@code null} igniter
     * and would silently drop the remote detonator's attribution to the player who fired it.
     *
     * <p>⚠️ 1.21.5 renamed that method {@code prime} and gave it a {@code boolean} return, in the
     * same change that put a boolean on the loaders' hook — hence the two Fabric arms, which is also
     * why the {@code fabric} arm cannot simply be left unbounded. It stays a flat chain because a
     * Stonecutter gate does not nest inside another arm.
     *
     * <p>Returns nothing on every node even though the hook itself reports a {@code boolean} from
     * 1.21.5: the one call site ignores the answer, and a bare invocation is a legal statement
     * whatever it returns.
     */
    public static void onCaughtFire(net.minecraft.world.level.block.state.BlockState state,
                                    net.minecraft.world.level.Level level,
                                    net.minecraft.core.BlockPos pos,
                                    net.minecraft.core.Direction face,
                                    net.minecraft.world.entity.LivingEntity igniter) {
        //? if fabric && >=1.21.5 {
        /*if (state.getBlock() instanceof com.github.alexmodguy.alexscaves.server.block.ACIgnitableBlock typed) {
            typed.onCaughtFire(state, level, pos, face, igniter);
        } else if (state.getBlock() instanceof net.minecraft.world.level.block.TntBlock) {
            net.minecraft.world.level.block.TntBlock.prime(level, pos, igniter);
        }
        *///?} elif fabric {
        /*if (state.getBlock() instanceof com.github.alexmodguy.alexscaves.server.block.ACIgnitableBlock typed) {
            typed.onCaughtFire(state, level, pos, face, igniter);
        } else if (state.getBlock() instanceof net.minecraft.world.level.block.TntBlock) {
            net.minecraft.world.level.block.TntBlock.explode(level, pos, igniter);
        }
        *///?} else {
        state.onCaughtFire(level, pos, face, igniter);
        //?}
    }

    /**
     * The direction a rail carries a minecart in.
     *
     * <p>{@code IForgeRailBlock#getRailDirection} exists so a rail can lie about its shape per
     * cart; no rail in this mod does, and the default simply reads the block's own shape property.
     * {@code BaseRailBlock#getShapeProperty()} is public abstract vanilla, so the Fabric arm is the
     * default verbatim.
     */
    public static net.minecraft.world.level.block.state.properties.RailShape railDirection(
            net.minecraft.world.level.block.BaseRailBlock rail,
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.entity.vehicle.AbstractMinecart cart) {
        //? if fabric {
        /*return state.getValue(rail.getShapeProperty());
        *///?} else {
        return rail.getRailDirection(state, level, pos, cart);
        //?}
    }

    /**
     * The sound a block makes underfoot, as heard by one particular entity at one particular place.
     *
     * <p>{@code IForgeBlockState#getSoundType(LevelReader, BlockPos, Entity)} is the loaders'
     * position-aware overload; vanilla has only the no-argument one, which reads the block's own
     * declared {@code SoundType}. The one block in this mod that varies its sound —
     * {@code PewenBranchBlock}, quiet grass when it still has pines — varies it on a
     * <em>block state</em> property, so it answers the vanilla accessor just as well; it simply has
     * to override the vanilla spelling on Fabric, which it does.
     */
    public static net.minecraft.world.level.block.SoundType soundType(
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.LevelReader level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.entity.Entity entity) {
        //? if fabric {
        /*return state.getSoundType();
        *///?} else {
        return state.getSoundType(level, pos, entity);
        //?}
    }

    /**
     * Builds a block-entity type from a supplier and the blocks it may sit in.
     *
     * <p>1.21.2 dropped {@code BlockEntityType.Builder} for a plain constructor, and the
     * {@code !mc2102-blockentitytype-builder} replacement rule rewrites all 21 registrations in
     * {@code ACBlockEntityRegistry} onto this method rather than onto the constructor directly —
     * because the constructor takes a {@code Set<Block>} and the call sites are variadic.
     * <b>NeoForge</b> patches a varargs overload back in, so it could name the constructor; Forge
     * does not, and Fabric will not either. One helper is the shape that holds on every node.
     */
    //? if >=1.21.2 {
    /*public static <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityType<T> blockEntityType(
            net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier<? extends T> supplier,
            net.minecraft.world.level.block.Block... blocks) {
        return new net.minecraft.world.level.block.entity.BlockEntityType<>(supplier, java.util.Set.of(blocks));
    }
    *///?}

    /**
     * The equipment-model id an {@code ArmorMaterial} carries from 1.21.2. All six of this mod's sets
     * are drawn by its own layer ({@code CustomArmorPostRender}), so nothing is shipped under the id —
     * a missing one resolves to an empty layer map and logs nothing. It is still namespaced to this
     * mod rather than borrowed from vanilla, so shipping a real model later needs no code change.
     *
     * <p>It lives here rather than on {@code ACArmorMaterial} because 1.21.4 turned it from a
     * {@code ResourceLocation} into a {@code ResourceKey<EquipmentAsset>}, and the call site is inside
     * an already-commented {@code >=1.21.2} arm, where a second block gate cannot nest.
     */
    //? if >=1.21.4 {
    /*public static net.minecraft.resources.ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> equipmentAsset(String name) {
        return net.minecraft.resources.ResourceKey.create(
                net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.github.alexmodguy.alexscaves.AlexsCaves.MODID, name));
    }
    *///?} elif >=1.21.2 {
    /*public static net.minecraft.resources.ResourceLocation equipmentAsset(String name) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.github.alexmodguy.alexscaves.AlexsCaves.MODID, name);
    }
    *///?}

    /**
     * Spawns a particle that ignores the client's particle-distance limiter — what the mod's thirteen
     * "kicked-up block dust" effects have always asked for.
     *
     * <p>1.21.4 inserted a second boolean into that overload:
     * {@code addParticle(options, overrideLimiter, decreased, x, y, z, dx, dy, dz)}. The old flag is the
     * first one; {@code decreased} is new, and {@code false} is what keeps the old behaviour (it is the
     * "reduced particles" hint vanilla passes for its own ambient effects).
     */
    public static void addParticleForced(net.minecraft.world.level.Level level,
                                         net.minecraft.core.particles.ParticleOptions options,
                                         double x, double y, double z,
                                         double dx, double dy, double dz) {
        //? if >=1.21.4 {
        /*level.addParticle(options, true, false, x, y, z, dx, dy, dz);
        *///?} else {
        level.addParticle(options, true, x, y, z, dx, dy, dz);
        //?}
    }

    // ── Entity members 1.21.5 renamed or folded away ───────────────────────────

    /**
     * 1.21.5 deleted {@code isInWaterOrBubble}. The bubble half was always redundant — a bubble column
     * block <em>is</em> waterlogged, so {@code isInWater()} was already true inside one — which is why
     * vanilla's own call sites simply became {@code isInWater()}. Old nodes keep the exact old call so
     * this cannot quietly change behaviour on versions that have been tested.
     */
    public static boolean isInWaterOrBubble(net.minecraft.world.entity.Entity entity) {
        //? if >=1.21.5 {
        /*return entity.isInWater();
        *///?} else {
        return entity.isInWaterOrBubble();
        //?}
    }

    /*
     * 1.21.5 renamed Entity#moveTo/absMoveTo to snapTo/absSnapTo. A replacements.string rule cannot do
     * this one: PathNavigation#moveTo kept its name, and the mod has 89 of those against 16 of these.
     */

    public static void moveTo(net.minecraft.world.entity.Entity entity, double x, double y, double z, float yRot, float xRot) {
        //? if >=1.21.5 {
        /*entity.snapTo(x, y, z, yRot, xRot);
        *///?} else {
        entity.moveTo(x, y, z, yRot, xRot);
        //?}
    }

    public static void moveTo(net.minecraft.world.entity.Entity entity, double x, double y, double z) {
        //? if >=1.21.5 {
        /*entity.snapTo(x, y, z);
        *///?} else {
        entity.moveTo(x, y, z);
        //?}
    }

    public static void moveTo(net.minecraft.world.entity.Entity entity, net.minecraft.world.phys.Vec3 pos) {
        //? if >=1.21.5 {
        /*entity.snapTo(pos);
        *///?} else {
        entity.moveTo(pos);
        //?}
    }

    public static void absMoveTo(net.minecraft.world.entity.Entity entity, double x, double y, double z, float yRot, float xRot) {
        //? if >=1.21.5 {
        /*entity.absSnapTo(x, y, z, yRot, xRot);
        *///?} else {
        entity.absMoveTo(x, y, z, yRot, xRot);
        //?}
    }

    // ── NBT reads, after 1.21.5 made every getter Optional ─────────────────────

    /*
     * 1.21.5 rebuilt CompoundTag and ListTag around Optional: getInt(String) now answers
     * Optional<Integer>, and each getter gained an "…Or(key, fallback)" sibling that keeps the old
     * value-returning shape. The fallbacks below are exactly what the pre-1.21.5 getters returned
     * for a missing or wrong-typed key, so a call site reads and behaves identically on every node.
     *
     * The same release deleted the three NBT UUID helpers outright — a UUID is stored through
     * UUIDUtil.CODEC now, which is the very int-array form putUUID always wrote, so old saves load
     * unchanged.
     *
     * These are static rather than gated at the call sites because there are ~460 of them; a
     * replacements.string rename could not do it either, since the new spelling needs a second
     * argument the old one does not have.
     */

    public static boolean getBoolean(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getBooleanOr(key, false);
        *///?} else {
        return tag.getBoolean(key);
        //?}
    }

    public static byte getByte(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getByteOr(key, (byte) 0);
        *///?} else {
        return tag.getByte(key);
        //?}
    }

    public static short getShort(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getShortOr(key, (short) 0);
        *///?} else {
        return tag.getShort(key);
        //?}
    }

    public static int getInt(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getIntOr(key, 0);
        *///?} else {
        return tag.getInt(key);
        //?}
    }

    public static long getLong(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getLongOr(key, 0L);
        *///?} else {
        return tag.getLong(key);
        //?}
    }

    public static float getFloat(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getFloatOr(key, 0.0F);
        *///?} else {
        return tag.getFloat(key);
        //?}
    }

    public static double getDouble(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getDoubleOr(key, 0.0D);
        *///?} else {
        return tag.getDouble(key);
        //?}
    }

    public static String getString(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getStringOr(key, "");
        *///?} else {
        return tag.getString(key);
        //?}
    }

    public static int[] getIntArray(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getIntArray(key).orElseGet(() -> new int[0]);
        *///?} else {
        return tag.getIntArray(key);
        //?}
    }

    public static byte[] getByteArray(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getByteArray(key).orElseGet(() -> new byte[0]);
        *///?} else {
        return tag.getByteArray(key);
        //?}
    }

    public static CompoundTag getCompound(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getCompoundOrEmpty(key);
        *///?} else {
        return tag.getCompound(key);
        //?}
    }

    /**
     * The {@code type} argument is the element id the pre-1.21.5 overload filtered on. 1.21.5 has no
     * filtering form — a list whose elements are of the wrong type simply reads back as whatever it
     * holds — so the argument is kept for the old nodes and ignored on the new ones.
     */
    public static net.minecraft.nbt.ListTag getList(CompoundTag tag, String key, int type) {
        //? if >=1.21.5 {
        /*return tag.getListOrEmpty(key);
        *///?} else {
        return tag.getList(key, type);
        //?}
    }

    public static boolean contains(CompoundTag tag, String key, int type) {
        //? if >=1.21.5 {
        /*net.minecraft.nbt.Tag found = tag.get(key);
        return found != null && found.getId() == type;
        *///?} else {
        return tag.contains(key, type);
        //?}
    }

    /**
     * A tag's value rendered for display.
     *
     * <p>{@code Tag#getAsString()} was the SNBT text of any tag; 1.21.5 renamed it {@code asString()}
     * and narrowed it to "the String this tag holds, if it holds one", answering an empty Optional
     * for every other type. Falling back to {@code toString()} keeps the old, type-agnostic output —
     * which is what the one call site (the spelunkery table's bad-tablet dump) wants.
     */
    public static String tagAsString(net.minecraft.nbt.Tag tag) {
        //? if >=1.21.5 {
        /*return tag.asString().orElseGet(tag::toString);
        *///?} else {
        return tag.getAsString();
        //?}
    }

    /**
     * The main + hotbar section of a player's inventory.
     *
     * <p>1.21.5 moved armour and the offhand out of {@code Inventory} into the equipment component
     * and made the remaining {@code items} list private, exposing it as
     * {@code getNonEquipmentItems()} — the same 36 slots in the same order on every version.
     */
    public static java.util.List<ItemStack> inventoryItems(net.minecraft.world.entity.player.Inventory inventory) {
        //? if >=1.21.5 {
        /*return inventory.getNonEquipmentItems();
        *///?} else {
        return inventory.items;
        //?}
    }

    public static java.util.Set<String> getAllKeys(CompoundTag tag) {
        //? if >=1.21.5 {
        /*return tag.keySet();
        *///?} else {
        return tag.getAllKeys();
        //?}
    }

    // ── The NBT UUID helpers 1.21.5 deleted ────────────────────────────────────

    public static boolean hasUUID(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.read(key, net.minecraft.core.UUIDUtil.CODEC).isPresent();
        *///?} else {
        return tag.hasUUID(key);
        //?}
    }

    /**
     * Answers {@link net.minecraft.Util#NIL_UUID} for a missing key, matching what the vanilla helper
     * did — it read the int array unguarded and would have thrown, and every call site here guards
     * with {@link #hasUUID} first.
     */
    public static java.util.UUID getUUID(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.read(key, net.minecraft.core.UUIDUtil.CODEC).orElse(net.minecraft.Util.NIL_UUID);
        *///?} else {
        return tag.getUUID(key);
        //?}
    }

    public static void putUUID(CompoundTag tag, String key, java.util.UUID uuid) {
        //? if >=1.21.5 {
        /*tag.store(key, net.minecraft.core.UUIDUtil.CODEC, uuid);
        *///?} else {
        tag.putUUID(key, uuid);
        //?}
    }

    // ── The same reads off a ListTag ───────────────────────────────────────────

    public static CompoundTag getCompound(net.minecraft.nbt.ListTag list, int index) {
        //? if >=1.21.5 {
        /*return list.getCompoundOrEmpty(index);
        *///?} else {
        return list.getCompound(index);
        //?}
    }

    public static int getInt(net.minecraft.nbt.ListTag list, int index) {
        //? if >=1.21.5 {
        /*return list.getIntOr(index, 0);
        *///?} else {
        return list.getInt(index);
        //?}
    }

    public static float getFloat(net.minecraft.nbt.ListTag list, int index) {
        //? if >=1.21.5 {
        /*return list.getFloatOr(index, 0.0F);
        *///?} else {
        return list.getFloat(index);
        //?}
    }

    public static double getDouble(net.minecraft.nbt.ListTag list, int index) {
        //? if >=1.21.5 {
        /*return list.getDoubleOr(index, 0.0D);
        *///?} else {
        return list.getDouble(index);
        //?}
    }

    public static String getString(net.minecraft.nbt.ListTag list, int index) {
        //? if >=1.21.5 {
        /*return list.getStringOr(index, "");
        *///?} else {
        return list.getString(index);
        //?}
    }

    // 1.21.5 made TagParser generic over a DynamicOps and renamed the "parse this whole string as a
    // compound" entry point to parseCompoundFully. Both throw CommandSyntaxException on bad input.
    public static net.minecraft.nbt.CompoundTag parseTag(String snbt) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        //? if >=1.21.5 {
        /*return net.minecraft.nbt.TagParser.parseCompoundFully(snbt);
        *///?} else {
        return net.minecraft.nbt.TagParser.parseTag(snbt);
        //?}
    }

    // "can this tool do that job?" — the loader's tool-action question, which vanilla has never asked
    // and only two of the three loaders answer. Forge and NeoForge patch it onto ItemStack (NeoForge
    // renaming the token to an item ability at 1.21); Fabric patches nothing, so the mod answers it
    // itself, in the stand-in that declares the tokens.
    //
    // ⚠️ NOT named canPerformAction: a replacement rule rewrites that name's ItemStack parameter on
    // NeoForge 26, and it matches by literal text, so a helper spelled the loader's way would be
    // retyped along with the overrides the rule is for.
    public static boolean canPerform(net.minecraft.world.item.ItemStack stack, net.minecraftforge.common.ToolAction action) {
        //? if fabric {
        /*return com.github.alexmodguy.alexscaves.fabric.forge.common.ToolActions.canPerform(stack, action);
        *///?} else {
        return stack.canPerformAction(action);
        //?}
    }

    // "is the thing this player is holding up a shield?". Blocking became a vanilla data component
    // in 1.21.5 (BlocksAttacks, which is what gives a shield its cooldown and its damage cap), and
    // both loaders dropped the tool-action/item-ability that used to answer this — NeoForge's
    // ItemAbilities has no shield constant left at all. The component's presence is the same
    // question and covers modded shields the same way the ability did.
    public static boolean canBlockAttacks(net.minecraft.world.item.ItemStack stack) {
        //? if >=1.21.5 {
        /*return stack.get(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS) != null;
        *///?} else {
        return canPerform(stack, net.minecraftforge.common.ToolActions.SHIELD_BLOCK);
        //?}
    }

    /**
     * "Is the sun up over this spot?" — the clock question two of this mod's features ask, without
     * regard to weather or to whether the sky is actually visible (both callers test {@code
     * canSeeSky} themselves).
     *
     * <p>Below 1.21.11 that is the upstream spelling verbatim: {@code Level#getTimeOfDay} is the sun's
     * angle as a fraction, {@code 0} at noon and {@code 0.5} at midnight, so the window either side of
     * noon is {@code < 0.259 || > 0.74}. 1.21.11 deleted {@code getTimeOfDay} — the celestial angles
     * became {@code EnvironmentAttributes} driven by keyframe tracks on a dimension's timeline — and
     * the successor to "the sun is up here" is {@code MONSTERS_BURN}: a boolean attribute whose
     * {@code DAY} timeline track is {@code false} from tick 12542 and {@code true} from 23460, and
     * whose only vanilla consumer is {@code Mob#isSunBurnTick}, i.e. exactly the old
     * {@code isBrightOutside}-plus-clock test. It is a clock track only, so weather does not enter
     * into it here any more than it did before.
     *
     * <p>⚠️ One deliberate behaviour change on 1.21.11 and up: a dimension with no {@code DAY}
     * timeline gets the attribute's default of {@code false}. The End used to answer {@code true}
     * here, because its fixed time of 6000 made {@code getTimeOfDay} return ~0 — permanent noon — so
     * {@link com.github.alexmodguy.alexscaves.server.potion.DarknessIncarnateEffect}, which does not
     * test {@code hasFixedTime}, treated the whole End as sunlit. It no longer does. That is the
     * defensible reading of both features (the End has no sun), and the Nether is unaffected either
     * way since nothing there can see the sky.
     */
    public static boolean sunAboveHorizon(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        //? if >=1.21.11 {
        /*return level.environmentAttributes().getValue(
                net.minecraft.world.attribute.EnvironmentAttributes.MONSTERS_BURN, pos);
        *///?} else {
        float timeOfDay = level.getTimeOfDay(1.0F); //night starts at 0.259 and ends at 0.74
        return timeOfDay < 0.259F || timeOfDay > 0.74F;
        //?}
    }

    // ── Player messages ────────────────────────────────────────────────────────

    /**
     * {@code Player#displayClientMessage(Component, boolean)}, which 26 deleted.
     *
     * <p>The one method split in two: {@code sendOverlayMessage} is the {@code true} (action-bar)
     * half and {@code sendSystemMessage} the {@code false} (chat) half. Both names already exist
     * lower down the range — {@code sendSystemMessage} since 1.19 — but the overlay half does not,
     * so the pre-26 arm still has to go through the old one-method spelling.
     *
     * <p>All thirteen call sites in this tree pass {@code true}; the {@code false} branch is here
     * so the helper keeps the vanilla signature and a future caller cannot get it wrong.
     */
    public static void displayClientMessage(net.minecraft.world.entity.player.Player player, Component message, boolean overlay) {
        //? if >=26 {
        /*if (overlay) {
            player.sendOverlayMessage(message);
        } else {
            player.sendSystemMessage(message);
        }
        *///?} else {
        player.displayClientMessage(message, overlay);
        //?}
    }

    // ── Item particles ─────────────────────────────────────────────────────────

    /**
     * {@code new ItemParticleOption(type, stack)}, whose {@link ItemStack} constructor 26 replaced
     * with {@code Item} and {@code ItemStackTemplate} overloads.
     *
     * <p>{@code ItemStackTemplate.fromNonEmptyStack} is the exact converter — same item holder,
     * count and component patch the old constructor copied — but it <em>throws</em> on an empty
     * stack where the old one happily built an air particle that drew nothing. The guard keeps the
     * pre-26 behaviour rather than turning a harmless no-op particle into a crash.
     */
    public static net.minecraft.core.particles.ItemParticleOption itemParticle(
            net.minecraft.core.particles.ParticleType<net.minecraft.core.particles.ItemParticleOption> type, ItemStack stack) {
        //? if >=26 {
        /*net.minecraft.world.item.ItemStackTemplate template = stack.isEmpty()
                ? new net.minecraft.world.item.ItemStackTemplate(net.minecraft.world.item.Items.AIR)
                : net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(stack);
        return new net.minecraft.core.particles.ItemParticleOption(type, template);
        *///?} else {
        return new net.minecraft.core.particles.ItemParticleOption(type, stack);
        //?}
    }

    /**
     * The stack an item particle was spawned for — the read half of {@link #itemParticle}.
     *
     * <p>From 26 the option stores an {@code ItemStackTemplate}, so {@code getItem()} no longer hands
     * back a stack; {@code create()} builds the one the particle's own code wants. It is a fresh stack
     * each call, which is what the receiver wants anyway (it only reads the item and its components).
     */
    public static ItemStack particleStack(net.minecraft.core.particles.ItemParticleOption option) {
        //? if >=26 {
        /*return option.getItem().create();
        *///?} else {
        return option.getItem();
        //?}
    }

    // ── Chunk positions ────────────────────────────────────────────────────────
    //
    // 26 made ChunkPos a record, and every one of its four legacy spellings went with it:
    //
    //   new ChunkPos(BlockPos)   -> ChunkPos.containing(BlockPos)
    //   new ChunkPos(long)       -> ChunkPos.unpack(long)
    //   pos.x / pos.z            -> pos.x() / pos.z()      (the fields are private now)
    //   ChunkPos.asLong(...)     -> ChunkPos.pack(...)  /  pos.toLong() -> pos.pack()
    //
    // Only `new ChunkPos(int, int)` survives unchanged, which is exactly why none of this can be a
    // `replacements.string` rule: a rule on `new ChunkPos(` would rewrite the two legal two-int call
    // sites as well, and one on `.x` is far too broad to be safe on any file in the tree. So the
    // four shims below keep the pre-26 spelling at the call site, the same way the rest of this
    // class does.

    /**
     * {@code new ChunkPos(BlockPos)}, which 26 replaced with the static {@code containing}.
     */
    public static net.minecraft.world.level.ChunkPos chunkPos(net.minecraft.core.BlockPos pos) {
        //? if >=26 {
        /*return net.minecraft.world.level.ChunkPos.containing(pos);
        *///?} else {
        return new net.minecraft.world.level.ChunkPos(pos);
        //?}
    }

    /**
     * {@code new ChunkPos(long)}, which 26 replaced with the static {@code unpack}.
     */
    public static net.minecraft.world.level.ChunkPos chunkPos(long packed) {
        //? if >=26 {
        /*return net.minecraft.world.level.ChunkPos.unpack(packed);
        *///?} else {
        return new net.minecraft.world.level.ChunkPos(packed);
        //?}
    }

    /**
     * {@code ChunkPos#x}, a public field until 26 made the class a record and the field private.
     */
    public static int chunkX(net.minecraft.world.level.ChunkPos pos) {
        //? if >=26 {
        /*return pos.x();
        *///?} else {
        return pos.x;
        //?}
    }

    /**
     * {@code ChunkPos#z}, a public field until 26 made the class a record and the field private.
     */
    public static int chunkZ(net.minecraft.world.level.ChunkPos pos) {
        //? if >=26 {
        /*return pos.z();
        *///?} else {
        return pos.z;
        //?}
    }

    /**
     * {@code ChunkPos#asLong(int, int)}, renamed {@code pack} in 26. The packing itself is
     * unchanged — low int is x, high int is z — so a long written on one version still reads on the
     * other, which matters because these keys go into vanilla's own chunk maps.
     */
    public static long chunkAsLong(int chunkX, int chunkZ) {
        //? if >=26 {
        /*return net.minecraft.world.level.ChunkPos.pack(chunkX, chunkZ);
        *///?} else {
        return net.minecraft.world.level.ChunkPos.asLong(chunkX, chunkZ);
        //?}
    }

    /**
     * {@code ChunkPos#toLong()}, renamed {@code pack()} in 26.
     */
    public static long chunkAsLong(net.minecraft.world.level.ChunkPos pos) {
        //? if >=26 {
        /*return pos.pack();
        *///?} else {
        return pos.toLong();
        //?}
    }

    /**
     * {@code BlockStateProvider#getState}, which gained a leading {@link
     * net.minecraft.world.level.WorldGenLevel} in 26 so a provider can look at what it is being placed
     * into. Every one of this mod's eight call sites already has that level in hand, so the shim keeps
     * the pre-26 shape and simply drops the argument below 26.
     */
    public static net.minecraft.world.level.block.state.BlockState providerState(
            net.minecraft.world.level.WorldGenLevel level,
            net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider provider,
            net.minecraft.util.RandomSource random,
            net.minecraft.core.BlockPos pos) {
        //? if >=26 {
        /*return provider.getState(level, random, pos);
        *///?} else {
        return provider.getState(random, pos);
        //?}
    }

    /**
     * {@code LivingEntity#knockback(strength, xRatio, zRatio)}, which 26.2 replaced with a form
     * carrying the {@link net.minecraft.world.damagesource.DamageSource} and damage amount that
     * caused the shove — so that an override can tell a projectile hit from a melee one without the
     * caller having to compute the direction itself (see {@code dealDefaultKnockback}, which is the
     * new front door and derives xRatio/zRatio from the source's position).
     *
     * <p>All 24 of this mod's call sites compute the direction themselves and want the plain "shove
     * this entity that way" they always had, so the shim keeps the three-argument shape and fills
     * the two new parameters with a generic source and zero damage on 26.2. That is behaviourally
     * exact on vanilla: the 6-argument implementation reads only strength, xRatio and zRatio — the
     * source, the amount and the trailing flag are passed through for overrides and are untouched by
     * the physics (read in the bytecode of the patched jar, not assumed).
     */
    public static void knockback(net.minecraft.world.entity.LivingEntity target, double strength, double xRatio, double zRatio) {
        //? if >=26.2 {
        /*target.knockback(strength, xRatio, zRatio, target.damageSources().generic(), 0.0F);
        *///?} else {
        target.knockback(strength, xRatio, zRatio);
        //?}
    }

    /**
     * {@code Entity#isMultipartEntity()}, which is a loader patch on vanilla rather than an API —
     * so it exists on two of the three loaders and there is nothing to rename it to on the third.
     *
     * <p>The seven parents in this mod implement {@link
     * com.github.alexmodguy.alexscaves.server.entity.util.ACMultipartOwner} on every loader and can
     * simply be asked; this shim is for the one caller that holds a bare {@code Entity} — the
     * multipart message handler, which resolves the parent by id off the wire. The ender dragon is
     * named explicitly because the patched method answers {@code true} for it and nothing else in
     * vanilla, so the two arms agree on every entity in the game, not just on this mod's.
     */
    public static boolean isMultipartEntity(net.minecraft.world.entity.Entity entity) {
        //? if fabric {
        /*return entity instanceof com.github.alexmodguy.alexscaves.server.entity.util.ACMultipartOwner owner && owner.isMultipartEntity()
                || entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon;
        *///?} else {
        return entity.isMultipartEntity();
        //?}
    }

    /**
     * Adds {@code minecraft:tempt_range} to an attribute builder, on the versions that have it.
     *
     * <p>⚠️ <b>This is a crash, not a cosmetic gap.</b> 1.21.2 moved {@code TemptGoal}'s range from a
     * hard-coded {@code TargetingConditions.range(10.0)} onto a new {@code TEMPT_RANGE} attribute,
     * read unconditionally at the top of {@code TemptGoal#canUse}. Vanilla adds it in {@code
     * Animal.createAnimalAttributes()} only — {@code Mob}/{@code Monster}/{@code LivingEntity} do
     * not carry it — and {@code AttributeSupplier#getAttributeInstance} throws {@code
     * IllegalArgumentException: Can't find attribute minecraft:tempt_range} for an attribute the
     * supplier never declared. All three loaders are identical here (checked in each one's own
     * patched jar), so every one of this mod's eleven {@code TemptGoal} mobs — all of which build
     * from {@code Monster.createMonsterAttributes()} — killed the server on its first goal tick on
     * every node from 1.21.2 up. Found by summoning an atlatitan on {@code 1.21.11-fabric}.
     *
     * <p>{@code 10.0} is not a guess: it is both vanilla's own default for the attribute and the
     * constant 1.20.1's {@code TemptGoal} baked in, so the behaviour is unchanged on all 58 nodes.
     */
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder temptable(net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder builder) {
        //? if >=1.21.2 {
        /*return builder.add(net.minecraft.world.entity.ai.attributes.Attributes.TEMPT_RANGE, 10.0D);
        *///?} else {
        return builder;
        //?}
    }
}
