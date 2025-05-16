package com.deadside.bot.db.repositories;

import com.deadside.bot.db.MongoDBConnection;
import com.deadside.bot.db.models.Player;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Player collection
 */
public class PlayerRepository {
    private static final Logger logger = LoggerFactory.getLogger(PlayerRepository.class);
    private final MongoCollection<Player> collection;
    
    public PlayerRepository() {
        this.collection = MongoDBConnection.getInstance().getDatabase()
                .getCollection("players", Player.class);
    }
    
    /**
     * Find a player by ID
     */
    public Player findByPlayerId(String playerId) {
        try {
            return collection.find(Filters.eq("playerId", playerId)).first();
        } catch (Exception e) {
            logger.error("Error finding player by ID: {}", playerId, e);
            return null;
        }
    }
    
    /**
     * Find a player by Deadside ID (alias for findByPlayerId)
     */
    public Player findByDeadsideId(String deadsideId) {
        return findByPlayerId(deadsideId);
    }
    
    /**
     * Find a player by exact name
     */
    public Player findByName(String name) {
        try {
            return collection.find(Filters.eq("name", name)).first();
        } catch (Exception e) {
            logger.error("Error finding player by name: {}", name, e);
            return null;
        }
    }
    
    /**
     * Find players by approximate name (case-insensitive)
     */
    public List<Player> findByNameLike(String name) {
        try {
            Bson filter = Filters.regex("name", name, "i"); // Case-insensitive regex
            return collection.find(filter).into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error finding players by name like: {}", name, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get top players by kills
     */
    public List<Player> getTopPlayersByKills(int limit) {
        try {
            return collection.find()
                    .sort(Sorts.descending("kills"))
                    .limit(limit)
                    .into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error getting top players by kills", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get top players by KD ratio (with minimum kills)
     */
    public List<Player> getTopPlayersByKD(int limit, int minKills) {
        try {
            Bson filter = Filters.gte("kills", minKills);
            return collection.find(filter)
                    .sort(Sorts.descending("kills"))
                    .limit(limit)
                    .map(player -> {
                        // Sort in Java since MongoDB can't sort by computed KD ratio
                        return player;
                    })
                    .into(new ArrayList<>())
                    .stream()
                    .sorted((p1, p2) -> Double.compare(p2.getKdRatio(), p1.getKdRatio()))
                    .limit(limit)
                    .toList();
        } catch (Exception e) {
            logger.error("Error getting top players by KD", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Save or update a player
     */
    public void save(Player player) {
        try {
            player.setLastUpdated(System.currentTimeMillis());
            
            if (player.getId() == null) {
                collection.insertOne(player);
                logger.debug("Inserted new player: {}", player.getName());
            } else {
                Bson filter = Filters.eq("_id", player.getId());
                collection.replaceOne(filter, player);
                logger.debug("Updated player: {}", player.getName());
            }
        } catch (Exception e) {
            logger.error("Error saving player: {}", player.getName(), e);
        }
    }
    
    /**
     * Update player kill stats
     */
    public void incrementKills(String playerId, int amount) {
        try {
            Bson filter = Filters.eq("playerId", playerId);
            Bson update = Updates.combine(
                    Updates.inc("kills", amount),
                    Updates.set("lastUpdated", System.currentTimeMillis())
            );
            collection.updateOne(filter, update);
        } catch (Exception e) {
            logger.error("Error incrementing kills for player ID: {}", playerId, e);
        }
    }
    
    /**
     * Update player death stats
     */
    public void incrementDeaths(String playerId, int amount) {
        try {
            Bson filter = Filters.eq("playerId", playerId);
            Bson update = Updates.combine(
                    Updates.inc("deaths", amount),
                    Updates.set("lastUpdated", System.currentTimeMillis())
            );
            collection.updateOne(filter, update);
        } catch (Exception e) {
            logger.error("Error incrementing deaths for player ID: {}", playerId, e);
        }
    }
    
    /**
     * Update player suicide stats
     */
    public void incrementSuicides(String playerId, int amount) {
        try {
            Bson filter = Filters.eq("playerId", playerId);
            Bson update = Updates.combine(
                    Updates.inc("suicides", amount),
                    Updates.set("lastUpdated", System.currentTimeMillis())
            );
            collection.updateOne(filter, update);
        } catch (Exception e) {
            logger.error("Error incrementing suicides for player ID: {}", playerId, e);
        }
    }
}