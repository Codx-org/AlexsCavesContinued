package com.github.alexmodguy.alexscaves.server.block.fluid;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.server.misc.ACPlatform;
import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.particle.ACParticleRegistry;
import com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class AcidFluidType extends FluidType implements ACClientExtensionFluidType {

    public static final ResourceLocation FLUID_STILL = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "block/acid_still");
    public static final ResourceLocation FLUID_FLOWING = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "block/acid_flowing");
    public static final ResourceLocation OVERLAY = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "textures/misc/under_acid.png");

    public AcidFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            // The two block-atlas sprites left this interface on NeoForge 26.1: a fluid's textures and
            // its chunk layer are one vanilla FluidModel now, registered from ClientProxy through
            // RegisterFluidModelsEvent, and both spellings read the very same FLUID_STILL/FLUID_FLOWING
            // constants below. Forge 62.0.9 still declares them here (its BakeFluidModels event takes a
            // baked model rather than an unbaked one), so this is a NeoForge-only deletion and the
            // overrides stay wherever they still override something. The overlay is unaffected — it is
            // a screen texture, not an atlas sprite, and survives on both loaders.
            //? if !neoforge || <26 {
            @Override
            public ResourceLocation getStillTexture() {
                return FLUID_STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLUID_FLOWING;
            }
            //?}

            @Override
            public ResourceLocation getRenderOverlayTexture(Minecraft mc) {
                return OVERLAY;
            }
        });
    }

    public boolean move(FluidState state, LivingEntity entity, Vec3 movementVector, double gravity) {
        double d9 = entity.getY();
        float f4 = 0.8F;
        float f5 = 0.02F;
        float f6 = ACCompat.depthStriderLevel(entity);
        double d0 = 0.08D;
        boolean flag = entity.getDeltaMovement().y <= 0.0D;
        if (f6 > 3.0F) {
            f6 = 3.0F;
        }

        if (!entity.onGround()) {
            f6 *= 0.5F;
        }

        if (f6 > 0.0F) {
            f4 += (0.54600006F - f4) * f6 / 3.0F;
            f5 += (entity.getSpeed() - f5) * f6 / 3.0F;
        }

        if (entity.hasEffect(MobEffects.DOLPHINS_GRACE)) {
            f4 = 0.96F;
        }

        f5 *= (float) entity.getAttribute(ACCompat.attribute(ACPlatform.swimSpeedAttribute())).getValue();
        entity.moveRelative(f5, movementVector);
        entity.move(MoverType.SELF, entity.getDeltaMovement());
        Vec3 vec36 = entity.getDeltaMovement();
        if (entity.horizontalCollision && entity.onClimbable()) {
            vec36 = new Vec3(vec36.x, 0.2D, vec36.z);
        }

        entity.setDeltaMovement(vec36.multiply((double) f4, (double) 0.8F, (double) f4));
        Vec3 vec32 = entity.getFluidFallingAdjustedMovement(d0, flag, entity.getDeltaMovement());
        entity.setDeltaMovement(vec32);
        if (entity.horizontalCollision && entity.isFree(vec32.x, vec32.y + (double) 0.6F - entity.getY() + d9, vec32.z)) {
            entity.setDeltaMovement(vec32.x, (double) 0.3F, vec32.z);
        }
        return true;
    }

    @Override
    public boolean isVaporizedOnPlacement(Level level, BlockPos pos, FluidStack stack) {
        // 1.21.11 deleted DimensionType#ultraWarm; "does water boil away here" is the
        // WATER_EVAPORATES environment attribute now, read at the position so a biome can
        // differ from its dimension. This is verbatim what Forge's own FluidType default does.
        //? if >=1.21.11 {
        /*return level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES, pos);
        *///?} else {
        return level.dimensionType().ultraWarm();
        //?}
    }

    // 1.21.5 widened the vaporising entity from Player to LivingEntity on both loaders (getSound
    // already took a LivingEntity). Only Level#playSound still wants a Player, to know whose client
    // may skip the sound it already predicted.
    @Override
    //? if >=1.21.5
    /*public void onVaporize(@Nullable net.minecraft.world.entity.LivingEntity player, Level level, BlockPos pos, FluidStack stack) {*/
    //? if <1.21.5
    public void onVaporize(@Nullable Player player, Level level, BlockPos pos, FluidStack stack) {
        SoundEvent sound = this.getSound(player, level, pos, SoundActions.FLUID_VAPORIZE);
        //? if >=1.21.5
        /*level.playSound(player instanceof Player acPlayer ? acPlayer : null, pos, sound != null ? sound : SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.8F);*/
        //? if <1.21.5
        level.playSound(player, pos, sound != null ? sound : SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.8F);

        for (int l = 0; l < 8; ++l) {
            level.addAlwaysVisibleParticle(ACParticleRegistry.RADGILL_SPLASH.get(), (double) pos.getX() + Math.random(), (double) pos.getY() + Math.random(), (double) pos.getZ() + Math.random(), (Math.random() - 0.5F) * 0.25F, Math.random() * 0.25F, (Math.random() - 0.5F) * 0.25F);
        }
        level.setBlockAndUpdate(pos, ACBlockRegistry.UNREFINED_WASTE.get().defaultBlockState());
    }
}
