package com.github.alexmodguy.alexscaves.server.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * An item an anvil accepts extra materials for, on top of whatever its tier already allows.
 *
 * <p>Up to 1.21.1 that was {@code Item#isValidRepairItem}, a predicate each item answered — and
 * three of the five implementors deferred to {@code super} for their tier's own material. 1.21.2
 * deleted the hook for a {@code minecraft:repairable} data component holding a {@code HolderSet} of
 * items, which the tool material fills in at construction, so the mod's extras are merged into
 * whatever is already there rather than replacing it (see
 * {@code AlexsCaves#modifyDefaultComponents} on NeoForge, {@code AlexsCaves#gatherItemComponents}
 * on Forge).
 *
 * <p>The materials are returned as resolved {@code Item}s, so this must not be called before item
 * registration has finished; both callers run well after it.
 */
public interface ACRepairableItem {

    Item[] acExtraRepairMaterials();

    /**
     * Whether {@link #acExtraRepairMaterials()} is the item's whole answer, rather than an addition
     * to what its tier already accepts. Only the desolate dagger says yes: its {@code <1.21.2}
     * override returned a bare {@code repairWith.is(PURE_DARKNESS)} with no {@code super} call, so a
     * diamond has never repaired it and merging would quietly change that.
     */
    default boolean acReplacesTierRepairMaterials() {
        return false;
    }

    /** The {@code <1.21.2} half of the same question, shared by the implementations. */
    static boolean isExtraRepairMaterial(ACRepairableItem item, ItemStack repairWith) {
        for (Item material : item.acExtraRepairMaterials()) {
            if (repairWith.is(material)) {
                return true;
            }
        }
        return false;
    }
}
