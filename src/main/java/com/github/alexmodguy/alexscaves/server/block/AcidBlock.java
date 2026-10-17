package com.github.alexmodguy.alexscaves.server.block;

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.client.particle.ACParticleRegistry;
import com.github.alexmodguy.alexscaves.server.block.fluid.ACFluidRegistry;
import com.github.alexmodguy.alexscaves.server.entity.living.RadgillEntity;
import com.github.alexmodguy.alexscaves.server.item.HazmatArmorItem;
import com.github.alexmodguy.alexscaves.server.message.WorldEventMessage;
import com.github.alexmodguy.alexscaves.server.misc.*;
import com.google.common.collect.Maps;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;
import java.util.function.Supplier;

import java.util.Map;
import com.github.alexmodguy.alexscaves.server.misc.ACFluids;

public class AcidBlock extends LiquidBlock {

    private static Map<Block, Block> CORRODES_INTERACTIONS;

    // The deferred-supplier LiquidBlock constructor is a Forge patch, so it exists on exactly the
    // nodes that have Forge's patch: NeoForge dropped it in 1.20.5 and Fabric never had it at all
    // (vanilla's takes the FlowingFluid itself, on every version in the range). Where it is missing
    // the fluid is resolved here, which is safe: FLUID sits above BLOCK in BuiltInRegistries and the
    // registrations run in that order — on Fabric that order is the flush order in the AlexsCaves
    // constructor, which puts ACFluidRegistry ahead of ACBlockRegistry for this very reason. The
    // signature stays a Supplier on every node, so ACBlockRegistry reads the same.
    //? if fabric || (neoforge && >=1.20.5) {
    /*public AcidBlock(Supplier<FlowingFluid> flowingFluid, BlockBehaviour.Properties properties) {
        super(flowingFluid.get(), properties);
    }
    *///?} else {
    public AcidBlock(Supplier<FlowingFluid> flowingFluid, BlockBehaviour.Properties properties) {
        super(flowingFluid, properties);
    }
    //?}

    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource randomSource) {
        if (randomSource.nextInt(400) == 0) {
            level.playLocalSound((double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, ACSoundRegistry.ACID_IDLE.get(), SoundSource.BLOCKS, 0.5F, randomSource.nextFloat() * 0.4F + 0.8F, false);
        }
        boolean top = level.getFluidState(pos.above()).isEmpty();
        if (randomSource.nextInt(top ? 10 : 40) == 0) {
            float height = top ? state.getFluidState().getHeight(level, pos) : randomSource.nextFloat();
            level.addParticle(ACParticleRegistry.ACID_BUBBLE.get(), pos.getX() + randomSource.nextFloat(), pos.getY() + height, pos.getZ() + randomSource.nextFloat(), (randomSource.nextFloat() - 0.5F) * 0.1F, 0.05F + randomSource.nextFloat() * 0.1F, (randomSource.nextFloat() - 0.5F) * 0.1F);
        }
    }

    public void entityInside(BlockState blockState, Level level, BlockPos pos, Entity entity) {
        if (!entity.getType().builtInRegistryHolder().is(ACTagRegistry.RESISTS_ACID) && ACFluids.acidHeight(entity) > 0.1) {
            boolean armor = false;
            boolean hurtSound = false;
            float dmgMultiplier = 1.0F;
            if (entity instanceof LivingEntity living) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (slot.isArmor()) {
                        ItemStack item = living.getItemBySlot(slot);
                        if (item != null && item.isDamageableItem() && !(item.getItem() instanceof HazmatArmorItem)) {
                            armor = true;
                            if (living.getRandom().nextFloat() < 0.05F && !(entity instanceof Player player && player.isCreative())) {
                                ACCompat.hurtAndBreak(item, 1, living, slot);
                            }
                        }
                    }
                }
                dmgMultiplier = 1.0F - (HazmatArmorItem.getWornAmount(living) / 4F);
            }
            if (armor) {
                ACAdvancementTriggerRegistry.ENTER_ACID_WITH_ARMOR.triggerForEntity(entity);
            }
            if (level.getRandom().nextFloat() < dmgMultiplier) {
                float golemAddition = entity.getType().builtInRegistryHolder().is(ACTagRegistry.WEAK_TO_ACID) ? 10.0F : 0.0F;
                hurtSound = ACCompat.hurt(entity, ACDamageTypes.causeAcidDamage(level.registryAccess()), dmgMultiplier * (float) (armor ? 0.01D : 1.0D) + golemAddition);
            }
            if (hurtSound) {
                entity.playSound(ACSoundRegistry.ACID_BURN.get());
            }
        }
        if (entity instanceof LivingEntity && entity.moveDist > entity.nextStep && !(entity instanceof RadgillEntity)) {
            entity.nextStep = entity.moveDist + 1F;
            Vec3 vec3 = entity.getDeltaMovement();
            float f1 = Math.min(1.0F, (float)vec3.length());
            entity.playSound(ACSoundRegistry.ACID_SWIM.get(), f1, 1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.4F);
        }
    }

    public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState state2, boolean isMoving) {
        super.onPlace(state, worldIn, pos, state2, isMoving);
        tickCorrosion(worldIn, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
        tickCorrosion(worldIn, pos);
    }

    public void tickCorrosion(Level worldIn, BlockPos pos) {
        initCorrosion();
        for (Direction direction : ACMath.HORIZONTAL_DIRECTIONS) {
            BlockPos offset = pos.relative(direction);
            BlockState state1 = worldIn.getBlockState(offset);
            if (CORRODES_INTERACTIONS.containsKey(state1.getBlock())) {
                AlexsCaves.sendMSGToAll(new WorldEventMessage(0, offset.getX(), offset.getY(), offset.getZ()));
                BlockState transform = CORRODES_INTERACTIONS.get(state1.getBlock()).defaultBlockState();
                for (Property prop : state1.getProperties()) {
                    transform = transform.hasProperty(prop) ? transform.setValue(prop, state1.getValue(prop)) : transform;
                }
                worldIn.setBlockAndUpdate(offset, transform);
                Vec3 vec3 = Vec3.atCenterOf(offset);
                Player player = worldIn.getNearestPlayer(vec3.x, vec3.y, vec3.z, 8, false);
                if (player != null) {
                    ACAdvancementTriggerRegistry.ACID_CREATE_RUST.triggerForEntity(player);
                }
            }
        }
    }

    public static boolean doesBlockCorrode(BlockState state) {
        initCorrosion();
        return CORRODES_INTERACTIONS.containsKey(state.getBlock());
    }

    private static void initCorrosion() {
        if (CORRODES_INTERACTIONS != null) {
            return;
        }
        CORRODES_INTERACTIONS = Util.make(Maps.newHashMap(), (map) -> {
            // 26.2 collapsed the sixteen weathering-copper constants on Blocks into four
            // WeatheringCopperCollection<Block> holders, one per shape, each addressed
            // .weathering().unaffected() / .exposed() / .weathered() / .oxidized(). Same twelve
            // blocks and the same twelve pairs; only the spelling of each constant moves, which is
            // why this is an arm rather than a rename rule — Blocks.CUT_COPPER is a prefix of
            // Blocks.CUT_COPPER_SLAB and a substring rule could not tell them apart.
            //
            // ⚠️ The PAIRING is upstream's and is reproduced verbatim, including the fact that it
            // runs unaffected → weathered → exposed → oxidized: it swaps vanilla's middle two
            // stages, so acid has aged copper in the wrong order in every version of this mod.
            // Correcting it here would be a gameplay change smuggled into a port.
            //? if <26.2 {
            map.put(Blocks.COPPER_BLOCK, Blocks.WEATHERED_COPPER);
            map.put(Blocks.WEATHERED_COPPER, Blocks.EXPOSED_COPPER);
            map.put(Blocks.EXPOSED_COPPER, Blocks.OXIDIZED_COPPER);
            map.put(Blocks.CUT_COPPER, Blocks.WEATHERED_CUT_COPPER);
            map.put(Blocks.WEATHERED_CUT_COPPER, Blocks.EXPOSED_CUT_COPPER);
            map.put(Blocks.EXPOSED_CUT_COPPER, Blocks.OXIDIZED_CUT_COPPER);
            map.put(Blocks.CUT_COPPER_SLAB, Blocks.WEATHERED_CUT_COPPER_SLAB);
            map.put(Blocks.WEATHERED_CUT_COPPER_SLAB, Blocks.EXPOSED_CUT_COPPER_SLAB);
            map.put(Blocks.EXPOSED_CUT_COPPER_SLAB, Blocks.OXIDIZED_CUT_COPPER_SLAB);
            map.put(Blocks.CUT_COPPER_STAIRS, Blocks.WEATHERED_CUT_COPPER_STAIRS);
            map.put(Blocks.WEATHERED_CUT_COPPER_STAIRS, Blocks.EXPOSED_CUT_COPPER_STAIRS);
            map.put(Blocks.EXPOSED_CUT_COPPER_STAIRS, Blocks.OXIDIZED_CUT_COPPER_STAIRS);
            //?} else {
            /*var block = Blocks.COPPER_BLOCK.weathering();
            var cut = Blocks.CUT_COPPER.weathering();
            var slab = Blocks.CUT_COPPER_SLAB.weathering();
            var stairs = Blocks.CUT_COPPER_STAIRS.weathering();
            map.put(block.unaffected(), block.weathered());
            map.put(block.weathered(), block.exposed());
            map.put(block.exposed(), block.oxidized());
            map.put(cut.unaffected(), cut.weathered());
            map.put(cut.weathered(), cut.exposed());
            map.put(cut.exposed(), cut.oxidized());
            map.put(slab.unaffected(), slab.weathered());
            map.put(slab.weathered(), slab.exposed());
            map.put(slab.exposed(), slab.oxidized());
            map.put(stairs.unaffected(), stairs.weathered());
            map.put(stairs.weathered(), stairs.exposed());
            map.put(stairs.exposed(), stairs.oxidized());
            *///?}
            map.put(ACBlockRegistry.SCRAP_METAL.get(), ACBlockRegistry.RUSTY_SCRAP_METAL.get());
            map.put(ACBlockRegistry.SCRAP_METAL_PLATE.get(), ACBlockRegistry.RUSTY_SCRAP_METAL_PLATE.get());
            map.put(ACBlockRegistry.METAL_BARREL.get(), ACBlockRegistry.RUSTY_BARREL.get());
            map.put(ACBlockRegistry.METAL_SCAFFOLDING.get(), ACBlockRegistry.RUSTY_SCAFFOLDING.get());
            map.put(ACBlockRegistry.METAL_REBAR.get(), ACBlockRegistry.RUSTY_REBAR.get());
        });
    }
}
