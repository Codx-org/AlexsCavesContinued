package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.entity.item.BurrowingArrowEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BurrowingArrowItem extends ArrowItem {
    public BurrowingArrowItem() {
        super(new Properties());
    }

    // 1.21 gave createArrow a fourth argument -- the weapon the arrow was fired from. Neither
    // arm reads it, but an ungated three-argument version overrides nothing from 1.21 up, so
    // vanilla builds a plain arrow and the custom behaviour never happens.
    //? if >=1.21 {
    /*@Override
    public AbstractArrow createArrow(Level level, ItemStack itemStack, LivingEntity livingEntity, ItemStack weapon) {
    *///?} else {
    @Override
    public AbstractArrow createArrow(Level level, ItemStack itemStack, LivingEntity livingEntity) {
    //?}
        return new BurrowingArrowEntity(level, livingEntity);
    }
}
