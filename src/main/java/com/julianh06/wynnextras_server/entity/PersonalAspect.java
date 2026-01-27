package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Represents a player's personal aspect with their progress
 */
@Entity
@Table(name = "personal_aspect", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"player_uuid", "aspect_name"})
})
public class PersonalAspect {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String playerUuid; // Normalized UUID without dashes

    @Column(nullable = false)
    private String playerName;

    @Column(nullable = false)
    private String aspectName;

    @Column(nullable = false)
    private String rarity; // Mythic, Fabled, Legendary

    @Column(nullable = false)
    private int amount; // Total aspect count (calculated from tier)

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(length = 50)
    private String modVersion;

    public PersonalAspect() {}

    public PersonalAspect(String playerUuid, String playerName, String aspectName, String rarity, int amount, String modVersion) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.aspectName = aspectName;
        this.rarity = rarity;
        this.amount = amount;
        this.modVersion = modVersion;
        this.updatedAt = Instant.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlayerUuid() { return playerUuid; }
    public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getAspectName() { return aspectName; }
    public void setAspectName(String aspectName) { this.aspectName = aspectName; }

    public String getRarity() { return rarity; }
    public void setRarity(String rarity) { this.rarity = rarity; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getModVersion() { return modVersion; }
    public void setModVersion(String modVersion) { this.modVersion = modVersion; }
}
