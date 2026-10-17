package com.github.alexmodguy.alexscaves.mixin;

import com.github.alexmodguy.alexscaves.server.level.biome.BiomeSourceAccessor;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Mixin(BiomeSource.class)
public class BiomeSourceMixin implements BiomeSourceAccessor {

    @Shadow
    public Supplier<Set<Holder<Biome>>> possibleBiomes;
    private boolean expanded;
    private Map<ResourceKey<Biome>, Holder<Biome>> map = new HashMap<>();

    @Override
    public void setResourceKeyMap(Map<ResourceKey<Biome>, Holder<Biome>> map) {
        this.map = map;
    }

    @Override
    public Map<ResourceKey<Biome>, Holder<Biome>> getResourceKeyMap() {
        return map;
    }

    @Override
    public void expandBiomesWith(Set<Holder<Biome>> newGenBiomes) {
        if(!expanded){
            // Union LAZILY. This runs at ServerAboutToStart, earlier than vanilla ever asks for the
            // set (its first read is at level construction), and reading the old supplier here froze
            // the source's biome list before any later mod had finished adding to it. A biome that
            // misses possibleBiomes still generates, but applyBiomeDecoration retains it out of every
            // feature step and ChunkGeneratorStructureState drops its structure sets, so it comes out
            // bare: no vegetation, no structures, and nothing logged. Wrapping the previous supplier
            // defers that read to the first real caller, whatever has been chained in by then.
            Supplier<Set<Holder<Biome>>> previous = this.possibleBiomes;
            Set<Holder<Biome>> added = ImmutableSet.copyOf(newGenBiomes);
            possibleBiomes = Suppliers.memoize(() -> ImmutableSet.<Holder<Biome>>builder().addAll(previous.get()).addAll(added).build());
            expanded = true;
        }
    }

}
