package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "raid_lootpool_approved")
public class RaidLootPoolApproved {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String raidType; // NOTG, NOL, TCC, TNA

    @Column(nullable = false, columnDefinition = "TEXT")
    private String aspectsJson; // JSON string of approved aspects

    @Column(nullable = false)
    private Instant approvedAt;

    @Column(nullable = false, length = 10)
    private String weekIdentifier; // e.g., "2026-W04"

    @Column(nullable = false)
    private boolean locked; // locked at 10 submissions

    @Column(nullable = false)
    private int submissionCount; // how many submissions matched

    public RaidLootPoolApproved() {}

    public RaidLootPoolApproved(String raidType, String aspectsJson, String weekIdentifier, int submissionCount, boolean locked) {
        this.raidType = raidType;
        this.aspectsJson = aspectsJson;
        this.weekIdentifier = weekIdentifier;
        this.submissionCount = submissionCount;
        this.locked = locked;
        this.approvedAt = Instant.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRaidType() { return raidType; }
    public void setRaidType(String raidType) { this.raidType = raidType; }

    public String getAspectsJson() { return aspectsJson; }
    public void setAspectsJson(String aspectsJson) { this.aspectsJson = aspectsJson; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }

    public String getWeekIdentifier() { return weekIdentifier; }
    public void setWeekIdentifier(String weekIdentifier) { this.weekIdentifier = weekIdentifier; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public int getSubmissionCount() { return submissionCount; }
    public void setSubmissionCount(int submissionCount) { this.submissionCount = submissionCount; }
}
