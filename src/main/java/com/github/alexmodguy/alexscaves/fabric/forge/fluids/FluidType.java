package com.github.alexmodguy.alexscaves.fabric.forge.fluids;

import com.github.alexmodguy.alexscaves.fabric.forge.common.SoundAction;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Fabric stand-in for the loader's per-fluid "type" object.
 *
 * <p>On the other two loaders a fluid type is the seam through which a modded fluid becomes a
 * first-class one: the loader patches {@code Entity} to track immersion in it, asks it how an entity
 * should swim, what it sounds like, and whether it boils away where it is placed. None of that
 * machinery exists here, and {@code ACFluids} explains at length why the <i>queries</i> did not need
 * it — vanilla has answered those by fluid tag the whole way back.
 *
 * <p>The type is nonetheless reproduced rather than gated out, and that is a deliberate choice with a
 * cost attached. Gating {@code server/block/fluid/} out of the Fabric source set would be a smaller
 * diff, but it would delete {@code AcidFluidType#move} — forty lines of acid swim physics that are
 * this mod's own code, not the loader's — along with the two vaporise hooks and the fluids' texture
 * bindings. Keeping the type means those bodies stay compiled, in one spelling, on all 58 nodes, and
 * the Fabric work reduces to <i>calling</i> them.
 *
 * <p>So what is here is the shape the mod's two subclasses fill in and the state their {@code
 * Properties} carry, and nothing else. The loader's real class has some thirty overridable hooks;
 * the seven below are the ones this tree either overrides or reads.
 *
 * <p>⚠️ <b>Nothing calls any of this yet.</b> A {@code FluidType} is reachable from a {@link Fluid}
 * through {@link #of}, but who asks — the mixin that would route an entity's motion in acid into
 * {@link #move}, and the placement path that would consult {@link #isVaporizedOnPlacement} — is
 * dispatcher work. Until then the physics are compiled and dormant: an entity in acid on this loader
 * moves the way vanilla moves it in an untracked fluid, which is the same gap NeoForge 26.1 opened on
 * its own side (see {@code ACFluids}).
 */
public class FluidType {

    /**
     * Vanilla water's type. Not a registered object anywhere — it exists so that the ten fluid
     * interactions in {@code ACFluidRegistry#postInit} can name water and lava the way they name this
     * mod's two fluids, keeping that table one spelling across all three loaders.
     */
    public static final FluidType WATER = new FluidType(Properties.create().density(1000).viscosity(1000));

    /** Vanilla lava's type — see {@link #WATER}. */
    public static final FluidType LAVA = new FluidType(Properties.create().lightLevel(15).density(3000).viscosity(6000));

    private final int lightLevel;
    private final int density;
    private final int viscosity;
    @Nullable
    private final BlockPathTypes pathType;
    @Nullable
    private final BlockPathTypes adjacentPathType;
    private final Map<SoundAction, SoundEvent> sounds;

    public FluidType(Properties properties) {
        this.lightLevel = properties.lightLevel;
        this.density = properties.density;
        this.viscosity = properties.viscosity;
        this.pathType = properties.pathType;
        this.adjacentPathType = properties.adjacentPathType;
        this.sounds = Map.copyOf(properties.sounds);
    }

    /**
     * The type of a fluid, or null when it has none — which on this loader means "any fluid but
     * water, lava and this mod's two". The loader's answer covers every modded fluid in the game;
     * this one cannot, because a fluid type is not a thing other Fabric mods have.
     */
    @Nullable
    public static FluidType of(Fluid fluid) {
        if (fluid instanceof ForgeFlowingFluid modded) {
            return modded.getFluidType();
        }
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
            return WATER;
        }
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
            return LAVA;
        }
        return null;
    }

    public int getLightLevel() {
        return lightLevel;
    }

    public int getDensity() {
        return density;
    }

    public int getViscosity() {
        return viscosity;
    }

    @Nullable
    public BlockPathTypes getPathType() {
        return pathType;
    }

    @Nullable
    public BlockPathTypes getAdjacentPathType() {
        return adjacentPathType;
    }

    /**
     * The sound this fluid makes for the given action, or null when it declares none.
     *
     * <p>One signature covers both of the mod's call shapes: the loader widened the entity from
     * {@code Player} to {@code LivingEntity} at 1.21.5 and this takes the wider of the two, which a
     * {@code Player} is. The level is taken as a {@code Level} rather than the loader's
     * {@code BlockGetter} for the same reason — every caller has one, and narrowing it would only
     * make the parameter list a second thing to keep in step.
     */
    @Nullable
    public SoundEvent getSound(@Nullable LivingEntity entity, Level level, BlockPos pos, SoundAction action) {
        return sounds.get(action);
    }

    /**
     * Moves an entity through this fluid, returning whether it handled the motion. False here means
     * "not mine" — the loader then falls back to its water or lava handling.
     */
    public boolean move(FluidState state, LivingEntity entity, Vec3 movementVector, double gravity) {
        return false;
    }

    /** Whether placing this fluid at the position boils it away instead. */
    public boolean isVaporizedOnPlacement(Level level, BlockPos pos, FluidStack stack) {
        return false;
    }

    // The gate mirrors AcidFluidType's exactly — the loader widened the vaporising entity from Player
    // to LivingEntity at 1.21.5, and an override only overrides when the descriptors agree.
    //? if >=1.21.5
    /*public void onVaporize(@Nullable LivingEntity player, Level level, BlockPos pos, FluidStack stack) {*/
    //? if <1.21.5
    public void onVaporize(@Nullable net.minecraft.world.entity.player.Player player, Level level, BlockPos pos, FluidStack stack) {
    }

    /** The builder the two fluid types are constructed with. */
    public static class Properties {

        private int lightLevel = 0;
        private int density = 1000;
        private int viscosity = 1000;
        @Nullable
        private BlockPathTypes pathType = BlockPathTypes.WATER;
        @Nullable
        private BlockPathTypes adjacentPathType;
        private final Map<SoundAction, SoundEvent> sounds = new HashMap<>();

        private Properties() {
        }

        public static Properties create() {
            return new Properties();
        }

        public Properties lightLevel(int lightLevel) {
            this.lightLevel = lightLevel;
            return this;
        }

        public Properties density(int density) {
            this.density = density;
            return this;
        }

        public Properties viscosity(int viscosity) {
            this.viscosity = viscosity;
            return this;
        }

        public Properties pathType(@Nullable BlockPathTypes pathType) {
            this.pathType = pathType;
            return this;
        }

        public Properties adjacentPathType(@Nullable BlockPathTypes adjacentPathType) {
            this.adjacentPathType = adjacentPathType;
            return this;
        }

        public Properties sound(SoundAction action, SoundEvent sound) {
            this.sounds.put(action, sound);
            return this;
        }
    }
}
