package com.deadside.bot.db.repositories;

import com.deadside.bot.db.MongoDBConnection;
import com.deadside.bot.db.models.GuildConfig;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for GuildConfig model
 */
public class GuildConfigRepository {
    private static final Logger logger = LoggerFactory.getLogger(GuildConfigRepository.class);
    private static final String COLLECTION_NAME = "guild_configs";
    
    private final MongoCollection<GuildConfig> collection;
    
    public GuildConfigRepository() {
        this.collection = MongoDBConnection.getInstance().getDatabase().getCollection(COLLECTION_NAME, GuildConfig.class);
    }
    
    /**
     * Find a guild config by Discord guild ID
     */
    public GuildConfig findByGuildId(long guildId) {
        try {
            Bson filter = Filters.eq("guildId", guildId);
            return collection.find(filter).first();
        } catch (Exception e) {
            logger.error("Error finding guild config for guild ID: {}", guildId, e);
            return null;
        }
    }
    
    /**
     * Save or update a guild config
     */
    public void save(GuildConfig guildConfig) {
        try {
            Bson filter = Filters.eq("guildId", guildConfig.getGuildId());
            ReplaceOptions options = new ReplaceOptions().upsert(true);
            collection.replaceOne(filter, guildConfig, options);
        } catch (Exception e) {
            logger.error("Error saving guild config for guild ID: {}", guildConfig.getGuildId(), e);
        }
    }
    
    /**
     * Delete a guild config
     */
    public void delete(GuildConfig guildConfig) {
        try {
            Bson filter = Filters.eq("guildId", guildConfig.getGuildId());
            DeleteResult result = collection.deleteOne(filter);
            logger.debug("Deleted {} guild config(s)", result.getDeletedCount());
        } catch (Exception e) {
            logger.error("Error deleting guild config for guild ID: {}", guildConfig.getGuildId(), e);
        }
    }
    
    /**
     * Find all premium guild configs
     */
    public List<GuildConfig> findAllPremium() {
        try {
            Bson filter = Filters.eq("premium", true);
            FindIterable<GuildConfig> results = collection.find(filter);
            
            List<GuildConfig> configs = new ArrayList<>();
            for (GuildConfig config : results) {
                configs.add(config);
            }
            
            return configs;
        } catch (Exception e) {
            logger.error("Error finding premium guild configs", e);
            return new ArrayList<>();
        }
    }
}
