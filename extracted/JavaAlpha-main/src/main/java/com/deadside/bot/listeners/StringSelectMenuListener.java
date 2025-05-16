package com.deadside.bot.listeners;

import com.deadside.bot.commands.economy.RouletteCommand;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for string select menu interactions
 */
public class StringSelectMenuListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(StringSelectMenuListener.class);
    private final RouletteCommand rouletteCommand = new RouletteCommand();
    
    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        try {
            // Format: componentId = type:action:userId
            String[] menuData = event.getComponentId().split(":");
            
            if (menuData.length < 2) {
                return;
            }
            
            String menuType = menuData[0];
            
            switch (menuType) {
                case "roulette" -> rouletteCommand.handleSelectMenuInteraction(event, menuData);
                // Add more menu types here as needed
                default -> {
                    // Unknown menu type
                    logger.warn("Unknown select menu type: {}", menuType);
                }
            }
        } catch (Exception e) {
            logger.error("Error handling select menu interaction", e);
            event.reply("An error occurred: " + e.getMessage()).setEphemeral(true).queue();
        }
    }
}