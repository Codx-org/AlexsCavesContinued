package com.github.alexmodguy.alexscaves.server.potion;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class DeepsightEffect extends ACMobEffect {

    private int lastDuration = -1;
    private int firstDuration = -1;

    protected DeepsightEffect() {
        super(MobEffectCategory.BENEFICIAL, 0X002972);
    }

    public int getActiveTime() {
        return firstDuration - lastDuration;
    }

    @Override
    public boolean shouldTick(int duration, int amplifier) {
        lastDuration = duration;
        if (duration <= 0) {
            lastDuration = -1;
            firstDuration = -1;
        }
        if (firstDuration == -1) {
            firstDuration = duration;
        }
        return duration > 0;
    }

    @Override
    public void onEffectEnd(LivingEntity entity) {
        lastDuration = -1;
        firstDuration = -1;
    }

    @Override
    public void onEffectStart(LivingEntity entity, int amplifier) {
        lastDuration = -1;
        firstDuration = -1;
    }


    public static float getIntensity(Player player, float partialTicks) {
        MobEffectInstance instance = player.getEffect(ACCompat.effect(ACEffectRegistry.DEEPSIGHT.get()));
        if (instance == null) {
            return 0.0F;
        } else if(instance.isInfiniteDuration()) {
            return 1.0F;
        } else {
            DeepsightEffect deepsightEffect = (DeepsightEffect) ACCompat.rawEffect(instance);
            float j = deepsightEffect.getActiveTime() + partialTicks;
            int duration = instance.getDuration();
            return Math.min(20, (Math.min(j, duration + partialTicks))) * 0.05F;
        }
    }
}
