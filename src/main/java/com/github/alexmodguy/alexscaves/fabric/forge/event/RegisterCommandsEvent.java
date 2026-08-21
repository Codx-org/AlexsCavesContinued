package com.github.alexmodguy.alexscaves.fabric.forge.event;

import com.github.alexmodguy.alexscaves.fabric.forge.eventbus.api.Event;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Fabric stand-in for "the server is building its command tree, add yours to it".
 *
 * <p>The three accessors mirror Forge's exactly — {@code getDispatcher()},
 * {@code getCommandSelection()}, {@code getBuildContext()} — and are byte-for-byte the same on
 * Forge 47.4.21 (1.20.1) through 65.1.0 (26.2) and on NeoForge 20.4.251 through 26.2.0.37-beta, so
 * {@code CommonEvents#onRegisterCommands} needs no gate on any of the 58 nodes. (Forge 65 made the
 * event a {@code Record}/{@code RecordEvent} with its own static {@code BUS}; the getters survive
 * that unchanged, and the mod reaches it through {@code @SubscribeEvent} either way.)
 *
 * <p>The producer is {@code ACGameEvents#register}, over Fabric API's
 * {@code CommandRegistrationCallback}, whose callback hands over precisely these three values in
 * this order — verified on both ends of the pinned range (fabric-command-api-v2 2.2.18 for 1.20.1
 * and 3.1.0 for 26.2). Fabric fires it from {@code Commands}' own constructor, which is the same
 * moment Forge posts its event, so a command registered here appears in the tree of a dedicated
 * server, a singleplayer world and a datapack reload alike.
 */
public class RegisterCommandsEvent extends Event {

    private final CommandDispatcher<CommandSourceStack> dispatcher;
    private final Commands.CommandSelection environment;
    private final CommandBuildContext context;

    public RegisterCommandsEvent(CommandDispatcher<CommandSourceStack> dispatcher, Commands.CommandSelection environment, CommandBuildContext context) {
        this.dispatcher = dispatcher;
        this.environment = environment;
        this.context = context;
    }

    public CommandDispatcher<CommandSourceStack> getDispatcher() {
        return dispatcher;
    }

    public Commands.CommandSelection getCommandSelection() {
        return environment;
    }

    public CommandBuildContext getBuildContext() {
        return context;
    }
}
