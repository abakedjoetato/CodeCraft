package com.deadside.bot.parsers;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.models.Player;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.sftp.SftpConnector;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Parser for Deadside CSV death log files
 * Format: timestamp;victim;victimId;killer;killerId;weapon;distance
 */
public class DeadsideCsvParser {
    private static final Logger logger = LoggerFactory.getLogger(DeadsideCsvParser.class);
    private final JDA jda;
    private final SftpConnector sftpConnector;
    private final PlayerRepository playerRepository;
    
    // Map to keep track of processed files for each server
    private final Map<String, Set<String>> processedFiles = new HashMap<>();
    
    // Format of the CSV death log: timestamp;victim;victimId;killer;killerId;weapon;distance
    private static final Pattern CSV_LINE_PATTERN = Pattern.compile("^\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2};.*;.*;.*;.*;.*;\\d+;$");
    private static final SimpleDateFormat CSV_DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss");
    
    // Death causes
    private static final Set<String> SUICIDE_CAUSES = new HashSet<>(Arrays.asList(
            "suicide_by_relocation", "suicide", "falling", "bleeding", "drowning", "starvation"
    ));
    
    public DeadsideCsvParser(JDA jda, SftpConnector sftpConnector, PlayerRepository playerRepository) {
        this.jda = jda;
        this.sftpConnector = sftpConnector;
        this.playerRepository = playerRepository;
        
        // Set timezone for date parsing
        CSV_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    /**
     * Process death logs for a server
     * @param server The game server to process
     * @return Number of deaths processed
     */
    public int processDeathLogs(GameServer server) {
        int totalProcessed = 0;
        
        try {
            // Skip if killfeed channel not set
            if (server.getKillfeedChannelId() == 0) {
                return 0;
            }
            
            // Get all CSV files from the server
            List<String> csvFiles = sftpConnector.findDeathlogFiles(server);
            if (csvFiles.isEmpty()) {
                return 0;
            }
            
            // Get list of already processed files for this server
            Set<String> processed = processedFiles.computeIfAbsent(
                    server.getName(), k -> new HashSet<>());
            
            // Sort files by name (which includes date)
            Collections.sort(csvFiles);
            
            // Process each file that hasn't been processed yet
            for (String csvFile : csvFiles) {
                // Skip already processed files
                if (processed.contains(csvFile)) {
                    continue;
                }
                
                try {
                    String content = sftpConnector.readDeathlogFile(server, csvFile);
                    int deathsProcessed = processDeathLog(server, content);
                    totalProcessed += deathsProcessed;
                    
                    // Mark as processed
                    processed.add(csvFile);
                    logger.info("Processed death log file {} for server {}, {} deaths", 
                            csvFile, server.getName(), deathsProcessed);
                } catch (Exception e) {
                    logger.error("Error processing death log file {} for server {}: {}", 
                            csvFile, server.getName(), e.getMessage(), e);
                }
            }
            
            // Limit the size of the processed files set to prevent memory issues
            if (processed.size() > 100) {
                // Keep only the most recent 50 files
                List<String> filesList = new ArrayList<>(processed);
                Collections.sort(filesList);
                Set<String> newProcessed = new HashSet<>(
                        filesList.subList(Math.max(0, filesList.size() - 50), filesList.size()));
                processedFiles.put(server.getName(), newProcessed);
            }
            
            return totalProcessed;
        } catch (Exception e) {
            logger.error("Error processing death logs for server {}: {}", 
                    server.getName(), e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Process a death log file content
     * @param server The game server
     * @param content The file content
     * @return Number of deaths processed
     */
    private int processDeathLog(GameServer server, String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        
        String[] lines = content.split("\\n");
        int count = 0;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            
            // Simple validation that this looks like a death log line
            if (!CSV_LINE_PATTERN.matcher(line).matches()) {
                continue;
            }
            
            try {
                String[] parts = line.split(";");
                if (parts.length < 7) {
                    continue;
                }
                
                // Parse death log entry
                String timestamp = parts[0];
                String victim = parts[1];
                String victimId = parts[2];
                String killer = parts[3];
                String killerId = parts[4];
                String weapon = parts[5];
                int distance = Integer.parseInt(parts[6]);
                
                // Skip old entries (based on timestamp)
                try {
                    Date deathTime = CSV_DATE_FORMAT.parse(timestamp);
                    if (deathTime.getTime() < server.getLastProcessedTimestamp()) {
                        continue;
                    }
                } catch (ParseException e) {
                    logger.warn("Could not parse death timestamp: {}", timestamp);
                }
                
                // Process death
                processDeath(server, timestamp, victim, victimId, killer, killerId, weapon, distance);
                count++;
            } catch (Exception e) {
                logger.warn("Error processing death log line: {}", line, e);
            }
        }
        
        // Update server's last processed timestamp
        if (count > 0) {
            server.setLastProcessedTimestamp(System.currentTimeMillis());
        }
        
        return count;
    }
    
    /**
     * Process a death event
     */
    private void processDeath(GameServer server, String timestamp, String victim, String victimId, 
                             String killer, String killerId, String weapon, int distance) {
        // Handle different death types
        boolean isSuicide = SUICIDE_CAUSES.contains(weapon.toLowerCase()) || 
                            victim.equals(killer);
        
        if (isSuicide) {
            sendSuicideKillfeed(server, timestamp, victim, victimId, weapon);
        } else {
            sendPlayerKillKillfeed(server, timestamp, victim, victimId, killer, killerId, weapon, distance);
            
            // Update player stats
            updateKillerStats(killer, killerId);
            updateVictimStats(victim, victimId);
        }
    }
    
    /**
     * Update the killer's stats
     */
    private void updateKillerStats(String killer, String killerId) {
        try {
            Player player = playerRepository.findByDeadsideId(killerId);
            if (player == null) {
                // If player doesn't exist in database, don't create yet
                // They can be created when they link their account
                return;
            }
            
            // Update kills and score
            player.setKills(player.getKills() + 1);
            player.setScore(player.getScore() + 10); // +10 points per kill
            
            // Add kill reward
            // TODO: Add economy reward here if implemented
            
            playerRepository.save(player);
        } catch (Exception e) {
            logger.error("Error updating killer stats for {}: {}", killer, e.getMessage(), e);
        }
    }
    
    /**
     * Update the victim's stats
     */
    private void updateVictimStats(String victim, String victimId) {
        try {
            Player player = playerRepository.findByDeadsideId(victimId);
            if (player == null) {
                // If player doesn't exist in database, don't create yet
                return;
            }
            
            // Update deaths
            player.setDeaths(player.getDeaths() + 1);
            
            playerRepository.save(player);
        } catch (Exception e) {
            logger.error("Error updating victim stats for {}: {}", victim, e.getMessage(), e);
        }
    }
    
    /**
     * Send killfeed message for player kill
     */
    private void sendPlayerKillKillfeed(GameServer server, String timestamp, String victim, String victimId,
                                       String killer, String killerId, String weapon, int distance) {
        try {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Player Kill")
                    .setDescription(killer + " killed " + victim)
                    .setColor(new Color(255, 0, 0)) // Red
                    .addField("Weapon", weapon, true)
                    .addField("Distance", distance + "m", true)
                    .setFooter(timestamp + " • " + server.getName(), null);
            
            sendToKillfeedChannel(server, embed.build());
        } catch (Exception e) {
            logger.error("Error sending kill feed for {}: {}", victim, e.getMessage(), e);
        }
    }
    
    /**
     * Send killfeed message for suicide
     */
    private void sendSuicideKillfeed(GameServer server, String timestamp, String victim, String victimId, String cause) {
        try {
            // Format the cause for better readability
            String formattedCause = cause.replace("_", " ");
            if (formattedCause.equals("falling")) {
                formattedCause = "fall damage";
            }
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Player Death")
                    .setDescription(victim + " died from " + formattedCause)
                    .setColor(new Color(128, 128, 128)) // Gray
                    .setFooter(timestamp + " • " + server.getName(), null);
            
            sendToKillfeedChannel(server, embed.build());
        } catch (Exception e) {
            logger.error("Error sending suicide feed for {}: {}", victim, e.getMessage(), e);
        }
    }
    
    /**
     * Send embed message to the server's killfeed channel
     */
    private void sendToKillfeedChannel(GameServer server, net.dv8tion.jda.api.entities.MessageEmbed embed) {
        Guild guild = jda.getGuildById(server.getGuildId());
        if (guild == null) {
            logger.warn("Guild not found for server {}: {}", server.getName(), server.getGuildId());
            return;
        }
        
        TextChannel killfeedChannel = guild.getTextChannelById(server.getKillfeedChannelId());
        if (killfeedChannel == null) {
            logger.warn("Killfeed channel not found for server {}: {}", 
                    server.getName(), server.getKillfeedChannelId());
            return;
        }
        
        killfeedChannel.sendMessageEmbeds(embed).queue(
                success -> logger.debug("Sent killfeed to channel {}", killfeedChannel.getId()),
                error -> logger.error("Failed to send killfeed: {}", error.getMessage())
        );
    }
}