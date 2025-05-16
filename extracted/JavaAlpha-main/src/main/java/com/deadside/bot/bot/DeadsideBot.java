package com.deadside.bot.bot;

import com.deadside.bot.commands.CommandManager;
import com.deadside.bot.config.Config;
import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.listeners.ButtonListener;
import com.deadside.bot.listeners.CommandListener;
import com.deadside.bot.listeners.StringSelectMenuListener;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.schedulers.KillfeedScheduler;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.sftp.SftpConnector;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main bot class that initializes JDA and sets up listeners and commands
 */
public class DeadsideBot {
    private static final Logger logger = LoggerFactory.getLogger(DeadsideBot.class);
    
    private final String token;
    private JDA jda;
    private CommandManager commandManager;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private KillfeedScheduler killfeedScheduler;
    private DeadsideLogParser logParser;
    private DeadsideCsvParser csvParser;
    
    public DeadsideBot(String token) {
        this.token = token;
    }
    
    /**
     * Start the bot and initialize all components
     */
    public void start() {
        try {
            // Initialize the command manager
            commandManager = new CommandManager();
            
            // Build the JDA instance with necessary intents
            jda = JDABuilder.createDefault(token)
                    .setStatus(OnlineStatus.ONLINE)
                    .setActivity(Activity.playing("Deadside"))
                    .enableIntents(
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.GUILD_MESSAGE_REACTIONS,
                            GatewayIntent.MESSAGE_CONTENT
                    )
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .enableCache(EnumSet.of(
                            CacheFlag.MEMBER_OVERRIDES,
                            CacheFlag.ROLE_TAGS,
                            CacheFlag.EMOJI,
                            CacheFlag.VOICE_STATE
                    ))
                    .addEventListeners(
                            new CommandListener(commandManager),
                            new ButtonListener(),
                            new StringSelectMenuListener()
                    )
                    .build();
            
            // Wait for JDA to be ready
            jda.awaitReady();
            logger.info("JDA initialized and connected to Discord gateway");

            // Register slash commands
            commandManager.registerCommands(jda);
            
            // Start schedulers
            startSchedulers();
            
            logger.info("Bot is now online and all systems operational!");
        } catch (Exception e) {
            logger.error("Failed to initialize bot", e);
            throw new RuntimeException("Failed to initialize bot", e);
        }
    }
    
    /**
     * Start all scheduled tasks
     */
    private void startSchedulers() {
        Config config = Config.getInstance();
        
        // Initialize killfeed scheduler
        killfeedScheduler = new KillfeedScheduler();
        killfeedScheduler.initialize(jda);
        int killfeedInterval = config.getKillfeedUpdateInterval();
        
        // Schedule killfeed updates
        scheduler.scheduleAtFixedRate(
                killfeedScheduler::processAllServers,
                1, // Initial delay of 1 second to allow bot to fully initialize
                killfeedInterval,
                TimeUnit.SECONDS
        );
        
        // Initialize common dependencies for parsers
        GameServerRepository gameServerRepository = new GameServerRepository();
        PlayerRepository playerRepository = new PlayerRepository();
        SftpConnector sftpConnector = new SftpConnector();
        
        // Initialize and start log parser
        logParser = new DeadsideLogParser(jda, gameServerRepository, sftpConnector);
        int logParserInterval = config.getLogParsingInterval();
        
        // Schedule log parsing
        scheduler.scheduleAtFixedRate(
                logParser::processAllServerLogs,
                5, // Initial delay of 5 seconds to allow full initialization
                logParserInterval,
                TimeUnit.SECONDS
        );
        
        // Initialize and start CSV death log parser
        csvParser = new DeadsideCsvParser(jda, sftpConnector, playerRepository);
        int csvParserInterval = config.getLogParsingInterval(); // Use same interval as log parser
        
        // Schedule CSV death log parsing
        scheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        List<GameServer> servers = gameServerRepository.findAll();
                        for (GameServer server : servers) {
                            csvParser.processDeathLogs(server);
                        }
                    } catch (Exception e) {
                        logger.error("Error processing CSV death logs: {}", e.getMessage(), e);
                    }
                },
                10, // Initial delay of 10 seconds
                csvParserInterval,
                TimeUnit.SECONDS
        );
        
        logger.info("Scheduled killfeed updates every {} seconds", killfeedInterval);
        logger.info("Scheduled log parsing every {} seconds", logParserInterval);
        logger.info("Scheduled CSV death log parsing every {} seconds", csvParserInterval);
    }
    
    /**
     * Shutdown the bot gracefully
     */
    public void shutdown() {
        logger.info("Shutting down schedulers...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Shutting down JDA...");
        if (jda != null) {
            jda.shutdown();
            try {
                // Wait for JDA to shutdown properly
                if (!jda.awaitShutdown(30, TimeUnit.SECONDS)) {
                    jda.shutdownNow();
                    jda.awaitStatus(JDA.Status.SHUTDOWN);
                }
            } catch (InterruptedException e) {
                jda.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public JDA getJda() {
        return jda;
    }
}
