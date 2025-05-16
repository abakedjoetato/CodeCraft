package com.deadside.bot.listeners;

import com.deadside.bot.commands.economy.BlackjackCommand;
import com.deadside.bot.commands.economy.RouletteCommand;
import com.deadside.bot.commands.economy.SlotCommand;
import com.deadside.bot.db.models.LinkedPlayer;
import com.deadside.bot.db.models.Player;
import com.deadside.bot.db.repositories.LinkedPlayerRepository;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for button interactions
 */
public class ButtonListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ButtonListener.class);
    private final SlotCommand slotCommand = new SlotCommand();
    private final BlackjackCommand blackjackCommand = new BlackjackCommand();
    private final RouletteCommand rouletteCommand = new RouletteCommand();
    private final LinkedPlayerRepository linkedPlayerRepository = new LinkedPlayerRepository();
    private final PlayerRepository playerRepository = new PlayerRepository();
    
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        try {
            String[] buttonData = event.getComponentId().split(":");
            
            if (buttonData.length < 2) {
                return;
            }
            
            String buttonType = buttonData[0];
            String action = buttonData[1];
            
            switch (buttonType) {
                case "slot" -> handleSlotButton(event, buttonData);
                case "blackjack" -> blackjackCommand.handleButtonInteraction(event, buttonData);
                case "roulette" -> rouletteCommand.handleButtonInteraction(event, buttonData);
                // Add more button types here as needed
                default -> {
                    // Unknown button type
                    logger.warn("Unknown button type: {}", buttonType);
                }
            }
        } catch (Exception e) {
            logger.error("Error handling button interaction", e);
            event.reply("An error occurred: " + e.getMessage()).setEphemeral(true).queue();
        }
    }
    
    /**
     * Handle slot machine buttons
     */
    private void handleSlotButton(ButtonInteractionEvent event, String[] buttonData) {
        if (buttonData.length < 3) {
            return;
        }
        
        String action = buttonData[1];
        int originalBet = Integer.parseInt(buttonData[2]);
        int newBet = originalBet;
        
        // Calculate new bet amount based on action
        switch (action) {
            case "playAgain" -> newBet = originalBet;
            case "double" -> newBet = originalBet * 2;
            case "half" -> newBet = Math.max(10, originalBet / 2);
            default -> {
                // Unknown action
                logger.warn("Unknown slot button action: {}", action);
                return;
            }
        }
        
        // Get linked player
        LinkedPlayer linkedPlayer = linkedPlayerRepository.findByDiscordId(event.getUser().getIdLong());
        
        if (linkedPlayer == null) {
            event.reply("You don't have a linked Deadside account. Use `/link` to connect your Discord and Deadside accounts.")
                 .setEphemeral(true).queue();
            return;
        }
        
        // Get player
        Player player = playerRepository.findByPlayerId(linkedPlayer.getMainPlayerId());
        
        if (player == null) {
            event.reply("Unable to find your player data. This could be because you haven't been active yet.")
                 .setEphemeral(true).queue();
            return;
        }
        
        // Check if player has enough balance
        if (player.getCurrency().getCoins() < newBet) {
            event.reply("You don't have enough coins for this bet. Your current balance is " + 
                       String.format("%,d", player.getCurrency().getCoins()) + " coins.")
                 .setEphemeral(true).queue();
            return;
        }
        
        // Acknowledge the button click by deferring edit
        event.deferEdit().queue();
        
        // Start a new slot machine game with the new bet
        startNewSlotGame(event, player, newBet);
    }
    
    /**
     * Start a new slot machine game
     */
    private void startNewSlotGame(ButtonInteractionEvent event, Player player, int betAmount) {
        // First, take the bet
        player.getCurrency().removeCoins(betAmount);
        
        // Animation phases
        final String[] spinningSymbols = {"🎰", "💫", "✨", "🎲", "🎯"};
        
        // Initial message with spinning animation
        String initialDisplay = "[ " + spinningSymbols[0] + " | " + spinningSymbols[0] + " | " + spinningSymbols[0] + " ]";
        
        // Create the initial response message
        StringBuilder initialMessage = new StringBuilder();
        initialMessage.append("**Bet**: `").append(String.format("%,d", betAmount)).append(" coins`\n\n");
        initialMessage.append("# ").append(initialDisplay).append("\n\n");
        initialMessage.append("*Spinning the reels again...*\n\n");
        initialMessage.append("**Balance**: `").append(String.format("%,d", player.getCurrency().getCoins())).append(" coins`");
        
        // Update the message to show spinning
        event.getHook().editOriginalEmbeds(
                EmbedUtils.customEmbed("Slot Machine - Spinning", initialMessage.toString(), java.awt.Color.BLUE)
        ).setComponents().queue(message -> {
            // Start the slot machine animation using the existing method in SlotCommand
            slotCommand.accessAnimateSlotMachine(message, player, betAmount);
        });
        
        // Log the new bet
        logger.info("User {} bet {} coins on slots (via button)", event.getUser().getName(), betAmount);
    }
}