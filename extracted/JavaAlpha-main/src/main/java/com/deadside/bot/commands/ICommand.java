package com.deadside.bot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

/**
 * Interface for all slash commands
 */
public interface ICommand {
    /**
     * Get the name of the command
     */
    String getName();
    
    /**
     * Get the command data for registration
     */
    CommandData getCommandData();
    
    /**
     * Execute the command
     */
    void execute(SlashCommandInteractionEvent event);
}