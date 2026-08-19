package com.github.alexmodguy.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.item.ACTickingItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives {@link ACTickingItem} from the head of {@code ItemStack#inventoryTick}.
 *
 * <p>See {@link ACTickingItem} for why the mod's ticking items no longer ride
 * {@code Item#inventoryTick}: 1.21.5 made that hook server-only. This is the same call site vanilla
 * itself uses to reach the item, so an implementor is ticked exactly when it always was — once per
 * player-inventory slot per tick, on both logical sides — with {@code selected} meaning what it
 * always did, "this is the stack in the main hand".
 *
 * <p>Only the signature moves: below 1.21.5 the slot arrives as an index plus that flag, from 1.21.5
 * as a nullable {@code EquipmentSlot} that is {@code MAINHAND} for the selected slot and null
 * otherwise.
 */
@Mixin(ItemStack.class)
public class ItemStackTickMixin {

    //? if >=1.21.5 {
    /*@Inject(method = "inventoryTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EquipmentSlot;)V", at = @At("HEAD"))
    private void ac_inventoryTick(Level level, Entity entity, net.minecraft.world.entity.EquipmentSlot slot, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() instanceof ACTickingItem ticking) {
            ticking.acInventoryTick(stack, level, entity, slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        }
    }
    *///?} else {
    @Inject(method = "inventoryTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;IZ)V", at = @At("HEAD"))
    private void ac_inventoryTick(Level level, Entity entity, int slot, boolean selected, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() instanceof ACTickingItem ticking) {
            ticking.acInventoryTick(stack, level, entity, selected);
        }
    }
    //?}
}
