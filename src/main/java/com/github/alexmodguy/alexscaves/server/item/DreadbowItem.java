package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.particle.ACParticleRegistry;
import com.github.alexmodguy.alexscaves.server.enchantment.ACEnchantmentRegistry;
import com.github.alexmodguy.alexscaves.server.entity.item.DarkArrowEntity;
import com.github.alexmodguy.alexscaves.server.message.UpdateItemTagMessage;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import com.github.alexmodguy.alexscaves.server.potion.DarknessIncarnateEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
//? if <1.21.2
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public class DreadbowItem extends ProjectileWeaponItem implements UpdatesStackTags, ACClientExtensionItem, ACEnchantableItem, ACTickingItem {

    public DreadbowItem() {
        super(new Item.Properties().rarity(ACItemRegistry.RARITY_DEMONIC).durability(500));
    }

    @Nullable
    public static EntityType getTypeOfArrow(ItemStack itemStackIn) {
        if(ACCompat.getTag(itemStackIn) != null && ACCompat.getTag(itemStackIn).contains("LastUsedArrowType")) {
            String str = ACCompat.getString(ACCompat.getTag(itemStackIn), "LastUsedArrowType");
            return BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(str));
        }
        return null;
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions) AlexsCaves.PROXY.getISTERProperties());
    }

    // 1.21.2 merged the result-plus-stack pair back into a plain InteractionResult.
    //? if >=1.21.2
    /*public net.minecraft.world.InteractionResult use(Level level, Player player, InteractionHand interactionHand) {*/
    //? if <1.21.2
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
        ItemStack itemstack = player.getItemInHand(interactionHand);
        ItemStack ammo = player.getProjectile(itemstack);
        boolean flag = player.isCreative();
        if(flag || !ammo.isEmpty()){
            AbstractArrow lastArrow = createArrow(player, itemstack, ItemStack.EMPTY);
            EntityType lastArrowType = lastArrow == null ? EntityType.ARROW : lastArrow.getType();
            CompoundTag writeBackTag = ACCompat.getOrCreateTag(itemstack);
            writeBackTag.putString("LastUsedArrowType", BuiltInRegistries.ENTITY_TYPE.getKey(lastArrowType).toString());
            ACCompat.setTag(itemstack, writeBackTag);
            player.startUsingItem(interactionHand);
            return ACCompat.useConsume(itemstack);
        }else{
            return ACCompat.useFail(itemstack);
        }
    }

    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    public void acInventoryTick(ItemStack stack, Level level, Entity entity, boolean held) {
        boolean using = entity instanceof LivingEntity living && living.getUseItem().equals(stack);
        int useTime = getUseTime(stack);
        if (level.isClientSide()) {
            CompoundTag tag = ACCompat.getOrCreateTag(stack);
            if (ACCompat.getInt(tag, "PrevUseTime") != ACCompat.getInt(tag, "UseTime")) {
                tag.putInt("PrevUseTime", getUseTime(stack));
            }
            ACCompat.setTag(stack, tag);

            if (using && getPerfectShotTicks(stack) > 0) {
                setPerfectShotTicks(stack, getPerfectShotTicks(stack) - 1);
                AlexsCaves.sendMSGToServer(new UpdateItemTagMessage(entity.getId(), stack));
            }
            boolean relentless = ACCompat.enchantLevel(stack, ACEnchantmentRegistry.RELENTLESS_DARKNESS) > 0;
            int twilightPerfection = ACCompat.enchantLevel(stack, ACEnchantmentRegistry.TWILIGHT_PERFECTION);
            int maxLoadTime = getMaxLoadTime(stack);
            if (using && useTime < maxLoadTime) {
                int set = useTime + (relentless ? 3 : 1);
                setUseTime(stack, set);
                if(twilightPerfection > 0){
                    if(set >= maxLoadTime && useTime <= maxLoadTime){
                        setPerfectShotTicks(stack, 4 + (twilightPerfection - 1) * 3);
                        AlexsCaves.sendMSGToServer(new UpdateItemTagMessage(entity.getId(), stack));
                    }else{
                        setPerfectShotTicks(stack, 0);
                        AlexsCaves.sendMSGToServer(new UpdateItemTagMessage(entity.getId(), stack));
                    }
                }
            }
            if(relentless){
                if (using && useTime >= maxLoadTime) {
                    setUseTime(stack, 0);
                }
            }
            if (!using && useTime > 0.0F) {
                setUseTime(stack, Math.max(0, useTime - 5));
                setPerfectShotTicks(stack, 0);
            }
            if(using){
                Vec3 particlePos = entity.position().add((level.getRandom().nextFloat() - 0.5F) * 2.5F, 0F, (level.getRandom().nextFloat() - 0.5F) * 2.5F);
                level.addParticle(ACParticleRegistry.UNDERZEALOT_MAGIC.get(), particlePos.x, particlePos.y, particlePos.z, entity.getX(), entity.getY(0.5F), entity.getZ());
            }
        }
    }

    private static int getMaxLoadTime(ItemStack stack) {
        if(ACCompat.enchantLevel(stack, ACEnchantmentRegistry.RELENTLESS_DARKNESS) > 0){
            return 5;
        }else{
            return 40 - 8 * ACCompat.enchantLevel(stack, ACEnchantmentRegistry.DARK_NOCK);
        }
    }

    public static int getUseTime(ItemStack stack) {
        CompoundTag compoundtag = ACCompat.getTag(stack);
        return compoundtag != null ? ACCompat.getInt(compoundtag, "UseTime") : 0;
    }

    public static void setUseTime(ItemStack stack, int useTime) {
        CompoundTag tag = ACCompat.getOrCreateTag(stack);
        tag.putInt("PrevUseTime", getUseTime(stack));
        tag.putInt("UseTime", useTime);
        ACCompat.setTag(stack, tag);
    }
    public static int getPerfectShotTicks(ItemStack stack) {
        CompoundTag compoundtag = ACCompat.getTag(stack);
        return compoundtag != null ? ACCompat.getInt(compoundtag, "PerfectShotTicks") : 0;
    }

    public static void setPerfectShotTicks(ItemStack stack, int ticks) {
        CompoundTag tag = ACCompat.getOrCreateTag(stack);
        tag.putInt("PerfectShotTicks", ticks);
        ACCompat.setTag(stack, tag);
    }

    public static float getLerpedUseTime(ItemStack stack, float f) {
        CompoundTag compoundtag = ACCompat.getTag(stack);
        float prev = compoundtag != null ? (float) ACCompat.getInt(compoundtag, "PrevUseTime") : 0F;
        float current = compoundtag != null ? (float) ACCompat.getInt(compoundtag, "UseTime") : 0F;
        return prev + f * (current - prev);
    }

    public static float getPullingAmount(ItemStack itemStack, float partialTicks){
        return Math.min(getLerpedUseTime(itemStack, partialTicks) / (float) getMaxLoadTime(itemStack), 1F);
    }


    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    public static float getPowerForTime(int i, ItemStack itemStack) {
        float f = (float) i / (float)getMaxLoadTime(itemStack);
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.getCount() == 1;
    }

    public void releaseUsing(ItemStack itemStack, Level level, LivingEntity livingEntity, int i1) {
        if (livingEntity instanceof Player player && ACCompat.enchantLevel(itemStack, ACEnchantmentRegistry.RELENTLESS_DARKNESS) <= 0) {
            int i = this.getUseDuration(itemStack) - i1;
            float f = getPowerForTime(i, itemStack);
            boolean precise = ACCompat.enchantLevel(itemStack, ACEnchantmentRegistry.PRECISE_VOLLEY) > 0;
            boolean respite = ACCompat.enchantLevel(itemStack, ACEnchantmentRegistry.SHADED_RESPITE) > 0 && !DarknessIncarnateEffect.isInLight(player, 11);
            boolean perfectShot = ACCompat.enchantLevel(itemStack, ACEnchantmentRegistry.TWILIGHT_PERFECTION) > 0 && getPerfectShotTicks(itemStack) > 0;
            if (f > 0.1D) {
                player.playSound(ACSoundRegistry.DREADBOW_RELEASE.get());
                ItemStack ammoStack = player.getProjectile(itemStack);
                if(respite && ammoStack.isEmpty()){
                    ammoStack = new ItemStack(Items.ARROW);
                }
                AbstractArrow abstractArrow = createArrow(player, itemStack, ammoStack);
                if(abstractArrow != null){
                    float maxDist = 128 * f;
                    HitResult realHitResult = ProjectileUtil.getHitResultOnViewVector(player, Entity::canBeHitByProjectile, maxDist);
                    if(realHitResult.getType() == HitResult.Type.MISS){
                        realHitResult = ProjectileUtil.getHitResultOnViewVector(player, Entity::canBeHitByProjectile, f * 42);
                    }
                    BlockPos mutableSkyPos = new BlockPos.MutableBlockPos(realHitResult.getLocation().x, realHitResult.getLocation().y + 1.5, realHitResult.getLocation().z);
                    int maxFallHeight = 15;
                    int k = 0;
                    while(mutableSkyPos.getY() < level.getMaxBuildHeight() && level.isEmptyBlock(mutableSkyPos) && k < maxFallHeight){
                        mutableSkyPos = mutableSkyPos.above();
                        k++;
                    }
                    boolean darkArrows = isConvertibleArrow(abstractArrow);
                    int maxArrows = darkArrows ? 30 : 8;
                    abstractArrow.pickup = AbstractArrow.Pickup.ALLOWED;
                    for(int j = 0; j < Math.ceil(maxArrows * f); j++){
                        if(darkArrows){
                            DarkArrowEntity darkArrowEntity = new DarkArrowEntity(level, livingEntity);
                            darkArrowEntity.setShadowArrowDamage(precise ? 2.0F : 3.0F);
                            darkArrowEntity.setPerfectShot(perfectShot);
                            abstractArrow = darkArrowEntity;
                        }else if(perfectShot){
                            // getBaseDamage went away in 1.21.5 — see AbstractArrowAccessor.
                            abstractArrow.setBaseDamage(((com.github.alexmodguy.alexscaves.mixin.AbstractArrowAccessor) abstractArrow).ac$getBaseDamage() * 2.0F);
                        }
                        Vec3 vec3 = Vec3.atCenterOf(mutableSkyPos).add(level.getRandom().nextFloat() * 16 - 8, level.getRandom().nextFloat() * 4 - 2, level.getRandom().nextFloat() * 16 - 8);
                        int clearTries = 0;
                        while (clearTries < 6 && !level.isEmptyBlock(BlockPos.containing(vec3)) && level.getFluidState(BlockPos.containing(vec3)).isEmpty()){
                            clearTries++;
                            vec3 = Vec3.atCenterOf(mutableSkyPos).add(level.getRandom().nextFloat() * 16 - 8, level.getRandom().nextFloat() * 4 - 2, level.getRandom().nextFloat() * 16 - 8);
                        }
                        if(!level.isEmptyBlock(BlockPos.containing(vec3)) && level.getFluidState(BlockPos.containing(vec3)).isEmpty()){
                            vec3 = Vec3.atCenterOf(mutableSkyPos);
                        }
                        abstractArrow.setPos(vec3);
                        Vec3 vec31 = realHitResult.getLocation().subtract(vec3);
                        float randomness = precise ? 0.0F : (darkArrows ? 20F : 5F) + level.getRandom().nextFloat() * 10F;
                        if(!precise && level.getRandom().nextFloat() < 0.25F){
                            randomness = level.getRandom().nextFloat();
                        }
                        abstractArrow.shoot(vec31.x, vec31.y, vec31.z, 0.5F + 1.5F * level.getRandom().nextFloat(),  randomness);
                        level.addFreshEntity(abstractArrow);
                        abstractArrow = createArrow(player, itemStack, ammoStack);
                        abstractArrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                    }
                    if(darkArrows){
                        Vec3 vec3 = realHitResult.getLocation();
                        level.playSound((Player)null, vec3.x, vec3.y, vec3.z, ACSoundRegistry.DREADBOW_RAIN.get(), SoundSource.PLAYERS, 12.0F, 1.0F);
                    }
                    if(!player.isCreative()){
                        if(!respite){
                            ACCompat.hurtAndBreakUsedHand(itemStack, 1, player);
                        }
                        if(!respite || !ammoStack.is(Items.ARROW)){
                            ammoStack.shrink(1);
                        }
                    }
                }
            }
        }
        //? if >=1.21.2
        /*return false;*/
    }


    public void onUseTick(Level level, LivingEntity living, ItemStack itemStack, int timeUsing) {
        super.onUseTick(level, living, itemStack, timeUsing);
        if(living instanceof Player player && ACCompat.enchantLevel(itemStack, ACEnchantmentRegistry.RELENTLESS_DARKNESS) > 0 && timeUsing % 3 == 0){
            boolean respite = ACCompat.enchantLevel(itemStack, ACEnchantmentRegistry.SHADED_RESPITE) > 0 && !DarknessIncarnateEffect.isInLight(living, 11);
            player.playSound(ACSoundRegistry.DREADBOW_RELEASE.get());
            ItemStack ammoStack = player.getProjectile(itemStack);
            if(respite && ammoStack.isEmpty()){
                ammoStack = new ItemStack(Items.ARROW);
            }
            AbstractArrow abstractArrow = createArrow(player, itemStack, ammoStack);
            boolean darkArrows = isConvertibleArrow(abstractArrow);
            int maxArrows = darkArrows ? 1 + living.getRandom().nextInt(2) : 1;
            float randomness = 0.5F;
            for(int i = 0; i < maxArrows; i++){
                abstractArrow.pickup = AbstractArrow.Pickup.ALLOWED;
                if(darkArrows){
                    DarkArrowEntity darkArrowEntity = new DarkArrowEntity(level, living);
                    darkArrowEntity.setShadowArrowDamage(2.0F);
                    abstractArrow = darkArrowEntity;
                }
                abstractArrow.setPos(abstractArrow.position().add(level.getRandom().nextFloat() - 0.5F, level.getRandom().nextFloat() - 0.5F, level.getRandom().nextFloat() - 0.5F));
                Vec3 vec3 = player.getViewVector(1.0F);
                abstractArrow.shoot(vec3.x, vec3.y, vec3.z, 4F + 3F * level.getRandom().nextFloat(),  randomness);
                randomness += 2.0F;
                level.addFreshEntity(abstractArrow);
                abstractArrow = createArrow(player, itemStack, ammoStack);
                abstractArrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            if(!player.isCreative()){
                if(!respite){
                    ACCompat.hurtAndBreakUsedHand(itemStack, 1, player);
                }
                if(!respite || !ammoStack.is(Items.ARROW)){
                    ammoStack.shrink(1);
                }
            }
        }
    }

    private AbstractArrow createArrow(Player player, ItemStack bowStack, ItemStack ammoIn) {
        ItemStack ammo = ammoIn.isEmpty() ? player.getProjectile(bowStack) : ammoIn;
        ArrowItem arrowitem = (ArrowItem)(ammo.getItem() instanceof ArrowItem ? ammo.getItem() : Items.ARROW);
        // 1.21 hands createArrow the weapon as well, so an arrow can read the bow's components
        // (piercing, the new enchantment effects). The bow here is the stack the shot came from.
        //? if >=1.21
        /*AbstractArrow abstractArrow =  arrowitem.createArrow(player.level(), ammo, player, bowStack);*/
        //? if <1.21
        AbstractArrow abstractArrow =  arrowitem.createArrow(player.level(), ammo, player);
        return abstractArrow;
    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return !oldStack.is(ACItemRegistry.DREADBOW.get()) || !newStack.is(ACItemRegistry.DREADBOW.get());
    }
    public static boolean isConvertibleArrow(Entity arrowEntity){
        return arrowEntity instanceof Arrow arrow && arrow.getColor() == -1;
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return ARROW_ONLY;
    }

    @Override
    public int getDefaultProjectileRange() {
        return 64;
    }

    // 1.20.5 pulled the "aim and launch one projectile" step out of BowItem and up into
    // ProjectileWeaponItem as an abstract, so that crossbows, bows and anything else share the
    // multishot/inaccuracy loop. The dreadbow does its own firing in use(), and this override is
    // only reached through that shared loop, so vanilla's bow behaviour is the right answer.
    //? if >=1.20.5 {
    /*@Override
    protected void shootProjectile(LivingEntity shooter, net.minecraft.world.entity.projectile.Projectile projectile, int index, float velocity, float inaccuracy, float angle, @Nullable LivingEntity target) {
        projectile.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot() + angle, 0.0F, velocity, inaccuracy);
    }
    *///?}
}
