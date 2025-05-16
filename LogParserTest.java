import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParserTest {
    // Updated patterns based on actual log format
    private static final Pattern PLAYER_JOIN_PATTERN = Pattern.compile("LogOnline: Warning: Player \\|(.+?) successfully registered!");
    private static final Pattern PLAYER_LEAVE_PATTERN = Pattern.compile("LogOnline: Warning: Player \\|(.+?) successfully unregistered from the session.");
    private static final Pattern PLAYER_CONNECTION_TIMEOUT = Pattern.compile("LogNet: Warning: UNetConnection::Tick: Connection TIMED OUT.+UniqueId: EOS:\\|(.+?)($|,)");
    private static final Pattern PLAYER_QUEUE_PATTERN = Pattern.compile("LogNet: Warning: Player (.+?) joined the queue");
    
    // Event patterns
    private static final Pattern AIRDROP_PATTERN = Pattern.compile("LogSFPS: AirDrop switched to (\\w+)");
    private static final Pattern HELI_CRASH_PATTERN = Pattern.compile("LogSFPS: Helicopter crash spawned at position (.+)");
    private static final Pattern TRADER_EVENT_PATTERN = Pattern.compile("LogSFPS: Trader event started at (.+)");
    private static final Pattern MISSION_PATTERN = Pattern.compile("LogSFPS: Mission (.+?) switched to (\\w+)");
    private static final Pattern PLAYER_KILLED_PATTERN = Pattern.compile("LogSFPS: \\[Kill\\] (.+?) killed (.+?) with (.+?) at distance (\\d+)");
    private static final Pattern PLAYER_DIED_PATTERN = Pattern.compile("LogSFPS: \\[Death\\] (.+?) died from (.+?)");

    public static void main(String[] args) {
        String logFile = "attached_assets/Deadside.log";
        
        // Track individual events
        Set<String> joinedPlayers = new HashSet<>();
        Set<String> leftPlayers = new HashSet<>();
        Set<String> timedOutPlayers = new HashSet<>();
        Set<String> queuedPlayers = new HashSet<>();
        
        // For online player tracking
        Set<String> currentOnlinePlayers = new HashSet<>();
        
        Map<String, Integer> eventCounts = new HashMap<>();
        eventCounts.put("Airdrops", 0);
        eventCounts.put("Helicopter Crashes", 0);
        eventCounts.put("Trader Events", 0);
        eventCounts.put("Missions", 0);
        eventCounts.put("Player Kills", 0);
        eventCounts.put("Player Deaths", 0);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            int lineCount = 0;
            
            while ((line = reader.readLine()) != null) {
                lineCount++;
                
                // Player join events
                Matcher joinMatcher = PLAYER_JOIN_PATTERN.matcher(line);
                if (joinMatcher.find()) {
                    String playerName = joinMatcher.group(1).trim();
                    joinedPlayers.add(playerName);
                    currentOnlinePlayers.add(playerName); // Add to online players
                    continue;
                }
                
                // Player leave events
                Matcher leaveMatcher = PLAYER_LEAVE_PATTERN.matcher(line);
                if (leaveMatcher.find()) {
                    String playerName = leaveMatcher.group(1).trim();
                    leftPlayers.add(playerName);
                    currentOnlinePlayers.remove(playerName); // Remove from online players
                    continue;
                }
                
                // Connection timeout events
                Matcher timeoutMatcher = PLAYER_CONNECTION_TIMEOUT.matcher(line);
                if (timeoutMatcher.find()) {
                    String playerName = timeoutMatcher.group(1).trim();
                    timedOutPlayers.add(playerName);
                    currentOnlinePlayers.remove(playerName); // Remove from online players
                    continue;
                }
                
                // Player queue events
                Matcher queueMatcher = PLAYER_QUEUE_PATTERN.matcher(line);
                if (queueMatcher.find()) {
                    String playerName = queueMatcher.group(1).trim();
                    queuedPlayers.add(playerName);
                    continue;
                }
                
                // Airdrop events
                Matcher airdropMatcher = AIRDROP_PATTERN.matcher(line);
                if (airdropMatcher.find()) {
                    eventCounts.put("Airdrops", eventCounts.get("Airdrops") + 1);
                    continue;
                }
                
                // Helicopter crash events
                Matcher heliMatcher = HELI_CRASH_PATTERN.matcher(line);
                if (heliMatcher.find()) {
                    eventCounts.put("Helicopter Crashes", eventCounts.get("Helicopter Crashes") + 1);
                    continue;
                }
                
                // Trader events
                Matcher traderMatcher = TRADER_EVENT_PATTERN.matcher(line);
                if (traderMatcher.find()) {
                    eventCounts.put("Trader Events", eventCounts.get("Trader Events") + 1);
                    continue;
                }
                
                // Mission events
                Matcher missionMatcher = MISSION_PATTERN.matcher(line);
                if (missionMatcher.find()) {
                    eventCounts.put("Missions", eventCounts.get("Missions") + 1);
                    continue;
                }
                
                // Player killed events
                Matcher killedMatcher = PLAYER_KILLED_PATTERN.matcher(line);
                if (killedMatcher.find()) {
                    eventCounts.put("Player Kills", eventCounts.get("Player Kills") + 1);
                    continue;
                }
                
                // Player died events
                Matcher diedMatcher = PLAYER_DIED_PATTERN.matcher(line);
                if (diedMatcher.find()) {
                    eventCounts.put("Player Deaths", eventCounts.get("Player Deaths") + 1);
                    continue;
                }
            }
            
            System.out.println("=== Log Parser Test Results ===");
            System.out.println("Total lines processed: " + lineCount);
            
            System.out.println("\n=== CURRENT ONLINE PLAYERS (" + currentOnlinePlayers.size() + " players) ===");
            for (String player : currentOnlinePlayers) {
                System.out.println("- " + player);
            }
            
            System.out.println("\n=== PLAYER JOINS (" + joinedPlayers.size() + " unique players) ===");
            for (String player : joinedPlayers) {
                System.out.println("- " + player);
            }
            
            System.out.println("\n=== PLAYER LEAVES (" + leftPlayers.size() + " unique players) ===");
            for (String player : leftPlayers) {
                System.out.println("- " + player);
            }
            
            System.out.println("\n=== CONNECTION TIMEOUTS (" + timedOutPlayers.size() + " unique players) ===");
            for (String player : timedOutPlayers) {
                System.out.println("- " + player);
            }
            
            System.out.println("\n=== QUEUED PLAYERS (" + queuedPlayers.size() + " unique players) ===");
            for (String player : queuedPlayers) {
                System.out.println("- " + player);
            }
            
            System.out.println("\n=== EVENTS ===");
            for (Map.Entry<String, Integer> entry : eventCounts.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
            
        } catch (IOException e) {
            System.err.println("Error reading log file: " + e.getMessage());
        }
    }
}