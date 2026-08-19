package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
//? if <1.21.5
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * The six armour sets' shared stat block.
 *
 * <p>Up to 1.20.4 {@code ArmorMaterial} was an interface a mod implemented, so this class simply was
 * one. 1.20.5 made it a record: the stats moved into a value object, the value object is addressed
 * through a {@code Holder}, and two of the numbers changed owner — durability moved out to the
 * item's {@code Properties}, and the equip sound became a {@code Holder<SoundEvent>}.
 *
 * <p>So from 1.20.5 this class is no longer an {@code ArmorMaterial}; it is a description of one,
 * which builds the record on demand. The two things an {@link ArmorItem} constructor needs are
 * {@link #vanilla()} and {@link #properties}, and those two methods read identically on every
 * version — which is why the six armour item classes take an {@code ACArmorMaterial} rather than a
 * vanilla type.
 */
public class ACArmorMaterial
        //? if <1.20.5
        implements ArmorMaterial
{

    protected static final int[] MAX_DAMAGE_ARRAY = new int[]{13, 15, 16, 11};
    private String name;
    private int durability;
    private int[] damageReduction;
    private int encantability;
    private SoundEvent sound;
    private float toughness;
    private Ingredient ingredient = null;
    public float knockbackResistance = 0.0F;

    public ACArmorMaterial(String name, int durability, int[] damageReduction, int encantability, SoundEvent sound, float toughness) {
        this(name, durability, damageReduction, encantability, sound, toughness, 0);
    }

    public ACArmorMaterial(String name, int durability, int[] damageReduction, int encantability, SoundEvent sound, float toughness, float knockbackResist) {
        this.name = name;
        this.durability = durability;
        this.damageReduction = damageReduction;
        this.encantability = encantability;
        this.sound = sound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResist;
    }

    /**
     * The material value an {@link ArmorItem} constructor takes — this object itself up to 1.20.4,
     * and from 1.20.5 a {@code Holder} around a freshly built record.
     *
     * <p>The holder is deliberately {@code direct} rather than registered. Nothing here needs a
     * registry key: all six sets are drawn by this mod's own armour layers rather than by
     * {@code HumanoidArmorLayer}'s texture lookup, and none of them are trimmable, so the two
     * consumers of the key never run. Registering would also mean ordering a new
     * {@code DeferredRegister} against {@code ACItemRegistry}'s static initialiser, which builds
     * these materials as a side effect of the item registry loading.
     *
     * <p>Note the repair ingredient is a supplier: {@code ACItemRegistry} calls
     * {@link #setRepairMaterial} after the items exist, which is long after this record is built.
     *
     * <p>1.21.2 changed it again: the record moved to {@code net.minecraft.world.item.equipment},
     * took the durability base back off the item, dropped the {@code Holder} indirection, and
     * replaced the two remaining free-form fields with ids — the repair ingredient became a
     * {@code TagKey<Item>} and an equipment-model id arrived. See {@link #repairTag()} and
     * {@link ACCompat#equipmentAsset} for what this mod points those at — the latter because 1.21.4
     * turned that id into a {@code ResourceKey<EquipmentAsset>}, and a nested Stonecutter gate cannot
     * live inside this already-commented arm.
     */
    //? if >=1.21.2 {
    /*public ArmorMaterial vanilla() {
        java.util.Map<ArmorItem.Type, Integer> defense = new java.util.EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            // BODY arrived in 1.20.5 for wolf armour and has no entry in this mod's four-slot array.
            if (type.ordinal() < damageReduction.length) {
                defense.put(type, damageReduction[type.ordinal()]);
            }
        }
        return new ArmorMaterial(
                durability,
                defense,
                encantability,
                ACCompat.soundHolder(sound),
                toughness,
                knockbackResistance,
                repairTag(),
                ACCompat.equipmentAsset(name));
    }

    // The tag whose contents repair this armour in an anvil. 1.21.2 takes an item TAG rather than
    // an ingredient, and it is read while the item is being constructed — so setRepairMaterial,
    // which ACItemRegistry#setup calls long afterwards, cannot feed it and is dead from here on.
    // The tags are named after the material and defined in data/alexscaves/tags/items/; a tag file
    // that did not exist would resolve to an empty set, i.e. armour that no anvil can repair.
    private net.minecraft.tags.TagKey<Item> repairTag() {
        return net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "repairs_" + name + "_armor"));
    }

    *///?} elif >=1.20.5 {
    /*public net.minecraft.core.Holder<ArmorMaterial> vanilla() {
        java.util.Map<ArmorItem.Type, Integer> defense = new java.util.EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            // BODY arrived in 1.20.5 for wolf armour and has no entry in this mod's four-slot array.
            if (type.ordinal() < damageReduction.length) {
                defense.put(type, damageReduction[type.ordinal()]);
            }
        }
        return net.minecraft.core.Holder.direct(new ArmorMaterial(
                defense,
                encantability,
                ACCompat.soundHolder(sound),
                this::getRepairIngredient,
                java.util.List.of(),
                toughness,
                knockbackResistance));
    }
    *///?} else {
    public ArmorMaterial vanilla() {
        return this;
    }
    //?}

    /**
     * Finishes an armour piece's {@code Item.Properties}.
     *
     * <p>Up to 1.20.4 durability came from the material via {@link #getDurabilityForType}; from
     * 1.20.5 the item carries it, so it has to be stated here. The arithmetic is upstream's own
     * {@link #MAX_DAMAGE_ARRAY} rather than {@code ArmorItem.Type#getDurability}, because the two
     * disagree — upstream's array is in the pre-1.13 slot order — and the point is to keep the
     * numbers identical to what 1.20.1 shipped.
     *
     * <p>From 1.21.2 there is nothing to state: durability went back into the material, and
     * {@code ArmorItem}'s constructor runs {@code ArmorMaterial#humanoidProperties} over whatever
     * this returns, so a {@code durability} set here is overwritten a moment later. The number
     * therefore becomes {@code ArmorType#getDurability}'s — {@code unitDurability * durability},
     * with vanilla's per-slot units.
     *
     * <p>That un-transposes upstream's helmet/boots and chestplate/leggings figures. {@link
     * #MAX_DAMAGE_ARRAY} is vanilla's own {@code HEALTH_PER_SLOT}, which vanilla indexes by
     * {@code EquipmentSlot#getIndex} (feet 13, legs 15, chest 16, head 11) while upstream indexes
     * it by {@code Type#ordinal} (head 13, chest 15, legs 16, feet 11) — so a helmet was carrying
     * the boots' unit and vice versa. 1.21.2+ gets vanilla's proportions; the older nodes keep the
     * numbers they shipped with, since there is no way to hand 1.21.2 a per-slot durability base.
     *
     * <p>1.21.5 deleted {@code ArmorItem} outright, and with it the constructor that used to apply
     * {@code ArmorMaterial#humanoidProperties}. The identical work is {@code
     * Item.Properties#humanoidArmor} now — durability, defence, equippable, repairable, the lot — so
     * from that version this method is the only thing that makes an armour piece armour, and the six
     * sets extend a plain {@code Item}.
     */
    public Item.Properties properties(Item.Properties properties, ArmorItem.Type type) {
        //? if >=1.21.5 {
        /*return properties.humanoidArmor(vanilla(), type);
        *///?} elif >=1.21.2 {
        /*return properties;
        *///?} elif >=1.20.5 {
        /*return properties.durability(MAX_DAMAGE_ARRAY[type.ordinal()] * durability);
        *///?} else {
        return properties;
        //?}
    }

    /**
     * The armour points this material gives in {@code type}.
     *
     * <p>Upstream read these off {@code ArmorItem#getDefense}; 1.21.2 deleted that method along
     * with the {@code type} field it answered from, so the two items that state their own attribute
     * map ({@link DivingArmorItem}, {@link GingerbreadArmorItem}) ask the material directly. Same
     * number on every version — {@code getDefense} was only ever this array lookup.
     */
    public int defense(ArmorItem.Type type) {
        return this.damageReduction[type.ordinal()];
    }

    //? if <1.20.5 {
    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return MAX_DAMAGE_ARRAY[type.ordinal()] * this.durability;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return this.damageReduction[type.ordinal()];
    }

    @Override
    public int getEnchantmentValue() {
        return this.encantability;
    }

    @Override
    public SoundEvent getEquipSound() {
        return this.sound;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return knockbackResistance;
    }
    //?}

    /**
     * Toughness and knockback resistance, readable on every version.
     *
     * <p>The {@code getToughness}/{@code getKnockbackResistance} pair above only exists below 1.20.5,
     * where it implements the interface; {@code ArmorItem} carried the same two numbers as protected
     * state there and lost them in 1.20.5. {@link DivingArmorItem} needs both while building its own
     * attribute map, so it reads them here instead.
     */
    public float toughness() {
        return toughness;
    }

    public float knockbackResistance() {
        return knockbackResistance;
    }

    /**
     * The anvil repair ingredient, for the versions that take one. 1.21.2 replaced it with a
     * {@code TagKey<Item>} (see {@link #repairTag()}) and deleted {@code Ingredient.EMPTY} along
     * with the idea of an empty ingredient, so from there this method has no callers and no body
     * that would compile. {@link #setRepairMaterial} stays on every version — it only writes a
     * field, and ACItemRegistry#setup calls it unconditionally.
     */
    //? if <1.21.2 {
    public Ingredient getRepairIngredient() {
        return this.ingredient == null ? Ingredient.EMPTY : this.ingredient;
    }
    //?}

    public void setRepairMaterial(Ingredient ingredient) {
        this.ingredient = ingredient;
    }

}
