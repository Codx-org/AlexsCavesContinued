package com.github.alexmodguy.alexscaves.citadel.server.tick.modifier;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public abstract class TickRateModifier {

    private final TickRateModifierType type;
    private float maxDuration;
    private float duration;
    private float tickRateMultiplier;

    public TickRateModifier(TickRateModifierType type, int maxDuration, float tickRateMultiplier) {
        this.type = type;
        this.maxDuration = maxDuration;
        this.tickRateMultiplier = tickRateMultiplier;
    }

    public TickRateModifier(CompoundTag tag) {
        this.type = TickRateModifierType.fromId(ACCompat.getInt(tag, "TickRateType"));
        this.maxDuration = ACCompat.getFloat(tag, "MaxDuration");
        this.duration = ACCompat.getFloat(tag, "Duration");
        this.tickRateMultiplier = ACCompat.getFloat(tag, "SpeedMultiplier");
    }

    public TickRateModifierType getType() {
        return type;
    }

    public float getMaxDuration() {
        return maxDuration;
    }

    public float getTickRateMultiplier() {
        return tickRateMultiplier;
    }

    public void setMaxDuration(float maxDuration) {
        this.maxDuration = maxDuration;
    }

    public void setTickRateMultiplier(float tickRateMultiplier) {
        this.tickRateMultiplier = tickRateMultiplier;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("TickRateType", this.type.toId());
        tag.putFloat("MaxDuration", maxDuration);
        tag.putFloat("Duration", duration);
        tag.putFloat("SpeedMultiplier", tickRateMultiplier);
        return tag;
    }

    public static TickRateModifier fromTag(CompoundTag tag) {
        TickRateModifierType typeFromNbt = TickRateModifierType.fromId(ACCompat.getInt(tag, "TickRateType"));
        try {
            return typeFromNbt.getTickRateClass().getConstructor(CompoundTag.class).newInstance(tag);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isGlobal() {
        return this.type.isLocal();
    }

    public void masterTick() {
        duration++;
    }


    public boolean doRemove() {
        float f = tickRateMultiplier == 0 || this.getType() == TickRateModifierType.CELESTIAL ? 1.0F : 1F / tickRateMultiplier;
        return duration >= maxDuration * f;
    }

    public abstract boolean appliesTo(Level level, double x, double y, double z);
}
