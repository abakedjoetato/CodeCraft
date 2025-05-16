package com.deadside.bot.parsers;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.models.KillRecord;
import com.deadside.bot.db.models.Player;
import com.deadside.bot.db.repositories.KillRecordRepository;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.sftp.SftpManager;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Deadside killfeed CSV files
 */
public class KillfeedParser {
    private static final Logger logger = LoggerFactory.getLogger(KillfeedParser.class);
    private final SftpManager sftpManager;
    private final KillRecordRepository killRecordRepository;
    private final PlayerRepository playerRepository;
    private final JDA jda;
    
    // CSV format: "1970/01/01-00:00:00","PlayerName1","killed","PlayerName2","with","WeaponName","from","100m"
    private static final Pattern CSV_PATTERN = Pattern.compile(
            "\"([^\"]+)\",\"([^\"]+)\",\"([^\"]+)\",\"([^\"]+)\",\"([^\"]+)\",\"([^\"]+)\",\"([^\"]+)\",\"(\\d+)m\""
    );
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
    
    public KillfeedParser(JDA jda) {
        this.jda = jda;
        this.sftpManager = new SftpManager();
        this.killRecordRepository = new KillRecordRepository();
        this.playerRepository = new PlayerRepository();
    }
    
    /**
     * Process killfeed for a server
     * @param server The game server to process
     * @return Number of new kill records processed
     */
    public int processServer(GameServer server) {
        try {
            TextChannel killfeedChannel = jda.getTextChannelById(server.getKillfeedChannelId());
            if (killfeedChannel == null) {
                logger.warn("Killfeed channel not found for server: {}", server.getName());
                return 0;
            }
            
            // Get CSV files
            List<String> files = sftpManager.getKillfeedFiles(server);
            if (files.isEmpty()) {
                logger.warn("No killfeed files found for server: {}", server.getName());
                return 0;
            }
            
            // Sort files by name (should be date-based)
            Collections.sort(files);
            
            String lastProcessedFile = server.getLastProcessedKillfeedFile();
            long lastProcessedLine = server.getLastProcessedKillfeedLine();
            
            // If no file has been processed yet, start with the newest file
            if (lastProcessedFile.isEmpty()) {
                lastProcessedFile = files.get(files.size() - 1);
                lastProcessedLine = -1;
            }
            
            // Check if we need to move to a newer file
            int fileIndex = files.indexOf(lastProcessedFile);
            if (fileIndex < 0) {
                // File no longer exists, start with the newest file
                lastProcessedFile = files.get(files.size() - 1);
                lastProcessedLine = -1;
            } else if (fileIndex < files.size() - 1) {
                // Newer files available, move to the next one
                lastProcessedFile = files.get(fileIndex + 1);
                lastProcessedLine = -1;
            }
            
            // Process the file
            String fileContent = sftpManager.readKillfeedFile(server, lastProcessedFile);
            if (fileContent.isEmpty()) {
                logger.warn("Empty or unreadable killfeed file: {} for server: {}", 
                        lastProcessedFile, server.getName());
                return 0;
            }
            
            String[] lines = fileContent.split("\n");
            List<KillRecord> newRecords = new ArrayList<>();
            int processedKills = 0;
            
            // Process each line after the last processed line
            for (int i = 0; i < lines.length; i++) {
                if (i <= lastProcessedLine) continue;
                
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                
                KillRecord killRecord = parseKillRecord(line, server);
                if (killRecord != null) {
                    newRecords.add(killRecord);
                    processedKills++;
                    
                    // Update player stats
                    updatePlayerStats(killRecord);
                    
                    // Send to Discord channel
                    sendKillfeedMessage(killfeedChannel, killRecord);
                }
                
                lastProcessedLine = i;
            }
            
            // Save all new records to database
            if (!newRecords.isEmpty()) {
                killRecordRepository.saveAll(newRecords);
            }
            
            // Update server progress
            server.updateKillfeedProgress(lastProcessedFile, lastProcessedLine);
            
            logger.info("Processed {} new kills for server: {}", processedKills, server.getName());
            return processedKills;
        } catch (Exception e) {
            logger.error("Error processing killfeed for server: {}", server.getName(), e);
            return 0;
        }
    }
    
    /**
     * Parse a CSV line into a KillRecord
     */
    private KillRecord parseKillRecord(String line, GameServer server) {
        Matcher matcher = CSV_PATTERN.matcher(line);
        if (!matcher.matches()) {
            logger.warn("Killfeed line does not match expected format: {}", line);
            return null;
        }
        
        try {
            String timestamp = matcher.group(1);
            String killer = matcher.group(2);
            String action = matcher.group(3);
            String victim = matcher.group(4);
            String weapon = matcher.group(6);
            String distanceStr = matcher.group(8);
            
            if (!action.equals("killed")) {
                logger.warn("Unknown killfeed action: {} in line: {}", action, line);
                return null;
            }
            
            long distance = Long.parseLong(distanceStr);
            long timeMs = DATE_FORMAT.parse(timestamp).getTime();
            
            return new KillRecord(
                    server.getGuildId(),
                    server.getName(),
                    killer,
                    victim,
                    weapon,
                    distance,
                    timeMs,
                    line
            );
        } catch (ParseException e) {
            logger.error("Error parsing killfeed timestamp in line: {}", line, e);
            return null;
        } catch (NumberFormatException e) {
            logger.error("Error parsing killfeed distance in line: {}", line, e);
            return null;
        } catch (Exception e) {
            logger.error("Error parsing killfeed line: {}", line, e);
            return null;
        }
    }
    
    /**
     * Update player statistics from a kill record
     */
    private void updatePlayerStats(KillRecord record) {
        try {
            // Find or create killer player
            Player killer = playerRepository.findByName(record.getKiller());
            if (killer == null) {
                // Create new player with a generated ID based on name
                killer = new Player(record.getKiller().toLowerCase().replace(" ", "_") + "_id", record.getKiller());
                playerRepository.save(killer);
            }
            
            // Find or create victim player
            Player victim = playerRepository.findByName(record.getVictim());
            if (victim == null) {
                // Create new player with a generated ID based on name
                victim = new Player(record.getVictim().toLowerCase().replace(" ", "_") + "_id", record.getVictim());
                playerRepository.save(victim);
            }
            
            // Update killer stats
            killer.addKill();
            playerRepository.save(killer);
            
            // Update victim stats
            victim.addDeath();
            playerRepository.save(victim);
            
            // Track weapon stats
            if (killer.getMostUsedWeapon().equals(record.getWeapon())) {
                killer.setMostUsedWeaponKills(killer.getMostUsedWeaponKills() + 1);
            } else if (killer.getMostUsedWeaponKills() == 0) {
                killer.setMostUsedWeapon(record.getWeapon());
                killer.setMostUsedWeaponKills(1);
            }
            // More weapon tracking logic would go here
            
            // Track victim stats
            if (killer.getMostKilledPlayer().equals(record.getVictim())) {
                killer.setMostKilledPlayerCount(killer.getMostKilledPlayerCount() + 1);
            } else if (killer.getMostKilledPlayerCount() == 0) {
                killer.setMostKilledPlayer(record.getVictim());
                killer.setMostKilledPlayerCount(1);
            }
            // More victim tracking logic would go here
            
            // Track killer stats for victim
            if (victim.getKilledByMost().equals(record.getKiller())) {
                victim.setKilledByMostCount(victim.getKilledByMostCount() + 1);
            } else if (victim.getKilledByMostCount() == 0) {
                victim.setKilledByMost(record.getKiller());
                victim.setKilledByMostCount(1);
            }
            // More killer tracking logic would go here
        } catch (Exception e) {
            logger.error("Error updating player stats for kill record: {} -> {}", 
                    record.getKiller(), record.getVictim(), e);
        }
    }
    
    /**
     * Send a killfeed message to Discord
     */
    private void sendKillfeedMessage(TextChannel channel, KillRecord record) {
        if (channel == null) return;
        
        String title = "🎯 Killfeed";
        String description = String.format(
                "**%s** killed **%s**\n" +
                "Weapon: **%s**\n" +
                "Distance: **%d m**\n" +
                "Time: <t:%d:R>",
                record.getKiller(),
                record.getVictim(),
                record.getWeapon(),
                record.getDistance(),
                record.getTimestamp() / 1000
        );
        
        channel.sendMessageEmbeds(EmbedUtils.killfeedEmbed(title, description)).queue();
    }
}
