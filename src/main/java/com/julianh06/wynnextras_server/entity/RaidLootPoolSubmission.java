package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "raid_lootpool_submission")
public class RaidLootPoolSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String raidType; // NOTG, NOL, TCC, TNA

    @Column(nullable = false)
    private String submittedBy; // username

    @Column(nullable = false, columnDefinition = "TEXT")
    private String aspectsJson; // JSON string of sorted aspects

    @Column(nullable = false)
    private Instant submittedAt;

    @Column(nullable = false, length = 10)
    private String weekIdentifier; // e.g., "2026-W04"

    public RaidLootPoolSubmission() {}

    public RaidLootPoolSubmission(String raidType, String submittedBy, String aspectsJson, String weekIdentifier) {
        this.raidType = raidType;
        this.submittedBy = submittedBy;
        this.aspectsJson = aspectsJson;
        this.weekIdentifier = weekIdentifier;
        this.submittedAt = Instant.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRaidType() { return raidType; }
    public void setRaidType(String raidType) { this.raidType = raidType; }

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }

    public String getAspectsJson() { return aspectsJson; }
    public void setAspectsJson(String aspectsJson) { this.aspectsJson = aspectsJson; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public String getWeekIdentifier() { return weekIdentifier; }
    public void setWeekIdentifier(String weekIdentifier) { this.weekIdentifier = weekIdentifier; }
}
