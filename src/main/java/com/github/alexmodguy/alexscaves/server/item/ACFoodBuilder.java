package com.github.alexmodguy.alexscaves.server.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * This mod's food descriptions, and the one place that knows how a version spells them.
 *
 * <p>Up to 1.21.1 a {@code FoodProperties} carried everything about eating something: how long it
 * took, what effects it applied, whether it was meat, what it left behind. 1.21.2 cut it down to
 * three fields — nutrition, saturation, can-always-eat — and moved the rest onto two separate data
 * components, {@code CONSUMABLE} (duration, animation, sound, consume effects) and
 * {@code USE_REMAINDER} (the empty bowl). So a food is no longer one object, and
 * {@code Item.Properties#food(FoodProperties)} alone can no longer express one of this mod's.
 *
 * <p>Rather than gate 63 declarations in {@link ACFoods} and 46 registration sites, this builder
 * accumulates the description in plain fields — upstream's vocabulary, unchanged on every node —
 * and resolves it once in {@link #build()}. {@link #food(Item.Properties, FoodProperties)} is the
 * other half: it applies whatever did not fit in the {@code FoodProperties} to the item.
 *
 * <p>The two are linked by an identity map rather than by a richer return type, so that
 * {@code ACFoods}' constants stay plain {@code FoodProperties} and the dozen places that read one
 * off a stack keep working untouched. Identity is the right key: each constant is a single static
 * instance, and from 1.21.2 the record has so few fields that distinct foods compare equal.
 *
 * <p>This class is why {@code ACFoods} carries no version conditionals and needs no replacement
 * rules — the four {@code !mc205-food-*} rules that used to rewrite its method names are gone.
 *
 * <p>Effects arrive as suppliers on every version, which is more than 1.20.5+ needs but is what
 * keeps {@code ACFoods}' initialiser from resolving {@code ACEffectRegistry} entries before the
 * effect registry is populated — see {@code ACBlockRegistry.registerBlockAndItemEdible}.
 */
public class ACFoodBuilder {

    private record Effect(Supplier<MobEffectInstance> instance, float probability) {
    }

    /**
     * The extras of each food built here, keyed by the {@code FoodProperties} it resolved to.
     *
     * <p>Only read from 1.21.2 on, and only by {@link #food(Item.Properties, FoodProperties)} —
     * which runs immediately after {@link ACFoods} has finished initialising, on the very
     * instances put here. Nothing reads it off a stack later; the two places that ask a stack
     * about its effects or its bowl read the components back instead (see {@code ACCompat}).
     */
    private static final Map<FoodProperties, ACFoodBuilder> BUILT = new IdentityHashMap<>();

    private int nutrition;
    private float saturationMod;
    private boolean alwaysEat;
    private boolean meat;
    private boolean fast;
    private boolean bowl;
    private final List<Effect> effects = new ArrayList<>();
    //? if >=1.21.2 {
    /*private net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound;
    *///?} else {
    private net.minecraft.sounds.SoundEvent sound;
    //?}

    private ACFoodBuilder() {
    }

    public static ACFoodBuilder of() {
        return new ACFoodBuilder();
    }

    public ACFoodBuilder nutrition(int nutrition) {
        this.nutrition = nutrition;
        return this;
    }

    public ACFoodBuilder saturationMod(float saturationMod) {
        this.saturationMod = saturationMod;
        return this;
    }

    public ACFoodBuilder alwaysEat() {
        this.alwaysEat = true;
        return this;
    }

    /**
     * Halves the time the food takes to eat: 16 ticks instead of 32, which is 0.8s against the
     * 1.6s a {@code Consumable} defaults to from 1.21.2 — the same pair of numbers under both APIs.
     */
    public ACFoodBuilder fast() {
        this.fast = true;
        return this;
    }

    /**
     * Kept only so {@link ACFoods} still reads as upstream wrote it. The flag stopped existing in
     * 1.20.5 and the five foods that set it are listed in the {@code minecraft:meat} item tag
     * instead — see {@code ACCompat.isMeat}, which is where the tag is explained.
     */
    public ACFoodBuilder meat() {
        this.meat = true;
        return this;
    }

    public ACFoodBuilder effect(Supplier<MobEffectInstance> instance, float probability) {
        this.effects.add(new Effect(instance, probability));
        return this;
    }

    /** Eating this hands an empty bowl back. Before 1.21 that is {@code BowlFoodItem}'s job. */
    public ACFoodBuilder bowl() {
        this.bowl = true;
        return this;
    }

    /**
     * The sound eating this makes, where it is not the default {@code GENERIC_EAT}.
     *
     * <p>Only three foods set one, and only from 1.21.2 does it belong here: before that an item
     * answered {@code getEatingSound}/{@code getDrinkingSound} itself, and the three items in
     * question still do on those versions. 1.21.2 deleted both hooks — the sound is a field of the
     * {@code CONSUMABLE} component now, and the one escape hatch left, {@code
     * Consumable.OverrideConsumeSound}, is tested against the *entity* doing the eating rather
     * than the stack, so an item cannot use it.
     *
     * <p>The eat animation did not move and needs no equivalent here: {@code Item#getUseAnimation}
     * survives, and the eleven overrides of it in this tree still decide what the player sees.
     *
     * <p>The parameter type is gated because {@code SoundEvents}' constants became registry holders
     * in the same release; the argument every call site writes is identical on both sides.
     */
    //? if >=1.21.2 {
    /*public ACFoodBuilder sound(net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound) {
    *///?} else {
    public ACFoodBuilder sound(net.minecraft.sounds.SoundEvent sound) {
    //?}
        this.sound = sound;
        return this;
    }

    public FoodProperties build() {
        FoodProperties food = resolve();
        BUILT.put(food, this);
        return food;
    }

    private FoodProperties resolve() {
        //? if >=1.21.2 {
        /*FoodProperties.Builder builder = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturationMod);
        if (alwaysEat) {
            builder.alwaysEdible();
        }
        return builder.build();
        *///?} else if >=1.21 {
        /*FoodProperties.Builder builder = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturationMod);
        if (alwaysEat) {
            builder.alwaysEdible();
        }
        if (fast) {
            builder.fast();
        }
        if (bowl) {
            builder.usingConvertsTo(net.minecraft.world.item.Items.BOWL);
        }
        for (Effect effect : effects) {
            builder.effect(effect.instance().get(), effect.probability());
        }
        return builder.build();
        *///?} else if >=1.20.5 {
        /*FoodProperties.Builder builder = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturationMod);
        if (alwaysEat) {
            builder.alwaysEdible();
        }
        if (fast) {
            builder.fast();
        }
        for (Effect effect : effects) {
            builder.effect(effect.instance().get(), effect.probability());
        }
        return builder.build();
        *///?} else {
        FoodProperties.Builder builder = new FoodProperties.Builder().nutrition(nutrition).saturationMod(saturationMod);
        if (alwaysEat) {
            builder.alwaysEat();
        }
        if (fast) {
            builder.fast();
        }
        if (meat) {
            builder.meat();
        }
        for (Effect effect : effects) {
            builder.effect(effect.instance().get(), effect.probability());
        }
        return builder.build();
        //?}
    }

    /**
     * Applies one of {@link ACFoods}' constants to an item, in place of {@code Properties#food}.
     *
     * <p>Below 1.21.2 that is all {@code food(FoodProperties)} ever did. From 1.21.2 the eat
     * duration and the consume effects live on the {@code CONSUMABLE} component and the bowl on
     * {@code USE_REMAINDER}, so all three are set here; the food itself no longer carries any of
     * them.
     */
    public static Item.Properties food(Item.Properties properties, FoodProperties food) {
        //? if >=1.21.2 {
        /*ACFoodBuilder built = BUILT.get(food);
        net.minecraft.world.item.component.Consumable.Builder consumable = net.minecraft.world.item.component.Consumable.builder();
        if (built != null) {
            if (built.fast) {
                consumable.consumeSeconds(0.8F);
            }
            if (built.sound != null) {
                consumable.sound(built.sound);
            }
            for (Effect effect : built.effects) {
                consumable.onConsume(new net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect(
                        effect.instance().get(), effect.probability()));
            }
        }
        properties.food(food, consumable.build());
        if (built != null && built.bowl) {
            properties.usingConvertsTo(net.minecraft.world.item.Items.BOWL);
        }
        return properties;
        *///?} else {
        return properties.food(food);
        //?}
    }

}
