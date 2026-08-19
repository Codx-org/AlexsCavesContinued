package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
//? if <1.21.2
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import com.github.alexmodguy.alexscaves.server.item.ACClientExtensionItem;

public class SpearItem extends Item implements ACClientExtensionItem, ACDynamicAttributeItem {

    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    public SpearItem(Properties properties, double damage) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", damage, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", (double) -3F, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    // 1.21.5 made Item#hurtEnemy void — the "did the hit connect" half of its old return value is
    // postHurtEnemy now (see DesolateDaggerItem, the one place that read it). Nothing here reads it,
    // so the body stays one boolean method that the two spear subclasses chain onto with super, and
    // only the two-line bridge is gated — once, here in the base class.
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

    protected boolean acHurtEnemy(ItemStack stack, LivingEntity hurtEntity, LivingEntity player) {
        ACCompat.hurtAndBreak(stack, 1, player, EquipmentSlot.MAINHAND);
        return true;
    }

    public boolean mineBlock(ItemStack itemStack, Level level, BlockState state, BlockPos blockPos, LivingEntity livingEntity) {
        if ((double) state.getDestroySpeed(level, blockPos) != 0.0D) {
            ACCompat.hurtAndBreak(itemStack, 2, livingEntity, EquipmentSlot.MAINHAND);
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

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions) AlexsCaves.PROXY.getISTERProperties());
    }

    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    // 1.21.2 merged the result-plus-stack pair back into a plain InteractionResult.
    //? if >=1.21.2
    /*public net.minecraft.world.InteractionResult use(Level level, Player player, InteractionHand interactionHand) {*/
    //? if <1.21.2
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
        ItemStack itemstack = player.getItemInHand(interactionHand);
        if(!itemstack.isDamageableItem() || itemstack.getDamageValue() < itemstack.getMaxDamage() - 1){
            player.startUsingItem(interactionHand);
            return ACCompat.useConsume(itemstack);
        }
        return ACCompat.usePass(itemstack);
    }

    public float getPowerForTime(int i) {
        float f = (float) i / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }
}
