import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventParserTest {
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
    
    public static void main(String[] args) {
        String logFile = "attached_assets/Deadside.log";
        
        // Event counters
        Map<String, Integer> eventCounts = new HashMap<>();
        eventCounts.put("Player Joins", 0);
        eventCounts.put("Player Leaves", 0);
        eventCounts.put("Player Timeouts", 0);
        eventCounts.put("Queue Events", 0);
        eventCounts.put("Player Kills", 0);
        eventCounts.put("Player Deaths", 0);
        eventCounts.put("Airdrop Flying", 0);
        eventCounts.put("Airdrop Dropping", 0);
        eventCounts.put("Airdrop Waiting", 0);
        eventCounts.put("Helicopter Crashes", 0);
        eventCounts.put("Trader Events", 0);
        eventCounts.put("Mission Initial", 0);
        eventCounts.put("Mission Ready", 0);
        eventCounts.put("Mission Waiting", 0);
        eventCounts.put("Mission Active", 0);
        eventCounts.put("Mission Completed", 0);
        eventCounts.put("Convoy Events", 0);
        eventCounts.put("Wandering Trader Events", 0);
        eventCounts.put("Dynamic Events", 0);
        
        // Mission level counter
        Map<Integer, Integer> missionLevelCount = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            missionLevelCount.put(i, 0);
        }
        
        // Mission events to print details of
        Set<String> missionEventsDetails = new HashSet<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            int lineCount = 0;
            
            System.out.println("Parsing Deadside.log for events...");
            System.out.println("------------------------------------------------------------");
            
            while ((line = reader.readLine()) != null) {
                lineCount++;
                
                // Player join events
                Matcher joinMatcher = PLAYER_JOIN_PATTERN.matcher(line);
                if (joinMatcher.find()) {
                    eventCounts.put("Player Joins", eventCounts.get("Player Joins") + 1);
                    continue;
                }
                
                // Player leave events
                Matcher leaveMatcher = PLAYER_LEAVE_PATTERN.matcher(line);
                if (leaveMatcher.find()) {
                    eventCounts.put("Player Leaves", eventCounts.get("Player Leaves") + 1);
                    continue;
                }
                
                // Connection timeout events
                Matcher timeoutMatcher = PLAYER_CONNECTION_TIMEOUT.matcher(line);
                if (timeoutMatcher.find()) {
                    eventCounts.put("Player Timeouts", eventCounts.get("Player Timeouts") + 1);
                    continue;
                }
                
                // Player queue events
                Matcher queueMatcher = PLAYER_QUEUE_PATTERN.matcher(line);
                if (queueMatcher.find()) {
                    eventCounts.put("Queue Events", eventCounts.get("Queue Events") + 1);
                    continue;
                }
                
                // Legacy join/leave patterns
                Matcher legacyJoinMatcher = LEGACY_PLAYER_JOIN_PATTERN.matcher(line);
                if (legacyJoinMatcher.find()) {
                    System.out.println("Legacy Player Join found at line " + lineCount + ": " + line);
                    continue;
                }
                
                Matcher legacyLeaveMatcher = LEGACY_PLAYER_LEAVE_PATTERN.matcher(line);
                if (legacyLeaveMatcher.find()) {
                    System.out.println("Legacy Player Leave found at line " + lineCount + ": " + line);
                    continue;
                }
                
                // Combat events
                Matcher killedMatcher = PLAYER_KILLED_PATTERN.matcher(line);
                if (killedMatcher.find()) {
                    eventCounts.put("Player Kills", eventCounts.get("Player Kills") + 1);
                    System.out.println("Player Kill found at line " + lineCount + ": " + line);
                    continue;
                }
                
                Matcher diedMatcher = PLAYER_DIED_PATTERN.matcher(line);
                if (diedMatcher.find()) {
                    eventCounts.put("Player Deaths", eventCounts.get("Player Deaths") + 1);
                    System.out.println("Player Death found at line " + lineCount + ": " + line);
                    continue;
                }
                
                // Airdrop events
                Matcher airdropMatcher = AIRDROP_PATTERN.matcher(line);
                if (airdropMatcher.find()) {
                    String status = airdropMatcher.group(1).trim();
                    if (status.equals(STATE_AIRDROP_FLYING)) {
                        eventCounts.put("Airdrop Flying", eventCounts.get("Airdrop Flying") + 1);
                        System.out.println("Airdrop Flying at line " + lineCount + ": " + line);
                    } else if (status.equals(STATE_AIRDROP_DROPPING)) {
                        eventCounts.put("Airdrop Dropping", eventCounts.get("Airdrop Dropping") + 1);
                        System.out.println("Airdrop Dropping at line " + lineCount + ": " + line);
                    } else if (status.equals(STATE_AIRDROP_WAITING)) {
                        eventCounts.put("Airdrop Waiting", eventCounts.get("Airdrop Waiting") + 1);
                        System.out.println("Airdrop Waiting at line " + lineCount + ": " + line);
                    }
                    continue;
                }
                
                // Helicopter crash events
                Matcher heliMatcher = HELI_CRASH_PATTERN.matcher(line);
                if (heliMatcher.find()) {
                    eventCounts.put("Helicopter Crashes", eventCounts.get("Helicopter Crashes") + 1);
                    System.out.println("Helicopter Crash at line " + lineCount + ": " + line);
                    continue;
                }
                
                // Trader events
                Matcher traderMatcher = TRADER_EVENT_PATTERN.matcher(line);
                if (traderMatcher.find()) {
                    eventCounts.put("Trader Events", eventCounts.get("Trader Events") + 1);
                    System.out.println("Trader Event at line " + lineCount + ": " + line);
                    continue;
                }
                
                // Mission events
                Matcher missionMatcher = MISSION_PATTERN.matcher(line);
                if (missionMatcher.find()) {
                    String missionName = missionMatcher.group(1).trim();
                    String status = missionMatcher.group(2).trim();
                    
                    // Track mission by state
                    if (status.equals(STATE_MISSION_INITIAL)) {
                        eventCounts.put("Mission Initial", eventCounts.get("Mission Initial") + 1);
                    } else if (status.equals(STATE_MISSION_READY)) {
                        eventCounts.put("Mission Ready", eventCounts.get("Mission Ready") + 1);
                    } else if (status.equals(STATE_MISSION_WAITING)) {
                        eventCounts.put("Mission Waiting", eventCounts.get("Mission Waiting") + 1);
                    } else if (status.equals(STATE_MISSION_ACTIVE)) {
                        eventCounts.put("Mission Active", eventCounts.get("Mission Active") + 1);
                        missionEventsDetails.add("Mission ACTIVE: " + missionName + " at line " + lineCount);
                    } else if (status.equals(STATE_MISSION_COMPLETED)) {
                        eventCounts.put("Mission Completed", eventCounts.get("Mission Completed") + 1);
                        missionEventsDetails.add("Mission COMPLETED: " + missionName + " at line " + lineCount);
                    }
                    
                    // Analyze mission level
                    int missionLevel = getMissionLevel(missionName);
                    missionLevelCount.put(missionLevel, missionLevelCount.get(missionLevel) + 1);
                    
                    continue;
                }
                
                // Convoy events
                Matcher convoyMatcher = CONVOY_PATTERN.matcher(line);
                if (convoyMatcher.find()) {
                    eventCounts.put("Convoy Events", eventCounts.get("Convoy Events") + 1);
                    System.out.println("Convoy Event at line " + lineCount + ": " + line);
                    continue;
                }
                
                // Wandering Trader events
                Matcher wanderingTraderMatcher = WANDERING_TRADER_PATTERN.matcher(line);
                if (wanderingTraderMatcher.find()) {
                    eventCounts.put("Wandering Trader Events", eventCounts.get("Wandering Trader Events") + 1);
                    System.out.println("Wandering Trader at line " + lineCount + ": " + line);
                    continue;
                }
                
                // Dynamic Events
                Matcher dynamicEventMatcher = DYNAMIC_EVENT_PATTERN.matcher(line);
                if (dynamicEventMatcher.find()) {
                    eventCounts.put("Dynamic Events", eventCounts.get("Dynamic Events") + 1);
                    System.out.println("Dynamic Event at line " + lineCount + ": " + line);
                    continue;
                }
            }
            
            // Print summary of all events found
            System.out.println("\n====== EVENT SUMMARY ======");
            System.out.println("Total lines processed: " + lineCount);
            System.out.println("\nPlayer Events:");
            System.out.println("- Player Joins: " + eventCounts.get("Player Joins"));
            System.out.println("- Player Leaves: " + eventCounts.get("Player Leaves"));
            System.out.println("- Player Timeouts: " + eventCounts.get("Player Timeouts"));
            System.out.println("- Queue Events: " + eventCounts.get("Queue Events"));
            System.out.println("- Player Kills: " + eventCounts.get("Player Kills"));
            System.out.println("- Player Deaths: " + eventCounts.get("Player Deaths"));
            
            System.out.println("\nAirdrop Events:");
            System.out.println("- Airdrop Flying: " + eventCounts.get("Airdrop Flying"));
            System.out.println("- Airdrop Dropping: " + eventCounts.get("Airdrop Dropping"));
            System.out.println("- Airdrop Waiting: " + eventCounts.get("Airdrop Waiting"));
            
            System.out.println("\nMission Events:");
            System.out.println("- Mission Initial: " + eventCounts.get("Mission Initial"));
            System.out.println("- Mission Ready: " + eventCounts.get("Mission Ready"));
            System.out.println("- Mission Waiting: " + eventCounts.get("Mission Waiting"));
            System.out.println("- Mission Active: " + eventCounts.get("Mission Active"));
            System.out.println("- Mission Completed: " + eventCounts.get("Mission Completed"));
            
            System.out.println("\nMission Level Distribution:");
            for (int i = 1; i <= 5; i++) {
                System.out.println("- Level " + i + " Missions: " + missionLevelCount.get(i));
            }
            
            System.out.println("\nOther Events:");
            System.out.println("- Helicopter Crashes: " + eventCounts.get("Helicopter Crashes"));
            System.out.println("- Trader Events: " + eventCounts.get("Trader Events"));
            System.out.println("- Convoy Events: " + eventCounts.get("Convoy Events"));
            System.out.println("- Wandering Trader Events: " + eventCounts.get("Wandering Trader Events"));
            System.out.println("- Dynamic Events: " + eventCounts.get("Dynamic Events"));
            
            System.out.println("\nDetailed Mission Events (Active & Completed):");
            for (String detail : missionEventsDetails) {
                System.out.println("- " + detail);
            }
            
        } catch (IOException e) {
            System.err.println("Error reading log file: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to extract mission level from mission name
     */
    private static int getMissionLevel(String missionName) {
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
        
        return missionLevel;
    }
}