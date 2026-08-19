package com.github.alexmodguy.alexscaves.server.potion;


import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.item.ACItemRegistry;
import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.IBrewingRecipe;
//? if fabric || <1.20.5
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ACEffectRegistry {

    public static final DeferredRegister<MobEffect> DEF_REG = DeferredRegister.create(Registries.MOB_EFFECT, AlexsCaves.MODID);
    public static final DeferredRegister<Potion> POTION_DEF_REG = DeferredRegister.create(Registries.POTION, AlexsCaves.MODID);
    public static final Supplier<MobEffect> MAGNETIZING = DEF_REG.register("magnetizing", () -> new MagnetizedEffect());
    public static final Supplier<MobEffect> STUNNED = DEF_REG.register("stunned", () -> new StunnedEffect());
    public static final Supplier<MobEffect> RAGE = DEF_REG.register("rage", () -> new RageEffect());
    public static final Supplier<MobEffect> IRRADIATED = DEF_REG.register("irradiated", () -> new IrradiatedEffect());
    public static final Supplier<MobEffect> BUBBLED = DEF_REG.register("bubbled", () -> new BubbledEffect());
    public static final Supplier<MobEffect> DEEPSIGHT = DEF_REG.register("deepsight", () -> new DeepsightEffect());
    public static final Supplier<MobEffect> DARKNESS_INCARNATE = DEF_REG.register("darkness_incarnate", () -> new DarknessIncarnateEffect());
    public static final Supplier<MobEffect> SUGAR_RUSH = DEF_REG.register("sugar_rush", () -> new SugarRushEffect());
    public static final Supplier<Potion> MAGNETIZING_POTION = POTION_DEF_REG.register("magnetizing", () -> new Potion("magnetizing", new MobEffectInstance(ACCompat.effect(MAGNETIZING.get()), 3600)));
    public static final Supplier<Potion> LONG_MAGNETIZING_POTION = POTION_DEF_REG.register("long_magnetizing", () -> new Potion("long_magnetizing", new MobEffectInstance(ACCompat.effect(MAGNETIZING.get()), 9600)));
    public static final Supplier<Potion> DEEPSIGHT_POTION = POTION_DEF_REG.register("deepsight", () -> new Potion("deepsight", new MobEffectInstance(ACCompat.effect(DEEPSIGHT.get()), 3600)));
    public static final Supplier<Potion> LONG_DEEPSIGHT_POTION = POTION_DEF_REG.register("long_deepsight", () -> new Potion("long_deepsight", new MobEffectInstance(ACCompat.effect(DEEPSIGHT.get()), 9600)));
    public static final Supplier<Potion> GLOWING_POTION = POTION_DEF_REG.register("glowing", () -> new Potion("glowing", new MobEffectInstance(MobEffects.GLOWING, 3600)));
    public static final Supplier<Potion> LONG_GLOWING_POTION = POTION_DEF_REG.register("long_glowing", () -> new Potion("long_glowing", new MobEffectInstance(MobEffects.GLOWING, 9600)));
    public static final Supplier<Potion> HASTE_POTION = POTION_DEF_REG.register("haste", () -> new Potion("haste", new MobEffectInstance(MobEffects.DIG_SPEED, 3600)));
    public static final Supplier<Potion> LONG_HASTE_POTION = POTION_DEF_REG.register("long_haste", () -> new Potion("long_haste", new MobEffectInstance(MobEffects.DIG_SPEED, 9600)));
    public static final Supplier<Potion> STRONG_HASTE_POTION = POTION_DEF_REG.register("strong_haste", () -> new Potion("strong_haste", new MobEffectInstance(MobEffects.DIG_SPEED, 1800, 1)));
    public static final Supplier<Potion> STRONG_HUNGER_POTION = POTION_DEF_REG.register("strong_hunger", () -> new Potion("strong_hunger", new MobEffectInstance(MobEffects.HUNGER, 1800, 4)));
    public static final Supplier<Potion> SUGAR_RUSH_POTION = POTION_DEF_REG.register("sugar_rush", () -> new Potion("sugar_rush", new MobEffectInstance(ACCompat.effect(SUGAR_RUSH.get()), 1800)));
    public static final Supplier<Potion> LONG_SUGAR_RUSH_POTION = POTION_DEF_REG.register("long_sugar_rush", () -> new Potion("long_sugar_rush", new MobEffectInstance(ACCompat.effect(SUGAR_RUSH.get()), 3600)));


    /**
     * Up to 1.20.4 brewing recipes lived in a static registry that could be filled at any point
     * during mod setup. 1.20.5 made the recipe set part of the world — {@code PotionBrewing} is
     * rebuilt per load from a builder — so from there on the eleven recipes are contributed from an
     * event instead, and on Forge and NeoForge this method has nothing left to do.
     *
     * <p>Fabric keeps the static registry on every version, which is why its arm of the gate is
     * unbounded. There is no brewing event to contribute to on that loader and vanilla's builder
     * takes only {@code Mix} triples of potion holders, which is a strictly narrower shape than the
     * {@code IBrewingRecipe} these eleven are — {@code ProperBrewingRecipe} matches its input bottle
     * on the whole stack. So the recipes stay in the vendored registry and
     * {@code mixin.fabric.PotionBrewingMixin} consults it after vanilla, on both sides of 1.20.5.
     */
    public static void setup() {
        // ⚠ Fabric DEFERS rather than fills. An IBrewingRecipe holds finished ItemStacks and from
        // 26.1 building one throws "Components not bound yet" until the first datapack reload —
        // and this method stands in for FMLCommonSetupEvent, which on this loader runs from
        // onInitialize, long before any world exists. See BrewingRecipeRegistry#deferRecipes.
        // The Forge/NeoForge <1.20.5 arm below is safe as-is: components did not exist yet.
        //? if fabric {
        /*BrewingRecipeRegistry.deferRecipes(() -> registerBrewing(BrewingRecipeRegistry::addRecipe));
        *///?} elif <1.20.5 {
        registerBrewing(BrewingRecipeRegistry::addRecipe);
        //?}
    }

    /**
     * The eleven brewing recipes this mod adds, handed one at a time to {@code out}.
     *
     * <p>Split out from {@link #setup()} because the sink differs per era and per loader: the static
     * registry below 1.20.5, Forge's {@code BrewingRecipeRegisterEvent#addRecipe} above it, and
     * NeoForge's {@code PotionBrewing.Builder#addRecipe} reached through its own event. All three are
     * {@code IBrewingRecipe} consumers, which is the whole reason this reads as one list.
     */
    public static void registerBrewing(Consumer<IBrewingRecipe> out) {
        out.accept(new ProperBrewingRecipe(createPotion(ACCompat.vanillaPotion(Potions.AWKWARD)), Ingredient.of(ACItemRegistry.FERROUSLIME_BALL.get()), createPotion(MAGNETIZING_POTION)));
        out.accept(new ProperBrewingRecipe(createPotion(MAGNETIZING_POTION), Ingredient.of(Items.REDSTONE), createPotion(LONG_MAGNETIZING_POTION)));
        out.accept(new ProperBrewingRecipe(createPotion(ACCompat.vanillaPotion(Potions.AWKWARD)), Ingredient.of(ACItemRegistry.LANTERNFISH.get()), createPotion(DEEPSIGHT_POTION)));
        out.accept(new ProperBrewingRecipe(createPotion(DEEPSIGHT_POTION), Ingredient.of(Items.REDSTONE), createPotion(LONG_DEEPSIGHT_POTION)));
        out.accept(new ProperBrewingRecipe(createPotion(ACCompat.vanillaPotion(Potions.AWKWARD)), Ingredient.of(ACItemRegistry.BIOLUMINESSCENCE.get()), createPotion(GLOWING_POTION)));
        out.accept(new ProperBrewingRecipe(createPotion(GLOWING_POTION), Ingredient.of(Items.REDSTONE), createPotion(LONG_GLOWING_POTION)));
        out.accept(new ProperBrewingRecipe(createPotion(ACCompat.vanillaPotion(Potions.AWKWARD)), Ingredient.of(ACItemRegistry.CORRODENT_TEETH.get()), createPotion(HASTE_POTION)));
        out.accept(new ProperBrewingRecipe(createPotion(HASTE_POTION), Ingredient.of(Items.REDSTONE), createPotion(LONG_HASTE_POTION)));
        out.accept(new ProperBrewingRecipe(createPotion(HASTE_POTION), Ingredient.of(Items.GLOWSTONE_DUST), createPotion(STRONG_HASTE_POTION)));
        out.accept(new ProperBrewingRecipe(createPotion(ACCompat.vanillaPotion(Potions.STRONG_SWIFTNESS)), Ingredient.of(ACItemRegistry.SWEET_TOOTH.get()), createPotion(SUGAR_RUSH_POTION)));
        out.accept(new ProperBrewingRecipe(createPotion(SUGAR_RUSH_POTION), Ingredient.of(Items.REDSTONE), createPotion(LONG_SUGAR_RUSH_POTION)));
    }

    public static ItemStack createPotion(Supplier<Potion> potion) {
        return createPotion(potion.get());
    }

    public static ItemStack createPotion(Potion potion) {
        return ACCompat.potionStack(Items.POTION, potion);
    }

    public static ItemStack createSplashPotion(Potion potion) {
        return ACCompat.potionStack(Items.SPLASH_POTION, potion);
    }

    public static ItemStack createLingeringPotion(Potion potion) {
        return ACCompat.potionStack(Items.LINGERING_POTION, potion);
    }

    public static ItemStack createJellybean(Potion potion) {
        return ACCompat.potionStack(ACItemRegistry.JELLY_BEAN.get(), potion);
    }
}
