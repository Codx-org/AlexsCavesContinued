package com.github.alexmodguy.alexscaves.server.potion;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

public class RageEffect extends ACMobEffect {

    // The identity of the modifier this effect adds and removes. 1.21 swapped the UUID for a
    // ResourceLocation, and AttributeInstance#getModifier/#removeModifier moved with it, so the
    // constant changes type and its three uses below need no edit.
    //? if >=1.21 {
    /*private static final net.minecraft.resources.ResourceLocation RAGE_ATTACK_DAMAGE_UUID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("alexscaves", "effect.rage");
    *///?} else {
    private static final UUID RAGE_ATTACK_DAMAGE_UUID = UUID.fromString("1eaf83ff-7207-4596-b37a-d7a07b3ec4ff");
    //?}

    protected RageEffect() {
        super(MobEffectCategory.NEUTRAL, 0XBA2E2E);
    }

    public void tick(LivingEntity entity, int level) {
        AttributeInstance attributeinstance = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attributeinstance != null) {
            float levelScale = (1 + level) * 2.5F;
            float f = (1F - (entity.getHealth() / entity.getMaxHealth())) * levelScale;
            removeRageModifier(entity);
            //? if >=1.21 {
            /*attributeinstance.addTransientModifier(new AttributeModifier(RAGE_ATTACK_DAMAGE_UUID, (double) f, AttributeModifier.Operation.ADDITION));
            *///?} else {
            attributeinstance.addTransientModifier(new AttributeModifier(RAGE_ATTACK_DAMAGE_UUID, "Rage attack boost", (double) f, AttributeModifier.Operation.ADDITION));
            //?}
        }
        if (!entity.level().isClientSide() && entity instanceof Mob mob && mob.getTarget() == null && entity.tickCount % 10 == 0 && entity.getRandom().nextInt(2) == 0) {
            AABB aabb = mob.getBoundingBox().inflate(80);
            LivingEntity randomTarget = null;
            for (LivingEntity living : mob.level().getEntitiesOfClass(LivingEntity.class, aabb, EntitySelector.LIVING_ENTITY_STILL_ALIVE)) {
                if ((randomTarget == null || randomTarget.distanceTo(mob) > living.distanceTo(mob) && mob.getRandom().nextInt(2) == 0) && !mob.is(living)) {
                    if (!mob.isAlliedTo(living) && !living.isAlliedTo(mob) && mob.canAttack(living)) {
                        randomTarget = living;
                    }
                }
            }
            if (randomTarget != null && !randomTarget.is(mob)) {
                mob.setLastHurtByMob(randomTarget);
                mob.setTarget(randomTarget);
                for (int i = 0; i < 3 + mob.getRandom().nextInt(3); i++) {
                    ((ServerLevel) entity.level()).sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getRandomX(0.5F), mob.getEyeY() + mob.getRandom().nextFloat() * 0.2F, mob.getRandomZ(0.5F), 0, 0, 0, 0, 1.0D);
                }
            }
        }
    }

    @Override
    public boolean shouldTick(int duration, int amplifier) {
        return duration > 0;
    }

    protected void removeRageModifier(LivingEntity living) {
        AttributeInstance attributeinstance = living.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attributeinstance != null) {
            if (attributeinstance.getModifier(RAGE_ATTACK_DAMAGE_UUID) != null) {
                attributeinstance.removeModifier(RAGE_ATTACK_DAMAGE_UUID);
            }

        }
    }

    @Override
    public void onEffectEnd(LivingEntity entity) {
        removeRageModifier(entity);
    }

}