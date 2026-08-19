package com.github.alexmodguy.alexscaves.server.potion;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.message.UpdateEffectVisualityEntityMessage;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class MagnetizedEffect extends ACMobEffect {

    protected MagnetizedEffect() {
        super(MobEffectCategory.NEUTRAL, 0X53556C);
    }

    public void tick(LivingEntity entity, int tick) {
        if (!entity.level().isClientSide() && entity.tickCount % 20 == 0) {
            MobEffectInstance instance = entity.getEffect(ACCompat.effect(this));
            if (instance != null) {
                AlexsCaves.sendMSGToAll(new UpdateEffectVisualityEntityMessage(entity.getId(), entity.getId(), 2, instance.getDuration()));
            }
        }
    }

    @Override
    public boolean shouldTick(int duration, int amplifier) {
        return duration > 0;
    }

    @Override
    public void onEffectStart(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide()) {
            MobEffectInstance instance = entity.getEffect(ACCompat.effect(this));
            if (instance != null) {
                AlexsCaves.sendMSGToAll(new UpdateEffectVisualityEntityMessage(entity.getId(), entity.getId(), 2, instance.getDuration()));
            }
        }
    }

}
