package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.enchantment.ACEnchantmentRegistry;
import com.github.alexmodguy.alexscaves.server.entity.item.WaterBoltEntity;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
//? if <1.21.2
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import com.github.alexmodguy.alexscaves.server.item.ACClientExtensionItem;

public class SeaStaffItem extends Item implements ACClientExtensionItem, ACEnchantableItem, ACTickingItem {
    public SeaStaffItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions) AlexsCaves.PROXY.getISTERProperties());
    }

    // 1.21.2 merged the result-plus-stack pair back into a plain InteractionResult.
    //? if >=1.21.2
    /*public net.minecraft.world.InteractionResult use(Level level, Player player, InteractionHand hand) {*/
    //? if <1.21.2
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        level.playSound((Player) null, player.getX(), player.getY(), player.getZ(), ACSoundRegistry.SEA_STAFF_CAST.get(), SoundSource.PLAYERS, 0.5F, (level.getRandom().nextFloat() * 0.45F + 0.75F));
        player.swing(hand);
        float seekAmount = ACCompat.enchantLevel(itemstack, ACEnchantmentRegistry.SOAK_SEEKING);
        if (!level.isClientSide()) {
            double dist = 128;
            Entity closestValid = getClosestLookingAtEntityFor(level, player, dist);
            int bolts = ACCompat.enchantLevel(itemstack, ACEnchantmentRegistry.TRIPLE_SPLASH) > 0 ? 3 : 1;
            for(int i = 0; i < bolts; i++){
                float shootRot = i == 0 ? 0 : i == 1 ? -50 : 50;
                WaterBoltEntity bolt = new WaterBoltEntity(level, player);
                float rot = player.yHeadRot + (hand == InteractionHand.MAIN_HAND ? 45 : -45);
                bolt.setPos(player.getX() - (double) (player.getBbWidth()) * 1.1F * (double) Mth.sin(rot * ((float) Math.PI / 180F)), player.getEyeY() - (double) 0.4F, player.getZ() + (double) (player.getBbWidth()) * 1.1F * (double) Mth.cos(rot * ((float) Math.PI / 180F)));
                bolt.shootFromRotation(player, player.getXRot(), player.getYRot() + shootRot, -20.0F, i > 0 ? 1F : 2F, 12F);
                if (ACCompat.enchantLevel(itemstack, ACEnchantmentRegistry.ENVELOPING_BUBBLE) > 0) {
                    bolt.setBubbling(player.getRandom().nextBoolean());
                }
                if (ACCompat.enchantLevel(itemstack, ACEnchantmentRegistry.BOUNCING_BOLT) > 0) {
                    bolt.ricochet = true;
                }
                bolt.seekAmount = 0.3F + seekAmount * 0.2F;
                if (closestValid != null) {
                    bolt.setArcingTowards(closestValid.getUUID());
                }
                level.addFreshEntity(bolt);
            }

        }
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            ACCompat.hurtAndBreak(itemstack, 1, player, hand);
        }
        return ACCompat.useSidedSuccess(itemstack, level.isClientSide());
    }

    public static Entity getClosestLookingAtEntityFor(Level level, Player player, double dist) {
        Entity closestValid = null;
        Vec3 playerEyes = player.getEyePosition(1.0F);
        HitResult hitresult = level.clip(new ClipContext(playerEyes, playerEyes.add(player.getLookAngle().scale(dist)), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
        if (hitresult instanceof EntityHitResult) {
            Entity entity = ((EntityHitResult) hitresult).getEntity();
            if (!entity.equals(player) && !player.isAlliedTo(entity) && !entity.isAlliedTo(player) && entity instanceof Mob && player.hasLineOfSight(entity)) {
                closestValid = entity;
            }
        } else {
            Vec3 at = hitresult.getLocation();
            AABB around = new AABB(at.add(-0.5F, -0.5F, -0.5F), at.add(0.5F, 0.5F, 0.5F)).inflate(15);
            for (Entity entity : level.getEntitiesOfClass(LivingEntity.class, around.inflate(dist))) {
                if (!entity.equals(player) && !player.isAlliedTo(entity) && !entity.isAlliedTo(player) && entity instanceof Mob && player.hasLineOfSight(entity)) {
                    if (closestValid == null || entity.distanceToSqr(at) < closestValid.distanceToSqr(at)) {
                        closestValid = entity;
                    }
                }
            }
        }
        return closestValid;
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.getCount() == 1;
    }

    public void acInventoryTick(ItemStack stack, Level level, Entity entity, boolean held) {
        boolean using = entity instanceof LivingEntity living && living.getUseItem().equals(stack);
        if (!level.isClientSide()) {
            if (ACCompat.enchantLevel(stack, ACEnchantmentRegistry.SEAPAIRING) > 0 && !using) {
                if (level.getRandom().nextFloat() < 0.02F) {
                    if (entity.isInWaterRainOrBubble()) {
                        stack.setDamageValue(Math.min(0, stack.getDamageValue() - 1));
                    }
                }
            }
        }
    }

}
