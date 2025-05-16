package com.deadside.bot.db.repositories;

import com.deadside.bot.db.MongoDBConnection;
import com.deadside.bot.db.models.LinkedPlayer;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for LinkedPlayer collection
 */
public class LinkedPlayerRepository {
    private static final Logger logger = LoggerFactory.getLogger(LinkedPlayerRepository.class);
    private final MongoCollection<LinkedPlayer> collection;
    
    public LinkedPlayerRepository() {
        this.collection = MongoDBConnection.getInstance().getDatabase()
                .getCollection("linked_players", LinkedPlayer.class);
    }
    
    /**
     * Find a linked player by Discord ID
     */
    public LinkedPlayer findByDiscordId(long discordId) {
        try {
            return collection.find(Filters.eq("discordId", discordId)).first();
        } catch (Exception e) {
            logger.error("Error finding linked player by Discord ID: {}", discordId, e);
            return null;
        }
    }
    
    /**
     * Find a linked player by in-game player ID (main or alt)
     */
    public LinkedPlayer findByPlayerId(String playerId) {
        try {
            // Check if it's a main player
            LinkedPlayer mainLink = collection.find(Filters.eq("mainPlayerId", playerId)).first();
            if (mainLink != null) {
                return mainLink;
            }
            
            // Check if it's an alt player
            return collection.find(Filters.in("altPlayerIds", playerId)).first();
        } catch (Exception e) {
            logger.error("Error finding linked player by player ID: {}", playerId, e);
            return null;
        }
    }
    
    /**
     * Save or update a linked player
     */
    public void save(LinkedPlayer linkedPlayer) {
        try {
            linkedPlayer.setUpdated(System.currentTimeMillis());
            
            if (linkedPlayer.getId() == null) {
                collection.insertOne(linkedPlayer);
                logger.debug("Inserted new linked player for Discord ID: {}", linkedPlayer.getDiscordId());
            } else {
                Bson filter = Filters.eq("_id", linkedPlayer.getId());
                collection.replaceOne(filter, linkedPlayer);
                logger.debug("Updated linked player for Discord ID: {}", linkedPlayer.getDiscordId());
            }
        } catch (Exception e) {
            logger.error("Error saving linked player: {}", linkedPlayer.getDiscordId(), e);
        }
    }
    
    /**
     * Delete a linked player
     */
    public void delete(LinkedPlayer linkedPlayer) {
        try {
            if (linkedPlayer.getId() != null) {
                Bson filter = Filters.eq("_id", linkedPlayer.getId());
                collection.deleteOne(filter);
                logger.debug("Deleted linked player for Discord ID: {}", linkedPlayer.getDiscordId());
            }
        } catch (Exception e) {
            logger.error("Error deleting linked player: {}", linkedPlayer.getDiscordId(), e);
        }
    }
    
    /**
     * Find all linked players
     */
    public List<LinkedPlayer> findAll() {
        try {
            return collection.find().into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error finding all linked players", e);
            return new ArrayList<>();
        }
    }
}