package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class SharpenedCandyCaneItem extends Item implements ACDynamicAttributeItem {

    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    public SharpenedCandyCaneItem(Properties properties) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", 3F, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", 4F, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    // 1.21.5 made Item#hurtEnemy void; see SpearItem for the full note. The body keeps its
    // boolean shape so only this two-line bridge has to be gated.
    //? if >=1.21.5 {
    /*@Override
    public void hurtEnemy(ItemStack stack, LivingEntity hurtEntity, LivingEntity player) {
        acHurtEnemy(stack, hurtEntity, player);
    }
    *///?} else {
    public boolean hurtEnemy(ItemStack stack, LivingEntity hurtEntity, LivingEntity player) {
        return acHurtEnemy(stack, hurtEntity, player);
    }
    //?}

    private boolean acHurtEnemy(ItemStack stack, LivingEntity hurtEntity, LivingEntity player) {
        if (player instanceof Player player1 && !player1.isCreative()) {
            stack.shrink(1);
            player1.playSound(ACCompat.rawSound(SoundEvents.ITEM_BREAK));
        }
        ACCompat.knockback(hurtEntity, 0.15F, hurtEntity.getX() - player.getX(), hurtEntity.getZ() - player.getZ());
        if(!hurtEntity.level().isClientSide() && hurtEntity.level() instanceof ServerLevel serverLevel){
            ItemParticleOption itemParticleOption = ACCompat.itemParticle(ParticleTypes.ITEM, new ItemStack(ACItemRegistry.SHARPENED_CANDY_CANE.get()));
            Vec3 hurtCenter = hurtEntity.position().add(0, hurtEntity.getBbHeight() * 0.5F, 0);
            Vec3 playerCenter = player.position().add(0, player.getBbHeight() * 0.5F, 0);
            Vec3 particlePos = playerCenter.subtract(hurtCenter).normalize().scale(hurtEntity.getBbWidth() * 0.6F).add(hurtCenter);
            serverLevel.sendParticles(itemParticleOption, particlePos.x, particlePos.y, particlePos.z, 15, 0.3D, hurtEntity.getRandom().nextFloat() * 0.1F - 0.05F, 0.2F, hurtEntity.getRandom().nextFloat() * 0.1F - 0.05F);
        }
        return true;
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

}
