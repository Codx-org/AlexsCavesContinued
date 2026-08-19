package com.github.alexmodguy.alexscaves.server.misc;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

/**
 * The "underground cabin" marker the cabin treasure map is stamped with.
 *
 * <p>Two eras, and they are opposites. Up to 1.20.4 {@code MapDecoration.Type} was a hard enum with
 * no extension point, so the mod grew one: {@code MapDecorationTypeMixin} appends a constant to
 * {@code $VALUES} at class-init, and the marker is drawn by a hand-rolled quad in {@code ClientEvents}
 * because vanilla's renderer only knows how to index its own icon sheet.
 *
 * <p>1.20.5 made decoration types a registry — {@code MapDecorationType} carries its own sprite id
 * and vanilla looks that sprite up on the {@code map_decorations} atlas — so from there on the
 * marker is an ordinary registered object, the two mixins are switched off, and the custom rendering
 * goes with them. The sprite lives at {@code textures/map/decorations/underground_cabin.png},
 * contributed to the atlas by {@code assets/alexscaves/atlases/map_decorations.json}.
 *
 * <p>The field keeps its name across both eras — only its type differs — so the two call sites that
 * stamp a map ({@code VillagerUndergroundCabinMapTrade}, {@code CabinMapLootModifier}) read the same
 * on every node.
 */
public class ACVanillaMapUtil {

    //? if >=1.20.5 {
    /*public static final net.minecraftforge.registries.DeferredRegister<net.minecraft.world.level.saveddata.maps.MapDecorationType> DEF_REG =
            net.minecraftforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.MAP_DECORATION_TYPE, com.github.alexmodguy.alexscaves.AlexsCaves.MODID);

    private static final java.util.function.Supplier<net.minecraft.world.level.saveddata.maps.MapDecorationType> UNDERGROUND_CABIN_TYPE =
            DEF_REG.register("underground_cabin", () -> new net.minecraft.world.level.saveddata.maps.MapDecorationType(
                    ResourceLocation.fromNamespaceAndPath(com.github.alexmodguy.alexscaves.AlexsCaves.MODID, "underground_cabin"),
                    true,
                    0X6B6B6B,
                    false,
                    false));

    // The registry hands out a Holder for the same object; addTargetDecoration wants that, not the
    // value. Resolved lazily because the DeferredRegister has not run when this class is loaded.
    public static net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> undergroundCabin() {
        return net.minecraft.core.registries.BuiltInRegistries.MAP_DECORATION_TYPE.wrapAsHolder(UNDERGROUND_CABIN_TYPE.get());
    }
    *///?} else {
    public static final MapDecoration.Type UNDERGROUND_CABIN_MAP_DECORATION = MapDecoration.Type.valueOf("AC_UNDERGROUND_CABIN");

    public static MapDecoration.Type undergroundCabin() {
        return UNDERGROUND_CABIN_MAP_DECORATION;
    }

    public static byte getMapIconRenderOrdinal(MapDecoration.Type type) {
        return (byte) (type == UNDERGROUND_CABIN_MAP_DECORATION ? 0 : -1);
    }
    //?}
}
