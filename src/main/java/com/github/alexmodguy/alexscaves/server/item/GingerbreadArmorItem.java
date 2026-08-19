package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
//? if <1.21.5
import net.minecraft.world.item.ArmorItem;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// 1.21.5 deleted net.minecraft.world.item.ArmorItem — an armour piece is a plain Item whose
// Properties carry humanoidArmor(material, ArmorType). See ACArmorMaterial#properties.
//? if >=1.21.5 {
/*public class GingerbreadArmorItem extends net.minecraft.world.item.Item implements CustomArmorPostRender, ACClientExtensionItem, ACDynamicAttributeItem {
*///?} else {
public class GingerbreadArmorItem extends ArmorItem implements CustomArmorPostRender, ACClientExtensionItem, ACDynamicAttributeItem {
//?}

    private static final double MIN_SPEED_BOOST = 0.1D;
    private static final double MAX_SPEED_BOOST = 1.0D;
    private static final UUID[] ARMOR_MODIFIERS = new UUID[]{UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B77"), UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E12"), UUID.fromString("9F3D476D-C118-4544-8365-64846904B43F"), UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB111")};
    private Map<Integer, Multimap<Attribute, AttributeModifier>> gingerbreadDurabilityDependentAttributes = new HashMap<>();
    private final Multimap<Attribute, AttributeModifier> defaultAttributes;
    // 1.21.2 deleted ArmorItem's protected `type` field along with getDefense(); both are kept here
    // so the durability-scaled rebuild below reads the same numbers on every version.
    private final ArmorItem.Type acSlot;
    private final int acDefense;

    public GingerbreadArmorItem(ACArmorMaterial armorMaterial, ArmorItem.Type slot) {
        super(armorMaterial.vanilla(), slot, armorMaterial.properties(new Properties(), slot));
        this.acSlot = slot;
        this.acDefense = armorMaterial.defense(slot);
        UUID uuid = ARMOR_MODIFIERS[slot.ordinal()];
        String modifierId = "gingerbread_armor." + slot.getName();
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ARMOR, ACCompat.attributeModifier(uuid, "Armor modifier", modifierId, this.acDefense, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.MOVEMENT_SPEED, ACCompat.attributeModifier(uuid, "Movement speed", modifierId, MIN_SPEED_BOOST, AttributeModifier.Operation.MULTIPLY_BASE));
        defaultAttributes = builder.build();
    }

    private Multimap<Attribute, AttributeModifier> getOrCreateDurabilityAttributes(int durabilityIn, int maxDurability) {
        if (gingerbreadDurabilityDependentAttributes.containsKey(durabilityIn)) {
            return gingerbreadDurabilityDependentAttributes.get(durabilityIn);
        } else {
            float scaledDurability = durabilityIn / (float) maxDurability;
            double speed = MIN_SPEED_BOOST + (MAX_SPEED_BOOST - MIN_SPEED_BOOST) * scaledDurability;
            UUID uuid = ARMOR_MODIFIERS[this.acSlot.ordinal()];
            String modifierId = "gingerbread_armor." + this.acSlot.getName();
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.put(Attributes.ARMOR, ACCompat.attributeModifier(uuid, "Armor modifier", modifierId, this.acDefense, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.MOVEMENT_SPEED, ACCompat.attributeModifier(uuid, "Movement speed", modifierId, speed, AttributeModifier.Operation.MULTIPLY_BASE));
            Multimap<Attribute, AttributeModifier> attributeModifierMultimap = builder.build();
            gingerbreadDurabilityDependentAttributes.put(durabilityIn, attributeModifierMultimap);
            return attributeModifierMultimap;
        }
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions) AlexsCaves.PROXY.getArmorProperties());
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
        return slot == this.acSlot.getSlot() ? getOrCreateDurabilityAttributes(stack.getDamageValue(), stack.getMaxDamage()) : ImmutableMultimap.of();
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

    @Nullable
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        if (slot == EquipmentSlot.LEGS) {
            return AlexsCaves.MODID + ":textures/armor/gingerbread_armor_1.png";
        } else {
            return AlexsCaves.MODID + ":textures/armor/gingerbread_armor_0.png";
        }
    }
}
