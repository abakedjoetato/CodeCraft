package com.deadside.bot.commands.stats;

import com.deadside.bot.commands.ICommand;
import com.deadside.bot.db.models.Player;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.premium.PremiumManager;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Command for viewing leaderboards
 */
public class LeaderboardCommand implements ICommand {
    private static final Logger logger = LoggerFactory.getLogger(LeaderboardCommand.class);
    private final PlayerRepository playerRepository = new PlayerRepository();
    private final PremiumManager premiumManager = new PremiumManager();
    private final DecimalFormat df = new DecimalFormat("#.##");
    
    @Override
    public String getName() {
        return "leaderboard";
    }
    
    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), "View Deadside leaderboards")
                .addOptions(
                        new OptionData(OptionType.STRING, "type", "Leaderboard type", true)
                                .addChoice("kills", "kills")
                                .addChoice("kd", "kd")
                );
    }
    
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("This command can only be used in a server.").setEphemeral(true).queue();
            return;
        }
        
        // Check for premium if feature is restricted
        long guildId = event.getGuild().getIdLong();
        if (!premiumManager.hasPremium(guildId)) {
            event.reply("This command is only available with premium. The killfeed is available for free.").setEphemeral(true).queue();
            return;
        }
        
        String type = event.getOption("type", "kills", OptionMapping::getAsString);
        
        event.deferReply().queue();
        
        try {
            switch (type) {
                case "kills" -> displayKillsLeaderboard(event);
                case "kd" -> displayKDLeaderboard(event);
                default -> event.getHook().sendMessage("Unknown leaderboard type: " + type).queue();
            }
        } catch (Exception e) {
            logger.error("Error retrieving leaderboard", e);
            event.getHook().sendMessage("An error occurred while retrieving the leaderboard.").queue();
        }
    }
    
    private void displayKillsLeaderboard(SlashCommandInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        
        // Get top players by kills
        List<Player> allPlayers = playerRepository.getTopPlayersByKills(10);
        
        if (allPlayers.isEmpty()) {
            event.getHook().sendMessage("No player statistics found yet.").queue();
            return;
        }
        
        // Build leaderboard
        StringBuilder description = new StringBuilder();
        
        for (int i = 0; i < allPlayers.size(); i++) {
            Player player = allPlayers.get(i);
            description.append("`").append(i + 1).append(".` **")
                    .append(player.getName()).append("** - ")
                    .append(player.getKills()).append(" kills (")
                    .append(player.getDeaths()).append(" deaths)\n");
        }
        
        event.getHook().sendMessageEmbeds(
                EmbedUtils.infoEmbed("Top Killers Leaderboard", description.toString())
        ).queue();
    }
    
    private void displayKDLeaderboard(SlashCommandInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        
        // Get top 10 players by K/D ratio (minimum 10 kills to qualify)
        List<Player> kdPlayers = playerRepository.getTopPlayersByKD(10, 10);
        
        if (kdPlayers.isEmpty()) {
            event.getHook().sendMessage("No player statistics found yet with enough kills to qualify.").queue();
            return;
        }
        
        // Sort by K/D ratio
        kdPlayers.sort((p1, p2) -> {
            double kd1 = p1.getDeaths() > 0 ? (double) p1.getKills() / p1.getDeaths() : p1.getKills();
            double kd2 = p2.getDeaths() > 0 ? (double) p2.getKills() / p2.getDeaths() : p2.getKills();
            return Double.compare(kd2, kd1);
        });
        
        // Limit to top 10
        if (kdPlayers.size() > 10) {
            kdPlayers = kdPlayers.subList(0, 10);
        }
        
        // Build leaderboard
        StringBuilder description = new StringBuilder();
        
        for (int i = 0; i < kdPlayers.size(); i++) {
            Player player = kdPlayers.get(i);
            double kd = player.getDeaths() > 0 
                    ? (double) player.getKills() / player.getDeaths() 
                    : player.getKills();
            
            description.append("`").append(i + 1).append(".` **")
                    .append(player.getName()).append("** - ")
                    .append(df.format(kd)).append(" K/D (")
                    .append(player.getKills()).append("k/")
                    .append(player.getDeaths()).append("d)\n");
        }
        
        event.getHook().sendMessageEmbeds(
                EmbedUtils.infoEmbed("Top K/D Ratio Leaderboard", description.toString())
        ).queue();
    }
}
