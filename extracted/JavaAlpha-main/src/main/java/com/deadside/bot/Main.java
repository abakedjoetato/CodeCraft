package com.deadside.bot;

import com.deadside.bot.bot.DeadsideBot;
import com.deadside.bot.config.Config;
import com.deadside.bot.db.MongoDBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Deadside Discord Bot
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starting Deadside Discord Bot...");
        
        try {
            // Load configuration
            Config config = Config.getInstance();
            
            // Initialize MongoDB connection
            MongoDBConnection.initialize(config.getMongoUri());
            
            // Initialize and start the bot
            DeadsideBot bot = new DeadsideBot(config.getDiscordToken());
            bot.start();
            
            // Add shutdown hook to properly close resources
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down bot...");
                bot.shutdown();
                MongoDBConnection.getInstance().close();
                logger.info("Bot shutdown complete");
            }));
            
            logger.info("Bot started successfully!");
        } catch (Exception e) {
            logger.error("Failed to start bot", e);
            System.exit(1);
        }
    }
}
