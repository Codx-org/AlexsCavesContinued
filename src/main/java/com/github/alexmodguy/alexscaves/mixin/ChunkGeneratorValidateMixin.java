package com.github.alexmodguy.alexscaves.mixin;

import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;

// MC 26.1 added ChunkGenerator#validate(), and on a singleplayer world it is what makes this mod's
// biomes crash chunk decoration.
//
// The whole body of validate() is `this.featuresPerStep.get(); return;` — byte-identical on 26.1,
// 26.1.1, 26.1.2 and 26.2, and on the Forge/NeoForge patched class too, where the field is a
// ClearableLazy rather than a memoised Supplier. Its only effect is therefore to FORCE the
// per-step feature index, which FeatureSorter builds from the generator's current biome set.
//
// The client calls it before the integrated server exists: WorldOpenFlows#openWorldLoadLevelStem
// loops over every LevelStem of the freshly-loaded WorldStem and validates its generator before
// Minecraft#doWorldLoad, and WorldCreationContext#validate does the same when a world is created.
// This mod's biomes are added to the biome source from ServerAboutToStartEvent (CommonEvents), so
// at validate() time the index is built WITHOUT them and then never rebuilt — nothing on any
// loader calls Forge's refreshFeaturesPerStep(). Decoration then asks
// stepFeatureData.indexMapping() for one of this mod's placed features, gets the map's -1 default
// and dies in ChunkGenerator#applyBiomeDecoration with
// `IndexOutOfBoundsException: Index -1 out of bounds for length N`, killing the chunk worker the
// moment a player reaches an Alex's Caves biome.
//
// Cancelling validate() lets the index memoise lazily inside the first applyBiomeDecoration
// instead, which runs on a chunk worker long after ServerAboutToStartEvent, so it sees the
// expanded biome set. The only thing given up is vanilla's early "feature order cycle" diagnosis
// at the world-load screen; the same throw still happens at the first decorated chunk.
//
// Dedicated servers never call validate() (MinecraftServer and ServerLevel do not), which is why
// every RCON boot of this matrix passed. The member does not exist below 26.1, so the handler
// lives in an arm and this is an empty (harmless) mixin on the other 46 nodes — the same shape as
// SurfaceRulesContextAccessor.
@Mixin(ChunkGenerator.class)
public class ChunkGeneratorValidateMixin {

    //? if >=26.1 {
    /*@org.spongepowered.asm.mixin.injection.Inject(method = "validate()V", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private void ac_skipValidate(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ci.cancel();
    }
    *///?}
}
