package com.github.alexmodguy.alexscaves.server.misc;

import com.github.alexmodguy.alexscaves.citadel.animation.Animation;
import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class ACMath {

    public static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    public static final Direction[] NOT_UP_DIRECTIONS = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.DOWN};
    public static final float HALF_SQRT_3 = (float) (Math.sqrt(3.0D) / 2.0D);

    public static final float QUARTER_PI = ((float)Math.PI / 4F);

    public static float smin(float a, float b, float k) {
        float h = Math.max(k - Math.abs(a - b), 0.0F) / k;
        return Math.min(a, b) - h * h * k * (1.0F / 4.0F);
    }

    public static float cullAnimationTick(int tick, float amplitude, Animation animation, float partialTick, int startOffset) {
        return cullAnimationTick(tick, amplitude, animation, partialTick, startOffset, animation.getDuration() - startOffset);
    }

    public static float cullAnimationTick(int tick, float amplitude, Animation animation, float partialTick, int startOffset, int endAt) {
        float i = Mth.clamp(tick + partialTick - startOffset, 0, endAt);
        float f = (float) Math.sin((i / (float) (endAt)) * Math.PI) * amplitude;
        return ACMath.smin(f, 1.0F, 0.1F);
    }

    public static float sampleNoise2D(int x, int z, float simplexSampleRate) {
        return (float) ((ACSimplexNoise.noise((x + simplexSampleRate) / simplexSampleRate, (z + simplexSampleRate) / simplexSampleRate)));
    }

    public static float sampleNoise3D(int x, int y, int z, float simplexSampleRate) {
        return (float) ((ACSimplexNoise.noise((x + simplexSampleRate) / simplexSampleRate, (y + simplexSampleRate) / simplexSampleRate, (z + simplexSampleRate) / simplexSampleRate)));
    }

    public static float sampleNoise3D(float x, float y, float z, float simplexSampleRate) {
        return (float) ((ACSimplexNoise.noise((x + simplexSampleRate) / simplexSampleRate, (y + simplexSampleRate) / simplexSampleRate, (z + simplexSampleRate) / simplexSampleRate)));
    }

    public static VoxelShape buildShape(VoxelShape... from) {
        return Stream.of(from).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    }

    public static float walkValue(float limbSwing, float limbSwingAmount, float speed, float offset, float degree, boolean inverse) {
        return (float) ((Math.cos(limbSwing * speed + offset) * degree * limbSwingAmount) * (inverse ? -1 : 1));
    }

    public static float approachRotation(float current, float target, float max) {
        float f = Mth.wrapDegrees(target - current);
        if (f > max) {
            f = max;
        }

        if (f < -max) {
            f = -max;
        }

        return Mth.wrapDegrees(current + f);
    }

    public static Vec3 getGroundBelowPosition(BlockGetter level, Vec3 in) {
        BlockPos pos = BlockPos.containing(in);
        while (pos.getY() > level.getMinBuildHeight() && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            pos = pos.below();
        }
        float top;
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty()) {
            top = 0.0F;
        } else {
            Vec3 modIn = new Vec3(in.x % 1.0D, 1.0D, in.z % 1.0D);
            Optional<Vec3> closest = shape.closestPointTo(modIn);
            top = closest.isPresent() ? (float) closest.get().y : 0.0F;
        }
        return Vec3.upFromBottomCenterOf(pos, top);
    }

    public static Vec3 readVec3(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static FriendlyByteBuf writeVec3(FriendlyByteBuf buf, Vec3 vec3) {
        buf.writeDouble(vec3.x());
        buf.writeDouble(vec3.y());
        buf.writeDouble(vec3.z());
        return buf;
    }

    public static float approachDegreesNoWrap(float from, float to, float by) {
        float f = (to - from)  % 360.0F;
        return Mth.approach(from, from + f, by);
    }
    public static float canyonStep(float heightScale, int scaleTo) {
        int clampTo100 = (int) ((heightScale) * scaleTo * scaleTo);
        return Mth.clamp((float) (Math.round(clampTo100 / (float) scaleTo)) / (float) scaleTo, 0F, 1F);
    }

    /**
     * A biome lookup with the sampler already bound, so a loop pays for the binding once rather
     * than per cell.
     *
     * <p>26.3 pulled {@code BiomeSource} and {@code BiomeResolver} apart. A source no longer
     * answers {@code getNoiseBiome} itself — it hands out a resolver through
     * {@code createResolver(Climate.Sampler)}, and that resolver's lookup drops the sampler
     * argument it no longer needs. Below 26.3 a {@code BiomeSource} <i>is</i> a
     * {@code BiomeResolver} and carries the sampler on every call. Both shapes fit behind this
     * one interface, which is why the eight call sites in this tree stayed version-neutral and
     * the whole split is expressed by the single gate in {@code biomeLookup} below.
     */
    @FunctionalInterface
    public interface ACBiomeLookup {
        Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ);
    }

    public static ACBiomeLookup biomeLookup(BiomeSource biomeSource, Climate.Sampler sampler) {
        //? if >=26.3 {
        /*net.minecraft.world.level.biome.BiomeResolver resolver = biomeSource.createResolver(sampler);
        return resolver::getNoiseBiome;
        *///?} else {
        return (quartX, quartY, quartZ) -> biomeSource.getNoiseBiome(quartX, quartY, quartZ, sampler);
        //?}
    }

    /**
     * One-shot form of {@code biomeLookup}, for the handful of sites that ask a single column.
     * Anything in a loop should hoist the lookup instead.
     */
    public static Holder<Biome> noiseBiomeAt(BiomeSource biomeSource, int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        return biomeLookup(biomeSource, sampler).getNoiseBiome(quartX, quartY, quartZ);
    }

    public static Set<Holder<Biome>> getBiomesWithinAtY(BiomeSource biomeSource, int x, int y, int z, int xzDist, Climate.Sampler sampler) {
        int i = QuartPos.fromBlock(x - xzDist);
        int j = QuartPos.fromBlock(y);
        int k = QuartPos.fromBlock(z - xzDist);
        int l = QuartPos.fromBlock(x + xzDist);
        int j1 = QuartPos.fromBlock(z + xzDist);
        int k1 = l - i + 1;
        int i2 = j1 - k + 1;
        Set<Holder<Biome>> set = Sets.newHashSet();
        ACBiomeLookup lookup = biomeLookup(biomeSource, sampler);

        for(int j2 = 0; j2 < i2; ++j2) {
            for(int k2 = 0; k2 < k1; ++k2) {
                int i3 = i + k2;
                int k3 = k + j2;
                set.add(lookup.getNoiseBiome(i3, j, k3));
            }
        }
        return set;
    }
}
