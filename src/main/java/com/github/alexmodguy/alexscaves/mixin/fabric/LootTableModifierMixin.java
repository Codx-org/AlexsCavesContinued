package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.loot.ACFabricLootModifiers;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The roll-time half of Fabric's global-loot-modifier stand-in; the near half, and all of the
 * reasoning about why the mechanism looks like this, is {@link ACFabricLootModifiers}.
 *
 * <p>This is the exact place Forge puts its own hook. Forge rewrites the tail of the <em>private</em>
 * {@code getRandomItems(LootContext)} to {@code return ForgeHooks.modifyLoot(getLootTableId(), list,
 * context)} — so injecting at {@code RETURN} of that same method reproduces it position for position,
 * and this mixin's only difference is that it looks the modifier up by table identity rather than by
 * an id field Fabric has no reason to add.
 *
 * <p><b>One injection is complete coverage.</b> Every public roll path funnels here — checked in the
 * 1.20.1 bytecode, {@code getRandomItems(LootParams, long, Consumer)}, {@code (LootParams, Consumer)},
 * {@code (LootContext, Consumer)}, {@code (LootParams, long)}, {@code (LootParams)} and
 * {@code fill(...)} all reach it with an {@code invokevirtual}. The one path that does not is
 * {@code getRandomItemsRaw(LootParams, Consumer)}, and that bypasses modifiers on Forge too, so
 * matching it is faithful rather than a gap.
 *
 * <p>⚠️ Deliberately ungated, for the reason spelled out on {@link ACFabricLootModifiers}: this
 * should fail loudly rather than quietly stop firing. The 1.21 break both files braced for did not
 * happen — the loot-table registry move landed at 1.20.5 and touched only the near half's lookup,
 * while {@code getRandomItems(LootContext)} kept its name, visibility and descriptor. Re-derive the
 * target on the next wave that reshapes {@code LootTable} rather than assuming it holds forever.
 */
@Mixin(LootTable.class)
public class LootTableModifierMixin {

    @Inject(
            method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void ac_modifyLoot(LootContext context, CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir) {
        ObjectArrayList<ItemStack> loot = cir.getReturnValue();
        ObjectArrayList<ItemStack> modified = ACFabricLootModifiers.modifyLoot((LootTable) (Object) this, loot, context);
        // Both of this mod's modifiers mutate the list in place and hand back the same instance, so
        // in practice this branch never runs — it is here because IGlobalLootModifier's contract
        // permits returning a different list and a future modifier is free to.
        if (modified != loot) {
            cir.setReturnValue(modified);
        }
    }
}
