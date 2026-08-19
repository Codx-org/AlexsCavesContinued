package com.github.alexmodguy.alexscaves.server.potion;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.citadel.server.tick.ServerTickRateTracker;
import com.github.alexmodguy.alexscaves.citadel.server.tick.modifier.LocalEntityTickRateModifier;
import com.github.alexmodguy.alexscaves.citadel.server.tick.modifier.TickRateModifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class SugarRushEffect extends ACMobEffect {


    protected SugarRushEffect() {
        super(MobEffectCategory.BENEFICIAL, 0XFFA4EB);
        // See StunnedEffect: the modifier's identity is a ResourceLocation from 1.21.
        //? if >=1.21 {
        /*this.addAttributeModifier(Attributes.MOVEMENT_SPEED, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("alexscaves", "effect.sugar_rush"), 0.7F, AttributeModifier.Operation.MULTIPLY_TOTAL);
        *///?} else {
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, "91AEAA56-376B-4498-935B-2F7F68020638", 0.7F, AttributeModifier.Operation.MULTIPLY_TOTAL);
        //?}
    }

    @Override
    public boolean shouldTick(int duration, int amplifier) {
        return duration > 0;
    }

    public void tick(LivingEntity entity, int amplifier) {
        if(entity.level().isClientSide()){
            AlexsCaves.PROXY.playWorldSound(entity, (byte)18);
        }
    }

    public static void enterSlowMotion(Player entity, Level level, int duration, float speed) {
        if (!level.isClientSide() && level instanceof ServerLevel) {
            ServerTickRateTracker tracker = ServerTickRateTracker.getForServer(level.getServer());
            for (TickRateModifier modifier : tracker.tickRateModifierList) {
                if (modifier instanceof LocalEntityTickRateModifier entityTick && entityTick.getEntityId() == entity.getId()) {
                    modifier.setMaxDuration(duration);
                    return;
                }
            }
            tracker.addTickRateModifier(new LocalEntityTickRateModifier(entity.getId(), entity.getType(), 10, level.dimension(), duration, speed));
        }
    }

    public static void leaveSlowMotion(Player entity, Level level) {
        if (!level.isClientSide() && level instanceof ServerLevel) {
            ServerTickRateTracker tracker = ServerTickRateTracker.getForServer(level.getServer());
            TickRateModifier toRemove = null;
            for (TickRateModifier modifier : tracker.tickRateModifierList) {
                if (modifier instanceof LocalEntityTickRateModifier entityTick && entityTick.getEntityId() == entity.getId()) {
                    toRemove = modifier;
                }
            }
            if (toRemove != null) {
                tracker.tickRateModifierList.remove(toRemove);
            }
        }
    }

}
