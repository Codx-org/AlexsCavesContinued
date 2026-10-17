package com.github.alexmodguy.alexscaves.server.misc;

import codx.codxlib.api.CodxLib;

public class ACLoadedMods {

    private static boolean distantHorizonsLoaded;
    private static boolean entityCullingLoaded;

    public static void afterAllModsLoaded(){
        // CodxLib.isModLoaded, not ModList.get().isLoaded — "which mods are installed" is answered
        // differently on every loader, and this is one of the few places Alex's Caves asks.
        distantHorizonsLoaded = CodxLib.isModLoaded("distanthorizons");
        entityCullingLoaded = CodxLib.isModLoaded("entityculling");
    }

    public static boolean isDistantHorizonsLoaded() {
        return distantHorizonsLoaded;
    }

    public static boolean isEntityCullingLoaded() {
        return entityCullingLoaded;
    }
}
