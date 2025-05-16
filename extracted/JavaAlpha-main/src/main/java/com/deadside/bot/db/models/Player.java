package com.deadside.bot.db.models;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

/**
 * Database model for a Deadside player
 */
public class Player {
    @BsonId
    private ObjectId id;
    private String playerId;     // Unique game ID for the player
    private String name;         // Player's in-game name
    private int kills;           // Total kills
    private int deaths;          // Total deaths
    private int suicides;        // Total suicides
    private String mostUsedWeapon; // Most frequently used weapon
    private int mostUsedWeaponKills; // Kills with most frequently used weapon
    private String mostKilledPlayer; // Player this player has killed the most
    private int mostKilledPlayerCount; // Number of times killed the most killed player
    private String killedByMost; // Player that has killed this player the most
    private int killedByMostCount; // Number of times killed by the player that killed the most
    private long lastUpdated;    // Timestamp of last update
    private Currency currency;   // Player's currency and economy data
    private FactionMember factionMember;  // Player's faction membership
    
    public Player() {
        // Required for MongoDB POJO codec
        this.currency = new Currency();
        this.factionMember = null;
    }
    
    public Player(String playerId, String name) {
        this.playerId = playerId;
        this.name = name;
        this.kills = 0;
        this.deaths = 0;
        this.suicides = 0;
        this.mostUsedWeapon = "";
        this.mostUsedWeaponKills = 0;
        this.mostKilledPlayer = "";
        this.mostKilledPlayerCount = 0;
        this.killedByMost = "";
        this.killedByMostCount = 0;
        this.lastUpdated = System.currentTimeMillis();
        this.currency = new Currency();
        this.factionMember = null;
    }
    
    public ObjectId getId() {
        return id;
    }
    
    public void setId(ObjectId id) {
        this.id = id;
    }
    
    public String getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getKills() {
        return kills;
    }
    
    public void setKills(int kills) {
        this.kills = kills;
    }
    
    public void addKill() {
        this.kills++;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public int getDeaths() {
        return deaths;
    }
    
    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }
    
    public void addDeath() {
        this.deaths++;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public int getSuicides() {
        return suicides;
    }
    
    public void setSuicides(int suicides) {
        this.suicides = suicides;
    }
    
    public void addSuicide() {
        this.suicides++;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public String getMostUsedWeapon() {
        return mostUsedWeapon;
    }
    
    public void setMostUsedWeapon(String mostUsedWeapon) {
        this.mostUsedWeapon = mostUsedWeapon;
    }
    
    public int getMostUsedWeaponKills() {
        return mostUsedWeaponKills;
    }
    
    public void setMostUsedWeaponKills(int mostUsedWeaponKills) {
        this.mostUsedWeaponKills = mostUsedWeaponKills;
    }
    
    public String getMostKilledPlayer() {
        return mostKilledPlayer;
    }
    
    public void setMostKilledPlayer(String mostKilledPlayer) {
        this.mostKilledPlayer = mostKilledPlayer;
    }
    
    public int getMostKilledPlayerCount() {
        return mostKilledPlayerCount;
    }
    
    public void setMostKilledPlayerCount(int mostKilledPlayerCount) {
        this.mostKilledPlayerCount = mostKilledPlayerCount;
    }
    
    public String getKilledByMost() {
        return killedByMost;
    }
    
    public void setKilledByMost(String killedByMost) {
        this.killedByMost = killedByMost;
    }
    
    public int getKilledByMostCount() {
        return killedByMostCount;
    }
    
    public void setKilledByMostCount(int killedByMostCount) {
        this.killedByMostCount = killedByMostCount;
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public double getKdRatio() {
        if (deaths == 0) {
            return kills;
        }
        return (double) kills / deaths;
    }
    
    /**
     * Update this player's weapon statistics
     */
    public void updateWeaponStats(String weapon, int killCount) {
        if (killCount > mostUsedWeaponKills) {
            mostUsedWeapon = weapon;
            mostUsedWeaponKills = killCount;
            lastUpdated = System.currentTimeMillis();
        }
    }
    
    /**
     * Update this player's victim statistics
     */
    public void updateVictimStats(String victimName, int killCount) {
        if (killCount > mostKilledPlayerCount) {
            mostKilledPlayer = victimName;
            mostKilledPlayerCount = killCount;
            lastUpdated = System.currentTimeMillis();
        }
    }
    
    /**
     * Update this player's killer statistics
     */
    public void updateKillerStats(String killerName, int deathCount) {
        if (deathCount > killedByMostCount) {
            killedByMost = killerName;
            killedByMostCount = deathCount;
            lastUpdated = System.currentTimeMillis();
        }
    }
    
    /**
     * Get player's currency
     */
    public Currency getCurrency() {
        if (currency == null) {
            currency = new Currency();
        }
        return currency;
    }
    
    /**
     * Set player's currency
     */
    public void setCurrency(Currency currency) {
        this.currency = currency;
    }
    
    /**
     * Get player's faction membership
     */
    public FactionMember getFactionMember() {
        return factionMember;
    }
    
    /**
     * Set player's faction membership
     */
    public void setFactionMember(FactionMember factionMember) {
        this.factionMember = factionMember;
    }
    
    /**
     * Check if player is in a faction
     */
    public boolean isInFaction() {
        return factionMember != null;
    }
    
    /**
     * Add a kill reward (coins and experience)
     */
    public void addKillReward(int coinsAmount, int experienceAmount) {
        // Add coins to the player's wallet
        getCurrency().addCoins(coinsAmount);
        
        // If player is in a faction, add experience
        if (isInFaction()) {
            factionMember.addContributedXp(experienceAmount);
        }
        
        lastUpdated = System.currentTimeMillis();
    }
    
    /**
     * Get player's score (calculated based on kills, deaths, etc)
     */
    public int getScore() {
        // Initial score calculation formula
        int score = (kills * 10) - (deaths * 5);
        
        // Ensure score doesn't go below zero
        return Math.max(0, score);
    }
    
    /**
     * Set player's score directly
     */
    public void setScore(int score) {
        // Since score is calculated based on kills and deaths, 
        // we'll set kills to match the desired score assuming 10 points per kill
        this.kills = score / 10;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    /**
     * Get the Deadside ID of the player
     * This is an alias for getPlayerId to maintain consistent naming
     */
    public String getDeadsideId() {
        return playerId;
    }
    
    /**
     * Set the Deadside ID of the player
     * This is an alias for setPlayerId to maintain consistent naming
     */
    public void setDeadsideId(String deadsideId) {
        this.playerId = deadsideId;
    }
}