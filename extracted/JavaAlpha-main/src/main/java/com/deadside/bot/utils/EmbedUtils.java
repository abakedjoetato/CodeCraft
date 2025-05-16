package com.deadside.bot.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.time.Instant;

/**
 * Utility class for creating Discord message embeds
 */
public class EmbedUtils {
    // Standard colors for different embed types
    private static final Color SUCCESS_COLOR = new Color(46, 204, 113); // Green
    private static final Color ERROR_COLOR = new Color(231, 76, 60);   // Red
    private static final Color INFO_COLOR = new Color(52, 152, 219);   // Blue
    private static final Color WARNING_COLOR = new Color(243, 156, 18); // Orange
    
    /**
     * Create a success embed
     */
    public static MessageEmbed successEmbed(String title, String description) {
        return new EmbedBuilder()
                .setTitle("✅ " + title)
                .setDescription(description)
                .setColor(SUCCESS_COLOR)
                .setTimestamp(Instant.now())
                .build();
    }
    
    /**
     * Create an error embed
     */
    public static MessageEmbed errorEmbed(String title, String description) {
        return new EmbedBuilder()
                .setTitle("❌ " + title)
                .setDescription(description)
                .setColor(ERROR_COLOR)
                .setTimestamp(Instant.now())
                .build();
    }
    
    /**
     * Create an info embed
     */
    public static MessageEmbed infoEmbed(String title, String description) {
        return new EmbedBuilder()
                .setTitle("ℹ️ " + title)
                .setDescription(description)
                .setColor(INFO_COLOR)
                .setTimestamp(Instant.now())
                .build();
    }
    
    /**
     * Create a warning embed
     */
    public static MessageEmbed warningEmbed(String title, String description) {
        return new EmbedBuilder()
                .setTitle("⚠️ " + title)
                .setDescription(description)
                .setColor(WARNING_COLOR)
                .setTimestamp(Instant.now())
                .build();
    }
    
    /**
     * Create a custom colored embed
     */
    public static MessageEmbed customEmbed(String title, String description, Color color) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setTimestamp(Instant.now())
                .build();
    }
    
    /**
     * Create a custom colored embed with thumbnail
     */
    public static MessageEmbed customEmbedWithThumbnail(String title, String description, 
                                                       Color color, String thumbnailUrl) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setThumbnail(thumbnailUrl)
                .setTimestamp(Instant.now())
                .build();
    }
    
    /**
     * Create a player stats embed
     */
    public static EmbedBuilder playerStatsEmbed(String playerName) {
        return new EmbedBuilder()
                .setTitle("📊 Stats for " + playerName)
                .setColor(new Color(75, 0, 130)) // Deep purple for stats
                .setTimestamp(Instant.now());
    }
    
    /**
     * Create a killfeed embed
     */
    public static MessageEmbed killfeedEmbed(String title, String description) {
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(new Color(204, 0, 0)) // Dark red for killfeed
                .setTimestamp(Instant.now())
                .build();
    }
}