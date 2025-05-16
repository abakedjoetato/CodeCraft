package com.deadside.bot.commands.economy;

import com.deadside.bot.commands.ICommand;
import com.deadside.bot.config.Config;
import com.deadside.bot.db.models.LinkedPlayer;
import com.deadside.bot.db.models.Player;
import com.deadside.bot.db.repositories.LinkedPlayerRepository;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Admin commands for managing the economy system
 */
public class AdminEconomyCommand implements ICommand {
    private static final Logger logger = LoggerFactory.getLogger(AdminEconomyCommand.class);
    private final LinkedPlayerRepository linkedPlayerRepository = new LinkedPlayerRepository();
    private final PlayerRepository playerRepository = new PlayerRepository();
    private final Config config = Config.getInstance();
    
    @Override
    public String getName() {
        return "admineconomy";
    }
    
    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), "Admin commands for managing the economy system")
                .addSubcommands(
                        new SubcommandData("give", "Give coins to a player")
                                .addOption(OptionType.USER, "user", "The user to give coins to", true)
                                .addOption(OptionType.INTEGER, "amount", "The amount of coins to give", true)
                                .addOption(OptionType.STRING, "reason", "The reason for giving coins", false),
                        new SubcommandData("take", "Take coins from a player")
                                .addOption(OptionType.USER, "user", "The user to take coins from", true)
                                .addOption(OptionType.INTEGER, "amount", "The amount of coins to take", true)
                                .addOption(OptionType.STRING, "reason", "The reason for taking coins", false),
                        new SubcommandData("set", "Set a player's coin balance")
                                .addOption(OptionType.USER, "user", "The user to set coins for", true)
                                .addOption(OptionType.INTEGER, "amount", "The amount of coins to set", true)
                                .addOption(OptionType.STRING, "reason", "The reason for setting coins", false),
                        new SubcommandData("reset", "Reset a player's economy data")
                                .addOption(OptionType.USER, "user", "The user to reset economy data for", true)
                                .addOption(OptionType.BOOLEAN, "confirm", "Confirm the reset", true),
                        new SubcommandData("setdaily", "Set the daily reward amount for all users")
                                .addOption(OptionType.INTEGER, "amount", "The new daily reward amount", true),
                        new SubcommandData("setworkmax", "Set the maximum work reward amount")
                                .addOption(OptionType.INTEGER, "amount", "The new maximum work reward amount", true),
                        new SubcommandData("setworkmin", "Set the minimum work reward amount")
                                .addOption(OptionType.INTEGER, "amount", "The new minimum work reward amount", true)
                );
    }
    
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Check if user is owner or admin
        if (!isAdminOrOwner(event.getUser().getIdLong())) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Unauthorized", "You don't have permission to use admin economy commands."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", "No subcommand provided."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        switch (subcommand) {
            case "give" -> handleGiveCommand(event);
            case "take" -> handleTakeCommand(event);
            case "set" -> handleSetCommand(event);
            case "reset" -> handleResetCommand(event);
            case "setdaily" -> handleSetDailyCommand(event);
            case "setworkmax" -> handleSetWorkMaxCommand(event);
            case "setworkmin" -> handleSetWorkMinCommand(event);
            default -> event.replyEmbeds(EmbedUtils.errorEmbed("Error", "Unknown subcommand: " + subcommand))
                    .setEphemeral(true)
                    .queue();
        }
    }
    
    /**
     * Handle give command - give coins to a player
     */
    private void handleGiveCommand(SlashCommandInteractionEvent event) {
        User targetUser = event.getOption("user", OptionMapping::getAsUser);
        long amount = event.getOption("amount", 0L, OptionMapping::getAsLong);
        String reason = event.getOption("reason", "No reason provided", OptionMapping::getAsString);
        
        if (targetUser == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", "No user specified."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        if (amount <= 0) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", "Amount must be greater than 0."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        // Find linked player
        LinkedPlayer linkedPlayer = linkedPlayerRepository.findByDiscordId(targetUser.getIdLong());
        if (linkedPlayer == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Player Not Linked", 
                    targetUser.getAsMention() + " doesn't have a linked Deadside account."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        // Find player
        Player player = playerRepository.findByPlayerId(linkedPlayer.getMainPlayerId());
        if (player == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Player Not Found", 
                    "Unable to find player data for " + targetUser.getAsMention() + "."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        // Add coins
        long oldBalance = player.getCurrency().getCoins();
        player.getCurrency().addCoins(amount);
        
        // Save player
        playerRepository.save(player);
        
        // Log transaction
        logger.info("Admin {} gave {} coins to {} ({}). Reason: {}", 
                event.getUser().getId(), amount, targetUser.getId(), player.getName(), reason);
        
        // Reply with success message
        event.replyEmbeds(EmbedUtils.successEmbed("Coins Added",
                String.format("Successfully gave **%,d coins** to %s.\n\n" +
                        "Old balance: **%,d coins**\n" +
                        "New balance: **%,d coins**\n\n" +
                        "Reason: %s",
                        amount, targetUser.getAsMention(), oldBalance, player.getCurrency().getCoins(), reason)))
                .queue();
    }
    
    /**
     * Handle take command - take coins from a player
     */
    private void handleTakeCommand(SlashCommandInteractionEvent event) {
        User targetUser = event.getOption("user", OptionMapping::getAsUser);
        long amount = event.getOption("amount", 0L, OptionMapping::getAsLong);
        String reason = event.getOption("reason", "No reason provided", OptionMapping::getAsString);
        
        if (targetUser == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", "No user specified."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        if (amount <= 0) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", "Amount must be greater than 0."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        // Find linked player
        LinkedPlayer linkedPlayer = linkedPlayerRepository.findByDiscordId(targetUser.getIdLong());
        if (linkedPlayer == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Player Not Linked", 
                    targetUser.getAsMention() + " doesn't have a linked Deadside account."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        // Find player
        Player player = playerRepository.findByPlayerId(linkedPlayer.getMainPlayerId());
        if (player == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Player Not Found", 
                    "Unable to find player data for " + targetUser.getAsMention() + "."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        // Check if player has enough coins
        long currentBalance = player.getCurrency().getCoins();
        if (currentBalance < amount) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Insufficient Funds", 
                    String.format("%s only has %,d coins, cannot take %,d.", 
                            targetUser.getAsMention(), currentBalance, amount)))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        // Remove coins
        player.getCurrency().removeCoins(amount);
        
        // Save player
        playerRepository.save(player);
        
        // Log transaction
        logger.info("Admin {} took {} coins from {} ({}). Reason: {}", 
                event.getUser().getId(), amount, targetUser.getId(), player.getName(), reason);
        
        // Reply with success message
        event.replyEmbeds(EmbedUtils.successEmbed("Coins Removed",
                String.format("Successfully took **%,d coins** from %s.\n\n" +
                        "Old balance: **%,d coins**\n" +
                        "New balance: **%,d coins**\n\n" +
                        "Reason: %s",
                        amount, targetUser.getAsMention(), currentBalance, player.getCurrency().getCoins(), reason)))
                .queue();
    }
    
    /**
     * Handle set command - set a player's coin balance
     */
    private void handleSetCommand(SlashCommandInteractionEvent event) {
        User targetUser = event.getOption("user", OptionMapping::getAsUser);
        long amount = event.getOption("amount", 0L, OptionMapping::getAsLong);
        String reason = event.getOption("reason", "No reason provided", OptionMapping::getAsString);
        
        if (targetUser == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", "No user specified."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        if (amount < 0) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", "Amount cannot be negative."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        // Find linked player
        LinkedPlayer linkedPlayer = linkedPlayerRepository.findByDiscordId(targetUser.getIdLong());
        if (linkedPlayer == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Player Not Linked", 
                    targetUser.getAsMention() + " doesn't have a linked Deadside account."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        // Find player
        Player player = playerRepository.findByPlayerId(linkedPlayer.getMainPlayerId());
        if (player == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Player Not Found", 
                    "Unable to find player data for " + targetUser.getAsMention() + "."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        // Remember old balance
        long oldBalance = player.getCurrency().getCoins();
        
        // Set coins
        player.getCurrency().setCoins(amount);
        
        // Save player
        playerRepository.save(player);
        
        // Log transaction
        logger.info("Admin {} set {} coins for {} ({}). Reason: {}", 
                event.getUser().getId(), amount, targetUser.getId(), player.getName(), reason);
        
        // Reply with success message
        event.replyEmbeds(EmbedUtils.successEmbed("Balance Set",
                String.format("Successfully set %s's balance to **%,d coins**.\n\n" +
                        "Old balance: **%,d coins**\n" +
                        "New balance: **%,d coins**\n\n" +
                        "Reason: %s",
                        targetUser.getAsMention(), amount, oldBalance, player.getCurrency().getCoins(), reason)))
                .queue();
    }
    
    /**
     * Handle reset command - reset a player's economy data
     */
    private void handleResetCommand(SlashCommandInteractionEvent event) {
        User targetUser = event.getOption("user", OptionMapping::getAsUser);
        boolean confirm = event.getOption("confirm", false, OptionMapping::getAsBoolean);
        
        if (targetUser == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", "No user specified."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        if (!confirm) {
            event.replyEmbeds(EmbedUtils.warningEmbed("Confirmation Required", 
                    "You must confirm this action by setting the confirm option to true. " +
                    "This will reset all economy data for " + targetUser.getAsMention() + "."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        // Find linked player
        LinkedPlayer linkedPlayer = linkedPlayerRepository.findByDiscordId(targetUser.getIdLong());
        if (linkedPlayer == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Player Not Linked", 
                    targetUser.getAsMention() + " doesn't have a linked Deadside account."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        // Find player
        Player player = playerRepository.findByPlayerId(linkedPlayer.getMainPlayerId());
        if (player == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Player Not Found", 
                    "Unable to find player data for " + targetUser.getAsMention() + "."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        // Reset economy data
        player.setCurrency(new com.deadside.bot.db.models.Currency());
        
        // Save player
        playerRepository.save(player);
        
        // Log action
        logger.info("Admin {} reset economy data for {} ({})", 
                event.getUser().getId(), targetUser.getId(), player.getName());
        
        // Reply with success message
        event.replyEmbeds(EmbedUtils.successEmbed("Economy Data Reset",
                "Successfully reset all economy data for " + targetUser.getAsMention() + "."))
                .queue();
    }
    
    /**
     * Handle setdaily command - set the daily reward amount for all users
     */
    private void handleSetDailyCommand(SlashCommandInteractionEvent event) {
        long amount = event.getOption("amount", 0L, OptionMapping::getAsLong);
        
        if (amount <= 0) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", "Daily reward amount must be greater than 0."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        // Set the daily reward amount in the global config
        config.setDailyRewardAmount(amount);
        
        // Log action
        logger.info("Admin {} set daily reward amount to {}", 
                event.getUser().getId(), amount);
        
        // Reply with success message
        event.replyEmbeds(EmbedUtils.successEmbed("Daily Reward Updated",
                String.format("Successfully set the daily reward amount to **%,d coins** for all users.", amount)))
                .queue();
    }
    
    /**
     * Handle setworkmax command - set the maximum work reward amount
     */
    private void handleSetWorkMaxCommand(SlashCommandInteractionEvent event) {
        long amount = event.getOption("amount", 0L, OptionMapping::getAsLong);
        
        if (amount <= 0) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", "Maximum work reward amount must be greater than 0."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        // Get the current min work amount
        long minWorkAmount = config.getWorkMinAmount();
        if (amount < minWorkAmount) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", 
                    String.format("Maximum work reward amount (%,d) cannot be less than minimum work reward amount (%,d).", 
                            amount, minWorkAmount)))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        // Set the work reward amount in the global config
        config.setWorkMaxAmount(amount);
        
        // Log action
        logger.info("Admin {} set maximum work reward amount to {}", 
                event.getUser().getId(), amount);
        
        // Reply with success message
        event.replyEmbeds(EmbedUtils.successEmbed("Work Reward Updated",
                String.format("Successfully set the maximum work reward amount to **%,d coins**.", amount)))
                .queue();
    }
    
    /**
     * Handle setworkmin command - set the minimum work reward amount
     */
    private void handleSetWorkMinCommand(SlashCommandInteractionEvent event) {
        long amount = event.getOption("amount", 0L, OptionMapping::getAsLong);
        
        if (amount <= 0) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", "Minimum work reward amount must be greater than 0."))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        // Get the current max work amount
        long maxWorkAmount = config.getWorkMaxAmount();
        if (amount > maxWorkAmount) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Error", 
                    String.format("Minimum work reward amount (%,d) cannot be greater than maximum work reward amount (%,d).", 
                            amount, maxWorkAmount)))
                    .setEphemeral(true)
                    .queue();
            return;
        }
        
        // Set the work reward amount in the global config
        config.setWorkMinAmount(amount);
        
        // Log action
        logger.info("Admin {} set minimum work reward amount to {}", 
                event.getUser().getId(), amount);
        
        // Reply with success message
        event.replyEmbeds(EmbedUtils.successEmbed("Work Reward Updated",
                String.format("Successfully set the minimum work reward amount to **%,d coins**.", amount)))
                .queue();
    }
    
    /**
     * Check if a user is an admin or the bot owner
     */
    private boolean isAdminOrOwner(long userId) {
        return userId == config.getBotOwnerId() || config.getAdminUserIds().contains(userId);
    }
}