package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.enchantment.ACEnchantmentRegistry;
import com.github.alexmodguy.alexscaves.server.entity.item.WaveEntity;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
//? if <1.21.2
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
//? if <1.20.5
import net.minecraft.world.item.Vanishable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

// 1.20.5 deleted the Vanishable marker interface: "does the curse of vanishing apply" is now a
// property of the stack's components rather than of the item's type, and nothing ever asked this
// mod's items the question directly.
public class OrtholanceItem extends Item implements ACClientExtensionItem, ACEnchantableItem, ACDynamicAttributeItem
        //? if <1.20.5
        , Vanishable
{
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    public OrtholanceItem(Item.Properties properties) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", 5.0D, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", (double) -2.4F, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    public UseAnim getUseAnimation(ItemStack p_43417_) {
        return UseAnim.BOW;
    }

    /**
     * The modifiers this item contributes in {@code slot}, or {@code null} to defer to the superclass.
     *
     * <p>1.20.5 replaced the per-slot {@code Multimap} with the {@code ItemAttributeModifiers} data
     * component, so the hook below has two shapes; keeping the decision itself in one un-gated
     * method means only the two-line bridge is duplicated.
     *
     * <p>The hook overridden is the loader's stack-aware {@code getAttributeModifiers} rather than
     * vanilla's {@code getDefaultAttributeModifiers} even where the answer does not depend on the
     * stack: pre-1.20.5 both Forge's patch and Fabric API's {@code FabricItem} default delegate to the
     * vanilla one, so all three are equivalent, and using one hook everywhere keeps the gate identical
     * in all seven items.
     */
    @Override
    public Multimap<Attribute, AttributeModifier> acModifiers(EquipmentSlot slot, ItemStack stack) {
        return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : null;
    }

    // Forge deleted this hook in 1.21.2 and left nothing behind it; from there the answer is fed in
    // by ItemStackAttributeModifiersMixin instead. See ACDynamicAttributeItem.
    //? if forge && >=1.20.5 && <1.21.2 {
    /*@Override
    public net.minecraft.world.item.component.ItemAttributeModifiers getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return ACCompat.itemAttributes(acModifiers(slot, stack), slot, () -> super.getAttributeModifiers(slot, stack));
    }
    *///?}

    // NeoForge's IItemExtension has no per-slot hook from 1.20.5 — an item states its whole
    // attribute set at once — so the same acModifiers is handed over and ACCompat asks it slot by
    // slot. See ACCompat#itemAttributes(BiFunction, ItemStack, Supplier).
    //? if neoforge && >=1.20.5 {
    /*@Override
    public net.minecraft.world.item.component.ItemAttributeModifiers getAttributeModifiers(ItemStack stack) {
        return ACCompat.itemAttributes(this::acModifiers, stack, () -> super.getAttributeModifiers(stack));
    }
    *///?}

    // Fabric API supplies exactly the same stack-aware hook, under the same name, with its two
    // arguments the other way round — and mixes it into ItemStack#getAttributeModifiers in place of
    // vanilla's getDefaultAttributeModifiers(slot) call, which is precisely what the Forge patch
    // does. So this is the direct counterpart of the arm below rather than an approximation. The
    // interface carrying it, FabricItem, is injected into Item by the loader, so nothing here
    // declares or imports it.
    //? if fabric && <1.20.5 {
    /*@Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(ItemStack stack, EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> mine = acModifiers(slot, stack);
        return mine == null ? super.getAttributeModifiers(stack, slot) : mine;
    }
    *///?}

    //? if !fabric && <1.20.5 {
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> mine = acModifiers(slot, stack);
        return mine == null ? super.getAttributeModifiers(slot, stack) : mine;
    }
    //?}

    public int getEnchantmentValue() {
        return 1;
    }

    public int getUseDuration(ItemStack itemStack) {
        return 72000;
    }

    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int useTime) {
        int i = Mth.clamp(this.getUseDuration(stack) - useTime, 0, 60);
        int flinging = ACCompat.enchantLevel(stack, ACEnchantmentRegistry.FLINGING);
        boolean tsunami = ACCompat.enchantLevel(stack, ACEnchantmentRegistry.TSUNAMI) > 0;
        if (i > 0) {
            float f = 0.1F * i + flinging * 0.1F;
            Vec3 vec3 = livingEntity.getDeltaMovement().add(livingEntity.getViewVector(1.0F).normalize().multiply(f, f * 0.15F, f));
            if (i >= 10 && !level.isClientSide()) {
                level.playSound(null, livingEntity, ACSoundRegistry.ORTHOLANCE_WAVE.get(), SoundSource.NEUTRAL, 4.0F, 1.0F);
                ACCompat.hurtAndBreakUsedHand(stack, 1, livingEntity);
                int maxWaves = i / 5;
                if(tsunami){
                    maxWaves = 5;
                    Vec3 waveCenterPos = livingEntity.position().add(vec3);
                    WaveEntity tsunamiWaveEntity = new WaveEntity(level, livingEntity);
                    tsunamiWaveEntity.setPos(waveCenterPos.x, livingEntity.getY(), waveCenterPos.z);
                    tsunamiWaveEntity.setLifespan(20);
                    tsunamiWaveEntity.setWaveScale(5.0F);
                    tsunamiWaveEntity.setWaitingTicks(2);
                    tsunamiWaveEntity.setYRot(-(float) (Mth.atan2(vec3.x, vec3.z) * (double) (180F / (float) Math.PI)));
                    level.addFreshEntity(tsunamiWaveEntity);
                }else{
                    for (int wave = 0; wave < maxWaves; wave++) {
                        float f1 = (float) wave / maxWaves;
                        int lifespan = 3 + (int) ((1F - f1) * 3);
                        Vec3 waveCenterPos = livingEntity.position().add(vec3.scale(f1 * 2));
                        WaveEntity leftWaveEntity = new WaveEntity(level, livingEntity);
                        leftWaveEntity.setPos(waveCenterPos.x, livingEntity.getY(), waveCenterPos.z);
                        leftWaveEntity.setLifespan(lifespan);
                        leftWaveEntity.setYRot(-(float) (Mth.atan2(vec3.x, vec3.z) * (double) (180F / (float) Math.PI)) + 60 - 15 * wave);
                        level.addFreshEntity(leftWaveEntity);
                        WaveEntity rightWaveEntity = new WaveEntity(level, livingEntity);
                        rightWaveEntity.setPos(waveCenterPos.x, livingEntity.getY(), waveCenterPos.z);
                        rightWaveEntity.setLifespan(lifespan);
                        rightWaveEntity.setYRot(-(float) (Mth.atan2(vec3.x, vec3.z) * (double) (180F / (float) Math.PI)) - 60 + 15 * wave);
                        level.addFreshEntity(rightWaveEntity);
                    }
                    if(ACCompat.enchantLevel(stack, ACEnchantmentRegistry.SECOND_WAVE) > 0){
                        int maxSecondWaves = Math.max(1, maxWaves - 1);
                        for (int wave = 0; wave < maxSecondWaves; wave++) {
                            float f1 = (float) wave / maxSecondWaves;
                            int lifespan = 3 + (int) ((1F - f1) * 3);
                            Vec3 waveCenterPos = livingEntity.position().add(vec3.scale(f1 * 2));
                            WaveEntity leftWaveEntity = new WaveEntity(level, livingEntity);
                            leftWaveEntity.setPos(waveCenterPos.x, livingEntity.getY(), waveCenterPos.z);
                            leftWaveEntity.setLifespan(lifespan);
                            leftWaveEntity.setYRot(-(float) (Mth.atan2(vec3.x, vec3.z) * (double) (180F / (float) Math.PI)) + 60 - 15 * wave);
                            leftWaveEntity.setWaitingTicks(8);
                            level.addFreshEntity(leftWaveEntity);
                            WaveEntity rightWaveEntity = new WaveEntity(level, livingEntity);
                            rightWaveEntity.setPos(waveCenterPos.x, livingEntity.getY(), waveCenterPos.z);
                            rightWaveEntity.setLifespan(lifespan);
                            rightWaveEntity.setYRot(-(float) (Mth.atan2(vec3.x, vec3.z) * (double) (180F / (float) Math.PI)) - 60 + 15 * wave);
                            rightWaveEntity.setWaitingTicks(8);
                            level.addFreshEntity(rightWaveEntity);
                        }
                    }
                }
                AABB aabb = new AABB(livingEntity.position(), livingEntity.position().add(vec3.scale(maxWaves))).inflate(1);
                DamageSource source = livingEntity.damageSources().mobAttack(livingEntity);
                double d = ACCompat.attackDamageBonus(stack);
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, aabb)) {
                    if (!livingEntity.isAlliedTo(entity) && !livingEntity.equals(entity) && livingEntity.hasLineOfSight(entity)) {
                        entity.hurt(source, (float) d);
                        entity.stopRiding();
                    }
                }
            }
            livingEntity.setDeltaMovement(vec3.add(0, (livingEntity.onGround() ? 0.2F : 0) + (flinging * 0.1F), 0));
        }
        //? if >=1.21.2
        /*return false;*/
    }

    // 1.21.2 merged the result-plus-stack pair back into a plain InteractionResult.
    //? if >=1.21.2
    /*public net.minecraft.world.InteractionResult use(Level level, Player player, InteractionHand interactionHand) {*/
    //? if <1.21.2
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
        ItemStack itemstack = player.getItemInHand(interactionHand);
        if (itemstack.getDamageValue() >= itemstack.getMaxDamage() - 1) {
            return ACCompat.useFail(itemstack);
        } else {
            player.startUsingItem(interactionHand);
            return ACCompat.useConsume(itemstack);
        }
    }

    // 1.21.5 made Item#hurtEnemy void; see SpearItem for the full note. The body keeps its
    // boolean shape so only this two-line bridge has to be gated.
    //? if >=1.21.5 {
    /*@Override
    public void hurtEnemy(ItemStack stack, LivingEntity hurt, LivingEntity player) {
        acHurtEnemy(stack, hurt, player);
    }
    *///?} else {
    public boolean hurtEnemy(ItemStack stack, LivingEntity hurt, LivingEntity player) {
        return acHurtEnemy(stack, hurt, player);
    }
    //?}

    private boolean acHurtEnemy(ItemStack stack, LivingEntity hurt, LivingEntity player) {
        ACCompat.hurtAndBreak(stack, 1, player, EquipmentSlot.MAINHAND);
        Vec3 vec3 = player.getViewVector(1.0F);
        if(ACCompat.enchantLevel(stack, ACEnchantmentRegistry.SEA_SWING) > 0){
            WaveEntity waveEntity = new WaveEntity(hurt.level(), player);
            waveEntity.setPos(player.getX(), hurt.getY(), player.getZ());
            waveEntity.setLifespan(5);
            waveEntity.setYRot(-(float) (Mth.atan2(vec3.x, vec3.z) * (double) (180F / (float) Math.PI)));
            player.level().addFreshEntity(waveEntity);
        }
        return true;
    }

    public boolean mineBlock(ItemStack itemStack, Level level, BlockState state, BlockPos blockPos, LivingEntity livingEntity) {
        if ((double) state.getDestroySpeed(level, blockPos) != 0.0D) {
            ACCompat.hurtAndBreak(itemStack, 2, livingEntity, EquipmentSlot.MAINHAND);
        }

        return true;
    }


    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions) AlexsCaves.PROXY.getISTERProperties());
    }
}
