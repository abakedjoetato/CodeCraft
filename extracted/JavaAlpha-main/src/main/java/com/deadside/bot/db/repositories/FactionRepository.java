package com.deadside.bot.db.repositories;

import com.deadside.bot.db.MongoDBConnection;
import com.deadside.bot.db.models.Faction;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Faction collection
 */
public class FactionRepository {
    private static final Logger logger = LoggerFactory.getLogger(FactionRepository.class);
    private final MongoCollection<Faction> collection;
    
    public FactionRepository() {
        this.collection = MongoDBConnection.getInstance().getDatabase()
                .getCollection("factions", Faction.class);
    }
    
    /**
     * Find a faction by ID
     */
    public Faction findById(ObjectId id) {
        try {
            return collection.find(Filters.eq("_id", id)).first();
        } catch (Exception e) {
            logger.error("Error finding faction by ID: {}", id, e);
            return null;
        }
    }
    
    /**
     * Find a faction by name in a guild
     */
    public Faction findByNameInGuild(long guildId, String name) {
        try {
            Bson filter = Filters.and(
                    Filters.eq("guildId", guildId),
                    Filters.regex("name", "^" + name + "$", "i")  // Case-insensitive exact match
            );
            return collection.find(filter).first();
        } catch (Exception e) {
            logger.error("Error finding faction by name: {} in guild: {}", name, guildId, e);
            return null;
        }
    }
    
    /**
     * Find a faction by tag in a guild
     */
    public Faction findByTagInGuild(long guildId, String tag) {
        try {
            Bson filter = Filters.and(
                    Filters.eq("guildId", guildId),
                    Filters.regex("tag", "^" + tag + "$", "i")  // Case-insensitive exact match
            );
            return collection.find(filter).first();
        } catch (Exception e) {
            logger.error("Error finding faction by tag: {} in guild: {}", tag, guildId, e);
            return null;
        }
    }
    
    /**
     * Find factions by owner ID
     */
    public List<Faction> findByOwner(long ownerId) {
        try {
            return collection.find(Filters.eq("ownerId", ownerId))
                    .into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error finding factions by owner: {}", ownerId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find factions where a user is a member (owner, officer or regular member)
     */
    public List<Faction> findByMember(long memberId) {
        try {
            Bson filter = Filters.or(
                    Filters.eq("ownerId", memberId),
                    Filters.in("officerIds", memberId),
                    Filters.in("memberIds", memberId)
            );
            return collection.find(filter).into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error finding factions by member: {}", memberId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find all factions in a guild
     */
    public List<Faction> findByGuild(long guildId) {
        try {
            return collection.find(Filters.eq("guildId", guildId))
                    .sort(Sorts.descending("level", "experience"))
                    .into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error finding factions by guild: {}", guildId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find top factions by level in a guild
     */
    public List<Faction> findTopFactionsByLevel(long guildId, int limit) {
        try {
            return collection.find(Filters.eq("guildId", guildId))
                    .sort(Sorts.descending("level", "experience"))
                    .limit(limit)
                    .into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error finding top factions by level for guild: {}", guildId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Find top factions by member count in a guild
     */
    public List<Faction> findTopFactionsByMemberCount(long guildId, int limit) {
        try {
            // This is a bit tricky because we need to calculate total members
            // MongoDB aggregation would be more efficient, but for simplicity we'll fetch all and sort in-memory
            List<Faction> factions = collection.find(Filters.eq("guildId", guildId))
                    .into(new ArrayList<>());
            
            return factions.stream()
                    .sorted((f1, f2) -> Integer.compare(f2.getTotalMemberCount(), f1.getTotalMemberCount()))
                    .limit(limit)
                    .toList();
        } catch (Exception e) {
            logger.error("Error finding top factions by member count for guild: {}", guildId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Save or update a faction
     */
    public void save(Faction faction) {
        try {
            if (faction.getId() == null) {
                collection.insertOne(faction);
                logger.debug("Inserted new faction: {}", faction.getName());
            } else {
                Bson filter = Filters.eq("_id", faction.getId());
                collection.replaceOne(filter, faction);
                logger.debug("Updated faction: {}", faction.getName());
            }
        } catch (Exception e) {
            logger.error("Error saving faction: {}", faction.getName(), e);
        }
    }
    
    /**
     * Delete a faction
     */
    public boolean delete(Faction faction) {
        try {
            if (faction.getId() != null) {
                Bson filter = Filters.eq("_id", faction.getId());
                collection.deleteOne(filter);
                logger.debug("Deleted faction: {}", faction.getName());
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error deleting faction: {}", faction.getName(), e);
            return false;
        }
    }
    
    /**
     * Add experience to a faction
     */
    public boolean addExperience(ObjectId factionId, int amount) {
        try {
            Faction faction = findById(factionId);
            if (faction == null) {
                return false;
            }
            
            boolean leveledUp = faction.addExperience(amount);
            save(faction);
            return leveledUp;
        } catch (Exception e) {
            logger.error("Error adding experience to faction: {}", factionId, e);
            return false;
        }
    }
    
    /**
     * Update faction bank balance
     */
    public boolean updateBalance(ObjectId factionId, long amount) {
        try {
            Faction faction = findById(factionId);
            if (faction == null) {
                return false;
            }
            
            if (amount >= 0) {
                faction.deposit(amount);
            } else {
                faction.withdraw(-amount);
            }
            
            save(faction);
            return true;
        } catch (Exception e) {
            logger.error("Error updating balance for faction: {}", factionId, e);
            return false;
        }
    }
}