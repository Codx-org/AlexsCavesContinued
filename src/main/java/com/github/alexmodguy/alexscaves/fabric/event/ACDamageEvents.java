package com.github.alexmodguy.alexscaves.fabric.event;

import com.github.alexmodguy.alexscaves.fabric.forge.common.MinecraftForge;
import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living.LivingAttackEvent;
import com.github.alexmodguy.alexscaves.fabric.forge.event.entity.living.LivingDamageEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * The two damage-pipeline game-bus events, posted from four mixin sites that share these two lines.
 *
 * <p>They live here rather than as {@code @Unique} helpers because Forge splits each event across
 * <b>two</b> patch sites — {@code LivingEntity} and {@code Player} — and a mixin class can name only
 * one target, so the bodies would otherwise be written twice and drift apart.
 *
 * <h2>Why {@code Player} needs its own copy of both</h2>
 *
 * {@code Player} overrides {@code hurt}/{@code hurtServer} <i>and</i> {@code actuallyHurt}, and its
 * {@code actuallyHurt} <b>never calls super</b> (disassembled on 1.20.1 and 26.2 — the override is a
 * complete reimplementation that adds the exhaustion and absorption statistics). So a hook on
 * {@code LivingEntity} alone is silently dead for every player, which is exactly the half of the
 * damage pipeline this mod cares most about: the rainbounce boots' fall-damage refusal and the
 * extinction spear's block are both player-only.
 *
 * <p>{@code hurt} is the mirror image — {@code Player#hurt} <i>does</i> call super, so the event would
 * fire <b>twice</b> for a player if both sites posted unconditionally. Forge solves it with a guard
 * inside the hook itself ({@code onLivingAttack} returns early for a {@code Player}, and
 * {@code Player#hurt}'s own {@code onPlayerAttack} does the posting), which is what
 * {@link #postAttack} reproduces via its {@code fromPlayerOverride} flag. The reason Forge bothers
 * rather than simply letting the super call carry it: {@code Player#hurt} returns early — for
 * invulnerability, for creative-mode {@code abilities.invulnerable}, for an already-dead player —
 * <i>before</i> reaching super, so a {@code LivingEntity}-only hook would miss those attempts
 * entirely.
 *
 * <p>⚠️ Both descriptors split at <b>1.21.2</b>, where the whole damage path gained a leading
 * {@code ServerLevel}: {@code hurt} → {@code hurtServer} and {@code actuallyHurt} grew a parameter.
 * All four call sites therefore carry the same two-arm gate.
 */
public final class ACDamageEvents {

    private ACDamageEvents() {
    }

    /**
     * Posts {@link LivingAttackEvent} and returns whether the attack was refused.
     *
     * @param fromPlayerOverride {@code true} at the {@code Player#hurt} site, {@code false} at the
     *                           {@code LivingEntity#hurt} one. A player reaches both, so exactly one
     *                           of them must decline to post; Forge picks the player's, because it is
     *                           the one that sees every attempt.
     */
    public static boolean postAttack(LivingEntity entity, DamageSource source, float amount, boolean fromPlayerOverride) {
        if (fromPlayerOverride != entity instanceof net.minecraft.world.entity.player.Player) {
            return false;
        }
        return MinecraftForge.EVENT_BUS.post(new LivingAttackEvent(entity, source, amount));
    }

    /**
     * Posts {@link LivingDamageEvent} and returns whether the damage was refused.
     *
     * <p>⚠️ <b>One deliberate divergence from Forge, and the reason it is safe is a property of this
     * mod's listeners rather than of the event.</b> Forge posts this <i>after</i> armour, enchantment
     * and absorption have been applied, so its {@code getAmount()} is the number that is about to be
     * subtracted from the health bar; this posts at the head of {@code actuallyHurt}, where the amount
     * is still the raw incoming one. That is invisible here because the only listener,
     * {@code CommonEvents#livingHurt}, is a pure predicate over the entity and the damage <i>source</i>
     * — it never reads the amount, and it only ever cancels. Anchoring after mitigation instead would
     * mean tracking a call whose surrounding arithmetic is reshaped several times across the range for
     * no observable gain.
     *
     * <p>⚠️ <b>If a listener is ever added that reads or modifies the amount, this anchor is wrong</b>
     * and has to move to the pre-{@code setHealth} position Forge uses. Do not quietly extend the
     * listener instead.
     *
     * <p>Forge's hook also sits <i>inside</i> {@code actuallyHurt}'s {@code isInvulnerableTo} guard
     * while this sits above it. Not observable either: {@code actuallyHurt} is only ever reached from
     * {@code hurt}/{@code hurtServer}, both of which already returned early for an invulnerable
     * entity, so the inner test is defensive redundancy that no live call can fail.
     */
    public static boolean postDamage(LivingEntity entity, DamageSource source, float amount) {
        return MinecraftForge.EVENT_BUS.post(new LivingDamageEvent(entity, source, amount));
    }
}
