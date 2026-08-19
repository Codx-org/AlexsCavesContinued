package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.server.enchantment.ACEnchantmentRegistry;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.item.DesolateDaggerEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if <1.21.5
import net.minecraft.world.item.SwordItem;
//? if <1.21.2
import net.minecraft.world.item.Tiers;

// 1.21.5 deleted SwordItem the same way it deleted ArmorItem: a sword is a plain Item whose
// Properties carry sword(material, damage, speed), which sets the tool component, the attack
// modifiers and the block-breaking behaviour the class used to provide.
//? if >=1.21.5 {
/*public class DesolateDaggerItem extends net.minecraft.world.item.Item implements ACRepairableItem {
*///?} else {
public class DesolateDaggerItem extends SwordItem implements ACRepairableItem {
//?}
    /**
     * 1.20.5 moved a sword's damage and attack speed out of the constructor and into the item's
     * attribute modifiers ({@code SwordItem.createAttributes}); the {@code !mc205-swordctor}
     * replacement rule rewrites the call below into that shape. 1.21.2 undid it — the four-argument
     * constructor is back, applying the same two numbers through {@code ToolMaterial}, and
     * {@code createAttributes} is gone. That is a source-level gate rather than a third replacement
     * rule because the ≥1.20.5 rule still has to fire on every node between the two versions, and
     * two rules rewriting the same line would not be order-independent.
     *
     * <p>The tier enum itself moved in the same version: {@code Tiers} became the
     * {@code ToolMaterial} record, with the same six constants.
     */
    public DesolateDaggerItem() {
        //? if >=1.21.5 {
        /*super((new Item.Properties()).sword(net.minecraft.world.item.ToolMaterial.DIAMOND, 0, -2F).rarity(ACItemRegistry.RARITY_DEMONIC));
        *///?} elif >=1.21.2 {
        /*super(net.minecraft.world.item.ToolMaterial.DIAMOND, 0, -2F, (new Item.Properties()).rarity(ACItemRegistry.RARITY_DEMONIC));
        *///?} else {
        super(Tiers.DIAMOND, 0, -2F, (new Item.Properties()).rarity(ACItemRegistry.RARITY_DEMONIC));
        //?}
    }

    public int getMaxDamage(ItemStack stack) {
        return 360;
    }

    // The orbiting daggers spawn on a landed hit. Up to 1.21.4 that is `hurtEnemy` returning true
    // — SwordItem's override always does, so the else branch was unreachable. 1.21.5 made
    // Item#hurtEnemy void and split the "it actually connected" half out as postHurtEnemy, which
    // is the same moment under a different name.
    //? if >=1.21.5 {
    /*@Override
    public void postHurtEnemy(ItemStack stack, LivingEntity hurt, LivingEntity player) {
        super.postHurtEnemy(stack, hurt, player);
        acSpawnDaggers(stack, hurt, player);
    }
    *///?} else {
    public boolean hurtEnemy(ItemStack stack, LivingEntity hurt, LivingEntity player) {
        if (super.hurtEnemy(stack, hurt, player)) {
            acSpawnDaggers(stack, hurt, player);
            return true;
        } else {
            return false;
        }
    }
    //?}

    private void acSpawnDaggers(ItemStack stack, LivingEntity hurt, LivingEntity player) {
        int delayedLevel = ACCompat.enchantLevel(stack, ACEnchantmentRegistry.IMPENDING_STAB);
        for(int i = 0; i < 1 + ACCompat.enchantLevel(stack, ACEnchantmentRegistry.DOUBLE_STAB); i++){
            DesolateDaggerEntity daggerEntity = ACCompat.createEntity(ACEntityRegistry.DESOLATE_DAGGER.get(), player.level());
            daggerEntity.setTargetId(hurt.getId());
            daggerEntity.copyPosition(player);
            daggerEntity.setItemStack(stack);
            daggerEntity.orbitFor = (delayedLevel > 0 ? 40 : 20) + player.getRandom().nextInt(10);
            player.level().addFreshEntity(daggerEntity);
        }
    }

    @Override
    public Item[] acExtraRepairMaterials() {
        return new Item[]{ACItemRegistry.PURE_DARKNESS.get()};
    }

    @Override
    public boolean acReplacesTierRepairMaterials() {
        return true;
    }

    // 1.21.2 deleted Item#isValidRepairItem for the minecraft:repairable component; see
    // ACRepairableItem, which every version answers through.
    //? if <1.21.2 {
    public boolean isValidRepairItem(ItemStack itemStack, ItemStack repairWith) {
        return ACRepairableItem.isExtraRepairMaterial(this, repairWith);
    }
    //?}

}
