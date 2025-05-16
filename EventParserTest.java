import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventParserTest {
    // Updated event patterns
    private static final Pattern AIRDROP_PATTERN = Pattern.compile("LogSFPS: AirDrop switched to (\\w+)");
    private static final Pattern MISSION_PATTERN = Pattern.compile("LogSFPS: Mission (.+?) switched to (\\w+)");
    private static final Pattern GAMEPLAY_EVENT_PATTERN = Pattern.compile("LogSFPS: GameplayEvent (.+?) switched to (\\w+)");
    private static final Pattern HELI_CRASH_PATTERN = Pattern.compile("LogSFPS: GameplayEvent (HelicrashManager.+?)HelicrashEvent.+? switched to (\\w+)");
    private static final Pattern ROAMING_TRADER_PATTERN = Pattern.compile("LogSFPS: GameplayEvent (RoamingTraderManager.+?)RoamingTraderEvent.+? switched to (\\w+)");
    
    /**
     * Check if a mission is level 3 or higher
     */
    private static boolean isHighLevelMission(String missionName) {
        // Check for specific level indicators in mission name
        if (missionName.contains("_03") || missionName.contains("_3_") || 
            missionName.contains("_04") || missionName.contains("_4_") ||
            missionName.contains("_05") || missionName.contains("_5_") ||
            missionName.contains("lvl_3") || missionName.contains("lvl_4") || missionName.contains("lvl_5") ||
            missionName.contains("Level3") || missionName.contains("Level4") || missionName.contains("Level5")) {
            return true;
        }
        // Return false for low-level missions
        return false;
    }
    
    public static void main(String[] args) {
        String logFile = "attached_assets/Deadside.log";
        
        // Track event counts by state
        Map<String, Integer> airdropStates = new HashMap<>();
        Map<String, Integer> missionStates = new HashMap<>();
        Map<String, Integer> missionLevels = new HashMap<>();
        Map<String, Integer> heliCrashStates = new HashMap<>();
        Map<String, Integer> roamingTraderStates = new HashMap<>();
        Map<String, Integer> gameplayEventStates = new HashMap<>();
        
        // Count for high-level mission notification criteria
        int highLevelActiveMissions = 0;
        int flyingAirdrops = 0;
        int totalGameplayEvents = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            int lineCount = 0;
            
            while ((line = reader.readLine()) != null) {
                lineCount++;
                
                // Airdrop events
                Matcher airdropMatcher = AIRDROP_PATTERN.matcher(line);
                if (airdropMatcher.find()) {
                    String status = airdropMatcher.group(1).trim();
                    airdropStates.put(status, airdropStates.getOrDefault(status, 0) + 1);
                    
                    // Count Flying airdrops (for notification criteria)
                    if (status.equalsIgnoreCase("Flying")) {
                        flyingAirdrops++;
                    }
                    continue;
                }
                
                // Helicopter crash events
                Matcher heliMatcher = HELI_CRASH_PATTERN.matcher(line);
                if (heliMatcher.find()) {
                    String eventId = heliMatcher.group(1).trim();
                    String state = heliMatcher.group(2).trim();
                    heliCrashStates.put(state, heliCrashStates.getOrDefault(state, 0) + 1);
                    continue;
                }
                
                // Roaming trader events
                Matcher traderMatcher = ROAMING_TRADER_PATTERN.matcher(line);
                if (traderMatcher.find()) {
                    String eventId = traderMatcher.group(1).trim();
                    String state = traderMatcher.group(2).trim();
                    roamingTraderStates.put(state, roamingTraderStates.getOrDefault(state, 0) + 1);
                    continue;
                }
                
                // Generic gameplay events (excluding helicopter crashes and roaming traders)
                Matcher gameplayMatcher = GAMEPLAY_EVENT_PATTERN.matcher(line);
                if (gameplayMatcher.find()) {
                    String eventId = gameplayMatcher.group(1).trim();
                    String state = gameplayMatcher.group(2).trim();
                    
                    // Skip helicopter and trader events as they're handled separately
                    if (eventId.contains("HelicrashManager") || eventId.contains("RoamingTrader")) {
                        continue;
                    }
                    
                    // Track the event state
                    gameplayEventStates.put(state, gameplayEventStates.getOrDefault(state, 0) + 1);
                    totalGameplayEvents++;
                    continue;
                }
                
                // Mission events
                Matcher missionMatcher = MISSION_PATTERN.matcher(line);
                if (missionMatcher.find()) {
                    String missionName = missionMatcher.group(1).trim();
                    String status = missionMatcher.group(2).trim();
                    
                    missionStates.put(status, missionStates.getOrDefault(status, 0) + 1);
                    
                    // Identify mission level from name
                    if (missionName.contains("01") || missionName.contains("_1_") || missionName.contains("lvl_1")) {
                        missionLevels.put("Level 1", missionLevels.getOrDefault("Level 1", 0) + 1);
                    } else if (missionName.contains("02") || missionName.contains("_2_") || missionName.contains("lvl_2")) {
                        missionLevels.put("Level 2", missionLevels.getOrDefault("Level 2", 0) + 1);
                    } else if (missionName.contains("03") || missionName.contains("_3_") || missionName.contains("lvl_3")) {
                        missionLevels.put("Level 3", missionLevels.getOrDefault("Level 3", 0) + 1);
                    } else if (missionName.contains("04") || missionName.contains("_4_") || missionName.contains("lvl_4")) {
                        missionLevels.put("Level 4", missionLevels.getOrDefault("Level 4", 0) + 1);
                    } else if (missionName.contains("05") || missionName.contains("_5_") || missionName.contains("lvl_5")) {
                        missionLevels.put("Level 5", missionLevels.getOrDefault("Level 5", 0) + 1);
                    } else {
                        missionLevels.put("Unknown", missionLevels.getOrDefault("Unknown", 0) + 1);
                    }
                    
                    // Count high-level missions that are ACTIVE (for notification criteria)
                    if (status.equalsIgnoreCase("ACTIVE") && isHighLevelMission(missionName)) {
                        highLevelActiveMissions++;
                    }
                    
                    continue;
                }
            }
            
            System.out.println("=== Enhanced Event Parser Test Results ===");
            System.out.println("Total lines processed: " + lineCount);
            
            System.out.println("\n=== AIRDROP STATES ===");
            int totalAirdrops = 0;
            for (Map.Entry<String, Integer> entry : airdropStates.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
                totalAirdrops += entry.getValue();
            }
            System.out.println("Total Airdrop Events: " + totalAirdrops);
            System.out.println("Flying Airdrops (Notification Trigger): " + flyingAirdrops);
            
            System.out.println("\n=== MISSION STATES ===");
            int totalMissions = 0;
            for (Map.Entry<String, Integer> entry : missionStates.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
                totalMissions += entry.getValue();
            }
            System.out.println("Total Mission Events: " + totalMissions);
            System.out.println("High-Level ACTIVE Missions (Notification Trigger): " + highLevelActiveMissions);
            
            System.out.println("\n=== MISSION LEVELS ===");
            for (Map.Entry<String, Integer> entry : missionLevels.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
            
            System.out.println("\n=== HELICOPTER CRASH STATES ===");
            int totalHeliCrashes = 0;
            for (Map.Entry<String, Integer> entry : heliCrashStates.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
                totalHeliCrashes += entry.getValue();
            }
            System.out.println("Total Helicopter Crash Events: " + totalHeliCrashes);
            
            System.out.println("\n=== ROAMING TRADER STATES ===");
            int totalRoamingTraders = 0;
            for (Map.Entry<String, Integer> entry : roamingTraderStates.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
                totalRoamingTraders += entry.getValue();
            }
            System.out.println("Total Roaming Trader Events: " + totalRoamingTraders);
            
            System.out.println("\n=== OTHER GAMEPLAY EVENT STATES ===");
            for (Map.Entry<String, Integer> entry : gameplayEventStates.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
            System.out.println("Total Other Gameplay Events: " + totalGameplayEvents);
            
        } catch (IOException e) {
            System.err.println("Error reading log file: " + e.getMessage());
        }
    }
}