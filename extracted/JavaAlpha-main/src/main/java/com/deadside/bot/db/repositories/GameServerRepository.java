package com.deadside.bot.db.repositories;

import com.deadside.bot.db.MongoDBConnection;
import com.deadside.bot.db.models.GameServer;
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
 * Repository for GameServer model
 */
public class GameServerRepository {
    private static final Logger logger = LoggerFactory.getLogger(GameServerRepository.class);
    private static final String COLLECTION_NAME = "game_servers";
    
    private final MongoCollection<GameServer> collection;
    
    public GameServerRepository() {
        this.collection = MongoDBConnection.getInstance().getDatabase().getCollection(COLLECTION_NAME, GameServer.class);
    }
    
    /**
     * Find a game server by ID
     */
    public GameServer findById(String id) {
        try {
            Bson filter = Filters.eq("_id", id);
            return collection.find(filter).first();
        } catch (Exception e) {
            logger.error("Error finding game server with ID: {}", id, e);
            return null;
        }
    }
    
    /**
     * Find a game server by guild ID and server name
     */
    public GameServer findByGuildIdAndName(long guildId, String name) {
        try {
            Bson filter = Filters.and(
                    Filters.eq("guildId", guildId),
                    Filters.eq("name", name)
            );
            return collection.find(filter).first();
        } catch (Exception e) {
            logger.error("Error finding game server for guild ID: {} and name: {}", guildId, name, e);
            return null;
        }
    }
    
    /**
     * Find all game servers for a guild
     */
    public List<GameServer> findAllByGuildId(long guildId) {
        try {
            Bson filter = Filters.eq("guildId", guildId);
            FindIterable<GameServer> results = collection.find(filter);
            
            List<GameServer> servers = new ArrayList<>();
            for (GameServer server : results) {
                servers.add(server);
            }
            
            return servers;
        } catch (Exception e) {
            logger.error("Error finding game servers for guild ID: {}", guildId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find all game servers
     */
    public List<GameServer> findAll() {
        try {
            FindIterable<GameServer> results = collection.find();
            
            List<GameServer> servers = new ArrayList<>();
            for (GameServer server : results) {
                servers.add(server);
            }
            
            return servers;
        } catch (Exception e) {
            logger.error("Error finding all game servers", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Save or update a game server
     */
    public void save(GameServer gameServer) {
        try {
            Bson filter;
            if (gameServer.getId() != null) {
                filter = Filters.eq("_id", gameServer.getId());
            } else {
                filter = Filters.and(
                        Filters.eq("guildId", gameServer.getGuildId()),
                        Filters.eq("name", gameServer.getName())
                );
            }
            
            ReplaceOptions options = new ReplaceOptions().upsert(true);
            collection.replaceOne(filter, gameServer, options);
        } catch (Exception e) {
            logger.error("Error saving game server: {}", gameServer.getName(), e);
        }
    }
    
    /**
     * Delete a game server
     */
    public void delete(GameServer gameServer) {
        try {
            Bson filter;
            if (gameServer.getId() != null) {
                filter = Filters.eq("_id", gameServer.getId());
            } else {
                filter = Filters.and(
                        Filters.eq("guildId", gameServer.getGuildId()),
                        Filters.eq("name", gameServer.getName())
                );
            }
            
            DeleteResult result = collection.deleteOne(filter);
            logger.debug("Deleted {} game server(s)", result.getDeletedCount());
        } catch (Exception e) {
            logger.error("Error deleting game server: {}", gameServer.getName(), e);
        }
    }
}
