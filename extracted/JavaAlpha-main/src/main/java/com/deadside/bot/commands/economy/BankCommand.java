package com.deadside.bot.commands.economy;

import com.deadside.bot.commands.ICommand;
import com.deadside.bot.db.models.LinkedPlayer;
import com.deadside.bot.db.models.Player;
import com.deadside.bot.db.repositories.LinkedPlayerRepository;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;

/**
 * Command for banking operations
 */
public class BankCommand implements ICommand {
    private static final Logger logger = LoggerFactory.getLogger(BankCommand.class);
    private final LinkedPlayerRepository linkedPlayerRepository = new LinkedPlayerRepository();
    private final PlayerRepository playerRepository = new PlayerRepository();
    
    @Override
    public String getName() {
        return "bank";
    }
    
    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), "Manage your bank account")
                .addSubcommands(
                        new SubcommandData("deposit", "Deposit coins into your bank account")
                                .addOption(OptionType.INTEGER, "amount", "Amount to deposit", true),
                        new SubcommandData("withdraw", "Withdraw coins from your bank account")
                                .addOption(OptionType.INTEGER, "amount", "Amount to withdraw", true),
                        new SubcommandData("info", "View information about your bank account")
                );
    }
    
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        try {
            String subCommand = event.getSubcommandName();
            if (subCommand == null) {
                event.reply("Invalid command usage.").setEphemeral(true).queue();
                return;
            }
            
            // Defer reply to give us time to process
            event.deferReply().queue();
            
            // Get linked player information
            long userId = event.getUser().getIdLong();
            LinkedPlayer linkedPlayer = linkedPlayerRepository.findByDiscordId(userId);
            
            if (linkedPlayer == null) {
                event.getHook().sendMessageEmbeds(
                        EmbedUtils.errorEmbed("Not Linked", 
                                "You don't have a linked Deadside account. Use `/link` to connect your Discord and Deadside accounts.")
                ).queue();
                return;
            }
            
            // Get player stats
            Player player = playerRepository.findByPlayerId(linkedPlayer.getMainPlayerId());
            
            if (player == null) {
                event.getHook().sendMessageEmbeds(
                        EmbedUtils.errorEmbed("Player Not Found", 
                                "Unable to find player data. This could be because the player hasn't been active yet.")
                ).queue();
                return;
            }
            
            // Process the appropriate subcommand
            switch (subCommand) {
                case "deposit" -> handleDeposit(event, player);
                case "withdraw" -> handleWithdraw(event, player);
                case "info" -> handleInfo(event, player);
                default -> event.getHook().sendMessage("Unknown subcommand: " + subCommand).queue();
            }
            
        } catch (Exception e) {
            logger.error("Error executing bank command", e);
            if (event.isAcknowledged()) {
                event.getHook().sendMessageEmbeds(
                        EmbedUtils.errorEmbed("Error", "An error occurred while processing your bank operation.")
                ).queue();
            } else {
                event.reply("An error occurred: " + e.getMessage()).setEphemeral(true).queue();
            }
        }
    }
    
    /**
     * Handle deposit operation
     */
    private void handleDeposit(SlashCommandInteractionEvent event, Player player) {
        long amount = event.getOption("amount", 0L, OptionMapping::getAsLong);
        
        // Validate amount
        if (amount <= 0) {
            event.getHook().sendMessageEmbeds(
                    EmbedUtils.errorEmbed("Invalid Amount", 
                            "Please specify a positive amount to deposit.")
            ).queue();
            return;
        }
        
        // Check if player has enough funds
        if (player.getCurrency().getCoins() < amount) {
            event.getHook().sendMessageEmbeds(
                    EmbedUtils.errorEmbed("Insufficient Funds", 
                            "You don't have enough coins in your wallet. You currently have " + 
                            formatAmount(player.getCurrency().getCoins()) + " coins.")
            ).queue();
            return;
        }
        
        // Deposit the amount
        boolean success = player.getCurrency().depositToBank(amount);
        
        if (!success) {
            event.getHook().sendMessageEmbeds(
                    EmbedUtils.errorEmbed("Transaction Failed", 
                            "Failed to deposit the coins. Please try again later.")
            ).queue();
            return;
        }
        
        // Save the changes
        playerRepository.save(player);
        
        // Send success message
        StringBuilder message = new StringBuilder();
        message.append("Successfully deposited ").append(formatAmount(amount)).append(" coins into your bank account.\n\n");
        message.append("**New Balances**\n");
        message.append("💰 Wallet: `").append(formatAmount(player.getCurrency().getCoins())).append(" coins`\n");
        message.append("🏦 Bank: `").append(formatAmount(player.getCurrency().getBankCoins())).append(" coins`\n");
        
        event.getHook().sendMessageEmbeds(
                EmbedUtils.successEmbed("Deposit Successful", message.toString())
        ).queue();
        
        logger.info("User {} deposited {} coins to bank", event.getUser().getName(), amount);
    }
    
    /**
     * Handle withdraw operation
     */
    private void handleWithdraw(SlashCommandInteractionEvent event, Player player) {
        long amount = event.getOption("amount", 0L, OptionMapping::getAsLong);
        
        // Validate amount
        if (amount <= 0) {
            event.getHook().sendMessageEmbeds(
                    EmbedUtils.errorEmbed("Invalid Amount", 
                            "Please specify a positive amount to withdraw.")
            ).queue();
            return;
        }
        
        // Check if player has enough funds in bank
        if (player.getCurrency().getBankCoins() < amount) {
            event.getHook().sendMessageEmbeds(
                    EmbedUtils.errorEmbed("Insufficient Funds", 
                            "You don't have enough coins in your bank account. You currently have " + 
                            formatAmount(player.getCurrency().getBankCoins()) + " coins in the bank.")
            ).queue();
            return;
        }
        
        // Withdraw the amount
        boolean success = player.getCurrency().withdrawFromBank(amount);
        
        if (!success) {
            event.getHook().sendMessageEmbeds(
                    EmbedUtils.errorEmbed("Transaction Failed", 
                            "Failed to withdraw the coins. Please try again later.")
            ).queue();
            return;
        }
        
        // Save the changes
        playerRepository.save(player);
        
        // Send success message
        StringBuilder message = new StringBuilder();
        message.append("Successfully withdrew ").append(formatAmount(amount)).append(" coins from your bank account.\n\n");
        message.append("**New Balances**\n");
        message.append("💰 Wallet: `").append(formatAmount(player.getCurrency().getCoins())).append(" coins`\n");
        message.append("🏦 Bank: `").append(formatAmount(player.getCurrency().getBankCoins())).append(" coins`\n");
        
        event.getHook().sendMessageEmbeds(
                EmbedUtils.successEmbed("Withdrawal Successful", message.toString())
        ).queue();
        
        logger.info("User {} withdrew {} coins from bank", event.getUser().getName(), amount);
    }
    
    /**
     * Handle info operation
     */
    private void handleInfo(SlashCommandInteractionEvent event, Player player) {
        StringBuilder description = new StringBuilder();
        
        description.append("**Bank Account Information**\n\n");
        description.append("🏦 **Current Balance**: `").append(formatAmount(player.getCurrency().getBankCoins())).append(" coins`\n");
        description.append("💰 **Wallet Balance**: `").append(formatAmount(player.getCurrency().getCoins())).append(" coins`\n");
        description.append("💸 **Total Assets**: `").append(formatAmount(player.getCurrency().getTotalBalance())).append(" coins`\n\n");
        
        // Add some tips
        description.append("**Bank Benefits**\n");
        description.append("• Coins in your bank account are safe when you die\n");
        description.append("• No interest is earned on bank deposits currently\n");
        description.append("• Use `/bank deposit <amount>` to deposit coins\n");
        description.append("• Use `/bank withdraw <amount>` to withdraw coins\n");
        
        event.getHook().sendMessageEmbeds(
                EmbedUtils.customEmbed("Bank Account", description.toString(), new Color(0, 128, 255))
        ).queue();
    }
    
    /**
     * Format a currency amount with commas
     */
    private String formatAmount(long amount) {
        return String.format("%,d", amount);
    }
}