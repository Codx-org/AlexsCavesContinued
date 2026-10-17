package com.github.alexmodguy.alexscaves.citadel.server.tick;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.citadel.server.tick.modifier.TickRateModifier;
import com.github.alexmodguy.alexscaves.citadel.server.tick.modifier.TickRateModifierType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public abstract class TickRateTracker {

    public List<TickRateModifier> tickRateModifierList = new ArrayList<>();
    public List<Entity> specialTickRateEntities = new ArrayList<>();

    private long masterTickCount;

    public void masterTick() {
        masterTickCount++;
        specialTickRateEntities.forEach(this::tickBlockedEntity);
        specialTickRateEntities.removeIf(this::hasNormalTickRate);
        for (TickRateModifier modifier : tickRateModifierList) {
            modifier.masterTick();
        }
        if (!tickRateModifierList.isEmpty()) {
            if (tickRateModifierList.removeIf(TickRateModifier::doRemove)) {
                sync();
            }
        }
    }

    protected void sync() {
    }

    public boolean hasModifiersActive() {
        return !tickRateModifierList.isEmpty();
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (TickRateModifier modifier : tickRateModifierList) {
            if (!modifier.doRemove()) {
                list.add(modifier.toTag());
            }
        }
        tag.put("TickRateModifiers", list);
        return tag;
    }

    public void fromTag(CompoundTag tag) {
        if (tag.contains("TickRateModifiers")) {
            ListTag list = ACCompat.getList(tag, "TickRateModifiers", 10);
            for (int i = 0; i < list.size(); ++i) {
                CompoundTag tag1 = ACCompat.getCompound(list, i);
                TickRateModifier modifier = TickRateModifier.fromTag(tag1);
                if (!modifier.doRemove()) {
                    tickRateModifierList.add(modifier);
                }
            }
        }
    }

    /**
     * The product of every active {@link TickRateModifierType#CELESTIAL} modifier — how much faster
     * (or slower) the sky should move than the world around it. {@link #getDayTimeIncrement} applies
     * this to a whole-tick counter and so has to fake fractions with a modulo; MC 26 replaced the
     * day-time counter with a {@code WorldClock} that already carries a float rate and a partial-tick
     * accumulator, so from there the multiplier is used directly (see {@code ServerClockInstanceMixin}).
     */
    public float getDayTimeRateMultiplier() {
        float f = 1F;
        for (TickRateModifier modifier : tickRateModifierList) {
            if (modifier.getType() == TickRateModifierType.CELESTIAL) {
                f *= modifier.getTickRateMultiplier();
            }
        }
        return f;
    }

    public long getDayTimeIncrement(long timeIn) {
        float f = getDayTimeRateMultiplier();
        if (f < 1F && f > 0F) {
            int inverse = (int) (1 / f);
            return masterTickCount % inverse == 0 ? timeIn : 0;
        }
        return (long) (timeIn * f);
    }

    public float getEntityTickLengthModifier(Entity entity) {
        float f = 1.0F;
        for (TickRateModifier modifier : tickRateModifierList) {
            if (modifier.getType().isLocal() && modifier.appliesTo(entity.level(), entity.getX(), entity.getY(), entity.getZ())) {
                f *= modifier.getTickRateMultiplier();
            }
        }
        return f;
    }

    public boolean hasNormalTickRate(Entity entity) {
        return getEntityTickLengthModifier(entity) == 1.0F;
    }

    public boolean isTickingHandled(Entity entity) {
        return specialTickRateEntities.contains(entity);
    }

    public void addTickBlockedEntity(Entity entity) {
        if (!isTickingHandled(entity)) {
            specialTickRateEntities.add(entity);
        }
    }

    protected void tickBlockedEntity(Entity entity) {
        tickEntityAtCustomRate(entity);
    }


    protected abstract void tickEntityAtCustomRate(Entity entity);

}
