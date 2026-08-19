package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.server.entity.living.DinosaurEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.RelicheirusEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.TremorsaurusEntity;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
//? if <1.21
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

// See AlexMealItem: BowlFoodItem is gone from 1.21, and the bowl leftover moved onto the food.
//? if >=1.21 {
/*public class PrehistoricMixtureItem extends net.minecraft.world.item.Item {
*///?} else {
public class PrehistoricMixtureItem extends BowlFoodItem {
//?}

    public PrehistoricMixtureItem(Properties properties) {
        super(properties);
    }

    public InteractionResult interactLivingEntity(ItemStack itemStack, Player player, LivingEntity livingEntity, InteractionHand hand) {
        FoodProperties foodProperties = ACCompat.food(itemStack, livingEntity);
        if (!livingEntity.level().isClientSide() && livingEntity instanceof Mob && canFeedMob(player, (Mob) livingEntity) && foodProperties != null) {
            livingEntity.heal(ACCompat.nutrition(foodProperties));
            if (!(livingEntity instanceof DinosaurEntity dinosaur && dinosaur.onFeedMixture(itemStack, player))) {
                for (MobEffectInstance mobEffectInstance : ACCompat.foodEffects(itemStack, livingEntity)) {
                    livingEntity.addEffect(mobEffectInstance);
                }
                if (this == ACItemRegistry.SERENE_SALAD.get()) {
                    livingEntity.removeEffect(ACCompat.effect(ACEffectRegistry.STUNNED.get()));
                }
            }
            for (int i = 0; i < 4 + livingEntity.getRandom().nextInt(3); i++) {
                ((ServerLevel) livingEntity.level()).sendParticles(ACCompat.itemParticle(ParticleTypes.ITEM, itemStack), livingEntity.getRandomX(0.8F), livingEntity.getRandomY(), livingEntity.getRandomZ(0.8F), 0, 0, 0, 0, 0);
            }
            if (!player.isCreative()) {
                itemStack.shrink(1);
                if (!player.addItem(new ItemStack(Items.BOWL))) {
                    player.drop(new ItemStack(Items.BOWL), true);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.interactLivingEntity(itemStack, player, livingEntity, hand);
    }

    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (this == ACItemRegistry.SERENE_SALAD.get()) {
            livingEntity.removeEffect(ACCompat.effect(ACEffectRegistry.STUNNED.get()));
        }
        return super.finishUsingItem(stack, level, livingEntity);
    }

    private boolean canFeedMob(Player player, Mob mob) {
        if (mob instanceof TremorsaurusEntity && mob.hasEffect(ACCompat.effect(ACEffectRegistry.STUNNED.get())) && this == ACItemRegistry.SERENE_SALAD.get()) {
            return true;
        }
        if (mob instanceof RelicheirusEntity relicheirus && relicheirus.getPushingTreesFor() > 0 && this == ACItemRegistry.PRIMORDIAL_SOUP.get()) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        return target == null || !target.is(player);
    }
}
