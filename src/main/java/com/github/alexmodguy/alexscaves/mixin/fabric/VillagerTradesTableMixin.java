package com.github.alexmodguy.alexscaves.mixin.fabric;

import com.github.alexmodguy.alexscaves.fabric.event.ACFabricVillagerTrades;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

/**
 * Hands a villager the merged trade table {@link ACFabricVillagerTrades} built at server start,
 * in place of vanilla's own — see that class for why the table is substituted at the read rather
 * than written back the way Forge writes it back.
 *
 * <p>The field is read twice in {@code updateTrades} from 1.21.5 (once for the profession's own
 * entry and once for the trade-rebalance fallback) and both reads want the same substitution, so
 * the injector deliberately has no {@code ordinal}. {@code EXPERIMENTAL_TRADES} is a different
 * field and is left alone.
 *
 * <p>The target class and the field owner are fully qualified rather than imported: 1.21.11 moved
 * both into an {@code npc.villager} sub-package, and there is no rename rule for the bare
 * {@code Villager} name — one could not be added without colliding with the two rules that already
 * rewrite {@code VillagerProfession} and {@code VillagerTrades} at the same offset.
 */
//? if >=1.21.11 {
/*@Mixin(net.minecraft.world.entity.npc.villager.Villager.class)
*///?} else {
@Mixin(net.minecraft.world.entity.npc.Villager.class)
//?}
public class VillagerTradesTableMixin {

    //? if >=1.21.11 {
    /*@com.llamalad7.mixinextras.injector.ModifyExpressionValue(
            method = "updateTrades",
            at = @At(value = "FIELD", opcode = org.objectweb.asm.Opcodes.GETSTATIC,
                    target = "Lnet/minecraft/world/entity/npc/villager/VillagerTrades;TRADES:Ljava/util/Map;"))
    *///?} else {
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(
            method = "updateTrades",
            at = @At(value = "FIELD", opcode = org.objectweb.asm.Opcodes.GETSTATIC,
                    target = "Lnet/minecraft/world/entity/npc/VillagerTrades;TRADES:Ljava/util/Map;"))
    //?}
    @SuppressWarnings("rawtypes")
    private Map ac_villagerTrades(Map original) {
        return ACFabricVillagerTrades.villagerTrades(original);
    }
}
