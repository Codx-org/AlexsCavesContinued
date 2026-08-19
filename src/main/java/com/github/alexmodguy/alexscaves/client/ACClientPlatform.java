package com.github.alexmodguy.alexscaves.client;

import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * The client-side counterpart to {@code ACPlatform}: rendering APIs that moved between the
 * Minecraft versions this mod spans. Kept separate so nothing on a dedicated server can reach a
 * client-only class through it.
 *
 * <p>Two changes live here so far. 1.20.2 gathered a player's skin texture, cape, elytra texture
 * and arm model into a single {@code PlayerSkin} record and routed every lookup through it,
 * replacing the five separate getters Alex's Caves used; the same version split the advancement
 * tree into {@code Advancement} (the data) and {@code AdvancementNode} (its place in the tree), so
 * the widget and tab classes hold a node rather than an advancement.
 */
public class ACClientPlatform {

    /** The skin texture of the player being rendered. */
    public static ResourceLocation skinTexture(AbstractClientPlayer player) {
        //? if >=1.21.9
        /*return player.getSkin().body().texturePath();*/
        //? if >=1.20.2 && <1.21.9
        /*return player.getSkin().texture();*/
        //? if <1.20.2
        return player.getSkinTextureLocation();
    }

    /** The skin texture of a player known only by their entry in the tab list. */
    public static ResourceLocation skinTexture(PlayerInfo playerInfo) {
        //? if >=1.21.9
        /*return playerInfo.getSkin().body().texturePath();*/
        //? if >=1.20.2 && <1.21.9
        /*return playerInfo.getSkin().texture();*/
        //? if <1.20.2
        return playerInfo.getSkinLocation();
    }

    /** The skin Mojang hands out to a player who has none, chosen from their UUID. */
    public static ResourceLocation defaultSkinTexture(UUID uuid) {
        //? if >=1.21.9
        /*return DefaultPlayerSkin.get(uuid).body().texturePath();*/
        //? if >=1.20.2 && <1.21.9
        /*return DefaultPlayerSkin.get(uuid).texture();*/
        //? if <1.20.2
        return DefaultPlayerSkin.getDefaultSkin(uuid);
    }

    /** Which arm model a player uses — {@code "default"} (wide) or {@code "slim"}. */
    public static String skinModelName(PlayerInfo playerInfo) {
        //? if >=1.21.9
        /*return playerInfo.getSkin().model().getSerializedName();*/
        //? if >=1.20.2 && <1.21.9
        /*return playerInfo.getSkin().model().id();*/
        //? if <1.20.2
        return playerInfo.getModelName();
    }

    /** The arm model of a player who has no skin, chosen from their UUID. */
    public static String defaultSkinModelName(UUID uuid) {
        //? if >=1.21.9
        /*return DefaultPlayerSkin.get(uuid).model().getSerializedName();*/
        //? if >=1.20.2 && <1.21.9
        /*return DefaultPlayerSkin.get(uuid).model().id();*/
        //? if <1.20.2
        return DefaultPlayerSkin.getSkinModelName(uuid);
    }

    /**
     * Which advancement a widget in the advancement screen stands for.
     * <p>The field is private on both eras and widened by the access transformer; 1.20.2 renamed
     * it {@code advancement} to {@code advancementNode} and retyped it, so it needs a fresh SRG
     * entry as well as this gate.
     */
    public static ResourceLocation advancementId(AdvancementWidget widget) {
        //? if >=1.20.2
        /*return widget.advancementNode.holder().id();*/
        //? if <1.20.2
        return widget.advancement.getId();
    }

    /** Which advancement a tab of the advancement screen is rooted at. */
    public static ResourceLocation advancementId(AdvancementTab tab) {
        //? if >=1.20.2
        /*return tab.getRootNode().holder().id();*/
        //? if <1.20.2
        return tab.getAdvancement().getId();
    }
}
