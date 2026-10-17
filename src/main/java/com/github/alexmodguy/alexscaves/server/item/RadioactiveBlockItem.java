package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.message.UpdateEffectVisualityEntityMessage;
import com.github.alexmodguy.alexscaves.server.potion.ACEffectRegistry;
import com.github.alexmodguy.alexscaves.citadel.item.BlockItemWithSupplier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import java.util.function.Supplier;

public class RadioactiveBlockItem extends BlockItemWithSupplier implements ACTickingItem {

    private final float randomChanceOfRadiation;

    public RadioactiveBlockItem(Supplier<Block> blockSupplier, Properties props, float randomChanceOfRadiation) {
        super(blockSupplier, props);
        this.randomChanceOfRadiation = randomChanceOfRadiation;
    }

    public void acInventoryTick(ItemStack stack, Level level, Entity entity, boolean held) {
        if (!level.isClientSide() && entity instanceof LivingEntity living && !(living instanceof Player player && player.isCreative())) {
            float stackChance = stack.getCount() * randomChanceOfRadiation;
            float hazmatMultiplier = 1F - HazmatArmorItem.getWornAmount(living) / 4F;
            if (!living.hasEffect(ACCompat.effect(ACEffectRegistry.IRRADIATED.get())) && level.getRandom().nextFloat() < stackChance * hazmatMultiplier) {
                MobEffectInstance instance = new MobEffectInstance(ACCompat.effect(ACEffectRegistry.IRRADIATED.get()), 1800);
                living.addEffect(instance);
                AlexsCaves.sendMSGToAll(new UpdateEffectVisualityEntityMessage(entity.getId(), entity.getId(), 0, instance.getDuration()));
            }
        }
    }


}
