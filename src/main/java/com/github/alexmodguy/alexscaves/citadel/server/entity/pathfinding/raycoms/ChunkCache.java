package com.github.alexmodguy.alexscaves.citadel.server.entity.pathfinding.raycoms;
/*
    All of this code is used with permission from Raycoms, one of the developers of the minecolonies project.
 */

import com.github.alexmodguy.alexscaves.server.misc.ACCompat;
import com.github.alexmodguy.alexscaves.citadel.server.world.WorldChunkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;


public class ChunkCache implements LevelReader {
    /**
     * Dimensiontype.
     */
    private final DimensionType dimType;
    protected int chunkX;
    protected int chunkZ;
    protected LevelChunk[][] chunkArray;
    /**
     * set by !chunk.getAreLevelsEmpty
     */
    protected boolean empty;
    /**
     * Reference to the World object.
     */
    protected Level world;

    private final int minBuildHeight;
    private final int maxBuildHeight;

    public ChunkCache(Level worldIn, BlockPos posFromIn, BlockPos posToIn, int subIn, final DimensionType type) {
        this.world = worldIn;
        this.chunkX = posFromIn.getX() - subIn >> 4;
        this.chunkZ = posFromIn.getZ() - subIn >> 4;
        int i = posToIn.getX() + subIn >> 4;
        int j = posToIn.getZ() + subIn >> 4;
        this.chunkArray = new LevelChunk[i - this.chunkX + 1][j - this.chunkZ + 1];
        this.empty = true;

        for (int k = this.chunkX; k <= i; ++k) {
            for (int l = this.chunkZ; l <= j; ++l) {
                if (WorldChunkUtil.isEntityChunkLoaded(world, new ChunkPos(k, l)) && worldIn.getChunkSource() instanceof ServerChunkCache serverChunkCache) {
                    final ChunkHolder holder = serverChunkCache.chunkMap.getVisibleChunkIfPresent(ACCompat.chunkAsLong(k, l));
                    if (holder != null) {
                        this.chunkArray[k - this.chunkX][l - this.chunkZ] = holder.getFullChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).left().orElse(null);
                    }
                }
            }
        }
        this.dimType = type;

        minBuildHeight = worldIn.getMinBuildHeight();
        maxBuildHeight = worldIn.getMaxBuildHeight();
    }

    /**
     * set by !chunk.getAreLevelsEmpty
     *
     * @return if so.
     */
    public boolean isEmpty() {
        return this.empty;
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(@NotNull BlockPos pos) {
        return this.getTileEntity(pos, LevelChunk.EntityCreationType.CHECK); // Forge: don't modify world from other threads
    }

    @Nullable
    public BlockEntity getTileEntity(BlockPos pos, LevelChunk.EntityCreationType createType) {
        int i = (pos.getX() >> 4) - this.chunkX;
        int j = (pos.getZ() >> 4) - this.chunkZ;
        if (!withinBounds(i, j)) {
            return null;
        }
        return this.chunkArray[i][j].getBlockEntity(pos, createType);
    }

    // These two are the only DECLARATIONS of the build-height accessors in the tree, so they
    // cannot ride the !mc2102-min/maxbuildheight replacement rules — a textual expansion of the
    // exclusive max would land in the method's own signature. Gated by hand instead. The
    // maxBuildHeight field keeps its exclusive meaning on every version; 1.21.2's getMaxY() is
    // inclusive, hence the -1, and the rule's +1 at each call site cancels it back out.
    //? if >=1.21.2 {
    /*@Override
    public int getMinY() {
        return minBuildHeight;
    }

    @Override
    public int getMaxY() {
        return maxBuildHeight - 1;
    }
    *///?} else {
    @Override
    public int getMinBuildHeight() {
        return minBuildHeight;
    }

    @Override
    public int getMaxBuildHeight() {
        return maxBuildHeight;
    }
    //?}

    @NotNull
    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (pos.getY() >= getMinBuildHeight() && pos.getY() < getMaxBuildHeight()) {
            int i = (pos.getX() >> 4) - this.chunkX;
            int j = (pos.getZ() >> 4) - this.chunkZ;

            if (i >= 0 && i < this.chunkArray.length && j >= 0 && j < this.chunkArray[i].length) {
                LevelChunk chunk = this.chunkArray[i][j];

                if (chunk != null) {
                    return chunk.getBlockState(pos);
                }
            }
        }

        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(final BlockPos pos) {
        if (pos.getY() >= getMinBuildHeight() && pos.getY() < getMaxBuildHeight()) {
            int i = (pos.getX() >> 4) - this.chunkX;
            int j = (pos.getZ() >> 4) - this.chunkZ;

            if (i >= 0 && i < this.chunkArray.length && j >= 0 && j < this.chunkArray[i].length) {
                LevelChunk chunk = this.chunkArray[i][j];

                if (chunk != null) {
                    return chunk.getFluidState(pos);
                }
            }
        }

        return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(final int x, final int y, final int z) {
        return null;
    }

    /**
     * Checks to see if an air block exists at the provided location. Note that this only checks to see if the blocks material is set to air, meaning it is possible for non-vanilla
     * blocks to still pass this check.
     */
    @Override
    public boolean isEmptyBlock(BlockPos pos) {
        BlockState state = this.getBlockState(pos);
        return state.isAir();
    }

    @Nullable
    @Override
    public ChunkAccess getChunk(final int x, final int z, final ChunkStatus requiredStatus, final boolean nonnull) {
        int i = x - this.chunkX;
        int j = z - this.chunkZ;

        if (i >= 0 && i < this.chunkArray.length && j >= 0 && j < this.chunkArray[i].length) {
            return this.chunkArray[i][j];
        }
        return null;
    }

    @Override
    public boolean hasChunk(final int chunkX, final int chunkZ) {
        return false;
    }

    @Override
    public BlockPos getHeightmapPos(final Heightmap.Types heightmapType, final BlockPos pos) {
        return null;
    }

    @Override
    public int getHeight(final Heightmap.Types heightmapType, final int x, final int z) {
        return 0;
    }

    @Override
    public int getSkyDarken() {
        return 0;
    }

    @Override
    public BiomeManager getBiomeManager() {
        return null;
    }

    @Override
    public WorldBorder getWorldBorder() {
        return null;
    }

    @Override
    public boolean isUnobstructed(@Nullable final Entity entityIn, final VoxelShape shape) {
        return false;
    }

    @Override
    public List<VoxelShape> getEntityCollisions(@org.jetbrains.annotations.Nullable final Entity p_186427_, final AABB p_186428_) {
        return null;
    }

    @Override
    public int getDirectSignal(BlockPos pos, Direction direction) {
        return this.getBlockState(pos).getDirectSignal(this, pos, direction);
    }

    @Override
    public RegistryAccess registryAccess() {
        return RegistryAccess.EMPTY;
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return FeatureFlagSet.of();
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public @NotNull DimensionType dimensionType() {
        return dimType;
    }

    // 1.21.11 made every LevelReader answer for its environment attributes — the fog/sky/music/light
    // block that replaced DimensionSpecialEffects and the biome getters. This cache is a read-only
    // window onto one level's chunks, so it forwards to that level rather than reporting EMPTY: a
    // pathfinding job asks nothing of it today, but an empty reader would silently answer with
    // defaults if one ever did.
    //? if >=1.21.11 {
    /*@Override
    public net.minecraft.world.attribute.EnvironmentAttributeReader environmentAttributes() {
        return this.world.environmentAttributes();
    }
    *///?}

    private boolean withinBounds(int x, int z) {
        return x >= 0 && x < chunkArray.length && z >= 0 && z < chunkArray[x].length && chunkArray[x][z] != null;
    }

    // 26 took BlockAndTintGetter out of LevelReader's hierarchy: it moved to the client package
    // (net.minecraft.client.renderer.block) and LevelReader now extends the narrower
    // BlockAndLightGetter, which keeps getLightEngine but not getShade — and on the interface that
    // did keep it, getShade was replaced by cardinalLighting(). So the override has no supertype to
    // implement and simply goes away here rather than being translated. That costs nothing: this is
    // a pathfinding window onto one level's chunks, never a render view, and nothing in the tree
    // asks a ChunkCache for face shading.
    //? if <26 {
    @Override
    public float getShade(final Direction direction, final boolean b) {
        return 0;
    }
    //?}

    @Override
    public LevelLightEngine getLightEngine() {
        return null;
    }
}
