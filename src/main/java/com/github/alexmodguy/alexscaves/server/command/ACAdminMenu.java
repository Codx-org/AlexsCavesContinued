package com.github.alexmodguy.alexscaves.server.command;

import codx.codxlib.api.CodxLib;
import codx.codxlib.api.CodxNotify;
import codx.codxlib.api.UpdateChecker;
import codx.codxlib.api.ui.menu.CodxMenu;
import codx.codxlib.api.ui.menu.CodxMenuClick;
import codx.codxlib.api.ui.menu.CodxMenuLayout;
import codx.codxlib.api.ui.menu.PagedMenuBuilder;
import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * The chest-style admin panel behind {@code /acc menu}.
 *
 * <p>Built on codxlib's {@code api.ui.menu} toolkit, which is server-side only: the menu is a
 * vanilla {@code GENERIC_9x6} container, so it opens for a vanilla client and needs no per-loader
 * registration and no networking of its own. Every widget reads and writes an
 * {@link ACConfigOptions.Option}, so the panel and {@code /acc config} can never disagree about
 * what an option is called, what it defaults to or what range it accepts.</p>
 *
 * <p>Geometry is fixed by codxlib: a paged menu is always six rows, and the bottom row (slots
 * 45-53) is the navigation strip it draws itself (reset, previous, page title, next, close). This
 * class therefore only ever writes slots 0-44 — a header at slot 4 and up to 28 widgets in the
 * four interior rows, which is comfortably more than the largest page needs.</p>
 */
public final class ACAdminMenu {

    /** Slot the per-page header sits in — centred on the top row. */
    private static final int HEADER_SLOT = 4;

    /** How many widgets fit on one interior row (columns 1-7, leaving the border columns clear). */
    private static final int COLUMNS = 7;

    private ACAdminMenu() {
    }

    /**
     * Opens the panel for one player. The caller is responsible for the permission check; the
     * {@code canUse} predicate below is a second gate that also survives a de-op mid-session,
     * since codxlib re-tests it on every click.
     */
    public static void open(ServerPlayer player) {
        PagedMenuBuilder builder = CodxMenu.paged("§6§lAlex's Caves")
                .onChange(ACConfigOptions::save)
                .canUse(ACCommands::isOperator)
                .resetButton(ACAdminMenu::resetCurrentPage);
        for (int i = 0; i < ACConfigOptions.PAGES.length; i++) {
            final int index = i;
            builder.page(ACConfigOptions.PAGES[i].title, layout -> buildPage(layout, index, player));
        }
        builder.open(player);
    }

    private static void buildPage(CodxMenuLayout layout, int page, ServerPlayer viewer) {
        ACConfigOptions.Page meta = ACConfigOptions.PAGES[page];
        layout.info(HEADER_SLOT, meta.icon, "§e§l" + meta.title, "§7" + meta.blurb);
        if (page == 0) {
            buildStatusPage(layout, viewer);
            return;
        }
        List<ACConfigOptions.Option> options = ACConfigOptions.onPage(page);
        for (int i = 0; i < options.size(); i++) {
            options.get(i).layout(layout, slotFor(i));
        }
    }

    /**
     * Read-only page 0. Everything here answers a question a server owner asks before they touch a
     * setting: which build is installed, which library build is under it, and whether this world's
     * configuration has been changed from stock.
     */
    private static void buildStatusPage(CodxMenuLayout layout, ServerPlayer viewer) {
        int modified = ACConfigOptions.modifiedCount();
        layout.info(slotFor(0), Items.NAME_TAG, "§bAlex's Caves Continued",
                "§7Version: §f" + CodxLib.version(AlexsCaves.MODID));
        layout.info(slotFor(1), Items.GRASS_BLOCK, "§bMinecraft",
                "§7Version: §f" + CodxLib.minecraftVersion());
        layout.info(slotFor(2), Items.ANVIL, "§bMod loader",
                "§7Running on: §f" + CodxLib.loaderName());
        layout.info(slotFor(3), Items.BOOK, "§bCodxLib",
                "§7Version: §f" + CodxLib.version("codxlib"),
                "§8The shared library this mod builds on");
        layout.info(slotFor(4), Items.MAP, "§bCave biomes",
                "§7Enabled: §f" + ACCommands.enabledBiomeCount()
                        + "§7 of §f" + BiomeGenerationConfig.getBiomeCount(),
                "§8Edited in config/alexscaves_biome_generation/");
        layout.info(slotFor(5), Items.WRITABLE_BOOK, "§bSettings changed",
                modified == 0
                        ? "§7Everything is at its §adefault"
                        : "§7§f" + modified + "§7 option" + (modified == 1 ? "" : "s")
                                + " differ" + (modified == 1 ? "s" : "") + " from default",
                "§8Changed options are marked §e*§8 on their page");
        layout.info(slotFor(6), Items.PLAYER_HEAD, "§bPlayers online",
                "§7Currently: §f" + onlinePlayers(viewer));

        layout.action(slotFor(7), Items.REPEATER, "§aReload biome settings",
                click -> {
                    BiomeGenerationConfig.reloadConfig();
                    tell(click, "§aReloaded the cave-biome generation settings.");
                },
                "§7Re-reads config/alexscaves_biome_generation/",
                "§8Takes effect in chunks generated from now on");
        layout.action(slotFor(9), Items.HOPPER, "§aSave settings now",
                click -> {
                    ACConfigOptions.save();
                    tell(click, "§aWrote the server settings to disk.");
                },
                "§7Flushes every change to the config file",
                "§8The panel already saves as you edit");
        layout.action(slotFor(11), Items.SPYGLASS, "§aCheck for updates",
                ACAdminMenu::checkForUpdates,
                "§7Asks Modrinth whether a newer",
                "§7build of this mod exists");
    }

    private static void checkForUpdates(CodxMenuClick click) {
        ServerPlayer player = click.player();
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        tell(click, "§7Checking for updates...");
        UpdateChecker.checkVersionAsync(server, AlexsCaves.modInfo(), (outdated, latest) -> {
            if (outdated != null && outdated) {
                CodxNotify.toPlayer(player, UpdateChecker.updateAvailableMessage(AlexsCaves.modInfo(), latest));
            } else {
                CodxNotify.toPlayer(player, Component.literal("§a[Alex's Caves] You are on the latest version."));
            }
        });
    }

    /**
     * The reset button is page-aware on purpose: a single "reset everything" on a 39-option panel is
     * far too easy to hit by accident, and codxlib hands the click the page it happened on.
     */
    private static void resetCurrentPage(CodxMenuClick click) {
        List<ACConfigOptions.Option> options = ACConfigOptions.onPage(click.page());
        if (options.isEmpty()) {
            tell(click, "§7Nothing on this page to reset.");
            return;
        }
        int reset = 0;
        for (ACConfigOptions.Option option : options) {
            if (option.isModified()) {
                option.reset();
                reset++;
            }
        }
        ACConfigOptions.save();
        click.markChanged();
        if (reset == 0) {
            tell(click, "§7This page is already at its defaults.");
        } else {
            tell(click, "§aReset §f" + reset + "§a setting" + (reset == 1 ? "" : "s")
                    + " on this page to default.");
        }
    }

    private static void tell(CodxMenuClick click, String message) {
        CodxNotify.toPlayer(click.player(), Component.literal(message));
    }

    private static int onlinePlayers(ServerPlayer viewer) {
        MinecraftServer server = viewer.level().getServer();
        return server == null ? 1 : server.getPlayerList().getPlayers().size();
    }

    /**
     * Maps a widget index onto a slot in the four interior rows, so a page's options flow left to
     * right and top to bottom without ever touching the border columns or codxlib's nav row.
     */
    private static int slotFor(int index) {
        return 10 + (index / COLUMNS) * 9 + (index % COLUMNS);
    }
}
