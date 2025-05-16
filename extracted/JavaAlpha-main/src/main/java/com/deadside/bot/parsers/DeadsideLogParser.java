package com.deadside.bot.parsers;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.sftp.SftpConnector;
import com.deadside.bot.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Deadside.log files
 * Monitors server logs for various events like player joins/leaves, missions, airdrops, etc.
 */
public class DeadsideLogParser {
    private static final Logger logger = LoggerFactory.getLogger(DeadsideLogParser.class);
    private final JDA jda;
    private final GameServerRepository serverRepository;
    private final SftpConnector sftpConnector;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // Map to keep track of last processed line for each server
    private final Map<String, Integer> lastLineProcessed = new HashMap<>();
    
    // Regex patterns for different event types
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\[(\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2}:\\d{3})\\]\\[\\s*\\d+\\]");
    
    // Updated Player patterns based on actual log format
    private static final Pattern PLAYER_JOIN_PATTERN = Pattern.compile("LogOnline: Warning: Player \\|(.+?) successfully registered!");
    private static final Pattern PLAYER_LEAVE_PATTERN = Pattern.compile("LogOnline: Warning: Player \\|(.+?) successfully unregistered from the session.");
    private static final Pattern PLAYER_QUEUE_PATTERN = Pattern.compile("LogNet: Warning: Player (.+?) joined the queue");
    private static final Pattern PLAYER_CONNECTION_TIMEOUT = Pattern.compile("LogNet: Warning: UNetConnection::Tick: Connection TIMED OUT.+UniqueId: EOS:\\|(.+?)($|,)");
    
    // Legacy patterns (keeping for backward compatibility)
    private static final Pattern LEGACY_PLAYER_JOIN_PATTERN = Pattern.compile("LogSFPS: \\[Login\\] Player (.+?) connected");
    private static final Pattern LEGACY_PLAYER_LEAVE_PATTERN = Pattern.compile("LogSFPS: \\[Logout\\] Player (.+?) disconnected");
    
    // Combat and event patterns
    private static final Pattern PLAYER_KILLED_PATTERN = Pattern.compile("LogSFPS: \\[Kill\\] (.+?) killed (.+?) with (.+?) at distance (\\d+)");
    private static final Pattern PLAYER_DIED_PATTERN = Pattern.compile("LogSFPS: \\[Death\\] (.+?) died from (.+?)");
    private static final Pattern AIRDROP_PATTERN = Pattern.compile("LogSFPS: AirDrop switched to (\\w+)");
    private static final Pattern HELI_CRASH_PATTERN = Pattern.compile("LogSFPS: Helicopter crash spawned at position (.+)");
    private static final Pattern TRADER_EVENT_PATTERN = Pattern.compile("LogSFPS: Trader event started at (.+)");
    private static final Pattern MISSION_PATTERN = Pattern.compile("LogSFPS: Mission (.+?) switched to (\\w+)");
    
    // Additional event patterns
    private static final Pattern CONVOY_PATTERN = Pattern.compile("LogSFPS: Convoy (.+?) (started|spawned|arrived)");
    private static final Pattern WANDERING_TRADER_PATTERN = Pattern.compile("LogSFPS: WanderingTrader (spawned|arrived) at (.+)");
    private static final Pattern DYNAMIC_EVENT_PATTERN = Pattern.compile("LogSFPS: DynamicEvent (.+?) (started|ended|activated)");
    
    // Event state constants
    private static final String STATE_AIRDROP_FLYING = "Flying";
    private static final String STATE_AIRDROP_DROPPING = "Dropping";
    private static final String STATE_AIRDROP_WAITING = "Waiting";
    
    private static final String STATE_MISSION_INITIAL = "INITIAL";
    private static final String STATE_MISSION_READY = "READY";
    private static final String STATE_MISSION_WAITING = "WAITING";
    private static final String STATE_MISSION_ACTIVE = "ACTIVE";
    private static final String STATE_MISSION_COMPLETED = "COMPLETED";
    
    // Log parsing interval in seconds
    private static final int LOG_PARSE_INTERVAL = 60; // 1 minute
    
    public DeadsideLogParser(JDA jda, GameServerRepository serverRepository, SftpConnector sftpConnector) {
        this.jda = jda;
        this.serverRepository = serverRepository;
        this.sftpConnector = sftpConnector;
    }
    
    /**
     * Start the log parser scheduler
     */
    public void startScheduler() {
        logger.info("Starting Deadside log parser scheduler (interval: {} seconds)", LOG_PARSE_INTERVAL);
        scheduler.scheduleAtFixedRate(this::processAllServerLogs, 0, LOG_PARSE_INTERVAL, TimeUnit.SECONDS);
    }
    
    /**
     * Stop the log parser scheduler
     */
    public void stopScheduler() {
        logger.info("Stopping Deadside log parser scheduler");
        scheduler.shutdown();
    }
    
    /**
     * Process logs for all servers in the database
     */
    public void processAllServerLogs() {
        try {
            List<GameServer> servers = serverRepository.findAll();
            
            for (GameServer server : servers) {
                try {
                    // Skip servers without log channel configured
                    if (server.getLogChannelId() == 0) {
                        continue;
                    }
                    
                    parseServerLog(server);
                } catch (Exception e) {
                    logger.error("Error parsing logs for server {}: {}", server.getName(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error in log parser scheduler: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Parse the log file for a specific server
     */
    private void parseServerLog(GameServer server) {
        String logPath = getServerLogPath(server);
        
        try {
            // Get the last line processed for this server
            int lastLine = lastLineProcessed.getOrDefault(server.getName(), 0);
            
            // Read new lines from the log file
            List<String> newLines;
            
            try {
                // Read lines after the last processed line
                newLines = sftpConnector.readLinesAfter(server, logPath, lastLine);
                
                if (newLines.isEmpty()) {
                    return;
                }
                
                // Update last processed line
                lastLineProcessed.put(server.getName(), lastLine + newLines.size());
                
                // Process new lines
                processLogLines(server, newLines);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("No such file")) {
                    logger.warn("Log file not found for server {}: {}", server.getName(), logPath);
                } else if (e.getMessage() != null && e.getMessage().contains("smaller than expected")) {
                    // Log rotation detected, reset counter
                    logger.info("Log rotation detected for server {}, resetting line counter", server.getName());
                    lastLineProcessed.put(server.getName(), 0);
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            logger.error("Error reading log file for server {}: {}", server.getName(), e.getMessage(), e);
        }
    }
    
    /**
     * Process log lines and detect events
     */
    private void processLogLines(GameServer server, List<String> lines) {
        Set<String> joinedPlayers = new HashSet<>();
        Set<String> leftPlayers = new HashSet<>();
        
        for (String line : lines) {
            // Extract timestamp if present
            String timestamp = "";
            Matcher timestampMatcher = TIMESTAMP_PATTERN.matcher(line);
            if (timestampMatcher.find()) {
                timestamp = timestampMatcher.group(1);
            }
            
            // Player join events
            Matcher joinMatcher = PLAYER_JOIN_PATTERN.matcher(line);
            if (joinMatcher.find()) {
                String playerName = joinMatcher.group(1).trim();
                joinedPlayers.add(playerName);
                // Process individually for immediate notification
                sendPlayerJoinNotification(server, playerName, timestamp);
                continue;
            }
            
            // Player leave events
            Matcher leaveMatcher = PLAYER_LEAVE_PATTERN.matcher(line);
            if (leaveMatcher.find()) {
                String playerName = leaveMatcher.group(1).trim();
                leftPlayers.add(playerName);
                // Process individually for immediate notification
                sendPlayerLeaveNotification(server, playerName, timestamp);
                continue;
            }
            
            // Player killed events
            Matcher killedMatcher = PLAYER_KILLED_PATTERN.matcher(line);
            if (killedMatcher.find()) {
                String killer = killedMatcher.group(1).trim();
                String victim = killedMatcher.group(2).trim();
                String weapon = killedMatcher.group(3).trim();
                String distance = killedMatcher.group(4).trim();
                
                // Send kill notification
                sendKillNotification(server, killer, victim, weapon, distance, timestamp);
                continue;
            }
            
            // Player died events
            Matcher diedMatcher = PLAYER_DIED_PATTERN.matcher(line);
            if (diedMatcher.find()) {
                String player = diedMatcher.group(1).trim();
                String cause = diedMatcher.group(2).trim();
                
                // Send death notification
                sendDeathNotification(server, player, cause, timestamp);
                continue;
            }
            
            // Airdrop events
            Matcher airdropMatcher = AIRDROP_PATTERN.matcher(line);
            if (airdropMatcher.find()) {
                String status = airdropMatcher.group(1).trim();
                if (status.equalsIgnoreCase("Waiting")) {
                    // Airdrop is now available
                    sendEventNotification(server, "Airdrop Event", "An airdrop is inbound!", 
                            "Status: " + status, Color.BLUE, timestamp);
                } else if (status.equalsIgnoreCase("Dropped") || status.equalsIgnoreCase("Active")) {
                    // Airdrop has been deployed
                    sendEventNotification(server, "Airdrop Event", "An airdrop has been deployed!", 
                            "Status: " + status, Color.BLUE, timestamp);
                }
                continue;
            }
            
            // Helicopter crash events
            Matcher heliMatcher = HELI_CRASH_PATTERN.matcher(line);
            if (heliMatcher.find()) {
                String position = heliMatcher.group(1).trim();
                sendEventNotification(server, "Helicopter Crash", "A helicopter has crashed nearby!", 
                        "Location: " + position, new Color(150, 75, 0), timestamp); // Brown
                continue;
            }
            
            // Trader events
            Matcher traderMatcher = TRADER_EVENT_PATTERN.matcher(line);
            if (traderMatcher.find()) {
                String position = traderMatcher.group(1).trim();
                sendEventNotification(server, "Trader Event", "A special trader has appeared!", 
                        "Location: " + position, new Color(0, 128, 0), timestamp); // Green
                continue;
            }
            
            // Mission events
            Matcher missionMatcher = MISSION_PATTERN.matcher(line);
            if (missionMatcher.find()) {
                String missionName = missionMatcher.group(1).trim();
                String status = missionMatcher.group(2).trim();
                
                // Only notify on important mission state changes and only for high level missions
                if (isMissionLevelReportable(missionName)) {
                    if (status.equalsIgnoreCase("ACTIVE")) {
                        // Mission is now active and players can interact with it
                        sendEventNotification(server, "High Level Mission", "A high-level mission is now active!", 
                                getMissionLocation(missionName), 
                                new Color(148, 0, 211), timestamp); // Purple
                    } else if (status.equalsIgnoreCase("COMPLETED")) {
                        // Mission was successfully completed
                        sendEventNotification(server, "Mission Completed", "A high-level mission has been completed!", 
                                getMissionLocation(missionName), 
                                new Color(76, 175, 80), timestamp); // Green
                    }
                }
                continue;
            }
            
            // Convoy events
            Matcher convoyMatcher = CONVOY_PATTERN.matcher(line);
            if (convoyMatcher.find()) {
                String convoyName = convoyMatcher.group(1).trim();
                String action = convoyMatcher.group(2).trim();
                
                if (action.equalsIgnoreCase("started") || action.equalsIgnoreCase("spawned")) {
                    sendEventNotification(server, "Convoy Event", "A military convoy has been spotted!", 
                            "A valuable military convoy is moving through the region.", 
                            new Color(255, 140, 0), timestamp); // Orange
                } else if (action.equalsIgnoreCase("arrived")) {
                    sendEventNotification(server, "Convoy Arrived", "A military convoy has arrived at its destination!", 
                            "The convoy has stopped and can now be looted.", 
                            new Color(255, 69, 0), timestamp); // Orange Red
                }
                continue;
            }
            
            // Wandering Trader events
            Matcher wanderingTraderMatcher = WANDERING_TRADER_PATTERN.matcher(line);
            if (wanderingTraderMatcher.find()) {
                String action = wanderingTraderMatcher.group(1).trim();
                String location = wanderingTraderMatcher.group(2).trim();
                
                sendEventNotification(server, "Wandering Trader", "A wandering trader has appeared!", 
                        "Location: " + location, 
                        new Color(46, 204, 113), timestamp); // Green
                continue;
            }
            
            // Dynamic Events
            Matcher dynamicEventMatcher = DYNAMIC_EVENT_PATTERN.matcher(line);
            if (dynamicEventMatcher.find()) {
                String eventName = dynamicEventMatcher.group(1).trim();
                String status = dynamicEventMatcher.group(2).trim();
                
                if (status.equalsIgnoreCase("started") || status.equalsIgnoreCase("activated")) {
                    sendEventNotification(server, "Dynamic Event", "A special event has begun!", 
                            "Event: " + eventName, 
                            new Color(142, 68, 173), timestamp); // Purple
                }
                continue;
            }
        }
        
        // Send summary if needed for multiple players
        if (joinedPlayers.size() > 3) {
            sendPlayerSummary(server, joinedPlayers, true);
        }
        
        if (leftPlayers.size() > 3) {
            sendPlayerSummary(server, leftPlayers, false);
        }
    }
    
    /**
     * Send notification for player kill
     */
    private void sendKillNotification(GameServer server, String killer, String victim, 
                                      String weapon, String distance, String timestamp) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Player Kill")
                .setDescription(killer + " killed " + victim)
                .setColor(Color.RED)
                .addField("Weapon", weapon, true)
                .addField("Distance", distance + "m", true)
                .setTimestamp(new Date().toInstant());
        
        if (!timestamp.isEmpty()) {
            embed.setFooter(timestamp + " • " + server.getName(), null);
        } else {
            embed.setFooter(server.getName(), null);
        }
        
        sendToLogChannel(server, embed.build());
    }
    
    /**
     * Send notification for player death (not kill)
     */
    private void sendDeathNotification(GameServer server, String player, String cause, String timestamp) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Player Death")
                .setDescription(player + " died from " + cause)
                .setColor(Color.GRAY)
                .setTimestamp(new Date().toInstant());
        
        if (!timestamp.isEmpty()) {
            embed.setFooter(timestamp + " • " + server.getName(), null);
        } else {
            embed.setFooter(server.getName(), null);
        }
        
        sendToLogChannel(server, embed.build());
    }
    
    /**
     * Send notification for player joining
     */
    private void sendPlayerJoinNotification(GameServer server, String playerName, String timestamp) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Player Connected")
                .setDescription(playerName + " has joined the server")
                .setColor(Color.GREEN)
                .setTimestamp(new Date().toInstant());
        
        if (!timestamp.isEmpty()) {
            embed.setFooter(timestamp + " • " + server.getName(), null);
        } else {
            embed.setFooter(server.getName(), null);
        }
        
        sendToLogChannel(server, embed.build());
    }
    
    /**
     * Send notification for player leaving
     */
    private void sendPlayerLeaveNotification(GameServer server, String playerName, String timestamp) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Player Disconnected")
                .setDescription(playerName + " has left the server")
                .setColor(Color.RED)
                .setTimestamp(new Date().toInstant());
        
        if (!timestamp.isEmpty()) {
            embed.setFooter(timestamp + " • " + server.getName(), null);
        } else {
            embed.setFooter(server.getName(), null);
        }
        
        sendToLogChannel(server, embed.build());
    }
    
    /**
     * Send summary for multiple player joins/leaves
     */
    private void sendPlayerSummary(GameServer server, Set<String> players, boolean joining) {
        String title = joining ? "Multiple Players Connected" : "Multiple Players Disconnected";
        Color color = joining ? Color.GREEN : Color.RED;
        
        StringBuilder desc = new StringBuilder();
        int count = 0;
        for (String player : players) {
            if (count < 10) { // Limit to 10 names to avoid too long messages
                desc.append("• ").append(player).append("\n");
                count++;
            } else {
                desc.append("• And ").append(players.size() - 10).append(" more players...");
                break;
            }
        }
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(desc.toString())
                .setColor(color)
                .setTimestamp(new Date().toInstant())
                .setFooter(server.getName(), null);
        
        sendToLogChannel(server, embed.build());
    }
    
    /**
     * Send notification for server events
     */
    private void sendEventNotification(GameServer server, String title, String description, 
                                       String details, Color color, String timestamp) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setTimestamp(new Date().toInstant());
        
        if (details != null && !details.isEmpty()) {
            embed.addField("Details", details, false);
        }
        
        if (!timestamp.isEmpty()) {
            embed.setFooter(timestamp + " • " + server.getName(), null);
        } else {
            embed.setFooter(server.getName(), null);
        }
        
        sendToLogChannel(server, embed.build());
    }
    
    /**
     * Send embed message to the server's log channel
     */
    private void sendToLogChannel(GameServer server, net.dv8tion.jda.api.entities.MessageEmbed embed) {
        Guild guild = jda.getGuildById(server.getGuildId());
        if (guild == null) {
            logger.warn("Guild not found for server {}: {}", server.getName(), server.getGuildId());
            return;
        }
        
        TextChannel logChannel = guild.getTextChannelById(server.getLogChannelId());
        if (logChannel == null) {
            logger.warn("Log channel not found for server {}: {}", server.getName(), server.getLogChannelId());
            return;
        }
        
        logChannel.sendMessageEmbeds(embed).queue(
                success -> logger.debug("Sent log notification to channel {}", logChannel.getId()),
                error -> logger.error("Failed to send log notification: {}", error.getMessage())
        );
    }
    
    /**
     * Get the path to the server log file
     */
    private String getServerLogPath(GameServer server) {
        // Base path for Deadside server logs
        String basePath = server.getGameServerId() + "/Deadside/Saved/Logs/";
        return basePath + "Deadside.log";
    }
    
    /**
     * Helper method to extract location information from mission name and determine mission level
     * @return String containing normalized location name with level information
     */
    private String getMissionLocation(String missionName) {
        // Extract location and level from mission name pattern
        // Most mission names follow format: GA_LocationName_XX_MissionNumber or GA_LocationName_MissionNumber
        if (missionName.contains("_")) {
            String[] parts = missionName.split("_");
            if (parts.length >= 2) {
                // Convert location name to a more readable format
                String location = parts[1];
                int missionLevel = 1; // Default level
                
                // Try to extract mission level from the name
                for (String part : parts) {
                    // Check if part contains only digits or has a number followed by "Mis"
                    if (part.matches("\\d+") || (part.startsWith("0") && part.length() == 2)) {
                        try {
                            missionLevel = Integer.parseInt(part);
                        } catch (NumberFormatException e) {
                            // If parsing fails, try to extract from a pattern like "03Mis"
                            if (part.matches("\\d+.*")) {
                                String numPart = part.replaceAll("[^0-9]", "");
                                try {
                                    missionLevel = Integer.parseInt(numPart);
                                } catch (NumberFormatException ex) {
                                    // Ignore if we can't parse the level
                                }
                            }
                        }
                    }
                }
                
                // Format the location name
                String formattedLocation;
                
                // Special case handling for common locations
                if (location.equalsIgnoreCase("Settle")) {
                    if (missionName.contains("ChernyLog")) {
                        formattedLocation = "Cherny Log Settlement";
                    } else if (missionName.contains("05")) {
                        formattedLocation = "Northern Settlement";
                    } else if (missionName.contains("09")) {
                        formattedLocation = "Eastern Settlement";
                    } else {
                        formattedLocation = "Settlement";
                    }
                } else if (location.equalsIgnoreCase("Military")) {
                    formattedLocation = "Military Base";
                } else if (location.equalsIgnoreCase("Sawmill")) {
                    formattedLocation = "Sawmill";
                } else if (location.equalsIgnoreCase("Lighthouse")) {
                    formattedLocation = "Lighthouse";
                } else if (location.equalsIgnoreCase("Bunker")) {
                    formattedLocation = "Bunker";
                } else if (location.equalsIgnoreCase("Ind")) {
                    formattedLocation = "Industrial Zone";
                } else if (location.equalsIgnoreCase("KhimMash")) {
                    formattedLocation = "Chemical Plant";
                } else if (location.equalsIgnoreCase("PromZone")) {
                    formattedLocation = "Industrial Complex";
                } else if (location.equalsIgnoreCase("Kamensk")) {
                    formattedLocation = "Kamensk";
                } else if (location.equalsIgnoreCase("Elevator")) {
                    formattedLocation = "Grain Elevator";
                } else if (location.equalsIgnoreCase("Bochki")) {
                    formattedLocation = "Bochki Storage";
                } else if (location.equalsIgnoreCase("Vostok")) {
                    formattedLocation = "Vostok";
                } else if (location.equalsIgnoreCase("Beregovoy")) {
                    formattedLocation = "Beregovoy";
                } else if (location.equalsIgnoreCase("Krasnoe")) {
                    formattedLocation = "Krasnoe";
                } else if (location.equalsIgnoreCase("Dubovoe")) {
                    formattedLocation = "Dubovoe";
                } else if (location.equalsIgnoreCase("Airport")) {
                    formattedLocation = "Airfield";
                } else {
                    // Default case: just return the location part with first letter capitalized
                    formattedLocation = location.substring(0, 1).toUpperCase() + location.substring(1);
                }
                
                // Return formatted location with level
                return "Lvl " + missionLevel + " Mission at " + formattedLocation;
            }
        }
        
        // Default response if we can't parse the location
        return "Unknown Location";
    }
    
    /**
     * Check if mission level is high enough to be reported
     * Only reports missions of level 3 or higher
     */
    private boolean isMissionLevelReportable(String missionName) {
        int missionLevel = 1; // Default level
        
        if (missionName.contains("_")) {
            String[] parts = missionName.split("_");
            
            // Try to extract mission level from the name
            for (String part : parts) {
                // Look for parts with numbers that might indicate level
                if (part.matches("\\d+") || (part.startsWith("0") && part.length() == 2)) {
                    try {
                        missionLevel = Integer.parseInt(part);
                    } catch (NumberFormatException e) {
                        // If parsing fails, try to extract from a pattern like "03Mis"
                        if (part.matches("\\d+.*")) {
                            String numPart = part.replaceAll("[^0-9]", "");
                            try {
                                missionLevel = Integer.parseInt(numPart);
                            } catch (NumberFormatException ex) {
                                // Ignore if we can't parse the level
                            }
                        }
                    }
                }
            }
        }
        
        // Only report level 3 and above missions
        return missionLevel >= 3;
    }
}