package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public class ModFishBucketItem extends MobBucketItem {

    /**
     * The bucketed entity type.
     *
     * <p>Kept alongside the one {@code MobBucketItem} stores because NeoForge made its copy private
     * in 1.20.5 — {@code getFishType()} is gone there — and because holding the supplier keeps the
     * lookup lazy for the arms that can afford it.
     */
    private final Supplier<? extends EntityType<?>> acFishType;

    // NeoForge dropped Forge's deferred-supplier MobBucketItem constructor in 1.20.5 (and its
    // Supplier<SoundEvent> parameter with it), and vanilla — so every Fabric node — never had
    // it at all, so the three values are resolved here. ENTITY_TYPE
    // and FLUID both precede ITEM in BuiltInRegistries, which is the order the registration events
    // fire in, so they exist by the time this item is built.
    //? if fabric || (neoforge && >=1.20.5) {
    /*@SuppressWarnings("unchecked")
    public ModFishBucketItem(Supplier<? extends EntityType<?>> fishTypeIn, Supplier<? extends Fluid> fluid, Item.Properties builder) {
        super((EntityType<? extends net.minecraft.world.entity.Mob>) fishTypeIn.get(), fluid.get(), SoundEvents.BUCKET_EMPTY_FISH, builder.stacksTo(1));
        this.acFishType = fishTypeIn;
    }
    *///?} else {
    // Forge 1.21.4 tightened the deferred constructor's bound from EntityType<?> to
    // EntityType<? extends Mob>. The double cast satisfies that without a gate, since the narrower
    // supplier is still accepted by the older, wider parameter.
    @SuppressWarnings("unchecked")
    public ModFishBucketItem(Supplier<? extends EntityType<?>> fishTypeIn, Supplier<? extends Fluid> fluid, Item.Properties builder) {
        super((Supplier<? extends EntityType<? extends net.minecraft.world.entity.Mob>>) (Supplier<?>) fishTypeIn, fluid, () -> SoundEvents.BUCKET_EMPTY_FISH, builder.stacksTo(1));
        this.acFishType = fishTypeIn;
    }
    //?}

    /**
     * Deliberately empty: it overrides {@code MobBucketItem}'s tropical-fish variant tooltip, which
     * says nothing useful about a radgill.
     */
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
    }

    // 1.21.5 widened the placer from Player to LivingEntity — anything that can empty a bucket. The
    // body only passes it on to gameEvent, which takes an Entity either way.
    @Override
    //? if >=1.21.5
    /*public void checkExtraContent(@Nullable net.minecraft.world.entity.LivingEntity player, Level level, ItemStack stack, BlockPos pos) {*/
    //? if <1.21.5
    public void checkExtraContent(@Nullable Player player, Level level, ItemStack stack, BlockPos pos) {
        if (level instanceof ServerLevel) {
            this.spawnFish((ServerLevel) level, stack, pos);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        }
    }

    private void spawnFish(ServerLevel serverLevel, ItemStack stack, BlockPos pos) {
        Entity entity = acFishType.get().spawn(serverLevel, stack, (Player) null, pos, MobSpawnType.BUCKET, true, false);
        if (entity instanceof Bucketable) {
            Bucketable bucketable = (Bucketable) entity;
            bucketable.loadFromBucketTag(ACCompat.getOrCreateTag(stack));
            bucketable.setFromBucket(true);
        }
        addExtraAttributes(entity, stack);
    }

    protected void addExtraAttributes(Entity entity, ItemStack stack) {

    }


}
