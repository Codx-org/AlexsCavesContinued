package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.particle.ACParticleRegistry;
import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import com.github.alexmodguy.alexscaves.server.enchantment.ACEnchantmentRegistry;
import com.github.alexmodguy.alexscaves.server.entity.living.TremorzillaEntity;
import com.github.alexmodguy.alexscaves.server.message.UpdateEffectVisualityEntityMessage;
import com.github.alexmodguy.alexscaves.server.message.UpdateItemTagMessage;
import com.github.alexmodguy.alexscaves.server.misc.ACDamageTypes;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import com.github.alexmodguy.alexscaves.server.potion.IrradiatedEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
//? if <1.21.2
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;
import com.github.alexmodguy.alexscaves.server.item.ACClientExtensionItem;

public class RaygunItem extends Item implements UpdatesStackTags, AlwaysCombinableOnAnvil, ACClientExtensionItem, ACEnchantableItem, ACTickingItem {

    private static final int MAX_CHARGE = 1000;

    public static final Predicate<ItemStack> AMMO = (stack) -> {
        return stack.getItem() == ACBlockRegistry.URANIUM_ROD.get().asItem();
    };

    public RaygunItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions) AlexsCaves.PROXY.getISTERProperties());
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.getCount() == 1;
    }

    public static boolean hasCharge(ItemStack stack) {
        return getCharge(stack) < MAX_CHARGE;
    }

    // 1.21.2 merged the result-plus-stack pair back into a plain InteractionResult.
    //? if >=1.21.2
    /*public net.minecraft.world.InteractionResult use(Level level, Player player, InteractionHand interactionHand) {*/
    //? if <1.21.2
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
        ItemStack itemstack = player.getItemInHand(interactionHand);
        if (hasCharge(itemstack)) {
            player.startUsingItem(interactionHand);
            player.playSound(ACSoundRegistry.RAYGUN_START.get());
            return ACCompat.useConsume(itemstack);
        } else {
            ItemStack ammo = findAmmo(player);
            boolean flag = player.isCreative();
            if (!ammo.isEmpty()) {
                ammo.shrink(1);
                flag = true;
            }
            if (flag) {
                setCharge(itemstack, 0);
                player.level().playSound((Player) null, player.getX(), player.getY(), player.getZ(), ACSoundRegistry.RAYGUN_RELOAD.get(), player.getSoundSource(), 1.0F, 1.0F);
            } else {
                player.level().playSound((Player) null, player.getX(), player.getY(), player.getZ(), ACSoundRegistry.RAYGUN_EMPTY.get(), player.getSoundSource(), 1.0F, 1.0F);
            }
            return ACCompat.useFail(itemstack);
        }
    }

    private ItemStack findAmmo(Player entity) {
        if (entity.isCreative()) {
            return ItemStack.EMPTY;
        }
        for (int i = 0; i < entity.getInventory().getContainerSize(); ++i) {
            ItemStack itemstack1 = entity.getInventory().getItem(i);
            if (AMMO.test(itemstack1)) {
                return itemstack1;
            }
        }
        return ItemStack.EMPTY;
    }

    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    public void acInventoryTick(ItemStack stack, Level level, Entity entity, boolean held) {
        boolean using = entity instanceof LivingEntity living && living.getUseItem().equals(stack);
        int useTime = getUseTime(stack);
        if(!level.isClientSide()){
            if (ACCompat.enchantLevel(stack, ACEnchantmentRegistry.SOLAR) > 0 && !using) {
                int charge = getCharge(stack);
                if (charge > 0 && level.getRandom().nextFloat() < 0.02F) {
                    BlockPos playerPos = entity.blockPosition().above();
                    if (level.canSeeSky(playerPos) && level.isDay() && !level.dimensionType().hasFixedTime() && ACCompat.sunAboveHorizon(level, playerPos)) {
                        setCharge(stack, charge - 1);
                        setUseTime(stack, 0);
                    }
                }
            }
        }else{
            CompoundTag tag = ACCompat.getOrCreateTag(stack);
            if (ACCompat.getInt(tag, "PrevUseTime") != ACCompat.getInt(tag, "UseTime")) {
                tag.putInt("PrevUseTime", getUseTime(stack));
            }
            ACCompat.setTag(stack, tag);
            if (using && useTime < 5.0F) {
                setUseTime(stack, useTime + 1);
            }
            if (!using && useTime > 0.0F) {
                setUseTime(stack, useTime - 1);
            }
        }
    }

    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int timeUsing) {
        int i = getUseDuration(stack) - timeUsing;
        int realStart = 15;
        float time = i < realStart ? i / (float) realStart : 1F;
        float maxDist = 25.0F * time;
        boolean xRay = ACCompat.enchantLevel(stack, ACEnchantmentRegistry.X_RAY) > 0;
        HitResult realHitResult = ProjectileUtil.getHitResultOnViewVector(living, Entity::canBeHitByProjectile, maxDist);
        HitResult blockOnlyHitResult = living.pick(maxDist, 0.0F, false);
        Vec3 xRayVec = living.getViewVector(0.0F).scale(maxDist).add(living.getEyePosition());
        Vec3 vec3 = xRay ? xRayVec : blockOnlyHitResult.getLocation();
        Vec3 vec31 = xRay ? xRayVec : blockOnlyHitResult.getLocation();
        if (!hasCharge(stack)) {
            if (level.isClientSide()) {
                AlexsCaves.sendMSGToServer(new UpdateItemTagMessage(living.getId(), stack));
            }
            living.stopUsingItem();
            level.playSound((Player) null, living.getX(), living.getY(), living.getZ(), ACSoundRegistry.RAYGUN_EMPTY.get(), living.getSoundSource(), 1.0F, 1.0F);
            return;
        }
        if (level.isClientSide()) {
            setRayPosition(stack, vec3.x, vec3.y, vec3.z);
            AlexsCaves.PROXY.playWorldSound(living, (byte) 8);
            int efficency = ACCompat.enchantLevel(stack, ACEnchantmentRegistry.ENERGY_EFFICIENCY);
            int divis = 2 + (int) Math.floor(efficency * 1.5F);
            if (time >= 1F && i % divis == 0 && (!(living instanceof Player) || !((Player) living).isCreative())) {
                int charge = getCharge(stack);
                setCharge(stack, Math.min(charge + 1, MAX_CHARGE));
            }
        }

        float deltaX = 0;
        float deltaY = 0;
        float deltaZ = 0;
        boolean gamma = ACCompat.enchantLevel(stack, ACEnchantmentRegistry.GAMMA_RAY) > 0;
        ParticleOptions particleOptions;
        if (level.getRandom().nextBoolean() && time >= 1F) {
            particleOptions = gamma ? ACParticleRegistry.BLUE_RAYGUN_EXPLOSION.get() : ACParticleRegistry.RAYGUN_EXPLOSION.get();
        } else {
            particleOptions = gamma ? ACParticleRegistry.BLUE_HAZMAT_BREATHE.get() : ACParticleRegistry.HAZMAT_BREATHE.get();
            deltaX = (level.getRandom().nextFloat() - 0.5F) * 0.2F;
            deltaY = (level.getRandom().nextFloat() - 0.5F) * 0.2F;
            deltaZ = (level.getRandom().nextFloat() - 0.5F) * 0.2F;
        }
        level.addParticle(particleOptions, vec3.x + (level.getRandom().nextFloat() - 0.5F) * 0.45F, vec3.y + 0.2F, vec3.z + (level.getRandom().nextFloat() - 0.5F) * 0.45F, deltaX, deltaY, deltaZ);
        Direction blastHitDirection = null;
        Vec3 blastHitPos = null;
        if(xRay){
            AABB maxAABB = living.getBoundingBox().inflate(maxDist);
            float fakeRayTraceProgress = 1.0F;
            Vec3 startClip = living.getEyePosition();
            while(fakeRayTraceProgress < maxDist){
                startClip = startClip.add(living.getViewVector(1.0F));
                Vec3 endClip = startClip.add(living.getViewVector(1.0F));
                HitResult attemptedHitResult = ProjectileUtil.getEntityHitResult(level, living, startClip, endClip, maxAABB, Entity::canBeHitByProjectile);
                if(attemptedHitResult != null){
                    realHitResult = attemptedHitResult;
                    break;
                }
                fakeRayTraceProgress++;
            }
        }else{
            if (realHitResult instanceof BlockHitResult blockHitResult) {
                BlockPos pos = blockHitResult.getBlockPos();
                BlockState state = level.getBlockState(pos);
                blastHitDirection = blockHitResult.getDirection();
                if (!state.isAir() && state.isFaceSturdy(level, pos, blastHitDirection)) {
                    blastHitPos = realHitResult.getLocation();
                }
            }
        }
        if (realHitResult instanceof EntityHitResult entityHitResult) {
            blastHitPos = entityHitResult.getEntity().position();
            blastHitDirection = Direction.UP;
            vec31 = blastHitPos;
        }
        if (blastHitPos != null && i % 2 == 0) {
            float offset = 0.05F + level.getRandom().nextFloat() * 0.09F;
            Vec3 particleVec = blastHitPos.add(offset * blastHitDirection.getStepX(), offset * blastHitDirection.getStepY(), offset * blastHitDirection.getStepZ());
            level.addParticle(ACParticleRegistry.RAYGUN_BLAST.get(), particleVec.x, particleVec.y, particleVec.z, blastHitDirection.get3DDataValue(), 0, 0);
        }
        if (!level.isClientSide() && (i - realStart) % 3 == 0) {
            AABB hitBox = new AABB(vec31.add(-1, -1, -1), vec31.add(1, 1, 1));
            int radiationLevel = gamma ? IrradiatedEffect.BLUE_LEVEL : 0;
            for (Entity entity : level.getEntities(living, hitBox, Entity::canBeHitByProjectile)) {
                if (!entity.is(living) && !entity.isAlliedTo(living) && !living.isAlliedTo(entity) && !living.isPassengerOfSameVehicle(entity)) {
                    boolean flag = entity instanceof TremorzillaEntity || ACCompat.hurt(entity, ACDamageTypes.causeRaygunDamage(level.registryAccess(), living), gamma ? 2F : 1.5F);
                    if (flag && entity instanceof LivingEntity livingEntity && !livingEntity.getType().builtInRegistryHolder().is(ACTagRegistry.RESISTS_RADIATION)) {
                        if (livingEntity.addEffect(new MobEffectInstance(ACCompat.effect(ACEffectRegistry.IRRADIATED.get()), 800, radiationLevel))) {
                            AlexsCaves.sendMSGToAll(new UpdateEffectVisualityEntityMessage(entity.getId(), living.getId(), gamma ? 4 : 0, 800));
                        }
                    }
                }
            }
        }
    }

    public static void setUseTime(ItemStack stack, int useTime) {
        CompoundTag tag = ACCompat.getOrCreateTag(stack);
        tag.putInt("PrevUseTime", getUseTime(stack));
        tag.putInt("UseTime", useTime);
        ACCompat.setTag(stack, tag);
    }

    public static void setRayPosition(ItemStack stack, double x, double y, double z) {
        CompoundTag tag = ACCompat.getOrCreateTag(stack);
        Vec3 prev = getRayPosition(stack);
        tag.putDouble("PrevRayX", prev.x);
        tag.putDouble("PrevRayY", prev.y);
        tag.putDouble("PrevRayZ", prev.z);
        tag.putDouble("RayX", x);
        tag.putDouble("RayY", y);
        tag.putDouble("RayZ", z);
        ACCompat.setTag(stack, tag);
    }

    public static int getUseTime(ItemStack stack) {
        CompoundTag compoundtag = ACCompat.getTag(stack);
        return compoundtag != null ? ACCompat.getInt(compoundtag, "UseTime") : 0;
    }

    public static int getCharge(ItemStack stack) {
        CompoundTag compoundtag = ACCompat.getTag(stack);
        return compoundtag != null ? ACCompat.getInt(compoundtag, "ChargeUsed") : 0;
    }

    public static void setCharge(ItemStack stack, int charge) {
        CompoundTag compoundtag = ACCompat.getOrCreateTag(stack);
        compoundtag.putInt("ChargeUsed", charge);
        ACCompat.setTag(stack, compoundtag);
    }

    public static Vec3 getRayPosition(ItemStack stack) {
        CompoundTag compoundtag = ACCompat.getTag(stack);
        if (compoundtag != null && compoundtag.contains("RayX")) {
            return new Vec3(ACCompat.getDouble(compoundtag, "RayX"), ACCompat.getDouble(compoundtag, "RayY"), ACCompat.getDouble(compoundtag, "RayZ"));
        } else {
            return Vec3.ZERO;
        }
    }

    public static float getLerpedUseTime(ItemStack stack, float f) {
        CompoundTag compoundtag = ACCompat.getTag(stack);
        float prev = compoundtag != null ? (float) ACCompat.getInt(compoundtag, "PrevUseTime") : 0F;
        float current = compoundtag != null ? (float) ACCompat.getInt(compoundtag, "UseTime") : 0F;
        return prev + f * (current - prev);
    }

    @Nullable
    public static Vec3 getLerpedRayPosition(ItemStack stack, float f) {
        CompoundTag compoundtag = ACCompat.getTag(stack);
        if (compoundtag != null) {
            double prevX = (float) ACCompat.getDouble(compoundtag, "PrevRayX");
            double x = (float) ACCompat.getDouble(compoundtag, "RayX");
            double prevY = (float) ACCompat.getDouble(compoundtag, "PrevRayY");
            double y = (float) ACCompat.getDouble(compoundtag, "RayY");
            double prevZ = (float) ACCompat.getDouble(compoundtag, "PrevRayZ");
            double z = (float) ACCompat.getDouble(compoundtag, "RayZ");
            return new Vec3(prevX + f * (x - prevX), prevY + f * (y - prevY), prevZ + f * (z - prevZ));
        } else {
            return null;
        }
    }

    public void releaseUsing(ItemStack stack, Level level, LivingEntity player, int useTimeLeft) {
        super.releaseUsing(stack, level, player, useTimeLeft);
        if (level.isClientSide()) {
            AlexsCaves.sendMSGToServer(new UpdateItemTagMessage(player.getId(), stack));
        }
        AlexsCaves.PROXY.clearSoundCacheFor(player);
        //? if >=1.21.2
        /*return false;*/
    }

    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) != 0;
    }

    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F - (float) getCharge(stack) * 13.0F / (float) MAX_CHARGE);
    }

    public int getBarColor(ItemStack stack) {
        float pulseRate = (float) getCharge(stack) / (float) MAX_CHARGE * 2.0F;
        float f = AlexsCaves.PROXY.getPlayerTime() + AlexsCaves.PROXY.getPartialTicks();
        float f1 = 0.5F * (float) (1.0F + Math.sin(f * pulseRate));
        return Mth.hsvToRgb(0.3F, f1 * 0.6F + 0.2F, 1.0F);
    }


    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        if (getCharge(stack) != 0) {
            String chargeLeft = "" + (int) (MAX_CHARGE - getCharge(stack));
            tooltip.add(Component.translatable("item.alexscaves.raygun.charge", chargeLeft, MAX_CHARGE).withStyle(ChatFormatting.GREEN));
        }
    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return !oldStack.is(ACItemRegistry.RAYGUN.get()) || !newStack.is(ACItemRegistry.RAYGUN.get());
    }
}
