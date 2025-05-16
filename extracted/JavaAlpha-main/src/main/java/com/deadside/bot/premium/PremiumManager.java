package com.deadside.bot.premium;

import com.deadside.bot.db.models.GuildConfig;
import com.deadside.bot.db.repositories.GuildConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for premium features and subscriptions
 * Handles enabling/disabling premium features based on guild subscription status
 */
public class PremiumManager {
    private static final Logger logger = LoggerFactory.getLogger(PremiumManager.class);
    private final GuildConfigRepository guildConfigRepository;
    
    public PremiumManager() {
        this.guildConfigRepository = new GuildConfigRepository();
    }
    
    /**
     * Check if a guild has premium status
     * @param guildId The Discord guild ID
     * @return True if the guild has an active premium subscription
     */
    public boolean hasPremium(long guildId) {
        try {
            GuildConfig guildConfig = guildConfigRepository.findByGuildId(guildId);
            
            if (guildConfig == null) {
                return false;
            }
            
            return guildConfig.isPremium();
        } catch (Exception e) {
            logger.error("Error checking premium status for guild ID: {}", guildId, e);
            return false;
        }
    }
    
    /**
     * Enable premium for a guild
     * @param guildId The Discord guild ID
     * @param durationDays Duration in days (0 for unlimited)
     */
    public void enablePremium(long guildId, int durationDays) {
        try {
            GuildConfig guildConfig = guildConfigRepository.findByGuildId(guildId);
            
            if (guildConfig == null) {
                guildConfig = new GuildConfig(guildId);
            }
            
            guildConfig.setPremium(true);
            
            // Set expiration time if duration is specified
            if (durationDays > 0) {
                long expirationTime = System.currentTimeMillis() + (durationDays * 24L * 60L * 60L * 1000L);
                guildConfig.setPremiumUntil(expirationTime);
                logger.info("Premium enabled for guild ID: {} for {} days (until {})", 
                        guildId, durationDays, new java.util.Date(expirationTime));
            } else {
                guildConfig.setPremiumUntil(0); // No expiration
                logger.info("Premium enabled for guild ID: {} with no expiration", guildId);
            }
            
            guildConfigRepository.save(guildConfig);
        } catch (Exception e) {
            logger.error("Error enabling premium for guild ID: {}", guildId, e);
        }
    }
    
    /**
     * Disable premium for a guild
     * @param guildId The Discord guild ID
     */
    public void disablePremium(long guildId) {
        try {
            GuildConfig guildConfig = guildConfigRepository.findByGuildId(guildId);
            
            if (guildConfig != null) {
                guildConfig.setPremium(false);
                guildConfig.setPremiumUntil(0);
                guildConfigRepository.save(guildConfig);
                logger.info("Premium disabled for guild ID: {}", guildId);
            }
        } catch (Exception e) {
            logger.error("Error disabling premium for guild ID: {}", guildId, e);
        }
    }
    
    /**
     * Check and update expired premium subscriptions
     */
    public void checkExpiredSubscriptions() {
        try {
            logger.debug("Checking for expired premium subscriptions");
            
            // Find all premium guilds
            for (GuildConfig config : guildConfigRepository.findAllPremium()) {
                if (config.getPremiumUntil() > 0 && config.getPremiumUntil() < System.currentTimeMillis()) {
                    // Premium has expired
                    config.setPremium(false);
                    guildConfigRepository.save(config);
                    
                    logger.info("Premium subscription expired for guild ID: {}", config.getGuildId());
                }
            }
        } catch (Exception e) {
            logger.error("Error checking expired premium subscriptions", e);
        }
    }
    
    /**
     * Get premium status details for a guild
     * @param guildId The Discord guild ID
     * @return A string with premium status information
     */
    public String getPremiumStatusDetails(long guildId) {
        try {
            GuildConfig guildConfig = guildConfigRepository.findByGuildId(guildId);
            
            if (guildConfig == null || !guildConfig.isPremium()) {
                return "No active premium subscription";
            }
            
            if (guildConfig.getPremiumUntil() == 0) {
                return "Premium subscription active (no expiration)";
            } else {
                long remaining = guildConfig.getPremiumUntil() - System.currentTimeMillis();
                if (remaining <= 0) {
                    return "Premium subscription expired";
                }
                
                // Calculate days remaining
                long daysRemaining = remaining / (24L * 60L * 60L * 1000L);
                return String.format("Premium subscription active (expires in %d days)", daysRemaining);
            }
        } catch (Exception e) {
            logger.error("Error getting premium status details for guild ID: {}", guildId, e);
            return "Error retrieving premium status";
        }
    }
    
    /**
     * Check if Tip4serv payment should be verified
     * This would be integrated with Tip4serv API in a full implementation
     * @param guildId The Discord guild ID
     * @param userId The Discord user ID
     * @return True if payment is verified
     */
    public boolean verifyTip4servPayment(long guildId, long userId) {
        // In a real implementation, this would call the Tip4serv API
        // For now, we just log the verification request
        logger.info("Verifying Tip4serv payment for guild ID: {} and user ID: {}", guildId, userId);
        
        // Always return false as this is just a placeholder
        return false;
    }
}
