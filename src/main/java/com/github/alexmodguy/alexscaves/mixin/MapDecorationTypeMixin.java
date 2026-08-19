package com.github.alexmodguy.alexscaves.mixin;

import net.minecraft.world.level.saveddata.maps.MapDecoration;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.ArrayList;
import java.util.Arrays;

@Mixin(MapDecoration.Type.class)
@Unique
public class MapDecorationTypeMixin {

    @Shadow
    @Final
    @Mutable
    private static MapDecoration.Type[] $VALUES;

    private static final MapDecoration.Type AC_UNDERGROUND_CABIN = ac_addType("AC_UNDERGROUND_CABIN", true, 0X6B6B6B, false);

    //? if <1.20.2 {
    @Invoker("<init>")
    public static MapDecoration.Type ac_invokeInit(String internalName, int internalId, boolean renderOnFrame, int mapColor, boolean trackCount) {
        throw new AssertionError();
    }
    //?}

    // 1.20.2 gave the enum a serialised name — it became StringRepresentable so map decorations could
    // be written by name rather than by icon index — and an isExplorationMapElement flag, both ahead
    // of the fields that were already there.
    //? if >=1.20.2 {
    /*@Invoker("<init>")
    public static MapDecoration.Type ac_invokeInit(String internalName, int internalId, String serializedName, boolean renderOnFrame, int mapColor, boolean explorationMapElement, boolean trackCount) {
        throw new AssertionError();
    }
    *///?}

    private static MapDecoration.Type ac_addType(String internalName, boolean renderOnFrame, int mapColor, boolean trackCount) {
        ArrayList<MapDecoration.Type> variants = new ArrayList<MapDecoration.Type>(Arrays.asList($VALUES));
        int ordinal = variants.get(variants.size() - 1).ordinal() + 1;
        //? if <1.20.2
        MapDecoration.Type instrument = ac_invokeInit(internalName, ordinal, renderOnFrame, mapColor, trackCount);
        //? if >=1.20.2
        /*MapDecoration.Type instrument = ac_invokeInit(internalName, ordinal, internalName.toLowerCase(java.util.Locale.ROOT), renderOnFrame, mapColor, false, trackCount);*/
        variants.add(instrument);
        MapDecorationTypeMixin.$VALUES = variants.toArray(new MapDecoration.Type[0]);
        return instrument;
    }
}
