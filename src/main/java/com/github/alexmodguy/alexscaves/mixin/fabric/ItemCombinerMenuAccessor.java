package com.github.alexmodguy.alexscaves.mixin.fabric;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The three fields {@link AnvilMenuUpdateMixin} needs that {@code AnvilMenu} does not declare.
 *
 * <p>{@code inputSlots}, {@code resultSlots} and {@code player} all live on {@code ItemCombinerMenu},
 * and <b>a {@code @Shadow} or {@code @Accessor} resolves against the target class alone, with no
 * hierarchy walk</b> — Mixin throws <i>"was not located in the target class"</i> for an inherited
 * member — so an {@code AnvilMenu} mixin cannot reach them however visible Java thinks they are.
 * Hence this second, tiny mixin declared on the class that actually owns them.
 *
 * <p>All three descriptors are identical on every one of the 22 Fabric nodes ({@code Container},
 * {@code ResultContainer}, {@code Player}), so this needs no Stonecutter gate.
 */
@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {

    @Accessor("inputSlots")
    Container ac_getInputSlots();

    @Accessor("resultSlots")
    ResultContainer ac_getResultSlots();

    @Accessor("player")
    Player ac_getPlayer();
}
