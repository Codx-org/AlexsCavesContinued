package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.forge.common.brewing.BrewingRecipeRegistry;
import com.github.alexmodguy.alexscaves.fabric.forge.common.brewing.IBrewingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The far half of Fabric's brewing stand-in — the consumer {@link BrewingRecipeRegistry}'s javadoc
 * says belongs to the event-dispatch batch. With this in place the eleven recipes
 * {@code ACEffectRegistry#registerBrewing} pours into that registry are live rather than inert.
 *
 * <p>⚠️ The loaders do <em>not</em> patch {@code PotionBrewing} at all — its disassembly is
 * byte-identical between the Forge and vanilla 1.20.1 merged jars, which is exactly the trap the
 * house rule about diffing with {@code -c} rather than {@code -p} exists for. Forge splices four
 * calls into {@code BrewingStandBlockEntity} instead:
 * {@code isBrewable} → {@code BrewingRecipeRegistry.canBrew}, {@code doBrew} → {@code brewPotions},
 * and the two halves of {@code canPlaceItem} → {@code isValidIngredient} / {@code isValidInput}.
 * Grep the disassembly for the registry's own name before assuming where a hook lives.
 *
 * <p>This mixin takes the smaller, more portable route: vanilla's brewing stand reaches all four of
 * those decisions through three methods on {@code PotionBrewing}, so hooking those three covers
 * every call site indirectly and stays indifferent to the block entity's internals.
 *
 * <p><b>Vanilla wins ties, and that is faithful rather than a shortcut.</b> Forge's
 * {@code BrewingRecipeRegistry.<clinit>} seeds its list with {@code VanillaBrewingRecipe} at index 0
 * and every one of the six methods above is a first-match loop over it — so on the loaders vanilla is
 * consulted first too. Injecting at {@code RETURN} and only acting when vanilla found nothing
 * reproduces that ordering without needing to vendor {@code VanillaBrewingRecipe} at all.
 *
 * <p>{@code isValidInput} needs no counterpart: all eleven recipes take an {@code Items.POTION}
 * bottle, which vanilla's {@code canPlaceItem} already admits into slots 0–2.
 *
 * <p>Argument orders are read off the bytecode and the two are <em>opposite</em>, which is worth
 * stating because nothing in the names says so: {@code isBrewable} calls
 * {@code hasMix(bottle, ingredient)} (1.20.1 offset 53–55) while {@code doBrew} calls
 * {@code mix(ingredient, bottle)} (offset 21–31). {@code mix} also returns its <em>bottle argument
 * unchanged</em> when nothing matched (1.20.1 offset 181, 1.20.5 offset 200: {@code aload_2;
 * areturn}), not {@code EMPTY} — so the "did vanilla match?" test is an identity comparison against
 * that argument. Deriving it from emptiness would silently never fire.
 *
 * <p><b>1.20.5 moved the three targets from static to instance and changed nothing else about
 * them.</b> Names, descriptors, argument order and the fall-through return are all identical either
 * side of that line — {@code PotionBrewing} simply became a per-world object built from a
 * {@code Builder} instead of a static table. Mixin matches a handler's static-ness against its
 * target's, so the two arms below differ in exactly one modifier apiece; the decisions themselves
 * live in the three {@code @Unique} helpers, which are shared.
 *
 * <p><b>Why Fabric keeps the static registry above 1.20.5 when both loaders dropped it.</b> The
 * builder 1.20.5 introduced takes {@code Mix} triples of potion holders, which is strictly narrower
 * than an {@code IBrewingRecipe}: {@code ProperBrewingRecipe} matches its input bottle on the whole
 * stack, not on a potion id. Translating the eleven into vanilla mixes would change what they match,
 * so {@code ACEffectRegistry#setup} keeps filling the vendored registry on this loader on every
 * version and this mixin keeps consulting it.
 */
@Mixin(PotionBrewing.class)
public class PotionBrewingMixin {

    /** True when one of the mod's recipes accepts {@code ingredient} as its reagent. */
    @Unique
    private static boolean ac_isModIngredient(ItemStack ingredient) {
        for (IBrewingRecipe recipe : BrewingRecipeRegistry.getRecipes()) {
            if (recipe.isIngredient(ingredient)) {
                return true;
            }
        }
        return false;
    }

    /**
     * What the mod's recipes brew {@code bottle} into with {@code ingredient}, or an empty stack when
     * none of them matches. First match wins, as it does on the loaders.
     */
    @Unique
    private static ItemStack ac_modOutput(ItemStack bottle, ItemStack ingredient) {
        for (IBrewingRecipe recipe : BrewingRecipeRegistry.getRecipes()) {
            ItemStack output = recipe.getOutput(bottle, ingredient);
            if (!output.isEmpty()) {
                return output;
            }
        }
        return ItemStack.EMPTY;
    }

    //? if <1.20.5 {
    @Inject(
            method = "isIngredient(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void ac_isIngredient(ItemStack ingredient, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && ac_isModIngredient(ingredient)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "hasMix(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void ac_hasMix(ItemStack bottle, ItemStack ingredient, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && !ac_modOutput(bottle, ingredient).isEmpty()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "mix(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void ac_mix(ItemStack ingredient, ItemStack bottle, CallbackInfoReturnable<ItemStack> cir) {
        if (cir.getReturnValue() != bottle) {
            return;
        }
        ItemStack output = ac_modOutput(bottle, ingredient);
        if (!output.isEmpty()) {
            cir.setReturnValue(output);
        }
    }
    //?} else {
    /*@Inject(
            method = "isIngredient(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("RETURN"),
            cancellable = true
    )
    private void ac_isIngredient(ItemStack ingredient, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && ac_isModIngredient(ingredient)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "hasMix(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("RETURN"),
            cancellable = true
    )
    private void ac_hasMix(ItemStack bottle, ItemStack ingredient, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && !ac_modOutput(bottle, ingredient).isEmpty()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "mix(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void ac_mix(ItemStack ingredient, ItemStack bottle, CallbackInfoReturnable<ItemStack> cir) {
        if (cir.getReturnValue() != bottle) {
            return;
        }
        ItemStack output = ac_modOutput(bottle, ingredient);
        if (!output.isEmpty()) {
            cir.setReturnValue(output);
        }
    }
    *///?}
}
