package com.github.alexmodguy.alexscaves.server.command;

import codx.codxlib.api.CodxLib;
import codx.codxlib.api.CodxNotify;
import codx.codxlib.api.UpdateChecker;
import com.github.alexmodguy.alexscaves.AlexsCaves;
import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationConfig;
import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationNoiseCondition;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;

import java.util.List;
import java.util.Map;

/**
 * The {@code /acc} command tree — this mod's only command, and the door to {@link ACAdminMenu}.
 *
 * <p>Registered from {@code CommonEvents#onRegisterCommands}, which listens for Forge's
 * {@code RegisterCommandsEvent}. On Fabric that event is a stand-in type posted by
 * {@code fabric/event/ACGameEvents} out of {@code CommandRegistrationCallback}, so this class is
 * byte-identical on all three loaders and needs no gate of its own.
 *
 * <p>Every subcommand carries its own {@code requires}, rather than the root doing it, so that
 * {@code /acc} and {@code /acc version} stay visible to ordinary players while everything that can
 * change the server stays behind {@link #isOperator}. Brigadier merges literals, so a future
 * subcommand can be grafted on without touching what is here.
 *
 * <p>{@code config} exists alongside {@code menu} because a chest menu needs a player: on a
 * dedicated-server console there is nobody to open one for, and {@code /acc config} is the same
 * table rendered as text.
 */
public final class ACCommands {

    /** The highest {@code /acc config <n>} page. Page 0 of the menu is the status page. */
    private static final int MAX_PAGE = ACConfigOptions.PAGES.length - 1;

    private ACCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("acc")
                .executes(ACCommands::help)
                .then(Commands.literal("help").executes(ACCommands::help))
                .then(Commands.literal("version").executes(ACCommands::version))
                .then(Commands.literal("menu")
                        .requires(ACCommands::isOperator)
                        .executes(ACCommands::menu))
                .then(Commands.literal("config")
                        .requires(ACCommands::isOperator)
                        .executes(context -> configOverview(context.getSource()))
                        .then(Commands.literal("all")
                                .executes(context -> configAll(context.getSource())))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1, MAX_PAGE))
                                .executes(context -> configPage(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "page")))))
                .then(Commands.literal("biomes")
                        .requires(ACCommands::isOperator)
                        .executes(context -> biomes(context.getSource())))
                .then(Commands.literal("reload")
                        .requires(ACCommands::isOperator)
                        .executes(context -> reload(context.getSource())))
                .then(Commands.literal("reset")
                        .requires(ACCommands::isOperator)
                        .executes(context -> {
                            context.getSource().sendFailure(Component.literal(
                                    "This puts all " + ACConfigOptions.all().size()
                                            + " server settings back to their defaults. "
                                            + "Run /acc reset confirm if that is what you want."));
                            return 0;
                        })
                        .then(Commands.literal("confirm")
                                .executes(context -> reset(context.getSource())))));
    }

    // ------------------------------------------------------------------ permission

    /**
     * True for a server operator, and for the host of a singleplayer world.
     *
     * <p>The permission read moved at 1.21.11; the singleplayer short-circuit is what makes the
     * menu usable in a local world, where the host may not hold a permission level at all.
     */
    public static boolean isOperator(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        if (server != null && server.isSingleplayer()) {
            return true;
        }
        //? if >=1.21.11 {
        /*return source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        *///?} else {
        return source.hasPermission(2);
        //?}
    }

    /** The {@link ServerPlayer} form, which is what the menu re-checks on every click. */
    public static boolean isOperator(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server != null && server.isSingleplayer()) {
            return true;
        }
        //? if >=1.21.11 {
        /*return player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        *///?} else {
        return player.hasPermissions(2);
        //?}
    }

    // ------------------------------------------------------------------ subcommands

    private static int help(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean operator = isOperator(source);
        line(source, ChatFormatting.GOLD, "Alex's Caves Continued "
                + CodxLib.version(AlexsCaves.MODID) + " — /acc");
        line(source, ChatFormatting.GRAY, " version  — this mod's version, and whether it is current");
        if (operator) {
            line(source, ChatFormatting.GRAY, " menu     — open the settings panel (in-game only)");
            line(source, ChatFormatting.GRAY, " config   — the same settings as text");
            line(source, ChatFormatting.GRAY, " biomes   — the six cave biomes and whether each is on");
            line(source, ChatFormatting.GRAY, " reload   — re-read the cave-biome generation files");
            line(source, ChatFormatting.GRAY, " reset    — put every setting back to its default");
        }
        return 1;
    }

    private static int version(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        line(source, ChatFormatting.GOLD, "Alex's Caves Continued " + CodxLib.version(AlexsCaves.MODID));
        line(source, ChatFormatting.GRAY, " Minecraft " + CodxLib.minecraftVersion()
                + " on " + CodxLib.loaderName() + ", with CodxLib " + CodxLib.version("codxlib"));
        MinecraftServer server = source.getServer();
        if (server == null) {
            return 1;
        }
        line(source, ChatFormatting.GRAY, " Checking Modrinth for a newer build...");
        ServerPlayer player = source.getPlayer();
        UpdateChecker.checkVersionAsync(server, AlexsCaves.modInfo(), (outdated, latest) -> {
            Component message = outdated != null && outdated
                    ? UpdateChecker.updateAvailableMessage(AlexsCaves.modInfo(), latest)
                    : Component.literal("[Alex's Caves] You are on the latest version.")
                            .withStyle(ChatFormatting.GREEN);
            if (player != null) {
                CodxNotify.toPlayer(player, message);
            } else {
                CodxNotify.toConsole(server, message);
            }
        });
        return 1;
    }

    private static int menu(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal(
                    "The settings panel is a chest menu, so only a player can open it. "
                            + "Use /acc config from the console."));
            return 0;
        }
        ACAdminMenu.open(player);
        return 1;
    }

    private static int configOverview(CommandSourceStack source) {
        int modified = ACConfigOptions.modifiedCount();
        line(source, ChatFormatting.GOLD, ACConfigOptions.all().size()
                + " server settings, in " + MAX_PAGE + " groups:");
        for (int page = 1; page <= MAX_PAGE; page++) {
            ACConfigOptions.Page meta = ACConfigOptions.PAGES[page];
            int count = ACConfigOptions.onPage(page).size();
            line(source, ChatFormatting.YELLOW, " " + page + ". " + meta.title
                    + " (" + count + ") — " + meta.blurb);
        }
        line(source, ChatFormatting.GRAY, modified == 0
                ? " Everything is at its default."
                : " " + modified + " setting(s) differ from default.");
        line(source, ChatFormatting.DARK_GRAY,
                " /acc config <1-" + MAX_PAGE + ">, /acc config all, or /acc menu");
        return 1;
    }

    private static int configPage(CommandSourceStack source, int page) {
        ACConfigOptions.Page meta = ACConfigOptions.PAGES[page];
        line(source, ChatFormatting.GOLD, meta.title + " — " + meta.blurb);
        printOptions(source, ACConfigOptions.onPage(page));
        return 1;
    }

    private static int configAll(CommandSourceStack source) {
        line(source, ChatFormatting.GOLD, "All " + ACConfigOptions.all().size() + " server settings:");
        printOptions(source, ACConfigOptions.all());
        return 1;
    }

    private static void printOptions(CommandSourceStack source, List<ACConfigOptions.Option> options) {
        for (ACConfigOptions.Option option : options) {
            boolean modified = option.isModified();
            line(source, modified ? ChatFormatting.AQUA : ChatFormatting.YELLOW,
                    " " + option.key + " = " + option.display()
                            + (modified ? "  (default " + option.displayDefault() + ")" : ""));
        }
    }

    private static int biomes(CommandSourceStack source) {
        line(source, ChatFormatting.GOLD, "Cave biomes (edited in config/alexscaves_biome_generation/):");
        for (Map.Entry<ResourceKey<Biome>, BiomeGenerationNoiseCondition> entry
                : BiomeGenerationConfig.BIOMES.entrySet()) {
            boolean off = entry.getValue().isDisabledCompletely();
            line(source, off ? ChatFormatting.RED : ChatFormatting.GREEN,
                    " " + prettyBiomeName(entry.getKey()) + (off ? " — disabled" : " — enabled"));
        }
        line(source, ChatFormatting.DARK_GRAY,
                " " + enabledBiomeCount() + " of " + BiomeGenerationConfig.getBiomeCount()
                        + " will generate. Changes need /acc reload and fresh chunks.");
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        BiomeGenerationConfig.reloadConfig();
        line(source, ChatFormatting.GREEN, "Re-read the cave-biome generation files: "
                + enabledBiomeCount() + " of " + BiomeGenerationConfig.getBiomeCount() + " enabled.");
        line(source, ChatFormatting.GRAY, "Only chunks generated from now on are affected.");
        return 1;
    }

    private static int reset(CommandSourceStack source) {
        int reset = 0;
        for (ACConfigOptions.Option option : ACConfigOptions.all()) {
            if (option.isModified()) {
                option.reset();
                reset++;
            }
        }
        ACConfigOptions.save();
        line(source, ChatFormatting.GREEN, reset == 0
                ? "Every setting was already at its default."
                : "Put " + reset + " setting(s) back to their defaults.");
        return 1;
    }

    // ------------------------------------------------------------------ helpers

    /** How many of the six cave biomes are actually switched on. */
    public static int enabledBiomeCount() {
        int count = 0;
        for (BiomeGenerationNoiseCondition condition : BiomeGenerationConfig.BIOMES.values()) {
            if (!condition.isDisabledCompletely()) {
                count++;
            }
        }
        return count;
    }

    /** {@code alexscaves:magnetic_caves} to {@code Magnetic Caves}. */
    private static String prettyBiomeName(ResourceKey<Biome> key) {
        String path = key.location().getPath();
        StringBuilder out = new StringBuilder(path.length());
        boolean capitalise = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '_') {
                out.append(' ');
                capitalise = true;
            } else {
                out.append(capitalise ? Character.toUpperCase(c) : c);
                capitalise = false;
            }
        }
        return out.toString();
    }

    private static void line(CommandSourceStack source, ChatFormatting colour, String text) {
        source.sendSuccess(() -> Component.literal(text).withStyle(colour), false);
    }
}
