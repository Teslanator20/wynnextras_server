package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "gambit_approved")
public class GambitApproved {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String gambitsJson; // JSON array of approved gambits

    @Column(nullable = false)
    private Instant approvedAt;

    @Column(nullable = false, length = 10)
    private String dayIdentifier; // e.g., "2026-01-27"

    @Column(nullable = false)
    private boolean locked; // locked at 2+ submissions

    public GambitApproved() {}

    public GambitApproved(String gambitsJson, String dayIdentifier, boolean locked) {
        this.gambitsJson = gambitsJson;
        this.dayIdentifier = dayIdentifier;
        this.locked = locked;
        this.approvedAt = Instant.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGambitsJson() { return gambitsJson; }
    public void setGambitsJson(String gambitsJson) { this.gambitsJson = gambitsJson; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }

    public String getDayIdentifier() { return dayIdentifier; }
    public void setDayIdentifier(String dayIdentifier) { this.dayIdentifier = dayIdentifier; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
}
