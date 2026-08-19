package com.github.alexmodguy.alexscaves.server.item;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.enchantment.ACEnchantmentRegistry;
import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.item.SpinningPeppermintEntity;
import com.github.alexmodguy.alexscaves.server.entity.item.SugarStaffHexEntity;
import com.github.alexmodguy.alexscaves.server.misc.ACMath;
import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
//? if <1.21.2
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import com.github.alexmodguy.alexscaves.server.item.ACClientExtensionItem;

public class SugarStaffItem extends Item implements ACClientExtensionItem, ACEnchantableItem {

    public SugarStaffItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientItemExtensions> consumer) {
        consumer.accept((IClientItemExtensions) AlexsCaves.PROXY.getISTERProperties());
    }

    // 1.21.2 merged the result-plus-stack pair back into a plain InteractionResult.
    //? if >=1.21.2
    /*public net.minecraft.world.InteractionResult use(Level level, Player player, InteractionHand hand) {*/
    //? if <1.21.2
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if(!level.isClientSide()){
            boolean hex = player.isShiftKeyDown();
            player.swing(hand);
            Entity lookingAtEntity = SeaStaffItem.getClosestLookingAtEntityFor(level, player, 32);
            if(hex){
                Vec3 ground = ACMath.getGroundBelowPosition(player.level(), player.getEyePosition());
                SugarStaffHexEntity sugarStaffHexEntity = ACCompat.createEntity(ACEntityRegistry.SUGAR_STAFF_HEX.get(), player.level());
                sugarStaffHexEntity.setOwner(player);
                sugarStaffHexEntity.setPos(ground.x, ground.y, ground.z);
                sugarStaffHexEntity.setHexScale(1.0F + 0.25F * ACCompat.enchantLevel(itemstack, ACEnchantmentRegistry.HUMUNGOUS_HEX));
                level.addFreshEntity(sugarStaffHexEntity);
                level.playSound((Player)null, player.blockPosition(), ACSoundRegistry.SUGAR_STAFF_CAST_HEX.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                sugarStaffHexEntity.setLifespan(100 + 60 * ACCompat.enchantLevel(itemstack, ACEnchantmentRegistry.SPELL_LASTING));
                ACCompat.addCooldown(player, itemstack, 100);
            }else{
                int spawnIn = 3 + ACCompat.enchantLevel(itemstack, ACEnchantmentRegistry.MULTIPLE_MINT);
                boolean flag = false;
                int despawnTime = 80;
                for (int i = 0; i < spawnIn; i++) {
                    SpinningPeppermintEntity spinningPeppermintEntity = ACCompat.createEntity(ACEntityRegistry.SPINNING_PEPPERMINT.get(), player.level());
                    spinningPeppermintEntity.setPos(player.position().add(0, player.getBbHeight() * 0.45F, 0));
                    if(ACCompat.enchantLevel(itemstack, ACEnchantmentRegistry.PEPPERMINT_PUNTING) > 0){
                        spinningPeppermintEntity.setStraight(true);
                        spinningPeppermintEntity.setYRot(180 + player.getYHeadRot() + (i - 1) * 15);
                        spinningPeppermintEntity.setSpinSpeed(8F);
                        despawnTime = 20;
                    }else{
                        spinningPeppermintEntity.setStraight(false);
                        spinningPeppermintEntity.setYRot(180 + (i - 1) * 30);
                        spinningPeppermintEntity.setSpinSpeed(12F);
                    }
                    if(lookingAtEntity != null && ACCompat.enchantLevel(itemstack, ACEnchantmentRegistry.SEEKCANDY) > 0){
                        spinningPeppermintEntity.setSeekingEntityId(lookingAtEntity.getId());
                        spinningPeppermintEntity.setSpinSpeed(50F);
                        despawnTime = 50;
                    }
                    spinningPeppermintEntity.setSpinRadius(3.5F);
                    spinningPeppermintEntity.setOwner(player);
                    spinningPeppermintEntity.setStartAngle(i * 360 / (float) spawnIn);
                    spinningPeppermintEntity.setLifespan(80);
                    level.addFreshEntity(spinningPeppermintEntity);
                }
                level.playSound((Player)null, player.blockPosition(), ACSoundRegistry.SUGAR_STAFF_CAST_PEPPERMINT.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                ACCompat.addCooldown(player, itemstack, despawnTime);
            }
            ACCompat.hurtAndBreakUsedHand(itemstack, 1, player);
        }
        return ACCompat.useSidedSuccess(itemstack, level.isClientSide());
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.getCount() == 1;
    }

}
