package com.deadside.bot.db.models;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * Database model for guild (Discord server) configuration
 */
public class GuildConfig {
    @BsonId
    private ObjectId id;
    private long guildId;
    private boolean premium;
    private long premiumUntil;
    private List<String> adminRoleIds;
    private List<Long> adminUserIds;
    
    public GuildConfig() {
        // Required for MongoDB POJO codec
        this.adminRoleIds = new ArrayList<>();
        this.adminUserIds = new ArrayList<>();
    }
    
    public GuildConfig(long guildId) {
        this.guildId = guildId;
        this.premium = false;
        this.premiumUntil = 0;
        this.adminRoleIds = new ArrayList<>();
        this.adminUserIds = new ArrayList<>();
    }
    
    public ObjectId getId() {
        return id;
    }
    
    public void setId(ObjectId id) {
        this.id = id;
    }
    
    public long getGuildId() {
        return guildId;
    }
    
    public void setGuildId(long guildId) {
        this.guildId = guildId;
    }
    
    public boolean isPremium() {
        return premium && (premiumUntil == 0 || premiumUntil > System.currentTimeMillis());
    }
    
    public void setPremium(boolean premium) {
        this.premium = premium;
    }
    
    public long getPremiumUntil() {
        return premiumUntil;
    }
    
    public void setPremiumUntil(long premiumUntil) {
        this.premiumUntil = premiumUntil;
    }
    
    public List<String> getAdminRoleIds() {
        return adminRoleIds;
    }
    
    public void setAdminRoleIds(List<String> adminRoleIds) {
        this.adminRoleIds = adminRoleIds;
    }
    
    public List<Long> getAdminUserIds() {
        return adminUserIds;
    }
    
    public void setAdminUserIds(List<Long> adminUserIds) {
        this.adminUserIds = adminUserIds;
    }
    
    public void addAdminRole(String roleId) {
        if (!adminRoleIds.contains(roleId)) {
            adminRoleIds.add(roleId);
        }
    }
    
    public void removeAdminRole(String roleId) {
        adminRoleIds.remove(roleId);
    }
    
    public void addAdminUser(long userId) {
        if (!adminUserIds.contains(userId)) {
            adminUserIds.add(userId);
        }
    }
    
    public void removeAdminUser(long userId) {
        adminUserIds.remove(userId);
    }
}
