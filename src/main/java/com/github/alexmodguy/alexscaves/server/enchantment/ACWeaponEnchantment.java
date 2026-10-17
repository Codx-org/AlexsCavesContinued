package com.github.alexmodguy.alexscaves.server.enchantment;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import net.minecraft.world.entity.EquipmentSlot;
//? if <1.21
import net.minecraft.world.item.enchantment.Enchantment;
//? if <1.20.5
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * Base class for Alex's Caves' 51 weapon enchantments.
 *
 * <p>1.20.5 rebuilt Enchantment: the constructor now takes a single
 * {@code EnchantmentDefinition} record, and {@code getMinCost}/{@code getMaxCost}/{@code getMaxLevel}
 * became {@code final} because their values live in that record. Upstream expressed exactly those
 * three numbers by overriding the three methods, so the port is a straight translation rather than a
 * behaviour change — see the constructor for the arithmetic.
 *
 * <p>The other half of the rebuild is applicability: an {@code EnchantmentCategory} (a
 * {@code Predicate<Item>} built in code) became a {@code TagKey<Item>}. The fourteen categories are
 * therefore item tags now; {@link com.github.alexmodguy.alexscaves.server.misc.ACTagRegistry} holds
 * them and {@link ACEnchantmentRegistry} picks the right kind per version.
 *
 * <p>1.21 finished the job by sealing Enchantment into a final record loaded from a data pack, so
 * there is no class left to extend and nothing to construct. From there this file keeps only
 * {@link Grade} — still the single place the weight and anvil cost of each grade are written down,
 * now consumed by the generator that emits the enchantment JSON rather than by a constructor.
 */
//? if >=1.21 {
/*public class ACWeaponEnchantment {
*///?} else {
public class ACWeaponEnchantment extends Enchantment {
//?}

    private int levels;
    private int minXP;
    private String registryName;

    // Fabric below 1.20.5 only: what Forge's extended EnchantmentCategory enum constant would have
    // held. See the constructor arm and canEnchant below, and ACEnchantmentRegistry for the whole
    // argument.
    //? if fabric && <1.20.5 {
    /*private java.util.function.Predicate<net.minecraft.world.item.Item> acCategory;
    *///?}

    /**
     * Upstream's {@code Enchantment.Rarity}, plus the anvil cost that 1.20.4 and earlier derived from
     * it inside {@code AnvilMenu} and 1.20.5 wants stated up front. Written as a mod enum so the call
     * sites in {@link ACEnchantmentRegistry} read the same on every version — {@code Rarity} itself is
     * gone from 1.20.5.
     */
    public enum Grade {
        COMMON(10, 1),
        UNCOMMON(5, 2),
        RARE(2, 4),
        VERY_RARE(1, 8);

        public final int weight;
        public final int anvilCost;

        Grade(int weight, int anvilCost) {
            this.weight = weight;
            this.anvilCost = anvilCost;
        }

        //? if <1.20.5 {
        /** The constant names are deliberately identical to vanilla's, so the mapping is by name. */
        public Enchantment.Rarity vanilla() {
            return Enchantment.Rarity.valueOf(name());
        }
        //?}
    }

    //? if >=1.21 {
    /*// Nothing to build: the 51 enchantments are data-pack entries under data/alexscaves/enchantment.
    *///?} elif >=1.20.5 {
    /*protected ACWeaponEnchantment(String name, Grade grade, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> supportedItems, int levels, int minXP, EquipmentSlot... equipmentSlot) {
        // The two Cost curves reproduce the overrides this class used to carry:
        //   getMinCost(i) = 1 + (i - 1) * minXP            -> dynamicCost(1, minXP)
        //   getMaxCost(i) = super.getMinCost(i) + 30, and the pre-1.20.5 base implementation of
        //                   getMinCost was 1 + i * 10, so that is 41 + (i - 1) * 10
        //                                                  -> dynamicCost(41, 10)
        // Enchantment.Cost#calculate is base + (level - 1) * perLevel, matching both exactly.
        super(Enchantment.definition(
                supportedItems,
                grade.weight,
                levels,
                Enchantment.dynamicCost(1, minXP),
                Enchantment.dynamicCost(41, 10),
                grade.anvilCost,
                equipmentSlot));
        this.levels = levels;
        this.minXP = minXP;
        this.registryName = name;
    }
    *///?} elif fabric {
    /*protected ACWeaponEnchantment(String name, Grade grade, java.util.function.Predicate<net.minecraft.world.item.Item> category, int levels, int minXP, EquipmentSlot... equipmentSlot) {
        super(grade.vanilla(), EnchantmentCategory.VANISHABLE, equipmentSlot);
        this.acCategory = category;
        this.levels = levels;
        this.minXP = minXP;
        this.registryName = name;
    }
    *///?} else {
    protected ACWeaponEnchantment(String name, Grade grade, EnchantmentCategory category, int levels, int minXP, EquipmentSlot... equipmentSlot) {
        super(grade.vanilla(), category, equipmentSlot);
        this.levels = levels;
        this.minXP = minXP;
        this.registryName = name;
    }
    //?}

    // Final from 1.20.5 — the definition passed to super() carries these numbers instead. Hoisted out
    // of the constructor chain above rather than repeated in each of its arms, which is what the extra
    // Fabric arm would otherwise have cost.
    //? if <1.20.5 {
    public int getMinCost(int i) {
        return 1 + (i - 1) * minXP;
    }

    public int getMaxCost(int i) {
        return super.getMinCost(i) + 30;
    }

    public int getMaxLevel() {
        return levels;
    }
    //?}

    // The placeholder category this loader hands super() is vanilla's VANISHABLE, which accepts very
    // nearly everything, so the question has to be answered here instead. Vanilla's own body is
    // `this.category.canEnchant(stack.getItem())` — the same shape, over the predicate Forge would
    // have wrapped in an enum constant. This is what the anvil, enchanted books and every loot
    // function consult; the enchanting table alone reads the raw `category` field, and
    // mixin.fabric.EnchantmentHelperMixin sends it here as well.
    //? if fabric && <1.20.5 {
    /*public boolean canEnchant(net.minecraft.world.item.ItemStack stack) {
        return acCategory.test(stack.getItem());
    }

    public java.util.function.Predicate<net.minecraft.world.item.Item> acCategory() {
        return acCategory;
    }
    *///?}


    // All four are Enchantment overrides, and 1.21 replaced each with a field of the JSON record:
    // exclusive_set, and membership of #minecraft:tradeable / #minecraft:in_enchanting_table /
    // #minecraft:non_treasure. That makes the enchantmentsInLoot config toggle static from 1.21 on.
    //? if <1.21 {
    protected boolean checkCompatibility(Enchantment enchantment) {
        return this != enchantment && ACEnchantmentRegistry.areCompatible(this, enchantment);
    }

    public boolean isTradeable() {
        return AlexsCaves.COMMON_CONFIG.enchantmentsInLoot.get();
    }

    public boolean isDiscoverable() {
        return true;
    }

    public boolean isAllowedOnBooks() {
        return AlexsCaves.COMMON_CONFIG.enchantmentsInLoot.get();
    }
    //?}

    public String getName(){
        return registryName;
    }
}
