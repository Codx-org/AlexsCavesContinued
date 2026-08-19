package com.github.alexmodguy.alexscaves.mixin.client;

import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.google.common.collect.ImmutableList;
//? if <1.20.2
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
//? if >=1.20.2
/*import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;*/
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.sources.PalettedPermutations;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

// 1.20.2 split the old SpriteResourceLoader in two: the name stayed on a new interface that loads a
// single sprite's contents, while the atlas' source list — the `sources` field this mixin is after —
// moved to SpriteSourceList. Both spell the factory `load(ResourceManager, ResourceLocation)`, so
// only the class being mixed into and the returned type differ; the return value is read through a
// raw CallbackInfoReturnable so one handler covers both.
//? if <1.20.2
@Mixin(SpriteResourceLoader.class)
//? if >=1.20.2
/*@Mixin(SpriteSourceList.class)*/
public abstract class SpriteResourceLoaderMixin {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "load",
            at = @At("RETURN"))
    private static void ac_load(ResourceManager resourceManager, ResourceLocation location, CallbackInfoReturnable cir) {
        if (location.getPath().equals("armor_trims")) {
            Object ret = cir.getReturnValue();
            for (SpriteSource source : ((SpriteResourceLoaderMixin) ret).getSources()) {
                if (source instanceof PalettedPermutationsAccessor permutations && permutations.getPaletteKey().getPath().equals("trims/color_palettes/trim_palette")) {
                    ResourceLocation trimLocation = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "trims/models/armor/polarity");
                    ResourceLocation leggingsTrimLocation = ResourceLocation.fromNamespaceAndPath(AlexsCaves.MODID, "trims/models/armor/polarity").withSuffix("_leggings");
                    permutations.setTextures(ImmutableList.<ResourceLocation>builder().addAll(permutations.getTextures()).add(trimLocation, leggingsTrimLocation).build());
                }
            }
        }
    }

    @Accessor("sources")
    abstract List<SpriteSource> getSources();

    @Mixin(PalettedPermutations.class)
    private interface PalettedPermutationsAccessor {

        @Accessor
        List<ResourceLocation> getTextures();

        @Accessor("textures")
        @Mutable
        void setTextures(List<ResourceLocation> value);

        @Accessor
        ResourceLocation getPaletteKey();
    }
}
